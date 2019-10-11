package kr.ac.kaist.arrc.imustreamphone;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.SensorManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.text.SimpleDateFormat;


import kr.ac.kaist.arrc.R;
import kr.ac.kaist.arrc.imustreamlib.CONSTANTS;
import kr.ac.kaist.arrc.imustreamlib.SocketComm;
import kr.ac.kaist.arrc.imustreamlib.ReturningValues;
import kr.ac.kaist.arrc.imustreamlib.SendWriteService;
import kr.ac.kaist.arrc.imustreamlib.SocketOnetimeSend;
import kr.ac.kaist.arrc.imustreamlib.Utils;
import kr.ac.kaist.arrc.imustreamlib.VibratorTool;
import kr.ac.kaist.arrc.imustreamlib.network.NetUtils;

import static kr.ac.kaist.arrc.imustreamlib.CONSTANTS.ERROR_NOTI_TIME;
import static kr.ac.kaist.arrc.imustreamlib.SendWriteService.CONNECTION_INFO;
import static kr.ac.kaist.arrc.imustreamlib.SendWriteService.LAST_CLASSRESULT;


//public class MainActivity extends AppCompatActivity implements SensorEventListener {
public class MainActivity extends AppCompatActivity  {

    private boolean EXP_TESTING = false;
    private SensorManager sensorManager;
    private TextView tv1, tv2, device_info;
    Button btn_sync;
    private Menu main_menu;

    private String device_info_str = "";
    private long last_error_time = 0;
    private boolean disconnected = false;

    private VibratorTool vib_tool;

    private String NAME = "jyS6";

    private static final String TAG = "PhoneMainActivity";
    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;
    WifiManager wifiManager;
    WifiManager.WifiLock wifiLock;

    private BlockingQueue bufferQueue;
    ByteBuffer msgBuffer;
    private Boolean msgSending = false;
    private Boolean msgWriting = false;
    private Boolean classifying = false;
    private int syncInfo = 0; //0:nothing, 1:sending, 2:receiving


    public static final String myPref = "IP_addr";

    private boolean isBind;


    android.app.AlertDialog ip_change_show;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);



        tv1 = (TextView) findViewById(R.id.tv1);
        tv1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                // WiFi connection forcing part
//                NetUtils.changeWifiToTargetNetwork(getApplicationContext());

                updateUI();


            }});
        tv2 = (TextView) findViewById(R.id.tv2);


        btn_sync = (Button) findViewById(R.id.syncServer_btn);
//        btn_sync.setOnClickListener(syncbtnClickListener);
        btn_sync.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (syncInfo == 0) {
                    syncInfo = 2;
                    classifying = true;

                    last_error_time = System.currentTimeMillis();
                    disconnected = false;
                } else {
                    Toast.makeText(getApplicationContext(), "Long click to stop!", Toast.LENGTH_SHORT).show();
                }

                startService();
                updateUI();
            }});
        btn_sync.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean  onLongClick(View v) {
                if (syncInfo == 2) {

                    syncInfo = 0;
                    classifying = false;
                }

                startService();
                updateUI();

                return true;
            }});


        // need initialization before calling
        vib_tool = new VibratorTool((Vibrator) getSystemService(Context.VIBRATOR_SERVICE));


        // store float values as byte array
        msgBuffer = ByteBuffer.allocate(CONSTANTS.BYTE_SIZE);
        bufferQueue = new ArrayBlockingQueue(CONSTANTS.QUEUE_SIZE);

        //wakelock for prevent shutdown
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "selfsync:PhoneWakelock");
        wakeLock.acquire();

        //wakelock for prevent WiFi shutdown
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF , "selfsync:PhoneWWiFilock");
        wifiLock.acquire();

        device_info = (TextView) findViewById(R.id.device_info_txt);



        // Get data from SendWriteService
        LocalBroadcastManager.getInstance(this).registerReceiver(
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        int result = intent.getIntExtra(LAST_CLASSRESULT, -1);
                        device_info_str = intent.getStringExtra(CONNECTION_INFO);

                        // vibration noti for connection errors
                        if(syncInfo==2 && device_info_str.contains("error")){
                            if (disconnected) {
                                // noti vibrate when error last for certain seconds(CONSTANTS.ERROR_NOTI_TIME)
                                if (System.currentTimeMillis() - last_error_time > ERROR_NOTI_TIME) {
                                    if(!EXP_TESTING)    VibratorTool.vibrateError();

                                    // set last error time to make noti repeat every 3sec
                                    last_error_time = (System.currentTimeMillis() - ERROR_NOTI_TIME) + CONSTANTS.ERROR_NOTI_REPEAT;
                                }

                            }else{
                                disconnected = true;
                                last_error_time = System.currentTimeMillis();
                            }
                        }
                        else{
                            // connection recovered
                            disconnected = false;

                            //reset last_error_time when connection recovered
                            last_error_time = System.currentTimeMillis();
                        }
                        updateUI();

                    }
                }, new IntentFilter(SendWriteService.ACTION_CLASS)
        );


        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.ACCESS_FINE_LOCATION
        };
        if(!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }


        // Disable BT
        NetUtils.disableBluetooth();

        readSettings();

        updateUI();

        turnOffDozeMode(getApplicationContext());
