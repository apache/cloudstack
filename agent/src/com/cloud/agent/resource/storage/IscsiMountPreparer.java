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
package com.cloud.agent.resource.storage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.resource.DiskPreparer;
import com.cloud.storage.Volume;
import com.cloud.storage.VolumeVO;
import com.cloud.storage.VirtualMachineTemplate.BootloaderType;
import com.cloud.storage.Volume.VolumeType;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.script.Script;

public class IscsiMountPreparer implements DiskPreparer {
    private static final Logger s_logger = Logger.getLogger(IscsiMountPreparer.class);
    
    private String _name;
    
    private String _mountvmPath;
    private String _mountRootdiskPath;
    private String _mountDatadiskPath;
    protected String _mountParent;
    protected int _mountTimeout;

    @Override
    public String mount(String vmName, VolumeVO vol, BootloaderType type) {
        return null;
    }

    @Override
    public boolean unmount(String path) {
        return false;
    }
    
    protected static VolumeVO findVolume(final List<VolumeVO> volumes, final Volume.VolumeType vType) {
        if (volumes == null) return null;

        for (final VolumeVO v: volumes) {
            if (v.getVolumeType() == vType)
                return v;
        }

        return null;
    }
    
    protected static List<VolumeVO>  findVolumes(final List<VolumeVO> volumes, final Volume.VolumeType vType) {
        if (volumes == null) return null;
        final List<VolumeVO> result = new ArrayList<VolumeVO>();
        for (final VolumeVO v: volumes) {
            if (v.getVolumeType() == vType)
                result.add(v);
        }

        return result;
    }
    
    protected static VolumeVO  findVolume(final List<VolumeVO> volumes, final Volume.VolumeType vType, final String storageHost) {
        if (volumes == null) return null;
        for (final VolumeVO v: volumes) {
            if ((v.getVolumeType() == vType) && (v.getHostIp().equalsIgnoreCase(storageHost)))
                return v;
        }
        return null;
    }
    
    protected static boolean mirroredVolumes(final List<VolumeVO> vols, final Volume.VolumeType vType) {
        final List<VolumeVO> volumes = findVolumes(vols, vType);
        return volumes.size() > 1;
    }

    public synchronized String mountImage(final String host, final String dest, final String vmName, final List<VolumeVO> volumes, final BootloaderType bootloader) {
        final Script command = new Script(_mountvmPath, _mountTimeout, s_logger);
        command.add("-h", host);
        command.add("-l", dest);
        command.add("-n", vmName);
        command.add("-b", bootloader.toString());

        command.add("-t");

        
        final VolumeVO root = findVolume(volumes, Volume.VolumeType.ROOT);
        if (root == null) {
            return null;
        }
        command.add(root.getIscsiName());
        command.add("-r", root.getFolder());
        
        final VolumeVO swap = findVolume(volumes, Volume.VolumeType.SWAP);
        if (swap !=null && swap.getIscsiName() != null) {
            command.add("-w", swap.getIscsiName());
        }
        
        final VolumeVO datadsk = findVolume(volumes, Volume.VolumeType.DATADISK);
        if (datadsk !=null && datadsk.getIscsiName() != null) {
            command.add("-1", datadsk.getIscsiName());
        }
        
        return command.execute();
    }
    
    public synchronized String mountImage(final String storageHosts[], final String dest, final String vmName, final List<VolumeVO> volumes, final boolean mirroredVols, final BootloaderType booter) {
        if (!mirroredVols) {
            return mountImage(storageHosts[0], dest, vmName, volumes, booter);
        } else {
            return mountMirroredImage(storageHosts, dest, vmName, volumes, booter);
        }
    }
    
    protected String mountMirroredImage(final String hosts[], final String dest, final String vmName, final List<VolumeVO> volumes, final BootloaderType booter) {
        final List<VolumeVO> rootDisks = findVolumes(volumes, VolumeType.ROOT);
        final String storIp0 = hosts[0];
        final String storIp1 = hosts[1];
        //mountrootdisk.sh -m -h $STORAGE0 -t $iqn0 -l $src -n $vmname -r $dest -M -H $STORAGE1 -T $iqn1
        final Script command = new Script(_mountRootdiskPath, _mountTimeout, s_logger);
        command.add("-m");
        command.add("-M");
        command.add("-h", storIp0);
        command.add("-H", storIp1);
        command.add("-l", dest);
        command.add("-r", rootDisks.get(0).getFolder());
        command.add("-n", vmName);
        command.add("-t", rootDisks.get(0).getIscsiName());
        command.add("-T", rootDisks.get(1).getIscsiName());
        command.add("-b", booter.toString());

        final List<VolumeVO> swapDisks = findVolumes(volumes, VolumeType.SWAP);
        if (swapDisks.size() == 2) {
            command.add("-w", swapDisks.get(0).getIscsiName());
            command.add("-W", swapDisks.get(1).getIscsiName());
        }

        final String result = command.execute();
        if (result == null){
            final List<VolumeVO> dataDisks = findVolumes(volumes, VolumeType.DATADISK);
            if (dataDisks.size() == 2) {
                final Script mountdata = new Script(_mountDatadiskPath, _mountTimeout, s_logger);
                mountdata.add("-m");
                mountdata.add("-M");
                mountdata.add("-h", storIp0);
                mountdata.add("-H", storIp1);
                mountdata.add("-n", vmName);
                mountdata.add("-c", "1");
                mountdata.add("-d", dataDisks.get(0).getIscsiName());
                mountdata.add("-D", dataDisks.get(1).getIscsiName());
                return mountdata.execute();

            } else if (dataDisks.size() == 0){
                return result;
            }
        }
        
        return result;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        _name = name;
        
        String scriptsDir = (String)params.get("mount.scripts.dir");
        if (scriptsDir == null) {
            scriptsDir = "scripts/vm/storage/iscsi/comstar/filebacked";
        }
        
        _mountDatadiskPath = Script.findScript(scriptsDir, "mountdatadisk.sh");
        if (_mountDatadiskPath == null) {
            throw new ConfigurationException("Unable to find mountdatadisk.sh");
        }
        s_logger.info("mountdatadisk.sh found in " + _mountDatadiskPath);
        
        String value = (String)params.get("mount.script.timeout");
        _mountTimeout = NumbersUtil.parseInt(value, 240) * 1000;
        
        _mountvmPath = Script.findScript(scriptsDir, "mountvm.sh");
        if (_mountvmPath == null) {
            throw new ConfigurationException("Unable to find mountvm.sh");
        }
        s_logger.info("mountvm.sh found in " + _mountvmPath);
        
        _mountRootdiskPath = Script.findScript(scriptsDir, "mountrootdisk.sh");
        if (_mountRootdiskPath == null) {
            throw new ConfigurationException("Unable to find mountrootdisk.sh");
        }
        s_logger.info("mountrootdisk.sh found in " + _mountRootdiskPath);
        
        return true;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
