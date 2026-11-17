package com.example.assignmenttracker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * SEC-0-1 (device half): checks the merged manifest of the installed APK, so a library
 * or a later edit can't put Login or SignUp back on the launch path unnoticed.
 */
@RunWith(AndroidJUnit4.class)
public class LaunchPathSecurityTest {

    private final Context ctx = ApplicationProvider.getApplicationContext();
    private final PackageManager pm = ctx.getPackageManager();

    @Test
    public void onlyLauncherIsMainActivity() {
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(ctx.getPackageName());
        List<ResolveInfo> found = pm.queryIntentActivities(launcher, 0);
        assertEquals(1, found.size());
        assertEquals(MainActivity.class.getName(), found.get(0).activityInfo.name);
    }

    @Test
    public void loginAndSignUpAreNotExported() throws Exception {
        for (Class<?> c : new Class<?>[]{LoginActivity.class, SignUpActivity.class}) {
            ActivityInfo info = pm.getActivityInfo(new ComponentName(ctx, c), 0);
            assertFalse(c.getSimpleName() + " must not be exported", info.exported);
        }
    }

    @Test
    public void mainIsTheOnlyExportedActivity() throws Exception {
        PackageInfo pkg = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
        for (ActivityInfo a : pkg.activities) {
            if (a.exported) {
                assertEquals("unexpected exported activity", MainActivity.class.getName(), a.name);
            }
        }
    }
}
