package com.walfud.dustofappearance;

import android.app.Activity;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Created by walfud on 2017/3/17.
 */

public class DustOfAppearance {
    public static void inject(Activity activity) {
        try {
            Class clazz = Class.forName(activity.getClass().getCanonicalName() + "$$" + Constants.CLASS_NAME);
            Constructor constructor = clazz.getConstructor(Class.forName(activity.getClass().getCanonicalName()));
            Object obj = constructor.newInstance(activity);

            Method find = clazz.getMethod(Constants.METHOD_FIND_VIEW, View.class);
            find.invoke(obj, activity.getWindow().getDecorView());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
