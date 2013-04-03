/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.framework.ws.jackson;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Url can be placed onto a method to construct an URL from the returned
 * results.
 *
 * This annotation is supplemental to JAX-RS 2.0's annotations.  JAX-RS 2.0
 * annotations do not include a way to construct an URL.  Of
 * course, this only works with how CloudStack works.
 *
 */
@Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface Url {
    /**
     * @return the class that the path should belong to.
     */
    Class<?> clazz() default Object.class;

    /**
     * @return the name of the method that the path should call back to.
     */
    String method();

    String name() default "";

    Class<?> type() default String.class;
}
