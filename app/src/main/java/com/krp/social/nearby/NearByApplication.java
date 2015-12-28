package com.krp.social.nearby;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.widget.Toast;

/**
 * Created by Kumar Purushottam on 27-12-2015.
 */
public class NearByApplication extends Application {

    private static NearByApplication mInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;
    }

    public static NearByApplication getInstance() {
        return mInstance;
    }
}
