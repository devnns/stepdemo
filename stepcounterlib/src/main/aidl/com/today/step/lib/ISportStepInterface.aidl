// ISportStepInterface.aidl
package com.today.step.lib;

interface ISportStepInterface {

    /**
     * 获取当天运动步数
     */
     int getCurrentSteps();


    /**
     * 根据时间获取步数列表
     *
     * @param date 格式yyyy-MM-dd
     * @return
     */
     int getStepsByDate(String date);

     /**
     *
     *获取传感器类型,返回值Sensor.TYPE_XXX
     *
     */
     int getSensorType();
}
