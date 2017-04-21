package com.walfud.dustofappearance;

import java.lang.reflect.Field;

/**
 * Created by walfud on 2017/4/21.
 */

public class Utils {
    public static void reflectSet(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Field NOT Found: %s", fieldName));
        }
    }
}
