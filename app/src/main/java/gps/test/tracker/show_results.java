package gps.test.tracker;

import android.app.Activity;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by user_0 on 9/2/2017.
 */

public class show_results extends Activity implements OnMapReadyCallback {
    private RecyclerView recyclerView;
    private fileadapter adap;
    private MapView mapView;
    private GoogleMap googleMap;
    private Database_io db_io;

    public static class location_info {
        private double lat, longt, alt;
        long timestamp;
        private String date;
    }

    private ArrayList<location_info> data = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_result);

        db_io = new Database_io(getApplicationContext());
        recyclerView = (RecyclerView) findViewById(R.id.recycy);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(layoutManager);
        adap = new fileadapter();
        recyclerView.setAdapter(adap);
        load_locations();

        mapView = (MapView) findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        try {
            MapsInitializer.initialize(getApplicationContext());
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public void onMapReady(GoogleMap map) {
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap = map;
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

        public fileholder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemview = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.rec, parent, false);
            return new fileholder(itemview);
        }

        @Override
        public void onBindViewHolder(final fileholder holder, final int position) {
//            Calendar calendar=Calendar.getInstance(Locale.ENGLISH);
//            calendar.setTimeInMillis(1000L*(long)(back_ground_tracking.Stringnumtoint(data.get(position)[2])));


            holder.lat.setText(String.valueOf(data.get(position).lat));
            holder.longt.setText(String.valueOf(data.get(position).longt));
            holder.alt.setText(String.valueOf(data.get(position).alt));
            holder.time.setText(String.valueOf(data.get(position).timestamp));


            holder.rec_lo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Marker marker = googleMap.addMarker(new MarkerOptions()
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
        db_io.close();
    }

    @Override
    public void onLowMemory() {
        mapView.onLowMemory();
        super.onLowMemory();
    }
}
