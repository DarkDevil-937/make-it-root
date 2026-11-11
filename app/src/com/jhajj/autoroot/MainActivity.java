package com.jhajj.autoroot;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        RootUtil.init(this);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Make it ROOT");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, pad / 2, 0, pad / 2);
        root.addView(status);

        Button rootBtn = new Button(this);
        rootBtn.setText("Root now");
        rootBtn.setOnClickListener(v -> startRoot());
        root.addView(rootBtn);

        Button checkBtn = new Button(this);
        checkBtn.setText("Check root");
        checkBtn.setOnClickListener(v -> refresh());
        root.addView(checkBtn);

        Button stopBtn = new Button(this);
        stopBtn.setText("Stop root");
        stopBtn.setOnClickListener(v -> stopRoot());
        root.addView(stopBtn);

        setContentView(root);
        refresh();
    }

    private void startRoot() {
        status.setText("Rooting... (takes ~10-15s)");
        Intent i = new Intent(this, RootService.class);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(i);
        } else {
            startService(i);
        }
        for (int n = 1; n <= 15; n++) {
            status.postDelayed(this::refresh, n * 2000L);
        }
    }

    private void stopRoot() {
        status.setText("Stopping root...");
        Intent i = new Intent(this, RootService.class);
        i.setAction(RootService.ACTION_STOP);
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(i);
        } else {
            startService(i);
        }
        status.postDelayed(this::refresh, 4000);
    }

    private void refresh() {
        new Thread(() -> {
            final String s = RootUtil.checkRoot();
            runOnUiThread(() -> status.setText(s));
        }).start();
    }
}
