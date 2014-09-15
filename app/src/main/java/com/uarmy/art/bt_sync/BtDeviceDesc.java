package com.uarmy.art.bt_sync;

import android.bluetooth.BluetoothDevice;

/**
 * Created by a on 8/23/14.
 */
public class BtDeviceDesc {
    private String myName;
    private String myMAC;
    private BluetoothDevice myDevice;
    private boolean myIsPaired;

    public BtDeviceDesc(BluetoothDevice device, boolean is_paired) {
        myName = device.getName();
        myName = myName==null ? "NULL" : myName;
        myMAC = device.getAddress();
        myDevice = device;
        myIsPaired = is_paired;
    }

    @Override public String toString() {
        return myName + "\n" + myMAC + "\n" + "paired:" + myIsPaired;
    }

    public String getName() { return myName; }
    public String getMAC() { return myMAC; }
    public BluetoothDevice getDevice() { return myDevice; }

    public boolean equals(BtDeviceDesc desc) {
        return myName.equals(desc.getName()) && myMAC.equals(desc.getMAC());
    }
}
