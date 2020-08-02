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

package com.cloud.agent.transport;

import java.nio.ByteBuffer;
import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.mockito.Mockito;

import org.apache.cloudstack.storage.command.DownloadCommand;
import org.apache.cloudstack.storage.to.TemplateObjectTO;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.BadCommand;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.GetVolumeStatsCommand;
import com.cloud.agent.api.SecStorageFirewallCfgCommand;
import com.cloud.agent.api.UpdateHostPasswordCommand;
import com.cloud.agent.api.storage.DownloadAnswer;
import com.cloud.agent.api.storage.ListTemplateCommand;
import com.cloud.agent.api.to.NfsTO;
import com.cloud.agent.transport.Request.Version;
import com.cloud.exception.UnsupportedVersionException;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.serializer.GsonHelper;
import com.cloud.storage.DataStoreRole;
import com.cloud.storage.Storage.ImageFormat;
import com.cloud.storage.Storage.TemplateType;
import com.cloud.storage.VMTemplateStorageResourceAssoc.Status;
import com.cloud.template.VirtualMachineTemplate;

/**
 *
 *
 *
 *
 */

public class RequestTest extends TestCase {
    private static final Logger s_logger = Logger.getLogger(RequestTest.class);

    public void testSerDeser() {
        s_logger.info("Testing serializing and deserializing works as expected");

        s_logger.info("UpdateHostPasswordCommand should have two parameters that doesn't show in logging");
        UpdateHostPasswordCommand cmd1 = new UpdateHostPasswordCommand("abc", "def");
        s_logger.info("SecStorageFirewallCfgCommand has a context map that shouldn't show up in debug level");
        SecStorageFirewallCfgCommand cmd2 = new SecStorageFirewallCfgCommand();
        s_logger.info("GetHostStatsCommand should not show up at all in debug level");
        GetHostStatsCommand cmd3 = new GetHostStatsCommand("hostguid", "hostname", 101);
        cmd2.addPortConfig("abc", "24", true, "eth0");
        cmd2.addPortConfig("127.0.0.1", "44", false, "eth1");
        Request sreq = new Request(2, 3, new Command[] {cmd1, cmd2, cmd3}, true, true);
        sreq.setSequence(892403717);

        Logger logger = Logger.getLogger(GsonHelper.class);
        Level level = logger.getLevel();

        logger.setLevel(Level.DEBUG);
        String log = sreq.log("Debug", true, Level.DEBUG);
        assert (log.contains(UpdateHostPasswordCommand.class.getSimpleName()));
        assert (log.contains(SecStorageFirewallCfgCommand.class.getSimpleName()));
        assert (!log.contains(GetHostStatsCommand.class.getSimpleName()));
        assert (!log.contains("username"));
        assert (!log.contains("password"));

        logger.setLevel(Level.TRACE);
        log = sreq.log("Trace", true, Level.TRACE);
        assert (log.contains(UpdateHostPasswordCommand.class.getSimpleName()));
        assert (log.contains(SecStorageFirewallCfgCommand.class.getSimpleName()));
        assert (log.contains(GetHostStatsCommand.class.getSimpleName()));
        assert (!log.contains("username"));
        assert (!log.contains("password"));

        logger.setLevel(Level.INFO);
        log = sreq.log("Info", true, Level.INFO);
        assert (log == null);

        logger.setLevel(level);

        byte[] bytes = sreq.getBytes();

        assert Request.getSequence(bytes) == 892403717;
        assert Request.getManagementServerId(bytes) == 3;
        assert Request.getAgentId(bytes) == 2;
        assert Request.getViaAgentId(bytes) == 2;
        Request creq = null;
        try {
            creq = Request.parse(bytes);
        } catch (ClassNotFoundException e) {
            s_logger.error("Unable to parse bytes: ", e);
        } catch (UnsupportedVersionException e) {
            s_logger.error("Unable to parse bytes: ", e);
        }

        assert creq != null : "Couldn't get the request back";

        compareRequest(creq, sreq);

        Answer ans = new Answer(cmd1, true, "No Problem");
        Response cresp = new Response(creq, ans);

        bytes = cresp.getBytes();

        Response sresp = null;
        try {
            sresp = Response.parse(bytes);
        } catch (ClassNotFoundException e) {
            s_logger.error("Unable to parse bytes: ", e);
        } catch (UnsupportedVersionException e) {
            s_logger.error("Unable to parse bytes: ", e);
        }

        assert sresp != null : "Couldn't get the response back";

        compareRequest(cresp, sresp);
    }

    public void testSerDeserTO() {
        s_logger.info("Testing serializing and deserializing interface TO works as expected");

        NfsTO nfs = new NfsTO("nfs://192.168.56.10/opt/storage/secondary", DataStoreRole.Image);
        // SecStorageSetupCommand cmd = new SecStorageSetupCommand(nfs, "nfs://192.168.56.10/opt/storage/secondary", null);
        ListTemplateCommand cmd = new ListTemplateCommand(nfs);
        Request sreq = new Request(2, 3, cmd, true);
        sreq.setSequence(892403718);

        byte[] bytes = sreq.getBytes();

        assert Request.getSequence(bytes) == 892403718;
        assert Request.getManagementServerId(bytes) == 3;
        assert Request.getAgentId(bytes) == 2;
        assert Request.getViaAgentId(bytes) == 2;
        Request creq = null;
        try {
            creq = Request.parse(bytes);
        } catch (ClassNotFoundException e) {
            s_logger.error("Unable to parse bytes: ", e);
        } catch (UnsupportedVersionException e) {
            s_logger.error("Unable to parse bytes: ", e);
        }

        assert creq != null : "Couldn't get the request back";

        compareRequest(creq, sreq);
        assertEquals("nfs://192.168.56.10/opt/storage/secondary", ((NfsTO)((ListTemplateCommand)creq.getCommand()).getDataStore()).getUrl());
    }

