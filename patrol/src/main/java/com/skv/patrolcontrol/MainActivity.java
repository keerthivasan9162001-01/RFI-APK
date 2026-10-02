package com.skv.patrolcontrol;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    private final int NAVY = Color.rgb(11,31,51);
    private final int GREEN = Color.rgb(22,163,106);
    private final int RED = Color.rgb(180,35,24);
    private LinearLayout root;
    private TextView status, speed, distance, latitude, longitude, accuracy, lastUpdate, started, incidents;
    private Button patrolButton;
    private BroadcastReceiver receiver;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY);
        buildUi();
        requestNeededPermissions();
        receiver = new BroadcastReceiver() { @Override public void onReceive(Context c, Intent i) { refresh(); } };
        refresh();
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter(PatrolService.ACTION_UPDATE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, f);
        refresh();
    }

    @Override protected void onStop() {
        super.onStop();
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
    }

    private void buildUi() {
        ScrollView sv = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(28));
        root.setBackgroundColor(Color.rgb(245,247,250));
        sv.addView(root);

        TextView title = text("PATROL CONTROL", 24, Typeface.BOLD, Color.WHITE);
        title.setPadding(dp(18), dp(20), dp(18), dp(20));
        title.setBackgroundColor(NAVY);
        root.addView(title, mp(-1, -2, 0));

        TextView section = text("NH-44  •  Patrol Vehicle Monitoring", 14, Typeface.BOLD, NAVY);
        section.setPadding(dp(4), dp(16), 0, dp(8));
        root.addView(section);

        LinearLayout statusCard = card();
        status = text("PATROL NOT STARTED", 15, Typeface.BOLD, Color.DKGRAY);
        status.setGravity(Gravity.CENTER);
        statusCard.addView(status, mp(-1,-2,8));
        TextView route = text("Krishnagiri – Thumbipadi  |  Km 94+000 – 180+000", 13, Typeface.NORMAL, Color.DKGRAY);
        route.setGravity(Gravity.CENTER);
        statusCard.addView(route, mp(-1,-2,0));
        root.addView(statusCard, mp(-1,-2,8));

        LinearLayout row1 = row();
        speed = metric(row1, "SPEED", "— km/h");
        distance = metric(row1, "DISTANCE", "0.00 km");
        root.addView(row1, mp(-1,-2,8));

        LinearLayout row2 = row();
        latitude = metric(row2, "LATITUDE", "—");
        longitude = metric(row2, "LONGITUDE", "—");
        root.addView(row2, mp(-1,-2,8));

        LinearLayout row3 = row();
        accuracy = metric(row3, "GPS ACCURACY", "—");
        started = metric(row3, "PATROL STARTED", "—");
        root.addView(row3, mp(-1,-2,8));

        LinearLayout updateCard = card();
        lastUpdate = text("Waiting for GPS", 14, Typeface.NORMAL, Color.DKGRAY);
        updateCard.addView(lastUpdate);
        root.addView(updateCard, mp(-1,-2,8));

        patrolButton = new Button(this);
        patrolButton.setText("START PATROL");
        patrolButton.setTextColor(Color.WHITE);
        patrolButton.setTextSize(16);
        patrolButton.setTypeface(Typeface.DEFAULT_BOLD);
        patrolButton.setAllCaps(false);
        patrolButton.setOnClickListener(v -> togglePatrol());
        root.addView(patrolButton, mp(-1, dp(58), 12));

        Button report = actionButton("REPORT INCIDENT", NAVY);
        report.setOnClickListener(v -> showIncidentDialog());
        root.addView(report, mp(-1, dp(54), 8));

        Button controlLog = actionButton("OPEN CONTROL LOG", Color.rgb(22,125,163));
        controlLog.setOnClickListener(v -> startActivity(new Intent(this, ControlLogActivity.class)));
        root.addView(controlLog, mp(-1, dp(54), 8));

        Button reset = actionButton("RESET TODAY'S DISTANCE", Color.rgb(95,106,120));
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Reset distance?")
                .setMessage("This clears only the distance counter on this phone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Reset", (d,w) -> {
                    float oldKm = getSharedPreferences("patrol", MODE_PRIVATE).getFloat("distance_m",0f) / 1000f;
                    PatrolLog.add(this, "DISTANCE RESET", String.format(Locale.US, "Previous distance: %.2f km", oldKm));
                    getSharedPreferences("patrol", MODE_PRIVATE).edit().putFloat("distance_m",0f).apply();
                    refresh();
                }).show());
        root.addView(reset, mp(-1, dp(50), 14));

        TextView logTitle = text("RECENT INCIDENT LOG", 14, Typeface.BOLD, NAVY);
        logTitle.setPadding(dp(4), dp(8), 0, dp(8));
        root.addView(logTitle);
        LinearLayout logCard = card();
        incidents = text("No incidents recorded.", 13, Typeface.NORMAL, Color.DKGRAY);
        logCard.addView(incidents);
        root.addView(logCard, mp(-1,-2,8));

        TextView note = text("Control Log records patrol start/stop, incident reports and other control actions on this phone. GPS tracking continues with a foreground service while the screen is locked.", 12, Typeface.NORMAL, Color.GRAY);
        note.setPadding(dp(4), dp(8), dp(4), 0);
        root.addView(note);
        setContentView(sv);
    }

    private void togglePatrol() {
        android.content.SharedPreferences p = getSharedPreferences("patrol", MODE_PRIVATE);
        boolean active = p.getBoolean("active", false);
        if (!active) {
            if (!hasLocationPermission()) { requestNeededPermissions(); return; }
            p.edit().putFloat("distance_m",0f).putLong("started_at",System.currentTimeMillis()).apply();
            Intent i = new Intent(this, PatrolService.class).setAction(PatrolService.ACTION_START);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        } else {
            startService(new Intent(this, PatrolService.class).setAction(PatrolService.ACTION_STOP));
        }
        new Handler(Looper.getMainLooper()).postDelayed(this::refresh, 350);
    }

    private void refresh() {
        android.content.SharedPreferences p = getSharedPreferences("patrol", MODE_PRIVATE);
        boolean active = p.getBoolean("active", false);
        status.setText(active ? "●  PATROL ACTIVE" : "PATROL NOT STARTED");
        status.setTextColor(active ? GREEN : Color.DKGRAY);
        patrolButton.setText(active ? "STOP PATROL" : "START PATROL");
        patrolButton.setBackground(round(active ? RED : GREEN, 14));
        speed.setText(String.format(Locale.US, "%.0f km/h", p.getFloat("speed_kmh",0f)));
        distance.setText(String.format(Locale.US, "%.2f km", p.getFloat("distance_m",0f)/1000f));
        latitude.setText(p.getString("lat","—"));
        longitude.setText(p.getString("lon","—"));
        float a = p.getFloat("accuracy",0f);
        accuracy.setText(a > 0 ? String.format(Locale.US,"± %.0f m",a) : "—");
        long st = p.getLong("started_at",0L);
        started.setText(st > 0 ? new SimpleDateFormat("HH:mm",Locale.getDefault()).format(new Date(st)) : "—");
        long ts = p.getLong("last_ts",0L);
        lastUpdate.setText(ts > 0 ? "Last GPS update: " + new SimpleDateFormat("dd MMM yyyy, HH:mm:ss",Locale.getDefault()).format(new Date(ts)) : "Waiting for GPS…");
        String log = p.getString("incident_log", "");
        incidents.setText(log == null || log.trim().isEmpty() ? "No incidents recorded." : log);
    }

    private void showIncidentDialog() {
        final String[] categories = {"Accident","Vehicle breakdown","Road obstruction","Dead animal","Unauthorized access","Road defect","Signage / crash barrier defect","Other"};
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(6),dp(20),0);
        Spinner sp = new Spinner(this); ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories); sp.setAdapter(ad); box.addView(sp);
        EditText remarks = new EditText(this); remarks.setHint("Remarks / action taken"); remarks.setMinLines(3); remarks.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE); box.addView(remarks, mp(-1,-2,0));
        new AlertDialog.Builder(this).setTitle("Report Incident").setView(box).setNegativeButton("Cancel",null)
                .setPositiveButton("SAVE", (d,w) -> saveIncident(categories[sp.getSelectedItemPosition()], remarks.getText().toString().trim())).show();
    }

    private void saveIncident(String category, String remarks) {
        android.content.SharedPreferences p = getSharedPreferences("patrol", MODE_PRIVATE);
        String lat = p.getString("lat","—"), lon = p.getString("lon","—");
        String now = new SimpleDateFormat("dd MMM HH:mm",Locale.getDefault()).format(new Date());
        String line = now + "  •  " + category + "\nGPS: " + lat + ", " + lon + (remarks.isEmpty()?"":"\n"+remarks);
        String old = p.getString("incident_log","");
        String next = line + (old == null || old.isEmpty()?"":"\n\n"+old);
        if (next.length() > 5000) next = next.substring(0,5000);
        p.edit().putString("incident_log",next).apply();
        PatrolLog.add(this, "INCIDENT REPORTED — " + category, remarks.isEmpty() ? "No remarks" : remarks);
        Toast.makeText(this,"Incident saved to Incident Log and Control Log",Toast.LENGTH_SHORT).show();
        refresh();
    }

    private void requestNeededPermissions() {
        ArrayList<String> req = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) req.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) req.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) req.add(Manifest.permission.POST_NOTIFICATIONS);
        if (!req.isEmpty()) requestPermissions(req.toArray(new String[0]), 10);
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private LinearLayout row() { LinearLayout x = new LinearLayout(this); x.setOrientation(LinearLayout.HORIZONTAL); x.setWeightSum(2); return x; }
    private TextView metric(LinearLayout row, String label, String value) {
        LinearLayout c = card(); c.setPadding(dp(14),dp(12),dp(14),dp(12));
        TextView l = text(label,11,Typeface.BOLD,Color.GRAY); c.addView(l);
        TextView v = text(value,18,Typeface.BOLD,NAVY); c.addView(v);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f); lp.setMargins(0,0,dp(7),0); row.addView(c,lp);
        return v;
    }
    private LinearLayout card() { LinearLayout l = new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); l.setPadding(dp(16),dp(16),dp(16),dp(16)); l.setBackground(round(Color.WHITE,16)); l.setElevation(dp(2)); return l; }
    private Button actionButton(String s, int color) { Button b = new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setTypeface(Typeface.DEFAULT_BOLD); b.setAllCaps(false); b.setBackground(round(color,14)); return b; }
    private TextView text(String s, int sp, int style, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(sp); t.setTypeface(Typeface.DEFAULT,style); t.setTextColor(color); return t; }
    private GradientDrawable round(int color, int radiusDp) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radiusDp)); return g; }
    private LinearLayout.LayoutParams mp(int w,int h,int bottom) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w,h); p.setMargins(0,0,0,dp(bottom)); return p; }
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
