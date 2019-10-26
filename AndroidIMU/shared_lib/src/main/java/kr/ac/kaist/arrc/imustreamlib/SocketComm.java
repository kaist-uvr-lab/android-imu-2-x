package kr.ac.kaist.arrc.imustreamlib;


import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;


/**
 * Created by arrc on 3/20/2018.
 */

public class SocketComm extends AsyncTask<Void, Void, Void> {

    private final String TAG = "SocketComm";

    private Boolean isGLASS = false;


    private Timer sending_timer;
    private int q_size = 0;
    private ByteBuffer sum_buffer;
    InetAddress IPAddress;
    DatagramSocket sending_socket;


    private ByteBuffer msgBuffer;
    private BlockingQueue bufferQueue;
    Boolean isSending = false;
    private Boolean isBackgroundRunning = false;
    int syncMode = 0;

    public ArrayList<String> headData_str = new ArrayList<String>();
    public ArrayList<String> handData_str = new ArrayList<String>();
    public ArrayList<String> legData_str = new ArrayList<String>();

    IMUData data_stream;

    //    String baseDir = "/mnt/sdcard/";
//    String baseDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SyncLimb/";

    String USER_NAME = "";
    String baseDir_str;
    File baseDir;
    FileOutputStream file_stream;
    DecimalFormat df = new DecimalFormat("0.00##");
    DecimalFormat no_df = new DecimalFormat("0");


    long writing_start_t = 0;
    private final int write_file_term = 1000 * 60 * 10;

    String DEVICE_INFO = Build.MANUFACTURER
            + " " + Build.MODEL + " " + Build.VERSION.RELEASE
            + " " + Build.VERSION_CODES.class.getFields()[android.os.Build.VERSION.SDK_INT].getName();





//    int write_file_term = 1000;


    public SocketComm() {
        super();

        // store float values as byte array
        msgBuffer = ByteBuffer.allocate(CONSTANTS.BYTE_SIZE);


    }

    public SocketComm(BlockingQueue q) {
        super();
        this.bufferQueue = q;
        this.isGLASS = Build.MODEL.contains("Glass");

        baseDir_str = Utils.getDirForDevice();
//        setDirForDevice();

    }

    public SocketComm(BlockingQueue q, Boolean isGLASS) {

        super();
        this.bufferQueue = q;
        this.isGLASS = isGLASS;

        baseDir_str = Utils.getDirForDevice();
//        setDirForDevice();

    }


    public void setIsSending(boolean isSending) {
        this.isSending = isSending;
    }

    public Boolean getSending() {
        return isSending;
    }


    @Override
    protected Void doInBackground(Void... params) {
        Log.d(TAG, "doInBackground started");
        if (syncMode == 0) {
            sendToServer();
        } else if (syncMode == 2) {
            getFromClient();
        }

        Log.d(TAG, "doInBackground stopped");
        return null;
    }

    public void sendToServer() {
        Log.d(TAG, "Build.MODEL: "+ Build.MODEL);

        try {
            sending_socket = new DatagramSocket(CONSTANTS.PORT);
            sending_socket.setReuseAddress(true);
            IPAddress = InetAddress.getByName(CONSTANTS.IP_ADDRESS);

            Log.d(TAG, "sendToServer|loop starting");

            long last_time = 0;

/*
            sending_timer = new Timer();
            sending_timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (isCancelled())
                        sending_timer.cancel();

                    try {
                        q_size = Math.min(bufferQueue.size(), 10);
                        if(q_size!=0){
                            sum_buffer = ByteBuffer.allocate(4+CONSTANTS.BYTE_SIZE * q_size);
                            sum_buffer.putInt(q_size);
                            for (int i = 0; i < q_size; i++) {
                                msgBuffer = (ByteBuffer) bufferQueue.take();
                                sum_buffer.put(msgBuffer);
                            }

                            // Only work when it is in Sending state
                            if (isSending) {
                                DatagramPacket pkt = new DatagramPacket(sum_buffer.array(),
                                        sum_buffer.capacity(),
                                        IPAddress, CONSTANTS.PORT);
                                sending_socket.send(pkt);
                            }

                        }



                    } catch (InterruptedException ex) {
                        Log.d(TAG, "sendToServer|Error on take");
                    }

                }
            }, 500, CONSTANTS.SENDING_INTERVAL);*/




            while (this.isSending || syncMode != 0) {
                if (isCancelled())
                    break;

                //String str = new String(msgBuffer.array());


                try {
                    q_size = Math.min(bufferQueue.size(), 10);
                    if(q_size==0)
                        continue;
                    sum_buffer = ByteBuffer.allocate(4+CONSTANTS.BYTE_SIZE * q_size);
                    sum_buffer.putInt(q_size);
                    for (int i = 0; i < q_size; i++) {
                        ByteBuffer tmp_buf = (ByteBuffer) bufferQueue.take();

                        sum_buffer.put(tmp_buf);
                    }

                    // Only work when it is in Sending state
                    if (this.isSending) {
                        DatagramPacket pkt = new DatagramPacket(sum_buffer.array(),
                                                                sum_buffer.capacity(),
                                                                IPAddress, CONSTANTS.PORT);
                        sending_socket.send(pkt);
                    }


//                    while (bufferQueue.size() > 0) {
//
//                        msgBuffer = (ByteBuffer) bufferQueue.take();
//
//                        // Pass if it is same value with previous input
//                        if (last_time == msgBuffer.getLong(40))
//                            continue;
//
//                        // Only work when it is in Sending state
//                        if (this.isSending) {
//                            DatagramPacket pkt = new DatagramPacket(msgBuffer.array(),
//                                    msgBuffer.capacity(),
//                                    IPAddress, PORT);
//                            socket.send(pkt);
//                        }
//
//                        //update last sending
//                        last_time = msgBuffer.getLong(40);
//                    }

                } catch (InterruptedException ex) {
                    Log.d(TAG, "sendToServer|Error on take");
                }
                Thread.sleep(CONSTANTS.SENDING_INTERVAL); // remove if it is too slow
            }
        } catch (Exception e) {
            Log.e(TAG, "sendToServer|Other Exception: " + e.toString());

            e.printStackTrace();
        }

        try{
            sending_socket.close();
            Log.d(TAG, "sendToServer|loop end");
        }catch (Exception e) {
            Log.d(TAG, "sendToServer|loop end with error");
            Log.e(TAG, "sendToServer|Other Exception: " + e.toString());
            e.printStackTrace();
        }


    }

