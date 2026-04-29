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

package org.apache.cloudstack.test.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.component.ComponentInstantiationPostProcessor;
import com.cloud.utils.component.ComponentMethodInterceptor;
import com.cloud.utils.db.TransactionContextBuilder;
import com.cloud.utils.exception.CloudRuntimeException;

public class SpringUtils {

    /**
     * This method allows you to use @ComponentScan for your unit testing but
     * it limits the scope of the classes found to the class specified in
     * the @ComponentScan annotation.
     *
     * Without using this method, the default behavior of @ComponentScan is
     * to actually scan in the package of the class specified rather than
     * only the class. This can cause extra classes to be loaded which causes
     * the classes these extra classes depend on to be loaded. The end effect
     * is often most of the project gets loaded.
     *
     * In order to use this method properly, you must do the following: <li>
     *   - Specify @ComponentScan with basePackageClasses, includeFilters, and
     *     useDefaultFilters=true.  See the following example.
     *
     * <pre>
     *     @ComponentScan(basePackageClasses={AffinityGroupServiceImpl.class, EventUtils.class},
     *     includeFilters={@Filter(value=TestConfiguration.Library.class, type=FilterType.CUSTOM)},
     *     useDefaultFilters=false)
     * </pre>
     *
     *   - Create a Library class and use that to call this method.  See the
     *     following example.  The Library class you define here is the Library
     *     class being added in the filter above.
     *
     * <pre>
     * public static class Library implements TypeFilter {
     *      @Override
     *      public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
     *          ComponentScan cs = TestConfiguration.class.getAnnotation(ComponentScan.class);
     *          return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
     *      }
     * }
     * </pre>
     *
     * @param clazzName name of the class that should be included in the Spring components
     * @param cs ComponentScan annotation that was declared on the configuration
     *
     * @return
     */
    public static boolean includedInBasePackageClasses(String clazzName, ComponentScan cs) {
        Class<?> clazzToCheck;
        try {
            clazzToCheck = Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException("Unable to find " + clazzName);
        }
        Class<?>[] clazzes = cs.basePackageClasses();
        for (Class<?> clazz : clazzes) {
            if (clazzToCheck.isAssignableFrom(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static class CloudStackTestConfiguration {

        @Bean
        public ComponentContext componentContext() {
            return new ComponentContext();
        }

        @Bean
        public TransactionContextBuilder transactionContextBuilder() {
            return new TransactionContextBuilder();
        }

        @Bean
        public ComponentInstantiationPostProcessor instantiatePostProcessor() {
            ComponentInstantiationPostProcessor processor = new ComponentInstantiationPostProcessor();

            List<ComponentMethodInterceptor> interceptors = new ArrayList<ComponentMethodInterceptor>();
            interceptors.add(new TransactionContextBuilder());
            processor.setInterceptors(interceptors);

            return processor;
        }

    }
}
