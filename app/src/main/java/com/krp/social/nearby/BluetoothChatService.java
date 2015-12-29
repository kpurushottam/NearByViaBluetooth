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
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connectedReceiver.
 */
public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";

    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothChatSecure";
    private static final String NAME_INSECURE = "BluetoothChatInsecure";

    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");


    private static final UUID MY_FETCH_UUID_SECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    private static final UUID MY_FETCH_UUID_INSECURE =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private AcceptFetchThread mSecureAcceptFetchThread;
    private AcceptFetchThread mInsecureAcceptFetchThread;
    private ConnectThread mConnectThread;
    private ConnectedReceiverThread mConnectedReceiverThread;
    private ConnectedSenderThread mConnectedSenderThread;
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connectedReceiver to a remote device
    public static final int STATE_FETCHING_DATA = 4;
    public static final int STATE_FETCHING_COMPLETED = 5;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param context The UI Activity Context
     * @param handler A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        //Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume()
     */
    public synchronized void start() {
        setState(STATE_LISTEN);

        // Start the thread to listen on a BluetoothServerSocket
        /*if (mSecureAcceptThread == null) {
            mSecureAcceptThread = new AcceptThread(true);
            mSecureAcceptThread.start();
        }*/
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread(false);
            mInsecureAcceptThread.start();
        }

        /*if (mSecureAcceptFetchThread == null) {
            mSecureAcceptFetchThread = new AcceptFetchThread(true);
            mSecureAcceptFetchThread.start();
        }*/
        if (mInsecureAcceptFetchThread == null) {
            mInsecureAcceptFetchThread = new AcceptFetchThread(false);
            mInsecureAcceptFetchThread.start();
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device, boolean secure) {
        new ConnectThread(device, secure).start();
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device, boolean secure, int state) {
        new ConnectThread(device, secure).start();
        setState(state);
    }

    /**
     * Start the ConnectedReceiverThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connectedReceiver
     */
    public synchronized void connectedReceiver(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        //Log.d(TAG, "connectedReceiver, Socket Type:" + socketType);

        // Start the thread to manage the connection and perform transmissions
        if(mConnectedReceiverThread != null) {
            mConnectedReceiverThread.cancel();
            mConnectedReceiverThread = null;
        }

        mConnectedReceiverThread = new ConnectedReceiverThread(socket, socketType);
        mConnectedReceiverThread.start();

        setState(STATE_CONNECTED);
    }

    /**
     * Start the ConnectedReceiverThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connectedReceiver
     */
    public synchronized void connectedSender(BluetoothSocket socket, BluetoothDevice
            device, final String socketType) {
        //Log.d(TAG, "connectedReceiver, Socket Type:" + socketType);

        // Start the thread to manage the connection and perform transmissions
        if(mConnectedSenderThread != null) {
            mConnectedSenderThread.cancel();
            mConnectedSenderThread = null;
        }

        mConnectedSenderThread = new ConnectedSenderThread(socket, socketType);
        mConnectedSenderThread.start();

        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        //Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedReceiverThread != null) {
            mConnectedReceiverThread.cancel();
            mConnectedReceiverThread = null;
        }

        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }

        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }
        setState(STATE_NONE);
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Start the service over to restart listening mode
        BluetoothChatService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_UUID_INSECURE);
                }
            } catch (IOException e) {
                //Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread" + mSocketType);
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connectedReceiver
            while (true) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    // TODO nothing
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        mHandler.obtainMessage(Constants.NEAR_BY_USER_FOUND, new Device(
                                socket, socket.getRemoteDevice(), mSocketType))
                                .sendToTarget();
                    }
                }
            }
        }

        public void cancel() {
            //Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                //Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }




    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    private class AcceptFetchThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptFetchThread(boolean secure) {
            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            // Create a new listening server socket
            try {
                if (secure) {
                    tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE,
                            MY_FETCH_UUID_SECURE);
                } else {
                    tmp = mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            NAME_INSECURE, MY_FETCH_UUID_INSECURE);
                }
            } catch (IOException e) {
                //Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread" + mSocketType);
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connectedReceiver
            while (true) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    // TODO nothing
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        connectedSender(socket, socket.getRemoteDevice(),
                                mSocketType);
                    }
                }
            }
        }

        public void cancel() {
            //Log.d(TAG, "Socket Type" + mSocketType + "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                //Log.e(TAG, "Socket Type" + mSocketType + "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private String mSocketType;

        public ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";
            UUID uuid_secure, uuid_insecure;

            synchronized (BluetoothChatService.this) {
                if(BluetoothChatService.this.getState() == STATE_FETCHING_DATA) {
                    uuid_secure = MY_FETCH_UUID_SECURE;
                    uuid_insecure = MY_FETCH_UUID_INSECURE;

                } else {
                    uuid_secure = MY_UUID_SECURE;
                    uuid_insecure = MY_UUID_INSECURE;
                }
            }

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = device.createRfcommSocketToServiceRecord(
                            uuid_secure);
                } else {
                    tmp = device.createInsecureRfcommSocketToServiceRecord(
                            uuid_insecure);
                }
            } catch (IOException e) {
                //Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            //Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    //Log.e(TAG, "unable to close() " + mSocketType +
                            //" socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Start the connectedReceiver thread
            /*connectedReceiver(mmSocket, mmDevice, mSocketType);*/
            synchronized (BluetoothChatService.this) {
                if(BluetoothChatService.this.getState() == STATE_FETCHING_DATA) {
                    connectedReceiver(mmSocket, mmDevice, mSocketType);
                } else {
                    mHandler.obtainMessage(Constants.NEAR_BY_USER_FOUND, new Device(mmSocket, mmDevice, mSocketType))
                            .sendToTarget();
                }
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                //Log.e(TAG, "close() of connect " + mSocketType + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedReceiverThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final ObjectInputStream mmInStream;

        public ConnectedReceiverThread(BluetoothSocket socket, String socketType) {
            //Log.d(TAG, "create ConnectedReceiverThread: " + socketType);
            mmSocket = socket;
            ObjectInputStream tmpIn = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                // TODO "temp sockets not created"
            }

            mmInStream = tmpIn;
        }

        public void run() {
            // Keep listening to the InputStream while connectedReceiver
            while (true) {
                try {
                    // Read from the InputStream
                    User user = (User) new ObjectInputStream(mmInStream).readObject();

                    // Send the obtained bytes to the Profile Activity
                    if(user != null) {
                        mHandler.obtainMessage(Constants.MESSAGE_READ_OBJ, user)
                                .sendToTarget();

                        synchronized (BluetoothChatService.this) {
                            setState(STATE_FETCHING_COMPLETED);
                        }
                        break;
                    }
                } catch (IOException e) {
                    // TODO "disconnected"
                    connectionLost();
                    // Start the service over to restart listening mode
                    BluetoothChatService.this.start();
                    break;
                } catch (ClassNotFoundException e) {
                    // TODO nothing
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

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedSenderThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final ObjectOutputStream mmOutStream;

        public ConnectedSenderThread(BluetoothSocket socket, String socketType) {
            //Log.d(TAG, "create ConnectedReceiverThread: " + socketType);
            mmSocket = socket;
            ObjectOutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpOut = new ObjectOutputStream(socket.getOutputStream());
            } catch (IOException e) {
                // TODO "temp sockets not created"
            }

            mmOutStream = tmpOut;
        }

        public void run() {
            try {
                NearByApplication application = NearByApplication.getInstance();
                mmOutStream.writeObject(new User(
                        application.getUserName(),
                        application.getUserAge(),
                        application.isUserGenderMale(),
                        application.getUserInterests()));
            } catch (IOException e) {
                // TODO response to user profile requests fails
            }

            synchronized (BluetoothChatService.this) {
                setState(STATE_FETCHING_COMPLETED);
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
