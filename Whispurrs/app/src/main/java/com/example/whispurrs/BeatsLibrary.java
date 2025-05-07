package com.example.whispurrs;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BeatsLibrary extends AppCompatActivity {

    //    private LinearLayout songContainer;
    private MediaPlayer mediaPlayer;
    private DatabaseReference dbRef;
    private GridLayout songContainer; // was LinearLayout before
    private ImageButton pauseplay;
    private Button selectsong;
    private ProgressBar progress;
    private TextView title;
    private EditText search;
    private String SongUrl;
    private String SongName;

    List<CloudBelt> belts = new ArrayList<>(); // stores all cloud belts
    Handler handler = new Handler(); // handler scheduals repeated updates (game loop)
    Runnable runnable; // (moves all clouds each frame)
    FrameLayout parentLayout; // holds all cloud views
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_beats_library);
        View selected = findViewById(R.id.selected_song_screen);
//        View selectedSongLayout = findViewById(R.id.selected_song_screen);
        selected.setVisibility(View.GONE);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

//        LinearLayout selected = findViewById(R.id.selectedsong);

        // Load the animated GIF into the ImageView using Glide
        ImageView imageViewGif1 = findViewById(R.id.imageViewGif1);
        Glide.with(this)
                .asGif()
                .load(R.drawable.resizedsleepycat)  // Your GIF in the drawable folder
                .into(imageViewGif1);  // Load it into the ImageView

        ImageView imageViewGif = findViewById(R.id.imageViewGif);
        Glide.with(this)
                .asGif()
                .load(R.drawable.resizedsleepycat)  // Your GIF in the drawable folder
                .into(imageViewGif);  // Load it into the ImageView



        songContainer = findViewById(R.id.songContainer);

        parentLayout = findViewById(R.id.mainLayout); // main container where all clouds are added

        // Example: Add 2 belts
        addCloudBelt(R.drawable.cloud1, R.drawable.cloud22, 3, 2f, -210);
        addCloudBelt(R.drawable.cloud11, R.drawable.cloud3, 4, 1f, -10);// each cloud belt, this one using 3 clouds, 2 speed, and vertical 200px
        addCloudBelt(R.drawable.cloud33, R.drawable.cloud11, 5, 2f, 170);


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



        dbRef = FirebaseDatabase.getInstance().getReference("Beats");
        title = findViewById(R.id.songTitle);
        search = findViewById(R.id.search);

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadSongs(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });


        pauseplay = findViewById(R.id.playPauseButton); // Ensure your ImageButton has this ID in XML
        progress = findViewById(R.id.songProgress);
        pauseplay.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    pauseplay.setImageResource(R.drawable.play);
                } else {
                    mediaPlayer.start();
                    pauseplay.setImageResource(R.drawable.pause);
                    startUpdatingProgress(); // resume updating progress
                }
            }
        });

        ImageButton replay = findViewById(R.id.replay);
        replay.setOnClickListener(v -> {
            playSong(SongUrl, SongName);
        });


        selectsong = findViewById(R.id.selectsong);
        selectsong.setOnClickListener(v ->
        {
            if (selected.getVisibility() == View.GONE) {
                selected.setVisibility(View.VISIBLE);
                // Example: show the song name in a TextView
                TextView name1 = selected.findViewById(R.id.name1); // ensure this ID exists in activity_selected_song.xml
                TextView name2 = selected.findViewById(R.id.name2);
                TextView name3 = selected.findViewById(R.id.name3);
                TextView name4 = selected.findViewById(R.id.name4);
                TextView name5 = selected.findViewById(R.id.name5);
                name1.setText(SongName);
                name2.setText(SongName);
                name3.setText(SongName);
                name4.setText(SongName);
                name5.setText(SongName);

            } else {
                selected.setVisibility(View.GONE);
            }

            Button back = selected.findViewById(R.id.backbutton);
            back.setOnClickListener(v1 -> {

                selected.setVisibility(View.GONE);
            });

        });
        loadSongs(""); // load all songs at startup
        }


    private void loadSongs(String query) {
        songContainer.removeAllViews(); // clear old buttons

        dbRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot songSnap : snapshot.getChildren()) {
                    String songName = songSnap.getKey();
                    String songUrl = songSnap.getValue(String.class);

                    if (query.isEmpty() || songName.toLowerCase().contains(query.toLowerCase())) {
                        Button songBtn = new Button(BeatsLibrary.this);
                        songBtn.setTextColor(Color.parseColor("#000000"));
                        songBtn.setTextSize(12);

                        GradientDrawable shape = new GradientDrawable();
                        shape.setCornerRadius(24);
                        shape.setColor(Color.parseColor("#FFF4B3"));
                        songBtn.setBackground(shape);

                        Typeface typeface = ResourcesCompat.getFont(BeatsLibrary.this, R.font.pixelify_sans_medium);
                        songBtn.setTypeface(typeface);

                        songBtn.setText(songName);
                        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                        params.width = 0;
                        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
                        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                        params.setMargins(10, 10, 10, 10);
                        songBtn.setLayoutParams(params);

                        songBtn.setOnClickListener(v -> {
//                            playSong(songUrl, songName);
                            Pair<String, String> songInfo = playSong(songUrl, songName);
                            SongUrl = songInfo.first;
                            SongName = songInfo.second;
                            title.setText(songName);
                        });

                        songContainer.addView(songBtn);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(BeatsLibrary.this, "Error loading songs", Toast.LENGTH_SHORT).show();
            }
        });
    }


//    private void playSong(String url, String name) {
//
//
//        if (mediaPlayer != null) {
//            if (mediaPlayer.isPlaying()) {
//                mediaPlayer.stop();
//            }
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//
////        selectsong.setOnClickListener(v -> {
////            Intent intent = new Intent(BeatsLibrary.this, SelectedSongActivity.class);
////            intent.putExtra("songName", name);
////            intent.putExtra("songUrl", url);
////            startActivity(intent);
////        });
//
//
//        mediaPlayer = new MediaPlayer();
//        try {
//            mediaPlayer.setDataSource(url);
//            mediaPlayer.setOnPreparedListener(mp -> {
//                mp.start();
//                progress.setMax(mp.getDuration());
//                startUpdatingProgress();
//                pauseplay.setImageResource(R.drawable.pause);
//            });
//            mediaPlayer.prepareAsync(); // prepare asynchronously
//        } catch (IOException e) {
//            e.printStackTrace();
//            Toast.makeText(this, "Failed to play song", Toast.LENGTH_SHORT).show();
//        }
//    }

    private Pair<String, String> playSong(String url, String name) {

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                progress.setMax(mp.getDuration());
                startUpdatingProgress();
                pauseplay.setImageResource(R.drawable.pause);
            });
            mediaPlayer.prepareAsync(); // prepare asynchronously
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to play song", Toast.LENGTH_SHORT).show();
        }

        return new Pair<>(url, name);
    }


    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
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

    private void startUpdatingProgress() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    progress.setProgress(mediaPlayer.getCurrentPosition(), true);
                    progressHandler.postDelayed(this, 500); // update every 0.5 sec
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

}


