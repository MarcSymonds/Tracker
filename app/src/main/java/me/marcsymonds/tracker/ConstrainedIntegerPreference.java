package me.marcsymonds.tracker;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Created by Marc on 27/04/2017.
 */

public class ConstrainedIntegerPreference extends Preference implements View.OnTouchListener {
    private static final String TAG = "ConstrainedIntegerPref";

    private long mMinValue = 0;
    private long mMaxValue = 100;
    private long mValue = 0;

    private int mAdjustValue = 0;
    private Handler mAdjustHandler;
    private Runnable mAdjustRunnable;

    private ImageButton mValueDownView;
    private ImageButton mValueUpView;
    private TextView mValueView;

    public ConstrainedIntegerPreference(Context context) {
        super(context);
    }

    public ConstrainedIntegerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(attrs, 0);
    }

    public ConstrainedIntegerPreference(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        initAttrs(attrs, defaultStyle);
    }

    private void initAttrs(AttributeSet attrs, int defStyle) {
        TypedArray a =
                getContext()
                        .getTheme()
                        .obtainStyledAttributes(attrs, R.styleable.ConstrainedIntegerPreference, defStyle, defStyle);

        try {
            mMinValue = a.getInteger(R.styleable.ConstrainedIntegerPreference_minValue, 0);
            mMaxValue = a.getInteger(R.styleable.ConstrainedIntegerPreference_maxValue, 100);
        } finally {
            a.recycle();
        }

        setWidgetLayoutResource(R.layout.preference_constrained_integer);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);

        mValueView = (TextView) view.findViewById(R.id.preference_constrained_integer_value);

        mValueDownView = (ImageButton) view.findViewById(R.id.preference_constrained_integer_down);
        mValueUpView = (ImageButton) view.findViewById(R.id.preference_constrained_integer_up);

        mValueUpView.setOnTouchListener(this);
        mValueDownView.setOnTouchListener(this);

        //setEnabled(isEnabled());

        setValue(mValue);
    }

    /**
     * Called when the dependency changes.
     *
     * @param dependency       The Preference that this Preference depends on.
     * @param disableDependent Set true to disable this Preference.
     */
    @Override
    public void onDependencyChanged(Preference dependency, boolean disableDependent) {
        super.onDependencyChanged(dependency, disableDependent);

        setEnabled(!disableDependent);
    }

    /**
     * Sets whether this Preference is enabled. If disabled, it will
     * not handle clicks.
     *
     * @param enabled Set true to enable it.
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        if (mValueDownView != null) {
            Log.d(TAG, "setPreferenceEnableState: " + String.valueOf(enabled));
            mValueDownView.setEnabled(enabled);
            mValueUpView.setEnabled(enabled);
            mValueView.setEnabled(enabled);
        }
    }

    /**
     * Called when a touch event is dispatched to a view. This allows listeners to
     * get a chance to respond before the target view.
     *
     * @param v     The view the touch event has been dispatched to.
     * @param event The MotionEvent object containing full information about
     *              the event.
     * @return True if the listener has consumed the event, false otherwise.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.equals(mValueDownView) || v.equals(mValueUpView)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    boolean adjustUp = (v.equals(mValueUpView));
                    startChangingValue(adjustUp);

                    v.playSoundEffect(SoundEffectConstants.CLICK);
                    v.setPressed(true);
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    stopChangingValue();
                    v.setPressed(false);
                    break;
            }

            return true;
        }

        return false;
    }

    private void startChangingValue(final boolean adjustUp) {
        if (mAdjustHandler != null) {
            if (mAdjustRunnable != null) {
                mAdjustHandler.removeCallbacks(mAdjustRunnable);
            }
        } else {
            mAdjustHandler = new Handler();
        }

        if (!isAtLimit(adjustUp)) {
            setValue(mValue + (adjustUp ? 1 : -1));

            mAdjustRunnable = new Runnable() {
                private int adjustBy = 1;
                private int speedUp = 0;

                @Override
                public void run() {
                    if (!isAtLimit(adjustUp)) {
                        setValue(mValue + (adjustUp ? adjustBy : -adjustBy));

                        ++speedUp;
                        if (speedUp > 10) {
                            switch (adjustBy) {
                                case 1:
                                    adjustBy = 5;
                                    break;
                                case 5:
                                    adjustBy = 10;
                                    break;
                                case 10:
                                    adjustBy = 25;
                                    break;
                            }

                            speedUp = 0;
                        }

                        mAdjustHandler.postDelayed(this, 100); // <- here
                    }
                }
            };

            // Use long initial repeat delay. Shorter delays will be used subsequently; see above.
            mAdjustHandler.postDelayed(mAdjustRunnable, 750);
        }
    }

    private void stopChangingValue() {
        if (mAdjustHandler != null) {
            if (mAdjustRunnable != null) {
                mAdjustHandler.removeCallbacks(mAdjustRunnable);
                mAdjustRunnable = null;
            }

            mAdjustHandler = null;
        }

        saveValue();
    }

    /**
     * Called when the Preference hierarchy has been attached to the
     * {@link PreferenceActivity}. This can also be called when this
     * Preference has been attached to a group that was already attached
     * to the {@link PreferenceActivity}.
     */
    @Override
    protected void onAttachedToActivity() {
        try {
            super.onAttachedToActivity();
        } catch (ClassCastException cce) {
            // This happens when trying to view this control in the AndroidStudio editor.
            // So you can't preview this control :(
        }
    }

    /**
     * Called when a Preference is being inflated and the default value
     * attribute needs to be read. Since different Preference types have
     * different value types, the subclass should get and return the default
     * value which will be its value type.
     * <p>
     * For example, if the value type is String, the body of the method would
     * proxy to {@link TypedArray#getString(int)}.
     *
     * @param a     The set of attributes.
     * @param index The index of the default value attribute.
     * @return The default value of this preference type.
     */
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, (int) mMinValue);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        try {
            setValue(restoreValue ? getPersistedLong(mMinValue) : (Integer) defaultValue);
        } catch (Exception ex) {
            Log.d(TAG, String.format("onSetInitialValue [%s] - %s", String.valueOf(defaultValue), ex.toString()));
        }
    }

    long getValue() {
        return mValue;
    }

    private void setValue(long newValue) {
        if (newValue < mMinValue) {
            newValue = mMinValue;
        } else if (newValue > mMaxValue) {
            newValue = mMaxValue;
        }

        // Call any Change Listeners that might be attached.
        if (callChangeListener(newValue)) {
            mValue = newValue;

            if (mValueView != null) {
                mValueView.setText(String.valueOf(mValue));
            }
        }
    }

    private boolean isAtLimit(boolean adjustUp) {
        return ((adjustUp && mValue == mMaxValue) || (!adjustUp && mValue == mMinValue));
    }

    private void saveValue() {
        if (isPersistent() && hasKey()) {
            persistLong(mValue);
        }

        // Side Note: If you call notifyChanged() after handling an ACTION_DOWN touch event, this
        // seems to cancel the touch event and send an ACTION_CANCEL touch event, and you never get
        // the ACTION_UP touch event.
        // Hence this is being called after the ACTION_UP touch event.

        notifyChanged();
    }
}
