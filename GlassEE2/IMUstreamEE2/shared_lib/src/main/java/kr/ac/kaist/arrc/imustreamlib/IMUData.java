package kr.ac.kaist.arrc.imustreamlib;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class IMUData {
    private final String TAG = "IMUData";
    private int size;

    private boolean pause = false;

    private String HEAD_IP, HAND_IP, LEG_IP;
    private long HEAD_last_t, HAND_last_t, LEG_last_t;
    private long HEAD_added_t, HAND_added_t, LEG_added_t;
    private int HEAD_dt, HAND_dt, LEG_dt;
    private int HEAD_index, HAND_index, LEG_index;

    private String[] data_label = {"GYRO_X", "GYRO_Y", "GYRO_Z"};
    Map<String, ArrayList<Double>> HEAD_DATA, HAND_DATA, LEG_DATA;
    ArrayList<Long> HEAD_TIME, HAND_TIME, LEG_TIME;

    public IMUData(){
        this(1000);
    }
    public IMUData(int size){
        HEAD_DATA = new HashMap<String, ArrayList<Double>>();
        HAND_DATA = new HashMap<String, ArrayList<Double>>();
        LEG_DATA = new HashMap<String, ArrayList<Double>>();

        HEAD_last_t = HAND_last_t = LEG_last_t = 0;

        this.size = size;
        Double [] arr = new Double[this.size];    //create zeros array for initialization
        Long [] arr2 = new Long[this.size];
        for(int i = 0; i < this.size ; i++) {
            arr[i] = 0.0;
            arr2[i] = (long) 0.0;
        }

        for(int i = 0; i<this.data_label.length; i++) {
//            Log.d("selfsync.SelfSyncData:initData", data_label[i]+" initialized");
            HEAD_DATA.put(data_label[i], new ArrayList<Double>(Arrays.asList(arr)));
            HAND_DATA.put(data_label[i], new ArrayList<Double>(Arrays.asList(arr)));
            LEG_DATA.put(data_label[i], new ArrayList<Double>(Arrays.asList(arr)));
        }

        HEAD_TIME = new ArrayList<Long>(Arrays.asList(arr2));
        HAND_TIME = new ArrayList<Long>(Arrays.asList(arr2));
        LEG_TIME = new ArrayList<Long>(Arrays.asList(arr2));
    }

    public void setIPs(String head_ip, String hand_ip){
        this.HEAD_IP = head_ip;
        this.HAND_IP = hand_ip;
        this.LEG_IP = "this";
    }
    public void setIPs(String head_ip, String hand_ip, String leg_ip){
        this.HEAD_IP = head_ip;
        this.HAND_IP = hand_ip;
        this.LEG_IP = leg_ip;
    }

    public static int getIndexByTime(ArrayList<Long> TIME, int dt_ms) {
        int index = 0;

        long target_t = TIME.get(TIME.size()-1) - dt_ms;

        if(target_t<0){
            Log.d("getIndexByTime", "if(target_t<0)");
            return 10;
        }

        long this_t = 0;
        for(int i = 1; i < TIME.size(); i++){

            this_t = TIME.get(TIME.size() - i);
//            if(this_t!=0 && this_t < target_t){
            if(this_t < target_t){
                index = i-1;
                break;
            }
//            else if ( this_t == 0 ) {
//                index = i-1;
//                break;
//            }
        }
        return index;
    }
    public static double[][] hashTo2DArray(Map<String, ArrayList<Double>> ONE_DATA, int index) {
        double[][] OUT_ARRAY = new double[3][index];

        int start_index = ONE_DATA.get("GYRO_X").size() - index;
        for(int i = 0; i < index; i++) {
            OUT_ARRAY[0][i] = ONE_DATA.get("GYRO_X").get(start_index + i);
            OUT_ARRAY[1][i] = ONE_DATA.get("GYRO_Y").get(start_index + i);
            OUT_ARRAY[2][i] = ONE_DATA.get("GYRO_Z").get(start_index + i);
        }
        return OUT_ARRAY;
    }

    public HashMap<String, double[][]> getData(int dt_ms){
        HashMap<String, double[][]> result_sigs = new HashMap<String, double[][]>();

        Map<String, ArrayList<Double>> HEAD_DATA_cp, HAND_DATA_cp, LEG_DATA_cp;
        HEAD_DATA_cp = new HashMap<String, ArrayList<Double>>();
        HAND_DATA_cp = new HashMap<String, ArrayList<Double>>();
        LEG_DATA_cp = new HashMap<String, ArrayList<Double>>();
        ArrayList<Long> HEAD_TIME_cp, HAND_TIME_cp, LEG_TIME_cp;

        this.pause = true;
        // copy data to process
        for(int i = 0; i<this.data_label.length; i++) {
            HEAD_DATA_cp.put(data_label[i], new ArrayList<Double>(HEAD_DATA.get(data_label[i])));
            HAND_DATA_cp.put(data_label[i], new ArrayList<Double>(HAND_DATA.get(data_label[i])));
            LEG_DATA_cp.put(data_label[i], new ArrayList<Double>(LEG_DATA.get(data_label[i])));
        }
        HEAD_TIME_cp = new ArrayList<Long>(HEAD_TIME);
        HAND_TIME_cp = new ArrayList<Long>(HAND_TIME);
        LEG_TIME_cp = new ArrayList<Long>(LEG_TIME);
        this.pause = false;

        // find index to return
        HEAD_index = getIndexByTime(HEAD_TIME_cp, dt_ms);
        HAND_index = getIndexByTime(HAND_TIME_cp, dt_ms);
        LEG_index = getIndexByTime(LEG_TIME_cp, dt_ms);

        //copy selected part to array
        result_sigs.put("head", hashTo2DArray(HEAD_DATA_cp, HEAD_index));
        result_sigs.put("hand", hashTo2DArray(HAND_DATA_cp, HAND_index));
        result_sigs.put("leg", hashTo2DArray(LEG_DATA_cp, LEG_index));


        return result_sigs;
    }

    public void addValue(int device_id, long cur_t, float val_x, float val_y, float val_z){

        switch (device_id) {
            case 0:
                this.addValue(LEG_IP, cur_t, (double)val_x, (double)val_y, (double)val_z );
                break;
            case 1:
                this.addValue(HAND_IP, cur_t, (double)val_x, (double)val_y, (double)val_z );
                break;
            case 2:
                this.addValue(HEAD_IP, cur_t, (double)val_x, (double)val_y, (double)val_z );
                break;
            default:
                Log.d(TAG, "no id found: "+device_id);
                break;
        }
    }
    public void addValue(String ip, long cur_t, float val_x, float val_y, float val_z){
        this.addValue(ip, cur_t, (double)val_x, (double)val_y, (double)val_z );
    }
    public void addValue(String ip, long cur_t, double val_x, double val_y, double val_z){
        if(pause)
            return;
//        Log.d("SelfSyncData|addValue", "IP: "+ip);
        if(ip.equals(HEAD_IP)){
            // HEAD
//            Log.d("SelfSyncData|addValue", "Head: "+ip);
            HEAD_DATA.get("GYRO_X").add(val_x);
            HEAD_DATA.get("GYRO_Y").add(val_y);
            HEAD_DATA.get("GYRO_Z").add(val_z);
            HEAD_TIME.add(cur_t);

            HEAD_dt = (int)(cur_t-HEAD_last_t);
            HEAD_last_t = cur_t;
            HEAD_added_t = System.currentTimeMillis();

            if (HEAD_TIME.size() >= this.size){
                HEAD_TIME.remove(0);
                for(int i=0; i<data_label.length; i++) {
                    HEAD_DATA.get(data_label[i]).remove(0);
                }
            }

        }else if(ip.equals(HAND_IP)){
            // HAND
//            Log.d("SelfSyncData|addValue", "Hand: "+ip);
            HAND_DATA.get("GYRO_X").add(val_x);
            HAND_DATA.get("GYRO_Y").add(val_y);
            HAND_DATA.get("GYRO_Z").add(val_z);
            HAND_TIME.add(cur_t);

            HAND_dt = (int)(cur_t-HAND_last_t);
            HAND_last_t = cur_t;
            HAND_added_t = System.currentTimeMillis();

            if (HAND_TIME.size() >= this.size){
                HAND_TIME.remove(0);
                for(int i=0; i<data_label.length; i++) {
                    HAND_DATA.get(data_label[i]).remove(0);
                }
            }


        }else if(ip.equals(LEG_IP)){
            //LEG
//            Log.d("SelfSyncData|addValue", "Leg: "+ip);
            LEG_DATA.get("GYRO_X").add(val_x);
            LEG_DATA.get("GYRO_Y").add(val_y);
            LEG_DATA.get("GYRO_Z").add(val_z);
            LEG_TIME.add(cur_t);

            LEG_dt = (int)(cur_t-LEG_last_t);
            LEG_last_t = cur_t;
            LEG_added_t = System.currentTimeMillis();

            if (LEG_TIME.size() >= this.size){
                LEG_TIME.remove(0);
                for(int i=0; i<data_label.length; i++) {
                    LEG_DATA.get(data_label[i]).remove(0);
                }
            }

        }else{
            Log.d(TAG, "no id found: "+ip);
        }
    }

    public String lastTs(){
        return TAG+"|Ts: Head:"+HEAD_last_t+" | Hand:"+HAND_last_t+" | Leg:"+LEG_last_t;
    }

    public String dTs(){
        return "selfsync.SelfSyncData|dt: "+HEAD_dt+" | "+HAND_dt+" | "+LEG_dt;
    }
    public String indexes(){
        return ""+HEAD_index+" | "+HAND_index+" | "+LEG_index ;
    }
    public boolean[] incoming(){
        boolean[] incoming_state = new boolean[3];

        long current_millis = System.currentTimeMillis();
        incoming_state[0] = (current_millis-HEAD_added_t)<1000;
        incoming_state[1] = (current_millis-HAND_added_t)<1000;
        incoming_state[2] = (current_millis-LEG_added_t)<1000;
//        Log.d(TAG, current_millis-HEAD_added_t +" | "+ (current_millis-HAND_added_t) + " | " + (current_millis-LEG_added_t));

        return incoming_state;

    }








}