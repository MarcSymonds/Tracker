package me.marcsymonds.tracker;

/**
 * Created by Marc on 04/03/2017.
 */

public interface ITrackedItemEntryActions {
    void onTrackedItemEnabledStateChanged(TrackedItem trackedItem, boolean state);
    void onTrackedItemAppearenceChanged(TrackedItem trackedItem);
}
