package com.example.roomrental;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.roomrental.model.Property;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomerActivity extends AppCompatActivity {

    private ExecutorService executorService;
    private CalendarView calendarView;
    private String selectedDate;
    private EditText propertyNameEditText;
    private EditText maxPeopleEditText;
    private EditText maxPriceEditText;
    private EditText minimumStarsEditText;
    private EditText areaEditText;
    private EditText editDaysToStay;
    private String customerID;
    private Socket socket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private ListView filteredPropertiesListView; // New ListView variable

    private List<Property> filteredProperties;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer);


        executorService = Executors.newFixedThreadPool(4);

        // Get customer ID from intent
        customerID = getIntent().getStringExtra("userID");
        // Initialize calendar view
        calendarView = findViewById(R.id.calendarView);
        propertyNameEditText = findViewById(R.id.propertyNameEditText);
        maxPeopleEditText = findViewById(R.id.editTextMaxPeople);
        maxPriceEditText = findViewById(R.id.editTextMaxPrice);
        minimumStarsEditText = findViewById(R.id.editTextMinimumStars);
        areaEditText = findViewById(R.id.editTextAreaName);
        editDaysToStay = findViewById(R.id.editDaysToStay);
        filteredPropertiesListView = findViewById(R.id.filteredPropertiesListView);
        executorService.execute(this::initializeConnection);

        // Set listener to handle date selection
        calendarView.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                // Handle selected date
                selectedDate = dayOfMonth + "/" + (month + 1) + "/" + year;
                // You can store this selected date in a variable or pass it to a filter method
            }
        });



        // Setup the "Apply Filters" button
        Button applyFiltersButton = findViewById(R.id.applyFiltersButton);
        applyFiltersButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applyFilters();
            }
        });

        // Set item click listener for the ListView
        filteredPropertiesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Property property = (Property) parent.getItemAtPosition(position);
                showPropertyDialog(property);
            }
        });
    }

    private void showPropertyDialog(Property property) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Property Options")
                .setMessage("Choose an option for " + property.getRoomName())
                .setPositiveButton("Book", (dialog, which) -> {
                    propertyNameEditText.setText(property.getRoomName());
                    bookProperty(property);
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Rate", (dialog, which) -> {
                    showRatingDialog(property);
                })
                .show();
    }

    private void showRatingDialog(Property property) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_rate_property, null);
        builder.setView(dialogView);

        EditText ratingEditText = dialogView.findViewById(R.id.ratingEditText);

        builder.setTitle("Rate Property")
                .setPositiveButton("Submit", (dialog, which) -> {
                    String ratingStr = ratingEditText.getText().toString();
                    if (!ratingStr.isEmpty()) {
                        int rating = Integer.parseInt(ratingStr);
                        if (rating >= 0 && rating <= 5) {
                            rateProperty(property, rating);
                        } else {
                            Toast.makeText(this, "Please enter a rating between 0 and 5", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Please enter a rating", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String calculateEndDate(String startDate, int daysToStay) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(sdf.parse(startDate));
            calendar.add(Calendar.DATE, daysToStay); // Adding days to start date
            return sdf.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void initializeConnection() {
        try {
            // Read IP address and port from config.txt
            String ipAddress = "";
            int port = 0;
            try {
                InputStream inputStream = getAssets().open("config.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = reader.readLine();
                if (line != null && !line.isEmpty()) {
                    String[] parts = line.split(":");
                    ipAddress = parts[0];
                    port = Integer.parseInt(parts[1]);
                }
                Log.d("TAG", "test");
                Log.d("TAG", ipAddress);
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error reading config file", Toast.LENGTH_SHORT).show());
                return; // Exit method if config file cannot be read
            }

            // Initialize socket and streams
            socket = new Socket(ipAddress, port);
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectInputStream = new ObjectInputStream(socket.getInputStream());
            objectOutputStream.writeObject("CLIENT");

            runOnUiThread(() -> Toast.makeText(this, "Connected to server", Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(this, "Error connecting to server", Toast.LENGTH_SHORT).show());
        }
    }


    private void bookProperty(Property property) {
        executorService.execute(() -> {
            try {
                String propertyName = property.getRoomName();
                String startDate = selectedDate;
                int daysToStay = Integer.parseInt(editDaysToStay.getText().toString());
                String endDate = calculateEndDate(startDate, daysToStay);

                objectOutputStream.writeObject("BOOK");
                objectOutputStream.writeObject(customerID);
                objectOutputStream.writeObject(propertyName);
                objectOutputStream.writeObject(startDate);
                objectOutputStream.writeObject(endDate);

                String response = (String) objectInputStream.readObject();
                runOnUiThread(() -> Toast.makeText(this, response, Toast.LENGTH_SHORT).show());
//                runOnUiThread(() -> Toast.makeText(this, "Booking succesful", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error booking property", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void rateProperty(Property property, int rating) {
        executorService.execute(() -> {
            try {
                String propertyName = property.getRoomName();
                int stars = rating;

                objectOutputStream.writeObject("RATE");
                objectOutputStream.writeObject(customerID);
                objectOutputStream.writeObject(propertyName);
                objectOutputStream.writeObject(stars);

                // Get response from server
                String response = (String) objectInputStream.readObject();
                runOnUiThread(() -> Toast.makeText(this, response, Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error rating property", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void applyFilters() {
        executorService.execute(() -> {
            String area = areaEditText.getText().toString().isEmpty() ? "-" : areaEditText.getText().toString();
            String startDate = selectedDate;
            int daysToStay = editDaysToStay.getText().toString().isEmpty() ? 0 : Integer.parseInt(editDaysToStay.getText().toString());
            String endDate = calculateEndDate(startDate, daysToStay);
            int people = maxPeopleEditText.getText().toString().isEmpty() ? 0 : Integer.parseInt(maxPeopleEditText.getText().toString());
            int price = maxPriceEditText.getText().toString().isEmpty() ? 0 : Integer.parseInt(maxPriceEditText.getText().toString());
            int stars = minimumStarsEditText.getText().toString().isEmpty() ? 0 : Integer.parseInt(minimumStarsEditText.getText().toString());

            try {
                objectOutputStream.writeObject("FILTER");
                objectOutputStream.writeObject(area);
                objectOutputStream.writeObject(startDate);
                objectOutputStream.writeObject(endDate);
                objectOutputStream.writeObject(people);
                objectOutputStream.writeObject(price);
                objectOutputStream.writeObject(stars);

//                filteredProperties = new ArrayList<Property>();
//                filteredProperties.add(new Property("1",2,"1",1,1,"2",1));
                filteredProperties = (List<Property>) objectInputStream.readObject();

                runOnUiThread(this::updatePropertyListView);
                runOnUiThread(() -> Toast.makeText(this, "Applied filters", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error applying filters", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void updatePropertyListView() {
        PropertyListAdapter adapter = new PropertyListAdapter(this, filteredProperties);
        filteredPropertiesListView.setAdapter(adapter);
    }

    public class PropertyListAdapter extends ArrayAdapter<Property> {

        public PropertyListAdapter(Context context, List<Property> properties) {
            super(context, 0, properties);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View itemView = convertView;
            if (itemView == null) {
                itemView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_property, parent, false);
            }

            Property currentProperty = getItem(position);

            TextView propertyNameTextView = itemView.findViewById(R.id.propertyNameTextView);
            TextView propertyDetailsTextView = itemView.findViewById(R.id.propertyDetailsTextView);
            ImageView propertyImageView = itemView.findViewById(R.id.propertyImageView);

            if (currentProperty != null) {
                Log.d("PropertyListAdapter", "Binding data for property: " + currentProperty.getRoomName());

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
            } else {
                Log.d("PropertyListAdapter", "Current property is null");
            }

            return itemView;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Shutdown the ExecutorService when the activity is destroyed
        if (executorService != null) {
            executorService.shutdown();
        }
    }

}