/*

        // WiFi connection forcing part
        NetUtils.changeWifiToTargetNetwork(getApplicationContext());
        // Register Wifi manager
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new WifiReceiver(), intentFilter);
*/







//        testFeatureGenerating();



    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_menu, menu);
        main_menu = menu;
        updateUI();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id){

            case R.id.connect_opt:
                if(!msgSending){
                    msgSending = true;
                }
                startService();
                updateUI();

                Toast.makeText(getApplicationContext(),"Now Sending",Toast.LENGTH_LONG).show();
                return true;
            case R.id.changeip_opt:
                showAlertDialogforIPchange();
                return true;

            case R.id.broadcast_opt:
                showAlertDialogforBroadcast();
                return true;
            case R.id.stop_opt:
                msgSending = false;
                msgWriting = false;

//                startService(); // 서비스 종료
                stopService();
                updateUI();

                Toast.makeText(getApplicationContext(),"All connections are terminated",Toast.LENGTH_LONG).show();
                return true;
            case R.id.write_opt:
                if(!msgWriting){
                    msgWriting = true;
                }
                startService();
                updateUI();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
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
        //sensorManager.unregisterListener(this);
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
        wifiLock.release();
        super.onDestroy();
    }

    Button.OnClickListener syncbtnClickListener = new Button.OnClickListener() {
        public void onClick(View arg0) {
            if(syncInfo==0){
                syncInfo = 2;
                classifying = true;
            }else{
                syncInfo = 0;
                classifying = false;
            }

            startService();
            updateUI();


        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 0) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                Toast.makeText(getApplicationContext(), "Video taken", Toast.LENGTH_SHORT).show();
                // Do something with the contact here (bigger example below)
            }
        }
    }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        if (event.getAction()==KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                case KeyEvent.KEYCODE_VOLUME_DOWN:

//                    if(!isSyncing){
//                        Toast.makeText(getApplicationContext(), "Sync start", Toast.LENGTH_SHORT).show();
//                        syncStart = System.currentTimeMillis();
//
//                        isSyncing = true;
//                    }


                    return true;
                default:
                    return super.dispatchKeyEvent(event);
            }
        }
        else if (event.getAction()==KeyEvent.ACTION_UP) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
