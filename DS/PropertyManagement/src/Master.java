import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.roomrental.model.DateRange;
import com.example.roomrental.model.Property;

public class Master {
	
	private int propertyCounter;//diamerismata pou einai sto systhma
    private ArrayList<Socket> workerSockets;//ta sockets twn workers
    private ArrayList<ObjectOutputStream> workerOutputStreams;//output streams twn workers gia na ginetai h metadosh mynhmatwn se aytous
    private int connectionCounter;//connecitons pou uparxoun
    private ArrayList<String> clientRequests;//Katastash request pelath, einai "SATISFIED", "RECEIVED" h "PENDING"
    private ArrayList<Object> clientResponses;//Antikeimena pou epistrefontai ws apanthsh apton reducer
    Socket sock;
    ServerSocket serverSocket;//Socket tou master
    
    public Master() throws UnknownHostException, IOException, ClassNotFoundException, ParseException {
    	
    	sock=null;
    	this.propertyCounter=0;
        workerSockets=new ArrayList<>();
        workerOutputStreams = new ArrayList<>();
        connectionCounter=0;
        clientRequests=new ArrayList<>();
        clientResponses=new ArrayList<>();
        
        BufferedReader configReader = new BufferedReader(new FileReader("config.txt"));
        
        String firstLine=configReader.readLine();
        String parts [] = firstLine.split(":");
        
        int masterPort=Integer.parseInt(parts[1]);
        
        InetAddress inetAddress = InetAddress.getByName(parts[0]);
        serverSocket = new ServerSocket(masterPort, 50, inetAddress);

        configReader.close(); // Close the reader when done reading
        
    }


    public void start() throws IOException, ParseException, ClassNotFoundException{

    	try {
		    Thread.sleep(20000); // Perimene 20 deytera
		} catch (InterruptedException e) {
		    // Xeirismos exception
		    e.printStackTrace();
		}
    	
    	//diavase ta ports apto arxeio
    	BufferedReader configReader2 = new BufferedReader(new FileReader("config.txt"));
        String line2;
        int counter=0;
        String parts [];
        while ((line2 = configReader2.readLine()) != null) {
            if (line2.contains("%")) {
            	counter++;
                continue; // telos diavasmatos otan vrethei to %
                
            }
            
            if (counter==2) {
            	break;
            }

            if (counter==1) {
            	parts=line2.split(":");
            	int port = Integer.parseInt(parts[1]);
                sock=new Socket(parts[0], port);
    			workerOutputStreams.add(new ObjectOutputStream(sock.getOutputStream()));
    			workerSockets.add(sock);
            }
            
        }
        configReader2.close(); // kleisimo arxeiou
    	
        System.out.println("Master server started...");
        while (true) {
        	//apodoxh newn connections sto master socket
        	Socket clientSocket = serverSocket.accept();
            
            // Xeirismos request tou client se neo thread pou trexei parallhla
            new Thread(() -> {
                try {
                    handleClientRequest(clientSocket);
                } catch (IOException | ClassNotFoundException | ParseException e) {
                    e.printStackTrace();
                }
            }).start();
        }
  
    }

