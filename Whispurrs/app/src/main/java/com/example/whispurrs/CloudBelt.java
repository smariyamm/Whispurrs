package com.example.whispurrs;
import android.widget.ImageView;
import java.util.ArrayList;
import java.util.List;

public class CloudBelt {
    List<ImageView> clouds = new ArrayList<>(); // list of cloud images in belt
    float speed; // speed of the belt moving (pixels per update)
    float yPosition; // the y-coords of where the belt is placed on screen
    float cloudResID1; // resource ID of the cloud (from layout) (e.g., R.drawable.cloud)
    float cloudResID2;
    int cloudCount; // number of cloud ImageViews in the belt (used to determine how many repeat
}
