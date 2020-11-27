/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kr.ac.kaist.arrc.imustreamee2;


import androidx.core.app.ActivityCompat;

//import kr.ac.kaist.arrc.R;
import kr.ac.kaist.arrc.imustreamlib.ReturningValues;
import kr.ac.kaist.arrc.imustreamlib.SendWriteService;
import kr.ac.kaist.arrc.imustreamlib.SocketComm;
import kr.ac.kaist.arrc.imustreamlib.network.NetUtils;
import kr.ac.kaist.arrc.imustreamlib.Utils;
import kr.ac.kaist.arrc.imustreamlib.CONSTANTS;
import kr.ac.kaist.arrc.imustreamlib.network.WifiReceiver;


import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import kr.ac.kaist.arrc.imustreamee2.fragments.BaseFragment;
import kr.ac.kaist.arrc.imustreamee2.fragments.MainLayoutFragment;
import kr.ac.glass.ui.GlassGestureDetector;

import kr.ac.kaist.arrc.imustreamee2.R;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Main activity of the application. It provides viewPager to move between fragments.
 */
public class MainActivity extends BaseActivity {
    private final String TAG = "MainActivity";

    private List<BaseFragment> fragments = new ArrayList<>();
    private ViewPager viewPager;
    private ScreenSlidePagerAdapter screenSlidePagerAdapter;

    private int view_size = 5;
    private View[] mView = new View[view_size];

    private Boolean msgSending = false;
    private Boolean msgWriting = false;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;
    WifiReceiver wifirecevier;
    WifiManager wifiManager;
    WifiManager.WifiLock wifiLock;

    private String NAME = "GlassEE2";

    int cnt = 0;

    public static final String myPref = "IP_addr";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_pager_layout);

        // check permission
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.ACCESS_FINE_LOCATION
//                Manifest.permission.ACCESS_NETWORK_STATE,
//                Manifest.permission.ACCESS_WIFI_STATE
        };
        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }


        // set MAC address to NAME
        NAME = NetUtils.getMACAddress("wlan0");

        String stored_default_ip = getIPfromPreference("default_ip");
        String stored_modified_ip = getIPfromPreference("modified_ip");


        //wakelock for prevent shutdown
        powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "selfsync:PhoneWakelock");
        wakeLock.acquire();

        //wakelock for prevent WiFi shutdown
//        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF , "selfsync:GlassWiFilock");
//        wifiLock.acquire();


        // WiFi connection forcing part
        NetUtils.changeWifiToTargetNetwork(getApplicationContext());
        // Register Wifi manager
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
//        wifirecevier = new WifiReceiver();
//        registerReceiver(wifirecevier, intentFilter);

        if (stored_default_ip.equals("TheDefaultValueIfNoValueFoundOfThisKey") || !stored_default_ip.equals(CONSTANTS.IP_ADDRESS)) {
            //nothing stored yet OR default ip changed
            writeIPToPreference("default_ip", CONSTANTS.IP_ADDRESS);
        } else if (stored_default_ip.equals(CONSTANTS.IP_ADDRESS) && !stored_modified_ip.equals("TheDefaultValueIfNoValueFoundOfThisKey")) {
            CONSTANTS.IP_ADDRESS = stored_modified_ip;
        }


        // add cards
        screenSlidePagerAdapter = new ScreenSlidePagerAdapter(
                getSupportFragmentManager());
        viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(screenSlidePagerAdapter);

