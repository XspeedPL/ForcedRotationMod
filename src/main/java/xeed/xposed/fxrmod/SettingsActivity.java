package xeed.xposed.fxrmod;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;

import java.util.ArrayList;
import java.util.Locale;

import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;
import xeed.library.ui.BaseSettings;

public final class SettingsActivity extends BaseSettings {
    @Override
    protected final void onCreatePreferences(PreferenceManager mgr) {
        addPreferencesToCategory(R.xml.settings, Category.general);
        onPreferenceChanged(mgr, mgr.getSharedPreferences(), "ovrMode");
    }

    @Override
    protected final void onPreferenceChanged(PreferenceManager mgr, SharedPreferences prefs, String key) {
        if (key.equals("ovrMode")) {
            ArrayList<CharSequence> entries = new ArrayList<>();
            ArrayList<CharSequence> values = new ArrayList<>();
            entries.add("Original");
            values.add("666");
            if (Integer.parseInt(prefs.getString(key, "0")) != 1) {
                entries.add("Landscape");
                entries.add("Portrait");
                entries.add("Reverse landscape");
                entries.add("Reverse portrait");
                entries.add("Sensor landscape");
                entries.add("Sensor portrait");
                values.add(Integer.toString(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
                values.add(Integer.toString(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
                values.add(Integer.toString(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE));
                values.add(Integer.toString(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT));
                values.add(Integer.toString(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE));
                values.add(Integer.toString(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT));
            } else {
                for (java.lang.reflect.Field f : ActivityInfo.class.getDeclaredFields())
                    if (f.getName().startsWith("SCREEN_ORIENTATION_")) {
                        f.setAccessible(true);
                        String name = f.getName().substring(19).replace('_', ' ').toLowerCase(Locale.getDefault());
                        try {
                            values.add(Integer.toString(f.getInt(null)));
                            entries.add(Character.toUpperCase(name.charAt(0)) + name.substring(1));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
            }
            CharSequence[] ent = entries.toArray(new CharSequence[0]);
            CharSequence[] val = values.toArray(new CharSequence[0]);
            ListPreference lp0 = mgr.findPreference("setRot0");
            ListPreference lp1 = mgr.findPreference("setRot1");
            lp0.setEntries(ent);
            lp0.setEntryValues(val);
            lp1.setEntries(ent);
            lp1.setEntryValues(val);
        }
    }
}
