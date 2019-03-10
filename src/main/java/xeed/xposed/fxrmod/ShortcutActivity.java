package xeed.xposed.fxrmod;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ShortcutActivity extends AppCompatActivity {
    @Override
    protected final void onCreate(Bundle b) {
        super.onCreate(b);
        if (getIntent().getBooleanExtra("xeed.xposed.fxrmod.Toggle", false)) {
            if (Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(this)) toggle();
            else {
                Intent in = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                in.setData(uri);
                startActivityForResult(in, 666);
            }
        } else {
            Intent in = new Intent(this, ShortcutActivity.class);
            in.putExtra("xeed.xposed.fxrmod.Toggle", true);
            Intent res = new Intent();
            res.putExtra(Intent.EXTRA_SHORTCUT_INTENT, in);
            res.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(R.string.act_toggle));
            res.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher));
            setResult(RESULT_OK, res);
            finish();
        }
    }

    @Override
    protected final void onActivityResult(int req, int res, Intent data) {
        if (req == 666) {
            if (Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(this)) toggle();
            else finish();
        }
    }

    private void toggle() {
        int i = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
        Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, i == 0 ? 1 : 0);
        Toast.makeText(this, getString(R.string.diag_rot) + " " + getString(i == 0 ? R.string.diag_en : R.string.diag_dis), Toast.LENGTH_SHORT).show();
        finish();
    }
}
