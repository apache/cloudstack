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

package org.apache.cloudstack.network.contrail.model;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.Serializable;
import java.util.Comparator;
import java.util.TreeSet;

public abstract class ModelObjectBase implements ModelObject {
    protected Logger logger = LogManager.getLogger(getClass());
    public static class UuidComparator implements Comparator<ModelObject>, Serializable {
        @Override
        public int compare(ModelObject lhs, ModelObject rhs) {
            if (lhs == null) {
                if (rhs == null) {
                    return 0;
                }
                return -1;
            }
            if (rhs == null) {
                return 1;
            }
            return lhs.compareTo(rhs);
        }
    }

    private TreeSet<ModelReference> _ancestors;

    private TreeSet<ModelObject> _successors;

    ModelObjectBase() {
        _ancestors = new TreeSet<ModelReference>();
        _successors = new TreeSet<ModelObject>(new UuidComparator());
    }

    @Override
    public void addSuccessor(ModelObject child) {
        _successors.add(child);
        ModelObjectBase base = (ModelObjectBase)child;
        base._ancestors.add(new ModelReference(this));
    }

    @Override
    public TreeSet<ModelReference> ancestors() {
        return _ancestors;
    }

    private void clearAncestorReference(ModelObjectBase child) {
        ModelReference ref = null;
        for (ModelReference objref : child._ancestors) {
            if (objref.get() == this) {
                ref = objref;
                break;
            }
        }
        if (ref != null) {
            child._ancestors.remove(ref);
        }
    }

    @Override
    public void clearSuccessors() {
        for (ModelObject successor : _successors) {
            clearAncestorReference((ModelObjectBase)successor);
        }
        _successors.clear();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((_ancestors == null) ? 0 : _ancestors.hashCode());
        result = prime * result + ((_successors == null) ? 0 : _successors.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object rhs) {
        if (this == rhs)
            return true;
        if (rhs == null)
            return false;
        ModelObject other;
        try {
            other = (ModelObject)rhs;
        } catch (ClassCastException ex) {
            return false;
        }
        return compareTo(other) == 0;
    }

    @Override
    protected void finalize() {
        clearSuccessors();
    }

    public boolean hasDescendents() {
        return !successors().isEmpty();
    }

    @Override
    public void removeSuccessor(ModelObject child) {
        clearAncestorReference((ModelObjectBase)child);
        _successors.remove(child);
    }

    @Override
    public TreeSet<ModelObject> successors() {
        return _successors;
    }
}