    public void testDownload() {
        s_logger.info("Testing Download answer");
        VirtualMachineTemplate template = Mockito.mock(VirtualMachineTemplate.class);
        Mockito.when(template.getId()).thenReturn(1L);
        Mockito.when(template.getFormat()).thenReturn(ImageFormat.QCOW2);
        Mockito.when(template.getName()).thenReturn("templatename");
        Mockito.when(template.getTemplateType()).thenReturn(TemplateType.USER);
        Mockito.when(template.getDisplayText()).thenReturn("displayText");
        Mockito.when(template.getHypervisorType()).thenReturn(HypervisorType.KVM);
        Mockito.when(template.getUrl()).thenReturn("url");

        NfsTO nfs = new NfsTO("secUrl", DataStoreRole.Image);
        TemplateObjectTO to = new TemplateObjectTO(template);
        to.setImageDataStore(nfs);
        DownloadCommand cmd = new DownloadCommand(to, 30000000l);
        Request req = new Request(1, 1, cmd, true);

        req.logD("Debug for Download");

        DownloadAnswer answer = new DownloadAnswer("jobId", 50, "errorString", Status.ABANDONED, "filesystempath", "installpath", 10000000, 20000000, "chksum");
        Response resp = new Response(req, answer);
        resp.logD("Debug for Download");

    }

    public void testCompress() {
        s_logger.info("testCompress");
        int len = 800000;
        ByteBuffer inputBuffer = ByteBuffer.allocate(len);
        for (int i = 0; i < len; i++) {
            inputBuffer.array()[i] = 1;
        }
        inputBuffer.limit(len);
        ByteBuffer compressedBuffer = ByteBuffer.allocate(len);
        compressedBuffer = Request.doCompress(inputBuffer, len);
        s_logger.info("compressed length: " + compressedBuffer.limit());
        ByteBuffer decompressedBuffer = ByteBuffer.allocate(len);
        decompressedBuffer = Request.doDecompress(compressedBuffer, len);
        for (int i = 0; i < len; i++) {
            if (inputBuffer.array()[i] != decompressedBuffer.array()[i]) {
                Assert.fail("Fail at " + i);
            }
        }
    }

    public void testLogging() {
        s_logger.info("Testing Logging");
        GetHostStatsCommand cmd3 = new GetHostStatsCommand("hostguid", "hostname", 101);
        Request sreq = new Request(2, 3, new Command[] {cmd3}, true, true);
        sreq.setSequence(1);
        Logger logger = Logger.getLogger(GsonHelper.class);
        Level level = logger.getLevel();

        logger.setLevel(Level.DEBUG);
        String log = sreq.log("Debug", true, Level.DEBUG);
        assert (log == null);

        log = sreq.log("Debug", false, Level.DEBUG);
        assert (log != null);

        logger.setLevel(Level.TRACE);
        log = sreq.log("Trace", true, Level.TRACE);
        assert (log.contains(GetHostStatsCommand.class.getSimpleName()));
        s_logger.debug(log);

        logger.setLevel(level);
    }

    protected void compareRequest(Request req1, Request req2) {
        assert req1.getSequence() == req2.getSequence();
        assert req1.getAgentId() == req2.getAgentId();
        assert req1.getManagementServerId() == req2.getManagementServerId();
        assert req1.isControl() == req2.isControl();
        assert req1.isFromServer() == req2.isFromServer();
        assert req1.executeInSequence() == req2.executeInSequence();
        assert req1.stopOnError() == req2.stopOnError();
        assert req1.getVersion().equals(req2.getVersion());
        assert req1.getViaAgentId() == req2.getViaAgentId();
        Command[] cmd1 = req1.getCommands();
        Command[] cmd2 = req2.getCommands();
        for (int i = 0; i < cmd1.length; i++) {
            assert cmd1[i].getClass().equals(cmd2[i].getClass());
        }
    }

    public void testGoodCommand() {
        s_logger.info("Testing good Command");
        String content = "[{\"com.cloud.agent.api.GetVolumeStatsCommand\":{\"volumeUuids\":[\"dcc860ac-4a20-498f-9cb3-bab4d57aa676\"],"
                + "\"poolType\":\"NetworkFilesystem\",\"poolUuid\":\"e007c270-2b1b-3ce9-ae92-a98b94eef7eb\",\"contextMap\":{},\"wait\":5}}]";
        Request sreq = new Request(Version.v2, 1L, 2L, 3L, 1L, (short)1, content);
        sreq.setSequence(1);
        Command cmds[] = sreq.getCommands();
        s_logger.debug("Command class = " + cmds[0].getClass().getSimpleName());
        assert cmds[0].getClass().equals(GetVolumeStatsCommand.class);
    }

    public void testBadCommand() {
        s_logger.info("Testing Bad Command");
        String content = "[{\"com.cloud.agent.api.SomeJunkCommand\":{\"volumeUuids\":[\"dcc860ac-4a20-498f-9cb3-bab4d57aa676\"],"
                + "\"poolType\":\"NetworkFilesystem\",\"poolUuid\":\"e007c270-2b1b-3ce9-ae92-a98b94eef7eb\",\"contextMap\":{},\"wait\":5}}]";
        Request sreq = new Request(Version.v2, 1L, 2L, 3L, 1L, (short)1, content);
        sreq.setSequence(1);
        Command cmds[] = sreq.getCommands();
        s_logger.debug("Command class = " + cmds[0].getClass().getSimpleName());
        assert cmds[0].getClass().equals(BadCommand.class);
    }

}
