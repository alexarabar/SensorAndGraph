package br.com.barboza.alexandre.sensorandgraph;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.weike.manager.CommandManager;
import com.weike.chiginon.BroadcastCommand;
import com.weike.chiginon.BroadcastData;
import com.weike.chiginon.DataPacket;

/*
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
*/

import java.util.ArrayList;
import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;
import de.greenrobot.event.EventBus;

public class MainActivity extends AppCompatActivity {


    @InjectView(R.id.gridview)
    GridView gridview;
    @InjectView(R.id.address)
    TextView address;

    private List<String> list;
    private BluetoothLeService mBluetoothLeService;
    private String mDeviceAddress;
    private CommandManager manager;

    //private GraphView graph;
    //private LineGraphSeries<DataPoint> mSeries;
    //private EditText text;

    private final int step = 50;
    private ArrayList<Integer> HeartBeat = new ArrayList<>();

    private final BroadcastReceiver BLEStatusChangeReceiver = new BroadcastReceiver() {

        @SuppressLint("UseValueOf")

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(BroadcastCommand.ACTION_DATA_AVAILABLE)) {
                BroadcastData bData = (BroadcastData) intent
                        .getSerializableExtra(BroadcastData.keyword);
                if (bData.commandID == BroadcastCommand.BLE_SEND_DATA) {
                    DataPacket dataPacket = (DataPacket) bData.data;
                    ArrayList<Byte> datas = dataPacket.data;

                    //byte ---> int
                    ArrayList<Integer> data = new ArrayList<>();
                    for (int i = 0; i < datas.size(); i++) {
                        int ii = datas.get(i) & 0xff;
                        data.add(ii);
                    }

                    StringBuffer stringBuffer = new StringBuffer();
                    stringBuffer.append("Received data：");
                    for (int i = 0; i < data.size(); i++) {
                        stringBuffer.append(data.get(i) + " ");
                    }
                    Toast.makeText(context, stringBuffer.toString(), Toast.LENGTH_LONG).show();

                    //battery power
                    if (data.get(0) == 0x91) {

                    }

                    //Bracelet version information
                    if (data.get(0) == 0x92) {

                    }

                    //The first package of integer data
                    if (data.get(0) == 0X51 && data.get(1) == 0x20) {

                    }

                    //The second package of integer data
                    if (dataPacket.commandID == 0) {

                    }

                    //Single, real-time measurement data
                    if (data.get(0) == 0x31) {
                        //Single measurement
                        if (data.get(1) == 0x09 || data.get(1) == 0x11 || data.get(1) == 0x21) {
                            if (data.get(1) == 0x09) {
                                //Heart rate

                            } else if (data.get(1) == 0x11) {
                                //Blood oxygen

                            } else if (data.get(1) == 0x21) {
                                //blood pressure
                            }

                        } else {
                            //Real-time measurement
                            if (data.get(1) == 0x0A) {
                                //Heart rate
                                HeartBeat=data;
                            } else if (data.get(1) == 0x12) {
                                //Blood oxygen

                            } else if (data.get(1) == 0x22) {
                                //blood pressure

                            }
                        }
                    }

                    //One-button measurement
                    if (data.get(0) == 0x32) {

                    }

                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);
        EventBus.getDefault().register(this);

        manager = CommandManager.getInstance(this);

        //Bluetooth service
        Intent bindIntent = new Intent(this, BluetoothLeService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        //Registration broadcast
        LocalBroadcastManager.getInstance(this).registerReceiver(
                BLEStatusChangeReceiver, makeGattUpdateIntentFilter());

        //Connect to Bluetooth
        initdata();

        MyAdapter myAdapter = new MyAdapter(list);
        try {
            gridview.setAdapter(myAdapter);
        } catch (NullPointerException e) {
            return;
        }

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        Log.i("test", "Find the bracelet");
                        manager.findBand();
                        break;
                    case 1:
                        Log.i("test", "Pull-down sync");
                        manager.setSyncData(System.currentTimeMillis() - 7*24*3600*1000);//发这个时间点之后 的数据过来  ， 7天前的数据不发过来
                        break;
                    case 2:
                        Log.i("test", "Smart reminder");
                        manager.smartWarnInfo(7, 2, "MB Band");
//                        参数1
//                        1	Incoming calling
//                        2	Missed Call
//                        3	Messages
//                        4	Mail
//                        5	Calendar
//                        6	FaceTime
//                        7	QQ
//                        8	Skype

//                        参数2
//                        0:关 1：开 2:来消息通知

//                        参数3
//                        消息内容
                        break;
                    case 3:
                        Log.i("test", "Raise your hand to brighten");
                        manager.upHandLightScreen(1);//0关 1开
                        break;
                    case 4:
                        Log.i("test", "Shake to take pictures");
                        manager.sharkTakePhoto(1);//0关 1开
                        break;
                    case 5:
                        Log.i("test", "Anti-lost");
                        manager.antiLost(1);//0关 1开
                        break;
                    case 6:
                        Log.i("test", "Electricity");
                        manager.getBattery();
                        break;
                    case 7:
                        Log.i("test", "version number");
                        manager.versionInfo();
                        break;
                    case 8:
                        Log.i("test", "synchronised time");
                        manager.setTimeSync();
                        break;
                    case 9:
                        Log.i("test", "Clear bracelet data");
                        manager.clearData();
                        break;
                    case 10:
                        Log.i("test", "Fall");
                        manager.falldownWarn(1);//0关 1开
                        break;
                    case 11:
                        Log.i("test", "Heart rate single measurement");
                        manager.realTimeAndOnceMeasure(0X09,1);
                        //Parameter 1
                        //// Distinguish between single and real-time measurements
                        //
                        //// parameter 2
                        //// 0 off 1 open
                        break;
                    case 12:
                        Log.i("test", "Heart rate real-time measurement");
                        manager.realTimeAndOnceMeasure(0X0A,1);

                        break;
                    case 13:
                        Log.i("test", "Blood oxygen single measurement");
                        manager.realTimeAndOnceMeasure(0X11,1);

                        break;
                    case 14:
                        Log.i("test", "Blood oxygen real-time measurement");
                        manager.realTimeAndOnceMeasure(0X12,1);

                        break;
                    case 15:
                        Log.i("test", "Blood pressure single measurement");
                        manager.realTimeAndOnceMeasure(0X21,1);

                        break;
                    case 16:
                        Log.i("test", "Blood pressure real-time measurement");
                        manager.realTimeAndOnceMeasure(0X22,1);

                        break;
                }
            }
        });
    }

    private IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadcastCommand.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BroadcastCommand.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BroadcastCommand.ACTION_GATT_DISCONNECTED);
        return intentFilter;
    }
    /*
    private DataPoint[] generateData() {
        DataPoint[] values = new DataPoint[step +1];
        for (int i = 0; i < (step +1); i++) {
            double x = i;
            Integer y = HeartBeat.get(i);
            DataPoint v = new DataPoint(x, y);
            values[i] = v;
        }
        return values;
    }
    */

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            Log.i("zgy", "onServiceConnected---==---");
            if (!mBluetoothLeService.initialize()) {
                Log.e("FragmentActivity", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress, false);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
            Log.i("zgy", "onServiceDisconnected");
        }
    };

    private void initdata() {
        list = new ArrayList<>();
        list.add("Find the bracelet");
        list.add("Pull-down sync");
        list.add("Smart reminder");
        list.add("Find the bracelet");
        list.add("Shake to take pictures");
        list.add("Anti-lost");
        list.add("Electricity");
        list.add("version number");
        list.add("synchronised time");
        list.add("clear data");
        list.add("Fall");
        list.add("Heart rate single measurement");
        list.add("Heart rate real-time measurement");
        list.add("Blood oxygen single measurement");
        list.add("Blood oxygen real-time measurement");
        list.add("Blood pressure single measurement");
        list.add("Blood pressure real-time measurement");
    }

    class MyAdapter extends BaseAdapter {
        private List<String> list;

        public MyAdapter(List<String> list) {
            this.list = list;
        }

        @Override
        public int getCount() {
            return list == null ? 0 : list.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.channel_item, null);
                viewHolder.text = (TextView) convertView.findViewById(R.id.channel_text);
                convertView.setTag(viewHolder);

            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }


            viewHolder.text.setText(list.get(i));
            return convertView;
        }

    }

    class ViewHolder {
        TextView text;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(BLEStatusChangeReceiver);
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.setting, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_search:
                intent = new Intent(MainActivity.this, DeviceScanActivity.class);
                startActivityForResult(intent, 0);
                break;
            case R.id.action_graph:
                intent = new Intent(MainActivity.this, GraphActivity.class);
                startActivityForResult(intent, 0);
                break;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 1) {
            mDeviceAddress=data.getStringExtra("mac");
            mBluetoothLeService.connect(mDeviceAddress,false);
            address.setText("connecting...");
        }

    }

    public void onEventMainThread(BaseEvent baseEvent) {
        switch (baseEvent.getEventType()) {
            case BLUETOOTH_CONNECTED:
                address.setText(mDeviceAddress+"  connected");

                break;
            case BLUETOOTH_DISCONNECTED:
                Log.e("zgy", "BLUETOOTH_DISCONNECTED");
                address.setText("Disconnect...");
//
                break;

            default:
                break;
        }
    }
}