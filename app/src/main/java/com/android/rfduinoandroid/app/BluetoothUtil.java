package com.android.rfduinoandroid.app;

import java.util.UUID;

/**
 * https://github.com/lann/RFDuinoTest/blob/master/src/main/java/com/lannbox/rfduinotest/BluetoothHelper.java
 */
public class BluetoothUtil {
	public static String shortUuidFormat = "0000%04X-0000-1000-8000-00805F9B34FB";

	public static UUID sixteenBitUuid(long shortUuid) {
		assert shortUuid >= 0 && shortUuid <= 0xFFFF;
		return UUID.fromString(String.format(shortUuidFormat, shortUuid & 0xFFFF));
	}

	// Bluetooth Spec V4.0 - Vol 3, Part C, section 8
	public static String parseScanRecord(byte[] scanRecord) {
		StringBuilder output = new StringBuilder();
		int i = 0;
		while (i < scanRecord.length) {
			int len = scanRecord[i++] & 0xFF;
			if (len == 0) break;
			switch (scanRecord[i] & 0xFF) {
				// https://www.bluetooth.org/en-us/specification/assigned-numbers/generic-access-profile
				case 0x0A: // Tx Power
					output.append("Tx Power: ").append(scanRecord[i + 1]);
					break;
				case 0xFF: // Manufacturer Specific data (RFduinoBLE.advertisementData)
					output.append("\nAdvertisement Data: ")
							.append(HexAsciiUtil.bytesToHex(scanRecord, i + 3, len));

					String ascii = HexAsciiUtil.bytesToAsciiMaybe(scanRecord, i + 3, len);
					if (ascii != null) {
						output.append(" (\"").append(ascii).append("\")");
					}
					break;
			}
			i += len;
		}
		return output.toString();
	}
}
