package org.campbelll.android.photomapper.utility;

/**
 * Utility class for storing details of a photo.
 *
 * @author Campbell Lockley
 */
public class Photo {
    /* Fields */
    public String uri;                  // URI for photo
    public byte[] thumbnail;            // Thumbnail of photo as raw bytes
    public Double gps_latitude;         // GPS latitude, i.e. between -90.0 and 90.0
    public String gps_latitude_ref;     // Reference of latitude, i.e. "N" or "S"
    public Double gps_longitude;        // GPS longitude, i.e. between -180.0 and 180.0
    public String gps_longitude_ref;    // Reference of longitude, i.e. "E" or "W"
    public String date;                 // Date of photo, i.e. "YYYY:MM:DD:
    public String time;                 // Time of photo, i.e. "HH:MM:SS"
    public String make;                 // Make of phone
    public String model;                // Model of phone
}
