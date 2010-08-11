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
package com.cloud.storage.resource;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.CheckHealthAnswer;
import com.cloud.agent.api.CheckHealthCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetStorageStatsAnswer;
import com.cloud.agent.api.GetStorageStatsCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingStorageCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.SecStorageFirewallCfgCommand;
import com.cloud.agent.api.SecStorageSetupCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.SecStorageFirewallCfgCommand.PortConfig;
import com.cloud.agent.api.storage.DeleteTemplateCommand;
import com.cloud.agent.api.storage.DownloadCommand;
import com.cloud.agent.api.storage.DownloadProgressCommand;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.Volume;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.template.DownloadManager;
import com.cloud.storage.template.DownloadManagerImpl;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.net.NfsUtils;
import com.cloud.utils.script.Script;

public class NfsSecondaryStorageResource extends ServerResourceBase implements ServerResource {
    private static final Logger s_logger = Logger.getLogger(NfsSecondaryStorageResource.class);
    int _timeout;
    
    String _instance;
    String _parent;
    
    String _dc;
    String _pod;
    String _guid;
    String _nfsPath;
    String _mountParent;
    Map<String, Object> _params;
    StorageLayer _storage;
    boolean _inSystemVM = false;
    boolean _sslCopy = false;
    
    Random _rand = new Random(System.currentTimeMillis());
    
    DownloadManager _dlMgr;
	private String _configSslScr;
	private String _configAuthScr;
	private String _publicIp;
	private String _hostname;
	private String _localgw;
	private String _eth1mask;
	private String _eth1ip;
    
