package gps.test.tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

public class Database_io extends SQLiteOpenHelper {
    private SQLiteDatabase database;
    public static class Database_info{
        public static final String recorded_locations_table_name="rdtn";
        public static final String recorded_locations_column_latitude="lat";
        public static final String recorded_locations_column_longtitude="longt";
        public static final String recorded_locations_column_altitude="alt";
        public static final String recorded_locations_column_timestamp="timestamp";
    }
    public static class Common_Queries{
        public static final String create_table_for_location_records="create table "+Database_info.recorded_locations_table_name+" (" +
                Database_info.recorded_locations_column_timestamp+" integer primary key, " +
                Database_info.recorded_locations_column_latitude+" double not null, " +
                Database_info.recorded_locations_column_longtitude+" double not null, " +
                Database_info.recorded_locations_column_altitude+" double not null " +
                ");";
    }
    public static class Filters{
        public long start_date_timestamp=0,end_date_timestamp=Long.MAX_VALUE;
        public String create_where_clause_columns(){
            return Database_info.recorded_locations_column_timestamp+">=? and "+Database_info.recorded_locations_column_timestamp+"<=?";
        }
        public String[] create_where_clause_values(){
            return new String[]{String.valueOf(start_date_timestamp),String.valueOf(end_date_timestamp)};
        }
    }
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "FeedReader.db";
    public Filters filters;
    public Database_io(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        filters=new Database_io.Filters();
    }
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(Common_Queries.create_table_for_location_records);
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
//        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }
    public void write_location_to_database(Location location){
        ContentValues contentValues=new ContentValues();
        contentValues.put(Database_info.recorded_locations_column_latitude,location.getLatitude());
        contentValues.put(Database_info.recorded_locations_column_longtitude,location.getLongitude());
        contentValues.put(Database_info.recorded_locations_column_altitude,location.getAltitude());
        contentValues.put(Database_info.recorded_locations_column_timestamp,location.getTime());
        if(getWritableDatabase().insert(Database_info.recorded_locations_table_name,null,contentValues)==-1)
            Log.d("debug__","Error writing data to database");
    }
    public Cursor get_all_locations_within(int offset,int limit){
        return getReadableDatabase().query(
                Database_info.recorded_locations_table_name,   // The table to query
                null,             // The array of columns to return (pass null to get all)
                filters.create_where_clause_columns(),              // The columns for the WHERE clause
                filters.create_where_clause_values(),          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                Database_info.recorded_locations_column_timestamp + " ASC",//sort order
                String.valueOf(offset)+", "+String.valueOf(limit)// limit, offset
        );
    }
    public boolean tracked_locations_exists(){
        if(getReadableDatabase().rawQuery("SELECT 1 " +
                "FROM "+Database_info.recorded_locations_table_name,null).getCount()>0)return true;
        return false;
    }
    public long get_all_rows_count(){
        return DatabaseUtils.queryNumEntries(getReadableDatabase(),
                Database_info.recorded_locations_table_name,
                filters.create_where_clause_columns(),
                filters.create_where_clause_values());
    }
}