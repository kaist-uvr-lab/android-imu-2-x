package kr.ac.kaist.arrc.imustreamwear;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.wear.widget.BoxInsetLayout;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;

import kr.ac.kaist.arrc.R;
import kr.ac.kaist.arrc.imustreamlib.CONSTANTS;
import kr.ac.kaist.arrc.imustreamlib.SocketComm;
import kr.ac.kaist.arrc.imustreamlib.ReturningValues;
import kr.ac.kaist.arrc.imustreamlib.VibratorTool;
import kr.ac.kaist.arrc.imustreamlib.network.NetUtils;
import kr.ac.kaist.arrc.imustreamlib.network.WifiReceiver;

import static kr.ac.kaist.arrc.imustreamlib.CONSTANTS.FORCE_WIFI;


public class MainActivity extends WearableActivity {


    private TextView tv_targetip, tv_deviceinfo;
    Button btn_startstop, btn_stopall, btn_write, btn_freqchange;

    private VibratorTool vib_tool;


    private static final String TAG = "WatchMainActivity";

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;
    WifiManager wifiManager;
    WifiManager.WifiLock wifiLock;

    //    private String NAME = "Moto360";
    private String NAME = "Urbane";
    //    private String NAME = "Sport";
//    private String NAME = "PinkA";
//    private String NAME = "PinkB";
    private Boolean msgSending = false;
    private Boolean msgWriting = false;


    TwoFingersDoubleTapDetector twoFingersListener;

    public static final String myPref = "IP_addr";

    AlertDialog ip_change_show;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_targetip = (TextView) findViewById(R.id.tv_target);
//        tv_targetip.setText(CONSTANTS.IP_ADDRESS+":"+CONSTANTS.PORT);
        tv_targetip.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // WiFi connection forcing part
                NetUtils.changeWifiToTargetNetwork(getApplicationContext());

                updateUI();


            }});

        tv_deviceinfo = (TextView) findViewById(R.id.tv_deviceinfo);
//        updateMyStatus();
//        tv_deviceinfo.setText(NAME+"\n"+Utils.getIPAddress(true));


        btn_startstop = (Button) findViewById(R.id.btn_startstop);
        btn_startstop.setOnClickListener(btn1ClickListener);
        btn_startstop.setOnLongClickListener(btn1LongClickListener);
        vib_tool = new VibratorTool((Vibrator) getSystemService(Context.VIBRATOR_SERVICE));

        btn_stopall = (Button) findViewById(R.id.btn_stopall);
        btn_stopall.setOnClickListener(btnStopClickListener);
        btn_stopall.setVisibility(View.GONE);

        btn_write = (Button) findViewById(R.id.btn_write);
        btn_write.setOnClickListener(btnWriteClickListener );
