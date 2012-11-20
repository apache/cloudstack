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
package com.cloud.agent.api;

import com.cloud.agent.api.*;
import static org.junit.Assert.*;
import org.junit.Test;

public class SnapshotCommandTest {
	SnapshotCommand ssc = new SnapshotCommand("primaryStoragePoolNameLabel",
                                           "http://secondary.Storage.Url",
                                           "420fa39c-4ef1-a83c-fd93-46dc1ff515ae",
                                           "snapshotName",
                                           101L,
                                           102L,
                                           103L);
	
	SnapshotCommand ssc1 = new SnapshotCommand();

	@Test
	public void testGetPrimaryStoragePoolNameLabel() {
		String label = ssc.getPrimaryStoragePoolNameLabel();
		assertTrue(label.equals("primaryStoragePoolNameLabel"));
	}

	@Test
	public void testGetSecondaryStorageUrl() {
		String url = ssc.getSecondaryStorageUrl();
		assertTrue(url.equals("http://secondary.Storage.Url"));
	}

	@Test
	public void testGetSnapshotUuid() {
		String uuid = ssc.getSnapshotUuid();
	    assertTrue(uuid.equals("420fa39c-4ef1-a83c-fd93-46dc1ff515ae"));
	}

	@Test
	public void testGetSnapshotName() {
		String name = ssc.getSnapshotName();
	    assertTrue(name.equals("snapshotName"));
	}

	@Test
	public void testGetVolumePath() {
		ssc.setVolumePath("vPath");
		String path = ssc.getVolumePath();
	    assertTrue(path.equals("vPath"));
	    
		ssc1.setVolumePath("vPath1");
		path = ssc1.getVolumePath();
	    assertTrue(path.equals("vPath1"));
	}

	@Test
	public void testExecuteInSequence() {
		boolean b = ssc.executeInSequence();
		assertFalse(b);

		b = ssc1.executeInSequence();
		assertFalse(b);
	}

	@Test
	public void testGetDataCenterId() {
		Long dcId = ssc.getDataCenterId();
		Long expected = 101L;
		assertEquals(expected, dcId);
	}

	@Test
	public void testGetAccountId() {
		Long aId = ssc.getAccountId();
		Long expected = 102L;
		assertEquals(expected, aId);
	}

	@Test
	public void testGetVolumeId() {
		Long vId = ssc.getVolumeId();
		Long expected = 103L;
		assertEquals(expected, vId);
	}
}
