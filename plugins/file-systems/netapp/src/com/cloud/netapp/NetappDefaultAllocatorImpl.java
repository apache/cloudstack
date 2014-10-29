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
package com.cloud.netapp;

import java.rmi.ServerException;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

public class NetappDefaultAllocatorImpl implements NetappAllocator {
    private static HashMap<String, Integer> s_poolNameToLastVolumeIdAllocated = new HashMap<String, Integer>();
    private final NetappManager _netappMgr;
    public static final Logger s_logger = Logger.getLogger(NetappDefaultAllocatorImpl.class.getName());

    public NetappDefaultAllocatorImpl(NetappManager netappMgr) {
        _netappMgr = netappMgr;
    }

    @Override
    public synchronized NetappVolumeVO chooseLeastFullVolumeFromPool(String poolName, long lunSizeGb) {
        List<NetappVolumeVO> volumesOnPoolAscending = _netappMgr.listVolumesAscending(poolName);

        if (volumesOnPoolAscending == null) {
            //no pools exist in db
            return null;
        }

        long maxAvailable = 0;
        NetappVolumeVO selectedVol = null;
        for (NetappVolumeVO vol : volumesOnPoolAscending) {
            try {
                long availableBytes = _netappMgr.returnAvailableVolumeSize(vol.getVolumeName(), vol.getUsername(), vol.getPassword(), vol.getIpAddress());

                if (lunSizeGb <= bytesToGb(availableBytes) && availableBytes > maxAvailable) {
                    maxAvailable = availableBytes; //new max
                    selectedVol = vol; //new least loaded vol
                }
            } catch (ServerException se) {
                s_logger.debug("Ignoring failure to obtain volume size for volume " + vol.getVolumeName());
                continue;
            }
        }

        return selectedVol;
    }

    /**
     * This method does the actual round robin allocation
     * @param poolName
     * @param lunSizeGb
     * @return -- the selected volume to create the lun on
     */
    @Override
    public synchronized NetappVolumeVO chooseVolumeFromPool(String poolName, long lunSizeGb) {
        int pos = 0; //0 by default
        List<NetappVolumeVO> volumesOnPoolAscending = _netappMgr.listVolumesAscending(poolName);

        if (volumesOnPoolAscending == null) {
            //no pools exist in db
            return null;
        }

        //get the index of the record from the map
        if (s_poolNameToLastVolumeIdAllocated.get(poolName) == null) {
            pos = 0;
        } else {
            pos = s_poolNameToLastVolumeIdAllocated.get(poolName);
        }

        //update for RR effect
        s_poolNameToLastVolumeIdAllocated.put(poolName, (pos + 1) % volumesOnPoolAscending.size());

        //now iterate over the records
        Object[] volumesOnPoolAscendingArray = volumesOnPoolAscending.toArray();
        int counter = 0;
        while (counter < volumesOnPoolAscendingArray.length) {
            NetappVolumeVO vol = (NetappVolumeVO)volumesOnPoolAscendingArray[pos];

            //check if the volume fits the bill
            long availableBytes;
            try {
                availableBytes = _netappMgr.returnAvailableVolumeSize(vol.getVolumeName(), vol.getUsername(), vol.getPassword(), vol.getIpAddress());

                if (lunSizeGb <= bytesToGb(availableBytes)) {
                    //found one
                    return vol;
                }
                pos = (pos + 1) % volumesOnPoolAscendingArray.length;
                counter++;
            } catch (ServerException e) {
                s_logger.debug("Ignoring failure to obtain volume size for volume " + vol.getVolumeName());
                continue;
            }
        }

        return null;
    }

    /**
     * This method does the byte to gb conversion
     * @param bytes
     * @return -- converted gb
     */
    private long bytesToGb(long bytes) {
        long returnVal = (bytes / (1024 * 1024 * 1024));
        return returnVal;
    }

}
