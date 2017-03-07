package me.marcsymonds.tracker;

import com.google.android.gms.maps.GoogleMap;

/**
 * Created by Marc on 04/03/2017.
 */

interface IMapFragmentActions {
    void onMapReady(MapFragment mapFragment, GoogleMap map);
    void updateMyLocation(Location location); // Returns indicates whether to move map to location.
}
