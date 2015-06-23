package org.campbelll.android.photomapper;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.maps.model.LatLng;

import org.campbelll.android.photomapper.db.PhotoDBHelper;
import org.campbelll.android.photomapper.utility.ExifExtractor;
import org.campbelll.android.photomapper.utility.Photo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * This app accepts share intents containing singular images, extracts GPS (and other) data from the image's EXIF
 * header, and plots the images on a {@link com.google.android.gms.maps.GoogleMap GoogleMap}.
 * <p>
 * This app uses an underlying database to store the shared image's EXIF data, as well as a compressed thumbnail of the
 * image. If a user selects a marker on the GoogleMap the thumbnail of the image and the image's details are displayed
 * in a pop-up window.
 *
 * @author Campbell Lockley
 */
public class PhotoMapperActivity extends FragmentActivity {
    /* Tags */
    private static final String TAG = "PhotoMapperActivity";
    private static final String MAP_FRAGMENT_TAG = "PhotoMapFragment";

    /* Members */
    private boolean shareReceived = false;  // Indicates app started by share intent
    private LatLng sharedLoc = null;        // Lat and long of image which has just been shared

    /* Fragments */
    private PhotoMapFragment photoMapFragment = null;

    /**
     * Handles the intents which launched this app and sets up the user interface.
     *
     * @param savedInstanceState {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Get launching intent */
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        /* Parse intent type */
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            handleShareIntent(intent);  // Handle share intent and then launch app
        } else {
            handleMainIntent();         // Launch app normally
        }
    }

    /** Set up the user interface and add the fragments to this activity. */
    private void handleMainIntent() {
        setContentView(R.layout.activity_photo_mapper);
        addFragments();
    }

    /**
     * Set up the map fragment.
     * <p>
     * Attempt to 're-connect' to map fragment or, failing that, create one.
     */
    private void addFragments() {
        FragmentManager fragmentManager = getFragmentManager();

        photoMapFragment = (PhotoMapFragment) fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG);

        if (photoMapFragment == null) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            photoMapFragment = new PhotoMapFragment();
            ft.add(R.id.map, photoMapFragment, MAP_FRAGMENT_TAG);
            ft.commit();
            fragmentManager.executePendingTransactions();
        }

        /* If started from a share intent, centre map over shared image */
        if (shareReceived) {
            shareReceived = false;
            photoMapFragment.updateCamera(sharedLoc);
        }
    }

    /**
     * Perform image share intent handling.
     * <p>
     * If image contained in the share intent contains valid EXIF GPS data, add image to the photo database and
     * continue launching app as normal. If Image doesn't have valid GPS data, display an error screen.
     *
     * @param intent Image share intent being handled.
     */
    private void handleShareIntent(Intent intent) {
        /* Extract image URI from intent and get file system path for image */
        Uri imageUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
        String path = getPath(imageUri);
        Log.d(TAG, "getPath(): " + path);

        /* Try and get EXIF data */
        Photo photo = null;
        try { photo = ExifExtractor.extract(Uri.parse(path)); }
        catch (IOException e) { Log.e(TAG, "Failed to open file "+path, e); }
        Log.d(TAG, "Exif data " + ((photo == null) ? "WASN'T" : "WAS") + " present");

        if (photo != null) {
            /* Get thumbnail of image */
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                Bitmap thumbnail = bitmap.createScaledBitmap(bitmap, 512, 384, false);
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 50, bout);   // Do a bit of compression on thumbnail
                photo.thumbnail = bout.toByteArray();
            } catch (IOException e) { Log.e(TAG, "couldn't get thumbnail of " + imageUri.toString(), e); }

            /* Add photo to db if it contains valid GPS EXIF data */
            ContentValues cv = PhotoDBHelper.getContentValues(photo);
            getContentResolver().insert(PhotoProvider.CONTENT_URI, cv);

            /* We will centre the map over the shared image */
            shareReceived = true;
            sharedLoc = new LatLng(photo.gps_latitude, photo.gps_longitude);

            /* Clear intent so it isn't processed again, e.g on screen rotate */
            intent.setAction("");

            /* Continue launching app as normal */
            handleMainIntent();
        } else {
            /* If no exif data, display error */
            setContentView(R.layout.error_no_exif_data);
            ImageView imageView = (ImageView)findViewById(R.id.error_no_exif_data_image);
            imageView.setImageURI(imageUri);
        }
    }

    /**
     * Uses the {@link MediaStore} to get the file path of a photo from its content// type URI.
     *
     * @param imageUri content// type URI for an image.
     * @return File system URI for the image.
     */
    private String getPath(Uri imageUri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor c = getContentResolver().query(imageUri, projection, null, null, null);
        int dataCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        c.moveToFirst();
        return c.getString(dataCol);
    }

    /**
     * Gets the user's last known location from a suitable system service.
     *
     * @return Last known location of user.
     */
    protected LatLng getUserLatLng() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location loc = lm.getLastKnownLocation(lm.getBestProvider(new Criteria(), false));
        return new LatLng(loc.getLatitude(), loc.getLongitude());
    }

}
