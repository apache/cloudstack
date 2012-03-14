/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.hypervisor.vmware.manager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BackupSnapshotAnswer;
import com.cloud.agent.api.BackupSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromSnapshotCommand;
import com.cloud.agent.api.CreatePrivateTemplateFromVolumeCommand;
import com.cloud.agent.api.CreateVolumeFromSnapshotAnswer;
import com.cloud.agent.api.CreateVolumeFromSnapshotCommand;
import com.cloud.agent.api.storage.CopyVolumeAnswer;
import com.cloud.agent.api.storage.CopyVolumeCommand;
import com.cloud.agent.api.storage.CreatePrivateTemplateAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadAnswer;
import com.cloud.agent.api.storage.PrimaryStorageDownloadCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.hypervisor.vmware.mo.CustomFieldConstants;
import com.cloud.hypervisor.vmware.mo.DatacenterMO;
import com.cloud.hypervisor.vmware.mo.DatastoreMO;
import com.cloud.hypervisor.vmware.mo.VirtualMachineMO;
import com.cloud.hypervisor.vmware.mo.VmwareHypervisorHost;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.cloud.hypervisor.vmware.util.VmwareHelper;
import com.cloud.storage.JavaStorageLayer;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.StorageLayer;
import com.cloud.storage.template.VmdkProcessor;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.Pair;
import com.cloud.utils.StringUtils;
import com.cloud.utils.Ternary;
import com.cloud.utils.script.Script;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VirtualDeviceConfigSpec;
import com.vmware.vim25.VirtualDeviceConfigSpecOperation;
import com.vmware.vim25.VirtualDisk;
import com.vmware.vim25.VirtualLsiLogicController;
import com.vmware.vim25.VirtualMachineConfigSpec;
import com.vmware.vim25.VirtualMachineFileInfo;
import com.vmware.vim25.VirtualMachineGuestOsIdentifier;
import com.vmware.vim25.VirtualSCSISharing;

public class VmwareStorageManagerImpl implements VmwareStorageManager {
    private static final Logger s_logger = Logger.getLogger(VmwareStorageManagerImpl.class);
    
    private final VmwareStorageMount _mountService;
    private final StorageLayer _storage = new JavaStorageLayer();
    
    private int _timeout;
    
    public VmwareStorageManagerImpl(VmwareStorageMount mountService) {
        assert(mountService != null);
        _mountService = mountService;
    }
    
    public void configure(Map<String, Object> params) {
        s_logger.info("Configure VmwareStorageManagerImpl");
        
        String value = (String)params.get("scripts.timeout");
        _timeout = NumbersUtil.parseInt(value, 1440) * 1000;
    }
    
    @Override
    public Answer execute(VmwareHostService hostService, PrimaryStorageDownloadCommand cmd) {
		String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
		assert (secondaryStorageUrl != null);

		String templateUrl = cmd.getUrl();

		String templateName = null;
		String mountPoint = null;
		if (templateUrl.endsWith(".ova")) {
			int index = templateUrl.lastIndexOf("/");
			mountPoint = templateUrl.substring(0, index);
			mountPoint = mountPoint.substring(secondaryStorageUrl.length() + 1);
			if (!mountPoint.endsWith("/")) {
				mountPoint = mountPoint + "/";
			}

			templateName = templateUrl.substring(index + 1).replace("." + ImageFormat.OVA.getFileExtension(), "");

			if (templateName == null || templateName.isEmpty()) {
				templateName = cmd.getName();
			}
		} else {
			mountPoint = templateUrl.substring(secondaryStorageUrl.length() + 1);
			if (!mountPoint.endsWith("/")) {
				mountPoint = mountPoint + "/";
			}
			templateName = cmd.getName();
		}
		
		VmwareContext context = hostService.getServiceContext(cmd);
		try {
			VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
			
			String templateUuidName = UUID.nameUUIDFromBytes((templateName + "@" + cmd.getPoolUuid() + "-" + hyperHost.getMor().get_value()).getBytes()).toString();
			// truncate template name to 32 chars to ensure they work well with vSphere API's.
			templateUuidName = templateUuidName.replace("-", ""); 
			
			DatacenterMO dcMo = new DatacenterMO(context, hyperHost.getHyperHostDatacenter());
			VirtualMachineMO templateMo = VmwareHelper.pickOneVmOnRunningHost(dcMo.findVmByNameAndLabel(templateUuidName), true);
			
			if (templateMo == null) {
			    if(s_logger.isInfoEnabled())
			        s_logger.info("Template " + templateName + " is not setup yet, setup template from secondary storage with uuid name: " + templateUuidName);
				ManagedObjectReference morDs = hyperHost.findDatastore(cmd.getPoolUuid());
				assert (morDs != null);
				DatastoreMO primaryStorageDatastoreMo = new DatastoreMO(context, morDs);

				copyTemplateFromSecondaryToPrimary(hyperHost,
					primaryStorageDatastoreMo, secondaryStorageUrl,
					mountPoint, templateName, templateUuidName);
			} else {
				s_logger.info("Template " + templateName + " has already been setup, skip the template setup process in primary storage");
			}

			return new PrimaryStorageDownloadAnswer(templateUuidName, 0);
		} catch (Throwable e) {
			if (e instanceof RemoteException) {
				hostService.invalidateServiceContext(context);
			}

			String msg = "Unable to execute PrimaryStorageDownloadCommand due to exception";
			s_logger.error(msg, e);
			return new PrimaryStorageDownloadAnswer(msg);
		}
    }
    
