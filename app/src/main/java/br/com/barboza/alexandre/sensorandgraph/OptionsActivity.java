package br.com.barboza.alexandre.sensorandgraph;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class OptionsActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener, View.OnClickListener{
    private SeekBar skb_scale, skb_speed;
    int vScale, vSpeed;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_options);
        this.setTitle("AppFit - Opções");
        mButton = (Button) findViewById(R.id.button_ok);
        mButton.setOnClickListener(this);
        skb_scale = (SeekBar) findViewById(R.id.seekbar_scale);
        skb_speed = (SeekBar) findViewById(R.id.seekbar_speed);
        skb_scale.setOnSeekBarChangeListener(this);
        skb_speed.setOnSeekBarChangeListener(this);
        vScale = vSpeed = 5;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (b && i > 1) {
            switch (seekBar.getId()) {
                case R.id.seekbar_scale:
                    vScale = i;
                    break;
                case R.id.seekbar_speed:
                    vSpeed = i;
                    break;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onClick(View view) {
        Intent intent = getIntent();
        if (vScale != 5 || vSpeed != 5) {
            if (vScale != 5) {
                intent.putExtra("STEP", vScale);
            }
            if (vSpeed != 5) {
                intent.putExtra("TIME", vSpeed);
            }
            setResult(Activity.RESULT_OK, intent);
        } else {
            setResult(Activity.RESULT_CANCELED, intent);
        }
        finish();
    }
}
