package com.bt901;

import android.Manifest;
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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.bt901.dialog.AddressDialog;
import com.bt901.dialog.DevDialog;
import com.bt901.dialog.PwmCycleDialog;
import com.bt901.dialog.PwmDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import cn.wch.ch34xuartdriver.CH34xUARTDriver;
import com.bt901.bluetooth.BluetoothService;
import com.wtzn.wtfile.util.*;
import com.bt901.dialog.AlarmDialog;
import com.github.mikephil.charting.charts.LineChart;

public class DataMonitor extends FragmentActivity implements OnClickListener {

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothService mBluetoothService = null;
    private String mConnectedDeviceName = null;
    private static final String ACTION_USB_PERMISSION = "cn.wch.wchusbdriver.USB_PERMISSION";
    private Button mTitle;
    private boolean recordStartorStop = false;
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    private static final int REQUEST_CONNECT_DEVICE = 1;
    public byte[] writeBuffer;
    public byte[] readBuffer;
    private boolean isOpen;
    private int retval;
    private static int type;
    static MyFile myFile;
    DrawerLayout drawerLayout;
    ExLisViewAdapter adapter;
    private Switch outputSwitch;
    List<GroupBeen> groupList = new ArrayList<>();
    private static int ar = 16, av = 2000;
    private static float[] ac = new float[]{0,0,0};
    private static float[] w = new float[]{0,0,0};
    private static float[] h = new float[]{0,0,0};
    private static float[] Angle = new float[]{0,0,0};
    private static float[] d = new float[]{0,0,0,0};
    private static float[] q = new float[]{0,0,0,0};
    private static float T = 20;
    private static float pressure,height,longitude,latitude,altitude,yaw,velocity,sn,pdop,hdop,vdop,voltage,version;
    private static short IDSave = 0;
    private static short IDNow;
    private static int SaveState = -1;
    private static int sDataSave = 0;
    static int iCurrentGroup = 3;
    private static String strDate = "", strTime = "";
    private boolean bBTConnet = false;
    private LineChart lineChart;
    private LineChartManager lineChartManager;
    private List<Integer> qColour = new ArrayList<Integer>(Arrays.asList(Color.RED,Color.GREEN,Color.BLUE,Color.GRAY));//折线颜色集合
    private float norm(float x[]){
        return (float)Math.sqrt(x[0]*x[0]+x[1]*x[1]+x[2]*x[2]);
    }
    private TextView tvLabelX,tvLabelY,tvLabelZ,tvLabelAll,tvX,tvY,tvZ,tvAll;
    public void setTableName(String str1,String str2,String str3,String str4){
        tvLabelX.setText(str1);
        tvLabelY.setText(str2);
        tvLabelZ.setText(str3);
        tvLabelAll.setText(str4);
    }
    public void setTableData(String str1,String str2,String str3,String str4){
        tvX.setText(str1);
        tvY.setText(str2);
        tvZ.setText(str3);
        tvAll.setText(str4);
    }
    public void setTableData(String format,Object d1,Object d2,Object d3,Object d4){
        setTableData(String.format(format,d1),String.format(format,d2),String.format(format,d3),String.format(format,d4));
    }
    static float fTempT;
    static int iError = 0;
    static Queue<Byte> queueBuffer = new LinkedList<Byte>();
    static boolean [] bDataUpdate = new boolean[20];

