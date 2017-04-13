package me.marcsymonds.tracker;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("me.marcsymonds.tracker", appContext.getPackageName());
    }

    @Test
    public void getAppID() throws Exception {
        String appID;
        Context context = InstrumentationRegistry.getTargetContext();

        appID = ApplicationIdentifier.getIdentifier(context);
    }

    @Test
    public void testMessageParsing() throws Exception {
        String[] msgs = new String[]{
                "sensor alarm!\n" +
                        "lat:-48.404748\n" +
                        "long:1.506225\n" +
                        "speed:0.0\n" +
                        "T:17/02/26 19:12\n" +
                        "http://maps.google.com/maps?f=q&q=148.404748,-11.506225&z=16\n" +
                        "Pwr: ON Door: OFF ACC: OFF\n",

                "battery low!\n",

                "lat:-48.404748\n" +
                        "long:1.506225\n" +
                        "speed:0.0\n" +
                        "T:17/02/26 19:12\n" +
                        "http://maps.google.com/maps?f=q&q=148.404748,-11.506225&z=16\n" +
                        "Pwr: ON Door: OFF ACC: OFF\n",

                "lat:-48.404748\n" +
                        "long:1.506225\n" +
                        "speed:0.0\n" +
                        "T:17/02/26 19:12\n" +
                        "http://maps.google.com/maps?f=q&q=148.404748,-11.506225&z=16\n" +
                        "Pwr: ON Door: OFF ACC: OFF\n"
        };


        // DOTALL means the dot (.) will match new-line characters.
        /*Pattern pat = Pattern.compile(
                "(([^\\n]+)\\n)?" +
                "(" +
                    "lac:((\\p{XDigit}+) (\\p{XDigit}+))" +
                    "|" +
                    "lat:(-?[0-9\\.]+)\\nlong:(-?[0-9\\.]+)" +
                ")?" +
                "(.*http://.*q=(-?[0-9\\.]+),(-?[0-9\\.]+))?", Pattern.MULTILINE + Pattern.CASE_INSENSITIVE + Pattern.DOTALL);

        Matcher mtch = pat.matcher(msgIn);*/


        Pattern pat;
        Matcher mtch;
        String lac, msg;
        Double lat, lng, llat, llng;
        boolean possibleMsg;

        for (String msgIn : msgs) {
            possibleMsg = true;
            lac = null;
            msg = null;
            lat = 0.0;
            lng = 0.0;
            llat = 0.0;
            llng = 0.0;

            pat = Pattern.compile("Lac:(\\p{XDigit}+) (\\p{XDigit}+)", Pattern.CASE_INSENSITIVE);
            mtch = pat.matcher(msgIn);
            if (mtch.find()) {
                if (mtch.start() == 0) {
                    possibleMsg = false;
                }
            }

            pat = Pattern.compile("lat:(-?[0-9\\.]+)\\nlong:(-?[0-9\\.]+)", Pattern.CASE_INSENSITIVE);
            mtch = pat.matcher(msgIn);
            if (mtch.find()) {
                if (mtch.start() == 0) {
                    possibleMsg = false;
                }

            }

            pat = Pattern.compile("http://.*q=(-?[0-9\\\\.]+),(-?[0-9\\\\.]+)", Pattern.CASE_INSENSITIVE);
            mtch = pat.matcher(msgIn);
            if (mtch.find()) {

            }

            if (possibleMsg) {
                pat = Pattern.compile("^([^\\n]+)");
                mtch = pat.matcher(msgIn);
                if (mtch.find()) {
                }
            }
        }
        //Pattern pat = Pattern.compile("Lac:.*http://.*q=(-?[0-9\\.]*),(-?[0-9\\.]*)", Pattern.MULTILINE + Pattern.CASE_INSENSITIVE + Pattern.DOTALL);

        // look for lac:
        // look for lat: long:
        // look for http://...x,y
        // look for message

    }

    @Test
    public void testMessageReceived() throws Exception {
        String[] msgs = new String[]{
                "sensor alarm!\n" +
                        "lat:-48.404748\n" +
                        "long:1.506225\n" +
                        "speed:0.0\n" +
                        "T:17/02/26 19:12\n" +
                        "http://maps.google.com/maps?f=q&q=148.404748,-11.506225&z=16\n" +
                        "Pwr: ON Door: OFF ACC: OFF\n",

                "battery low!\n",

                "lat:-48.404748\n" +
                        "long:1.506225\n" +
                        "speed:0.0\n" +
                        "T:17/02/26 19:12\n" +
                        "http://maps.google.com/maps?f=q&q=148.404748,-11.506225&z=16\n" +
                        "Pwr: ON Door: OFF ACC: OFF\n",

                "Lac:1234 AB01\n" +
                        "speed:0.0\n" +
                        "T:17/02/26 19:12\n" +
                        "http://maps.google.com/maps?f=q&q=148.404748,-11.506225&z=16\n" +
                        "Pwr: ON Door: OFF ACC: OFF\n"
        };

        TrackedItem ti = new TrackedItem();

        TrackerDevice td = new TK103AB();
        ((TK103AB) td).setTelephoneNumber("1234");
        ((TK103AB) td).setTelephoneCountryCode("44");

        Context context = InstrumentationRegistry.getTargetContext();

        for (String msgIn : msgs) {
            td.messageReceived(context, ti, "1234", msgIn);
        }
    }
}
