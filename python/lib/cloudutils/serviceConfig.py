# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
from .utilities import writeProgressBar, bash
from .cloudException import CloudRuntimeException, CloudInternalException, formatExceptionInfo
import logging
from .networkConfig import networkConfig
import re
from .configFileOps import configFileOps
import os
import shutil

# exit() error constants
Unknown = 0
CentOS6 = 1
CentOS7 = 2
CentOS8 = 3
Ubuntu = 4
RHEL6 = 5
RHEL7 = 6
RHEL8 = 7
distro = None

#=================== DISTRIBUTION DETECTION =================
if os.path.exists("/etc/centos-release"):
    version = open("/etc/centos-release").readline()
    if version.find("CentOS release 6") != -1:
      distro = CentOS6
    elif version.find("CentOS Linux release 7") != -1:
      distro = CentOS7
    elif version.find("CentOS Linux release 8") != -1:
      distro = CentOS8
elif os.path.exists("/etc/redhat-release"):
    version = open("/etc/redhat-release").readline()
    if version.find("Red Hat Enterprise Linux Server release 6") != -1:
      distro = RHEL6
    elif version.find("Red Hat Enterprise Linux Server 7") != -1:
      distro = RHEL7
    elif version.find("Red Hat Enterprise Linux Server 8") != -1:
      distro = RHEL8
elif os.path.exists("/etc/lsb-release") and "Ubuntu" in open("/etc/lsb-release").read(-1): distro = Ubuntu
else: distro = Unknown
#=================== DISTRIBUTION DETECTION =================

class serviceCfgBase(object):
    def __init__(self, syscfg):
        self.status = None
        self.serviceName = ""
        self.cfoHandlers = []
        self.syscfg = syscfg
        self.netMgrRunning = False

    def configration(self):
        writeProgressBar("Configure " + self.serviceName + " ...", None)
        result = False
        try:
            result = self.config()
            if result is None:
                result = False

            self.status = result
            writeProgressBar(None, result)
            return result
        except CloudRuntimeException as e:
            self.status = result
            writeProgressBar(None, result)
            logging.debug(e.getDetails())
            raise e
        except CloudInternalException as e:
            self.status = result
            writeProgressBar(None, result)
            raise e
        except:
            logging.debug(formatExceptionInfo())
            if self.syscfg.env.mode == "Server":
                raise CloudRuntimeException("Configure %s failed, Please check the /var/log/cloudstack/management/setupManagement.log for detail"%self.serviceName)
            else:
                raise CloudRuntimeException("Configure %s failed, Please check the /var/log/cloudstack/agent/setup.log for detail"%self.serviceName)

    def backup(self):
        if self.status is None:
            return True

        writeProgressBar("Restore " + self.serviceName + " ...", None)
        result = False
        try:
            for cfo in self.cfoHandlers:
                cfo.backup()

            result = self.restore()
        except (CloudRuntimeException, CloudInternalException) as e:
            logging.debug(e)

        writeProgressBar(None, result)

    def config(self):
        return True

    def restore(self):
        return True

class networkConfigBase:
    def __init__(self, syscfg):
        self.netcfg = networkConfig()
        self.serviceName = "Network"
        self.brName = None
        self.dev = None
        self.syscfg = syscfg

    def isPreConfiged(self):
        preCfged = False
        for br in self.syscfg.env.nics:
            if not self.netcfg.isNetworkDev(br):
                logging.debug("%s is not a network device, is it down?"%br)
                return False
            if self.syscfg.env.bridgeType == "openvswitch" and not self.netcfg.isOvsBridge(br):
                raise CloudInternalException("%s is not an openvswitch bridge" % br)
            if self.syscfg.env.bridgeType == "native" and not self.netcfg.isBridge(br) and not self.netcfg.isNetworkDev(br):
                # traffic label doesn't have to be a bridge, we'll create bridges on it
                raise CloudInternalException("%s is not a bridge and not a net device" % br)
            preCfged = True

        return preCfged

    def cfgNetwork(self, dev=None, brName=None):
        if dev is None:
            device = self.netcfg.getDefaultNetwork()
        else:
            device = self.netcfg.getDevInfo(dev)

        if device.type == "dev":
            if brName is None:
                brName = "cloudbr0"

            self.writeToCfgFile(brName, device)
        elif device.type == "brport":
            brName = self.netcfg.getBridge(dev)
            brDevice = self.netcfg.getDevInfo(brName)
            self.writeToCfgFile(brDevice.name, device)
        elif device.type == "bridge":
            #Fixme, assuming the outgoing physcial device is on port 1
            enslavedDev = self.netcfg.getEnslavedDev(device.name, 1)
            if enslavedDev is None:
                raise CloudInternalException("Failed to get enslaved devices on bridge:%s"%device.name)

            brDevice = device
            device = self.netcfg.getDevInfo(enslavedDev)
            brName = brDevice.name
            self.writeToCfgFile(brName, device)

        self.brName = brName
        self.dev = device.name

    def writeToCfgFile(self):
        pass

