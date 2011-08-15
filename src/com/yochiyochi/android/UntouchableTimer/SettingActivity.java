package com.yochiyochi.android.UntouchableTimer;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;

public class SettingActivity extends PreferenceActivity {
    static final String TAG = "SettingActivity";
    {
        Log.d( TAG, "@@@---start---@@@" );
    }

    @Override
    public void onCreate( Bundle savedInstanceState ) {

        super.onCreate( savedInstanceState );

        // XML で Preference を設定
        addPreferencesFromResource( R.xml.preference );

        // ListPreference の取得
        // ListPreference listPreferrence = (ListPreference) findPreference(getString(R.string.pref_key_sound));

    }
}
