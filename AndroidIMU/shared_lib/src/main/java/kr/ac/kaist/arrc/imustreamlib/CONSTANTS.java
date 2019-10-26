package kr.ac.kaist.arrc.imustreamlib;

/**
 * Created by arrc on 4/10/2018.
 */

public class CONSTANTS {

    // force devices to connect to this wifi network
    // should put Phone(leg)'s hotspot ssid and passwd
    // ***IMPORTANT***CANNOT CHANGE ON APP
    public static boolean FORCE_WIFI = false;
    public static String SSID = "ssid";
    public static String PASSWD = "xxxxxx";



    // target IP address
    // usually 192.168.43.1 is gateway(phone) IP for android hotspot
    // can change by sending broadcast message
    public static String IP_ADDRESS = "192.168.0.8";

    // each devices' IP address
    // DON'T NEED TO CHANGE (initialized by input packet)
    public static String GLASS_IP = "192.168.43.106";
    public static String WEAR_IP = "192.168.43.191";
    public static String PHONE_IP = "this";

    public static int DEVICE_ID = 0; //0:leg, 1:hand, 2:head

    public static int SS_W_SIZE = 1000;

    // port for sensor data stream
    public static int PORT = 12562;

    // port for Glass noti sharing
    public static int DATA_PORT = 11563;
    public static int DATA_BYTE_SIZE = 48;

    /**
     * Android sensor manager registered Interval Value (ms)
     **/

    //sensor delay for SensorManager in mssec
    public static int SENSOR_DELAY = 8;


    /**
     * Resample Interval Value (ms)
     * - Sensor manager doesn't give correct interval
     **/
//    public static final int SENDING_INTERVAL = 50; //50Hz
    public static int SENDING_INTERVAL = 20;
//    public static int SENDING_INTERVAL = SENSOR_DELAY - 1;    //100Hz
    public static int SENDING_INTERVAL_HALF = SENDING_INTERVAL / 2;    // for thread sleep in UDP socket part

    // variable to share with sending part
    // Gyro    Acc,   Rotvec  Millis  ID
    // 4,4,4, 4,4,4, 4,4,4,4,   8,    4
    public static int BYTE_SIZE = 52;
    public static int QUEUE_SIZE = 30;

    public static void setSensorDelay(int delay) {
        SENSOR_DELAY = delay;
        SENDING_INTERVAL = SENSOR_DELAY *2;
        SENDING_INTERVAL_HALF = SENDING_INTERVAL / 2;
    }


    // error notification
    // ERROR_NOTI_TIME: alert error when connection failed more than this time(msec)
    // ERROR_NOTI_REPEAT: repeat alert in this period
    public static int ERROR_NOTI_TIME = 10000;
    public static int ERROR_NOTI_REPEAT = 2000;


}

