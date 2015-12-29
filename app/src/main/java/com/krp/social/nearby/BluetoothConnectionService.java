/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.krp.social.nearby;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connectedReceiver.
 */
public class BluetoothConnectionService {
    private static final String TAG = "BluetoothConnectionService";

    // Name for the SDP record when creating server socket
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_FETCH_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mInsecureAcceptThread;
    private AcceptFetchThread mInsecureAcceptFetchThread;
    private ConnectedReceiverThread mConnectedReceiverThread;
    private ConnectedSenderThread mConnectedSenderThread;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothConnectionService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        // Start the thread to listen on a BluetoothServerSocket
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }

        if (mInsecureAcceptFetchThread == null) {
            mInsecureAcceptFetchThread = new AcceptFetchThread();
            mInsecureAcceptFetchThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        new ConnectThread(device).start();
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device The BluetoothDevice to connect
     */
    public synchronized void fetch(BluetoothDevice device) {
        new ConnectFetchThread(device).start();
    }

    /**
     * Start the ConnectedReceiverThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connectedReceiver
     */
    public synchronized void connectedReceiver(BluetoothSocket socket, BluetoothDevice device) {

        // Start the thread to manage the connection and perform transmissions
        if(mConnectedReceiverThread != null) {
            mConnectedReceiverThread.cancel();
            mConnectedReceiverThread = null;
        }

        mConnectedReceiverThread = new ConnectedReceiverThread(socket);
        mConnectedReceiverThread.start();
    }

    /**
     * Start the ConnectedReceiverThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connectedReceiver
     */
    public synchronized void connectedSender(BluetoothSocket socket, BluetoothDevice device) {

        // Start the thread to manage the connection and perform transmissions
        if(mConnectedSenderThread != null) {
            mConnectedSenderThread.cancel();
            mConnectedSenderThread = null;
        }

        mConnectedSenderThread = new ConnectedSenderThread(socket);
        mConnectedSenderThread.start();
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        if (mConnectedReceiverThread != null) {
            mConnectedReceiverThread.cancel();
            mConnectedReceiverThread = null;
        }

        if (mConnectedSenderThread != null) {
            mConnectedSenderThread.cancel();
            mConnectedSenderThread = null;
        }

        if (mInsecureAcceptFetchThread != null) {
            mInsecureAcceptFetchThread.cancel();
            mInsecureAcceptFetchThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs infinitely to able other nearBy users to discover me/others
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        NAME_INSECURE, MY_UUID_INSECURE);
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connectedReceiver
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    // TODO nothing
                }

                // Listen to the server socket to allow nearBy users to find me
                if (socket != null) {
                    synchronized (BluetoothConnectionService.this) {
                        mHandler.obtainMessage(Constants.NEAR_BY_USER_FOUND, new Device(
                                socket, socket.getRemoteDevice()))
                                .sendToTarget();
                        try {
                            socket.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }




    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     *
     * This thread is used to accept a request to get my profile data
     */
    private class AcceptFetchThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptFetchThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        NAME_INSECURE, MY_FETCH_UUID_INSECURE);
            } catch (IOException e) {
            }
            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;

            // Listen to the server socket to allow transfer profile data
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    // TODO nothing
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothConnectionService.this) {
                        connectedSender(socket, socket.getRemoteDevice());
                    }
                }
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     *
     * This thread is used to find a nearBy user of this APP
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(
                        MY_UUID_INSECURE);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            try {
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                }
                connectionFailed();
                return;
            }

            // found a nearBy user
            mHandler.obtainMessage(Constants.NEAR_BY_USER_FOUND, new Device(mmSocket, mmDevice))
                    .sendToTarget();
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     *
     * This thread is to fetch nearBy user's Profile data
     */
    private class ConnectFetchThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectFetchThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(
                        MY_FETCH_UUID_INSECURE);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            try {
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                }
                connectionFailed();
                return;
            }

            // Start the connectedReceiver thread
            connectedReceiver(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * This thread runs during a connection with a nearby device, with this current APP
     * It handles the transfer of SUCCESSFULL transfer of Profile data to the sending device
     */
    private class ConnectedReceiverThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedReceiverThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                // TODO "temp sockets not created"
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connectedReceiver
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);

                    // Send the obtained bytes to the Profile data
                    if(readMessage != null) {
                        mHandler.obtainMessage(Constants.MESSAGE_READ_OBJ, readMessage)
                                .sendToTarget();

                        // send the sender an DISCONNET request and SUCCESSFULL retrieval of profile data
                        write("disconnect".getBytes());
                        try {
                            sleep(100);
                            mmSocket.close();
                            mHandler.obtainMessage(777, "Disconnected").sendToTarget();
                        } catch (InterruptedException e) {
                        } catch (IOException e) {
                        }
                        break;
                    }
                } catch (IOException e) {
                    // TODO "on ConnectionLost"
                    connectionLost();
                    try {
                        mmSocket.close();
                    } catch (IOException ee) {
                    }
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                // TODO "Exception during write"
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                // TODO "close() of connect socket failed"
            }
        }
    }

    /**
     * This thread runs during a connection with a nearby device, with this current APP
     * It handles the transfer of profile data to the requesting device
     */
    private class ConnectedSenderThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedSenderThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                // TODO "temp sockets not created"
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            try {
                NearByApplication application = NearByApplication.getInstance();

                String message = new StringBuilder()
                        .append(application.getUserName()).append(":")
                        .append(application.getUserAge()).append(":")
                        .append(application.isUserGenderMale()).append(":")
                        .append(application.getUserInterests()).toString();
                byte[] sendBuffer = message.getBytes();
                mmOutStream.write(sendBuffer);
            } catch (IOException e) {
                try {
                    mmSocket.close();
                } catch (IOException ee) {
                }
            }

            byte[] buffer = new byte[1024];
            int bytes;
            // Keep listening to the InputStream while connectedReceiver
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);

                    // read the disconnection message to disconnect the socket
                    // ensuring the data sent successfully
                    if(readMessage != null && readMessage.equalsIgnoreCase("disconnect")) {
                        try {
                            mmSocket.close();
                        } catch (IOException e) {
                        }
                        break;
                    }
                } catch (IOException e) {
                    // TODO "on connection lost"
                    connectionLost();
                    try {
                        mmSocket.close();
                    } catch (IOException ee) {
                    }
                    break;
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                // TODO "close() of connect socket failed"
            }
        }
    }
}
