/*******************************************************************
 * @file BleService.java
 * @par Copyright (C) 2014-2015 MegaChips Corporation
 * @date 2014/02/26
 *******************************************************************/

package br.com.barboza.alexandre.sensorandgraph;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.weike.ble_service.BleDataHandler;
import com.weike.chiginon.BroadcastCommand;
import com.weike.chiginon.BroadcastData;
import com.weike.chiginon.DataPacket;
import com.weike.utils.DataHandlerUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import de.greenrobot.event.EventBus;

/**
 * Service for managing connection and data communication with a GATT server
 * hosted on a given Bluetooth LE device.
 */
@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BluetoothLeService extends Service {

    private BleDataHandler bleDataHandler = new BleDataHandler();
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private boolean mForceDisconnectd = false;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private boolean mConnecting = false;
    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    public static final int STATE_CONNECTED = 2;
    public DataFromActivityReceiver dataFromActivityReceiver;

    public static final UUID TX_POWER_UUID = UUID
            .fromString("00001804-0000-1000-8000-00805f9b34fb");
    public static final UUID TX_POWER_LEVEL_UUID = UUID
            .fromString("00002a07-0000-1000-8000-00805f9b34fb");
    public static final UUID CCCD = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static final UUID FIRMWARE_REVISON_UUID = UUID
            .fromString("00002a26-0000-1000-8000-00805f9b34fb");
    public static final UUID DIS_UUID = UUID
            .fromString("0000180a-0000-1000-8000-00805f9b34fb");
    public static final UUID RX_SERVICE_UUID = UUID
            .fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID RX_CHAR_UUID = UUID
            .fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    public static final UUID TX_CHAR_UUID = UUID
            .fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    //    private static final byte DATA_RECEIVE = (byte) 171;//0xAB
    private static final int SEND_PACKET_SIZE = 20;
    private static final int FREE = 0;
    private static final int SENDING = 1;
    private static final int RECEIVING = 2;
    private int STOP_SCAN = 1;
    private int CONNECT_DEVICE = 3;
    private int CONNECT_Gatt = 2;
    private String mScanAddress = null;
    private boolean mScanning = false;
    public String address;
    public String name;
    public int rrsi;
    /**
     * 开关手机蓝牙的广播
     */
    private BroadcastReceiver mBtReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED
                    .equals(intent.getAction())) {
                if (mBluetoothAdapter == null) {
                    mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    mBluetoothAdapter = mBluetoothManager.getAdapter();
                }
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                        == BluetoothAdapter.STATE_OFF) {
                    Log.e("lq369","Bluetooth off");
                    if (mScanning) {
                        //停止扫描
                        mHandler.sendEmptyMessage(STOP_SCAN);
                    } else {
                        if (mConnectionState == STATE_DISCONNECTED) {
                            close();
                        } else {
                            mForceDisconnectd = true;
                        }
                    }
                } else if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)
                        == BluetoothAdapter.STATE_ON) {
                    Log.e("lq369","Bluetooth open");
                    //连接DEVICE
                    mHandler.sendEmptyMessage(CONNECT_DEVICE);
                }
            }
        }
    };

    /**
     * 扫描
     */
    private LeScanCallback mScanCallback = new LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
