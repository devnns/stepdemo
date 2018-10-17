package com.today.step.lib;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.today.step.lib.entity.StepData;
import com.today.step.util.LogUtil;
import com.today.step.util.SportUtil;
import com.today.step.util.StepRealmManager;
import com.today.step.util.WakeLockUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import io.realm.Realm;

public class StepService extends Service implements Handler.Callback {

    private static final String TAG = StepService.class.getSimpleName();
    /**
     * 数据库中保存多少天的运动数据
     */
    private static final int DB_LIMIT = 2;

    //保存数据库频率
    private static final int DB_SAVE_COUNTER = 50;

    //传感器的采样周期，这里使用SensorManager.SENSOR_DELAY_FASTEST，如果使用SENSOR_DELAY_UI会导致部分手机后台清理内存之后传感器不记步
    private static final int SAMPLING_PERIOD_US = SensorManager.SENSOR_DELAY_FASTEST;

    private static final int HANDLER_WHAT_SAVE_STEP = 0;
    //如果走路停止，10秒钟后保存数据库
    private static final int LAST_SAVE_STEP_DURATION = 10 * 1000;

    private static final int BROADCAST_REQUEST_CODE = 100;

    public static final String INTENT_NAME_0_SEPARATE = "intent_name_0_separate";
    public static final String INTENT_NAME_BOOT = "intent_name_boot";
//    public static final String INTENT_JOB_SCHEDULER = "intent_job_scheduler";

    public static int CURRENT_STEP = 0;

    private SensorManager sensorManager;
    //    private TodayStepDcretor stepDetector;
    private StepAccelerometerEventListener stepAccelerometerEventListener;
    private StepCounterEventListener stepCounterEventListener;

    private NotificationManager nm;
    private Notification notification;
    private NotificationCompat.Builder builder;

    private boolean mSeparate = false;

    private boolean mBoot = false;

    private int mDbSaveCount = 0;

    private final Handler mHandler = new Handler(this);

    private int sensorType = Sensor.TYPE_ACCELEROMETER;

