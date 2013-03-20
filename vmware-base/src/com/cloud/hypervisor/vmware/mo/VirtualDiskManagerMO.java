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

import org.apache.log4j.Logger;

import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.HostDiskDimensionsChs;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualDiskSpec;

public class VirtualDiskManagerMO extends BaseMO {
    private static final Logger s_logger = Logger.getLogger(VirtualDiskManagerMO.class);

	public VirtualDiskManagerMO(VmwareContext context, ManagedObjectReference morDiskMgr) {
		super(context, morDiskMgr);
	}

	public VirtualDiskManagerMO(VmwareContext context, String morType, String morValue) {
		super(context, morType, morValue);
	}

    public void copyVirtualDisk(String srcName, ManagedObjectReference morSrcDc,
    	String destName, ManagedObjectReference morDestDc, VirtualDiskSpec diskSpec,
    	boolean force) throws Exception {

    	ManagedObjectReference morTask = _context.getService().copyVirtualDiskTask(_mor, srcName, morSrcDc, destName, morDestDc, diskSpec, force);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(!result)
			throw new Exception("Unable to copy virtual disk " + srcName + " to " + destName
				+ " due to " + TaskMO.getTaskFailureInfo(_context, morTask));

		_context.waitForTaskProgressDone(morTask);
    }

    public void createVirtualDisk(String name, ManagedObjectReference morDc, VirtualDiskSpec diskSpec) throws Exception {
    	ManagedObjectReference morTask = _context.getService().createVirtualDiskTask(_mor, name, morDc, diskSpec);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(!result)
			throw new Exception("Unable to create virtual disk " + name
				+ " due to " + TaskMO.getTaskFailureInfo(_context, morTask));

		_context.waitForTaskProgressDone(morTask);
    }

    public void defragmentVirtualDisk(String name, ManagedObjectReference morDc) throws Exception {
    	ManagedObjectReference morTask = _context.getService().defragmentVirtualDiskTask(_mor, name, morDc);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(!result)
			throw new Exception("Unable to defragment virtual disk " + name + " due to " + result);

		_context.waitForTaskProgressDone(morTask);
    }

    public void deleteVirtualDisk(String name, ManagedObjectReference morDc) throws Exception {
    	ManagedObjectReference morTask = _context.getService().deleteVirtualDiskTask(_mor, name, morDc);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(!result)
			throw new Exception("Unable to delete virtual disk " + name + " due to " + TaskMO.getTaskFailureInfo(_context, morTask));

		_context.waitForTaskProgressDone(morTask);
    }

    public void eagerZeroVirtualDisk(String name, ManagedObjectReference morDc) throws Exception {
    	ManagedObjectReference morTask = _context.getService().eagerZeroVirtualDiskTask(_mor, name, morDc);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(!result)
			throw new Exception("Unable to eager zero virtual disk " + name + " due to " + TaskMO.getTaskFailureInfo(_context, morTask));

		_context.waitForTaskProgressDone(morTask);
    }

    public void extendVirtualDisk(String name, ManagedObjectReference morDc, long newCapacityKb, boolean eagerZero) throws Exception {
    	ManagedObjectReference morTask = _context.getService().extendVirtualDiskTask(_mor, name, morDc, newCapacityKb, eagerZero);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(!result)
			throw new Exception("Unable to extend virtual disk " + name + " due to " + TaskMO.getTaskFailureInfo(_context, morTask));

		_context.waitForTaskProgressDone(morTask);
    }

    public void inflateVirtualDisk(String name, ManagedObjectReference morDc) throws Exception {
    	ManagedObjectReference morTask = _context.getService().inflateVirtualDiskTask(_mor, name, morDc);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(!result)
			throw new Exception("Unable to inflate virtual disk " + name + " due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		_context.waitForTaskProgressDone(morTask);
    }

    public void shrinkVirtualDisk(String name, ManagedObjectReference morDc, boolean copy) throws Exception {
    	ManagedObjectReference morTask = _context.getService().shrinkVirtualDiskTask(_mor, name, morDc, copy);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(!result)
			throw new Exception("Unable to shrink virtual disk " + name + " due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		_context.waitForTaskProgressDone(morTask);
    }

    public void zeroFillVirtualDisk(String name, ManagedObjectReference morDc) throws Exception {
    	ManagedObjectReference morTask = _context.getService().zeroFillVirtualDiskTask(_mor, name, morDc);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(!result)
			throw new Exception("Unable to zero fill virtual disk " + name + " due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		_context.waitForTaskProgressDone(morTask);
    }

    public void moveVirtualDisk(String srcName, ManagedObjectReference morSrcDc,
    	String destName, ManagedObjectReference morDestDc, boolean force) throws Exception {

    	ManagedObjectReference morTask = _context.getService().moveVirtualDiskTask(_mor, srcName, morSrcDc,
    		destName, morDestDc, force);

		boolean result = _context.getVimClient().waitForTask(morTask);
		if(!result)
			throw new Exception("Unable to move virtual disk " + srcName + " to " + destName
				+ " due to " + TaskMO.getTaskFailureInfo(_context, morTask));
		_context.waitForTaskProgressDone(morTask);
    }

    public int queryVirtualDiskFragmentation(String name, ManagedObjectReference morDc) throws Exception {
    	return _context.getService().queryVirtualDiskFragmentation(_mor, name, morDc);
    }

    public HostDiskDimensionsChs queryVirtualDiskGeometry(String name, ManagedObjectReference morDc) throws Exception {
    	return _context.getService().queryVirtualDiskGeometry(_mor, name, morDc);
    }

    public String queryVirtualDiskUuid(String name, ManagedObjectReference morDc) throws Exception {
    	return _context.getService().queryVirtualDiskUuid(_mor, name, morDc);
    }

    public void setVirtualDiskUuid(String name, ManagedObjectReference morDc, String uuid) throws Exception {
    	_context.getService().setVirtualDiskUuid(_mor, name, morDc, uuid);
    }
}
