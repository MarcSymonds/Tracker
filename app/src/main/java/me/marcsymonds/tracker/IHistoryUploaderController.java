package me.marcsymonds.tracker;

/**
 * Created by Marc on 11/04/2017.
 */

interface IHistoryUploaderController {
    void HistoryUploadComplete(HistoryUploader uploader, HistoryUploaderState result);
}
