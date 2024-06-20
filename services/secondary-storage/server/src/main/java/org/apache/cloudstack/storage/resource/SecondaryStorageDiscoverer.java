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
package org.apache.cloudstack.storage.resource;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;
import javax.naming.ConfigurationException;


import com.cloud.agent.AgentManager;
import com.cloud.host.HostVO;
import com.cloud.host.Status.Event;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.resource.Discoverer;
import com.cloud.resource.DiscovererBase;
import com.cloud.resource.ServerResource;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VMTemplateZoneVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VMTemplateZoneDao;
import com.cloud.storage.resource.DummySecondaryStorageResource;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.net.NfsUtils;
import com.cloud.utils.script.Script;

/**
 * SecondaryStorageDiscoverer is used to discover secondary
 * storage servers and make sure everything it can do is
 * correct.
 */
public class SecondaryStorageDiscoverer extends DiscovererBase implements Discoverer {

    long _timeout = 2 * 60 * 1000; // 2 minutes
    String _mountParent;
    boolean _useServiceVM = false;

    Random _random = new Random(System.currentTimeMillis());
    @Inject
    protected VMTemplateDao _tmpltDao = null;
    @Inject
    protected VMTemplateZoneDao _vmTemplateZoneDao = null;
    @Inject
    protected VMTemplateDao _vmTemplateDao = null;
    @Inject
    protected AgentManager _agentMgr = null;

    protected SecondaryStorageDiscoverer() {
    }

    @Override
    public Map<? extends ServerResource, Map<String, String>>
        find(long dcId, Long podId, Long clusterId, URI uri, String username, String password, List<String> hostTags) {
        if (!uri.getScheme().equalsIgnoreCase("nfs") && !uri.getScheme().equalsIgnoreCase("cifs") && !uri.getScheme().equalsIgnoreCase("file") &&
            !uri.getScheme().equalsIgnoreCase("iso") && !uri.getScheme().equalsIgnoreCase("dummy")) {
            logger.debug("It's not NFS or file or ISO, so not a secondary storage server: " + uri.toString());
            return null;
        }

        if (uri.getScheme().equalsIgnoreCase("nfs") || uri.getScheme().equalsIgnoreCase("cifs") || uri.getScheme().equalsIgnoreCase("iso")) {
            return createNfsSecondaryStorageResource(dcId, podId, uri);
        } else if (uri.getScheme().equalsIgnoreCase("file")) {
            return createLocalSecondaryStorageResource(dcId, podId, uri);
        } else if (uri.getScheme().equalsIgnoreCase("dummy")) {
            return createDummySecondaryStorageResource(dcId, podId, uri);
        } else {
            return null;
        }
    }

    protected Map<? extends ServerResource, Map<String, String>> createNfsSecondaryStorageResource(long dcId, Long podId, URI uri) {

        if (_useServiceVM) {
            return createDummySecondaryStorageResource(dcId, podId, uri);
        }
        String mountStr = NfsUtils.uri2Mount(uri);

        Script script = new Script(true, "mount", _timeout, logger);
        String mntPoint = null;
        File file = null;
        do {
            mntPoint = _mountParent + File.separator + Integer.toHexString(_random.nextInt(Integer.MAX_VALUE));
            file = new File(mntPoint);
        } while (file.exists());

        if (!file.mkdirs()) {
            logger.warn("Unable to make directory: " + mntPoint);
            return null;
        }

        script.add(mountStr, mntPoint);
        String result = script.execute();
        if (result != null && !result.contains("already mounted")) {
            logger.warn("Unable to mount " + uri.toString() + " due to " + result);
            file.delete();
            return null;
        }

        script = new Script(true, "umount", 0, logger);
        script.add(mntPoint);
        script.execute();

        file.delete();

        Map<NfsSecondaryStorageResource, Map<String, String>> srs = new HashMap<NfsSecondaryStorageResource, Map<String, String>>();

        NfsSecondaryStorageResource storage;
        if (_configDao.isPremium()) {
            Class<?> impl;
            String name = "com.cloud.storage.resource.PremiumSecondaryStorageResource";
            try {
                impl = Class.forName(name);
                final Constructor<?> constructor = impl.getDeclaredConstructor();
                constructor.setAccessible(true);
                storage = (NfsSecondaryStorageResource)constructor.newInstance();
            } catch (final ClassNotFoundException e) {
                logger.error("Unable to load com.cloud.storage.resource.PremiumSecondaryStorageResource due to ClassNotFoundException");
                return null;
            } catch (final SecurityException e) {
                logger.error("Unable to load com.cloud.storage.resource.PremiumSecondaryStorageResource due to SecurityException");
                return null;
            } catch (final NoSuchMethodException e) {
                logger.error("Unable to load com.cloud.storage.resource.PremiumSecondaryStorageResource due to NoSuchMethodException");
                return null;
            } catch (final IllegalArgumentException e) {
                logger.error("Unable to load com.cloud.storage.resource.PremiumSecondaryStorageResource due to IllegalArgumentException");
                return null;
            } catch (final InstantiationException e) {
                logger.error("Unable to load com.cloud.storage.resource.PremiumSecondaryStorageResource due to InstantiationException");
                return null;
            } catch (final IllegalAccessException e) {
                logger.error("Unable to load com.cloud.storage.resource.PremiumSecondaryStorageResource due to IllegalAccessException");
                return null;
            } catch (final InvocationTargetException e) {
                logger.error("Unable to load com.cloud.storage.resource.PremiumSecondaryStorageResource due to InvocationTargetException");
                return null;
            }
        } else {
            storage = new NfsSecondaryStorageResource();
        }

        Map<String, String> details = new HashMap<String, String>();
        details.put("mount.path", mountStr);
        details.put("orig.url", uri.toString());
        details.put("mount.parent", _mountParent);

        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(details);
        params.put("zone", Long.toString(dcId));
        if (podId != null) {
            params.put("pod", podId.toString());
        }
        params.put("guid", uri.toString());
        params.put("secondary.storage.vm", "false");
        params.put("max.template.iso.size", _configDao.getValue("max.template.iso.size"));

        try {
            storage.configure("Storage", params);
        } catch (ConfigurationException e) {
            logger.warn("Unable to configure the storage ", e);
            return null;
        }
        srs.put(storage, details);

        return srs;
    }

