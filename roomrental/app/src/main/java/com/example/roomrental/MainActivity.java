package com.example.roomrental;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onNextButtonClick(View view) {
        // Retrieve selected user type
        RadioGroup radioGroup = findViewById(R.id.radioGroupUserType);
        int selectedId = radioGroup.getCheckedRadioButtonId();
        RadioButton radioButton = findViewById(selectedId);
        String userType = radioButton.getText().toString();

        // Retrieve user ID
        EditText editTextUserID = findViewById(R.id.editTextUserID);
        String userID = editTextUserID.getText().toString();

        // Start the next activity based on user type
        if (userType.equals("Customer")) {
            Intent intent = new Intent(this, CustomerActivity.class);
            // Pass user ID to the next activity
            intent.putExtra("userID", userID);
            startActivity(intent);
        } else if (userType.equals("Manager")) {
            // Start manager activity
            Intent intent = new Intent(this, ManagerActivity.class);
            // Pass user ID to the next activity if needed
            intent.putExtra("userID", userID);
            startActivity(intent);
        }
    }
}
