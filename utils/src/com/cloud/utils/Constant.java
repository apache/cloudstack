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

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Constant is different from Enum in that it allows anyone who declares a
 * class that extends from it to allow for more Constants to be added.  It does
 * not all need to be declared in the same place like Enums are.  The class
 * is really geared toward String as the underneath value.
 * 
 * However, the flexibility of Constant can lead to bad programming practices
 * since you can dynamically add Constant values at any time.  The correct 
 * thing to do is declare Constant like you would declare other constants, 
 * as static variables.  Unfortunately, I have no way to enforce that so I
 * leave it up to the developer to behave. .... So behave! 
 */
public abstract class Constant {
    static Map<Class<?>, Map<String, Constant>> _values = new HashMap<Class<?>, Map<String, Constant>>();
    
    Serializable _value;
    public Constant(Serializable value) {
        Class<?> clazz = value.getClass();
        synchronized(Constant.class) {
            Map<String, Constant> map = alreadyContains(clazz, value);
            if (map == null) {
                map = new HashMap<String, Constant>();
                _values.put(clazz, map);
            }
           _value = value; 
           map.put(value.toString(), this);
        }
    }
    
    final private Map<String, Constant> alreadyContains(Class<?> clazz, Object value) {
        Map<String, Constant> map = _values.get(clazz);
        if (map == null) {
            return null;
        }

        assert !map.containsKey(value.toString()) : "Unfortunately you can not declare this constant as it has been declared by someone else";
        
        return map;
    }
    
    public final Serializable value() {
        return _value;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Constant)) {
            return false;
        }
        Constant that = (Constant)obj;
        return _value.equals(that._value);
    }

    @Override
    public int hashCode() {
        return _value.hashCode();
    }
    
    @Override
    public String toString() {
        return _value.toString();
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Constant> Collection<T> constants(Class<T> clazz) {
        // Note that this method is not synchronized for a reason.  We
        // expect the developer to behave and use Constant as it is intended.
        return (Collection<T>)_values.get(clazz).values();
    }
    
    public static Collection<? extends Constant> values(Class<?> clazz) {
        // Note that this method is not synchronized for a reason.  We
        // expect the developer to behave and use Constant as it is intended.
        return _values.get(clazz).values();
    }
    
    public Collection<? extends Constant> values() {
        return values(this.getClass());
    }
    
    public static Constant constant(Class<?> clazz, Serializable value) {
        // Note that this method is not synchronized for a reason.  We
        // expect the developer to behave and use Constant as it is intended.
        return _values.get(clazz).get(value.toString());
    }
    
    public static Constant parseConstant(Class<?> clazz, String value) {
        // Note that this method is not synchronized for a reason.  We
        // expect the developer to behave and use Constant as it is intended.
        return _values.get(clazz).get(value);
    }
}