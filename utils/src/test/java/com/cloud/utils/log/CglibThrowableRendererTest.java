//
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
//

package com.cloud.utils.log;

import java.lang.reflect.Method;

import org.junit.Assert;
import org.junit.Test;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class CglibThrowableRendererTest {

    CglibThrowableRenderer cglibThrowableRenderer = new CglibThrowableRenderer();

    @Test
    public void testDoRendere() {
        SampleClass sampleClass = (SampleClass)Enhancer.create(SampleClass.class, new MyInvocationHandler());
        try {
            sampleClass.theFirstMethodThatCapturesAnException();
        } catch (Exception e) {
            String[] exceptions = cglibThrowableRenderer.doRender(e);
            dumpExceptions(exceptions);
            Assert.assertEquals(40, exceptions.length);
        }
    }

    private void dumpExceptions(String[] exceptions) {
        System.out.println("Dumping exception for debugging purposes");
        for (String s : exceptions) {
            System.out.println(s);
        }
    }

    static class SampleClass {
        public void theFirstMethodThatCapturesAnException() {
            try {
                methodThatCapturesAndThrowsException();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void methodThatCapturesAndThrowsException() throws Exception {
            try {
                methodThatThrowsAnError();
            } catch (Error e) {
                throw new Exception("Throws an exception", e);
            }
        }

        private void methodThatThrowsAnError() {
            throw new Error("Exception to test the CglibThrowableRenderer.");
        }
    }

    static class MyInvocationHandler implements MethodInterceptor {
        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            return proxy.invoke(new SampleClass(), args);
        }
    }
}
