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

package com.cloud.vm;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ScaleVmAnswer;
import com.cloud.agent.api.ScaleVmCommand;
import com.cloud.capacity.CapacityManager;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.service.ServiceOfferingVO;
import com.cloud.storage.VMTemplateVO;
import com.cloud.storage.VolumeManager;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.dao.VMTemplateDao;
import com.cloud.storage.dao.VolumeDao;
import com.cloud.user.*;
import com.cloud.user.dao.AccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.dao.UserVmDao;
import com.cloud.vm.dao.VMInstanceDao;
import org.apache.cloudstack.api.command.user.vm.RestoreVMCmd;
import org.apache.cloudstack.api.command.user.vm.ScaleVMCmd;
import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import static org.mockito.Mockito.*;


import java.lang.reflect.Field;
import java.util.List;

public class VirtualMachineManagerImplTest {


        @Spy VirtualMachineManagerImpl _vmMgr = new VirtualMachineManagerImpl();
        @Mock
        VolumeManager _storageMgr;
        @Mock
        Account _account;
        @Mock
        AccountManager _accountMgr;
        @Mock
        ConfigurationManager _configMgr;
        @Mock
        CapacityManager _capacityMgr;
        @Mock
        AgentManager _agentMgr;
        @Mock
        AccountDao _accountDao;
        @Mock
        ConfigurationDao _configDao;
        @Mock
        HostDao _hostDao;
        @Mock
        UserDao _userDao;
        @Mock
        UserVmDao _vmDao;
        @Mock
        ItWorkDao _workDao;
        @Mock
        VMInstanceDao _vmInstanceDao;
        @Mock
        VMTemplateDao _templateDao;
        @Mock
        VolumeDao _volsDao;
        @Mock
        RestoreVMCmd _restoreVMCmd;
        @Mock
        AccountVO _accountMock;
        @Mock
        UserVO _userMock;
        @Mock
        UserVmVO _vmMock;
        @Mock
        VMInstanceVO _vmInstance;
        @Mock
        HostVO _host;
        @Mock
        VMTemplateVO _templateMock;
        @Mock
        VolumeVO _volumeMock;
        @Mock
        List<VolumeVO> _rootVols;
        @Mock
        ItWorkVO _work;
        @Before
        public void setup(){
            MockitoAnnotations.initMocks(this);

            _vmMgr._templateDao = _templateDao;
            _vmMgr._volsDao = _volsDao;
            _vmMgr.volumeMgr = _storageMgr;
            _vmMgr._accountDao = _accountDao;
            _vmMgr._userDao = _userDao;
            _vmMgr._accountMgr = _accountMgr;
            _vmMgr._configMgr = _configMgr;
            _vmMgr._capacityMgr = _capacityMgr;
            _vmMgr._hostDao = _hostDao;
            _vmMgr._nodeId = 1L;
            _vmMgr._workDao = _workDao;
            _vmMgr._agentMgr = _agentMgr;

            when(_vmMock.getId()).thenReturn(314l);
            when(_vmInstance.getId()).thenReturn(1L);
            when(_vmInstance.getServiceOfferingId()).thenReturn(2L);
            when(_vmInstance.getInstanceName()).thenReturn("myVm");
            when(_vmInstance.getHostId()).thenReturn(2L);
            when(_vmInstance.getType()).thenReturn(VirtualMachine.Type.User);
            when(_host.getId()).thenReturn(1L);
            when(_hostDao.findById(anyLong())).thenReturn(null);
            when(_configMgr.getServiceOffering(anyLong())).thenReturn(getSvcoffering(512));
            when(_workDao.persist(_work)).thenReturn(_work);
            when(_workDao.update("1", _work)).thenReturn(true);
            when(_work.getId()).thenReturn("1");
            doNothing().when(_work).setStep(ItWorkVO.Step.Done);
            //doNothing().when(_volsDao).detachVolume(anyLong());
            //when(_work.setStep(ItWorkVO.Step.Done)).thenReturn("1");

        }


    @Test(expected=CloudRuntimeException.class)
    public void testScaleVM1()  throws Exception {


        DeployDestination dest = new DeployDestination(null, null, null, _host);
        long l = 1L;

        when(_vmInstanceDao.findById(anyLong())).thenReturn(_vmInstance);
        _vmMgr.migrateForScale(_vmInstance, l, dest, l);

    }

    @Test (expected=CloudRuntimeException.class)
    public void testScaleVM2()  throws Exception {

        DeployDestination dest = new DeployDestination(null, null, null, _host);
        long l = 1L;

        when(_vmInstanceDao.findById(anyLong())).thenReturn(_vmInstance);
        ServiceOfferingVO newServiceOffering = getSvcoffering(512);
        ScaleVmCommand reconfigureCmd = new ScaleVmCommand("myVmName", newServiceOffering.getCpu(),
                newServiceOffering.getSpeed(), newServiceOffering.getRamSize(), newServiceOffering.getRamSize(), newServiceOffering.getLimitCpuUse());
        Answer answer = new ScaleVmAnswer(reconfigureCmd, true, "details");
        when(_agentMgr.send(2l, reconfigureCmd)).thenReturn(null);
        _vmMgr.reConfigureVm(_vmInstance, getSvcoffering(256), false);

    }

    @Test (expected=CloudRuntimeException.class)
    public void testScaleVM3()  throws Exception {

        /*VirtualMachineProfile<VMInstanceVO> profile = new VirtualMachineProfileImpl<VMInstanceVO>(vm);

        Long srcHostId = vm.getHostId();
        Long oldSvcOfferingId = vm.getServiceOfferingId();
        if (srcHostId == null) {
            throw new CloudRuntimeException("Unable to scale the vm because it doesn't have a host id");
        }*/

        when(_vmInstance.getHostId()).thenReturn(null);
        when(_vmInstanceDao.findById(anyLong())).thenReturn(_vmInstance);
        _vmMgr.findHostAndMigrate(VirtualMachine.Type.User, _vmInstance, 2l);

    }


    private ServiceOfferingVO getSvcoffering(int ramSize){

        long id  = 4L;
        String name = "name";
        String displayText = "displayText";
        int cpu = 1;
        //int ramSize = 256;
        int speed = 128;

        boolean ha = false;
        boolean useLocalStorage = false;

        ServiceOfferingVO serviceOffering = new ServiceOfferingVO(name, cpu, ramSize, speed, null, null, ha, displayText, useLocalStorage, false, null, false, null, false);
        return serviceOffering;
    }


}
