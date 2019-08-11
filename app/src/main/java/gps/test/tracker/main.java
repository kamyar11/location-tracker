package gps.test.tracker;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
    private View permission_warning;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST=0;
    private LocationManager locationManager;

    @TargetApi(23)
    private void handle_permissions(){
        if (ContextCompat.checkSelfPermission(main.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?

            if (ActivityCompat.shouldShowRequestPermissionRationale(main.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                //it was just denied, let the user decide
                ActivityCompat.requestPermissions(main.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION},
                        WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
            } else {
                ActivityCompat.requestPermissions(main.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
                //it was denied permanently; either user made this decision or the OS policies prevented it from being granted
            }
        } else {
            init_activity();
        }
    }
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        permission_warning=findViewById(R.id.permission_warrning);
        findViewById(R.id.ask_permissions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handle_permissions();
            }
        });
        if(Build.VERSION.SDK_INT>22)handle_permissions();


    }
    private void init_activity(){
        //permission 'granet' :)
        permission_warning.setVisibility(View.GONE);
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
                    Toast.makeText(getApplicationContext(),"GPS sensor is turned off",Toast.LENGTH_LONG).show();
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
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST){
            /*if both permissions are granted start the activity, else display an explaination about why the
            permissions are necessary;
            * */
            if(grantResults.length>1&&((grantResults[0]|grantResults[1])== PackageManager.PERMISSION_GRANTED)){
                init_activity();
            }else{
                //show an explanation and have user decide;
                if(permission_warning.getVisibility()==View.GONE)permission_warning.setVisibility(View.VISIBLE);
            }
        }
    }
}
