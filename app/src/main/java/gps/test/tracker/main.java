package gps.test.tracker;

import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * Created by user_0 on 9/2/2017.
 */

public class main extends AppCompatActivity {
    private Button start_service,stop_service,show_result_but;
    private TextView textView;
    private EditText dist,interv;
    private Intent intent;

    @TargetApi(23)
    protected void askPermissions() {
        String[] permissions = {
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION"
        };
        int requestCode = 200;
        requestPermissions(permissions, requestCode);
    }

    private LocationManager locationManager;
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        if(Build.VERSION.SDK_INT>22)askPermissions();
        locationManager=(LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        start_service=(Button)findViewById(R.id.start_service_id);stop_service=(Button)findViewById(R.id.stop_service_id);show_result_but=(Button)findViewById(R.id.show_result_but_id);

        intent=new Intent(getApplicationContext(),back_ground_tracking.class);
        new File("sdcard/data_cap");

        start_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Start();
            }
        });
        stop_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start_service.setText("Start tracking location");
                stopService(intent);
            }
        });
        show_result_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(new File("sdcard/data_cap").length()>0){
                Intent intent1=new Intent(getApplicationContext(),show_results.class);
                startActivity(intent1);}
                else{
                    Toast.makeText(getApplicationContext(),"No location recorded" ,Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
    private void Start(){
        final Dialog dialog=new Dialog(main.this);
        dialog.setContentView(R.layout.options);
        dialog.show();
        dist=(EditText)dialog.findViewById(R.id.distance_id);interv=(EditText)dialog.findViewById(R.id.interval_id);
        dialog.findViewById(R.id.done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    Toast.makeText(getApplicationContext(),"gps sensor is turned off",Toast.LENGTH_LONG).show();
                    return;
                }
                intent.putExtra("distance",back_ground_tracking.Stringnumtoint(dist.getText().toString()));
                intent.putExtra("interval",back_ground_tracking.Stringnumtoint(interv.getText().toString())*60000);
                startService(intent);
                start_service.setText("Restart with new configuration");
                dialog.dismiss();
            }
        });
    }
}
