package com.example.whispurrs;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BeatsLibrary extends AppCompatActivity {

    // UI components
    private GridLayout songContainer;
    private ImageButton pausePlayButton, selectSongButton;
    private ProgressBar progressBar;
    private TextView songTitle;
    private EditText searchInput;
    private View bottomBar, homeMenu;
    private FrameLayout parentLayout;

    // Media player
    private MediaPlayer mediaPlayer;

    // Firebase DB reference
    private DatabaseReference beatsRef;

    // Currently playing song info
    private String currentSongUrl;
    private String currentSongName;

    // Cloud animation
    private List<CloudBelt> cloudBelts = new ArrayList<>();
    private Handler cloudHandler = new Handler();
    private Runnable cloudRunnable;

    // Progress updater
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_beats_library);

        // Hide selected song UI initially
        View selectedSongScreen = findViewById(R.id.selected_song_screen);
        selectedSongScreen.setVisibility(View.GONE);

        bottomBar = findViewById(R.id.bottombar);
        bottomBar.setVisibility(View.GONE);

        homeMenu = findViewById(R.id.activity_home_2);
        homeMenu.setVisibility(View.GONE);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();


        // Handle system bars inset padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initUI(selectedSongScreen);
        initCloudAnimation();
        initFirebase();
        initListeners();

        loadSongs(""); // load all songs on startup
    }

    private void initUI(View selectedSongScreen) {
        songContainer = findViewById(R.id.songContainer);
        parentLayout = findViewById(R.id.mainLayout);

        songTitle = findViewById(R.id.songTitle);
        searchInput = findViewById(R.id.search);

        pausePlayButton = bottomBar.findViewById(R.id.playPauseButton);
        progressBar = bottomBar.findViewById(R.id.songProgress);
        selectSongButton = bottomBar.findViewById(R.id.selectsong);

        // Load GIF animations into ImageViews using Glide
        Glide.with(this)
                .asGif()
                .load(R.drawable.resizedsleepycat)
                .into((ImageView) findViewById(R.id.imageViewGif1));

        Glide.with(this)
                .asGif()
                .load(R.drawable.resizedsleepycat)
                .into((ImageView) findViewById(R.id.imageViewGif));

        // Search input text change listener
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadSongs(s.toString().trim());
            }
        });
    }

    private void initCloudAnimation() {
        // Add cloud belts with different images, counts, speeds, and vertical positions
        addCloudBelt(R.drawable.cloud1, R.drawable.cloud22, 3, 2f, -210);
        addCloudBelt(R.drawable.cloud11, R.drawable.cloud3, 4, 1f, -10);
        addCloudBelt(R.drawable.cloud33, R.drawable.cloud11, 5, 2f, 170);

        cloudRunnable = new Runnable() {
            @Override
            public void run() {
                for (CloudBelt belt : cloudBelts) {
                    moveCloudBelt(belt);
                }
                cloudHandler.postDelayed(this, 10);
            }
        };
        cloudHandler.post(cloudRunnable);
    }

    private void initFirebase() {
        beatsRef = FirebaseDatabase.getInstance().getReference("Beats");
    }

    private void initListeners() {
        // Bottom bar play/pause button toggle
//        pausePlayButton.setOnClickListener(v -> {
//            if (mediaPlayer != null) {
//                if (mediaPlayer.isPlaying()) {
//                    mediaPlayer.pause();
//                    pausePlayButton.setImageResource(R.drawable.play);
//                } else {
//                    mediaPlayer.start();
//                    pausePlayButton.setImageResource(R.drawable.pause);
//                    startUpdatingProgress();
//                }
//            }
//        });

        pausePlayButton.setOnClickListener(v -> {
            MediaPlayer mp = MusicPlayer.getInstance();
            if (mp != null) {
                if (mp.isPlaying()) {
                    mp.pause();
                    pausePlayButton.setImageResource(R.drawable.play);
                } else {
                    mp.start();
                    pausePlayButton.setImageResource(R.drawable.pause);
                    startUpdatingProgress(); // restart progress updates if paused
                }
            }
        });


        // Replay button
        ImageButton replayButton = findViewById(R.id.replay);
        replayButton.setOnClickListener(v -> {
            if (currentSongUrl != null && currentSongName != null) {
                playSong(currentSongUrl, currentSongName);
            } else {
                Toast.makeText(BeatsLibrary.this, "No song to replay", Toast.LENGTH_SHORT).show();
            }
        });

        // Select song button toggles selected song screen visibility and updates displayed names
//        View selectedSongScreen = findViewById(R.id.selected_song_screen);
//        selectSongButton.setOnClickListener(v -> {
//            if (selectedSongScreen.getVisibility() == View.GONE) {
//                selectedSongScreen.setVisibility(View.VISIBLE);
//
//                // Update all name TextViews with current song name
//                int[] nameTextViewIds = {R.id.name1, R.id.name2, R.id.name3, R.id.name4, R.id.name5};
//                for (int id : nameTextViewIds) {
//                    ((TextView) selectedSongScreen.findViewById(id)).setText(currentSongName);
//                }
//            } else {
//                selectedSongScreen.setVisibility(View.GONE);
//            }
//
//            Button backButton = selectedSongScreen.findViewById(R.id.backbutton);
//            backButton.setOnClickListener(v1 -> selectedSongScreen.setVisibility(View.GONE));
//        });

        // Get the selected song screen view
        View selectedSongScreen = findViewById(R.id.selected_song_screen);

// Get buttons inside the selected song screen
        ImageButton addPlaylist = selectedSongScreen.findViewById(R.id.addplaylist);
        Button backButton = selectedSongScreen.findViewById(R.id.backbutton);

// Toggle visibility of selected song screen
        selectSongButton.setOnClickListener(v -> {
            if (selectedSongScreen.getVisibility() == View.GONE) {
                selectedSongScreen.setVisibility(View.VISIBLE);

                // Update all name TextViews with current song name
                int[] nameTextViewIds = {R.id.name1, R.id.name2, R.id.name3, R.id.name4, R.id.name5};
                for (int id : nameTextViewIds) {
                    ((TextView) selectedSongScreen.findViewById(id)).setText(currentSongName);
                }

                // Set up Add to Playlist click listener
                addPlaylist.setOnClickListener(v1 -> {
                    if (currentSongUrl == null || currentSongName == null) {
                        Toast.makeText(this, "No song is currently playing", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, String> currentSongData = new HashMap<>();
                    currentSongData.put("name", currentSongName);
                    currentSongData.put("beat", currentSongUrl);

                    String[] options = {"Create New Playlist", "Add to Existing Playlist"};
                    new AlertDialog.Builder(this)
                            .setTitle("Add to Playlist")
                            .setItems(options, (dialog, which) -> {
                                if (which == 0) {
                                    showCreatePlaylistDialog(currentSongData);
                                } else {
                                    showExistingPlaylistsDialog(currentSongData);
                                }
                            })
                            .show();
                });

                // Set up back button click listener
                backButton.setOnClickListener(v2 -> selectedSongScreen.setVisibility(View.GONE));

            } else {
                selectedSongScreen.setVisibility(View.GONE);
            }
        });




        // Home button toggle menu
        Button homeButton = bottomBar.findViewById(R.id.home);
        homeButton.setOnClickListener(v -> {
            if (homeMenu.getVisibility() == View.VISIBLE) {
                homeMenu.setVisibility(View.GONE);
            } else {
                Button beats = findViewById(R.id.beats);
                Button playlists = findViewById(R.id.myplaylists);
                Button upload = findViewById(R.id.upload);

                beats.setOnClickListener(v1 -> homeMenu.setVisibility(View.GONE));
                playlists.setOnClickListener(v1 -> startActivity(new Intent(BeatsLibrary.this, PlaylistsActivity.class)));
                upload.setOnClickListener(v1 -> startActivity(new Intent(BeatsLibrary.this, UploadActivity.class)));

                homeMenu.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadSongs(String query) {
        songContainer.removeAllViews();

        beatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot songSnap : snapshot.getChildren()) {
                    String songName = songSnap.getKey();
                    String songUrl = songSnap.child("beat").getValue(String.class);
                    String gifUrl = songSnap.child("gif").getValue(String.class);

                    if (songUrl == null) continue;

                    if (query.isEmpty() || songName.toLowerCase().contains(query.toLowerCase())) {
                        Button songButton = createSongButton(songName);

                        songButton.setOnClickListener(v -> {
                            bottomBar.setVisibility(View.VISIBLE);

                            playSong(songUrl, songName);
                            currentSongUrl = songUrl;
                            currentSongName = songName;
                            songTitle.setText(songName);

                            selectSongButton.setPadding(15, 15, 15, 15);
                            Glide.with(BeatsLibrary.this)
                                    .asBitmap()
                                    .load(gifUrl)
                                    .centerCrop()
                                    .into(selectSongButton);

                            ImageView gifView = findViewById(R.id.songgif);
                            gifView.setPadding(45, 45, 45, 45);
                            Glide.with(BeatsLibrary.this)
                                    .asGif()
                                    .load(gifUrl)
                                    .centerCrop()
                                    .into(gifView);
                        });

                        songContainer.addView(songButton);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(BeatsLibrary.this, "Error loading songs", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Button createSongButton(String songName) {
        Button button = new Button(this);
        button.setText(songName);
        button.setTextColor(Color.BLACK);
        button.setTextSize(12);

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(24);
        background.setColor(Color.parseColor("#FFF4B3"));
        button.setBackground(background);

        Typeface typeface = ResourcesCompat.getFont(this, R.font.pixelify_sans_medium);
        button.setTypeface(typeface);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(10, 10, 10, 10);
        button.setLayoutParams(params);

        return button;
    }

//    private void playSong(String url, String name) {
//        if (mediaPlayer != null) {
//            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
//            mediaPlayer.release();
//            mediaPlayer = null;
//        }
//
//        mediaPlayer = new MediaPlayer();
//        try {
//            mediaPlayer.setDataSource(url);
//            mediaPlayer.setOnPreparedListener(mp -> {
//                mp.start();
//                progressBar.setMax(mp.getDuration());
//                startUpdatingProgress();
//                pausePlayButton.setImageResource(R.drawable.pause);
//            });
//            mediaPlayer.prepareAsync();
//        } catch (IOException e) {
//            e.printStackTrace();
//            Toast.makeText(this, "Failed to play song", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void playSong(String url, String name) {
        MediaPlayer mp = MusicPlayer.getInstance();
        MusicPlayer.stop(); // stop previous song

        try {
            mp.reset();
            mp.setDataSource(url);
            mp.setOnPreparedListener(mediaPlayer -> {
                mediaPlayer.start();
                progressBar.setMax(mediaPlayer.getDuration());
                startUpdatingProgress();
                pausePlayButton.setImageResource(R.drawable.pause);
            });
            mp.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to play song", Toast.LENGTH_SHORT).show();
        }
    }


//    private void startUpdatingProgress() {
//        progressRunnable = new Runnable() {
//            @Override
//            public void run() {
//                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
//                    progressBar.setProgress(mediaPlayer.getCurrentPosition(), true);
//                    progressHandler.postDelayed(this, 500);
//                }
//            }
//        };
//        progressHandler.post(progressRunnable);
//    }

    private void startUpdatingProgress() {
        progressHandler.removeCallbacks(progressRunnable); // remove previous callbacks
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                MediaPlayer mp = MusicPlayer.getInstance();
                if (mp != null && mp.isPlaying()) {
                    progressBar.setProgress(mp.getCurrentPosition(), true);
                    progressHandler.postDelayed(this, 500);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }


    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }

    // Cloud belt helper methods

    private void addCloudBelt(int resId1, int resId2, int count, float speed, float y) {
        CloudBelt belt = new CloudBelt();
        belt.speed = speed;
        belt.yPosition = y;
        belt.cloudResID1 = resId1;
        belt.cloudResID2 = resId2;
        belt.cloudCount = count;

        for (int i = 0; i < count; i++) {
            ImageView cloud = new ImageView(this);
            cloud.setImageResource((i % 2 == 0) ? resId1 : resId2);
            cloud.setLayoutParams(new FrameLayout.LayoutParams(500, 500));
            cloud.setY(y);
            parentLayout.addView(cloud);
            belt.clouds.add(cloud);
        }

        // Position clouds horizontally after layout
        parentLayout.post(() -> {
            float width = belt.clouds.get(0).getWidth();
            for (int i = 0; i < belt.clouds.size(); i++) {
                belt.clouds.get(i).setX(i * width);
            }
        });

        cloudBelts.add(belt);
    }

    private void moveCloudBelt(CloudBelt belt) {
        float width = belt.clouds.get(0).getWidth();
        for (ImageView cloud : belt.clouds) {
            cloud.setX(cloud.getX() - belt.speed);
            if (cloud.getX() + width < 0) {
                float maxX = -1;
                for (ImageView c : belt.clouds) {
                    if (c.getX() > maxX) maxX = c.getX();
                }
                cloud.setX(maxX + width);
            }
        }
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