    @Override
    public void disconnected() {
        if (_parent != null && !_inSystemVM) {
            Script script = new Script(!_inSystemVM, "umount", _timeout, s_logger);
            script.add(_parent);
            script.execute();
            
            File file = new File(_parent);
            file.delete();
        }
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof DownloadProgressCommand) {
            return _dlMgr.handleDownloadCommand((DownloadProgressCommand)cmd);
        } else if (cmd instanceof DownloadCommand) {
            return _dlMgr.handleDownloadCommand((DownloadCommand)cmd);
        } else if (cmd instanceof GetStorageStatsCommand) {
        	return execute((GetStorageStatsCommand)cmd);
        } else if (cmd instanceof CheckHealthCommand) {
            return new CheckHealthAnswer((CheckHealthCommand)cmd, true);
        } else if (cmd instanceof DeleteTemplateCommand) {
        	return execute((DeleteTemplateCommand) cmd);
        } else if (cmd instanceof ReadyCommand) {
            return new ReadyAnswer((ReadyCommand)cmd);
        } else if (cmd instanceof SecStorageFirewallCfgCommand){
        	return execute((SecStorageFirewallCfgCommand)cmd);
        } else if (cmd instanceof SecStorageSetupCommand){
        	return execute((SecStorageSetupCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }
    
    private Answer execute(SecStorageSetupCommand cmd) {
    	if (!_inSystemVM){
			return new Answer(cmd, true, null);
		}
		boolean success = true;
		StringBuilder result = new StringBuilder();
		for (String cidr: cmd.getAllowedInternalSites()) {
			String tmpresult = allowOutgoingOnPrivate(cidr);
			if (tmpresult != null) {
				result.append(", ").append(tmpresult);
				success = false;
			}
		}
		if (success) {
			if (cmd.getCopyPassword() != null && cmd.getCopyUserName() != null) {
				String tmpresult = configureAuth(cmd.getCopyUserName(), cmd.getCopyPassword());
				if (tmpresult != null) {
					result.append("Failed to configure auth for copy ").append(tmpresult);
					success = false;
				}
			}
		}
		return new Answer(cmd, success, result.toString());

	}
    
    private String allowOutgoingOnPrivate(String destCidr) {
    	
    	Script command = new Script("/bin/bash", s_logger);
    	String intf = "eth1";
    	command.add("-c");
    	command.add("iptables -I OUTPUT -o " + intf + " -d " + destCidr + " -p tcp -m state --state NEW -m tcp  -j ACCEPT");

    	String result = command.execute();
    	if (result != null) {
    		s_logger.warn("Error in allowing outgoing to " + destCidr + ", err=" + result );
    		return "Error in allowing outgoing to " + destCidr + ", err=" + result;
    	}
    	addRouteToInternalIpOrCidr(_localgw, _eth1ip, _eth1mask, destCidr);
    	return null;
	}
    
    

	private Answer execute(SecStorageFirewallCfgCommand cmd) {
		if (!_inSystemVM){
			return new Answer(cmd, true, null);
		}
		List<String> iptablesCfg = new ArrayList<String>();
		iptablesCfg.add("iptables -F HTTP");
		for (PortConfig pCfg:cmd.getPortConfigs()){
			if (pCfg.isAdd()) {
				iptablesCfg.add("iptables -A HTTP -i " +  pCfg.getIntf() + " -s " + pCfg.getSourceIp() + " -p tcp -m state --state NEW -m tcp --dport " + pCfg.getPort() + " -j ACCEPT");
			}
		}
		boolean success = true;
		StringBuilder result = new StringBuilder();
		for (String rule: iptablesCfg) {
			Script command = new Script("/bin/bash", s_logger);
			command.add("-c");
			command.add(rule);
			String tmpresult = command.execute();
			if (tmpresult != null) {
				result.append(tmpresult);
				success = false;
			}
		}
		return new Answer(cmd, success, result.toString());
	}

	protected GetStorageStatsAnswer execute(final GetStorageStatsCommand cmd) {
        final long usedSize = getUsedSize();
        final long totalSize = getTotalSize();
        if (usedSize == -1 || totalSize == -1) {
        	return new GetStorageStatsAnswer(cmd, "Unable to get storage stats");
        } else {
        	return new GetStorageStatsAnswer(cmd, totalSize, usedSize) ;
        }
    }
    
    protected Answer execute(final DeleteTemplateCommand cmd) {
    	String relativeTemplatePath = cmd.getTemplatePath();
    	String parent = _parent;
    	
    	if (relativeTemplatePath.startsWith(File.separator)) {
    		relativeTemplatePath = relativeTemplatePath.substring(1);
    	}
    	
    	if (!parent.endsWith(File.separator)) {
            parent += File.separator;
        }
    	String absoluteTemplatePath = parent + relativeTemplatePath;
		File tmpltParent = new File(absoluteTemplatePath).getParentFile();
		
		boolean result = true;
		if (tmpltParent.exists()) {
			File [] tmpltFiles = tmpltParent.listFiles();
			if (tmpltFiles != null) {
				for (File f : tmpltFiles) {
					f.delete();
				}
			}
		
			result = _storage.delete(tmpltParent.getAbsolutePath());
		}
    	
    	if (result) {
    		return new Answer(cmd, true, null);
    	} else {
    		return new Answer(cmd, false, "Failed to delete file");
    	}
    }
    
    protected long getUsedSize() {
    	return _storage.getUsedSpace(_parent);
    }
    
    protected long getTotalSize() {
      	return _storage.getTotalSpace(_parent);
    }
    
    protected long convertFilesystemSize(final String size) {
        if (size == null || size.isEmpty()) {
            return -1;
        }

        long multiplier = 1;
        if (size.endsWith("T")) {
            multiplier = 1024l * 1024l * 1024l * 1024l;
        } else if (size.endsWith("G")) {
            multiplier = 1024l * 1024l * 1024l;
        } else if (size.endsWith("M")) {
            multiplier = 1024l * 1024l;
        } else {
            assert (false) : "Well, I have no idea what this is: " + size;
        }

        return (long)(Double.parseDouble(size.substring(0, size.length() - 1)) * multiplier);
    }
    

    @Override
    public Type getType() {
        return Host.Type.SecondaryStorage;
    }
    
    @Override
    public PingCommand getCurrentStatus(final long id) {
        return new PingStorageCommand(Host.Type.Storage, id, new HashMap<String, Boolean>());
    }
    
    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
    	_eth1ip = (String)params.get("eth1ip");
        if (_eth1ip != null) { //can only happen inside service vm
        	params.put("private.network.device", "eth1");
        } else {
        	s_logger.warn("Wait, what's going on? eth1ip is null!!");
        }
        String eth2ip = (String) params.get("eth2ip");
        if (eth2ip != null) {
            params.put("public.network.device", "eth2");
        }         
        _publicIp = (String) params.get("eth2ip");
        _hostname = (String) params.get("name");
        
        super.configure(name, params);
        
        _params = params;
        String value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 1440) * 1000;
        
        _storage = (StorageLayer)params.get(StorageLayer.InstanceConfigKey);
        if (_storage == null) {
            value = (String)params.get(StorageLayer.ClassConfigKey);
            if (value == null) {
                value = "com.cloud.storage.JavaStorageLayer";
            }
            
            try {
                Class<?> clazz = Class.forName(value);
                _storage = (StorageLayer)ComponentLocator.inject(clazz);
                _storage.configure("StorageLayer", params);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Unable to find class " + value);
            }
        }
        _configSslScr = Script.findScript(getDefaultScriptsDir(), "config_ssl.sh");
        if (_configSslScr != null) {
            s_logger.info("config_ssl.sh found in " + _configSslScr);
        }

        _configAuthScr = Script.findScript(getDefaultScriptsDir(), "config_auth.sh");
        if (_configSslScr != null) {
            s_logger.info("config_auth.sh found in " + _configAuthScr);
        }
        
        _guid = (String)params.get("guid");
        if (_guid == null) {
            throw new ConfigurationException("Unable to find the guid");
        }
        
        _dc = (String)params.get("zone");
        if (_dc == null) {
            throw new ConfigurationException("Unable to find the zone");
        }
        _pod = (String)params.get("pod");
        
        _instance = (String)params.get("instance");

        _mountParent = (String)params.get("mount.parent");
        if (_mountParent == null) {
            _mountParent = File.separator + "mnt";
        }
        
        if (_instance != null) {
            _mountParent = _mountParent + File.separator + _instance;
        }

        _nfsPath = (String)params.get("mount.path");
        if (_nfsPath == null) {
            throw new ConfigurationException("Unable to find mount.path");
        }
        

        
        String inSystemVM = (String)params.get("secondary.storage.vm");
        if (inSystemVM == null || "true".equalsIgnoreCase(inSystemVM)) {
        	_inSystemVM = true;
            _localgw = (String)params.get("localgw");
            if (_localgw != null) { //can only happen inside service vm
            	_eth1mask = (String)params.get("eth1mask");
            	String internalDns1 = (String)params.get("dns1");
            	String internalDns2 = (String)params.get("dns2");

            	if (internalDns1 == null) {
            		s_logger.warn("No DNS entry found during configuration of NfsSecondaryStorage");
            	} else {
            		addRouteToInternalIpOrCidr(_localgw, _eth1ip, _eth1mask, internalDns1);
            	}
            	
            	String mgmtHost = (String)params.get("host");
            	String nfsHost = NfsUtils.getHostPart(_nfsPath);
            	if (nfsHost == null) {
            		s_logger.error("Invalid or corrupt nfs url " + _nfsPath);
            		throw new CloudRuntimeException("Unable to determine host part of nfs path");
            	}
            	try {
            		InetAddress nfsHostAddr = InetAddress.getByName(nfsHost);
            		nfsHost = nfsHostAddr.getHostAddress();
            	} catch (UnknownHostException uhe) {
            		s_logger.error("Unable to resolve nfs host " + nfsHost);
            		throw new CloudRuntimeException("Unable to resolve nfs host to an ip address " + nfsHost);
            	}
            	addRouteToInternalIpOrCidr(_localgw, _eth1ip, _eth1mask, nfsHost);
            	addRouteToInternalIpOrCidr(_localgw, _eth1ip, _eth1mask, mgmtHost);
            	if (internalDns2 != null) {
                	addRouteToInternalIpOrCidr(_localgw, _eth1ip, _eth1mask, internalDns2);
            	}

            }
            String useSsl = (String)params.get("sslcopy");
            if (useSsl != null) {
            	_sslCopy = Boolean.parseBoolean(useSsl);
            	if (_sslCopy) {
            		configureSSL();
            	}
            }
        	startAdditionalServices();
        	_params.put("install.numthreads", "50");
        	_params.put("secondary.storage.vm", "true");
        }
        _parent = mount(_nfsPath, _mountParent);
        if (_parent == null) {
            throw new ConfigurationException("Unable to create mount point");
        }
        
        
        s_logger.info("Mount point established at " + _parent);
        
        try {
            _params.put("template.parent", _parent);
            _params.put(StorageLayer.InstanceConfigKey, _storage);
            _dlMgr = new DownloadManagerImpl();
            _dlMgr.configure("DownloadManager", _params);
        } catch (ConfigurationException e) {
            s_logger.warn("Caught problem while configuring DownloadManager", e);
            return false;
        }
        return true;
    }
    