class networkConfigUbuntu(serviceCfgBase, networkConfigBase):
    def __init__(self, syscfg):
        super(networkConfigUbuntu, self).__init__(syscfg)
        networkConfigBase.__init__(self, syscfg)
        self.netCfgFile = "/etc/network/interfaces"

    def getNetworkMethod(self, line):
        if line.find("static") != -1:
            return "static"
        elif line.find("dhcp") != -1:
            return "dhcp"
        else:
            logging.debug("Failed to find the network method from:%s"%line)
            raise CloudInternalException("Failed to find the network method from /etc/network/interfaces")

    def addBridge(self, br, dev):
        bash("ifdown %s"%dev.name)
        for line in open(self.netCfgFile).readlines():
            match = re.match("^ *iface %s.*"%dev.name, line)
            if match is not None:
                dev.method = self.getNetworkMethod(match.group(0))
                cfo = configFileOps(self.netCfgFile, self)
                if self.syscfg.env.bridgeType == "openvswitch":
                    bridgeCfg = "\n".join(("",
                        "iface {device} inet manual",
                        "  ovs_type OVSPort",
                        "  ovs_bridge {bridge}",
                        "",
                        "auto {bridge}",
                        "allow-ovs {bridge}",
                        "iface {bridge} inet {device_method}",
                        "  ovs_type OVSBridge",
                        "  ovs_ports {device}",
                        "")).format(bridge=br, device=dev.name, device_method=dev.method)
                    cfo.replace_line("^ *auto %s.*" % dev.name,
                        "allow-{bridge} {device}".format(bridge=br, device=dev.name))
                elif self.syscfg.env.bridgeType == "native":
                    bridgeCfg = "\niface %s inet manual\n \
                                 auto %s\n \
                                 iface %s inet %s\n \
                                 bridge_ports %s\n"%(dev.name, br, br, dev.method, dev.name)
                else:
                    raise CloudInternalException("Unknown network.bridge.type %s" % self.syscfg.env.bridgeType)
                cfo.replace_line("^ *iface %s.*"%dev.name, bridgeCfg)

    def addDev(self, br, dev):
        logging.debug("Haven't implement yet")

    def addBridgeAndDev(self, br, dev):
        logging.debug("Haven't implement yet")

    def writeToCfgFile(self, br, dev):
        cfg = open(self.netCfgFile).read()
        ifaceDev = re.search("^ *iface %s.*"%dev.name, cfg, re.MULTILINE)
        ifaceBr = re.search("^ *iface %s.*"%br, cfg, re.MULTILINE)
        if ifaceDev is not None and ifaceBr is not None:
            logging.debug("%s:%s already configured"%(br, dev.name))
            return True
        elif ifaceDev is not None and ifaceBr is None:
            #reconfig bridge
            self.addBridge(br, dev)
        elif ifaceDev is None and ifaceBr is not None:
            #reconfig dev
            raise CloudInternalException("Missing device configuration, Need to add your network configuration into /etc/network/interfaces at first")
        else:
            raise CloudInternalException("Missing bridge/device network configuration, need to add your network configuration into /etc/network/interfaces at first")

    def config(self):
        try:
            if super(networkConfigUbuntu, self).isPreConfiged():
                return True

            self.netMgrRunning = self.syscfg.svo.isServiceRunning("network-manager")
            super(networkConfigUbuntu, self).cfgNetwork()
            if self.netMgrRunning:
                self.syscfg.svo.stopService("network-manager")
                self.syscfg.svo.disableService("network-manager")

            ifup_op = bash("ifup %s"%self.brName)
            if not ifup_op.isSuccess():
                raise CloudInternalException("Can't start network:%s %s" % (self.brName, ifup_op.getErrMsg()))

            self.syscfg.env.nics.append(self.brName)
            self.syscfg.env.nics.append(self.brName)
            self.syscfg.env.nics.append(self.brName)
            return True
        except:
            raise

    def restore(self):
        try:
            if self.netMgrRunning:
                self.syscfg.svo.enableService("network-manager")
                self.syscfg.svo.startService("network-manager")

            bash("/etc/init.d/networking stop")
            bash("/etc/init.d/networking start")
            return True
        except:
            logging.debug(formatExceptionInfo())
            return False

