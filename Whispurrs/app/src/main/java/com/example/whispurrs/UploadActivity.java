package com.example.whispurrs;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class UploadActivity extends AppCompatActivity {

    EditText name, beaturl, animationurl;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_upload);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button upload = findViewById(R.id.upload);
        Button home = findViewById(R.id.home);

        name = findViewById(R.id.name);
        beaturl = findViewById(R.id.beaturl);
        animationurl = findViewById(R.id.gifurl);

        home.setOnClickListener(v -> {
            Intent intent = new Intent(UploadActivity.this, HomeActivity.class);
            startActivity(intent);
        });
    }
}