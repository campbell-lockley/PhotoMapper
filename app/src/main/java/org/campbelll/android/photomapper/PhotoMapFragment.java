package org.campbelll.android.photomapper;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.campbelll.android.photomapper.db.PhotoDBHelper;
import org.campbelll.android.photomapper.utility.Photo;

import java.util.ArrayList;
import java.util.HashMap;

import static org.campbelll.android.photomapper.db.PhotoContract.PhotoEntry;

/**
 * Manages a {@link GoogleMap} view which automatically plots photos obtained from an underlying database whenever the
 * data in the database changes.
 * <p>
 * The data set is managed by {@link PhotoProvider}. {@link #onCreateLoader(int, Bundle)} sets up a database cursor
 * loader which automatically calls {@link #onLoadFinished(Loader, Cursor)} when the data set changes.
 *
 * @author Campbell Lockley
 */
public class PhotoMapFragment extends MapFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        GoogleMap.InfoWindowAdapter, GoogleMap.OnMarkerClickListener, GoogleMap.OnMapClickListener {
    /* Tag */
    private static final String TAG = "PhotoMapFragment";

    /* Columns we require from db - i.e. all of them */
    private static final String[] PHOTO_PROJECTION = new String[] {
            PhotoEntry._ID,
            PhotoEntry.COL_URI,
            PhotoEntry.COL_THUMBNAIL,
            PhotoEntry.COL_GPS_LATITUDE,
            PhotoEntry.COL_GPS_LATITUDE_REF,
            PhotoEntry.COL_GPS_LONGITUDE,
            PhotoEntry.COL_GPS_LONGITUDE_REF,
            PhotoEntry.COL_DATE,
            PhotoEntry.COL_TIME,
            PhotoEntry.COL_MAKE,
            PhotoEntry.COL_MODEL
    };

    /* Constants */
    private static final float START_ZOOM = 13;     // Initial GoogleMap zoom level

    /* Members */
    private GoogleMap map = null;                       // GoogleMap instance
    private ArrayList<Photo> photos;                    // List of photos
    private HashMap<String, Photo> marker_to_photo;     // Hashmap of Google Maps markers and matching photos
    private HashMap<String, Marker> photo_to_marker;    // Hashmap of photo URIs and matching markers
    private String selected = null;                     // Currently selected photo
    private View customInfoWindow;                      // View used by getWindowInfo() to generate custom info windows
    private LatLng startLatLng = null;                  // Position to start map at

    /** Constructor */
    public PhotoMapFragment() {
    }

    /**
     * Sets starting location of google map to be the user's location. If app was started by a share intent then this
     * will be overridden by {@link #updateCamera(LatLng)}.
     *
     * @param savedInstanceState {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);    // Don't kill map fragment on rotate

        /* On first create, start us at user's location */
        startLatLng = ((PhotoMapperActivity)getActivity()).getUserLatLng();
    }

    /**
     * Sets up the {@link GoogleMap} view and the cursor loader which is providing it the photo data.
     *
     * @param savedInstanceState {@inheritDoc}
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        /* Set up info window view */
        customInfoWindow = getActivity().getLayoutInflater().inflate(R.layout.photo_info_window, null);

        /* Set up cursor loader */
        getActivity().getLoaderManager().initLoader(0, null, this);

        /* Set up map */
        if (map == null) map = getMap();
        if (map != null) setUpMap();
    }

    /** Sets up the {@link GoogleMap} and centres the map on either the user's location or the shared image. */
    private void setUpMap() {
        /* Use our custom info window */
        map.setInfoWindowAdapter(this);

        /* Use our custom listeners */
        map.setOnMarkerClickListener(this);
        map.setOnMapClickListener(this);

        /* Set up map options */
        map.setMyLocationEnabled(true);

        /* Centre map over shared image or over current location, depending on if app started by share intent */
        if (startLatLng != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, START_ZOOM));
            startLatLng = null;
        }
    }

    /**
     * Updates the {@link GoogleMap}'s camera to centre around the given position.
     * <p>
     * Must be called before {@link #onActivityCreated(Bundle)}, i.e. in the activity's onCreate() method.
     *
     * @param latlng Position to move camera to.
     */
    protected void updateCamera(LatLng latlng) {
        Log.d(TAG, "updateCamera() called");
        startLatLng = latlng;
    }

    /** Creates cursor loader for automatic database queries. */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), PhotoProvider.CONTENT_URI, PHOTO_PROJECTION, "", null, "");
    }

    /** Callback which populates the  {@link GoogleMap} with photos when data set changes. */
    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        /* Get all photos from the database */
        photos = PhotoDBHelper.getAllPhotos(cursor);

        Log.d(TAG, "Loading " + ((photos == null) ? 0 : photos.size()) + " photos");

        /* Clear the map */
        map.clear();
        marker_to_photo = new HashMap<>();
        photo_to_marker = new HashMap<>();

        /* Add all photos to the map */
        if (photos != null) {
            for (Photo photo : photos) {
                Log.d(TAG, "Photo: " + photo.uri + " Lat: " + photo.gps_latitude + " Long: " + photo.gps_longitude);
                Marker mark = map.addMarker(
                        new MarkerOptions().position(new LatLng(photo.gps_latitude, photo.gps_longitude)));
                marker_to_photo.put(mark.getId(), photo);
                photo_to_marker.put(photo.uri, mark);
            }
        }

        /* Re-select selected marker */
        if (selected != null) {
            Log.d(TAG, "re-selecting marker");
            photo_to_marker.get(selected).showInfoWindow();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        Log.d(TAG, TAG+".onLoaderReset()");
        photos = null;
        marker_to_photo = null;
        photo_to_marker = null;
        map.clear();
    }

    /** Not implemented. Returns null. */
    @Override
    public View getInfoWindow(Marker marker) { return null; }

    /** Used to implement custom marker info window. Displays photo and its details. */
    @Override
    public View getInfoContents(Marker marker) {
        ImageView photoView     = (ImageView) customInfoWindow.findViewById(R.id.info_window_photo);
        TextView dateView       = (TextView) customInfoWindow.findViewById(R.id.info_window_date);
        TextView timeView       = (TextView) customInfoWindow.findViewById(R.id.info_window_time);
        TextView makeView       = (TextView) customInfoWindow.findViewById(R.id.info_window_make);
        TextView modelView      = (TextView) customInfoWindow.findViewById(R.id.info_window_model);

        /* Get selected photo's details */
        Photo photo = marker_to_photo.get(marker.getId());

        /* Update infoWindow with this photo's details */
        photoView.setImageBitmap(BitmapFactory.decodeByteArray(photo.thumbnail, 0, photo.thumbnail.length));
        dateView.setText("Date: " + photo.date);
        timeView.setText("Time: " + photo.time);
        makeView.setText("Make: " + photo.make);
        modelView.setText("Model: " + photo.model);

        return customInfoWindow;    // Return updated view
    }

    /** Captures selected marker to retain selection on device rotation. */
    @Override
    public boolean onMarkerClick(Marker marker) {
        selected = marker_to_photo.get(marker.getId()).uri;
        return false;
    }

    /** Invalidates selected marker. */
    @Override
    public void onMapClick(LatLng point) { selected = null; }

}