class networkConfigRedhat(serviceCfgBase, networkConfigBase):
    def __init__(self, syscfg):
        super(networkConfigRedhat, self).__init__(syscfg)
        networkConfigBase.__init__(self, syscfg)

    def writeToCfgFile(self, brName, dev):
        self.devCfgFile = "/etc/sysconfig/network-scripts/ifcfg-%s" % dev.name
        self.brCfgFile = "/etc/sysconfig/network-scripts/ifcfg-%s" % brName

        isDevExist = os.path.exists(self.devCfgFile)
        isBrExist = os.path.exists(self.brCfgFile)
        if isDevExist and isBrExist:
            logging.debug("%s:%s already configured"%(brName, dev.name))
            return True
        elif isDevExist and not isBrExist:
            #reconfig bridge
            self.addBridge(brName, dev)
        elif not isDevExist and isBrExist:
            #reconfig dev
            raise CloudInternalException("Missing device configuration, Need to add your network configuration into /etc/sysconfig/network-scripts at first")
        else:
            raise CloudInternalException("Missing bridge/device network configuration, need to add your network configuration into /etc/sysconfig/network-scripts at first")


    def addBridge(self, brName, dev):
        bash("ifdown %s" % dev.name)

        if not os.path.exists(self.brCfgFile):
            shutil.copy(self.devCfgFile, self.brCfgFile)

        #config device file at first: disable nm, set onboot=yes if not
        cfo = configFileOps(self.devCfgFile, self)
        cfo.addEntry("NM_CONTROLLED", "no")
        cfo.addEntry("ONBOOT", "yes")
        if self.syscfg.env.bridgeType == "openvswitch":
            if cfo.getEntry("IPADDR"):
                cfo.rmEntry("IPADDR", cfo.getEntry("IPADDR"))
            cfo.addEntry("DEVICETYPE", "ovs")
            cfo.addEntry("TYPE", "OVSPort")
            cfo.addEntry("OVS_BRIDGE", brName)
        elif self.syscfg.env.bridgeType == "native":
            cfo.addEntry("BRIDGE", brName)
        else:
            raise CloudInternalException("Unknown network.bridge.type %s" % self.syscfg.env.bridgeType)
        cfo.save()

        cfo = configFileOps(self.brCfgFile, self)
        cfo.addEntry("NM_CONTROLLED", "no")
        cfo.addEntry("ONBOOT", "yes")
        cfo.addEntry("DEVICE", brName)
        if self.syscfg.env.bridgeType == "openvswitch":
            if cfo.getEntry("HWADDR"):
                cfo.rmEntry("HWADDR", cfo.getEntry("HWADDR"))
            if cfo.getEntry("UUID"):
                cfo.rmEntry("UUID", cfo.getEntry("UUID"))
            cfo.addEntry("STP", "yes")
            cfo.addEntry("DEVICETYPE", "ovs")
            cfo.addEntry("TYPE", "OVSBridge")
        elif self.syscfg.env.bridgeType == "native":
            cfo.addEntry("TYPE", "Bridge")
        else:
            raise CloudInternalException("Unknown network.bridge.type %s" % self.syscfg.env.bridgeType)
        cfo.save()

    def config(self):
        try:
            if super(networkConfigRedhat, self).isPreConfiged():
                return True

            super(networkConfigRedhat, self).cfgNetwork()

            self.netMgrRunning = self.syscfg.svo.isServiceRunning("NetworkManager")
            if self.netMgrRunning:
                self.syscfg.svo.stopService("NetworkManager")
                self.syscfg.svo.disableService("NetworkManager")

            cfo = configFileOps("/etc/sysconfig/network", self)
            cfo.addEntry("NOZEROCONF", "yes")
            cfo.save()

            if not bash("service network restart").isSuccess():
                if not bash("systemctl restart NetworkManager.service").isSuccess():
                    raise CloudInternalException("Can't restart network")

            self.syscfg.env.nics.append(self.brName)
            self.syscfg.env.nics.append(self.brName)
            self.syscfg.env.nics.append(self.brName)
            return True
        except:
            raise

    def restore(self):
        try:
            if self.netMgrRunning:
                self.syscfg.svo.enableService("NetworkManager")
                self.syscfg.svo.startService("NetworkManager")
            bash("service network restart")
            return True
        except:
            logging.debug(formatExceptionInfo())
            return False

