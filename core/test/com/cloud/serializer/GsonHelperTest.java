package com.cloud.serializer;

import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.GetHostStatsAnswer;
import com.cloud.agent.api.GetHostStatsCommand;
import com.cloud.agent.api.UpdateHostPasswordCommand;
import com.google.gson.Gson;

import junit.framework.TestCase;

public class GsonHelperTest extends TestCase {
    private static final Logger s_logger = Logger.getLogger(GsonHelperTest.class);
    public void testCommandAdaptors() {
        final Gson gson = GsonHelper.getGson();
        s_logger.info("Testing gson");
        Command cmd_in;
        Command cmd_out;
        String json;
        String hostguid = "hostguid";
        String hostname = "hostname";
        Long hostId = 101l;

        cmd_in = new GetHostStatsCommand(hostguid, hostname, hostId);
        json = gson.toJson(cmd_in, GetHostStatsCommand.class);
        cmd_out = gson.fromJson(json, GetHostStatsCommand.class);

        assertEquals(((GetHostStatsCommand)cmd_out).getHostGuid(),hostguid);
        assertEquals(((GetHostStatsCommand)cmd_out).getHostName(),hostname);
        assertEquals(((GetHostStatsCommand)cmd_out).getHostId(),hostId);

        String username = "abc";
        String password = "def";
        cmd_in = new UpdateHostPasswordCommand(username, password);
        json = gson.toJson(cmd_in, UpdateHostPasswordCommand.class);
        cmd_out = gson.fromJson(json, UpdateHostPasswordCommand.class);

        assertEquals(((UpdateHostPasswordCommand)cmd_out).getUsername(), username);
        assertEquals(((UpdateHostPasswordCommand)cmd_out).getNewPassword(), password);
    }

    public void testAnswerAdaptors() {
        final Gson gson = GsonHelper.getGson();
        s_logger.info("Testing gson");
        Answer ans_in;
        Answer ans_out;
        String json;
        String hostGuid = "hostguid";
        String hostName = "hostname";
        long hostId = 101l;
        double cpuUtilization = 1.1;
        double freeMemoryKBs = 2.2;
        double totalMemoryKBs = 3.3;
        double networkReadKBs = 4.4;
        double networkWriteKBs = 5.5;
        String entityType = "bla";

        ans_in = new GetHostStatsAnswer(new GetHostStatsCommand(hostGuid, hostName, hostId),cpuUtilization,freeMemoryKBs,totalMemoryKBs,networkReadKBs,networkWriteKBs,entityType);
        json = gson.toJson(ans_in, GetHostStatsAnswer.class);
        ans_out = gson.fromJson(json, GetHostStatsAnswer.class);

        assertEquals(((GetHostStatsAnswer)ans_out).getCpuUtilization(),cpuUtilization);
    }
}
