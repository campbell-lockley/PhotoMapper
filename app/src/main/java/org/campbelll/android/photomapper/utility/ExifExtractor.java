package org.campbelll.android.photomapper.utility;

import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * Converts the Latitude-Longitude from an Exif tag to the values required by
 * {@link com.google.android.gms.maps.model.LatLng}.
 *
 * @author Campbell Lockley
 */
public class ExifExtractor {
    /* TAG */
    public static final String TAG = "ExifExtractor";

    /**
     * Extracts EXIF data from photo, returning it as a {@link Photo}.
     * <p>
     * If photo doesn't contain gps data in EXIF data null is returned instead.
     *
     * @param uri URI of photo on file system.
     * @return {@link Photo} instance with photo details.
     * @throws IOException e.g. if file doesn't exist.
     */
    public static Photo extract(Uri uri) throws IOException {
        ExifInterface exif = new ExifInterface(new File(uri.getPath()).getAbsolutePath());

        Photo photo = new Photo();
        photo.uri = uri.toString();
        photo.gps_latitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
        photo.gps_longitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
        try {
            photo.gps_latitude =
                    getLatitude(exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE), photo.gps_latitude_ref);
            photo.gps_longitude =
                    getLongitude(exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE), photo.gps_longitude_ref);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Photo " + uri.toString() + " didn't have GPS data");
            return null;
        }
        String[] datetime = exif.getAttribute(ExifInterface.TAG_DATETIME).split(" ");
        photo.date = datetime[0];
        photo.time = datetime[1];
        photo.make = exif.getAttribute(ExifInterface.TAG_MAKE);
        photo.model = exif.getAttribute(ExifInterface.TAG_MODEL);

        return photo;
    }

    /** Returns GPS latitude as a double. Throws {@link java.lang.IllegalArgumentException} if no GPS data present. */
    private static double getLatitude(String latSrc, String latRef) throws IllegalArgumentException {
        if ((latSrc == null) || (latRef == null)) throw new IllegalArgumentException();
        /* Check orientation of coordinates */
        if(latRef.equals("N")) return convertToDegree(latSrc);
        else return 0 - convertToDegree(latSrc);
    }

    /** Returns GPS longitude as a double. Throws {@link java.lang.IllegalArgumentException} if no GPS data present. */
    private static double getLongitude(String longSrc, String longRef) throws IllegalArgumentException {
        if ((longSrc == null) || (longRef == null)) throw new IllegalArgumentException();
        /* Check orientation of coordinates */
        if(longRef.equals("E")) return convertToDegree(longSrc);
        else return 0 - convertToDegree(longSrc);
    }

    /** Converts a gps exif tag to a double format. */
    private static double convertToDegree(String source) {
        String[] degMinSec = source.split(",");
        String[] degSrc = degMinSec[0].split("/");
        String[] minSrc = degMinSec[1].split("/");
        String[] secSrc = degMinSec[2].split("/");

        double deg = Double.parseDouble(degSrc[0]) / Double.parseDouble(degSrc[1]);
        double min = Double.parseDouble(minSrc[0]) / Double.parseDouble(minSrc[1]);
        double sec = Double.parseDouble(secSrc[0]) / Double.parseDouble(secSrc[1]);

        return deg + (min / 60) + (sec / (60 * 60));    // Fold degrees, minutes and seconds together
    }

}
