package kr.ac.kaist.arrc.imustreamlib.network;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

import kr.ac.kaist.arrc.imustreamlib.CONSTANTS;

import static android.content.Context.WIFI_SERVICE;

public class NetUtils {
    /**
     * Returns MAC address of the given interface name.
     * @param interfaceName eth0, wlan0 or NULL=use first interface
     * @return  mac address or empty string
     */
    public static String getMACAddress(String interfaceName) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                if (interfaceName != null) {
                    if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
                }
                byte[] mac = intf.getHardwareAddress();
                if (mac==null) return "";
                StringBuilder buf = new StringBuilder();
                for (int idx=0; idx<mac.length; idx++)
                    buf.append(String.format("%02X_", mac[idx]));
//                buf.append(String.format("%02X:", mac[idx]));
                if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
                return buf.toString();
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
        /*try {
            // this is so Linux hack
            return loadFileAsString("/sys/class/net/" +interfaceName + "/address").toUpperCase().trim();
        } catch (IOException ex) {
            return null;
        }*/
    }

    /**Get IP address from first non-localhost interface
     * @param useIPv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
//                        Log.d("getIPAddress", sAddr);
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4 && !(sAddr.contains("192.168.167")) )
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }
    public static void disableBluetooth(){
        //Disable bluetooth
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
        }
    }

    public static void changeWifiToTargetNetwork(Context context){
        String ssid_name = getWifiName(context);
        Log.d("WIFI_NAME", "SSID: "+ssid_name);
        if (!("\"" + CONSTANTS.SSID + "\"").equals(getWifiName(context) )) {
            changeWifiNetwork(context);
        }else{
            Log.d("WIFI_NAME", "connected to target ssid");
        }
    }
    public static void changeWifiToTargetNetwork(Context context, String ssid, String passwd){
        String ssid_name = getWifiName(context);
        Log.d("WIFI_NAME", "SSID: "+ssid_name);
        if (!("\"" + CONSTANTS.SSID + "\"").equals(getWifiName(context) )) {
            changeWifiNetwork(context, ssid, passwd);
        }else{
            Log.d("WIFI_NAME", "connected to target ssid");
        }
    }

    public static String getWifiName(Context context) {
        // will return ssid name with quatation marks ("[SSID]")
        WifiManager manager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        if (manager.isWifiEnabled()) {
            WifiInfo wifiInfo = manager.getConnectionInfo();
            if (wifiInfo != null) {
                NetworkInfo.DetailedState state = WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState());
                if (state == NetworkInfo.DetailedState.CONNECTED || state == NetworkInfo.DetailedState.OBTAINING_IPADDR) {
                    return wifiInfo.getSSID();
                }
            }
        }
        return null;
    }
    public static void changeWifiNetwork(Context ctxt){
        // WiFi connection forcing function
        // Target is stored in CONSTANTS class
        changeWifiNetwork(ctxt, CONSTANTS.SSID, CONSTANTS.PASSWD);

    }
    public static void changeWifiNetwork(Context ctxt, String ssid, String passwd){
        // WiFi connection forcing function
        // reference: https://stackoverflow.com/questions/8818290/how-do-i-connect-to-a-specific-wi-fi-network-in-android-programmatically

        WifiManager wifiManager = (WifiManager) ctxt.getSystemService(WIFI_SERVICE);

        // check all stored wifi network
        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        boolean in_saved_list = false;
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + ssid + "\"")) {

                wifiManager.disconnect();
                wifiManager.enableNetwork( i.networkId, true);
                wifiManager.reconnect();
                break;
            }
        }
        if (!in_saved_list) {
            WifiConfiguration wifiConfig = new WifiConfiguration();
            wifiConfig.SSID = String.format("\"%s\"", ssid);
            wifiConfig.preSharedKey = String.format("\"%s\"", passwd);

            wifiManager.disconnect();
            wifiManager.enableNetwork( wifiManager.addNetwork(wifiConfig), true);
            wifiManager.reconnect();
        }


        Toast.makeText(ctxt, "Wifi connected", Toast.LENGTH_LONG);
    }
}
