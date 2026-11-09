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

package com.cloud.hypervisor.kvm.resource.wrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cloudstack.backup.DeleteVmCheckpointCommand;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libvirt.Connect;
import org.libvirt.Domain;
import org.libvirt.LibvirtException;
import org.mockito.MockedConstruction;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.agent.api.Answer;
import com.cloud.hypervisor.kvm.resource.LibvirtComputingResource;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;

@RunWith(MockitoJUnitRunner.class)
public class LibvirtDeleteVmCheckpointCommandWrapperTest {

    // vda is an overlay on /p/base.qcow2 (a VM snapshot): cp-5 in the top layer, cp-4 only in the base; vdb has no
    // bitmaps. Each disk has a format node and a storage node with the same file, as libvirt builds them.
    private static final String NAMED_NODES = "{\"return\": ["
            + "{\"node-name\": \"libvirt-2-format\", \"file\": \"/p/vda.qcow2\", \"drv\": \"qcow2\", \"ro\": false,"
            + "  \"image\": {\"filename\": \"/p/vda.qcow2\", \"backing-image\": {\"filename\": \"/p/base.qcow2\"}},"
            + "  \"dirty-bitmaps\": [{\"name\": \"cp-5\"}]},"
            + "{\"node-name\": \"libvirt-2-storage\", \"file\": \"/p/vda.qcow2\", \"drv\": \"file\", \"ro\": false, \"image\": {\"filename\": \"/p/vda.qcow2\"}},"
            + "{\"node-name\": \"libvirt-3-format\", \"file\": \"/p/base.qcow2\", \"drv\": \"qcow2\", \"ro\": true,"
            + "  \"image\": {\"filename\": \"/p/base.qcow2\"}, \"dirty-bitmaps\": [{\"name\": \"cp-4\"}, {\"name\": \"cp-5\"}]},"
            + "{\"node-name\": \"libvirt-3-storage\", \"file\": \"/p/base.qcow2\", \"drv\": \"file\", \"ro\": false, \"image\": {\"filename\": \"/p/base.qcow2\"}},"
            + "{\"node-name\": \"libvirt-1-format\", \"file\": \"/p/vdb.qcow2\", \"drv\": \"qcow2\", \"ro\": false,"
            + "  \"image\": {\"filename\": \"/p/vdb.qcow2\"}, \"dirty-bitmaps\": []},"
            + "{\"node-name\": \"libvirt-1-storage\", \"file\": \"/p/vdb.qcow2\", \"drv\": \"file\", \"ro\": false, \"image\": {\"filename\": \"/p/vdb.qcow2\"}}"
            + "]}";
    private static final Map<String, String> DISK_LABELS = new LinkedHashMap<>(Map.of("/p/vda.qcow2", "vda"));
    static {
        DISK_LABELS.put("/p/vdb.qcow2", "vdb");
    }

    // qemu-img info --backing-chain of a stopped volume whose overlay has no bitmap and whose base has cp-2.
    private static final String CHAIN_INFO = "["
            + "{\"filename\": \"/p/vol.qcow2\", \"format\": \"qcow2\", \"backing-filename\": \"/p/old.qcow2\","
            + "  \"format-specific\": {\"type\": \"qcow2\", \"data\": {\"compat\": \"1.1\"}}},"
            + "{\"filename\": \"/p/old.qcow2\", \"format\": \"qcow2\", \"backing-filename\": \"/p/template.qcow2\","
            + "  \"format-specific\": {\"type\": \"qcow2\", \"data\": {\"bitmaps\": [{\"name\": \"cp-1\"}, {\"name\": \"cp-2\"}]}}},"
            + "{\"filename\": \"/p/template.qcow2\", \"format\": \"qcow2\", \"format-specific\": {\"type\": \"qcow2\", \"data\": {}}}"
            + "]";
    private static final String JSON_OUTPUT = "JSON:";

    private LibvirtDeleteVmCheckpointCommandWrapper wrapper;
    private DeleteVmCheckpointCommand command;
    private LibvirtComputingResource resource;
    private Domain domain;

