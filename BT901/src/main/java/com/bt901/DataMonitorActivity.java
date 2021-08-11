package com.bt901;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.usb.UsbManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import com.bt901.bluetooth.BluetoothService;
import com.bt901.dialog.AddressDialog;
import com.bt901.dialog.AlarmDialog;
import com.bt901.dialog.SmoothingDialog;
import com.bt901.dialog.PwmCycleDialog;
import com.bt901.dialog.PwmDialog;
import com.github.mikephil.charting.charts.LineChart;
import com.wtzn.wtfile.util.MyFile;
import com.wtzn.wtfile.util.SharedUtil;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import cn.wch.ch34xuartdriver.CH34xUARTDriver;

@SuppressWarnings("ALL")
@SuppressLint("DefaultLocale")
public class DataMonitorActivity extends FragmentActivity implements OnClickListener {
    public static final String TAG = DataMonitorActivity.class.getName();

    // Index of tabs
    public static final int TAB_SYSTEM = 0;
    public static final int TAB_ACCELERATION = 1;
    public static final int TAB_ANGULAR_VELOCITY = 2;
    public static final int TAB_ANGLE = 3;
    public static final int TAB_MAGNETIC_FIELD = 4;
    public static final int TAB_PORT = 5;
    public static final int TAB_PRESSURE = 6;
    public static final int TAB_LOCATION = 7;
    public static final int TAB_GPS_VELOCITY = 8;
    public static final int TAB_QUATERNION = 9;
    public static final int TAB_SATELLITE_NUMBER = 10;

    public static final int UNSELECTED_BACKGROUND_COLOR = 0xff33b5e5;
    public static final int SELECTED_BACKGROUND_COLOR = 0xff0099cc;

    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    private static final int REQUEST_CONNECT_DEVICE = 1;

    public static final int RECORDING_STOPPED = -1;
    public static final int RECORDING_STOP_REQUESTED = 0;
    public static final int RECORDING_START_REQUESTED = 1;
    public static final int RECORDING_STARTED = 2;
    public static final int MESSAGE_START_BYTE = 0x55;

    private static int sensorTypeNumaxis;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private String mConnectedDeviceName = null;
    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";
    private Button bluetoothScanButton;
    private boolean recordStartorStop = false;
    public byte[] writeBuffer;
    public byte[] readBuffer;
    private boolean isOpen;
    static MyFile myFile;
    DrawerLayout drawerLayout;
    ExLisViewAdapter adapter;
    private Switch outputSwitch;
    List<MenuGroup> groupList = new ArrayList<>();
    private static int ar = 16, av = 2000;
    private static final float[] ac = new float[]{0, 0, 0};
    private static final float[] w = new float[]{0, 0, 0};
    private static final float[] h = new float[]{0, 0, 0};
    private static final float[] angle = new float[]{0, 0, 0};
    private static final float[] d = new float[]{0, 0, 0, 0};
    private static final float[] q = new float[]{0, 0, 0, 0};
    private static float T = 20;
    private static float pressure, height, longitude, latitude, altitude, yaw, velocity, sn, pdop, hdop, vdop, voltage, version;
    private static short fieldsToSaveBitmap = 0;
    private static short currentFieldsBitmap;
    private static int recordingState = RECORDING_STOPPED;
    private static int sDataSave = 0;
    static int currentTab = TAB_ANGLE;
    private static String strDate = "", strTime = "";
    private boolean bBTConnet = false;
    private LineChart lineChart;
    private LineChartManager lineChartManager;
    private final List<Integer> qColour = new ArrayList<>(Arrays.asList(Color.RED, Color.GREEN, Color.BLUE, Color.GRAY)); //Polyline color collection

    private float norm(float x[]) {
        return (float) Math.sqrt(x[0] * x[0] + x[1] * x[1] + x[2] * x[2]);
    }

    private TextView tvLabelX, tvLabelY, tvLabelZ, tvLabelAll, tvX, tvY, tvZ, tvAll;

    public void setTableName(String str1, String str2, String str3, String str4) {
        tvLabelX.setText(str1);
        tvLabelY.setText(str2);
        tvLabelZ.setText(str3);
        tvLabelAll.setText(str4);
    }

    public void setTableData(String str1, String str2, String str3, String str4) {
        tvX.setText(str1);
        tvY.setText(str2);
        tvZ.setText(str3);
        tvAll.setText(str4);
    }

    public void setTableData(String format, Object d1, Object d2, Object d3, Object d4) {
        setTableData(String.format(format, d1), String.format(format, d2), String.format(format, d3), String.format(format, d4));
    }

    static float fTempT;
    static int iError = 0;
    static Queue<Byte> queueBuffer = new LinkedList<>();
    static boolean[] hasPendingUpdate = new boolean[20];

