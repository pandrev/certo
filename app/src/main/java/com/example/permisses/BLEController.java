package com.example.permisses;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BLEController {
    public final static String ACTION_GATT_CONNECTED = "com.examples.permisses.ACTION_GATT_CONNECTED";
    public static BLEController instance;

    private BluetoothDevice device;
    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner scanner;
    private BluetoothGatt bluetoothGatt;
    private BluetoothGattCharacteristic btGattChar;
    private HashMap<String, BluetoothDevice> devices = new HashMap<>();
    private Context ctx;

    private ConectivityManager cm;


    private BLEController(Context ctx) {
        this.ctx = ctx;
        this.bluetoothManager = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        cm = ConectivityManager.getInstance(ctx);
    }

    public static BLEController getInstance(Context ctx) {
        if (instance == null){
            instance = new BLEController((ctx));
        }
        return instance;
    }

    public void startScan() {
        cm.enqueueOperation(new Scan());
    }

    @SuppressLint("MissingPermission")
    public void init() {
        this.devices.clear();
        this.scanner = this.bluetoothManager.getAdapter().getBluetoothLeScanner();
        scanner.startScan(bleCallback);
    }

    private ScanCallback bleCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (!devices.containsKey(device.getAddress())  && isThisTheDevice(device)){
                deviceFound(device);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for(ScanResult sr : results) {
                BluetoothDevice device = sr.getDevice();
                if(!devices.containsKey(device.getAddress()) && isThisTheDevice(device)) {
                    deviceFound(device);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    @SuppressLint("MissingPermission")
    private boolean isThisTheDevice(BluetoothDevice device) {
        return null != device.getName() && device.getName().startsWith("Zer0");
    }

    @SuppressLint("MissingPermission")
    private void deviceFound(BluetoothDevice device) {
        this.devices.put(device.getAddress(), device);
        if (device != null && device.getName() != null){
            ((MainActivity)this.ctx).printDevices(device.getName(), device.getAddress());
            //Log.i("TAGHGGGGGGGGGGG", "ENCONTRADOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
            cm.signalEndOfOperation();
        }

    }

    @SuppressLint("MissingPermission")
    public void connectToDevice(String address) {
        this.device = this.devices.get(address);
        this.scanner.stopScan(this.bleCallback);
        this.bluetoothGatt = device.connectGatt(null, false, this.bleConnectCallback);
    }

    @SuppressLint("MissingPermission")
    private void enableServicesAndCharacteristics(BluetoothGatt gatt) {
        for (BluetoothGattService service : gatt.getServices()) {
            List<BluetoothGattCharacteristic> gattCharacteristics = service.getCharacteristics();
            for (BluetoothGattCharacteristic bgc : gattCharacteristics) {
                    int chprop = bgc.getProperties();
                        btGattChar = bgc;
                        //fireConnected();
                        readCharacteristic(bgc);
                        setCharacteristicNotification(bgc, true);

                        BluetoothGattDescriptor descriptor = bgc.getDescriptor(UUID.fromString("ac70a6e0-08df-40e2-a226-c3660abd3b64"));
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            bluetoothGatt.writeDescriptor(descriptor);
                        }

            }
        }
    }
    private final BluetoothGattCallback bleConnectCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
               loga("Conectado ao Dispositivo");
                bluetoothGatt.discoverServices();
                broadcastUpdate(ACTION_GATT_CONNECTED);
            }else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                btGattChar = null;
               //fireDisconnected();
            }
        }


        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(null == btGattChar) {
                enableServicesAndCharacteristics(gatt);
            }
        }

        public void onCharacteristicRead(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.getUuid().toString().equals("ac70a6e0-08df-40e2-a226-c3660abd3b64")){
                    loga(characteristic.getStringValue(0).toString());
                }
                if (characteristic.getUuid().toString().equals("163cc657-880e-4bf7-bef2-45f9fa959b9d")){
                    loga(characteristic.getStringValue(0).toString());
                }

            }
        }

        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] value) {
            Log.i("tag", "NOTIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIIII");
        }

        public void onCharacteristicChanged(
                BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic
        ) {
            Log.i("DDD", "MUDOUUUUUUUUUUUUUUUUUUUUUUUUUUUUUU");
        }
    };

    @SuppressLint("MissingPermission")
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bluetoothGatt == null) {
            Log.w("DDD", "BluetoothGatt not initialized");

        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    @SuppressLint("MissingPermission")
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt == null) {
            Log.w("DDD", "BluetoothGatt not initialized");
            return;
        }

        bluetoothGatt.readCharacteristic(characteristic);

    }

    public void getMonitorReadings() {
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString("ffac24fd-b2af-4746-a39a-5160c1091740"));
        BluetoothGattCharacteristic charac = service.getCharacteristic(UUID.fromString("ac70a6e0-08df-40e2-a226-c3660abd3b64"));
        readCharacteristic(charac);
    }

    public void getAnother() {
        BluetoothGattService service = bluetoothGatt.getService(UUID.fromString("ffac24fd-b2af-4746-a39a-5160c1091740"));
        BluetoothGattCharacteristic charac = service.getCharacteristic(UUID.fromString("163cc657-880e-4bf7-bef2-45f9fa959b9d"));
        readCharacteristic(charac);
    }
    private void loga(String msg) {
        ((MainActivity)this.ctx).log(msg);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        ctx.sendBroadcast(intent);
    }


}
