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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import com.cloud.api.ApiSerializerHelper;
import com.cloud.async.AsyncJob;
import com.cloud.async.AsyncJobConstants;
import com.cloud.async.AsyncJobDispatcher;
import com.cloud.async.AsyncJobManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserContext;
import com.cloud.user.dao.AccountDao;
import com.cloud.utils.component.AdapterBase;
import com.cloud.vm.dao.VMInstanceDao;

public class VmWorkJobDispatcher extends AdapterBase implements AsyncJobDispatcher {
    private static final Logger s_logger = Logger.getLogger(VmWorkJobDispatcher.class);

	@Inject private VirtualMachineManager _vmMgr;
	@Inject private AsyncJobManager _asyncJobMgr;
    @Inject private AccountDao _accountDao;
    @Inject private VMInstanceDao _instanceDao;
    
    private Map<String, Method> _handlerMap = new HashMap<String, Method>();
	
	@Override
	public void RunJob(AsyncJob job) {
        try {
        	String cmd = job.getCmd();
        	assert(cmd != null);
        	
        	VmWork work = (VmWork)ApiSerializerHelper.fromSerializedString(job.getCmdInfo());
        	assert(work != null);
        	
            AccountVO account = _accountDao.findById(work.getAccountId());
            assert(account != null);
            
            VMInstanceVO vm = _instanceDao.findById(work.getVmId());
            assert(vm != null);
    
            //
            // Due to legcy massive generic usage in VirtualMachineManagerImpl, we can't dispatch job handling
            // directly to VirtualMachineManagerImpl, since most handling method are generic method.
            //
            // to solve the problem, we have to go through an instantiated VirtualMachineGuru so that it can carry
            // down correct type back to VirtualMachineManagerImpl. It is sad that we have to write code like this
            //
            VirtualMachineGuru<VMInstanceVO> guru = _vmMgr.getVmGuru(vm);
            
            UserContext.registerContext(work.getUserId(), account, null, false);
            try {
            	Method handler = getHandler(guru, cmd);
            	if(handler != null) {
        			handler.invoke(guru, work);
            		_asyncJobMgr.completeAsyncJob(job.getId(), AsyncJobConstants.STATUS_SUCCEEDED, 0, null);
            	} else {
            		_asyncJobMgr.completeAsyncJob(job.getId(), AsyncJobConstants.STATUS_FAILED, 0, null);
            	}
            } finally {
                UserContext.unregisterContext();
            }
        } catch(Throwable e) {
            _asyncJobMgr.completeAsyncJob(job.getId(), AsyncJobConstants.STATUS_FAILED, 0, null);
        }
	}
	
	private Method getHandler(VirtualMachineGuru<?> guru, String cmd) {
		
		synchronized(_handlerMap) {
			Class<?> clz = guru.getClass();
			String key = clz.getCanonicalName() + cmd;
			Method method = _handlerMap.get(key);
			if(method != null)
				return method;
			
			try {
				method = clz.getMethod(cmd, VmWork.class);
				method.setAccessible(true);
			} catch (SecurityException e) {
				assert(false);
				s_logger.error("Unexpected exception", e);
				return null;
			} catch (NoSuchMethodException e) {
				assert(false);
				s_logger.error("Unexpected exception", e);
				return null;
			}
			
			_handlerMap.put(key, method);
			return method;
		}
	}
}
