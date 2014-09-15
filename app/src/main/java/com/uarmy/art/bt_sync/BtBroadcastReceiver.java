package com.uarmy.art.bt_sync;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.ArrayAdapter;

/**
 * Created by a on 8/23/14.
 */
public class BtBroadcastReceiver extends BroadcastReceiver {
    private ArrayAdapter<BtDeviceDesc> myAdapter;
    private static final String TAG = "BtReceiver";

    public void setAdapter(ArrayAdapter<BtDeviceDesc> adapter)
    {
        myAdapter = adapter;
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        // When discovery finds a device
        if (BluetoothDevice.ACTION_FOUND.equals(action)) {
            // Get the BluetoothDevice object from the Intent
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            // Add the name and address to an array adapter to show in a ListView
            String name = device.getName() + "\n" + device.getAddress();
            Log.d(TAG, "discovered: " + name);
            BtDeviceDesc new_device = new BtDeviceDesc(device, false);
            int n = myAdapter.getCount();
            for(int i=0; i<n;++i) {
                if(myAdapter.getItem(i).equals(new_device)) {
                    return;
                }
            }
            myAdapter.add(new_device);
        }
    }
};
