package br.com.barboza.alexandre.sensorandgraph;

/**
 * Created by sunny on 2015/11/20.
 * to accecpt evnetbus event base class
 */
public class BaseEvent {
    private int id;
    private String eventName;
    private EventType mEventType;
    private Object mObject;

    public void setEventType(EventType mEventType) {
        this.mEventType = mEventType;
    }

    public EventType getEventType() {
        return mEventType;
    }

    public BaseEvent() {
    }

    public BaseEvent(EventType mEventType) {
        this.mEventType = mEventType;
    }

    public Object getmObject() {
        return mObject;
    }

    public void setmObject(Object mObject) {
        this.mObject = mObject;
    }

    public enum EventType {
        LOGIN,
        LOGINOUT,
        REGISTER,
        CAHNGE_INFO,
        BLOOTH_PRESSURE_VISIBLE,
        BLOOTH_PRESSURE_GONE,
        BREATH_PRESSURE_VISIBLE,
        BREATH_PRESSURE_GONE,
        HEART_RATE_VISIBLE,
        HEART_RATE_GONE,
        MOOD_VISIBLE,
        MOOD_GONE,
        TIRED_VISIBLE,
        TIRED_GONE,

        //请求网络
        STEP,
        BLOOTH_PRESSURE,
        BREATH,
        HEART_RATE,
        TIRED,
        MOOD,

        NEXT_MEASURE_TIME,

        HEARTRATE_CHECKING,
        HEARTRATE_CANCEL,
        BLOOD_PRESSURE_CHECKING,
        BLOOD_PRESSURE_CANCEL,
        AUTO_MEASURE,
        HEARTRATE_DISABLE,
        HEARTRATE_ENABLE,
        BLOOD_PRESSURE_DISABLE,
        BLOOD_PRESSURE_ENABLE,

        BATTERY_PERCENT,

        UNBIND_DEVICE,

        DB_CHANGE,

        //实时测量
        HR_CHANGE,
        BO_CHANGE,
        BP_CHANGE,

        //一键测量
        ONE_KEY_MEASURE,

        AUTO_MEASURE_START,
        AUTO_MEASURE_END,

        OTA_UPDATE_START,
        BAND_VERSION_GOT,

        mEventType, STEP_SLEEP_CHANGE, //计步睡眠数据库改变
        RUN_RECORD,
        PAUSE_OR_CONTINUE,
        UPDATE_DISTANCE,
        RUN_TIMING,
        GPS_COUNT,
        DEVICE_CONNECT_CHANGE,

        CLEAR_DATA_SUCCESS,//清除数据
        CHARGE,
        SENSOR,
        SENSOR_HR,
        RSSI,
        KEY_TEST,
        BLUETOOTH_DISCONNECTED,
        BLUETOOTH_CONNECTED,
        DFUSUCCESS,
        HEARTTEST
    }
}
