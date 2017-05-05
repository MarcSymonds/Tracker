package me.marcsymonds.tracker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Created by Marc on 10/04/2017.
 */

class HistoryUploader extends AsyncTask<String, Integer, HistoryUploaderState> implements View.OnClickListener {
    private static final String TAG = "HistoryUploader";

    private static final String URL_PING = "api/Ping";
    private static final String URL_TRACKER_HISTORY = "api/TrackerHistory";

    private static final SimpleDateFormat mXmlDateFormatter;

    static {
        // The single quotes mean output the T as-is. The quotes are not output.
        // The XXX means output timezone as [+-]HH:MM
        // Older APIs (Acer B1) don't recognise the XXX.
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            mXmlDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        } else {
            mXmlDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        }
    }

    private final DocumentBuilderFactory mDocumentBuilderFactory = DocumentBuilderFactory.newInstance();
    private final String mAppID;
    private final String mLocalURL;
    private final String mRemoteURL;
    private final HistoryManager mHistoryManager;
    private final LinearLayout mProgressBar;
    private final ProgressBar mProgressBarProgress;
    private final Button mCancelButton;
    private boolean mCompleted = false;
    private boolean mStopped = false;
    private Exception mException = null;
    private IHistoryUploaderController mController = null;

    HistoryUploader(Activity activity) {
        Context context = activity.getApplicationContext();
        mAppID = ApplicationIdentifier.getIdentifier(context);

        mHistoryManager = HistoryRecorder.getInstance(activity.getApplicationContext()).getHistoryManager();

        if (activity instanceof IHistoryUploaderController) {
            mController = (IHistoryUploaderController) activity;
        }

        // Get the 2 URLs we could upload to.
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        mLocalURL = sp.getString(Pref.PREF_HISTORY_UPLOAD_LOCAL_URL, "");
        mRemoteURL = sp.getString(Pref.PREF_HISTORY_UPLOAD_REMOTE_URL, "");

        // See if the activity calling us has a "progressBar" LinearLayout. If so, then we will use
        // it to show progress of the upload.

        mProgressBar = (LinearLayout) activity.findViewById(R.id.progressBar);
        if (mProgressBar == null) {
            mProgressBarProgress = null;
            mCancelButton = null;
        } else {
            mProgressBarProgress = (ProgressBar) mProgressBar.findViewById(R.id.progressBarProgress);
            mProgressBarProgress.setProgress(0);
            mProgressBarProgress.setMax(1);

            TextView tv = (TextView) mProgressBar.findViewById(R.id.progressBarText);
            tv.setText(activity.getString(R.string.uploading_history));

            // If the "progressBar" contains a cancel button, then we can use that to cancel the
            // upload.
            mCancelButton = (Button) mProgressBar.findViewById(R.id.progressBarCancelButton);
        }
    }

    void stop() {
        if (!mStopped && !isCancelled() && !isCompleted()) {
            mStopped = true;
            this.cancel(true);
        }
    }

    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     * <p>
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param fileNames The parameters of the task.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @Override
    protected HistoryUploaderState doInBackground(String... fileNames) {
        HistoryUploaderState result = HistoryUploaderState.SUCCESS;
        String url;
        boolean gotURL = false;

        if (mLocalURL.length() == 0 && mRemoteURL.length() == 0) {
            result = HistoryUploaderState.NO_URLS;
        } else {
            url = getTargetURL();
            if (url == null) {
                result = HistoryUploaderState.UNABLE_TO_CONNECT;
            } else {
                url = String.format("%s%s%s", url, url.endsWith("/") ? "" : "/", URL_TRACKER_HISTORY);

                int count = 0;
                this.publishProgress(0, fileNames.length);
                for (String fileName : fileNames) {
                    if (this.isCancelled()) {
                        result = mStopped ? HistoryUploaderState.STOPPED : HistoryUploaderState.CANCELLED;
                        break;
                    }

                    if (uploadHistoryFile(url, fileName)) {
                        mHistoryManager.deleteFile(fileName);
                    } else {
                        result = HistoryUploaderState.FAILED;
                        break;
                    }
                    ;

                    ++count;
                    publishProgress(count);
                }
            }
        }

        return result;
    }

    /**
     * Handler for the cancel button on the progress bar.
     *
     * @param view the clicked button.
     */
    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.progressBarCancelButton) {
            if (!this.isCancelled()) {
                view.setEnabled(false);
                this.cancel(false);
            }
        }
    }

    /**
     * Runs on the UI thread before {@link #doInBackground}.
     *
     * @see #onPostExecute
     * @see #doInBackground
     */
    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.VISIBLE);

            if (mCancelButton != null) {
                mCancelButton.setOnClickListener(this);
            }
        }
    }

    /**
     * <p>Runs on the UI thread after {@link #doInBackground}. The
     * specified result is the value returned by {@link #doInBackground}.</p>
     * <p>
     * <p>This method won't be invoked if the task was cancelled.</p>
     *
     * @param result The result of the operation computed by {@link #doInBackground}.
     * @see #onPreExecute
     * @see #doInBackground
     * @see #onCancelled(Object)
     */
    @Override
    protected void onPostExecute(HistoryUploaderState result) {
        super.onPostExecute(result);

        hideProgressBar();

        mCompleted = true;

        if (mController != null) {
            mController.HistoryUploadComplete(this, result);
        }
    }

    /**
     * <p>Runs on the UI thread after {@link #cancel(boolean)} is invoked and
     * {@link #doInBackground(Object[])} has finished.</p>
     * <p>
     * <p>The default implementation simply invokes {@link #onCancelled()} and
     * ignores the result. If you write your own implementation, do not call
     * <code>super.onCancelled(result)</code>.</p>
     *
     * @param result The result, if any, computed in
     *               {@link #doInBackground(Object[])}, can be null
     * @see #cancel(boolean)
     * @see #isCancelled()
     */
    @Override
    protected void onCancelled(HistoryUploaderState result) {
        super.onCancelled(result);

        hideProgressBar();

        mCompleted = true;

        if (mController != null) {
            mController.HistoryUploadComplete(this, result);
        }
    }

    /**
     * Runs on the UI thread after {@link #publishProgress} is invoked.
     * The specified values are the values passed to {@link #publishProgress}.
     *
     * @param values The values indicating progress.
     * @see #publishProgress
     * @see #doInBackground
     */
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);

        if (mProgressBarProgress != null) {
            if (values.length > 1) {
                mProgressBarProgress.setMax(values[1]);
            }

            mProgressBarProgress.setProgress(values[0]);
        }
    }

    private void hideProgressBar() {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);

            if (mCancelButton != null) {
                mCancelButton.setOnClickListener(null);
            }
        }
    }

    public boolean isCompleted() {
        return mCompleted;
    }

    public Exception getException() {
        return mException;
    }

    @Nullable
    private String getTargetURL() {
        if (mLocalURL.length() > 0) {
            if (pingWebService(mLocalURL)) {
                return mLocalURL;
            }
        }

        if (mRemoteURL.length() > 0) {
            if (pingWebService(mRemoteURL)) {
                return mRemoteURL;
            }
        }

        return null;
    }

    private boolean uploadHistoryFile(String url, String fileName) {
        boolean sent = false;

        try {
            //Log.d(TAG, String.format("Uploading history file %s", fileName));
            File file = mHistoryManager.getFileFor(fileName);
            //Log.d(TAG, String.format("Loading history from %s", file.getAbsoluteFile()));
            ArrayList<Location> locations = mHistoryManager.loadHistoryFile(file);
            //Log.d(TAG, String.format("Loaded %d history items", locations.size()));

            if (locations.size() > 0) {
                //Log.d(TAG, "Building XML document from history");
                Document dataXML = buildXMLMessage(locations);

                //Log.d(TAG, String.format("Opening connection to %s", url));
                URL dest = new URL(url);
                HttpURLConnection req = (HttpURLConnection) dest.openConnection();
                try {
                    //Log.d(TAG, String.format("Connection open - setting up request"));
                    req.setDoOutput(true);
                    req.setRequestMethod("POST");
                    req.setConnectTimeout(10000);
                    req.setReadTimeout(10000);

                    //Log.d(TAG, "Sending data upstream");
                    putDataIntoHTTPStream(req, dataXML);

                    //Log.d(TAG, "Reading response");
                    if (req.getResponseCode() == HttpURLConnection.HTTP_ACCEPTED) {
                        sent = true;
                    }
                    //Log.d(TAG, String.format("Read response %s", String.valueOf(req.getResponseCode())));
                } finally {
                    //Log.d(TAG, "Disconnecting");
                    req.disconnect();
                }
            } else {
                // No locations in file, so assume sent.
                sent = true;
            }
        } catch (Exception ex) {
            mException = ex;
        }

        return sent;
    }

    /**
     * Build the XML message that will be sent to the server.
     *
     * @param locations the list of locations (Location) that will be included within the message.
     * @return Document with the XML.
     * @throws ParserConfigurationException
     */
    private Document buildXMLMessage(ArrayList<Location> locations) throws ParserConfigurationException {
        // Build XML for sending to the server.

        DocumentBuilder documentBuilder = mDocumentBuilderFactory.newDocumentBuilder();
        Document doc = documentBuilder.newDocument();

        Element trackerHistoryNode = doc.createElement("TrackerHistory");
        doc.appendChild(trackerHistoryNode);

        Element applicationNode = doc.createElement("Application");
        applicationNode.setAttribute("guid", mAppID);
        trackerHistoryNode.appendChild(applicationNode);

        TrackedItem trackedItem;
        int currentTrackerDeviceID = -1;
        boolean started = false;

        Element trackerDeviceNode = null;
        Element historyNode, additionalNode;

        for (Location location : locations) {
            if (location.getDevice() != currentTrackerDeviceID) {
                trackerDeviceNode = doc.createElement("TrackerDevice");

                currentTrackerDeviceID = location.getDevice();
                if (currentTrackerDeviceID == 0) {
                    trackerDeviceNode.setAttribute("guid", mAppID);
                    trackerDeviceNode.setAttribute("isMyLocation", "true");
                } else {
                    trackedItem = TrackedItems.getItemByID(currentTrackerDeviceID);

                    trackerDeviceNode.setAttribute("guid", trackedItem.getGUID());
                    trackerDeviceNode.setAttribute("isMyLocation", "false");
                    trackerDeviceNode.setAttribute("name", trackedItem.getName());
                    trackerDeviceNode.setAttribute("telephoneNumber", trackedItem.getTrackerDevice().getTelephoneNumber());
                }

                trackerHistoryNode.appendChild(trackerDeviceNode);
            }

            historyNode = doc.createElement("History");
            historyNode.setAttribute("whenRecorded", mXmlDateFormatter.format(location.getDateTime()));

            if (location.hasLocation()) {
                historyNode.setAttribute("latitude", Double.toString(location.getLatitude()));
                historyNode.setAttribute("longitude", Double.toString(location.getLongitude()));
                historyNode.setAttribute("isGps", location.isGPS() ? "true" : "false");
            }

            if (location.hasAdditionalInfo()) {
                additionalNode = doc.createElement("Additional");

                if (location.getLAC() != null) {
                    additionalNode.setAttribute("lac", location.getLAC());
                }

                if (location.getCID() != null) {
                    additionalNode.setAttribute("cid", location.getCID());
                }

                if (location.getMessage() != null) {
                    additionalNode.setAttribute("message", location.getMessage());
                }

                if (location.hasLastKnownLocation()) {
                    additionalNode.setAttribute("lastKnownLatitude", Double.toString(location.getLastKnownLatitude()));
                    additionalNode.setAttribute("lastKnownLongitude", Double.toString(location.getLastKnownLongitude()));
                }

                historyNode.appendChild(additionalNode);
            }

            trackerDeviceNode.appendChild(historyNode);
        }

        return doc;
    }

    /**
     * Send the message, which is in the passed Document, to the server.
     *
     * @param req HttpURLConnection request being used to send the data to the server.
     * @param doc Document containing the XML message to send to the server.
     * @throws TransformerException
     * @throws IOException
     */
    private void putDataIntoHTTPStream(HttpURLConnection req, Document doc) throws TransformerException, IOException {
        // Wot, no "doc.OuterXml"!
        // A lot of faffing to convert the XML doc to a string.
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        // We could stream directly from the Document to the output stream, but we need to know the
        // size of the data for the HTTP header. So we convert to a String first, then output that
        // to the stream.
        transformer.transform(new DOMSource(doc.getDocumentElement()), new StreamResult(writer));

        String data = writer.toString();

        req.setFixedLengthStreamingMode(data.length());

        OutputStreamWriter out = new OutputStreamWriter(req.getOutputStream());
        out.write(data);
        out.close();
    }

    /**
     * Ping the TrackerHistory web service. This is meant to just be a simple call to see if we can
     * connect to the server.
     *
     * @param url URL to connect to. /api/Ping will be added this.
     * @return true if managed to connect to the server, otherwise false.
     */
    private boolean pingWebService(String url) {
        boolean pinged = false;

        try {
            url = String.format("%s%s%s", url, url.endsWith("/") ? "" : "/", URL_PING);
            URL dest = new URL(url);
            HttpURLConnection req = (HttpURLConnection) dest.openConnection();
            try {
                req.setDoInput(false);
                req.setDoOutput(false);
                req.setConnectTimeout(10000);
                req.setReadTimeout(10000);
                req.setRequestMethod("GET");
                req.connect();

                if (req.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    pinged = true;
                }
            } finally {
                req.disconnect();
                req = null;
            }
        } catch (Exception ex) {
            mException = ex;
            Log.e(TAG, String.format("Exception pinging %s - %s", url, ex.getMessage()));
        }

        return pinged;
    }
}
