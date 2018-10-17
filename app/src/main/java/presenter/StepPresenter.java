package presenter;

import android.Manifest;
import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.util.Log;

import com.amap.api.location.AMapLocation;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.model.LatLng;
import com.devnn.step.utils.DeviceUtils;
import com.devnn.step.view.StepView;
import com.today.step.activity.AMapActivity;
import com.today.step.lib.ISportStepInterface;
import com.today.step.lib.StepService;
import com.today.step.service.LocationService;
import com.today.step.util.DateUtils;
import com.today.step.util.TodayStepManager;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * Create by nandy on 2018/6/8
 */
public class StepPresenter extends BasePresenter implements Handler.Callback, EasyPermissions.PermissionCallbacks {
    private String TAG = this.getClass().getSimpleName();
    private StepView stepView;
    private Activity activity;
    private boolean hasStarted = false;
    private SensorManager sensorManager;
    private Sensor mStepCounterSensor;//历史累计步数感应器(系统级别的累计的行走步数)
    private Sensor mStepDetectorSensor;//单次行走感应器(行走就会回调，但是没有步数，需要自己计算)
    private final int REFRESH_STEP_WHAT = 100;
    private final int REFRESH_STEP_INTERVAL = 1000;//1秒更新一次步数
    private ISportStepInterface iSportStepInterface;
    private Handler mDelayHandler = new Handler(this);
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private ServiceConnection serviceConnection = new MyServiceConnection();
    private int runSeconds;
    private ArrayList<LatLng> movementPoints = new ArrayList<>();
    private LatLng lastPreciseLatLng;//上一次正确定位的经纬度,用来计算再次定位的距离

    public StepPresenter(StepView stepView) {
        this.stepView = stepView;
    }

    @Override
    public void onCreate(LifecycleOwner owner) {
        this.activity = this.stepView.getActivity();
        EventBus.getDefault().register(this);//用来获取高德定位数据
        sensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mStepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        mStepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        listAllSensors();
        if (isStepCounter()) {
            stepView.setSensorType(Sensor.TYPE_STEP_COUNTER);
        } else {
            stepView.setSensorType(Sensor.TYPE_ACCELEROMETER);
        }
    }

