package me.marcsymonds.tracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Locale;

/**
 *
 */
public class MapFragment
        extends Fragment
        implements OnMapReadyCallback,
                GoogleApiClient.ConnectionCallbacks,
                GoogleApiClient.OnConnectionFailedListener,
                LocationListener {

    final private String TAG = "MapFragment";

    private IMapFragmentActions mMapFragmentActions = null;

    private GoogleMap mMap = null;
    private GoogleApiClient mGoogleApiClient = null;

    public MapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MapFragment.
     */
    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GoogleApiClient.Builder apiBuilder = new GoogleApiClient.Builder(this.getContext());
        apiBuilder.addApi(LocationServices.API);
        apiBuilder.addConnectionCallbacks(this);
        apiBuilder.addOnConnectionFailedListener(this);

        mGoogleApiClient = apiBuilder.build();
    }

    public void onConnected(Bundle bundle) {
        try {
            //android.location.Location mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            // Note that this can be NULL if last location isn't already known.
            //if (mCurrentLocation != null) {
                // Print current location if not null
                //Log.d(TAG, "current location: " + mCurrentLocation.toString());
                //LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            //}
            // Begin polling for new location updates.
            startLocationUpdates();
        }
        catch(SecurityException se) {
            Log.e(TAG, String.format("Security exception getting last location - %s", se.toString()));
        }
    }

    public void onConnectionSuspended(int cause) {
        String msg;

        if (cause == CAUSE_SERVICE_DISCONNECTED) {
            msg = "Disconnected. Please re-connect.";
        }
        else if (cause == CAUSE_NETWORK_LOST) {
            msg = "Network lost. Please re-connect.";
        }
        else {
            msg = String.format(Locale.getDefault(), "Connection lost (%d)", cause);
        }

        Toast.makeText(this.getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Toast.makeText(this.getActivity(), String.format("Google API connection failed: %s", connectionResult.getErrorMessage()), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    // Trigger new location updates at interval
    private void startLocationUpdates() {
        // Create the location request
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10000)
                .setFastestInterval(5000);
        // Request location updates
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
        }
        catch (SecurityException se) {
            Log.e(TAG, String.format("Security exception requesting location updates - %s", se.toString()));
        }
    }

    public void onLocationChanged(android.location.Location location) {
        // New location has now been determined
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());

        Log.d(TAG, msg);

        if (mMapFragmentActions != null) {
            Location myLocation = new Location(location); // Convert android.location.Location to Tracker.Location.

            mMapFragmentActions.updateMyLocation(myLocation);
        }
    }

    public void centerMap(Location loc) {
        centerMap(loc, -1);
    }

    public void centerMap(Location loc, float zoom) {
        if (loc != null) {
            LatLng latLng = loc.getLatLng();

            CameraPosition.Builder cpb = CameraPosition.builder();
            cpb.target(latLng);
            cpb.zoom(zoom >= 0 ? zoom : mMap.getCameraPosition().zoom);

            CameraUpdate cu = CameraUpdateFactory.newCameraPosition(cpb.build());

            mMap.animateCamera(cu);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_map, container, false);

        SupportMapFragment mapFragment = (SupportMapFragment)(getChildFragmentManager().findFragmentById(R.id.map));

        Log.d(TAG, String.format("Got MapFragment = %s", (mapFragment == null ? "NO" : "YES")));

        if (mapFragment != null)
            mapFragment.getMapAsync(this);

        return v;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof IMapFragmentActions) {
            mMapFragmentActions = (IMapFragmentActions) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMapFragmentActions = null;
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG, "onMapReady");

        mMap = googleMap;
        UiSettings mapSettings = mMap.getUiSettings();

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151);

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Have permission, so setting MyLoc");
            mMap.setMyLocationEnabled(true);
        }
        else {
            Log.d(TAG, "Don't have permission, so requesting");
            checkMyLocationPermission();
        }

        mapSettings.setZoomControlsEnabled(true);
        mapSettings.setZoomGesturesEnabled(true);
        mapSettings.setCompassEnabled(true);

        if (mMapFragmentActions != null) {
            mMapFragmentActions.onMapReady(this, mMap);
        }

        Log.d(TAG, "onMapReady End");
    }

    private boolean checkMyLocationPermission() {
        // Do we have permission to access the users location?
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)) {
                Toast msg = Toast.makeText(getActivity(), "Cannot use My Location feature of Google Map due to insufficient permission. Enable 'Location' permission .", Toast.LENGTH_LONG);
                msg.show();
            }
            else {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Tracker.PERMISSION_REQUEST.LOCATION.getValue());
            }
        }

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == Tracker.PERMISSION_REQUEST.LOCATION.getValue()) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                try {
                    mMap.setMyLocationEnabled(true);
                } catch (SecurityException sec) {
                    Toast m = Toast.makeText(this.getActivity(), "Error enabling My Location feature: " + sec.toString(), Toast.LENGTH_LONG);
                    m.show();
                }
            }
        }
    }

    Marker createMapMarker(TrackedItem trackedItem, Location location) {
        MarkerOptions options = new MarkerOptions();
        options.position(location.getLatLng());
        options.icon(BitmapDescriptorFactory.defaultMarker(trackedItem.getColour()));
        options.visible(true);

        return mMap.addMarker(options);
    }
}