    private void startAdditionalServices() {
    	Script command = new Script("/bin/bash", s_logger);
		command.add("-c");
    	command.add("service sshd restart ");
    	String result = command.execute();
    	if (result != null) {
    		s_logger.warn("Error in starting sshd service err=" + result );
    	}
		command = new Script("/bin/bash", s_logger);
		command.add("-c");
    	command.add("iptables -I INPUT -i eth1 -p tcp -m state --state NEW -m tcp --dport 3922 -j ACCEPT");
    	result = command.execute();
    	if (result != null) {
    		s_logger.warn("Error in opening up ssh port err=" + result );
    	}
	}
    
    private void addRouteToInternalIpOrCidr(String localgw, String eth1ip, String eth1mask, String destIpOrCidr) {
    	s_logger.debug("addRouteToInternalIp: localgw=" + localgw + ", eth1ip=" + eth1ip + ", eth1mask=" + eth1mask + ",destIp=" + destIpOrCidr);
    	if (destIpOrCidr == null) {
    		s_logger.debug("addRouteToInternalIp: destIp is null");
			return;
		}
    	if (!NetUtils.isValidIp(destIpOrCidr) && !NetUtils.isValidCIDR(destIpOrCidr)){
    		s_logger.warn(" destIp is not a valid ip address or cidr destIp=" + destIpOrCidr);
    		return;
    	}
    	boolean inSameSubnet = false;
    	if (NetUtils.isValidIp(destIpOrCidr)) {
    		if (eth1ip != null && eth1mask != null) {
    			inSameSubnet = NetUtils.sameSubnet(eth1ip, destIpOrCidr, eth1mask);
    		} else {
    			s_logger.warn("addRouteToInternalIp: unable to determine same subnet: _eth1ip=" + eth1ip + ", dest ip=" + destIpOrCidr + ", _eth1mask=" + eth1mask);
    		}
    	} else {
            inSameSubnet = NetUtils.isNetworkAWithinNetworkB(destIpOrCidr, NetUtils.ipAndNetMaskToCidr(eth1ip, eth1mask));
    	}
    	if (inSameSubnet) {
			s_logger.debug("addRouteToInternalIp: dest ip " + destIpOrCidr + " is in the same subnet as eth1 ip " + eth1ip);
    		return;
    	}
    	Script command = new Script("/bin/bash", s_logger);
		command.add("-c");
    	command.add("ip route delete " + destIpOrCidr);
    	command.execute();
		command = new Script("/bin/bash", s_logger);
		command.add("-c");
    	command.add("ip route add " + destIpOrCidr + " via " + localgw);
    	String result = command.execute();
    	if (result != null) {
    		s_logger.warn("Error in configuring route to internal ip err=" + result );
    	} else {
			s_logger.debug("addRouteToInternalIp: added route to internal ip=" + destIpOrCidr + " via " + localgw);
    	}
    }

