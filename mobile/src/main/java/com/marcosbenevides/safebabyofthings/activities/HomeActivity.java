package com.marcosbenevides.safebabyofthings.activities;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.marcosbenevides.safebabyofthings.ActivityIntentService;
import com.marcosbenevides.safebabyofthings.R;
import com.marcosbenevides.safebabyofthings.entities.CountLeSearch;
import com.marcosbenevides.safebabyofthings.utils.Constants;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.media.RingtoneManager.TYPE_ALARM;
import static android.media.RingtoneManager.TYPE_NOTIFICATION;

public class HomeActivity extends AppCompatActivity implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    private static final Integer BABY_PRESENT = 1;
    private static final Integer BABY_NOT_PRESENT = 0;
    private static final String DEVICE_ADDRESS = "5B:44:FF:EF:34:E5";
    private static final String DEVICE_NAME = "sbot";
    private static final UUID SERVICE_UUID = UUID.fromString("997deb98-f7fb-4ca2-a899-684c1d2aee2b");
    private static final UUID BABY_STATUS = UUID.fromString("c012dcbf-a04c-4c55-8cae-28c0ac63c2bc");
    private final String TAG = getClass().getSimpleName();
    private CardView alerta;
    private ProgressBar progressBar;
    private TextView status_search, baby_description;
    private ImageView device_status, baby_status;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mGatt;
    private Handler mHandler;
    private Boolean device_found = false;

    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private DetectionBroadcast mDetectionBroadcast;

    private ActivityRecognitionClient mActivityRecognitionClient;
    private List<ActivityTransition> transitions = new ArrayList<>();

    private CountLeSearch contador;
    private Integer last_baby_status = 1;

    private MediaPlayer mediaPlayer;

    private BluetoothLeScanner mLeScanner;
    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e(TAG, "Connected!");
                mGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e(TAG, "Disconnected! :(");
                scanBabySafe(true);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.e(TAG, "Erro ao conectaro ao servico.");
            } else {
                Log.e(TAG, "Servico encontrado, analisando as caracteriscidas");
                BluetoothGattCharacteristic characteristic = gatt
                        .getService(SERVICE_UUID)
                        .getCharacteristic(BABY_STATUS);
                gatt.readCharacteristic(characteristic);
                gatt.setCharacteristicNotification(characteristic, true);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            BluetoothGattCharacteristic characteristic = mGatt
                    .getService(SERVICE_UUID)
                    .getCharacteristic(BABY_STATUS);
            mGatt.readCharacteristic(characteristic);

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                readBabyStatus(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            if (gatt.connect())
                readBabyStatus(characteristic);
        }
    };
    private ScanCallback mScanLeCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.e(TAG, "OnResultCallback " + result.getDevice().getName() + " - " + result.getDevice().getAddress());
            if (result.getDevice().getName() != null && result.getDevice().getName().equalsIgnoreCase(DEVICE_NAME)) {
                Log.e(TAG, "Results Match " + result.getDevice().getAddress());
                device_found = true;
                connectToSafeBaby(result.getDevice());
                scanBabySafe(false);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            if (!results.isEmpty()) {

                ScanResult result = results.get(0);
                BluetoothDevice device = result.getDevice();

                connectToSafeBaby(device);

            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "Erro ao iniciar o scan " + errorCode);
            super.onScanFailed(errorCode);
        }
    };

    private void readBabyStatus(BluetoothGattCharacteristic characteristic) {
        if (BABY_STATUS.equals(characteristic.getUuid())) {

            byte[] val = characteristic.getValue();
            ByteBuffer wrapper = ByteBuffer.wrap(val);
            last_baby_status = wrapper.getInt();

            Log.e(TAG, " -------> Baby Status: " + last_baby_status);

            if (last_baby_status.equals(BABY_PRESENT)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        baby_description.setText(R.string.presente);
                        Log.e(TAG, "Bebê no carro!");
                        showNotification(1);
                    }
                });

            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        baby_description.setText(R.string.ausente);
                        showNotification(4);
                        if (mediaPlayer.isPlaying()) {
                            mediaPlayer.stop();
                            mediaPlayer.release();
                        }
                    }
                });
            }
        }
    }

    private void connectToSafeBaby(BluetoothDevice device) {

        Log.e(TAG, "Connecting to device " + device.getAddress());
        BluetoothDevice remote = mBluetoothAdapter.getRemoteDevice(device.getAddress());
        mGatt = remote.connectGatt(this, false, mBluetoothGattCallback);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        setTitle("Safe Baby of Things");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        alerta = findViewById(R.id.alerta);
        progressBar = findViewById(R.id.progress_bar);
        status_search = findViewById(R.id.status_search);
        device_status = findViewById(R.id.device_found);
        baby_description = findViewById(R.id.description_baby_status);
        baby_status = findViewById(R.id.image_baby);

        mHandler = new Handler();

        mediaPlayer = MediaPlayer.create(this, R.raw.alarm);

        BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (!mBluetoothAdapter.isEnabled())
            finish();

        mLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mActivityRecognitionClient = ActivityRecognition.getClient(this);

        mDetectionBroadcast = new DetectionBroadcast();

        scanBabySafe(true);

        configTransitions();

    }

    public PendingIntent getActivityDetectionPendingIntent() {

        Intent intent = new Intent(this, ActivityIntentService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void configTransitions() {
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
        );
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()
        );
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
        );
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.WALKING)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()
        );
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
        );
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.STILL)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()
        );
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.ON_FOOT)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build()
        );
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.ON_FOOT)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build()
        );
        mActivityRecognitionClient.requestActivityUpdates(10L, getActivityDetectionPendingIntent());
        //mActivityRecognitionClient.requestActivityTransitionUpdates(new ActivityTransitionRequest(transitions), getActivityDetectionPendingIntent());
    }

    private void progressStatus() {

        if (progressBar.getVisibility() == View.GONE && !device_found) {
            progressBar.setVisibility(View.VISIBLE);
            device_status.setVisibility(View.GONE);
            status_search.setText(getText(R.string.searching_device));
        } else {
            progressBar.setVisibility(View.GONE);
            device_status.setVisibility(View.VISIBLE);
            changeStatus();
        }

    }

    private void changeStatus() {
        if (device_found) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    device_status.setImageDrawable(getResources().getDrawable(R.drawable.ic_check_green_48dp));
                    status_search.setText(R.string.dispositivo_encontrado);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    device_status.setImageDrawable(getResources().getDrawable(R.drawable.ic_close_black_24dp));
                    status_search.setText(R.string.dispositivo_nao_encontrado);
                    showNotification(3);
                }
            });
        }
    }

    private void scanBabySafe(final boolean enable) {

        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mLeScanner.stopScan(mScanLeCallback);
                    progressStatus();
                    //callAlert();
                }
            }, 30000);

            progressStatus();
            mLeScanner.startScan(mScanLeCallback);
        } else {
            progressStatus();
            mLeScanner.stopScan(mScanLeCallback);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mDetectionBroadcast, new IntentFilter(Constants.ACTION));
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDetectionBroadcast);
        super.onPause();
    }

    private void showNotification(Integer nivel) {

        NotificationManagerCompat man = NotificationManagerCompat.from(this);
        man.notify(0, getNotification(nivel));

    }

    private Notification getNotification(Integer nivel) {

        Uri notificationSound = RingtoneManager.getDefaultUri(TYPE_NOTIFICATION);
        Uri alarmSound = RingtoneManager.getDefaultUri(TYPE_ALARM);

        switch (nivel) {
            case 1:
                return new NotificationCompat.Builder(this, "BSOT")
                        .setSmallIcon(R.drawable.ic_child_care_black_24dp)
                        .setContentTitle(getResources().getString(R.string.alerta_nivel_1))
                        .setContentText(getResources().getText(R.string.content_alerta_nivel_1))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setSound(notificationSound)
                        .build();
            case 2:
                return new NotificationCompat.Builder(this, "BSOT")
                        .setSmallIcon(R.drawable.ic_child_care_black_24dp)
                        .setContentTitle(getResources().getString(R.string.alerta_nivel_1))
                        .setContentText(getResources().getText(R.string.content_alerta_nivel_2))
                        .setSound(alarmSound)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .build();
            case 3:
                return new NotificationCompat.Builder(this, "BSOT")
                        .setSmallIcon(R.drawable.ic_phonelink_off_black_24dp)
                        .setContentTitle(getResources().getString(R.string.alerta_device))
                        .setContentText(getResources().getText(R.string.alerta_device_not_found))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setSound(notificationSound)
                        .build();

            default:
                return new NotificationCompat.Builder(this, "BSOT")
                        .setSmallIcon(R.drawable.ic_thumb_up_black_24dp)
                        .setContentTitle(getResources().getString(R.string.alerta_bom))
                        .setContentText(getResources().getText(R.string.alerta_bebe_a_salvo))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setSound(notificationSound)
                        .build();
        }
    }

    private void inVehicle() {

    }

    public void callAlert() {
        alerta.setVisibility(View.VISIBLE);
        mLeScanner.stopScan(mScanLeCallback);
        showNotification(2);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onConnected(@Nullable Bundle bundle) {

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {

                if (location != null) {
                    Log.d(TAG, "Location found ---->" + location.toString());
                    mLocation = location;
                } else {
                    Log.d(TAG, "Location Null");
                }
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection Suspended -> line 326");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "Erro ao conectar (332)" + connectionResult.getErrorMessage());
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mDetectionBroadcast);
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Locations changed -> " + location.toString());
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess())
            Log.d(TAG, "Detectado activity adicionada (394)");
        Log.e(TAG, "Erro ao detectar activity (395) ->" + status.getStatusMessage());

    }

    public class DetectionBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            DetectedActivity detectedActivities = intent.getParcelableExtra(Constants.EXTRA);
            //for (DetectedActivity activity : detectedActivities) {
            switch (detectedActivities.getType()) {
                case DetectedActivity.IN_VEHICLE: {
                    inVehicle();
                    break;
                }
                case DetectedActivity.WALKING: {
                    callAlert();
                    break;
                }
                case DetectedActivity.ON_FOOT: {
                    callAlert();
                    break;
                }
                default: {
                    Log.d(TAG, "Not in vehicle and not walking, what are you doing? " + detectedActivities.toString());
                }
            }
            //}
        }
    }
}
