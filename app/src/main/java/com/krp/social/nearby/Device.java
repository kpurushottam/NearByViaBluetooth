package com.krp.social.nearby;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

/**
 * Created by Kumar Purushottam on 28-12-2015.
 */
public class Device {
    BluetoothSocket bluetoothSocket;
    BluetoothDevice bluetoothDevice;
    String connectionType;

    public Device(BluetoothSocket bluetoothSocket, BluetoothDevice bluetoothDevice, String connectionType) {
        this.bluetoothSocket = bluetoothSocket;
        this.bluetoothDevice = bluetoothDevice;
        this.connectionType = connectionType;
    }
}
