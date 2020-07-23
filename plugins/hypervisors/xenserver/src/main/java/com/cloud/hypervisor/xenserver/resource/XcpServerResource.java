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
package com.cloud.hypervisor.xenserver.resource;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VM;

import static com.cloud.utils.NumbersUtil.toHumanReadableSize;

public class XcpServerResource extends CitrixResourceBase {

    private final static Logger s_logger = Logger.getLogger(XcpServerResource.class);
    private final static long mem_32m = 33554432L;

    @Override
    protected String getPatchFilePath() {
        return "scripts/vm/hypervisor/xenserver/xcpserver/patch";
    }

    /**
     XCP provides four memory configuration fields through which
     administrators can control this behaviour:

     * static-min
     * dynamic-min
     * dynamic-max
     * static-max

     The fields static-{min,max} act as *hard* lower and upper
     bounds for a guest's memory. For a running guest:
     * it's not possible to assign the guest more memory than
     static-max without first shutting down the guest.
     * it's not possible to assign the guest less memory than
     static-min without first shutting down the guest.

     The fields dynamic-{min,max} act as *soft* lower and upper
     bounds for a guest's memory. It's possible to change these
     fields even when a guest is running.

     The dynamic range must lie wholly within the static range. To
     put it another way, XCP at all times ensures that:

     static-min <= dynamic-min <= dynamic-max <= static-max

     At all times, XCP will attempt to keep a guest's memory usage
     between dynamic-min and dynamic-max.

     If dynamic-min = dynamic-max, then XCP will attempt to keep
     a guest's memory allocation at a constant size.

     If dynamic-min < dynamic-max, then XCP will attempt to give
     the guest as much memory as possible, while keeping the guest
     within dynamic-min and dynamic-max.

     If there is enough memory on a given host to give all resident
     guests dynamic-max, then XCP will attempt do so.

     If there is not enough memory to give all guests dynamic-max,
     then XCP will ask each of the guests (on that host) to use
     an amount of memory that is the same *proportional* distance
     between dynamic-min and dynamic-max.

     XCP will refuse to start guests if starting those guests would
     cause the sum of all the dynamic-min values to exceed the total
     host memory (taking into account various memory overheads).

     cf: https://wiki.xenserver.org/index.php?title=XCP_FAQ_Dynamic_Memory_Control
     */
    @Override
    protected void setMemory(final Connection conn, final VM vm, final long minMemsize, final long maxMemsize) throws XmlRpcException, XenAPIException {
        //setMemoryLimits(staticMin, staticMax, dynamicMin, dynamicMax)
        if (s_logger.isDebugEnabled()) {
           s_logger.debug("Memory Limits for VM [" + vm.getNameLabel(conn) + "[staticMin:" + toHumanReadableSize(mem_32m) + ", staticMax:" + toHumanReadableSize(maxMemsize) + ", dynamicMin: " + toHumanReadableSize(minMemsize) +
                    ", dynamicMax:" + toHumanReadableSize(maxMemsize) + "]]");
        }
        vm.setMemoryLimits(conn, mem_32m, maxMemsize, minMemsize, maxMemsize);
    }

    @Override
    public boolean isDmcEnabled(final Connection conn, final Host host) {
        //Dynamic Memory Control (DMC) is a technology provided by Xen Cloud Platform (XCP), starting from the 0.5 release
        //For the supported XCPs dmc is default enabled, XCP 1.0.0, 1.1.0, 1.4.x, 1.5 beta, 1.6.x;
        return true;
    }
}
