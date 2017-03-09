package me.marcsymonds.tracker;

import me.marcsymonds.tracker.BuildConfig;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class AboutDialogFragment extends android.support.v4.app.DialogFragment {
    public AboutDialogFragment() {
        // Required empty public constructor
    }

    public static AboutDialogFragment newInstance() {
        return new AboutDialogFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override @NonNull
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog d = super.onCreateDialog(savedInstanceState);

        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return d;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_about_dialog, container, false);

        AboutDialogFrame df = (AboutDialogFrame)v.findViewById(R.id.about_box_frame);
        df.setController(this);

        TextView tv = (TextView)v.findViewById(R.id.legal_text);
        String text = readRawTextFile(getActivity(), R.raw.legal);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            tv.setText(Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
        else
            tv.setText(Html.fromHtml(text));

        // Example of reading text from a raw file and putting in to a text field.

        /*tv = (TextView)v.findViewById(R.id.about_text);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            tv.setText(Html.fromHtml(readRawTextFile(getActivity(), R.raw.about), Html.FROM_HTML_MODE_LEGACY));
        else
            tv.setText(Html.fromHtml(readRawTextFile(getActivity(), R.raw.about)));

        tv.setLinkTextColor(Color.WHITE);

        Linkify.addLinks(tv, Linkify.ALL);*/

        tv = (TextView)v.findViewById(R.id.about_version_number);
        tv.setText(BuildConfig.VERSION_NAME);

        tv = (TextView)v.findViewById(R.id.about_copyright);
        tv.setText(getString(R.string.about_copyright));
        
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Nullable
    private static String readRawTextFile(Context context, int id) {
        String output;

        try {
            InputStream inputStream = context.getResources().openRawResource(id);

            InputStreamReader in = new InputStreamReader(inputStream);
            BufferedReader buf = new BufferedReader(in);

            String line;

            StringBuilder text = new StringBuilder();

            try {
                while ((line = buf.readLine()) != null) {
                    text.append(line);
                }

                in.close();
                buf.close();
                inputStream.close();

                output = text.toString();
            } catch (IOException e) {
                output = "!ERROR! " + e.getMessage();
            }
        }
        catch (Resources.NotFoundException nfe) {
            output = "!Not Found!";
        }

        return output;
    }
}
