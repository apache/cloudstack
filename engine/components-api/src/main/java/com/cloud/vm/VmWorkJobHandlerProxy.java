// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.vm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;

import org.apache.cloudstack.framework.jobs.impl.JobSerializerHelper;
import org.apache.cloudstack.jobs.JobInfo;

import com.cloud.serializer.GsonHelper;
import com.cloud.utils.Pair;

/**
 * VmWorkJobHandlerProxy can not be used as standalone due to run-time
 * reflection usage in its implementation, run-time reflection conflicts with Spring proxy mode.
 * It means that we can not instantiate VmWorkJobHandlerProxy beans directly in Spring and expect
 * it can handle VmWork directly from there.
 *
 */
public class VmWorkJobHandlerProxy implements VmWorkJobHandler {

    protected Logger logger = LogManager.getLogger(getClass());

    private Object _target;
    private Map<Class<?>, Method> _handlerMethodMap = new HashMap<Class<?>, Method>();

    private Gson _gsonLogger;

    public VmWorkJobHandlerProxy(Object target) {
        _gsonLogger = GsonHelper.getGsonLogger();

        buildLookupMap(target.getClass());
        _target = target;
    }

    private void buildLookupMap(Class<?> hostClass) {
        Class<?> clz = hostClass;
        while (clz != null && clz != Object.class) {
            Method[] hostHandlerMethods = clz.getDeclaredMethods();

            for (Method method : hostHandlerMethods) {
                if (isVmWorkJobHandlerMethod(method)) {
                    Class<?> paramType = method.getParameterTypes()[0];
                    assert (_handlerMethodMap.get(paramType) == null);

                    method.setAccessible(true);
                    _handlerMethodMap.put(paramType, method);
                }
            }

            clz = clz.getSuperclass();
        }
    }

    @SuppressWarnings("deprecation")
    private boolean isVmWorkJobHandlerMethod(Method method) {
        if (method.getParameterTypes().length != 1)
            return false;

        Class<?> returnType = method.getReturnType();
        if (!Pair.class.isAssignableFrom(returnType))
            return false;

        Class<?> paramType = method.getParameterTypes()[0];
        if (!VmWork.class.isAssignableFrom(paramType))
            return false;

        return true;
    }

    private Method getHandlerMethod(Class<?> paramType) {
        return _handlerMethodMap.get(paramType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Pair<JobInfo.Status, String> handleVmWorkJob(VmWork work) throws Exception {

        Method method = getHandlerMethod(work.getClass());
        if (method != null) {

            try {
                if (logger.isDebugEnabled())
                    logger.debug("Execute VM work job: " + work.getClass().getName() + _gsonLogger.toJson(work));

                Object obj = method.invoke(_target, work);

                if (logger.isDebugEnabled())
                    logger.debug("Done executing VM work job: " + work.getClass().getName() + _gsonLogger.toJson(work));

                assert (obj instanceof Pair);
                return (Pair<JobInfo.Status, String>)obj;
            } catch (InvocationTargetException e) {
                logger.error("Invocation exception, caused by: " + e.getCause());

                // legacy CloudStack code relies on checked exception for error handling
                // we need to re-throw the real exception here
                if (e.getCause() != null && e.getCause() instanceof Exception) {
                    logger.info("Rethrow exception " + e.getCause());
                    throw (Exception)e.getCause();
                }

                throw e;
            }
        } else {
            logger.error("Unable to find handler for VM work job: " + work.getClass().getName() + _gsonLogger.toJson(work));

            RuntimeException ex = new RuntimeException("Unable to find handler for VM work job: " + work.getClass().getName());
            return new Pair<JobInfo.Status, String>(JobInfo.Status.FAILED, JobSerializerHelper.toObjectSerializedString(ex));
        }
    }
}
