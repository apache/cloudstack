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

package org.apache.cloudstack.storage.fileshare.lifecycle;

import static org.apache.cloudstack.storage.fileshare.FileShare.FileShareVmNamePrefix;

import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.OperationTimedoutException;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;

import com.cloud.exception.ResourceUnavailableException;
import com.cloud.vm.UserVmManager;
import com.cloud.vm.UserVmService;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;

import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.storage.fileshare.FileShare;
import org.apache.cloudstack.storage.fileshare.FileShareLifeCycle;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import com.cloud.dc.DataCenter;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Network;
import com.cloud.resource.ResourceManager;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.service.dao.ServiceOfferingDao;
import com.cloud.storage.DiskOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.dao.DiskOfferingDao;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.VirtualMachineManager;

public class StorageFsVmFileShareLifeCycle implements FileShareLifeCycle {
    @Inject
    private AccountManager accountMgr;

    @Inject
    private EntityManager entityMgr;

    @Inject
    protected ResourceManager resourceMgr;

    @Inject
    private VirtualMachineManager virtualMachineManager;

    @Inject
    protected UserVmService userVmService;

    @Inject
    protected UserVmManager userVmManager;

    @Inject
    private VMTemplateDao templateDao;

    @Inject
    VMInstanceDao vmDao;

    @Inject
    private UserVmDao userVmDao;

    @Inject
    ServiceOfferingDao serviceOfferingDao;

    @Inject
    private DiskOfferingDao diskOfferingDao;

    protected String readResourceFile(String resource) throws IOException {
        return IOUtils.toString(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)), com.cloud.utils.StringUtils.getPreferredCharset());
    }

    @Override
    public Pair<String, Long> deployFileShare(FileShare fileShare, Long networkId, Long diskOfferingId, Long size) {
        Account owner = accountMgr.getActiveAccountById(fileShare.getAccountId());

        Long zoneId = fileShare.getDataCenterId();
        DataCenter zone = entityMgr.findById(DataCenter.class, zoneId);
        if (zone == null) {
            throw new InvalidParameterValueException("Unable to find zone by id=" + zoneId);
        }

        Hypervisor.HypervisorType availableHypervisor = resourceMgr.getAvailableHypervisor(zoneId);
        VMTemplateVO template = templateDao.findSystemVMReadyTemplate(zoneId, availableHypervisor);

        if (template == null) {
            throw new CloudRuntimeException(String.format("Unable to find the system templates or it was not downloaded in %s.", zone.toString()));
        }

        ServiceOfferingVO serviceOffering = serviceOfferingDao.findById(fileShare.getServiceOfferingId());
        DiskOfferingVO diskOffering = diskOfferingDao.findById(diskOfferingId);
        Long diskSize = 0L;
        if (diskOffering.isCustomized()) {
            diskSize = size;
        }

        String suffix = Long.toHexString(System.currentTimeMillis());
        String hostName = String.format("%s-node-%s", FileShareVmNamePrefix, suffix);

        Network.IpAddresses addrs = new Network.IpAddresses(null, null);
        Map<String, String> customParameterMap = new HashMap<String, String>();

        UserVm vm = null;
        List<String> keypairs = new ArrayList<String>();
        try {
            String fsVmConfig = readResourceFile("/conf/fsvm-init.yml");
            String base64UserData = Base64.encodeBase64String(fsVmConfig.getBytes(com.cloud.utils.StringUtils.getPreferredCharset()));
            vm = userVmService.createAdvancedVirtualMachine(zone, serviceOffering, template, List.of(networkId), owner, hostName, hostName,
                    diskOffering.getId(), diskSize, null, Hypervisor.HypervisorType.None, BaseCmd.HTTPMethod.POST, base64UserData,
                   null, null, keypairs, null, addrs, null, null, null,
                    customParameterMap, null, null, null, null,
                   true, UserVmManager.STORAGEFSVM, null);

        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to deploy vm");
        }

        try {
            userVmManager.startVirtualMachine(vm);
        } catch (OperationTimedoutException | ResourceUnavailableException | InsufficientCapacityException ex) {
            throw new CloudRuntimeException("Failed to start VM");
        }

        return new Pair<>(vm.getPrivateIpAddress(), vm.getId());
    }

    @Override
    public boolean initializeFileShare(FileShare fileShare) {
        return false;
    }

    @Override
    public boolean deleteFileShare(FileShare fileShare) {
        Long vmId = fileShare.getVmId();
        if (vmId != null) {
            VirtualMachine vm = vmDao.findById(vmId);
            try {
                virtualMachineManager.expunge(vm.getUuid());
            } catch (ResourceUnavailableException e) {
                //logger.warn(String.format("Unable to destroy storagefsvm [%s] due to [%s].", vm, e.getMessage()), e);
                return false;
            }
            userVmDao.remove(vmId);
        }
        return true;
    }

    @Override
    public boolean resizeFileShare(FileShare fileShare, Long newSize) {
        return false;
    }
}
