package com.example.whispurrs;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {



    List<CloudBelt> belts = new ArrayList<>(); // stores all cloud belts
    Handler handler = new Handler(); // handler scheduals repeated updates (game loop)
    Runnable runnable; // (moves all clouds each frame)
    FrameLayout parentLayout; // holds all cloud views

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ActivityCounter.home_activity++; // access or update


        Button beats = findViewById(R.id.beats);
        Button playlists = findViewById(R.id.myplaylists);
        Button upload = findViewById(R.id.upload);

        beats.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, BeatsLibrary.class);
            startActivity(intent);
        });
        playlists.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, PlaylistsActivity.class);
            startActivity(intent);
        });
        upload.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, UploadActivity.class);
            startActivity(intent);

        });

//
        parentLayout = findViewById(R.id.mainLayout); // main container where all clouds are added

        // Example: Add 2 belts
        addCloudBelt(R.drawable.cloud1, R.drawable.cloud22, 3, 2f, -110);
        addCloudBelt(R.drawable.cloud11, R.drawable.cloud11, 4, 1f, 100);// each cloud belt, this one using 3 clouds, 2 speed, and vertical 200px
        addCloudBelt(R.drawable.cloud2, R.drawable.cloud11, 4, 2f, 300);
        addCloudBelt(R.drawable.cloud22, R.drawable.cloud11, 3, 1f, 600);
        addCloudBelt(R.drawable.cloud3, R.drawable.cloud11,4, 2f, 1600);
        addCloudBelt(R.drawable.cloud33, R.drawable.cloud11,3, 1f, 960);
        addCloudBelt(R.drawable.cloud4, R.drawable.cloud11,4, 1f, 1800);
        addCloudBelt(R.drawable.cloud44, R.drawable.cloud11,3, 1f, 1250);

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
    }
