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
package com.cloud.agent.api.storage;

import com.cloud.agent.api.to.DataTO;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.util.List;

public class SnapshotMergeTreeTO {
    DataTO parent;
    DataTO child;
    List<DataTO> grandChildren;

    public SnapshotMergeTreeTO(DataTO parent, DataTO child, List<DataTO> grandChildren) {
        this.parent = parent;
        this.child = child;
        this.grandChildren = grandChildren;
    }

    public DataTO getParent() {
        return parent;
    }

    public DataTO getChild() {
        return child;
    }

    public List<DataTO> getGrandChildren() {
        return grandChildren;
    }

    public void addGrandChild(DataTO grandChild) {
        grandChildren.add(grandChild);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
