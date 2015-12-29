package com.krp.social.nearby;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener, NearByRecyclerAdapter.OnNearByUserSelectListener {

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavHome;

    private ImageView mUserProfile, mBtnRefreshSearch, mBtnEnableDisableBluetooth;
    private TextView mUserName, mUserAge;
    private ProgressBar mProgressSearching;

    private RecyclerView mRecyclerNearBy;
    private NearByRecyclerAdapter mRecyclerAdapter;

    private Toast mToast;
    private BluetoothAdapter mBluetoothAdapter;

    private final int REQUEST_ENABLE_BT = 1001;
    private final int REQUEST_BT_DEVICE_DISCOVERY = 1002;

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_SHOW_USER_PROFILE = 1003;

    private List<User> connectedUsers = new ArrayList<>();

    /**
     * Name of the connectedReceiver device
     */
    private String mConnectedDeviceName = null;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;
    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;

    private ProgressDialog mWaitingDialog;


    private final DialogFragment mBluetoothWarningDialog = new DialogFragment() {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                    .setTitle("Turning off bluetooth?")
                    .setMessage("Disabling bluetooth closes app.")
                    .setPositiveButton("Disable", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mBtnEnableDisableBluetooth.setAlpha(1f);
                            mBluetoothAdapter.enable();

                            if(mBluetoothAdapter.getScanMode() !=
                                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                                startActivityForResult(discoverableIntent, REQUEST_BT_DEVICE_DISCOVERY);
                            }
                        }
                    })
                    .create();
        }
    };

    private final BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, Intent intent) {
            String action = intent.getAction();

            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch(state) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        mBluetoothWarningDialog.show(getSupportFragmentManager(), "alert-dialog");
                        break;

                    case BluetoothAdapter.STATE_ON:
                        mBtnRefreshSearch.setVisibility(View.GONE);
                        mProgressSearching.setVisibility(View.VISIBLE);
                        setupChat();
                        break;
                }

            } else if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                connectDevice(device.getAddress(), false);

            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                mBtnRefreshSearch.setVisibility(View.VISIBLE);
                mProgressSearching.setVisibility(View.GONE);
                mRecyclerAdapter.notifyDataSetChanged();
            }
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            DashboardActivity activity = DashboardActivity.this;
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            //setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            //mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            //setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            //setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;

                case Constants.MESSAGE_WRITE_OBJ:
                    User user = (User) msg.obj;
                    alert(user.toString() + " : message sent");
                    break;

                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;

                case Constants.MESSAGE_READ_OBJ:
                    dismissWaitingDialog();
                    User user1 = (User) msg.obj;
                    connectedUsers.add(user1);
                    startActivityForResult(new Intent(DashboardActivity.this, LoginActivity.class)
                                    .putExtra(LoginActivity.KEY_INTENT_ACTIVITY_PROFILE, user1),
                            REQUEST_SHOW_USER_PROFILE);
                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connectedReceiver device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.NEAR_BY_USER_FOUND:
                    if (null != activity) {
                        Device device = (Device) msg.obj;
                        nearBy.put(device.bluetoothDevice.getAddress(), device);
                        mRecyclerAdapter.addData(new User(
                                device.bluetoothDevice.getName(), device.bluetoothDevice.getAddress()));
                        mRecyclerAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };

    private Map<String, Device> nearBy = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!setupBluetoothAdapter()) {
            finish();
        }
        setContentView(R.layout.activity_dashboard);

        setupToolbar();
        setupDrawerLayout();
        setupHomeNavigationView();
        setupNearByNavigationView();
        setupWaitingDialog();

        // Register for broadcast listeners
        registerReceiver(mBluetoothReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(mBluetoothReceiver,
                new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(mBluetoothReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mBluetoothAdapter.isEnabled()) {
            if(mChatService == null) {
                setupChat();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT :
                if(resultCode == RESULT_CANCELED) {
                    alert("Turn on Bluetooth! Then try again.");
                    finish();

                } else if(resultCode == RESULT_OK) {
                    mDrawerLayout.openDrawer(GravityCompat.END);
                }
                break;

            case REQUEST_BT_DEVICE_DISCOVERY:
                if(resultCode == RESULT_CANCELED) {
                    alert("Turnning off \"Discovery\" other users will not be able to find you!");
                }
                if(!mBluetoothAdapter.isDiscovering()) {
                    mBluetoothAdapter.startDiscovery();
                }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mBluetoothAdapter.isEnabled()) {
            if(mBluetoothAdapter.getScanMode() !=
                    BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                startActivityForResult(discoverableIntent, REQUEST_BT_DEVICE_DISCOVERY);

            } else if(!mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.startDiscovery();
            }

            if (mChatService != null) {
                if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                    mChatService.start();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mChatService != null) {
            mChatService.stop();
        }
        // Unregister broadcast listeners
        unregisterReceiver(mBluetoothReceiver);
    }

    /**
     * @return true : if setup is successful else returns false;
     */
    private boolean setupBluetoothAdapter() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            alert("Device not supports Bluetooth");
            return false;
        }
        return true;
    }

    private void setupToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
    }

    private void setupDrawerLayout() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawerLayout, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();
    }

    private void setupHomeNavigationView() {
        mNavHome = (NavigationView) findViewById(R.id.nav_home);
        mNavHome.setNavigationItemSelectedListener(this);

        mUserProfile = (ImageView) mNavHome.getHeaderView(0).findViewById(R.id.iv_profile);
        mUserName = (TextView) mNavHome.getHeaderView(0).findViewById(R.id.tv_user_name);
        mUserAge = (TextView) mNavHome.getHeaderView(0).findViewById(R.id.tv_user_age);
    }

    private void setupNearByNavigationView() {
        mRecyclerNearBy = (RecyclerView) findViewById(R.id.recycler);
        mRecyclerNearBy.setLayoutManager(new LinearLayoutManager(this));

        mRecyclerAdapter = new NearByRecyclerAdapter(this);
        mRecyclerNearBy.setAdapter(mRecyclerAdapter);

        mBtnEnableDisableBluetooth = (ImageView) findViewById(R.id.iv_bluetooth);
        mBtnEnableDisableBluetooth.setOnClickListener(this);

        mBtnRefreshSearch = (ImageView) findViewById(R.id.iv_refresh);
        mBtnRefreshSearch.setOnClickListener(this);

        mProgressSearching = (ProgressBar) findViewById(R.id.progress_searching);
    }

    private void setupWaitingDialog() {
        mWaitingDialog = new ProgressDialog(this);
        mWaitingDialog.setTitle("");
        mWaitingDialog.setMessage("Connecting...");
        mWaitingDialog.setIndeterminate(true);
        mWaitingDialog.setCancelable(false);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
            mDrawerLayout.closeDrawer(GravityCompat.START);

        } else if (mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawer(GravityCompat.END);

        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.dashboard, menu);
        return true;
    }

    /**
     * To show alert messages as Toasts
     * @param message
     */
    public void alert(String message) {
        if(mToast == null) {
            mToast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        }

        if(mToast.getView().isShown()) {
            mToast.cancel();
        }

        mToast.setText(message);
        mToast.show();
    }

    public void showWaitingDialog() {
        if(mWaitingDialog == null) {
            setupWaitingDialog();
        }

        if(!mWaitingDialog.isShowing()) {
            mWaitingDialog.show();
        }
    }

    public void dismissWaitingDialog() {
        if(mWaitingDialog.isShowing()) {
            mWaitingDialog.dismiss();
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        //Log.d(TAG, "setupChat()");

        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_frnds) {
            if (!mBluetoothAdapter.isEnabled()) {
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                        REQUEST_ENABLE_BT);
            } else {
                mDrawerLayout.openDrawer(GravityCompat.END);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_nav_frnds:
                if (!mBluetoothAdapter.isEnabled()) {
                    startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),
                            REQUEST_ENABLE_BT);
                } else {
                    mDrawerLayout.openDrawer(GravityCompat.END);
                }
                break;

            default: return false;
        }

        mDrawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_bluetooth:
                if(mBluetoothAdapter.isEnabled()) {
                    mBtnEnableDisableBluetooth.setAlpha(0.3f);
                    mBluetoothAdapter.disable();

                } else  {
                    mBluetoothAdapter.enable();
                    mBtnEnableDisableBluetooth.setAlpha(1f);

                    if(mBluetoothAdapter.getScanMode() !=
                            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                        startActivityForResult(discoverableIntent, REQUEST_BT_DEVICE_DISCOVERY);
                    }
                }
                mRecyclerAdapter.refreshDataSet();
                break;

            case R.id.iv_refresh:
                if(mBluetoothAdapter.isEnabled()) {
                    mBtnRefreshSearch.setVisibility(View.GONE);
                    mProgressSearching.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @Override
    public void onUserSelected(User user) {
        //if(connectedUsers.contains(user)) {
            showWaitingDialog();
            Device device = nearBy.get(user.deviceAddress);
            mChatService.connect(device.bluetoothDevice, Boolean.valueOf(device.connectionType),
                    BluetoothChatService.STATE_FETCHING_DATA);
        //}
    }

    /**
     * Establish connection with other divice
     */
    private void connectDevice(String address, boolean secure) {
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        mChatService.connect(device, secure);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        DashboardActivity activity = this;
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(resId);
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        DashboardActivity activity = this;
        if (null == activity) {
            return;
        }
        final ActionBar actionBar = activity.getActionBar();
        if (null == actionBar) {
            return;
        }
        actionBar.setSubtitle(subTitle);
    }
}
