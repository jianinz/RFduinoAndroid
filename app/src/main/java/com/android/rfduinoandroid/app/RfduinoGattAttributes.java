/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.rfduinoandroid.app;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class RfduinoGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
	public final static UUID UUID_SERVICE = BluetoothUtil.sixteenBitUuid(0x2220);
	public final static UUID UUID_RECEIVE = BluetoothUtil.sixteenBitUuid(0x2221);
	public final static UUID UUID_SEND = BluetoothUtil.sixteenBitUuid(0x2222);
	public final static UUID UUID_DISCONNECT = BluetoothUtil.sixteenBitUuid(0x2223);
	public final static UUID UUID_CLIENT_CONFIGURATION = BluetoothUtil.sixteenBitUuid(0x2902);
    static {
        // Rfduino Gatt Services.
        attributes.put(UUID_SERVICE.toString(), "Rfduino service");
        // Rfduino Gatt Characteristics.
        attributes.put(UUID_RECEIVE.toString(), "Rfduino receive characteristic");
		attributes.put(UUID_SEND.toString(), "Rfduino send characteristic");
		// Rfduino Gatt Characteristics.
		attributes.put(UUID_CLIENT_CONFIGURATION.toString(), "Rfduino receive characteristic descriptor");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
