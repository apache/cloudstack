/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */


package com.cloud.agent.resource.computing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import javax.ejb.Local;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.DomainInfo;
import org.libvirt.DomainInterfaceStats;
import org.libvirt.LibvirtException;
import org.libvirt.Network;
import org.libvirt.NodeInfo;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo;
import org.libvirt.StorageVol;
import org.libvirt.StorageVolInfo;
import org.libvirt.StoragePoolInfo.StoragePoolState;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.AttachIsoCommand;
import com.cloud.agent.api.AttachVolumeAnswer;
import com.cloud.agent.api.AttachVolumeCommand;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.CheckStateCommand;
import com.cloud.agent.api.CheckVirtualMachineAnswer;
import com.cloud.agent.api.CheckVirtualMachineCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.DeleteStoragePoolCommand;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.GetVmStatsAnswer;
import com.cloud.agent.api.GetVmStatsCommand;
import com.cloud.agent.api.GetVncPortAnswer;
import com.cloud.agent.api.GetVncPortCommand;
import com.cloud.agent.api.HostStatsEntry;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.ManageSnapshotAnswer;
import com.cloud.agent.api.ManageSnapshotCommand;
import com.cloud.agent.api.MigrateAnswer;
import com.cloud.agent.api.MigrateCommand;
import com.cloud.agent.api.MirrorCommand;
import com.cloud.agent.api.ModifySshKeysCommand;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.NetworkIngressRuleAnswer;
import com.cloud.agent.api.NetworkIngressRulesCmd;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.PingTestCommand;
import com.cloud.agent.api.PrepareForMigrationAnswer;
import com.cloud.agent.api.PrepareForMigrationCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RebootAnswer;
import com.cloud.agent.api.RebootCommand;
import com.cloud.agent.api.StartAnswer;
import com.cloud.agent.api.StartCommand;
import com.cloud.agent.api.StartConsoleProxyAnswer;
import com.cloud.agent.api.StartConsoleProxyCommand;
import com.cloud.agent.api.StartRouterAnswer;
import com.cloud.agent.api.StartRouterCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StopAnswer;
import com.cloud.agent.api.StopCommand;
import com.cloud.agent.api.VmStatsEntry;
import com.cloud.agent.api.proxy.CheckConsoleProxyLoadCommand;
import com.cloud.agent.api.proxy.ConsoleProxyLoadAnswer;
import com.cloud.agent.api.proxy.WatchConsoleProxyLoadCommand;
import com.cloud.agent.api.routing.RoutingCommand;
import com.cloud.agent.api.storage.CreateAnswer;
import com.cloud.agent.api.storage.CreateCommand;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.CreatePrivateTemplateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.DiskCharacteristicsTO;
import com.cloud.agent.api.to.StoragePoolTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.resource.computing.LibvirtStoragePoolDef.poolType;
import com.cloud.agent.resource.computing.LibvirtStorageVolumeDef.volFormat;
import com.cloud.agent.resource.computing.LibvirtVMDef.consoleDef;
import com.cloud.agent.resource.computing.LibvirtVMDef.devicesDef;
import com.cloud.agent.resource.computing.LibvirtVMDef.diskDef;
import com.cloud.agent.resource.computing.LibvirtVMDef.featuresDef;
import com.cloud.agent.resource.computing.LibvirtVMDef.graphicDef;
import com.cloud.agent.resource.computing.LibvirtVMDef.guestDef;
import com.cloud.agent.resource.computing.LibvirtVMDef.guestResourceDef;
import com.cloud.agent.resource.computing.LibvirtVMDef.inputDef;
import com.cloud.agent.resource.computing.LibvirtVMDef.interfaceDef;
import com.cloud.agent.resource.computing.LibvirtVMDef.serialDef;
import com.cloud.agent.resource.computing.LibvirtVMDef.termPolicy;
import com.cloud.agent.resource.computing.LibvirtVMDef.interfaceDef.hostNicType;
import com.cloud.agent.resource.virtualnetwork.VirtualRoutingResource;
import com.cloud.exception.InternalErrorException;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.NetworkEnums.RouterPrivateIpStrategy;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.StoragePoolVO;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume.StorageResourceType;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.storage.template.Processor;
import com.cloud.storage.template.Processor.FormatInfo;
import com.cloud.storage.template.QCOW2Processor;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.storage.template.TemplateLocation;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.cloud.vm.ConsoleProxyVO;
import com.cloud.vm.DomainRouter;
import com.cloud.vm.State;
import com.cloud.vm.VirtualMachineName;

/**
 * LibvirtComputingResource execute requests on the computing/routing host using the libvirt API
 * 
 * @config
 * {@table
 *    || Param Name | Description | Values | Default ||
 *    || hypervisor.type | type of local hypervisor | string | kvm ||
 *    || hypervisor.uri | local hypervisor to connect to | URI | qemu:///system ||
 *    || domr.arch | instruction set for domr template | string | i686 ||
 *    || private.bridge.name | private bridge where the domrs have their private interface | string | vmops0 ||
 *    || public.bridge.name | public bridge where the domrs have their public interface | string | br0 ||
 *    || private.network.name | name of the network where the domrs have their private interface | string | vmops-private ||
 *    || private.ipaddr.start | start of the range of private ip addresses for domrs | ip address | 192.168.166.128 ||
 *    || private.ipaddr.end | end of the range of private ip addresses for domrs | ip address | start + 126  ||
 *    || private.macaddr.start | start of the range of private mac addresses for domrs | mac address | 00:16:3e:77:e2:a0 ||
 *    || private.macaddr.end | end of the range of private mac addresses for domrs | mac address | start + 126 ||
 *    || pool | the parent of the storage pool hierarchy
 *  }
 **/
@Local(value={ServerResource.class})
public class LibvirtComputingResource extends ServerResourceBase implements ServerResource {
    private static final Logger s_logger = Logger.getLogger(LibvirtComputingResource.class);
    
    private String _createvnetPath;
    private String _vnetcleanupPath;
    private String _modifyVlanPath;
    private String _versionstringpath;
    private String _patchdomrPath;
    private String _createvmPath;
    private String _host;
    private String _dcId;
    private String _pod;
    private long _hvVersion;
    private final String _SSHKEYSPATH = "/root/.ssh";
    private final String _SSHPRVKEYPATH = _SSHKEYSPATH + File.separator + "id_rsa.cloud";
    private final String _SSHPUBKEYPATH = _SSHKEYSPATH + File.separator + "id_rsa.pub.cloud";
    private final String _mountPoint = "/mnt";
    StorageLayer _storage;
    
	private static final class KeyValueInterpreter extends OutputInterpreter {
		private final Map<String, String> map = new HashMap<String, String>();
		
		@Override
		public String interpret(BufferedReader reader) throws IOException {
			String line = null;
		    int numLines=0;
		    while ((line = reader.readLine()) != null) {
		       String [] toks = line.trim().split("=");
		       if (toks.length < 2) s_logger.warn("Failed to parse Script output: " + line);
		       else map.put(toks[0].trim(), toks[1].trim());
		       numLines++;
 		    }
		    if (numLines == 0) {
		    	s_logger.warn("KeyValueInterpreter: no output lines?");
		    }
		    return null;
		}

		public Map<String, String> getKeyValues() {
			return map;
		}
	}
	
	@Override
    protected String getDefaultScriptsDir() {
	    return null;
	}
	
	protected static MessageFormat domrXMLformat= new MessageFormat( "<domain type=''{0}''>" +
		"  <name>{1}</name>" +
		"  <uuid>{24}</uuid>" +
		"  <memory>{2}</memory>" +
		"  <vcpu>1</vcpu>" +
		"  <os>" +
		"    <type arch=''{3}''>hvm</type>" +
		"  </os>" +
		"  <features>" +
		"    <acpi/>" +
		"    <pae/>" +
		"  </features>" +
		"  <on_poweroff>destroy</on_poweroff>" +
		"  <on_reboot>restart</on_reboot>" +
		"  <on_crash>destroy</on_crash>" +
		"  <devices>" +
		"    <emulator>{23}</emulator>" +
		"    <disk type=''file'' device=''disk''>" +
		"      <source file=''{4}''/>" +
		"      <target dev=''hda'' bus=''ide''/>" +
		"    </disk>" +
		"    <disk type=''file'' device=''disk''>" +
		"      <source file=''{25}''/>" +
		"      <target dev=''hdb'' bus=''ide''/>" +
		"    </disk>" +
		"    <interface type=''bridge''>" +
		"      <mac address=''{5}'' />" +
		"      <source bridge=''{6}''/>" +
		"      <target dev=''{7}''/>" +
		"      <model type=''virtio''/>" +
		"    </interface>" +
		"    <interface type=''network''>" +
		"      <source network=''{8}''/>" +
		"      <mac address=''{9}'' />" +
		"      <model type=''virtio''/>" +
		"    </interface>" +
		"    <interface type=''bridge''>" +
		"      <mac address=''{10}'' />" +
		"      <source bridge=''{11}''/>" +
		"      <target dev=''{12}''/>" +
		"      <model type=''virtio''/>" +
		"    </interface>" +
		"    <console type=''pty''/>" +
		"    <input type=''mouse'' bus=''ps2''/>" +
		"    <graphics type=''vnc'' autoport=''yes'' listen=''''/>" +
		"  </devices>" +
		"</domain>");
	
	protected static MessageFormat consoleProxyXMLformat= new MessageFormat( "<domain type=''{0}''>" +
			"  <name>{1}</name>" +
			"  <uuid>{2}</uuid>" +
			"  <memory>{3}</memory>" +
			"  <vcpu>1</vcpu>" +
			"  <os>" +
			"    <type arch=''{4}''>hvm</type>" +
			"  </os>" +
			"  <features>" +
			"    <acpi/>" +
			"    <pae/>" +
			"  </features>" +
			"  <on_poweroff>destroy</on_poweroff>" +
			"  <on_reboot>restart</on_reboot>" +
			"  <on_crash>destroy</on_crash>" +
			"  <devices>" +
			"    <emulator>{13}</emulator>" +
			"    <disk type=''file'' device=''disk''>" +
			"      <source file=''{14}''/>" +
			"      <target dev=''hda'' bus=''ide''/>" +
			"    </disk>" +
			"    <disk type=''file'' device=''disk''>" +
			"      <source file=''{24}''/>" +
			"      <target dev=''hdb'' bus=''ide''/>" +
			"    </disk>" +
			"    <interface type=''network''>" +
			"      <source network=''default''/>" +
			"      <model type=''virtio''/>" +
			"    </interface>" +
			"    <interface type=''network''>" +
			"      <source network=''{15}''/>" +
			"      <mac address=''{16}'' />" +
			"      <model type=''virtio''/>" +
			"    </interface>" +
			"    <interface type=''bridge''>" +
			"      <mac address=''{17}'' />" +
			"      <source bridge=''{18}''/>" +
			"      <model type=''virtio''/>" +
			"    </interface>" +
			"    <console type=''pty''/>" +
			"    <input type=''mouse'' bus=''ps2''/>" +
			"    <graphics type=''vnc'' autoport=''yes'' listen=''''/>" +
			"  </devices>" +
			"</domain>");
	
	protected static MessageFormat vmXMLformat= new MessageFormat( "<domain type=''{0}''>" +
			"  <name>{1}</name>" +
			"  <uuid>{2}</uuid>" +
			"  <memory>{3}</memory>" +
			"  <vcpu>{4}</vcpu>" +
			"  <os>" +
			"    <type arch=''{5}''>hvm</type>" +
			"    <boot dev=''cdrom''/>" +
			"    <boot dev=''hd''/>" +
			"  </os>" +
			"  <features>" +
			"    <acpi/>" +
			"    <pae/>" +
			"  </features>" +
			"  <on_poweroff>destroy</on_poweroff>" +
			"  <on_reboot>restart</on_reboot>" +
			"  <on_crash>destroy</on_crash>" +
			"  <devices>" +
			"    <emulator>{6}</emulator>" +
			"    <disk type=''file'' device=''disk''>" +
			"      <source file=''{7}''/>" +
			"      <target dev=''hda'' bus=''ide''/>" +
			"    </disk>" +
			"	 <disk type=''file'' device=''cdrom''>" +
			"      <source file=''{8}''/>" +
			"	   <target dev=''hdc'' bus=''ide''/>" +
			"	   <readonly/>" +
			"	 </disk>" +
			"    <interface type=''bridge''>" +
			"      <mac address=''{9}'' />" +
			"      <source bridge=''{10}''/>" +
			"      <model type=''e1000''/>" +
			"    </interface>" +
			"    <console type=''pty''/>" +
			"    <graphics type=''vnc'' autoport=''yes'' listen=''''/>" +
			"    <input type=''tablet'' bus=''usb''/>" +
			"  </devices>" +
			"</domain>");
	
	protected static MessageFormat IsoXMLformat = new MessageFormat(
			"	<disk type=''file'' device=''cdrom''>" +
			"		<source file=''{0}''/>" +
			"		<target dev=''hdc'' bus=''ide''/>" +
			"		<readonly/>" +
			"	</disk>");
	
	protected static MessageFormat DiskXMLformat = new MessageFormat(
			"	<disk type=''file'' device=''disk''>" +
			"		<source file=''{0}''/>" +
			"		<target dev=''{1}'' bus=''scsi''/>" +
			"	</disk>");
	
	protected Connect _conn;
	protected String _hypervisorType;
	protected String _hypervisorURI;
	protected String _hypervisorPath;
	protected String _privNwName;
	protected String _privBridgeName;
	protected String _linkLocalBridgeName;
	protected String _publicBridgeName;
	protected String _privateBridgeIp;
	protected String _domrArch;
	protected String _domrKernel;
	protected String _domrRamdisk;
	protected String _pool;
	private boolean _can_bridge_firewall;
	private final Map<String, vmStats> _vmStats = new ConcurrentHashMap<String, vmStats>();
	
	protected boolean _disconnected = true;
	protected int _timeout;
	protected int _stopTimeout;
    protected static HashMap<DomainInfo.DomainState, State> s_statesTable;
    static {
            s_statesTable = new HashMap<DomainInfo.DomainState, State>();
            s_statesTable.put(DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF, State.Stopped);
            s_statesTable.put(DomainInfo.DomainState.VIR_DOMAIN_PAUSED, State.Running);
            s_statesTable.put(DomainInfo.DomainState.VIR_DOMAIN_RUNNING, State.Running);
            s_statesTable.put(DomainInfo.DomainState.VIR_DOMAIN_BLOCKED, State.Running);
            s_statesTable.put(DomainInfo.DomainState.VIR_DOMAIN_NOSTATE, State.Unknown);
            s_statesTable.put(DomainInfo.DomainState.VIR_DOMAIN_SHUTDOWN, State.Stopping);
    }