    @Override
	public Answer execute(VmwareHostService hostService, BackupSnapshotCommand cmd) {
		Long accountId = cmd.getAccountId();
		Long volumeId = cmd.getVolumeId();
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
        String snapshotUuid = cmd.getSnapshotUuid(); // not null: Precondition.
		String prevSnapshotUuid = cmd.getPrevSnapshotUuid();
		String prevBackupUuid = cmd.getPrevBackupUuid();
        VirtualMachineMO workerVm=null;
        String workerVMName = null;
		String volumePath = cmd.getVolumePath();
		ManagedObjectReference morDs = null;
		String snapshotUUID = null;
		DatastoreMO dsMo=null;

		// By default assume failure
		String details = null;
		boolean success = false;
		String snapshotBackupUuid = null;

		VmwareContext context = hostService.getServiceContext(cmd);
		VirtualMachineMO vmMo = null;
		
		try {
			VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
			morDs = hyperHost.findDatastore(cmd.getPool().getUuid());

			try {
				vmMo = hyperHost.findVmOnHyperHost(cmd.getVmName());
				if (vmMo == null) {
					if(s_logger.isDebugEnabled())
						s_logger.debug("Unable to find owner VM for BackupSnapshotCommand on host " + hyperHost.getHyperHostName() + ", will try within datacenter");
	
					vmMo = hyperHost.findVmOnPeerHyperHost(cmd.getVmName());
					if(vmMo == null) {
						dsMo = new DatastoreMO(hyperHost.getContext(), morDs);
						
						workerVMName = hostService.getWorkerName(context, cmd, 0);
	
						// attach a volume to dummay wrapper VM for taking snapshot and exporting the VM for backup
						if (!hyperHost.createBlankVm(workerVMName, 1, 512, 0, false, 4, 0, VirtualMachineGuestOsIdentifier._otherGuest.toString(), morDs, false)) {
							String msg = "Unable to create worker VM to execute BackupSnapshotCommand";
							s_logger.error(msg);
							throw new Exception(msg);
						}
						vmMo = hyperHost.findVmOnHyperHost(workerVMName);
						if (vmMo == null) {
							throw new Exception("Failed to find the newly create or relocated VM. vmName: " + workerVMName);
						}
						workerVm = vmMo;
	
						// attach volume to worker VM
						String datastoreVolumePath = String.format("[%s] %s.vmdk", dsMo.getName(), volumePath);
						vmMo.attachDisk(new String[] { datastoreVolumePath }, morDs);
						snapshotUUID = UUID.randomUUID().toString();
						if (!vmMo.createSnapshot(snapshotUUID, "Snapshot taken for " + cmd.getSnapshotName(), false, false)) {
							throw new Exception("Failed to take snapshot " + cmd.getSnapshotName() + " on vm: " + cmd.getVmName());
						}
					}
				} else {
	                if (!vmMo.createSnapshot(snapshotUuid, "Snapshot taken for " + cmd.getSnapshotName(), false, false)) {
	                    throw new Exception("Failed to take snapshot " + cmd.getSnapshotName() + " on vm: " + cmd.getVmName());
	                }
				}
				
	            snapshotBackupUuid = backupSnapshotToSecondaryStorage(vmMo, accountId, volumeId, cmd.getVolumePath(), snapshotUuid, secondaryStorageUrl, prevSnapshotUuid, prevBackupUuid,
	                    hostService.getWorkerName(context, cmd, 1));

                success = (snapshotBackupUuid != null);
                if (success) {
                    details = "Successfully backedUp the snapshotUuid: " + snapshotUuid + " to secondary storage.";
                }
				
			} finally {
                if(vmMo != null)
                    vmMo.removeAllSnapshots();
			    
				try {
		            if (workerVm != null) {
		                // detach volume and destroy worker vm
		                workerVm.detachAllDisks();
		                workerVm.destroy();
		            }
		        } catch (Throwable e) {
		        	s_logger.warn("Failed to destroy worker VM: " + workerVMName);
		        }			
			}
		} catch (Throwable e) {
			if (e instanceof RemoteException) {
				hostService.invalidateServiceContext(context);
			}

			s_logger.error("Unexpecpted exception ", e);

			details = "BackupSnapshotCommand exception: " + StringUtils.getExceptionStackInfo(e);
			return new BackupSnapshotAnswer(cmd, false, details, snapshotBackupUuid, true);
		}

		return new BackupSnapshotAnswer(cmd, success, details, snapshotBackupUuid, true);
	}

