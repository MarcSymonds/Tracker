package me.marcsymonds.tracker;

import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.Preference;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * This class (and associated objects) are based on a colour (color) picker widget by Kizito Nwose,
 * which can be found at https://github.com/kizitonwose/colorpreference.
 *
 * I couldn't get that component to work - probably something I was doing wrong. So I decided to use
 * the code to build the component directly in to my app.
 */
public class ColourPickerPreference extends Preference implements ColourPickerDialog.OnColourSelectedListener {
    private final String TAG = "ColourPickerPreference";

    private final Context mContext;
    private int mColour = 0;
    private int[] mColourChoices = {};

    public ColourPickerPreference(Context context) {
        super(context);
        mContext = context;
    }

    public ColourPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initAttrs(attrs, 0);
    }

    public ColourPickerPreference(Context context, AttributeSet attrs, int defaultStyle) {
        super(context, attrs, defaultStyle);
        mContext = context;
        initAttrs(attrs, defaultStyle);
    }

    private void initAttrs(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().getTheme().obtainStyledAttributes(
                attrs, R.styleable.ColourPickerPreference, defStyle, defStyle);

        try {
            int choicesResId = a.getResourceId(R.styleable.ColourPickerPreference_colourPickerChoices,
                    R.array.preference_colour_picker_default_colours);

            if (choicesResId > 0) {
                String[] choices = a.getResources().getStringArray(choicesResId);

                mColourChoices = new int[choices.length];
                for (int i = 0; i < choices.length; i++) {
                    mColourChoices[i] = Integer.parseInt(choices[i]);// Color.parseColor(choices[i]);
                }
            }

        } finally {
            a.recycle();
        }

        //setTitle("Colour");
        //setSummary("Colour of marker on the map");

        setWidgetLayoutResource(R.layout.preference_colour_picker_preview);
    }

    @Override
    protected void onBindView(View view) {
        super.onBindView(view);
        View preview = view.findViewById(R.id.preference_colour_picker_preview_colour);
        ColourPickerUtils.setColourViewValue(
                preview,
                Color.HSVToColor(new float [] {mColour, 1, 1}),
                false,
                2);
    }

    private int getColour() {
        return mColour;
    }

    private void setColour(int colour) {
        Log.d(TAG, String.format("setColour %d - Has Key:%s, Is Persistent:%s", colour, hasKey() ? "YES" : "NO", isPersistent() ? "YES" : "NO"));
        if (callChangeListener(colour)) {
            mColour = colour;
            Log.d(TAG, String.format("shouldPersist = %s", shouldPersist() ? "YES": "NO"));
            persistInt(colour);
            notifyChanged();
        }
        else
            Log.d(TAG, "callChangeListener failed");
    }

    @Override
    protected void onClick() {
        super.onClick();

        ColourPickerDialog fragment = ColourPickerDialog.newInstance(getColour(), mColourChoices);
        fragment.setOnColourSelectedListener(this);

        Activity activity = (Activity) getContext();
        activity.getFragmentManager()
                .beginTransaction()
                .add(fragment, getFragmentTag())
                .commit();
    }

    @Override
    protected void onAttachedToActivity() {
        try {
            super.onAttachedToActivity();

            Activity activity = (Activity) getContext();
            ColourPickerDialog fragment = (ColourPickerDialog) activity.getFragmentManager().findFragmentByTag(getFragmentTag());

            if (fragment != null) {
                // re-bind preference to fragment
                fragment.setOnColourSelectedListener(this);
            }
        } catch (ClassCastException cce) {
            // This happens when trying to view this control in the AndroidStudio editor.
            // So you can't preview this control :(
        }
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        try {
            setColour(restoreValue ? getPersistedInt(0) : (Integer) defaultValue);
        }
        catch(Exception ex) {
            Log.d(TAG, String.format("onSetInitialValue [%s] - %s", String.valueOf(defaultValue), ex.toString()));
        }
    }

    private String getFragmentTag() {
        return "colour_" + getKey();
    }

    @Override
    public void onColourSelected(int colour, String tag) {
        setColour(colour);
    }
}
