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
package org.apache.cloudstack.api;

import static java.lang.annotation.ElementType.FIELD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.BaseCmd.CommandType;

@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD})
public @interface Parameter {
    String name() default "";

    String description() default "";

    boolean required() default false;

    CommandType type() default CommandType.OBJECT;

    CommandType collectionType() default CommandType.OBJECT;

    Class<?>[] entityType() default Object.class;

    boolean expose() default true;

    boolean includeInApiDoc() default true;

    int length() default 255;

    String since() default "";

    RoleType[] authorized() default {};
}
