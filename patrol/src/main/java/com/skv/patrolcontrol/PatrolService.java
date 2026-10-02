package com.skv.patrolcontrol;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;

public class PatrolService extends Service implements LocationListener {
    public static final String ACTION_START = "com.skv.patrolcontrol.START";
    public static final String ACTION_STOP = "com.skv.patrolcontrol.STOP";
    public static final String ACTION_UPDATE = "com.skv.patrolcontrol.UPDATE";
    private static final String CHANNEL = "patrol_tracking";
    private LocationManager lm;
    private Location lastAccepted;
    private float totalMeters = 0f;
    private long startedAt = 0L;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        android.content.SharedPreferences p = getSharedPreferences("patrol", MODE_PRIVATE);
        totalMeters = p.getFloat("distance_m", 0f);
        startedAt = p.getLong("started_at", 0L);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? ACTION_START : intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopTracking();
            return START_NOT_STICKY;
        }
        if (startedAt == 0L) startedAt = System.currentTimeMillis();
        getSharedPreferences("patrol", MODE_PRIVATE).edit()
                .putBoolean("active", true).putLong("started_at", startedAt).apply();
        startForeground(41, buildNotification("Starting GPS…"));
        beginTracking();
        return START_STICKY;
    }

    private void beginTracking() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 3f, this);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 8000L, 10f, this);
        } catch (Exception ignored) {}
    }

    private void stopTracking() {
        try { if (lm != null) lm.removeUpdates(this); } catch (Exception ignored) {}
        getSharedPreferences("patrol", MODE_PRIVATE).edit().putBoolean("active", false).apply();
        sendBroadcast(new Intent(ACTION_UPDATE).setPackage(getPackageName()));
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
        stopSelf();
    }

    @Override public void onLocationChanged(Location location) {
        if (location == null) return;
        if (location.hasAccuracy() && location.getAccuracy() > 80f) return;
        if (lastAccepted != null) {
            float d = lastAccepted.distanceTo(location);
            long dt = Math.max(1L, location.getTime() - lastAccepted.getTime());
            float impliedKmh = (d / (dt / 1000f)) * 3.6f;
            if (d >= 2f && impliedKmh < 180f) totalMeters += d;
        }
        lastAccepted = new Location(location);
        float speedKmh = location.hasSpeed() ? location.getSpeed() * 3.6f : 0f;
        getSharedPreferences("patrol", MODE_PRIVATE).edit()
                .putLong("last_ts", System.currentTimeMillis())
                .putString("lat", String.format(java.util.Locale.US, "%.6f", location.getLatitude()))
                .putString("lon", String.format(java.util.Locale.US, "%.6f", location.getLongitude()))
                .putFloat("accuracy", location.hasAccuracy() ? location.getAccuracy() : 0f)
                .putFloat("speed_kmh", speedKmh)
                .putFloat("distance_m", totalMeters)
                .apply();
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(41, buildNotification(String.format(java.util.Locale.US, "%.0f km/h • %.1f km", speedKmh, totalMeters/1000f)));
        sendBroadcast(new Intent(ACTION_UPDATE).setPackage(getPackageName()));
    }

    private Notification buildNotification(String text) {
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("Patrol Control — Tracking active")
                .setContentText(text)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Patrol tracking", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Keeps patrol GPS tracking active while the screen is off.");
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);
        }
    }

    @Override public void onProviderEnabled(String provider) {}
    @Override public void onProviderDisabled(String provider) {}
    @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
    @Override public IBinder onBind(Intent intent) { return null; }
}
