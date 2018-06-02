package com.marcosbenevides.safebabyofthings.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import com.marcosbenevides.safebabyofthings.R;

public class SplashScreen extends AppCompatActivity implements View.OnClickListener, LocationListener {

    private static final int MY_BLUETOOTH_PERMISSION = 1;
    private static final int MY_LOCATION_PERMISSION = 2;
    private static final String BLUETOOTH_PERMISSION = Manifest.permission.BLUETOOTH_ADMIN;
    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private final String TAG = getClass().getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private LocationManager mLocationManager;

    private Switch bluetooth_switch, location_switch;
    private Button next_button;
    private MyBroadcast myBroadcast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        bluetooth_switch = findViewById(R.id.bluetooth_switch);
        location_switch = findViewById(R.id.location_switch);
        next_button = findViewById(R.id.next_button);

        bluetooth_switch.setOnClickListener(this);
        location_switch.setOnClickListener(this);
        next_button.setOnClickListener(this);

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        checkAdapters();

        IntentFilter intent_location = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        IntentFilter intent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);


        myBroadcast = new MyBroadcast();

        registerReceiver(myBroadcast, intent);
        registerReceiver(myBroadcast, intent_location);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        //unregisterReceiver(myBroadcast);
        super.onStop();
    }

    private void checkAdapters() {
        if (mBluetoothAdapter.isEnabled()) {
            change_button_state(bluetooth_switch, true);
        }
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            change_button_state(location_switch, true);
        }
    }

    private void config_bluetooth() {

        if (!mBluetoothAdapter.isEnabled()) {
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        } else {
            Log.e(TAG, "BluetoothAdapter is already enabled.");
            change_button_state(bluetooth_switch, true);
        }
    }

    @SuppressLint("MissingPermission")
    private void config_location() {


        if (mLocationManager != null && !mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), MY_LOCATION_PERMISSION);
        } else {
            Log.d(TAG, "Locations is already enabled.");
            change_button_state(location_switch, true);
        }
    }

    private void showPermissionRequest(String[] manifest, int permission) {
        if (manifest.length == 1) {
            if (ContextCompat.checkSelfPermission(this, manifest[0]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, manifest, permission);
            } else if (permission == MY_LOCATION_PERMISSION) {
                config_location();
            } else {
                config_bluetooth();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, manifest[0]) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, manifest[1]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, manifest, permission);
            } else if (permission == MY_LOCATION_PERMISSION) {
                config_location();
            } else {
                config_bluetooth();
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case MY_BLUETOOTH_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    config_bluetooth();
                }
                break;
            }
            case MY_LOCATION_PERMISSION: {

                break;
            }
        }
    }

    private void check_status() {
        if (mBluetoothAdapter.isEnabled() && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(this, HomeActivity.class);
            startActivity(intent);
        }
    }

    private void change_button_state(Switch mSwitch, boolean enable) {
        Log.d(TAG, "Changing status switch: " + enable);
        mSwitch.setChecked(enable);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bluetooth_switch: {
                showPermissionRequest(new String[]{BLUETOOTH_PERMISSION}, MY_BLUETOOTH_PERMISSION);
                break;
            }
            case R.id.location_switch: {
                showPermissionRequest(new String[]{LOCATION_PERMISSION, COARSE_PERMISSION}, MY_LOCATION_PERMISSION);
                break;
            }
            case R.id.next_button: {
                check_status();
                break;
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER))
            change_button_state(location_switch, true);

    }

    @Override
    public void onProviderDisabled(String provider) {

        if (provider.equals(LocationManager.GPS_PROVIDER))
            change_button_state(location_switch, false);

    }

    private class MyBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            switch (action) {

                case BluetoothAdapter.ACTION_STATE_CHANGED: {

                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON)
                        change_button_state(bluetooth_switch, true);
                    else
                        change_button_state(bluetooth_switch, false);
                    break;
                }
            }

        }
    }
}
