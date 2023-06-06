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

package com.cloud.hypervisor.vmware.mo;

import com.cloud.hypervisor.vmware.util.VmwareClient;
import com.cloud.hypervisor.vmware.util.VmwareContext;
import com.vmware.vim25.FileInfo;
import com.vmware.vim25.HostDatastoreBrowserSearchResults;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.VimPortType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * Created by sudharma_jain on 6/13/17.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(DatastoreMO.class)
public class DatastoreMOTest {
    @Mock
    VmwareContext _context ;
    @Mock
    VmwareClient _client;
    @Mock
    ManagedObjectReference _mor;
    @Mock
    HostDatastoreBrowserMO browserMo;
    @Mock
    VimPortType vimPortType;

    DatastoreMO datastoreMO ;
    String fileName = "ROOT-5.vmdk";


    @Before
    public void setUp() throws Exception {

        datastoreMO = new DatastoreMO(_context, _mor);
        PowerMockito.whenNew(HostDatastoreBrowserMO.class).withAnyArguments().thenReturn(browserMo);
        when(_context.getVimClient()).thenReturn(_client);
        when(_client.getDynamicProperty(any(ManagedObjectReference.class), eq("name"))).thenReturn("252d36c96cfb32f48ce7756ccb79ae37");

        ArrayList<HostDatastoreBrowserSearchResults> results = new ArrayList<>();

        HostDatastoreBrowserSearchResults r1 =  new HostDatastoreBrowserSearchResults();
        FileInfo f1 = new FileInfo();
        f1.setPath(fileName);
        r1.getFile().add(f1);
        r1.setFolderPath("[252d36c96cfb32f48ce7756ccb79ae37] .snapshot/hourly.2017-02-23_1705/i-2-5-VM/");

        HostDatastoreBrowserSearchResults r2 =  new HostDatastoreBrowserSearchResults();
        FileInfo f2 = new FileInfo();
        f2.setPath(fileName);
        r2.getFile().add(f2);
        r2.setFolderPath("[252d36c96cfb32f48ce7756ccb79ae37] .snapshot/hourly.2017-02-23_1605/i-2-5-VM/");

        HostDatastoreBrowserSearchResults r3 =  new HostDatastoreBrowserSearchResults();
        FileInfo f3 = new FileInfo();
        f3.setPath(fileName);
        r3.getFile().add(f3);
        r3.setFolderPath("[252d36c96cfb32f48ce7756ccb79ae37] i-2-5-VM/");

        results.add(r1);
        results.add(r2);
        results.add(r3);

        when(browserMo.searchDatastore(any(String.class), any(String.class), eq(true))).thenReturn(null);
        when(browserMo.searchDatastoreSubFolders(any(String.class),any(String.class), any(Boolean.class) )).thenReturn(results);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testSearchFileInSubFolders() throws Exception {
        assertEquals("Unexpected Behavior: search should exclude .snapshot folder", "[252d36c96cfb32f48ce7756ccb79ae37] i-2-5-VM/ROOT-5.vmdk", datastoreMO.searchFileInSubFolders(fileName, false, ".snapshot") );
    }

    @Test
    public void testSearchFileInSubFoldersWithExcludeMultipleFolders() throws Exception {
        assertEquals("Unexpected Behavior: search should exclude folders", datastoreMO.searchFileInSubFolders(fileName, false, "i-2-5-VM, .snapshot/hourly.2017-02-23_1705"), "[252d36c96cfb32f48ce7756ccb79ae37] .snapshot/hourly.2017-02-23_1605/i-2-5-VM/ROOT-5.vmdk" );
    }

}