    @Before
    public void setUp() throws Exception {
        wrapper = new LibvirtDeleteVmCheckpointCommandWrapper();
        command = Mockito.mock(DeleteVmCheckpointCommand.class);
        resource = Mockito.mock(LibvirtComputingResource.class);
        domain = Mockito.mock(Domain.class);
        LibvirtUtilitiesHelper helper = Mockito.mock(LibvirtUtilitiesHelper.class);
        Connect conn = Mockito.mock(Connect.class);
        Mockito.lenient().when(resource.getLibvirtUtilitiesHelper()).thenReturn(helper);
        Mockito.lenient().when(helper.getConnection()).thenReturn(conn);
        Mockito.lenient().when(resource.getDomain(conn, "i-2-3-VM")).thenReturn(domain);
        Mockito.lenient().when(resource.getDiskPathLabelMap("i-2-3-VM")).thenReturn(DISK_LABELS);
    }

    private void runningVmCommand(String checkpointId) {
        Mockito.when(command.isStoppedVM()).thenReturn(false);
        Mockito.when(command.getVmName()).thenReturn("i-2-3-VM");
        Mockito.when(command.getCheckpointId()).thenReturn(checkpointId);
    }

    private void stoppedVmCommand(String checkpointId, String diskPath) {
        Mockito.when(command.isStoppedVM()).thenReturn(true);
        Mockito.when(command.getCheckpointId()).thenReturn(checkpointId);
        Mockito.when(command.getDiskPathUuidMap()).thenReturn(Map.of(diskPath, "vol-1"));
    }

    private void qmpReturns(String namedBlockNodes) throws LibvirtException {
        Mockito.when(domain.qemuMonitorCommand(Mockito.contains("query-named-block-nodes"), Mockito.eq(0))).thenReturn(namedBlockNodes);
    }

    // Each Script gets the next result; its arguments, plus the content of an --xmlfile (deleted afterwards), are
    // recorded in order. A result starting with JSON_OUTPUT feeds the parser of execute(parser) and counts as success.
    private static MockedConstruction<Script> scripts(List<List<String>> recordedArgs, String... results) {
        AtomicInteger n = new AtomicInteger();
        return Mockito.mockConstruction(Script.class, (mock, context) -> {
            int k = n.getAndIncrement();
            String result = k < results.length ? results[k] : null;
            org.mockito.stubbing.Answer<String> run = invocation -> {
                List<String> args = new ArrayList<>();
                Mockito.mockingDetails(mock).getInvocations().stream()
                        .filter(inv -> inv.getMethod().getName().equals("add"))
                        .forEach(inv -> { for (Object a : inv.getArguments()) { args.add(String.valueOf(a)); } });
                int xml = args.indexOf("--xmlfile");
                if (xml >= 0) {
                    args.add(Files.readString(new File(args.get(xml + 1)).toPath()));
                }
                recordedArgs.add(args);
                if (result != null && result.startsWith(JSON_OUTPUT) && invocation.getArguments().length == 1) {
                    ((OutputInterpreter) invocation.getArgument(0)).interpret(new BufferedReader(new StringReader(result.substring(JSON_OUTPUT.length()))));
                    return null;
                }
                return result;
            };
            Mockito.when(mock.execute()).thenAnswer(run);
            Mockito.when(mock.execute(Mockito.any(OutputInterpreter.class))).thenAnswer(run);
        });
    }

    @Test
    public void testFindDisksWithBitmapLooksThroughTheBackingChain() {
        Assert.assertEquals(Map.of("vda", true, "vdb", false), LibvirtDeleteVmCheckpointCommandWrapper.findDisksWithBitmap(NAMED_NODES, DISK_LABELS, "cp-4"));
        Assert.assertEquals(Map.of("vda", true, "vdb", false), LibvirtDeleteVmCheckpointCommandWrapper.findDisksWithBitmap(NAMED_NODES, DISK_LABELS, "cp-5"));
        Assert.assertEquals(Map.of("vda", false, "vdb", false), LibvirtDeleteVmCheckpointCommandWrapper.findDisksWithBitmap(NAMED_NODES, DISK_LABELS, "cp-9"));
        Assert.assertEquals(List.of("vda", "vdb"), new ArrayList<>(LibvirtDeleteVmCheckpointCommandWrapper.findDisksWithBitmap(NAMED_NODES, DISK_LABELS, "cp-4").keySet()));
        Assert.assertThrows(CloudRuntimeException.class, () -> LibvirtDeleteVmCheckpointCommandWrapper.findDisksWithBitmap("{\"return\": {}}", DISK_LABELS, "cp-4"));
        Assert.assertThrows(CloudRuntimeException.class, () -> LibvirtDeleteVmCheckpointCommandWrapper.findDisksWithBitmap("{\"error\": {\"desc\": \"busy\"}}", DISK_LABELS, "cp-4"));
    }

