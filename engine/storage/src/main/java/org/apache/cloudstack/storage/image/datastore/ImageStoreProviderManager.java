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
package org.apache.cloudstack.storage.image.datastore;

import java.util.List;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStore;
import org.apache.cloudstack.engine.subsystem.api.storage.Scope;
import org.apache.cloudstack.engine.subsystem.api.storage.ZoneScope;
import org.apache.cloudstack.storage.image.ImageStoreDriver;

public interface ImageStoreProviderManager {
    ImageStoreEntity getImageStore(long dataStoreId);

    ImageStoreEntity getImageStore(String uuid);

    List<DataStore> listImageStores();

    List<DataStore> listImageCacheStores();

    List<DataStore> listImageStoresByScope(ZoneScope scope);

    List<DataStore> listImageStoreByProvider(String provider);

    List<DataStore> listImageCacheStores(Scope scope);

    boolean registerDriver(String uuid, ImageStoreDriver driver);

    /**
     * Return a random DataStore from the a list of DataStores.
     *
     * @param imageStores the list of image stores from which a random store
     *                    to be returned
     * @return            random DataStore
     */
    DataStore getRandomImageStore(List<DataStore> imageStores);

    /**
     * Return a DataStore which has free capacity. Stores will be sorted
     * based on their free space and capacity check will be done based on
     * the predefined threshold value. If a store is full beyond the
     * threshold it won't be considered for response. First store in the
     * sorted list free capacity will be returned. When there is no store
     * with free capacity in the list a null value will be returned.
     *
     * @param imageStores the list of image stores from which stores with free
     *                    capacity stores to be returned
     * @return            the DataStore which has free capacity
     */
    DataStore getImageStoreWithFreeCapacity(List<DataStore> imageStores);

    /**
     * Return a list of DataStore which have free capacity. Free capacity check
     * will be done based on the predefined threshold value. If a store is full
     * beyond the threshold it won't be considered for response. An empty list
     * will be returned when no store in the parameter list has free capacity.
     *
     * @param imageStores the list of image stores from which stores with free
     *                    capacity stores to be returned
     * @return            the list of DataStore which have free capacity
     */
    List<DataStore> listImageStoresWithFreeCapacity(List<DataStore> imageStores);
}
