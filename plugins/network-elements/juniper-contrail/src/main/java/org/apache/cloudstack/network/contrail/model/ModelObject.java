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

import java.io.IOException;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.cloud.exception.InternalErrorException;

/**
 * ModelObject
 *
 * A model object represents the desired state of the system.
 *
 * The object constructor should set the uuid and the internal id of the cloudstack objects.
 *
 * The build method reads the primary database (typically cloudstack mysql) and derives the state that
 * we wish to reflect in the contrail API. This method should not modify the Contrail API state.
 *
 * The verify method reads the API server state and compares with cached properties.
 *
 * The update method pushes updates to the contrail API server.
 */
public interface ModelObject {

    public static class ModelReference implements Comparable<ModelReference>, Serializable {

        private static final long serialVersionUID = -2019113974956703526L;
        private static final Logger s_logger = Logger.getLogger(ModelReference.class);

        /*
         * WeakReference class is not serializable by definition. So, we cannot enforce its serialization unless we write the implementation of
         * methods writeObject() and readObject(). Since the code was already not serializing it, it's been marked as transient.
         */
        transient WeakReference<ModelObject> reference;

        ModelReference(ModelObject obj) {
            reference = new WeakReference<ModelObject>(obj);
        }

        @Override
        public int compareTo(ModelReference other) {
            ModelObject lhs = reference.get();
            ModelObject rhs = other.reference.get();
            if (lhs == null) {
                if (rhs == null) {
                    return 0;
                }
                return -1;
            }

            return lhs.compareTo(rhs);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((reference == null) ? 0 : reference.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (other == null)
                return false;
            try {
                ModelReference rhs = (ModelReference)other;
                return compareTo(rhs) == 0;
            } catch (ClassCastException ex) {
                // not this class , so
                return false;
            }
        }

        public ModelObject get() {
            return reference.get();
        }

    };

    public void addSuccessor(ModelObject child);

    public TreeSet<ModelReference> ancestors();

    public void clearSuccessors();

    public int compareTo(ModelObject o);

    /**
     * Delete the object from the API server.
     * @param controller
     * @throws IOException
     */
    public void delete(ModelController controller) throws IOException;

    /**
     * Deletes the object from the data model graph.
     *
     * @param controller
     * @throws IOException
     */
    public void destroy(ModelController controller) throws IOException;

    public void removeSuccessor(ModelObject child);

    public TreeSet<ModelObject> successors();

    /**
     * Push updates to Contrail API server. This API is only valid for objects in the database.
     * @param controller
     * @throws IOException
     * @throws InternalErrorException
     */
    public void update(ModelController controller) throws InternalErrorException, IOException;

    /**
     * Check that the state of the current object matches the state of the API server.
     * @param controller
     * @return
     */
    public boolean verify(ModelController controller);

    /*
     * Compare the state of existing model object with latest model object
     */
    public boolean compare(ModelController controller, ModelObject current);
}
