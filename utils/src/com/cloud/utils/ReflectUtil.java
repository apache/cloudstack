/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.cloud.utils.exception.CloudRuntimeException;

public class ReflectUtil {
    public static Pair<Class<?>, Field> getAnyField(Class<?> clazz, String fieldName) {
        try {
            return new Pair<Class<?>, Field>(clazz, clazz.getDeclaredField(fieldName));
        } catch (SecurityException e) {
            throw new CloudRuntimeException("How the heck?", e);
        } catch (NoSuchFieldException e) {
            // Do I really want this?  No I don't but what can I do?  It only throws the NoSuchFieldException.
            Class<?> parent = clazz.getSuperclass();
            if (parent != null) {
                return getAnyField(parent, fieldName);
            }
            return null;
        }
    }
    
    public static Method findMethod(Class<?> clazz, String methodName) {
        do {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (methodName.equals(method.getName())) {
                    return method;
                }
            }
            clazz = clazz.getSuperclass();
        } while (clazz != null);
        return null;
    }

}
