import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.roomrental.model.DateRange;
import com.example.roomrental.model.Property;


public class Worker extends Thread {
	
	private ServerSocket serverSocket;//sockets twn worker
    private Socket clientSocket;//socket gia na lamvanei o worker minimata apton client
    private BufferedReader reader;
    private PrintWriter writer;
    private Socket reducerSocket;//reducer socket
    private ObjectOutputStream reducerOutputStream;//reducer outputstream gia na stelnei o wokrer minimata ston reducer
    private Map<String, ArrayList<Integer>> managerPropertyMap;//hashmap me key managerId/managerUsername kai value ta diamerismata tou manager se lista
    private Map<String, String> propertyNameManagerMap;//hashmap witmeh key onoma diamerismatos and value to id tou manager
    private Map<String, Property> namePropertyMap;//hashmap me key onoma diamerismatos kai value to antikeimeno property
    private Map<Integer, Property> propertyMap;//hashmap me key to id tou diamerismatos kai value to property antikeimeno
    private Map<Integer, byte []> photosMap;//hashmap me key id diamerismatos kai value thn eikona se array apo bytes
    private Map<Integer, List<DateRange>> bookingMap;//hashmap me key property id kai value bookings tou diamerismatos se lista
    private Map<Integer, List<String>> bookingRentersMap;//hashmap me key id diamerismatos kai value ta onomata twn pelatwn
    private Map<String, List<Property>> rentersProperties;//hashmap me key onoma pelath kai value ta diamerismata pou exei noikiasei se lista
    private Map<Integer, DateRange> availabilityMap;//hashmap me key id diamerismatos kai value to euros diathesimwn hmeromhniwn
    private Map<String, Integer> propertyNamePropertyIdMap;//hashmap me key to onoma diamerismatos kai value to id tou diamerismatos

