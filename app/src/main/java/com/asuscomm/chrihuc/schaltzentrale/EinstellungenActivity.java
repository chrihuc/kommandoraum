package com.asuscomm.chrihuc.schaltzentrale;

/**
 * Created by christoph on 29.01.17.
 */

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class EinstellungenActivity extends PreferenceActivity
        implements Preference.OnPreferenceChangeListener {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        //Toast.makeText(this, "Einstellungen-Activity gestartet.", Toast.LENGTH_SHORT).show();
        //Toast.makeText(this, "Zurück mit Back-Button.", Toast.LENGTH_SHORT).show();
        //Preference aktienlistePref = findPreference(getString(R.string.preference_name_key));
        //aktienlistePref.setOnPreferenceChangeListener(this);
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    //public boolean onPreferenceChange(Preference preference, Object value) {
    //    return false;
    //}
    public boolean onPreferenceChange(Preference preference, Object value) {

        preference.setSummary(value.toString());

        return true;
    }
}