//        fragments.add(MainLayoutFragment
//                .newInstance(getString(R.string.text_sample), getString(R.string.footnote_sample),
//                        getString(R.string.timestamp_sample), null));
//        fragments.add(MainLayoutFragment
//                .newInstance(getString(R.string.different_options), getString(R.string.empty_string),
//                        getString(R.string.empty_string), R.menu.main_menu));
//        fragments.add(ColumnLayoutFragment
//                .newInstance(R.drawable.ic_style, getString(R.string.columns_sample),
//                        getString(R.string.footnote_sample), getString(R.string.timestamp_sample)));
//        fragments.add(MainLayoutFragment
//                .newInstance(getString(R.string.like_this_sample), getString(R.string.empty_string),
//                        getString(R.string.empty_string), null));

        screenSlidePagerAdapter.notifyDataSetChanged();

        final TabLayout tabLayout = findViewById(R.id.page_indicator);
        tabLayout.setupWithViewPager(viewPager, true);


    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");

        if(wifirecevier!=null) unregisterReceiver(wifirecevier);
        if(wakeLock!=null) wakeLock.release();
        if(wifiLock!=null) wifiLock.release();

        super.onDestroy();
    }

    @Override
    public boolean onGesture(GlassGestureDetector.Gesture gesture) {
        switch (gesture) {
            case TAP:
//                fragments.get(viewPager.getCurrentItem()).onSingleTapUp();
                Log.d(TAG, "getCurrentItem: " + viewPager.getCurrentItem());
                switch (viewPager.getCurrentItem()) {
                    case 3:
                        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                        intent.putExtra(android.provider.MediaStore.EXTRA_SIZE_LIMIT, 5242880);
                        intent.putExtra("EXTRA_VIDEO_QUALITY", 1);
                        startActivityForResult(intent, 0);
                        break;
                    case 1:
                        Intent startMain = new Intent(Intent.ACTION_MAIN);
                        startMain.addCategory(Intent.CATEGORY_HOME);
                        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(startMain);
                        break;
                    case 2:
                        if (!msgWriting) {

                            msgWriting = true;
                            startService();

                            playSound(RingtoneManager.TYPE_NOTIFICATION);

//                            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//                            am.playSoundEffect(Sounds.SUCCESS);
                        } else {
                            msgWriting = false;
                            startService();

                            playSound(RingtoneManager.TYPE_ALARM);
//                            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//                            am.playSoundEffect(Sounds.DISALLOWED);
                        }
                        break;
                    case 0:
                        if (!msgSending) {
                            msgSending = true;
                            startService();
                            playSound(RingtoneManager.TYPE_NOTIFICATION);

//                            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//                            am.playSoundEffect(Sounds.SUCCESS);
                        } else {
                            msgSending = false;
                            startService();
                            playSound(RingtoneManager.TYPE_ALARM);
//                            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//                            am.playSoundEffect(Sounds.DISALLOWED);
                        }
                        break;
                    case 4:
                        msgWriting = false;
                        msgSending = false;
                        startService();

                        Intent intent_ = new Intent(
                                getApplicationContext(),//현재제어권자
                                SendWriteService.class); // 이동할 컴포넌트
                        stopService(intent_); // 서비스 종료

                        finish();
                        break;
                    default:
                        playSound(RingtoneManager.TYPE_ALARM);
//                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
//                        am.playSoundEffect(Sounds.DISALLOWED);
                        break;
                }
                screenSlidePagerAdapter.notifyDataSetChanged();

                /*if (viewPager.getCurrentItem() == 0) {
                    Bundle args = fragments.get(viewPager.getCurrentItem()).getArguments();
                    cnt++;
                    args.putString("text_key", "cnt: " + cnt);
                    fragments.set(viewPager.getCurrentItem(),
                            MainLayoutFragment.newInstance("cnt: " + cnt, "no footnote", "no timestamp", null));

                    fragments.add(MainLayoutFragment.newInstance("cnt: " + cnt, "no footnote", "no timestamp", null));
                    screenSlidePagerAdapter.notifyDataSetChanged();
                    Log.d(TAG, "set");

                }*/


                return true;
            default:
                return super.onGesture(gesture);
        }
    }

    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {

        ScreenSlidePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return buildView(position);
        }

        @Override
        public int getCount() {
            return view_size;
        }
    }






    private Fragment buildView(int i) {
        switch(i){
            case 2:
                return buildWritingView();
            case 0:
                return buildUDPView();
            case 4:
                return MainLayoutFragment.newInstance(getResources().getString(R.string.close_app),
                        "","", null);
            case 3:
                return MainLayoutFragment.newInstance("Tap to record Video now",
                        "","", null);
            default:
                return MainLayoutFragment.newInstance(getResources().getString(R.string.to_home),
                        "","", null);
        }

        /*if (i == 2) {
            return buildWritingView();
        }
        else if (i == 0) {
            return buildUDPView();
        } else if (i == 4) {
            return MainLayoutFragment.newInstance(getResources().getString(R.string.close_app),
                    "","", null);
        } else if (i == 3) {
            return MainLayoutFragment.newInstance("Tap to record Video now",
                    "","", null);
        } else {
            return MainLayoutFragment.newInstance(getResources().getString(R.string.to_home),
                    "","", null);
        }*/
    }

    private Fragment buildUDPView() {
        BaseFragment frag;
        String disp_str = "";
        if (!msgWriting) {
            disp_str = getResources().getString(R.string.network_start)
                    + "\nserver:" + CONSTANTS.IP_ADDRESS
                    + "\n(ssid=" + NetUtils.getWifiName(MainActivity.this) + ")";
        } else {
            disp_str = getResources().getString(R.string.network_on) + "device:" + NetUtils.getIPAddress(true);
        }

        frag = MainLayoutFragment.newInstance(disp_str, "","", null);
        return frag;
    }

    private Fragment buildWritingView() {

        BaseFragment frag;

        if (!msgWriting) {
            frag = MainLayoutFragment.newInstance(getString(R.string.write_start), "","", null);
        } else {
            frag = MainLayoutFragment.newInstance(getString(R.string.write_on), "","", null);
        }
        return frag;
    }

    public void playSound(int type_) {
        try {
            Uri notification = RingtoneManager.getDefaultUri(type_);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

}
