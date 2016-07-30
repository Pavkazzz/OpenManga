package org.nv95.openmanga.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;

import org.nv95.openmanga.R;
import org.nv95.openmanga.helpers.ScheduleHelper;
import org.nv95.openmanga.utils.AppHelper;
import org.nv95.openmanga.utils.sync.AccountSelector;
import org.nv95.openmanga.utils.sync.SyncService;

/**
 * Created by nv95 on 24.07.16.
 */

public class SyncSettingsActivity extends BaseAppActivity implements Preference.OnPreferenceClickListener,
        View.OnClickListener {

    private SettingsFragment mSettingsFragment;
    private SwitchCompat mSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_syncsttings);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        enableHomeAsUp();
        mSwitch = (SwitchCompat) findViewById(R.id.switch_toggle);
        mSwitch.setChecked(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getBoolean("sync.enabled", false));
        mSwitch.setOnClickListener(this);

        mSettingsFragment = new SettingsFragment();
        if (mSwitch.isChecked()) {
            getFragmentManager().beginTransaction()
                    .add(R.id.content, mSettingsFragment)
                    .commit();
            findViewById(R.id.textView).setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onPreferenceClick(final Preference preference) {
        switch (preference.getKey()) {
            case "sync.account":
                if (checkPermission(Manifest.permission.GET_ACCOUNTS)) {
                    new AccountSelector(this)
                            .setOnAccountSelectListener(new AccountSelector.OnAccountSelectListener() {
                                @Override
                                public void onAccountSelected(String email) {
                                    preference.setSummary(email);
                                    preference.getEditor().putString("sync.account", email).apply();
                                }
                            }).show();
                }
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onClick(View v) {
        boolean checked = ((SwitchCompat) v).isChecked();
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit()
                .putBoolean("sync.enabled", checked)
                .apply();
        if (checked) {
            getFragmentManager().beginTransaction()
                    .add(R.id.content, mSettingsFragment)
                    .commit();
            findViewById(R.id.textView).setVisibility(View.GONE);
        } else {
            getFragmentManager().beginTransaction()
                    .remove(mSettingsFragment)
                    .commit();
            findViewById(R.id.textView).setVisibility(View.VISIBLE);
        }
    }

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Activity activity = getActivity();
            addPreferencesFromResource(R.xml.pref_sync);

            Preference preference = findPreference("sync.account");
            preference.setSummary(preference.getSharedPreferences().getString(preference.getKey(), getString(R.string.nothing_selected)));
            preference.setOnPreferenceClickListener((Preference.OnPreferenceClickListener) activity);

            preference = findPreference("sync.status");
            long lastSync = new ScheduleHelper(activity).getActionRawTime(ScheduleHelper.ACTION_SYNC);
            if (lastSync < 0) {
                preference.setSummary(R.string.never);
            } else {
                preference.setSummary(AppHelper.getReadableDateTimeRelative(lastSync));
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (mSwitch.isChecked()) {
            startService(new Intent(this, SyncService.class));
        }
        super.onDestroy();
    }
}
