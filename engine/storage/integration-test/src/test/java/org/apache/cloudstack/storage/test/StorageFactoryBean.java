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
package org.apache.cloudstack.storage.test;

import org.mockito.Mockito;
import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} for creating mocked beans based on Mockito so that they
 * can be {@link @Autowired} into Spring test configurations.
 *
 * @author Mattias Severson, Jayway
 *
 * @see FactoryBean
 * @see org.mockito.Mockito
 */
public class StorageFactoryBean<T> implements FactoryBean<T> {

    private Class<T> classToBeMocked;

    /**
     * Creates a Mockito mock instance of the provided class.
     *
     * @param classToBeMocked
     *            The class to be mocked.
     */
    public StorageFactoryBean(Class<T> classToBeMocked) {
        this.classToBeMocked = classToBeMocked;
    }

    @Override
    public T getObject() throws Exception {
        return Mockito.mock(classToBeMocked);
    }

    @Override
    public Class<?> getObjectType() {
        return classToBeMocked;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
