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

package com.cloud.utils;

public class Ternary<T, U, V> {
    private T t;
    private U u;
    private V v;

    public Ternary(T t, U u, V v) {
        this.t = t;
        this.u = u;
        this.v = v;
    }

    public T first() {
        return t;
    }

    public void first(T t) {
        this.t = t;
    }

    public U second() {
        return u;
    }

    public void second(U u) {
        this.u = u;
    }

    public V third() {
        return v;
    }

    public void third(V v) {
        this.v = v;
    }

    @Override
    // Note: This means any two pairs with null for both values will match each
    // other but what can I do?  This is due to stupid type erasure.
        public
        int hashCode() {
        return (t != null ? t.hashCode() : 0) | (u != null ? u.hashCode() : 0) | (v != null ? v.hashCode() : 0);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Ternary)) {
            return false;
        }
        Ternary<?, ?, ?> that = (Ternary<?, ?, ?>)obj;
        return (t != null ? t.equals(that.t) : that.t == null) && (u != null ? u.equals(that.u) : that.u == null) && (v != null ? v.equals(that.v) : that.v == null);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("T[");
        b.append(t != null ? t.toString() : "null");
        b.append(":");
        b.append(u != null ? u.toString() : "null");
        b.append(":");
        b.append(v != null ? v.toString() : "null");
        b.append("]");
        return b.toString();
    }
}
