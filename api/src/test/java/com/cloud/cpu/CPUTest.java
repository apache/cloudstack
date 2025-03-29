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
package com.cloud.cpu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CPUTest {
    @Test
    public void testCPUArchGetType() {
        assertEquals("i686", CPU.CPUArch.x86.getType());
        assertEquals("x86_64", CPU.CPUArch.amd64.getType());
        assertEquals("aarch64", CPU.CPUArch.arm64.getType());
    }

    @Test
    public void testCPUArchGetBits() {
        assertEquals(32, CPU.CPUArch.x86.getBits());
        assertEquals(64, CPU.CPUArch.amd64.getBits());
        assertEquals(64, CPU.CPUArch.arm64.getBits());
    }

    @Test
    public void testCPUArchFromTypeWithValidValues() {
        assertEquals(CPU.CPUArch.x86, CPU.CPUArch.fromType("i686"));
        assertEquals(CPU.CPUArch.amd64, CPU.CPUArch.fromType("x86_64"));
        assertEquals(CPU.CPUArch.arm64, CPU.CPUArch.fromType("aarch64"));
    }

    @Test
    public void testCPUArchFromTypeWithDefaultForBlankOrNull() {
        assertEquals(CPU.CPUArch.amd64, CPU.CPUArch.fromType(""));
        assertEquals(CPU.CPUArch.amd64, CPU.CPUArch.fromType("   "));
        assertEquals(CPU.CPUArch.amd64, CPU.CPUArch.fromType(null));
    }

    @Test
    public void testCPUArchFromTypeWithInvalidValue() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            CPU.CPUArch.fromType("unsupported");
        });
        assertTrue(exception.getMessage().contains("Unsupported arch type: unsupported"));
    }

    @Test
    public void testCPUArchGetTypesAsCSV() {
        String expectedCSV = "i686,x86_64,aarch64";
        assertEquals(expectedCSV, CPU.CPUArch.getTypesAsCSV());
    }
}