    @Override
	public Answer execute(VmwareHostService hostService, CreatePrivateTemplateFromVolumeCommand cmd) {
        String secondaryStoragePoolURL = cmd.getSecondaryStorageUrl();
		String volumePath = cmd.getVolumePath();
		Long accountId = cmd.getAccountId();
		Long templateId = cmd.getTemplateId();
		String details = null;

		VmwareContext context = hostService.getServiceContext(cmd);
		try {
			VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
			
			VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(cmd.getVmName());
			if (vmMo == null) {
				if(s_logger.isDebugEnabled())
					s_logger.debug("Unable to find the owner VM for CreatePrivateTemplateFromVolumeCommand on host " + hyperHost.getHyperHostName() + ", try within datacenter");
				vmMo = hyperHost.findVmOnPeerHyperHost(cmd.getVmName());

				if(vmMo == null) {
					String msg = "Unable to find the owner VM for volume operation. vm: " + cmd.getVmName();
					s_logger.error(msg);
					throw new Exception(msg);
				}
			}

			Ternary<String, Long, Long> result = createTemplateFromVolume(vmMo,
					accountId, templateId, cmd.getTemplateName(),
					secondaryStoragePoolURL, volumePath, 
					hostService.getWorkerName(context, cmd, 0));

			return new CreatePrivateTemplateAnswer(cmd, true, null,
					result.first(), result.third(), result.second(),
					cmd.getTemplateName(), ImageFormat.OVA);

		} catch (Throwable e) {
			if (e instanceof RemoteException) {
				hostService.invalidateServiceContext(context);
			}

			s_logger.error("Unexpecpted exception ", e);

			details = "CreatePrivateTemplateFromVolumeCommand exception: " + StringUtils.getExceptionStackInfo(e);
			return new CreatePrivateTemplateAnswer(cmd, false, details);
		}
	}

    @Override
	public Answer execute(VmwareHostService hostService, CreatePrivateTemplateFromSnapshotCommand cmd) {
		Long accountId = cmd.getAccountId();
		Long volumeId = cmd.getVolumeId();
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
		String backedUpSnapshotUuid = cmd.getSnapshotUuid();
		Long newTemplateId = cmd.getNewTemplateId();
		String details;
		String uniqeName = UUID.randomUUID().toString();

		VmwareContext context = hostService.getServiceContext(cmd);
		try {
			Ternary<String, Long, Long> result = createTemplateFromSnapshot(accountId,
				newTemplateId, uniqeName,
				secondaryStorageUrl, volumeId,
				backedUpSnapshotUuid);

			return new CreatePrivateTemplateAnswer(cmd, true, null,
					result.first(), result.third(), result.second(),
					uniqeName, ImageFormat.OVA);
		} catch (Throwable e) {
			if (e instanceof RemoteException) {
				hostService.invalidateServiceContext(context);
			}

			s_logger.error("Unexpecpted exception ", e);

			details = "CreatePrivateTemplateFromSnapshotCommand exception: " + StringUtils.getExceptionStackInfo(e);
			return new CreatePrivateTemplateAnswer(cmd, false, details);
		}
	}
	