    public void getFromClient() {
        try {
            DatagramSocket serverSocket = new DatagramSocket(CONSTANTS.PORT);
            byte[] receiveData = new byte[CONSTANTS.BYTE_SIZE*10+4];

            Log.d(TAG, "getFromClient|Listening on udp: " +
                    InetAddress.getLocalHost().getHostAddress() + ":" + CONSTANTS.PORT);
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

            data_stream = new IMUData(200);
            data_stream.setIPs(CONSTANTS.GLASS_IP, CONSTANTS.WEAR_IP, CONSTANTS.PHONE_IP);

            long this_last_time = 0;
            long this_time = 0;

            int this_device_id = 0;
            String this_IP = "";
            while (syncMode == 2) {

                try {
                    // only work when other socket comm works


                    while (bufferQueue.size() > 0) {
                        msgBuffer = ByteBuffer.allocate(CONSTANTS.BYTE_SIZE);
                        msgBuffer = (ByteBuffer) bufferQueue.take();
                        this_time = msgBuffer.getLong(40);
                        if (this_last_time != this_time) {
                            this_last_time = this_time;
                            data_stream.addValue(0, this_time,
                                    msgBuffer.getFloat(0), msgBuffer.getFloat(4), msgBuffer.getFloat(8));
                            addToStringArr(msgBuffer);
                        }

                    }

//                    msgBuffer = (ByteBuffer) bufferQueue.take();
//                    this_time = msgBuffer.getLong(40);
//                    if (this_last_time != this_time) {
//                        this_last_time = this_time;
//                        data_stream.addValue(0, this_time,
//                                msgBuffer.getFloat(0), msgBuffer.getFloat(4), msgBuffer.getFloat(8));
//                        addToStringArr(msgBuffer);
//                    }
                } catch (InterruptedException ex) {
                    Log.e(TAG, "getFromClient|Error on take");
                }


                serverSocket.receive(receivePacket);

                ByteBuffer buf = ByteBuffer.wrap(receivePacket.getData());

                int pre_fix = 0;
                int number_of_data = buf.getInt(0);
                for (int i = 0; i < number_of_data; i++) {
                    pre_fix = 4+i*CONSTANTS.BYTE_SIZE;

                    this_device_id = buf.getInt(pre_fix+48);

                    data_stream.addValue(buf.getInt(pre_fix+48), buf.getLong(pre_fix+40),
                            buf.getFloat(pre_fix+0), buf.getFloat(pre_fix+4), buf.getFloat(pre_fix+8));

                    addToStringArr(buf.getInt(pre_fix+48), buf.getLong(pre_fix+40),
                            buf.getFloat(pre_fix+0), buf.getFloat(pre_fix+4), buf.getFloat(pre_fix+8));
                }

                this_IP = receivePacket.getAddress().getHostAddress();
                // Update IP address
                if ((!this_IP.equals(CONSTANTS.GLASS_IP))
                        && (!this_IP.equals(CONSTANTS.WEAR_IP))
                        && (!this_IP.equals(CONSTANTS.PHONE_IP))) {

                    switch (this_device_id) {
                        case 0:
                            CONSTANTS.PHONE_IP = this_IP;
                            break;
                        case 1:
                            CONSTANTS.WEAR_IP = this_IP;
                            break;
                        case 2:
                            CONSTANTS.GLASS_IP = this_IP;
                            break;
                    }
                    Log.d(TAG, this_IP+
                            " reset IP: "+CONSTANTS.GLASS_IP +", "+CONSTANTS.WEAR_IP +", "+CONSTANTS.PHONE_IP);
                }

//                Log.d(TAG, "getFromServer|RECEIVED: "+ receivePacket.getAddress().getHostAddress());
//                Log.d(TAG, "getFromServer|last_ts: " + data_stream.lastTs());
//                Log.d(TAG,  data_stream.dTs());

                // now send acknowledgement packet back to sender
//                InetAddress IPAddress = receivePacket.getAddress();
//                String sendString = "polo";
//                byte[] sendData = sendString.getBytes("UTF-8");
//                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
//                        IPAddress, receivePacket.getPort());
//                serverSocket.send(sendPacket);
            }
            serverSocket.close();
            Log.d(TAG, "getFromClient|loop stopped");
        } catch (IOException e) {
            Log.e(TAG, "getFromClient|loop IOException");
            Log.e(TAG, e.toString());
        }
        Log.d(TAG, "getFromClient|loop ended");


    }

