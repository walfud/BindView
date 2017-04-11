package com.walfud.findview;

import android.app.Activity;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by walfud on 2017/3/17.
 */

public class FindView {
    public static void inject(Activity activity) {
        try {
            Class clazz = Class.forName(activity.getClass().getCanonicalName() + "$$FindView");
            Constructor constructor = clazz.getConstructor(Class.forName(activity.getClass().getCanonicalName()));
            Object obj = constructor.newInstance(activity);

            Method find = clazz.getMethod("find", View.class);
            find.invoke(obj, activity.getWindow().getDecorView());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
