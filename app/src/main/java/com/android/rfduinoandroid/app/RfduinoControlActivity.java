package com.android.rfduinoandroid.app;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code RfduinoBleService}, which in turn interacts with the
 * Bluetooth LE API.
 * http://developer.android.com/guide/topics/connectivity/bluetooth-le.html
 */
public class RfduinoControlActivity extends Activity {
	private final static String TAG = RfduinoControlActivity.class.getSimpleName();

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

	private TextView mConnectionState;
	private TextView mDataField;
	private TextView mCurrentValue;
	private SeekBar mChangeAngle;
	private String mDeviceName;
	private String mDeviceAddress;
	private RadioButton mInsertCard;
	private RadioButton mRemoveCard;
	private Button mMinus;
	private Button mPlus;
	private RfduinoBleService mRfduinoBleService;
	private BluetoothGattCharacteristic characteristicTX;
	private BluetoothGattCharacteristic characteristicRX;
	private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
			new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
	private boolean mConnected = false;
	/**
	 * int array representing control mode and degree.
	 * eg: Auto-mode, it will look like {1, 0} and {1, 1}, respectively means {auto-mode, card removed} and {auto-mode, card inserted}
	 * Manual-mode, it will look like {2, 0} and {2, 180} or {2, x}, respectively means {manual-mode, 0 deg} and {manual-mode, 180 deg} or {manual-mode, x deg}
	 */
	private int servoData[] = {1, 0}; // Initial value is {auto-mode, card removed}

	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";

	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			mRfduinoBleService = ((RfduinoBleService.LocalBinder) service).getService();
			if (!mRfduinoBleService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up initialization.
			mRfduinoBleService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mRfduinoBleService = null;
		}
	};

	// Handles various events fired by the Service.
	// ACTION_GATT_CONNECTED: connected to a GATT server.
	// ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
	// ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
	// ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
	//                        or notification operations.
	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (RfduinoBleService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				updateConnectionState(R.string.connected);
				invalidateOptionsMenu();
			} else if (RfduinoBleService.ACTION_GATT_DISCONNECTED.equals(action)) {
				mConnected = false;
				updateConnectionState(R.string.disconnected);
				invalidateOptionsMenu();
				clearUI();
			} else if (RfduinoBleService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				// Show all the supported services and characteristics on the user interface.
				displayGattServices(mRfduinoBleService.getSupportedGattServices());
			} else if (RfduinoBleService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent.getStringExtra(RfduinoBleService.EXTRA_DATA));
			}
		}
	};

	private void clearUI() {
		mDataField.setText(R.string.no_data);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gatt_services_characteristics);

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

		// Sets up UI references.
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		mConnectionState = (TextView) findViewById(R.id.connection_state);
		mDataField = (TextView) findViewById(R.id.data_value);
		mCurrentValue = (TextView) findViewById(R.id.current_value);
		mChangeAngle = (SeekBar) findViewById(R.id.changeAngle);
		mInsertCard = (RadioButton) findViewById(R.id.insert_card);
		mRemoveCard = (RadioButton) findViewById(R.id.remove_card);
		mMinus = (Button) findViewById(R.id.minus);
		mPlus = (Button) findViewById(R.id.plus);

		// Set seekbar listner
		mChangeAngle.setMax(180);
		setSeekBarListner();

		// Set servo button listner
		setInsertCardRadioListner();
		setRemoveCardRadioListener();

		// Set stepper listner
		setMinusButtonListner();
		setPlusButtonListner();

		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		Intent gattServiceIntent = new Intent(this, RfduinoBleService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
	}

	private void setPlusButtonListner() {
		mPlus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int currentValue = servoData[1]++;
				mChangeAngle.setProgress(currentValue);
				mCurrentValue.setText(String.valueOf(currentValue));
				writeDataToBle(false);
			}
		});
	}

	private void setMinusButtonListner() {
		mMinus.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int currentValue = servoData[1]--;
				mChangeAngle.setProgress(currentValue);
				mCurrentValue.setText(String.valueOf(currentValue));
				writeDataToBle(false);
			}
		});
	}

	private void setRemoveCardRadioListener() {
		mRemoveCard.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				servoData[0] = 1;
				servoData[1] = 40;
				mChangeAngle.setProgress(servoData[1]);
				mCurrentValue.setText(String.valueOf(servoData[1]));
				writeDataToBle(true);
			}
		});
	}

	private void setInsertCardRadioListner() {
		mInsertCard.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				servoData[0] = 1;
				servoData[1] = 140;
				mChangeAngle.setProgress(servoData[1]);
				mCurrentValue.setText(String.valueOf(servoData[1]));
				writeDataToBle(true);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		if (mRfduinoBleService != null) {
			final boolean result = mRfduinoBleService.connect(mDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
		mRfduinoBleService = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.gatt_services, menu);
		if (mConnected) {
			menu.findItem(R.id.menu_connect).setVisible(false);
			menu.findItem(R.id.menu_disconnect).setVisible(true);
		} else {
			menu.findItem(R.id.menu_connect).setVisible(true);
			menu.findItem(R.id.menu_disconnect).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_connect:
				mRfduinoBleService.connect(mDeviceAddress);
				return true;
			case R.id.menu_disconnect:
				mRfduinoBleService.disconnect();
				return true;
			case android.R.id.home:
				onBackPressed();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateConnectionState(final int resourceId) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mConnectionState.setText(resourceId);
			}
		});
	}

	private void displayData(String data) {
		if (data != null) {
			mDataField.setText(data);
		}
	}

	// Demonstrates how to iterate through the supported GATT Services/Characteristics.
	// In this sample, we populate the data structure that is bound to the ExpandableListView
	// on the UI.
	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null) return;
		String uuid = null;
		String unknownServiceString = getResources().getString(R.string.unknown_service);
		String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
		ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
				= new ArrayList<ArrayList<HashMap<String, String>>>();
		mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			currentServiceData.put(
					LIST_NAME, RfduinoGattAttributes.lookup(uuid, unknownServiceString));
			currentServiceData.put(LIST_UUID, uuid);
			gattServiceData.add(currentServiceData);

			ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
					new ArrayList<HashMap<String, String>>();
			List<BluetoothGattCharacteristic> gattCharacteristics =
					gattService.getCharacteristics();
			ArrayList<BluetoothGattCharacteristic> charas =
					new ArrayList<BluetoothGattCharacteristic>();

			// Loops through available Characteristics.
			for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
				charas.add(gattCharacteristic);
				HashMap<String, String> currentCharaData = new HashMap<String, String>();
				uuid = gattCharacteristic.getUuid().toString();
				currentCharaData.put(
						LIST_NAME, RfduinoGattAttributes.lookup(uuid, unknownCharaString));
				currentCharaData.put(LIST_UUID, uuid);
				gattCharacteristicGroupData.add(currentCharaData);

				if (gattCharacteristic.getUuid().equals(RfduinoGattAttributes.UUID_SEND)) {
					characteristicTX = gattService.getCharacteristic(RfduinoGattAttributes.UUID_SEND);
				} else if (gattCharacteristic.getUuid().equals(RfduinoGattAttributes.UUID_RECEIVE)) {
					characteristicRX = gattService.getCharacteristic(RfduinoGattAttributes.UUID_RECEIVE);
				}
			}
			mGattCharacteristics.add(charas);
			gattCharacteristicData.add(gattCharacteristicGroupData);
		}
	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(RfduinoBleService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(RfduinoBleService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(RfduinoBleService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(RfduinoBleService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

	private void setSeekBarListner() {
		mChangeAngle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				servoData[1] = progress;
				mCurrentValue.setText(String.valueOf(progress));
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				writeDataToBle(false);
			}
		});
	}

	private void writeDataToBle(boolean auto) {
		if (auto) {
			servoData[0] = 1;
		} else {
			servoData[0] = 2;
		}
		String str = servoData[0] + "," + servoData[1];
		Log.d(TAG, "Sending result=" + str);
		final byte[] tx = str.getBytes();
		if (mConnected && characteristicRX != null && mRfduinoBleService != null) {
			boolean valueSet = characteristicTX.setValue(tx);
			Log.d(TAG, "value set is :" + valueSet);
			mRfduinoBleService.writeCharacteristic(characteristicTX);
			mRfduinoBleService.setCharacteristicNotification(characteristicRX, true);
		}
	}
}
