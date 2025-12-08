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
import java.util.TreeSet;

import net.juniper.contrail.api.ApiConnector;
import net.juniper.contrail.api.types.FloatingIpPool;


import org.apache.cloudstack.network.contrail.management.ContrailManager;

import com.cloud.exception.InternalErrorException;
import com.cloud.utils.exception.CloudRuntimeException;

public class FloatingIpPoolModel extends ModelObjectBase {

    private String _name;

    /*
     * cached API server objects
     */
    private FloatingIpPool _fipPool;
    private VirtualNetworkModel _vnModel;

    public FloatingIpPoolModel() {

    }

    public void addToVirtualNetwork(VirtualNetworkModel vnModel) {
        _vnModel = vnModel;
        if (vnModel != null) {
            vnModel.addSuccessor(this);
        }
    }

    public FloatingIpModel getFloatingIpModel(String uuid) {
        TreeSet<ModelObject> tree = successors();
        FloatingIpModel fipKey = new FloatingIpModel(uuid);
        FloatingIpModel current = (FloatingIpModel)tree.ceiling(fipKey);
        if (current != null && current.getUuid().equals(uuid)) {
            return current;
        }
        return null;
    }

    /*
     * Resynchronize internal state from the cloudstack DB object.
     */
    public void build(ModelController controller) {
        setProperties(controller);
    }

    @Override
    public int compareTo(ModelObject o) {
        /* there can be only one instance */
        return 0;
    }

    @Override
    public void delete(ModelController controller) throws IOException {
        ApiConnector api = controller.getApiAccessor();
        for (ModelObject successor : successors()) {
            successor.delete(controller);
        }
        try {
            if (_fipPool != null) {
                api.delete(_fipPool);
            }
            _fipPool = null;
        } catch (IOException ex) {
            logger.warn("floating ip pool delete", ex);
        }
    }

    @Override
    public void destroy(ModelController controller) throws IOException {
        delete(controller);
        for (ModelObject successor : successors()) {
            successor.destroy(controller);
        }
        clearSuccessors();
    }

    public String getName() {
        return _name;
    }

    public FloatingIpPool getFloatingIpPool() {
        return _fipPool;
    }

    /**
     * Initialize the object properties based on the DB object.
     * Common code between plugin calls and DBSync.
     */
    public void setProperties(ModelController controller) {
        _name = "PublicIpPool";
        assert _vnModel != null : "vn nodel is not initialized";
    }

    @Override
    public void update(ModelController controller) throws InternalErrorException, IOException {

        assert _vnModel != null : "vn model is not set";

        ApiConnector api = controller.getApiAccessor();
        ContrailManager manager = controller.getManager();
        FloatingIpPool fipPool = _fipPool;

        if (fipPool == null) {
            String fipPoolName = manager.getDefaultPublicNetworkFQN() + ":PublicIpPool";
            _fipPool = fipPool = (FloatingIpPool)controller.getApiAccessor().findByFQN(FloatingIpPool.class, fipPoolName);
            if (fipPool == null) {
                fipPool = new FloatingIpPool();
                fipPool.setName(_name);
                fipPool.setParent(_vnModel.getVirtualNetwork());
            }
        }

        if (_fipPool == null) {
            try {
                api.create(fipPool);
            } catch (Exception ex) {
                logger.debug("floating ip pool create", ex);
                throw new CloudRuntimeException("Failed to create floating ip pool", ex);
            }
            _fipPool = fipPool;
        } else {
            try {
                api.update(fipPool);
            } catch (IOException ex) {
                logger.warn("floating ip pool update", ex);
                throw new CloudRuntimeException("Unable to update floating ip ppol object", ex);
            }
        }

        for (ModelObject successor : successors()) {
            successor.update(controller);
        }
    }

    @Override
    public boolean verify(ModelController controller) {
        assert _vnModel != null : "vn model is not set";
        return false;
    }

    @Override
    public boolean compare(ModelController controller, ModelObject o) {
        return true;
    }
}