//            Log.e("zgy",device.getAddress()+"___"+rssi);
            if (device != null && device.getAddress().equals(mScanAddress)
                    && !mConnecting) {
                mConnecting = true;
                mHandler.sendEmptyMessage(STOP_SCAN);
                mHandler.sendEmptyMessage(CONNECT_Gatt);
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == STOP_SCAN) {
                if (mBluetoothAdapter == null) {
                    mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
                    mBluetoothAdapter = mBluetoothManager.getAdapter();
                }
                mBluetoothAdapter.stopLeScan(mScanCallback);
            } else if (msg.what == CONNECT_Gatt) {
                BluetoothDevice remote = mBluetoothAdapter
                        .getRemoteDevice(mScanAddress);
                //连接手环
                mBluetoothGatt = remote.connectGatt(BluetoothLeService.this, false,
                        mGattCallback);
                mBluetoothDeviceAddress = mScanAddress;
                mConnectionState = STATE_CONNECTING;
            } else if (msg.what == CONNECT_DEVICE) {
//                connect(SPUtils.getString(BluetoothLeService.this,
//                        SPUtils.BIND_DEVICE_ADDRESS), false);
            }
        }
    };
    private int ble_status = FREE;
    private int packet_counter = 0;
    private int send_data_pointer = 0;
    private byte[] send_data = null;
    private boolean first_packet = false;
    private boolean final_packet = false;
    private boolean packet_send = false;

    private Timer mTimer;
    private int time_out_counter = 0;
    private int TIMER_INTERVAL = 100; // Timer interval for time out
    private int TIME_OUT_LIMIT = 100;

    public ArrayList<byte[]> data_queue = new ArrayList<byte[]>();
    boolean sendingStoredData = false;
    //    public static final String UNBIND = "unbind";
    private Intent it = new Intent("com.example.test.ble.RSSIReciver");

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        //连接状态改变的时候调用
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            Log.w(TAG, "onConnectionStateChange status: " + status
                    + "/newState:" + newState);
            String intentAction;
            // Attempts to discover services after successful connection.
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //已连接
                intentAction = BroadcastCommand.ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                //broadcastUpdate(intentAction);
                mBluetoothGatt.discoverServices();
                Log.i(TAG, address);
                EventBus.getDefault().post(new BaseEvent(BaseEvent.EventType.BLUETOOTH_CONNECTED));

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                //断开连接
                intentAction = BroadcastCommand.ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
               // broadcastUpdate(intentAction);
                Log.i(TAG, "Disconnected from GATT server.");
                EventBus.getDefault().post(new BaseEvent(BaseEvent.EventType.BLUETOOTH_DISCONNECTED));
                if (mForceDisconnectd) {
                    close();
                } else {
                    // try to reconnect
//                    connect(SPUtils.getString(BluetoothLeService.this,
//                            SPUtils.BIND_DEVICE_ADDRESS), false);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "mBluetoothGatt = " + mBluetoothGatt);
                broadcastUpdate(BroadcastCommand.ACTION_GATT_SERVICES_DISCOVERED);
                enableTXNotification();
            } else {
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead received: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(BroadcastCommand.ACTION_DATA_AVAILABLE,
                        characteristic);
            }
        }

        /**
         * 手环接收指令
         * @param gatt
         * @param characteristic
         * @param status
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            // Log.d(TAG, "onCharacteristicWrite: " + status);
            switch (status) {
                case BluetoothGatt.GATT_SUCCESS:
                    Log.e(TAG, "onCharacteristicWrite: GATT_SUCCESS");//接受指令
                    packet_send = true;
                    break;
            }
        }

        // Server -> Client(Android) 当有数据传过来的时候调用
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            final byte[] received = characteristic.getValue();
            String temp_text = "";
            //把字节数组转换成十六进制的字符串
            for (int i = 0; i < received.length; i++) {
                temp_text = (temp_text
                        + Integer.toHexString(received[i] & 0xff) + " ");
            }
            //获取蓝牙中的数据的广播
            broadcastUpdate(BroadcastCommand.ACTION_DATA_AVAILABLE,
                    characteristic);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                it.putExtra("RSSI", rssi);
                Log.e("zgy", "rssi:" + rssi);
                sendBroadcast(it);
            } else {
                close();
//                connect(SPUtils.getString(BleService.this, SPUtils.BIND_DEVICE_ADDRESS));
            }
        }
    };

    /**
     * 创建向蓝牙写入数据的广播   向手环发送数据
     */
    class DataFromActivityReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BroadcastCommand.DATA_RECEIVED_FROM_ACTIVITY)) {
                //获取BroadcastData对象
                BroadcastData bData = (BroadcastData) intent
                        .getSerializableExtra(BroadcastData.keyword);
                //蓝牙状态检查
                if (bData.commandID == BroadcastCommand.BLE_STATUS_CHECK) {
                    BroadCastConnectionStatus();
                    //向蓝牙中写数据
                } else if (bData.commandID == BroadcastCommand.BLE_RECEIVE_DATA) {

                    //获取要写入的字节数组
                    byte[] send_data = (byte[]) bData.data;
                    //发送数据
                    BLE_send_data_set(send_data, false);
                    for (byte b : send_data) {
                        Log.d(TAG,"BLE_RECEIVE_DATA:"+b);

                    }
                }
            }
