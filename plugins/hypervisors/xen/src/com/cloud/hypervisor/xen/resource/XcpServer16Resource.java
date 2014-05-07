/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.cloud.hypervisor.xen.resource;

import org.apache.log4j.Logger;

public class XcpServer16Resource extends XcpServerResource {
    private final static Logger s_logger = Logger.getLogger(XcpServer16Resource.class);
    public XcpServer16Resource() {
        super();
    }

    @Override
    protected String getGuestOsType(String stdType, String platformEmulator, boolean bootFromCD) {
        if (platformEmulator == null) {
            s_logger.debug("Can't find the guest os: " + stdType + " mapping into XCP's guestOS type, start it as HVM guest");
            platformEmulator = "Other install media";
        }
        return platformEmulator;
    }
}