	private void configureSSL() {
		Script command = new Script(_configSslScr);
		command.add(_publicIp);
		command.add(_hostname);
		String result = command.execute();
		if (result != null) {
			s_logger.warn("Unable to configure httpd to use ssl");
		}
	}
	
	private String configureAuth(String user, String passwd) {
		Script command = new Script(_configAuthScr);
		command.add(user);
		command.add(passwd);
		String result = command.execute();
		if (result != null) {
			s_logger.warn("Unable to configure httpd to use auth");
		}
		return result;
	}
	
	protected String mount(String path, String parent) {
        String mountPoint = null;
        for (int i = 0; i < 10; i++) {
            String mntPt = parent + File.separator + Integer.toHexString(_rand.nextInt(Integer.MAX_VALUE));
            File file = new File(mntPt);
            if (!file.exists()) {
                if (_storage.mkdir(mntPt)) {
                    mountPoint = mntPt;
                    break;
                }
            }
            s_logger.debug("Unable to create mount: " + mntPt);
        }
        
        if (mountPoint == null) {
            s_logger.warn("Unable to create a mount point");
            return null;
        }
       
        Script script = null;
        String result = null;
        script = new Script(!_inSystemVM, "umount", _timeout, s_logger);
        script.add(path);
        result = script.execute();
        
        if( _parent != null ) {
            script = new Script("rmdir", _timeout, s_logger);
            script.add(_parent);
            result = script.execute();
        }
 
        Script command = new Script(!_inSystemVM, "mount", _timeout, s_logger);
        command.add("-t", "nfs");
        if (_inSystemVM) {
        	//Fedora Core 12 errors out with any -o option executed from java
        	command.add("-o", "soft,timeo=133,retrans=2147483647,tcp,acdirmax=0,acdirmin=0");
        }
        command.add(path);
        command.add(mountPoint);
        result = command.execute();
        if (result != null) {
            s_logger.warn("Unable to mount " + path + " due to " + result);
            File file = new File(mountPoint);
            if (file.exists())
            	file.delete();
            return null;
        }
        
        // Change permissions for the mountpoint
        script = new Script(!_inSystemVM, "chmod", _timeout, s_logger);
        script.add("777", mountPoint);
        result = script.execute();
        if (result != null) {
            s_logger.warn("Unable to set permissions for " + mountPoint + " due to " + result);
            return null;
        }
        
        // XXX: Adding the check for creation of snapshots dir here. Might have to move it somewhere more logical later.
        if (!checkForSnapshotsDir(mountPoint)) {
        	return null;
        }
        
        // Create the volumes dir
        if (!checkForVolumesDir(mountPoint)) {
        	return null;
        }
        
        return mountPoint;
    }
    
    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public StartupCommand[] initialize() {
        /*disconnected();
        
        _parent = mount(_nfsPath, _mountParent);
        
        if( _parent == null ) {
            s_logger.warn("Unable to mount the nfs server");
            return null;
        }
        
        try {
            _params.put("template.parent", _parent);
            _params.put(StorageLayer.InstanceConfigKey, _storage);
            _dlMgr = new DownloadManagerImpl();
            _dlMgr.configure("DownloadManager", _params);
        } catch (ConfigurationException e) {
            s_logger.warn("Caught problem while configuring folers", e);
            return null;
        }*/
        
        final StartupStorageCommand cmd = new StartupStorageCommand(_parent, StoragePoolType.NetworkFilesystem, getTotalSize(), new HashMap<String, TemplateInfo>());
        
        cmd.setResourceType(Volume.StorageResourceType.SECONDARY_STORAGE);
        cmd.setIqn(null);
        
        fillNetworkInformation(cmd);
        cmd.setDataCenter(_dc);
        cmd.setPod(_pod);
        cmd.setGuid(_guid);
        cmd.setName(_guid);
        cmd.setVersion(NfsSecondaryStorageResource.class.getPackage().getImplementationVersion());
        /* gather TemplateInfo in second storage */
        final Map<String, TemplateInfo> tInfo = _dlMgr.gatherTemplateInfo();
        cmd.setTemplateInfo(tInfo);
        cmd.getHostDetails().put("mount.parent", _mountParent);
        cmd.getHostDetails().put("mount.path", _nfsPath);
        String tok[] = _nfsPath.split(":");
        cmd.setNfsShare("nfs://" + tok[0] + tok[1]);
        if (cmd.getHostDetails().get("orig.url") == null) {
            if (tok.length != 2) {
                throw new CloudRuntimeException("Not valid NFS path" + _nfsPath);
            }
            String nfsUrl = "nfs://" + tok[0] + tok[1];
            cmd.getHostDetails().put("orig.url", nfsUrl);
        }
        InetAddress addr;
        try {
            addr = InetAddress.getByName(tok[0]);
            cmd.setPrivateIpAddress(addr.getHostAddress());
        } catch (UnknownHostException e) {
            cmd.setPrivateIpAddress(tok[0]);
        }
        return new StartupCommand [] {cmd};
    }