    public static void handleSerialData(int acceptedLen, byte[] tempInputBuffer) {
        byte[] dataBuffer = new byte[11];
        byte fieldTypeByte;
        float fTemp;
        for (int i = 0; i < acceptedLen; i++) {
            queueBuffer.add(tempInputBuffer[i]);// The data read from the buffer is stored in the queue
        }
        while (queueBuffer.size() >= 11) {
            // Decode message.
            // Format is: 0x55 (MESSAGE_START) + header byte (data_type) + data buffer (8 bytes)
            // Note: peek() returns to the first item but does not delete it. poll() removes and returns
            if ((queueBuffer.poll()) != MESSAGE_START_BYTE) {
                iError++;
                continue;
            }
            // First byte is a header indicating the type of data received
            fieldTypeByte = queueBuffer.poll();
            if ((fieldTypeByte & 0xF0) == 0x50) {
                // OK, this is a valid data type. Reset error count.
                iError = 0;
            }

            // Copy the 8 data bytes in the dataBuffer
            for (int j = 0; j < 9; j++) {
                dataBuffer[j] = queueBuffer.poll();
            }

            // Check message validity (checksum)
            byte checksum = (byte) (MESSAGE_START_BYTE + fieldTypeByte);
            for (int i = 0; i < 8; i++) {
                checksum = (byte) (checksum + dataBuffer[i]);
            }
            if (checksum != dataBuffer[8]) {
                Log.e(TAG, String.format("handleSerialData: %2x %2x %2x %2x %2x %2x %2x %2x %2x SUM:%2x %2x", fieldTypeByte, dataBuffer[0], dataBuffer[1], dataBuffer[2], dataBuffer[3], dataBuffer[4], dataBuffer[5], dataBuffer[6], dataBuffer[7], dataBuffer[8], checksum));
                continue;
            }

            // Interpret message
            switch (fieldTypeByte) {
                case 0x50: // Time
                    int ms = ((((short) dataBuffer[7]) << 8) | ((short) dataBuffer[6] & 0xff));
                    strDate = String.format("20%02d-%02d-%02d", dataBuffer[0], dataBuffer[1], dataBuffer[2]);
                    strTime = String.format("%02d:%02d:%02d.%03d", dataBuffer[3], dataBuffer[4], dataBuffer[5], ms);
                    break;

                case 0x51:
                    if (SharedUtil.getInt("ar") != -1) {
                        ar = SharedUtil.getInt("ar");
                    }
                    // ac[3], 16-bit each
                    for (int i = 0; i < 3; i++) {
                        ac[i] = ((((short) dataBuffer[i * 2 + 1]) << 8) | ((short) dataBuffer[i * 2] & 0xff)) / 32768.0f * ar;
                    }
                    // temperature, 16-bit too
                    fTempT = ((((short) dataBuffer[7]) << 8) | ((short) dataBuffer[6] & 0xff)) / 100.0f;
                    if (sensorTypeNumaxis == 6) {
                        T = (float) (fTempT / 340 + 36.53);
                    }
                    else {
                        T = fTempT;
                    }
                    break;

                case 0x52: // Angular velocity
                    if (SharedUtil.getInt("av") != -1) {
                        av = SharedUtil.getInt("av");
                    }
                    // w[3], 16-bit each
                    for (int i = 0; i < 3; i++) {
                        w[i] = ((((short) dataBuffer[i * 2 + 1]) << 8) | ((short) dataBuffer[i * 2] & 0xff)) / 32768.0f * av;
                    }
                    // voltage, 16-bit too
                    fTemp = ((((short) dataBuffer[7]) << 8) | ((short) dataBuffer[6] & 0xff)) / 100.0f;
                    if (fTemp != fTempT) {
                        voltage = fTemp;
                    }
                    else {
                        voltage = 0;
                    }
                    break;

                case 0x53: // Angle
                    // angle[3], 16-bit each
                    for (int i = 0; i < 3; i++) {
                        angle[i] = ((((short) dataBuffer[i * 2 + 1]) << 8) | ((short) dataBuffer[i * 2] & 0xff)) / 32768.0f * 180;
                    }
                    // version, 16-bit too
                    fTemp = ((((short) dataBuffer[7]) << 8) | ((short) dataBuffer[6] & 0xff)) / 100.0f;
                    if (fTemp != fTempT) {
                        version = fTemp * 100;
                    }
                    else {
                        version = 0;
                    }
                    break;

                case 0x54: // Magnetic field
                    // h[3], 16-bit each
                    for (int i = 0; i < 3; i++) {
                        h[i] = ((((short) dataBuffer[i * 2 + 1]) << 8) | ((short) dataBuffer[i * 2] & 0xff));
                    }
                    break;

                case 0x55: // port
                    // d[4], 16-bit each
                    for (int i = 0; i < 4; i++) {
                        d[i] = ((((short) dataBuffer[i * 2 + 1]) << 8) | ((short) dataBuffer[i * 2] & 0xff));
                    }
                    break;

                case 0x56: // Air pressure, height
                    // pressure, 32-bit
                    pressure = ((((long) dataBuffer[3]) << 24) & 0xff000000) | ((((long) dataBuffer[2]) << 16) & 0xff0000) | ((((long) dataBuffer[1]) << 8) & 0xff00) | ((((long) dataBuffer[0]) & 0xff));
                    // altitude, 32-bit
                    height = (((((long) dataBuffer[7]) << 24) & 0xff000000) | ((((long) dataBuffer[6]) << 16) & 0xff0000) | ((((long) dataBuffer[5]) << 8) & 0xff00) | ((((long) dataBuffer[4]) & 0xff))) / 100.0f;
                    break;

                case 0x57: // Latitude and longitude
                    // longitude, 32-bit
                    long binLongitude = ((((long) dataBuffer[3]) << 24) & 0xff000000) | ((((long) dataBuffer[2]) << 16) & 0xff0000) | ((((long) dataBuffer[1]) << 8) & 0xff00) | ((((long) dataBuffer[0]) & 0xff));
                    longitude = (float) (binLongitude / 10000000 + ((float) (binLongitude % 10000000) / 100000.0 / 60.0));
                    // latitude, 32-bit
                    long binLatitude = (((((long) dataBuffer[7]) << 24) & 0xff000000) | ((((long) dataBuffer[6]) << 16) & 0xff0000) | ((((long) dataBuffer[5]) << 8) & 0xff00) | ((((long) dataBuffer[4]) & 0xff)));
                    latitude = (float) (binLatitude / 10000000 + ((float) (binLatitude % 10000000) / 100000.0 / 60.0));
                    break;

                case 0x58: // Altitude, heading, ground speed
                    altitude = (float) ((((short) dataBuffer[1]) << 8) | ((short) dataBuffer[0] & 0xff)) / 10;
                    yaw = (float) ((((short) dataBuffer[3]) << 8) | ((short) dataBuffer[2] & 0xff)) / 100;
                    velocity = (float) (((((long) dataBuffer[7]) << 24) & 0xff000000) | ((((long) dataBuffer[6]) << 16) & 0xff0000) | ((((long) dataBuffer[5]) << 8) & 0xff00) | ((((long) dataBuffer[4]) & 0xff))) / 1000;
                    break;

                case 0x59: // Quaternion
                    // q[4], 16-bit each
                    for (int i = 0; i < 4; i++) {
                        q[i] = ((((short) dataBuffer[i * 2 + 1]) << 8) | ((short) dataBuffer[i * 2] & 0xff)) / 32768.0f;
                    }
                    break;

                case 0x5a: // Number of satellites
                    sn = ((((short) dataBuffer[1]) << 8) | ((short) dataBuffer[0] & 0xff));
                    pdop = ((((short) dataBuffer[3]) << 8) | ((short) dataBuffer[2] & 0xff)) / 100.0f;
                    hdop = ((((short) dataBuffer[5]) << 8) | ((short) dataBuffer[4] & 0xff)) / 100.0f;
                    vdop = ((((short) dataBuffer[7]) << 8) | ((short) dataBuffer[6] & 0xff)) / 100.0f;
                    break;
            } //switch

            if ((fieldTypeByte >= 0x50) && (fieldTypeByte <= 0x5a)) {
                recordData(fieldTypeByte);
                int fieldTypeId = fieldTypeByte - 0x50;
                hasPendingUpdate[fieldTypeId] = true;
            }
        }
    }

    private int byteToInt(byte byteL, byte byteH) {
        return (byteH << 8) | byteL;
    }

