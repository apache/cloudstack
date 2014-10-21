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

import java.lang.reflect.AnnotatedElement;
import java.util.List;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

/**
 * Adds introspectors for the annotations added specifically for CloudStack
 * Web Services.
 *
 */
public class CSJacksonAnnotationIntrospector extends NopAnnotationIntrospector {

    private static final long serialVersionUID = 5532727887216652602L;

    @Override
    public Version version() {
        return new Version(1, 7, 0, "abc", "org.apache.cloudstack", "cloudstack-framework-rest");
    }

    @Override
    public Object findSerializer(Annotated a) {
        AnnotatedElement ae = a.getAnnotated();
        Url an = ae.getAnnotation(Url.class);
        if (an == null) {
            return null;
        }

        if (an.type() == String.class) {
            return new UriSerializer(an);
        } else if (an.type() == List.class) {
            return new UrisSerializer(an);
        }

        throw new UnsupportedOperationException("Unsupported type " + an.type());

    }
}
