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
package com.cloud.consoleproxy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cloud.consoleproxy.util.Logger;

public class AjaxFIFOImageCache {
    private static final Logger s_logger = Logger.getLogger(AjaxFIFOImageCache.class);

    private List<Integer> fifoQueue;
    private Map<Integer, byte[]> cache;
    private int cacheSize;
    private int nextKey = 0;

    public AjaxFIFOImageCache(int cacheSize) {
        this.cacheSize = cacheSize;
        fifoQueue = new ArrayList<Integer>();
        cache = new HashMap<Integer, byte[]>();
    }

    public synchronized void clear() {
        fifoQueue.clear();
        cache.clear();
    }

    public synchronized int putImage(byte[] image) {
        while (cache.size() >= cacheSize) {
            Integer keyToRemove = fifoQueue.remove(0);
            cache.remove(keyToRemove);

            if (s_logger.isTraceEnabled())
                s_logger.trace("Remove image from cache, key: " + keyToRemove);
        }

        int key = getNextKey();

        if (s_logger.isTraceEnabled())
            s_logger.trace("Add image to cache, key: " + key);

        cache.put(key, image);
        fifoQueue.add(key);
        return key;
    }

    public synchronized byte[] getImage(int key) {
        if (key == 0) {
            key = nextKey;
        }
        if (cache.containsKey(key)) {
            if (s_logger.isTraceEnabled())
                s_logger.trace("Retrieve image from cache, key: " + key);

            return cache.get(key);
        }

        if (s_logger.isTraceEnabled())
            s_logger.trace("Image is no long in cache, key: " + key);
        return null;
    }

    public synchronized int getNextKey() {
        return ++nextKey;
    }
}