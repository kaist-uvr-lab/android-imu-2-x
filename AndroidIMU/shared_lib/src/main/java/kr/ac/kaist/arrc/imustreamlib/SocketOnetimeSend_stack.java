package kr.ac.kaist.arrc.imustreamlib;


import android.os.AsyncTask;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


/**
 * Created by arrc on 3/20/2018.
 */

class packetMsg {
    String IP;
    float msg_code;

    packetMsg(String target_ip, float msg_code) {
        this.IP = target_ip;
        this.msg_code = msg_code;
    }

}

public class SocketOnetimeSend_stack extends AsyncTask<Object, Void, Void> {

    private final String TAG = "SocketOnetimeSend_stack";


    Boolean isSending = false;
    private BlockingQueue<packetMsg> send_stock;



    @Override
    protected Void doInBackground(Object... params) {
        Log.d(TAG, "doInBackground started");

        send_stock = new ArrayBlockingQueue<packetMsg>(50);

        sendingLoop();

        Log.d(TAG, "doInBackground stopped");
        return null;
    }

    public void sendThis(String IP, float msg_code){
        try {
            if(send_stock.remainingCapacity() < 1){
                send_stock.take();
            }
            send_stock.put(new packetMsg(IP, msg_code));
        } catch (InterruptedException ex) {
            Log.d(TAG, "Error on put msg on stock");
        }
    }

    public void sendingLoop(){
        DatagramSocket socket;
        Log.d(TAG, "sendToClient|start");


        isSending = true;
        while (isSending) {
            try {

                if (send_stock.size() <= 0) {
                    Thread.sleep(100);
                    continue;
                }


                packetMsg this_msg = send_stock.take();

                ByteBuffer buf_sending = ByteBuffer.allocate(CONSTANTS.DATA_BYTE_SIZE);

                socket = new DatagramSocket(CONSTANTS.DATA_PORT);
                socket.setReuseAddress(true);


                buf_sending.putFloat(0, this_msg.msg_code);
                buf_sending.putLong(40, System.currentTimeMillis());


                InetAddress IPAddress = InetAddress.getByName(this_msg.IP);
                // Only work when it is in Sending state
                DatagramPacket pkt = new DatagramPacket(buf_sending.array(),
                        buf_sending.capacity(),
                        IPAddress, CONSTANTS.DATA_PORT);
                socket.send(pkt);


                socket.close();
                Log.d(TAG, "sendToClient|sent: "+this_msg.IP+"| "+this_msg.msg_code);
            } catch (Exception e) {
                Log.e(TAG, "sendToClient|Other Exception: " + e.toString());

                e.printStackTrace();
            }
        }


    }


    public void disconnect() {
        this.isSending = false;

        Log.d(TAG, "getFromServer|disconnected ");


    }

}