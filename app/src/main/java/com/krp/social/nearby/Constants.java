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

/**
 * Defines several constants used between {@link BluetoothConnectionService} and the UI.
 */
public interface Constants {

    // Message types sent from the BluetoothConnectionService Handler
    public static final int MESSAGE_TOAST = 1;
    public static final int MESSAGE_WRITE_OBJ = 2;
    public static final int MESSAGE_READ_OBJ = 3;
    public static final int NEAR_BY_USER_FOUND = 4;


    public static final String KEY_PREF_USER_NAME = "userName";
    public static final String KEY_PREF_USER_AGE = "userAge";
    public static final String KEY_PREF_USER_GENDER_MALE = "userGender";
    public static final String KEY_PREF_USER_INTEREST = "userInterest";
}
