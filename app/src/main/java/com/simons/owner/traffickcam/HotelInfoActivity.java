package com.simons.owner.traffickcam;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class HotelInfoActivity extends AppCompatActivity {

    TextView textView, textView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.simons.owner.traffickcam.R.layout.activity_hotel_info);
        String hotel_name = getIntent().getStringExtra("HOTEL");
        String address = getIntent().getStringExtra("ADDRESS");
        textView = (TextView) findViewById(com.simons.owner.traffickcam.R.id.textView);
        textView2 = (TextView) findViewById(com.simons.owner.traffickcam.R.id.textView2);
        textView.setText(hotel_name);
        textView2.setText(address);

    }

    public void exitOnClick(View view)
    {
        Intent intent = new Intent(this, ExitActivity.class);
        startActivity(intent);
    }
}
