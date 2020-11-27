package kr.ac.kaist.arrc.imustreamglass;

import com.google.android.glass.media.Sounds;
import com.google.android.glass.widget.CardBuilder;
import com.google.android.glass.widget.CardScrollAdapter;
import com.google.android.glass.widget.CardScrollView;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import kr.ac.kaist.arrc.R;
import kr.ac.kaist.arrc.imustreamlib.CONSTANTS;
import kr.ac.kaist.arrc.imustreamlib.SocketComm;
import kr.ac.kaist.arrc.imustreamlib.ReturningValues;
import kr.ac.kaist.arrc.imustreamlib.SendWriteService;
import kr.ac.kaist.arrc.imustreamlib.network.NetUtils;
import kr.ac.kaist.arrc.imustreamlib.network.WifiReceiver;


/**
 * An {@link Activity} showing a tuggable "Hello World!" card.
 * <p>
 * The main content view is composed of a one-card {@link CardScrollView} that provides tugging
 * feedback to the user when swipe gestures are detected.
 * If your Glassware intends to intercept swipe gestures, you should set the content view directly
 * and use a {@link com.google.android.glass.touchpad.GestureDetector}.
 *
 * @see <a href="https://developers.google.com/glass/develop/gdk/touch">GDK Developer Guide</a>
 */
public class MainActivity extends Activity {

    private CardScrollView mCardScroller;

    private int view_size = 5;
    private View[] mView = new View[view_size];


    private String NAME = "GlassJY";

    private static final String TAG = "GlassMainActivity";


    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;
    WifiManager wifiManager;
    WifiManager.WifiLock wifiLock;

    private Boolean msgSending = false;
    private Boolean msgWriting = false;

    public static final String myPref = "IP_addr";

    long start_t;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        NAME = NetUtils.getMACAddress("wlan0");
        updateActionBar();
//        mView[0] = buildView(0);
//        mView[1] = buildView(1);
//        mView[2] = buildView(2);
//        mView[3] = buildView(3);
        AudioManager am =
                (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        am.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                am.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0);


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



        mCardScroller = new CardScrollView(this);
        mCardScroller.setAdapter(new CardScrollAdapter() {
            @Override
            public int getCount() {

                return view_size;
            }

            @Override
            public Object getItem(int position) {
//                mView = buildView(position);
                return buildView(position);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
//                mView = buildView(position);
                return buildView(position);
            }

            @Override
            public int getPosition(Object item) {
                if (mView.equals(item)) {
                    return 0;
                }
                return AdapterView.INVALID_POSITION;
            }
        });

