package me.marcsymonds.tracker;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by Marc on 14/04/2017.
 */

class PermissionChecker {
    private static HashMap<Integer, Integer> mRequestOptions = new HashMap<>();
    private static int mRequestID = 0;

    static void checkNeededPermissions(Activity activity) {
        ArrayList<String> neededPermissions = new ArrayList<>();
        Context context = activity.getApplicationContext();

        if (!hasPermissions(context, Manifest.permission.RECEIVE_SMS)) {
            neededPermissions.add(Manifest.permission.RECEIVE_SMS + "|Receive SMS messages");
        }

        if (!hasPermissions(context, Manifest.permission.READ_SMS)) {
            neededPermissions.add(Manifest.permission.READ_SMS + "|Read SMS messages");
        }

        if (!hasPermissions(context, Manifest.permission.SEND_SMS)) {
            neededPermissions.add(Manifest.permission.SEND_SMS + "|Send SMS messages");
        }

        if (neededPermissions.size() > 0) {
            acquirePermission(activity, neededPermissions, 0);
        }
    }

    static boolean hasPermissions(Context context, String permission) {
        return ActivityCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    static boolean acquireSMSSendPermissionForDevice(Activity activity, int deviceID) {
        int requestID;

        if (hasPermissions(activity.getApplicationContext(), Manifest.permission.SEND_SMS)) {
            return true;
        }

        synchronized (mRequestOptions) {
            requestID = ++mRequestID;
            mRequestOptions.put(requestID, deviceID);
        }

        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.SEND_SMS + "|Send SMS messages");

        if (!acquirePermission(activity, permissions, requestID)) {
            // We're not asynchronously getting permission, so remove the value from the hash table.

            synchronized (mRequestOptions) {
                mRequestOptions.remove(requestID);
            }
        }

        return false;
    }

    static boolean acquirePermission(Activity activity, ArrayList<String> permissions, int requestID) {
        String denied = "";
        ArrayList<String> requests = new ArrayList<>();
        boolean requesting = false;

        for (String permission : permissions) {
            int i = permission.indexOf("|");
            String msg = permission.substring(i + 1);
            String perm = permission.substring(0, i);

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)) {
                if (denied.length() > 0) {
                    denied += ", ";
                }

                denied += msg;
            } else {
                requests.add(perm);
            }
        }

        if (denied.length() > 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            Resources res = activity.getResources();

            builder
                    .setTitle("Required permissions")
                    .setMessage(denied)
                    .setNegativeButton(res.getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setCancelable(true)
                    .show();
            //try {
            //builder.wait();
            //}
            //catch (InterruptedException ie) {
            //}
        }

        if (requests.size() > 0) {
            requesting = true;

            ActivityCompat.requestPermissions(
                    activity,
                    requests.toArray(new String[requests.size()]),
                    requestID);
        }

        return requesting;
    }

    static void RequestPermissionsResult(Activity activity, int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        boolean denied = false;

        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                denied = true;
                break;
            }
        }

        if (denied) {
            Toast.makeText(activity,
                    "Not all permissions were granted.",
                    Toast.LENGTH_LONG).show();
        }
        /*else if (permissions.length == 1) {
            if (permissions[0].equals(Manifest.permission.SEND_SMS) && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                synchronized (mRequestOptions) {
                    if (mRequestOptions.containsKey(requestCode)) {
                        int trackedItemID = mRequestOptions.get(requestCode);
                        // Try sending the message again.
                    }
                }
            }
        }*/

        synchronized (mRequestOptions) {
            if (mRequestOptions.containsKey(requestCode)) {
                mRequestOptions.remove(requestCode);
            }
        }
    }
}