    public Worker(int id) throws UnknownHostException, IOException {
    	
    	
    	this.propertyNameManagerMap= new HashMap<>();
    	this.managerPropertyMap = new HashMap<>();
    	this.namePropertyMap = new HashMap<>();
        this.propertyMap = new HashMap<>();
        this.photosMap = new HashMap<>();
        this.bookingMap = new HashMap<>();
        this.bookingRentersMap = new HashMap<>();
        this.rentersProperties = new HashMap<>();
        this.availabilityMap = new HashMap<>();
        this.propertyNamePropertyIdMap = new HashMap<>();
        
        //ports 
        BufferedReader configReader = new BufferedReader(new FileReader("config.txt"));
        String line;
        while ((line = configReader.readLine()) != null) {
            if (line.contains("%")) {
                break; // telos diabasmatos sto %
            }
        }

        for (int i = 0; i < id; i++) {
            line=configReader.readLine();
        }
        // xrhsh port kai ip ths sygkekrimenhs grammhs
        String [] parts=line.split(":");
        int port = Integer.parseInt(parts[1]);
        InetAddress inetAddress = InetAddress.getByName(parts[0]);
        configReader.close();
        
        try {
        	System.out.println("Worker started at port: "+port);
            this.serverSocket = new ServerSocket(port, 50, inetAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    @Override
    public void run() {
    	
    	try {
		    Thread.sleep(20000); // perimene 20 deytera
		} catch (InterruptedException e) {
		    e.printStackTrace();
		}
    	
    	try {
    		
    		BufferedReader configReader = new BufferedReader(new FileReader("config.txt"));
            
            int reducerPort=6000; // Default port
            String [] parts = null;
            String line;
            while ((line = configReader.readLine()) != null) {
            	if (line.contains("%")) {
                    continue; // skip grammes me "%"
                }
            	
            	parts=line.split(":");
                reducerPort = Integer.parseInt(parts[1]); // Enhmerwsh port 
            }

            configReader.close(); // kleisimo reader
			reducerSocket=new Socket(parts[0], reducerPort);
			reducerOutputStream=new ObjectOutputStream(reducerSocket.getOutputStream());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
    	
    	
        try {
            // Apodoxh request apton master ston worker
            clientSocket = serverSocket.accept();
            // Xeirismos requests apton master
            handleRequest();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} finally {
            try {
                // Kleisimo sockets kai streams
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (clientSocket != null) clientSocket.close();
                if (serverSocket != null) serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRequest() throws ClassNotFoundException, ParseException, IOException {
    	
    	//topikes metavlhtes
    	String managerUsername; 
    	String renterUsername; 
    	DateRange bookingRange; 
    	String propertyName; 
    	String dateRangeAvailability; 
    	Integer stars;
    	String area;
    	DateRange dateRange;
    	Integer people;
    	Integer price;
    	int clientId;
    	
		ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
    		while(true) {
    			
    			
    			String action = (String) objectInputStream.readObject();
    	        
            switch (action) {
                case "ADD":
                	managerUsername = (String) objectInputStream.readObject();
                	Property propertyToAdd = (Property) objectInputStream.readObject();
                	byte[] imageData = (byte[]) objectInputStream.readObject();
                	addProperty(managerUsername,propertyToAdd, imageData);
                	System.out.println("Property Added");
                    break;
                case "SHOW":
                	managerUsername = (String) objectInputStream.readObject();
                	clientId = (Integer) objectInputStream.readObject();
                	if (managerPropertyMap.get(managerUsername)==null) {
                		reducerOutputStream.writeObject(clientId);
                		reducerOutputStream.writeObject("SHOW");
                		reducerOutputStream.writeObject(new ArrayList<>());
                	}
                	else {
                		ArrayList<Property> temp=new ArrayList<>();
                		
                		ArrayList<Integer> tempIds=managerPropertyMap.get(managerUsername);
                		
                		for (int i=0;i<tempIds.size();i++) {
                			temp.add(propertyMap.get(tempIds.get(i)));
                		}
                		
                		reducerOutputStream.writeObject(clientId);
                		reducerOutputStream.writeObject("SHOW");
                		reducerOutputStream.writeObject(temp);
                	}
                	
                    System.out.println("Response sent for SHOW action");
                    break;
                case "AVAILABILITY":
                	managerUsername = (String) objectInputStream.readObject();
                	propertyName = (String) objectInputStream.readObject();
                	dateRangeAvailability = (String) objectInputStream.readObject();
                	addAvailability(managerUsername, propertyName, dateRangeAvailability);
                    break;
                case "BOOKINGS":
                	
                	managerUsername = (String) objectInputStream.readObject();
                	clientId = (Integer) objectInputStream.readObject();
                	Map<String,List<DateRange>> propertyNameBookings = new HashMap<>();
                	
                	for (Map.Entry<String, ArrayList<Integer>> entry : managerPropertyMap.entrySet()) {
                        
                        if (entry.getKey().equals(managerUsername)) {
                        	ArrayList<Integer> properties = entry.getValue();
                        	for (int i=0;i<properties.size();i++) {
                        		propertyNameBookings.put(propertyMap.get(properties.get(i)).getRoomName(),bookingMap.get(properties.get(i)));
                        	}
                        }
                        
                    }
                	
                	
                	reducerOutputStream.writeObject(clientId);
                	reducerOutputStream.writeObject("BOOKINGS");
                	reducerOutputStream.writeObject(propertyNameBookings);
                    System.out.println("Response sent for BOOKINGS action");
                    break;
                case "BOOK":
                	boolean unavailable=false;
                	renterUsername = (String) objectInputStream.readObject();
                	propertyName = (String) objectInputStream.readObject();
                	bookingRange = (DateRange) objectInputStream.readObject();
                	if (bookingRange.isContainedWithin(availabilityMap.get(propertyNamePropertyIdMap.get(propertyName)))) {
                		
                		List<DateRange> bookings = bookingMap.get(propertyNamePropertyIdMap.get(propertyName));
                		for (int i=0;i<bookings.size();i++) {
                			if (bookings.get(i).overlaps(bookingRange)) {
                				unavailable=true;
                				break;
                			}
                		}
                		
                		if (!unavailable) {
                			bookingMap.get(propertyNamePropertyIdMap.get(propertyName)).add(bookingRange);
                			bookingRentersMap.get(propertyNamePropertyIdMap.get(propertyName)).add(renterUsername);
                			if (rentersProperties.get(renterUsername) == null)rentersProperties.put(renterUsername, new ArrayList<>());
                			rentersProperties.get(renterUsername).add(propertyMap.get(propertyNamePropertyIdMap.get(propertyName)));
                			System.out.println("Succesful booking");
                		}
                		else {
                			System.out.println("Booking dates unavailable");
                		}
                		
                	}
                	else {
                		System.out.println("It's not available in that date range.");
                	}
                    break;
                case "RATE":
                    renterUsername = (String) objectInputStream.readObject();
                    propertyName = (String) objectInputStream.readObject();
                    stars = (Integer) objectInputStream.readObject();
                    
                    if (rentersProperties.get(renterUsername) == null) {
                        System.out.println("Renter has no bookings to rate.");
                        return;
                    }
                    
                    boolean rated = false;
                    for (Property property : rentersProperties.get(renterUsername)) {
                        if (property.getRoomName().equals(propertyName)) {
                            Property propertyToRate = propertyMap.get(propertyNamePropertyIdMap.get(propertyName));
                            if (propertyToRate != null) {
                                propertyToRate.rate(stars); // Update rating in the property object
                                
                                // Update rating in all relevant data structures
                                namePropertyMap.put(property.getRoomName(), propertyToRate);
                                propertyMap.put(propertyToRate.getId(), propertyToRate);
                                
                                rated = true;
                                break;
                            }
                        }
                    }
                    
                    if (!rated) {
                        System.out.println("Could not find the property to rate.");
                    } else {
                        System.out.println("Successful rating");
                    }
                    break;
                case "FILTER":
                	
                	area = (String) objectInputStream.readObject();
                	dateRange = (DateRange) objectInputStream.readObject();
                	people = (Integer) objectInputStream.readObject();
                	price = (Integer) objectInputStream.readObject();
                	stars = (Integer) objectInputStream.readObject();
                	clientId = (Integer) objectInputStream.readObject();
                	ArrayList<Property> filteredProperties= new ArrayList<Property>();
                	boolean skip;
                	for (Map.Entry<Integer, Property> entry : propertyMap.entrySet()) {
                        skip=false;
                		//agnohse thn perioxh
                		if (!area.equals("-")) {
                			if (!entry.getValue().getArea().equals(area))continue;
                		}
                		//agnohse to euros hmeromhniwn
                		if (dateRange!=null) {
                			if (!dateRange.isContainedWithin(availabilityMap.get(propertyNamePropertyIdMap.get(entry.getValue().getRoomName())))) {
                				continue;
                			}
                			
                			List<DateRange> bookings = bookingMap.get(propertyNamePropertyIdMap.get(entry.getValue().getRoomName()));
                    		for (int i=0;i<bookings.size();i++) {
                    			if (bookings.get(i).overlaps(dateRange)) {
                    				skip=true;
                    				continue;
                    			}
                    		}
                		}
                		
                		if (skip)continue;
                		//agnohse atoma
                		if (!people.equals(0)) {
                			if (((Integer)entry.getValue().getNoOfPersons())<people)continue;
                		}
                		//agnohse timh
						if (!price.equals(0)) {
							if (((Integer)entry.getValue().getPrice())>price)continue;
						}
						//agnohse asteria
						if (!stars.equals(0)) {
							if (((Float)entry.getValue().getStars())<stars)continue;
						}
						
						//an kanei match ola ta filters valto sthn apanthsh
						filteredProperties.add(entry.getValue());
                        
                    }
                	
                	reducerOutputStream.writeObject(clientId);
                	reducerOutputStream.writeObject("FILTER");
                	reducerOutputStream.writeObject(filteredProperties);
                    break;
                
                default:
                    System.out.println("ERROR: Unknown action");
                    break;
            	}
    		}

    }
    

    // Prosthiki diamerismatos 
    public synchronized void addProperty(String managerUsername, Property property, byte [] imageData) {
    	if (managerPropertyMap.containsKey(managerUsername)) {
    		managerPropertyMap.get(managerUsername).add(property.getId());
    	}
    	else {
    		managerPropertyMap.put(managerUsername, new ArrayList<>());
    	    managerPropertyMap.get(managerUsername).add(property.getId());
    	}
    	propertyNameManagerMap.put(managerUsername, property.getRoomName());
    	namePropertyMap.put(property.getRoomName(), property);
        propertyMap.put(property.getId(), property);
        photosMap.put(property.getId(), imageData);
        bookingMap.put(property.getId(), new ArrayList<>());
        bookingRentersMap.put(property.getId(), new ArrayList<>());
        propertyNamePropertyIdMap.put(property.getRoomName(), property.getId());
    }
    
    //synchronized gia na mhn trexei tautoxrona me alla threads
    public synchronized void addAvailability(String managerUsername, String propertyName, String dateRangeAvailability) throws ParseException {
    	if (propertyNameManagerMap.get(managerUsername).equals(propertyName)) {
    		availabilityMap.put(propertyNamePropertyIdMap.get(propertyName),new DateRange(dateRangeAvailability));
    		System.out.println("Availability updated succesfully");
    	}
    	else {
    		System.out.println("You are not the manager of this property.");
    	}
    }

}
