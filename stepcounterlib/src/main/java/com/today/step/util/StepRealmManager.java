package com.today.step.util;

import android.content.Context;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.exceptions.RealmMigrationNeededException;

/**
 * Create by nandy on 2018-06-05
 */
public class StepRealmManager {
    private static volatile StepRealmManager stepRealmManager;

    private RealmConfiguration realmConfiguration;

    private StepRealmManager() {
    }

    public static StepRealmManager getInstance() {
        if (stepRealmManager == null) {
            synchronized (StepRealmManager.class) {
                if (stepRealmManager == null) {
                    stepRealmManager = new StepRealmManager();
                }
            }
        }
        return stepRealmManager;
    }

    private void initRealm(Context context) {
        Realm.init(context);
        if (realmConfiguration == null) {
            realmConfiguration = new RealmConfiguration.Builder()
                    .name("StepData.realm")
                    .schemaVersion(2)
                    .build();
        }
    }

    public Realm getRealm(Context context) {
        if (realmConfiguration == null) {
            initRealm(context);
        }
        try {
            return Realm.getInstance(realmConfiguration);
        } catch (RealmMigrationNeededException e) {
            Realm.deleteRealm(realmConfiguration);
            return Realm.getInstance(realmConfiguration);
        }

    }


}
