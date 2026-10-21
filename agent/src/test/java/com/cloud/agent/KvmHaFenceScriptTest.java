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
package com.cloud.agent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Runs scripts/vm/hypervisor/kvm/kvmha-fence.sh with stand-ins for virsh, systemctl, logger and the sysrq trigger,
 * so nothing on the machine running the test is stopped, restarted or rebooted.
 */
public class KvmHaFenceScriptTest {

    private static final File SCRIPT = new File("../scripts/vm/hypervisor/kvm/kvmha-fence.sh");

    private Path dir;
    private Path running;
    private Path calls;
    private Path syslog;
    private Path sysrq;
    private Path props;

    @Before
    public void setUp() throws IOException {
        Assume.assumeTrue("needs bash", new File("/bin/bash").canExecute());
        Assume.assumeTrue("needs the fence script", SCRIPT.isFile());
        dir = Files.createTempDirectory("kvmha-fence");
        running = dir.resolve("running");
        calls = dir.resolve("calls");
        syslog = dir.resolve("syslog");
        sysrq = dir.resolve("sysrq");
        props = dir.resolve("agent.properties");
        Files.createFile(calls);
        Files.createFile(syslog);
        stub("virsh", "echo \"virsh $*\" >> '" + calls + "'\n"
                + "case \" $* \" in\n"
                + "  *' list '*) cat '" + running + "' ;;\n"
                + "  *' destroy '*) vm=\"${@: -1}\"; grep -qx \"$vm\" '" + dir.resolve("stuck") + "' 2>/dev/null && exit 1;"
                + " grep -vx \"$vm\" '" + running + "' > '" + running + ".new'; mv '" + running + ".new' '" + running + "' ;;\n"
                + "esac\n");
        stub("systemctl", "echo \"systemctl $*\" >> '" + calls + "'\n");
        stub("logger", "echo \"$*\" >> '" + syslog + "'\n");
        stub("sleep", "exit 0\n");
    }

    @After
    public void tearDown() throws IOException {
        if (dir == null) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    private void stub(final String name, final String body) throws IOException {
        final Path file = dir.resolve(name);
        Files.write(file, ("#!/bin/bash\n" + body).getBytes(StandardCharsets.UTF_8));
        Assert.assertTrue(file.toFile().setExecutable(true));
    }

    private int fence(final String properties, final String... runningVms) throws Exception {
        Files.write(props, properties.getBytes(StandardCharsets.UTF_8));
        Files.write(running, (String.join("\n", runningVms) + "\n").getBytes(StandardCharsets.UTF_8));
        final ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", "source \"$1\"; fence_action test.sh", "fence",
                SCRIPT.getAbsolutePath());
        pb.environment().put("PATH", dir + File.pathSeparator + System.getenv("PATH"));
        pb.environment().put("AGENT_PROPS", props.toString());
        pb.environment().put("FENCE_LOGGER", dir.resolve("logger").toString());
        pb.environment().put("FENCE_SYSRQ_TRIGGER", sysrq.toString());
        pb.redirectErrorStream(true);
        pb.redirectOutput(dir.resolve("out").toFile());
        final Process process = pb.start();
        Assert.assertTrue("fence script did not finish", process.waitFor(60, TimeUnit.SECONDS));
        return process.exitValue();
    }

    private String read(final Path file) throws IOException {
        return file.toFile().exists() ? new String(Files.readAllBytes(file), StandardCharsets.UTF_8) : "";
    }

    private boolean rebooted() throws IOException {
        return read(sysrq).trim().equals("b");
    }

    @Test
    public void logOnlyStopsEveryVmAndDoesNotReboot() throws Exception {
        Assert.assertEquals(0, fence("kvm.heartbeat.fence.action=log-only\n", "i-2-10-VM", "r-4-VM"));
        Assert.assertTrue(read(calls).contains("destroy i-2-10-VM"));
        Assert.assertTrue(read(calls).contains("destroy r-4-VM"));
        Assert.assertEquals("", read(running).trim());
        Assert.assertFalse(read(calls).contains("systemctl"));
        Assert.assertFalse(rebooted());
    }

    @Test
    public void restartAgentStopsEveryVmBeforeRestartingTheAgent() throws Exception {
        Assert.assertEquals(0, fence("kvm.heartbeat.fence.action=restart-agent\n", "i-2-10-VM"));
        final String log = read(calls);
        Assert.assertTrue(log.contains("destroy i-2-10-VM"));
        Assert.assertTrue(log.contains("systemctl restart cloudstack-agent"));
        Assert.assertTrue("VMs must be stopped before the agent restarts",
                log.indexOf("destroy i-2-10-VM") < log.indexOf("systemctl restart cloudstack-agent"));
        Assert.assertFalse(rebooted());
    }

