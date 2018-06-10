package com.marcosbenevides.safebabyofthings;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.marcosbenevides.safebabyofthings.utils.Constants;

public class ActivityIntentService extends IntentService {

    private static final String TAG = "ActivityIntentService";

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     */
    public ActivityIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
        DetectedActivity activity = result.getMostProbableActivity();
        //ArrayList<DetectedActivity> detectedActivities = (ArrayList<DetectedActivity>) result.getProbableActivities();

        Intent detectionIntent = new Intent(Constants.ACTION);
        detectionIntent.putExtra(Constants.EXTRA, activity);
        LocalBroadcastManager.getInstance(this).sendBroadcast(detectionIntent);
    }
}
