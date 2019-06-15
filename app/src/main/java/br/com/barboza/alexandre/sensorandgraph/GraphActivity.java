package br.com.barboza.alexandre.sensorandgraph;

/*
  Author:  Alexandre A. Barbosa
  E-mal:   alexandrebarboza@globo.com
  Whats:  (21) 99401-8767
  Release: 16/10/2018
*/

import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

public class GraphActivity extends AppCompatActivity implements SensorEventListener {
    final int ALERT_REQUEST = 1;
    final int OPTIONS_REQUEST = 2;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private GraphView graph;
    private double ax[], ay[], az[];
    private int i_ax, i_ay, i_az;
    private final Handler mHandler = new Handler();
    private Runnable mTimer;
    private LineGraphSeries<DataPoint> mSeries;
    private EditText text;
    private int step; // min 20, max 200
    private int time; // min 20  max 200
    private double limit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle("AppFit - Home");
        setContentView(R.layout.activity_graph);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        graph = (GraphView) findViewById(R.id.graph);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        step = time = 50;
        setScaleSpeedArray();
        mSeries = new LineGraphSeries<>(generateData());
        text = (EditText) findViewById(R.id.edit_text);
        text.setText("0.0");
        limit = 0.0;
    }

    @Override
    protected void onResume() {
        super.onResume();
        graph.addSeries(mSeries);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mTimer = new Runnable() {
            @Override
            public void run() {
                mSeries.resetData(generateData());
                mHandler.postDelayed(this, time);
            }
        };
        mHandler.postDelayed(mTimer, time);
    }

    @Override
    protected void onPause() {
        super.onPause();
        graph.removeAllSeries();
        mSensorManager.unregisterListener(this);
        mHandler.removeCallbacks(mTimer);
        resetValuesToSensor();
    }

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {

    }

    @Override
     public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            setValuesToSensor(ax, i_ax, (int) event.values[0], 1);
            setValuesToSensor(ay, i_ay, (int) event.values[1], 2);
            setValuesToSensor(az, i_az, (int) event.values[2], 3);
            double m = Math.max(Math.max(ax[i_ax-1], ay[i_ay-1]), az[i_az-1]);
            text.setText(String.format("%.2f", m));
            if (limit > 0 && m > limit) {
                onPause();
                ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
                toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
                displayAlert(limit);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.graphic, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent it;
        if(item.getItemId() == R.id.m_alerta) {
            it = new Intent(this, AlertActivity.class);
            startActivityForResult(it, ALERT_REQUEST);
        } else if (item.getItemId() == R.id.m_options) {
            it = new Intent(this, OptionsActivity.class);
            startActivityForResult(it, OPTIONS_REQUEST);
        } else if (item.getItemId() == R.id.m_sair) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == ALERT_REQUEST) {
            if (resultCode == RESULT_OK) {
                String value = data.getStringExtra("VALUE");
                try {
                    limit = Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    limit = 0.0;
                }
            }
        } else if (requestCode == OPTIONS_REQUEST) {
            onPause();
            int scale;
            int speed;
            if (resultCode == RESULT_OK) {
                scale = data.getIntExtra("STEP", 5);
                speed = data.getIntExtra("TIME", 5);
            } else {
                scale = speed = 5;
            }
            step = scale * 10;
            time = speed * 10;
            setScaleSpeedArray();
            onResume();
        }
    }

    private DataPoint[] generateData() {
        DataPoint[] values = new DataPoint[step +1];
        for (int i = 0; i < (step +1); i++) {
            double x = i;
            double y = Math.max(Math.max(ax[i], ay[i]), az[i]);
            DataPoint v = new DataPoint(x, y);
            values[i] = v;
        }
        return values;
    }

    private void setValuesToSensor(double a[], int i, int v, int id) {
        if (i == (step +1)) {
            i = 0;
        }
        a[i] = v;
        i++;
        switch (id) {
            case 1:
                ax = a;
                i_ax = i;
                break;
            case 2:
                ay = a;
                i_ay = i;
                break;
            case 3:
                az = a;
                i_az = i;
                break;
            default:
                return;
        }
    }

    private void resetValuesToSensor() {
        for (int i = 0; i < (step +1); i++) {
            ax[i] = 0;
            ay[i] = 0;
            az[i] = 0;
        }
        i_ax = i_ay = i_az = 0;
    }

    private void displayAlert(Double f) {
        AlertDialog alertDialog = new AlertDialog.Builder(GraphActivity.this).create();
        alertDialog.setTitle("Aviso:");
        alertDialog.setMessage("O valor limite (" + f + ") do gráfico foi ultrapassado!");
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        onResume();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancela",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        alertDialog.show();
    }

    private void setScaleSpeedArray() {
        int min = (step / 10) * (-1);
        int max = step / 2;
        graph.getViewport().setMinX(0);
        graph.getViewport().setMaxX(step);
        graph.getViewport().setMinY(min);
        graph.getViewport().setMaxY((max));
        ax = ay = az = new double[step +1];
        i_ax = i_ay = i_az = 0;
    }
}
