package com.example.permisses;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ActivityResultLauncher<String[]> permissionRequet;
    ActivityResultLauncher<Intent> BleCheck;
    TextView logView;

    String deviceAddress;

    Button connectButton;

    TextView status;
    Button getMonitorReadings;

    ConectivityManager cm;

    Button getAnother;

    BLEController bleController;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logView = findViewById(R.id.logView);
        logView.setMovementMethod(new ScrollingMovementMethod());

        permissionRequet = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            boolean fineLocationPermission = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            boolean coarseLocationPermission = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
            boolean bluetoothScan = result.getOrDefault(Manifest.permission.BLUETOOTH_SCAN, false);
            boolean bluetoothConnect = result.getOrDefault(Manifest.permission.BLUETOOTH_CONNECT, false);
        });

        permissionRequet.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
        });

        checkBLE();


        bleController = BLEController.getInstance(this);
        bleController.startScan();
        bleController.init();

        /*cm = ConectivityManager.getInstance(this);
        cm.startScan(new Scan());*/

        initConnectButton();
        getMonitorReadings();
        getAnother();
        status = findViewById(R.id.mystatus);
    }

    public void log(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
               logView.setText(logView.getText() + "\n" + text);
            }
        });
    }

    public void printDevices(String name, String address) {
        log("Device " + name + " found with address " + address);
        this.deviceAddress = address;
        this.connectButton.setEnabled(true);
    }

    //Verifica se o telemóvel suporta/tem BLE
    public void checkBLE() {
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish();
        }
    }

    @SuppressLint("MissingPermission")
    protected void onStart() {
        super.onStart();
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        //Se tem BLE então esta função verifica se o BLE está ligado ou não. Se não estiver ligado, ele pergunta ao usuário se quer ligar (BleCheck.launch
        if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent bleIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            BleCheck = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result-> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.i("TAG", "DEU COM SUCESSO");
                }
            });

            BleCheck.launch(bleIntent);

        }
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(gattUpdateReceiver);
    }

    private void initConnectButton() {
        this.connectButton = findViewById(R.id.connectButton);
        this.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectButton.setEnabled(false);
                log("Connecting...");
                bleController.connectToDevice(deviceAddress);
            }
        });
    }

    private void getMonitorReadings() {
        this.getMonitorReadings = findViewById(R.id.getMonitorReadings);
        this.getMonitorReadings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bleController.getMonitorReadings();
            }
        });
    }

    private void getAnother() {
        this.getAnother = findViewById(R.id.getAnother);
        this.getAnother.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bleController.getAnother();
            }
        });
    }

    private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (bleController.ACTION_GATT_CONNECTED.equals(action)) {
                updateConnectionChange();
            }
        }
    };

    private void updateConnectionChange() {
        status.setText("Conectado com sucesso ao servidor!!!");
        Toast.makeText(this, "Conectado com sucesso", Toast.LENGTH_SHORT).show();
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BLEController.ACTION_GATT_CONNECTED);
        return intentFilter;
    }
}