class cgroupConfig(serviceCfgBase):
    def __init__(self, syscfg):
        super(cgroupConfig, self).__init__(syscfg)
        self.serviceName = "Cgroup"

    def config(self):
        try:
            cfo = configFileOps("/etc/cgconfig.conf", self)
            addConfig = "group virt {\n \
                            cpu {\n \
                                cpu.shares = 9216;\n \
                            }\n \
                        }\n"
            cfo.add_lines(addConfig)

            self.syscfg.svo.stopService("cgconfig", True)
            self.syscfg.svo.enableService("cgconfig",forcestart=True)

            cfo = configFileOps("/etc/cgrules.conf", self)
            cfgline = "root:/usr/sbin/libvirtd  cpu virt/\n"
            cfo.add_lines(cfgline)

            self.syscfg.svo.stopService("cgred", True)
            if not self.syscfg.svo.enableService("cgred"):
                return False
            return True
        except:
            raise

    def restore(self):
        try:
            self.syscfg.svo.stopService("cgconfig")
            self.syscfg.svo.enableService("cgconfig",forcestart=True)
            self.syscfg.svo.stopService("cgred")
            self.syscfg.svo.enableService("cgred")
            return True
        except:
            logging.debug(formatExceptionInfo())
            return False

class nfsConfig(serviceCfgBase):
    def __init__(self, syscfg):
        super(nfsConfig, self).__init__(syscfg)
        self.serviceName = "Nfs"

    def config(self):
        try:
            if not os.path.exists("/etc/nfsmount.conf"):
                return True

            cfo = configFileOps("/etc/nfsmount.conf")
            cfo.addEntry("Ac", "False")
            cfo.addEntry("actimeo", "0")
            cfo.save()

            self.syscfg.svo.enableService("rpcbind")
            self.syscfg.svo.stopService("rpcbind")
            self.syscfg.svo.startService("rpcbind")

            self.syscfg.svo.enableService("nfs")
            self.syscfg.svo.stopService("nfs")
            self.syscfg.svo.startService("nfs")

            return True
        except:
            logging.debug(formatExceptionInfo())
            return False

class securityPolicyConfigUbuntu(serviceCfgBase):
    def __init__(self, syscfg):
        super(securityPolicyConfigUbuntu, self).__init__(syscfg)
        self.serviceName = "Apparmor"

    def config(self):
        try:
            cmd = bash("service apparmor status")
            if not cmd.isSuccess() or cmd.getStdout() == "":
                self.spRunning = False
                return True

            if not bash("apparmor_status |grep libvirt").isSuccess():
                return True

            bash("ln -s /etc/apparmor.d/usr.sbin.libvirtd /etc/apparmor.d/disable/")
            bash("ln -s /etc/apparmor.d/usr.lib.libvirt.virt-aa-helper /etc/apparmor.d/disable/")
            bash("apparmor_parser -R /etc/apparmor.d/usr.sbin.libvirtd")
            bash("apparmor_parser -R /etc/apparmor.d/usr.lib.libvirt.virt-aa-helper")

            return True
        except:
            raise CloudRuntimeException("Failed to configure apparmor, please see the /var/log/cloudstack/agent/setup.log for detail, \
                                        or you can manually disable it before starting myCloud")

    def restore(self):
        try:
            self.syscfg.svo.enableService("apparmor")
            self.syscfg.svo.startService("apparmor")
            return True
        except:
            logging.debug(formatExceptionInfo())
            return False