    private MyStepCounterListener myStepCounterListener = new MyStepCounterListener();

    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd",Locale.CHINA);

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case HANDLER_WHAT_SAVE_STEP: {
                LogUtil.e(TAG, "HANDLER_WHAT_SAVE_STEP");
                mDbSaveCount = 0;
                saveDb(CURRENT_STEP);
                break;
            }
            default:
                break;
        }
        return false;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "----------------------------------");
        LogUtil.e(TAG, "onCreate:" + CURRENT_STEP);
        super.onCreate();

        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);

        initForgroundNotification(CURRENT_STEP);

        clearOldStepData();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.e(TAG, "onStartCommand:" + CURRENT_STEP);
        LogUtil.e(TAG, "onStartCommand ACTION:" + intent.getAction());
        if (null != intent) {
            mSeparate = intent.getBooleanExtra(INTENT_NAME_0_SEPARATE, false);
            mBoot = intent.getBooleanExtra(INTENT_NAME_BOOT, false);
        }

        mDbSaveCount = 0;

        updateNotification(CURRENT_STEP);

        //注册传感器
        registerSensor();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        LogUtil.e(TAG, "onBind:" + CURRENT_STEP);
        return mIBinder.asBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtil.e(TAG, "onUnbind:" + CURRENT_STEP);
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        LogUtil.e(TAG, "onDestroy:" + CURRENT_STEP);
        unRegisterSensor();
        super.onDestroy();
    }

    private void registerSensor() {
//        getLock(this);
        //android4.4以后如果有stepcounter可以使用计步传感器
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && getStepCounterEventListener()) {
            sensorType = Sensor.TYPE_STEP_COUNTER;
            registerStepCounterSensor();
        } else {
            sensorType = Sensor.TYPE_ACCELEROMETER;
            registerAcceleratorSensor();
        }
    }


    private void registerStepCounterSensor() {
        LogUtil.e(TAG, "addStepCounterListener");
        if (null != stepCounterEventListener) {
            LogUtil.e(TAG, "已经注册TYPE_STEP_COUNTER");
            WakeLockUtils.getLock(this);
            CURRENT_STEP = stepCounterEventListener.getCurrentStep();
            updateNotification(CURRENT_STEP);
            return;
        }
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (null == countSensor) {
            return;
        }
        stepCounterEventListener = new StepCounterEventListener(getApplicationContext(), myStepCounterListener, mSeparate, mBoot);
        LogUtil.e(TAG, "countSensor");
        sensorManager.registerListener(stepCounterEventListener, countSensor, SAMPLING_PERIOD_US);
    }


    private void registerAcceleratorSensor() {
        LogUtil.e(TAG, "registerAcceleratorListener");
        if (null != stepAccelerometerEventListener) {
            WakeLockUtils.getLock(this);
            LogUtil.e(TAG, "已经注册TYPE_ACCELEROMETER");
            CURRENT_STEP = stepAccelerometerEventListener.getCurrentStep();
            updateNotification(CURRENT_STEP);
            return;
        }
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (null == sensor) {
            return;
        }
        stepAccelerometerEventListener = new StepAccelerometerEventListener(this, myStepCounterListener);
//        Log.e(TAG, "TodayStepDcretor");
        // 获得传感器的类型，这里获得的类型是加速度传感器
        // 此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        sensorManager.registerListener(stepAccelerometerEventListener, sensor, SAMPLING_PERIOD_US);
    }

    private void unRegisterSensor() {
        if (stepAccelerometerEventListener != null) {
            stepAccelerometerEventListener.unRegisterDateChangeBroadcastReceiver();
            sensorManager.unregisterListener(stepAccelerometerEventListener);
        }
        if (stepCounterEventListener != null) {
            stepCounterEventListener.unRegisterDateChangeBroadcastReceiver();
            sensorManager.unregisterListener(stepCounterEventListener);
        }

    }

    private class MyStepCounterListener implements com.today.step.lib.MyStepCounterListener {
        @Override
        public void onChangeStepCounter(int step) {
            CURRENT_STEP = step;
            updateNotification(CURRENT_STEP);
            saveStep(CURRENT_STEP);
        }

        @Override
        public void onStepCounterClean() {
            CURRENT_STEP = 0;
            updateNotification(CURRENT_STEP);
            clearOldStepData();
        }

    }

    private void saveStep(int currentStep) {
        mHandler.removeMessages(HANDLER_WHAT_SAVE_STEP);
        mHandler.sendEmptyMessageDelayed(HANDLER_WHAT_SAVE_STEP, LAST_SAVE_STEP_DURATION);
        if (DB_SAVE_COUNTER > mDbSaveCount) {
            mDbSaveCount++;
            return;
        }
        mDbSaveCount = 0;
        saveDb(currentStep);
    }

    /**
     * @param currentStep
     */
    private void saveDb(int currentStep) {
        String date = dateFormat.format(System.currentTimeMillis());
        Realm realm = StepRealmManager.getInstance().getRealm(getApplicationContext());
        StepData stepData = realm.where(StepData.class).equalTo("userId", 0).equalTo("date", date).findFirst();
        if (stepData != null) {
            //更新
            LogUtil.e(TAG, "更新本地今日频数:" + currentStep);
            realm.beginTransaction();
            stepData.setStep(currentStep);
            realm.commitTransaction();
        } else {
            //添加
            LogUtil.e(TAG, "添加本地今日步数:" + currentStep);
            realm.beginTransaction();
            StepData newStepData = realm.createObject(StepData.class);
            newStepData.setDate(date);
            newStepData.setStep(currentStep);
            newStepData.setLastUpdateTime(System.currentTimeMillis());
            realm.commitTransaction();
        }

    }

    /**
     * 清除&{DA_LIMIT}天之前的数据
     */
    private void clearOldStepData() {
        LogUtil.e(TAG, "cleanDb");
        mDbSaveCount = 0;
        Realm realm = StepRealmManager.getInstance().getRealm(getApplicationContext());
        realm.beginTransaction();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, -DB_LIMIT * 24);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        realm.where(StepData.class).lessThan("lastUpdateTime", calendar.getTimeInMillis()).findAll().deleteAllFromRealm();
        realm.commitTransaction();

    }


    private void initForgroundNotification(int currentStep) {
        builder = new NotificationCompat.Builder(this, "com.devnn.step");
        builder.setPriority(Notification.PRIORITY_MIN);

//        String receiverName = getReceiver(getApplicationContext());
        PendingIntent contentIntent = PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
//        if (!TextUtils.isEmpty(receiverName)) {
//            try {
//                contentIntent = PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE, new Intent(this, Class.forName(receiverName)), PendingIntent.FLAG_UPDATE_CURRENT);
//            } catch (Exception e) {
//                e.printStackTrace();
//                contentIntent = PendingIntent.getBroadcast(this, BROADCAST_REQUEST_CODE, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
//            }
//        }
        builder.setContentIntent(contentIntent);
        int smallIcon = getResources().getIdentifier("icon_step_small", "mipmap", getPackageName());
        if (0 != smallIcon) {
            LogUtil.e(TAG, "smallIcon");
            builder.setSmallIcon(smallIcon);
        } else {
            builder.setSmallIcon(R.mipmap.ic_notification_default);// 设置通知小ICON
        }
        int largeIcon = getResources().getIdentifier("icon_step_large", "mipmap", getPackageName());
        if (0 != largeIcon) {
            LogUtil.e(TAG, "largeIcon");
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), largeIcon));
        } else {
            builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_notification_default));

        }
        builder.setTicker(getString(R.string.app_name));
        builder.setContentTitle(getString(R.string.title_notification_bar, String.valueOf(currentStep)));
        String km = SportUtil.getDistanceByStep(currentStep);
        String calorie = SportUtil.getCalorieByStep(currentStep);
        builder.setContentText(calorie + " 千卡  " + km + " 公里");

        //设置不可清除
        builder.setOngoing(true);
        notification = builder.build();
        //将Service设置前台，这里的id和notify的id一定要相同否则会出现后台清理内存Service被杀死通知还存在的bug
        startForeground(R.string.app_name, notification);
        nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.notify(R.string.app_name, notification);

    }

    /**
     * 更新通知
     */
    private void updateNotification(int stepCount) {
        if (null == builder || null == nm) {
            return;
        }
        builder.setContentTitle(getString(R.string.title_notification_bar, String.valueOf(stepCount)));
        String km = SportUtil.getDistanceByStep(stepCount);
        String calorie = SportUtil.getCalorieByStep(stepCount);
        builder.setContentText(calorie + " 千卡  " + km + " 公里");
        notification = builder.build();
        nm.notify(R.string.app_name, notification);
    }

    private boolean getStepCounterEventListener() {
        return getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER);
    }


    private final ISportStepInterface.Stub mIBinder = new ISportStepInterface.Stub() {


        @Override
        public int getCurrentSteps() {
            return CURRENT_STEP;
        }

        @Override
        public int getStepsByDate(String date) {
            Realm realm = StepRealmManager.getInstance().getRealm(getApplicationContext());
            realm.beginTransaction();
            StepData stepData = realm.where(StepData.class).equalTo("date", date).equalTo("userId", 0).findFirst();
            realm.commitTransaction();
            if (stepData != null) {
                return stepData.getStep();
            } else {
                return 0;
            }

        }

        @Override
        public int getSensorType() {
            return sensorType;
        }
    };
}
