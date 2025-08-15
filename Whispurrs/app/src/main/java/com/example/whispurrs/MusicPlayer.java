package com.example.whispurrs;

import android.media.MediaPlayer;

public class MusicPlayer {

    private static MediaPlayer mediaPlayer;

    public static MediaPlayer getInstance() {
        if (mediaPlayer == null) mediaPlayer = new MediaPlayer();
        return mediaPlayer;
    }

    public static void stop() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.reset();
        }
    }

    public static void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
