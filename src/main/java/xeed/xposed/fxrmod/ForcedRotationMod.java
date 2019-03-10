package xeed.xposed.fxrmod;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.provider.Settings;

import java.util.Arrays;
import java.util.LinkedList;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import xeed.library.xposed.BaseModule;

public final class ForcedRotationMod extends BaseModule {
    private final LinkedList<String> wlist = new LinkedList<>();
    private Object acsvc = null, pwm;
    private int rot0 = 0, rot1 = 1, mode = 0;
    private String prevpkg = "";

    @Override
    public final long getVersion() {
        return BuildConfig.VERSION_CODE;
    }

    @Override
    protected final String getLogTag() {
        return "FSRM";
    }

    @Override
    public final void handleLoadPackage(XC_LoadPackage.LoadPackageParam param) throws Throwable {
        super.handleLoadPackage(param);

        if ("android".equals(param.packageName)) {
            Class<?> cWOL = tryFindClass(param.classLoader, "com.android.server.policy.WindowOrientationListener", "com.android.internal.policy.impl.WindowOrientationListener", "android.view.WindowOrientationListener");
            XposedBridge.hookAllMethods(cWOL, "getProposedRotation", handleGPR);
            XposedBridge.hookAllMethods(cWOL, "getCurrentRotation", handleGPR);

            Class<?> cPWM = tryFindClass(param.classLoader, ClassDB.PHONE_WINDOW_MANAGER);
            XposedBridge.hookAllMethods(cPWM, "rotationForOrientationLw", handleRFOL);

            Class<?> cAMS = XposedHelpers.findClass("com.android.server.am.ActivityManagerService", param.classLoader);
            if (SDK > 20) XposedBridge.hookAllConstructors(cAMS, handleAMS);
            XposedBridge.hookAllMethods(cAMS, "main", handleAMS);
        }
    }

    private final XC_MethodHook handleGPR = new XC_MethodHook() {
        @Override
        protected final void afterHookedMethod(MethodHookParam mhp) {
            if (!isReady()) return;
            boolean auto = Settings.System.getInt(mCtx.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 0;
            int rot = auto ? rot0 : rot1;
            int org = (Integer) mhp.getResult();
            int ret = org;
            if (rot != 666) {
                if (rot == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || (rot == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE && !isAnyLandscape(org)))
                    ret = XposedHelpers.getIntField(pwm, "mLandscapeRotation");
                else if (rot == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || (rot == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT && !isAnyPortrait(org)))
                    ret = XposedHelpers.getIntField(pwm, "mPortraitRotation");
                else if (rot == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                    ret = XposedHelpers.getIntField(pwm, "mSeascapeRotation");
                else if (rot == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
                    ret = XposedHelpers.getIntField(pwm, "mUpsideDownRotation");
            }
            dlog("GPR: Hooked -> autorot " + auto + ", original " + org + ", override " + rot + ", return " + ret);
            if (mode != 0 || isWhitelisted()) return;
            mhp.setResult(ret);
        }
    };

    @Override
    protected final void initPWM(Object _pwm) {
        pwm = _pwm;
    }

    private boolean isAnyPortrait(int rot) {
        return (Boolean) XposedHelpers.callMethod(pwm, "isAnyPortrait", rot);
    }

    private boolean isAnyLandscape(int rot) {
        return (Boolean) XposedHelpers.callMethod(pwm, "isLandscapeOrSeascape", rot);
    }

    private final XC_MethodHook handleAMS = new XC_MethodHook() {
        @Override
        protected final void afterHookedMethod(MethodHookParam mhp) {
            dlog("AMS init");
            acsvc = SDK > 20 ? mhp.thisObject : XposedHelpers.getStaticObjectField(mhp.thisObject.getClass(), "mSelf");
        }
    };

    private String getTopPackage() {
        Object record;
        if (SDK > 18) {
            Object src = XposedHelpers.getObjectField(acsvc, "mStackSupervisor"); // ActivityStackSupervisor
            record = XposedHelpers.callMethod(src, "topRunningActivityLocked");
        } else {
            Object src = XposedHelpers.getObjectField(acsvc, "mMainStack"); // ActivityStack
            record = XposedHelpers.callMethod(src, "topRunningActivityLocked", (Object) null);
        }
        return record == null ? null : (String) XposedHelpers.getObjectField(record, "packageName");
    }

    @Override
    protected final void reloadPrefs(Intent i) {
        rot0 = Integer.parseInt(mPrefs.getString("setRot0", Integer.toString(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)));
        rot1 = Integer.parseInt(mPrefs.getString("setRot1", Integer.toString(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)));
        mode = Integer.parseInt(mPrefs.getString("ovrMode", "0"));
        wlist.clear();
        wlist.addAll(Arrays.asList(mPrefs.getString("whiteList", "").split(" ")));
        dlog("Override mode: " + mode + ", normal override: " + rot0 + ", autorot override: " + rot1);
        dlog("Whitelist contains " + wlist.size() + " apps");
    }

    private boolean isWhitelisted() {
        String pkg = getTopPackage();
        if (pkg == null) pkg = "<null>";
        if (!pkg.equals(prevpkg)) {
            dlog("New top package -> " + pkg);
            prevpkg = pkg;
        }
        return wlist.contains(pkg);
    }

    private final XC_MethodHook handleRFOL = new XC_MethodHook() {
        @Override
        protected final void beforeHookedMethod(MethodHookParam mhp) {
            if (!isReady()) return;
            boolean auto = Settings.System.getInt(mCtx.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 0;
            int rot = auto ? rot0 : rot1;
            dlog("RFOL Before: Hooked -> autorot " + auto + ", argument " + mhp.args[0] + ", override " + rot);
            if (mode != 1 || isWhitelisted()) return;
            if (rot == 666) return;
            mhp.args[0] = rot;
        }

        @Override
        protected final void afterHookedMethod(MethodHookParam mhp) {
            if (!isReady()) return;
            boolean auto = Settings.System.getInt(mCtx.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0) == 0;
            int rot = auto ? rot0 : rot1;
            int org = (Integer) mhp.getResult();
            int ret = org;
            if (rot != 666) {
                if (rot == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE || (rot == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE && !isAnyLandscape(org)))
                    ret = XposedHelpers.getIntField(pwm, "mLandscapeRotation");
                else if (rot == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT || (rot == ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT && !isAnyPortrait(org)))
                    ret = XposedHelpers.getIntField(pwm, "mPortraitRotation");
                else if (rot == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
                    ret = XposedHelpers.getIntField(pwm, "mSeascapeRotation");
                else if (rot == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
                    ret = XposedHelpers.getIntField(pwm, "mUpsideDownRotation");
            }
            dlog("RFOL After: Hooked -> autorot " + auto + ", original " + org + ", override " + rot + ", return " + ret);
            if (mode != 2 || isWhitelisted()) return;
            mhp.setResult(ret);
        }
    };
}
