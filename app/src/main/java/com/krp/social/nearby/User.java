package com.krp.social.nearby;

import java.util.List;

/**
 * Created by Kumar Purushottam on 26-12-2015.
 */
public class User {
    String username = "Username";
    String age = "18";
    boolean male = true;
    List<String> interests;
    String profileImageUrl;

    String deviceName;
    String deviceAddress;

    public User(String deviceName, String deviceAddress) {
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
    }
}
