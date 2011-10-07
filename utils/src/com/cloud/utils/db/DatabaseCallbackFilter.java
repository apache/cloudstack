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
package com.cloud.utils.db;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.CallbackFilter;

public class DatabaseCallbackFilter implements CallbackFilter {
    @Override
    public int accept(Method method) {
        return checkAnnotation(method) ? 1 : 0;
    }
    
    public static boolean checkAnnotation(Method method) {
    	/*Check self*/
        DB db = method.getAnnotation(DB.class);
        if (db != null) {
            return db.txn();
        }
        Class<?> clazz = method.getDeclaringClass();
        
        /*Check parent method*/
        try {
	        Method pMethod = clazz.getMethod(method.getName(), method.getParameterTypes());
	        db = pMethod.getAnnotation(DB.class);
	        if (db != null) {
	            return db.txn();
	        }
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
        }
        
        /*Check class's annotation and ancestor's annotation*/
        do {
            db = clazz.getAnnotation(DB.class);
            if (db != null) {
                return db.txn();
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class && clazz != null);
        return false;
    }
}
