package me.marcsymonds.tracker;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void locationDistance() throws Exception {
        Location l1 = new Location(0, 51.405673, 0.906152, false);
        Location l2 = new Location(0, 51.405333, 0.904840, false);

        double d = l1.distanceTo(l2);

        assertTrue(String.format("Distance = %f", d), d <= 1.0);
    }

    @Test
    public void testMessageParsing() throws Exception {
        String msgIn = "sensor alarm!\\n" +
                "lat:48.404748\\n" +
                "long:1.506225\\n" +
                "speed:0.0\\n" +
                "T:17/02/26 19:12\\n" +
                "http://maps.google.com/maps?f=q&q=48.404748,1.506225&z=16\\n" +
                "Pwr: ON Door: OFF ACC: OFF\\n";

        // DOTALL means the dot (.) will match new-line characters.
        Pattern pat = Pattern.compile("^(([^\\n]*)\\n)?(lac:|lat:)?", Pattern.MULTILINE + Pattern.CASE_INSENSITIVE + Pattern.DOTALL);
        Matcher mtch = pat.matcher(msgIn);

        //Pattern pat = Pattern.compile("Lac:.*http://.*q=(-?[0-9\\.]*),(-?[0-9\\.]*)", Pattern.MULTILINE + Pattern.CASE_INSENSITIVE + Pattern.DOTALL);

        if (mtch.find()) {
            //Log.d("TEST", mtch.group());
        }
    }
}