    protected Map<? extends ServerResource, Map<String, String>> createLocalSecondaryStorageResource(long dcId, Long podId, URI uri) {
        Map<LocalSecondaryStorageResource, Map<String, String>> srs = new HashMap<LocalSecondaryStorageResource, Map<String, String>>();

        LocalSecondaryStorageResource storage = new LocalSecondaryStorageResource();
        storage = ComponentContext.inject(storage);

        Map<String, String> details = new HashMap<String, String>();

        File file = new File(uri);
        details.put("mount.path", file.getAbsolutePath());
        details.put("orig.url", uri.toString());

        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(details);
        params.put("zone", Long.toString(dcId));
        if (podId != null) {
            params.put("pod", podId.toString());
        }
        params.put("guid", uri.toString());

        try {
            storage.configure("Storage", params);
        } catch (ConfigurationException e) {
            logger.warn("Unable to configure the storage ", e);
            return null;
        }
        srs.put(storage, details);

        return srs;
    }

    protected Map<ServerResource, Map<String, String>> createDummySecondaryStorageResource(long dcId, Long podId, URI uri) {
        Map<ServerResource, Map<String, String>> srs = new HashMap<ServerResource, Map<String, String>>();

        DummySecondaryStorageResource storage = new DummySecondaryStorageResource(_useServiceVM);
        storage = ComponentContext.inject(storage);

        Map<String, String> details = new HashMap<String, String>();

        details.put("mount.path", uri.toString());
        details.put("orig.url", uri.toString());

        Map<String, Object> params = new HashMap<String, Object>();
        params.putAll(details);
        params.put("zone", Long.toString(dcId));
        if (podId != null) {
            params.put("pod", podId.toString());
        }
        params.put("guid", uri.toString());

        try {
            storage.configure("Storage", params);
        } catch (ConfigurationException e) {
            logger.warn("Unable to configure the storage ", e);
            return null;
        }
        srs.put(storage, details);

        return srs;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        super.configure(name, params);

        _mountParent = _params.get("mount.parent");
        if (_mountParent == null) {
            _mountParent = "/mnt";
        }

        String useServiceVM = _params.get("secondary.storage.vm");
        if ("true".equalsIgnoreCase(useServiceVM)) {
            _useServiceVM = true;
        }
        return true;
    }

    @Override
    public boolean matchHypervisor(String hypervisor) {
        if (hypervisor.equals("SecondaryStorage")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Hypervisor.HypervisorType getHypervisorType() {
        return Hypervisor.HypervisorType.None;
    }

    @Override
    public void postDiscovery(List<HostVO> hosts, long msId) {
        if (_useServiceVM) {
            for (HostVO h : hosts) {
                _agentMgr.agentStatusTransitTo(h, Event.AgentDisconnected, msId);
            }
        }
        for (HostVO h : hosts) {
            associateTemplatesToZone(h.getId(), h.getDataCenterId());
        }

    }

    private void associateTemplatesToZone(long hostId, long dcId) {
        VMTemplateZoneVO tmpltZone;

        List<VMTemplateVO> allTemplates = _vmTemplateDao.listAll();
        for (VMTemplateVO vt : allTemplates) {
            if (vt.isCrossZones()) {
                tmpltZone = _vmTemplateZoneDao.findByZoneTemplate(dcId, vt.getId());
                if (tmpltZone == null) {
                    VMTemplateZoneVO vmTemplateZone = new VMTemplateZoneVO(dcId, vt.getId(), new Date());
                    _vmTemplateZoneDao.persist(vmTemplateZone);
                }
            }
        }
    }
}
