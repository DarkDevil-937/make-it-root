package com.jhajj.autoroot;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.IBinder;

import java.io.File;
import java.io.FileWriter;

public class RootService extends Service {
    public static final String ACTION_STOP = "com.jhajj.autoroot.STOP";
    private static final String CHANNEL = "autoroot";
    private static final int NOTIF_ID = 0x526f;

    private static volatile Process activeExploit;
    private Thread worker;

    @Override
    public void onCreate() {
        super.onCreate();
        RootUtil.init(this);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL, "AutoRoot", NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            startForeground(NOTIF_ID, buildNotification("AutoRoot: stopping root..."));
            new Thread(this::stopRoot, "autoroot-stop").start();
            return START_NOT_STICKY;
        }
        startForeground(NOTIF_ID, buildNotification("AutoRoot: gaining root..."));
        if (worker == null || !worker.isAlive()) {
            worker = new Thread(this::runExploit, "autoroot-worker");
            worker.start();
        }
        return START_STICKY;
    }

    private void runExploit() {
        File log = new File(getFilesDir(), "autoroot.log");
        try {
            ApplicationInfo ai = getApplicationInfo();
            File bin = new File(ai.nativeLibraryDir, "libglroot.so");
            append(log, "=== boot/start attempt " + System.currentTimeMillis() + " ===");
            if (!bin.exists()) {
                append(log, "binary missing: " + bin);
                updateNotification("AutoRoot: exploit binary missing");
                stopSelf();
                return;
            }
            if (RootUtil.checkRoot().startsWith("ROOTED")) {
                append(log, "already rooted");
                updateNotification("AutoRoot: root active");
                stopSelf();
                return;
            }
            ProcessBuilder pb = new ProcessBuilder(bin.getAbsolutePath(), "--tries", "6");
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
            pb.directory(getFilesDir());
            Process exploit = pb.start();
            activeExploit = exploit;
            append(log, "exploit launched from " + bin.getAbsolutePath());

            for (int i = 0; i < 80; i++) {
                Thread.sleep(1500);
                if (RootUtil.checkRoot().startsWith("ROOTED")) {
                    append(log, "root active after " + ((i + 1) * 1500) + "ms");
                    stopExploitParent(exploit, log);
                    updateNotification("AutoRoot: root active");
                    stopSelf();
                    return;
                }
            }
            append(log, "root not confirmed within timeout; will retry next boot");
            updateNotification("AutoRoot: root not confirmed (retry on boot)");
        } catch (Exception e) {
            append(log, "error: " + e);
            updateNotification("AutoRoot: error " + e.getClass().getSimpleName());
        }
        stopSelf();
    }

    private void stopExploitParent(Process exploit, File log) {
        try {
            Thread.sleep(2000);
            exploit.destroy();
            Thread.sleep(1500);
            if (exploit.isAlive()) {
                exploit.destroyForcibly();
            }
            append(log, "exploit parent stopped; detached daemons remain");
        } catch (Exception e) {
            append(log, "stop parent error: " + e);
        } finally {
            activeExploit = null;
        }
    }

    private void stopRoot() {
        File log = new File(getFilesDir(), "autoroot.log");
        append(log, "=== stop root " + System.currentTimeMillis() + " ===");
        Process ex = activeExploit;
        if (ex != null) {
            try {
                ex.destroy();
                Thread.sleep(500);
                if (ex.isAlive()) ex.destroyForcibly();
            } catch (Exception ignored) {
            }
            activeExploit = null;
        }
        try {
            String cmd = "rm -f /data/local/tmp/gl/su; "
                    + "for p in /proc/[0-9]*; do c=$(cat $p/comm 2>/dev/null); "
                    + "[ \"$c\" = libglroot.so ] && kill -9 ${p#/proc/}; done; true";
            Process p = new ProcessBuilder(RootUtil.client(), "--su", "-c", cmd)
                    .redirectErrorStream(true)
                    .start();
            p.waitFor();
            append(log, "stop root cleanup issued");
        } catch (Exception e) {
            append(log, "stop root cleanup error: " + e);
        }
        updateNotification("AutoRoot: root stopped - reboot to clear exploit state");
        stopSelf();
    }

    private void append(File f, String s) {
        try (FileWriter w = new FileWriter(f, true)) {
            w.write(s);
            w.write('\n');
        } catch (Exception ignored) {
        }
    }

    private Notification buildNotification(String text) {
        Notification.Builder b;
        if (Build.VERSION.SDK_INT >= 26) {
            b = new Notification.Builder(this, CHANNEL);
        } else {
            b = new Notification.Builder(this);
        }
        b.setContentTitle("Make it ROOT")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setOngoing(true);
        return b.build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(text));
    }

    @Override
    public void onDestroy() {
        try {
            stopForeground(true);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