class securityPolicyConfigRedhat(serviceCfgBase):
    def __init__(self, syscfg):
        super(securityPolicyConfigRedhat, self).__init__(syscfg)
        self.serviceName = "SElinux"

    def config(self):
        selinuxEnabled = True

        if not bash("selinuxenabled").isSuccess():
            selinuxEnabled = False

        if selinuxEnabled:
            try:
                bash("setenforce 0")
                cfo = configFileOps("/etc/selinux/config", self)
                cfo.replace_line("SELINUX=", "SELINUX=permissive")
                return True
            except:
                raise CloudRuntimeException("Failed to configure selinux, please see the /var/log/cloudstack/agent/setup.log for detail, \
                                            or you can manually disable it before starting myCloud")
        else:
            return True

    def restore(self):
        try:
            bash("setenforce 1")
            return True
        except:
            logging.debug(formatExceptionInfo())
            return False

def configureLibvirtConfig(tls_enabled = True, cfg = None):
    cfo = configFileOps("/etc/libvirt/libvirtd.conf", cfg)
    if tls_enabled:
        cfo.addEntry("listen_tcp", "0")
        cfo.addEntry("listen_tls", "1")
        cfo.addEntry("key_file", "\"/etc/pki/libvirt/private/serverkey.pem\"")
        cfo.addEntry("cert_file", "\"/etc/pki/libvirt/servercert.pem\"")
        cfo.addEntry("ca_file", "\"/etc/pki/CA/cacert.pem\"")
    else:
        cfo.addEntry("listen_tcp", "1")
        cfo.addEntry("listen_tls", "0")
    cfo.addEntry("tcp_port", "\"16509\"")
    cfo.addEntry("tls_port", "\"16514\"")
    cfo.addEntry("auth_tcp", "\"none\"")
    cfo.addEntry("auth_tls", "\"none\"")
    cfo.save()

class libvirtConfigRedhat(serviceCfgBase):
    def __init__(self, syscfg):
        super(libvirtConfigRedhat, self).__init__(syscfg)
        self.serviceName = "Libvirt"

    def config(self):
        try:
            configureLibvirtConfig(self.syscfg.env.secure, self)

            cfo = configFileOps("/etc/sysconfig/libvirtd", self)
            if distro in (CentOS6,RHEL6):
                cfo.addEntry("export CGROUP_DAEMON", "'cpu:/virt'")
            cfo.addEntry("LIBVIRTD_ARGS", "-l")
            cfo.save()
            if os.path.exists("/lib/systemd/system/libvirtd.socket"):
                bash("/bin/systemctl mask libvirtd.socket");
                bash("/bin/systemctl mask libvirtd-ro.socket");
                bash("/bin/systemctl mask libvirtd-admin.socket");
                bash("/bin/systemctl mask libvirtd-tls.socket");
                bash("/bin/systemctl mask libvirtd-tcp.socket");

            filename = "/etc/libvirt/qemu.conf"

            cfo = configFileOps(filename, self)
            cfo.addEntry("security_driver", "\"none\"")
            cfo.addEntry("user", "\"root\"")
            cfo.addEntry("group", "\"root\"")
            cfo.addEntry("vnc_listen", "\"0.0.0.0\"")
            cfo.save()

            self.syscfg.svo.stopService("libvirtd")
            if not self.syscfg.svo.startService("libvirtd"):
                return False

            return True
        except:
            raise

    def restore(self):
        pass

