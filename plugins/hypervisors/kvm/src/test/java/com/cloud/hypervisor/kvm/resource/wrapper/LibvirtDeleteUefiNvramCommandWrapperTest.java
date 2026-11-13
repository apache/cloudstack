//
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
//

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.Error;
import org.libvirt.LibvirtException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.DeleteUefiNvramCommand;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtDeleteUefiNvramCommandWrapperTest {

    @Spy
    private LibvirtDeleteUefiNvramCommandWrapper wrapper = new LibvirtDeleteUefiNvramCommandWrapper();

    @Mock
    private LibvirtComputingResource resource;
    @Mock
    private LibvirtUtilitiesHelper libvirtUtilitiesHelper;
    @Mock
    private Connect conn;

    @Rule
    public TemporaryFolder nvramFolder = new TemporaryFolder();

    private final String vmUuid = UUID.randomUUID().toString();

    private File createNvramFile(String uuid) throws IOException {
        File nvramFile = nvramFolder.newFile(uuid + ".fd");
        Mockito.lenient().when(resource.getUefiNvramPath(uuid)).thenReturn(nvramFile.getAbsolutePath());
        return nvramFile;
    }

    private LibvirtException libvirtException(Error.ErrorNumber errorNumber) {
        Error error = Mockito.mock(Error.class);
        Mockito.when(error.getCode()).thenReturn(errorNumber);
        LibvirtException exception = Mockito.mock(LibvirtException.class);
        Mockito.when(exception.getError()).thenReturn(error);
        return exception;
    }

    @Test
    public void testExecuteDeletesOnlyTheNvramFileOfTheExpungedVm() throws Exception {
        File nvramFile = createNvramFile(vmUuid);
        File otherNvramFile = nvramFolder.newFile(UUID.randomUUID() + ".fd");
        Mockito.doReturn(false).when(wrapper).isDomainDefined(resource, vmUuid);

        Answer answer = wrapper.execute(new DeleteUefiNvramCommand("i-2-3-VM", vmUuid), resource);

        Assert.assertTrue(answer.getResult());
        Assert.assertFalse(nvramFile.exists());
        Assert.assertTrue(otherNvramFile.exists());
    }

    @Test
    public void testExecuteSucceedsWhenNvramFileDoesNotExist() throws Exception {
        String missingPath = new File(nvramFolder.getRoot(), vmUuid + ".fd").getAbsolutePath();
        Mockito.when(resource.getUefiNvramPath(vmUuid)).thenReturn(missingPath);
        Mockito.doReturn(false).when(wrapper).isDomainDefined(resource, vmUuid);

        Answer answer = wrapper.execute(new DeleteUefiNvramCommand("i-2-3-VM", vmUuid), resource);

        Assert.assertTrue(answer.getResult());
    }

    @Test
    public void testExecuteKeepsNvramFileWhenDomainIsStillDefined() throws Exception {
        File nvramFile = createNvramFile(vmUuid);
        Mockito.doReturn(true).when(wrapper).isDomainDefined(resource, vmUuid);

        Answer answer = wrapper.execute(new DeleteUefiNvramCommand("i-2-3-VM", vmUuid), resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(nvramFile.exists());
    }

    @Test
    public void testExecuteKeepsNvramFileWhenDomainCheckFails() throws Exception {
        File nvramFile = createNvramFile(vmUuid);
        Mockito.doThrow(LibvirtException.class).when(wrapper).isDomainDefined(resource, vmUuid);

        Answer answer = wrapper.execute(new DeleteUefiNvramCommand("i-2-3-VM", vmUuid), resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertTrue(nvramFile.exists());
    }

    @Test
    public void testExecuteDoesNothingWhenNoNvramPathIsConfigured() throws Exception {
        Mockito.when(resource.getUefiNvramPath(vmUuid)).thenReturn(null);

        Answer answer = wrapper.execute(new DeleteUefiNvramCommand("i-2-3-VM", vmUuid), resource);

        Assert.assertTrue(answer.getResult());
        Mockito.verify(wrapper, Mockito.never()).isDomainDefined(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testExecuteRejectsAnInvalidUuid() throws Exception {
        Answer answer = wrapper.execute(new DeleteUefiNvramCommand("i-2-3-VM", "../" + vmUuid), resource);

        Assert.assertFalse(answer.getResult());
        Mockito.verify(resource, Mockito.never()).getUefiNvramPath(Mockito.anyString());
        Mockito.verify(wrapper, Mockito.never()).isDomainDefined(Mockito.any(), Mockito.anyString());
    }

    @Test
    public void testIsValidUuid() {
        Assert.assertTrue(wrapper.isValidUuid(vmUuid));
        Assert.assertFalse(wrapper.isValidUuid(null));
        Assert.assertFalse(wrapper.isValidUuid(""));
        Assert.assertFalse(wrapper.isValidUuid("not-a-uuid"));
        Assert.assertFalse(wrapper.isValidUuid(vmUuid + "/../x"));
    }

    @Test
    public void testIsDomainDefinedReturnsTrueAndFreesTheDomain() throws Exception {
        Domain domain = Mockito.mock(Domain.class);
        Mockito.when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        Mockito.when(libvirtUtilitiesHelper.getConnection()).thenReturn(conn);
        Mockito.when(conn.domainLookupByUUIDString(vmUuid)).thenReturn(domain);

        Assert.assertTrue(wrapper.isDomainDefined(resource, vmUuid));
        Mockito.verify(domain).free();
    }

    @Test
    public void testIsDomainDefinedReturnsFalseWhenTheDomainDoesNotExist() throws Exception {
        Mockito.when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        Mockito.when(libvirtUtilitiesHelper.getConnection()).thenReturn(conn);
        LibvirtException noDomain = libvirtException(Error.ErrorNumber.VIR_ERR_NO_DOMAIN);
        Mockito.when(conn.domainLookupByUUIDString(vmUuid)).thenThrow(noDomain);

        Assert.assertFalse(wrapper.isDomainDefined(resource, vmUuid));
    }

    @Test(expected = LibvirtException.class)
    public void testIsDomainDefinedRethrowsLibvirtErrorsOtherThanMissingDomain() throws Exception {
        Mockito.when(resource.getLibvirtUtilitiesHelper()).thenReturn(libvirtUtilitiesHelper);
        Mockito.when(libvirtUtilitiesHelper.getConnection()).thenReturn(conn);
        Mockito.when(conn.domainLookupByUUIDString(vmUuid)).thenThrow(LibvirtException.class);

        wrapper.isDomainDefined(resource, vmUuid);
    }
}
