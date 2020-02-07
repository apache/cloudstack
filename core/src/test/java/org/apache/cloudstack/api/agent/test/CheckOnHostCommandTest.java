//
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
//

package org.apache.cloudstack.api.agent.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

import com.cloud.agent.api.CheckOnHostCommand;
import com.cloud.agent.api.to.HostTO;
import com.cloud.host.Host;
import com.cloud.host.Status;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.resource.ResourceState;

public class CheckOnHostCommandTest {
    public Host host = new Host() {
        @Override
        public PartitionType partitionType() {
            return PartitionType.Host;
        }

        @Override
        public Status getState() {
            return Status.Up;
        };

        @Override
        public long getId() {
            return 101L;
        };

        @Override
        public String getUuid() {
            return "101";
        }

        @Override
        public String getName() {
            return "hostName";
        };

        @Override
        public Type getType() {
            return Host.Type.Storage;
        };

        @Override
        public Date getCreated() {
            Date date = null;
            try {
                date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse("01/01/1970 12:12:12");
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return date;
        }

        @Override
        public Status getStatus() {
            return Status.Up;
        };

        @Override
        public String getPrivateIpAddress() {
            return "10.1.1.1";
        };

        public String getStorageUrl() {
            return null;
        }

        public String getStorageIpAddress() {
            return "10.1.1.2";
        };

        @Override
        public String getGuid() {
            return "bed9f83e-cac3-11e1-ac8a-0050568b007e";
        };

        @Override
        public Long getTotalMemory() {
            return 100000000000L;
        }

        @Override
        public Integer getCpuSockets() {
            return 1;
        };

        @Override
        public Integer getCpus() {
            return 16;
        };

        @Override
        public Long getSpeed() {
            return 2000000000L;
        };

        @Override
        public Integer getProxyPort() {
            return 22;
        };

        @Override
        public Long getPodId() {
            return 16L;
        };

        @Override
        public long getDataCenterId() {
            return 17L;
        };

        @Override
        public String getParent() {
            return "parent";
        };

        @Override
        public String getStorageIpAddressDeux() {
            return "10.1.1.3";
        };

        @Override
        public HypervisorType getHypervisorType() {
            return HypervisorType.XenServer;
        };

        @Override
        public Date getDisconnectedOn() {
            Date date = null;
            try {
                date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse("01/01/2012 12:12:12");
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return date;
        }

        @Override
        public String getVersion() {
            return "4.0.1";
        };

        @Override
        public long getTotalSize() {
            return 100000000000L;
        };

        @Override
        public String getCapabilities() {
            return "capabilities";
        };

        @Override
        public long getLastPinged() {
            return 1L;
        };

        @Override
        public Long getManagementServerId() {
            return 2L;
        };

        @Override
        public Date getRemoved() {
            Date date = null;
            try {
                date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse("02/01/2012 12:12:12");
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return date;
        };

        @Override
        public Long getClusterId() {
            return 3L;
        }

        @Override
        public ResourceType resourceType() {
            return ResourceType.Host;
        }

        @Override
        public String getPublicIpAddress() {
            return "10.1.1.4";
        };

        @Override
        public String getPublicNetmask() {
            return "255.255.255.8";
        };

        @Override
        public String getPrivateNetmask() {
            return "255.255.255.16";
        };

        @Override
        public String getStorageNetmask() {
            return "255.255.255.24";
        };

        @Override
        public String getStorageMacAddress() {
            return "01:f4:17:38:0e:26";
        };

        @Override
        public String getPublicMacAddress() {
            return "02:f4:17:38:0e:26";
        };

        @Override
        public String getPrivateMacAddress() {
            return "03:f4:17:38:0e:26";
        };

        @Override
        public String getStorageNetmaskDeux() {
            return "255.255.255.25";
        };

        @Override
        public String getStorageMacAddressDeux() {
            return "01:f4:17:38:0e:27";
        };

        @Override
        public String getHypervisorVersion() {
            return "1.2.3.0";
        };

        @Override
        public boolean isInMaintenanceStates() {
            return false;
        };

        @Override
        public boolean isDisabled() {
            return false;
        };

        @Override
        public ResourceState getResourceState() {
            return ResourceState.Enabled;
        };
    };

    CheckOnHostCommand cohc = new CheckOnHostCommand(host);

    @Test
    public void testGetHost() {
        HostTO h = cohc.getHost();
        assertNotNull(h);
    }

    @Test
    public void testGetState() {
        Status s = host.getState();
        assertTrue(s == Status.Up);
    }

    @Test
    public void testGetId() {
        Long id = host.getId();
        assertTrue(101L == id);
    }

    @Test
    public void testGetName() {
        String name = host.getName();
        assertTrue(name.equals("hostName"));
    }

    @Test
    public void testGetType() {
        Host.Type t = host.getType();
        assertTrue(t == Host.Type.Storage);
    }

    @Test
    public void testGetCreated() {
        try {
            Date date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse("01/01/1970 12:12:12");
            Date d = host.getCreated();
            assertTrue(d.compareTo(date) == 0);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetStatus() {
        Status s = host.getStatus();
        assertTrue(s == Status.Up);
    }

    @Test
    public void testGetPrivateIpAddress() {
        String addr = host.getPrivateIpAddress();
        assertTrue(addr.equals("10.1.1.1"));
    }

    @Test
    public void testGetStorageIpAddress() {
        String addr = host.getStorageIpAddress();
        assertTrue(addr.equals("10.1.1.2"));
    }

    @Test
    public void testGetGuid() {
        String guid = host.getGuid();
        assertTrue(guid.equals("bed9f83e-cac3-11e1-ac8a-0050568b007e"));
    }

    @Test
    public void testGetTotalMemory() {
        Long m = host.getTotalMemory();
        assertTrue(m == 100000000000L);
    }

    @Test
    public void testGetCpuSockets() {
        Integer cpuSockets = host.getCpuSockets();
        assertTrue(cpuSockets == 1);
    }

    @Test
    public void testGetCpus() {
        int cpus = host.getCpus();
        assertTrue(cpus == 16);
    }

    @Test
    public void testGetSpeed() {
        Long spped = host.getSpeed();
        assertTrue(spped == 2000000000L);
    }

    @Test
    public void testGetProxyPort() {
        Integer port = host.getProxyPort();
        assertTrue(port == 22);
    }

    @Test
    public void testGetPodId() {
        Long pID = host.getPodId();
        assertTrue(pID == 16L);
    }

    @Test
    public void testGetDataCenterId() {
        long dcID = host.getDataCenterId();
        assertTrue(dcID == 17L);
    }

    @Test
    public void testGetParent() {
        String p = host.getParent();
        assertTrue(p.equals("parent"));
    }

    @Test
    public void testGetStorageIpAddressDeux() {
        String addr = host.getStorageIpAddressDeux();
        assertTrue(addr.equals("10.1.1.3"));
    }

    @Test
    public void testGetHypervisorType() {
        HypervisorType type = host.getHypervisorType();
        assertTrue(type == HypervisorType.XenServer);
    }

    @Test
    public void testGetDisconnectedOn() {
        try {
            Date date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse("01/01/2012 12:12:12");
            Date d = host.getDisconnectedOn();
            assertTrue(d.compareTo(date) == 0);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetVersion() {
        String v = host.getVersion();
        assertTrue(v.equals("4.0.1"));
    }

    @Test
    public void testGetTotalSize() {
        long size = host.getTotalSize();
        assertTrue(size == 100000000000L);
    }

    @Test
    public void testGetCapabilities() {
        String c = host.getCapabilities();
        assertTrue(c.equals("capabilities"));
    }

    @Test
    public void testGetLastPinged() {
        long lp = host.getLastPinged();
        assertTrue(lp == 1L);
    }

    @Test
    public void testGetManagementServerId() {
        Long msID = host.getManagementServerId();
        assertTrue(msID == 2L);
    }

    @Test
    public void testGetRemoved() {
        try {
            Date date = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").parse("02/01/2012 12:12:12");
            Date d = host.getRemoved();
            assertTrue(d.compareTo(date) == 0);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetClusterId() {
        Long cID = host.getClusterId();
        assertTrue(cID == 3L);
    }

    @Test
    public void testGetPublicIpAddress() {
        String pipAddr = host.getPublicIpAddress();
        assertTrue(pipAddr.equals("10.1.1.4"));
    }

    @Test
    public void testGetPublicNetmask() {
        String pMask = host.getPublicNetmask();
        assertTrue(pMask.equals("255.255.255.8"));
    }

    @Test
    public void testGetPrivateNetmask() {
        String pMask = host.getPrivateNetmask();
        assertTrue(pMask.equals("255.255.255.16"));
    }

    @Test
    public void testGetStorageNetmask() {
        String sMask = host.getStorageNetmask();
        assertTrue(sMask.equals("255.255.255.24"));
    }

    @Test
    public void testGetStorageMacAddress() {
        String sMac = host.getStorageMacAddress();
        assertTrue(sMac.equals("01:f4:17:38:0e:26"));
    }

    @Test
    public void testGetPublicMacAddress() {
        String pMac = host.getPublicMacAddress();
        assertTrue(pMac.equals("02:f4:17:38:0e:26"));
    }

    @Test
    public void testGetPrivateMacAddress() {
        String pMac = host.getPrivateMacAddress();
        assertTrue(pMac.equals("03:f4:17:38:0e:26"));
    }

    @Test
    public void testGetStorageNetmaskDeux() {
        String sMask = host.getStorageNetmaskDeux();
        assertTrue(sMask.equals("255.255.255.25"));
    }

    @Test
    public void testGetStorageMacAddressDeux() {
        String sMac = host.getStorageMacAddressDeux();
        assertTrue(sMac.equals("01:f4:17:38:0e:27"));
    }

    @Test
    public void testGetHypervisorVersion() {
        String v = host.getHypervisorVersion();
        assertTrue(v.equals("1.2.3.0"));
    }

    @Test
    public void testIsInMaintenanceStates() {
        boolean b = host.isInMaintenanceStates();
        assertFalse(b);
    }

    @Test
    public void testGetResourceState() {
        ResourceState r = host.getResourceState();
        assertTrue(r == ResourceState.Enabled);
    }

    @Test
    public void testGetWait() {
        int wait = cohc.getWait();
        assertTrue(20 == wait);
    }

    @Test
    public void testExecuteInSequence() {
        boolean b = cohc.executeInSequence();
        assertFalse(b);
    }
}
