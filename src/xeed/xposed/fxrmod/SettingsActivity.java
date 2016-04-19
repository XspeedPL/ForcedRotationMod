package xeed.xposed.fxrmod;

import java.util.ArrayList;
import java.util.Locale;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import xeed.library.ui.*;

public final class SettingsActivity extends BaseSettings
{
    @Override
    protected final String getPrefsName() { return "fsrmsettings"; }
    
    @Override
    protected final void onCreatePreferences(final PreferenceManager mgr)
    {
        addPreferencesToCategory(R.xml.settings, Category.general);
        onPreferenceChanged(mgr, mgr.getSharedPreferences(), "modeRot");
    }
    
    @Override
    protected final void onPreferenceChanged(final PreferenceManager mgr, final SharedPreferences prefs, final String key)
    {
        if (key.equals("modeRot"))
        {
            final ArrayList<CharSequence> entries = new ArrayList<CharSequence>();
            final ArrayList<CharSequence> values = new ArrayList<CharSequence>();
            entries.add("Original");
            values.add("666");
            if (prefs.getString(key, "N").charAt(0) != 'F')
            {
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
            }
            else
            {
                for (final java.lang.reflect.Field f : ActivityInfo.class.getDeclaredFields())
                    if (f.getName().startsWith("SCREEN_ORIENTATION_"))
                    {
                        f.setAccessible(true);
                        final String name = f.getName().substring(19).replace('_', ' ').toLowerCase(Locale.getDefault());
                        try
                        {
                            values.add(Integer.toString(f.getInt(null)));
                            entries.add(Character.toUpperCase(name.charAt(0)) + name.substring(1));
                        }
                        catch (final Exception ex) { ex.printStackTrace(); }
                    }
            }
            final CharSequence[] ent = entries.toArray(new CharSequence[entries.size()]);
            final CharSequence[] val = values.toArray(new CharSequence[values.size()]);
            final ListPreference lp0 = (ListPreference)mgr.findPreference("setRot0");
            final ListPreference lp1 = (ListPreference)mgr.findPreference("setRot1");
            lp0.setEntries(ent);
            lp0.setEntryValues(val);
            lp1.setEntries(ent);
            lp1.setEntryValues(val);
        }
    }
}
