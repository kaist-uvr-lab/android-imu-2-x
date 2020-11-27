package kr.ac.kaist.arrc.imustreamlib;


import android.os.AsyncTask;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import kr.ac.kaist.arrc.imustreamlib.IMUData;

/**
 * Created by arrc on 3/20/2018.
 */

public class SocketOnetimeSend extends AsyncTask<Object, Void, Void> {

    private final String TAG = "SocketOnetimeSend";


    Boolean isSending = false;


    @Override
    protected Void doInBackground(Object... params) {
        Log.d(TAG, "doInBackground started");
        String target_ip = (String) params[0];
        int target_gesture_id = (int) params[1];

        sendToClient(target_ip, target_gesture_id);

        Log.d(TAG, "doInBackground stopped");
        return null;
    }

    public void sendToClient(String IP, float msg_code){
        DatagramSocket socket;
        Log.d(TAG, "sendToClient|start");


        try {
            ByteBuffer buf_sending = ByteBuffer.allocate(CONSTANTS.DATA_BYTE_SIZE);

            socket = new DatagramSocket(CONSTANTS.DATA_PORT);
            socket.setReuseAddress(true);


            buf_sending.putFloat(0, msg_code);
            buf_sending.putLong(40, System.currentTimeMillis());


            InetAddress IPAddress = InetAddress.getByName(IP);
            // Only work when it is in Sending state
            DatagramPacket pkt = new DatagramPacket(buf_sending.array(),
                    buf_sending.capacity(),
                    IPAddress, CONSTANTS.DATA_PORT);
            socket.send(pkt);


            socket.close();
            Log.d(TAG, "sendToClient|sent: "+IP+"| "+msg_code);
        } catch (Exception e) {
            Log.e(TAG, "sendToClient|Other Exception: " + e.toString());

            e.printStackTrace();
        }

    }


    public void disconnect() {
        this.isSending = false;

        Log.d(TAG, "getFromServer|disconnected ");


    }

}