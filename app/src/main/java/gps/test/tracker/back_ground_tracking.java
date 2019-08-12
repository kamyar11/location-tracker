package gps.test.tracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Created by user_0 on 9/2/2017.
 */

public class back_ground_tracking extends Service {
    private int distance=0;
    private int interval=0;
    private int counter=0;
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        locationManager=(LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        locationListener=new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                SimpleDateFormat simpleDateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                simpleDateFormat.setTimeZone(TimeZone.getDefault());
                write_data(String.valueOf(location.getLatitude()),String.valueOf(location.getLongitude()),String.valueOf(location.getAltitude()),simpleDateFormat.format(new Date()));
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }
            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, distance, locationListener);

        }catch (SecurityException e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        distance=intent.getExtras().getInt("distance");
        interval=intent.getExtras().getInt("interval");
        locationManager.removeUpdates(locationListener);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval, distance, locationListener);
        }catch (SecurityException e){
            Toast.makeText(getApplicationContext(),e.toString(),Toast.LENGTH_LONG).show();
        }


        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        locationManager.removeUpdates(locationListener);
    }
    public void write_data(String latitude,String longtitude,String altitude,String time){
        try{
            File f=new File("/sdcard/data_cap");
            OutputStream zos=new FileOutputStream(f,true);
            zos.write(latitude.getBytes());zos.write('\n');
            zos.write(longtitude.getBytes());zos.write('\n');
            zos.write(altitude.getBytes());zos.write('\n');
            zos.write(time.getBytes());zos.write('\n');
            zos.write('\n');
            zos.flush();zos.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static int Stringnumtoint(String x){
        if(x==null)return 0;
        byte[] b=x.getBytes();
        int i=0,num_in_int=0;
        while(i<b.length){
            b[i]=(byte)(b[i]-0x30);
            num_in_int=num_in_int*10+b[i];
            i++;
        }
        return num_in_int;
    }

}
