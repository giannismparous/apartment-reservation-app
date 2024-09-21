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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.roomrental.model.DateRange;
import com.example.roomrental.model.Property;

public class Reducer extends Thread{
	
	private Socket masterSocket;//socket tou master
	private ArrayList<Socket> workerSockets;//sockets twn workers
    private ArrayList<ObjectInputStream> workerInputStreams;//input streams twn workers gia na lambanei minimata apo workers
    private ObjectOutputStream masterOutputStream;//output stream tou master gia na stelnei minima ston master
    private HashMap<Integer,String> clientRequests;//hashmap me key to id tou client kai value to request tou
    private HashMap<Integer,Object> clientResponses;//hashmap me key to id tou client kai value thn apanthsh
    private HashMap<Integer,Integer> clientWorkerResponseCounter;//hashmap me key to id tou client kai value ton arithmo twn workers pou apanthsan
    ServerSocket serverSocket;//socket tou reducer
    private int workerCounter;//metrhths workers workers
	
	public Reducer()  throws UnknownHostException, IOException {
		
		workerSockets=new ArrayList<>();
		workerInputStreams=new ArrayList<>();
		clientRequests=new HashMap<>();
		clientResponses=new HashMap<>();
		clientWorkerResponseCounter=new HashMap<>();
		
		//ports setarisma
		try (BufferedReader configReader = new BufferedReader(new FileReader("config.txt"))) {
            String line;
            String lastLine = null;
            while ((line = configReader.readLine()) != null) {
                lastLine = line;
            }
            // pare apto teleutaio line to ip kai to port kai anoixe socket gia na sou stelnoyn minimata
            if (lastLine != null) {
            	String [] parts=lastLine.split(":");
                int port = Integer.parseInt(parts[1]);
                InetAddress inetAddress = InetAddress.getByName(parts[0]);
                serverSocket = new ServerSocket(port,50,inetAddress);
            } else {
                throw new IOException("Config file is empty");
            }
        }
		
		
		workerCounter=0;
	}
	
