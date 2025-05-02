package com.example.whispurrs;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    List<CloudBelt> belts = new ArrayList<>(); // stores all cloud belts
    Handler handler = new Handler(); // handler scheduals repeated updates (game loop)
    Runnable runnable; // (moves all clouds each frame)
    FrameLayout parentLayout; // holds all cloud views
    private Button enter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_login);
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

        // create account button
        Button createaccount = findViewById(R.id.createaccount);
        createaccount.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, CreateAccountActivity.class);
            startActivity(intent);
        });

        mAuth = FirebaseAuth.getInstance();
        EditText user = findViewById(R.id.username);
        EditText pass = findViewById(R.id.password);
        enter = findViewById(R.id.enter);
        enter.setOnClickListener(v -> {
            String username = user.getText().toString();
            String password = pass.getText().toString();
            signIn(username, password);
//            onStart();
        });


        parentLayout = findViewById(R.id.mainLayout); // main container where all clouds are added

        // Example: Add 2 belts
        addCloudBelt(R.drawable.cloud1, R.drawable.cloud22, 3, 2f, -110);
        addCloudBelt(R.drawable.cloud11, R.drawable.cloud11, 4, 1f, 100);// each cloud belt, this one using 3 clouds, 2 speed, and vertical 200px
        addCloudBelt(R.drawable.cloud2, R.drawable.cloud11, 4, 2f, 300);
        addCloudBelt(R.drawable.cloud22, R.drawable.cloud11, 3, 1f, 600);
        addCloudBelt(R.drawable.cloud3, R.drawable.cloud11,4, 2f, 1600);
        addCloudBelt(R.drawable.cloud33, R.drawable.cloud11,3, 1f, 800);
        addCloudBelt(R.drawable.cloud4, R.drawable.cloud11,4, 1f, 1800);
        addCloudBelt(R.drawable.cloud44, R.drawable.cloud11,3, 1f, 950);

        runnable = new Runnable() { // this loop moves clouds continously
            @Override
            public void run() {
                for (CloudBelt belt : belts) { // move each belts clouds
                    moveCloudBelt(belt);
                }
                handler.postDelayed(this, 10); // run again after 10 milliseconds
            }
        };
        handler.post(runnable); // start loop
    }

    void addCloudBelt(int resId1, int resId2,  int count, float speed, float y) { // adds new cloud belt to screen
        CloudBelt belt = new CloudBelt(); // new belt instance
        belt.speed = speed;
        belt.yPosition = y;
        belt.cloudResID1 = resId1;
        belt.cloudResID2 = resId2;
        belt.cloudCount = count;

        for (int i = 0; i < count; i++) {
            ImageView cloud = new ImageView(this);
//            cloud.setImageResource(resId);

            // Randomly select which cloud image to use
            if (i % 2 == 0) {
                cloud.setImageResource(resId1); // Even index gets the first cloud type
            } else {
                cloud.setImageResource(resId2); // Odd index gets the second cloud type
            }

            cloud.setLayoutParams(new FrameLayout.LayoutParams( // controls how big each cloud is
                    500, 500
            ));
            cloud.setY(y);
            parentLayout.addView(cloud);
            belt.clouds.add(cloud);
        }

        // Wait for layout to load to set X properly
        parentLayout.post(() -> {
            float width = belt.clouds.get(0).getWidth();
            for (int i = 0; i < belt.clouds.size(); i++) {
                belt.clouds.get(i).setX(i * width);
            }
        });

        belts.add(belt);
    }

    void moveCloudBelt(CloudBelt belt) { // moves each cloud in a belt that loops around when it exists screen
        float width = belt.clouds.get(0).getWidth();
        for (ImageView cloud : belt.clouds) {
            cloud.setX(cloud.getX() - belt.speed); // move left by speed
            if (cloud.getX() + width < 0) { // if cloud is offscreen to left
                // Move to the rightmost edge of the belt
                float maxX = -1;
                for (ImageView c : belt.clouds) { // find rightmost cloud
                    if (c.getX() > maxX) maxX = c.getX();
                }
                cloud.setX(maxX + width); // place off-screen cloud to right of furthest cloud
            }
        }
    }

    // !!!!!!!!!!!!!!!!!! LOGIN FUNCTIONS/METHODS !!!!!!!!!!!!!!!!!!

    // Java - Example snippet for signing in an existing user
    // to sign out --> mAuth.signOut();
    public void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();
                            // Do something with the signed-in user, e.g., navigate to a different screen
                            Intent intent = new Intent(LoginActivity.this, BeatsLibrary.class);
                            startActivity(intent);
                            finish(); // optional: prevent going back to login
                        } else {
                            // If sign in fails, display a message to the user.
                            Exception e = task.getException();
                            // Handle specific errors, e.g., wrong password, user not found
                            Log.e("FirebaseAuth", "Login failed", e);
                            Toast.makeText(LoginActivity.this,
                                    "Login failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();
                            // You can show a Toast message or update UI
                        }
                    }
                });
    }

    // Java - Checking the current user
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in, navigate to main screen or update UI
        } else {
            // No user signed in, show login/signup UI
            Toast.makeText(LoginActivity.this,
                    "username or password is incorrect. Try Again.",
                    Toast.LENGTH_LONG).show();
        }
    }




}









