package kr.ac.kaist.arrc.imustreamlib.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import kr.ac.kaist.arrc.imustreamlib.CONSTANTS;

public class WifiReceiver extends BroadcastReceiver {
    String TAG = "WifiReceiver";
    WifiManager wifiManager;
    HashSet<String> wifi_ssid_hash = new HashSet<String>();;

    public WifiReceiver() {
    }
    public WifiReceiver(WifiManager wifiManager) {
        this.wifiManager = wifiManager;
    }
    public void registerManager(WifiManager wifiManager) {
        this.wifiManager = wifiManager;

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // e.g. To check the Network Name or other info:
        String action = intent.getAction();
        Log.d(TAG, "action: "+action);


        // update wifi ssid list
        wifi_ssid_hash = new HashSet<String>();
        List<ScanResult> wifiList = wifiManager.getScanResults();
        for (ScanResult scanResult : wifiList) {
            wifi_ssid_hash.add(scanResult.SSID);
        }
        Log.d(TAG, wifi_ssid_hash.toString());

        // change wifi network if target ssid is in list
        if (wifi_ssid_hash.contains(CONSTANTS.SSID)) {
            try {
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                if (wifiManager.isWifiEnabled()) {
                    //wifi is enabled
                    NetUtils.changeWifiToTargetNetwork(context);
                } else {
                    wifiManager.setWifiEnabled(true);
                    NetUtils.changeWifiToTargetNetwork(context);
                }

                String ssid = NetUtils.getWifiName(context);
                Log.d(TAG, "connected ssid: " + ssid);

            } catch (Exception e) {
                Log.d(TAG, "exception: " + e.getMessage());
            }

        } else {
            // show TOAST message if target ssid is not in network
            Log.d(TAG, "Target SSID does not exist...please check");
            Toast.makeText(context, "Target SSID does not exist...please check", Toast.LENGTH_SHORT).show();
        }

    }
}