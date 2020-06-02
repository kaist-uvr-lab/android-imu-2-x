package kr.ac.kaist.arrc.imustreamlib;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


import kr.ac.kaist.arrc.imustreamlib.network.NetUtils;

public class SendWriteService extends Service implements SensorEventListener {
    final String TAG = "SendWriteService";

    public final int FOREFROUND_ID = 011;
    public final String NOTI_CHANNEL_ID = "SendWriteService";

    private int this_device_id = 0;

    private SensorManager sensorManager;
    private long sensorTimeReference = 0l;
    private long myTimeReference = 0l;
    private long lastSensorTime = 0l;


    private BlockingQueue bufferQueue;
    private ByteBuffer msgBuffer;
    private Boolean msgSending = false;
    private Boolean msgWriting = false;
    private Boolean Classifying = false;
    private int syncInfo = 0;

    private SocketComm sendToServer;
    private SocketCommMsg_get msg_socket_get;

    private float prev_x, prev_y, prev_z;
    private long time;

    private float ACC_X, ACC_Y, ACC_Z;
    private float GYRO_X, GYRO_Y, GYRO_Z;
    private float ROT_X, ROT_Y, ROT_Z, ROT_W;
    private float MAG_X, MAG_Y, MAG_Z;
    private long TIME;


    private String acc_str = "0, 0, 0,";
    private String rot_str = "0, 0, 0, 0,";

    public static final String
            ACTION_CLASS = SendWriteService.class.getName()+"ResultBroadcast",
            LAST_CLASSRESULT = "last_result",
            CONNECTION_INFO = "connection_info";
    private String CONNECTION_STR = "";


    private int LAST_RESULT = 5;
    private long LAST_RESULT_TIME;

    private int[] results = new int[5];


    IBinder mBinder = new MyBinder();

    private Timer write_data = new Timer();
    private ArrayList<String> gyroData = new ArrayList<String>();
    DecimalFormat df = new DecimalFormat("0.00##");
    int WRITING_TERM = 10*60*1000;
//    int WRITING_TERM = 10000;
    private SaveGyroDataTask saveGyrodata;


    private Timer write_devices_data = new Timer();
    private SaveDevicesDataTask saveDevicesdata;
    ArrayList<String> class_result_str = new ArrayList<String>();



    private Timer video_timer = new Timer();
    int VIDEO_TERM = 30*60*1000;
//    int VIDEO_TERM = 10*1000;



    public class MyBinder extends Binder {
        public SendWriteService getService() { // 서비스 객체를 리턴
            return SendWriteService.this;
        }
    }


    public SendWriteService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");

        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");


        VibratorTool.setVibrator((Vibrator) getSystemService(Context.VIBRATOR_SERVICE));
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);


        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                CONSTANTS.SENSOR_DELAY);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                CONSTANTS.SENSOR_DELAY);
//        sensorManager.registerListener(this,
//                sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
//                CONSTANTS.SENSOR_DELAY);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                CONSTANTS.SENSOR_DELAY);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                CONSTANTS.SENSOR_DELAY);
//        sensorManager.registerListener(this,
//                sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR),
//                CONSTANTS.SENSOR_DELAY);
        // store float values as byte array
        msgBuffer = ByteBuffer.allocate(CONSTANTS.BYTE_SIZE);
        bufferQueue = new ArrayBlockingQueue(CONSTANTS.QUEUE_SIZE);

        sendToServer = new SocketComm(bufferQueue);
        msg_socket_get = new SocketCommMsg_get();


        if(Build.MODEL.contains( "Glass")){
            this_device_id = 2;
            video_timer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    // activate repeatable video recording for data save mode
                    if(msgWriting && !msgSending){
                        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
//                    intent.putExtra("android.intent.extra.durationLimit", 10000);
                        intent.putExtra(android.provider.MediaStore.EXTRA_SIZE_LIMIT, 5242880);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("EXTRA_VIDEO_QUALITY", 1);
                        startActivity(intent);
                    }


                }
            }, 10000, VIDEO_TERM);
            Log.d(TAG, "Device type: 2(glass)");
        } else if (Build.MODEL.contains("Watch") || Build.MODEL.contains("Q Explorist HR") || Build.MODEL.contains("Q Venture HR") ) {
            this_device_id = 1;
            Log.d(TAG, "Device type: 1(watch)");
        } else{
            this_device_id = 0;
            Log.d(TAG, "Device type: default |"+Build.MODEL);
        }



        write_data.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if(msgWriting){
                    saveGyrodata = new SaveGyroDataTask();
                    saveGyrodata.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                }
            }
        }, WRITING_TERM, WRITING_TERM);

        write_devices_data.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                Log.d(TAG, "write_devices_data");
                saveDevicesdata = new SaveDevicesDataTask();
                saveDevicesdata.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

            }
        }, WRITING_TERM, WRITING_TERM);


        sendBroadcastMessage();

        Intent notificationIntent = new Intent(this, SendWriteService.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);



    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        msgSending = intent.getBooleanExtra("Send", false);


        Boolean new_msgWriting = intent.getBooleanExtra("Write", false);
        if(msgWriting && !new_msgWriting){
            saveGyrodata = new SaveGyroDataTask();
            saveGyrodata.execute();
        }
        msgWriting = new_msgWriting;
