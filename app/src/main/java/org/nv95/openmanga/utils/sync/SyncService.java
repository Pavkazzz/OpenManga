package org.nv95.openmanga.utils.sync;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.WorkerThread;

import org.json.JSONObject;
import org.nv95.openmanga.helpers.ScheduleHelper;
import org.nv95.openmanga.helpers.StorageHelper;
import org.nv95.openmanga.utils.FileLogger;

/**
 * Created by nv95 on 24.07.16.
 */

public class SyncService extends IntentService {

    private static final int INTERVAL_HOURS = 3;

    public SyncService() {
        super("SyncService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        synchronizeNow();
    }

    @WorkerThread
    private void synchronizeNow() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        if (!prefs.getBoolean("sync.enabled", false) || !SyncUtils.internetConnectionIsValid(this, prefs.getBoolean("sync.wifionly", false))) {
            return;
        }
        String account = prefs.getString("sync.account", null);
        if (account == null) {
            FileLogger.getInstance().report("sync: no account");
            return;
        }
        int delay = new ScheduleHelper(this).getActionIntervalHours(ScheduleHelper.ACTION_SYNC);
        if (delay >= 0 && delay <= INTERVAL_HOURS) {
            return;
        }
        boolean syncHistory = prefs.getBoolean("sync.history", true);
        boolean syncFavourites = prefs.getBoolean("sync.favourites", true);
        try {
            SyncQueue syncQueue = new SyncQueue(SyncService.this);
            StorageHelper storageHelper = new StorageHelper(this);
            JSONObject syncData = new JSONObject();
            if (syncHistory) {
                syncData.put("history", storageHelper.extractTableData("history"));
                syncData.put("history_deleted", syncQueue.getDeletedHistory());
            }
            if (syncFavourites) {
                syncData.put("favourites", storageHelper.extractTableData("favourites"));
                syncData.put("favourites_deleted", syncQueue.getDeletedFavourites());
            }
            storageHelper.close();
            syncData.put("user", account);
            syncData.put("security_key", SyncUtils.getSecurityKey(SyncService.this));

            // TODO: 01.08.16

            if (syncFavourites) {
                syncQueue.clearFavourites();
            }
            if (syncHistory) {
                syncQueue.clearHistory();
            }
            new ScheduleHelper(SyncService.this).actionDone(ScheduleHelper.ACTION_SYNC);
        } catch (Exception e) {
            FileLogger.getInstance().report(e);
        }
    }
}
