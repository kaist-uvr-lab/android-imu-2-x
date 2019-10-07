package kr.ac.kaist.arrc.imustreamlib.network;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // e.g. To check the Network Name or other info:

        try{
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager.isWifiEnabled()){
                //wifi is enabled
                NetUtils.changeWifiToTargetNetwork(context);
            }else{
                wifiManager.setWifiEnabled(true);
                NetUtils.changeWifiToTargetNetwork(context);
            }

            String ssid = NetUtils.getWifiName(context);
            Log.d("WifiReceiverJY", "connected ssid: "+ssid);

        } catch(Exception e){
            Log.d("WifiReceiverJY", "exception: "+e.getMessage());
        }



    }
}