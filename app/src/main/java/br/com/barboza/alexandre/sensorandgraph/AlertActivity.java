package br.com.barboza.alexandre.sensorandgraph;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class AlertActivity extends AppCompatActivity implements View.OnClickListener {
    private Button mButton;
    private EditText mText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setTitle("AppFit - Alerta");
        setContentView(R.layout.activity_alert);
        mButton = (Button) findViewById(R.id.button_ok);
        mButton.setOnClickListener(this);
        mText = (EditText) findViewById(R.id.edit_text);
    }

    @Override
    public void onClick(View view) {
        Intent intent = getIntent();
        if (!mText.getText().toString().isEmpty()) {
            intent.putExtra("VALUE", mText.getText().toString());
            setResult(Activity.RESULT_OK, intent);
        } else {
            setResult(Activity.RESULT_CANCELED, intent);
        }
        finish();
    }
}
