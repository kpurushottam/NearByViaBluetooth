package com.krp.social.nearby;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavHome;

    private ImageView mUserProfile, mBtnRefreshSearch, mBtnEnableDisableBluetooth;
    private TextView mUserName, mUserAge;

    private RecyclerView mRecyclerNearBy;
    private NearByRecyclerAdapter mRecyclerAdapter;

    private Toast mToast;
    private BluetoothAdapter mBluetoothAdapter;

    private final int REQUEST_ENABLE_BT = 1001;
    private final int REQUEST_BT_DEVICE_DISCOVERY = 1002;

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
                        searchBluetoothDevices();
                        break;
                }

            } else if(action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mRecyclerAdapter.addData(new User(device.getName(), device.getAddress()));

            } else if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                mRecyclerAdapter.notifyDataSetChanged();
            }
        }
    };

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

        // Register for broadcast listeners
        registerReceiver(mBluetoothReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        registerReceiver(mBluetoothReceiver,
                new IntentFilter(BluetoothDevice.ACTION_FOUND));
        registerReceiver(mBluetoothReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT :
                if(resultCode == RESULT_CANCELED) {
                    alert("Turn on Bluetooth! Then try again.");
                    finish();

                } else {
                    mDrawerLayout.openDrawer(GravityCompat.END);
                }
                break;

            case REQUEST_BT_DEVICE_DISCOVERY:
                if(resultCode == RESULT_CANCELED) {
                    alert("Turnning off \"Discovery\" other users will not be able to find you!");
                }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mBluetoothAdapter.isEnabled()) {
            if(!mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.startDiscovery();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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

        mRecyclerAdapter = new NearByRecyclerAdapter();
        mRecyclerNearBy.setAdapter(mRecyclerAdapter);

        mBtnEnableDisableBluetooth = (ImageView) findViewById(R.id.iv_bluetooth);
        mBtnEnableDisableBluetooth.setOnClickListener(this);

        mBtnRefreshSearch = (ImageView) findViewById(R.id.iv_refresh);
        mBtnRefreshSearch.setOnClickListener(this);
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

    public void searchBluetoothDevices() {
        alert("Searching...");

        // search paired devices
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        // If there are paired devices
        int size = pairedDevices.size();
        if(size == 0) {
            return;
        }

        List<User> users = new ArrayList<>(size);
        for (BluetoothDevice device : pairedDevices) {
            users.add(new User(device.getName(), device.getAddress()));
        }
        mRecyclerAdapter.refreshDataSet(users);

        // via discovery
        mBluetoothAdapter.startDiscovery();
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
                    searchBluetoothDevices();
                }
                break;
        }
    }
}