package me.marcsymonds.tracker;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import static android.os.Build.VERSION.SDK_INT;

class TrackedItemButton implements Button.OnClickListener, Button.OnLongClickListener {
    private static final String TAG = "TrackedItemButton";

    private Activity mActivity;

    private TrackedItem mTrackedItem;

    private View mButtonContainerView;
    private Button mButtonView;
    private ImageView mFollowingImage;
    private ImageView mPingingImage;
    private Handler mDoubleClickHandler = null;
    private Runnable mDoubleClickRunner = null;

    TrackedItemButton(Activity activity, TrackedItem trackedItem) {
        mActivity = activity;
        mTrackedItem = trackedItem;

        LayoutInflater inflater = LayoutInflater.from(mActivity);
        mButtonContainerView = inflater.inflate(R.layout.tracked_item_button, null);

        mButtonView = (Button)mButtonContainerView.findViewById(R.id.butTrackedItem);
        mFollowingImage = (ImageView)mButtonContainerView.findViewById(R.id.imgTrackedItemFollow);
        mPingingImage = (ImageView)mButtonContainerView.findViewById(R.id.imgTrackedItemPing);

        mButtonView.setOnClickListener(this);
        mButtonView.setOnLongClickListener(this);

        setButtonAppearance();

        setFollowingImage(false);
        setPingingImage(false);
    }

    void dispose() {
        if (mButtonContainerView != null) {
            ViewGroup parent = (ViewGroup) mButtonContainerView.getParent();
            if (parent != null) {
                parent.removeView(mButtonContainerView);
            }

            mButtonView = null;
            mFollowingImage = null;
            mPingingImage = null;
            mButtonContainerView = null;

            if (mDoubleClickHandler != null && mDoubleClickRunner != null) {
                mDoubleClickHandler.removeCallbacks(mDoubleClickRunner);
            }

            mDoubleClickHandler = null;
            mDoubleClickRunner = null;
        }
    }


    void setButtonAppearance() {
        int itemColour = mTrackedItem.getColour();

        String text = mTrackedItem.getName();
        if (text.length() > 8) {
            int spc = text.indexOf(" ");
            if (spc < 0 || spc > 8) {
                spc = 8;
            }
            text = text.substring(0, spc);
        }

        mButtonView.setText(text);

        /*
        ColorStateList:-

        res/color/tracked_item_button_colours.xml

        <selector xmlns:android="http://schemas.android.com/apk/res/android">
            <item android:state_focused="true" android:color="#FF0000" />
            <item android:state_pressed="true" android:color="#00FF00" />
            <item android:state_enabled="false"android:color="#0000FF" />
            <item android:color="#FF00FF" />
        </selector>
        */

        int[][] stateList = {
                {android.R.attr.state_focused }, // Don't really need this for this app.
                {android.R.attr.state_pressed },
                {-android.R.attr.state_enabled }, // Negative means if state is false.
                { } // Empty list indicates the default colour.
        };

        int pressedColour = Color.HSVToColor(150, new float [] {(float)itemColour, 1, 1}); // Darker colour when pressed.
        int normalColour = Color.HSVToColor(75, new float[] {(float)itemColour, 1, 1}); // Lighter colour normally.

        int[] backgroundColours = {
                Color.BLUE, // Not used in this app - just an example.
                pressedColour,
                Color.GREEN, // Not used in this app - just an example.
                normalColour
        };

        int pressedText;
        int normalText;

        if (Build.VERSION.SDK_INT > 24) {
            pressedText = Color.luminance(pressedColour) < 0.5f ? Color.WHITE : Color.BLACK;
            normalText = Color.luminance(normalColour) < 0.5f ? Color.WHITE : Color.BLACK;
        }
        else {
            pressedText = Color.BLACK;
            normalText = Color.BLACK;
        }

        int[] textColours = {
                Color.WHITE,
                pressedText,
                Color.WHITE,
                normalText
        };

        ColorStateList csl = new ColorStateList(stateList, backgroundColours);

        if (SDK_INT >= 23) {
            mButtonView.setBackgroundTintList(csl);

            csl = new ColorStateList(stateList, textColours);
            mButtonView.setTextColor(csl);
        }
    }


    View getButtonContainerView() {
        return mButtonContainerView;
    }

    View getButtonView() {
        return mButtonView;
    }

    void setFollowingImage(boolean on) {
        mFollowingImage.setImageBitmap(TrackedItemButtonHelper.getFollowingImage(on));
    }

    void setPingingImage(boolean on) {
        mPingingImage.setImageBitmap(TrackedItemButtonHelper.getPingingImage(on));
    }

    @Override
    public boolean onLongClick(View view) {
        if (mActivity instanceof ITrackedItemActions) {
            ((ITrackedItemActions) mActivity).trackedItemButtonLongClick(mTrackedItem);
        }
        else {
            Log.e(TAG, "AppActivity not instance of ITrackedItemActions");
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "Click");
        if (mActivity instanceof ITrackedItemActions) {
            if (mDoubleClickHandler == null) {
                Log.d(TAG, "Setting up Single Click");
                mDoubleClickHandler = new Handler();
                mDoubleClickRunner = new Runnable() {
                    @Override
                    public void run() {
                        ((ITrackedItemActions) mActivity).trackedItemButtonClick(mTrackedItem);

                        mDoubleClickHandler = null;
                        mDoubleClickRunner = null;
                    }
                };

                mDoubleClickHandler.postDelayed(mDoubleClickRunner, 250);
            } else {
                mDoubleClickHandler.removeCallbacks(mDoubleClickRunner);
                mDoubleClickHandler = null;
                mDoubleClickRunner = null;

                ((ITrackedItemActions) mActivity).trackedItemButtonDoubleClick(mTrackedItem);
            }
        }
    }
}
