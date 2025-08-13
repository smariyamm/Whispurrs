package com.example.whispurrs;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistsActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private GridLayout playlistContainer;

    private View bottomBar;
    private View homeMenu;
    private MediaPlayer mediaPlayer;
    private Runnable progressRunnable;
    private Handler progressHandler = new Handler();

    private ImageButton playPauseButton;
    private ProgressBar progressBar;
    private TextView songTitle;
    private ImageButton replayButton;

    private String currentSongUrl;
    private String currentSongName;
    private String currentGifUrl;
    private ImageButton pausePlayButton, selectSongButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_playlists);

        // Window Insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_playlists), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });



        initFirebase();
        initViews();
        initListeners();

        loadPlaylists();

    }

    private void initFirebase() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private void initViews() {
        playlistContainer = findViewById(R.id.playlistContainer);
        bottomBar = findViewById(R.id.bottombar);
        homeMenu = findViewById(R.id.activity_home_2);
        replayButton = bottomBar.findViewById(R.id.replay);



        bottomBar.setVisibility(View.GONE);
        homeMenu.setVisibility(View.GONE);

        playPauseButton = bottomBar.findViewById(R.id.playPauseButton);
        progressBar = bottomBar.findViewById(R.id.songProgress);
        songTitle = bottomBar.findViewById(R.id.songTitle);
        selectSongButton = bottomBar.findViewById(R.id.selectsong);
    }

