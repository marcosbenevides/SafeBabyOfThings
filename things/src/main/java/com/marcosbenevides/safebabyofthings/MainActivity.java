package com.marcosbenevides.safebabyofthings;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;
import com.marcosbenevides.safebabyofthings.callback.BroadcastCallback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity implements BroadcastCallback {

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    private static final Integer ENABLE_BLUETOOTH = 1;

    private static final UUID SERVICE_UUID = UUID.fromString("997deb98-f7fb-4ca2-a899-684c1d2aee2b");
    private static final UUID BABY_STATUS = UUID.fromString("c012dcbf-a04c-4c55-8cae-28c0ac63c2bc");

    private static final String SAFEBABYOFTHINGS = "sbot";
    private static final String GPIO_PORT = "BCM3";
    private static String REMOTE_DEVICE_NAME = "ASUS_MARCOS";
    private Gpio mGpio;
    private String remote_device_address;
    private TextView connection_status, device_name;
    private CardView alarm;
    private UsbDevice deviceConnected;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattServer mGattServer;
    private ArrayList<BluetoothDevice> mConnectedDevices;
    private ArrayAdapter<BluetoothDevice> mConnectedDevicesAdapter;
    private Handler mHandler;
    private PeripheralManager mPeripheralManager;
    private UsbHostBroadcast mReceiver;

    private GpioCallback gpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {
            try {
                Log.e(getClass().getSimpleName(), "GPIO change -> " + gpio.getValue());
            } catch (IOException e) {
                Log.e(MainActivity.this.getClass().getSimpleName(), "Erro ao acessar GPIO (96) " + e);
            }
            return true;
        }
    };

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                checkDevice(device, true);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                checkDevice(device, false);
            }

        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Log.d("START", "Servidor gatt adicionado");
            super.onServiceAdded(status, service);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d("READING DATA", "Solicitacao de leitura de dados " + characteristic.getUuid().toString());
            if (BABY_STATUS.equals(characteristic.getUuid())) {
                byte[] value = ByteBuffer.allocate(4).putInt(1).array();
                mGattServer.sendResponse(device, requestId, GATT_SUCCESS, 0, value);
            }
        }
    };

    private AdvertiseCallback mAdvertisingCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            Log.d(getClass().getSimpleName(), "Advertising started");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            Log.d(getClass().getSimpleName(), "Advertising failed " + errorCode);

        }
    };

    private void checkDevice(BluetoothDevice device, boolean connected) {

        Log.e(getClass().getSimpleName(),
                "Connected: " + connected + " Device: " + device.getName() + "(" + device.getAddress() + ")");

        if (connected) {
            remote_device_address = device.getAddress();
            alarm.setVisibility(View.GONE);
        } else if (remote_device_address != null && device.getAddress().equals(remote_device_address)) {
            alarm.setVisibility(View.VISIBLE);
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        connection_status = findViewById(R.id.connection_status);
        device_name = findViewById(R.id.device_name);
        alarm = findViewById(R.id.alarm);

        mHandler = new Handler();

        mReceiver = new UsbHostBroadcast(this);

        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        if (mBluetoothManager != null) {
            mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
        mBluetoothAdapter.setName(SAFEBABYOFTHINGS);

        try {
            mPeripheralManager = PeripheralManager.getInstance();
            List<String> list = mPeripheralManager.getGpioList();
            if (list.isEmpty()) {
                Log.e(getClass().getSimpleName(), "GPIO unavaliable.");
            } else {
                Log.d(getClass().getSimpleName(), "GPIO avaliable port " + list);
            }

            mGpio = mPeripheralManager.openGpio(GPIO_PORT);
            mGpio.setDirection(Gpio.DIRECTION_IN);
            mGpio.setActiveType(Gpio.ACTIVE_LOW);
            mGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
            mGpio.registerGpioCallback(gpioCallback);
            Log.i(getClass().getSimpleName(), "BABY STATUS IS -> " + mGpio.getValue());
        } catch (IOException ex) {
            Log.e(getClass().getSimpleName(), "Erro ao abrir porta -> " + ex.getMessage());
        }

        //delegando intent para deteccao de conexao ou desconexao de usb
        IntentFilter connected_filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        IntentFilter disconected_filter = new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED);
        IntentFilter permission = new IntentFilter(UsbManager.EXTRA_PERMISSION_GRANTED);
        IntentFilter bluetooth_filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);

        //registrando um receiver para cada filtro
        registerReceiver(mReceiver, connected_filter);
        registerReceiver(mReceiver, disconected_filter);
        registerReceiver(mReceiver, permission);
        registerReceiver(mReceiver, bluetooth_filter);

        if (!mBluetoothAdapter.isEnabled()) {
            Log.d(getClass().getSimpleName(), "Bluetooth disabled, enabling ...");
            mBluetoothAdapter.enable();
        } else {
            Log.d(getClass().getSimpleName(), "Bluetooth already anabled");
            initServer();
            startAdvertising();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

/*        if(mBluetoothAdapter == null || mBluetoothAdapter.getState() == BluetoothAdapter.STATE_OFF){

            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(intent);
            return;

        }*/


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        mBluetoothAdapter.disable();
        if (mGpio != null) {
            try {
                mGpio.close();
                mGpio = null;
            } catch (IOException ex) {
                Log.w(getClass().getSimpleName(), "Erro ao fechar porta GPIO " + ex);
            }
        }
    }

    private void startAdvertising() {
        mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(SERVICE_UUID))
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertisingCallback);

    }

    private void initServer() {

        mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mGattServer == null) {
            Log.e(getClass().getSimpleName(), "Unable to init server.");
            return;
        }

        BluetoothGattService UART_SERVICE = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        BluetoothGattCharacteristic baby_status = new BluetoothGattCharacteristic(BABY_STATUS, BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ);

        UART_SERVICE.addCharacteristic(baby_status);

        mGattServer.addService(UART_SERVICE);
    }

    private void startConection(UsbDevice device, UsbManager usbManager) {

        final byte[] bytes = {1};

        UsbInterface usbInterface = device.getInterface(0);
        final UsbEndpoint usbEndpoint = usbInterface.getEndpoint(0);

        final UsbDeviceConnection connection = usbManager.openDevice(device);

        connection.claimInterface(usbInterface, true);
        new Thread(new Runnable() {
            @Override
            public void run() {
                int val = connection.bulkTransfer(usbEndpoint, bytes, bytes.length, 15);
                Log.i("TESTE", String.valueOf(val));
            }
        }).start();

    }

    private void changeDeviceName(String message) {
        device_name.setText(message);
    }

    private void changeDeviceStatus(Boolean connected) {
        if (connected) {
            connection_status.setText(getResources().getText(R.string.conectado));
        } else {
            connection_status.setText(getResources().getText(R.string.desconectado));
        }
    }

    @Override
    public void onUsbDeviceAttached(UsbDevice device) {
        deviceConnected = device;

        changeDeviceStatus(true);
        changeDeviceName(device.getDeviceName());
    }

    @Override
    public void onUsbDeviceDetached() {
        changeDeviceStatus(false);
        changeDeviceName(getResources().getString(R.string.no_devices));

        alarm.setVisibility(View.VISIBLE);

    }

    @Override
    public void onUsbDeviceStartCommunication(UsbManager manager) {
        startConection(deviceConnected, manager);
    }

    @Override
    public void onBluetoothOn() {
        initServer();
        startAdvertising();
    }

    @Override
    public void onBluetoothOff() {
        if (mBluetoothLeAdvertiser != null)
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertisingCallback);
        if (mGattServer != null)
            mGattServer.close();
    }
}