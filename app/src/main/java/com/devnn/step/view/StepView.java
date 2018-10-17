package com.devnn.step.view;


/**
 * Create by nandy on 2018/6/8
 */
public interface StepView extends IView{

    void setStepStatus(String status);

    void setStepButtonText(String text);

    void setTodayStep(int step,String calories,String distance);

    void setYesterdayStep(int step,String calories,String distance);

    void setBeforeYesterdayStep(int step,String calories,String distance);

    void setStepRunningPeriod(String period);

    void setSensorType(int type);

    void setCurLocation(String location);
}
