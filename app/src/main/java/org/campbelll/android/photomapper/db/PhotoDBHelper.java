package org.campbelll.android.photomapper.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.util.Log;

import org.campbelll.android.photomapper.PhotoProvider;
import org.campbelll.android.photomapper.utility.Photo;

import java.util.ArrayList;

import static org.campbelll.android.photomapper.db.PhotoContract.PhotoEntry;

/**
 * Database helper class for the photo database.
 * <p>
 * This class is used to create and delete the database, as well as providing utility methods to extract {@link Photo}s
 * from the database cursors returned by {@link PhotoProvider} and converting {@link Photo}s to {@link ContentValues}
 * for {@link PhotoProvider#insert(Uri, ContentValues)} calls.
 *
 * @see PhotoContract
 * @author Campbell Lockley
 */
public class PhotoDBHelper extends SQLiteOpenHelper {
    /** Tag */
    private static final String TAG = "PhotoDBHelper";

    /** Current database version. */
    public static final int DATABASE_VERSION = 1;
    /** Database name. */
    public static final String DATABASE_NAME = "photos.db";

    /** Command used to create photo table with. This must match {@link PhotoEntry} and should match {@link Photo}. */
    private static final String CREATE_ENTRIES =
            "CREATE TABLE " + PhotoEntry.TABLE_NAME + " (" +
                    PhotoEntry._ID +                    " INTEGER PRIMARY KEY," +
                    PhotoEntry.COL_URI +                " TEXT," +
                    PhotoEntry.COL_THUMBNAIL +          " BLOB," +
                    PhotoEntry.COL_GPS_LATITUDE +       " REAL," +
                    PhotoEntry.COL_GPS_LATITUDE_REF +   " TEXT," +
                    PhotoEntry.COL_GPS_LONGITUDE +      " REAL," +
                    PhotoEntry.COL_GPS_LONGITUDE_REF +  " TEXT," +
                    PhotoEntry.COL_DATE +               " TEXT," +
                    PhotoEntry.COL_TIME +               " TEXT," +
                    PhotoEntry.COL_MAKE +               " TEXT," +
                    PhotoEntry.COL_MODEL +              " TEXT" +
                    " )";

    /** Command used to delete photo table with */
    private static final String DELETE_ENTRIES = "DROP TABLE IF EXISTS " + PhotoEntry.TABLE_NAME;

    /** Singleton to synchronize database access. */
    private static PhotoDBHelper singleton = null;

    /** Constructor. */
    public PhotoDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Safely returns PhotoDBHelper instance.
     *
     * @param context Context of PhotoDBHelper.
     * @return Instance of PhotoDBHelper.
     */
    synchronized public static PhotoDBHelper getInstance(Context context) {
        if (singleton == null) singleton = new PhotoDBHelper(context.getApplicationContext());
        return singleton;
    }

    /** Creates the database table. */
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.beginTransaction();
            db.execSQL(CREATE_ENTRIES);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** This database doesn't upgrade. Photo table is dropped. */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.beginTransaction();
            db.execSQL(DELETE_ENTRIES);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        onCreate(db);
    }

    /** This database doesn't downgrade. Photo table is dropped. */
    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /** Utility method to extract all photos from db cursor. */
    public static ArrayList<Photo> getAllPhotos(Cursor c) {
        if (c.moveToFirst() == false) return null;

        ArrayList<Photo> photos = new ArrayList<>();

        do {
            Photo photo = new Photo();
            photo.uri               = c.getString(c.getColumnIndexOrThrow(PhotoEntry.COL_URI));
            photo.thumbnail         = c.getBlob(c.getColumnIndexOrThrow(PhotoEntry.COL_THUMBNAIL));
            photo.gps_latitude      = c.getDouble(c.getColumnIndexOrThrow(PhotoEntry.COL_GPS_LATITUDE));
            photo.gps_latitude_ref  = c.getString(c.getColumnIndexOrThrow(PhotoEntry.COL_GPS_LATITUDE_REF));
            photo.gps_longitude     = c.getDouble(c.getColumnIndexOrThrow(PhotoEntry.COL_GPS_LONGITUDE));
            photo.gps_longitude_ref = c.getString(c.getColumnIndexOrThrow(PhotoEntry.COL_GPS_LONGITUDE_REF));
            photo.date              = c.getString(c.getColumnIndexOrThrow(PhotoEntry.COL_DATE));
            photo.time              = c.getString(c.getColumnIndexOrThrow(PhotoEntry.COL_TIME));
            photo.make              = c.getString(c.getColumnIndexOrThrow(PhotoEntry.COL_MAKE));
            photo.model             = c.getString(c.getColumnIndexOrThrow(PhotoEntry.COL_MODEL));
            photos.add(photo);
        } while (c.moveToNext() != false);

        Log.d(TAG, photos.size() + " photos loaded from db");

        return photos;
    }

    /** Utility method to convert a {@link Photo} so that {@link PhotoProvider} can handle it. */
    public static ContentValues getContentValues(Photo photo) {
        ContentValues cv = new ContentValues();

        cv.put(PhotoEntry.COL_URI, photo.uri);
        cv.put(PhotoEntry.COL_THUMBNAIL, photo.thumbnail);
        cv.put(PhotoEntry.COL_GPS_LATITUDE, photo.gps_latitude);
        cv.put(PhotoEntry.COL_GPS_LATITUDE_REF, photo.gps_latitude_ref);
        cv.put(PhotoEntry.COL_GPS_LONGITUDE, photo.gps_longitude);
        cv.put(PhotoEntry.COL_GPS_LATITUDE_REF, photo.gps_latitude_ref);
        cv.put(PhotoEntry.COL_DATE, photo.date);
        cv.put(PhotoEntry.COL_TIME, photo.time);
        cv.put(PhotoEntry.COL_MAKE, photo.make);
        cv.put(PhotoEntry.COL_MODEL, photo.model);

        return cv;
    }

}
