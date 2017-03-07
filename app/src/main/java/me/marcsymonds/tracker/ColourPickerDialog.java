package me.marcsymonds.tracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ArrayRes;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;

/**
 * Created by Marc on 20/02/2017.
 */

public class ColourPickerDialog extends DialogFragment {
    private final String TAG = "ColourPickerDialog";

    private static final String KEY_CURRENT_COLOUR = "current_colour";
    private static final String KEY_COLOUR_CHOICES = "colour_choices";

    private int mCurrentColour = 0;
    private int[] mColourChoices;
    private GridLayout mColourGrid;
    private ColourPickerDialog.OnColourSelectedListener colourSelectedListener;

    public interface OnColourSelectedListener {
        void onColourSelected(int colour, String tag);
    }

    public ColourPickerDialog() {
    }

    public static ColourPickerDialog newInstance(int currentColour, int[] colourChoices) {
        ColourPickerDialog d = new ColourPickerDialog();
        Bundle args = new Bundle();

        args.putInt(KEY_CURRENT_COLOUR, currentColour);
        args.putIntArray(KEY_COLOUR_CHOICES, colourChoices);
        d.setArguments(args);

        return d;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mCurrentColour = args.getInt(KEY_CURRENT_COLOUR);
        mColourChoices = args.getIntArray(KEY_COLOUR_CHOICES);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
        View rootView = layoutInflater.inflate(R.layout.preference_colour_picker_grid, null);

        mColourGrid = (GridLayout) rootView.findViewById(R.id.colour_picker_dialog_grid);
        mColourGrid.setColumnCount(5);
        repopulateItems();

        return new AlertDialog.Builder(getActivity())
                .setView(rootView)
                .create();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof OnColourSelectedListener) {
            setOnColourSelectedListener((OnColourSelectedListener) context);
        } else {
            repopulateItems();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        sizeDialog();
    }

    public void setOnColourSelectedListener(OnColourSelectedListener colourSelectedListener) {
        this.colourSelectedListener = colourSelectedListener;
        repopulateItems();
    }

    private void repopulateItems() {
        if (colourSelectedListener == null || mColourGrid == null) {
            return;
        }

        Context context = mColourGrid.getContext();
        mColourGrid.removeAllViews();
        for (final int colour : mColourChoices) {
            View itemView = LayoutInflater.from(context)
                    .inflate(R.layout.preference_colour_picker_grid_item, mColourGrid, false);

            ColourPickerUtils.setColourViewValue(
                    itemView.findViewById(R.id.preference_colour_picker_view),
                    Color.HSVToColor(new float [] {colour, 1, 1}),
                    colour == mCurrentColour,
                    2);

            itemView.setClickable(true);
            itemView.setFocusable(true);
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (colourSelectedListener != null) {
                        colourSelectedListener.onColourSelected(colour, getTag());
                    }
                    dismiss();
                }
            });

            mColourGrid.addView(itemView);
        }

        sizeDialog();
    }

    private void sizeDialog() {
        if (colourSelectedListener == null || mColourGrid == null) {
            return;
        }

        Dialog dialog = getDialog();
        if (dialog == null) {
            return;
        }

        final Resources res = mColourGrid.getContext().getResources();
        DisplayMetrics dm = res.getDisplayMetrics();

        int w = View.MeasureSpec.makeMeasureSpec(dm.widthPixels, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(dm.heightPixels, View.MeasureSpec.UNSPECIFIED);

        Log.d(TAG, String.format("Dialog measurements: %d, %s", w, h));

        mColourGrid.measure(w, h);

        int width = mColourGrid.getMeasuredWidth();
        int height = mColourGrid.getMeasuredHeight();

        Log.d(TAG, String.format("Grid measurements: %d, %d", width, height));

        int extraPadding = res.getDimensionPixelSize(R.dimen.colour_picker_grid_extra_padding);

        width += extraPadding;
        height += extraPadding;

        Log.d(TAG, String.format("Size for dialog: %d, %d", width, height));

        dialog.getWindow().setLayout(width, height);
    }

    public static class Builder {
        private int[] mColourChoices;
        private Context context;
        private int mSelectedColour;
        private String tag;

        public <ColorActivityType extends Activity & OnColourSelectedListener> Builder(@NonNull ColorActivityType context) {
            this.context = context;
            setColourChoices(R.array.preference_colour_picker_default_colours);
        }

        /*public Builder setNumColumns(int numColumns) {
            this.numColumns = numColumns;
            return this;
        }*/

        public Builder setColourChoices(@ArrayRes int colourChoicesRes) {
            String[] choices = context.getResources().getStringArray(colourChoicesRes);

            int[] colours = new int[choices.length];
            for (int i = 0; i < choices.length; i++) {
                colours[i] = Integer.parseInt(choices[i]);// ColourPickerUtils.hsv2rgb(Float.parseFloat(choices[i]), 1, 1);
            }

            mColourChoices = colours;
            return this;
        }

        /*public Builder setColorShape(ColorShape colorShape) {
            this.colorShape = colorShape;
            return this;
        }*/

        public Builder setSelectedColour(@ColorInt int selectedColour) {
            mSelectedColour = selectedColour;
            return this;
        }

        public Builder setTag(String tag) {
            this.tag = tag;
            return this;
        }

        protected ColourPickerDialog build() {
            ColourPickerDialog dialog = ColourPickerDialog.newInstance(mSelectedColour, mColourChoices);
            dialog.setOnColourSelectedListener((OnColourSelectedListener) context);
            return dialog;
        }

        public ColourPickerDialog show() {
            ColourPickerDialog dialog = build();
            dialog.show(resolveContext(context).getFragmentManager(), tag == null ? String.valueOf(System.currentTimeMillis()) : tag);
            return dialog;
        }

        protected Activity resolveContext(Context context) {
            if (context instanceof Activity) {
                return (Activity) context;
            } else if (context instanceof ContextWrapper) {
                return resolveContext(((ContextWrapper) context).getBaseContext());
            }
            return null;
        }
    }
}




