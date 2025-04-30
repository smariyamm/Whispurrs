package com.example.whispurrs;
//package com.example.whispurrs.ui.activities; // Change to match your manifest

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
public class MainActivity extends AppCompatActivity {

    private Button button;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Link XML layout to this activity

        // Initialize views
        button = findViewById(R.id.button); // Button from the XML
        textView = findViewById(R.id.textView); // TextView from the XML

        // Set button click listener
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Change the text of the TextView when the button is clicked
                textView.setText("Button clicked!");
            }
        });
    }
}
