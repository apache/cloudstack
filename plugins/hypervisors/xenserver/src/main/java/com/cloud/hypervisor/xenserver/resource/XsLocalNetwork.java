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
import com.xensource.xenapi.Network;
import com.xensource.xenapi.PIF;
import com.xensource.xenapi.Types.XenAPIException;

/**
 * XsNic represents a network and the host's specific PIF.
 */
public class XsLocalNetwork {

    private static final Logger s_logger = Logger.getLogger(XsLocalNetwork.class);

    private final CitrixResourceBase _citrixResourceBase;
    private final Network _n;
    private Network.Record _nr;
    private PIF _p;
    private PIF.Record _pr;

    public XsLocalNetwork(final CitrixResourceBase citrixResourceBase, final Network n) {
        this(citrixResourceBase, n, null, null, null);
    }

    public XsLocalNetwork(final CitrixResourceBase citrixResourceBase, final Network n, final Network.Record nr, final PIF p, final PIF.Record pr) {
        _citrixResourceBase = citrixResourceBase;
        _n = n;
        _nr = nr;
        _p = p;
        _pr = pr;
    }

    public Network getNetwork() {
        return _n;
    }

    public Network.Record getNetworkRecord(final Connection conn) throws XenAPIException, XmlRpcException {
        if (_nr == null) {
            _nr = _n.getRecord(conn);
        }

        return _nr;
    }

    public PIF getPif(final Connection conn) throws XenAPIException, XmlRpcException {
        if (_p == null) {
            final Network.Record nr = getNetworkRecord(conn);
            for (final PIF pif : nr.PIFs) {
                final PIF.Record pr = pif.getRecord(conn);
                if (_citrixResourceBase.getHost().getUuid().equals(pr.host.getUuid(conn))) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Found a network called " + nr.nameLabel + " on host=" + _citrixResourceBase.getHost().getIp() + ";  Network=" + nr.uuid + "; pif=" + pr.uuid);
                    }
                    _p = pif;
                    _pr = pr;
                    break;
                }
            }
        }
        return _p;
    }

    public PIF.Record getPifRecord(final Connection conn) throws XenAPIException, XmlRpcException {
        if (_pr == null) {
            final PIF p = getPif(conn);
            if (_pr == null) {
                _pr = p.getRecord(conn);
            }
        }
        return _pr;
    }
}
