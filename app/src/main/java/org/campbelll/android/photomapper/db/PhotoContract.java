package org.campbelll.android.photomapper.db;

import android.provider.BaseColumns;

/**
 * This is a contract for the photo database so that the table name and column names are defined here once package wide.
 *
 * @see PhotoDBHelper
 * @author Campbell Lockley
 */
public final class PhotoContract {

    /** Empty constructor. */
    public PhotoContract() {}

    /** Defines table contents for photo tab;e. */
    public static final class PhotoEntry implements BaseColumns {
        public static final String TABLE_NAME               = "photo";
        public static final String COL_URI                  = "uri";
        public static final String COL_THUMBNAIL            = "thumbnail";
        public static final String COL_GPS_LATITUDE         = "gps_latitude";
        public static final String COL_GPS_LATITUDE_REF     = "gps_latitude_ref";
        public static final String COL_GPS_LONGITUDE        = "gps_longitude";
        public static final String COL_GPS_LONGITUDE_REF    = "gps_longitude_ref";
        public static final String COL_DATE                 = "date";
        public static final String COL_TIME                 = "time";
        public static final String COL_MAKE                 = "make";
        public static final String COL_MODEL                = "model";
    }
}
