package com.krp.social.nearby;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Kumar Purushottam on 26-12-2015.
 */
public class User implements Serializable, Comparable<User> {
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

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", age='" + age + '\'' +
                ", male=" + male +
                ", interests=" + interests +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceAddress='" + deviceAddress + '\'' +
                '}';
    }

    @Override
    public int compareTo(User another) {
        return this.deviceAddress.compareTo(another.deviceAddress);
    }
}
