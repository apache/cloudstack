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
import javax.ejb.Local;
import org.apache.log4j.Logger;
import com.cloud.resource.ServerResource;

@Local(value = ServerResource.class)
public class XenServer602Resource extends XenServer600Resource {
    private static final Logger s_logger = Logger.getLogger(XenServer602Resource.class);

    public XenServer602Resource() {
        super();
    }

    @Override
    protected String getGuestOsType(String stdType, boolean bootFromCD) {
        return CitrixHelper.getXenServer602GuestOsType(stdType, bootFromCD);
    }

    @Override
    public long getStaticMax(String os, boolean b, long dynamicMinRam, long dynamicMaxRam) {
        long recommendedValue = CitrixHelper.getXenServer602StaticMax(os, b);
        if (recommendedValue == 0) {
            s_logger.warn("No recommended value found for dynamic max, setting static max and dynamic max equal");
            return dynamicMaxRam;
        }
        long staticMax = Math.min(recommendedValue, 4l * dynamicMinRam);  // XS constraint for stability
        if (dynamicMaxRam > staticMax) { // XS contraint that dynamic max <= static max
            s_logger.warn("dynamixMax " + dynamicMaxRam + " cant be greater than static max " + staticMax +
                ", can lead to stability issues. Setting static max as much as dynamic max ");
            return dynamicMaxRam;
        }
        return staticMax;
    }

    @Override
    public long getStaticMin(String os, boolean b, long dynamicMinRam, long dynamicMaxRam) {
        long recommendedValue = CitrixHelper.getXenServer602StaticMin(os, b);
        if (recommendedValue == 0) {
            s_logger.warn("No recommended value found for dynamic min");
            return dynamicMinRam;
        }
        if (dynamicMinRam < recommendedValue) {   // XS contraint that dynamic min > static min
            s_logger.warn("Vm is set to dynamixMin " + dynamicMinRam + " less than the recommended static min " + recommendedValue + ", could lead to stability issues");
        }
        return dynamicMinRam;
    }

}
