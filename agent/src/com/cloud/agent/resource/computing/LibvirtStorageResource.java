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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.libvirt.Connect;
import org.libvirt.LibvirtException;
import org.libvirt.StoragePool;
import org.libvirt.StoragePoolInfo;
import org.libvirt.StorageVol;
import org.libvirt.StoragePoolInfo.StoragePoolState;

import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.agent.resource.computing.KVMHABase.PoolType;
import com.cloud.agent.resource.computing.LibvirtStoragePoolDef.poolType;
import com.cloud.agent.resource.computing.LibvirtStorageVolumeDef.volFormat;
import com.cloud.exception.InternalErrorException;
import com.cloud.hypervisor.xen.resource.CitrixResourceBase.SRType;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.Storage;
import com.cloud.storage.StorageLayer;
import com.cloud.utils.script.Script;

public class LibvirtStorageResource {
    private static final Logger s_logger = Logger.getLogger(LibvirtStorageResource.class);
    private LibvirtComputingResource _computingResource;
    private final Map<String, Object> _storagePools = new ConcurrentHashMap<String, Object>();
    private StorageLayer _storageLayer;
    private String _createvmPath;
    private int _timeout;
    private String _mountPoint;
    private KVMHAMonitor _monitor;
    
    public LibvirtStorageResource(LibvirtComputingResource resource, StorageLayer storage, String createDiskScript, int timeout,
                                  String mountPoint, KVMHAMonitor monitor) {
        _computingResource = resource;
        _storageLayer = storage;
        _createvmPath = createDiskScript;
        _timeout = timeout;
        _mountPoint = mountPoint;
        _monitor = monitor;
    }
    public StoragePool getStoragePool(Connect conn, String uuid) throws LibvirtException {
        StoragePool storage = null;
        try {
            storage = conn.storagePoolLookupByUUIDString(uuid);
        } catch (LibvirtException e) {
            throw e;
        }
        
        if ( storage.getInfo().state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
            storage.create(0);
        }
        return storage;
    }
    
    public boolean deleteStoragePool(Connect conn, StorageFilerTO spt) throws LibvirtException {
        StoragePool pool = getStoragePool(conn, spt.getUuid());
        LibvirtStoragePoolDef spd = getStoragePoolDef(conn, pool);
        
        synchronized (getStoragePool(pool.getUUIDString())) {
            pool.destroy();
            pool.undefine();
        }
        
       
        
        if (spd.getPoolType() == poolType.NETFS) {
            KVMHABase.NfsStoragePool sp = new KVMHABase.NfsStoragePool(spt.getUuid(),
                    spt.getHost(),
                    spt.getPath(),
                    spd.getTargetPath(),
                    PoolType.PrimaryStorage);
            _monitor.removeStoragePool(sp);
        }
        rmStoragePool(spt.getUuid());
        return true;
    }
    
    public boolean deleteStoragePool(Connect conn, StoragePool pool) throws LibvirtException {
        if (pool != null) {
            String uuid = pool.getUUIDString();
            synchronized (getStoragePool(uuid)) {
                pool.destroy();
                pool.undefine();
                pool.free();
            }
            rmStoragePool(uuid);
        }
        return true;
    }
    
    public StorageVol getVolume(Connect conn, StoragePool pool, String volPath) throws LibvirtException {
        StorageVol vol = null;
        try {
            vol = conn.storageVolLookupByKey(volPath);
        } catch (LibvirtException e) {
            
        }
        if (vol == null) {
            storagePoolRefresh(pool);
            vol = conn.storageVolLookupByKey(volPath);
        }
        return vol;
    }
    
