package com.marcosbenevides.safebabyofthings;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.marcosbenevides.safebabyofthings.utils.Constants;

import java.util.ArrayList;

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
        Intent i = new Intent(Constants.ACTION);

        ArrayList<DetectedActivity> detectedActivities = (ArrayList<DetectedActivity>) result.getProbableActivities();

        i.putExtra(Constants.EXTRA, detectedActivities);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);

    }
}
