package com.wtzn.wtapp;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.bt901.DataMonitorActivity;
import com.bt901.db.SQLite;

public class DeviceSelectionActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String TAG = DeviceSelectionActivity.class.getName();

    TextView title;

    private BluetoothAdapter mBluetoothAdapter = null;

    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View mContentView = LayoutInflater.from(this).inflate(R.layout.device_selection_activity, null);
        setContentView(mContentView);
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        title = findViewById(R.id.title_text);
        title.setText(getString(R.string.select_module));
        SQLite sqLite = SQLite.init(getApplicationContext());
        sqLite.open();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
            }
            if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3);
            }
        }

        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
                return;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
        }
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
        findViewById(R.id.bt_three).setOnClickListener(this);
        findViewById(R.id.bt_six).setOnClickListener(this);
        findViewById(R.id.bt_nine).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.bt_three) {
            Intent intent = new Intent(this, DataMonitorActivity.class);
            intent.putExtra("type", 3);
            startActivity(intent);
        }
        else if (id == R.id.bt_six) {
            Intent intent = new Intent(this, DataMonitorActivity.class);
            intent.putExtra("type", 6);
            startActivity(intent);
        }
        else if (id == R.id.bt_nine) {
            Intent intent = new Intent(this, DataMonitorActivity.class);
            intent.putExtra("type", 9);
            startActivity(intent);
        }
    }

}