    private void writeReg(final int address, final int data, int delayMs) {
        //if(mBluetoothService==null) return;
        if (delayMs == 0) {
            sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) address, (byte) (data & 0xff), (byte) ((data >> 8) & 0xff)});
        }
        else {
            new Handler().postDelayed(
                    () -> sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) address, (byte) (data & 0xff), (byte) ((data >> 8) & 0xff)}),
                    delayMs
            );
        }
    }

    private void writeReg(int addr, int data) {
        writeReg(addr, data, 0);
    }

    private void unLockReg(int delayMs) {
        writeReg(0x69, 0xb588, delayMs);//unlock
    }

    private void saveReg(int delayMs) {
        writeReg(0x00, 0x00, delayMs);//unlock
    }

    private void writeLockReg(int addr, int data) {
        unLockReg(0);//unlock
        writeReg(addr, data, 50);//write Reg
    }

    private void writeAndSaveReg(int addr, int data) {
        unLockReg(0);//unlock
        writeReg(addr, data, 50);//write Reg
        saveReg(100);//save
    }

    private void highlightCurrentTab(View v) {
        (findViewById(R.id.systemTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.accelerationTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.angularVelocityTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.angleTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.magneticFieldTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.portTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.pressureTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.locationTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.gpsVelocityTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.quaternionTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        (findViewById(R.id.satelliteNumberTabBtn)).setBackgroundColor(UNSELECTED_BACKGROUND_COLOR);
        v.setBackgroundColor(SELECTED_BACKGROUND_COLOR);
    }

    public void onOutputSwitchClick(View v) {
        Log.e(TAG, "onOutputSwitchClick: " + String.format("Output:0x%x", getOutputInt()));
        if (sensorTypeNumaxis == 9) {
            outputPackage[currentTab] = outputSwitch.isChecked();
            int outputContent = getOutputInt();
            writeAndSaveReg(0x02, outputContent);
            SharedUtil.putInt("Out", outputContent);
        }
    }

    public void onTabBtnClick(View v) {
        lineChartManager.setbPause(true);
        int i = v.getId();
        highlightCurrentTab(v);
        if (i == R.id.systemTabBtn) {
            currentTab = TAB_SYSTEM;
            setTableName(getString(R.string.version), getString(R.string.voltage), getString(R.string.date), getString(R.string.time));
            Log.i(TAG, "Voltage:" + getString(R.string.voltage));
            setTableData("1.0", "3.3V", "2020-1-1", "00:00:00.0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX", "AngleY", "AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_chart));
            if (sensorTypeNumaxis == 9) {
                unLockReg(0);
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH) + 1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);
                writeReg(0x30, byteToInt((byte) (year - 2000), (byte) month), 50);
                writeReg(0x31, byteToInt((byte) day, (byte) hour), 100);
                writeReg(0x32, byteToInt((byte) minute, (byte) second), 150);
            }
        }
        else if (i == R.id.accelerationTabBtn) {
            currentTab = TAB_ACCELERATION;
            setTableName("ax:", "ay:", "az:", "|a|");
            setTableData("0", "0", "0", "0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("ax", "ay", "az"), qColour);
            lineChartManager.setDescription(getString(R.string.acc_chart));
        }
        else if (i == R.id.angularVelocityTabBtn) {
            currentTab = TAB_ANGULAR_VELOCITY;
            setTableName("wx:", "wy:", "wz:", "|w|");
            setTableData("0", "0", "0", "0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("wx", "wy", "wz"), qColour);
            lineChartManager.setDescription(getString(R.string.w_chart));
        }
        else if (i == R.id.angleTabBtn) {
            currentTab = TAB_ANGLE;
            setTableName("AngleX:", "AngleY:", "AngleZ:", "T:");
            setTableData("0", "0", "0", "25℃");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX", "AngleY", "AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_chart));
        }
        else if (i == R.id.magneticFieldTabBtn) {
            currentTab = TAB_MAGNETIC_FIELD;
            setTableName("hx:", "hy:", "hz:", "|h|");
            setTableData("0", "0", "0", "0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("hx", "hy", "hz"), qColour);
            lineChartManager.setDescription(getString(R.string.mag_chart));
        }
        else if (i == R.id.portTabBtn) {
            currentTab = TAB_PORT;
            setTableName("D0:", "D1:", "D2:", "D3:");
            setTableData("0", "0", "0", "0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("D0", "D1", "D2", "D3"), qColour);
            lineChartManager.setDescription(getString(R.string.port_chart));
        }
        else if (i == R.id.pressureTabBtn) {
            currentTab = TAB_PRESSURE;
            setTableName("Pressure:", "Altitude:", "wz:", "|w|");
            setTableData("0", "0", "0", "0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("pressure"), qColour);
            lineChartManager.setDescription(getString(R.string.pressure_chart));
        }
        else if (i == R.id.locationTabBtn) {
            currentTab = TAB_LOCATION;
            setTableName("Longitude:", "Latitude:", "", "");
            setTableData("0", "0", "", "");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX", "AngleY", "AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_chart));
        }
        else if (i == R.id.gpsVelocityTabBtn) {
            currentTab = TAB_GPS_VELOCITY;
            setTableName("Altitude:", "Yaw:", "Velocity:", "");
            setTableData("0", "0", "0", "");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX", "AngleY", "AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_chart));
        }
        else if (i == R.id.quaternionTabBtn) {
            currentTab = TAB_QUATERNION;
            setTableName("q0:", "q1:", "q2:", "q3:");
            setTableData("0", "0", "0", "0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("q0", "q1", "q2", "q3"), qColour);
            lineChartManager.setDescription(getString(R.string.quaternion_chart));
        }
        else if (i == R.id.satelliteNumberTabBtn) {
            currentTab = TAB_SATELLITE_NUMBER;
            setTableName("SN:", "PDOP:", "HDOP:", "VDOP");
            setTableData("0", "0", "0", "0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX", "AngleY", "AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_chart));
        }
        if (sensorTypeNumaxis == 9) {
            outputSwitch.setVisibility(View.VISIBLE);
            outputSwitch.setChecked(outputPackage[currentTab]);
        }
        else {
            outputSwitch.setVisibility(View.INVISIBLE);
        }

        new Handler().postDelayed(() -> lineChartManager.setbPause(false), 100);
    }

    public static void recordData(byte fieldTypeByte) {
        try {
            boolean isRepeat = false;
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date curDate = new Date(System.currentTimeMillis()); // Get the current time

            // Convert the data type byte (0x50 - 0x5a) into a mask
            // Only keep last nibble (0x0 - 0xa)
            int fieldTypeId = fieldTypeByte & 0x0f;
            // Create corresponding mask
            short fieldTypeMask = (short) (0x01 << fieldTypeId);
            // If this data bit is set in the current mask and ... ? TODO
            if (((currentFieldsBitmap & fieldTypeMask) == fieldTypeMask) && (fieldTypeMask < sDataSave)) {
                fieldsToSaveBitmap = currentFieldsBitmap;
                currentFieldsBitmap = fieldTypeMask;
                isRepeat = true;
            }
            else {
                // Add the current data type to the current list
                currentFieldsBitmap |= fieldTypeMask;
            }
            sDataSave = fieldTypeMask;

            switch (recordingState) {
                case RECORDING_STOP_REQUESTED:
                    myFile.close();
                    recordingState = RECORDING_STOPPED;
                    break;

                case RECORDING_START_REQUESTED:
                    // Create file
                    SimpleDateFormat formatterFileName = new SimpleDateFormat("MMdd_HHmmss");
                    Date curDateFileName = new Date(System.currentTimeMillis()); // Get the current time
                    myFile = new MyFile(Environment.getExternalStorageDirectory() + "/Records/Rec_" + formatterFileName.format(curDateFileName) + ".txt");
                    // Write header line
                    String s = "Start time:" + formatter.format(curDate) + "\r\n" + "Record Time:";
                    if ((fieldsToSaveBitmap & 0x01) > 0) s += " ChipTime:";
                    if ((fieldsToSaveBitmap & 0x02) > 0) s += " ax: ay: az:";
                    if ((fieldsToSaveBitmap & 0x04) > 0) s += "  wx: wy: wz:";
                    if ((fieldsToSaveBitmap & 0x08) > 0) s += "    AngleX:   AngleY:   AngleZ:";
                    if ((fieldsToSaveBitmap & 0x10) > 0) s += "   hx:   hy:   hz:";
                    if ((fieldsToSaveBitmap & 0x20) > 0) s += "d0:d1:d2:d3:";
                    if ((fieldsToSaveBitmap & 0x40) > 0) s += "    Pressure:    Height:";
                    if ((fieldsToSaveBitmap & 0x80) > 0) s += "        Longitude:        Latitude:";
                    if ((fieldsToSaveBitmap & 0x100) > 0) s += "    ALtitude:    Yaw:    Velocity:";
                    if ((fieldsToSaveBitmap & 0x200) > 0) s += "   q0:   q1:   q2:   q3:";
                    if ((fieldsToSaveBitmap & 0x400) > 0) s += "SN:PDOP: HDOP: VDOP:";
                    myFile.write(s);
                    // Switch to "recording"
                    recordingState = RECORDING_STARTED;
                    break;

                case RECORDING_STARTED:
                    if (isRepeat) {
                        myFile.write("  \r\n");
                        myFile.write(formatter.format(curDate) + " ");
                        if ((fieldsToSaveBitmap & 0x01) > 0) {
                            myFile.write(strDate + " " + strTime + " ");
                        }
                        if ((fieldsToSaveBitmap & 0x02) > 0) {
                            myFile.write(String.format("% 10.4f", ac[0]) + String.format("% 10.4f", ac[1]) + String.format("% 10.4f", ac[2]) + " ");
                        }
                        if ((fieldsToSaveBitmap & 0x04) > 0) {
                            myFile.write(String.format("% 10.4f", w[0]) + String.format("% 10.4f", w[1]) + String.format("% 10.4f", w[2]) + " ");
                        }
                        if ((fieldsToSaveBitmap & 0x08) > 0) {
                            myFile.write(String.format("% 10.4f", angle[0]) + String.format("% 10.4f", angle[1]) + String.format("% 10.4f", angle[2]));
                        }
                        if ((fieldsToSaveBitmap & 0x10) > 0) {
                            myFile.write(String.format("% 10.0f", h[0]) + String.format("% 10.0f", h[1]) + String.format("% 10.0f", h[2]));
                        }
                        if ((fieldsToSaveBitmap & 0x20) > 0) {
                            myFile.write(String.format("% 7.0f", d[0]) + String.format("% 7.0f", d[1]) + String.format("% 7.0f", d[2]) + String.format("% 7.0f", d[3]));
                        }
                        if ((fieldsToSaveBitmap & 0x40) > 0) {
                            myFile.write(String.format("% 10.0f", pressure) + String.format("% 10.2f", height));
                        }
                        if ((fieldsToSaveBitmap & 0x80) > 0) {
                            myFile.write(String.format("% 14.6f", longitude) + String.format("% 14.6f", latitude));
                        }
                        if ((fieldsToSaveBitmap & 0x100) > 0) {
                            myFile.write(String.format("% 10.4f", altitude) + String.format("% 10.2f", yaw) + String.format("% 10.2f", velocity));
                        }
                        if ((fieldsToSaveBitmap & 0x200) > 0) {
                            myFile.write(String.format("% 7.4f", q[0]) + String.format("% 7.4f", q[1]) + String.format("% 7.4f", q[2]) + String.format("% 7.4f", q[3]));
                        }
                        if ((fieldsToSaveBitmap & 0x400) > 0) {
                            myFile.write(String.format("% 5.0f", sn) + String.format("% 7.1f", pdop) + String.format("% 7.1f", hdop) + String.format("% 7.1f", vdop));
                        }
                    }
                    break;

                default:
                    break;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "recordData: ", e);
        }
    }

    public void setRecord(boolean record) {
        if (record) {
            recordingState = 1;
        }
        else {
            recordingState = 0;
        }
    }

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        // Anonymous inner class, implementing some of the Handler interface
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            bBTConnet = true;
                            initButton();
                            if (bluetoothScanButton != null) {
                                bluetoothScanButton.setText(getString(R.string.title_connected_to, mConnectedDeviceName));
                            }
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            if (bluetoothScanButton != null) {
                                bluetoothScanButton.setText(getString(R.string.title_connecting));
                            }
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            bBTConnet = false;
                            if (bluetoothScanButton != null) {
                                bluetoothScanButton.setText(getString(R.string.title_not_connected));
                            }
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    break;
                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString("device_name");
                    Toast.makeText(getApplicationContext(), getString(R.string.title_connected_to, mConnectedDeviceName), Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    int iBaudJY61Select = 1;
    final int[] baud = new int[]{4800, 9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600};

    private void usbBaudrateInit() {
        if (sensorTypeNumaxis == 9) {
            iBaudJY901Select = SharedUtil.getInt("JY901BAUD");
            if ((iBaudJY901Select > 0) && (iBaudJY901Select < 9)) {
                iBaud = baud[iBaudJY901Select];
            }
            else {
                iBaud = 9600;
            }
            setBaudrate(iBaud);
            selectJY901Baudrate();
        }
        else {
            iBaudJY61Select = SharedUtil.getInt("JY61BAUD");
            if (iBaudJY61Select == 0) {
                iBaud = 9600;
            }
            else {
                iBaud = 115200;
            }
            setBaudrate(iBaud);
            selectJY61Baudrate();
        }
    }

    private void selectJY61Baudrate() {
        String[] s = new String[]{"9600", "115200"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_baud_rate))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iBaudJY61Select, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iBaudJY61Select = i;
                    }
                })
                .setPositiveButton(getString(R.string.end), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        SharedUtil.putInt("JY61BAUD", iBaudJY61Select);
                        if (iBaudJY61Select == 0) {
                            iBaud = 9600;
                        }
                        else {
                            iBaud = 115200;
                        }
                        setBaudrate(iBaud);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int iBaudJY901Select = 5;

    private void selectJY901Baudrate() {
        String[] s = new String[]{"4800", "9600", "19200", "38400", "57600", "115200", "230400", "460800", "921600"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_baud_rate))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iBaudJY901Select, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iBaudJY901Select = i;
                    }
                })
                .setPositiveButton(getString(R.string.end), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        SharedUtil.putInt("JY901BAUD", iBaudJY901Select);
                        iBaud = baud[iBaudJY901Select];
                        setBaudrate(iBaud);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private int iBaud = 9600;

    public void setBaudrate(int iBaudrate) {
        iBaud = iBaudrate;
        SharedUtil.putInt("Baud", iBaudrate);
        MyApp.driver.SetConfig(iBaud, (byte) 8, (byte) 0, (byte) 0, (byte) 0);
    }

    private final Handler refreshHandler = new Handler(Looper.getMainLooper()) {
        public void handleMessage(Message msg) {
            if (bPause) return;
            // If there is no pending update for this tab, exit
            if (!hasPendingUpdate[currentTab]) return;

            hasPendingUpdate[currentTab] = false;
            switch (currentTab) {
                case TAB_SYSTEM:
                    ((TextView) findViewById(R.id.tvZ)).setText(strDate);
                    ((TextView) findViewById(R.id.tvAll)).setText(strTime);
                    ((TextView) findViewById(R.id.tvY)).setText(String.format("%10.2fV", voltage));
                    ((TextView) findViewById(R.id.tvX)).setText(String.format("% 10.0f", version));
                    break;

                case TAB_ACCELERATION:
                    setTableData("% 10.4fg", ac[0], ac[1], ac[2], norm(ac));
                    // Log.d("--",String.format("acc:% 10.2fg,% 10.2fg,% 10.2fg,% 10.2fg", ac[0], ac[1], ac[2], norm(ac)));
                    lineChartManager.addEntry(Arrays.asList(ac[0], ac[1], ac[2]));
                    break;

                case TAB_ANGULAR_VELOCITY:
                    setTableData("% 10.4f°/s", w[0], w[1], w[2], norm(w));
                    //  Log.d("--", String.format("axw:% 10.2f,% 10.2f,% 10.2f,% 10.2f", w[0], w[1], w[2], norm(w)));
                    lineChartManager.addEntry(Arrays.asList(w[0], w[1], w[2]));
                    break;

                case TAB_ANGLE:
                    setTableData(String.format("%10.4f°", angle[0]), String.format("%10.4f°", angle[1]), String.format("%10.4f°", angle[2]), String.format("%10.2f℃", T));
                    break;

                case TAB_MAGNETIC_FIELD:
                    setTableData("% 10.0f", h[0], h[1], h[2], norm(h));
                    lineChartManager.addEntry(Arrays.asList(h[0], h[1], h[2]));
                    break;

                case TAB_PORT:
                    setTableData("% 10.0f", d[0], d[1], d[2], d[3]);
                    lineChartManager.addEntry(Arrays.asList(d[0], d[1], d[2], d[3]));
                    break;

                case TAB_PRESSURE: // Air pressure, altitude
                    setTableData(String.format("% 10.2fPa", pressure), String.format("% 10.2fPa", height), "", "");
                    lineChartManager.addEntry(Arrays.asList(pressure));
                    break;

                case TAB_LOCATION: // Latitude and longitude
                    setTableData(String.format("% 14.6f°", longitude), String.format("% 14.6f°", latitude), "", "");
                    break;

                case TAB_GPS_VELOCITY: // Altitude, heading, ground speed
                    setTableData(String.format("% 10.2f", altitude), String.format("% 10.2f°", yaw), String.format("% 10.2fkm/s", velocity), "");
                    break;

                case TAB_QUATERNION: // Quaternion
                    setTableData("% 7.4f", q[0], q[1], q[2], q[3]);
                    lineChartManager.addEntry(Arrays.asList(q[0], q[1], q[2], q[3]));
                    break;

                case TAB_SATELLITE_NUMBER: // Number of satellites
                    setTableData(String.format("% 5.0f", sn), String.format("% 7.1f", pdop), String.format("% 7.1f", hdop), String.format("% 7.1f", vdop));
                    break;
            } // end switch

            // For tabs with no data to draw, fill chart with angle data, just to see some animation...
            if (       (currentTab == TAB_SATELLITE_NUMBER)
                    || (currentTab == TAB_GPS_VELOCITY)
                    || (currentTab == TAB_LOCATION)
                    || (currentTab == TAB_ANGLE)
                    || (currentTab == TAB_SYSTEM)
            ) {
                lineChartManager.addEntry(Arrays.asList(angle[0], angle[1], angle[2]));
            }

        }
    };

    private boolean checkGpsIsOpen() {
        boolean isOpen;
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        isOpen = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isOpen;
    }

    private void openGPSSetting() {
        if (checkGpsIsOpen()) {
            Toast.makeText(this, "true", Toast.LENGTH_SHORT).show();
        }
        else {
            new AlertDialog.Builder(this).setTitle("open GPS")
                    .setMessage("go to open")
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(DataMonitorActivity.this, "close", Toast.LENGTH_SHORT).show();
                            dialogInterface.dismiss();
                        }
                    })
                    .setPositiveButton("setting", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent, GPS_REQUEST_CODE);
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    private static final int GPS_REQUEST_CODE = 2;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        setContentView(R.layout.lay_data);
        SharedUtil.init(getApplicationContext());
        setOutputBoolean(SharedUtil.getInt("Out"));

        openGPSSetting();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
            }
            if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 3);
            }
        }

        Intent intent = getIntent();
        sensorTypeNumaxis = intent.getIntExtra("type", 0);
        MyApp.driver = new CH34xUARTDriver((UsbManager) getSystemService(Context.USB_SERVICE), this, ACTION_USB_PERMISSION);
        // Determine whether the system supports USB HOST
        if (!MyApp.driver.UsbFeatureSupported()) {
            Dialog dialog = new AlertDialog.Builder(DataMonitorActivity.this)
                    .setTitle(getString(R.string.hint))
                    .setMessage(getString(R.string.USB_HOST))
                    .setPositiveButton(getString(R.string.ok), (arg0, arg1) -> System.exit(0)).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // Keep the screen always on

        writeBuffer = new byte[512];
        readBuffer = new byte[512];
        isOpen = false;
        serialPortOpen();
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Toast.makeText(this, getString(R.string.bluetooth_bad), Toast.LENGTH_LONG).show();
                return;
            }
        }
        catch (Exception e) {
            Log.e(TAG, "onCreate: ", e);
        }

        if (displayThread == null) {
            bDisplay = true;
            displayThread = new Thread(() -> {
                while (bDisplay) {
                    refreshHandler.sendMessage(Message.obtain());
                    try {
                        Thread.sleep(100);
                    }
                    catch (Exception ignored) {
                    }
                }
            });
            displayThread.start();
        }
    }

    private boolean bDisplay = true;
    private Thread displayThread;

    // Warning : menu actions are triggered by position!
    // Do not change menu order without reflecting the change in onGroupClick()/onChildClick()
    private void initButton() {
        // Side menu
        if (!groupList.isEmpty()) groupList.clear();
        ExpandableListView listview = findViewById(R.id.expandableLisView);
        drawerLayout = findViewById(R.id.drawerLayout);
        List<MenuItem> menuItemList = new ArrayList<>();

        bluetoothScanButton = findViewById(R.id.bluetoothScanButton);
        tvLabelX = findViewById(R.id.X);
        tvLabelY = findViewById(R.id.Y);
        tvLabelZ = findViewById(R.id.Z);
        tvLabelAll = findViewById(R.id.all);
        tvX = findViewById(R.id.tvX);
        tvY = findViewById(R.id.tvY);
        tvZ = findViewById(R.id.tvZ);
        tvAll = findViewById(R.id.tvAll);

        if (sensorTypeNumaxis == 3) {
            // Hide most tabs
            findViewById(R.id.angularVelocityTabBtn).setVisibility(View.GONE);
            findViewById(R.id.magneticFieldTabBtn).setVisibility(View.GONE);
            findViewById(R.id.portTabBtn).setVisibility(View.GONE);
            findViewById(R.id.pressureTabBtn).setVisibility(View.GONE);
            findViewById(R.id.locationTabBtn).setVisibility(View.GONE);
            findViewById(R.id.gpsVelocityTabBtn).setVisibility(View.GONE);
            findViewById(R.id.quaternionTabBtn).setVisibility(View.GONE);
            findViewById(R.id.satelliteNumberTabBtn).setVisibility(View.GONE);

            // Menu is only made of 2 top-level entries
            MenuGroup group = new MenuGroup();
            group.setName(getString(R.string.acc_calibration));
            group.setChildList(menuItemList);
            MenuGroup group2 = new MenuGroup();
            group2.setName(getString(R.string.smoothing_factor));
            group2.setChildList(menuItemList);
            groupList.add(group);
            groupList.add(group2);
        }
        else if (sensorTypeNumaxis == 6) {
            // Hide most tabs
            findViewById(R.id.magneticFieldTabBtn).setVisibility(View.GONE);
            findViewById(R.id.portTabBtn).setVisibility(View.GONE);
            findViewById(R.id.pressureTabBtn).setVisibility(View.GONE);
            findViewById(R.id.locationTabBtn).setVisibility(View.GONE);
            findViewById(R.id.gpsVelocityTabBtn).setVisibility(View.GONE);
            findViewById(R.id.quaternionTabBtn).setVisibility(View.GONE);
            findViewById(R.id.satelliteNumberTabBtn).setVisibility(View.GONE);

            // Menu is only made of 7 top-level entries
            MenuGroup group = new MenuGroup();
            group.setName(getString(R.string.acc_calibration));
            group.setChildList(menuItemList);
            groupList.add(group);
            MenuGroup group2 = new MenuGroup();
            group2.setName(getString(R.string.dormancy));
            group2.setChildList(menuItemList);
            groupList.add(group2);
            MenuGroup group3 = new MenuGroup();
            group3.setName(getString(R.string.reset_Z_axis));
            group3.setChildList(menuItemList);
            groupList.add(group3);
            MenuGroup group4 = new MenuGroup();
            if (bUsbConnect) {
                group4.setName(getString(R.string.baudrate));
            }
            else {
                group4.setName(getString(R.string.retrieval_rate));
            }
            group4.setChildList(menuItemList);
            groupList.add(group4);
            MenuGroup group5 = new MenuGroup();
            group5.setName(getString(R.string.installation_orientation));
            group5.setChildList(menuItemList);
            groupList.add(group5);
            MenuGroup group6 = new MenuGroup();
            group6.setName(getString(R.string.static_detection_threshold));
            group6.setChildList(menuItemList);
            groupList.add(group6);
            MenuGroup group7 = new MenuGroup();
            group7.setName(getString(R.string.measurement_bandwidth));
            group7.setChildList(menuItemList);
            groupList.add(group7);
        }
        else if (sensorTypeNumaxis == 9 || isOpen) {
            // Hide some tabs
            if (bBTConnet) {
                findViewById(R.id.portTabBtn).setVisibility(View.GONE);
                findViewById(R.id.locationTabBtn).setVisibility(View.GONE);
                findViewById(R.id.gpsVelocityTabBtn).setVisibility(View.GONE);
                findViewById(R.id.satelliteNumberTabBtn).setVisibility(View.GONE);
            }
            else {
                findViewById(R.id.portTabBtn).setVisibility(View.VISIBLE);
                findViewById(R.id.locationTabBtn).setVisibility(View.VISIBLE);
                findViewById(R.id.gpsVelocityTabBtn).setVisibility(View.VISIBLE);
                findViewById(R.id.satelliteNumberTabBtn).setVisibility(View.VISIBLE);
            }

            // Menu is a full-fledged two-level structure
            MenuGroup systemMenu = new MenuGroup();
            systemMenu.setName(getString(R.string.system));
            List<MenuItem> sysList = new ArrayList<>();
            sysList.add(new MenuItem(getString(R.string.factory_reset)));
            sysList.add(new MenuItem(getString(R.string.dormancy)));
            sysList.add(new MenuItem(getString(R.string.algorithm)));
            sysList.add(new MenuItem(getString(R.string.installation_orientation)));
            sysList.add(new MenuItem(getString(R.string.__instruction_start)));
            sysList.add(new MenuItem(getString(R.string.alarm)));
            systemMenu.setChildList(sysList);
            groupList.add(systemMenu);

            MenuGroup calibrationMenu = new MenuGroup();
            calibrationMenu.setName(getString(R.string.calibration));
            List<MenuItem> cbList = new ArrayList<>();
            cbList.add(new MenuItem(getString(R.string.acc_calibration)));
            cbList.add(new MenuItem(getString(R.string.magnetic_field_calibration_start)));
            cbList.add(new MenuItem(getString(R.string.magnetic_field_calibration_end)));
            cbList.add(new MenuItem(getString(R.string.reset_height)));
            cbList.add(new MenuItem(getString(R.string.gyroscope_automatic_calibration)));
            cbList.add(new MenuItem(getString(R.string.Z_axis_angle_to_zero)));
            cbList.add(new MenuItem(getString(R.string.setting_angle_reference)));
            calibrationMenu.setChildList(cbList);
            groupList.add(calibrationMenu);

            MenuGroup rangeMenu = new MenuGroup();
            rangeMenu.setName(getString(R.string.range));
            List<MenuItem> spcopeList = new ArrayList<>();
            spcopeList.add(new MenuItem(getString(R.string.acceleration_range)));
            spcopeList.add(new MenuItem(getString(R.string.angular_velocity_range)));
            spcopeList.add(new MenuItem(getString(R.string.bandwidth)));
            rangeMenu.setChildList(spcopeList);
            groupList.add(rangeMenu);

            MenuGroup communicationMenu = new MenuGroup();
            communicationMenu.setName(getString(R.string.signal_communication));
            List<MenuItem> comList = new ArrayList<>();
            comList.add(new MenuItem(getString(R.string.retrieval_rate)));
            comList.add(new MenuItem(getString(R.string.address)));
            if (isOpen) {
                comList.add(new MenuItem(getString(R.string.communication_rate)));
            }
            communicationMenu.setChildList(comList);
            groupList.add(communicationMenu);
            if (bUsbConnect) {
                // Port mode
                MenuGroup port = new MenuGroup();
                port.setName(getString(R.string.port_mode));
                List<MenuItem> portList = new ArrayList<>();
                portList.add(new MenuItem(getString(R.string.d0_mode)));
                portList.add(new MenuItem(getString(R.string.d1_mode)));
                portList.add(new MenuItem(getString(R.string.d2_mode)));
                portList.add(new MenuItem(getString(R.string.d3_mode)));
                port.setChildList(portList);
                groupList.add(port);

                // Port PWM pulse width
                MenuGroup pwm = new MenuGroup();
                pwm.setName(getString(R.string.port_PWM_pulse_width));
                List<MenuItem> pwmList = new ArrayList<>();
                pwmList.add(new MenuItem(getString(R.string.D0PWM_pulse_width)));
                pwmList.add(new MenuItem(getString(R.string.D1PWM_pulse_width)));
                pwmList.add(new MenuItem(getString(R.string.D2PWM_pulse_width)));
                pwmList.add(new MenuItem(getString(R.string.D3PWM_pulse_width)));
                pwm.setChildList(pwmList);
                groupList.add(pwm);

                // Port PWM cycle
                MenuGroup cycle = new MenuGroup();
                cycle.setName(getString(R.string.port_PWM_cycle));
                List<MenuItem> cycleList = new ArrayList<>();
                cycleList.add(new MenuItem(getString(R.string.D0PWM_cycle)));
                cycleList.add(new MenuItem(getString(R.string.D1PWM_cycle)));
                cycleList.add(new MenuItem(getString(R.string.D2PWM_cycle)));
                cycleList.add(new MenuItem(getString(R.string.D3PWM_cycle)));
                cycle.setChildList(cycleList);
                groupList.add(cycle);
            }
        }
        adapter = new ExLisViewAdapter(this, groupList);
        listview.setAdapter(adapter);
        listview.setGroupIndicator(null);

        if (sensorTypeNumaxis == 3) {
            listview.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                @Override
                public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    if (i == 0) {
                        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x01, (byte) 0x00});
                    }
                    else if (i == 1) {
                        SmoothingDialog smoothingDialog = SmoothingDialog.newInstance();
                        smoothingDialog.setDevDialogCallBack(new SmoothingDialog.SmoothingDialogCallBack() {
                            @Override
                            public void save(String value) {
                                byte[] values = value.getBytes();
                                if (values.length == 1) {
                                    values[0] = 0x00;
                                }
                                writeReg(0x6c, byteToInt(values[0], values[1]));
                            }

                            @Override
                            public void back() {
                                // noop
                            }
                        });
                        smoothingDialog.show(getSupportFragmentManager());
                    }
                    return false;
                }
            });
        }

        if (sensorTypeNumaxis == 6) {
            listview.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                @Override
                public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                    // TODO switch
                    if (i == 0) {
                        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x67});
                    }
                    else if (i == 1) {
                        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x60});
                    }
                    else if (i == 2) {
                        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x52});
                    }
                    else if (i == 3) {
                        onClickSetJy61Baud();
                    }
                    else if (i == 4) {
                        orientation601();
                    }
                    else if (i == 5) {
                        staticDetect601();
                    }
                    else if (i == 6) {
                        bandwidth601();
                    }
                    else if (i == 7) {
                        mode601();
                    }
                    return false;
                }
            });
        }

        if (sensorTypeNumaxis == 9) {
            listview.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {
                    // TODO switch
                    if (i == 0) {
                        if (i1 == 0) {
                            writeLockReg(0x00, 0x01);
                        }
                        else if (i1 == 1) {
                            writeLockReg(0x22, 0x01);
                        }
                        else if (i1 == 2) {
                            selectAlgorithm();
                        }
                        else if (i1 == 3) {
                            orientation901();
                        }
                        else if (i1 == 4) {
                            cmdStartUp();
                        }
                        else if (i1 == 5) {
                            setAlarm();
                        }
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    if (i == 1) {
                        // TODO switch
                        if (i1 == 0) {
                            accCali();
                        }
                        else if (i1 == 1) {
                            writeLockReg(0x01, 0x07); // Start calibration
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_calibrating), Toast.LENGTH_LONG).show();
                        }
                        else if (i1 == 2) {
                            writeAndSaveReg(0x01, 0x00);
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_cali_done), Toast.LENGTH_LONG).show();
                            drawerLayout.closeDrawer(GravityCompat.START);
                        }
                        else if (i1 == 3) {
                            writeAndSaveReg(0x01, 0x03);
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_cali_done), Toast.LENGTH_LONG).show();
                            drawerLayout.closeDrawer(GravityCompat.START);
                        }
                        else if (i1 == 4) {
                            autoCalibrate();
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_cali_done), Toast.LENGTH_LONG).show();
                            drawerLayout.closeDrawer(GravityCompat.START);
                        }
                        else if (i1 == 5) {
                            writeAndSaveReg(0x01, 0x04);
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_cali_done), Toast.LENGTH_LONG).show();
                            drawerLayout.closeDrawer(GravityCompat.START);
                        }
                        else if (i1 == 6) {
                            writeAndSaveReg(0x01, 0x08);
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_cali_done), Toast.LENGTH_LONG).show();
                            drawerLayout.closeDrawer(GravityCompat.START);
                        }
                    }
                    if (i == 2) {
                        if (i1 == 0) {
                            selectAccelerationRange();
                        }
                        else if (i1 == 1) {
                            selectAngularVelocityRange();
                        }
                        else if (i1 == 2) {
                            selectBandwidth901();
                        }
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    if (i == 3) {
                        if (i1 == 0) {
                            selectOutputRate();
                        }
                        else if (i1 == 1) {
                            selectAddress();
                        }
                        else if (i1 == 2) {
                            selectCommunicationBaudrate();
                        }
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    if (i == 4) {
                        // TODO switch
                        if (i1 == 0) {
                            String value = getString(R.string.select_D0_port_mode);
                            dMode(i1, value);
                        }
                        else if (i1 == 1) {
                            String value = getString(R.string.select_D1_port_mode);
                            dMode(i1, value);
                        }
                        else if (i1 == 2) {
                            String value = getString(R.string.select_D2_port_mode);
                            dMode(i1, value);
                        }
                        else if (i1 == 3) {
                            String value = getString(R.string.select_D3_port_mode);
                            dMode(i1, value);
                        }
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    if (i == 5) {
                        // TODO wtf
                        if (i1 == 0) {
                            selectPwmPulseWidth(i1);
                        }
                        else if (i1 == 1) {
                            selectPwmPulseWidth(i1);
                        }
                        else if (i1 == 2) {
                            selectPwmPulseWidth(i1);
                        }
                        else if (i1 == 3) {
                            selectPwmPulseWidth(i1);
                        }
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    if (i == 6) {
                        // TODO wtf
                        if (i1 == 0) {
                            selectPwmCycle(i1);
                        }
                        else if (i1 == 1) {
                            selectPwmCycle(i1);
                        }
                        else if (i1 == 2) {
                            selectPwmCycle(i1);
                        }
                        else if (i1 == 3) {
                            selectPwmCycle(i1);
                        }
                        drawerLayout.closeDrawer(GravityCompat.START);
                    }
                    return true;
                }
            });
        }
    }

    private void sendData(byte[] byteSend) {
        if (bUsbConnect) {
            if (MyApp.driver.isConnected()) MyApp.driver.WriteData(byteSend, byteSend.length);
        }
        else {
            if (mBluetoothService != null) {
                mBluetoothService.send(byteSend);
            }
        }
    }

    int iChipBaudSelect = 2;

    private void selectCommunicationBaudrate() {
        String[] s = new String[]{"2400", "4800", "9600", "19200", "38400", "57600", "115200", "230400", "460800", "921600"};
        new AlertDialog.Builder(this)
                .setTitle("Please select the communication rate:")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iChipBaudSelect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iChipBaudSelect = i;
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x04, (byte) iChipBaudSelect, (byte) 0x00});
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void selectPwmCycle(final int pos) {
        PwmCycleDialog pwmCycle = PwmCycleDialog.newInstance();
        pwmCycle.setPwmCycleDialogCallBack(new PwmCycleDialog.PwmCycleDialogCallBack() {
            @Override
            public void save(String value) {
                writeAndSaveReg(0x16 + pos, Integer.parseInt(value));
            }

            @Override
            public void back() {
            }
        });
        pwmCycle.show(getSupportFragmentManager());
    }

    private void selectPwmPulseWidth(final int pos) {
        PwmDialog pwmDialog = PwmDialog.newInstance();
        pwmDialog.setPwmDialogCallBack(new PwmDialog.PwmDialogCallBack() {
            @Override
            public void save(String value) {
                writeAndSaveReg(0x12 + pos, Integer.parseInt(value));
            }

            @Override
            public void back() {
            }
        });
        pwmDialog.show(getSupportFragmentManager());
    }

    private void selectAddress() {
        AddressDialog addDialog = AddressDialog.newInstance();
        addDialog.setAddressDialogCallBack(new AddressDialog.AddressDialogCallBack() {
            @Override
            public void save(String value) {
                int v = Integer.parseInt(value);
                writeAndSaveReg(0x1a, (byte) v);
            }

            @Override
            public void back() {
            }
        });
        addDialog.show(getSupportFragmentManager());
    }


    int iRetrivalRateSelect = 5;

    private void selectOutputRate() {
        String[] s = new String[]{"0.2Hz", "0.5Hz", "1Hz", "2Hz", "5Hz", "10HZ", "20Hz", "50Hz", "100Hz", "125Hz", "200Hz"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.select_return_rate))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iRetrivalRateSelect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iRetrivalRateSelect = i;
                    }
                })
                .setPositiveButton(getString(R.string.end), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x03, (byte) (iRetrivalRateSelect + 1));
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int[] iPortMode = new int[]{0, 0, 0, 0};

    private void dMode(final int index, String v) {
        String[] s;
        if (index == 1) {
            s = new String[]{getString(R.string.analog_input), getString(R.string.digital_input), getString(R.string.output_digital_high_level)
                    , getString(R.string.output_digital_low_level), getString(R.string.output_PWM), getString(R.string.CLR_relative_posture)};
        }
        else {
            s = new String[]{getString(R.string.analog_input), getString(R.string.digital_input), getString(R.string.output_digital_high_level)
                    , getString(R.string.output_digital_low_level), getString(R.string.output_PWM)};
        }
        new AlertDialog.Builder(this).setTitle(v)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iPortMode[index], new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iPortMode[index] = i;
                    }
                })
                .setPositiveButton(getString(R.string.end), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x0e + index, iPortMode[index]);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }


    int iBandwidth901 = 4;

    private void selectBandwidth901() {
        String[] s = new String[]{"256HZ", "184HZ", "94HZ", "42HZ", "21HZ", "10HZ", "5HZ"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_bandwith))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iBandwidth901, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iBandwidth901 = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x1f, (byte) iBandwidth901);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int angularVelocityRangeParam = 3;

    private void selectAngularVelocityRange() {
        String[] s = new String[]{"250deg/s", "500deg/s", "1000deg/s", "2000deg/s"};
        new AlertDialog.Builder(this)
                .setTitle((R.string.choose_angle))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, angularVelocityRangeParam, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        angularVelocityRangeParam = i;
                    }
                })
                .setPositiveButton((R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x20, (byte) angularVelocityRangeParam);
                        av = 250 * (int) Math.pow(2, angularVelocityRangeParam);//1,2,4,8
                        Log.i("range", String.format("w range = %d", av));
                        SharedUtil.putInt("av", av);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int accRangeParam = 3;

    private void selectAccelerationRange() {
        String[] s = new String[]{"2g", "4g", "8g", "16g"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_range))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, accRangeParam, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        accRangeParam = i;
                    }
                })
                .setPositiveButton(getString(R.string.end), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x21, (byte) accRangeParam);
                        ar = (int) Math.pow(2, accRangeParam + 1);
                        Log.i("range", String.format("acc range = %d", ar));
                        SharedUtil.putInt("ar", ar);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int iAutoCali = 0;

    private void autoCalibrate() {
        String[] s = new String[]{"Yes", "No"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.automatic_calibration_of_helix))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iAutoCali, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iAutoCali = i;
                    }
                })
                .setPositiveButton(getString(R.string.end), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x63, (byte) iAutoCali);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    boolean bMagCali = false;

    private void magCali() {
        if (bMagCali) {
            writeAndSaveReg(0x01, 0x00);
            groupList.get(1).getChildList().get(1).setName(getString(R.string.magnetic_field_calibration_start));
            adapter.notifyDataSetChanged();
            bMagCali = false;
        }
        else {//End calibration
            writeLockReg(0x01, 0x07); // Start calibration
            groupList.get(1).getChildList().get(1).setName(getString(R.string.finish));
            adapter.notifyDataSetChanged();
            bMagCali = true;
        }
    }

    private void accCali() {
        writeLockReg(0x01, 0x01);
        saveReg(3000);

        Toast.makeText(this, getString(R.string.calibrating), Toast.LENGTH_LONG).show();

        new Handler().postDelayed(() -> Toast.makeText(getApplicationContext(), getString(R.string.calibrated), Toast.LENGTH_SHORT).show(), 3000);
    }

    private void setAlarm() {
        AlarmDialog alarmDialog = new AlarmDialog();
        alarmDialog.setPoliceDialogCallBack(new AlarmDialog.PoliceDialogCallBack() {
            @Override
            public void save(String strXMin, final String strXMax, final String strYMin, final String strYMax, final String time, final int tag) {
                unLockReg(0);
                writeReg(0x5a, Integer.parseInt(strXMin) * 32768 / 180, 50);
                writeReg(0x5a, Integer.parseInt(strXMax) * 32768 / 180, 100);
                writeReg(0x5a, Integer.parseInt(strYMin) * 32768 / 180, 150);
                writeReg(0x5a, Integer.parseInt(strYMax) * 32768 / 180, 200);
                writeReg(0x5a, Integer.parseInt(time));
                if (tag == 0) {
                    writeReg(0x62, 0x00, 250);
                }
                else if (tag == 1) writeReg(0x62, 0x01, 250);
                saveReg(300);
            }

            @Override
            public void back() {
            }
        });
        alarmDialog.show(getSupportFragmentManager());
    }

    int iAlgrithm = 1;

    private void selectAlgorithm() {
        String[] s = new String[]{getString(R.string.six_axis_algorithm), getString(R.string.nine_axis_algorithm)};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_algorithm))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iAlgrithm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iAlgrithm = i;
                    }
                })
                .setPositiveButton(getString(R.string.end), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (iAlgrithm == 0) {
                            writeAndSaveReg(0x24, 0x01);
                        }
                        else if (iAlgrithm == 1) {
                            writeAndSaveReg(0x24, 0x00);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void serialPortClose() {
        MyApp.driver.CloseDevice();
        isOpen = false;
    }

    private Thread readThread;
    boolean bUsbConnect = false;

    private boolean serialPortOpen() {
        int retval = MyApp.driver.ResumeUsbList();
        if (retval == -1) // The ResumeUsbList() method is used to enumerate CH34X devices and open related devices
        {
            MyApp.driver.CloseDevice();
        }
        else if (retval == 0) {
            if (!MyApp.driver.UartInit()) { // Initialize the serial device
                Toast.makeText(DataMonitorActivity.this, getString(R.string.open_device_failure), Toast.LENGTH_SHORT).show();
                return false;
            }
            Toast.makeText(DataMonitorActivity.this, getString(R.string.open_device_success), Toast.LENGTH_SHORT).show();
            if (MyApp.driver.isConnected()) {
                usbBaudrateInit();
                bBTConnet = false;
                bUsbConnect = true;
            }
            isOpen = true;

            return true;
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.unauthorized_authority));
            builder.setMessage(getString(R.string.confirm_quit));
            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    System.exit(0);
                }
            });
            builder.setNegativeButton(getString(R.string.back), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });
            builder.show();
            return false;
        }
        return false;
    }


    public void onClickedBTSet(View v) {
        try {
            if (mBluetoothService == null) {
                // Used to manage Bluetooth connections
                mBluetoothService = new BluetoothService(this, mHandler);
            }
            else {
                mBluetoothService.stop();
            }
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        }
        catch (Exception e) {
            Log.e(TAG, "onClickedBTSet: ", e);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public synchronized void onResume() {
        super.onResume();
        initButton();
        if (!bUsbConnect) {
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mBluetoothService != null) {
                        if (mBluetoothService.getState() == BluetoothService.STATE_NONE) {
                            mBluetoothService.start();
                        }
                    }
                    else {
                        Log.e(TAG, "onResume.run: service is null");
                        String address = SharedUtil.getString("BTName");
                        if (address != null) {
                            Log.e("--", "BTName = " + address);
                            mBluetoothService = new BluetoothService(getApplicationContext(), mHandler); // Used to manage Bluetooth connections
                            device = mBluetoothAdapter.getRemoteDevice(address);// Get the BLuetoothDevice object
                            mBluetoothService.connect(device);// Attempt to connect to the device
                        }
                        else {
                            onClickedBTSet(null);
                        }
                    }
                }
            }, 1000);
        }
        if (lineChart == null) {
            lineChart = findViewById(R.id.lineChart);
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX", "AngleY", "AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_chart));
        }
        outputSwitch = findViewById(R.id.dataSwitch);
        if (sensorTypeNumaxis == 9) {
            outputSwitch.setVisibility(View.VISIBLE);
        }
        else {
            outputSwitch.setVisibility(View.INVISIBLE);
        }
        if (readThread == null) {
            // Open the reader thread to read the data received by the serial port
            readThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (isOpen) {
                    int length = MyApp.driver.ReadData(buffer, 4096);
                    if (length > 0) {
                        handleSerialData(length, buffer);
                    }
                }
                try {
                    Thread.sleep(10);
                }
                catch (Exception ignored) {
                }
            });
            readThread.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) mBluetoothService.stop();
        serialPortClose();
        bDisplay = false;
    }

    public BluetoothDevice device;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BluetoothDevice object
                    device = mBluetoothAdapter.getRemoteDevice(address);
                    SharedUtil.putString("BTName", address);
                    // Attempt to connect to the device
                    mBluetoothService.connect(device);
                }
                break;
            case GPS_REQUEST_CODE:
                openGPSSetting();
                break;
        }
    }

    int iJY61Baud = 0;
    int iJY61RateSelect = 0;

    public void onClickSetJy61Baud() {
        if (bUsbConnect) {
            switch (iBaud) {
                case 9600:
                    iJY61Baud = 0;
                    break;
                case 115200:
                    iJY61Baud = 1;
                    break;
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.select_return_rate))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setSingleChoiceItems(new String[]{"9600", "115200"}, iJY61Baud, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            iJY61Baud = i;
                        }
                    })
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            try {
                                sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x64 - iJY61Baud)});
                                Thread.sleep(100);
                                if (iJY61Baud == 0) {
                                    iBaud = 9600;
                                }
                                else {
                                    iBaud = 115200;
                                }
                                if (MyApp.driver.isConnected()) {
                                    MyApp.driver.SetConfig(iBaud, (byte) 8, (byte) 0, (byte) 0, (byte) 0);
                                }
                            }
                            catch (Exception e) {
                                Log.e(TAG, "onClickSetJy61Baud: ", e);
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }
        else {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.select_return_rate))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setSingleChoiceItems(new String[]{"20Hz", "100Hz"}, iJY61RateSelect, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            iJY61RateSelect = i;
                        }
                    })
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            try {
                                sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x64 - iJY61RateSelect)});
                            }
                            catch (Exception e) {
                                Log.e(TAG, "onClick: ", e);
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }
    }

    int iDirection = 0;

    public void orientation601() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_install_orientation))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{getString(R.string.horizontal_installation), getString(R.string.vertical_installation)}, iDirection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iDirection = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (iDirection == 0) {
                            sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x65});
                        }
                        else if (iDirection == 1) {
                            sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x66});
                        }
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int getiDirection901 = 1;

    public void orientation901() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_install_orientation))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{getString(R.string.horizontal_installation), getString(R.string.vertical_installation)}, getiDirection901, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getiDirection901 = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x23, 0x01 - getiDirection901);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int iCmdStartup = 1;

    // No idea what this is about...
    public void cmdStartUp() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.__whether_or_not))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{"Yes", "No"}, iCmdStartup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iCmdStartup = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x2d, iCmdStartup);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int iStaticDetect61 = 4;

    public void staticDetect601() {
        String[] s = new String[]{"0.122°/s", "0.244°/s", "0.366°/s", "0.488°/s", "0.610°/s", "0.732°/s", "0.854°/s", "0.976°/s"
                , "1.098°/s", "1.221°/s", "1.343°/s", "1.456°/s", "1.587°/s", "1.709°/s", "1.831°/s"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.static_detection_threshold))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iStaticDetect61, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iStaticDetect61 = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x71 + iStaticDetect61)});
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int iBandwidth61 = 4;

    public void bandwidth601() {
        String[] s = new String[]{"256HZ", "184HZ", "94HZ", "44HZ", "21HZ", "10HZ", "5HZ"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_bandwith))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iBandwidth61, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iBandwidth61 = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x81 + iBandwidth61)});
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    int iMode61 = 0;

    public void mode601() {
        String[] s = new String[]{"Serial", "IIC"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_model))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iMode61, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iMode61 = i;
                    }
                })
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        sendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x61 + iMode61)});
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    public void onBluetoothScanButtonClick(View v) {
        if (v.getId() == R.id.bluetoothScanButton) {
            onClickedBTSet(v);
        }
    }


    static boolean[] outputPackage = new boolean[]{true, true, true, true, true, true, true, true, true, true, true};

    public void setOutputBoolean(int iOut) {
        if (iOut == -1) iOut = 0x0F;
        for (int i = 0; i < outputPackage.length; i++) {
            outputPackage[i] = ((iOut >> i) & 0x01) == 0x01;
        }
    }

    public int getOutputInt() {
        int iTemp = 0;
        for (int i = 0; i < outputPackage.length; i++) {
            if (outputPackage[i]) iTemp |= 0x01 << i;
        }
        return iTemp;
    }

    public void onSideMenuButtonClick(View v) {
        drawerLayout.openDrawer(GravityCompat.START);
    }

    boolean bPause = false;

    public void onClickPause(View v) {
        bPause = !bPause;
    }

    public void onClickRecord(View v) {
        if (!this.recordStartorStop) {
            this.recordStartorStop = true;
            setRecord(true);
            ((Button) v).setText(getString(R.string.stop));
        }
        else {
            this.recordStartorStop = false;
            setRecord(false);
            ((Button) findViewById(R.id.btnRecord)).setText(getString(R.string.record));
            if (myFile == null) {
                Toast.makeText(DataMonitorActivity.this, "No file recorded!", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.hint))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getString(R.string.recorded_root_directory) + myFile.file.getPath() + getString(R.string.open_file))
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            try {
                                myFile.openFile(getApplicationContext());
                            }
                            catch (Exception e) {
                                Log.e(TAG, "myFile.openFile: ", e);
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        }
    }

    @Override
    public void onClick(View v) {
    }

}
