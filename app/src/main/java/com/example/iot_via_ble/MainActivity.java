package com.example.iot_via_ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // To control UI elements, we will need to access them in code.
    Button startScanningButton;
    Button stopScanningButton;
    ListView deviceListView;

    // ListViews in Android are backed by adapters, which hold the data being displayed in a ListView.
    // System services not available to Activities before onCreate().
    ArrayAdapter<String> listAdapter; // = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

    ArrayList<BluetoothDevice> deviceList = new ArrayList<>();

    private static final String TAG = "MyActivity";

    // The in-code representation of the actual Bluetooth Manager present on the Android device.
    // System services not available to Activities before onCreate().
    BluetoothManager bluetoothManager; // = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
    BluetoothAdapter bluetoothAdapter; // = bluetoothManager.getAdapter();
    BluetoothLeScanner bluetoothLeScanner; // = bluetoothAdapter.getBluetoothLeScanner();

    private final static int REQUEST_ENABLE_BT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        deviceListView = findViewById(R.id.deviceListView);
        startScanningButton = findViewById(R.id.StartScanButton);
        stopScanningButton = findViewById(R.id.StopScanButton);

        // We don't need the button initially (we only need it once we start scanning.)
        stopScanningButton.setVisibility(View.INVISIBLE);

        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        // https://developer.android.com/guide/topics/ui/declaring-layout#AdapterViews
        deviceListView.setAdapter(listAdapter);
        deviceListView.setOnItemClickListener(listClickListener);

        bluetoothManager  = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    public void startScanning() {
        listAdapter.clear();
        deviceList.clear();
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);

        // Instruct the BluetoothLEScanner to start scanning.
        new Thread(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.startScan(bleScanCallback);
            }
        }).start(); // [1][Note1]
    }

    public void stopScanning() {
        stopScanningButton.setVisibility(View.INVISIBLE);
        startScanningButton.setVisibility(View.VISIBLE);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.stopScan(bleScanCallback);
            }
        });
    }

    // Scan result callback.
    private ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if (result.getDevice() != null)
                if (!isDuplicate(result.getDevice()))
                    synchronized (result.getDevice()) {
                        String itemDetail = result.getDevice().getName() == null ? result.getDevice().getAddress() : result.getDevice().getName();
                        listAdapter.add(itemDetail);
                        deviceList.add(result.getDevice());
                    }
        }
    };

    // Avoid adding the same device to the lists of device and adapter.
    private boolean isDuplicate(BluetoothDevice device){
        for (int i = 0; i < listAdapter.getCount(); i++) {
            String addedDeviceDetail = listAdapter.getItem(i);

            if (addedDeviceDetail.equals(device.getAddress()) || addedDeviceDetail.equals(device.getName()))
                return true;
        }

        return false;
    }

    // Define what would happen once the list items containing scan result is clicked.
    AdapterView.OnItemClickListener listClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            stopScanning();
            listAdapter.clear();

            // Determine which of the list items is clicked.
            BluetoothDevice device = deviceList.get(position);

            // Connect to Xiaomi night light
            device.connectGatt(MainActivity.this, true, gattCallback);
        }
    };

    // Callback from Xiaomi night light
    protected BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        // When Services are discovered.
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            Log.i(TAG, "onServiceDiscovered()");

            // Get Xiaomi night light's services to a list
            final List<BluetoothGattService> services = gatt.getServices();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    // Iterate through available GATT services in order to get each service's
                    // available characteristics.
                    for (int i = 0; i < services.size(); i++) {
                        BluetoothGattService service = services.get(i);
                        Log.i(TAG, "onServicesDiscovered() :: " + service.getUuid().toString());

                        // String Buffer is like a String, but mutable and thread-safe.
                        // Get each service's UUID.
                        StringBuffer buffer = new StringBuffer(services.get(i).getUuid().toString());

                        // Get each service's available characteristics to a list.
                        List<BluetoothGattCharacteristic> characteristics = service.getCharacteristics();
                        for (int j = 0; j < characteristics.size(); j++) {
                            buffer.append("\n");
                            buffer.append("Characteristic: ").append(characteristics.get(j).getUuid().toString());
                        }
                        listAdapter.add(buffer.toString());
                    }
                }
            });
        }
    };
}

// References:
// 1. https://developer.android.com/guide/components/processes-and-threads
// 2. https://developer.android.com/reference/android/os/AsyncTask

// Notes:
// 1. This class (Async) was deprecated in API level 30 because it would cause Context leaks,
// missed callbacks, or crashes on configuration changes. It also has inconsistent behavior on
// different versions of the platform, swallows exceptions from doInBackground,
// and does not provide much utility over using Executors directly.[2]