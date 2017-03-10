package me.marcsymonds.tracker;

import com.google.android.gms.maps.GoogleMap;

interface IMapFragmentActions {
    void onMapReady(MapFragment mapFragment, GoogleMap map);
    void updateMyLocation(Location location); // Return indicates whether to move map to location.
}
