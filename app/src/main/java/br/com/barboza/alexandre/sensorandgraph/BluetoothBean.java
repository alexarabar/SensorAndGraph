package br.com.barboza.alexandre.sensorandgraph;

import android.bluetooth.BluetoothDevice;

/**
 * Created by liuqiong on 2016/12/15.
 */

public class BluetoothBean {
    private BluetoothDevice device;
    private int rssi;

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public String toString() {
        return "BluetoothBean{" +
                "device=" + device.getAddress() +
                ", rssi=" + rssi +
                '}';
    }
}
