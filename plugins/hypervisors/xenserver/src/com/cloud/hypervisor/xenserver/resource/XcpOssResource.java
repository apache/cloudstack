// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.cloud.hypervisor.xenserver.resource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Local;

import org.apache.xmlrpc.XmlRpcException;

import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;

@Local(value = ServerResource.class)
public class XcpOssResource extends CitrixResourceBase {

    private static final long mem_32m = 33554432L;

    @Override
    protected List<File> getPatchFiles() {
        final List<File> files = new ArrayList<File>();
        final String patch = "scripts/vm/hypervisor/xenserver/xcposs/patch";
        final String patchfilePath = Script.findScript("", patch);
        if (patchfilePath == null) {
            throw new CloudRuntimeException("Unable to find patch file "
                    + patch);
        }
        final File file = new File(patchfilePath);
        files.add(file);
        return files;
    }

    @Override
    protected String getGuestOsType(final String stdType,
            final String platformEmulator, final boolean bootFromCD) {
        if (stdType.equalsIgnoreCase("Debian GNU/Linux 6(64-bit)")) {
            return "Debian Squeeze 6.0 (64-bit)";
        } else if (stdType.equalsIgnoreCase("CentOS 5.6 (64-bit)")) {
            return "CentOS 5 (64-bit)";
        } else {
            return super.getGuestOsType(stdType, platformEmulator, bootFromCD);
        }
    }

    @Override
    protected void setMemory(final Connection conn, final VM vm, long minMemsize, final long maxMemsize) throws XmlRpcException, XenAPIException {
        vm.setMemoryLimits(conn, mem_32m, maxMemsize, minMemsize, maxMemsize);
    }
}
