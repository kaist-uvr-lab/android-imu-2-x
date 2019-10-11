package kr.ac.kaist.arrc.imustreamphone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import kr.ac.kaist.arrc.R;
import kr.ac.kaist.arrc.imustreamlib.SendWriteService;

public class SendWritePhone extends SendWriteService {
    final String TAG = "SendWritePhone";
    public static final String CHANNEL_ID = "ForegroundServiceChannel";
    public static final int FOREGROUND_ID = 1;

    public SendWritePhone() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setContentTitle("IMUstream Service")
                .setContentText("foreground service")
                .setSmallIcon(R.drawable.gesture_null)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(FOREGROUND_ID, notification);
        Log.d(TAG, "foreground started");
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        stopForeground(true);
        Log.d(TAG, "onDestroy");
    }
}
