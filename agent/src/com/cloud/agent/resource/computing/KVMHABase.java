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

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo;
import org.libvirt.StoragePoolInfo.StoragePoolState;

import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.OutputInterpreter.AllLinesParser;
import com.cloud.utils.script.Script;


public class KVMHABase {
	private long _timeout = 60000; /*1 minutes*/
	protected static String _heartBeatPath;
	protected long _heartBeatUpdateTimeout = 60000;
	protected long _heartBeatUpdateFreq = 60000;
	protected long _heartBeatUpdateMaxRetry = 3;
	public static enum PoolType {
		PrimaryStorage,
		SecondaryStorage
	}
	public static class NfsStoragePool {
		String _poolUUID;
		String _poolIp;
		String _poolMountSourcePath;
		String _mountDestPath;
		PoolType _type;

		public NfsStoragePool(String poolUUID, String poolIp, String poolSourcePath, String mountDestPath, PoolType type) {
			this._poolUUID = poolUUID;
			this._poolIp = poolIp;
			this._poolMountSourcePath = poolSourcePath;
			this._mountDestPath = mountDestPath;
			this._type = type;
		}
	}

	protected String checkingMountPoint(NfsStoragePool pool, String poolName) {
		String mountSource = pool._poolIp + ":" + pool._poolMountSourcePath;
		String mountPaths = Script.runSimpleBashScript("cat /proc/mounts | grep " + mountSource);
		String destPath = pool._mountDestPath;

		if (mountPaths != null) {
			String token[] = mountPaths.split(" ");
			String mountType = token[2];
			String mountDestPath = token[1];
			if (mountType.equalsIgnoreCase("nfs")) {
				if (poolName != null && !mountDestPath.startsWith(destPath)) {
					/*we need to mount it under poolName*/
					Script mount = new Script("/bin/bash", 60000);
					mount.add("-c");
					mount.add("mount " + mountSource + " " + destPath);
					String result = mount.execute();
					if (result != null) {
						destPath = null;
					}
					destroyVMs(destPath);
				} else if (poolName == null) {
					destPath = mountDestPath;
				}
			}
		} else {
			/*Can't find the mount point?*/
			/*we need to mount it under poolName*/
			if (poolName != null) {
				Script mount = new Script("/bin/bash", 60000);
				mount.add("-c");
				mount.add("mount " + mountSource + " " + destPath);
				String result = mount.execute();
				if (result != null) {
					destPath = null;
				}

				destroyVMs(destPath);
			}
		}

		return destPath;
	}

	protected String getMountPoint(NfsStoragePool storagePool) {

		StoragePool pool = null;
		String poolName = null;
		try {
			pool = LibvirtConnection.getConnection().storagePoolLookupByUUIDString(storagePool._poolUUID);
			if (pool != null) {
				StoragePoolInfo spi = pool.getInfo();
				if (spi.state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
					pool.create(0);
				} else {
					/*Sometimes, the mount point is lost, even libvirt thinks the storage pool still running*/
				}
			}
			poolName = pool.getName();
		} catch (LibvirtException e) {

		} finally {
			try {
				if (pool != null) {
					pool.free();
				}
			} catch (LibvirtException e) {

			}
		}

		return checkingMountPoint(storagePool, poolName);
	}

	protected void destroyVMs(String mountPath) {
		/*if there are VMs using disks under this mount path, destroy them*/
		Script cmd = new Script("/bin/bash", _timeout);
		cmd.add("-c");
		cmd.add("ps axu|grep qemu|grep " + mountPath + "* |awk '{print $2}'");
		AllLinesParser parser = new OutputInterpreter.AllLinesParser();
		String result = cmd.execute(parser);

		if (result != null) {
			return;
		}

		String pids[] = parser.getLines().split("\n");
		for (String pid : pids) {
			Script.runSimpleBashScript("kill -9 " + pid);
		}
	}

	protected String getHBFile(String mountPoint, String hostIP) {
		return mountPoint + File.separator + "KVMHA" + File.separator + "hb-" + hostIP;
	}

	protected String getHBFolder(String mountPoint) {
		return mountPoint + File.separator + "KVMHA" + File.separator;
	}

	protected String runScriptRetry(String cmdString, OutputInterpreter interpreter) {
		String result = null;
		for (int i = 0; i < 3; i++) {
			Script cmd = new Script("/bin/bash", _timeout);
			cmd.add("-c");
			cmd.add(cmdString);
			if (interpreter != null)
				result = cmd.execute(interpreter);
			else {
				result = cmd.execute();
			}
			if (result == Script.ERR_TIMEOUT) {
				continue;
			} else if (result == null) {
				break;
			}
		}

		return result;
	}

	public static void main(String[] args) {

		NfsStoragePool pool = new KVMHAMonitor.NfsStoragePool(null,null,null,null, PoolType.PrimaryStorage);

		KVMHAMonitor haWritter = new KVMHAMonitor(pool, "192.168.1.163", null);
		Thread ha = new Thread(haWritter);
		ha.start();

		KVMHAChecker haChecker = new KVMHAChecker(haWritter.getStoragePools(), "192.168.1.163");

		ExecutorService exe = Executors.newFixedThreadPool(1);
		Future<Boolean> future = exe.submit((Callable<Boolean>)haChecker);
		try {
			for (int i = 0; i < 10; i++) {
				System.out.println(future.get());
				future = exe.submit((Callable<Boolean>)haChecker);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
