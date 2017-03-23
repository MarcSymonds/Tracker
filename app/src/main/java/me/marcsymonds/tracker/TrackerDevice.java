package me.marcsymonds.tracker;

/**
 * Created by Marc on 15/03/2017.
 */

abstract class TrackerDevice implements ITrackerDevice {
    static TrackerDevice newInstance(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class c = Class.forName("me.marcsymonds.tracker." + className);
        return (TrackerDevice) c.newInstance();
    }
}
