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

package org.apache.cloudstack.veeam.api.dto;

import static org.junit.Assert.assertEquals;

import org.apache.cloudstack.api.ApiConstants;
import org.junit.Test;

import com.cloud.utils.Pair;

public class VmTest {

    @Test
    public void of_SetsHrefAndId() {
        Vm vm = Vm.of("/ovirt-engine/api/vms/1", "1");

        assertEquals("/ovirt-engine/api/vms/1", vm.getHref());
        assertEquals("1", vm.getId());
    }

    @Test
    public void biosGetDefault_UsesSeaBiosAndDisabledBootMenu() {
        Vm.Bios bios = Vm.Bios.getDefault();

        assertEquals(Vm.Bios.Type.q35_sea_bios.name(), bios.getType());
        assertEquals("false", bios.getBootMenu().getEnabled());
    }

    @Test
    public void biosUpdateBios_UsesSecureBootWhenRequested() {
        Vm.Bios bios = Vm.Bios.getDefault();

        Vm.Bios.updateBios(bios, ApiConstants.BootMode.SECURE.toString());

        assertEquals(Vm.Bios.Type.q35_secure_boot.name(), bios.getType());
    }

    @Test
    public void biosUpdateBios_UsesOvmfForNonSecureMode() {
        Vm.Bios bios = Vm.Bios.getDefault();

        Vm.Bios.updateBios(bios, ApiConstants.BootMode.LEGACY.toString());

        assertEquals(Vm.Bios.Type.q35_ovmf.name(), bios.getType());
    }

    @Test
    public void biosGetBiosFromOrdinal_FallsBackToDefaultWhenInvalid() {
        Vm.Bios bios = Vm.Bios.getBiosFromOrdinal("not-a-number");

        assertEquals(Vm.Bios.Type.q35_sea_bios.name(), bios.getType());
    }

    @Test
    public void biosGetBiosFromOrdinal_MapsKnownOrdinals() {
        Vm.Bios secure = Vm.Bios.getBiosFromOrdinal(String.valueOf(Vm.Bios.Type.q35_secure_boot.ordinal()));
        Vm.Bios ovmf = Vm.Bios.getBiosFromOrdinal(String.valueOf(Vm.Bios.Type.q35_ovmf.ordinal()));

        assertEquals(Vm.Bios.Type.q35_secure_boot.name(), secure.getType());
        assertEquals(Vm.Bios.Type.q35_ovmf.name(), ovmf.getType());
    }

    @Test
    public void biosRetrieveBootOptions_ReturnsExpectedMappings() {
        Pair<ApiConstants.BootType, ApiConstants.BootMode> defaults = Vm.Bios.retrieveBootOptions(null);
        Pair<ApiConstants.BootType, ApiConstants.BootMode> secure = Vm.Bios.retrieveBootOptions(typeOnly(Vm.Bios.Type.q35_secure_boot.name()));
        Pair<ApiConstants.BootType, ApiConstants.BootMode> uefiLegacy = Vm.Bios.retrieveBootOptions(typeOnly(Vm.Bios.Type.q35_ovmf.name()));

        assertEquals(ApiConstants.BootType.BIOS, defaults.first());
        assertEquals(ApiConstants.BootMode.LEGACY, defaults.second());
        assertEquals(ApiConstants.BootType.UEFI, secure.first());
        assertEquals(ApiConstants.BootMode.SECURE, secure.second());
        assertEquals(ApiConstants.BootType.UEFI, uefiLegacy.first());
        assertEquals(ApiConstants.BootMode.LEGACY, uefiLegacy.second());
    }

    private Vm.Bios typeOnly(String type) {
        Vm.Bios bios = new Vm.Bios();
        bios.setType(type);
        return bios;
    }
}
