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
package com.cloud.storage;

import java.io.File;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.ModifyStoragePoolAnswer;
import com.cloud.agent.api.ModifyStoragePoolCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.StoragePoolInfo;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.CreatePrivateTemplateCommand;
import com.cloud.agent.api.storage.DestroyCommand;
import com.cloud.agent.api.storage.UpgradeDiskAnswer;
import com.cloud.agent.api.storage.UpgradeDiskCommand;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Volume.StorageResourceType;
import com.cloud.storage.template.TemplateInfo;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NfsUtils;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

public abstract class FileSystemStorageResource extends StorageResource {
	protected static final Logger s_logger = Logger.getLogger(FileSystemStorageResource.class);
	protected String _templateRootDir;
	protected String _poolName;
	protected String _poolUuid;
	protected String _localStoragePath;

	@Override
	public boolean existPath(String path) {
		if (path == null) {
			return false;
		}
		final Script cmd = new Script("ls", _timeout, s_logger);
	    cmd.add(File.separator + path);
	
	    final String result = cmd.execute();
	    if (result == null) {
	        return true;
	    }
	
	    if (result == Script.ERR_TIMEOUT) {
	        throw new CloudRuntimeException("Script timed out");
	    }
	
	    return !result.contains("No such file or directory");
	}

	@Override
	public String createPath(final String createPath) {
	    final Script cmd = new Script("mkdir", _timeout, s_logger);
	    cmd.add("-p", File.separator + createPath);
	
	    return cmd.execute();
	}

	@Override
	protected void fillNetworkInformation(StartupCommand cmd) {
		super.fillNetworkInformation(cmd);
		cmd.setIqn(null);
	}



	@Override
	protected long getUsedSize() {
		return getUsedSize(_rootDir);
	}

	
	protected long getUsedSize(String poolPath) {
		poolPath = getPoolPath(poolPath);
		if (poolPath == null) {
			return 0;
		}
		Script command = new Script("/bin/bash", _timeout, s_logger);
	    command.add("-c");
	    command.add("df -Ph " + poolPath + " | grep -v Used | awk '{print $3}' ");
	
	    final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
	    if (command.execute(parser) != null) {
	        return -1;
	    }
	    return convertFilesystemSize(parser.getLine());
	}
	
	private String getPoolPath(String poolPath) {
		if (!existPath(poolPath)) {
			poolPath = File.separator + poolPath;
			if (!existPath(poolPath))
			  return null;
		}
		return poolPath;
	}

	protected long getTotalSize(String poolPath) {
		poolPath = getPoolPath(poolPath);
		if (poolPath == null) {
			return 0;
		}
		Script command = new Script("/bin/bash", _timeout, s_logger);
	    command.add("-c");
	    command.add("df -Ph " + poolPath + " | grep -v Size | awk '{print $2}' ");
	
	    final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
	    if (command.execute(parser) != null) {
	        return -1;
	    }
	    return convertFilesystemSize(parser.getLine());
	}
	
	@Override
	protected long getTotalSize() {
		return getTotalSize(_rootDir);
	}

	@Override
	protected String createTrashDir(String imagePath, StringBuilder path) {
	    final int index = imagePath.lastIndexOf(File.separator) + File.separator.length();
	    path.append(_trashcanDir);
	    path.append(imagePath.substring(_parent.length(), index));
	    path.append(Long.toHexString(System.currentTimeMillis()));
	
	    final Script cmd = new Script("mkdir", _timeout, s_logger);
	    cmd.add("-p", path.toString());
	
	    final String result = cmd.execute();
	    if (result != null) {
	        return result;
	    }
	
	    path.append(File.separator).append(imagePath.substring(index));
	    return null;
	}

	@Override
	protected String destroy(String imagePath) {
		final StringBuilder trashPath = new StringBuilder();
	    String result = createTrashDir(imagePath, trashPath);
	    if (result != null) {
	        return result;
	    }
	
	    final Script cmd = new Script("mv", _timeout, s_logger);
	    cmd.add(imagePath);
	    cmd.add(trashPath.toString());
	    result = cmd.execute();
	    if (result != null) {
	        return result;
	    }
	
	    s_logger.warn("Path " + imagePath + " has been moved to " + trashPath.toString());
	
	    cleanUpEmptyParents(imagePath);
	    return null;
	}

