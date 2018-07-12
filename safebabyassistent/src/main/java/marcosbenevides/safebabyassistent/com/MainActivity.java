package marcosbenevides.safebabyassistent.com;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
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
import android.location.LocationProvider;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import static android.media.RingtoneManager.TYPE_ALARM;
import static android.media.RingtoneManager.TYPE_NOTIFICATION;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final int MY_BLUETOOTH_PERMISSION = 1;
    private static final int MY_LOCATION_PERMISSION = 2;
    private static final String BLUETOOTH_PERMISSION = Manifest.permission.BLUETOOTH_ADMIN;
    private static final String LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final String TAG = "MAINACTIVITY";
    private CardView locationCard, bluetoothCard, textCard, messageCard;
    private TextView telefone;
    private ProgressBar progressBar;
    private Switch locationSwitch, bluetoothSwitch;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private LocationManager mLocationManager;
    private MyBroadcast myBroadcast;
    private BroadcastIntent broadcastIntent;
    private Intent intent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationCard = findViewById(R.id.locationCard);
        bluetoothCard = findViewById(R.id.bluetoothCard);
        textCard = findViewById(R.id.alertCard);
        messageCard = findViewById(R.id.messageCard);
        progressBar = findViewById(R.id.progressBar);

        telefone = findViewById(R.id.telefone);

        locationSwitch = findViewById(R.id.locationSwitch);
        bluetoothSwitch = findViewById(R.id.bluetoothSwitch);

        locationSwitch.setOnClickListener(view -> turnOn("LOCATION"));
        bluetoothSwitch.setOnClickListener(view -> turnOn("BLUETOOTH"));

        locationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                locationCard.setVisibility(View.GONE);
            else
                locationCard.setVisibility(View.VISIBLE);
        });
        bluetoothSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked)
                bluetoothCard.setVisibility(View.GONE);
            else
                bluetoothCard.setVisibility(View.VISIBLE);
        });

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        checkAdapters();

        MyBroadcast myBroadcast = new MyBroadcast();
        IntentFilter intent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(myBroadcast, intent);

        broadcastIntent = new BroadcastIntent();
        IntentFilter filter = new IntentFilter();
        filter.addAction("BABYSAFESERVICE");
        registerReceiver(broadcastIntent, filter);

    }

    private void checkAdapters() {
        if (mBluetoothAdapter.isEnabled()) {
            change_button_state(bluetoothSwitch, true);
        }
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            change_button_state(locationSwitch, true);
        }
    }

    private void turnOn(String tag) {
        if (tag.equals("BLUETOOTH")) {
            showPermissionRequest(new String[]{BLUETOOTH_PERMISSION}, MY_BLUETOOTH_PERMISSION);
        } else {
            showPermissionRequest(new String[]{LOCATION_PERMISSION}, MY_LOCATION_PERMISSION);
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

    private void config_bluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
        } else {
            Log.e(TAG, "BluetoothAdapter is already enabled.");
            change_button_state(bluetoothSwitch, true);
        }
    }

    @SuppressLint("MissingPermission")
    private void config_location() {
        if (mLocationManager != null && !mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), MY_LOCATION_PERMISSION);
        } else {
            Log.d(TAG, "Locations is already enabled.");
            change_button_state(locationSwitch, true);
        }
    }

    private void change_button_state(Switch mSwitch, boolean enable) {
        Log.d(TAG, "Changing status switch: " + enable);
        mSwitch.setChecked(enable);
        check_status();
    }

    private void check_status() {
        if (mBluetoothAdapter.isEnabled() && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            textCard.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                intent = new Intent();
                intent.setClass(this, BabySafeService.class);
                intent.putExtra("StartService", 1);
                startService(intent);
            }, 1500);
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

    /**
     * Called when the location has changed.
     * <p>
     * <p> There are no restrictions on the use of the supplied Location object.
     *
     * @param location The new location, as a Location object.
     */
    @Override
    public void onLocationChanged(Location location) {
    }

    /**
     * Called when the provider status changes. This method is called when
     * a provider is unable to fetch a location or if the provider has recently
     * become available after a period of unavailability.
     *
     * @param provider the name of the location provider associated with this
     *                 update.
     * @param status   {@link LocationProvider#OUT_OF_SERVICE} if the
     *                 provider is out of service, and this is not expected to change in the
     *                 near future; {@link LocationProvider#TEMPORARILY_UNAVAILABLE} if
     *                 the provider is temporarily unavailable but is expected to be available
     *                 shortly; and {@link LocationProvider#AVAILABLE} if the
     *                 provider is currently available.
     * @param extras   an optional Bundle which will contain provider specific
     *                 status variables.
     *                 <p>
     *                 <p> A number of common key/value pairs for the extras Bundle are listed
     *                 below. Providers that use any of the keys on this list must
     *                 provide the corresponding value as described below.
     *                 <p>
     *                 <ul>
     *                 <li> satellites - the number of satellites used to derive the fix
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    /**
     * Called when the provider is enabled by the user.
     *
     * @param provider the name of the location provider associated with this
     *                 update.
     */
    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            change_button_state(locationSwitch, true);
        }
    }

    /**
     * Called when the provider is disabled by the user. If requestLocationUpdates
     * is called on an already disabled provider, this method is called
     * immediately.
     *
     * @param provider the name of the location provider associated with this
     *                 update.
     */
    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            change_button_state(locationSwitch, false);
        }
    }

    private void notification(Integer telefone) {

        NotificationManagerCompat man = NotificationManagerCompat.from(this);
        Uri alarmSound = RingtoneManager.getDefaultUri(TYPE_NOTIFICATION);

        Notification notification = new NotificationCompat.Builder(this, "BSOT")
                .setSmallIcon(R.drawable.ic_child_care_black_24dp)
                .setContentTitle("Atenção!")
                .setContentText(String.format("Ligue já no %d, criança esquecida no veículo!", telefone))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(alarmSound)
                .build();
        man.notify(0, notification);

    }

    private class MyBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED: {
                    if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON) {
                        change_button_state(bluetoothSwitch, true);
                    } else {
                        change_button_state(bluetoothSwitch, false);
                    }
                    break;
                }
            }

        }
    }

    private class BroadcastIntent extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            Integer message = intent.getIntExtra("BABYSAFESERVICE", 0);
            runOnUiThread(() -> {
                telefone.setText(String.valueOf(message));
                progressBar.setVisibility(View.GONE);
                messageCard.setVisibility(View.VISIBLE);
                notification(message);
            });
            //stopService(intent);
        }
    }
}
