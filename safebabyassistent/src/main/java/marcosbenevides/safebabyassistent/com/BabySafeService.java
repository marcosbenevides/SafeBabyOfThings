package marcosbenevides.safebabyassistent.com;

import android.app.IntentService;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.UUID;

public class BabySafeService extends IntentService {

    private static final String TAG = "BABYSAFESERVICE";
    private static final UUID ALERT_UUID_SERVICE = UUID.fromString("453f48fa-3de5-4694-9f19-c5564d502db7");
    private static final UUID ALERT_MESSAGE = UUID.fromString("f512b66b-cae7-4354-b0ad-90236bd999df");
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothGatt mBluetoothGatt;
    private int alert_result;
    private BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        /**
         * Callback indicating when GATT client has connected/disconnected to/from a remote
         * GATT server.
         *
         * @param gatt     GATT client
         * @param status   Status of the connect or disconnect operation.
         *                 {@link BluetoothGatt#GATT_SUCCESS} if the operation succeeds.
         * @param newState Returns the new connection state. Can be one of
         *                 {@link BluetoothProfile#STATE_DISCONNECTED} or
         *                 {@link BluetoothProfile#STATE_CONNECTED}
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                Log.d(TAG, "Connected");
                gatt.discoverServices();
            }
            super.onConnectionStateChange(gatt, status, newState);
        }

        /**
         * Callback reporting the result of a characteristic read operation.
         *
         * @param gatt           GATT client invoked {@link BluetoothGatt#readCharacteristic}
         * @param characteristic Characteristic that was read from the associated
         *                       remote device.
         * @param status         {@link BluetoothGatt#GATT_SUCCESS} if the read operation
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            readCharacteristic(characteristic);
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        /**
         * Callback triggered as a result of a remote characteristic notification.
         *
         * @param gatt           GATT client the characteristic is associated with
         * @param characteristic Characteristic that has been updated as a result
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        /**
         * Callback invoked when the list of remote services, characteristics and descriptors
         * for the remote device have been updated, ie new services have been discovered.
         *
         * @param gatt   GATT client invoked {@link BluetoothGatt#discoverServices}
         * @param status {@link BluetoothGatt#GATT_SUCCESS} if the remote device
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            BluetoothGattCharacteristic characteristic = gatt
                    .getService(ALERT_UUID_SERVICE)
                    .getCharacteristic(ALERT_MESSAGE);
            gatt.readCharacteristic(characteristic);
            super.onServicesDiscovered(gatt, status);
        }
    };
    private ScanCallback mLeScanCallback = new ScanCallback() {
        /**
         * Callback when a BLE advertisement has been found.
         *
         * @param callbackType Determines how this callback was triggered. Could be one of
         *                     {@link ScanSettings#CALLBACK_TYPE_ALL_MATCHES},
         *                     {@link ScanSettings#CALLBACK_TYPE_FIRST_MATCH} or
         *                     {@link ScanSettings#CALLBACK_TYPE_MATCH_LOST}
         * @param result       A Bluetooth LE scan result.
         */
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            Log.d(TAG, "Scan found devices -> " + result.getDevice().getAddress());
            connectBLE(result.getDevice());

            super.onScanResult(callbackType, result);
        }
    };

    public BabySafeService() {
        super(TAG);
    }

    /**
     * This method is invoked on the worker thread with a request to process.
     * Only one Intent is processed at a time, but the processing happens on a
     * worker thread that runs independently from other application logic.
     * So, if this code takes a long time, it will hold up other requests to
     * the same IntentService, but it will not hold up anything else.
     * When all requests have been handled, the IntentService stops itself,
     * so you should not call {@link #stopSelf}.
     *
     * @param intent The value passed to {@link
     *               Context#startService(Intent)}.
     *               This may be null if the service is being restarted after
     *               its process has gone away; see
     *               {@link Service#onStartCommand}
     *               for details.
     */
    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        mBluetoothManager = getSystemService(BluetoothManager.class);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mBluetoothLeScanner.startScan(mLeScanCallback);
    }

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void connectBLE(BluetoothDevice device) {

        Log.d(TAG, "Connecting device -> " + device.getAddress());
        mBluetoothGatt = device.connectGatt(this, false, mBluetoothGattCallback);

    }

    private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        byte[] val = characteristic.getValue();
        ByteBuffer wrapper = ByteBuffer.wrap(val);
        alert_result = wrapper.getInt();

        mBluetoothGatt.close();
        mBluetoothLeScanner.stopScan(mLeScanCallback);

        Log.e(TAG, " -------> Alert data: " + alert_result);

        Intent intent = new Intent();
        intent.setAction(TAG);
        intent.putExtra(TAG, alert_result);
        sendBroadcast(intent);

    }
}