	@Override
	protected void cleanUpEmptyParents(String imagePath) {
	    imagePath = imagePath.substring(0, imagePath.lastIndexOf(File.separator));
	    String destroyPath = null;
	    while (imagePath.length() > _parent.length() && !hasChildren(imagePath)) {
	    	destroyPath = imagePath;
	        imagePath = imagePath.substring(0, imagePath.lastIndexOf(File.separator));
	    }
	    
	    if (destroyPath != null) {
	        final Script cmd = new Script("rm", _timeout, s_logger);
	        cmd.add("-rf", destroyPath);
	        cmd.execute();
	    }
	}

	@Override
	protected Answer execute(DestroyCommand cmd) {
	    VolumeTO volume = cmd.getVolume();
	    String result = null;

  		result = delete(volume.getPath());
	    return new Answer(cmd, result == null, result);
	}

	private String delete(String image) {
	    final Script cmd = new Script(_delvmPath, _timeout, s_logger);
	    cmd.add("-i", image);
	
	
	    final String result = cmd.execute();
	    if (result != null) {
	        return result;
	    }
	
	    //cleanUpEmptyParents(image);
	    return null;
	}


	@Override
	protected String delete(String imagePath, String extra) {
		return delete (imagePath);
	}
	
	protected boolean isSharedNetworkFileSystem(String path) {
		if (path.endsWith("/")) {
			path = path.substring(0, path.length()-1);
		}
		Script command = new Script("/bin/bash", _timeout, s_logger);
	    command.add("-c");
	    command.add("mount -t nfs | grep nfs | awk '{print $3}' | grep -x " + path);
	
	    OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
	    if (command.execute(parser) != null || parser.getLine() == null) { //not NFS client
			 command = new Script("/bin/bash", _timeout, s_logger);
			 command.add("-c");
			 command.add("grep " + _rootDir + " /etc/exports");
			 parser = new OutputInterpreter.OneLineParser();
			  if (command.execute(parser) == null && parser.getLine() != null) {
				  return true;
			  }
	    } else if (parser.getLine() != null) {
	    	return true;
	    }
	    return false;
	}
	
	
	private List<String[]> getNfsMounts(String path) {
		if (path != null && path.endsWith("/")) {
			path = path.substring(0, path.length()-1);
		}
		Script command = new Script("/bin/bash", _timeout, s_logger);
	    command.add("-c");
	    if (path != null) {
	    	command.add("cat /proc/mounts | grep nfs | grep " + path );
	    } else {
	    	command.add("cat /proc/mounts | grep nfs | grep -v rpc_pipefs ");
	    }
	    
	    OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();
	    if (command.execute(parser) != null || parser.getLines() == null) { //not NFS client
			 return null;
	    } else {
	    	List<String[]> result = new ArrayList<String[]>();
	    	String[] lines = parser.getLines().split("\\n");

	    	for (String line: lines){
	    		String [] toks = line.split(" ");
	    		if ( toks.length < 4) {
				continue;
	    		}
	    		String [] hostpart = toks[0].split(":");
	    		if (hostpart.length != 2) {
				continue;
	    		}
	    		String localPath = toks[1];
	    		result.add(new String [] {hostpart[0], hostpart[1], localPath});
	    		
	    	}
	    	return result;
	    }
	}
	
	public List<String[]> getNetworkFileSystemServer(String path) {
		return getNfsMounts(path);
	}

	@Override
    protected String create( String templatePath, final String rootdiskFolder, final String userPath, final String datadiskFolder, final String datadiskName, final int datadiskSize, String localPath) {
 
    	s_logger.debug("Creating volumes by cloning " + templatePath);
        final Script command = new Script(_createvmPath, _timeout, s_logger);

        command.add("-t", templatePath);
        command.add("-i", rootdiskFolder);
        command.add("-u", userPath);
        if (datadiskSize != 0) {
        	command.add("-f", datadiskFolder);
            command.add("-s", Integer.toString(datadiskSize));
            command.add("-n", datadiskName);
        }

        return command.execute();
    }