    protected HashMap<String, State> _vms = new HashMap<String, State>(20);
    protected List<String> _vmsKilled = new ArrayList<String>();

    protected BitSet _domrIndex;
    
	private VirtualRoutingResource _virtRouterResource;

	private boolean _debug;

	private String _pingTestPath;

	private int _dom0MinMem;

	private enum defineOps {
		UNDEFINE_VM,
		DEFINE_VM
	}
	
	private String getEndIpFromStartIp(String startIp, int numIps) {
		String[] tokens = startIp.split("[.]");
		assert(tokens.length == 4);
		int lastbyte = Integer.parseInt(tokens[3]);
		lastbyte = lastbyte + numIps;
		tokens[3] = Integer.toString(lastbyte);
		StringBuilder end = new StringBuilder(15);
		end.append(tokens[0]).append(".").append(tokens[1]).append(".").append(tokens[2]).append(".").append(tokens[3]);
		return end.toString();
	}
	
	private Map<String, Object> getDeveloperProperties() throws ConfigurationException {
		final File file = PropertiesUtil.findConfigFile("developer.properties");
        if (file == null) {
            throw new ConfigurationException("Unable to find developer.properties.");
        }

        s_logger.info("developer.properties found at " + file.getAbsolutePath());
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));

            String startMac = (String)properties.get("private.macaddr.start");
            if (startMac == null) {
            	throw new ConfigurationException("Developers must specify start mac for private ip range");
            }

            String startIp  = (String)properties.get("private.ipaddr.start");
            if (startIp == null) {
            	throw new ConfigurationException("Developers must specify start ip for private ip range");
            }
            final Map<String, Object> params = PropertiesUtil.toMap(properties);

            String endIp  = (String)properties.get("private.ipaddr.end");
            if (endIp == null) {
            	endIp = getEndIpFromStartIp(startIp, 16);
            	params.put("private.ipaddr.end", endIp);
            }
            return params;
        } catch (final FileNotFoundException ex) {
            throw new CloudRuntimeException("Cannot find the file: " + file.getAbsolutePath(), ex);
        } catch (final IOException ex) {
            throw new CloudRuntimeException("IOException in reading " + file.getAbsolutePath(), ex);
        }
	}
	
	protected String getDefaultNetworkScriptsDir() {
	    return "scripts/vm/network/vnet";
	}
	
	protected String getDefaultStorageScriptsDir() {
	    return "scripts/storage/qcow2";
	}

	@Override
	public boolean configure(String name, Map<String, Object> params)
			throws ConfigurationException {
		boolean success = super.configure(name, params);
		if (! success)
			return false;
		_virtRouterResource = new VirtualRoutingResource();
		
		// Set the domr scripts directory
		params.put("domr.scripts.dir", "scripts/network/domr/kvm");
		
		success = _virtRouterResource.configure(name, params);
		
		String kvmScriptsDir = (String)params.get("kvm.scripts.dir");
		if (kvmScriptsDir == null) {
		    kvmScriptsDir = "scripts/vm/hypervisor/kvm";
		}
		
		String networkScriptsDir = (String)params.get("network.scripts.dir");
		if (networkScriptsDir == null) {
		    networkScriptsDir = getDefaultNetworkScriptsDir();
		}
		
		String storageScriptsDir = (String)params.get("storage.scripts.dir");
		if (storageScriptsDir == null) {
			storageScriptsDir = getDefaultStorageScriptsDir();
		}
		
		if ( ! success) {
			return false;
		}
		
		_host = (String)params.get("host");
		if (_host == null) {
			_host = "localhost";
        }


		_dcId = (String) params.get("zone");
		if (_dcId == null) {
			_dcId = "default";
		}

        _pod = (String) params.get("pod");
        if (_pod == null) {
        	_pod = "default";
        }
		
        _createvnetPath = Script.findScript(networkScriptsDir, "createvnet.sh");
        if(_createvnetPath == null) {
            throw new ConfigurationException("Unable to find createvnet.sh");
        }

        _vnetcleanupPath = Script.findScript(networkScriptsDir, "vnetcleanup.sh");
        if(_vnetcleanupPath == null) {
            throw new ConfigurationException("Unable to find createvnet.sh");
        }
        
        _modifyVlanPath = Script.findScript(networkScriptsDir, "modifyvlan.sh");
        if (_modifyVlanPath == null) {
            throw new ConfigurationException("Unable to find modifyvlan.sh");
        }
        
        _versionstringpath = Script.findScript(kvmScriptsDir, "versions.sh");
        if (_versionstringpath == null) {
            throw new ConfigurationException("Unable to find versions.sh");
        }
        
        _patchdomrPath = Script.findScript(kvmScriptsDir + "/patch/", "rundomrpre.sh");
        if (_patchdomrPath == null) {
        	throw new ConfigurationException("Unable to find rundomrpre.sh");
        }
        
        _createvmPath = Script.findScript(storageScriptsDir, "createvm.sh");
        if (_createvmPath == null) {
            throw new ConfigurationException("Unable to find the createvm.sh");
        }
        s_logger.info("createvm.sh found in " + _createvmPath);
        
		String value = (String)params.get("developer");
        boolean isDeveloper = Boolean.parseBoolean(value);
        
        if (isDeveloper) {
        	params.putAll(getDeveloperProperties());
        }
        
        _pool = (String) params.get("pool");
        if (_pool == null) {
        	_pool = "/root";
        }

        
        String instance = (String)params.get("instance");
        
		_hypervisorType = (String)params.get("hypervisor.type");
		if (_hypervisorType == null) {
			_hypervisorType = "kvm";
		}
		
		_hypervisorURI = (String)params.get("hypervisor.uri");
		if (_hypervisorURI == null) {
			_hypervisorURI = "qemu:///system";
		}
        String startMac = (String)params.get("private.macaddr.start");
        if (startMac == null) {
        	startMac = "00:16:3e:77:e2:a0";
        }

        String startIp  = (String)params.get("private.ipaddr.start");
        if (startIp == null) {
        	startIp = "192.168.166.128";
        }
        
        _pingTestPath = Script.findScript(kvmScriptsDir, "pingtest.sh");
        if (_pingTestPath == null) {
            throw new ConfigurationException("Unable to find the pingtest.sh");
        }
        
        _linkLocalBridgeName = (String)params.get("private.bridge.name");
        if (_linkLocalBridgeName == null) {
        	if (isDeveloper) {
        		_linkLocalBridgeName = "cloud-" + instance + "-0";
        	} else {
        		_linkLocalBridgeName = "cloud0";
        	}
        }
        
        _publicBridgeName = (String)params.get("public.network.device");
        if (_publicBridgeName == null) {
        	_publicBridgeName = "cloudbr0";
        }
        
        _privBridgeName = (String)params.get("private.network.device");
        if (_privBridgeName == null) {
        	_privBridgeName = "cloudbr1";
        }
        
        _privNwName = (String)params.get("private.network.name");
        if (_privNwName == null) {
        	if (isDeveloper) {
        		_privNwName = "cloud-" + instance + "-private";
        	} else {
        		_privNwName = "cloud-private";
        	}
        }
        
         value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 120) * 1000;
        
        value = (String)params.get("stop.script.timeout");
        _stopTimeout = NumbersUtil.parseInt(value, 120) * 1000;
        

        _domrArch = (String)params.get("domr.arch");
        if (_domrArch == null ) {
        	_domrArch = "i686";
        } else if (!"i686".equalsIgnoreCase(_domrArch) && !"x86_64".equalsIgnoreCase(_domrArch)) {
        	throw new ConfigurationException("Invalid architecture (domr.arch) -- needs to be i686 or x86_64");
        }
        
        _domrKernel = (String)params.get("domr.kernel");
        if (_domrKernel == null ) {
        	_domrKernel = new File("/var/lib/libvirt/images/vmops-domr-kernel").getAbsolutePath();
        }
        
        _domrRamdisk = (String)params.get("domr.ramdisk");
        if (_domrRamdisk == null ) {
        	_domrRamdisk = new File("/var/lib/libvirt/images/vmops-domr-initramfs").getAbsolutePath();
        }
        
        
        value = (String)params.get("host.reserved.mem.mb");
        _dom0MinMem = NumbersUtil.parseInt(value, 0)*1024*1024;
         
        
        value = (String)params.get("debug.mode");
        _debug = Boolean.parseBoolean(value);
        
		try{
			_conn = new Connect(_hypervisorURI, false);
		} catch (LibvirtException e){
			throw new ConfigurationException("Unable to connect to hypervisor: " +  e.getMessage());
		}
		
		/* Does node support HVM guest? If not, exit*/
		if (!IsHVMEnabled()) {
			throw new ConfigurationException("NO HVM support on this machine, pls make sure: " +
												"1. VT/SVM is supported by your CPU, or is enabled in BIOS. " +
												"2. kvm modules is installed");
		}
		
		_hypervisorPath = getHypervisorPath();
		try {
			_hvVersion = _conn.getVersion();
			_hvVersion = (_hvVersion % 1000000) / 1000;
		} catch (LibvirtException e) {
			
		}
		
		try {
            Class<?> clazz = Class.forName("com.cloud.storage.JavaStorageLayer");
            _storage = (StorageLayer)ComponentLocator.inject(clazz);
            _storage.configure("StorageLayer", params);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Unable to find class " + "com.cloud.storage.JavaStorageLayer");
        }

		
		_can_bridge_firewall = can_bridge_firewall();
		
		Network vmopsNw = null;
		try {
			 vmopsNw = _conn.networkLookupByName(_privNwName);
		} catch (LibvirtException lve){
			
		}
		deletExitingLinkLocalRoutTable(_linkLocalBridgeName);
		if (vmopsNw == null) {
			try {
				/*vmopsNw = conn.networkCreateXML("<network>" +
						"  <name>vmops-private</name>"+
						"  <bridge name='vmops0'/>"+
						"  <ip address='169.254.0.1' netmask='255.255.0.0'>"+
						"    <dhcp>"+
						"      <range start='169.254.0.2' end='169.254.255.254'/>+
						"    </dhcp>"+
						"  </ip>"+
				"</network>");*/
				
				_virtRouterResource.cleanupPrivateNetwork(_privNwName, _linkLocalBridgeName);
				LibvirtNetworkDef networkDef = new LibvirtNetworkDef(_privNwName, null, null);
				networkDef.defLocalNetwork(_linkLocalBridgeName, false, 0, NetUtils.getLinkLocalGateway(), NetUtils.getLinkLocalNetMask());
				
				String nwXML = networkDef.toString();
				s_logger.debug(nwXML);
				vmopsNw = _conn.networkCreateXML(nwXML);
				
			} catch (LibvirtException lve) {
				throw new ConfigurationException("Unable to define private network " +  lve.getMessage());
			}
		} else {
			s_logger.info("Found private network " + _privNwName + " already defined");
		}
		
		return true;
	}
	
	private String getVnetId(String vnetId) {
		String id = "0000" + vnetId;
		return id.substring(id.length() - 4);
	}
	
	private void patchSystemVm(String cmdLine, String dataDiskPath, String vmName) throws InternalErrorException {
		String result;
        final Script command = new Script(_patchdomrPath, _timeout, s_logger);
        command.add("-l", vmName);
        command.add("-t", "all");
        command.add("-d", dataDiskPath);
        command.add("-p", cmdLine.replaceAll(" ", ","));
        result = command.execute();
        if (result != null) {
        	throw new InternalErrorException(result);
        }
	}
	
	private void disableBridgeForwardding(String vnetBridge) {
		//TODO: workaround for KVM on ubuntu, disable bridge forward table
		Script disableFD = new Script("/bin/bash", _timeout);
		disableFD.add("-c");
		disableFD.add("brctl setfd " + vnetBridge + " 0; brctl setageing " + vnetBridge + " 0");
		disableFD.execute();
	}
	
	boolean isDirectAttachedNetwork(String type) {
		if ("untagged".equalsIgnoreCase(type))
			return true;
		else
			return false;
	}
	
	protected synchronized String startDomainRouter(StartRouterCommand cmd) {
		DomainRouter router = cmd.getRouter();
		List<interfaceDef> nics = null;
		try {
			nics = createRouterVMNetworks(cmd);

			List<diskDef> disks = createSystemVMDisk(cmd.getVolumes());
			
			String dataDiskPath = null;
			for (diskDef disk : disks) {
				if (disk.getDiskLabel().equalsIgnoreCase("hdb")) {
					dataDiskPath = disk.getDiskPath();
				}
			}

			String vmName = cmd.getVmName();
			patchSystemVm(cmd.getBootArgs(), dataDiskPath, vmName);

			String uuid = UUID.nameUUIDFromBytes(vmName.getBytes()).toString();
			String domXML =  defineVMXML(cmd.getVmName(), uuid, router.getRamSize(), 1, _domrArch, nics,  disks, router.getVncPassword(), "Fedora 12");

			s_logger.debug(domXML);

			startDomain(vmName, domXML);

			for (interfaceDef nic : nics) {
				if (nic.getHostNetType() == hostNicType.VNET) {
					disableBridgeForwardding(nic.getBrName());
				}
			}
			
			if (isDirectAttachedNetwork(router.getVlanId()))
				default_network_rules_for_systemvm(vmName);
		} catch (LibvirtException e) {
			if (nics != null) {
				cleanupVMNetworks(nics);
			}
			s_logger.debug("Failed to start domr: " + e.toString());
			return e.toString();
		}catch (InternalErrorException e) {
			if (nics != null) {
				cleanupVMNetworks(nics);
			}
			s_logger.debug("Failed to start domr: " + e.toString());
			return e.toString();
		}
		return null;
	}
	
	protected synchronized String startConsoleProxy(StartConsoleProxyCommand cmd) {
		ConsoleProxyVO console = cmd.getProxy();
		List<interfaceDef> nics = null;
		try {
			nics = createConsoleVMNetworks(cmd);

			List<diskDef> disks = createSystemVMDisk(cmd.getVolumes());
			
			String dataDiskPath = null;
			for (diskDef disk : disks) {
				if (disk.getDiskLabel().equalsIgnoreCase("hdb")) {
					dataDiskPath = disk.getDiskPath();
				}
			}

			String vmName = cmd.getVmName();
			patchSystemVm(cmd.getBootArgs(), dataDiskPath, vmName);

			String uuid = UUID.nameUUIDFromBytes(vmName.getBytes()).toString();
			String domXML =  defineVMXML(cmd.getVmName(), uuid, console.getRamSize(), 1, _domrArch, nics,  disks, console.getVncPassword(), "Fedora 12");

			s_logger.debug(domXML);

			startDomain(vmName, domXML);
		} catch (LibvirtException e) {
			s_logger.debug("Failed to start domr: " + e.toString());
			return e.toString();
		}catch (InternalErrorException e) {
			s_logger.debug("Failed to start domr: " + e.toString());
			return e.toString();
		}
		return null;
	}
	
	private String defineVMXML(String vmName, String uuid, int memSize, int cpus, String arch, List<interfaceDef> nics, List<diskDef> disks, String vncPaswd,  String guestOSType) {
		LibvirtVMDef vm = new LibvirtVMDef();
		vm.setHvsType(_hypervisorType);
		vm.setDomainName(vmName);
		vm.setDomUUID(uuid);
		vm.setDomDescription(guestOSType);
		
		guestDef guest = new guestDef();
		guest.setGuestType(guestDef.guestType.KVM);
		guest.setGuestArch(arch);

		guest.setBootOrder(guestDef.bootOrder.CDROM);
		guest.setBootOrder(guestDef.bootOrder.HARDISK);
			
		vm.addComp(guest);

		guestResourceDef grd = new guestResourceDef();
		grd.setMemorySize(memSize*1024);
		grd.setVcpuNum(cpus);
		vm.addComp(grd);

		featuresDef features = new featuresDef();
		features.addFeatures("pae");
		features.addFeatures("apic");
		features.addFeatures("acpi");
		vm.addComp(features);

		termPolicy term = new termPolicy();
		term.setCrashPolicy("destroy");
		term.setPowerOffPolicy("destroy");
		term.setRebootPolicy("restart");
		vm.addComp(term);

		devicesDef devices = new devicesDef();
		devices.setEmulatorPath(_hypervisorPath);

		for (interfaceDef nic : nics) {
			devices.addDevice(nic);
		}
		
		for (diskDef disk : disks) {
			if (!disk.isAttachDeferred())
				devices.addDevice(disk);
		}

		serialDef serial = new serialDef("pty", null, (short)0);
		devices.addDevice(serial);

		consoleDef console = new consoleDef("pty", null, null, (short)0);
		devices.addDevice(console);

		graphicDef	grap = new graphicDef("vnc", (short)0, true, null, null, null);
		devices.addDevice(grap);

		inputDef input = new inputDef("tablet", "usb");
		devices.addDevice(input);

		vm.addComp(devices);

		String domXML = vm.toString();

		s_logger.debug(domXML);
		return domXML;
	}
	protected String startDomain(String vmName, String domainXML) throws LibvirtException{
		/*No duplicated vm, we will success, or failed*/
		boolean failed =false;
		Domain dm = null;
		try {
			dm = _conn.domainDefineXML(domainXML);
		} catch (final LibvirtException e) {
			/*Duplicated defined vm*/
			s_logger.warn("Failed to define domain " + vmName + ": " + e.getMessage());
			failed = true;
		} finally {
			try {
				if (dm != null)
					dm.free();
			} catch (final LibvirtException e) {
				
			}
		}
		
		/*If failed, undefine the vm*/
		Domain dmOld = null;
		Domain dmNew = null;
		try {
			if (failed) {
				dmOld = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
				dmOld.undefine();
				dmNew = _conn.domainDefineXML(domainXML);
			}
		} catch (final LibvirtException e) {
			s_logger.warn("Failed to define domain (second time) " + vmName + ": " + e.getMessage());
			throw e;
		} finally {
			try {
				if (dmOld != null)
					dmOld.free();
				if (dmNew != null)
					dmNew.free();
			} catch (final LibvirtException e) {
				
			}
		}

		/*Start the VM*/
		try {
			dm = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
			dm.create();
		} catch (LibvirtException e) {
			s_logger.warn("Failed to start domain: " + vmName + ": " + e.getMessage());
			throw e;
		} finally {
			try {
				if (dm != null)
					dm.free();
			} catch (final LibvirtException e) {
				
			}
		}
		return null;
	}
		
	@Override
	public boolean stop() {
		if (_conn != null) {
			try {
				_conn.close();
			} catch (LibvirtException e) {
			}
			_conn = null;
		}
		return true;
	}

	public static void main(String[] args) {
		s_logger.addAppender(new org.apache.log4j.ConsoleAppender(new org.apache.log4j.PatternLayout(), "System.out"));
		LibvirtComputingResource test = new LibvirtComputingResource();
		Map<String, Object> params = new HashMap<String, Object>();
		try {
			test.configure("test", params);
		} catch (ConfigurationException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		String result = null;
		//String result = test.startDomainRouter("domr1", "/var/lib/images/centos.5-4.x86-64/centos-small.img", 128, "0064", "02:00:30:00:01:01", "00:16:3e:77:e2:a1", "02:00:30:00:64:01");
		boolean created = (result==null);
		s_logger.info("Domain " + (created?" ":" not ") + " created");
		
		s_logger.info("Rule " + (created?" ":" not ") + " created");
		test.stop();

	}

	@Override
	public Answer executeRequest(Command cmd) {

        try {
            if (cmd instanceof StartCommand) {
                return execute((StartCommand)cmd);
            } else if (cmd instanceof StopCommand) {
                return execute((StopCommand)cmd);
            } else if (cmd instanceof GetVmStatsCommand) {
                return execute((GetVmStatsCommand)cmd);
            } else if (cmd instanceof RebootCommand) {
                return execute((RebootCommand)cmd);
            } else if (cmd instanceof GetHostStatsCommand) {
                return execute((GetHostStatsCommand)cmd);
            } else if (cmd instanceof CheckStateCommand) {
                return executeRequest(cmd);
            } else if (cmd instanceof MirrorCommand) {
                return executeRequest(cmd);
            } else if (cmd instanceof CheckHealthCommand) {
                return execute((CheckHealthCommand)cmd);
            } else if (cmd instanceof PrepareForMigrationCommand) {
                return execute((PrepareForMigrationCommand)cmd);
            } else if (cmd instanceof MigrateCommand) {
                return execute((MigrateCommand)cmd);
            } else if (cmd instanceof PingTestCommand) {
                return execute((PingTestCommand)cmd);
            } else if (cmd instanceof CheckVirtualMachineCommand) {
                return execute((CheckVirtualMachineCommand)cmd);
            } else if (cmd instanceof ReadyCommand) {
            	return execute((ReadyCommand)cmd);
            } else if (cmd instanceof StartRouterCommand) {
            	return execute((StartRouterCommand)cmd);
            } else if(cmd instanceof StartConsoleProxyCommand) {
            	return execute((StartConsoleProxyCommand)cmd);
            } else if (cmd instanceof AttachIsoCommand) {
            	return execute((AttachIsoCommand) cmd);
            } else if (cmd instanceof AttachVolumeCommand) {
            	return execute((AttachVolumeCommand) cmd);
            } else if (cmd instanceof StopCommand) {
            	return execute((StopCommand)cmd);
            } else if (cmd instanceof CheckConsoleProxyLoadCommand) {
            	return execute((CheckConsoleProxyLoadCommand)cmd);
            } else if (cmd instanceof WatchConsoleProxyLoadCommand) {
            	return execute((WatchConsoleProxyLoadCommand)cmd);
            } else if (cmd instanceof GetVncPortCommand) {
            	return execute((GetVncPortCommand)cmd);
            } else if (cmd instanceof ModifySshKeysCommand) {
            	return execute((ModifySshKeysCommand)cmd);
            } else if (cmd instanceof MaintainCommand) {
                return execute((MaintainCommand) cmd);
            } else if (cmd instanceof CreateCommand) {
                return execute((CreateCommand) cmd);
            } else if (cmd instanceof DestroyCommand) {
                return execute((DestroyCommand) cmd);
            } else if (cmd instanceof PrimaryStorageDownloadCommand) {
                return execute((PrimaryStorageDownloadCommand) cmd);
            } else if (cmd instanceof CreatePrivateTemplateCommand) {
                return execute((CreatePrivateTemplateCommand) cmd);
            } else if (cmd instanceof GetStorageStatsCommand) {
                return execute((GetStorageStatsCommand) cmd);
            } else if (cmd instanceof ManageSnapshotCommand) {
                return execute((ManageSnapshotCommand) cmd);
            } else if (cmd instanceof ModifyStoragePoolCommand) {
                return execute((ModifyStoragePoolCommand) cmd);
            } else if (cmd instanceof NetworkIngressRulesCmd) {
                return execute((NetworkIngressRulesCmd) cmd);
            } else if (cmd instanceof DeleteStoragePoolCommand) {
                return execute((DeleteStoragePoolCommand) cmd);
            } else if (cmd instanceof RoutingCommand) {
            	return _virtRouterResource.executeRequest(cmd);
            } else {
        		s_logger.warn("Unsupported command ");
                return Answer.createUnsupportedCommandAnswer(cmd);
            }
        } catch (final IllegalArgumentException e) {
            return new Answer(cmd, false, e.getMessage());
        }
	}
	
	
	protected Answer execute(DeleteStoragePoolCommand cmd) {
		try {
			StoragePool pool = _conn.storagePoolLookupByUUIDString(cmd.getPool().getUuid());
			pool.destroy();
			pool.undefine();
			return new Answer(cmd);
		} catch (LibvirtException e) {
			return new Answer(cmd, false, e.toString());
		}
	}
	
	protected StorageResourceType getStorageResourceType() {
		return StorageResourceType.STORAGE_POOL;
	}

    protected Answer execute(CreateCommand cmd) {
    	  StoragePoolTO pool = cmd.getPool();
          DiskCharacteristicsTO dskch = cmd.getDiskCharacteristics();
          StorageVol tmplVol = null;
          StoragePool primaryPool = null;
          StorageVol vol = null;
          long disksize;
          try {
        	  primaryPool = _conn.storagePoolLookupByUUIDString(pool.getUuid());
        	  if (primaryPool == null) {
        		  String result = "Failed to get primary pool";
        		  s_logger.debug(result);
        		  new CreateAnswer(cmd, result);
        	  }
        	  
        	  if (cmd.getTemplateUrl() != null) {
        		  tmplVol = getVolume(primaryPool, cmd.getTemplateUrl());
        		  if (tmplVol == null) {
        			  String result = "Failed to get tmpl vol";
        			  s_logger.debug(result);
        			  new CreateAnswer(cmd, result);
        		  }
        		  
        		  vol = createVolume(primaryPool, tmplVol);

        		  if (vol == null) {
        			  return new Answer(cmd, false, " Can't create storage volume on storage pool");
        		  }
        		  disksize = tmplVol.getInfo().capacity;
        	  } else {
        		  disksize = dskch.getSize();
        		  LibvirtStorageVolumeDef volDef = new LibvirtStorageVolumeDef(UUID.randomUUID().toString(), disksize, volFormat.QCOW2, null, null);
        		  s_logger.debug(volDef.toString());
        		  vol = primaryPool.storageVolCreateXML(volDef.toString(), 0);
        		  
        	  }
        	  VolumeTO volume = new VolumeTO(cmd.getVolumeId(), dskch.getType(), getStorageResourceType(), pool.getType(), pool.getPath(), vol.getName(),vol.getKey(), disksize);
        	  return new CreateAnswer(cmd, volume);
          } catch (LibvirtException e) {
        	 
        	  s_logger.debug("Failed to create volume: " + e.toString());
        	  return new CreateAnswer(cmd, e);
          } finally {
        	  try {
        		  if (vol != null) {
        			  vol.free();
        		  }
        		  if (tmplVol != null) {
        			  tmplVol.free();
        		  }
        		  if (primaryPool != null) {
        			  primaryPool.free();
        		  }
        	  } catch (LibvirtException e) {
        		  
        	  }
          }
    }
    
    public Answer execute(DestroyCommand cmd) {
    	 VolumeTO vol = cmd.getVolume();

    	 StorageVol volume = getVolume(vol.getPath());
    	 if (volume == null) {
    		 s_logger.debug("Failed to find the volume: " + vol.getPath());
    		 return new Answer(cmd, true, "Success");
    	 }
    	 try {
    		 volume.delete(0);
    		 volume.free();
    	 } catch (LibvirtException e) {
    		 s_logger.debug("Failed to delete volume: " + e.toString());
    		 return new Answer(cmd, false, e.toString());
    	 }
    	 return new Answer(cmd, true, "Success");
    }
    
    protected ManageSnapshotAnswer execute(final ManageSnapshotCommand cmd) {
    	/*TODO: no snapshot support for KVM right now, but create_private_template needs us to return true here*/
   	 	return new ManageSnapshotAnswer(cmd, cmd.getSnapshotId(), cmd.getVolumePath(), true, null);
    }
    
    protected GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {
    	StoragePool sp = null;
    	StoragePoolInfo spi = null;
    	try {
    		sp = _conn.storagePoolLookupByUUIDString(cmd.getStorageId());
    		spi = sp.getInfo();
    		return new GetStorageStatsAnswer(cmd, spi.capacity, spi.allocation);
    	} catch (LibvirtException e) {
    		return new GetStorageStatsAnswer(cmd, e.toString());
    	}
    }
    protected CreatePrivateTemplateAnswer execute(CreatePrivateTemplateCommand cmd) {
    	String secondaryStorageURL = cmd.getSecondaryStorageURL();
    	StorageVol snapshotVol = null;
    	String snapshotUUID = cmd.getSnapshotPath();
    	
        StoragePool secondaryStorage = null;
        try {
       	 String templateFolder = cmd.getAccountId() + File.separator + cmd.getTemplateId() + File.separator;
       	 String templateInstallFolder = "/template/tmpl/" + templateFolder;

       	 secondaryStorage = getNfsSPbyURI(_conn, new URI(secondaryStorageURL));
       	 /*TODO: assuming all the storage pools mounted under _mountPoint, the mount point should be got from pool.dumpxml*/
       	 String tmpltPath = _mountPoint + File.separator + secondaryStorage.getUUIDString() + templateInstallFolder;
       	 _storage.mkdirs(tmpltPath);

       	 snapshotVol = getVolume(snapshotUUID);
       	 String snapshotPath =  snapshotVol.getPath();
       	 
       	 Script.runSimpleBashScript("qemu-img convert -f qcow2 -O qcow2 " + snapshotPath + " " + tmpltPath + File.separator + cmd.getUniqueName() + ".qcow2");
       	 Script.runSimpleBashScript("touch " + tmpltPath + File.separator + "template.properties");
       	 Script.runSimpleBashScript("chmod a+r " + tmpltPath + File.separator + cmd.getUniqueName() + ".qcow2");
       	 Script.runSimpleBashScript("chmod a+r " + tmpltPath + File.separator + "template.properties");
       	 
       	 Map<String, Object> params = new HashMap<String, Object>();
       	 params.put(StorageLayer.InstanceConfigKey, _storage);
       	 Processor qcow2Processor = new QCOW2Processor();

       	 qcow2Processor.configure("QCOW2 Processor", params);

       	 FormatInfo info = qcow2Processor.process(tmpltPath, null, cmd.getUniqueName());

       	 TemplateLocation loc = new TemplateLocation(_storage, tmpltPath);
       	 loc.create(1, true, cmd.getUniqueName());
       	 loc.addFormat(info);
       	 loc.save();

       	 return new CreatePrivateTemplateAnswer(cmd,
       			 true,
       			 null,
       			 templateInstallFolder + cmd.getUniqueName() + ".qcow2",
       			 info.virtualSize,
       			 cmd.getUniqueName(),
       			 ImageFormat.QCOW2);
        } catch (URISyntaxException e) {
       	 return new CreatePrivateTemplateAnswer(cmd,
       			 false,
       			 e.toString(),
       			 null,
       			 0,
       			 null,
       			 null);
        } catch (LibvirtException e) {
       	 s_logger.debug("Failed to get secondary storage pool: " + e.toString());
       	 return new CreatePrivateTemplateAnswer(cmd,
       			 false,
       			 e.toString(),
       			 null,
       			 0,
       			 null,
       			 null);
        } catch (InternalErrorException e) {
       	 return new CreatePrivateTemplateAnswer(cmd,
       			 false,
       			 e.toString(),
       			 null,
       			 0,
       			 null,
       			 null);
		} catch (IOException e) {
			return new CreatePrivateTemplateAnswer(cmd,
      			 false,
      			 e.toString(),
      			 null,
      			 0,
      			 null,
      			 null);
		} catch (ConfigurationException e) {
			return new CreatePrivateTemplateAnswer(cmd,
	       			 false,
	       			 e.toString(),
	       			 null,
	       			 0,
	       			 null,
	       			 null);
		}

    }
    
    private StoragePool getNfsSPbyURI(Connect conn, URI uri) throws LibvirtException {
    	  String sourcePath = uri.getPath();
    	  sourcePath = sourcePath.replace("//", "/");
          String sourceHost = uri.getHost();
          String uuid = UUID.nameUUIDFromBytes(new String(sourceHost + sourcePath).getBytes()).toString();
          String targetPath = _mountPoint + File.separator + uuid;
          StoragePool sp = null;
          try {
        	  sp = conn.storagePoolLookupByUUIDString(uuid);
          }  catch (LibvirtException e) {
          }
          
          if (sp == null) {
        	  try {
        		  File tpFile = new File(targetPath);
        		  if (!tpFile.exists()) {
        			  tpFile.mkdir();
        		  }
        		  LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(poolType.NFS, uuid, uuid,
        				  sourceHost, sourcePath, targetPath);
        		  s_logger.debug(spd.toString());
        		  sp = conn.storagePoolDefineXML(spd.toString(), 0);
        		  if (sp == null) {
        			  s_logger.debug("Failed to define storage pool");
        			  return null;
        		  }
        		  sp.create(0);

        		  return sp;
        	  } catch (LibvirtException e) {
        		  try {
        			  if (sp != null) {
        				  sp.undefine();
        				  sp.free();
        			  }
        		  } catch (LibvirtException l) {

        		  }
        		  throw e;
        	  }
          } else {
        	  StoragePoolInfo spi = sp.getInfo();
        	  if (spi.state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
        		  sp.create(0);
        	  }
        	  return sp;
          }
    }
    protected Answer execute(final PrimaryStorageDownloadCommand cmd) {
    	 String tmplturl = cmd.getUrl();
         int index = tmplturl.lastIndexOf("/");
         String mountpoint = tmplturl.substring(0, index);
         String tmpltname = null;
         if (index < tmplturl.length() - 1)
             tmpltname = tmplturl.substring(index + 1);
         
         StoragePool secondaryPool = null;
         StoragePool primaryPool = null;
         StorageVol tmplVol = null;
         StorageVol primaryVol = null;
         String result;
         try {
        	 secondaryPool = getNfsSPbyURI(_conn, new URI(mountpoint));
        	 if (secondaryPool == null) {
        		 return new Answer(cmd, false, " Failed to create storage pool");
        	 }
        	 tmplVol = secondaryPool.storageVolLookupByName(tmpltname);
        	 if (tmplVol == null) {
        		 return new Answer(cmd, false, " Can't find volume");
        	 }
        	 primaryPool = _conn.storagePoolLookupByUUIDString(cmd.getPoolUuid());
        	 if (primaryPool == null) {
        		 return new Answer(cmd, false, " Can't find primary storage pool");
        	 }
        	 
        	 LibvirtStorageVolumeDef vol = new LibvirtStorageVolumeDef(UUID.randomUUID().toString(), tmplVol.getInfo().capacity, volFormat.QCOW2, null, null);
        	 s_logger.debug(vol.toString());
        	 primaryVol = copyVolume(primaryPool, vol, tmplVol);
        	 if (primaryVol == null) {
        		 return new Answer(cmd, false, " Can't create storage volume on storage pool");
        	 }
        	 StorageVolInfo priVolInfo = primaryVol.getInfo();
        	 DownloadAnswer answer = new DownloadAnswer(null,
                     100,
                     cmd,
                     com.cloud.storage.VMTemplateStorageResourceAssoc.Status.DOWNLOADED,
                     primaryVol.getKey(),
                     primaryVol.getKey());
        	 answer.setTemplateSize(priVolInfo.allocation);
        	 return answer;
         } catch (LibvirtException e) {
        	 result = "Failed to download template: " + e.toString();
        	 s_logger.debug(result);
        	 return new Answer(cmd, false, result);
         } catch (URISyntaxException e) {
			// TODO Auto-generated catch block
        	 return new Answer(cmd, false, e.toString());
		} finally {
			try {
				if (primaryVol != null) {
					primaryVol.free();
				}

				if (primaryPool != null) {
					primaryPool.free();
				}

				if (tmplVol != null) {
					tmplVol.free();
				}

				if (secondaryPool != null) {
					secondaryPool.free();
				}
			} catch (LibvirtException l) {

			}
		}

    }
    
    private StoragePool createNfsStoragePool(Connect conn, StoragePoolVO pool) {
    	String targetPath = _mountPoint + File.separator + pool.getUuid();
    	LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(poolType.NFS, pool.getUuid(), pool.getUuid(),
    														  pool.getHostAddress(), pool.getPath(), targetPath);
    	File tpFile = new File(targetPath);
		  if (!tpFile.exists()) {
			  tpFile.mkdir();
		  }
    	StoragePool sp = null;
    	try {
    		s_logger.debug(spd.toString());
    		sp = conn.storagePoolDefineXML(spd.toString(), 0);
    		sp.create(0);
    		return sp;
    	} catch (LibvirtException e) {
    		s_logger.debug(e.toString());
    		if (sp != null) {
    			try {
    				sp.undefine();
    				sp.free();
    			} catch (LibvirtException l) {
    				s_logger.debug("Failed to define nfs storage pool with: " + l.toString());
    			}
    		}
    		return null;
    	}
    }
    
    private StoragePool getStoragePool(Connect conn, StoragePoolVO pool) {
    	StoragePool sp = null;
    	try {
    		sp = conn.storagePoolLookupByUUIDString(pool.getUuid());
    	} catch (LibvirtException e) {
    		
    	}
    	
    	if (sp == null) {
			if (pool.getPoolType() == StoragePoolType.NetworkFilesystem) {
				sp = createNfsStoragePool(conn, pool);
			}
			if (sp == null) {
				s_logger.debug("Failed to create storage Pool");
				return null;
			}
		}
    	
    	try {
    		StoragePoolInfo spi = sp.getInfo();
    		if (spi.state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
    			sp.create(0);
    		}
    	} catch (LibvirtException e) {

    	}
		return sp;
    }
    
    protected Answer execute(ModifyStoragePoolCommand cmd) {
    	StoragePool storagePool = getStoragePool(_conn, cmd.getPool());
    	if (storagePool == null) {
    		return new Answer(cmd, false, " Failed to create storage pool");
    	}
    	
    	StoragePoolInfo spi = null;
    	try {
    		spi = storagePool.getInfo();
    	} catch (LibvirtException e) {
    		
    	}
    	
    	Map<String, TemplateInfo> tInfo = new HashMap<String, TemplateInfo>();
        ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd,
                                                                     spi.capacity,
                                                                     spi.allocation,
                                                                     tInfo);
        try {
        	storagePool.free();
        } catch (LibvirtException e) {
    		
    	}
        return answer;
    }
    private Answer execute(NetworkIngressRulesCmd cmd) {
    	 return new NetworkIngressRuleAnswer(cmd);
    }
    
	protected GetVncPortAnswer execute(GetVncPortCommand cmd) {
		return new GetVncPortAnswer(cmd, 5900 + getVncPort(cmd.getName()));
	}
	
	protected Answer execute(final CheckConsoleProxyLoadCommand cmd) {
		return executeProxyLoadScan(cmd, cmd.getProxyVmId(), cmd.getProxyVmName(), cmd.getProxyManagementIp(), cmd.getProxyCmdPort());
	}

	protected Answer execute(final WatchConsoleProxyLoadCommand cmd) {
		return executeProxyLoadScan(cmd, cmd.getProxyVmId(), cmd.getProxyVmName(), cmd.getProxyManagementIp(), cmd.getProxyCmdPort());
	}

	protected MaintainAnswer execute(MaintainCommand cmd) {
		return new MaintainAnswer(cmd);
	}
	 
	private Answer executeProxyLoadScan(final Command cmd, final long proxyVmId, final String proxyVmName, final String proxyManagementIp, final int cmdPort) {
		String result = null;

		final StringBuffer sb = new StringBuffer();
		sb.append("http://").append(proxyManagementIp).append(":" + cmdPort).append("/cmd/getstatus");

		boolean success = true;
		try {
			final URL url = new URL(sb.toString());
			final URLConnection conn = url.openConnection();

			final InputStream is = conn.getInputStream();
			final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
			final StringBuilder sb2 = new StringBuilder();
			String line = null;
			try {
				while ((line = reader.readLine()) != null)
					sb2.append(line + "\n");
				result = sb2.toString();
			} catch (final IOException e) {
				success = false;
			} finally {
				try {
					is.close();
				} catch (final IOException e) {
					s_logger.warn("Exception when closing , console proxy address : " + proxyManagementIp);
					success = false;
				}
			}
		} catch(final IOException e) {
			s_logger.warn("Unable to open console proxy command port url, console proxy address : " + proxyManagementIp);
			success = false;
		}

		return new ConsoleProxyLoadAnswer(cmd, proxyVmId, proxyVmName, success, result);
	}

	private Answer execute(StartConsoleProxyCommand cmd) {
		final ConsoleProxyVO router = cmd.getProxy();
		String result = null;

		State state = State.Stopped;
		synchronized(_vms) {
			_vms.put(cmd.getVmName(), State.Starting);
		}
		try {
			result = startConsoleProxy(cmd);
			if (result != null) {
				throw new ExecutionException(result, null);
			}

			result = _virtRouterResource.connect(router.getPrivateIpAddress(), cmd.getProxyCmdPort());
			if (result != null) {
				throw new ExecutionException(result, null);
			}

			state = State.Running;
			return new StartConsoleProxyAnswer(cmd, router.getPrivateIpAddress(), router.getPrivateMacAddress());
		} catch (final ExecutionException e) {
			return new Answer(cmd, false, e.getMessage());
		} catch (final Throwable th) {
			s_logger.warn("Exception while starting router.", th);
			return createErrorAnswer(cmd, "Unable to start router", th);
		} finally {
			synchronized(_vms) {
				_vms.put(cmd.getVmName(), state);
			}
		}
	}

	private Answer execute(AttachIsoCommand cmd) {
		try {
			attachOrDetachISO(cmd.getVmName(), cmd.getIsoPath(), cmd.isAttach());
		} catch (LibvirtException e) {
			return new Answer(cmd, false, e.toString());
		} catch (URISyntaxException e) {
			return new Answer(cmd, false, e.toString());
		}
		
		return new Answer(cmd);
	}
	
	private AttachVolumeAnswer execute(AttachVolumeCommand cmd) {
		try {
			attachOrDetachDisk(cmd.getAttach(), cmd.getVmName(), cmd.getVolumePath());
		} catch (LibvirtException e) {
			return new AttachVolumeAnswer(cmd, e.toString());
		}
		
		return new AttachVolumeAnswer(cmd, cmd.getDeviceId());
	}
	
	protected static List<VolumeVO> findVolumes(final List<VolumeVO> volumes, final Volume.VolumeType vType, boolean singleVolume) {
		List<VolumeVO> filteredVolumes = new ArrayList<VolumeVO>();
		
		if (volumes == null)
			return filteredVolumes;

		for (final VolumeVO v: volumes) {
			if (v.getVolumeType() == vType) {
				filteredVolumes.add(v);
				
				if(singleVolume)
					return filteredVolumes;
			}
		}

		return filteredVolumes;
	}
	
	private Answer execute(StartRouterCommand cmd) {

		final DomainRouter router = cmd.getRouter();

		String result = null;

		State state = State.Stopped;
		synchronized(_vms) {
			_vms.put(cmd.getVmName(), State.Starting);
		}
		try {
			result = startDomainRouter(cmd);
			if (result != null) {
				throw new ExecutionException(result, null);
			}

			result = _virtRouterResource.connect(router.getPrivateIpAddress());
			if (result != null) {
				throw new ExecutionException(result, null);
			}

			state = State.Running;
			return new StartRouterAnswer(cmd);
		} catch (final ExecutionException e) {
			return new Answer(cmd, false, e.getMessage());
		} catch (final Throwable th) {
			return createErrorAnswer(cmd, "Unable to start router", th);
		} finally {
			synchronized(_vms) {
				_vms.put(cmd.getVmName(), state);
			}
		}
	}

	private Answer execute(ReadyCommand cmd) {
		return new ReadyAnswer(cmd);
	}
	
	protected State convertToState(DomainInfo.DomainState ps) {
		final State state = s_statesTable.get(ps);
		return state == null ? State.Unknown : state;
	}

	protected State getVmState(final String vmName) {
		int retry = 3;
		Domain vms = null;
		while (retry-- > 0) {
			try {
				vms = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
				State s = convertToState(vms.getInfo().state);
				return s;
			} catch (final LibvirtException e) {
				s_logger.warn("Can't get vm state " + vmName + e.getMessage() + "retry:" + retry);
			} finally {
				try {
					if (vms != null) {
						vms.free();
					}
				} catch (final LibvirtException e) {

				}
			}
		}
		return State.Stopped;
	}

	private Answer execute(CheckVirtualMachineCommand cmd) {
		if (VirtualMachineName.isValidRouterName(cmd.getVmName()) || VirtualMachineName.isValidConsoleProxyName(cmd.getVmName()) ) {
			/*For domr, the trick is that the actual vmname is vmName-domrId.
			 *Here, we need to build the relationship between vmName and its actual name at first*/
			getAllVms();
		}

        final State state = getVmState(cmd.getVmName());
        Integer vncPort = null;
        if (state == State.Running) {
            vncPort = getVncPort(cmd.getVmName());
            synchronized(_vms) {
                _vms.put(cmd.getVmName(), State.Running);
            }
        }
        
        return new CheckVirtualMachineAnswer(cmd, state, vncPort);
	}

	private Answer execute(PingTestCommand cmd) {
        String result = null;
        final String computingHostIp = cmd.getComputingHostIp(); //TODO, split the command into 2 types

        if (computingHostIp != null) {
            result = doPingTest(computingHostIp);
        } else {
            result = doPingTest(cmd.getRouterIp(), cmd.getPrivateIp());
        }

        if (result != null) {
            return new Answer(cmd, false, result);
        }
        return new Answer(cmd);
	}

    private String doPingTest( final String computingHostIp ) {
        final Script command = new Script(_pingTestPath, 10000, s_logger);
        command.add("-h", computingHostIp);
        return command.execute();
    }

    private String doPingTest( final String domRIp, final String vmIp ) {
        final Script command = new Script(_pingTestPath, 10000, s_logger);
        command.add("-i", domRIp);
        command.add("-p", vmIp);
        return command.execute();
    }
    
	private Answer execute(MigrateCommand cmd) {
		String vmName = cmd.getVmName();
    	
		State state = null;
		String result = null;
		synchronized(_vms) {
			state = _vms.get(vmName);
			_vms.put(vmName, State.Stopping);
		}
		
		Domain dm = null;
		Connect dconn = null;
		Domain destDomain = null;
		try {
			dm = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
			dconn = new Connect("qemu+tcp://" + cmd.getDestinationIp() + "/system");
			/*Hard code lm flags: VIR_MIGRATE_LIVE(1<<0) and VIR_MIGRATE_PERSIST_DEST(1<<3)*/
			destDomain = dm.migrate(dconn, (1<<0)|(1<<3), vmName, "tcp:" + cmd.getDestinationIp(), 0);
		} catch (LibvirtException e) {
			s_logger.debug("Can't migrate domain: " + e.getMessage());
			result = e.getMessage();
		} finally {
			try {
				if (dm != null)
					dm.free();
				if (dconn != null)
					dconn.close();
				if (destDomain != null)
					destDomain.free();
			} catch (final LibvirtException e) {

			}
		}
		
		if (result != null) {
			synchronized(_vms) {
				_vms.put(vmName, state);
			}
		} else {
			cleanupVM(vmName, getVnetId(VirtualMachineName.getVnet(vmName)));
		}

		return new MigrateAnswer(cmd, result == null, result, null);
	}

	private Answer execute(PrepareForMigrationCommand cmd) {
		final String vmName = cmd.getVmName();
		String result = null;
		
		if (cmd.getVnet() != null && !isDirectAttachedNetwork(cmd.getVnet())) {
			final String vnet = getVnetId(cmd.getVnet());
			if (vnet != null) {
				try {
					createVnet(vnet);
				} catch (InternalErrorException e) {
					return new PrepareForMigrationAnswer(cmd, false, result);
				}
			}
		}

		synchronized(_vms) {
			_vms.put(vmName, State.Migrating);
		}

		return new PrepareForMigrationAnswer(cmd, result == null, result);
	}
	
    public void createVnet(String vnetId) throws InternalErrorException {
        final Script command  = new Script(_createvnetPath, _timeout, s_logger);
        command.add("-v", vnetId);

        final String result = command.execute();
       	if (result != null) {
       		throw new InternalErrorException("Failed to create vnet " + vnetId + ": " + result);
       	}
    }
    
	private Answer execute(CheckHealthCommand cmd) {
		return new CheckHealthAnswer(cmd, true);
	}
	
	private Answer execute(GetHostStatsCommand cmd) {
		
		
		final Script cpuScript = new Script("/bin/bash", s_logger);
		cpuScript.add("-c");
		cpuScript.add("idle=$(top -b -n 1|grep Cpu\\(s\\):|cut -d% -f4|cut -d, -f2);echo $idle");
		
		final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
		String result = cpuScript.execute(parser);
		if (result != null) {
			s_logger.info("Unable to get the host CPU state: " + result);
			return new Answer(cmd, false, result);
		}
		double cpuUtil = (100.0D - Double.parseDouble(parser.getLine()));
		
		long freeMem = 0;
		final Script memScript = new Script("/bin/bash", s_logger);
		memScript.add("-c");
		memScript.add("freeMem=$(free|grep cache:|awk '{print $4}');echo $freeMem");
		final OutputInterpreter.OneLineParser Memparser = new OutputInterpreter.OneLineParser();
		result = memScript.execute(Memparser);
		if (result != null) {
			s_logger.info("Unable to get the host Mem state: " + result);
			return new Answer(cmd, false, result);
		}
		freeMem = Long.parseLong(Memparser.getLine());
		
		Script totalMem = new Script("/bin/bash", s_logger);
		totalMem.add("-c");
		totalMem.add("free|grep Mem:|awk '{print $2}'");
		final OutputInterpreter.OneLineParser totMemparser = new OutputInterpreter.OneLineParser();
		result = totalMem.execute(totMemparser);
		if (result != null) {
			s_logger.info("Unable to get the host Mem state: " + result);
			return new Answer(cmd, false, result);
		}
		long totMem = Long.parseLong(totMemparser.getLine());
		
		double rx = 0.0;
		OutputInterpreter.OneLineParser rxParser = new  OutputInterpreter.OneLineParser();
		result = executeBashScript("cat /sys/class/net/" + _publicBridgeName + "/statistics/rx_bytes", rxParser);
		if (result == null && rxParser.getLine() != null) {
			rx = Double.parseDouble(rxParser.getLine())/1000;
		}
		
		double tx = 0.0;
		OutputInterpreter.OneLineParser txParser = new  OutputInterpreter.OneLineParser();
		result = executeBashScript("cat /sys/class/net/" + _publicBridgeName + "/statistics/tx_bytes", txParser);
		if (result == null && txParser.getLine() != null) {
			tx = Double.parseDouble(txParser.getLine())/1000;
		}
		
		int numCpus = 0;
		try {
			NodeInfo node = _conn.nodeInfo();
			numCpus = node.cpus;
		} catch (LibvirtException e) {
			
		}
		
		 HostStatsEntry hostStats = new HostStatsEntry(cmd.getHostId(), cpuUtil, rx, tx, numCpus, "host", totMem, freeMem, 0, 0);
		return new GetHostStatsAnswer(cmd, hostStats);
	}

	private Answer execute(RebootCommand cmd) {
		Long bytesReceived = null;
    	Long bytesSent = null;

    	synchronized(_vms) {
    		_vms.put(cmd.getVmName(), State.Starting);
    	}
    
    	try {
	    	final String result = rebootVM(cmd.getVmName());
	    	if (result == null) {
	    		/*TODO: need to reset iptables rules*/
	    		return new RebootAnswer(cmd, null, bytesSent, bytesReceived, getVncPort(cmd.getVmName()));
	    	} else {
	    		return new RebootAnswer(cmd, result);
	    	}
    	} finally {
    		synchronized(_vms) {
    			_vms.put(cmd.getVmName(), State.Running);
    		}
    	}
	}

	protected GetVmStatsAnswer execute(GetVmStatsCommand cmd) {
		List<String> vmNames = cmd.getVmNames();
		try {
			HashMap<String, VmStatsEntry> vmStatsNameMap = new HashMap<String, VmStatsEntry>();

			for (String vmName : vmNames) {
				VmStatsEntry statEntry = getVmStat(vmName);
				if( statEntry == null )
					continue;

				vmStatsNameMap.put(vmName, statEntry);
			}
			return new GetVmStatsAnswer(cmd, vmStatsNameMap);
		}catch (LibvirtException e) {
			s_logger.debug("Can't get vm stats: " + e.toString());
			return new GetVmStatsAnswer(cmd, null);
		}
	}

	
	protected Answer execute(StopCommand cmd) {
	    StopAnswer answer = null;
        final String vmName = cmd.getVmName();
        
        final Integer port = getVncPort(vmName);
        Long bytesReceived = new Long(0);
        Long bytesSent = new Long(0);
        
        State state = null;
        synchronized(_vms) {
            state = _vms.get(vmName);
            _vms.put(vmName, State.Stopping);
        }
        try {
        	if (isDirectAttachedNetwork(cmd.getVnet()))
        		destroy_network_rules_for_vm(vmName);
            String result = stopVM(vmName, defineOps.UNDEFINE_VM);
            
            answer =  new StopAnswer(cmd, null, port, bytesSent, bytesReceived);
            
            if (result != null) {
                answer = new StopAnswer(cmd, result, port, bytesSent, bytesReceived);
            }
            
            final String result2 = cleanupVnet(cmd.getVnet());
            if (result2 != null) {
                result = result2 + (result != null ? ("\n" + result) : "") ;
                answer = new StopAnswer(cmd, result, port, bytesSent, bytesReceived);
            }
            
           
            return answer;
        } finally {
            if (answer == null || !answer.getResult()) {
                synchronized(_vms) {
                    _vms.put(vmName, state);
                }
            }
        }
	}

	protected Answer execute(ModifySshKeysCommand cmd) {
		File sshKeysDir = new File(_SSHKEYSPATH);
		String result = null;
		if (!sshKeysDir.exists()) {
			sshKeysDir.mkdir();
			  // Change permissions for the 600
	        Script script = new Script("chmod", _timeout, s_logger);
	        script.add("600", _SSHKEYSPATH);
	        script.execute();
		}
		
		File pubKeyFile = new File(_SSHPUBKEYPATH);
		if (!pubKeyFile.exists()) {
			try {
				pubKeyFile.createNewFile();
			} catch (IOException e) {
				result = "Failed to create file: " + e.toString();
				s_logger.debug(result);
			}
		}
		
		if (pubKeyFile.exists()) {
			String pubKey = cmd.getPubKey();
			try {
				FileOutputStream pubkStream = new FileOutputStream(pubKeyFile);
				pubkStream.write(pubKey.getBytes());
				pubkStream.close();
			} catch (FileNotFoundException e) {
				result = "File" + _SSHPUBKEYPATH + "is not found:" + e.toString();
				s_logger.debug(result);
			} catch (IOException e) {
				result = "Write file " + _SSHPUBKEYPATH + ":" + e.toString();
				s_logger.debug(result);
			}
		}
		
		File prvKeyFile = new File(_SSHPRVKEYPATH);
		if (!prvKeyFile.exists()) {
			try {
				prvKeyFile.createNewFile();
			} catch (IOException e) {
				result = "Failed to create file: " + e.toString();
				s_logger.debug(result);
			}
		}
		
		if (prvKeyFile.exists()) {
			String prvKey = cmd.getPrvKey();
			try {
				FileOutputStream prvKStream = new FileOutputStream(prvKeyFile);
				prvKStream.write(prvKey.getBytes());
				prvKStream.close();
			} catch (FileNotFoundException e) {
				result = "File" + _SSHPRVKEYPATH + "is not found:" + e.toString();
				s_logger.debug(result);
			} catch (IOException e) {
				result = "Write file " + _SSHPRVKEYPATH + ":" + e.toString();
				s_logger.debug(result);
			}

			Script script = new Script("chmod", _timeout, s_logger);
			script.add("600", _SSHPRVKEYPATH);
			script.execute();
		}

		if (result != null)
			return new Answer(cmd, false, result);
		else
			return new Answer(cmd, true, null);
	}
	
	protected StartAnswer execute(StartCommand cmd) {
        final String vmName = cmd.getVmName();
        String result = null;
        
        State state = State.Stopped;
        synchronized(_vms) {
            _vms.put(vmName, State.Starting);
        }
        
        try {
            result = startVM(cmd);
            if (result != null) {
                return new StartAnswer(cmd, result);
            }
            
            state = State.Running;
            return new StartAnswer(cmd);
        } finally {
            synchronized(_vms) {
                _vms.put(vmName, state);
            }
        }
	}
	
	private StorageVol getVolume(Connect conn, String volPath) throws LibvirtException, URISyntaxException {
		int index = volPath.lastIndexOf("/");
		URI volDir = null;
		StoragePool sp = null;
		StorageVol vol = null;
		try {
			volDir = new URI(volPath.substring(0, index));
			String volName = volPath.substring(index + 1);
			sp = getNfsSPbyURI(_conn, volDir);
			vol = sp.storageVolLookupByName(volName);
			return vol;
		} catch (LibvirtException e) {
			s_logger.debug("Faild to get vol path: " + e.toString());
			throw e;
		} finally {
			try {
				if (sp != null)
					sp.free();
			} catch (LibvirtException e) {

			}
		}
	}

	protected synchronized String startVM(StartCommand cmd) {
		List<interfaceDef> nics = null;
		try {
				
			String uuid = UUID.nameUUIDFromBytes(cmd.getVmName().getBytes()).toString();
			
			nics = createUserVMNetworks(cmd);
			
			List<diskDef> disks = createVMDisk(cmd.getVolumes(), cmd.getGuestOSDescription(), cmd.getISOPath());
			


			String vmDomainXML = defineVMXML(cmd.getVmName(), uuid, cmd.getRamSize(), cmd.getCpu(), cmd.getArch(),
											 nics, disks, cmd.getVncPassword(),
											 cmd.getGuestOSDescription());
		
			s_logger.debug(vmDomainXML);
			
			// Start the domain
			startDomain(cmd.getVmName(), vmDomainXML);

			// Attach each data volume to the VM, if there is a deferred attached disk
			for (diskDef disk : disks) {
				if (disk.isAttachDeferred()) {
					 attachOrDetachDisk(true, cmd.getVmName(), disk.getDiskPath());
				}
			}
			
			if (isDirectAttachedNetwork(cmd.getGuestNetworkId()))
				default_network_rules(cmd.getVmName(), cmd.getGuestIpAddress());
	
			return null;
		} catch(LibvirtException e) {
			if (nics != null)
				cleanupVMNetworks(nics);
			s_logger.error("Unable to start VM: ", e);
			return "Unable to start VM due to: " + e.getMessage();
		} catch (InternalErrorException e) {
			if (nics != null)
				cleanupVMNetworks(nics);
			s_logger.error("Unable to start VM: ", e);
			return "Unable to start VM due to: " + e.getMessage();
		} catch (URISyntaxException e) {
			if (nics != null)
				cleanupVMNetworks(nics);
			s_logger.error("Unable to start VM: ", e);
			return "Unable to start VM due to: " + e.getMessage();
		}
	}
	
	protected synchronized String attachOrDetachISO(String vmName, String isoPath, boolean isAttach) throws LibvirtException, URISyntaxException {
		String isoXml = null;
		if (isoPath != null && isAttach) {
			StorageVol isoVol = getVolume(_conn, isoPath);

			isoPath = isoVol.getPath();
		
			diskDef iso = new diskDef();
			iso.defFileBasedDisk(isoPath, "hdc", diskDef.diskBus.IDE, diskDef.diskFmtType.RAW);
			iso.setDeviceType(diskDef.deviceType.CDROM);
			isoXml = iso.toString();
		} else {
			diskDef iso = new diskDef();
			iso.defFileBasedDisk(null, "hdc", diskDef.diskBus.IDE, diskDef.diskFmtType.RAW);
			iso.setDeviceType(diskDef.deviceType.CDROM);
			isoXml = iso.toString();
		}
		
		return attachOrDetachDevice(true, vmName, isoXml);
	}
	
	protected synchronized String attachOrDetachDisk(boolean attach, String vmName, String sourceFile) throws LibvirtException {
		if (isCentosHost()) {
			return "disk hotplug is not supported by hypervisor";
		}
		String diskDev = null;
		SortedMap<String, String> diskMaps = null;
		Domain dm = null;
		try {
			dm = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
			LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
			String xml = dm.getXMLDesc(0);
			parser.parseDomainXML(xml);
			diskMaps = parser.getDiskMaps();
		} catch (LibvirtException e) {
			throw e;
		} finally {
			if (dm != null) {
				dm.free();
			}
		}

		if (attach) {
			diskDev = diskMaps.lastKey();
			/*Find the latest disk dev, and add 1 on it: e.g. if we already attach sdc to a vm, the next disk dev is sdd*/
			diskDev = diskDev.substring(0, diskDev.length() - 1) + (char)(diskDev.charAt(diskDev.length() -1) + 1);
		} else {
			Set<Map.Entry<String, String>> entrySet = diskMaps.entrySet();
			Iterator<Map.Entry<String, String>> itr = entrySet.iterator();
			while (itr.hasNext()) {
				Map.Entry<String, String> entry = itr.next();
				if (entry.getValue().equalsIgnoreCase(sourceFile)) {
					diskDev = entry.getKey();
					break;
				}
			}
		}

		if (diskDev == null) {
			s_logger.warn("Can't get disk dev");
			return "Can't get disk dev";
		}
		diskDef disk = new diskDef();
		String guestOSType = getGuestType(vmName);
		if (isGuestPVEnabled(guestOSType)) {
			disk.defFileBasedDisk(sourceFile, diskDev, diskDef.diskBus.VIRTIO, diskDef.diskFmtType.QCOW2);
		} else {
			disk.defFileBasedDisk(sourceFile, diskDev, diskDef.diskBus.SCSI, diskDef.diskFmtType.QCOW2);
		}
		String xml = disk.toString();
		return attachOrDetachDevice(attach, vmName, xml);
	}
	
	private synchronized String attachOrDetachDevice(boolean attach, String vmName, String xml) throws LibvirtException{
		Domain dm = null;
		try {
			dm = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes((vmName.getBytes())));
			
			if (attach) {
				s_logger.debug("Attaching device: " + xml);
				dm.attachDevice(xml);
			} else {
				s_logger.debug("Detaching device: " + xml);
				dm.detachDevice(xml);
			}
		} catch (LibvirtException e) {
			if (attach)
				s_logger.warn("Failed to attach device to " + vmName + ": " + e.getMessage());
			else
				s_logger.warn("Failed to detach device from " + vmName + ": " + e.getMessage());
			throw e;
		} finally {
			if (dm != null) {
				try {
					dm.free();
				} catch (LibvirtException l) {
					
				}
			}
		}
		
		return null;
	}

	@Override
	public PingCommand getCurrentStatus(long id) {
        final HashMap<String, State> newStates = sync();
        return new PingRoutingCommand(com.cloud.host.Host.Type.Routing, id, newStates);
	}

	@Override
	public Type getType() {
		return Type.Routing;
	}

	private Map<String, String> getVersionStrings() {
        final Script command = new Script(_versionstringpath, _timeout, s_logger);
        KeyValueInterpreter kvi = new KeyValueInterpreter();
        String result = command.execute(kvi);
        if (result == null) {
        	return kvi.getKeyValues();
        }else {
        	return new HashMap<String, String>(1);
        }
	}
	
	@Override
	public StartupCommand [] initialize() {
        Map<String, State> changes = null;

        synchronized(_vms) {
        	_vms.clear();
        	changes = sync();
        }
 
        final List<Object> info = getHostInfo();
      
        final StartupRoutingCommand cmd = new StartupRoutingCommand((Integer)info.get(0), (Long)info.get(1), (Long)info.get(2), (Long)info.get(4), (String)info.get(3), Hypervisor.Type.KVM, RouterPrivateIpStrategy.HostLocal, changes);
        fillNetworkInformation(cmd);
        cmd.getHostDetails().putAll(getVersionStrings());
        cmd.setPool(_pool);

        return new StartupCommand[]{cmd};
	}
	
	protected HashMap<String, State> sync() {
		HashMap<String, State> newStates;
		HashMap<String, State> oldStates = null;

		final HashMap<String, State> changes = new HashMap<String, State>();

		synchronized(_vms) {
			newStates = getAllVms();
			if (newStates == null) {
				s_logger.debug("Unable to get the vm states so no state sync at this point.");
				return changes;
			}

			oldStates = new HashMap<String, State>(_vms.size());
			oldStates.putAll(_vms);

			for (final Map.Entry<String, State> entry : newStates.entrySet()) {
				final String vm = entry.getKey();

				State newState = entry.getValue();
				final State oldState = oldStates.remove(vm);

				if (newState == State.Stopped && oldState != State.Stopping && oldState != null && oldState != State.Stopped) {
					newState = getRealPowerState(vm);
				}

				if (s_logger.isTraceEnabled()) {
					s_logger.trace("VM " + vm + ": libvirt has state " + newState + " and we have state " + (oldState != null ? oldState.toString() : "null"));
				}

				if (vm.startsWith("migrating")) {
					s_logger.debug("Migration detected.  Skipping");
					continue;
				}
				if (oldState == null) {
					_vms.put(vm, newState);
					s_logger.debug("Detecting a new state but couldn't find a old state so adding it to the changes: " + vm);
					changes.put(vm, newState);
				} else if (oldState == State.Starting) {
					if (newState == State.Running) {
						_vms.put(vm, newState);
					} else if (newState == State.Stopped) {
						s_logger.debug("Ignoring vm " + vm + " because of a lag in starting the vm." );
					}
				} else if (oldState == State.Migrating) {
					if (newState == State.Running) {
						s_logger.debug("Detected that an migrating VM is now running: " + vm);
						_vms.put(vm, newState);
					}
				} else if (oldState == State.Stopping) {
					if (newState == State.Stopped) {
						_vms.put(vm, newState);
					} else if (newState == State.Running) {
						s_logger.debug("Ignoring vm " + vm + " because of a lag in stopping the vm. ");
					}
				} else if (oldState != newState) {
					_vms.put(vm, newState);
					if (newState == State.Stopped) {
						if (_vmsKilled.remove(vm)) {
							s_logger.debug("VM " + vm + " has been killed for storage. ");
							newState = State.Error;
						}
					}
					changes.put(vm, newState);
				}
			}

			for (final Map.Entry<String, State> entry : oldStates.entrySet()) {
				final String vm = entry.getKey();
				final State oldState = entry.getValue();

				if (s_logger.isTraceEnabled()) {
					s_logger.trace("VM " + vm + " is now missing from libvirt so reporting stopped");
				}

				if (oldState == State.Stopping) {
					s_logger.debug("Ignoring VM " + vm + " in transition state stopping.");
					_vms.remove(vm);
				} else if (oldState == State.Starting) {
					s_logger.debug("Ignoring VM " + vm + " in transition state starting.");
				} else if (oldState == State.Stopped) {
					_vms.remove(vm);
				} else if (oldState == State.Migrating) {
					s_logger.debug("Ignoring VM " + vm + " in migrating state.");
				} else {
					State state = State.Stopped;
					if (_vmsKilled.remove(entry.getKey())) {
						s_logger.debug("VM " + vm + " has been killed by storage monitor");
						state = State.Error;
					}
					changes.put(entry.getKey(),	state);
				}
			}
		}

		return changes;
	}
	
	protected State getRealPowerState(String vm) {
        int i = 0;
        s_logger.trace("Checking on the HALTED State");
        Domain dm = null;
        for (; i < 5; i++) {
            try {
                dm = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vm.getBytes()));
                DomainInfo.DomainState vps = dm.getInfo().state;
                if (vps != null && vps != DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF &&
                    vps != DomainInfo.DomainState.VIR_DOMAIN_NOSTATE) {
                    return convertToState(vps);
                }
            } catch (final LibvirtException e) {
                s_logger.trace(e.getMessage());
            } finally {
            	try {
            		if (dm != null)
            			dm.free();
            	} catch (final LibvirtException e) {
            		
            	}
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        return State.Stopped;
	}
	
	protected List<String> getAllVmNames() {
		ArrayList<String> la = new ArrayList<String>();
		try {
			final String names[] = _conn.listDefinedDomains();
			for (int i = 0; i < names.length; i++) {
				la.add(names[i]);
			}
		} catch (final LibvirtException e) {
			s_logger.warn("Failed to list Defined domains", e);
		}

		int[] ids = null;
		try {
			ids = _conn.listDomains();
		} catch (final LibvirtException e) {
			s_logger.warn("Failed to list domains", e);
			return la;
		}
		
		Domain dm = null;
		for (int i = 0 ; i < ids.length; i++) {
			try {
				dm = _conn.domainLookupByID(ids[i]);
				la.add(dm.getName());
			} catch (final LibvirtException e) {
				s_logger.warn("Unable to get vms", e);
			} finally {
				try {
					if (dm != null)
						dm.free();
				} catch (final LibvirtException e) {

				}
			}
		}
		
		return la;
	}

	
	private HashMap<String, State> getAllVms() {
        final HashMap<String, State> vmStates = new HashMap<String, State>();

        String[] vms = null;
        int[] ids = null;
        try {
                ids = _conn.listDomains();
        } catch (final LibvirtException e) {
                s_logger.warn("Unable to listDomains", e);
                return null;
        }
        try {
                vms = _conn.listDefinedDomains();
        } catch (final LibvirtException e){
                s_logger.warn("Unable to listDomains", e);
                return null;
        }
        
        Domain dm = null;
        for (int i =0; i < ids.length; i++) {
            try {
                s_logger.debug("domid" + ids[i]);
                dm = _conn.domainLookupByID(ids[i]);

                DomainInfo.DomainState ps = dm.getInfo().state;

                final State state = convertToState(ps);

                s_logger.trace("VM " + dm.getName() + ": powerstate = " + ps + "; vm state=" + state.toString());
                String vmName = dm.getName();
                vmStates.put(vmName, state);
            } catch (final LibvirtException e) {
                s_logger.warn("Unable to get vms", e);
            } finally {
            	try {
            		if (dm != null)
            			dm.free();
            	} catch (LibvirtException e) {
            		
            	}
            }
        }

        for (int i =0 ; i < vms.length; i++) {
            try {
            	
            	dm = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vms[i].getBytes()));
  
                 DomainInfo.DomainState ps = dm.getInfo().state;
                 final State state = convertToState(ps);
                 String vmName = dm.getName();
                 s_logger.trace("VM " + vmName + ": powerstate = " + ps + "; vm state=" + state.toString());

                 vmStates.put(vmName, state);
            } catch (final LibvirtException e) {
                 s_logger.warn("Unable to get vms", e);
            } finally {
            	try {
            		if (dm != null)
            			dm.free();
            	} catch (LibvirtException e) {

            	}
            }
        }

        return vmStates;
	}

	protected List<Object> getHostInfo() {
        final ArrayList<Object> info = new ArrayList<Object>();
        long speed = 0;
        long cpus = 0;
        long ram = 0;
        String osType = null;
        try {
        	final NodeInfo hosts = _conn.nodeInfo();

        	cpus = hosts.cpus;
        	speed = hosts.mhz;
        	ram = hosts.memory * 1024L;
            LibvirtCapXMLParser parser = new LibvirtCapXMLParser();
            parser.parseCapabilitiesXML(_conn.getCapabilities());
            ArrayList<String> oss = parser.getGuestOsType();
            for(String s : oss)
            	/*Even host supports guest os type more than hvm, we only report hvm to management server*/
            	if (s.equalsIgnoreCase("hvm"))
            		osType = "hvm";
        } catch (LibvirtException e) {

        }

        info.add((int)cpus);
        info.add(speed);
        info.add(ram);
        info.add(osType);
        long dom0ram = Math.min(ram/10, 768*1024*1024L);//save a maximum of 10% of system ram or 768M
        dom0ram = Math.max(dom0ram, _dom0MinMem);
        info.add(dom0ram);
    	s_logger.info("cpus=" + cpus + ", speed=" + speed + ", ram=" + ram + ", dom0ram=" + dom0ram);

        return info;
    }
	
    protected void cleanupVM(final String vmName, final String vnet) {
        s_logger.debug("Trying to cleanup the vnet: " + vnet);
        if (vnet != null)
        	cleanupVnet(vnet);
        
        _vmStats.remove(vmName);
    }

	protected String rebootVM(String vmName) {
		String msg = stopVM(vmName, defineOps.DEFINE_VM);

		if (msg == null) {
			Domain dm = null;
			try {
				dm = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
				dm.create();

				return null;
			} catch (LibvirtException e) {
				s_logger.warn("Failed to create vm", e);
				msg = e.getMessage();
		    } finally {
		    	try {
		    		if (dm != null)
		    			dm.free();
		    	} catch (LibvirtException e) {

		    	}
		    }
		}

		return msg;
	}
	protected String stopVM(String vmName, defineOps df) {
		DomainInfo.DomainState state = null;
		Domain dm = null;
		
		s_logger.debug("Try to stop the vm at first");
		String ret = stopVM(vmName, false);
		if (ret == Script.ERR_TIMEOUT) {
			ret = stopVM(vmName, true);
		} else if (ret != null) {
			/*There is a race condition between libvirt and qemu:
			 * libvirt listens on qemu's monitor fd. If qemu is shutdown, while libvirt is reading on
			 * the fd, then libvirt will report an error. */
			/*Retry 3 times, to make sure we can get the vm's status*/
			for (int i = 0; i < 3; i++) {
				try {
					dm = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
					state = dm.getInfo().state;
					break;
				} catch (LibvirtException e) {
					s_logger.debug("Failed to get vm status:" + e.getMessage());
				} finally {
					try {
						if (dm != null)
							dm.free();
					} catch (LibvirtException l) {

					}
				}
			}
			
			if (state == null) {
				s_logger.debug("Can't get vm's status, assume it's dead already");
				return null;
			}
			
			if (state != DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF) {
				s_logger.debug("Try to destroy the vm");
				ret = stopVM(vmName, true);
				if (ret != null) {
					return ret;
				}
			}
		}
		
		if (df == defineOps.UNDEFINE_VM) {
			try {
				dm = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
				dm.undefine();
			} catch (LibvirtException e) {

			} finally {
				try {
					if (dm != null)
						dm.free();
				} catch (LibvirtException l) {
					
				}
			}
		}
		return null;
	}
    protected String stopVM(String vmName, boolean force) {
    	Domain dm = null;
        try {
            dm = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
            if (force) {
				if (dm.getInfo().state != DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF) {
					dm.destroy();
				}
            } else {
            	if (dm.getInfo().state == DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF) {
            		return null;
            	}
    			dm.shutdown();
    			int retry = _stopTimeout/2000;
    			/*Wait for the domain gets into shutoff state*/
    			while ((dm.getInfo().state != DomainInfo.DomainState.VIR_DOMAIN_SHUTOFF) && (retry >= 0)) {
    				Thread.sleep(2000);
    				retry--;
    			}
    			if (retry < 0) {
    				s_logger.warn("Timed out waiting for domain " + vmName + " to shutdown gracefully");
    				return Script.ERR_TIMEOUT;
    			}
            }
        } catch (LibvirtException e) {
            s_logger.debug("Failed to stop VM :" + vmName + " :", e);
            return e.getMessage();
        } catch (InterruptedException ie) {
        	s_logger.debug("Interrupted sleep");
        } finally {
        	try {
        		if (dm != null)
        			dm.free();
        	} catch (LibvirtException e) {
        	}
        }
        
        return null;
    }

    public synchronized String cleanupVnet(final String vnetId) {
		// VNC proxy VMs do not have vnet
		if(vnetId == null || vnetId.isEmpty() || isDirectAttachedNetwork(vnetId))
			return null;

		final List<String> names = getAllVmNames();
		
		if (!names.isEmpty()) {
			for (final String name : names) {
				if (VirtualMachineName.getVnet(name).equals(vnetId)) {
					return null;    // Can't remove the vnet yet.
				}
			}
		}
		
        final Script command = new Script(_vnetcleanupPath, _timeout, s_logger);
        command.add("-v", vnetId);
        return command.execute();
    }

    protected Integer getVncPort( String vmName) {
    	LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
    	Domain dm = null;
    	try {
    		dm = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
    		String xmlDesc = dm.getXMLDesc(0);
    		parser.parseDomainXML(xmlDesc);
    		return parser.getVncPort();
    	} catch (LibvirtException e) {
    		
    	} finally {
    		try {
    			if (dm != null)
    				dm.free();
    		} catch (LibvirtException l) {

    		}
    	}
    	return null;
    }
    
    protected int[] gatherVncPorts(final Collection<String> names) {
        final ArrayList<Integer> ports = new ArrayList<Integer>(names.size());
        for (final String name : names) {
            final Integer port = getVncPort(name);
            
            if (port != null) {
                ports.add(port);
            }
        }
        
        final int[] results = new int[ports.size()];
        int i = 0;
        for (final Integer port : ports) {
            results[i++] = port;
        }
        
        return results;
    }
    
    private boolean IsHVMEnabled() {
		LibvirtCapXMLParser parser = new LibvirtCapXMLParser();
		try {
			parser.parseCapabilitiesXML(_conn.getCapabilities());
			ArrayList<String> osTypes = parser.getGuestOsType();
			for (String o : osTypes) {
				if (o.equalsIgnoreCase("hvm"))
					return true;
			}
		} catch (LibvirtException e) {
			
		}
    	return false;
    }
    
    private String getHypervisorPath() {
    	if (_conn == null)
    		return null;
    	
    	LibvirtCapXMLParser parser = new LibvirtCapXMLParser();
    	try {
    		parser.parseCapabilitiesXML(_conn.getCapabilities());
    	} catch (LibvirtException e) {

    	}
    	return parser.getEmulator();
    }
    
    private String getGuestType(String vmName) {
    	LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
    	Domain dm = null;
    	try {
    		dm = _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
    		String xmlDesc = dm.getXMLDesc(0);
    		parser.parseDomainXML(xmlDesc);
    		return parser.getDescription();
    	} catch (LibvirtException e) {
    		return null;
    	} finally {
    		try {
    			if (dm != null)
    				dm.free();
    		} catch (LibvirtException l) {

    		}
    	}
    }
    
    private boolean isGuestPVEnabled(String guestOS) {
    	if (guestOS == null)
    		return false;
    	if (guestOS.startsWith("Ubuntu 10.04") ||
    		guestOS.startsWith("Ubuntu 9") ||
    		guestOS.startsWith("Ubuntu 8.10") ||
    		guestOS.startsWith("Fedora 13") ||
    		guestOS.startsWith("Fedora 12") ||
    		guestOS.startsWith("Fedora 11") ||
    		guestOS.startsWith("Fedora 10") ||
    		guestOS.startsWith("Fedora 9") ||
    		guestOS.startsWith("CentOS 5.3") ||
    		guestOS.startsWith("CentOS 5.4") ||
    		guestOS.startsWith("CentOS 5.5") ||
    		guestOS.startsWith("Red Hat Enterprise Linux 5.3") ||
    		guestOS.startsWith("Red Hat Enterprise Linux 5.4") ||
    		guestOS.startsWith("Red Hat Enterprise Linux 5.5") ||
    		guestOS.startsWith("Red Hat Enterprise Linux 6") ||
    		guestOS.startsWith("Debain Lenney") ||
    		guestOS.startsWith("Debain Squeeze")
    		)
    		return true;
    	else
    		return false;
    }
    
    private boolean isCentosHost() {
    	if (_hvVersion <=9 ) {
    		return true;
    	} else
    		return false;
    }
    
    private StorageVol createTmplDataDisk(String rootkPath, long size) throws LibvirtException, InternalErrorException {
    	/*create a templ data disk, to contain patches*/
    	StorageVol rootVol = getVolume(rootkPath);
    	StoragePool rootPool = rootVol.storagePoolLookupByVolume();
    	LibvirtStorageVolumeDef volDef = new LibvirtStorageVolumeDef(UUID.randomUUID().toString(), size, volFormat.RAW, null, null);
    	StorageVol dataVol =  rootPool.storageVolCreateXML(volDef.toString(), 0);

    	/*Format/create fs on this disk*/
    	final Script command = new Script(_createvmPath, _timeout, s_logger);
    	command.add("-f", dataVol.getKey());
    	String result = command.execute();
    	if (result != null) {
    		s_logger.debug("Failed to create data disk: " + result);
    		throw new InternalErrorException("Failed to create data disk: " + result);
    	}
    	return dataVol;
    }
    
    private interfaceDef.nicModel getGuestNicModel(String guestOSType) {
    	if (isGuestPVEnabled(guestOSType) && !isCentosHost()) {
    		return interfaceDef.nicModel.VIRTIO;
    	} else {
    		return interfaceDef.nicModel.E1000;
    	}
    }
    
    private diskDef.diskBus getGuestDiskModel(String guestOSType) {
    	if (isGuestPVEnabled(guestOSType) && !isCentosHost()) {
    		return diskDef.diskBus.VIRTIO;
    	} else {
    		return diskDef.diskBus.IDE;
    	}
    }
    
    private String setVnetBrName(String vnetId) {
    	return "vnbr" + vnetId;
    }
    private String getVnetIdFromBrName(String vnetBrName) {
    	return vnetBrName.replaceAll("vnbr", "");
    }
    private List<interfaceDef> createUserVMNetworks(StartCommand cmd) throws InternalErrorException {
    	List<interfaceDef> nics = new ArrayList<interfaceDef>();
    	interfaceDef.nicModel nicModel = getGuestNicModel(cmd.getGuestOSDescription());
    	String guestMac = cmd.getGuestMacAddress();
    	String brName;
    	interfaceDef pubNic = new interfaceDef();
    	if (cmd.getGuestIpAddress() == null) {
    		/*guest network is direct attached without external DHCP server*/
    		brName = _privBridgeName;
    		pubNic.setHostNetType(hostNicType.DIRECT_ATTACHED_WITHOUT_DHCP);
    	} else if ("untagged".equalsIgnoreCase(cmd.getGuestNetworkId())){
    		/*guest network is direct attached with domr DHCP server*/
    		brName = _privBridgeName;
    		pubNic.setHostNetType(hostNicType.DIRECT_ATTACHED_WITH_DHCP);
    	} else {
    		/*guest network is vnet*/
    		String vnetId = getVnetId(cmd.getGuestNetworkId());
    		brName = setVnetBrName(vnetId);
    		createVnet(vnetId);
    		pubNic.setHostNetType(hostNicType.VNET);
    	}
		pubNic.defBridgeNet(brName, null, guestMac, nicModel);
		nics.add(pubNic);
		return nics;
    }
    
    private List<interfaceDef> createRouterVMNetworks(StartRouterCommand cmd) throws InternalErrorException {
    	List<interfaceDef> nics = new ArrayList<interfaceDef>();
    	DomainRouter router = cmd.getRouter();
    	String guestMac = router.getGuestMacAddress();
    	String privateMac = router.getPrivateMacAddress();
    	String pubMac = router.getPublicMacAddress();
    	String brName;
    	interfaceDef pubNic = new interfaceDef();
    	interfaceDef privNic = new interfaceDef();
    	interfaceDef vnetNic = new interfaceDef();
    	if ("untagged".equalsIgnoreCase(router.getVnet())){
    		/*guest network is direct attached with domr DHCP server*/
    		/*0 is on private nic, 1 is link local*/
    		vnetNic.defBridgeNet(_privBridgeName, null, guestMac, interfaceDef.nicModel.VIRTIO);
    		vnetNic.setHostNetType(hostNicType.DIRECT_ATTACHED_WITH_DHCP);
    		nics.add(vnetNic);
    		
    		privNic.defPrivateNet(_privNwName, null, privateMac, interfaceDef.nicModel.VIRTIO);
    		privNic.setHostNetType(hostNicType.DIRECT_ATTACHED_WITH_DHCP);
    		nics.add(privNic);
    	} else {
    		/*guest network is vnet: 0 is vnet, 1 is link local, 2 is pub nic*/
    		String vnetId = getVnetId(router.getVnet());
    		brName = setVnetBrName(vnetId);
    		String vnetDev = "vtap" + vnetId;
    		createVnet(vnetId);
    		vnetNic.defBridgeNet(brName, vnetDev, guestMac, interfaceDef.nicModel.VIRTIO);
    		vnetNic.setHostNetType(hostNicType.VNET);
    		nics.add(vnetNic);
    		
    		privNic.defPrivateNet(_privNwName, null, privateMac, interfaceDef.nicModel.VIRTIO);
    		nics.add(privNic);
    		
    		String pubDev = "tap" + vnetId;
    		pubNic.defBridgeNet(_publicBridgeName, pubDev, pubMac, interfaceDef.nicModel.VIRTIO);
    		nics.add(pubNic);
    	}
		return nics;
    }
    
    private List<interfaceDef> createConsoleVMNetworks(StartConsoleProxyCommand cmd) {
    	List<interfaceDef> nics = new ArrayList<interfaceDef>();
    	ConsoleProxyVO console = cmd.getProxy();
    	String privateMac = console.getPrivateMacAddress();
    	String pubMac = console.getPublicMacAddress();
    	interfaceDef pubNic = new interfaceDef();
    	interfaceDef privNic = new interfaceDef();
    	interfaceDef vnetNic = new interfaceDef();

    	/*guest network is vnet: 0 is not used, 1 is link local, 2 is pub nic*/
    	vnetNic.defPrivateNet("default", null, null, interfaceDef.nicModel.VIRTIO);
    	nics.add(vnetNic);

    	privNic.defPrivateNet(_linkLocalBridgeName, null, privateMac, interfaceDef.nicModel.VIRTIO);
    	nics.add(privNic);

    	pubNic.defBridgeNet(_publicBridgeName, null, pubMac, interfaceDef.nicModel.VIRTIO);
    	nics.add(pubNic);

		return nics;
    }
    
    private void cleanupVMNetworks(List<interfaceDef> nics) {
    	for (interfaceDef nic : nics) {
    		if (nic.getHostNetType() == hostNicType.VNET) {
    			cleanupVnet(getVnetIdFromBrName(nic.getBrName()));
    		}
    	}
    }
    
    private List<diskDef> createSystemVMDisk(List<VolumeVO> vols) throws InternalErrorException, LibvirtException{
    	List<diskDef> disks = new ArrayList<diskDef>();
    	// Get the root volume
		List<VolumeVO> rootVolumes = findVolumes(vols, VolumeType.ROOT, true);
        
		if (rootVolumes.size() != 1) {
			throw new InternalErrorException("Could not find systemVM root disk.");
		}
        
		 VolumeVO rootVolume = rootVolumes.get(0);
         String rootkPath = rootVolume.getPath();
         
         StorageVol tmplVol = createTmplDataDisk(rootkPath, 10L * 1024 * 1024);
         String datadiskPath = tmplVol.getKey();
	
		diskDef hda = new diskDef();
		hda.defFileBasedDisk(rootkPath, "hda", diskDef.diskBus.IDE, diskDef.diskFmtType.QCOW2);
		disks.add(hda);
		
		diskDef hdb = new diskDef();
		hdb.defFileBasedDisk(datadiskPath, "hdb",  diskDef.diskBus.IDE, diskDef.diskFmtType.RAW);
		disks.add(hdb);
		
		return disks;
    }
    
    private List<diskDef> createVMDisk(List<VolumeVO> vols, String guestOSType, String isoURI) throws InternalErrorException, LibvirtException, URISyntaxException{
    	List<diskDef> disks = new ArrayList<diskDef>();
    	// Get the root volume
		List<VolumeVO> rootVolumes = findVolumes(vols, VolumeType.ROOT, true);
        
		if (rootVolumes.size() != 1) {
			throw new InternalErrorException("Could not find UserVM root disk.");
		}
            
		VolumeVO rootVolume = rootVolumes.get(0);
		
		String isoPath = null;
		if (isoURI != null) {
			StorageVol isoVol = getVolume(_conn, isoURI);
			if (isoVol != null) {
				isoPath = isoVol.getPath();
			} else
				throw new InternalErrorException("Can't find iso volume");
		}
		
		List<VolumeVO> dataVolumes = findVolumes(vols, VolumeType.DATADISK, false);
		VolumeVO dataVolume = null;
		if (dataVolumes.size() > 0)
			dataVolume = dataVolumes.get(0);
		
		diskDef.diskBus diskBusType = getGuestDiskModel(guestOSType);
		
		
		diskDef hda = new diskDef();
		hda.defFileBasedDisk(rootVolume.getPath(), "hda", diskBusType, diskDef.diskFmtType.QCOW2);
		disks.add(hda);
		
		/*Centos doesn't support scsi hotplug. For other host OSes, we attach the disk after the vm is running, so that we can hotplug it.*/
		if (dataVolume != null) {
			diskDef hdb = new diskDef();
			hdb.defFileBasedDisk(dataVolume.getPath(), "hdb", diskBusType, diskDef.diskFmtType.QCOW2);
			if (!isCentosHost()) {
				hdb.setAttachDeferred(true);
			}
			disks.add(hdb);
		}

		diskDef hdc = new diskDef();
		hdc.defFileBasedDisk(isoPath, "hdc", diskDef.diskBus.IDE, diskDef.diskFmtType.RAW);
		hdc.setDeviceType(diskDef.deviceType.CDROM);
		disks.add(hdc);
	
		return disks;
    }
    
    private boolean can_bridge_firewall() {
    	String command = "iptables -N BRIDGE-FIREWALL; " +
    	 			 	 "iptables -I BRIDGE-FIREWALL -m state --state RELATED,ESTABLISHED -j ACCEPT";
    	
    	 String result = executeBashScript(command);
    	 if (result != null) {
    		 s_logger.debug("Chain BRIDGE-FIREWALL already exists");
    	 }
    	 
    	 boolean enabled = true;
    	 
    	 result = executeBashScript("iptables -n -L FORWARD|grep BRIDGE-FIREWALL");;
    	 if (result != null) {
        	 result = executeBashScript("iptables -I FORWARD -m physdev --physdev-is-bridged -j BRIDGE-FIREWALL");;
        	 if (result != null) {
        		 enabled = false;
        	 }
    	 }
    	 
    	 File logPath = new File("/var/run/cloud");
    	 if (!logPath.exists()) {
    		 logPath.mkdir();
    	 }
    	 
    	 cleanup_rules_for_dead_vms();
    	 cleanup_rules();
    	 return enabled;
    }
    
    private void cleanup_rules_for_dead_vms() {
    	
    }
    
    private void cleanup_rules() {
    	
    }
    
 /*   private void ipset(String ipsetname, String proto, String start, String end, List<String> ips) {
    Script command = new Script("/bin/bash", _timeout, s_logger);
   	 	command.add("-c");
   	 	command.add("ipset -N " + ipsetname + " iptreemap");
   	 	String result = command.execute();
   	 	if (result != null) {
   	 		s_logger.debug("ipset chain already exists: " + ipsetname);
   	 	}
   	 	boolean success = true;
   	 	String ipsettmp = ipsetname +
   	 	
    }
    */
    
    private Domain getDomain(String vmName) throws LibvirtException {
    	 return _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
    }
    
    private  List<String> getInterfaces(String vmName) {
    	LibvirtDomainXMLParser parser = new LibvirtDomainXMLParser();
    	Domain dm = null;
    	try {
    		dm =  _conn.domainLookupByUUID(UUID.nameUUIDFromBytes(vmName.getBytes()));
    		parser.parseDomainXML(dm.getXMLDesc(0));
    	} catch (LibvirtException e) {
    		s_logger.debug("Failed to get dom xml: " + e.toString());
    		return new ArrayList<String>();
    	} finally {
    		try {
    		if (dm != null) {
    			dm.free();
    		}
    		} catch (LibvirtException e) {
    			
    		}
    	}
    	
    	return parser.getInterfaces();
    	
    }
    
    private String executeBashScript(String script) {
    	Script command = new Script("/bin/bash", _timeout, s_logger);
		command.add("-c");
		command.add(script);
		return command.execute();
    }
    
    private String executeBashScript(String script, OutputInterpreter parser) {
    	Script command = new Script("/bin/bash", _timeout, s_logger);
		command.add("-c");
		command.add(script);
		return command.execute(parser);
    }
    
    private boolean default_network_rules_for_systemvm(String vmName) {
    	String command;
    	
    	List<String> nics = getInterfaces(vmName);
    	for (String vif : nics) {
    		command = "iptables -D BRIDGE-FIREWALL -m physdev --physdev-is-bridged --physdev-out " + vif + " -j "  + vmName + ";" +
    				  "iptables -D BRIDGE-FIREWALL -m physdev --physdev-is-bridged --physdev-in " + vif + " -j "  + vmName;
    		String result = executeBashScript(command);
    		if (result != null) {
    			s_logger.debug("Ingnoring failure to delete old rules");
    		}
    		command = "iptables -N " + vmName;
    		result = executeBashScript(command);
    		if (result != null) {
        		command = "iptables -F " + vmName;
        		result = executeBashScript(command);
    		}
    		command = "iptables -A BRIDGE-FIREWALL -m physdev --physdev-is-bridged --physdev-out " + vif + " -j "  + vmName + ";" +
    				   "iptables -A BRIDGE-FIREWALL -m physdev --physdev-is-bridged --physdev-in " + vif + " -j "  + vmName;
    	    result = executeBashScript(command);
    		if (result != null) {
    			s_logger.debug("Failed to program default rules");
    			return false;
    		}
    	}
    	command = "iptables -A " + vmName + " -j ACCEPT";
    	executeBashScript(command);
		return true;
    }
    
    private boolean default_network_rules(String vmName, String vmIP) {
    	String command;

    	List<String> nics = getInterfaces(vmName);
    	for (String vif : nics) {
    		command = "iptables -D BRIDGE-FIREWALL -m physdev --physdev-is-bridged --physdev-out " + vif + " -j "  + vmName + ";" +
    				  "iptables -D BRIDGE-FIREWALL -m physdev --physdev-is-bridged --physdev-in " + vif + " -j "  + vmName + ";" +
    				  "iptables -F " + vmName + ";" +
    				  "iptables -X " + vmName + ";";
    		String result = executeBashScript(command);
    		if (result != null) {
    			s_logger.debug("Ignoring failure to delete old rules");
    		}
    		
    		result = executeBashScript("iptables -N " + vmName);
    		if (result != null) {
    			executeBashScript("iptables -F " + vmName);
    		}
    		
    		command = "iptables -D BRIDGE-FIREWALL -m physdev --physdev-is-bridged --physdev-out " + vif + " -j "  + vmName + ";" +
		    		  "iptables -D BRIDGE-FIREWALL -m physdev --physdev-is-bridged --physdev-in " + vif + " -j "  + vmName + ";" +
		    		  "iptables -A " + vmName + " -m state --state RELATED,ESTABLISHED -j ACCEPT" + ";" +
		    		  "iptables -A " + vmName + " -p udp --dport 67:68 --sport 67:68 -j ACCEPT" + ";" +
		    		  "iptables -A " + vmName + " -m physdev --physdev-is-bridged --physdev-in " + vif + " --source " + vmIP + " -j RETURN" + ";" +
		    		  "iptables -A " + vmName + " -j DROP";
    		result = executeBashScript(command);
    		if (result != null) {
    			s_logger.debug("Failed to program default rules for vm:" + vmName);
    			return false;
    		}
    	}
    	return true;
    }
    
    private boolean destroy_network_rules_for_vm(String vmName) {
    	String command = "iptables-save |grep BRIDGE-FIREWALL |grep " + vmName + " | sed 's/-A/-D/'";
    	OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
    	String result = executeBashScript(command, parser);
    	
    	if (result == null && parser.getLines() != null) {
    		String[] lines = parser.getLines().split("\\n");
    		for (String cmd : lines) {
    			command = "iptables " + cmd;
    			executeBashScript(command);
    		}
    	}
    	
    	executeBashScript("iptables -F " + vmName);
    	executeBashScript("iptables -X " + vmName);
    	return true;
    }
    
    private void deletExitingLinkLocalRoutTable(String linkLocalBr) {
    	Script command = new Script("/bin/bash", _timeout);
    	command.add("-c");
    	command.add("ip route | grep " + NetUtils.getLinkLocalCIDR());
    	OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
    	String result = command.execute(parser);
    	boolean foundLinkLocalBr = false;
    	if (result == null && parser.getLines() != null) {
    		String[] lines = parser.getLines().split("\\n");
    		for (String line : lines) {
    			String[] tokens = line.split(" ");
    			if (!tokens[2].equalsIgnoreCase(linkLocalBr))
    				Script.runSimpleBashScript("ip route del " + NetUtils.getLinkLocalCIDR());
    			else
    				foundLinkLocalBr = true;
    		}
    	}
    	if (!foundLinkLocalBr) {
    		Script.runSimpleBashScript("ip route add " + NetUtils.getLinkLocalCIDR() + " dev " + linkLocalBr + " src " + NetUtils.getLinkLocalGateway());
    	}
    }
    
    private class vmStats {
    	long _usedTime;
    	long _tx;
    	long _rx;
    	Calendar _timestamp;
    }
    
    private VmStatsEntry getVmStat(String vmName) throws LibvirtException{
    	Domain dm = null;
    	try {
    		dm = getDomain(vmName);
    		DomainInfo info = dm.getInfo();

    		VmStatsEntry stats = new VmStatsEntry();
    		stats.setNumCPUs(info.nrVirtCpu);
    		stats.setEntityType("vm");
    		
    		/*get cpu utilization*/
    		vmStats oldStats = null;

    		Calendar now = Calendar.getInstance();
    		
    		oldStats = _vmStats.get(vmName);
  

    		long elapsedTime = 0;
    		if (oldStats != null) {
    			elapsedTime = now.getTimeInMillis() - oldStats._timestamp.getTimeInMillis();
    			double utilization = (info.cpuTime - oldStats._usedTime)/((double)elapsedTime*1000000);

    			NodeInfo node = _conn.nodeInfo();
    			utilization = utilization/node.cpus;
    			stats.setCPUUtilization(utilization*100);
    		}
			
    		/*get network stats*/

    		List<String> vifs = getInterfaces(vmName);
    		long rx = 0;
    		long tx = 0;
    		for (String vif : vifs) {
    			DomainInterfaceStats ifStats = dm.interfaceStats(vif);
    			rx += ifStats.rx_bytes;
    			tx += ifStats.tx_bytes;
    		}
    		
    		if (oldStats != null) {
    			stats.setNetworkReadKBs((rx - oldStats._rx)/1000);
    			stats.setNetworkWriteKBs((tx - oldStats._tx)/1000);
    		}
    		
    		vmStats newStat = new vmStats();
    		newStat._usedTime = info.cpuTime;
    		newStat._rx = rx;
    		newStat._tx = tx;
    		newStat._timestamp = now;
    		_vmStats.put(vmName, newStat);
    		return stats;
    	} finally {
    		if (dm != null) {
    			dm.free();
    		}
    	}
    }
    
    private StorageVol copyVolume(StoragePool destPool, LibvirtStorageVolumeDef destVol, StorageVol srcVol) throws LibvirtException {
    	if (isCentosHost()) {
    		/*define a volume, then override the file*/
    		
    		StorageVol vol = destPool.storageVolCreateXML(destVol.toString(), 0);
    		String srcPath = srcVol.getKey();
    		String destPath = vol.getKey();
    		Script.runSimpleBashScript("cp " + srcPath + " " + destPath );
    		return vol;
    	} else {
    		return destPool.storageVolCreateXMLFrom(destVol.toString(), srcVol, 0);
    	}
    }
    
    private StorageVol createVolume(StoragePool destPool, StorageVol tmplVol) throws LibvirtException {
    	if (isCentosHost()) {
    		LibvirtStorageVolumeDef volDef = new LibvirtStorageVolumeDef(UUID.randomUUID().toString(), tmplVol.getInfo().capacity, volFormat.QCOW2, null, null);
    		s_logger.debug(volDef.toString());
    		StorageVol vol = destPool.storageVolCreateXML(volDef.toString(), 0);
    		
    		/*create qcow2 image based on the name*/
    		Script.runSimpleBashScript("qemu-img create -f qcow2 -b  " + tmplVol.getPath() + " " + vol.getPath() );
    		return vol;
    	} else {
    		LibvirtStorageVolumeDef volDef = new LibvirtStorageVolumeDef(UUID.randomUUID().toString(), tmplVol.getInfo().capacity, volFormat.QCOW2, tmplVol.getPath(), volFormat.QCOW2);
    		s_logger.debug(volDef.toString());
    		return destPool.storageVolCreateXML(volDef.toString(), 0);
    	}
    }
    
    private StorageVol getVolume(StoragePool pool, String volKey) {
    	StorageVol vol = null;
    	try {
    		vol = _conn.storageVolLookupByKey(volKey);
    	} catch (LibvirtException e) {
    		
    	}
    	if (vol == null) {
    		try {
    			pool.refresh(0);
    		} catch (LibvirtException e) {
    			
    		}
    		try {
    			vol = _conn.storageVolLookupByKey(volKey);
    		} catch (LibvirtException e) {
    			
    		}
    	}
    	return vol;
    }
    
    private StorageVol getVolume(String volKey) {
    	StorageVol vol = null;
    	try {
    		vol = _conn.storageVolLookupByKey(volKey);
    	} catch (LibvirtException e) {
    		
    	}
    	if (vol == null) {
    		StoragePool pool = null;
    		try {
    			String token[] = volKey.split("/");
    			if (token.length <= 2) {
    				s_logger.debug("what the heck of volkey: " + volKey);
    				return null;
    			}
    			String poolUUID = token[token.length - 2];
    			pool = _conn.storagePoolLookupByUUIDString(poolUUID);
    			
    		} catch (LibvirtException e) {
    			s_logger.debug("Failed to get pool, with volKey: " + volKey + "due to" + e.toString());
    			return null;
    		}
    		
    		try {
    			pool.refresh(0);
    		} catch (LibvirtException e) {
    			
    		}
    		
    		try {
    			vol = _conn.storageVolLookupByKey(volKey);
    		} catch (LibvirtException e) {
    			
    		}
    	}
    	return vol;
    }
}
