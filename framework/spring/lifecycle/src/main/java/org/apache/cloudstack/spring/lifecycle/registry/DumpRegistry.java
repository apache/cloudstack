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
package org.apache.cloudstack.spring.lifecycle.registry;

import java.util.List;

import javax.inject.Inject;


import com.cloud.utils.component.ComponentLifecycleBase;
import com.cloud.utils.component.Named;
import com.cloud.utils.component.Registry;

public class DumpRegistry extends ComponentLifecycleBase {


    List<Registry<?>> registries;

    public List<Registry<?>> getRegistries() {
        return registries;
    }

    @Inject
    public void setRegistries(List<Registry<?>> registries) {
        this.registries = registries;
    }

    @Override
    public boolean start() {
        for (Registry<?> registry : registries) {
            StringBuilder buffer = new StringBuilder();

            for (Object o : registry.getRegistered()) {
                if (buffer.length() > 0)
                    buffer.append(", ");

                buffer.append(getName(o));
            }

            logger.info("Registry [" + registry.getName() + "] contains [" + buffer + "]");
        }

        return super.start();
    }

    protected String getName(Object o) {
        String name = null;
        if (o instanceof Named) {
            name = ((Named)o).getName();
        }

        if (name == null) {
            name = o.getClass().getSimpleName();
        }

        return name;
    }
}