//        btn_write.setVisibility(View.GONE);

        btn_freqchange = (Button) findViewById(R.id.btn3);
        btn_freqchange.setOnClickListener(btnFreqClickListener);
        btn_freqchange.setVisibility(View.GONE);

        // Enables Always-on
        setAmbientEnabled();

        //wakelock for prevent shutdown
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "selfsync:WatchWakelock");
        wakeLock.acquire();

        //wakelock for prevent WiFi shutdown
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF , "selfsync:WatchWWiFilock");
        wifiLock.acquire();






        //region Exit by two fingers double tap
        twoFingersListener = new TwoFingersDoubleTapDetector() {
            @Override
            public void onTwoFingersDoubleTap() {
                Toast.makeText(getApplicationContext(), "2 FINGERS CLOSE", Toast.LENGTH_SHORT).show();
                finish();
            }
        };

        BoxInsetLayout mContainerView = findViewById(R.id.container);
        mContainerView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                twoFingersListener.onTouchEvent(event);
                return true;
            }
        });


        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_FINE_LOCATION
        };
        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }
        turnOffDozeMode(getApplicationContext());



        // Disable BT
        NetUtils.disableBluetooth();

        if (FORCE_WIFI) {
            // WiFi connection forcing part
            NetUtils.changeWifiToTargetNetwork(getApplicationContext());

            // Register Wifi manager
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
            registerReceiver(new WifiReceiver(), intentFilter);
        }


        readSettings();

        updateUI();



    }

    @Override
    protected void onResume() {
        super.onResume();
        readSettings();

        updateMyStatus();
        updateUI();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onPause() {
        super.onPause();
    }
    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");

        super.onStop();
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");


        wakeLock.release();
        unregisterReceiver(new WifiReceiver());

        super.onDestroy();
    }

    Button.OnClickListener btn1ClickListener = new Button.OnClickListener() {
        public void onClick(View arg0) {
            if(!msgSending){
                msgSending = true;
                vib_tool.vibrateError();

                startSendWriteService();
                updateUI();
            }else{
                Toast.makeText(getApplicationContext(), "LONG CLICK to STOP", Toast.LENGTH_SHORT).show();
            }

        }
    };
    // Long click to change IP address
    Button.OnLongClickListener btn1LongClickListener = new Button.OnLongClickListener() {
        public boolean onLongClick(View view) {

            if (!msgSending) {
                Toast.makeText(getApplicationContext(), "LONG CLICK", Toast.LENGTH_SHORT).show();

                // Creating alert Dialog with one Button
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

                //AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();

                // Setting Dialog Title
                alertDialog.setTitle("Change IP address");

                // Setting Dialog Message
//            alertDialog.setMessage("Enter Correct IP");

                final EditText input = new EditText(MainActivity.this);
                input.setHint("xxx.xxx.xxx.xxx");
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input); // uncomment this line

                // Setting Positive "Yes" Button
                alertDialog.setPositiveButton("Confirm change",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int which) {
                                // Write your code here to execute after dialog
                                String new_ip = input.getText().toString();

                                CONSTANTS.IP_ADDRESS = new_ip;
                                writeIPToPreference("modified_ip",new_ip);
                                updateUI();
                                Toast.makeText(getApplicationContext(),"YES:"+new_ip, Toast.LENGTH_SHORT).show();

                            }
                        });
                // Setting Negative "NO" Button
                alertDialog.setNegativeButton("CANCEL",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Write your code here to execute after dialog
                                Toast.makeText(getApplicationContext(),"NO", Toast.LENGTH_SHORT).show();
                                dialog.cancel();
                            }
                        });

                // closed

                // Showing Alert Message
                ip_change_show = alertDialog.show();

                SocketCommIP_get sock = new SocketCommIP_get();
                sock.execute();

            } else if (msgSending) {
                msgSending = false;
                msgWriting = false;

//                startSendWriteService(); // 서비스 종료
                stopSendWriteService();
                updateUI();
                Toast.makeText(getApplicationContext(), "Service Terminated", Toast.LENGTH_SHORT).show();

            }





            return true;
        }
    };

    Button.OnClickListener btnStopClickListener = new Button.OnClickListener() {
        public void onClick(View arg0) {
            msgSending = false;
            msgWriting = false;

            startSendWriteService(); // 서비스 종료
//            stopService(intent); // 서비스 종료
            updateUI();


        }
    };

    Button.OnClickListener btnWriteClickListener = new Button.OnClickListener() {
        public void onClick(View arg0) {
            if(!msgWriting){
                msgWriting = true;
            }
            startSendWriteService();
            updateUI();

            updateUI();

        }
    };
    Button.OnClickListener btnFreqClickListener = new Button.OnClickListener() {
        public void onClick(View arg0) {

            if(CONSTANTS.SENSOR_DELAY==8){
                // switch to 200
                CONSTANTS.setSensorDelay(4);
                btn_freqchange.setText("200Hz");

            }else if(CONSTANTS.SENSOR_DELAY==4){
                // switch to 100
                CONSTANTS.setSensorDelay(8);
                btn_freqchange.setText("100Hz");
            }
            updateUI();

        }
    };


    private void readSettings(){
        Log.d(TAG, "Service running: " + isServiceRunning());
        if(isServiceRunning()){
            ReturningValues values = SocketComm.readCurrentStatus();
            if(values.msgWriting||values.msgSending||!(values.NAME.equals("terminated"))){
                this.msgWriting = values.msgWriting;
                this.msgSending = values.msgSending;
                this.NAME = values.NAME;
            }

            startSendWriteService();
            updateUI();
        }

    }

    private void updateMyStatus(){
        // check stored IP and change automatically
        String stored_default_ip = getIPfromPreference("default_ip");
        String stored_modified_ip = getIPfromPreference("modified_ip");

        Log.d(TAG, "updateUI");
        Log.d(TAG, "updateMyStatus|Stored:"+stored_default_ip+", Modified:"+stored_modified_ip);

        if(stored_default_ip.equals("TheDefaultValueIfNoValueFoundOfThisKey")||!stored_default_ip.equals(CONSTANTS.IP_ADDRESS)){
            //nothing stored yet OR default ip changed
            writeIPToPreference("default_ip",CONSTANTS.IP_ADDRESS);
        }else if(stored_default_ip.equals(CONSTANTS.IP_ADDRESS)&&!stored_modified_ip.equals("TheDefaultValueIfNoValueFoundOfThisKey")){
            CONSTANTS.IP_ADDRESS = stored_modified_ip;
        }
        tv_targetip.setText(CONSTANTS.IP_ADDRESS+":"+CONSTANTS.PORT);

        NAME = NetUtils.getWifiName(getApplicationContext())+"\n"+NetUtils.getMACAddress("wlan0");
        tv_deviceinfo.setText(NAME+"\n"+NetUtils.getIPAddress(true));
    }

    private void updateUI(){
        updateMyStatus();

        if (msgSending) {
            btn_startstop.setTextColor(Color.rgb(50,200,50));
            btn_startstop.setText(R.string.network_on);
        }else{
            btn_startstop.setTextColor(Color.rgb(255,255,255));
            btn_startstop.setText(R.string.network_start);
        }


        if (msgWriting) {
            btn_write.setTextColor(Color.rgb(50,200,50));
            btn_write.setText(R.string.write_on);
        }else{
            btn_write.setTextColor(Color.rgb(255,255,255));
            btn_write.setText(R.string.write_start);
        }

        if(CONSTANTS.SENSOR_DELAY==4){
            btn_freqchange.setText("200Hz");

        }else if(CONSTANTS.SENSOR_DELAY==8){
            btn_freqchange.setText("100Hz");

        }else{
            btn_freqchange.setText(Float.toString(1000/(CONSTANTS.SENSOR_DELAY+1))+"Hz");
        }
    }

    private void startSendWriteService(){
        Intent intent = new Intent(
                getApplicationContext(),//현재제어권자
                SendWriteWear.class); // 이동할 컴포넌트
//        Intent intent = new Intent(
//                getApplicationContext(),//현재제어권자
//                SendWriteService.class); // 이동할 컴포넌트
        intent.putExtra("Send", msgSending);
        intent.putExtra("Write", msgWriting);
        intent.putExtra("Name", NAME);

        SocketComm.writeCurrentStatus(msgSending, msgWriting, NAME);

        startService(intent); // 서비스 시작

    }
    private void stopSendWriteService(){
        Intent intent = new Intent(
                getApplicationContext(),//현재제어권자
                SendWriteWear.class); // 이동할 컴포넌트

//        Intent intent = new Intent(
//                getApplicationContext(),//현재제어권자
//                SendWriteService.class); // 이동할 컴포넌트

        SocketComm.writeCurrentStatus(msgSending, msgWriting, NAME);

        stopService(intent); // 서비스 시작

    }

    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("edu.gatech.cc.ubicomp.synclib.SendWriteService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
    public String getIPfromPreference(String name)
    {
        SharedPreferences sp = getSharedPreferences(myPref,0);
        String str = sp.getString("modified_ip","TheDefaultValueIfNoValueFoundOfThisKey");
        return str;
    }

    public void writeIPToPreference(String name, String thePreference)
    {
        SharedPreferences.Editor editor = getSharedPreferences(myPref,0).edit();
        editor.putString(name, thePreference);
        editor.commit();
    }
    public void turnOffDozeMode(Context context){  //you can use with or without passing context
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            String packageName = context.getPackageName();
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
//            if (pm.isIgnoringBatteryOptimizations(packageName)){
//                // if you want to disable doze mode for this package
//                intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
//            }
            if(!pm.isIgnoringBatteryOptimizations(packageName)) { // if you want to enable doze mode
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                context.startActivity(intent);
            }

        }
    }

    private class SocketCommIP_get extends AsyncTask<Void, Void, Void> {

        private final String TAG = "SocketCommMsg_get";


        Boolean isSending = false;
        private Boolean isBackgroundRunning = false;
        int syncMode = 0;

        @Override
        protected Void doInBackground(Void... params) {
            Log.d(TAG, "doInBackground started");

            getIP();

            Log.d(TAG, "doInBackground stopped");
            return null;
        }

        public void getIP() {
            try {
                DatagramSocket serverSocket = new DatagramSocket(CONSTANTS.DATA_PORT);
                byte[] receiveData = new byte[CONSTANTS.DATA_BYTE_SIZE];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

                Log.d(TAG, "getFromServer|Listening on udp: " +
                        InetAddress.getLocalHost().getHostAddress() + ":" + CONSTANTS.DATA_PORT);


                serverSocket.receive(receivePacket);
                ByteBuffer buf = ByteBuffer.wrap(receivePacket.getData());
                float receivedMsg = buf.getFloat(0);

                if (receivedMsg == -904) {
                    String new_ip = receivePacket.getAddress().getHostAddress();
                    CONSTANTS.IP_ADDRESS = new_ip;
                    writeIPToPreference("modified_ip", new_ip);
                    ip_change_show.dismiss();
                    Toast.makeText(getApplicationContext(),"Target IP changed..."+new_ip+"-Close app and re run", Toast.LENGTH_SHORT).show();

                    updateUI();
                }
                Log.d(TAG, "getFromServer|receive packet " + buf.getLong(40) + ":" + buf.getFloat(0));

                serverSocket.close();




            } catch (Exception e) {
                Log.e(TAG, "getFromServer|loop IOException");
                Log.e(TAG, "getFromServer \n"+e.toString());
            }
        }

        public void disconnect() {
            this.isSending = false;

            Log.d(TAG, "getFromServer|disconnected ");


        }
    }


}
