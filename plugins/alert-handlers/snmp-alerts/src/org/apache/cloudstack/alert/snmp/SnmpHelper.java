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
// under the License

package org.apache.cloudstack.alert.snmp;

import com.cloud.utils.exception.CloudRuntimeException;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;

public class SnmpHelper {
    private Snmp _snmp;
    private CommunityTarget _target;

    public SnmpHelper(String address, String community) {
        _target = new CommunityTarget();
        _target.setCommunity(new OctetString(community));
        _target.setVersion(SnmpConstants.version2c);
        _target.setAddress(new UdpAddress(address));
        try {
            _snmp = new Snmp(new DefaultUdpTransportMapping());
        } catch (IOException e) {
            _snmp = null;
            throw new CloudRuntimeException(" Error in crearting snmp object, " + e.getMessage());
        }
    }

    public void sendSnmpTrap(SnmpTrapInfo snmpTrapInfo) {
        try {
            if (_snmp != null) {
                _snmp.send(createPDU(snmpTrapInfo), _target, null, null);
            }
        } catch (IOException e) {
            throw new CloudRuntimeException(" Error in sending SNMP Trap, " + e.getMessage());
        }
    }

    private PDU createPDU(SnmpTrapInfo snmpTrapInfo) {
        PDU trap = new PDU();
        trap.setType(PDU.TRAP);

        int alertType = snmpTrapInfo.getAlertType() + 1;
        if (alertType > 0) {
            trap.add(new VariableBinding(SnmpConstants.snmpTrapOID, getOID(CsSnmpConstants.TRAPS_PREFIX + alertType)));
            if (snmpTrapInfo.getDataCenterId() != 0) {
                trap.add(new VariableBinding(getOID(CsSnmpConstants.DATA_CENTER_ID),
                    new UnsignedInteger32(snmpTrapInfo.getDataCenterId())));
            }

            if (snmpTrapInfo.getPodId() != 0) {
                trap.add(new VariableBinding(getOID(CsSnmpConstants.POD_ID), new UnsignedInteger32(snmpTrapInfo
                    .getPodId())));
            }

            if (snmpTrapInfo.getClusterId() != 0) {
                trap.add(new VariableBinding(getOID(CsSnmpConstants.CLUSTER_ID), new UnsignedInteger32(snmpTrapInfo
                    .getClusterId())));
            }

            if (snmpTrapInfo.getMessage() != null) {
                trap.add(new VariableBinding(getOID(CsSnmpConstants.MESSAGE), new OctetString(snmpTrapInfo.getMessage
                    ())));
            } else {
                throw new CloudRuntimeException(" What is the use of alert without message ");
            }

            if (snmpTrapInfo.getGenerationTime() != null) {
                trap.add(new VariableBinding(getOID(CsSnmpConstants.GENERATION_TIME),
                    new OctetString(snmpTrapInfo.getGenerationTime().toString())));
            } else {
                trap.add(new VariableBinding(getOID(CsSnmpConstants.GENERATION_TIME)));
            }
        } else {
            throw new CloudRuntimeException(" Invalid alert Type ");
        }

        return trap;
    }

    private OID getOID(String oidString) {
        return new OID(oidString);
    }
}