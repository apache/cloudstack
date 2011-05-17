package com.cloud.agent.api.transport;

import junit.framework.TestCase;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.SecStorageFirewallCfgCommand;
import com.cloud.agent.api.UpdateHostPasswordCommand;
import com.cloud.agent.transport.Request;
import com.cloud.agent.transport.Response;
import com.cloud.exception.UnsupportedVersionException;

/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

public class RequestTest extends TestCase {
    private static final Logger s_logger = Logger.getLogger(RequestTest.class);

    public void testSerDeser() {
        UpdateHostPasswordCommand cmd1 = new UpdateHostPasswordCommand("abc", "def");
        SecStorageFirewallCfgCommand cmd2 = new SecStorageFirewallCfgCommand();
        GetHostStatsCommand cmd3 = new GetHostStatsCommand("hostguid", "hostname", 101);
        cmd2.addPortConfig("abc", "24", true, "eth0");
        cmd2.addPortConfig("127.0.0.1", "44", false, "eth1");
        Request sreq = new Request(1, 2, 3, new Command[] { cmd1, cmd2, cmd3 }, true, true);

        Logger logger = Logger.getLogger(Request.class);
        Level level = logger.getLevel();

        logger.setLevel(Level.DEBUG);
        sreq.log(1, "Debug");

        logger.setLevel(Level.TRACE);
        sreq.log(1, "Trace");

        logger.setLevel(Level.INFO);
        sreq.log(1, "Info");

        logger.setLevel(level);

        byte[] bytes = sreq.getBytes();
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

}
