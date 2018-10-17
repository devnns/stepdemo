package com.today.step.util;

import android.app.Application;
import android.content.Intent;

import com.today.step.lib.StepService;


public class TodayStepManager {

    public static void init(Application application) {

        StepAlertManagerUtils.set0SeparateAlertManager(application);

        startTodayStepService(application);
    }
    public static void cancel(Application application) {

        StepAlertManagerUtils.concel0SeparateAlertManager(application);

        stopTodayStepService(application);
    }

    private static void startTodayStepService(Application application) {
        Intent intent = new Intent(application, StepService.class);
        application.startService(intent);
    }
    private static void stopTodayStepService(Application application) {
        Intent intent = new Intent(application, StepService.class);
        application.stopService(intent);
    }

}
