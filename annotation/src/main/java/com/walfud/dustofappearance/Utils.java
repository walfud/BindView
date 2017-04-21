package com.walfud.dustofappearance;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by walfud on 2017/4/21.
 */

public class Utils {
    public static <T> T reflectFieldSet(Object target, String fieldName, T value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Field NOT Found: %s", fieldName));
        }

        return value;
    }

    public static <T> T reflectMethod1Invoke(Object target, String methodName, Class param1, Object... args) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName, param1);
            method.setAccessible(true);
            return (T) method.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Method NOT Found: %s", methodName));
        }
    }
}
