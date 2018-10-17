package com.today.step.util;

import android.content.Context;
import android.os.PowerManager;

import com.today.step.lib.StepService;

import java.util.Calendar;

/**
 * @author :  jiahongfei
 * @email : jiahongfeinew@163.com
 * @date : 2018/2/12
 * @desc :
 */

public class WakeLockUtils {

    private static PowerManager.WakeLock mWakeLock;

    public synchronized static PowerManager.WakeLock getLock(Context context) {
        if (mWakeLock != null) {
            if (mWakeLock.isHeld())
                mWakeLock.release();
            mWakeLock = null;
        }

        if (mWakeLock == null) {
            PowerManager mgr = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);
            mWakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    StepService.class.getName());
            mWakeLock.setReferenceCounted(true);
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(System.currentTimeMillis());
            mWakeLock.acquire();
        }
        return (mWakeLock);
    }

}
