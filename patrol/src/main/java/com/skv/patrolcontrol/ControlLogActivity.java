package com.skv.patrolcontrol;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public class ControlLogActivity extends Activity {
    private final int NAVY = Color.rgb(11,31,51);
    private TextView logView;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(NAVY);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(16));
        root.setBackgroundColor(Color.rgb(245,247,250));

        TextView title = new TextView(this);
        title.setText("CONTROL LOG");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.WHITE);
        title.setPadding(dp(18),dp(20),dp(18),dp(20));
        title.setBackgroundColor(NAVY);
        root.addView(title, new LinearLayout.LayoutParams(-1,-2));

        TextView sub = new TextView(this);
        sub.setText("Patrol start/stop, incidents and control actions");
        sub.setTextColor(Color.DKGRAY);
        sub.setTextSize(13);
        sub.setPadding(dp(4),dp(14),dp(4),dp(10));
        root.addView(sub);

        ScrollView scroll = new ScrollView(this);
        logView = new TextView(this);
        logView.setTextSize(13);
        logView.setTextColor(Color.rgb(30,40,52));
        logView.setTextIsSelectable(true);
        logView.setPadding(dp(16),dp(16),dp(16),dp(16));
        logView.setBackground(round(Color.WHITE, 14));
        scroll.addView(logView, new ScrollView.LayoutParams(-1,-2));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1,0,1f);
        root.addView(scroll, slp);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0,dp(10),0,0);

        Button back = button("BACK", NAVY);
        back.setOnClickListener(v -> finish());
        actions.addView(back, weighted());

        Button share = button("SHARE LOG", Color.rgb(22,125,163));
        share.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "Patrol Control Log");
            i.putExtra(Intent.EXTRA_TEXT, PatrolLog.get(this));
            startActivity(Intent.createChooser(i, "Share control log"));
        });
        actions.addView(share, weighted());

        Button clear = button("CLEAR", Color.rgb(180,35,24));
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Clear control log?")
                .setMessage("This removes the stored control log from this phone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (d,w) -> {
                    getSharedPreferences("patrol", MODE_PRIVATE).edit().remove("control_log").apply();
                    refresh();
                }).show());
        actions.addView(clear, weighted());

        root.addView(actions, new LinearLayout.LayoutParams(-1,-2));
        setContentView(root);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private void refresh() { if (logView != null) logView.setText(PatrolLog.get(this)); }

    private Button button(String label, int color) {
        Button b = new Button(this);
        b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(12);
        b.setTypeface(Typeface.DEFAULT_BOLD); b.setAllCaps(false);
        b.setBackground(round(color,12));
        return b;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(50), 1f);
        p.setMargins(dp(3),0,dp(3),0);
        return p;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radiusDp)); return g;
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
