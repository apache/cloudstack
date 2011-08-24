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
 * @author-aj
 */

package com.cloud.netapp;

import java.io.IOException;
import java.rmi.ServerException;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import netapp.manage.NaException;

public class NetappDefaultAllocatorImpl implements NetappAllocator
{
	private static HashMap<String, Integer> _poolNameToLastVolumeIdAllocated = new HashMap<String, Integer>();
	private NetappManager _netappMgr;
    public static final Logger s_logger = Logger.getLogger(NetappDefaultAllocatorImpl.class.getName());
	
	public NetappDefaultAllocatorImpl(NetappManager netappMgr) {
		_netappMgr = netappMgr;
	}
	
	public synchronized NetappVolumeVO chooseLeastFullVolumeFromPool(String poolName, long lunSizeGb) 
	{
		List<NetappVolumeVO> volumesOnPoolAscending = _netappMgr.listVolumesAscending(poolName);
		
		if(volumesOnPoolAscending==null)
		{
			//no pools exist in db
			return null;
		}
		
		long maxAvailable = 0;
		NetappVolumeVO selectedVol = null;
		for(NetappVolumeVO vol : volumesOnPoolAscending)
		{
			try {
				long availableBytes = _netappMgr.returnAvailableVolumeSize(vol.getVolumeName(), vol.getUsername(), vol.getPassword(), vol.getIpAddress());

				if(lunSizeGb <= bytesToGb(availableBytes) && availableBytes>maxAvailable)
				{
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
	 * @throws IOException
	 * @throws NaException
	 */
	public synchronized NetappVolumeVO chooseVolumeFromPool(String poolName, long lunSizeGb)
	{
		int pos = 0; //0 by default
		List<NetappVolumeVO> volumesOnPoolAscending = _netappMgr.listVolumesAscending(poolName);
		
		if(volumesOnPoolAscending==null)
		{
			//no pools exist in db
			return null;
		}
		
		//get the index of the record from the map
		if(_poolNameToLastVolumeIdAllocated.get(poolName)==null)
		{
			pos=0;
		}
		else
		{
			pos=_poolNameToLastVolumeIdAllocated.get(poolName);
		}
		
		//update for RR effect
		_poolNameToLastVolumeIdAllocated.put(poolName, (pos+1)%volumesOnPoolAscending.size());
		
		//now iterate over the records
		Object[] volumesOnPoolAscendingArray = volumesOnPoolAscending.toArray();
		int counter=0;
		while (counter < volumesOnPoolAscendingArray.length)
		{
			NetappVolumeVO vol = (NetappVolumeVO)volumesOnPoolAscendingArray[pos];
			
			//check if the volume fits the bill
			long availableBytes;
			try {
				availableBytes = _netappMgr.returnAvailableVolumeSize(vol.getVolumeName(), vol.getUsername(), vol.getPassword(), vol.getIpAddress());
									
				if(lunSizeGb <= bytesToGb(availableBytes))
				{
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
	private long bytesToGb(long bytes){
		long returnVal = (bytes/(1024*1024*1024));
		return returnVal;
	}

}

	 