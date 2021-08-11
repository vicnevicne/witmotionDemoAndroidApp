package com.wtzn.wtapp;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import com.bt901.DataMonitor;
import com.bt901.db.SQLite;

public class RkActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int REQUEST_CODE_SCAN = 1;
    private static final int REQUEST_WIFI_MODULE = 3;
    private static final int TAKE_PHOTO_REQUEST_CODE = 2;

    private SQLite sqLite;
    TextView title;
    private View mContentView;

    private BluetoothAdapter mBluetoothAdapter = null;
    @RequiresApi(api = Build.VERSION_CODES.ECLAIR)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContentView = LayoutInflater.from(this).inflate(R.layout.act_rk, null);
        setContentView(mContentView);
//        ButterKnife.inject(this);
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        title = findViewById(R.id.tv_center);
        title.setText(getString(R.string.select_app));
        sqLite = SQLite.init(getApplicationContext());
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
                Toast.makeText(this, "Bluetooth dose not support!", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception err) {
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
        if (id==R.id.bt_three){
            Intent intent = new Intent(this, DataMonitor.class);
            intent.putExtra("type", 3);
            startActivity(intent);
        }else if (id == R.id.bt_six){
            Intent intent = new Intent(this, DataMonitor.class);
            intent.putExtra("type", 6);
            startActivity(intent);
        }else if (id==R.id.bt_nine){
            Intent intent = new Intent(this, DataMonitor.class);
            intent.putExtra("type", 9);
            startActivity(intent);
        }
    }

}
