package com.example.whispurrs;

import android.content.Intent;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SelectedSongActivity extends AppCompatActivity {
    private Runnable progressRunnable;
    private MediaPlayer mediaPlayer;
    private Handler progressHandler = new Handler();

    private ProgressBar progress;
    private FirebaseFirestore db;
    private FirebaseAuth auth;


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

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        String songName = getIntent().getStringExtra("songName");
        String songUrl = getIntent().getStringExtra("songUrl");

        ImageButton addplaylist = findViewById(R.id.addplaylist);
        Log.d("DEBUG", "addplaylist is: " + addplaylist);

        addplaylist.bringToFront();
        addplaylist.setClickable(true);
        addplaylist.setFocusable(true);

        ImageButton addliked = findViewById(R.id.addliked);


        Button back = findViewById(R.id.backbutton);
        back.setOnClickListener(v -> {
            Intent intent = new Intent(SelectedSongActivity.this, BeatsLibrary.class);
            startActivity(intent);

        });

        addplaylist.setOnClickListener(v1 -> {

             if (songUrl == null || songName == null) {
                Toast.makeText(this, "No song is currently playing", Toast.LENGTH_SHORT).show();
                return;
            }

            // Package the current song into the same format your playlists use
            Map<String, String> currentSongData = new HashMap<>();
            currentSongData.put("name", songName);
            currentSongData.put("beat", songUrl);

            // Let the user choose between new playlist or existing
            String[] options = {"Create New Playlist", "Add to Existing Playlist"};

            new AlertDialog.Builder(this)
                    .setTitle("Add to Playlist")
                    .setItems(options, (dialog, which) -> {
                        if (which == 0) {
                            // Create new playlist
                            showCreatePlaylistDialog(currentSongData);
                        } else {
                            // Add to existing playlist
                            showExistingPlaylistsDialog(currentSongData);
                        }
                    })
                    .show();

        });







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

    private void showCreatePlaylistDialog(Map<String, String> beatData) {
        EditText inputName = new EditText(this);
        inputName.setHint("Playlist Name");

        new AlertDialog.Builder(this)
                .setTitle("New Playlist")
                .setView(inputName)
                .setPositiveButton("Create", (dialog, which) -> {
                    String playlistName = inputName.getText().toString().trim();
                    if (!playlistName.isEmpty()) {
                        createPlaylist(playlistName, beatData);
                    } else {
                        Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void createPlaylist(String name, Map<String, String> beatData) {
        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> playlistData = new HashMap<>();
        playlistData.put("name", name);
        playlistData.put("createdAt", FieldValue.serverTimestamp());
        playlistData.put("songs", Collections.singletonList(beatData));

        db.collection("users")
                .document(userId)
                .collection("playlists")
                .add(playlistData)
                .addOnSuccessListener(doc -> Toast.makeText(this, "Playlist created!", Toast.LENGTH_SHORT).show());
    }

    private void showExistingPlaylistsDialog(Map<String, String> beatData) {
        String userId = auth.getCurrentUser().getUid();

        db.collection("users").document(userId).collection("playlists")
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Toast.makeText(this, "No playlists found. Create one first.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] playlistNames = query.getDocuments().stream()
                            .map(doc -> doc.getString("name"))
                            .toArray(String[]::new);
                    String[] playlistIds = query.getDocuments().stream()
                            .map(DocumentSnapshot::getId)
                            .toArray(String[]::new);

                    new AlertDialog.Builder(this)
                            .setTitle("Select Playlist")
                            .setItems(playlistNames, (dialog, which) -> {
                                db.collection("users")
                                        .document(userId)
                                        .collection("playlists")
                                        .document(playlistIds[which])
                                        .update("songs", FieldValue.arrayUnion(beatData))
                                        .addOnSuccessListener(a -> Toast.makeText(this, "Song added!", Toast.LENGTH_SHORT).show());
                            })
                            .show();
                });
    }


}