class libvirtConfigUbuntu(serviceCfgBase):
    def __init__(self, syscfg):
        super(libvirtConfigUbuntu, self).__init__(syscfg)
        self.serviceName = "Libvirt"

    def setupLiveMigration(self):
        configureLibvirtConfig(self.syscfg.env.secure, self)

        if os.path.exists("/etc/init/libvirt-bin.conf"):
            cfo = configFileOps("/etc/init/libvirt-bin.conf", self)
            cfo.replace_line("exec /usr/sbin/libvirtd","exec /usr/sbin/libvirtd -d -l")
        elif os.path.exists("/etc/default/libvirt-bin"):
            cfo = configFileOps("/etc/default/libvirt-bin", self)
            cfo.replace_or_add_line("libvirtd_opts=","libvirtd_opts='-l'")
        elif os.path.exists("/etc/default/libvirtd"):
            cfo = configFileOps("/etc/default/libvirtd", self)
            cfo.replace_or_add_line("libvirtd_opts=","libvirtd_opts='-l'")
            if os.path.exists("/lib/systemd/system/libvirtd.socket"):
                bash("/bin/systemctl mask libvirtd.socket");
                bash("/bin/systemctl mask libvirtd-ro.socket");
                bash("/bin/systemctl mask libvirtd-admin.socket");
                bash("/bin/systemctl mask libvirtd-tls.socket");
                bash("/bin/systemctl mask libvirtd-tcp.socket");

    def config(self):
        try:
            self.setupLiveMigration()

            filename = "/etc/libvirt/qemu.conf"

            cfo = configFileOps(filename, self)
            cfo.addEntry("security_driver", "\"none\"")
            cfo.addEntry("user", "\"root\"")
            cfo.addEntry("group", "\"root\"")
            cfo.save()

            if os.path.exists("/lib/systemd/system/libvirtd.service"):
                bash("systemctl restart libvirtd")
            else:
                self.syscfg.svo.stopService("libvirt-bin")
                self.syscfg.svo.enableService("libvirt-bin")
            if os.path.exists("/lib/systemd/system/libvirt-bin.socket"):
                bash("systemctl stop libvirt-bin.socket")
            return True
        except:
            raise

    def restore(self):
        try:
            self.syscfg.svo.stopService("libvirt-bin")
            self.syscfg.svo.startService("libvirt-bin")
            return True
        except:
            logging.debug(formatExceptionInfo())
            return False

class firewallConfigUbuntu(serviceCfgBase):
    def __init__(self, syscfg):
        super(firewallConfigUbuntu, self).__init__(syscfg)
        self.serviceName = "Firewall"

    def config(self):
        try:
            ports = "22 1798 16509 16514".split()
            for p in ports:
                bash("ufw allow %s"%p)
            bash("ufw allow proto tcp from any to any port 5900:6100")
            bash("ufw allow proto tcp from any to any port 49152:49216")
            self.syscfg.svo.stopService("ufw")
            self.syscfg.svo.startService("ufw")
            return True
        except:
            raise

    def restore(self):
        return True

class firewallConfigBase(serviceCfgBase):
    def __init__(self, syscfg):
        super(firewallConfigBase, self).__init__(syscfg)
        self.serviceName = "Firewall"
        self.rules = []

    def allowPort(self, port):
        status = False
        try:
            status = bash("iptables-save|grep INPUT|grep -w %s"%port).isSuccess()
        except:
            pass

        if not status:
            redo = False
            result = True
            try:
                result = bash("iptables -I INPUT -p tcp -m tcp --dport %s -j ACCEPT"%port).isSuccess()
            except:
                redo = True

            if not result or redo:
                bash("sleep 30")
                bash("iptables -I INPUT -p tcp -m tcp --dport %s -j ACCEPT"%port)

    def config(self):
        try:
            for port in self.ports:
                self.allowPort(port)

            for rule in self.rules:
                bash("iptables " + rule)

            bash("iptables-save > /etc/sysconfig/iptables")
            self.syscfg.svo.stopService("iptables")
            self.syscfg.svo.startService("iptables")
            return True
        except:
            raise

    def restore(self):
        return True

class firewallConfigAgent(firewallConfigBase):
    def __init__(self, syscfg):
        super(firewallConfigAgent, self).__init__(syscfg)
        self.ports = "22 16509 16514 5900:6100 49152:49216".split()
        if syscfg.env.distribution.getVersion() == "CentOS":
            self.rules = ["-D FORWARD -j RH-Firewall-1-INPUT"]
        else:
            self.rules = ["-D FORWARD -j REJECT --reject-with icmp-host-prohibited"]


