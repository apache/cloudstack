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
package com.cloud.bridge.util;

/**
 * Reusable class whose instances encode any ordered pair (or 2-tuple) of values of types T1 and T2
 * Provide getters: getFirst(), getSecond()
 * Provide setters: setFirst(val), setSecond(val)
 * @param <T1>
 * @param <T2>
 */
public class OrderedPair<T1, T2> {
    T1 first;
    T2 second;

    public OrderedPair(T1 t1, T2 t2) {
        first = t1;
        second = t2;
    }

    public T1 getFirst() {
        return first;
    }

    public OrderedPair<T1, T2> setFirst(T1 t1) {
        first = t1;
        return this;
    }

    public T2 getSecond() {
        return second;
    }

    public OrderedPair<T1, T2> setSecond(T2 t2) {
        second = t2;
        return this;
    }
}
