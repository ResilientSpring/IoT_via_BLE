package com.example.iot_via_ble;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    // To control UI elements, we will need to access them in code.
    Button startScanningButton;
    Button stopScanningButton;
    ListView deviceListView;

    // ListViews in Android are backed by adapters, which hold the data being displayed in a ListView.
    ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

    ArrayList<BluetoothDevice> deviceList = new ArrayList<>();

    private static final String TAG = "MyActivity";

    // The in-code representation of the actual Bluetooth Manager present on the Android device.
    BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
    BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

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

        // https://developer.android.com/guide/topics/ui/declaring-layout#AdapterViews
        deviceListView.setAdapter(listAdapter);
    }

    public void startScanning(View view) {
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
        }).start();
    }

    public void stopScanning(View view) {
        stopScanningButton.setVisibility(View.INVISIBLE);
        startScanningButton.setVisibility(View.VISIBLE);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                bluetoothLeScanner.stopScan(bleScanCallback);
            }
        });
    }

    private ScanCallback bleScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            if (result.getDevice() != null){

            }
        }
    }
}