package kr.ac.kaist.arrc.imustreamlib;

import android.media.AudioManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class VibratorTool{
    private static String TAG = "VibratorTool";
    private static Vibrator v;
    private static AudioManager am;

    private static long last_vib_time;
    private static final long MIMIMUM_DT = 3000;

    public VibratorTool(Vibrator system_v){
        v = system_v;

    }

    public static void setVibrator(Vibrator system_v){
        v = system_v;
    }
    public static void setAudioManager(AudioManager system_am){
        am = system_am;
    }

    public static void vibrateFor500ms() {
        if(!checkLastVibrateDt())
            return;

        v.vibrate(500);
        last_vib_time = System.currentTimeMillis();
//        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    public static void vibrateForLong() {
        if(!checkLastVibrateDt())
            return;

        v.vibrate(1500);
        last_vib_time = System.currentTimeMillis();
//        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    public static void vibrateError() {
        if(!checkLastVibrateDt())
            return;


        long[] pattern = {0, 150,80,150};
        v.vibrate(pattern, -1);
        last_vib_time = System.currentTimeMillis();
    }


    public static void vibrateTargetNoti() {
        if(!checkLastVibrateDt())
            return;

        long[] pattern = {0, 200,80,200,80,200,80,200};
        v.vibrate(pattern, -1);
        last_vib_time = System.currentTimeMillis();
    }

    public static void vibrateFinish() {
        if(!checkLastVibrateDt())
            return;

        long[] pattern = {0, 1000,400, 1000,400, 1000,400, 1000,400, 1000,400 };
        v.vibrate(pattern, -1);
        last_vib_time = System.currentTimeMillis();
    }

    public static void notiVibrate(){
        if(!checkLastVibrateDt())
            return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            last_vib_time = System.currentTimeMillis();
        } else {
            //deprecated in API 26
            v.vibrate(500);
            last_vib_time = System.currentTimeMillis();
        }

    }

    public static boolean checkLastVibrateDt(){
        return System.currentTimeMillis()-last_vib_time > MIMIMUM_DT;

    }
}
