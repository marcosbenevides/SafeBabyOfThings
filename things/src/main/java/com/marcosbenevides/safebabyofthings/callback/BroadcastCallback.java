package com.marcosbenevides.safebabyofthings.callback;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

public interface BroadcastCallback {

   void onUsbDeviceAttached(UsbDevice device);
   void onUsbDeviceDetached();
   void onUsbDeviceStartCommunication(UsbManager manager);
   void onBluetoothOn();
   void onBluetoothOff();
}