    /**
     * 列出所有支持的传感器
     */
    private void listAllSensors() {
        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensorList) {
            Log.i("sensor", sensor.getName());
        }
        Log.i("TYPE_STEP_COUNTER", "" + (mStepCounterSensor != null));
        Log.i("TYPE_STEP_DETECTOR", "" + (mStepDetectorSensor != null));
    }

    /**
     * 开关计步服务
     */
    public void switchStep() {
        if (!hasStarted) {
            hasStarted = true;
            stepView.setStepButtonText("停止计步");
            startStep();
        } else {
            hasStarted = false;
            stepView.setStepButtonText("开始计步");
            stopStep();
        }
    }

    /**
     * 开始计步,即开启计步service和定位service
     */
    private void startStep() {
        TodayStepManager.init(activity.getApplication());
        activity.bindService(new Intent(activity, StepService.class), serviceConnection, Context.BIND_AUTO_CREATE);
        startLocationService();
    }

    /**
     * 停止计步,即关闭计步service和定位service
     */
    private void stopStep() {
        TodayStepManager.cancel(activity.getApplication());
        activity.unbindService(serviceConnection);
        stepView.setStepStatus("");
        runSeconds = 0;
        mDelayHandler.removeMessages(REFRESH_STEP_WHAT);
        stopLocationService();
    }

    private void startLocationService() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE};
        if (!EasyPermissions.hasPermissions(activity,permissions)) {
            EasyPermissions.requestPermissions(activity, "需要定位和存储权限用来记录运动轨迹", 1001, permissions);
        } else {
            Intent intent = new Intent(activity, LocationService.class);
            activity.startService(intent);
        }
    }

    private void stopLocationService() {
        Intent intent = new Intent(activity, LocationService.class);
        activity.stopService(intent);
    }

    public void processProtectSetting() {
        DeviceUtils.openSystemProcessProtectPage(activity);
    }

    public void preventSleepSetting() {
        DeviceUtils.openSystemBatteryPage(activity);
    }


    public void showMoveTrace() {
        Intent intent = new Intent(activity, AMapActivity.class);
        intent.putParcelableArrayListExtra("trace", movementPoints);
        activity.startActivity(intent);
    }

    /**
     * 是否带有计步协处理器
     * @return
     */
    private boolean isStepCounter() {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if (requestCode == 1001) {
            Intent intent = new Intent(activity, LocationService.class);
            activity.startService(intent);
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        if (requestCode == 1001) {
            stepView.setCurLocation("已拒绝位置相关权限");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (msg.what != REFRESH_STEP_WHAT || iSportStepInterface == null) {
            return false;
        }
        runSeconds += REFRESH_STEP_INTERVAL / 1000;
        stepView.setStepRunningPeriod(DateUtils.getRunTime(runSeconds));
        try {
            int step = iSportStepInterface.getCurrentSteps();
            Calendar calendar = Calendar.getInstance();
//                            String todayDate = dateFormat.format(calendar.getTime());
            calendar.set(Calendar.HOUR_OF_DAY, -24);
            String yesterdayDate = dateFormat.format(calendar.getTime());
            calendar.set(Calendar.HOUR_OF_DAY, -24);
            String beforeYesterdayDate = dateFormat.format(calendar.getTime());
//                            Log.i(TAG, "\ntodayDate:" + todayDate);
//                            Log.i(TAG, "yesterdayDate:" + yesterdayDate);
//                            Log.i(TAG, "beforeYesterdayDate:" + beforeYesterdayDate);
            int stepYesterday = iSportStepInterface.getStepsByDate(yesterdayDate);
            int stepBeforeYesterday = iSportStepInterface.getStepsByDate(beforeYesterdayDate);
            stepView.setTodayStep(step, getCalorieByStep(step), getDistanceByStep(step));
            stepView.setYesterdayStep(stepYesterday, getCalorieByStep(stepYesterday), getDistanceByStep(stepYesterday));
            stepView.setBeforeYesterdayStep(stepBeforeYesterday, getCalorieByStep(stepBeforeYesterday), getCalorieByStep(stepBeforeYesterday));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        mDelayHandler.sendEmptyMessageDelayed(REFRESH_STEP_WHAT, REFRESH_STEP_INTERVAL);
        return false;
    }


    private class MyServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i(TAG, "onServiceConnected");
            iSportStepInterface = ISportStepInterface.Stub.asInterface(service);
            stepView.setStepStatus("正在计步...");
            mDelayHandler.sendEmptyMessageDelayed(REFRESH_STEP_WHAT, REFRESH_STEP_INTERVAL);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e(TAG, "已断开service连接");
            stepView.setStepStatus("已停止计步");
        }
    }

    private int detectTime;//检测正常定位的次数,超过5次都是躁点,那么最后一次被假定为正常定位点。

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onLocationUpdate(AMapLocation mapLocation) {

        Log.i(TAG, "onLocationUpdate:" + mapLocation.getLatitude() + "," + mapLocation.getLongitude());

        LatLng latLng = new LatLng(mapLocation.getLatitude(), mapLocation.getLongitude());

        float distance = 0;

        if (lastPreciseLatLng != null) {
            distance = AMapUtils.calculateLineDistance(lastPreciseLatLng, latLng);
        }

        String desc = String.format("%s,[%s]类型[%s]海拨[%s]精度[%s]距离[%s]速度[%s]", mapLocation.getAddress(), timeFormat.format(mapLocation.getTime()), mapLocation.getLocationType(), mapLocation.getAltitude(), mapLocation.getAccuracy(), distance,mapLocation.getSpeed());

        stepView.setCurLocation(desc);

        if (mapLocation.getAccuracy() > 10f) {
            Log.i(TAG, "检测到低精度定位,距离:" + distance);
            return;
        }
        if (lastPreciseLatLng == null) {
            lastPreciseLatLng = latLng;//假定第一次精度在10米以内的定位为起点
            movementPoints.add(latLng);
        } else if (distance >= 1f && distance <= 3.2f) {
            detectTime = 0;
            movementPoints.add(latLng);
            lastPreciseLatLng = latLng;
        } else if (distance > 3.2f) {
            detectTime++;
            Log.i(TAG, "检测到躁点,距离:" + distance);
        }
        if (detectTime > 4) {
            Log.i(TAG, "连续5次检测到躁点,距离:" + distance);
            detectTime = 0;
            lastPreciseLatLng = latLng;//将最后一次运动的位置假定为正常位置
        }
    }

    @Override
    public void onDestroy(LifecycleOwner owner) {
        EventBus.getDefault().unregister(this);
        stopStep();
    }

    @Override
    public void onLifecycleChanged(LifecycleOwner owner, Lifecycle.Event event) {

    }

    // 公里计算公式
    static String getDistanceByStep(long steps) {
        return String.format("%.2f", steps * 0.6f / 1000);
    }

    // 千卡路里计算公式
    static String getCalorieByStep(long steps) {
        return String.format("%.1f", steps * 0.6f * 60 * 1.036f / 1000);
    }
}