	@Override
	protected UpgradeDiskAnswer execute(UpgradeDiskCommand cmd) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public StartupCommand  []initialize() {
		StartupCommand [] cmds =  super.initialize();
		StartupStorageCommand cmd = (StartupStorageCommand)cmds[0];

		initLocalStorage(cmd);
		cmd.setTemplateInfo(new HashMap<String, TemplateInfo>()); //empty template info

		return new StartupCommand[] {cmd};
	}

	@Override
	protected StorageResourceType getStorageResourceType() {
		return StorageResourceType.STORAGE_POOL;
	}

	protected String mountNfs(String hostAddress, String hostPath, String localPath) {
		final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
		Script command = new Script("/bin/bash", _timeout, s_logger);
		command.add("-c");
		command.add("mount -t nfs -o acdirmax=0,acdirmin=0 " + hostAddress + ":" + hostPath + " " + localPath);
		String result = command.execute(parser);
		return result;
	}
	
	protected String umountNfs(String localPath) {
		Script command = new Script("/bin/bash", _timeout, s_logger);
		command.add("-c");
		command.add("umount " + localPath);
		String result = command.execute();
		return result;
	}
	
	protected void initLocalStorage(StartupStorageCommand cmd) {
		if (!existPath(_localStoragePath)) {
			createPath(_localStoragePath);
		}
		//setPoolPath(_localStoragePath);
		long capacity = getTotalSize(_localStoragePath);
		long used = getUsedSize(_localStoragePath);
		StoragePoolInfo poolInfo = new StoragePoolInfo( "Local Storage", "file://" + cmd.getPrivateIpAddress() + "/" + _localStoragePath, "localhost", _localStoragePath, _localStoragePath, StoragePoolType.Filesystem, capacity, capacity - used);
		cmd.setPoolInfo(poolInfo);
	}

	
	private Answer setFSStoragePool(ModifyStoragePoolCommand cmd) {
        StoragePoolVO pool = cmd.getPool();
		String localPath = pool.getPath();
		File localStorage = new File(localPath);
		if (!localStorage.exists()) {
			localStorage.mkdir();
		}
		/*String result = setPoolPath(localPath);
		if (result != null) {
			return new Answer(cmd, false, " Failed to create folders");
		}*/
		if (_instance != null) {
			localPath = localPath + File.separator + _instance;
		}
		_poolName = pool.getName();
		long capacity = getTotalSize(localPath);
		long used = getUsedSize(localPath);
		long available = capacity - used;
		Map<String, TemplateInfo> tInfo = new HashMap<String, TemplateInfo>();
		ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd, capacity, available, tInfo);
		return answer;
	}
	
	@Override
	protected Answer execute(ModifyStoragePoolCommand cmd) {
	    StoragePoolVO pool = cmd.getPool();
		if (pool.getPoolType() == StoragePoolType.Filesystem) {
			return setFSStoragePool(cmd);
		}
		if (cmd.getAdd()) {
			String result;
			String hostPath = pool.getPath();
			String hostPath2 = pool.getPath();
			if (hostPath.endsWith("/")) {
				hostPath2 = hostPath.substring(0, hostPath.length()-1);
			}
			String localPath = cmd.getLocalPath();
			boolean alreadyMounted = false;

			List<String[]> shareInfo = getNfsMounts(null);
			if (shareInfo != null) {
				for (String [] share: shareInfo){
					String host = share[0];
					String path = share[1];
					String path2 = path;
					if (path.endsWith("/")) {
						path2 = path.substring(0, path.length()-1);
					}
					if (!path.equals(hostPath)  && !path2.equals(hostPath2)){
						continue;
					}
					if (host.equalsIgnoreCase(pool.getHostAddress())){
						alreadyMounted = true;
						localPath = share[2];
						result = null;
						break;
					}  else {
						try {
							InetAddress currAddr = InetAddress.getByName(host);
							InetAddress hostAddr = InetAddress.getByName(pool.getHostAddress());
							if (currAddr.equals(hostAddr)){
								alreadyMounted = true;
								result = null;
								localPath = share[2];
								break;
							}
						} catch (UnknownHostException e) {
							continue;
						}
					}
				}
			}

			String localPath2 = localPath;
			if (localPath.endsWith("/")){
				localPath2 = localPath.substring(0,localPath.length()-1);
			}
			
			if (!alreadyMounted){
				Script mkdir = new Script("/bin/bash", _timeout, s_logger);
				mkdir.add("-c");
				mkdir.add("mkdir -p " + localPath);
				final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
				result = mkdir.execute(parser);

				if (result != null) {
					return new Answer(cmd, false, "Failed to create local path: " + result);
				}
				result = mountNfs(pool.getHostAddress(), pool.getPath(), localPath);
				if (result != null) {
					return new Answer(cmd, false, " Failed to mount: " + result);
				}
			}

			/*result = setPoolPath(localPath);
			if (result != null) {
				return new Answer(cmd, false, " Failed to create folders");
			}*/


			if (_instance != null) {
				localPath = localPath + File.separator + _instance;
			}
			_poolName =pool.getName();
			_poolUuid = pool.getUuid();
			long capacity = getTotalSize(localPath);
			long used = getUsedSize(localPath);
			long available = capacity - used;
			Map<String, TemplateInfo> tInfo = new HashMap<String, TemplateInfo>();
			ModifyStoragePoolAnswer answer = new ModifyStoragePoolAnswer(cmd, capacity, available, tInfo);
			return answer;
		} else {
			Script command = new Script("/bin/bash", _timeout, s_logger);
		    command.add("-c");
		    command.add("umount  " + cmd.getLocalPath());
		    final OutputInterpreter.OneLineParser parser = new OutputInterpreter.OneLineParser();
		    String result = command.execute(parser);
		    if (result != null) {
		        return new Answer(cmd, false, " Failed to unmount: " + result);
		    }
		    return new Answer(cmd);
		}
	}

	@Override
    protected String create(String templateFolder, String rootdiskFolder, String userPath, String dataPath, String localPath) {
		
		s_logger.debug("Creating volumes");
		final Script command = new Script(_createvmPath, _timeout, s_logger);

		command.add("-t", templateFolder);
		command.add("-i", rootdiskFolder);
        command.add("-u", userPath);
 

        return command.execute();
	}

	
	
	protected String mountSecondaryStorage(String tmplMpt, String templatePath, String hostMpt) {
		String mountStr = null;
		try {
			mountStr = NfsUtils.url2Mount(tmplMpt);
		} catch (URISyntaxException e) {
			s_logger.debug("Is not a valid url" + tmplMpt);
			return null;
		}
		String []tok = mountStr.split(":");
		/*Mount already?*/
		if (!isNfsMounted(tok[0], tok[1], hostMpt)) {
			mountNfs(tok[0], tok[1], hostMpt);
		}
		if (!templatePath.startsWith("/"))
			templatePath = hostMpt + "/" + templatePath;
		else
			templatePath = hostMpt + templatePath;

		return templatePath;
	}
	
	protected boolean isNfsMounted(final String remoteHost, final String remotePath, final String mountPath) {
		boolean alreadyMounted = false;
		List<String[]> shareInfo = getNfsMounts(null);
		if (shareInfo != null) {
			for (String [] share: shareInfo){
				String host = share[0];
				String path = share[1];
				String path2 = path;
				String localPath = share[2];
				String localPath2 = localPath;
			
				if (path.endsWith("/")) {
					path2 = path.substring(0, path.length()-1);
				}
				if (localPath.endsWith("/")) {
					localPath2 = localPath.substring(0, localPath.length() -1);
				}
				if ((!path.equals(remotePath)  && !path2.equals(remotePath)) || (!localPath.equals(mountPath) && !localPath2.equals(mountPath)) ){
					continue;
				}

				if (host.equalsIgnoreCase(remoteHost)){
					alreadyMounted = true;
					break;
				}  else {
					try {
						InetAddress currAddr = InetAddress.getByName(host);
						InetAddress hostAddr = InetAddress.getByName(remoteHost);
						if (currAddr.equals(hostAddr)){
							alreadyMounted = true;
							break;
						}
					} catch (UnknownHostException e) {
						continue;
					}
				}
			}
		}
		return alreadyMounted;
	}

	@Override
	protected String getDefaultScriptsDir() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected CreatePrivateTemplateAnswer execute(CreatePrivateTemplateCommand cmd) {
		CreatePrivateTemplateAnswer answer = super.execute(cmd);
		answer.setPath(answer.getPath().replaceFirst(_rootDir, ""));
		answer.setPath(answer.getPath().replaceFirst("^/*", "/"));
		return answer;
	}

	

}
