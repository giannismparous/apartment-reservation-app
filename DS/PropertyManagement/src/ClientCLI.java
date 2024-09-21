import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientCLI {
    public static void main(String[] args) throws ClassNotFoundException, IOException {
    	
    	//Anoigei to port
    	BufferedReader configReader = new BufferedReader(new FileReader("config.txt"));
    	String [] parts= configReader.readLine().split(":");
    	configReader.close();
        try (
                Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
             BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        		ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

        	//Leei ston master oti eisai client (pelaths h diaxeirisths diamerismatos)
        	objectOutputStream.writeObject("CLIENT");
        	
            System.out.println("Welcome to the Property Rental System!");
            System.out.println("Type 'ADD', 'SHOW', 'AVAILABILITY', 'BOOKINGS', 'BOOK', 'RATE', 'FILTER'");

            while (true) {
                System.out.print("> ");
                String input = consoleReader.readLine().trim();

                if ("EXIT".equalsIgnoreCase(input)) {
                    break;
                }

                //Stelnei to user action ston master 
                objectOutputStream.writeObject(input);

                // ADD, stelnei ta stoixeia tou diamerismatos sto master (leitourgia diaxeisth)
                if (input.contains("ADD")) {
                	
                	// id diaxeiristh
                	System.out.print("Enter manager ID: ");
                    String managerUsername = consoleReader.readLine().trim();
                    objectOutputStream.writeObject(managerUsername);
                	
                    //json arxeio pou perierxei plhrofories diamerismatos
                    System.out.print("Enter JSON file path: ");
                    String jsonFilePath = consoleReader.readLine().trim();

                    // Diavasma JSON arxeiou
                    try (BufferedReader jsonReader = new BufferedReader(new FileReader(jsonFilePath))) {
                        StringBuilder jsonContent = new StringBuilder();
                        String line;
                        while ((line = jsonReader.readLine()) != null) {
                            jsonContent.append(line);
                        }

                        // Steile plhrofories diamerismatos sto server
                        objectOutputStream.writeObject(jsonContent.toString());

                        String[] keyValuePairs = jsonContent.toString().substring(1, jsonContent.toString().length() - 1).split(",");
                        
                        String roomImage = null; // Initialize roomImage outside the switch statement
                        
                        for (String pair : keyValuePairs) {
                        	String[] keyValue = pair.split(":");
                            String key = keyValue[0].trim().replaceAll("\"", "");
                            String value = keyValue[1].trim().replaceAll("\"", ""); // Remove quotes

                         // Diavazei to path ths eikonas
                            switch (key) {
                                case "roomImage":
                                    roomImage = "C:"+value; // Assign value to roomImage if key is "roomImage"
                                    break;
                            }
                            
                        }

                        //Metatroph ths eikonas se array apo bytes
                        try (FileInputStream fileInputStream = new FileInputStream(roomImage)) {
                            byte[] imageBytes = fileInputStream.readAllBytes();

                            // Steile ta bytes ths eikonas ston master
                            objectOutputStream.writeObject(imageBytes);
                        } catch (IOException e) {
                            System.err.println("Error reading image file: " + e.getMessage());
                        }
                    } catch (IOException e) {
                        System.err.println("Error reading JSON file: " + e.getMessage());
                    }
                    
                }// SHOW, deixnei ola ta property tou diaxieiristh (leitourgia diaxeisth)
                else if (input.contains("SHOW")) {
                	
                	// id diaxeiristh
                	System.out.println("Enter manager ID: ");
                    String managerUsername = consoleReader.readLine().trim();
                    
                    objectOutputStream.writeObject(managerUsername);
                    //lamvanei ta stoixeia twn diamerismatwn apton master
                    String response = (String) objectInputStream.readObject();
                    
                    System.out.println("Showing properties received: "+response);
                } // AVAILABILITY, thetei hmeromhnies diathesimothtas enos diamerismatos (leitourgia diaxeisth)
                else if (input.contains("AVAILABILITY")) {
                	
                	// id diaxeiristh
                	System.out.print("Enter manager ID: ");
                    String managerUsername = consoleReader.readLine().trim();
                    
                    // onoma diamerismatos
                    System.out.print("Enter property name: ");
                    String propertyName = consoleReader.readLine().trim();
                    
                  //Diahtesimothta se morfh se morfh "dd/mm/yyyy-dd/mm/yyyy"
                    System.out.print("Enter date range availability: ");
                    String dateRangeAvailability = consoleReader.readLine().trim();
                    
                    objectOutputStream.writeObject(managerUsername);
                    objectOutputStream.writeObject(propertyName);
                    objectOutputStream.writeObject(dateRangeAvailability);
                }// BOOKINGS, deixnei ta bookings (leitourgia diaxeisth)
                else if (input.contains("BOOKINGS")) {
                	
                	// id diaxeiristh
                	System.out.print("Enter manager ID: ");
                    String managerUsername = consoleReader.readLine().trim();
                    
                    objectOutputStream.writeObject(managerUsername);
                    
                  //lamvanei bookings diaxeiristh apton master
                    String response = (String) objectInputStream.readObject();
                    
                    System.out.println(response);
                }// BOOK, kanei krathsh diamerismatos (leitourgia pelath)
                else if (input.contains("BOOK")) {
                	
                	//id pelath
                	System.out.print("Enter renter ID: ");
                    String renterUsername = consoleReader.readLine().trim();
                    
                    //Onoma diamerismatos
                    System.out.print("Enter property name: ");
                    String propertyName = consoleReader.readLine().trim();
                    
                    //Arxikh hmeromhnia se "dd/mm/yyyy"
                    System.out.print("Enter start date: ");
                    String startDate = consoleReader.readLine().trim();
                    //Telikh hmeromhnia se "dd/mm/yyyy"
                    System.out.print("Enter end date: ");
                    String endDate = consoleReader.readLine().trim();
                    
                    objectOutputStream.writeObject(renterUsername);
                    objectOutputStream.writeObject(propertyName);
                    objectOutputStream.writeObject(startDate);
                    objectOutputStream.writeObject(endDate);
                    
                    
                }// Kanei axiologhsh diamerismatos (leitourgia pelath)
                else if (input.contains("RATE")) {
                	
                	//id pelath 
                	System.out.print("Enter renter ID: ");
                    String renterUsername = consoleReader.readLine().trim();
                    
                    // onoma diamerismatos
                    System.out.print("Enter property name: ");
                    String propertyName = consoleReader.readLine().trim();
                    
                    //asteria axiologhshs ( 1 ws 5)
                    System.out.print("Enter stars: ");
                    Integer stars = Integer.parseInt(consoleReader.readLine().trim());
                    
                    objectOutputStream.writeObject(renterUsername);
                    objectOutputStream.writeObject(propertyName);
                    objectOutputStream.writeObject(stars);
                    
                }// FILTER, kanei filter diamerismatos (global leitourgia)
                else if (input.contains("FILTER")) {
                	
                	//Onoma perioxhs
                	System.out.print("Enter Area:");
                    String area = consoleReader.readLine().trim();
                    
                    //arxikh hmeromhnia se "dd/mm/yyyy"
                    System.out.print("Enter start date:");
                    String startDate = consoleReader.readLine().trim();
                    
                    //telikh hmeromhnia se "dd/mm/yyyy"
                    System.out.print("Enter end date:");
                    String endDate = consoleReader.readLine().trim();
                    
                    //arithmos atomwn
                    System.out.print("Enter number of people: ");
                    Integer people = Integer.parseInt(consoleReader.readLine().trim());
                    
                    //timh diamerismatos
                    System.out.print("Enter price: ");
                    Integer price = Integer.parseInt(consoleReader.readLine().trim());
                    
                    //asteria diamerismatos
                    System.out.print("Enter stars: ");
                    Integer stars = Integer.parseInt(consoleReader.readLine().trim());
                    
                    objectOutputStream.writeObject(area);
                    objectOutputStream.writeObject(startDate);
                    objectOutputStream.writeObject(endDate);
                    objectOutputStream.writeObject(people);
                    objectOutputStream.writeObject(price);
                    objectOutputStream.writeObject(stars);
                    
                    String response = (String) objectInputStream.readObject();
                    
                    System.out.println(response);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
