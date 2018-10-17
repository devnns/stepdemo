package com.devnn.step;


import android.app.Activity;
import android.hardware.Sensor;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import com.devnn.step.view.StepView;

import presenter.StepPresenter;

/**
 * Android计步有两种方式：
 * 一种使用计步传感器(也叫计步协处理器,低端手机没有),可直接获取步数
 * 另一种是使用加速度传感器(一般手机都有),需要借助算法取运动波形中的波峰。
 * 本demo优先使用计步传感器,没有计步传感器就使用加速度传感器Sensor.TYPE_ACCELEROMETER。
 * 计步传感器有两种，分别是Sensor.TYPE_STEP_COUNTER和Sensor.TYPE_STEP_DETECTOR
 * 前者是系统级别的步数,可以监听数量，后者是监听行走这个动作，没有步数。
 * 本demo使用到的两种传感器：Sensor.TYPE_STEP_COUNTER、Sensor.TYPE_ACCELEROMETER
 */
public class MainActivity extends AppCompatActivity implements StepView {
    private String TAG = this.getClass().getSimpleName();
    private TextView tvTodayStep, tvTodayCalories, tvTodayKilometer;//今日步数、卡路里、公里
    private TextView tvYesterdayStep, tvYesterdayCalories, tvYesterdayKilometer;////昨日步数、卡路里、公里
    private TextView tvBeforeYesterdayStep, tvBeforeYesterdayCalories, tvBeforeYesterdayKilometer;//前日步数、卡路里、公里
    private TextView tvStatus;//当前计步状态
    private TextView tvLocation;//当前位置
    private Button btnSwitch;//开启计步、停止计步
    private RadioButton radioButton1;//使用内置计算传感器(也叫计步协处理器,低端手机没有)
    private RadioButton radioButton2;//使用加速度传感器(大部分手机都有)
    private StepPresenter stepPresenter = new StepPresenter(this);//计步控制器

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "手机品牌:" + Build.BRAND);

        //今日数据
        tvTodayStep = findViewById(R.id.step_today_text);
        tvTodayCalories = findViewById(R.id.calories_today_text);
        tvTodayKilometer = findViewById(R.id.distance_today_text);
        //昨天数据
        tvYesterdayStep = findViewById(R.id.step_yesterday_text);
        tvYesterdayCalories = findViewById(R.id.calories_yesterday_text);
        tvYesterdayKilometer = findViewById(R.id.calories_before_yesterday_text);
        //前天数据
        tvBeforeYesterdayStep = findViewById(R.id.step_before_yesterday_text);
        tvBeforeYesterdayCalories = findViewById(R.id.calories_before_yesterday_text);
        tvBeforeYesterdayKilometer = findViewById(R.id.distance_before_yesterday_text);

        tvStatus = findViewById(R.id.sensor_status);
        tvLocation = findViewById(R.id.cur_location);
        radioButton1 = findViewById(R.id.sensor_choice_step);
        radioButton2 = findViewById(R.id.sensor_choice_accelerator);
        btnSwitch = findViewById(R.id.sensor_switch);
        getLifecycle().addObserver(stepPresenter);
    }

    /**
     * 开启进程保护设置
     *
     * @param view
     */
    public void processProtect(View view) {
        stepPresenter.processProtectSetting();
    }

    /**
     * 防睡眠设置
     *
     * @param view
     */
    public void batteryProtect(View view) {
        stepPresenter.preventSleepSetting();
    }

    /**
     * 显示运动轨迹
     *
     * @param view
     */
    public void moveTrace(View view) {
        stepPresenter.showMoveTrace();
    }

    /**
     * 开关计步
     *
     * @param view
     */
    public void onOffSwitch(View view) {
        stepPresenter.switchStep();
    }


    @Override
    public void setStepButtonText(String text) {
        btnSwitch.setText(text);
    }


    @Override
    public void setStepStatus(String status) {
        tvStatus.setText(status);
    }

    @Override
    public void setTodayStep(int step, String calories, String distance) {
        tvTodayStep.setText("今日步数:" + step);
        tvTodayCalories.setText("卡路里:" + calories);
        tvTodayKilometer.setText("里程:" + distance + "km");
    }

    @Override
    public void setYesterdayStep(int step, String calories, String distance) {
        tvYesterdayStep.setText("昨日步数:" + step);
        tvYesterdayCalories.setText("卡路里:" + calories);
        tvYesterdayKilometer.setText("里程:" + distance + "km");
    }

    @Override
    public void setBeforeYesterdayStep(int step, String calories, String distance) {
        tvBeforeYesterdayStep.setText("前日步数:" + step);
        tvBeforeYesterdayCalories.setText("卡路里:" + calories);
        tvBeforeYesterdayKilometer.setText("里程:" + calories + "km");
    }

    @Override
    public void setStepRunningPeriod(String period) {
        tvStatus.setText("用时: " + period);
    }

    @Override
    public void setSensorType(int type) {
        if (type == Sensor.TYPE_STEP_COUNTER) {
            radioButton1.setChecked(true);
        } else if (type == Sensor.TYPE_ACCELEROMETER) {
            radioButton2.setChecked(true);
        }
    }

    @Override
    public void setCurLocation(String location) {
        tvLocation.setText(location);
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        stepPresenter.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
    }


}