	@Override
    public void run(){
		try {
		    Thread.sleep(20000); // Perimene 2 deytera
		} catch (InterruptedException e) {
		    // Xeirismos exception
		    e.printStackTrace();
		}

		try (BufferedReader configReader = new BufferedReader(new FileReader("config.txt"))) {
		    // Xrhsh ip kai port apthn prwth grammh
		    String firstLine = configReader.readLine();
		    if (firstLine != null) {
		    	String [] parts=firstLine.split(":");
		        int port = Integer.parseInt(parts[1]);
		        // Syndesh ston master 
		        masterSocket = new Socket(parts[0], port);
		        masterOutputStream = new ObjectOutputStream(masterSocket.getOutputStream());
		        masterOutputStream.writeObject("REDUCER");
		        System.out.println("Reducer server started...");
		    } else {
		        throw new IOException("Config file is empty");
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
      
      while (true) {
    	  
    	  try {
    		  Socket tempSocket = serverSocket.accept();
    	        workerSockets.add(tempSocket);
    	        workerInputStreams.add(new ObjectInputStream(tempSocket.getInputStream()));

    	        final int currentWorkerIndex = workerCounter; // pare to worker index
    	        final Socket currentSocket = tempSocket; // pare to socket
    	        
    	        new Thread(() -> {
    	            try {
    	                handleWorkerRequest(currentSocket, currentWorkerIndex); // kanei xeirismo request analoga me to socket kai to worker index
    	            } catch (IOException | ClassNotFoundException | ParseException e) {
    	                e.printStackTrace();
    	            }
    	        }).start();
    	        
    	        workerCounter++; // Ayxhse ton metrhth twn workers
    	    } catch (IOException e1) {
    	        e1.printStackTrace();
    	    }
          
      }

  }
	
	private void handleWorkerRequest(Socket currentSocket,int currentWorkerIndex) throws IOException, ParseException, ClassNotFoundException {
		
    	while (true){
    	
    		 
    		 System.out.println("Worker connected to Reducer");
    	     Integer clientId = (Integer) workerInputStreams.get(currentWorkerIndex).readObject();
    	     String action = (String) workerInputStreams.get(currentWorkerIndex).readObject();
    	     Object newData = workerInputStreams.get(currentWorkerIndex).readObject();
    	     //synchronized gia na mhn allazoun ta threads thn idia perioxh mnhmhs tautoxrona
    	     synchronized (clientRequests) {
	    	     if (clientRequests.get(clientId)==null) {
	    	    	 clientRequests.put(clientId,"SATISFIED");
	    	    	 clientWorkerResponseCounter.put(clientId,0);
	    	     }
    	     
	    	     if (clientRequests.get(clientId).equals("SATISFIED") && clientWorkerResponseCounter.get(clientId).equals(0)) {
	    	    	 
	    	    	 clientRequests.put(clientId,"PENDING");
	    	    	 clientWorkerResponseCounter.put(clientId,1);
	    	    	 
	    	    	 if (action.equals("SHOW") || action.equals("FILTER"))clientResponses.put(clientId, (ArrayList<Property>) newData);
	    	    	 else if (action.equals("BOOKINGS"))clientResponses.put(clientId,(Map<String,List<DateRange>>) newData);
	    	    	 
	    	     }
	    	     else if (clientRequests.get(clientId).equals("PENDING")) {
    	    	 
    	    	 

    	    	    if (action.equals("SHOW") || action.equals("FILTER")) {
    	    	    	
    	    	    	ArrayList<Property> existingData;
    	    	    	if (clientResponses.get(clientId)==null)existingData=new ArrayList<>();
    	    	    	else existingData = (ArrayList<Property>) clientResponses.get(clientId);

    	    	        ArrayList<Property> mergedData = new ArrayList<Property>(existingData);
    	    	        mergedData.addAll((Collection<? extends Property>) newData);
    	    	        // Sygxwneush apanthsewn twn workers

    	    	        // enhmerwsh apanthshs ston client
    	    	        clientResponses.put(clientId, mergedData);
    	    	        clientWorkerResponseCounter.put(clientId,clientWorkerResponseCounter.get(clientId)+1);
    	    	        
    	    	    } else if (action.equals("BOOKINGS")) {
    	    	    	
    	    	        Map<String, List<DateRange>> existingData = (Map<String, List<DateRange>>) clientResponses.get(clientId);

    	    	        Map<String, List<DateRange>> mergedData = new HashMap<>(existingData);
    	    	        
    	    	        for (Map.Entry<String, List<DateRange>> entry : ((Map<String, List<DateRange>>) newData).entrySet()) {
    	    	            String propertyName = entry.getKey();
    	    	            List<DateRange> newBookings = entry.getValue();
    	    	            
    	    	            // des an to onoma diamerismatos uparxei hdh, an oxi xrhsimopoihse kenh lista, alliws prosthese ta nea dedomena
    	    	            if (mergedData.containsKey(propertyName)) {
    	    	                mergedData.get(propertyName).addAll(newBookings);
    	    	            } else {
    	    	                mergedData.put(propertyName, new ArrayList<>(newBookings));
    	    	            }
    	    	        }

    	    	        // Enhmerwsh me tis sygxwneumenes apanthseis
    	    	        clientResponses.put(clientId, mergedData);
    	    	        clientWorkerResponseCounter.put(clientId,clientWorkerResponseCounter.get(clientId)+1);
    	    	    }
    	    	    
    	    	    if (clientWorkerResponseCounter.get(clientId).equals(3)) {
    	    	    	clientRequests.put(clientId,"SATISFIED");
    	    	    	clientWorkerResponseCounter.put(clientId,0);
    	    	    	masterOutputStream.writeObject(clientId);
    	    	    	masterOutputStream.writeObject(clientResponses.get(clientId));
    	    	    	clientResponses.put(clientId, null);
    	    	    }  	    
    	     	}
    	     }
    	}
	}
	
	public static void main(String[] args) throws IOException, ParseException, ClassNotFoundException{
        Reducer reducer = new Reducer();
        reducer.start();
	}
	
}
