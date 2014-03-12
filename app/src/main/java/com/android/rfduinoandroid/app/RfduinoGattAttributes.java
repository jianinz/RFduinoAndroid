package com.android.rfduinoandroid.app;

import java.util.HashMap;
import java.util.UUID;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class RfduinoGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
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
