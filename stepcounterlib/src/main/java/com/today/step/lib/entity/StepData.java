package com.today.step.lib.entity;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;

@RealmClass
public class StepData extends RealmObject {

    private int userId;

    @Index
    private String date;//格式"2018-06-05"

    private int step;

    private String distance;

    private long lastUpdateTime;//最后一次更新步数时间，用来判断是否隔天


    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        return "StepData{" +
                "lastUpdateTime=" + lastUpdateTime +
                ", userId=" + userId +
                ", date='" + date + '\'' +
                ", step=" + step +
                '}';
    }
}