        // Handle the TAP event.
        mCardScroller.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Plays disallowed sound to indicate that TAP actions are not supported.
                Log.d(TAG, "id: " + id);
                updateActionBar();
//                if(id==0){
                if (id == 3) {

                    Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                    intent.putExtra(android.provider.MediaStore.EXTRA_SIZE_LIMIT, 5242880);
                    intent.putExtra("EXTRA_VIDEO_QUALITY", 1);
                    startActivityForResult(intent, 0);


                } else if (id == 1) {


                    Intent startMain = new Intent(Intent.ACTION_MAIN);
                    startMain.addCategory(Intent.CATEGORY_HOME);
                    startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(startMain);
                } else if (id == 2) {
                    if (!msgWriting) {
//                        sendToServer.setWriting("testing");

                        msgWriting = true;
                        startService();

                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.playSoundEffect(Sounds.SUCCESS);
                    } else {
                        msgWriting = false;
                        startService();
//                        msgSending = false;
//                        sendToServer.disconnect();
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.playSoundEffect(Sounds.DISALLOWED);
                    }

//                }else if(id == 3){
                } else if (id == 0) {
                    if (!msgSending) {
                        msgSending = true;

//                        sendToServer = new SocketComm(bufferQueue);
//                        sendToServer.setIsSending(true);
//                        sendToServer.execute();
                        startService();
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.playSoundEffect(Sounds.SUCCESS);
                    } else {
//                        msgWriting = false;
                        msgSending = false;
//                        sendToServer.disconnect();
                        startService();
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.playSoundEffect(Sounds.DISALLOWED);
                    }

                } else if (id == 4) {
                    msgWriting = false;
                    msgSending = false;
                    startService();

                    Intent intent = new Intent(
                            getApplicationContext(),//현재제어권자
                            SendWriteService.class); // 이동할 컴포넌트
                    stopService(intent); // 서비스 종료

                    finish();
                } else {
                    AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                    am.playSoundEffect(Sounds.DISALLOWED);

                }
                mCardScroller.getAdapter().notifyDataSetChanged();

            }
        });
        setContentView(mCardScroller);



        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "selfsync:GlassWakelock");
        wakeLock.acquire();

        //wakelock for prevent WiFi shutdown
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF , "selfsync:PhoneWWiFilock");
        wifiLock.acquire();

        // WiFi connection forcing part
        NetUtils.changeWifiToTargetNetwork(getApplicationContext());
        // Disable BT
        NetUtils.disableBluetooth();
        // Register Wifi manager
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(new WifiReceiver(wifiManager), intentFilter);


        readSettings();

    }

    @Override
    protected void onResume() {
        super.onResume();
        readSettings();
        mCardScroller.activate();
    }

    @Override
    protected void onPause() {
        mCardScroller.deactivate();
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

        writeIPToPreference("modified_ip", CONSTANTS.IP_ADDRESS);

        wakeLock.release();
        wifiLock.release();
        super.onDestroy();
    }

    /**
     * Builds a Glass styled "Hello World!" view using the {@link CardBuilder} class.
     */
    private View buildView(int i) {

        if(i == 2){
            return buildWritingView();
        }
//        else if(i == 3){
        else if(i == 0){
            return buildUDPView();
        }else if(i == 4){
            CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);

            card.setText(R.string.close_app);
            return card.getView();
//        }else if(i==0){
        }else if(i==3){
            CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);

            card.setText("Tap to record Video now");
            return card.getView();
        }
        else{
            CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);

            card.setText(R.string.to_home);
            return card.getView();
        }

    }

    private View buildUDPView() {

        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);
        if(!msgSending){
            card.setText(getResources().getString(R.string.network_start)+"\nserver:"+CONSTANTS.IP_ADDRESS+"\n(ssid="+NetUtils.getWifiName(this)+")");
        }else{
            card.setText(getResources().getString(R.string.network_on)+"device:"+NetUtils.getIPAddress(true));

        }
        return card.getView();
    }
    private View buildWritingView() {

        CardBuilder card = new CardBuilder(this, CardBuilder.Layout.TEXT);
        if(!msgWriting){
            card.setText(R.string.write_start);
        }else{
            card.setText(R.string.write_on);
        }
        return card.getView();
    }
    private void updateActionBar(){
        getActionBar().setTitle(getResources().getString(R.string.app_name)+"-"+NAME);
    }


    private void readSettings(){
        Log.d(TAG, "Service running: " + isServiceRunning());
        if(isServiceRunning()){
            ReturningValues values = SocketComm.readCurrentStatus();
            if(values.msgWriting||values.msgSending||!(values.NAME.equals("terminated"))){
                this.msgWriting = values.msgWriting;
                this.msgSending = values.msgSending;
                this.NAME = values.NAME;
            }

            startService();
        }

    }

    private void startService(){
        Intent intent = new Intent(
                getApplicationContext(),//현재제어권자
                SendWriteService.class); // 이동할 컴포넌트
        intent.putExtra("Send", msgSending);
        intent.putExtra("Write", msgWriting);
        intent.putExtra("Name", NAME);

        SocketComm.writeCurrentStatus(msgSending, msgWriting, NAME);

        startService(intent); // 서비스 시작

    }
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("edu.gatech.cc.ubicomp.synclib.SendWriteService".equals(service.service.getClassName())) {
                Log.d(TAG, "Service is running");
                return true;
            }
        }
        return false;
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



}
