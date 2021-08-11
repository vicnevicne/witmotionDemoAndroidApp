package com.bt901;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by 葛文博 on 2017/10/16.
 */
public class LineChartManager {
    public static final String TAG = LineChartManager.class.getName();
    private final LineChart lineChart;
    private final YAxis leftAxis;
    private final XAxis xAxis;
    private LineData lineData;
    private LineDataSet lineDataSet;
    private final List<ILineDataSet> lineDataSets = new ArrayList<>();
    private final List<String> timeList = new ArrayList<>(); // Store the time of the x-axis
    private int times = 1;
    private int iPointCnts;


    // Multiple charts
    public LineChartManager(LineChart mLineChart, List<String> names, List<Integer> colors) {
        this.lineChart = mLineChart;
        leftAxis = lineChart.getAxisLeft();
        xAxis = lineChart.getXAxis();
        lineDataSets.clear();
        timeList.clear();
        initLineChart();
        initLineDataSet(names, colors);
        iPointCnts = 100;
        Log.i(TAG, "Initialized.");
    }

    /**
     * 初始化LineChar
     */
    private void initLineChart() {
        lineChart.setTouchEnabled(true);
        lineChart.setDrawGridBackground(false);
        lineChart.setDrawBorders(true);
        // Line legend label settings
        Legend legend = lineChart.getLegend();
        legend.setForm(Legend.LegendForm.LINE);
        legend.setTextSize(11f);
        // Display position
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        // X axis position is at the bottom
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(10);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float v, AxisBase axisBase) {
                int i = (int) (v % timeList.size());
                if (i <= 0) {
                    return "";
                }
                else {
                    return timeList.get((int) v % timeList.size());
                }
            }
        });

        leftAxis.resetAxisMinimum();
    }


    /**
     * Initialize polyline (multiple lines)
     *
     */
    public void initLineDataSet(List<String> names, List<Integer> colors) {
        for (int i = 0; i < names.size(); i++) {
            lineDataSet = new LineDataSet(null, names.get(i));
            lineDataSet.setColor(colors.get(i));
            lineDataSet.setLineWidth(1.5f);
            lineDataSet.setCircleRadius(1.5f);
            lineDataSet.setColor(colors.get(i));
            lineDataSet.setDrawValues(false);
            lineDataSet.setDrawCircles(false);

            lineDataSet.setDrawFilled(false);
            lineDataSet.setCircleColor(colors.get(i));
            lineDataSet.setHighLightColor(colors.get(i));
            lineDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            lineDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            lineDataSet.setValueTextSize(10f);
            lineDataSets.add(lineDataSet);

        }
        // Add an empty LineData
        lineData = new LineData();
        lineChart.setData(lineData);
        lineChart.invalidate();
    }


    /**
     * Dynamically add data (multiple line graphs)
     */
    long lTimeStart = System.currentTimeMillis();
    long lTimeLast = System.currentTimeMillis();

    private boolean bPause = false;

    public void setbPause(boolean b) {
        bPause = b;
    }

    public void addEntry(List<Float> numbers) {
        if (bPause) return;

        lTimeLast = System.currentTimeMillis();

        if (lineDataSets.size() != numbers.size()) return;
        if (lineDataSets.get(0).getEntryCount() == 0) {
            lineData = new LineData(lineDataSets);
            lineChart.setData(lineData);
        }
        if (timeList.size() > 100) {
            timeList.clear();
        }
        timeList.add(String.format("%.1f", (float) (System.currentTimeMillis() - lTimeStart) / 1000.0));
        if ((System.currentTimeMillis() - lTimeLast) > 10000) {

            lineChart.setVisibleXRangeMaximum(iPointCnts);
            Log.e("x", String.format("cnts:%d ", iPointCnts));
            iPointCnts = 0;
            lTimeLast = System.currentTimeMillis();
        }
        for (int i = 0; i < numbers.size(); i++) {
            Entry entry = new Entry(lineDataSet.getEntryCount(), numbers.get(i));
            lineData.addEntry(entry, i);
        }
        lineData.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.moveViewToX(lineData.getEntryCount() - lineChart.getVisibleXRange());
        leftAxis.setAxisMaximum(lineChart.getYMax());
        leftAxis.setAxisMinimum(lineChart.getYMin());
        lineChart.setVisibleXRangeMaximum(100);
    }

    /**
     * Set description information
     */
    public void setDescription(String str) {
        Description description = new Description();
        description.setText(str);
        lineChart.setDescription(description);
        lineChart.invalidate();
    }


    public Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    handler.removeMessages(0);
                    handler.sendEmptyMessageDelayed(0, 1000);
                    times++;
                    Log.e("---", "Times:" + times);
                    timeList.add(times + "");
                    break;
                case 1:
                    handler.removeMessages(0);
                    break;
            }
        }
    };
}