    private void handleClientRequest(Socket clientSocket) throws IOException, ParseException, ClassNotFoundException {
    	
    	//topikes metablhtes
    	String managerUsername;
    	String renterUsername;
    	String propertyName;
    	Integer stars;
    	String area;
    	DateRange dateRange;
    	Integer people;
    	Integer price;
    	
    	//settarisma input kai output stream gia antallagh mynhmatwn me ton client
    	 try (ObjectInputStream objectInputStream = new ObjectInputStream(clientSocket.getInputStream());
    			 ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientSocket.getOutputStream());
    	        ) {
    		 
    		 //diavasma mode ths sygkrekrimenhs syndeshs (reducer h client syndesh)
    	     String mode = (String) objectInputStream.readObject();
    	     int clientId;
    	     
    	     System.out.println(mode+" connected to Master");
    	     
    	     if (mode.equals("CLIENT")) {
    	    	 //client initialization
    	    	 clientRequests.add(null);
    	    	 clientResponses.add(null);
    	    	 clientId=connectionCounter;
    	    	 connectionCounter++;
    	     }
    	     else {
    	    	 //to client id gia ton reducer einai -1
    	    	 clientId=-1;
    	     }
    		 
    	     if (mode.equals("CLIENT")) {
    	    	 while (true) {
        			 System.out.println("Awaiting action command from user");
    	    	     String action = (String) objectInputStream.readObject();
    	            switch (action) {
    	                case "ADD":
    	                	//diavase plhrorofires diamerismatos kai dwstes ston katallhlo worker
    	                	readPropertyFromStream(objectInputStream);
    	                    break;
    	                case "SHOW":
    	                	managerUsername = (String) objectInputStream.readObject();
    	                	System.out.println(managerUsername);
    	                	clientRequests.set(clientId,"PENDING");
    	                	
    	                	//steile request se olous tous workers
    	                	showManagerProperties(managerUsername, clientId);
    	                	//peirmene mexri to request na ikanopoithei
    	                	while (clientRequests.get(clientId).equals("PENDING")) {
    	                	    try {
    	                	    	
    	                	        Thread.sleep(2000); 
    	                	        
    	                	    } catch (InterruptedException e) {
    	                	        e.printStackTrace();
    	                	    }
    	                	}
    	                	//pare to apotelesma
    	                	ArrayList<Property> mergedProperties = (ArrayList<Property>) clientResponses.get(clientId);
//    	                	String returnedPropertiesAsString="";
//    	                	
//    	                	//ftiaxe string me thn apanthsh
//    	                	for (int i=0;i<mergedProperties.size();i++) {
//    	                		returnedPropertiesAsString=returnedPropertiesAsString+mergedProperties.get(i).toString();
//    	                	}
    	                	objectOutputStream.writeObject(mergedProperties);
    	                	clientRequests.set(clientId,"SATISFIED");
    	                    break;
    	                case "AVAILABILITY":
    	                	//steile request diathesimothtas ston katallhlo worker
    	                	readAvailabilityFromStream(objectInputStream);
    	                    break;
    	                case "BOOKINGS":
    	                	managerUsername = (String) objectInputStream.readObject();
    	                	
    	                	clientRequests.set(clientId,"PENDING");
    	                	//stiele request se olous tous worker
    	                	showManagerBookings(managerUsername, clientId);
    	                	//perimene mexri na lhfthei apanthsh gia to request
    	                	while (clientRequests.get(clientId).equals("PENDING")) {
    	                		
    	                	    try {
    	                	    	
    	                	        Thread.sleep(2000); 
    	                	        
    	                	    } catch (InterruptedException e) {
    	                	        e.printStackTrace();
    	                	    }
    	                	}
    	                	
    	                	//pare to apotelesma
    	                	Map<String, List<DateRange>> mergedPropertyNamesBookings = (Map<String, List<DateRange>>) clientResponses.get(clientId);
//    	                	StringBuilder stringBuilder = new StringBuilder();
//    	                	for (Map.Entry<String, List<DateRange>> entry : mergedPropertyNamesBookings.entrySet()) {
//    	                	    String tempPropertyName = entry.getKey();
//    	                	    List<DateRange> bookings = entry.getValue();
//    	                	    // vale to onoma tou diamerismatos
//    	                	    stringBuilder.append("Property Name: ").append(tempPropertyName).append("\n");
//    	                	    // loupa gia tis diathesimes hmeromhnies kathe diamerismatos
//    	                	    for (DateRange tempDateRange : bookings) {
//    	                	        // Prosthese kathe euros hmeromhniwn
//    	                	        stringBuilder.append("Date Range: ").append(tempDateRange).append("\n");
//    	                	    }
//    	                	    
//    	                	    stringBuilder.append("------------------------------------\n");
//    	                	}
    	                	//ftiaxe string me thn apanthsh
//    	                	String returnedBookingsAsString = stringBuilder.toString();
    	                	objectOutputStream.writeObject(mergedPropertyNamesBookings);
    	                	clientRequests.set(clientId,"SATISFIED");
    	                	
    	                    break;
    	                case "BOOK":
    	                    // steile request book ston katallhlo worker
    	                	renterUsername = (String) objectInputStream.readObject();
    	                	propertyName = (String) objectInputStream.readObject();
    	                	DateRange bookingRange = readDateRangeFromStream(objectInputStream);
    	                    forwardPropertyBooking(renterUsername, propertyName, bookingRange);
    	                    objectOutputStream.writeObject("Booking added.");
    	                    break;
    	                case "RATE":
    	                    // kane rate request ston katallhlo worker
    	                	renterUsername = (String) objectInputStream.readObject();
    	                	propertyName = (String) objectInputStream.readObject();
    	                	stars = (Integer) objectInputStream.readObject();
    	                    forwardPropertyRating(renterUsername, propertyName, stars);
    	                    objectOutputStream.writeObject("Rating added");
    	                    break;
    	                case "FILTER":
    	                	// steile filter request se olous tous workers
    	                	area = (String) objectInputStream.readObject();
    	                	dateRange=readDateRangeFromStream(objectInputStream);
    	                	people = (Integer) objectInputStream.readObject();
    	                	price = (Integer) objectInputStream.readObject();
    	                	stars = (Integer) objectInputStream.readObject();
    	                	clientRequests.set(clientId,"PENDING");
    	                	
    	                	forwardPropertyFiltering(area, dateRange, people, price, stars, clientId);
    	                	//peirmene mexri na lifthei apanthsh
    	                	while (clientRequests.get(clientId).equals("PENDING")) {
    	                	    try {
    	                	    	
    	                	        Thread.sleep(2000); 
    	                	        
    	                	    } catch (InterruptedException e) {
    	                	        e.printStackTrace();
    	                	    }
    	                	}
    	                	
    	                	ArrayList<Property> mergedFilteredProperties = (ArrayList<Property>) clientResponses.get(clientId);
//    	                	String returnedFilteredPropertiesAsString="";
    	                	
//    	                	for (int i=0;i<mergedFilteredProperties.size();i++) {
//    	                		returnedFilteredPropertiesAsString=returnedFilteredPropertiesAsString+mergedFilteredProperties.get(i).toString();
//    	                	}
    	                	
    	                	//steile string me thn apanthsh
    	                	objectOutputStream.writeObject(mergedFilteredProperties);
    	                	clientRequests.set(clientId,"SATISFIED");
    	                	
    	                    break;
    	                // 
    	                default:
    	                    break;
    	               }
    	    	 }
    	    	 
    	     }
    	     else if (mode.equals("REDUCER")) {
    	    	 while (true) {
    	    		 //steile apanthsh se equest tou client
    	    		 Integer currentClientId= (Integer) objectInputStream.readObject();
    	    		 Object currentClientResponse= (Object) objectInputStream.readObject();
    	    		 clientRequests.set(currentClientId, "RECEIVED");
    	    		 clientResponses.set(currentClientId, currentClientResponse);
    	    	 }
    	     }
    	     
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readPropertyFromStream(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
    	
    	String managerUsername = (String) objectInputStream.readObject();
    	
        String propertyInfo = (String) objectInputStream.readObject();

        // diavase plhroforires diamerismatos
        String[] keyValuePairs = propertyInfo.substring(1, propertyInfo.length() - 1).split(",");

        String roomName = "";
        int noOfPersons = 0;
        String area = "";
        int stars = 0;
        int noOfReviews = 0;
        String roomImage = "";

        // loupa gia ta stoixeia diamerismatwn
        for (String pair : keyValuePairs) {
            String[] keyValue = pair.split(":");
            String key = keyValue[0].trim().replaceAll("\"", "");
            String value = keyValue[1].trim().replaceAll("\"", "");

            switch (key) {
                case "roomName":
                    roomName = value;
                    break;
                case "noOfPersons":
                    noOfPersons = Integer.parseInt(value);
                    break;
                case "area":
                    area = value;
                    break;
                case "stars":
                    stars = Integer.parseInt(value);
                    break;
                case "noOfReviews":
                    noOfReviews = Integer.parseInt(value);
                    break;
                case "roomImage":
                    roomImage = value;
                    break;
            }
        }

        // Diavase byte eikonas
        byte[] imageData = (byte[]) objectInputStream.readObject();
        forwardPropertyAddition(roomName, managerUsername ,new Property(roomName, noOfPersons, area, stars, noOfReviews, roomImage,propertyCounter, imageData), imageData);
        propertyCounter=propertyCounter+1;
    }
    
    private void readAvailabilityFromStream(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
    	
    	String managerUsername = (String) objectInputStream.readObject();
    	
        String propertyName = (String) objectInputStream.readObject();
        
        String dateRangeAvailability = (String) objectInputStream.readObject();
        
        forwardPropertyAvailability(managerUsername, propertyName,dateRangeAvailability);

    }

    private Map<String, Object> readFiltersFromStream(BufferedReader reader) throws IOException {
        Map<String, Object> filters = new HashMap<>();
        
        String line;
        while ((line = reader.readLine()) != null) {
            String[] tokens = line.split(":");
            String filterName = tokens[0];
            Object filterValue = tokens[1]; // Assuming filter value is string for simplicity, you may need to parse it accordingly
            filters.put(filterName, filterValue);
        }
        
        return filters;
    }

    private DateRange readDateRangeFromStream(ObjectInputStream objectInputStream) throws IOException, ParseException, ClassNotFoundException {
    	
    	String startDateStr = (String) objectInputStream.readObject();
    	
        String endDateStr = (String) objectInputStream.readObject();
        
        if (startDateStr.equals("-") && endDateStr.equals("-"))return null;

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
        Date startDate = dateFormat.parse(startDateStr);
        Date endDate = dateFormat.parse(endDateStr);
        
        return new DateRange(startDate, endDate);
    }

    private void forwardPropertyAddition(String propertyName, String managerUsername,Property property, byte [] imageBytes) throws IOException {
        // dialexe worker me vash to onoma tou property
        int workerIndex = Math.abs(propertyName.hashCode()) % workerSockets.size();

        // Valto property ston katallhlo wokrer
        
        int port;
        
        if (workerIndex==0)port=5001;
        else if (workerIndex==1)port=5002;
        else port=5002;
        
        ObjectOutputStream objectOutputStream=workerOutputStreams.get(workerIndex);

       System.out.println("Established connection with worker on port:"+ port);
       System.out.println("Adding new property...");

       String add = "ADD";
       objectOutputStream.writeObject(add);
       System.out.println("ADD SENT");
       objectOutputStream.writeObject(managerUsername);
       objectOutputStream.writeObject(property);
       objectOutputStream.writeObject(imageBytes);

          
    }
    
    private void forwardPropertyAvailability(String managerUsername,String propertyName,String dateRangeAvailability) throws IOException {
        // dialexe worker me vash to onoma tou diamerismatos
        int workerIndex = Math.abs(propertyName.hashCode()) % workerSockets.size();

        // dhlwse tis hmoermhnies diathesimothtas tou
        
        ObjectOutputStream objectOutputStream=workerOutputStreams.get(workerIndex);
        

       System.out.println("Adding new availability...");

       String availability = "AVAILABILITY";
       objectOutputStream.writeObject(availability);
       objectOutputStream.writeObject(managerUsername);
       objectOutputStream.writeObject(propertyName);
       objectOutputStream.writeObject(dateRangeAvailability);

    }

    private void showManagerProperties(String managerUsername, Integer clientId) throws IOException, ClassNotFoundException {
        
    	// Loopa gia tous workers
    	for (int i = 0; i <= 2; i++) { 

    	    ObjectOutputStream objectOutputStream=workerOutputStreams.get(i);

    	        // Steile "SHOW" request sto worker
    	        objectOutputStream.writeObject("SHOW");
    	        objectOutputStream.writeObject(managerUsername);
    	        objectOutputStream.writeObject(clientId);
    	        
    	 
    	}
    	
    	return;
    	
    }
    	
    	private void showManagerBookings(String managerUsername, Integer clientId) throws IOException, ClassNotFoundException {
        	
        	// Loupa gia ta ports twn workers
        	for (int i = 0; i <= 2; i++) { 
        	    
        	    ObjectOutputStream objectOutputStream=workerOutputStreams.get(i);

        	        // Steile "SHOW" request ston worker
        	        objectOutputStream.writeObject("BOOKINGS");
        	        objectOutputStream.writeObject(managerUsername);
        	        objectOutputStream.writeObject(clientId);
        	        
        	        
        	}
    	
    	
    }


    private void forwardPropertyBooking(String renterUsername, String propertyName, DateRange bookingRange) throws IOException {
    	// Dialexe worker analoga me ton onoma diamerismatos
        int workerIndex = Math.abs(propertyName.hashCode()) % workerSockets.size();
        
        ObjectOutputStream objectOutputStream=workerOutputStreams.get(workerIndex);

       System.out.println("Adding new booking...");

       String book = "BOOK";
       objectOutputStream.writeObject(book);
       objectOutputStream.writeObject(renterUsername);
       objectOutputStream.writeObject(propertyName);
       objectOutputStream.writeObject(bookingRange);

           
    }
    
    private void forwardPropertyRating(String renterUsername, String propertyName, Integer stars) throws IOException {
    	// Dialexe worker analoga me ton onoma diamerismatos
        int workerIndex = Math.abs(propertyName.hashCode()) % workerSockets.size();

        // Dialexe asteria gia to diamerisma

        ObjectOutputStream objectOutputStream=workerOutputStreams.get(workerIndex);

       System.out.println("Adding new rating...");

       String rate = "RATE";
       objectOutputStream.writeObject(rate);
       objectOutputStream.writeObject(renterUsername);
       objectOutputStream.writeObject(propertyName);
       objectOutputStream.writeObject(stars);

    }
    
    private void forwardPropertyFiltering(String area, DateRange dateRange, Integer people, Integer price, Integer stars, Integer clientId) throws IOException, ClassNotFoundException {
    	
    	for (int i = 0; i <= 2; i++) { 

    	    ObjectOutputStream objectOutputStream=workerOutputStreams.get(i);

    	        // Steile filter request stous workers
	        objectOutputStream.writeObject("FILTER");
	        objectOutputStream.writeObject(area);
	        objectOutputStream.writeObject(dateRange);
	        objectOutputStream.writeObject(people);
	        objectOutputStream.writeObject(price);
	        objectOutputStream.writeObject(stars);
	        objectOutputStream.writeObject(clientId);
	        
    	}
    }
    
    public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException{
            Master master = new Master();
            master.start();
    }
    
}
