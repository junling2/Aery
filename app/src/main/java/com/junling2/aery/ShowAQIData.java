package com.junling2.aery;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ShowAQIData extends AppCompatActivity {

    String lat, lon;
    String token = "ae555c76ba3592734fb1ff2a1c4a7832623e3644";
    final OkHttpClient httpClient = new OkHttpClient();
    List<String> pm25, pm10, uvi;
    String AQI, lastUpdated;
    TextView tv_aqi_result, tv_date, tv_aqi_scale;
    BarChart PM25, PM10, UVI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_aqi_data);

        tv_aqi_result = findViewById(R.id.tv_aqi_result);
        tv_aqi_scale = findViewById(R.id.tv_aqi_scale);
        tv_date = findViewById(R.id.tv_date);
        PM25 = findViewById(R.id.chart_PM);
        PM10 = findViewById(R.id.chart_pm10);
        UVI = findViewById(R.id.chart_uvi);
        lat = getIntent().getStringExtra("LAT");
        lon = getIntent().getStringExtra("LONG");

        try {
            getAQI(lat, lon);
        } catch (Exception e) {
            e.printStackTrace();
        }
        createBarChart();
        updateUI();
    }

    private void getAQI(String lat, String lon) throws Exception {
        String url = "https://api.waqi.info/feed/geo:" + lat + ";" + lon + "/?token=" + token;
        Request request = new Request.Builder().url(url).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code" + response);
            }
            String json = Objects.requireNonNull(response.body()).string();
            JSONObject object = new JSONObject(json);

            Map data = ((Map) object.get("data"));
            AQI = (String) data.get("aqi");
            Map time = ((Map) object.get("time"));
            lastUpdated = (String) time.get("s");

            Map daily = (Map) ((Map)object.get("forecast")).get("daily");
            JSONArray ja1 = (JSONArray) daily.get("pm25");
            JSONArray ja2 = (JSONArray) daily.get("pm10");
            JSONArray ja3 = (JSONArray) daily.get("uvi");

            for (int i = 0; i < 5; i++) {
                JSONObject jo1 = ja1.getJSONObject(i);
                String pmData = (String) jo1.get("avg");
                pm25.add(pmData);

                JSONObject jo2 = ja2.getJSONObject(i);
                String pmData2 = (String) jo2.get("avg");
                pm10.add(pmData2);

                JSONObject jo3 = ja3.getJSONObject(i);
                String pmData3 = (String) jo3.get("avg");
                uvi.add(pmData3);
            }
        }
    }

    private ArrayList getXAxis() {
        ArrayList res = new ArrayList();
        Map<Integer, String> daysOfWeek = new HashMap<>();
        daysOfWeek.put(1, "SUN");
        daysOfWeek.put(2, "MON");
        daysOfWeek.put(3, "TUE");
        daysOfWeek.put(4, "WED");
        daysOfWeek.put(5, "THU");
        daysOfWeek.put(6, "FRI");
        daysOfWeek.put(7, "SAT");

        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_WEEK);

        for (int i = 0; i < 5; i++) {
            res.add(daysOfWeek.get(day));
            day = (day+1) % 7;
        }
        return res;
    }

    private BarDataSet getDataSet(List<String> ls, int label) {
        ArrayList valueSet = new ArrayList();
        Map<Integer, String> map = new HashMap<>();
        map.put(1, "PM2.5");
        map.put(2, "PM10");
        map.put(3, "UV Index");
        for (int i = 0; i < 5; i++) {
            BarEntry be = new BarEntry(i, Float.parseFloat(ls.get(i)));
            valueSet.add(be);
        }
        BarDataSet res = new BarDataSet(valueSet, map.get(label));
        return res;
    }

    private void createBarChart() {
        BarDataSet barDataSet1 = getDataSet(pm25, 1);
        BarDataSet barDataSet2 = getDataSet(pm10, 2);
        BarDataSet barDataSet3 = getDataSet(uvi, 3);

        BarData barData1 = new BarData(barDataSet1);
        barData1.setBarWidth(0.6f);
        PM25.setData(barData1);
        PM25.setFitBars(true);

        BarData barData2 = new BarData(barDataSet2);
        barData2.setBarWidth(0.6f);
        PM10.setData(barData2);
        PM10.setFitBars(true);

        BarData barData3 = new BarData(barDataSet3);
        barData3.setBarWidth(0.6f);
        UVI.setData(barData3);
        UVI.setFitBars(true);

        ArrayList<String> xAxis = getXAxis();
        ValueFormatter valueFormatter = new ValueFormatter() {
            @Override
            public String getAxisLabel(float value, AxisBase axis) {
                return xAxis.get((int) value);
            }
        };

        XAxis axis1 = PM25.getXAxis();
        axis1.setValueFormatter(valueFormatter);

        XAxis axis2 = PM10.getXAxis();
        axis2.setValueFormatter(valueFormatter);

        XAxis axis3 = UVI.getXAxis();
        axis3.setValueFormatter(valueFormatter);
    }

    private void updateUI() {
        tv_aqi_result.setText(AQI);
        int aqi = Integer.parseInt(AQI);

        if (aqi <= 50) {
            tv_aqi_scale.setText("Good");
            tv_aqi_scale.setTextColor(Color.GREEN);
        } else if (aqi <= 100) {
            tv_aqi_scale.setText("Moderate");
            tv_aqi_scale.setTextColor(Color.YELLOW);
        } else if (aqi <= 150) {
            tv_aqi_scale.setText("Unhealthy for Sensitive Groups");
            tv_aqi_scale.setTextColor(Color.parseColor("e67e22"));
        } else if (aqi <= 200) {
            tv_aqi_scale.setText("Unhealthy");
            tv_aqi_scale.setTextColor(Color.RED);
        } else if (aqi <= 300) {
            tv_aqi_scale.setText("Very Unhealthy");
            tv_aqi_scale.setTextColor(Color.parseColor("7d3c98"));
        } else {
            tv_aqi_scale.setText("Hazardous");
            tv_aqi_scale.setTextColor(Color.parseColor("7d6608"));
        }

        tv_date.setText(lastUpdated);

        PM25.animateXY(2000, 2000);
        PM25.invalidate();
        PM10.animateXY(2000, 2000);
        PM10.invalidate();
        UVI.animateXY(2000, 2000);
        UVI.invalidate();
    }
}