package me.marcsymonds.tracker;

import android.content.res.ColorStateList;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.XmlRes;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import static android.os.Build.VERSION.SDK_INT;

public class TrackedItemButton implements Button.OnClickListener, Button.OnLongClickListener {
    private static final String TAG = "TrackedItemButton";

    private TrackedItem mTrackedItem;

    private View mButtonContainerView;
    private Button mButtonView;
    private ImageView mFollowingImage;
    private ImageView mPingingImage;

    TrackedItemButton(TrackedItem trackedItem) {
        int itemColour;

        mTrackedItem = trackedItem;

        LayoutInflater inflater = LayoutInflater.from(TrackedItems.AppActivity);
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
                Color.BLUE,
                pressedColour,
                Color.GREEN,
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

    View getButtonView() {
        return mButtonContainerView;
    }

    void setFollowingImage(boolean on) {
        mFollowingImage.setImageBitmap(TrackedItemButtonHelper.getFollowingImage(on));
    }

    void setPingingImage(boolean on) {
        mPingingImage.setImageBitmap(TrackedItemButtonHelper.getPingingImage(on));
    }

    @Override
    public boolean onLongClick(View view) {
        //TODO: Start pinging device.
        if (TrackedItems.AppActivity instanceof ITrackedItemActions) {
            ((ITrackedItemActions) TrackedItems.AppActivity).trackedItemButtonClick(mTrackedItem, true);
        }
        else {
            Log.e(TAG, "AppActivity not instance of ITrackedItemActions");
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        //TODO: Start following device location.
        if (TrackedItems.AppActivity instanceof ITrackedItemActions) {
            ((ITrackedItemActions) TrackedItems.AppActivity).trackedItemButtonClick(mTrackedItem, false);
        }
    }
}