    public static void CopeSerialData(int acceptedLen, byte[] tempInputBuffer) {
        byte[] packBuffer = new byte[11];
        byte sHead;
        float fTemp;
        for (int i = 0; i < acceptedLen; i++) queueBuffer.add(tempInputBuffer[i]);// 从缓冲区读取到的数据，都存到队列里
        while (queueBuffer.size() >= 11) {
            if ((queueBuffer.poll()) != 0x55) {
                iError++;
                continue;
            }// peek()返回对首但不删除 poll 移除并返回
            sHead = queueBuffer.poll();
            if ((sHead & 0xF0) == 0x50) iError = 0;
            for (int j = 0; j < 9; j++) packBuffer[j] = queueBuffer.poll();
            byte value;
            value = (byte) (0x55 + sHead);
            for (int i = 0; i < 8; i++) value = (byte) (value + packBuffer[i]);
            if (value != packBuffer[8]) {
                Log.e("--", String.format("%2x %2x %2x %2x %2x %2x %2x %2x %2x SUM:%2x %2x", sHead, packBuffer[0], packBuffer[1], packBuffer[2], packBuffer[3], packBuffer[4], packBuffer[5], packBuffer[6], packBuffer[7], packBuffer[8], value));
                continue;
            }
            switch (sHead) {
                case 0x50:
                    int ms = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff));
                    strDate = String.format("20%02d-%02d-%02d", packBuffer[0], packBuffer[1], packBuffer[2]);
                    strTime = String.format("%02d:%02d:%02d.%03d", packBuffer[3], packBuffer[4], packBuffer[5], ms);
                    break;
                case 0x51:
                    if (SharedUtil.getInt("ar") != -1) ar = SharedUtil.getInt("ar");
                    for (int i = 0; i < 3; i++)
                        ac[i] = ((((short) packBuffer[i * 2 + 1]) << 8) | ((short) packBuffer[i * 2] & 0xff)) / 32768.0f * ar;
                    fTempT = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
                    if (type == 6) T = (float) (fTempT / 340 + 36.53);
                    else T = fTempT;
                    break;
                case 0x52:
                    //角速度
                    if (SharedUtil.getInt("av") != -1) av = SharedUtil.getInt("av");
                    for (int i = 0; i < 3; i++)
                        w[i] = ((((short) packBuffer[i * 2 + 1]) << 8) | ((short) packBuffer[i * 2] & 0xff)) / 32768.0f * av;
                    fTemp = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
                    if (fTemp != fTempT) {
                        voltage = fTemp;
                    } else voltage = 0;
                    break;
                case 0x53:
                    for (int i = 0; i < 3; i++)
                        Angle[i] = ((((short) packBuffer[i * 2 + 1]) << 8) | ((short) packBuffer[i * 2] & 0xff)) / 32768.0f * 180;
                    fTemp = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
                    if (fTemp != fTempT) {
                        version = fTemp * 100;
                    } else version = 0;
                    break;
                case 0x54://磁场
                    for (int i = 0; i < 3; i++)
                        h[i] = ((((short) packBuffer[i * 2 + 1]) << 8) | ((short) packBuffer[i * 2] & 0xff));
                    //  RecordData(sHead);
                    break;
                case 0x55://端口
                    for (int i = 0; i < 4; i++)
                        d[i] = ((((short) packBuffer[i * 2 + 1]) << 8) | ((short) packBuffer[i * 2] & 0xff));
                    break;
                case 0x56://气压、高度
                    pressure = ((((long) packBuffer[3]) << 24) & 0xff000000) | ((((long) packBuffer[2]) << 16) & 0xff0000) | ((((long) packBuffer[1]) << 8) & 0xff00) | ((((long) packBuffer[0]) & 0xff));
                    height = (((((long) packBuffer[7]) << 24) & 0xff000000) | ((((long) packBuffer[6]) << 16) & 0xff0000) | ((((long) packBuffer[5]) << 8) & 0xff00) | ((((long) packBuffer[4]) & 0xff))) / 100.0f;

                    break;
                case 0x57://经纬度
                    long Longitude = ((((long) packBuffer[3]) << 24) & 0xff000000) | ((((long) packBuffer[2]) << 16) & 0xff0000) | ((((long) packBuffer[1]) << 8) & 0xff00) | ((((long) packBuffer[0]) & 0xff));
                    longitude = (float) (Longitude / 10000000 + ((float) (Longitude % 10000000) / 100000.0 / 60.0));
                    long Latitude = (((((long) packBuffer[7]) << 24) & 0xff000000) | ((((long) packBuffer[6]) << 16) & 0xff0000) | ((((long) packBuffer[5]) << 8) & 0xff00) | ((((long) packBuffer[4]) & 0xff)));
                    latitude = (float) (Latitude / 10000000 + ((float) (Latitude % 10000000) / 100000.0 / 60.0));
                    break;
                case 0x58://海拔、航向、地速
                    altitude = (float) ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff)) / 10;
                    yaw = (float) ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 100;
                    velocity = (float) (((((long) packBuffer[7]) << 24) & 0xff000000) | ((((long) packBuffer[6]) << 16) & 0xff0000) | ((((long) packBuffer[5]) << 8) & 0xff00) | ((((long) packBuffer[4]) & 0xff))) / 1000;

                    break;
                case 0x59://四元数
                    for (int i = 0; i < 4; i++)
                        q[i] = ((((short) packBuffer[i * 2 + 1]) << 8) | ((short) packBuffer[i * 2] & 0xff)) / 32768.0f;
                    break;
                case 0x5a://卫星数
                    sn = ((((short) packBuffer[1]) << 8) | ((short) packBuffer[0] & 0xff));
                    pdop = ((((short) packBuffer[3]) << 8) | ((short) packBuffer[2] & 0xff)) / 100.0f;
                    hdop = ((((short) packBuffer[5]) << 8) | ((short) packBuffer[4] & 0xff)) / 100.0f;
                    vdop = ((((short) packBuffer[7]) << 8) | ((short) packBuffer[6] & 0xff)) / 100.0f;
                    break;
            }//switch

            if ((sHead >= 0x50) && (sHead <= 0x5a)) {
                RecordData(sHead);
                bDataUpdate[sHead - 0x50] = true;
                continue;
            }
        }
    }
    private int byteToInt(byte byteL,byte byteH){
        return (byteH<<8)|byteL;
    }
    private void writeReg(final int address,final int data,int delayMs){
        //if(mBluetoothService==null) return;
        if (delayMs==0)  SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) address, (byte) (data&0xff), (byte) ((data>>8)&0xff)});
        else {
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) address, (byte) (data & 0xff), (byte) ((data >> 8) & 0xff)});
                }
            }, delayMs);
        }
    }

    private void writeReg(int addr,int data){
        writeReg(addr,data,0);
    }

    private void unLockReg(int delayMs){
        writeReg(0x69,0xb588,delayMs);//unlock
    }
    private void saveReg(int delayMs){
        writeReg(0x00,0x00,delayMs);//unlock
    }
    private void writeLockReg(int addr,int data){
        unLockReg(0);//unlock
        writeReg(addr,data,50);//write Reg
    }
    private void writeAndSaveReg(int addr,int data){
        unLockReg(0);//unlock
        writeReg(addr,data,50);//write Reg
        saveReg(100);//save
    }
    private void setCurrentGroup(View v){
        (findViewById(R.id.button0)).setBackgroundColor(0xff33b5e5);
        (findViewById(R.id.button1)).setBackgroundColor(0xff33b5e5);
        (findViewById(R.id.button2)).setBackgroundColor(0xff33b5e5);
        (findViewById(R.id.button3)).setBackgroundColor(0xff33b5e5);
        (findViewById(R.id.button4)).setBackgroundColor(0xff33b5e5);
        (findViewById(R.id.button5)).setBackgroundColor(0xff33b5e5);
        (findViewById(R.id.button6)).setBackgroundColor(0xff33b5e5);
        (findViewById(R.id.button7)).setBackgroundColor(0xff33b5e5);
        (findViewById(R.id.button8)).setBackgroundColor(0xff33b5e5);
        (findViewById(R.id.button9)).setBackgroundColor(0xff33b5e5);
        (findViewById(R.id.buttonA)).setBackgroundColor(0xff33b5e5);
        v.setBackgroundColor(0xff0099cc);
    }

    public void OutputSwitchClick(View v){
        Log.e("--",String.format("Output:0x%x",getOutputInt()));
        if(type == 9)  {
            OutputPackage[iCurrentGroup] = outputSwitch.isChecked();
            int outputContent = getOutputInt();
            writeAndSaveReg(0x02,outputContent);
            SharedUtil.putInt("Out", outputContent);
        }
    }
    public void ControlClick(View v) {
        lineChartManager.setbPause(true);
        int i = v.getId();
        setCurrentGroup(v);
        if (i == R.id.button0) {
            iCurrentGroup = 0;
            setTableName(getString(R.string.Version),getString(R.string.Voltage),getString(R.string.Date),getString(R.string.Time));
            Log.e("--","123:"+getString(R.string.Voltage));
            setTableData("1.0","3.3V","2020-1-1","00:00:00.0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX","AngleY","AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_Chart));
            if(type==9){
                unLockReg(0);
                Calendar calendar = Calendar.getInstance();
                int year = calendar.get(Calendar.YEAR);
                int month = calendar.get(Calendar.MONTH)+1;
                int day = calendar.get(Calendar.DAY_OF_MONTH);
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);
                writeReg(0x30,byteToInt((byte)(year-2000),(byte)month),50);
                writeReg(0x31,byteToInt((byte)day,(byte)hour),100);
                writeReg(0x32,byteToInt((byte)minute,(byte)second),150);
            }
        } else if (i == R.id.button1) {
            iCurrentGroup = 1;
            setTableName("ax:","ay:","az:","|a|");
            setTableData("0","0","0","0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("ax","ay","az"), qColour);
            lineChartManager.setDescription(getString(R.string.acc_Chart));
        } else if (i == R.id.button2) {
            iCurrentGroup = 2;
            setTableName("wx:","wy:","wz:","|w|");
            setTableData("0","0","0","0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("wx","wy","wz"), qColour);
            lineChartManager.setDescription(getString(R.string.w_Chart));
        } else if (i == R.id.button3) {
            iCurrentGroup = 3;
            setTableName("AgnleX:","AngleY:","AngleZ:","T:");
            setTableData("0","0","0","25℃");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX","AngleY","AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_Chart));
        } else if (i == R.id.button4) {
            iCurrentGroup = 4;
            setTableName("hx:","hy:","hz:","|h|");
            setTableData("0","0","0","0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("hx","hy","hz"), qColour);
            lineChartManager.setDescription(getString(R.string.mag_chart));
        } else if (i == R.id.button5) {
            iCurrentGroup = 5;
            setTableName("D0:","D1:","D2:","D3:");
            setTableData("0","0","0","0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("D0","D1","D2","D3"), qColour);
            lineChartManager.setDescription(getString(R.string.port_chart));
        } else if (i == R.id.button6) {
            iCurrentGroup = 6;
            setTableName("Pressure:","Altitude:","wz:","|w|");
            setTableData("0","0","0","0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("pressure"), qColour);
            lineChartManager.setDescription(getString(R.string.pressure_chart));
        } else if (i == R.id.button7) {
            iCurrentGroup = 7;
            setTableName("Longitude:","Latitude:","","");
            setTableData("0","0","","");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX","AngleY","AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_Chart));
        } else if (i == R.id.button8) {
            iCurrentGroup = 8;
            setTableName("Altitude:","Yaw:","Velocity:","");
            setTableData("0","0","0","");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX","AngleY","AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_Chart));
        } else if (i == R.id.button9) {
            iCurrentGroup = 9;
            setTableName("q0:","q1:","q2:","q3:");
            setTableData("0","0","0","0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("q0","q1","q2","q3"), qColour);
            lineChartManager.setDescription(getString(R.string.quaternion_chart));
        } else if (i == R.id.buttonA) {
            iCurrentGroup = 10;
            setTableName("SN:","PDOP:","HDOP:","VDOP");
            setTableData("0","0","0","0");
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX","AngleY","AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_Chart));
        }
        if(type == 9) {
            outputSwitch.setVisibility(View.VISIBLE);
            outputSwitch.setChecked(OutputPackage[iCurrentGroup]);
        }
        else outputSwitch.setVisibility(View.INVISIBLE);


        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                lineChartManager.setbPause(false);
            }
        }, 100);
    }
    public static void RecordData(byte ID) {
        try {
            boolean Repeat = false;
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            Date curDate = new Date(System.currentTimeMillis());//获取当前时间
            short sData = (short) (0x01 << (ID & 0x0f));
            if (((IDNow & sData) == sData) && (sData < sDataSave)) {
                IDSave = IDNow;
                IDNow = sData;
                Repeat = true;
            } else IDNow |= sData;
            sDataSave = sData;
            switch (SaveState) {
                case 0:
                    myFile.Close();
                    SaveState = -1;
                    break;
                case 1:
                    SimpleDateFormat formatterFileName = new SimpleDateFormat("MMdd_HHmmss");
                    Date curDateFileName = new Date(System.currentTimeMillis());//获取当前时间
                    myFile = new MyFile(Environment.getExternalStorageDirectory() +"/Records/Rec_"+ formatterFileName.format(curDateFileName)+".txt");
                    String s = "Start time：" + formatter.format(curDate) + "\r\n"+"Record Time:";
                    if ((IDSave & 0x01) > 0) s += " ChipTime:";
                    if ((IDSave & 0x02) > 0) s += " ax： ay： az：";
                    if ((IDSave & 0x04) > 0) s += "  wx： wy： wz：";
                    if ((IDSave & 0x08) > 0) s += "    AngleX：   AngleY：   AngleZ：";
                    if ((IDSave & 0x10) > 0) s += "   hx：   hy：   hz：";
                    if ((IDSave & 0x20) > 0) s += "d0：d1：d2：d3：";
                    if ((IDSave & 0x40) > 0) s += "    Pressure：    Height：";
                    if ((IDSave & 0x80) > 0) s += "        Longitude：        Latitude：";
                    if ((IDSave & 0x100) > 0) s += "    ALtitude：    Yaw：    Velocity：";
                    if ((IDSave & 0x200) > 0) s += "   q0：   q1：   q2：   q3：";
                    if ((IDSave & 0x400) > 0) s += "SN：PDOP： HDOP： VDOP：";
                    myFile.Write(s );
                        SaveState = 2;
                    break;
                case 2:
                    if (Repeat) {
                        myFile.Write("  \r\n");
                        myFile.Write(formatter.format(curDate)+ " ");
                        if ((IDSave & 0x01) > 0) myFile.Write(strDate+" "+strTime+ " ");
                        if ((IDSave & 0x02) > 0)  myFile.Write(String.format("% 10.4f", ac[0]) + String.format("% 10.4f", ac[1]) + String.format("% 10.4f", ac[2]) + " ");
                        if ((IDSave & 0x04) > 0) myFile.Write(String.format("% 10.4f", w[0]) + String.format("% 10.4f", w[1]) + String.format("% 10.4f", w[2]) + " ");
                        if ((IDSave & 0x08) > 0) myFile.Write(String.format("% 10.4f", Angle[0]) + String.format("% 10.4f", Angle[1]) + String.format("% 10.4f", Angle[2]));
                        if ((IDSave & 0x10) > 0) myFile.Write(String.format("% 10.0f", h[0]) + String.format("% 10.0f", h[1]) + String.format("% 10.0f", h[2]));
                        if ((IDSave & 0x20) > 0) myFile.Write(String.format("% 7.0f", d[0]) + String.format("% 7.0f", d[1]) + String.format("% 7.0f", d[2]) + String.format("% 7.0f", d[3]));
                        if ((IDSave & 0x40) > 0) myFile.Write(String.format("% 10.0f", pressure) + String.format("% 10.2f", height));
                        if ((IDSave & 0x80) > 0) myFile.Write(String.format("% 14.6f", longitude) + String.format("% 14.6f", latitude));
                        if ((IDSave & 0x100) > 0) myFile.Write(String.format("% 10.4f", altitude) + String.format("% 10.2f", yaw) + String.format("% 10.2f", velocity));
                        if ((IDSave & 0x200) > 0) myFile.Write(String.format("% 7.4f", q[0]) + String.format("% 7.4f", q[1]) + String.format("% 7.4f", q[2]) + String.format("% 7.4f", q[3]));
                        if ((IDSave & 0x400) > 0) myFile.Write(String.format("% 5.0f", sn) + String.format("% 7.1f", pdop) + String.format("% 7.1f", hdop) + String.format("% 7.1f", vdop));
                    }
                    break;
                case -1:
                    break;
                default:
                    break;
            }
        } catch (Exception err) {
        }
    }
    public void setRecord(boolean record) {
        if (record) {SaveState = 1;}
        else SaveState = 0;
    }

    private final Handler mHandler = new Handler() {
        @Override        // 匿名内部类写法，实现接口Handler的一些
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            bBTConnet = true;
                            initButton();
                            if (mTitle != null)
                                mTitle.setText(getString(R.string.title_connected_to)+mConnectedDeviceName);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            if (mTitle != null)
                                mTitle.setText(getString(R.string.title_connecting));
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            bBTConnet = false;
                            if (mTitle != null)
                                mTitle.setText(getString(R.string.title_not_connected));
                            break;
                    }
                    break;
                case MESSAGE_READ:
                    break;
                case MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString("device_name");
                    Toast.makeText(getApplicationContext(), "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString("toast"), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    int iBaudJY61Select = 1;
    final int[] baud = new int[]{ 4800, 9600, 19200, 38400, 57600, 115200, 230400, 460800, 921600};
    private  void USBBaudInit(){
        if (type == 9  )
        {
            iBaudJY901Select = SharedUtil.getInt("JY901BAUD");
            if ((iBaudJY901Select>0)&&(iBaudJY901Select<9))
                iBaud = baud[iBaudJY901Select];
            else
                iBaud = 9600;
            SetBaud(iBaud);
            SelectedJY901Baudrate();
        }
        else
        {
            iBaudJY61Select = SharedUtil.getInt("JY61BAUD");
            if (iBaudJY61Select==0)
                iBaud = 9600;
            else
                iBaud = 115200;
            SetBaud(iBaud);
            SelectedJY61Baudrate();
        }
    }
    private void SelectedJY61Baudrate() {
        String[] s = new String[]{ "9600", "115200"};
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
                        SharedUtil.putInt("JY61BAUD",iBaudJY61Select);
                        if (iBaudJY61Select==0)
                            iBaud = 9600;
                        else
                            iBaud = 115200;
                        SetBaud(iBaud);
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }
    int iBaudJY901Select = 5;
    private void SelectedJY901Baudrate() {
        String[] s = new String[]{ "4800", "9600", "19200", "38400", "57600", "115200", "230400", "460800", "921600"};
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
                        SharedUtil.putInt("JY901BAUD",iBaudJY901Select);
                        iBaud = baud[iBaudJY901Select];
                        SetBaud(iBaud);
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }
    private int iBaud = 9600;
    public void SetBaud(int iBaudrate) {
        iBaud = iBaudrate;
        SharedUtil.putInt("Baud",iBaudrate);
        MyApp.driver.SetConfig(iBaud, (byte) 8, (byte) 0, (byte) 0, (byte) 0);
    }

    public void ChangeBaud() {
        if (MyApp.driver.isConnected() == false) return;
        switch (iBaud) {
            case 2400:
                SetBaud(4800);
                break;
            case 4800:
                SetBaud(9600);
                break;
            case 9600:
                SetBaud(19200);
                break;
            case 19200:
                SetBaud(38400);
                break;
            case 38400:
                SetBaud(57600);
                break;
            case 57600:
                SetBaud(115200);
                break;
            case 115200:
                SetBaud(230400);
                break;
            case 230400:
                SetBaud(460800);
                break;
            case 460800:
                SetBaud(921600);
                break;
            case 921600:
                SetBaud(2400);
                break;
            default:
                SetBaud(9600);
                break;
        }
        Toast.makeText(this, String.format("Try baudrate %d", iBaud), Toast.LENGTH_SHORT).show();
    }
    private final Handler refreshhandler = new Handler() {
        public void handleMessage(Message msg) {
                if (bPause) return;
                if (bDataUpdate[iCurrentGroup]==false) return;
                bDataUpdate[iCurrentGroup] = false;
                switch (iCurrentGroup) {
                    case 0:
                        ((TextView) findViewById(R.id.tvZ)).setText(strDate);
                        ((TextView) findViewById(R.id.tvAll)).setText(strTime);
                        ((TextView) findViewById(R.id.tvY)).setText(String.format("%10.2fV", voltage));
                        ((TextView) findViewById(R.id.tvX)).setText(String.format("% 10.0f", version));
                        break;
                    case 1:
                        setTableData("% 10.4fg", ac[0], ac[1], ac[2], norm(ac));
                        // Log.e("--",String.format("acc:% 10.2fg,% 10.2fg,% 10.2fg,% 10.2fg", ac[0], ac[1], ac[2], norm(ac)));
                        lineChartManager.addEntry(Arrays.asList(ac[0], ac[1], ac[2]));
                        break;
                    case 2:
                        setTableData("% 10.4f°/s", w[0], w[1], w[2], norm(w));
                      //  Log.e("--", String.format("axw:% 10.2f,% 10.2f,% 10.2f,% 10.2f", w[0], w[1], w[2], norm(w)));
                        lineChartManager.addEntry(Arrays.asList(w[0], w[1], w[2]));
                        break;
                    case 3:
                        setTableData(String.format("%10.4f°", Angle[0]), String.format("%10.4f°", Angle[1]), String.format("%10.4f°", Angle[2]), String.format("%10.2f℃", T));
                        break;
                    case 4://磁场
                        setTableData("% 10.0f", h[0], h[1], h[2], norm(h));
                        lineChartManager.addEntry(Arrays.asList(h[0], h[1], h[2]));
                        break;
                    case 5://端口

                        setTableData("% 10.0f", d[0], d[1], d[2], d[3]);
                        lineChartManager.addEntry(Arrays.asList(d[0], d[1], d[2], d[3]));
                        break;
                    case 6://气压、高度
                        setTableData(String.format("% 10.2fPa", pressure), String.format("% 10.2fPa", height), "", "");
                        lineChartManager.addEntry(Arrays.asList(pressure));
                        break;
                    case 7://经纬度
                        setTableData(String.format("% 14.6f°", longitude), String.format("% 14.6f°", latitude), "", "");
                        break;
                    case 8://海拔、航向、地速
                        setTableData(String.format("% 10.2f", altitude), String.format("% 10.2f°", yaw), String.format("% 10.2fkm/s", velocity), "");
                        break;
                    case 9://四元数
                        setTableData("% 7.4f", q[0], q[1], q[2], q[3]);
                        lineChartManager.addEntry(Arrays.asList(q[0], q[1], q[2], q[3]));
                        break;
                    case 10://卫星数
                        setTableData(String.format("% 5.0f", sn), String.format("% 7.1f", pdop), String.format("% 7.1f", hdop), String.format("% 7.1f", vdop));
                        break;
                }//switch
                if ((iCurrentGroup == 10) || (iCurrentGroup == 8) || (iCurrentGroup == 7) || (iCurrentGroup == 3) || (iCurrentGroup == 0))//10 8 7 3 0
                    lineChartManager.addEntry(Arrays.asList(Angle[0], Angle[1], Angle[2]));

        }
    };

    private boolean checkGpsIsOpen() {
        boolean isOpen;
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        isOpen = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return isOpen;
    }
    private void openGPSSetting() {
        if (checkGpsIsOpen()){
            Toast.makeText(this, "true", Toast.LENGTH_SHORT).show();
        }else {
            new AlertDialog.Builder(this).setTitle("open GPS")
                    .setMessage("go to open")
                    .setNegativeButton("cancel",new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Toast.makeText(DataMonitor.this, "close", Toast.LENGTH_SHORT).show();
                            dialogInterface.dismiss();
                        }
                    })
                    .setPositiveButton("setting", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivityForResult(intent,GPS_REQUEST_CODE);
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
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
        setContentView(R.layout.lay_data);
//        SelectFragment(0);
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
        type = intent.getIntExtra("type", 0);
        MyApp.driver = new CH34xUARTDriver((UsbManager) getSystemService(Context.USB_SERVICE), this, ACTION_USB_PERMISSION);
        if (!MyApp.driver.UsbFeatureSupported())// 判断系统是否支持USB HOST
        {
            Dialog dialog = new AlertDialog.Builder(DataMonitor.this)
                    .setTitle(getString(R.string.hint))
                    .setMessage(getString(R.string.USB_HOST))
                    .setPositiveButton(getString(R.string.Determine),
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface arg0,
                                                    int arg1) {
                                    System.exit(0);
                                }
                            }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);// 保持常亮的屏幕的状态

        writeBuffer = new byte[512];
        readBuffer = new byte[512];
        isOpen = false;
        SerialPortOpen();
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Toast.makeText(this, getString(R.string.Bluetoothbad), Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception err) {
        }
        if (displayThread==null){
            bDisplay = true;
            displayThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (bDisplay) {
                        refreshhandler.sendMessage(Message.obtain());
                        try
                        {
                            Thread.sleep(100);
                        }catch (Exception err){}
                    }
                }
            });
            displayThread.start();
        }
    }
    private boolean bDisplay = true;
    private Thread displayThread;
    private void initButton() {
        //侧滑
        if (!groupList.isEmpty()) groupList.clear();
        ExpandableListView listview = (ExpandableListView) findViewById(R.id.expandableLisView);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawerLayout);
        List<ChildBeen> childBeenList = new ArrayList<>();
        mTitle = (Button) findViewById(R.id.btnBluetoothSet);
        tvLabelX = ((TextView) findViewById(R.id.X));
        tvLabelY = ((TextView) findViewById(R.id.Y));
        tvLabelZ = ((TextView) findViewById(R.id.Z));
        tvLabelAll = ((TextView) findViewById(R.id.All));
        tvX = ((TextView) findViewById(R.id.tvX));
        tvY = ((TextView) findViewById(R.id.tvY));
        tvZ = ((TextView) findViewById(R.id.tvZ));
        tvAll =((TextView) findViewById(R.id.tvAll));

        if (type == 3) {
            findViewById(R.id.button2).setVisibility(View.GONE);
            findViewById(R.id.button4).setVisibility(View.GONE);
            findViewById(R.id.button5).setVisibility(View.GONE);
            findViewById(R.id.button6).setVisibility(View.GONE);
            findViewById(R.id.button7).setVisibility(View.GONE);
            findViewById(R.id.button8).setVisibility(View.GONE);
            findViewById(R.id.button9).setVisibility(View.GONE);
            findViewById(R.id.buttonA).setVisibility(View.GONE);
            GroupBeen grop = new GroupBeen();
            grop.setName(getString(R.string.Add_calibration));
            grop.setChildList(childBeenList);
            GroupBeen grop2 = new GroupBeen();
            grop2.setName(getString(R.string.Smoothness_coefficient));
            grop2.setChildList(childBeenList);
            groupList.add(grop);
            groupList.add(grop2);
        } else if (type == 6) {
            findViewById(R.id.button4).setVisibility(View.GONE);
            findViewById(R.id.button5).setVisibility(View.GONE);
            findViewById(R.id.button6).setVisibility(View.GONE);
            findViewById(R.id.button7).setVisibility(View.GONE);
            findViewById(R.id.button8).setVisibility(View.GONE);
            findViewById(R.id.button9).setVisibility(View.GONE);
            findViewById(R.id.buttonA).setVisibility(View.GONE);
            GroupBeen grop = new GroupBeen();
            grop.setName(getString(R.string.Add_calibration));
            grop.setChildList(childBeenList);
            groupList.add(grop);
            GroupBeen grop2 = new GroupBeen();
            grop2.setName(getString(R.string.dormancy));
            grop2.setChildList(childBeenList);
            groupList.add(grop2);
            GroupBeen grop3 = new GroupBeen();
            grop3.setName(getString(R.string.ToZero));
            grop3.setChildList(childBeenList);
            groupList.add(grop3);
            GroupBeen grop4 = new GroupBeen();
            if(bUsbConnect)
                grop4.setName(getString(R.string.baudrate));
            else
                grop4.setName(getString(R.string.retrieval_rate));
            grop4.setChildList(childBeenList);
            groupList.add(grop4);
            GroupBeen grop5 = new GroupBeen();
            grop5.setName(getString(R.string.Installation_direction));
            grop5.setChildList(childBeenList);
            groupList.add(grop5);
            GroupBeen grop6 = new GroupBeen();
            grop6.setName(getString(R.string.Static_detection_threshold));
            grop6.setChildList(childBeenList);
            groupList.add(grop6);
            GroupBeen grop7 = new GroupBeen();
            grop7.setName(getString(R.string.Measurement_bandwidth));
            grop7.setChildList(childBeenList);
            groupList.add(grop7);
        } else if (type == 9 || isOpen) {
            //系统
            if (bBTConnet){
                findViewById(R.id.button5).setVisibility(View.GONE);
                findViewById(R.id.button7).setVisibility(View.GONE);
                findViewById(R.id.button8).setVisibility(View.GONE);
                findViewById(R.id.buttonA).setVisibility(View.GONE);
            }
            else{
                findViewById(R.id.button5).setVisibility(View.VISIBLE);
                findViewById(R.id.button7).setVisibility(View.VISIBLE);
                findViewById(R.id.button8).setVisibility(View.VISIBLE);
                findViewById(R.id.buttonA).setVisibility(View.VISIBLE);
            }
            GroupBeen system = new GroupBeen();
            system.setName(getString(R.string.system));
            ChildBeen sys1 = new ChildBeen();
            sys1.setName(getString(R.string.Resume_out_of_the_factory));
            ChildBeen sys2 = new ChildBeen();
            sys2.setName(getString(R.string.dormancy));
            ChildBeen sys3 = new ChildBeen();
            sys3.setName(getString(R.string.algorithm));
            ChildBeen sys4 = new ChildBeen();
            sys4.setName(getString(R.string.Installation_direction));
            ChildBeen sys5 = new ChildBeen();
            sys5.setName(getString(R.string.Instruction_start));
            ChildBeen sys6 = new ChildBeen();
            sys6.setName(getString(R.string.Call_the_police));
            List<ChildBeen> sysList = new ArrayList<>();
            sysList.add(sys1);
            sysList.add(sys2);
            sysList.add(sys3);
            sysList.add(sys4);
            sysList.add(sys5);
            sysList.add(sys6);
            system.setChildList(sysList);
            groupList.add(system);

            //校准
            GroupBeen calibration = new GroupBeen();
            calibration.setName(getString(R.string.calibration));
            ChildBeen c1 = new ChildBeen();
            c1.setName(getString(R.string.Add_calibration));
            ChildBeen c2 = new ChildBeen();
            c2.setName(getString(R.string.Magnetic_field_calibration));
            ChildBeen c2ok = new ChildBeen();
            c2ok.setName(getString(R.string.Magnetic_field_calibration_finish));
            ChildBeen c3 = new ChildBeen();
            c3.setName(getString(R.string.High_zero));
            ChildBeen c4 = new ChildBeen();
            c4.setName(getString(R.string.Gyroscope_automatic_calibration));
            ChildBeen c5 = new ChildBeen();
            c5.setName(getString(R.string.Z_axis_angle_to_zero));
            ChildBeen c6 = new ChildBeen();
            c6.setName(getString(R.string.Setting_angle_reference));
            List<ChildBeen> cbList = new ArrayList<>();
            cbList.add(c1);
            cbList.add(c2);
            cbList.add(c2ok);
            cbList.add(c3);
            cbList.add(c4);
            cbList.add(c5);
            cbList.add(c6);
            calibration.setChildList(cbList);
            groupList.add(calibration);

            //范围
            GroupBeen scope = new GroupBeen();
            scope.setName(getString(R.string.Range));
            ChildBeen scope1 = new ChildBeen();
            scope1.setName(getString(R.string.Acceleration_range));
            ChildBeen scope2 = new ChildBeen();
            scope2.setName(getString(R.string.Angular_velocity_range));
            ChildBeen scope3 = new ChildBeen();
            scope3.setName(getString(R.string.bandwidth));
            List<ChildBeen> spcopeList = new ArrayList<>();
            spcopeList.add(scope1);
            spcopeList.add(scope2);
            spcopeList.add(scope3);
            scope.setChildList(spcopeList);
            groupList.add(scope);

            //通信
            GroupBeen communication = new GroupBeen();
            communication.setName(getString(R.string.Signal_communication));
            List<ChildBeen> comList = new ArrayList<>();
            ChildBeen com1 = new ChildBeen();
            com1.setName(getString(R.string.retrieval_rate));
            ChildBeen com2 = new ChildBeen();
            com2.setName(getString(R.string.address));
            comList.add(com1);
            comList.add(com2);
            if (isOpen) {
                ChildBeen com = new ChildBeen();
                com.setName(getString(R.string.Communication_rate));
                comList.add(com);
            }
            communication.setChildList(comList);
            groupList.add(communication);
            if (bUsbConnect) {
                //端口模式
                GroupBeen port = new GroupBeen();
                port.setName(getString(R.string.Port_mode));
                ChildBeen prot1 = new ChildBeen();
                prot1.setName(getString(R.string.d0_mode));
                ChildBeen prot2 = new ChildBeen();
                prot2.setName(getString(R.string.d1_mode));
                ChildBeen prot3 = new ChildBeen();
                prot3.setName(getString(R.string.d2_mode));
                ChildBeen prot4 = new ChildBeen();
                prot4.setName(getString(R.string.d3_mode));
                List<ChildBeen> protLlist = new ArrayList<>();
                protLlist.add(prot1);
                protLlist.add(prot2);
                protLlist.add(prot3);
                protLlist.add(prot4);
                port.setChildList(protLlist);
                groupList.add(port);

                //端口PWM脉宽
                GroupBeen pwm = new GroupBeen();
                pwm.setName(getString(R.string.Port_PWM_pulse_width));
                ChildBeen pwm1 = new ChildBeen();
                pwm1.setName(getString(R.string.D0PWM_pulse_width));
                ChildBeen pwm2 = new ChildBeen();
                pwm2.setName(getString(R.string.D1PWM_pulse_width));
                ChildBeen pwm3 = new ChildBeen();
                pwm3.setName(getString(R.string.D2PWM_pulse_width));
                ChildBeen pwm4 = new ChildBeen();
                pwm4.setName(getString(R.string.D3PWM_pulse_width));
                List<ChildBeen> pwmList = new ArrayList<>();
                pwmList.add(pwm1);
                pwmList.add(pwm2);
                pwmList.add(pwm3);
                pwmList.add(pwm4);
                pwm.setChildList(pwmList);
                groupList.add(pwm);

                //端口PWM周期
                GroupBeen cycle = new GroupBeen();
                cycle.setName(getString(R.string.Port_PWM_cycle));
                ChildBeen cycle1 = new ChildBeen();
                cycle1.setName(getString(R.string.D0PWM_cycle));
                ChildBeen cycle2 = new ChildBeen();
                cycle2.setName(getString(R.string.D1PWM_cycle));
                ChildBeen cycle3 = new ChildBeen();
                cycle3.setName(getString(R.string.D2PWM_cycle));
                ChildBeen cycle4 = new ChildBeen();
                cycle4.setName(getString(R.string.D3PWM_cycle));
                List<ChildBeen> cycleList = new ArrayList<>();
                cycleList.add(cycle1);
                cycleList.add(cycle2);
                cycleList.add(cycle3);
                cycleList.add(cycle4);
                cycle.setChildList(cycleList);
                groupList.add(cycle);
            }
        }
        adapter = new ExLisViewAdapter(this, groupList);
        listview.setAdapter(adapter);
        listview.setGroupIndicator(null);

        if (type == 3) {
            listview.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                @Override
                public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                    drawerLayout.closeDrawer(Gravity.LEFT);
                    if (i == 0) {
                        SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x01, (byte) 0x01, (byte) 0x00});
                    } else if (i == 1) {
                        DevDialog devDialog = DevDialog.newInstance();
                        devDialog.setDevDialogCallBack(new DevDialog.DevDialogCallBack() {
                            @Override
                            public void save(String value) {
                                byte[] values = value.getBytes();
                                if (values.length == 1) {
                                    values[1] = 0x00;
                                }
                                writeReg(0x6c,byteToInt(values[0],values[1]));//SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x6c, values[0], values[1]});
                            }

                            @Override
                            public void back() {
                            }
                        });
                        devDialog.show(getSupportFragmentManager());
                    }
                    return false;
                }
            });
        }

        if (type == 6) {
            listview.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                @Override
                public boolean onGroupClick(ExpandableListView expandableListView, View view, int i, long l) {
                    drawerLayout.closeDrawer(Gravity.LEFT);
                    if (i == 0) {
                        SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x67});
                    } else if (i == 1) {
                        SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x60});
                    } else if (i == 2) {
                        SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x52});
                    } else if (i == 3) {
                        OnClickSetJy61Baud();
                    } else if (i == 4) {
                        Direction601();
                    } else if (i == 5) {
                        staticDetect61();
                    } else if (i == 6) {
                        Bandwidth601();
                    } else if (i == 7) {
                        Mode601();
                    }
                    return false;
                }
            });
        }

        if (type == 9) {
            listview.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i1, long l) {

                    if (i == 0) {
                        if (i1 == 0) {
                            writeLockReg(0x00,0x01);
                        } else if (i1 == 1) {
                            writeLockReg(0x22,0x01);
                        } else if (i1 == 2) {
                            algrithmSet();
                        } else if (i1 == 3) {
                            Direction901();
                        } else if (i1 == 4) {
                            cmdStartUp();
                        } else if (i1 == 5) {
                            alarmSet();
                        }
                        drawerLayout.closeDrawer(Gravity.LEFT);
                    }
                    if (i == 1) {
                        if (i1 == 0) {
                            accCali();
                        } else if (i1 == 1) {
                            writeLockReg(0x01, 0x07 );//开始校准
                            Toast.makeText(getApplicationContext(), getString(R.string.toastCali), Toast.LENGTH_LONG).show();
                        } else if (i1 == 2) {
                            writeAndSaveReg(0x01,0x00);
                            Toast.makeText(getApplicationContext(), getString(R.string.toastCaliFinish), Toast.LENGTH_LONG).show();
                            drawerLayout.closeDrawer(Gravity.LEFT);
                        } else if (i1 == 3) {
                            writeAndSaveReg(0x01, 0x03 );
                            Toast.makeText(getApplicationContext(), getString(R.string.toastCaliFinish), Toast.LENGTH_LONG).show();
                            drawerLayout.closeDrawer(Gravity.LEFT);
                        } else if (i1 == 4) {
                            autoCalibrate();
                            Toast.makeText(getApplicationContext(), getString(R.string.toastCaliFinish), Toast.LENGTH_LONG).show();
                            drawerLayout.closeDrawer(Gravity.LEFT);
                        } else if (i1 == 5) {
                            writeAndSaveReg(0x01, 0x04);
                            Toast.makeText(getApplicationContext(), getString(R.string.toastCaliFinish), Toast.LENGTH_LONG).show();
                            drawerLayout.closeDrawer(Gravity.LEFT);
                        } else if (i1 == 6) {
                            writeAndSaveReg(0x01, 0x08 );
                            Toast.makeText(getApplicationContext(), getString(R.string.toastCaliFinish), Toast.LENGTH_LONG).show();
                            drawerLayout.closeDrawer(Gravity.LEFT);
                        }
                    }
                    if (i == 2) {

                        if (i1 == 0) {
                            accelartionRange();
                        } else if (i1 == 1) {
                            angularVelocityRange();
                        } else if (i1 == 2) {
                            Bandwidth901();
                        }
                        drawerLayout.closeDrawer(Gravity.LEFT);
                    }
                    if (i == 3) {
                        if (i1 == 0) {
                            outputRate();
                        } else if (i1 == 1) {
                            myAddress();
                        } else if (i1 == 2) {
                            ccSpeed();
                        }
                        drawerLayout.closeDrawer(Gravity.LEFT);
                    }
                    if (i == 4) {
                        if (i1 == 0) {
                            String value = getString(R.string.Please_select_the_D0_port_mode);
                            DMode(i1, value);
                        } else if (i1 == 1) {
                            String value = getString(R.string.Please_select_the_D1_port_mode);
                            DMode(i1, value);
                        } else if (i1 == 2) {
                            String value = getString(R.string.Please_select_the_D2_port_mode);
                            DMode(i1, value);
                        } else if (i1 == 3) {
                            String value = getString(R.string.Please_select_the_D3_port_mode);
                            DMode(i1, value);
                        }
                        drawerLayout.closeDrawer(Gravity.LEFT);
                    }
                    if (i == 5) {

                        if (i1 == 0) {
                            Pwm(i1);
                        } else if (i1 == 1) {
                            Pwm(i1);
                        } else if (i1 == 2) {
                            Pwm(i1);
                        } else if (i1 == 3) {
                            Pwm(i1);
                        }
                        drawerLayout.closeDrawer(Gravity.LEFT);
                    }
                    if (i == 6) {
                        if (i1 == 0) {
                            PwmCycle(i1);
                        } else if (i1 == 1) {
                            PwmCycle(i1);
                        } else if (i1 == 2) {
                            PwmCycle(i1);
                        } else if (i1 == 3) {
                            PwmCycle(i1);
                        }
                        drawerLayout.closeDrawer(Gravity.LEFT);
                    }
                    return true;
                }
            });
        }
    }
    private void SendData(byte[] byteSend){
        if (bUsbConnect){
            if (MyApp.driver.isConnected()) MyApp.driver.WriteData(byteSend, byteSend.length);
        }
        else{
            if (mBluetoothService!=null)
                mBluetoothService.Send(byteSend);
        }
    }
    int iChipBaudSelect = 2;
    private void ccSpeed() {
        String[] s = new String[]{"2400", "4800", "9600", "19200", "38400", "57600", "115200"
                , "230400", "460800", "921600"};
        new AlertDialog.Builder(this)
                .setTitle("请选择通信速率：")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iChipBaudSelect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iChipBaudSelect = i;
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x04, (byte)iChipBaudSelect, (byte) 0x00});
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }
    private void PwmCycle(final int pos) {
        PwmCycleDialog pwmCycle = PwmCycleDialog.newInstance();
        pwmCycle.setPwmCycleDialogCallBack(new PwmCycleDialog.PwmCycleDialogCallBack() {
            @Override
            public void save(String value) {
                    writeAndSaveReg(0x16+pos, Integer.parseInt(value));
                }
            @Override
            public void back() {
            }
        });
        pwmCycle.show(getSupportFragmentManager());
    }

    private void Pwm(final int pos) {
        PwmDialog pwmDialog = PwmDialog.newInstance();
        pwmDialog.setPwmDialogCallBack(new PwmDialog.PwmDialogCallBack() {
            @Override
            public void save(String value) {
                writeAndSaveReg(0x12+pos, Integer.parseInt(value));
            }
            @Override
            public void back() {
            }
        });
        pwmDialog.show(getSupportFragmentManager());
    }

    private void myAddress() {
        AddressDialog addDialog = AddressDialog.newInstance();
        addDialog.setAddressDialogCallBack(new AddressDialog.AddressDialogCallBack() {
            @Override
            public void save(String value) {
                int v = Integer.parseInt(value);
                writeAndSaveReg(0x1a, (byte)v );
            }
            @Override
            public void back() {
            }
        });
        addDialog.show(getSupportFragmentManager());
    }


    int iRetrivalRateSelect = 5;
    private void outputRate() {
        String[] s = new String[]{"0.2Hz", "0.5Hz", "1Hz", "2Hz", "5Hz", "10HZ", "20Hz", "50Hz", "100Hz", "125Hz", "200Hz"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.PleaseSelectTheReturnRate))
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
                        writeAndSaveReg(0x03, (byte)(iRetrivalRateSelect+1) );
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }
    int [] iPortMode = new int[]{0,0,0,0};
    private void DMode(final int index, String v) {
        String[] s;
        if (index == 1) {
            s = new String[]{getString(R.string.AnalogInput), getString(R.string.DigitalInput), getString(R.string.Output_digital_high_level)
                    , getString(R.string.Output_digital_low_level), getString(R.string.Output_PWM), getString(R.string.CLR_relative_posture)};
        } else {
            s = new String[]{getString(R.string.AnalogInput), getString(R.string.DigitalInput), getString(R.string.Output_digital_high_level)
                    , getString(R.string.Output_digital_low_level), getString(R.string.Output_PWM)};
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
                        writeAndSaveReg(0x0e+index,iPortMode[index] );
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }


    int iBandwidth901 = 4;
    private void Bandwidth901() {
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
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x1f, (byte)iBandwidth901 );
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }

    int angularVelocityRangeParam = 3;
    private void angularVelocityRange() {
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
                .setPositiveButton((R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x20, (byte)angularVelocityRangeParam );
                        av = 250*(int)Math.pow(2,angularVelocityRangeParam);//1,2,4,8
                        Log.i("range",String.format("w range = %d",av));
                        SharedUtil.putInt("av", av);
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }

    int accRangeParam = 3;
    private void accelartionRange() {
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
                        writeAndSaveReg(0x21, (byte)accRangeParam );
                        ar = (int)Math.pow(2,accRangeParam+1);
                        Log.i("range",String.format("acc range = %d",ar));
                        SharedUtil.putInt("ar", ar);
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }

    int iAutoCali = 0;
    private void autoCalibrate() {
        String[] s = new String[]{"Yes", "No"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.Automatic_calibration_of_helix))
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
                        writeAndSaveReg(0x63, (byte)iAutoCali );
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }

    boolean bMagCali = false;
    private void magCali() {
        if (bMagCali) {
            writeAndSaveReg(0x01,0x00);
            groupList.get(1).getChildList().get(1).setName(getString(R.string.Magnetic_field_calibration));
            adapter.notifyDataSetChanged();
            bMagCali = false;
        } else {//结束校准
            writeLockReg(0x01, 0x07 );//开始校准
            groupList.get(1).getChildList().get(1).setName(getString(R.string.Finish));
            adapter.notifyDataSetChanged();
            bMagCali = true;
        }
    }

    private void accCali() {
        writeLockReg(0x01,  0x01 );
        saveReg(3000);

        Toast.makeText(this, getString(R.string.Calibrating), Toast.LENGTH_LONG).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Toast.makeText(getApplicationContext(), getString(R.string.Calibrated), Toast.LENGTH_SHORT).show();
            }
        }, 3000);

    }

    private void alarmSet() {
        AlarmDialog alarmDialog = new AlarmDialog();
        alarmDialog.setPoliceDialogCallBack(new AlarmDialog.PoliceDialogCallBack() {
            @Override
            public void save(String strXMin, final String strXMax, final String strYMin, final String strYMax, final String time, final int tag) {
                unLockReg(0);
                writeReg(0x5a, Integer.parseInt(strXMin)* 32768 / 180,50 );
                writeReg(0x5a, Integer.parseInt(strXMax)* 32768 / 180,100 );
                writeReg(0x5a, Integer.parseInt(strYMin)* 32768 / 180,150 );
                writeReg(0x5a, Integer.parseInt(strYMax)* 32768 / 180,200 );
                writeReg(0x5a, Integer.parseInt(time));
                if (tag == 0) writeReg(0x62, 0x00,250);
                else if (tag == 1) writeReg(0x62, 0x01,250 );
                saveReg(300);
            }
            @Override
            public void back() {
            }
        });
        alarmDialog.show(getSupportFragmentManager());
    }

    int iAlgrithm = 1;
    private void algrithmSet() {
        String[] s = new String[]{getString(R.string.SixAxisAlgorithm), getString(R.string.NineAxisAlgorithm)};
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
                            writeAndSaveReg(0x24,0x01);
                        } else if (iAlgrithm == 1) {
                            writeAndSaveReg(0x24,0x00);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }

    private void SerialPortClose() {
        MyApp.driver.CloseDevice();
        isOpen = false;
    }

    private Thread readThread;
    boolean bUsbConnect = false;
    private boolean SerialPortOpen() {
        retval = MyApp.driver.ResumeUsbList();
        if (retval == -1)// ResumeUsbList方法用于枚举CH34X设备以及打开相关设备
        {
            MyApp.driver.CloseDevice();
        } else if (retval == 0) {
            if (!MyApp.driver.UartInit()) {//对串口设备进行初始化操作
                Toast.makeText(DataMonitor.this, getString(R.string.fail_to_open), Toast.LENGTH_SHORT).show();
                return false;
            }
            Toast.makeText(DataMonitor.this, getString(R.string.open_device_suc), Toast.LENGTH_SHORT).show();
            if (MyApp.driver.isConnected()) {
                USBBaudInit();
                bBTConnet = false;
                bUsbConnect = true;
            }
            isOpen = true;

            return true;
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.Unauthorized_authority));
            builder.setMessage(getString(R.string.quit));
            builder.setPositiveButton(getString(R.string.Determine), new DialogInterface.OnClickListener() {
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
            if (mBluetoothService == null)
                mBluetoothService = new BluetoothService(this, mHandler); // 用来管理蓝牙的连接
            else
                mBluetoothService.stop();
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
        } catch (Exception err) {

        }
    }

