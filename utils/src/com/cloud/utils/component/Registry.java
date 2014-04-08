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

package com.cloud.utils.component;

import java.util.List;

/**
 * Simple interface to represents a registry of items
 *
 * @param <T>
 */
public interface Registry<T> extends Named {

    /**
     * Registers an item.  If the item has already been registered the implementation
     * should detect that it is registered and not re-register it.
     *
     * @param type
     * @return true if register, false if not registered or already exists
     */
    boolean register(T type);

    void unregister(T type);

    /**
     * Returns a list that will dynamically change as items are registered/unregister.
     * The list is thread safe to iterate upon.  Traversing the list using an index
     * would not be safe as the size may changed during traversal.
     *
     * @return Unmodifiable list of registered items
     */
    List<T> getRegistered();

}
