package presenter;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleOwner;
import android.util.Log;

/**
 * Create by nandy on 2018/6/8
 */
public class BasePresenter implements IPresenter {
    private String TAG = this.getClass().getSimpleName();

    @Override
    public void onCreate(LifecycleOwner owner) {
        Log.d(TAG, "onCreate()");

    }

    @Override
    public void onDestroy(LifecycleOwner owner) {
        Log.d(TAG, "onDestroy()");
    }

    @Override
    public void onLifecycleChanged(LifecycleOwner owner, Lifecycle.Event event) {
        Log.d(TAG, "onLifecycleChanged()");
    }

}
