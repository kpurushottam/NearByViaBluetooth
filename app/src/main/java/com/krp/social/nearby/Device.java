package com.krp.social.nearby;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Created by Kumar Purushottam on 28-12-2015.
 */
public class Device {
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;

    public Device(BluetoothSocket bluetoothSocket, BluetoothDevice bluetoothDevice) {
        this.bluetoothSocket = bluetoothSocket;
        this.bluetoothDevice = bluetoothDevice;
    }
}