    protected boolean checkForSnapshotsDir(String mountPoint) {
        String snapshotsDirLocation = mountPoint + File.separator + "snapshots";
        return createDir("snapshots", snapshotsDirLocation, mountPoint);
    }
    
    protected boolean checkForVolumesDir(String mountPoint) {
    	String volumesDirLocation = mountPoint + "/" + "volumes";
    	return createDir("volumes", volumesDirLocation, mountPoint);
    }
    
    protected boolean createDir(String dirName, String dirLocation, String mountPoint) {
    	boolean dirExists = false;
    	
    	File dir = new File(dirLocation);
    	if (dir.exists()) {
    		if (dir.isDirectory()) {
    			s_logger.debug(dirName + " already exists on secondary storage, and is mounted at " + mountPoint);
    			dirExists = true;
    		} else {
    			if (dir.delete() && _storage.mkdir(dirLocation)) {
    				dirExists = true;
    			}
    		}
    	} else if (_storage.mkdir(dirLocation)) {
    		dirExists = true;
    	}

    	if (dirExists) {
    		s_logger.info(dirName  + " directory created/exists on Secondary Storage.");
    	} else {
    		s_logger.info(dirName + " directory does not exist on Secondary Storage.");
    	}
    	
    	return dirExists;
    }
    
    @Override
    protected String getDefaultScriptsDir() {
        return "./scripts/storage/secondary";
    }
}
