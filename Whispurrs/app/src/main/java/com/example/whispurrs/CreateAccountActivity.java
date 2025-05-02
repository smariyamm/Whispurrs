package com.example.whispurrs;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class CreateAccountActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button enter;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);

        // Applying window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();
        EditText user = findViewById(R.id.username);
        EditText pass = findViewById(R.id.password);
        enter = findViewById(R.id.enter);
        enter.setOnClickListener(v -> {
            String username = user.getText().toString();
            String password = pass.getText().toString();
            createAccount(username, password);
        });

        Button back = findViewById(R.id.backbutton);
        back.setOnClickListener(v -> {
            Intent intent = new Intent(CreateAccountActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        // Load the animated GIF into the ImageView using Glide
        ImageView imageViewGif = findViewById(R.id.imageViewGif);
        Glide.with(this)
                .asGif()
                .load(R.drawable.resizedsleepycat)  // Your GIF in the drawable folder
                .into(imageViewGif);  // Load it into the ImageView

    }

    // Java - Example snippet for signing up a new user
    public void createAccount(String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            Intent intent = new Intent(CreateAccountActivity.this, BeatsLibrary.class);
                            startActivity(intent);
                            // Do something with the signed-in user, e.g., navigate to a different screen
                        } else {
                            // If sign in fails, display a message to the user.
                            Exception e = task.getException();
                            // Handle specific errors, e.g., email already exists
                            // You can show a Toast message or update UI
                            Log.e("FirebaseAuth", "Account creation failed", e);
                            Toast.makeText(CreateAccountActivity.this,
                                    "Account creation failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }


}
