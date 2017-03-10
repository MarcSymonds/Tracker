package me.marcsymonds.tracker;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

class ColourPickerUtils {
    private static final String TAG = "ColourPickerUtils";

    static void setColourViewValue(View view, int color, boolean selected, int shape) {
        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;
            Resources res = imageView.getContext().getResources();

            Drawable currentDrawable = imageView.getDrawable();
            GradientDrawable colorChoiceDrawable;
            if (currentDrawable instanceof GradientDrawable) {
                // Reuse drawable
                colorChoiceDrawable = (GradientDrawable) currentDrawable;
            } else {
                colorChoiceDrawable = new GradientDrawable();
                colorChoiceDrawable.setShape(shape == 2 ? GradientDrawable.RECTANGLE : GradientDrawable.OVAL);
            }

            // Set stroke to dark version of color
            int darkenedColor = Color.rgb(
                    Color.red(color) * 192 / 256,
                    Color.green(color) * 192 / 256,
                    Color.blue(color) * 192 / 256);

            colorChoiceDrawable.setColor(color);
            colorChoiceDrawable.setStroke((int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 2, res.getDisplayMetrics()), darkenedColor);

            Drawable drawable = colorChoiceDrawable;
                /*if (selected) {
                    BitmapDrawable checkmark = (BitmapDrawable) res.getDrawable(isColorDark(color)
                            ? R.drawable.checkmark_white
                            : R.drawable.checkmark_black);
                    checkmark.setGravity(Gravity.CENTER);
                    drawable = new LayerDrawable(new Drawable[]{
                            colorChoiceDrawable,
                            checkmark});
                }*/

            imageView.setImageDrawable(drawable);

        } else if (view instanceof TextView) {
            ((TextView) view).setTextColor(color);
        }
    }

    /**
     * Convert a HSV (Hue, Saturation, Value) in to RGB. The markers on the GoogleMap use a Hue
     * to set the colour of the marker, so we need to ability to convert Hue to RGB for displaying
     * the colours.
     *
     * @ param h Hue (0 - 359).
     * @ param s Saturation (0 - 1).
     * @ param v Value (0 - 1).
     * @ return int (Colour).
     */
    /*static int hsv2rgb(float h, float s, float v)
    {
        //float hue = h;

        /*
            C = V × S
            X = C × (1 - |(H / 60°) mod 2 - 1|)
            m = V - C

            (R,G,B) = ((R'+m)×255, (G'+m)×255, (B'+m)×255)
        * /

        if (h < 0) {
            h = 0;
        }
        else if (h > 359) {
            h = 359;
        }

        float c = s * v;
        float x = c * (1 - Math.abs((h / 60) % 2 - 1));
        float m = v - c;
        float r = 0, g = 0, b = 0;

        switch((int)Math.floor(h / 60)) {
            case 0:
                r = c;
                g = x;
                break;
            case 1:
                r = x;
                g = c;
                break;
            case 2:
                g = c;
                b = x;
                break;
            case 3:
                g = x;
                b = c;
                break;
            case 4:
                r = x;
                b = c;
                break;
            default:
                r = c;
                b = x;
                break;
        }

        r = Math.round((r + m) * 255);
        g = Math.round((g + m) * 255);
        b = Math.round((b + m) * 255);

        //Log.d(TAG, String.format("HSV2RGB: %f -> %d, %d, %d", hue, (int)r, (int)g, (int)b));

        return Color.argb(255, (int)r, (int)g, (int)b);
    }*/

    private static final int BRIGHTNESS_THRESHOLD = 150;

    private static boolean isColourDark(int color) {
        return ((30 * Color.red(color) +
                59 * Color.green(color) +
                11 * Color.blue(color)) / 100) <= BRIGHTNESS_THRESHOLD;
    }
}
