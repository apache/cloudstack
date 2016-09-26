/*
 * Copyright 2015 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.hypervisor.xenserver.resource;

import java.io.File;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import com.cloud.utils.script.Script;

public class CitrixResourceBaseTest {

    protected CitrixResourceBase citrixResourceBase = new CitrixResourceBase() {
        @Override
        protected String getPatchFilePath() {
            return null;
        }
    };

    public void testGetPathFilesExeption() {
        String patch = citrixResourceBase.getPatchFilePath();

        PowerMockito.mockStatic(Script.class);
        Mockito.when(Script.findScript("", patch)).thenReturn(null);

        citrixResourceBase.getPatchFiles();

    }

    public void testGetPathFilesListReturned() {
        String patch = citrixResourceBase.getPatchFilePath();

        PowerMockito.mockStatic(Script.class);
        Mockito.when(Script.findScript("", patch)).thenReturn(patch);

        File expected = new File(patch);
        String pathExpected = expected.getAbsolutePath();

        List<File> files = citrixResourceBase.getPatchFiles();
        String receivedPath = files.get(0).getAbsolutePath();
        Assert.assertEquals(receivedPath, pathExpected);
    }

    @Test
    public void testGetGuestOsTypeNull() {
        String platformEmulator = null;

        String expected = "Other install media";
        String guestOsType = citrixResourceBase.getGuestOsType(platformEmulator);
        Assert.assertEquals(expected, guestOsType);
    }

    @Test
    public void testGetGuestOsTypeEmpty() {
        String platformEmulator = "";

        String expected = "Other install media";
        String guestOsType = citrixResourceBase.getGuestOsType(platformEmulator);
        Assert.assertEquals(expected, guestOsType);
    }

    @Test
    public void testGetGuestOsTypeBlank() {
        String platformEmulator = "    ";

        String expected = "Other install media";
        String guestOsType = citrixResourceBase.getGuestOsType(platformEmulator);
        Assert.assertEquals(expected, guestOsType);
    }

    @Test
    public void testGetGuestOsTypeOther() {
        String platformEmulator = "My Own Linux Distribution Y.M (64-bit)";

        String guestOsType = citrixResourceBase.getGuestOsType(platformEmulator);
        Assert.assertEquals(platformEmulator, guestOsType);
    }
}
