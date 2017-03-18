package com.walfud.bindview;

import android.app.Activity;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by walfud on 2017/3/17.
 */

public class BindView {
    public static void inject(Activity activity) {
        try {
            Class clazz = Class.forName(activity.getClass().getCanonicalName() + "$$BindView");
            Constructor constructor = clazz.getConstructor(Class.forName(activity.getClass().getCanonicalName()));
            Object obj = constructor.newInstance(activity);

            Method bind = clazz.getMethod("bind", View.class);
            bind.invoke(obj, activity.getWindow().getDecorView());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
