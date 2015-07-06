package org.apache.cloudstack.api;

import java.lang.reflect.Field;

public class ApiCmdTestUtil {
    public static void set(BaseCmd cmd, String fieldName, Object value)
            throws IllegalArgumentException, IllegalAccessException {
        for (Field field : cmd.getClass().getDeclaredFields()) {
            Parameter parameter = field.getAnnotation(Parameter.class);
            if (parameter != null && fieldName.equals(parameter.name())) {
                field.setAccessible(true);
                field.set(cmd, value);
            }
        }
    }

}