    @Test
    public void testFindDisksWithBitmapDoesNotDependOnTheNodeOrder() {
        // The protocol (file) node of vda listed before its qcow2 node: the chain must still be read from the qcow2 node.
        String storageFirst = NAMED_NODES.replace(
                "{\"node-name\": \"libvirt-2-format\"", "{\"node-name\": \"libvirt-2-storage\", \"file\": \"/p/vda.qcow2\", \"drv\": \"file\", \"ro\": false, \"image\": {\"filename\": \"/p/vda.qcow2\"}},"
                        + "{\"node-name\": \"libvirt-2-format\"");
        Assert.assertEquals(Map.of("vda", true, "vdb", false), LibvirtDeleteVmCheckpointCommandWrapper.findDisksWithBitmap(storageFirst, DISK_LABELS, "cp-4"));
    }

    @Test
    public void testFindDisksWithBitmapOnEncryptedImagesReportedAsJsonPseudoFilenames() {
        // qemu reports a LUKS qcow2 under json:{...} in both "file" and "image.filename", for the disk and its backing file.
        String enc = "{\"return\": ["
                + "{\"node-name\": \"libvirt-2-format\", \"drv\": \"qcow2\", \"ro\": false,"
                + "  \"file\": \"json:{\\\"encrypt.key-secret\\\": \\\"libvirt-2-format-encryption-secret0\\\", \\\"driver\\\": \\\"qcow2\\\", \\\"file\\\": {\\\"driver\\\": \\\"file\\\", \\\"filename\\\": \\\"/p/vda.qcow2\\\"}}\","
                + "  \"image\": {\"filename\": \"json:{\\\"encrypt.key-secret\\\": \\\"libvirt-2-format-encryption-secret0\\\", \\\"driver\\\": \\\"qcow2\\\", \\\"file\\\": {\\\"driver\\\": \\\"file\\\", \\\"filename\\\": \\\"/p/vda.qcow2\\\"}}\","
                + "            \"backing-image\": {\"filename\": \"json:{\\\"encrypt.key-secret\\\": \\\"libvirt-3-format-encryption-secret0\\\", \\\"driver\\\": \\\"qcow2\\\", \\\"file\\\": {\\\"driver\\\": \\\"file\\\", \\\"filename\\\": \\\"/p/base.qcow2\\\"}}\"}},"
                + "  \"dirty-bitmaps\": [{\"name\": \"cp-5\"}]},"
                + "{\"node-name\": \"libvirt-3-format\", \"drv\": \"qcow2\", \"ro\": true,"
                + "  \"file\": \"json:{\\\"encrypt.key-secret\\\": \\\"libvirt-3-format-encryption-secret0\\\", \\\"driver\\\": \\\"qcow2\\\", \\\"file\\\": {\\\"driver\\\": \\\"file\\\", \\\"filename\\\": \\\"/p/base.qcow2\\\"}}\","
                + "  \"image\": {\"filename\": \"/p/base.qcow2\"}, \"dirty-bitmaps\": [{\"name\": \"cp-4\"}]},"
                + "{\"node-name\": \"libvirt-1-format\", \"drv\": \"qcow2\", \"ro\": false, \"file\": \"/p/vdb.qcow2\", \"image\": {\"filename\": \"/p/vdb.qcow2\"}, \"dirty-bitmaps\": []}"
                + "]}";
        Assert.assertEquals(Map.of("vda", true, "vdb", false), LibvirtDeleteVmCheckpointCommandWrapper.findDisksWithBitmap(enc, DISK_LABELS, "cp-5"));
        Assert.assertEquals(Map.of("vda", true, "vdb", false), LibvirtDeleteVmCheckpointCommandWrapper.findDisksWithBitmap(enc, DISK_LABELS, "cp-4"));
        Assert.assertEquals(Map.of("vda", false, "vdb", false), LibvirtDeleteVmCheckpointCommandWrapper.findDisksWithBitmap(enc, DISK_LABELS, "cp-9"));
    }

