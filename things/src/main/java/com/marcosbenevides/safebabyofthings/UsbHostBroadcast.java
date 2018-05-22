package com.marcosbenevides.safebabyofthings;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.marcosbenevides.safebabyofthings.callback.BroadcastCallback;

import java.util.Map;

public class UsbHostBroadcast extends BroadcastReceiver {

    private final Integer AZUS_PID = 0x4ee7;
    private final Integer AZUS_VID = 0x18d1;
    private final Integer MULTILASER_PID = 0x201d;
    private final Integer MULTILAZER_VID = 0x0bb4;
    private BroadcastCallback broadcastCallback;

    UsbHostBroadcast(BroadcastCallback broadcastCallback) {
        this.broadcastCallback = broadcastCallback;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        UsbManager usbManager = context.getSystemService(UsbManager.class);
        Map<String, UsbDevice> connectedDevices = usbManager.getDeviceList();

        switch (action) {
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                for (UsbDevice device : connectedDevices.values()) {
                    if ((device.getVendorId() == MULTILAZER_VID && device.getProductId() == MULTILASER_PID) ||
                            (device.getVendorId() == AZUS_VID && device.getProductId() == AZUS_PID)) {

                        broadcastCallback.onUsbDeviceAttached(device);

                        break;
                    }
                }
                break;
            case UsbManager.ACTION_USB_DEVICE_DETACHED:
                Boolean desconected = true;
                for (UsbDevice device : connectedDevices.values()) {
                    if ((device.getVendorId() == MULTILAZER_VID && device.getProductId() == MULTILASER_PID) ||
                            (device.getVendorId() == AZUS_VID && device.getProductId() == AZUS_PID)) {
                        desconected = false;
                        break;
                    }
                }
                if (desconected) {
                    broadcastCallback.onUsbDeviceDetached();
                }
                break;
            case UsbManager.EXTRA_PERMISSION_GRANTED:
                broadcastCallback.onUsbDeviceStartCommunication(usbManager);
                break;

            case BluetoothAdapter.ACTION_STATE_CHANGED: {
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF) == BluetoothAdapter.STATE_ON)
                    broadcastCallback.onBluetoothOn();
                else
                    broadcastCallback.onBluetoothOff();
                break;
            }
        }
    }
}