    @Override
	public Answer execute(VmwareHostService hostService, CopyVolumeCommand cmd) {
		Long volumeId = cmd.getVolumeId();
		String volumePath = cmd.getVolumePath();
		String secondaryStorageURL = cmd.getSecondaryStorageURL();
		String vmName = cmd.getVmName();

		VmwareContext context = hostService.getServiceContext(cmd);
		try {
			VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);

			Pair<String, String> result;
			if (cmd.toSecondaryStorage()) {
				result = copyVolumeToSecStorage(hostService,
						hyperHost, cmd, vmName, volumeId, cmd.getPool().getUuid(), volumePath,
						secondaryStorageURL,
						hostService.getWorkerName(context, cmd, 0));
			} else {
				StorageFilerTO poolTO = cmd.getPool();

				ManagedObjectReference morDatastore = hyperHost.findDatastore(poolTO.getUuid());
				if (morDatastore == null) {
					morDatastore = hyperHost.mountDatastore(
							false,
							poolTO.getHost(), 0, poolTO.getPath(),
							poolTO.getUuid());

					if (morDatastore == null) {
						throw new Exception("Unable to mount storage pool on host. storeUrl: " + poolTO.getHost() + ":/" + poolTO.getPath());
					}
				}

				result = copyVolumeFromSecStorage(
						hyperHost, volumeId,
						new DatastoreMO(context, morDatastore),
						secondaryStorageURL, volumePath);
			}
			return new CopyVolumeAnswer(cmd, true, null, result.first(), result.second());
		} catch (Throwable e) {
			if (e instanceof RemoteException) {
				hostService.invalidateServiceContext(context);
			}

			String msg = "Unable to execute CopyVolumeCommand due to exception";
			s_logger.error(msg, e);
			return new CopyVolumeAnswer(cmd, false, "CopyVolumeCommand failed due to exception: " + StringUtils.getExceptionStackInfo(e), null, null);
		}
	}
    
    @Override
	public Answer execute(VmwareHostService hostService, CreateVolumeFromSnapshotCommand cmd) {

		String primaryStorageNameLabel = cmd.getPrimaryStoragePoolNameLabel();
		Long accountId = cmd.getAccountId();
		Long volumeId = cmd.getVolumeId();
        String secondaryStorageUrl = cmd.getSecondaryStorageUrl();
		String backedUpSnapshotUuid = cmd.getSnapshotUuid();

		String details = null;
		boolean success = false;
		String newVolumeName = UUID.randomUUID().toString().replaceAll("-", "");

		VmwareContext context = hostService.getServiceContext(cmd);
		try {
			VmwareHypervisorHost hyperHost = hostService.getHyperHost(context, cmd);
			
			ManagedObjectReference morPrimaryDs = hyperHost.findDatastore(primaryStorageNameLabel);
			if (morPrimaryDs == null) {
				String msg = "Unable to find datastore: " + primaryStorageNameLabel;
				s_logger.error(msg);
				throw new Exception(msg);
			}

			DatastoreMO primaryDsMo = new DatastoreMO(hyperHost.getContext(), morPrimaryDs);
			details = createVolumeFromSnapshot(hyperHost, primaryDsMo,
					newVolumeName, accountId, volumeId, secondaryStorageUrl, backedUpSnapshotUuid);
			if (details == null) {
				success = true;
			}
		} catch (Throwable e) {
			if (e instanceof RemoteException) {
				hostService.invalidateServiceContext(context);
			}

			s_logger.error("Unexpecpted exception ", e);
			details = "CreateVolumeFromSnapshotCommand exception: " + StringUtils.getExceptionStackInfo(e);
		}

		return new CreateVolumeFromSnapshotAnswer(cmd, success, details, newVolumeName);
	}
	
    // templateName: name in secondary storage
    // templateUuid: will be used at hypervisor layer
    private void copyTemplateFromSecondaryToPrimary(VmwareHypervisorHost hyperHost, DatastoreMO datastoreMo, String secondaryStorageUrl,
        String templatePathAtSecondaryStorage, String templateName, String templateUuid) throws Exception {
        
        s_logger.info("Executing copyTemplateFromSecondaryToPrimary. secondaryStorage: " 
            + secondaryStorageUrl + ", templatePathAtSecondaryStorage: " + templatePathAtSecondaryStorage
            + ", templateName: " + templateName);
        
        String secondaryMountPoint = _mountService.getMountPoint(secondaryStorageUrl);
        s_logger.info("Secondary storage mount point: " + secondaryMountPoint);
        
        String srcOVAFileName = secondaryMountPoint + "/" +  templatePathAtSecondaryStorage + 
            templateName + "." + ImageFormat.OVA.getFileExtension();
        
        String srcFileName = getOVFFilePath(srcOVAFileName);
        if(srcFileName == null) {
            Script command = new Script("tar", 0, s_logger);
            command.add("--no-same-owner");
            command.add("-xf", srcOVAFileName);
            command.setWorkDir(secondaryMountPoint + "/" +  templatePathAtSecondaryStorage);
            s_logger.info("Executing command: " + command.toString());
            String result = command.execute();
            if(result != null) {
                String msg = "Unable to unpack snapshot OVA file at: " + srcOVAFileName;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }
        
        srcFileName = getOVFFilePath(srcOVAFileName);
        if(srcFileName == null) {    
            String msg = "Unable to locate OVF file in template package directory: " + srcOVAFileName; 
            s_logger.error(msg);
            throw new Exception(msg);
        }
        
        String vmName = templateUuid;
        hyperHost.importVmFromOVF(srcFileName, vmName, datastoreMo, "thin");
        
        VirtualMachineMO vmMo = hyperHost.findVmOnHyperHost(vmName);
        if(vmMo == null) {
            String msg = "Failed to import OVA template. secondaryStorage: " 
                + secondaryStorageUrl + ", templatePathAtSecondaryStorage: " + templatePathAtSecondaryStorage
                + ", templateName: " + templateName + ", templateUuid: " + templateUuid;
            s_logger.error(msg);
            throw new Exception(msg);
        }
        
        if(vmMo.createSnapshot("cloud.template.base", "Base snapshot", false, false)) {
            vmMo.setCustomFieldValue(CustomFieldConstants.CLOUD_UUID, templateUuid);
            vmMo.markAsTemplate();
        } else {
            vmMo.destroy();
            String msg = "Unable to create base snapshot for template, templateName: " + templateName + ", templateUuid: " + templateUuid; 
            s_logger.error(msg);
            throw new Exception(msg);
        }
    }
    
    private Ternary<String, Long, Long> createTemplateFromVolume(VirtualMachineMO vmMo, long accountId, long templateId, String templateName, 
        String secStorageUrl, String volumePath, String workerVmName) throws Exception {
        
        String secondaryMountPoint = _mountService.getMountPoint(secStorageUrl);
        String installPath = getTemplateRelativeDirInSecStorage(accountId, templateId);
        String installFullPath = secondaryMountPoint + "/" + installPath;
        synchronized(installPath.intern()) {
            Script command = new Script(false, "mkdir", _timeout, s_logger);
            command.add("-p");
            command.add(installFullPath);
            
            String result = command.execute();
            if(result != null) {
                String msg = "unable to prepare template directory: " 
                    + installPath + ", storage: " + secStorageUrl + ", error msg: " + result;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }
        
        String snapshotUuidName = UUID.randomUUID().toString();
        VirtualMachineMO clonedVm = null;
        try {
            Pair<VirtualDisk, String> volumeDeviceInfo = vmMo.getDiskDevice(volumePath, false);
            if(volumeDeviceInfo == null) {
                String msg = "Unable to find related disk device for volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }
            
            if(!vmMo.createSnapshot(snapshotUuidName, "Temporary snapshot for template creation", false, false)) {
                String msg = "Unable to take snapshot for creating template from volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        
            // 4 MB is the minimum requirement for VM memory in VMware
            vmMo.cloneFromCurrentSnapshot(workerVmName, 0, 4, volumeDeviceInfo.second(), 
                VmwareHelper.getDiskDeviceDatastore(volumeDeviceInfo.first()));
            clonedVm = vmMo.getRunningHost().findVmOnHyperHost(workerVmName);
            if(clonedVm == null) {
                String msg = "Unable to create dummy VM to export volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        
            clonedVm.exportVm(secondaryMountPoint + "/" + installPath, snapshotUuidName, true, false);
            
            long physicalSize = new File(installFullPath + "/" + snapshotUuidName + ".ova").length();
            VmdkProcessor processor = new VmdkProcessor();
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            processor.configure("VMDK Processor", params);
            long virtualSize = processor.getTemplateVirtualSize(installFullPath, snapshotUuidName);

            postCreatePrivateTemplate(installFullPath, templateId, snapshotUuidName, physicalSize, virtualSize);
            return new Ternary<String, Long, Long>(installPath + "/" + snapshotUuidName + ".ova", physicalSize, virtualSize);
            
        } finally {
            if(clonedVm != null) {
                clonedVm.detachAllDisks();
                clonedVm.destroy();
            }
        
            vmMo.removeSnapshot(snapshotUuidName, false);
        }
    }
    
    private Ternary<String, Long, Long> createTemplateFromSnapshot(long accountId, long templateId, String templateName, 
        String secStorageUrl, long volumeId, String backedUpSnapshotUuid) throws Exception {
        
        String snapshotUuidName = UUID.randomUUID().toString();
        String secondaryMountPoint = _mountService.getMountPoint(secStorageUrl);
        String installPath = getTemplateRelativeDirInSecStorage(accountId, templateId);
        String installFullPath = secondaryMountPoint + "/" + installPath;
        String installFullName = installFullPath + "/" + snapshotUuidName + ".ova";
        String snapshotFullName = secondaryMountPoint + "/" + getSnapshotRelativeDirInSecStorage(accountId, volumeId) 
            + "/" + backedUpSnapshotUuid + ".ova";
        String result;
        Script command;
        
        synchronized(installPath.intern()) {
            command = new Script(false, "mkdir", _timeout, s_logger);
            command.add("-p");
            command.add(installFullPath);
            
            result = command.execute();
            if(result != null) {
                String msg = "unable to prepare template directory: " 
                    + installPath + ", storage: " + secStorageUrl + ", error msg: " + result;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }
        
        try {
            command = new Script(false, "cp", _timeout, s_logger);
            command.add(snapshotFullName);
            command.add(installFullName);
            result = command.execute();
            if(result != null) {
                String msg = "unable to copy snapshot " + snapshotFullName + " to " + installFullPath; 
                s_logger.error(msg);
                throw new Exception(msg);
            }
            
            // untar OVA file at template directory
            command = new Script("tar", 0, s_logger);
            command.add("--no-same-owner");
            command.add("-xf", installFullName);
            command.setWorkDir(installFullPath);
            s_logger.info("Executing command: " + command.toString());
            result = command.execute();
            if(result != null) {
                String msg = "unable to untar snapshot " + snapshotFullName + " to " 
                    + installFullPath; 
                s_logger.error(msg);
                throw new Exception(msg);
            }
            
            long physicalSize = new File(installFullPath + "/" + snapshotUuidName + ".ova").length();
            VmdkProcessor processor = new VmdkProcessor();
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(StorageLayer.InstanceConfigKey, _storage);
            processor.configure("VMDK Processor", params);
            long virtualSize = processor.getTemplateVirtualSize(installFullPath, snapshotUuidName);

            postCreatePrivateTemplate(installFullPath, templateId, snapshotUuidName, physicalSize, virtualSize);
            return new Ternary<String, Long, Long>(installPath + "/" + snapshotUuidName + ".ova", physicalSize, virtualSize);
        
        } catch(Exception e) {
            // TODO, clean up left over files
            throw e;
        }
    }
    
    private void postCreatePrivateTemplate(String installFullPath, long templateId, 
        String templateName, long size, long virtualSize) throws Exception {

        // TODO a bit ugly here
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(installFullPath + "/template.properties")));
            out.write("filename=" + templateName + ".ova"); 
            out.newLine();
            out.write("description="); 
            out.newLine();
            out.write("checksum="); 
            out.newLine();
            out.write("hvm=false"); 
            out.newLine();
            out.write("size=" + size); 
            out.newLine();
            out.write("ova=true"); 
            out.newLine();
            out.write("id=" + templateId); 
            out.newLine();
            out.write("public=false"); 
            out.newLine();
            out.write("ova.filename=" + templateName + ".ova"); 
            out.newLine();
            out.write("uniquename=" + templateName);
            out.newLine();
            out.write("ova.virtualsize=" + virtualSize); 
            out.newLine();
            out.write("virtualsize=" + virtualSize); 
            out.newLine();
            out.write("ova.size=" + size); 
            out.newLine();
        } finally {
            if(out != null)
                out.close();
        }
    }

    private String createVolumeFromSnapshot(VmwareHypervisorHost hyperHost, DatastoreMO primaryDsMo, String newVolumeName, 
        long accountId, long volumeId, String secStorageUrl, String snapshotBackupUuid) throws Exception {
        
        restoreVolumeFromSecStorage(hyperHost, primaryDsMo, newVolumeName, 
            secStorageUrl, getSnapshotRelativeDirInSecStorage(accountId, volumeId), snapshotBackupUuid);
        return null;
    }
    
    private void restoreVolumeFromSecStorage(VmwareHypervisorHost hyperHost, DatastoreMO primaryDsMo, String newVolumeName, 
        String secStorageUrl, String secStorageDir, String backupName) throws Exception {
        
        String secondaryMountPoint = _mountService.getMountPoint(secStorageUrl);
        String srcOVAFileName = secondaryMountPoint + "/" +  secStorageDir + "/"  
            + backupName + "." + ImageFormat.OVA.getFileExtension();
        
        String srcFileName = getOVFFilePath(srcOVAFileName);
        if(srcFileName == null) {
            Script command = new Script("tar", 0, s_logger);
            command.add("--no-same-owner");
            command.add("-xf", srcOVAFileName);
            command.setWorkDir(secondaryMountPoint + "/" +  secStorageDir);
            s_logger.info("Executing command: " + command.toString());
            String result = command.execute();
            if(result != null) {
                String msg = "Unable to unpack snapshot OVA file at: " + srcOVAFileName;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        }
        
        srcFileName = getOVFFilePath(srcOVAFileName);
        if(srcFileName == null) {
            String msg = "Unable to locate OVF file in template package directory: " + srcOVAFileName; 
            s_logger.error(msg);
            throw new Exception(msg);
        }
        
        VirtualMachineMO clonedVm = null;
        try {
            hyperHost.importVmFromOVF(srcFileName, newVolumeName, primaryDsMo, "thin");
            clonedVm = hyperHost.findVmOnHyperHost(newVolumeName);
            if(clonedVm == null)
                throw new Exception("Unable to create container VM for volume creation");
            
            clonedVm.moveAllVmDiskFiles(primaryDsMo, "", false);
            clonedVm.detachAllDisks();
        } finally {
            if(clonedVm != null) {
                clonedVm.detachAllDisks();
                clonedVm.destroy();
            }
        }
    }
    
    private String backupSnapshotToSecondaryStorage(VirtualMachineMO vmMo, long accountId, long volumeId, 
        String volumePath, String snapshotUuid, String secStorageUrl, 
        String prevSnapshotUuid, String prevBackupUuid, String workerVmName) throws Exception {
        
        String backupUuid = UUID.randomUUID().toString();
        exportVolumeToSecondaryStroage(vmMo, volumePath, secStorageUrl, 
            getSnapshotRelativeDirInSecStorage(accountId, volumeId), backupUuid, workerVmName);
        return backupUuid;
    }
    
    private void exportVolumeToSecondaryStroage(VirtualMachineMO vmMo, String volumePath,  
        String secStorageUrl, String secStorageDir, String exportName,
        String workerVmName) throws Exception {
        
        String secondaryMountPoint = _mountService.getMountPoint(secStorageUrl);
        String exportPath =  secondaryMountPoint + "/" + secStorageDir;
        
        synchronized(exportPath.intern()) {
            if(!new File(exportPath).exists()) {
                Script command = new Script(false, "mkdir", _timeout, s_logger);
                command.add("-p");
                command.add(exportPath);
                if(command.execute() != null)
                    throw new Exception("unable to prepare snapshot backup directory");
            }
        }

        VirtualMachineMO clonedVm = null;
        try {
        
            Pair<VirtualDisk, String> volumeDeviceInfo = vmMo.getDiskDevice(volumePath, false);
            if(volumeDeviceInfo == null) {
                String msg = "Unable to find related disk device for volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        
            // 4 MB is the minimum requirement for VM memory in VMware
            vmMo.cloneFromCurrentSnapshot(workerVmName, 0, 4, volumeDeviceInfo.second(), 
                VmwareHelper.getDiskDeviceDatastore(volumeDeviceInfo.first()));
            clonedVm = vmMo.getRunningHost().findVmOnHyperHost(workerVmName);
            if(clonedVm == null) {
                String msg = "Unable to create dummy VM to export volume. volume path: " + volumePath;
                s_logger.error(msg);
                throw new Exception(msg);
            }
        
            clonedVm.exportVm(exportPath, exportName, true, true);
        } finally {
            if(clonedVm != null) {
                clonedVm.detachAllDisks();
                clonedVm.destroy();
            }
        }
    }
    
    private String deleteSnapshotOnSecondaryStorge(long accountId, long volumeId, String secStorageUrl, String backupUuid) throws Exception {

        String secondaryMountPoint = _mountService.getMountPoint(secStorageUrl);
        String snapshotMountRoot = secondaryMountPoint + "/" + getSnapshotRelativeDirInSecStorage(accountId, volumeId);
        File file = new File(snapshotMountRoot + "/" + backupUuid + ".ova");
        if(file.exists()) {
            if(file.delete())
                return null;
            
        } else {
            return "Backup file does not exist. backupUuid: " + backupUuid;
        }
        
        return "Failed to delete snapshot backup file, backupUuid: " + backupUuid;
    }
    
    private Pair<String, String> copyVolumeToSecStorage(VmwareHostService hostService, VmwareHypervisorHost hyperHost, CopyVolumeCommand cmd, 
        String vmName, long volumeId, String poolId, String volumePath, 
        String secStorageUrl, String workerVmName) throws Exception {
        
        String volumeFolder = String.valueOf(volumeId) + "/";
        VirtualMachineMO workerVm=null;
        VirtualMachineMO vmMo=null;
        String exportName = UUID.randomUUID().toString();

        try {
            ManagedObjectReference morDs = hyperHost.findDatastore(poolId);

            if (morDs == null) {
                String msg = "Unable to find volumes's storage pool for copy volume operation";
                s_logger.error(msg);
                throw new Exception(msg);
            }

            vmMo = hyperHost.findVmOnHyperHost(vmName);
            if (vmMo == null) {
                // create a dummy worker vm for attaching the volume
                DatastoreMO dsMo = new DatastoreMO(hyperHost.getContext(), morDs);
                //restrict VM name to 32 chars, (else snapshot descriptor file name will be truncated to 32 chars of vm name)
                VirtualMachineConfigSpec vmConfig = new VirtualMachineConfigSpec();
                vmConfig.setName(workerVmName);
                vmConfig.setMemoryMB((long) 4);
                vmConfig.setNumCPUs(1);
                vmConfig.setGuestId(VirtualMachineGuestOsIdentifier._otherGuest.toString());
                VirtualMachineFileInfo fileInfo = new VirtualMachineFileInfo();
                fileInfo.setVmPathName(String.format("[%s]", dsMo.getName()));
                vmConfig.setFiles(fileInfo);

                // Scsi controller
                VirtualLsiLogicController scsiController = new VirtualLsiLogicController();
                scsiController.setSharedBus(VirtualSCSISharing.noSharing);
                scsiController.setBusNumber(0);
                scsiController.setKey(1);
                VirtualDeviceConfigSpec scsiControllerSpec = new VirtualDeviceConfigSpec();
                scsiControllerSpec.setDevice(scsiController);
                scsiControllerSpec.setOperation(VirtualDeviceConfigSpecOperation.add);
                vmConfig.setDeviceChange(new VirtualDeviceConfigSpec[] { scsiControllerSpec });
	
                hyperHost.createVm(vmConfig);
                workerVm = hyperHost.findVmOnHyperHost(workerVmName);
                if (workerVm == null) {
                    String msg = "Unable to create worker VM to execute CopyVolumeCommand";
                    s_logger.error(msg);
                    throw new Exception(msg);
                }
	
                //attach volume to worker VM
                String datastoreVolumePath = String.format("[%s] %s.vmdk", dsMo.getName(), volumePath);
                workerVm.attachDisk(new String[] { datastoreVolumePath }, morDs);
                vmMo = workerVm;
            }

            vmMo.createSnapshot(exportName, "Temporary snapshot for copy-volume command", false, false);

            exportVolumeToSecondaryStroage(vmMo, volumePath, secStorageUrl, "volumes/" + volumeFolder, exportName, 
                hostService.getWorkerName(hyperHost.getContext(), cmd, 1));
            return new Pair<String, String>(volumeFolder, exportName);

        } finally {
            vmMo.removeSnapshot(exportName, false);
            if (workerVm != null) {
                //detach volume and destroy worker vm
                workerVm.detachAllDisks();
                workerVm.destroy();
            }
        }
    }

    private Pair<String, String> copyVolumeFromSecStorage(VmwareHypervisorHost hyperHost, long volumeId, 
        DatastoreMO dsMo, String secStorageUrl, String exportName) throws Exception {

        String volumeFolder = String.valueOf(volumeId) + "/";
        String newVolume    = UUID.randomUUID().toString().replaceAll("-", "");
        restoreVolumeFromSecStorage(hyperHost, dsMo, newVolume, secStorageUrl, "volumes/" + volumeFolder, exportName);
        
        return new Pair<String, String>(volumeFolder, newVolume);
    }
    
    private String getOVFFilePath(String srcOVAFileName) {
        File file = new File(srcOVAFileName);
        assert(_storage != null);
        String[] files = _storage.listFiles(file.getParent());
        if(files != null) {
            for(String fileName : files) {
                if(fileName.toLowerCase().endsWith(".ovf")) {
                    File ovfFile = new File(fileName);
                    return file.getParent() + File.separator + ovfFile.getName();
                }
            }
        }
        return null;
    }
    
    private static String getTemplateRelativeDirInSecStorage(long accountId, long templateId) {
        return "template/tmpl/" + accountId + "/" + templateId;
    }
    
    private static String getSnapshotRelativeDirInSecStorage(long accountId, long volumeId) {
        return "snapshots/" + accountId + "/" + volumeId;
    }
}