    @Test
    public void testFindChainLayersWithBitmap() {
        Assert.assertEquals(List.of("/p/old.qcow2"), LibvirtDeleteVmCheckpointCommandWrapper.findChainLayersWithBitmap(CHAIN_INFO, "cp-2"));
        Assert.assertTrue(LibvirtDeleteVmCheckpointCommandWrapper.findChainLayersWithBitmap(CHAIN_INFO, "cp-9").isEmpty());
    }

    @Test
    public void testFindChainLayersWithBitmapSkipsWarningsBeforeTheJson() {
        String withWarning = "qemu-img: warning: Image /p/vol.qcow2 is in use by another process\n" + CHAIN_INFO;
        Assert.assertEquals(List.of("/p/old.qcow2"), LibvirtDeleteVmCheckpointCommandWrapper.findChainLayersWithBitmap(withWarning, "cp-2"));
        try {
            LibvirtDeleteVmCheckpointCommandWrapper.findChainLayersWithBitmap("qemu-img: Could not open '/p/vol.qcow2'", "cp-2");
            Assert.fail("no JSON must be an error");
        } catch (CloudRuntimeException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().startsWith("qemu-img info returned no JSON"));
        }
    }

    @Test
    public void testRunningVmLibvirtDeletesTheCheckpoint() throws Exception {
        runningVmCommand("cp-3");
        List<List<String>> args = new ArrayList<>();

        try (MockedConstruction<Script> ignored = scripts(args, (String) null)) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals("Checkpoint deleted", answer.getDetails());
            Assert.assertEquals(List.of(List.of("checkpoint-delete", "i-2-3-VM", "cp-3")), args);
            Mockito.verify(domain, Mockito.never()).qemuMonitorCommand(Mockito.anyString(), Mockito.anyInt());
        }
    }

    @Test
    public void testRunningVmUnknownCheckpointIsRedefinedWithTheDisksThatHaveItsBitmapThenDeleted() throws Exception {
        // Lost by a stop/start; its bitmap survives only in the backing file of vda, below a VM snapshot overlay.
        runningVmCommand("cp-4");
        Mockito.when(command.getCheckpointCreateTime()).thenReturn(1700000000L);
        qmpReturns(NAMED_NODES);
        List<List<String>> args = new ArrayList<>();

        try (MockedConstruction<Script> ignored = scripts(args, "error: Domain checkpoint not found: no domain checkpoint with matching name 'cp-4'", null, null)) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getDetails(), answer.getResult());
            Assert.assertEquals("Checkpoint deleted, bitmap removed from disks [vda]", answer.getDetails());
            Assert.assertEquals(3, args.size());
            List<String> redefine = args.get(1);
            Assert.assertEquals(List.of("checkpoint-create", "--domain", "i-2-3-VM", "--xmlfile"), redefine.subList(0, 4));
            Assert.assertEquals("--redefine", redefine.get(5));
            String xml = redefine.get(6);
            Assert.assertTrue(xml, xml.contains("<name>cp-4</name>"));
            Assert.assertTrue(xml, xml.contains("<creationTime>1700000000</creationTime>"));
            Assert.assertTrue(xml, xml.contains("<disk name='vda' checkpoint='bitmap'/>"));
            Assert.assertTrue(xml, xml.contains("<disk name='vdb' checkpoint='no'/>"));
            Assert.assertFalse(new File(redefine.get(4)).exists());
            Assert.assertEquals(List.of("checkpoint-delete", "i-2-3-VM", "cp-4"), args.get(2));
            Mockito.verify(domain).free();
        }
    }

    @Test
    public void testRunningVmRedefineWithoutCreateTimeUsesNow() throws Exception {
        runningVmCommand("cp-5");
        Mockito.when(command.getCheckpointCreateTime()).thenReturn(null);
        qmpReturns(NAMED_NODES);
        List<List<String>> args = new ArrayList<>();
        long before = System.currentTimeMillis() / 1000;

        try (MockedConstruction<Script> ignored = scripts(args, "error: not found", null, null)) {
            Assert.assertTrue(wrapper.execute(command, resource).getResult());

            String xml = args.get(1).get(6);
            long creationTime = Long.parseLong(xml.replaceAll("(?s).*<creationTime>(\\d+)</creationTime>.*", "$1"));
            Assert.assertTrue(creationTime >= before);
        }
    }

    @Test
    public void testRunningVmBitmapGoneEverywhereDropsLibvirtsRecord() throws Exception {
        // libvirt still lists the checkpoint but a listed disk lost its bitmap (VM snapshot revert), so the delete failed.
        runningVmCommand("cp-9");
        qmpReturns(NAMED_NODES);
        List<List<String>> args = new ArrayList<>();

        try (MockedConstruction<Script> ignored = scripts(args, "error: bitmap 'cp-9' not found in backing chain of 'vda'", null, null)) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals("Checkpoint not present", answer.getDetails());
            Assert.assertEquals(List.of("checkpoint-dumpxml", "--domain", "i-2-3-VM", "--checkpointname", "cp-9", "--no-domain"), args.get(1));
            Assert.assertEquals(List.of("checkpoint-delete", "i-2-3-VM", "cp-9", "--metadata"), args.get(2));
        }
    }

    @Test
    public void testRunningVmBitmapGoneAndCheckpointUnknownIsAlreadyDeleted() throws Exception {
        runningVmCommand("cp-9");
        qmpReturns(NAMED_NODES);
        List<List<String>> args = new ArrayList<>();

        try (MockedConstruction<Script> ignored = scripts(args, "error: Domain checkpoint not found", "error: Domain checkpoint not found")) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals("Checkpoint not present", answer.getDetails());
            Assert.assertEquals(2, args.size());
        }
    }

    @Test
    public void testRunningVmBitmapGoneButDumpxmlErrorOtherThanNotFoundReturnsFailure() throws Exception {
        runningVmCommand("cp-9");
        qmpReturns(NAMED_NODES);
        List<List<String>> args = new ArrayList<>();

        try (MockedConstruction<Script> ignored = scripts(args, "error: Domain checkpoint not found", "error: failed to connect to the hypervisor")) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails(), answer.getDetails().startsWith("Failed to check whether libvirt still knows checkpoint cp-9"));
            Assert.assertEquals(2, args.size());
        }
    }

    @Test
    public void testRunningVmRedefineFailureReturnsFailure() throws Exception {
        runningVmCommand("cp-4");
        qmpReturns(NAMED_NODES);

        try (MockedConstruction<Script> ignored = scripts(new ArrayList<>(), "error: not found", "error: XML error")) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("Failed to redefine checkpoint cp-4 with disks [vda]: error: XML error"));
        }
    }

    @Test
    public void testRunningVmSecondDeleteFailureReturnsFailure() throws Exception {
        runningVmCommand("cp-4");
        qmpReturns(NAMED_NODES);

        try (MockedConstruction<Script> ignored = scripts(new ArrayList<>(), "error: not found", null, "error: internal error")) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("after redefining it with disks [vda]: error: internal error"));
        }
    }

    @Test
    public void testRunningVmBitmapLookupFailureReturnsFailure() throws Exception {
        runningVmCommand("cp-4");
        Mockito.when(domain.qemuMonitorCommand(Mockito.anyString(), Mockito.eq(0))).thenThrow(LibvirtException.class);

        try (MockedConstruction<Script> ignored = scripts(new ArrayList<>(), "error: not found")) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("bitmap lookup failed"));
            Mockito.verify(domain).free();
        }
    }

    @Test
    public void testStoppedVmWithoutDisksReturnsFailure() {
        Mockito.when(command.isStoppedVM()).thenReturn(true);
        Mockito.when(command.getCheckpointId()).thenReturn("cp-1");
        Mockito.when(command.getDiskPathUuidMap()).thenReturn(Collections.emptyMap());

        Answer answer = wrapper.execute(command, resource);

        Assert.assertFalse(answer.getResult());
        Assert.assertEquals("No disks provided for bitmap removal", answer.getDetails());
    }

    @Test
    public void testStoppedVmRemovesBitmapFromTheLayersThatHaveIt() {
        // The bitmap sits in the base of a VM snapshot overlay; the overlay and the template are left alone.
        stoppedVmCommand("cp-2", "/p/vol.qcow2");
        List<List<String>> args = new ArrayList<>();

        try (MockedConstruction<Script> ignored = scripts(args, JSON_OUTPUT + CHAIN_INFO, null)) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getDetails(), answer.getResult());
            Assert.assertEquals("Checkpoint bitmap removed from [/p/old.qcow2]", answer.getDetails());
            Assert.assertEquals(List.of("info", "-U", "--backing-chain", "--output=json", "/p/vol.qcow2"), args.get(0));
            Assert.assertEquals(List.of("bitmap", "--remove", "/p/old.qcow2", "cp-2"), args.get(1));
            Assert.assertEquals(2, args.size());
        }
    }

    @Test
    public void testStoppedVmBitmapNowhereInTheChainIsNotAFailure() {
        stoppedVmCommand("cp-9", "/p/vol.qcow2");
        List<List<String>> args = new ArrayList<>();

        try (MockedConstruction<Script> ignored = scripts(args, JSON_OUTPUT + CHAIN_INFO)) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals("Checkpoint not present", answer.getDetails());
            Assert.assertEquals(1, args.size());
        }
    }

    @Test
    public void testStoppedVmChainLookupFailureReturnsFailure() {
        stoppedVmCommand("cp-2", "/p/vol.qcow2");

        try (MockedConstruction<Script> ignored = scripts(new ArrayList<>(), "qemu-img: Could not open '/p/vol.qcow2': No such file or directory")) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("Failed to read the backing chain of disk /p/vol.qcow2"));
        }
    }

    @Test
    public void testStoppedVmBitmapRemovalFailureReturnsFailure() {
        stoppedVmCommand("cp-2", "/p/vol.qcow2");

        try (MockedConstruction<Script> ignored = scripts(new ArrayList<>(), JSON_OUTPUT + CHAIN_INFO, "qemu-img: Could not open '/p/old.qcow2': Permission denied")) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
            Assert.assertTrue(answer.getDetails(), answer.getDetails().contains("Failed to remove bitmap cp-2 from disk /p/old.qcow2"));
        }
    }

    @Test
    public void testStoppedVmMissingBitmapIsNotAFailure() {
        // Removed between the lookup and the removal, or a retried delete.
        stoppedVmCommand("cp-2", "/p/vol.qcow2");

        try (MockedConstruction<Script> ignored = scripts(new ArrayList<>(), JSON_OUTPUT + CHAIN_INFO,
                "qemu-img: Operation remove on bitmap cp-2 failed: Dirty bitmap 'cp-2' not found")) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            Assert.assertEquals("Checkpoint not present", answer.getDetails());
        }
    }

    @Test
    public void testStoppedVmOtherNotFoundErrorIsAFailure() {
        // Only the exact missing-bitmap message is tolerated; "not found" in another error is not.
        stoppedVmCommand("cp-2", "/p/vol.qcow2");

        try (MockedConstruction<Script> ignored = scripts(new ArrayList<>(), JSON_OUTPUT + CHAIN_INFO,
                "qemu-img: Could not open '/p/old.qcow2': No secret with id 'sec0' not found")) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertFalse(answer.getResult());
        }
    }

    @Test
    public void testStoppedVmEncryptedDiskUsesPassphraseOnEachLayerAndDeletesKeyFile() {
        stoppedVmCommand("cp-2", "/p/vol.qcow2");
        Mockito.when(command.getDiskPathPassphraseMap()).thenReturn(Map.of("/p/vol.qcow2", "secret".getBytes()));
        List<List<String>> args = new ArrayList<>();

        try (MockedConstruction<Script> ignored = scripts(args, JSON_OUTPUT + CHAIN_INFO, null)) {
            Answer answer = wrapper.execute(command, resource);

            Assert.assertTrue(answer.getResult());
            List<String> a = args.get(1);
            Assert.assertEquals(List.of("bitmap", "--remove", "--object"), a.subList(0, 3));
            Assert.assertTrue(a.get(3).startsWith("secret,id=sec0,file="));
            Assert.assertEquals("--image-opts", a.get(4));
            Assert.assertEquals("driver=qcow2,file.driver=file,file.filename=/p/old.qcow2,encrypt.key-secret=sec0", a.get(5));
            Assert.assertEquals("cp-2", a.get(6));
            Assert.assertFalse(new File(a.get(3).substring("secret,id=sec0,file=".length())).exists());
        }
    }
}
