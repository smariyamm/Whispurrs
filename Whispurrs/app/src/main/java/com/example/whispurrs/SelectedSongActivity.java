package com.example.whispurrs;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SelectedSongActivity extends AppCompatActivity {
    private Runnable progressRunnable;
    private MediaPlayer mediaPlayer;
    private Handler progressHandler = new Handler();

    private ProgressBar progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_selected_song);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.selected_song_screen), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Button back = findViewById(R.id.backbutton);
        back.setOnClickListener(v -> {
            Intent intent = new Intent(SelectedSongActivity.this, BeatsLibrary.class);
            startActivity(intent);
        });


        String songName = getIntent().getStringExtra("songName");
        String songUrl = getIntent().getStringExtra("songUrl");

        // Example: show the song name in a TextView
        TextView name1 = findViewById(R.id.name1); // ensure this ID exists in activity_selected_song.xml
        TextView name2 = findViewById(R.id.name2);
        TextView name3 = findViewById(R.id.name3);
        TextView name4 = findViewById(R.id.name4);
        TextView name5 = findViewById(R.id.name5);
        name1.setText(songName);
        name2.setText(songName);
        name3.setText(songName);
        name4.setText(songName);
        name5.setText(songName);


        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(songUrl);
            mediaPlayer.setOnPreparedListener(mp -> {
                progress.setMax(mediaPlayer.getDuration()); // Set max when ready
                startUpdatingProgress(); // Start syncing only after playback starts
            });
            mediaPlayer.prepareAsync(); // Use async for streaming or slow media
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        progressHandler.removeCallbacks(progressRunnable);
    }


    private void startUpdatingProgress() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    progress.setProgress(mediaPlayer.getCurrentPosition(), true);
                    progressHandler.postDelayed(this, 500);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }


}