package me.marcsymonds.tracker;

import org.junit.Test;

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

}