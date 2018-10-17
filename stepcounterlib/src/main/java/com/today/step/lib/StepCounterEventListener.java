package com.today.step.lib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.SystemClock;

import com.today.step.util.LogUtil;
import com.today.step.util.PreferencesHelper;
import com.today.step.util.WakeLockUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * android4.4  Sensor.TYPE_STEP_COUNTER
 * 计步传感器计算当天步数，不需要后台Service
 * Created by jiahongfei on 2017/6/30.
 */

class StepCounterEventListener implements SensorEventListener {

    private static final String TAG = "StepCounterEventListener";

    private int sOffsetStep = 0;
    private int mCurrentStep = 0;//今日行走步数
    private String mTodayDate;
    private boolean mCleanStep = true;
    private boolean mShutdown = false;
    /**
     * 用来标识对象第一次创建，
     */
    private boolean mCounterStepReset = true;

    private Context mContext;
    private MyStepCounterListener mMyStepCounterListener;

    private boolean mSeparate = false;
    private boolean mBoot = false;

    private  BroadcastReceiver mBatInfoReceiver;

    public StepCounterEventListener(Context context, MyStepCounterListener myStepCounterListener, boolean separate, boolean boot) {
        this.mContext = context;
        this.mSeparate = separate;//是否是新的一天
        this.mBoot = boot;//是否重启过
        this.mMyStepCounterListener = myStepCounterListener;

        WakeLockUtils.getLock(mContext);

        mCurrentStep = (int) PreferencesHelper.getCurrentStep(mContext);
        mCleanStep = PreferencesHelper.getCleanStep(mContext);//默认true
        mTodayDate = PreferencesHelper.getStoredTodayDate(mContext);
        sOffsetStep = (int) PreferencesHelper.getStepOffset(mContext);
        mShutdown = PreferencesHelper.getShutdown(mContext);//默认false
        LogUtil.e(TAG, "mShutdown : " + mShutdown);
        //开机启动监听到，一定是关机开机了
        if (mBoot || shutdownBySystemRunningTime()) {
            mShutdown = true;
            PreferencesHelper.setShutdown(mContext, mShutdown);
            LogUtil.e(TAG, "开机启动监听到");
        }

        checkIfDateChanged();

        registerDateChangeBroadcastReceiver();

        updateStepCounterListener();

    }
    /**
     * 注册系统广播接收器，定时检查是否是新的一天。
     */
    private void registerDateChangeBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
         mBatInfoReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                if (Intent.ACTION_TIME_TICK.equals(intent.getAction())
                        || Intent.ACTION_TIME_CHANGED.equals(intent.getAction())) {

                    LogUtil.e(TAG, "ACTION_TIME_TICK");
                    //service存活做0点分隔
                    checkIfDateChanged();

                }
            }
        };
        mContext.registerReceiver(mBatInfoReceiver, filter);

    }
    public void unRegisterDateChangeBroadcastReceiver(){
        if(mBatInfoReceiver!=null) {
            mContext.unregisterReceiver(mBatInfoReceiver);
        }
    }
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {

            int counterStep = (int) event.values[0];

            if (mCleanStep) {
                //TODO:只有传感器回调才会记录当前传感器步数，然后对当天步数进行清零，所以步数会少，少的步数等于传感器启动需要的步数，假如传感器需要10步进行启动，那么就少10步
                cleanStep(counterStep);
            } else {
                //处理关机启动
                if (mShutdown || shutdownByCounterStep(counterStep)) {
                    LogUtil.e(TAG, "onSensorChanged shutdown");
                    shutdown(counterStep);
                }
            }
            mCurrentStep = counterStep - sOffsetStep;

            if (mCurrentStep < 0) {
                //容错处理，无论任何原因步数不能小于0，如果小于0，直接清零
                LogUtil.e(TAG, "容错处理，无论任何原因步数不能小于0，如果小于0，直接清零");
                cleanStep(counterStep);
            }

            PreferencesHelper.setCurrentStep(mContext, mCurrentStep);
            PreferencesHelper.setElapsedRealtime(mContext, SystemClock.elapsedRealtime());
            PreferencesHelper.setLastSensorStep(mContext, counterStep);

            LogUtil.e(TAG, "counterStep : " + counterStep + " --- " + "sOffsetStep : " + sOffsetStep + " --- " + "mCurrentStep : " + mCurrentStep);

            updateStepCounterListener();
        }
    }

    private void cleanStep(int counterStep) {
        //清除步数，步数归零，优先级最高
        mCurrentStep = 0;
        sOffsetStep = counterStep;
        PreferencesHelper.setStepOffset(mContext, sOffsetStep);

        mCleanStep = false;
        PreferencesHelper.setCleanStep(mContext, mCleanStep);

        LogUtil.e(TAG, "mCleanStep : " + "清除步数，步数归零");
    }

    private void shutdown(int counterStep) {
        int tmpCurrStep = (int) PreferencesHelper.getCurrentStep(mContext);
        //重新设置offset
        sOffsetStep = counterStep - tmpCurrStep;
        PreferencesHelper.setStepOffset(mContext, sOffsetStep);

        mShutdown = false;
        PreferencesHelper.setShutdown(mContext, mShutdown);
    }

    private boolean shutdownByCounterStep(int counterStep) {
        if (mCounterStepReset) {
            //只判断一次
            if (counterStep < PreferencesHelper.getLastSensorStep(mContext)) {
                //当前传感器步数小于上次传感器步数肯定是重新启动了，只是用来增加精度不是绝对的
                LogUtil.e(TAG, "当前传感器步数小于上次传感器步数肯定是重新启动了，只是用来增加精度不是绝对的");
                return true;
            }
            mCounterStepReset = false;
        }
        return false;
    }

    private boolean shutdownBySystemRunningTime() {
        if (PreferencesHelper.getElapsedRealtime(mContext) > SystemClock.elapsedRealtime()) {
            //上次运行的时间大于当前运行时间判断为重启，只是增加精度，极端情况下连续重启，会判断不出来
            LogUtil.e(TAG, "上次运行的时间大于当前运行时间判断为重启，只是增加精度，极端情况下连续重启，会判断不出来");
            return true;
        }
        return false;
    }
    /**
     * 检查手机时间是否改变，或是否是新的一天
     */
    private synchronized void checkIfDateChanged() {
        //时间改变了清零，或者0点分隔回调
        if (!getTodayDate().equals(mTodayDate) || mSeparate) {

            WakeLockUtils.getLock(mContext);

            mCleanStep = true;
            PreferencesHelper.setCleanStep(mContext, mCleanStep);

            mTodayDate = getTodayDate();
            PreferencesHelper.setStoredTodayDate(mContext, mTodayDate);

            mShutdown = false;
            PreferencesHelper.setShutdown(mContext, mShutdown);

            mBoot = false;

            mSeparate = false;

            mCurrentStep = 0;
            PreferencesHelper.setCurrentStep(mContext, mCurrentStep);

            if (null != mMyStepCounterListener) {
                mMyStepCounterListener.onStepCounterClean();
            }
        }
    }

    private String getTodayDate() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    private void updateStepCounterListener() {

        //每次回调都判断一下是否跨天
        checkIfDateChanged();

        if (null != mMyStepCounterListener) {
            mMyStepCounterListener.onChangeStepCounter(mCurrentStep);
        }
    }

    public int getCurrentStep() {
        mCurrentStep = (int) PreferencesHelper.getCurrentStep(mContext);
        return mCurrentStep;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
