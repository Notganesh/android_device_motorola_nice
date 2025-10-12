/*
 * Copyright (C) 2025 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.lineageos.settings.device;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.ViewConfiguration;
import android.util.Log;

import com.android.internal.os.DeviceKeyHandler;

public class KeyHandler implements DeviceKeyHandler {
    private static final String TAG = KeyHandler.class.getSimpleName();

    private static final String PROP_PREFIX = "persist.sys.keyhandler.";

    private static final int KEY_ASSISTANT = 583;
    private static final int SWITCH_WAKELOCK_DURATION = 3000;

    private final Context ctx;
    private final ContentResolver cr;
    private final PackageManager pm;

    private final AudioManager mAudioManager;
    private final PowerManager mPowerManager;
    private final WakeLock mSwitchWakeLock;
    private final Vibrator mVibrator;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mLongPressTriggered = false;
    private boolean mKeyDownTracking = false;

    // Supported action list (From stock)
    private static final int PERFORM_NO_ACTION = 0;
    private static final int PERFORM_ASSISTANT = 1;
    private static final int PERFORM_LENS = 2;
    private static final int PERFORM_LAUNCH_APP = 4;

    public KeyHandler(Context context) {
        ctx = context;
        cr = ctx.getContentResolver();
        pm = ctx.getPackageManager();

        mAudioManager = ctx.getSystemService(AudioManager.class);
        mPowerManager = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        mSwitchWakeLock = mPowerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "SwitchWakeLock");
        mVibrator = ctx.getSystemService(Vibrator.class);
    }

    @Override
    public KeyEvent handleKeyEvent(KeyEvent event) {
        int scanCode = event.getScanCode();
        if (scanCode != KEY_ASSISTANT) return event;

        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN: {
                if (event.getRepeatCount() == 0 && !mKeyDownTracking) {
                    mKeyDownTracking = true;
                    mLongPressTriggered = false;

                    int duration = SystemProperties.getInt(PROP_PREFIX + "hold_duration", 750);
                    mHandler.postDelayed(mLongPressRunnable, duration);
                }
                return null;
            }
            case KeyEvent.ACTION_UP: {
                // Remove the timer
                mHandler.removeCallbacks(mLongPressRunnable);

                if (mKeyDownTracking) {
                    if (!mLongPressTriggered) {
                        Log.d(TAG, "Performing short press action");
                        doWakeup();

                        int actionType = SystemProperties.getInt(PROP_PREFIX + "perform_type_short", PERFORM_NO_ACTION);
                        performAction(actionType);
                    }
                    mKeyDownTracking = false;
                    mLongPressTriggered = false;
                }
                return null;
            }
            default:
                return event;
        }
    }

    private final Runnable mLongPressRunnable = new Runnable() {
        @Override public void run() {
            if (!mKeyDownTracking) return;
            mLongPressTriggered = true;

            Log.d(TAG, "Performing long press action");
            doWakeup();
            doHapticFeedback();

            int actionType = SystemProperties.getInt(PROP_PREFIX + "perform_type_long", PERFORM_ASSISTANT);
            performAction(actionType);
        }
    };

    @Override
    public void onPocketStateChanged(boolean inPocket) {
        // do nothing
    }

    private void doWakeup() {
        // Wakeup display
        mSwitchWakeLock.acquire(SWITCH_WAKELOCK_DURATION);
        mPowerManager.wakeUp(SystemClock.uptimeMillis(), "switch-wakeup");
    }

    private void doHapticFeedback() {
        if (mVibrator == null || !mVibrator.hasVibrator()) return;
        mVibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    private void performAction(int actionType) {
        switch (actionType) {
            case PERFORM_ASSISTANT:
                try {
                    Intent intent = new Intent();
                    intent.setAction(Intent.ACTION_VOICE_COMMAND);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.resolveActivity(pm);
                    ctx.startActivityAsUser(intent, UserHandle.CURRENT);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to call assistant", e);
                }
                break;
            case PERFORM_LENS:
                try {
                    Intent intent = new Intent();
                    intent.setClassName("com.google.android.googlequicksearchbox", "com.google.android.apps.search.lens.LensActivity");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivityAsUser(intent, UserHandle.CURRENT);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to open google lens", e);
                }
                break;
            case PERFORM_LAUNCH_APP:
                String packageName = SystemProperties.get(PROP_PREFIX + "app_launch_package", "");
                try {
                    Intent intent = pm.getLaunchIntentForPackage(packageName);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    ctx.startActivityAsUser(intent, UserHandle.CURRENT);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to launch app", e);
                }
                break;
            default:
                Log.d(TAG, "Action is not set. Do nothing");
                break;
        }
    }
}
