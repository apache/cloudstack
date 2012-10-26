/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.test;

import static org.junit.Assert.*;

import java.awt.List;
import java.util.LinkedList;

import javax.inject.Inject;

import org.apache.cloudstack.storage.volume.VolumeMotionService;
import org.apache.cloudstack.storage.volume.VolumeService;
import org.apache.cloudstack.storage.volume.db.VolumeDao;
import org.apache.cloudstack.storage.volume.disktype.VolumeDiskTypeHelper;
import org.apache.cloudstack.storage.volume.type.VolumeTypeHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.mockito.Mockito.*;


import com.cloud.utils.db.DB;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="storageContext.xml")
public class volumeServiceTest {
	@Autowired
	protected VolumeService volService;
	@Inject
	protected VolumeDao volumeDao;
	@Autowired
	protected VolumeMotionService vmotion;
	@Autowired
	protected VolumeTypeHelper volTypeHelper;
	@Inject
	protected VolumeDiskTypeHelper volDiskTypeHelper;
	@Before
	public void setUp() {
		Mockito.when(vmotion.copyVolume(null, null)).thenReturn(false);
	}
	
	@DB
	public void test() {
		assertTrue(volService.deleteVolume(1) != false);
		assertNotNull(volumeDao);
		//VolumeVO vol = new VolumeVO(Volume.Type.DATADISK, "test", 1, 2, 2, 1, 1);
		//volumeDao.persist(vol);
		/*
		VolumeVO volume = new VolumeVO();
		String name = "test";
		long size = 100;
		volume.setName(name);
		volume.setSize(size);
		volumeDao.persist(volume);
		VolumeVO newVol = volumeDao.getVolumeByName(name);
		assertTrue(newVol.getSize() == volume.getSize());
		*/

		fail("Not yet implemented");
	}
	
	@Test
	public void test1() {
		System.out.println(volTypeHelper.getType("Root"));
		System.out.println(volDiskTypeHelper.getDiskType("vmdk"));
	}
}