//    private void initListeners() {
//        findViewById(R.id.backbutton).setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
//        findViewById(R.id.create_new_playlist).setOnClickListener(v -> loadBeatsAndSelect());
//
//        playPauseButton.setOnClickListener(v -> togglePlayback());
//
//        replayButton.setOnClickListener(v -> {
//            playSong(SongUrl, SongName);
//        });
//
//        Button homeButton = bottomBar.findViewById(R.id.home);
//        homeButton.setOnClickListener(v -> toggleHomeMenu());
//    }

    private void initListeners() {
        findViewById(R.id.backbutton).setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));

        findViewById(R.id.create_new_playlist).setOnClickListener(v -> loadBeatsAndSelect());

        playPauseButton.setOnClickListener(v -> togglePlayback());

        replayButton.setOnClickListener(v -> {
            if (currentSongUrl != null && currentSongName != null) {
                playSong(currentSongUrl, currentSongName, currentGifUrl);
            } else {
                Toast.makeText(this, "No song to replay", Toast.LENGTH_SHORT).show();
            }
        });

        Button homeButton = bottomBar.findViewById(R.id.home);
        homeButton.setOnClickListener(v -> toggleHomeMenu());

        View selectedSongScreen = findViewById(R.id.selected_song_screen);
        selectSongButton.setOnClickListener(v -> {
            if (selectedSongScreen.getVisibility() == View.GONE) {
                selectedSongScreen.setVisibility(View.VISIBLE);

                // Update all name TextViews with current song name
                int[] nameTextViewIds = {R.id.name1, R.id.name2, R.id.name3, R.id.name4, R.id.name5};
                for (int id : nameTextViewIds) {
                    ((TextView) selectedSongScreen.findViewById(id)).setText(currentSongName);
                }
            } else {
                selectedSongScreen.setVisibility(View.GONE);
            }

            Button backButton = selectedSongScreen.findViewById(R.id.backbutton);
            backButton.setOnClickListener(v1 -> {
                selectedSongScreen.setVisibility(View.GONE);
                finish(); // close activity only after hiding the selected song screen
            });

        });

    }



    private void togglePlayback() {
        if (mediaPlayer == null) return;

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            playPauseButton.setImageResource(R.drawable.play);
        } else {
            mediaPlayer.start();
            playPauseButton.setImageResource(R.drawable.pause);
        }
    }

    private void toggleHomeMenu() {
        if (homeMenu.getVisibility() == View.VISIBLE) {
            homeMenu.setVisibility(View.GONE);
        } else {
            Button beats = findViewById(R.id.beats);
            Button playlists = findViewById(R.id.myplaylists);
            Button upload = findViewById(R.id.upload);

            beats.setOnClickListener(v -> homeMenu.setVisibility(View.GONE));
            playlists.setOnClickListener(v -> startActivity(new Intent(this, PlaylistsActivity.class)));
            upload.setOnClickListener(v -> startActivity(new Intent(this, UploadActivity.class)));

            homeMenu.setVisibility(View.VISIBLE);
            finish();
        }
    }

    private void loadPlaylists() {
        playlistContainer.removeAllViews();
        String userId = auth.getCurrentUser().getUid();

        db.collection("users")
                .document(userId)
                .collection("playlists")
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query.getDocuments()) {
                        String id = doc.getId();
                        String name = doc.getString("name");
                        List<Map<String, String>> songs = (List<Map<String, String>>) doc.get("songs");
                        addPlaylistCard(id, name, songs);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load playlists", Toast.LENGTH_SHORT).show());
    }

    private void addPlaylistCard(String id, String name, List<Map<String, String>> songs) {
        Button playlistButton = new Button(this);
        playlistButton.setText(name);
        playlistButton.setAllCaps(false);
        playlistButton.setTextSize(16);
        playlistButton.setPadding(20, 20, 20, 20);
        playlistButton.setBackgroundResource(R.drawable.roundedcoral);
        playlistButton.setTextColor(Color.parseColor("#FFDF7B"));

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(16, 16, 16, 16);
        playlistButton.setLayoutParams(params);

        playlistButton.setOnClickListener(v -> showSongsDialog(name, songs));
        playlistContainer.addView(playlistButton);
    }

    private void showSongsDialog(String playlistName, List<Map<String, String>> songs) {
        if (songs == null || songs.isEmpty()) {
            Toast.makeText(this, "No songs in this playlist", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] songNames = songs.stream().map(song -> song.get("name")).toArray(String[]::new);

        new AlertDialog.Builder(this)
                .setTitle("Songs in " + playlistName)
                .setItems(songNames, (dialog, which) -> {
                    String songUrl = songs.get(which).get("beat");
                    String songName = songs.get(which).get("name");
                    String songGifUrl = songs.get(which).get("gif");
                    playSong(songUrl, songName, songGifUrl);

                })
                .show();
    }

//    private void playSong(String url, String name) {
//        songTitle.setText(name);
//
//        releaseMediaPlayer();
//
//        mediaPlayer = new MediaPlayer();
//        try {
//            mediaPlayer.setDataSource(url);
//            mediaPlayer.setOnPreparedListener(mp -> {
//                mp.start();
//                progressBar.setMax(mp.getDuration());
//                startUpdatingProgress();
//                playPauseButton.setImageResource(R.drawable.pause);
//                bottomBar.setVisibility(View.VISIBLE);
//            });
//            mediaPlayer.prepareAsync();
//        } catch (IOException e) {
//            e.printStackTrace();
//            Toast.makeText(this, "Failed to play song", Toast.LENGTH_SHORT).show();
//        }
//    }

    // Update your playSong method to accept gifUrl too and store these fields
    private void playSong(String url, String name, String gifUrl) {
        // Store current playing song info for replay
        currentSongUrl = url;
        currentSongName = name;
        currentGifUrl = gifUrl;

        songTitle.setText(name);

        releaseMediaPlayer();

        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                progressBar.setMax(mp.getDuration());
                startUpdatingProgress();
                playPauseButton.setImageResource(R.drawable.pause);
                bottomBar.setVisibility(View.VISIBLE);
            });
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to play song", Toast.LENGTH_SHORT).show();
        }
    }


    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void loadBeatsAndSelect() {
        DatabaseReference beatsRef = FirebaseDatabase.getInstance().getReference("Beats");
        beatsRef.get().addOnSuccessListener(snapshot -> {
            if (!snapshot.exists()) return;

            List<String> beatNames = new ArrayList<>();
            List<Map<String, String>> beatDataList = new ArrayList<>();

            for (DataSnapshot beatSnap : snapshot.getChildren()) {
                String name = beatSnap.getKey();
                String beatUrl = beatSnap.child("beat").getValue(String.class);
                String gifUrl = beatSnap.child("gif").getValue(String.class);

                beatNames.add(name);
                Map<String, String> beatInfo = new HashMap<>();
                beatInfo.put("name", name);
                beatInfo.put("beat", beatUrl);
                beatInfo.put("gif", gifUrl);
                beatDataList.add(beatInfo);
            }

            new AlertDialog.Builder(this)
                    .setTitle("Select a Beat")
                    .setItems(beatNames.toArray(new String[0]), (dialog, which) -> askPlaylistChoice(beatDataList.get(which)))
                    .show();
        });
    }

    private void askPlaylistChoice(Map<String, String> beatData) {
        String[] options = {"Create New Playlist", "Add to Existing Playlist"};

        new AlertDialog.Builder(this)
                .setTitle("What would you like to do?")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) showCreatePlaylistDialog(beatData);
                    else showExistingPlaylistsDialog(beatData);
                })
                .show();
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

    private void startUpdatingProgress() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    progressBar.setProgress(mediaPlayer.getCurrentPosition(), true);
                    progressHandler.postDelayed(this, 500);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        if (progressRunnable != null) progressHandler.removeCallbacks(progressRunnable);
    }
}
