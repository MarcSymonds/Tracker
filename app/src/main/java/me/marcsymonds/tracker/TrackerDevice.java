package me.marcsymonds.tracker;

/**
 * Created by Marc on 15/03/2017.
 */

abstract class TrackerDevice implements ITrackerDevice {
    final static int ACTION_SENT_PING = 1;
    final static int ACTION_SENT_ARM = 2;
    final static int ACTION_SENT_DISARM = 3;

    static TrackerDevice newInstance(String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        Class c = Class.forName("me.marcsymonds.tracker." + className);
        return (TrackerDevice) c.newInstance();
    }
}
