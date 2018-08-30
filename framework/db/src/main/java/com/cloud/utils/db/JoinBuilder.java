// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.db;

public class JoinBuilder<T> {

    public enum JoinType {
        INNER("INNER JOIN"), LEFT("LEFT JOIN"), RIGHT("RIGHT JOIN"), RIGHTOUTER("RIGHT OUTER JOIN"), LEFTOUTER("LEFT OUTER JOIN");

        private final String _name;

        JoinType(String name) {
            _name = name;
        }

        public String getName() {
            return _name;
        }
    }

    private final T t;
    private JoinType type;
    private Attribute firstAttribute;
    private Attribute secondAttribute;

    public JoinBuilder(T t, Attribute firstAttribute, Attribute secondAttribute, JoinType type) {
        this.t = t;
        this.firstAttribute = firstAttribute;
        this.secondAttribute = secondAttribute;
        this.type = type;
    }

    public T getT() {
        return t;
    }

    public JoinType getType() {
        return type;
    }

    public void setType(JoinType type) {
        this.type = type;
    }

    public Attribute getFirstAttribute() {
        return firstAttribute;
    }

    public void setFirstAttribute(Attribute firstAttribute) {
        this.firstAttribute = firstAttribute;
    }

    public Attribute getSecondAttribute() {
        return secondAttribute;
    }

    public void setSecondAttribute(Attribute secondAttribute) {
        this.secondAttribute = secondAttribute;
    }

}
