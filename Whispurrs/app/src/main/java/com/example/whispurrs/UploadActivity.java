package com.example.whispurrs;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class UploadActivity extends AppCompatActivity {



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

        EditText beatname = findViewById(R.id.name);
        EditText beaturl1 = findViewById(R.id.beaturl);
        EditText beatanimationurl = findViewById(R.id.gifurl);

        Button upload = findViewById(R.id.upload);
        Button home = findViewById(R.id.home);
        Button mybeats = findViewById(R.id.mybeatsbutton);

        mybeats.setOnClickListener(v -> {
            //Intent intent = new Intent(UploadActivity.this, )
        });

        home.setOnClickListener(v -> {
            Intent intent = new Intent(UploadActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        upload.setOnClickListener(v -> {
            String name = beatname.getText().toString();
            String beaturl = beaturl1.getText().toString();
            String animationurl = beatanimationurl.getText().toString();

            if (name.isEmpty() || beaturl.isEmpty() || animationurl.isEmpty()) {
                Toast.makeText(UploadActivity.this,
                        "please enter all fields",
                        Toast.LENGTH_LONG).show();
                return;
            }

            // add in checks for if url is legit
            // add user name to song when uploading





        });

    }
}