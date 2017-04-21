package com.walfud.dustofappearance;

import java.lang.reflect.Field;

/**
 * Created by walfud on 2017/4/21.
 */

public class Utils {
    public static void reflectSet(Object target, String fieldName, Object value) {
        try {
            reflect(target, fieldName).set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T reflectGet(T target, String fieldName) {
        try {
            return (T) reflect(target, fieldName).get(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Field reflect(Object target, String fieldName) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Field NOT Found: %s", fieldName));
        }
    }
}
