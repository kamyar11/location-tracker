package gps.test.tracker;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Fade;
import androidx.transition.TransitionManager;

import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by user_0 on 9/2/2017.
 */

public class main extends AppCompatActivity implements OnMapReadyCallback {
    private TextView start_service_button, stop_service_button,show_result_but;
    private EditText dist,interv;
    private Intent location_tracker_service_intent;
    private boolean app_exited;
    private Database_io db_io;
    private Thread check_if_service_is_running_thread;
    private View permission_warning;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST=0;
    private LocationManager locationManager;
    private RelativeLayout main_menu;

    private RecyclerView recyclerView;
    private fileadapter adap;
    private MapView mapView;
    private GoogleMap googleMap;

    public static class location_info {
        public double lat, longt, alt;
        long timestamp;
        private String date;
    }

    private ArrayList<location_info> data = new ArrayList<>();


    @TargetApi(23)
    private void handle_permissions(){
        if ((ContextCompat.checkSelfPermission(main.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)|ContextCompat.checkSelfPermission(main.this,
                Manifest.permission.ACCESS_FINE_LOCATION))
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?

            if (ActivityCompat.shouldShowRequestPermissionRationale(main.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)||ActivityCompat.shouldShowRequestPermissionRationale(main.this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                //it was just denied, let the user decide
                ActivityCompat.requestPermissions(main.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION},
                        WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
            } else {
                ActivityCompat.requestPermissions(main.this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.ACCESS_FINE_LOCATION},
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

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        try {
            MapsInitializer.initialize(getApplicationContext());
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }


        if(Build.VERSION.SDK_INT>22) {
            permission_warning = findViewById(R.id.permission_warrning);
            findViewById(R.id.ask_permissions).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handle_permissions();
                }
            });
            handle_permissions();
        }else{
            init_activity();
        }
    }
    private void apply_smaller_attributes_to_textview(TextView textView){
        RelativeLayout.LayoutParams small_layout_params=(RelativeLayout.LayoutParams) textView.getLayoutParams();
        int small_margin=(int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                5,
                getApplicationContext().getResources().getDisplayMetrics()
        );
        if(Build.VERSION.SDK_INT>=17){
            small_layout_params.setMarginEnd(small_margin);
            small_layout_params.setMarginStart(small_margin);
        }
        small_layout_params.setMargins(small_margin,small_margin,small_margin,small_margin);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,getResources().getDimension(R.dimen.text_size_small));
        textView.setLayoutParams(small_layout_params);
    }
    private void init_activity(){
        //permission 'granit' :)
        permission_warning.setVisibility(View.GONE);

        db_io=new Database_io(getApplicationContext());

        final RelativeLayout main=findViewById(R.id.main);
        TransitionManager.beginDelayedTransition(main,new Fade(Fade.OUT));
        main_menu =main.findViewById(R.id.main_menu);
        locationManager=(LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        start_service_button =(TextView) findViewById(R.id.start_service_id);
        stop_service_button =(TextView) findViewById(R.id.stop_service_id);show_result_but=(TextView) findViewById(R.id.show_result_but_id);
        recyclerView = (RecyclerView) findViewById(R.id.recycy);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);

        adap = new fileadapter();
        recyclerView.setAdapter(adap);
        db_io=new Database_io(getApplicationContext());

        location_tracker_service_intent =new Intent(getApplicationContext(),back_ground_tracking.class);

        start_service_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Start();
            }
        });
        stop_service_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start_service_button.setText("Start tracking location");
                stopService(location_tracker_service_intent);
                stop_service_button.setVisibility(View.GONE);
            }
        });
        show_result_but.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(db_io.tracked_locations_exists()){
                    apply_smaller_attributes_to_textview((TextView) main_menu.getChildAt(0));
                    apply_smaller_attributes_to_textview((TextView) main_menu.getChildAt(1));
                    apply_smaller_attributes_to_textview((TextView) main_menu.getChildAt(2));
                    main_menu.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT,RelativeLayout.LayoutParams.WRAP_CONTENT));
                    main_menu.setBackgroundColor(Color.TRANSPARENT);
                    load_locations();
                } else{
                    Toast.makeText(getApplicationContext(),"No location recorded" ,Toast.LENGTH_SHORT).show();
                }
            }
        });
        //check if for any reason(including user pressing the stop button in the notification on api levels>=26) the
        //service is destroyed;
        check_if_service_is_running_thread=new Thread(new Runnable() {
            @Override
            public void run() {
                while(!app_exited){
                    main.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(back_ground_tracking.Service_is_running&&!start_service_button.getText().equals("Restart tracking with new configuration")){
                                start_service_button.setText("Restart tracking with new configuration");
                                stop_service_button.setVisibility(View.VISIBLE);
                            }
                            if(!back_ground_tracking.Service_is_running&& start_service_button.getText().equals("Restart tracking with new configuration")){
                                start_service_button.setText("Start tracking location");
                                stop_service_button.setVisibility(View.GONE);
                            }
                        }
                    });
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        check_if_service_is_running_thread.start();
    }
    public void load_locations() {
        Cursor cursor = db_io.get_all_locations();
        while (cursor.moveToNext()) {
            location_info l_i = new location_info();
            l_i.timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(Database_io.Database_info.recorded_locations_column_timestamp));
            l_i.lat = cursor.getDouble(cursor.getColumnIndexOrThrow(Database_io.Database_info.recorded_locations_column_latitude));
            l_i.longt = cursor.getDouble(cursor.getColumnIndexOrThrow(Database_io.Database_info.recorded_locations_column_longtitude));
            l_i.alt = cursor.getDouble(cursor.getColumnIndexOrThrow(Database_io.Database_info.recorded_locations_column_altitude));

            Calendar calendar = Calendar.getInstance();
            TimeZone tz = TimeZone.getDefault();
            calendar.setTimeInMillis(l_i.timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date currenTimeZone = (Date) calendar.getTime();

            l_i.date=sdf.format(currenTimeZone);
            data.add(l_i);
        }
        adap.notifyDataSetChanged();
    }
    public class fileadapter extends RecyclerView.Adapter<fileadapter.fileholder> {

        private Marker marker;
        public fileadapter() {

        }

        public class fileholder extends RecyclerView.ViewHolder {
            public TextView lat, longt, time, alt;
            LinearLayout rec_lo;

            public fileholder(View view) {
                super(view);
                rec_lo = (LinearLayout) view.findViewById(R.id.rec_layout);
                lat = (TextView) view.findViewById(R.id.lat_id);
                longt = (TextView) view.findViewById(R.id.longt_id);
                time = (TextView) view.findViewById(R.id.time_stamp_id);
                alt = (TextView) view.findViewById(R.id.altit_id);
            }
        }

        public fileadapter.fileholder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemview = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rec, parent, false);
            return new fileadapter.fileholder(itemview);
        }

        @Override
        public void onBindViewHolder(final fileadapter.fileholder holder, final int position) {
//            Calendar calendar=Calendar.getInstance(Locale.ENGLISH);
//            calendar.setTimeInMillis(1000L*(long)(back_ground_tracking.Stringnumtoint(data.get(position)[2])));


            holder.lat.setText(String.valueOf(data.get(position).lat));
            holder.longt.setText(String.valueOf(data.get(position).longt));
            holder.alt.setText(String.valueOf(data.get(position).alt));
            holder.time.setText(String.valueOf(data.get(position).timestamp));
            holder.rec_lo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(marker!=null)marker.remove();
                    marker = googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(data.get(position).lat, data.get(position).longt))
                            .title(data.get(position).date));
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 1000));
                }
            });
        }

        public int getItemCount() {
            return data.size();
        }
    }
    private void Start(){
        final Dialog dialog=new Dialog(main.this);
        dialog.setContentView(R.layout.options);
        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dist=(EditText)dialog.findViewById(R.id.distance_id);interv=(EditText)dialog.findViewById(R.id.interval_id);
        dialog.findViewById(R.id.done).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                    Toast.makeText(getApplicationContext(),"GPS sensor is turned off",Toast.LENGTH_LONG).show();
                    return;
                }
                if(dist.getText().toString().length()==0){
                    Toast.makeText(getApplicationContext(),"Minimum distance change must be at least zero",Toast.LENGTH_LONG).show();
                    return;
                }
                if(interv.getText().toString().length()==0){
                    Toast.makeText(getApplicationContext(),"Minimum Time interval must be at least zero",Toast.LENGTH_LONG).show();
                    return;
                }

                location_tracker_service_intent.putExtra("distance",back_ground_tracking.Stringnumtoint(dist.getText().toString()));
                location_tracker_service_intent.putExtra("interval",back_ground_tracking.Stringnumtoint(interv.getText().toString())*60000);
                //from android O on sole background services are limited and will be killed if app gets idle; so we must start as a  foreground service
                // in order to keep it running
                if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
                    location_tracker_service_intent.putExtra("command",back_ground_tracking.COMMAND_START_RESTART_TRACKING);
                    startForegroundService(location_tracker_service_intent);
                }else {
                    startService(location_tracker_service_intent);
                }
                start_service_button.setText("Restart tracking with new configuration");
                stop_service_button.setVisibility(View.VISIBLE);
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
            //we need all the permissions; so we just loop through all the permissions;
            int i=0;
            while(i<grantResults.length){
                if(grantResults[i]!=PackageManager.PERMISSION_GRANTED)break;
                i++;
            }
            if(grantResults.length==i){//this condition means all the permissions were granted; otherwise the loop would have broken sooner that variable 'i' reaches 'grantResults.length';
                init_activity();
                return;
            }
            if(permission_warning.getVisibility()==View.GONE)permission_warning.setVisibility(View.VISIBLE);
        }
    }
    public void onMapReady(GoogleMap map) {
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap = map;
    }
    protected void onResume() {
        mapView.onResume();
        super.onResume();
    }

    @Override
    protected void onPause() {
        mapView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mapView.onDestroy();
        super.onDestroy();
        app_exited=true;
        db_io.close();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }

}