//            if(action.equals(UNBIND)){
//                disconnect();
//            }
        }
    }

    /**
     * @brief Initializing bluetooth function.
     * 初始化蓝牙功能
     */
    @SuppressLint("InlinedApi")
    public boolean initialize() {

        //不支持蓝牙
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Bluetooth is not supported");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "不支持蓝牙");
            return false;
        }
        //注册向蓝牙写入数据的广播
        if (dataFromActivityReceiver == null){
            dataFromActivityReceiver = new DataFromActivityReceiver();
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    dataFromActivityReceiver, makeGattUpdateIntentFilter());
        }
        return true;
    }

    /**
     * @brief Send Broadcast intent.
     * @param[in] action string of intent action.
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    /**
     * @brief Set data into internal buffer for sending the data on BLE.
     * 设置数据到内部缓冲区对BLE发送数据
     * @param[in] data sending data.
     */
    private void BLE_send_data_set(byte[] data, boolean retry_status) {

        // mConnectionState);
        //蓝牙连接后才能写数据
        if (ble_status != FREE || mConnectionState != STATE_CONNECTED) {
            if (sendingStoredData == true) {
                if (retry_status == true) {
                    // non
                } else {
                    //字节数组添加到集合
                    data_queue.add(data);
                }
                return;
            } else {
                data_queue.add(data);
                start_timer();
            }

            //蓝牙没有连接，拒接向蓝牙发送数据
            /*
            final Intent intent = new Intent(
                    BroadcastCommand.ACTION_BLE_SEND_REQUEST_DENIED);
            BroadcastData bData = new BroadcastData();
            bData.data = null;
            intent.putExtra(BroadcastData.keyword, bData);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            */
        } else {
            //蓝牙的状态，正在发送中
            ble_status = SENDING;

            if (data_queue.size() != 0) {
                //获取发送的字节数组
                send_data = data_queue.get(0);
                sendingStoredData = false;
            } else {
                send_data = data;
            }
            //发送的包的数量
            packet_counter = 0;
            send_data_pointer = 0;
            //第一个包
            first_packet = true;
            //往蓝牙写数据
            BLE_data_send();

            if (data_queue.size() != 0) {
                data_queue.remove(0);
            }

            if (data_queue.size() == 0) {
                if (mTimer != null) {
                    mTimer.cancel();
                }
            }
        }
    }

    /**
     * bytes转换成十六进制字符串
     *
     * @return String 每个Byte值之间空格分隔
     */
    public static String byte2HexStr(byte[] b) {
        String stmp = "";
        StringBuilder sb = new StringBuilder("");
        for (int n = 0; n < b.length; n++) {
            stmp = Integer.toHexString(b[n] & 0xFF);
            sb.append((stmp.length() == 1) ? "0" + stmp : stmp);
            sb.append(" ");
        }
        return sb.toString().toUpperCase().trim();
    }

    //获取蓝牙的数据发送广播
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        //数据可用的广播
        final Intent intent = new Intent(action);

        if (TX_CHAR_UUID.equals(characteristic.getUuid())) {//已连接蓝牙
            if (ble_status == FREE || ble_status == RECEIVING) {
                ble_status = RECEIVING;
                //从蓝牙中获取数据
                final byte[] received = characteristic.getValue();

                //将字节添加到集合
                boolean result = bleDataHandler.add_data(received);
                if (result == true) {

                    int packet_status = bleDataHandler.check_packet();
                    if (packet_status == BleDataHandler.PACKET_COMPLETE) {//添加完成
                        //获取DataPacket对象（封装了蓝牙指令、字节）
                        DataPacket dataPacket = bleDataHandler.get_packet();
                        if (dataPacket != null) {
                            //手环-->手机
                            BroadcastData bData = new BroadcastData(BroadcastCommand.BLE_SEND_DATA);
                            bData.data = dataPacket;
                            intent.putExtra(BroadcastData.keyword, bData);
                            //发送本地广播
                            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                        }
                        ble_status = FREE;

                    } else if (packet_status == BleDataHandler.PACKET_INCOMPLETE) {
                    } else if (packet_status == BleDataHandler.PACKET_DATA_OVERSIZE) {
                        ble_status = FREE;
                    }
                } else {
                    bleDataHandler.clear_packet();
                    ble_status = FREE;
                }
            } else if (ble_status == SENDING) {

                if (final_packet == true) {
                    final_packet = false;
                    ble_status = FREE;
                }
                //读取蓝牙中的数据
                final byte[] received = characteristic.getValue();
                if (received.length == 1) {
                    ble_status = FREE;
                } else {
                    ble_status = FREE;
                }
            }
        }
    }

    /**
     * @brief Send data using BLE.
     */
    private void BLE_data_send() {
        Log.d(TAG,"Send data to Bluetooth");
        int err_count = 0;
        int send_data_pointer_save;
        int wait_counter;
        boolean first_packet_save;
        while (final_packet == false) {//不是最后一个包
            byte[] temp_buffer = null;
            send_data_pointer_save = send_data_pointer;
            first_packet_save = first_packet;
            if (first_packet == true) {//第一个包
                //发送数据大于20个字节
                if ((send_data.length - send_data_pointer) > (SEND_PACKET_SIZE)) {
                    temp_buffer = new byte[SEND_PACKET_SIZE];//20

                    for (int i = 0; i < SEND_PACKET_SIZE; i++) {
                        //将原数组加入新创建的数组
                        temp_buffer[i] = send_data[send_data_pointer];
                        send_data_pointer++;
                    }
                } else {
                    //发送的数据包不大于20
                    temp_buffer = new byte[send_data.length - send_data_pointer];

                    for (int i = 0; i < temp_buffer.length; i++) {
                        //将原数组未发送的部分加入新创建的数组
                        temp_buffer[i] = send_data[send_data_pointer];
                        send_data_pointer++;
                    }
                    final_packet = true;
                }
                first_packet = false;
            } else {
                //不是第一个包
                if (send_data.length - send_data_pointer >= SEND_PACKET_SIZE) {//数据长度大于20
                    temp_buffer = new byte[SEND_PACKET_SIZE];
                    //第二个包是0，第三个是1...
                    temp_buffer[0] = (byte) packet_counter;
                    for (int i = 1; i < SEND_PACKET_SIZE; i++) {
                        temp_buffer[i] = send_data[send_data_pointer];
                        send_data_pointer++;
                    }
                } else {
                    //数据长度不大于20
                    final_packet = true;
                    temp_buffer = new byte[send_data.length - send_data_pointer + 1];
                    temp_buffer[0] = (byte) packet_counter;
                    for (int i = 1; i < temp_buffer.length; i++) {
                        temp_buffer[i] = send_data[send_data_pointer];
                        send_data_pointer++;
                    }
                }
                packet_counter++;
            }
            packet_send = false;

            //向蓝牙中写数据temp_buffer
            boolean status = writeRXCharacteristic(temp_buffer);
            if ((status == false) && (err_count < 3)) {//数据写入失败
                err_count++;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
                Log.e(TAG, "writeRXCharacteristic false");
                send_data_pointer = send_data_pointer_save;
                first_packet = first_packet_save;
                packet_counter--;
            }
            // Send Wait
            for (wait_counter = 0; wait_counter < 5; wait_counter++) {
                if (packet_send == true) {
                    break;
                }
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
            // Log.i(TAG, String.format("wait_counter = %d",wait_counter));
        }
        final_packet = false;
        ble_status = FREE;//发送完后，蓝牙空闲
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);
        //注册手机蓝牙开关的广播
        registerReceiver(mBtReceiver, new IntentFilter(
                BluetoothAdapter.ACTION_STATE_CHANGED));
        //注册向蓝牙写入数据的广播
        initialize();
//        connect(SPUtils.getString(this, SPUtils.BIND_DEVICE_ADDRESS), false);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
//        close();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        EventBus.getDefault().unregister(this);
        disconnect();
        unregisterReceiver(mBtReceiver);
        super.onDestroy();
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     * @brief connect Connects to the GATT server hosted on the Bluetooth LE
     * device.
     */
    public boolean connect(final String address, boolean firstBind) {
        this.address=address;

        if (mBluetoothAdapter == null || address == null) {
            Log.e("lq369","connect:"+"连接手环");
            return false;
        }
        if (!mBluetoothAdapter.isEnabled())
            return false;
        mForceDisconnectd = false;
        if (TextUtils.isEmpty(address)) {
            // no address to connect
            return false;
        }
        // Previously connected device. Try to reconnect.
//        if (mBluetoothDeviceAddress != null
//                && address.equals(mBluetoothDeviceAddress)
//                && mBluetoothGatt != null) {
//            Log.d(TAG,
//                    "Trying to use an existing mBluetoothGatt for connection.");
//            if (mBluetoothGatt.connect()) {
//                mConnectionState = STATE_CONNECTING;
//                return true;
//            } else {
//                return false;
//            }
//        }
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (!firstBind || device == null) {
            searchDevice(address);
            return false;
        } else {
            // We want to directly connect to the device, so we are setting the
            // autoConnect parameter to false.
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            // Log.d(TAG, "mBluetoothGatt == null ? " + (mBluetoothGatt == null));
            Log.d(TAG, " Trying to create a new connection.");
            mBluetoothDeviceAddress = address;
            mConnectionState = STATE_CONNECTING;
            return true;
        }
    }

    // search util closing bt or find device
    private void searchDevice(String address) {
        mScanAddress = address;
        mConnecting = false;
        mBluetoothAdapter.startLeScan(mScanCallback);
    }

    /**
     * @brief Start Timer.
     */
    private void start_timer() {
        sendingStoredData = true;
        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = new Timer(true);
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                timer_Tick();
            }
        }, 100, TIMER_INTERVAL);
    }

    /**
     * @brief Interval timer function.
     */
    private void timer_Tick() {

        if (data_queue.size() != 0) {
            sendingStoredData = true;
            BLE_send_data_set(data_queue.get(0), true);
        }

        if (time_out_counter < TIME_OUT_LIMIT) {
            time_out_counter++;
        } else {
            ble_status = FREE;
            time_out_counter = 0;
        }
        return;
    }

    /**
     * @brief disconnect Disconnects an existing connection or cancel a pending
     * connection. The disconnection result is reported asynchronously
     * through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
//        SPUtils.putFloat(getApplicationContext(), SPUtils.BAND_VERSION, 0);
//        SPUtils.putFloat(getApplicationContext(), SPUtils.BAND_VERSION_TYPE, 0);
        mForceDisconnectd = true;
        Log.d(TAG, "disconnect");
        if(mBluetoothGatt!=null){
            mBluetoothGatt.disconnect();

        }
    }

    /**
     * @brief close After using a given BLE device, the app must call this
     * method to ensure resources are released properly.
     */
    private void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        Log.d(TAG, "close");
    }

    /**
     * @param characteristic The characteristic to read from.
     * @brief readCharacteristic Request a read on a given
     * {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the
     * {@code BluetoothGattCallback#start(android.bluetooth.
     * BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {

        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.readCharacteristic(characteristic);
    }

    /**
     * @brief enableTXNotification
     */
    @SuppressLint("InlinedApi")
    public void enableTXNotification() {
        BluetoothGattService RxService = mBluetoothGatt
                .getService(RX_SERVICE_UUID);
        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(BroadcastCommand.DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }

        BluetoothGattCharacteristic TxChar = RxService
                .getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            showMessage("Tx charateristic not found!");
            broadcastUpdate(BroadcastCommand.DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        Log.d("Moon", "-----  enbale tx -----");
        mBluetoothGatt.setCharacteristicNotification(TxChar, true);
        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }

    /**
     * @brief writeRXCharacteristic
     */
    public boolean writeRXCharacteristic(byte[] value) {
        BluetoothGattService RxService = mBluetoothGatt
                .getService(RX_SERVICE_UUID);
        showMessage("mBluetoothGatt null"+ mBluetoothGatt);

        if (RxService == null) {
            showMessage("Rx service not found!");
            broadcastUpdate(BroadcastCommand.DEVICE_DOES_NOT_SUPPORT_UART);
            return false;
        }

        BluetoothGattCharacteristic RxChar = RxService
                .getCharacteristic(RX_CHAR_UUID);
        if (RxChar == null) {
            showMessage("Rx charateristic not found!");
            broadcastUpdate(BroadcastCommand.DEVICE_DOES_NOT_SUPPORT_UART);
            return false;
        }
        //往蓝牙写入字节数组
        RxChar.setValue(value);
        boolean status = mBluetoothGatt.writeCharacteristic(RxChar);
        Log.d("lq", "send:" + DataHandlerUtils.byte2hexStr(value));
        Log.d("lq", "write TXchar - status=" + status);
        return status;
    }

    /**
     * @brief showMessage
     */
    private void showMessage(String msg) {
        Log.e(TAG, msg);
    }

    /**
     * @return A {@code List} of supported services.
     * @brief getSupportedGattServices Retrieves a list of supported GATT
     * services on the connected device. This should be invoked only
     * after {@code BluetoothGatt#discoverServices()} completes
     * successfully.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null)
            return null;
        return mBluetoothGatt.getServices();
    }

    /**
     * @brief Broadcast connection status of BLE.
     */
    private void BroadCastConnectionStatus() {
        final Intent intent = new Intent(
                BroadcastCommand.ACTION_BLE_STATUS_RESULT);
        BroadcastData bData = new BroadcastData();
        bData.data = mConnectionState;
        intent.putExtra(BroadcastData.keyword, bData);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastCommand.DATA_RECEIVED_FROM_ACTIVITY);
        return intentFilter;
    }

    public void onEventMainThread(BaseEvent baseEvent) {
        switch (baseEvent.getEventType()) {
            case UNBIND_DEVICE:
                disconnect();
                break;

            default:
                break;
        }
    }

}