    public void disconnect() {
        this.isSending = false;

        Log.d(TAG, "getFromServer|disconnected ");


    }

    public static void writeCurrentStatus(boolean msgSending, boolean msgWriting, String NAME) {
        String baseDir_str;
        if (Build.MODEL.contains("Glass")) {
            Log.d("writeCurrentStatus", "this is google glass");
            baseDir_str = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SyncLimb/";
        } else {
            baseDir_str = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SyncLimb/";
        }

        File dir = new File(baseDir_str);
        if (!dir.exists()) {
            dir.mkdirs();
            Log.d("writeCurrentStatus", "Folder created: " + baseDir_str);
        }

        File file = new File(baseDir_str + "current_settings.txt");
        try {
            FileWriter f2 = new FileWriter(file, false);
            f2.write("" + msgSending + "," + msgWriting + "," + NAME);
            Log.d("writeCurrentStatus", "Current Status saved: " + "" + msgSending + "," + msgWriting + "," + NAME);
            f2.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static ReturningValues readCurrentStatus() {
        ReturningValues return_values = new ReturningValues();
        String baseDir_str;
        if (Build.MODEL.contains("Glass")) {
            Log.d("readCurrentStatus", "this is google glass");
            baseDir_str = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/SyncLimb/";
        } else {
            baseDir_str = Environment.getExternalStorageDirectory().getAbsolutePath() + "/SyncLimb/";
        }

        // check if File exists or not
        FileReader fr = null;
        try {
            fr = new FileReader(baseDir_str + "current_settings.txt");
        } catch (FileNotFoundException fe) {
            System.out.println("File not found");
            return return_values;
        }


        try {

            BufferedReader bufferreader = new BufferedReader(fr);
            // read from FileReader till the end of file
            String status = bufferreader.readLine();
            Log.d("readCurrentStatus", status);

            String[] split_result = status.split(",");

            return_values = new ReturningValues(Boolean.valueOf(split_result[0]),
                    Boolean.valueOf(split_result[1]),
                    split_result[2]);

            // close the file
            fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return return_values;

    }

    private void addToStringArr(int device_id, long device_time, float x, float y, float z){
        String str_ = System.currentTimeMillis() +","+ device_id +","+ df.format(device_time)+", "+
                df.format( x)+", "+df.format( y)+", "+df.format(z);
        switch (device_id) {
            case 0:
                legData_str.add(str_);
                break;
            case 1:
                handData_str.add(str_);
                break;
            case 2:
                headData_str.add(str_);
                break;
        }
    }
    private void addToStringArr(byte[] b_array){
        ByteBuffer buf = ByteBuffer.wrap(b_array);
        addToStringArr(buf);
    }
    private void addToStringArr(ByteBuffer buf){
        int device_id = buf.getInt(48);
//        Log.d(TAG, "leg:" + legData_str.size() + " hand:" + handData_str.size() + " head:" + headData_str.size());
        switch (device_id) {
            case 0:
                legData_str.add(convertToString(buf));
                break;
            case 1:
                handData_str.add(convertToString(buf));
                break;
            case 2:
                headData_str.add(convertToString(buf));
                break;
        }

    }

    private String convertToString(ByteBuffer buf){
        String str_ = System.currentTimeMillis() +","+buf.getInt(48) +","+ df.format(buf.getLong(40))+", "+df.format( buf.getFloat(0))+", "+df.format( buf.getFloat(4))+", "+df.format( buf.getFloat(8));
        return str_;
    }
}