class cloudAgentConfig(serviceCfgBase):
    def __init__(self, syscfg):
        super(cloudAgentConfig, self).__init__(syscfg)
        if syscfg.env.agentMode == "Agent":
            self.serviceName = "cloudAgent"
        elif syscfg.env.agentMode == "myCloud":
            self.serviceName = "myCloud"
        elif syscfg.env.agentMode == "Console":
            self.serviceName = "Console Proxy"

    def configMyCloud(self):
        try:
            cfo = configFileOps("/etc/cloudstack/agent/agent.properties", self)
            cfo.addEntry("host", self.syscfg.env.mgtSvr)
            cfo.addEntry("zone", self.syscfg.env.zone)
            cfo.addEntry("port", "443")
            cfo.addEntry("private.network.device", self.syscfg.env.nics[0])
            cfo.addEntry("public.network.device", self.syscfg.env.nics[1])
            cfo.addEntry("guest.network.device", self.syscfg.env.nics[2])
            if cfo.getEntry("local.storage.uuid") == "":
                cfo.addEntry("local.storage.uuid", str(bash("uuidgen").getStdout()))
            cfo.addEntry("guid", str(self.syscfg.env.uuid))
            cfo.addEntry("mount.path", "/mnt")
            cfo.addEntry("resource", "com.cloud.storage.resource.LocalSecondaryStorageResource|com.cloud.agent.resource.computing.CloudZonesComputingResource")
            cfo.save()

            #self.syscfg.svo.stopService("cloud-agent")
            #self.syscfg.svo.enableService("cloud-agent")
            return True
        except:
            raise

    def configAgent(self):
        try:
            cfo = configFileOps("/etc/cloudstack/agent/agent.properties", self)
            cfo.addEntry("host", self.syscfg.env.mgtSvr)
            cfo.addEntry("zone", self.syscfg.env.zone)
            cfo.addEntry("pod", self.syscfg.env.pod)
            cfo.addEntry("cluster", self.syscfg.env.cluster)
            cfo.addEntry("hypervisor.type", self.syscfg.env.hypervisor)
            cfo.addEntry("port", "8250")
            cfo.addEntry("private.network.device", self.syscfg.env.nics[0])
            cfo.addEntry("public.network.device", self.syscfg.env.nics[1])
            cfo.addEntry("guest.network.device", self.syscfg.env.nics[2])
            cfo.addEntry("guid", str(self.syscfg.env.uuid))
            if cfo.getEntry("local.storage.uuid") == "":
                cfo.addEntry("local.storage.uuid", str(bash("uuidgen").getStdout()))
            if cfo.getEntry("resource") == "":
                cfo.addEntry("resource", "com.cloud.hypervisor.kvm.resource.LibvirtComputingResource")
            cfo.save()

            self.syscfg.svo.stopService("cloudstack-agent")
            bash("sleep 30")
            self.syscfg.svo.enableService("cloudstack-agent")
            return True
        except:
            raise

    def configConsole(self):
        try:
            cfo = configFileOps("/etc/cloudstack/agent/agent.properties", self)
            cfo.addEntry("host", self.syscfg.env.mgtSvr)
            cfo.addEntry("zone", self.syscfg.env.zone)
            cfo.addEntry("pod", self.syscfg.env.pod)
            cfo.addEntry("cluster", self.syscfg.env.cluster)
            cfo.addEntry("port", "8250")
            cfo.addEntry("private.network.device", self.syscfg.env.nics[0])
            cfo.addEntry("public.network.device", self.syscfg.env.nics[1])
            cfo.addEntry("guest.network.device", self.syscfg.env.nics[2])
            cfo.addEntry("guid", str(self.syscfg.env.uuid))
            cfo.addEntry("resource", "com.cloud.agent.resource.computing.consoleProxyResource")
            cfo.save()

            self.syscfg.svo.stopService("cloudstack-agent")
            self.syscfg.svo.enableService("cloudstack-agent")
            return True
        except:
            raise

    def config(self):
        if self.syscfg.env.agentMode == "Agent":
            return self.configAgent()
        elif self.syscfg.env.agentMode == "myCloud":
            return self.configMyCloud()
        elif self.syscfg.env.agentMode == "console":
            return self.configConsole()

    def restore(self):
        return True
