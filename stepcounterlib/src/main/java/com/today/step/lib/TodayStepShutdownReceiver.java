package com.today.step.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.today.step.util.LogUtil;
import com.today.step.util.PreferencesHelper;

/**
 * Created by jiahongfei on 2017/9/27.
 */

public class TodayStepShutdownReceiver extends BroadcastReceiver {

    private static final String TAG = "TodayStepShutdownReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SHUTDOWN)) {
            LogUtil.e(TAG,"TodayStepShutdownReceiver");
            PreferencesHelper.setShutdown(context,true);
        }
    }

}
