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
            if(f.length()>100000){
                send_data_to_server(f);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }
    public void send_data_to_server(final File f){
        new AsyncTask<Void,Void,Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                try{
                    MultipartUtility multipartUtility=new MultipartUtility("http://192.168.1.6:81");
                    multipartUtility.addFilePart("file",f);
                }catch (Exception e){
                    e.printStackTrace();
                }

                return null;
            }
        }.execute();
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

    public class MultipartUtility {
        private HttpURLConnection httpConn;
        private OutputStream request;
        private final String boundary =  "*****";
        private final String crlf = "\r\n";
        private final String twoHyphens = "--";
        public MultipartUtility(String requestURL)
                throws IOException {
            URL url = new URL(requestURL);
            httpConn = (HttpURLConnection) url.openConnection();

            httpConn.setUseCaches(false);
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Connection", "Keep-Alive");
            httpConn.setRequestProperty("Cache-Control", "no-cache");
            httpConn.setRequestProperty(
                    "Content-Type", "multipart/form-data;boundary=" + this.boundary);
            request = httpConn.getOutputStream();
        }
        public void addFormField(String name, String value)throws IOException {
            request.write(( this.twoHyphens + this.boundary + this.crlf).getBytes());
            request.write(("Content-Disposition: form-data; name=\"" + name + "\""+ this.crlf).getBytes());
            request.write(this.crlf.getBytes());
            request.write((value).getBytes());
            request.write(this.crlf.getBytes());
            request.flush();
        }
        public void addFilePart(String fieldName, File uploadFile)
                throws IOException {
            String fileName = uploadFile.getName();
            request.write((this.twoHyphens + this.boundary + this.crlf).getBytes());
            request.write(("Content-Disposition: form-data; name=\"" +
                    fieldName + "\";filename=\"" +
                    fileName + "\"" + this.crlf).getBytes());
            request.write(this.crlf.getBytes());
            InputStream is=new FileInputStream(uploadFile);
            byte[] bytes = new byte[1024];
            int c=is.read(bytes);
            while(c>0){
                request.write(bytes,0,c);
                c=is.read(bytes);
            }
            request.write(this.crlf.getBytes());
            request.flush();
        }
        public String finish() throws IOException {
            String response ="";
            request.write((this.twoHyphens + this.boundary +
                    this.twoHyphens + this.crlf).getBytes());
            request.flush();
            request.close();
            int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                InputStream responseStream = httpConn.getInputStream();
                byte[] b=new byte[1024];
                int c=responseStream.read(b);
                while(c>0){
                    response=response+new String(b,0,c);
                    c=responseStream.read(b);
                }
                responseStream.close();
            } else {
                throw new IOException("Server returned non-OK status: " + status);
            }
            return response;
        }
        public InputStream finish_with_inputstream()throws Exception{
            request.write((this.twoHyphens + this.boundary +
                    this.twoHyphens + this.crlf).getBytes());
            request.flush();
            request.close();
            int status = httpConn.getResponseCode();
            if (status == HttpURLConnection.HTTP_OK) {
                return httpConn.getInputStream();
            } else {
                throw new IOException("Server returned non-OK status: " + status);
            }
        }
    }
}
