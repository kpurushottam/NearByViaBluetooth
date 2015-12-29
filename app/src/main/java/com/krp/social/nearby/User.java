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
    String interests;
    String profileImageUrl;

    String deviceName;
    String deviceAddress;

    public User(String username, String age, boolean isMale, String interests) {
        this.username = username;
        this.age = age;
        this.male = isMale;
        this.interests = interests;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return !(deviceAddress != null ? !deviceAddress.equals(user.deviceAddress) : user.deviceAddress != null);

    }

    @Override
    public int hashCode() {
        return deviceAddress != null ? deviceAddress.hashCode() : 0;
    }
}
