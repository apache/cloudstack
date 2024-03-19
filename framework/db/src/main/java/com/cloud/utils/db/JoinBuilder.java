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
    public enum JoinCondition {
        AND(" AND "), OR(" OR ");

        private final String name;

        JoinCondition(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private final T t;
    private final String name;
    private JoinType type;

    private JoinCondition condition;
    private Attribute[] firstAttributes;
    private Attribute[] secondAttribute;

    public JoinBuilder(String name, T t, Attribute firstAttributes, Attribute secondAttribute, JoinType type) {
        this.name = name;
        this.t = t;
        this.firstAttributes = new Attribute[]{firstAttributes};
        this.secondAttribute = new Attribute[]{secondAttribute};
        this.type = type;
    }

    public JoinBuilder(String name, T t, Attribute[] firstAttributes, Attribute[] secondAttribute, JoinType type) {
        this.name = name;
        this.t = t;
        this.firstAttributes = firstAttributes;
        this.secondAttribute = secondAttribute;
        this.type = type;
    }

    public JoinBuilder(String name, T t, Attribute[] firstAttributes, Attribute[] secondAttribute, JoinType type, JoinCondition condition) {
        this.name = name;
        this.t = t;
        this.firstAttributes = firstAttributes;
        this.secondAttribute = secondAttribute;
        this.type = type;
        this.condition = condition;
    }

    public String getName() {
        return name;
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

    public JoinCondition getCondition() {
        return condition;
    }

    public Attribute[] getFirstAttributes() {
        return firstAttributes;
    }

    public void setFirstAttributes(Attribute[] firstAttributes) {
        this.firstAttributes = firstAttributes;
    }

    public Attribute[] getSecondAttribute() {
        return secondAttribute;
    }

    public void setSecondAttribute(Attribute[] secondAttribute) {
        this.secondAttribute = secondAttribute;
    }

}
