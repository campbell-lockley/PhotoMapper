package org.campbelll.android.photomapper;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.campbelll.android.photomapper.db.PhotoDBHelper;

import static org.campbelll.android.photomapper.db.PhotoContract.PhotoEntry;

/**
 * This is the content provider for org.campbelll.android.photomapper.PhotoProvider.
 * <p>
 * Responds to queries for all photos via "content://org.campbelll.android.photomapper.PhotoProvider/photos" to return
 * all photos in the underlying photo database, and responds to inserts to add photos to the underlying database.
 *
 * @author Campbell Lockley
 */
public class PhotoProvider extends ContentProvider {
    /* Tag */
    private static final String TAG = "PhotoProvider";

    /* Uri match types. */
    private static final int ALL = 0;

    /* Content provider uri */
    public static final String AUTHORITY = "org.campbelll.android.photomapper.PhotoProvider";
    public static final Uri CONTENT_URI = Uri.parse("content://"+AUTHORITY+"/photos");

    /* Uri matcher */
    private UriMatcher uriMatcher = null;

    /** Sets up the {@link UriMatcher}. Returns true if an instance of {@link PhotoDBHelper} exists. */
    @Override
    public boolean onCreate() {
        /* Setup uri matcher to match our content provider */
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "photos", ALL);

        /* Check if PhotoDBHelper exists */
        boolean result = (PhotoDBHelper.getInstance(getContext()) != null);

        return result;
    }

    /**
     * Handles queries.
     * <p>
     * Only responds to requests for all photos, i.e. the request
     * "content://org.campbelll.android.photomapper.PhotoProvider/photos" will return a cursor containing all data for
     * every photo in the database.
     *
     *
     * @param uri Should be "content://org.campbelll.android.photomapper.PhotoProvider/photos" to get all photos,
     *            otherwise will return null.
     * @param projection This is ignored.
     * @param selection This is ignored.
     * @param selectionArgs This is ignored.
     * @param sortOrder This is ignored.
     * @return Cursor containing all data for all photos or null for any request other than for all photos.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        switch (uriMatcher.match(uri)) {
            case ALL:
                return getPhotos();
            default:
                return null;
        }
    }

    /** Utility method returns cursor with data for all photos. */
    private Cursor getPhotos() {
        String cmd = "SELECT "+PhotoEntry._ID+",* FROM "+PhotoEntry.TABLE_NAME;
        Cursor cursor = PhotoDBHelper.getInstance(getContext()).getReadableDatabase().rawQuery(cmd, null);
        cursor.setNotificationUri(getContext().getContentResolver(), CONTENT_URI);
        return cursor;
    }

    /** Returns null always. */
    @Override
    public String getType(Uri uri) { return null; }

    /** Handles inserts. */
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = PhotoDBHelper.getInstance(getContext()).getWritableDatabase();

        long id = db.insert(PhotoEntry.TABLE_NAME, null, values);
        getContext().getContentResolver().notifyChange(uri, null);

        return ContentUris.withAppendedId(CONTENT_URI, id);
    }

    /** Handles deletes. Only handles deleting all photos. */
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int numRows = 0;
        SQLiteDatabase db = PhotoDBHelper.getInstance(getContext()).getWritableDatabase();

        switch (uriMatcher.match(uri)) {
            case ALL:
                numRows = db.delete(PhotoEntry.TABLE_NAME, null, null);     // delete all rows
                getContext().getContentResolver().notifyChange(uri, null);
                break;
            default:
                break;
        }

        return numRows;
    }

    /** Updating is not supported. Returns 0 always. */
    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }

}