    public StorageVol createVolumeFromTempl(StoragePool destPool, StorageVol tmplVol) throws LibvirtException {
        if (_computingResource.isCentosHost()) {
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
    
    private void addStoragePool(String uuid) {
        synchronized (_storagePools) {
            if (!_storagePools.containsKey(uuid)) {
                _storagePools.put(uuid, new Object());
            }
        }
    }
    
    private void rmStoragePool(String uuid) {
        synchronized (_storagePools) {
            if (_storagePools.containsKey(uuid)) {
                _storagePools.remove(uuid);
            }
        }
    }
    
    private Object getStoragePool(String uuid) {
        synchronized (_storagePools) {
            if (!_storagePools.containsKey(uuid)) {         
                addStoragePool(uuid);
            }
            return _storagePools.get(uuid);
        }
    }
   
    
    public StorageVol createTmplDataDisk(Connect conn, StoragePool pool, long size) throws LibvirtException, InternalErrorException {
        /*create a templ data disk, to contain patches*/
        LibvirtStorageVolumeDef volDef = new LibvirtStorageVolumeDef(UUID.randomUUID().toString(), size, volFormat.RAW, null, null);
        StorageVol dataVol =  pool.storageVolCreateXML(volDef.toString(), 0);

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
    
    public StorageVol createVolume(Connect conn, StoragePool pool, String uuid, long size, volFormat format) throws LibvirtException {
        LibvirtStorageVolumeDef volDef = new LibvirtStorageVolumeDef(UUID.randomUUID().toString(), size, format, null, null);
        s_logger.debug(volDef.toString());
        return pool.storageVolCreateXML(volDef.toString(), 0);
    }
    
    public StoragePool getStoragePoolbyURI(Connect conn, URI uri) throws LibvirtException {
        String sourcePath;
        String uuid;
        String sourceHost = "";
        String protocal;
        if (uri.getScheme().equalsIgnoreCase("local")) {
            sourcePath = _mountPoint + File.separator + uri.toString().replace("local:///", "");
            sourcePath = sourcePath.replace("//", "/");
            uuid = UUID.nameUUIDFromBytes(new String(sourcePath).getBytes()).toString();
            protocal = "DIR";
        } else {
            sourcePath = uri.getPath();
            sourcePath = sourcePath.replace("//", "/");
            sourceHost = uri.getHost();
            uuid = UUID.nameUUIDFromBytes(new String(sourceHost + sourcePath).getBytes()).toString();
            protocal = "NFS";
        }
       
       
        String targetPath = _mountPoint + File.separator + uuid;
        StoragePool sp = null;
        try {
            sp = conn.storagePoolLookupByUUIDString(uuid);
        }  catch (LibvirtException e) {
        }

        if (sp == null) {
            try {
                LibvirtStoragePoolDef spd = null;
                if (protocal.equalsIgnoreCase("NFS")) {
                    _storageLayer.mkdir(targetPath);
                    spd = new LibvirtStoragePoolDef(poolType.NETFS, uuid, uuid,
                            sourceHost, sourcePath, targetPath);
                    s_logger.debug(spd.toString());
                    addStoragePool(uuid);

                } else if (protocal.equalsIgnoreCase("DIR")) {
                    _storageLayer.mkdir(targetPath);
                    spd = new LibvirtStoragePoolDef(poolType.DIR, uuid, uuid,
                            null, null, sourcePath);
                }
                
                synchronized (getStoragePool(uuid)) {
                    sp = conn.storagePoolDefineXML(spd.toString(), 0);

                    if (sp == null) {
                        s_logger.debug("Failed to define storage pool");
                        return null;
                    }
                    sp.create(0);
                }

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
    
    public void storagePoolRefresh(StoragePool pool) {
        try {
            synchronized (getStoragePool(pool.getUUIDString())) {
                pool.refresh(0);
            }
        } catch (LibvirtException e) {
            
        }
    }
    
    private StoragePool createNfsStoragePool(Connect conn, StorageFilerTO pool) {
        String targetPath = _mountPoint + File.separator + pool.getUuid();
        LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(poolType.NETFS, pool.getUuid(), pool.getUuid(),
                                                              pool.getHost(), pool.getPath(), targetPath);
        _storageLayer.mkdir(targetPath);
        StoragePool sp = null;
        try {
            s_logger.debug(spd.toString());
            addStoragePool(pool.getUuid());
            
            synchronized (getStoragePool(pool.getUuid())) {
                sp = conn.storagePoolDefineXML(spd.toString(), 0);
                sp.create(0);
            }
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
    
    private StoragePool CreateSharedStoragePool(Connect conn, StorageFilerTO pool) {
        String mountPoint = pool.getPath();
        if (!_storageLayer.exists(mountPoint)) {
            return null;
        }
        LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(poolType.DIR, pool.getUuid(), pool.getUuid(),
                                                             pool.getHost(), pool.getPath(), pool.getPath());
        StoragePool sp = null;
        try {
            s_logger.debug(spd.toString());
            addStoragePool(pool.getUuid());
            synchronized (getStoragePool(pool.getUuid())) {
                sp = conn.storagePoolDefineXML(spd.toString(), 0);
                sp.create(0);
            }
            return sp;
        } catch (LibvirtException e) {
            s_logger.debug(e.toString());
            if (sp != null) {
                try {
                    sp.undefine();
                    sp.free();
                } catch (LibvirtException l) {
                    s_logger.debug("Failed to define shared mount point storage pool with: " + l.toString());
                }
            }
            return null;
        }
    }
    
    public StoragePool getStoragePool(Connect conn, StorageFilerTO spt) {
        StoragePool sp = null;
        try {
            sp = conn.storagePoolLookupByUUIDString(spt.getUuid());
            //double check the mount point
            String targetPath = _mountPoint + File.separator + spt.getUuid();
            if (!_storageLayer.exists(targetPath)) {
            	try {
            		sp.destroy();
            		sp.undefine();
            	} catch(LibvirtException e) {
            		
            	}
            	sp = null;
            }
        } catch (LibvirtException e) {
        }
        
        if (sp == null) {
            if (spt.getType() == StoragePoolType.NetworkFilesystem) {
                sp = createNfsStoragePool(conn, spt);
            } else if (spt.getType() == StoragePoolType.SharedMountPoint) {
                sp = CreateSharedStoragePool(conn, spt);
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
        	s_logger.debug("Failed to create storage pool: " + e.toString());
        	return null;
        }
        
        if (spt.getType() == StoragePoolType.NetworkFilesystem) {
            KVMHABase.NfsStoragePool pool = new KVMHABase.NfsStoragePool(spt.getUuid(),
                    spt.getHost(),
                    spt.getPath(),
                    _mountPoint + File.separator + spt.getUuid(),
                    PoolType.PrimaryStorage);
            _monitor.addStoragePool(pool);
        }
        
        addStoragePool(spt.getUuid());

        return sp;
    }
    
    public StorageVol copyVolume(StoragePool destPool, LibvirtStorageVolumeDef destVol, StorageVol srcVol, int timeout) throws LibvirtException {
        StorageVol vol = destPool.storageVolCreateXML(destVol.toString(), 0);
        String srcPath = srcVol.getKey();
        String destPath = vol.getKey();
        Script.runSimpleBashScript("cp " + srcPath + " " + destPath, timeout);
        return vol;
    }
    
    public boolean copyVolume(String srcPath, String destPath, String volumeName, int timeout)  throws InternalErrorException{
        _storageLayer.mkdirs(destPath);
        if (!_storageLayer.exists(srcPath)) {
            throw new InternalErrorException("volume:" + srcPath + " is not exits");
        }
        destPath = destPath + File.separator + volumeName;
        String result = Script.runSimpleBashScript("qemu-img convert " + srcPath + " " + destPath, timeout);
        if (result != null) {
            return false;
        } else {
        	result = Script.runSimpleBashScript("chmod 777 " + destPath);
        	return true;
        }
        
    }
    
    public LibvirtStoragePoolDef getStoragePoolDef(Connect conn, StoragePool pool) throws LibvirtException {
        String poolDefXML = pool.getXMLDesc(0);
        LibvirtStoragePoolXMLParser parser = new LibvirtStoragePoolXMLParser();
        return parser.parseStoragePoolXML(poolDefXML);
    }
    
    public StorageVol getVolumeFromURI(Connect conn, String volPath) throws LibvirtException, URISyntaxException {
        int index = volPath.lastIndexOf("/");
        URI volDir = null;
        StoragePool sp = null;
        StorageVol vol = null;
        try {
            volDir = new URI(volPath.substring(0, index));
            String volName = volPath.substring(index + 1);
            sp = getStoragePoolbyURI(conn, volDir);
            vol = sp.storageVolLookupByName(volName);
            return vol;
        } catch (LibvirtException e) {
            s_logger.debug("Faild to get vol path: " + e.toString());
            throw e;
        } finally {
            try {
                if (sp != null) {
                    sp.free();
                }
            } catch (LibvirtException e) {

            }
        }
    }
    
    public com.cloud.agent.api.StoragePoolInfo initializeLocalStorage(Connect conn, String localStoragePath, String hostIp, String uuid) {
        if (!(_storageLayer.exists(localStoragePath) && _storageLayer.isDirectory(localStoragePath))) {
            return null;
        }
        
        File path = new File(localStoragePath);
        if (!(path.canWrite() && path.canRead() && path.canExecute())) {
            return null;
        }
        StoragePool pool = null;
        try {
            pool = conn.storagePoolLookupByUUIDString(uuid);
        } catch (LibvirtException e) {
            
        }
        
        if (pool == null) {
            LibvirtStoragePoolDef spd = new LibvirtStoragePoolDef(poolType.DIR, uuid, uuid,
                    null, null, localStoragePath);
            try {
                pool = conn.storagePoolDefineXML(spd.toString(), 0);
                pool.create(0);
            } catch (LibvirtException e) {
                if (pool != null) {
                    try {
                        pool.destroy();
                        pool.undefine();
                    } catch (LibvirtException e1) {
                    }
                    pool = null;
                }
            }
        }
        
        if (pool == null) {
            return null;
        }
        
        try {
            StoragePoolInfo spi = pool.getInfo();
            if (spi.state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
                pool.create(0);
            }
            
            spi = pool.getInfo();
            if (spi.state != StoragePoolState.VIR_STORAGE_POOL_RUNNING) {
                return null;
            }
            com.cloud.agent.api.StoragePoolInfo pInfo = new com.cloud.agent.api.StoragePoolInfo(uuid, hostIp, localStoragePath, localStoragePath, StoragePoolType.Filesystem, spi.capacity, spi.available);

            return pInfo;
        } catch (LibvirtException e) {

        }

        return null;
    }
    
}
