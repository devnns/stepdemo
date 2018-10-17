package com.devnn.step.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.util.regex.Pattern;

/**
 * 进程保活(高仿keep):锁定进程和设置进程自启动
 * 需要经常更新,因为手机厂家的权限设置界面的路径可能会修改。
 */
public class DeviceUtils {
    private static final String RegEx_HUAWEI = "huawei|honor|mate|nova";
    private static final String RegEx_XIAOMI = "xiaomi";
    private static final String RegEx_MEIZU = "meizu";
    private static final String RegEx_SAMSUNG = "samsung";
    private static final String RegEx_VIVO = "vivo";
    private static final String RegEx_OPPO = "oppo";

    public static void openSystemProcessProtectPage(Context context) {
        String brand = Build.BRAND;
        Pattern pattern_xiaomi = Pattern.compile(RegEx_XIAOMI, Pattern.CASE_INSENSITIVE);
        Pattern pattern_huawei = Pattern.compile(RegEx_HUAWEI, Pattern.CASE_INSENSITIVE);
        Pattern pattern_meizu = Pattern.compile(RegEx_MEIZU, Pattern.CASE_INSENSITIVE);
        Pattern pattern_sumsung = Pattern.compile(RegEx_SAMSUNG, Pattern.CASE_INSENSITIVE);
        Pattern pattern_vivo = Pattern.compile(RegEx_VIVO, Pattern.CASE_INSENSITIVE);
        Pattern pattern_oppo = Pattern.compile(RegEx_OPPO, Pattern.CASE_INSENSITIVE);
        Intent intent = new Intent();
        ComponentName componentName = null;
        if (pattern_xiaomi.matcher(brand).find()) {
            //小米
            componentName = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
        } else if (pattern_huawei.matcher(brand).find()) {
            //华为
//            componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");
            componentName = new ComponentName("com.android.settings", "com.android.settings/.HWSettings");
        } else if (pattern_meizu.matcher(brand).find()) {
            //魅族
            componentName = new ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity");
        } else if (pattern_sumsung.matcher(brand).find()) {
            //三星
        } else if (pattern_oppo.matcher(brand).find()) {
            //oppo
        } else if (pattern_vivo.matcher(brand).find()) {
            //vivo
        }
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK&Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.setComponent(componentName);
        try {
            context.startActivity(intent);
        } catch (Exception e) {//抛出异常就直接打开设置页面
            Log.e(DeviceUtils.class.getSimpleName(),e.getMessage());
            System.out.println(e.getMessage());
            intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
            context.startActivity(intent);
        }
    }

    public static void openSystemBatteryPage(Context context) {
        String brand = Build.BRAND;
        Pattern pattern_xiaomi = Pattern.compile(RegEx_XIAOMI, Pattern.CASE_INSENSITIVE);
        Pattern pattern_huawei = Pattern.compile(RegEx_HUAWEI, Pattern.CASE_INSENSITIVE);
        Pattern pattern_meizu = Pattern.compile(RegEx_MEIZU, Pattern.CASE_INSENSITIVE);
        Pattern pattern_sumsung = Pattern.compile(RegEx_SAMSUNG, Pattern.CASE_INSENSITIVE);
        Pattern pattern_vivo = Pattern.compile(RegEx_VIVO, Pattern.CASE_INSENSITIVE);
        Pattern pattern_oppo = Pattern.compile(RegEx_OPPO, Pattern.CASE_INSENSITIVE);
        Intent intent = new Intent();
        ComponentName componentName = null;
        if (pattern_xiaomi.matcher(brand).find()) {
            //小米
            componentName = new ComponentName("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsContainerManagementActivity");
        } else if (pattern_huawei.matcher(brand).find()) {
            //华为
            componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity");
        } else if (pattern_meizu.matcher(brand).find()) {
            //魅族
            componentName = new ComponentName("com.meizu.safe", "com.meizu.safe.SecurityMainActivity");
        } else if (pattern_sumsung.matcher(brand).find()) {
            //三星
        } else if (pattern_oppo.matcher(brand).find()) {
            //oppo
        } else if (pattern_vivo.matcher(brand).find()) {
            //vivo
        }
        intent.setComponent(componentName);
        try {
            context.startActivity(intent);
        } catch (Exception e) {//抛出异常就直接打开设置页面
            intent = new Intent(Settings.ACTION_SETTINGS);
            context.startActivity(intent);
        }
    }


}
