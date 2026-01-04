package com.example.moonpaw.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.example.moonpaw.MainActivity;
import com.example.moonpaw.R;

public class NapReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // QUAN TRỌNG: Đổi ID mới để reset cài đặt âm thanh trên máy thật
        String channelId = "moonpaw_alarm_v99_final";

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Báo thức Ngủ bù",
                    NotificationManager.IMPORTANCE_HIGH
            );

            // Cấu hình âm thanh BẮT BUỘC cho Android 8.0+
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM) // Quan trọng: Loại Báo thức
                    .build();

            channel.setSound(alarmSound, audioAttributes);
            channel.enableVibration(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC); // Hiện trên màn hình khóa

            notificationManager.createNotificationChannel(channel);
        }

        Intent appIntent = new Intent(context, MainActivity.class);
        appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_bedtime)
                .setContentTitle("Dậy thôi nào! ☀️")
                .setContentText("Hết giờ ngủ bù rồi.")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setSound(alarmSound)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true); // Quan trọng: Hiện popup đè lên màn hình

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_INSISTENT; // Reo liên tục

        notificationManager.notify(1001, notification);
    }
}