//    @SuppressLint("NewApi")
//    private void SelectFragment(int Index) {
//        // TODO Auto-generated method stub
//        android.support.v4.app.FragmentManager manager = getSupportFragmentManager();
//        android.support.v4.app.FragmentTransaction transaction = manager.beginTransaction();
//        if (dataFragment == null) {
//            dataFragment = new DataFragment();
//            transaction.add(R.id.id_content, dataFragment);
//        }
//        if (usFragment == null) {
//            usFragment = new UsFragment();
//            transaction.add(R.id.id_content, usFragment);
//        }
//        switch (Index) {
//            case 0:
//                transaction.show(dataFragment);
//                transaction.hide(usFragment);
//                break;
//            case 1:
//                transaction.hide(dataFragment);
//                transaction.show(usFragment);
//                break;
//            default:
//                break;
//        }
//        transaction.commit();
//    }

    @Override
    public void onStart() {
        super.onStart();
    }

    public synchronized void onResume() {
        super.onResume();
        initButton();
        if (bUsbConnect==false) {
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
                    } else {
                        Log.e("--", "service is null");
                        String address = SharedUtil.getString("BTName");
                        if (address != null) {
                            Log.e("--", "BTName = " + address);
                            mBluetoothService = new BluetoothService(getApplicationContext(), mHandler); // 用来管理蓝牙的连接
                            device = mBluetoothAdapter.getRemoteDevice(address);// Get the BLuetoothDevice object
                            mBluetoothService.connect(device);// Attempt to connect to the device
                        } else {
                            onClickedBTSet(null);
                        }
                    }
                }
            }, 1000);
        }
        if (lineChart==null) {
            lineChart = (LineChart) findViewById(R.id.lineChart);
            lineChartManager = new LineChartManager(lineChart, Arrays.asList("AngleX", "AngleY", "AngleZ"), qColour);
            lineChartManager.setDescription(getString(R.string.angle_Chart));
        }
        outputSwitch = (Switch)findViewById(R.id.dataSwitch);
        if (type==9) outputSwitch.setVisibility(View.VISIBLE);
        else outputSwitch.setVisibility(View.INVISIBLE);
        if (readThread==null) {
            readThread = new Thread(new Runnable() {
                public void run() {
                    byte[] buffer = new byte[4096];
                    while (true) {
                        if (!isOpen) break;
                        int length = MyApp.driver.ReadData(buffer, 4096);
                        if (length > 0) {
                            CopeSerialData(length, buffer);
                        }
                    }
                    try {
                        Thread.sleep(10);
                    } catch (Exception err) {
                    }
                }
            });
            readThread.start();
        }//开启读线程读取串口接收的数据
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothService != null) mBluetoothService.stop();
        SerialPortClose();
        bDisplay = false;
    }

    public BluetoothDevice device;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE:// When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);// Get the device MAC address
                    device = mBluetoothAdapter.getRemoteDevice(address);// Get the BLuetoothDevice object
                    SharedUtil.putString("BTName",address);
                    mBluetoothService.connect(device);// Attempt to connect to the device
                }
                break;
            case GPS_REQUEST_CODE:
                openGPSSetting();
                break;
        }
    }

    int iJY61Baud = 0;
    int iJY61RateSelect = 0;
    public void OnClickSetJy61Baud() {
        if(bUsbConnect)
        {
            switch (iBaud) {
                case 9600:
                    iJY61Baud = 0;
                    break;
                case 115200:
                    iJY61Baud = 1;
                    break;
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.PleaseSelectTheReturnRate))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setSingleChoiceItems(new String[]{ "9600", "115200"}, iJY61Baud, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            iJY61Baud = i;
                        }
                    })
                    .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            try {
                                SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x64 - iJY61Baud)});
                                Thread.sleep(100);
                                if (iJY61Baud==0) iBaud = 9600;
                                else iBaud = 115200;
                                if (MyApp.driver.isConnected()) {
                                    MyApp.driver.SetConfig(iBaud, (byte) 8, (byte) 0, (byte) 0, (byte) 0);
                                }
                            } catch (Exception err) {
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.abolish), null)
                    .show();
        }
        else{
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.PleaseSelectTheReturnRate))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setSingleChoiceItems( new String[]{"20Hz", "100Hz"}, iJY61RateSelect, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            iJY61RateSelect = i;
                        }
                    })
                    .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            try {
                                SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x64 - iJY61RateSelect)});
                            } catch (Exception err) {
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.abolish), null)
                    .show();
        }
    }

    int iDirection = 0;
    public void Direction601() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.please_choose_install_derection))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{getString(R.string.Horizontal_installation), getString(R.string.Vertical_installation)}, iDirection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iDirection = i;
                    }
                })
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        if (iDirection == 0) {
                            SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x65});
                        } else if (iDirection == 1) {
                            SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x66});
                        }
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }

    int getiDirection901 = 1;
    public void Direction901() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.please_choose_install_derection))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{getString(R.string.Horizontal_installation), getString(R.string.Vertical_installation)}, getiDirection901, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        getiDirection901 = i;
                    }
                })
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                            writeAndSaveReg(0x23,0x01-getiDirection901);
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }

    int iCmdStartup = 1;
    public void cmdStartUp() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.Whether_or_not))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(new String[]{"Yes", "No"}, iCmdStartup, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iCmdStartup = i;
                    }
                })
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        writeAndSaveReg(0x2d,iCmdStartup);
                    }
                })
                .setNegativeButton((R.string.abolish), null)
                .show();
    }

    int iStaticDetect61 = 4;
    public void staticDetect61() {
        String[] s = new String[]{"0.122°/s", "0.244°/s", "0.366°/s", "0.488°/s", "0.610°/s", "0.732°/s", "0.854°/s", "0.976°/s"
                , "1.098°/s", "1.221°/s", "1.343°/s", "1.456°/s", "1.587°/s", "1.709°/s", "1.831°/s"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.Static_detection_threshold))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iStaticDetect61, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iStaticDetect61 = i;
                    }
                })
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x71+iStaticDetect61)});
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }

    int iBandwidth61 = 4;
    public void Bandwidth601() {
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
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x81+iBandwidth61)});
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }

    int iMode61 = 0;
    public void Mode601() {
        String[] s = new String[]{"Serial", "IIC"};
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_modle))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setSingleChoiceItems(s, iMode61, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        iMode61 = i;
                    }
                })
                .setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) (0x61+iMode61)});
                    }
                })
                .setNegativeButton(getString(R.string.abolish), null)
                .show();
    }

