package com.bt901;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Set;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */

public class DeviceListActivity extends Activity {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    // Member fields
    private BluetoothAdapter mBtAdapter;
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    List<String> DeviceList = new ArrayList<String>();
    List<String> DeviceNameFilter = Arrays.asList("HC-06","");
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Set result CANCELED incase the user backs out
        //如果一个activity要返回数据到启动它的那个activity，可以调用setResult()方法
        setResult(Activity.RESULT_CANCELED);
        getPermission();
        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        DeviceList.clear();
        // Get a set of currently paired devices
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                if (DeviceNameFilter.contains(device.getName())){
                    mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            }
        } else {
        String noDevices = getResources().getText(R.string.none_paired).toString();
        mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }
        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    private  final int ACCESS_LOCATION=1;
    @SuppressLint("WrongConstant")
    private void getPermission(){
        if (Build.VERSION.SDK_INT>Build.VERSION_CODES.M){
            int permissionCheck = 0;
            permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            permissionCheck += this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED){
                this.requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},ACCESS_LOCATION
                );
            }
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[]grantResults){
        switch (requestCode){
            case ACCESS_LOCATION:
                if(hasAllPermissionGranted(grantResults)){
                    Log.e("--","用户允许权限");
                }else{
                    Log.e("--","用户禁止权限");
                }
                break;
        }
    }
    private boolean hasAllPermissionGranted(int[] grantResults){
        for (int grantResult:grantResults){
            if (grantResult == PackageManager.PERMISSION_DENIED) return false;
        }
        return  true;
    }

    private void doDiscovery() {
        if (D) Log.d(TAG, "doDiscovery()");
        // Indicate scanning in the title
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);
        // Turn on sub-title for new devices
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
        // If we're already discovering, stop it
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }
        // Request discover from BluetoothAdapter
        mBtAdapter.startDiscovery();
    }

    // The on-click listener for all devices in the ListViews
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            mBtAdapter.cancelDiscovery();
            // Get the device MAC address, which is the last 17 chars in the View
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            Log.e("--", "经典蓝牙" + info + "~" + address);
            // Create the result Intent and include the MAC address
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);
            finish();    //程序自动返回之前的activity
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e("--", "action=" + action);
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getExtras().getShort(BluetoothDevice.EXTRA_RSSI);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    Log.e("--", "device=" + device.getName() + "--" + device.getAddress());
                    if(DeviceList.contains(device.getAddress())){

                        int i = DeviceList.indexOf(device.getAddress());
                        Log.e("--", "find device at " +i);
                        mNewDevicesArrayAdapter.insert(device.getName() + "(" + rssi + "db)" + "\n" + device.getAddress(),i);
                        mNewDevicesArrayAdapter.remove(mNewDevicesArrayAdapter.getItem(i+1));
                        return;
                    }
                    if ((DeviceNameFilter.contains(device.getName()))||(device.getName()==null)){
                            Log.e("--", "device=" + device.getName() + "--" + device.getAddress());
                            mNewDevicesArrayAdapter.add(device.getName() + "(" + rssi + "db)" + "\n" + device.getAddress());
                            DeviceList.add(device.getAddress());
                    }
                  //  }

//                    if (device.getName()!=null) {
//                        if (DeviceNameFilter.contains(device.getName())){
//                            if(!DeviceList.contains(device.getAddress())){
//                                Log.e("--", "device=" + device.getName() + "--" + device.getAddress());
//                                mNewDevicesArrayAdapter.add(device.getName() + "(" + rssi + "db)" + "\n" + device.getAddress());
//                                DeviceList.add(device.getAddress());
//                            }
//                        }
//                    }
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setProgressBarIndeterminateVisibility(false);
                setTitle(R.string.select_device);
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };


}
