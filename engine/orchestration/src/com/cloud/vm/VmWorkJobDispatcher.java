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

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.jobs.AsyncJob;
import org.apache.cloudstack.framework.jobs.AsyncJobDispatcher;
import org.apache.cloudstack.framework.jobs.AsyncJobManager;
import org.apache.cloudstack.framework.jobs.impl.JobSerializerHelper;
import org.apache.cloudstack.jobs.JobInfo;

import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.dao.VMInstanceDao;

public class VmWorkJobDispatcher extends AdapterBase implements AsyncJobDispatcher {
    private static final Logger s_logger = Logger.getLogger(VmWorkJobDispatcher.class);

    public static final String VM_WORK_QUEUE = "VmWorkJobQueue";
    public static final String VM_WORK_JOB_DISPATCHER = "VmWorkJobDispatcher";
    public static final String VM_WORK_JOB_WAKEUP_DISPATCHER = "VmWorkJobWakeupDispatcher";
    
    @Inject private VirtualMachineManagerImpl _vmMgr;
	@Inject private AsyncJobManager _asyncJobMgr;
    @Inject private VMInstanceDao _instanceDao;
    
	@Override
    public void runJob(AsyncJob job) {
        VmWork work = null;
        try {
        	String cmd = job.getCmd();
        	assert(cmd != null);
        	
        	if(s_logger.isDebugEnabled())
        		s_logger.debug("Run VM work job: " + cmd);
        	
        	Class<?> workClz = null;
        	try {
        		workClz = Class.forName(job.getCmd());
        	} catch(ClassNotFoundException e) {
        		s_logger.error("VM work class " + cmd + " is not found", e);
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.FAILED, 0, e.getMessage());
        		return;
        	}
        	
        	work = VmWorkSerializer.deserialize(workClz, job.getCmdInfo());
            assert(work != null);
            if(work == null) {
            	s_logger.error("Unable to deserialize VM work " + job.getCmd() + ", job info: " + job.getCmdInfo());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.FAILED, 0, "Unable to deserialize VM work");
        		return;
            }
            
            CallContext.register(work.getUserId(), work.getAccountId(), job.getRelated());

            VMInstanceVO vm = _instanceDao.findById(work.getVmId());
            if (vm == null) {
                s_logger.info("Unable to find vm " + work.getVmId());
            }
            assert(vm != null);
            if(work instanceof VmWorkStart) {
            	VmWorkStart workStart = (VmWorkStart)work;
            	_vmMgr.orchestrateStart(vm.getUuid(), workStart.getParams(), workStart.getPlan());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, null);
            } else if(work instanceof VmWorkStop) {
            	VmWorkStop workStop = (VmWorkStop)work;
            	_vmMgr.orchestrateStop(vm.getUuid(), workStop.isCleanup());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, null);
            } else if(work instanceof VmWorkMigrate) {
            	VmWorkMigrate workMigrate = (VmWorkMigrate)work;
            	_vmMgr.orchestrateMigrate(vm.getUuid(), workMigrate.getSrcHostId(), workMigrate.getDeployDestination());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, null);
            } else if(work instanceof VmWorkMigrateWithStorage) {
            	VmWorkMigrateWithStorage workMigrateWithStorage = (VmWorkMigrateWithStorage)work;
            	_vmMgr.orchestrateMigrateWithStorage(vm.getUuid(), 
            		workMigrateWithStorage.getSrcHostId(), 
            		workMigrateWithStorage.getDestHostId(), 
            		workMigrateWithStorage.getVolumeToPool());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, null);
            } else if(work instanceof VmWorkMigrateForScale) { 
            	VmWorkMigrateForScale workMigrateForScale = (VmWorkMigrateForScale)work;
            	_vmMgr.orchestrateMigrateForScale(vm.getUuid(), 
            		workMigrateForScale.getSrcHostId(), 
            		workMigrateForScale.getDeployDestination(), 
            		workMigrateForScale.getNewServiceOfferringId());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, null);
            } else if(work instanceof VmWorkReboot) { 
                VmWorkReboot workReboot = (VmWorkReboot)work;
                _vmMgr.orchestrateReboot(vm.getUuid(), workReboot.getParams());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, null);
            } else if(work instanceof VmWorkAddVmToNetwork) { 
                VmWorkAddVmToNetwork workAddVmToNetwork = (VmWorkAddVmToNetwork)work;
                NicProfile nic = _vmMgr.orchestrateAddVmToNetwork(vm, workAddVmToNetwork.getNetwork(), 
                	workAddVmToNetwork.getRequestedNicProfile());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, 
                    JobSerializerHelper.toObjectSerializedString(nic));
            } else if(work instanceof VmWorkRemoveNicFromVm) { 
                VmWorkRemoveNicFromVm workRemoveNicFromVm = (VmWorkRemoveNicFromVm)work;
                boolean result = _vmMgr.orchestrateRemoveNicFromVm(vm, workRemoveNicFromVm.getNic());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, 
                	JobSerializerHelper.toObjectSerializedString(new Boolean(result)));
            } else if(work instanceof VmWorkRemoveVmFromNetwork) { 
                VmWorkRemoveVmFromNetwork workRemoveVmFromNetwork = (VmWorkRemoveVmFromNetwork)work;
                boolean result = _vmMgr.orchestrateRemoveVmFromNetwork(vm, 
                	workRemoveVmFromNetwork.getNetwork(), workRemoveVmFromNetwork.getBroadcastUri());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, 
                    	JobSerializerHelper.toObjectSerializedString(new Boolean(result)));
            } else if(work instanceof VmWorkReconfigure) { 
                VmWorkReconfigure workReconfigure = (VmWorkReconfigure)work;
                _vmMgr.reConfigureVm(vm.getUuid(), workReconfigure.getNewServiceOffering(), 
                	workReconfigure.isSameHost());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, null);
            } else if(work instanceof VmWorkStorageMigration) { 
                VmWorkStorageMigration workStorageMigration = (VmWorkStorageMigration)work;
                _vmMgr.orchestrateStorageMigration(vm.getUuid(), workStorageMigration.getDestStoragePool());
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.SUCCEEDED, 0, null);
            } else {
            	assert(false);
                s_logger.error("Unhandled VM work command: " + job.getCmd());
            	
            	RuntimeException e = new RuntimeException("Unsupported VM work command: " + job.getCmd());
                String exceptionJson = JobSerializerHelper.toSerializedString(e);
                s_logger.error("Serialize exception object into json: " + exceptionJson);
                _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.FAILED, 0, exceptionJson);
            }
        } catch(Throwable e) {
            s_logger.error("Unable to complete " + job, e);
            
            String exceptionJson = JobSerializerHelper.toSerializedString(e);
            s_logger.info("Serialize exception object into json: " + exceptionJson);
            _asyncJobMgr.completeAsyncJob(job.getId(), JobInfo.Status.FAILED, 0, exceptionJson);
        } finally {
            CallContext.unregister();
        }
	}
}
