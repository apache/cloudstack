// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.db;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

public class TransactionContextBuilder implements MethodInterceptor {
	public TransactionContextBuilder() {
	}
	
	public Object AroundAnyMethod(ProceedingJoinPoint call) throws Throwable {
		MethodSignature methodSignature = (MethodSignature)call.getSignature();
        Method targetMethod = methodSignature.getMethod();	
        if(needToIntercept(targetMethod)) {
			Transaction txn = Transaction.open(call.getSignature().getName());
			Object ret = null;
			try {
				 ret = call.proceed();
			} finally {
				txn.close();
			}
			return ret;
        }
        return call.proceed();
	}

	@Override
	public Object invoke(MethodInvocation method) throws Throwable {
		Method targetMethod = method.getMethod();
		
        if(needToIntercept(targetMethod)) {
			Transaction txn = Transaction.open(targetMethod.getName());
			Object ret = null;
			try {
				 ret = method.proceed();
			} finally {
				txn.close();
			}
			return ret;
        }
        return method.proceed();
	}
	
	private boolean needToIntercept(Method method) {
        DB db = method.getAnnotation(DB.class);
        if (db != null) {
            return true;
        }
        
        Class<?> clazz = method.getDeclaringClass();
        do {
            db = clazz.getAnnotation(DB.class);
            if (db != null) {
                return true;
            }
            clazz = clazz.getSuperclass();
        } while (clazz != Object.class && clazz != null);
        
        return false;
    }
}