    @Test
    public void aVmThatCannotBeStoppedFallsBackToHardReboot() throws Exception {
        Files.write(dir.resolve("stuck"), "i-2-11-VM\n".getBytes(StandardCharsets.UTF_8));
        fence("kvm.heartbeat.fence.action=restart-agent\n", "i-2-10-VM", "i-2-11-VM");
        Assert.assertTrue("a VM still running must not be left behind", rebooted());
        Assert.assertFalse(read(calls).contains("systemctl restart cloudstack-agent"));
        Assert.assertTrue(read(syslog).contains("falling back to hard-reboot"));
    }

    @Test
    public void keepVmsRunningIsHonouredWithAWarning() throws Exception {
        Assert.assertEquals(0, fence("kvm.heartbeat.fence.action=log-only\nkvm.heartbeat.fence.keep.vms.running=true\n", "i-2-10-VM"));
        Assert.assertFalse(read(calls).contains("destroy"));
        Assert.assertEquals("i-2-10-VM", read(running).trim());
        Assert.assertTrue(read(syslog).contains("WARNING"));
        Assert.assertFalse(rebooted());
    }

    @Test
    public void includeSystemVmsTrueStopsSystemVmsToo() throws Exception {
        Assert.assertEquals(0, fence("kvm.heartbeat.fence.action=log-only\nkvm.heartbeat.fence.include.system.vms=true\n",
                "i-2-10-VM", "r-4-VM", "s-1-VM", "v-2-VM", "b-5-VM"));
        Assert.assertEquals("", read(running).trim());
        Assert.assertFalse(rebooted());
    }

    @Test
    public void includeSystemVmsFalseStopsOnlyUserVms() throws Exception {
        Assert.assertEquals(0, fence("kvm.heartbeat.fence.action=restart-agent\nkvm.heartbeat.fence.include.system.vms=false\n",
                "i-2-10-VM", "r-4-VM", "r-7-VM-1234", "s-1-VM", "v-2-VM", "b-5-VM"));
        final String log = read(calls);
        Assert.assertTrue(log.contains("destroy i-2-10-VM"));
        for (final String systemVm : new String[] {"r-4-VM", "r-7-VM-1234", "s-1-VM", "v-2-VM", "b-5-VM"}) {
            Assert.assertFalse(systemVm + " must keep running", log.contains("destroy " + systemVm));
        }
        Assert.assertEquals("r-4-VM\nr-7-VM-1234\ns-1-VM\nv-2-VM\nb-5-VM", read(running).trim());
        Assert.assertTrue(log.contains("systemctl restart cloudstack-agent"));
        Assert.assertTrue(read(syslog).contains("include.system.vms=false"));
        Assert.assertFalse(rebooted());
    }

    @Test
    public void includeSystemVmsFalseIgnoresAStuckSystemVmButNotAStuckUserVm() throws Exception {
        Files.write(dir.resolve("stuck"), "r-4-VM\n".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals(0, fence("kvm.heartbeat.fence.action=log-only\nkvm.heartbeat.fence.include.system.vms=false\n",
                "i-2-10-VM", "r-4-VM"));
        Assert.assertFalse("a running system VM must not force a reboot", rebooted());

        Files.write(dir.resolve("stuck"), "i-2-11-VM\n".getBytes(StandardCharsets.UTF_8));
        fence("kvm.heartbeat.fence.action=log-only\nkvm.heartbeat.fence.include.system.vms=false\n", "i-2-11-VM", "r-4-VM");
        Assert.assertTrue("a user VM still running must still force a reboot", rebooted());
    }

    @Test
    public void keepVmsRunningWinsOverIncludeSystemVms() throws Exception {
        Assert.assertEquals(0, fence("kvm.heartbeat.fence.action=log-only\nkvm.heartbeat.fence.keep.vms.running=true\n"
                + "kvm.heartbeat.fence.include.system.vms=false\n", "i-2-10-VM", "r-4-VM"));
        Assert.assertFalse(read(calls).contains("destroy"));
        Assert.assertEquals("i-2-10-VM\nr-4-VM", read(running).trim());
        Assert.assertTrue(read(syslog).contains("overrides kvm.heartbeat.fence.include.system.vms"));
        Assert.assertFalse(rebooted());
    }

    @Test
    public void hardRebootIsTheDefaultAndDoesNotTouchVmsFirst() throws Exception {
        fence("", "i-2-10-VM");
        Assert.assertTrue(rebooted());
        Assert.assertFalse(read(calls).contains("destroy"));
    }

    @Test
    public void unknownActionFallsBackToHardReboot() throws Exception {
        fence("kvm.heartbeat.fence.action=custom\n", "i-2-10-VM");
        Assert.assertTrue(rebooted());
    }
}
