package me.marcsymonds.tracker;

interface ITrackedItemActions {
    void trackedItemButtonClick(TrackedItem trackedItem, boolean longClick);
    void trackedItemLocationUpdate(TrackedItem trackedItem, Location location);
}
