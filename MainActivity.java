package com.daxware.booster;

import android.app.Activity;
import android.os.Bundle;
import android.content.pm.PackageManager;
import android.content.pm.FeatureInfo;
import android.widget.*;
import android.graphics.Color;
import rikka.shizuku.Shizuku;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends Activity {
    private TextView tvStatus, tvGpu;
    private EditText etDpi, etResolution;
    private static final int REQUEST_CODE = 100;

    private final Shizuku.OnRequestPermissionResultListener permissionListener =
        new Shizuku.OnRequestPermissionResultListener() {
            @Override public void onRequestPermissionResult(int requestCode, int grantResult) {
                if (requestCode == REQUEST_CODE) {
                    updateStatus();
                    toast(grantResult == PackageManager.PERMISSION_GRANTED
                        ? "Shizuku permission granted" : "Shizuku permission denied");
                }
            }
        };
    private final Shizuku.OnBinderReceivedListener binderListener =
        new Shizuku.OnBinderReceivedListener() {
            @Override public void onBinderReceived() {
                runOnUiThread(new Runnable() {
                    @Override public void run() { updateStatus(); }
                });
            }
        };
    private final Shizuku.OnBinderDeadListener deadListener =
        new Shizuku.OnBinderDeadListener() {
            @Override public void onBinderDead() {
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        tvStatus.setText("SHIZUKU: DISCONNECTED");
                        tvStatus.setTextColor(Color.RED);
                    }
                });
            }
        };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvStatus = findViewById(R.id.tvStatus);
        tvGpu = findViewById(R.id.tvGpu);
        etDpi = findViewById(R.id.etDpi);
        etResolution = findViewById(R.id.etResolution);

        Shizuku.addRequestPermissionResultListener(permissionListener);
        Shizuku.addBinderReceivedListener(binderListener);
        Shizuku.addBinderDeadListener(deadListener);

        findViewById(R.id.btnConnect).setOnClickListener(v -> connectShizuku());
        findViewById(R.id.btnDpi).setOnClickListener(v -> applyDpi());
        findViewById(R.id.btnResolution).setOnClickListener(v -> applyResolution());
        findViewById(R.id.btnRestore).setOnClickListener(v -> confirmRestore());
        findViewById(R.id.btnGpu).setOnClickListener(v -> checkGpu());
        findViewById(R.id.btnOptimize).setOnClickListener(v -> optimize());
        findViewById(R.id.btnRestoreAnimation).setOnClickListener(v -> restoreAnimation());
        updateStatus();
    }

    private void connectShizuku() {
        try {
            if (!Shizuku.pingBinder()) { toast("Start Shizuku first"); return; }
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                updateStatus(); toast("Already connected");
            } else Shizuku.requestPermission(REQUEST_CODE);
        } catch (Exception e) { toast("Shizuku error: " + e.getMessage()); }
    }
    private boolean hasPermission() {
        try {
            return Shizuku.pingBinder()
                && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (Exception e) { return false; }
    }
    private void updateStatus() {
        if (hasPermission()) {
            tvStatus.setText("SHIZUKU: CONNECTED ✓"); tvStatus.setTextColor(Color.GREEN);
        } else if (Shizuku.pingBinder()) {
            tvStatus.setText("SHIZUKU: PERMISSION REQUIRED"); tvStatus.setTextColor(Color.YELLOW);
        } else {
            tvStatus.setText("SHIZUKU: DISCONNECTED"); tvStatus.setTextColor(Color.RED);
        }
    }
    private void executeShell(final String command) {
        if (!hasPermission()) { toast("Connect Shizuku first"); return; }
        new Thread(new Runnable() {
            @Override public void run() {
                java.lang.Process process = null;
                try {
                    process = Shizuku.newProcess(new String[]{"sh", "-c", command}, null, null);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    while (reader.readLine() != null) { }
                    int result = process.waitFor();
                    final String message = result == 0 ? "Command completed" : "Command failed (exit " + result + ")";
                    reader.close();
                    runOnUiThread(new Runnable() {
                        @Override public void run() { toast(message); }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() { toast("Error: " + e.getMessage()); }
                    });
                } finally { if (process != null) process.destroy(); }
            }
        }).start();
    }
    private void applyDpi() {
        String value = etDpi.getText().toString().trim();
        if (!value.matches("[0-9]{3,4}")) { toast("Enter DPI between 120 and 800"); return; }
        int dpi = Integer.parseInt(value);
        if (dpi < 120 || dpi > 800) { toast("DPI must be between 120 and 800"); return; }
        executeShell("wm density " + dpi);
    }
    private void applyResolution() {
        String value = etResolution.getText().toString().trim();
        if (!value.matches("[0-9]{3,4}x[0-9]{3,4}")) { toast("Example: 1080x2400"); return; }
        String[] parts = value.split("x");
        int width = Integer.parseInt(parts[0]), height = Integer.parseInt(parts[1]);
        if (width < 320 || width > 5000 || height < 480 || height > 5000) {
            toast("Resolution outside allowed range"); return;
        }
        executeShell("wm size " + width + "x" + height);
    }
    private void confirmRestore() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Restore display")
            .setMessage("Reset DPI and resolution overrides to system defaults?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Restore", (dialog, which) ->
                executeShell("wm size reset && wm density reset")).show();
    }
    private void optimize() {
        new android.app.AlertDialog.Builder(this)
            .setTitle("DAXWARE Optimize")
            .setMessage("Set interface animation scales to 0.5x? This changes UI animation speed, not GPU power or game FPS.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Apply", (dialog, which) ->
                executeShell("settings put global window_animation_scale 0.5"
                    + " && settings put global transition_animation_scale 0.5"
                    + " && settings put global animator_duration_scale 0.5")).show();
    }
    private void restoreAnimation() {
        executeShell("settings put global window_animation_scale 1.0"
            + " && settings put global transition_animation_scale 1.0"
            + " && settings put global animator_duration_scale 1.0");
    }
    private void checkGpu() {
        FeatureInfo[] features = getPackageManager().getSystemAvailableFeatures();
        StringBuilder info = new StringBuilder("OPENGL ES\n");
        for (FeatureInfo feature : features) {
            if (feature.reqGlEsVersion != 0) {
                int version = feature.reqGlEsVersion;
                info.append("Version: ").append((version >> 16) & 0xffff)
                    .append(".").append(version & 0xffff).append("\n");
            }
        }
        info.append("\nVULKAN SUPPORT\n");
        boolean found = false;
        for (FeatureInfo feature : features) {
            if (feature.name != null && feature.name.startsWith("android.hardware.vulkan")) {
                found = true; info.append(feature.name).append("\n");
            }
        }
        if (!found) info.append("No Vulkan hardware feature reported.");
        info.append("\nRenderer switching is not performed.");
        tvGpu.setText(info.toString());
    }
    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    @Override protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        Shizuku.removeBinderReceivedListener(binderListener);
        Shizuku.removeBinderDeadListener(deadListener);
        super.onDestroy();
    }
}