//        String name = intent.getStringExtra("Name");

        Classifying = intent.getBooleanExtra("Classify", false);

        syncInfo = intent.getIntExtra("Sync", 0);

        int send_code =  intent.getIntExtra("ToClient", -1);


        Log.d(TAG, "onStartCommand status:"+msgSending+"|"+msgWriting);

        startSocketComm();

        // get feedback from phone [[only in glass]]

        if(this_device_id==2){
            Log.d(TAG, "getFromServer "+msg_socket_get.getStatus()+"|"+msgSending +"|"+syncInfo);
            if(msg_socket_get.getStatus() == AsyncTask.Status.RUNNING){
                if ((!msgSending) && (syncInfo==0)) {
                    msg_socket_get.isSending = msgSending;
                    msg_socket_get.cancel(true);

                }else{
                    msg_socket_get.isSending = msgSending;
                    msg_socket_get.syncMode = syncInfo;
                }
            }else{
                msg_socket_get = new SocketCommMsg_get();

                msg_socket_get.isSending = msgSending;
                msg_socket_get.syncMode = syncInfo;
                msg_socket_get.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                Log.d(TAG, "getFromServer executed");
            }
        }


//        return super.onStartCommand(intent, flags, startId);
        Log.d(TAG, "onStartCommand end");
        return Service.START_REDELIVER_INTENT;
    }
    private void startSocketComm() {this.startSocketComm(false);}
    private void startSocketComm(boolean force_restart){

        if (force_restart) {
            sendToServer.disconnect();
            sendToServer.cancel(true);
            Log.d("startSocketComm", "force_restart");
        }

        if((sendToServer.getStatus() == AsyncTask.Status.RUNNING) && !force_restart){
            // Already sending
            if ( ((!msgSending) && (syncInfo==0)) ) {
                // saving mode
                sendToServer.isSending = msgSending;
                sendToServer.syncMode = syncInfo;
                sendToServer.cancel(true);

                saveDevicesdata = new SaveDevicesDataTask();
                saveDevicesdata.execute();

                Log.d("startSocketComm", "sendToServer terminated");

            }else{
                // state change
                sendToServer.syncMode = syncInfo;
                sendToServer.setIsSending(msgSending);
                Log.d("startSocketComm", "b");
            }
        }else{
            // start sending
            Log.d("startSocketComm", "Start");
            sendToServer = new SocketComm(bufferQueue);

            sendToServer.syncMode = syncInfo;
            sendToServer.setIsSending(msgSending);
            sendToServer.execute();
        }

    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        video_timer.cancel();
        video_timer.purge();

        write_devices_data.cancel();
        write_devices_data.purge();

        saveDevicesdata = new SaveDevicesDataTask();
        saveDevicesdata.execute();

        sendToServer.disconnect();
        sendToServer.cancel(true);

        msg_socket_get.disconnect();
        msg_socket_get.cancel(true);


        // 서비스가 종료될 때 실행



        Log.d(TAG, "onDestroy");
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        // set reference times
        if(sensorTimeReference == 0l && myTimeReference == 0l) {
            sensorTimeReference = event.timestamp;
            myTimeReference = System.currentTimeMillis();
        }
        // set event timestamp to current time in milliseconds
        event.timestamp = myTimeReference +
                Math.round((event.timestamp - sensorTimeReference) / 1000000.0);

        if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            GYRO_X = event.values[0];
            GYRO_Y = event.values[1];
            GYRO_Z = event.values[2];
            TIME = event.timestamp;
        }else if(event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION
                || event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            ACC_X = event.values[0];
            ACC_Y = event.values[1];
            ACC_Z = event.values[2];
        }else if(event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR
                || event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR){
            ROT_X = event.values[0];
            ROT_Y = event.values[1];
            ROT_Z = event.values[2];
            ROT_W = event.values[3];
        }else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD
                || event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED){
            MAG_X = event.values[0];
            MAG_Y = event.values[1];
            MAG_Z = event.values[2];
        }


        if( (msgSending||msgWriting||(syncInfo==2))
                && ( (event.timestamp - lastSensorTime) > CONSTANTS.SENSOR_DELAY)) {

            lastSensorTime = event.timestamp;

            // Sending UDP
            if(msgSending||(syncInfo==2)) {
                msgBuffer = ByteBuffer.allocate(CONSTANTS.BYTE_SIZE);

                msgBuffer.putFloat(0, GYRO_X);
                msgBuffer.putFloat(4, GYRO_Y);
                msgBuffer.putFloat(8, GYRO_Z);

                msgBuffer.putFloat(12, ACC_X);
                msgBuffer.putFloat(16, ACC_Y);
                msgBuffer.putFloat(20, ACC_Z);

                msgBuffer.putFloat(24, ROT_X);
                msgBuffer.putFloat(28, ROT_Y);
                msgBuffer.putFloat(32, ROT_Z);
                msgBuffer.putFloat(36, ROT_W);

                // set Time to use sending time as reference
                // TODO: need to include reference time of three sensors
                msgBuffer.putLong(40, lastSensorTime);
                msgBuffer.putInt(48, this_device_id);

                msgBuffer.putFloat(52, MAG_X);
                msgBuffer.putFloat(56, MAG_Y);
                msgBuffer.putFloat(60, MAG_Z);
                try {
                    if (bufferQueue.remainingCapacity() < 1) {
                        bufferQueue.take();
                    }
                    bufferQueue.put(msgBuffer);
                } catch (InterruptedException ex) {
                    Log.d(TAG, "Error on put sensor values");
                }

//                String testing = df.format(GYRO_X)+", "+df.format(GYRO_Y)+", "+df.format(GYRO_Z)+", "
//                        + df.format(ACC_X)+", "+df.format(ACC_Y)+", "+df.format(ACC_Z)+", "
//                        + df.format(ROT_X)+", "+df.format(ROT_Y)+", "+df.format(ROT_Z)+", "+df.format(ROT_W)+", "
//                        + TIME + "," +
//                        this_device_id + "," +
//                        df.format(MAG_X)+", "+df.format(MAG_Y)+", "+df.format(MAG_Z);
//                Log.d("testing", testing);
            }


            // WRITING
            if(msgWriting){
                gyroData.add(   df.format(GYRO_X)+", "+df.format(GYRO_Y)+", "+df.format(GYRO_Z)+", "
                        + df.format(ACC_X)+", "+df.format(ACC_Y)+", "+df.format(ACC_Z)+", "
                        + df.format(ROT_X)+", "+df.format(ROT_Y)+", "+df.format(ROT_Z)+", "+df.format(ROT_W)+", "
                        + TIME + "," +
                        this_device_id + "," +
                        df.format(MAG_X)+", "+df.format(MAG_Y)+", "+df.format(MAG_Z));

            }
        }
    }

    private void putToBufferQueue(){

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void sendBroadcastMessage() {
        Intent intent = new Intent(ACTION_CLASS);
        intent.putExtra(LAST_CLASSRESULT, LAST_RESULT);
        intent.putExtra(CONNECTION_INFO, CONNECTION_STR);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

    }




    private class SaveGyroDataTask extends AsyncTask<Void, Void, Boolean> {

        protected Boolean doInBackground(Void... voids) {


            File dir = new File(Utils.getDirForDevice());
            if (!dir.exists()) {dir.mkdirs();}

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
            String currentDateTime = sdf.format(new Date());

            String file_name = Utils.getDirForDevice() + currentDateTime + "_" + NetUtils.getMACAddress("wlan0") + "_" + Utils.getDeviceInfo() + ".csv";

            ArrayList<String> corrDataCopy = new ArrayList<String>(gyroData);
            gyroData.clear();
            Utils.writeDataCSV(file_name, corrDataCopy, "GyroX, GyroY, GyroZ, AccX, AccY, AccZ, RotX, RotY, RotZ, RotW, Time");

            return true;
        }

        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "completed save correlation data " + result);
        }
    }

    private class SaveDevicesDataTask extends AsyncTask<Void, Void, Boolean> {

        protected Boolean doInBackground(Void... voids) {


            File dir = new File(Utils.getDirForDevice());
            if (!dir.exists()) {dir.mkdirs();}

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
            String currentDateTime = sdf.format(new Date());


            String common_part = Utils.getDirForDevice() + currentDateTime;
            String common_part2 = NetUtils.getMACAddress("wlan0") + "_" + Utils.getDeviceInfo() + ".csv";

            String file_name =  common_part + "_HEAD_"+ common_part2;
            ArrayList<String> corrDataCopy = new ArrayList<String>(sendToServer.headData_str);
            sendToServer.headData_str.clear();
            Utils.writeDataCSV(file_name, corrDataCopy, "ServerTime, DeviceID, Time, GyroX, GyroY, GyroZ");

            file_name = common_part + "_HAND_"+ common_part2;
            corrDataCopy = new ArrayList<String>(sendToServer.handData_str);
            sendToServer.handData_str.clear();
            Utils.writeDataCSV(file_name, corrDataCopy, "ServerTime, DeviceID, Time, GyroX, GyroY, GyroZ");

            file_name = common_part + "_LEG_"+ common_part2;
            corrDataCopy = new ArrayList<String>(sendToServer.legData_str);
            sendToServer.legData_str.clear();
            Utils.writeDataCSV(file_name, corrDataCopy, "ServerTime, DeviceID, Time, GyroX, GyroY, GyroZ");

            Log.d(TAG, "SaveDevicesDataTask leg:" + sendToServer.legData_str.size() + " hand:" + sendToServer.handData_str.size() + " head:" + sendToServer.headData_str.size());

            file_name = common_part + "_CLASS_RESULT_"+ common_part2;
            corrDataCopy = new ArrayList<String>(class_result_str);
            class_result_str.clear();
            Utils.writeDataCSV(file_name, corrDataCopy, "Time, result, result_str, dis0, dis1, dis2, dis3, dis4, dis5");

            return true;
        }

        protected void onPostExecute(Boolean result) {
            Log.d(TAG, "completed save correlation data " + result);
        }
    }

    private class SocketCommMsg_get extends AsyncTask<Void, Void, Void> {

        private final String TAG = "SocketCommMsg_get";


        Boolean isSending = false;
        private Boolean isBackgroundRunning = false;
        int syncMode = 0;

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "doInBackground started");

            if( syncMode == 0){
                getFromServer();
            }

            Log.d(TAG, "doInBackground stopped");
            return null;
        }

        public void getFromServer(){
            try {
                DatagramSocket serverSocket = new DatagramSocket(CONSTANTS.DATA_PORT);
                byte[] receiveData = new byte[CONSTANTS.DATA_BYTE_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.setSoTimeout(10000);

                Log.d(TAG, "getFromServer|Listening on udp: " +
                        InetAddress.getLocalHost().getHostAddress() + ":" + CONSTANTS.DATA_PORT);

                while (isSending || syncMode != 0) {
                    if(isCancelled())
                        break;

                    try {
                        serverSocket.receive(receivePacket);
                        ByteBuffer buf = ByteBuffer.wrap(receivePacket.getData());
                        float receivedMsg = buf.getFloat(0);

                        if (receivedMsg == 2 || receivedMsg == 4 ||
                                receivedMsg == 100 || receivedMsg == 101 || receivedMsg == 102 || receivedMsg == 103 || receivedMsg == 104) {
                            AudioFeedbackTool.playNoti(getApplicationContext(), (int) receivedMsg);
                        } else if (receivedMsg == 5) {
                            AudioFeedbackTool.playError(getApplicationContext());
                        } else if (receivedMsg == -904) {
                            SharedPreferences.Editor editor = getSharedPreferences("IP_addr", 0).edit();
                            editor.putString("modified_ip", receivePacket.getAddress().getHostAddress());
                            editor.commit();

                            CONSTANTS.IP_ADDRESS = receivePacket.getAddress().getHostAddress();
                            AudioFeedbackTool.playSuccess(getApplicationContext());

                            startSocketComm(true);
                        }
                        Log.d(TAG, "getFromServer|receive packet " + buf.getLong(40) + ":" + buf.getFloat(0));


                    } catch (Exception e) {
                        Log.e(TAG, "getFromServer|inner loop Exception");
                        Log.e(TAG, "getFromServer \n"+e.toString());
                    }
                }
                serverSocket.close();


            } catch (Exception e) {
                Log.e(TAG, "getFromServer|loop IOException");
                Log.e(TAG, "getFromServer \n"+e.toString());
            }

            Log.d(TAG, "getFromServer|loop stopped ");
        }

        public void disconnect() {
            this.isSending = false;

            Log.d(TAG, "getFromServer|disconnected ");


        }

    }



}



