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
package org.apache.cloudstack.engine.subsystem.api.storage.type;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

public class VolumeTypeHelperTest {

    private VolumeTypeHelper helper;

    @Before
    public void setu() {
        helper = new VolumeTypeHelper();

        List<VolumeType> types = new ArrayList<VolumeType>();
        types.add(new BaseImage());
        types.add(new DataDisk());
        types.add(new Iso());
        types.add(new Unknown());
        types.add(new RootDisk());
        types.add(new VolumeTypeBase());

        helper.setTypes(types);
    }

    @Test
    public void testGetTypeBaseImage() throws Exception {
        VolumeType type = helper.getType("BaseImage");

        Assert.assertTrue(type instanceof BaseImage);
    }

    @Test
    public void testGetTypeDataDisk() throws Exception {
        VolumeType type = helper.getType("DataDisk");

        Assert.assertTrue(type instanceof DataDisk);
    }

    @Test
    public void testGetTypeIso() throws Exception {
        VolumeType type = helper.getType("Iso");

        Assert.assertTrue(type instanceof Iso);
    }

    @Test
    public void testGetTypeUnknown() throws Exception {
        VolumeType type = helper.getType("Unknown");

        Assert.assertTrue(type instanceof Unknown);
    }

    @Test
    public void testGetTypeRootDisk() throws Exception {
        VolumeType type = helper.getType("RootDisk");

        Assert.assertTrue(type instanceof RootDisk);
    }

    @Test
    public void testGetTypeVolumeTypeBase() throws Exception {
        VolumeType type = helper.getType("VolumeTypeBase");

        Assert.assertTrue(type instanceof VolumeTypeBase);
    }

    @Test
    public void testGetTypeVolumeString() throws Exception {
        VolumeType type = helper.getType("String");

        Assert.assertTrue(type instanceof Unknown);
    }

    @Test(expected = NullPointerException.class)
    public void testGetTypeVolumeNull() throws Exception {
        helper.getType(null);
    }
}