//                    if(isSyncing){
//                        syncEnd  = System.currentTimeMillis();
//                        Utils.appendStringToThisFile(Utils.getDirForDevice() + Utils.getMACAddress("wlan0")+"_SYNCinfo.csv",
//                                ""+syncStart+","+syncEnd+","+Utils.getMACAddress("wlan0")+"\n");
//
//                        isSyncing = false;
//                        Toast.makeText(getApplicationContext(), "Sync end: time saved", Toast.LENGTH_SHORT).show();
//
//                    }


                    return true;
                default:
                    return super.dispatchKeyEvent(event);
            }
        }else{
            return super.dispatchKeyEvent(event);
        }


    }



    private void readSettings(){
        Log.d(TAG, "Service running: " + isServiceRunning("edu.gatech.cc.ubicomp.synclib.SendWriteService"));

        if(isServiceRunning("edu.gatech.cc.ubicomp.synclib.SendWriteService")){
            ReturningValues values = SocketComm.readCurrentStatus();
            if(values.msgWriting||values.msgSending||!(values.NAME.equals("terminated"))){
                this.msgWriting = values.msgWriting;
                this.msgSending = values.msgSending;
                this.NAME = values.NAME;
            }

            startService();
            updateUI();
        }

    }
    private void updateMyStatus(){
        // check stored IP and change automatically
        String stored_default_ip = getIPfromPreference("default_ip");
        String stored_modified_ip = getIPfromPreference("modified_ip");

//        Log.d(TAG, "updateUI");
//        Log.d(TAG, "updateMyStatus|Stored:"+stored_default_ip+", Modified:"+stored_modified_ip);

        if(stored_default_ip.equals("TheDefaultValueIfNoValueFoundOfThisKey")||!stored_default_ip.equals(CONSTANTS.IP_ADDRESS)){
            //nothing stored yet OR default ip changed
            writeIPToPreference("default_ip",CONSTANTS.IP_ADDRESS);
        }else if(stored_default_ip.equals(CONSTANTS.IP_ADDRESS)&&!stored_modified_ip.equals("TheDefaultValueIfNoValueFoundOfThisKey")){
            CONSTANTS.IP_ADDRESS = stored_modified_ip;
        }
        tv1.setText(CONSTANTS.IP_ADDRESS+":"+CONSTANTS.PORT);

        NAME = NetUtils.getWifiName(getApplicationContext()) +"\n"+ NetUtils.getMACAddress("wlan0");
        tv2.setText(NAME+"\n"+NetUtils.getIPAddress(true));
    }

    private void updateUI(){
//        Log.d(TAG, "updateUI");
        updateMyStatus();

        if (main_menu != null) {
            if (msgSending) {
                MenuItem sending_menuItem = main_menu.findItem(R.id.connect_opt);
                SpannableString s = new SpannableString("Sending...");
                s.setSpan(new ForegroundColorSpan(Color.rgb(50,200,50)), 0, s.length(), 0);
                sending_menuItem.setTitle(s);

                tv1.setTextColor(Color.rgb(50,150,50));

            }else{
                MenuItem sending_menuItem = main_menu.findItem(R.id.connect_opt);
                sending_menuItem.setTitle("Connect(Start sending)");

                tv1.setTextColor(Color.rgb(50,50,50));
            }

            if (msgWriting) {
                MenuItem sending_menuItem = main_menu.findItem(R.id.write_opt);
                SpannableString s = new SpannableString("Writing...");
                s.setSpan(new ForegroundColorSpan(Color.rgb(50, 200, 50)), 0, s.length(), 0);
                sending_menuItem.setTitle(s);
            } else {
                MenuItem sending_menuItem = main_menu.findItem(R.id.write_opt);
                sending_menuItem.setTitle("Write FILE");
            }
        }

        if (syncInfo==2) {
            btn_sync.setTextColor(Color.rgb(50,200,50));
            btn_sync.setText("Receiving...");

            device_info.setText(device_info_str);
        }else{
            btn_sync.setTextColor(Color.rgb(50,50,50));
            btn_sync.setText("Device Sync(Server)");

            device_info.setText("");
        }

    }



    private void startService(){
//        Intent intent = new Intent(
//                getApplicationContext(),//현재제어권자
//                SendWriteService.class); // 이동할 컴포넌트
        Intent intent = new Intent(
                getApplicationContext(),//현재제어권자
                SendWritePhone.class); // 이동할 컴포넌트
        intent.putExtra("Send", msgSending);
        intent.putExtra("Write", msgWriting);
        intent.putExtra("Name", NAME);
        intent.putExtra("Sync", syncInfo);
        intent.putExtra("Classify", classifying);

//        intent.putExtra("ToClient", showing_gesture);

        SocketComm.writeCurrentStatus(msgSending, msgWriting, NAME);

        startService(intent); // 서비스 시작

    }
    private void stopService(){

        Intent intent = new Intent(
                getApplicationContext(),//현재제어권자
                SendWritePhone.class); // 이동할 컴포넌트


        SocketComm.writeCurrentStatus(msgSending, msgWriting, NAME);

        stopService(intent); // 서비스 시작

    }



    private boolean isServiceRunning(String serv_name) {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
//            if("edu.gatech.cc.ubicomp.synclib.SendWriteService".equals(service.service.getClassName())) {
            if(serv_name.equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void showAlertDialogforIPchange(){
        // Creating alert Dialog with one Button
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

        //AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();

        // Setting Dialog Title
        alertDialog.setTitle("Change IP address");

        // Setting Dialog Message
        alertDialog.setMessage("Enter Correct IP");

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
                        Toast.makeText(getApplicationContext(),"IP change canceled", Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                });

        // closed

        // Showing Alert Message
        ip_change_show = alertDialog.show();

        SocketCommIP_get sock = new SocketCommIP_get();
        sock.execute();

    }
    private void showAlertDialogforBroadcast(){
        // Creating alert Dialog with one Button
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

        //AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();

        // Setting Dialog Title
        alertDialog.setTitle(getString(R.string.opt_broadcast_ip));

        // Setting Dialog Message
        alertDialog.setMessage("Enter Correct IP");

        final EditText input = new EditText(MainActivity.this);
        input.setHint("xxx.xxx.xxx.xxx;xxx.xxx.xxx.xxx");
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
                        String ips = input.getText().toString();
                        String[] ip_arr = ips.split(";");

                        Log.d("showAlertDialogforBC", "len: "+ip_arr.length);

                        for (String a : ip_arr) {
                            Log.d("showAlertDialogforBC", "IP: "+a);
                            new SocketOnetimeSend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                    a, -904);

                            try{
                                Thread.sleep(1000);

                            }catch(InterruptedException e){
                                Log.d("showAlertDialogforBC", "failed to give delay");

                            }

                        }






                    }
                });
        // Setting Negative "NO" Button
        alertDialog.setNegativeButton("CANCEL",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Write your code here to execute after dialog
                        Toast.makeText(getApplicationContext(),"IP change canceled", Toast.LENGTH_SHORT).show();
                        dialog.cancel();
                    }
                });

        // closed

        // Showing Alert Message
        alertDialog.show();
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

    private void sendBroadcastMessage(int result, String conn_str) {
        Intent intent = new Intent(SendWriteService.ACTION_CLASS);
        intent.putExtra(SendWriteService.LAST_CLASSRESULT, result);
        intent.putExtra(SendWriteService.CONNECTION_INFO, conn_str);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

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
