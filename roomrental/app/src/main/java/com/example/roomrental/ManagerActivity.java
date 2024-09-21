package com.example.roomrental;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roomrental.model.DateRange;
import com.example.roomrental.model.Property;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ManagerActivity extends AppCompatActivity {

    private ListView propertyListView;
    private String managerId;
    private Socket socket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private ExecutorService executorService;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manager);

        // Retrieve manager ID from intent
        managerId = getIntent().getStringExtra("userID");

        // Initialize ListView
        propertyListView = findViewById(R.id.propertyListView);

        // Initialize ExecutorService
        executorService = Executors.newFixedThreadPool(4);

        // Initialize buttons
        Button addButton = findViewById(R.id.addButton);
        Button showButton = findViewById(R.id.showButton);
        Button bookingsButton = findViewById(R.id.bookingsButton);

        // Set button listeners
        addButton.setOnClickListener(v -> addProperty());
        showButton.setOnClickListener(v -> showProperties());
        bookingsButton.setOnClickListener(v -> showBookings());

        // Setup connection in a background thread
        executorService.execute(this::setupConnection);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown the ExecutorService when the activity is destroyed
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    private void setupConnection() {
        try {
            InputStream inputStream = getAssets().open("config.txt");
            BufferedReader configReader = new BufferedReader(new InputStreamReader(inputStream));
            String[] parts = configReader.readLine().split(":");
            configReader.close();

            socket = new Socket(parts[0], Integer.parseInt(parts[1]));
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());

            objectOutputStream.writeObject("CLIENT");

            runOnUiThread(() -> Toast.makeText(this, "Connected to server", Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Error connecting to server", Toast.LENGTH_SHORT).show());
        }
    }

    private void addProperty() {
        // Show dialog to input property details
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Property");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Create input fields
        EditText roomNameInput = new EditText(this);
        roomNameInput.setHint("Room Name (e.g., room1)");
        layout.addView(roomNameInput);

        EditText noOfPersonsInput = new EditText(this);
        noOfPersonsInput.setHint("Number of Persons (e.g., 5)");
        noOfPersonsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(noOfPersonsInput);

        EditText areaInput = new EditText(this);
        areaInput.setHint("Area (e.g., Area1)");
        layout.addView(areaInput);

        EditText starsInput = new EditText(this);
        starsInput.setHint("Stars (e.g., 3)");
        starsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(starsInput);

        EditText noOfReviewsInput = new EditText(this);
        noOfReviewsInput.setHint("Number of Reviews (e.g., 0)");
        noOfReviewsInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        layout.addView(noOfReviewsInput);

        Button selectImageButton = new Button(this);
        selectImageButton.setText("Select Image");
        selectImageButton.setOnClickListener(v -> pickImage());
        layout.addView(selectImageButton);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            // Get input values
            String roomName = roomNameInput.getText().toString();
            int noOfPersons = Integer.parseInt(noOfPersonsInput.getText().toString());
            String area = areaInput.getText().toString();
            int stars = Integer.parseInt(starsInput.getText().toString());
            int noOfReviews = Integer.parseInt(noOfReviewsInput.getText().toString());

            // Check if an image is selected
            if (selectedImageUri != null) {
                // Create JSON object
                String jsonContent = "{"
                        + "\"roomName\":\"" + roomName + "\","
                        + "\"noOfPersons\":" + noOfPersons + ","
                        + "\"area\":\"" + area + "\","
                        + "\"stars\":" + stars + ","
                        + "\"noOfReviews\":" + noOfReviews
                        + "}";

                // Send JSON content to server
                sendAddPropertyRequest(jsonContent);

                showAvailabilityDialog(roomName);
            } else {
                Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void showAvailabilityDialog(String propertyName) {
        // Show dialog to input availability details
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Availability for " + propertyName);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Create input field
        EditText dateRangeInput = new EditText(this);
        dateRangeInput.setHint("Enter date range (dd/mm/yyyy-dd/mm/yyyy)");
        layout.addView(dateRangeInput);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String dateRange = dateRangeInput.getText().toString();
            sendAvailabilityRequest(propertyName, dateRange);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            // You may want to display the selected image somewhere in your UI
        }
    }

    private void sendAddPropertyRequest(String jsonContent) {
        executorService.execute(() -> {
            try {
                // Send request to the server
                objectOutputStream.writeObject("ADD");
                objectOutputStream.writeObject(managerId);
                objectOutputStream.writeObject(jsonContent);

                // Send image file as byte array
                if (selectedImageUri != null) {
                    try (InputStream inputStream = getContentResolver().openInputStream(selectedImageUri)) {
                        byte[] imageBytes = inputStream.readAllBytes();
                        objectOutputStream.writeObject(imageBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(this, "Error reading image file", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show());
                }

                runOnUiThread(() -> Toast.makeText(this, "Property added successfully", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error adding property", Toast.LENGTH_SHORT).show());
            }
        });
    }



    private void showProperties() {
        executorService.execute(() -> {
            try {
                Log.d("ManagerActivity", "Sending SHOW request to server.");

                // Send request to the server
                objectOutputStream.writeObject("SHOW");
                objectOutputStream.writeObject(managerId);

                Log.d("ManagerActivity", "Request sent. Waiting for response...");

//                List<Property> propertyList = new ArrayList<Property>();
//                propertyList.add(new Property("1",2,"1",1,1,"2",1));
                // Receive response from server
                List<Property> propertyList = (List<Property>) objectInputStream.readObject();

                Log.d("ManagerActivity", "Response received. Number of properties: " + propertyList.size());

                // Update UI with properties
                runOnUiThread(() -> {
                    PropertyListAdapter adapter = new PropertyListAdapter(ManagerActivity.this, propertyList);
                    propertyListView.setAdapter(adapter);
                });

                runOnUiThread(() -> Toast.makeText(this, "Properties retrieved successfully", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                Log.e("ManagerActivity", "IOException occurred while retrieving properties", e);
                runOnUiThread(() -> Toast.makeText(this, "Error retrieving properties: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void setAvailability() {
        // Show dialog to input availability details
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Set Availability");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        // Create input fields
        EditText propertyNameInput = new EditText(this);
        propertyNameInput.setHint("Enter property name");
        layout.addView(propertyNameInput);

        EditText dateRangeInput = new EditText(this);
        dateRangeInput.setHint("Enter date range (dd/mm/yyyy-dd/mm/yyyy)");
        layout.addView(dateRangeInput);

        builder.setView(layout);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String propertyName = propertyNameInput.getText().toString();
            String dateRange = dateRangeInput.getText().toString();
            sendAvailabilityRequest(propertyName, dateRange);
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    private void sendAvailabilityRequest(String propertyName, String dateRange) {
        executorService.execute(() -> {
            try {
                // Send request to the server
                objectOutputStream.writeObject("AVAILABILITY");
                objectOutputStream.writeObject(managerId);
                objectOutputStream.writeObject(propertyName);
                objectOutputStream.writeObject(dateRange);

                runOnUiThread(() -> Toast.makeText(this, "Availability set successfully", Toast.LENGTH_SHORT).show());
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error setting availability", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showBookings() {
        executorService.execute(() -> {
            try {
                // Send request to the server
                objectOutputStream.writeObject("BOOKINGS");
                objectOutputStream.writeObject(managerId);

                // Receive response from server
                Map<String, List<DateRange>> bookings = (Map<String, List<DateRange>>) objectInputStream.readObject();

                runOnUiThread(() -> {
                    // Display bookings properly
                    showBookingsDialog(bookings);
                });

                runOnUiThread(() -> Toast.makeText(this, "Bookings retrieved successfully", Toast.LENGTH_SHORT).show());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error retrieving bookings", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showBookingsDialog(Map<String, List<DateRange>> bookings) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Bookings");

        StringBuilder stringBuilder = new StringBuilder();

        // Iterate over each property and its bookings
        for (Map.Entry<String, List<DateRange>> entry : bookings.entrySet()) {
            String propertyName = entry.getKey();
            List<DateRange> bookingDates = entry.getValue();

            // Add property name to StringBuilder
            stringBuilder.append("Property: ").append(propertyName).append("\n");

            // Add booking dates to StringBuilder
            for (DateRange dateRange : bookingDates) {
                stringBuilder.append("Booking Dates: ").append(dateRange.toString()).append("\n");
            }

            // Add separator
            stringBuilder.append("\n");
        }

        // Set dialog message
        builder.setMessage(stringBuilder.toString());

        // Set OK button
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());

        // Show dialog
        builder.create().show();
    }

    // Custom ArrayAdapter (same as before)
    public class PropertyListAdapter extends ArrayAdapter<Property> {

        public PropertyListAdapter(Context context, List<Property> properties) {
            super(context, 0, properties);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View listItemView = convertView;
            if (listItemView == null) {
                listItemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_property, parent, false);
            }

            Property currentProperty = getItem(position);

            TextView propertyNameTextView = listItemView.findViewById(R.id.propertyNameTextView);
            TextView propertyDetailsTextView = listItemView.findViewById(R.id.propertyDetailsTextView);
            ImageView propertyImageView = listItemView.findViewById(R.id.propertyImageView);

            if (currentProperty != null) {
                propertyNameTextView.setText(currentProperty.getRoomName());
                String propertyDetails = "Stars: " + currentProperty.getStars() +
                        ", Capacity: " + currentProperty.getNoOfPersons() +
                        ", Area: " + currentProperty.getArea() +
                        ", Price: $" + currentProperty.getPrice();
                propertyDetailsTextView.setText(propertyDetails);

                byte[] imageData = currentProperty.getImage();
                if (imageData != null) {
                    Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
                    propertyImageView.setImageBitmap(bitmap);
                } else {
                    // If no image data is available, you can set a placeholder image or hide the ImageView
                    propertyImageView.setImageResource(R.drawable.placeholder_image); // Placeholder image resource
                }
            }

            return listItemView;
        }
    }

}
