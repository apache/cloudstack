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
package com.cloud.hypervisor.vmware.mo;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.LocalizableMessage;
import com.vmware.vim25.LocalizedMethodFault;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.TaskInfo;
import com.vmware.vim25.TaskInfoState;

public class TaskMO extends BaseMO {
    public TaskMO(VmwareContext context, ManagedObjectReference morTask) {
        super(context, morTask);
    }

    public TaskMO(VmwareContext context, String morType, String morValue) {
        super(context, morType, morValue);
    }

    public TaskInfo getTaskInfo() throws Exception {
		return (TaskInfo)getContext().getVimClient().getDynamicProperty(_mor, "info");
    }

    public void setTaskDescription(LocalizableMessage description) throws Exception {
    	_context.getService().setTaskDescription(_mor, description);
    }

    public void setTaskState(TaskInfoState state, Object result, LocalizedMethodFault fault) throws Exception {
    	_context.getService().setTaskState(_mor, state, result, fault);
    }

    public void updateProgress(int percentDone) throws Exception {
    	_context.getService().updateProgress(_mor, percentDone);
    }

    public void cancelTask() throws Exception {
    	_context.getService().cancelTask(_mor);
    }

    public static String getTaskFailureInfo(VmwareContext context, ManagedObjectReference morTask) {
    	StringBuffer sb = new StringBuffer();

    	try {
    		TaskInfo info = (TaskInfo)context.getVimClient().getDynamicProperty(morTask, "info");
    		if(info != null) {
    			LocalizedMethodFault fault = info.getError();
    			if(fault != null) {
    				sb.append(fault.getLocalizedMessage()).append(" ");

    				if(fault.getFault() != null)
    					sb.append(fault.getFault().getClass().getName());
    			}
    		}
    	} catch(Exception e) {
    	}

    	return sb.toString();
    }
}
