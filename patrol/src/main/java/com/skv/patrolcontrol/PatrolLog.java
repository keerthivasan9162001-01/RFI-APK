package com.skv.patrolcontrol;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class PatrolLog {
    private PatrolLog() {}

    public static synchronized void add(Context c, String event, String detail) {
        SharedPreferences p = c.getSharedPreferences("patrol", Context.MODE_PRIVATE);
        String now = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        String lat = p.getString("lat", "—");
        String lon = p.getString("lon", "—");
        float speed = p.getFloat("speed_kmh", 0f);
        float km = p.getFloat("distance_m", 0f) / 1000f;
        String line = now + " | " + event +
                "\n" + (detail == null || detail.trim().isEmpty() ? "" : detail + "\n") +
                "GPS: " + lat + ", " + lon +
                " | Speed: " + String.format(Locale.US, "%.0f", speed) + " km/h" +
                " | Distance: " + String.format(Locale.US, "%.2f", km) + " km";
        String old = p.getString("control_log", "");
        String next = line + (old == null || old.isEmpty() ? "" : "\n\n" + old);
        if (next.length() > 40000) next = next.substring(0, 40000);
        p.edit().putString("control_log", next).apply();
    }

    public static String get(Context c) {
        String log = c.getSharedPreferences("patrol", Context.MODE_PRIVATE).getString("control_log", "");
        return log == null || log.trim().isEmpty() ? "No control-log entries yet." : log;
    }
}
