package kr.ac.kaist.arrc.imustreamlib;

import android.content.Context;
import android.media.MediaPlayer;

public class AudioFeedbackTool {
    private static long last_audio_time;
    private static final long MINIMUM_DT = 1200;
    private static MediaPlayer mpintro;

    public static void playNoti(Context ctxt, int id_) {
        if(!checkLastDt())
            return;

        switch (id_) {
            case 0:
            case 2:
                mpintro = MediaPlayer.create(ctxt, R.raw.leg_);
                mpintro.setLooping(false);
                mpintro.start();
                break;
            case 1:
            case 4:
                mpintro = MediaPlayer.create(ctxt, R.raw.head_);
                mpintro.setLooping(false);
                mpintro.start();
                break;

            case 100:
                mpintro = MediaPlayer.create(ctxt, R.raw.head_leg_updown_0);
                mpintro.setLooping(false);
                mpintro.start();
                break;
            case 101:
                mpintro = MediaPlayer.create(ctxt, R.raw.hand_head_updown_1);
                mpintro.setLooping(false);
                mpintro.start();
                break;
            case 102:
                mpintro = MediaPlayer.create(ctxt, R.raw.hand_leg_leftright2);
                mpintro.setLooping(false);
                mpintro.start();
                break;
            case 103:
                mpintro = MediaPlayer.create(ctxt, R.raw.head_leg_leftright_3 );
                mpintro.setLooping(false);
                mpintro.start();
                break;
            case 104:
                mpintro = MediaPlayer.create(ctxt, R.raw.hand_head_leftright_4);
                mpintro.setLooping(false);
                mpintro.start();
                break;
        }

//        if(id_==2){
////            mpintro = MediaPlayer.create(ctxt, R.raw.b_handhead);
//            mpintro = MediaPlayer.create(ctxt, R.raw.head_);
//            mpintro.setLooping(false);
//            mpintro.start();
//        } else if (id_ == 4) {
////            mpintro = MediaPlayer.create(ctxt, R.raw.c_handleg);
//            mpintro = MediaPlayer.create(ctxt, R.raw.leg_);
//            mpintro.setLooping(false);
//            mpintro.start();
//        }
        last_audio_time = System.currentTimeMillis();
    }

    public static void playError(Context ctxt) {
        if(!checkLastDt())
            return;

        mpintro = MediaPlayer.create(ctxt, R.raw.bleep);
        mpintro.setLooping(false);
        mpintro.start();

        last_audio_time = System.currentTimeMillis();
    }
    public static void playSuccess(Context ctxt) {
        if(!checkLastDt())
            return;

        mpintro = MediaPlayer.create(ctxt, R.raw.tada);
        mpintro.setLooping(false);
        mpintro.start();

        last_audio_time = System.currentTimeMillis();
    }

    public static boolean checkLastDt(){
        return System.currentTimeMillis()-last_audio_time > MINIMUM_DT;

    }

    // <Example>
    // AudioFeedbackTool.playNoti(getApplicationContext(),4);
}
