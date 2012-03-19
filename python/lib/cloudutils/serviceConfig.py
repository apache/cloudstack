from utilities import writeProgressBar, bash
from cloudException import CloudRuntimeException, CloudInternalException, formatExceptionInfo
import logging
from networkConfig import networkConfig
import re
from configFileOps import configFileOps
import os
import shutil

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
        except CloudRuntimeException, e:
            self.status = result
            writeProgressBar(None, result)
            logging.debug(e.getDetails())
            raise e
        except CloudInternalException, e:
            self.status = result
            writeProgressBar(None, result)
            raise e
        except:
            logging.debug(formatExceptionInfo())
            if self.syscfg.env.mode == "Server":
                raise CloudRuntimeException("Configure %s failed, Please check the /var/log/cloud/setupManagement.log for detail"%self.serviceName)
            else:
                raise CloudRuntimeException("Configure %s failed, Please check the /var/log/cloud/setupAgent.log for detail"%self.serviceName)
            
    def backup(self):
        if self.status is None:
            return True
        
        writeProgressBar("Restore " + self.serviceName + " ...", None)
        result = False
        try:
            for cfo in self.cfoHandlers:
                cfo.backup()
                
            result = self.restore()
        except (CloudRuntimeException, CloudInternalException), e:
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
            if not self.netcfg.isBridge(br):
                raise CloudInternalException("%s is not a bridge"%br)
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
        for line in file(self.netCfgFile).readlines():
            match = re.match("^ *iface %s.*"%dev.name, line)
            if match is not None:
                dev.method = self.getNetworkMethod(match.group(0))
                bridgeCfg = "\niface %s inet manual\n \
                             auto %s\n \
                             iface %s inet %s\n \
                             bridge_ports %s\n"%(dev.name, br, br, dev.method, dev.name)
                cfo = configFileOps(self.netCfgFile, self)
                cfo.replace_line("^ *iface %s.*"%dev.name, bridgeCfg)

    def addDev(self, br, dev):
        logging.debug("Haven't implement yet")

    def addBridgeAndDev(self, br, dev):
        logging.debug("Haven't implement yet")

    def writeToCfgFile(self, br, dev):
        cfg = file(self.netCfgFile).read()
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
                
            if not bash("ifup %s"%self.brName).isSuccess():
                raise CloudInternalException("Can't start network:%s"%self.brName, bash.getErrMsg(self))
            
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
        self.devCfgFile = "/etc/sysconfig/network-scripts/ifcfg-%s"%dev.name
        self.brCfgFile = "/etc/sysconfig/network-scripts/ifcfg-%s"%brName
        
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
        bash("ifdown %s"%dev.name)
        
        if not os.path.exists(self.brCfgFile):
            shutil.copy(self.devCfgFile, self.brCfgFile)
        
        #config device file at first: disable nm, set onboot=yes if not
        cfo = configFileOps(self.devCfgFile, self)
        cfo.addEntry("NM_CONTROLLED", "no")
        cfo.addEntry("ONBOOT", "yes")
        cfo.addEntry("BRIDGE", brName)
        cfo.save()
        
        cfo = configFileOps(self.brCfgFile, self)
        cfo.addEntry("NM_CONTROLLED", "no")
        cfo.addEntry("ONBOOT", "yes")
        cfo.addEntry("DEVICE", brName)
        cfo.addEntry("TYPE", "Bridge")
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
            cfo.addEntry("AC", "False")
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
            raise CloudRuntimeException("Failed to configure apparmor, please see the /var/log/cloud/setupAgent.log for detail, \
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
                raise CloudRuntimeException("Failed to configure selinux, please see the /var/log/cloud/setupAgent.log for detail, \
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

class libvirtConfigRedhat(serviceCfgBase):
    def __init__(self, syscfg):
        super(libvirtConfigRedhat, self).__init__(syscfg)
        self.serviceName = "Libvirt"
    
    def config(self):
        try:
            cfo = configFileOps("/etc/libvirt/libvirtd.conf", self)
            cfo.addEntry("listen_tcp", "1")
            cfo.addEntry("tcp_port", "\"16509\"")
            cfo.addEntry("auth_tcp", "\"none\"")
            cfo.addEntry("listen_tls", "0")
            cfo.save()
            
            cfo = configFileOps("/etc/sysconfig/libvirtd", self)
            cfo.addEntry("export CGROUP_DAEMON", "'cpu:/virt'")
            cfo.addEntry("LIBVIRTD_ARGS", "-l")
            cfo.save()
            
            filename = "/etc/libvirt/qemu.conf"
        
            cfo = configFileOps(filename, self)
            cfo.addEntry("cgroup_controllers", "[\"cpu\"]")
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
        cfo = configFileOps("/etc/libvirt/libvirtd.conf", self)
        cfo.addEntry("listen_tcp", "1")
        cfo.addEntry("tcp_port", "\"16509\"");
        cfo.addEntry("auth_tcp", "\"none\"");
        cfo.addEntry("listen_tls", "0")
        cfo.save()
        
        if os.path.exists("/etc/init/libvirt-bin.conf"):
            cfo = configFileOps("/etc/init/libvirt-bin.conf", self)
            cfo.replace_line("exec /usr/sbin/libvirtd","exec /usr/sbin/libvirtd -d -l")
        else:
            cfo = configFileOps("/etc/default/libvirt-bin", self)
            cfo.replace_or_add_line("libvirtd_opts=","libvirtd_opts='-l -d'")
    
    def config(self):
        try:
            self.setupLiveMigration()
            
            filename = "/etc/libvirt/qemu.conf"
    
            cfo = configFileOps(filename, self)
            cfo.addEntry("security_driver", "\"none\"")
            cfo.addEntry("user", "\"root\"")
            cfo.addEntry("group", "\"root\"")
            cfo.save()

            self.syscfg.svo.stopService("libvirt-bin")
            self.syscfg.svo.enableService("libvirt-bin")
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
            ports = "22 1798 16509".split()
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
        self.ports = "22 16509 5900:6100 49152:49216".split()
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
            cfo = configFileOps("/etc/cloud/agent/agent.properties", self)  
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
            cfo = configFileOps("/etc/cloud/agent/agent.properties", self)  
            cfo.addEntry("host", self.syscfg.env.mgtSvr)
            cfo.addEntry("zone", self.syscfg.env.zone)
            cfo.addEntry("pod", self.syscfg.env.pod)
            cfo.addEntry("cluster", self.syscfg.env.cluster)
            cfo.addEntry("port", "8250")
            cfo.addEntry("private.network.device", self.syscfg.env.nics[0])
            cfo.addEntry("public.network.device", self.syscfg.env.nics[1])
            cfo.addEntry("guest.network.device", self.syscfg.env.nics[2])
            cfo.addEntry("guid", str(self.syscfg.env.uuid))
            if cfo.getEntry("local.storage.uuid") == "":
                cfo.addEntry("local.storage.uuid", str(bash("uuidgen").getStdout()))
            cfo.addEntry("resource", "com.cloud.agent.resource.computing.LibvirtComputingResource")
            cfo.save()
            
            self.syscfg.svo.stopService("cloud-agent")
            bash("sleep 30")
            self.syscfg.svo.enableService("cloud-agent")
            return True
        except:
            raise
        
    def configConsole(self):
        try:
            cfo = configFileOps("/etc/cloud/agent/agent.properties", self)  
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
            
            self.syscfg.svo.stopService("cloud-agent")
            self.syscfg.svo.enableService("cloud-agent")
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


class sudoersConfig(serviceCfgBase):
    def __init__(self, syscfg):
        super(sudoersConfig, self).__init__(syscfg)
        self.serviceName = "sudoers"
    def config(self):
        try:
            cfo = configFileOps("/etc/sudoers", self)
            cfo.addEntry("cloud ALL ", "NOPASSWD : ALL")
            cfo.rmEntry("Defaults", "requiretty", " ")
            cfo.save()
            return True
        except:
            raise
        
    def restore(self):
        return True
    
class firewallConfigServer(firewallConfigBase):
    def __init__(self, syscfg):
        super(firewallConfigServer, self).__init__(syscfg)
        #9090 is used for cluster management server
        if self.syscfg.env.svrMode == "myCloud":
            self.ports = "443 8080 8250 8443 9090".split()
        else:
            self.ports = "8080 8250 9090".split()

class ubuntuFirewallConfigServer(firewallConfigServer):
    def allowPort(self, port):
        status = False
        try:
            status = bash("iptables-save|grep INPUT|grep -w %s"%port).isSuccess()
        except:
            pass
        
        if not status: 
            bash("ufw allow %s/tcp"%port)
            
    def config(self):
        try:
            for port in self.ports:
                self.allowPort(port)
            
            #FIXME: urgly make /root writable 
            bash("sudo chmod 0777 /root")
                
            return True
        except:
            raise
