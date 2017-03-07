package me.marcsymonds.tracker;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * This class extends the FrameLayout class, and is used in the fragment_about_dialog.
 * We use the onInterceptTouchEvent and onTouchEvents methods to allow the user to touch anywhere
 * on the dialog to close it. We do this by intercepting all of the touch events within the
 * FrameLayout (including all children), and if there is a DOWN event, the dialog is closed.
 */
public class AboutDialogFrame extends FrameLayout {
    private final String TAG = "AboutDialogFrame";
    private AboutDialogFragment mController = null;

    /**
     * Standard constructors for a FrameLayout object.
     * Not sure which of these are needed, so included them all.
     *
     * @param context
     */
    public AboutDialogFrame(Context context) {
        super(context);
    }

    public AboutDialogFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AboutDialogFrame(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // This constructor is used in API>=21.
    @TargetApi(21)
    public AboutDialogFrame(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Sets the "controller" AboutDialogFragment object for this FrameLayout. This will then allow
     * us to call the "dismiss" method on the fragment when the user touches the fragment view.
     *
     * @param controller AboutDialogFragment that this FrameLayout is being used by.
     */
    public void setController(AboutDialogFragment controller) {
        mController = controller;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent mv) {
        Log.d(TAG, String.format("onInterceptTouchEvent: %s %s", mv.toString(), mv.getAction()));

        // We just want to look for the ACTION_DOWN event, and handle this ourselves.

        return (mv.getAction() == MotionEvent.ACTION_DOWN);
    }

    @Override
    public boolean onTouchEvent(MotionEvent mv) {
        Log.d(TAG, String.format("onTouchEvent: %s %s", mv.toString(), mv.getAction()));

        // If the event was ACTION_DOWN, then dismiss (close) the fragment.

        if (mv.getAction() == MotionEvent.ACTION_DOWN) {
            mController.dismiss();
            return true;
        }
        else {
            return false;
        }
    }
}