//    int[] iBauds = new int[]{2400, 4800, 9600, 19200, 38400, 5760, 115200, 230400, 460800, 921600};
//    int iBaudIndex = 2;
//    public void OnClickSetJy901Baud(View v) {
//        for (int i = 0; i < iBauds.length; i++) {
//            if (iBauds[i] == iBaud) {
//                iBaudIndex = i;
//                break;
//            }
//        }
//        new AlertDialog.Builder(this)
//                .setTitle("请选择波特率：")
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .setSingleChoiceItems(new String[]{"2400", "4800", "9600", "19200", "38400", "57600", "115200", "230400", "460800", "921600"}, iBaudIndex, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        iBaudIndex = i;
//                    }
//                })
//                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface arg0, int arg1) {
//                        SharedPreferences mySharedPreferences = getSharedPreferences("Output", Activity.MODE_PRIVATE);
//                        SharedPreferences.Editor editor = mySharedPreferences.edit();
//                        editor.putString("Baud", String.format("%d", iBaudIndex));
//                        editor.commit();
//                        try {
//                            SendData(new byte[]{(byte) 0xff, (byte) 0xaa, (byte) 0x04, (byte) iBaudIndex, (byte) 0});
//                            Thread.sleep(100);
//                            if (MyApp.driver.isConnected()) {
//                                MyApp.driver.SetConfig(iBauds[iBaudIndex], (byte) 8, (byte) 0, (byte) 0, (byte) 0);
//                                iBaud = iBauds[iBaudIndex];
//                                ;
//                            }
//                        } catch (Exception err) {
//                        }
//                    }
//                })
//                .setNegativeButton("取消", null)
//                .show();
//    }

    public void OnClickConfig(View v) {
        if (v.getId() == R.id.btnBluetoothSet) {
            onClickedBTSet(v);
        }
    }


    static boolean[] OutputPackage = new boolean[]{true, true, true, true, true, true, true, true, true, true, true};
    public void setOutputBoolean(int iOut) {
        if (iOut==-1) iOut = 0x0F;
        for (int i = 0; i < OutputPackage.length; i++) {
            OutputPackage[i] = ((iOut >> i) & 0x01) == 0x01;
        }
    }
    public int getOutputInt(){
        int iTemp = 0;
        for (int i = 0; i < OutputPackage.length; i++) {
            if (OutputPackage[i]) iTemp |= 0x01 << i;
        }
        return  iTemp;
    }
    public void onConfigClick(View v) {
        drawerLayout.openDrawer(Gravity.LEFT);
    }
    boolean bPause = false;
    public void onClickPause(View v){
        bPause = !bPause;
    }

    public  void onClickRecord(View v){
        if (this.recordStartorStop == false) {
            this.recordStartorStop = true;
            setRecord(true);
            ((Button) v).setText(getString(R.string.stop));
        } else {
            this.recordStartorStop = false;
            setRecord(false);
            ((Button) findViewById(R.id.btnRecord)).setText(getString(R.string.record));
            if (myFile==null) {
                Toast.makeText(DataMonitor.this, "No file recorded!", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.hint))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setMessage(getString(R.string.recorded_root_directory) + myFile.file.getPath() + getString(R.string.open_file))
                    .setPositiveButton(getString(R.string.Determine), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            try {
                                myFile.openFile(getApplicationContext());
                            } catch (Exception err) {
                                Log.e("--",err.toString());
                            }
                        }
                    })
                    .setNegativeButton(getString(R.string.abolish), null)
                    .show();
        }
    }
    @Override
    public void onClick(View v) {
    }

}
