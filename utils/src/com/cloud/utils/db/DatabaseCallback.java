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

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import com.cloud.utils.component.AnnotationInterceptor;

public class DatabaseCallback implements MethodInterceptor, AnnotationInterceptor<Transaction> {

    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        Transaction txn = interceptStart(method);
        try {
            return methodProxy.invokeSuper(object, args);
        } finally {
            interceptComplete(method, txn);
        }
    }

    @Override
    public boolean needToIntercept(AnnotatedElement element) {
        DB db = element.getAnnotation(DB.class);
        if (db != null) {
            return db.txn();
        }
        
        Class<?> clazz = element.getClass().getDeclaringClass();
        do {
            db = clazz.getAnnotation(DB.class);
            if (db != null) {
                return db.txn();
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class && clazz != null);
        
        return false;
    }

    @Override
    public Transaction interceptStart(AnnotatedElement element) {
        return Transaction.open(((Method)element).getName());
    }

    @Override
    public void interceptComplete(AnnotatedElement element, Transaction txn) {
        txn.close();
    }

    @Override
    public void interceptException(AnnotatedElement element, Transaction txn) {
        txn.close();
    }

    @Override
    public Callback getCallback() {
        return this;
    }
    
}
