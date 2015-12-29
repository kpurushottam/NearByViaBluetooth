package com.krp.social.nearby;

import android.app.Application;
import android.content.SharedPreferences;

/**
 * Created by Kumar Purushottam on 27-12-2015.
 */
public class NearByApplication extends Application {
    public static final String TAG = NearByApplication.class.getSimpleName();

    private static NearByApplication mInstance;

    public SharedPreferences mSharedPrefs;

    @Override
    public void onCreate() {
        super.onCreate();

        mInstance = this;

        mSharedPrefs = getSharedPreferences(TAG, MODE_PRIVATE);
    }

    public static NearByApplication getInstance() {
        return mInstance;
    }

    public void setUserName(String name) {
        mSharedPrefs.edit().putString(Constants.KEY_PREF_USER_NAME, name).commit();
    }

    public void setUserAge(String age) {
        mSharedPrefs.edit().putString(Constants.KEY_PREF_USER_AGE, age).commit();
    }

    public void setUserGender(boolean male) {
        mSharedPrefs.edit().putBoolean(Constants.KEY_PREF_USER_GENDER_MALE, Boolean.valueOf(male)).commit();
    }

    public void setUserInterests(String interests) {
        mSharedPrefs.edit().putString(Constants.KEY_PREF_USER_INTEREST, interests).commit();
    }

    public String getUserName() {
        return mSharedPrefs.getString(Constants.KEY_PREF_USER_NAME, null);
    }

    public String getUserAge() {
        return mSharedPrefs.getString(Constants.KEY_PREF_USER_AGE, null);
    }

    public boolean isUserGenderMale() {
        return mSharedPrefs.getBoolean(Constants.KEY_PREF_USER_GENDER_MALE, false);
    }

    public String getUserInterests() {
        return mSharedPrefs.getString(Constants.KEY_PREF_USER_INTEREST, null);
    }
}
