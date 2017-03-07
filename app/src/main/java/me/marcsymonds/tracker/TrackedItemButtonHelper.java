package me.marcsymonds.tracker;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Created by Marc on 02/03/2017.
 */

public class TrackedItemButtonHelper {
    private static Context mContext;

    private static Bitmap mFollowingOn;
    private static Bitmap mFollowingOff;
    private static Bitmap mPingingOn;
    private static Bitmap mPingingOff;

    private static Bitmap mMyLocationOn;
    private static Bitmap mMyLocationOff;

    public static void initialise(Context context) {
        mContext = context;

        Resources res = context.getResources();

        mFollowingOn = BitmapFactory.decodeResource(res, R.drawable.img_my_location_on2);
        mFollowingOff = BitmapFactory.decodeResource(res, R.drawable.img_my_location_off2);
        mPingingOn = BitmapFactory.decodeResource(res, R.drawable.img_ping_on);
        mPingingOff = BitmapFactory.decodeResource(res, R.drawable.img_ping_off);

        mMyLocationOn = BitmapFactory.decodeResource(res, R.drawable.img_my_location_on2);
        mMyLocationOff = BitmapFactory.decodeResource(res, R.drawable.img_my_location_off2);
    }

    public static Bitmap getFollowingImage(boolean on) {
        return on ? mFollowingOn : mFollowingOff;
    }

    public static Bitmap getPingingImage(boolean on) {
        return on ? mPingingOn : mPingingOff;
    }

    public static Bitmap getMyLocationImage(boolean on) {
        return on ? mMyLocationOn : mMyLocationOff;
    }
}