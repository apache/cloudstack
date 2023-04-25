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

package com.cloud.network.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.DirectNetworkUsageAnswer;
import com.cloud.agent.api.DirectNetworkUsageCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.RecurringNetworkUsageAnswer;
import com.cloud.agent.api.RecurringNetworkUsageCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupTrafficMonitorCommand;
import com.cloud.host.Host;
import com.cloud.resource.ServerResource;
import com.cloud.utils.exception.ExecutionException;
import java.net.HttpURLConnection;

public class TrafficSentinelResource implements ServerResource {

    private String _name;
    private String _zoneId;
    private String _ip;
    private String _guid;
    private String _url;
    private String _inclZones;
    private String _exclZones;

    private static final Logger s_logger = Logger.getLogger(TrafficSentinelResource.class);

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        try {

            _name = name;

            _zoneId = (String)params.get("zone");
            if (_zoneId == null) {
                throw new ConfigurationException("Unable to find zone");
            }

            _ip = (String)params.get("ipaddress");
            if (_ip == null) {
                throw new ConfigurationException("Unable to find IP");
            }

            _guid = (String)params.get("guid");
            if (_guid == null) {
                throw new ConfigurationException("Unable to find the guid");
            }

            _url = (String)params.get("url");
            if (_url == null) {
                throw new ConfigurationException("Unable to find url");
            }

            _inclZones = (String)params.get("inclZones");
            _exclZones = (String)params.get("exclZones");

            return true;
        } catch (Exception e) {
            throw new ConfigurationException(e.getMessage());
        }

    }

    @Override
    public StartupCommand[] initialize() {
        StartupTrafficMonitorCommand cmd = new StartupTrafficMonitorCommand();
        cmd.setName(_name);
        cmd.setDataCenter(_zoneId);
        cmd.setPod("");
        cmd.setPrivateIpAddress(_ip);
        cmd.setStorageIpAddress("");
        cmd.setVersion(TrafficSentinelResource.class.getPackage().getImplementationVersion());
        cmd.setGuid(_guid);
        return new StartupCommand[] {cmd};
    }

    @Override
    public Host.Type getType() {
        return Host.Type.TrafficMonitor;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public PingCommand getCurrentStatus(final long id) {
        return new PingCommand(Host.Type.TrafficMonitor, id);
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void disconnected() {
        return;
    }

    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        return;
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof ReadyCommand) {
            return execute((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand)cmd);
        } else if (cmd instanceof DirectNetworkUsageCommand) {
            return execute((DirectNetworkUsageCommand)cmd);
        } else if (cmd instanceof RecurringNetworkUsageCommand) {
            return execute((RecurringNetworkUsageCommand)cmd);
        } else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    private Answer execute(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    private synchronized RecurringNetworkUsageAnswer execute(RecurringNetworkUsageCommand cmd) {
        return new RecurringNetworkUsageAnswer(cmd);
    }

    private synchronized DirectNetworkUsageAnswer execute(DirectNetworkUsageCommand cmd) {
        try {
            return getPublicIpBytesSentAndReceived(cmd);
        } catch (ExecutionException e) {
            return new DirectNetworkUsageAnswer(cmd, e);
        }
    }

    private Answer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private DirectNetworkUsageAnswer getPublicIpBytesSentAndReceived(DirectNetworkUsageCommand cmd) throws ExecutionException {
        DirectNetworkUsageAnswer answer = new DirectNetworkUsageAnswer(cmd);

        try {
            //Direct Network Usage
            URL trafficSentinel;
            //Use Global include/exclude zones if there are no per TS zones
            if (_inclZones == null) {
                _inclZones = cmd.getIncludeZones();
            }

            if (_exclZones == null) {
                _exclZones = cmd.getExcludeZones();
            }

            BufferedReader in = null;
            OutputStream os = null;
            try {
                //Query traffic Sentinel using POST method. 3 parts to the connection call and subsequent writing.

                //Part 1 - Connect to the URL of the traffic sentinel's instance.
                trafficSentinel = new URL(_url + "/inmsf/Query");
                String postData = "script="+URLEncoder.encode(getScript(cmd.getPublicIps(), cmd.getStart(), cmd.getEnd()), "UTF-8")+"&authenticate=basic&resultFormat=txt";
                HttpURLConnection con = (HttpURLConnection) trafficSentinel.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("content-length", String.valueOf(postData.length()));
                con.setDoOutput(true);

                //Part 2 - Write Data
                os = con.getOutputStream();
                os.write(postData.getBytes("UTF-8"));

                //Part 3 - Read response of the request
                in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    //Parse the script output
                    StringTokenizer st = new StringTokenizer(inputLine, ",");
                    if (st.countTokens() == 3) {
                        String publicIp = st.nextToken();
                        Long bytesSent = new Long(st.nextToken());
                        Long bytesRcvd = new Long(st.nextToken());
                        long[] bytesSentAndReceived = new long[2];
                        bytesSentAndReceived[0] = bytesSent;
                        bytesSentAndReceived[1] = bytesRcvd;
                        answer.put(publicIp, bytesSentAndReceived);
                    }
                }
            } catch (MalformedURLException e1) {
                s_logger.info("Invalid Traffic Sentinel URL", e1);
                throw new ExecutionException(e1.getMessage());
            } catch (IOException e) {
                s_logger.debug("Error in direct network usage accounting", e);
                throw new ExecutionException(e.getMessage());
            } finally {
                if (os != null) {
                    os.close();
                }
                if (in != null) {
                    in.close();
                }
            }
        } catch (Exception e) {
            s_logger.debug(e);
            throw new ExecutionException(e.getMessage());
        }
        return answer;
    }

    private String getScript(List<String> ips, Date start, Date end) {
        String IpAddresses = "";
        for (int i = 0; i < ips.size(); i++) {
            IpAddresses += ips.get(i);
            if (i != (ips.size() - 1)) {
                // Append comma for all Ips except the last Ip
                IpAddresses += ",";
            }
        }
        String destZoneCondition = "";
        if (_inclZones != null && !_inclZones.isEmpty()) {
            destZoneCondition = " & destinationzone = " + _inclZones;
        }
        if (_exclZones != null && !_exclZones.isEmpty()) {
            destZoneCondition += " & destinationzone != " + _exclZones;
        }

        String srcZoneCondition = "";
        if (_inclZones != null && !_inclZones.isEmpty()) {
            srcZoneCondition = " & sourcezone = " + _inclZones;
        }
        if (_exclZones != null && !_exclZones.isEmpty()) {
            srcZoneCondition += " & sourcezone != " + _exclZones;
        }

        String startDate = getDateString(start);
        String endtDate = getDateString(end);
        StringBuffer sb = new StringBuffer();
        sb.append("var q = Query.topN(\"historytrmx\",");
        sb.append("                 \"ipsource,bytes\",");
        sb.append("                 \"ipsource = " + IpAddresses + destZoneCondition + "\",");
        sb.append("                 \"" + startDate + ", " + endtDate + "\",");
        sb.append("                 \"bytes\",");
        sb.append("                 100000);");
        sb.append("var totalsSent = {};");
        sb.append("var t = q.run(");
        sb.append("  function(row,table) {");
        sb.append("    if(row[0]) {    ");
        sb.append("      totalsSent[row[0]] = row[1];");
        sb.append("    }");
        sb.append("  });");
        sb.append("var q = Query.topN(\"historytrmx\",");
        sb.append("                 \"ipdestination,bytes\",");
        sb.append("                 \"ipdestination = " + IpAddresses + srcZoneCondition + "\",");
        sb.append("                 \"" + startDate + ", " + endtDate + "\",");
        sb.append("                 \"bytes\",");
        sb.append("                 100000);");
        sb.append("var totalsRcvd = {};");
        sb.append("var t = q.run(");
        sb.append("  function(row,table) {");
        sb.append("    if(row[0]) {");
        sb.append("      totalsRcvd[row[0]] = row[1];");
        sb.append("    }");
        sb.append("  });");
        sb.append("for (var addr in totalsSent) {");
        sb.append("    var TS = 0;");
        sb.append("    var TR = 0;");
        sb.append("    if(totalsSent[addr]) TS = totalsSent[addr];");
        sb.append("    if(totalsRcvd[addr]) TR = totalsRcvd[addr];");
        sb.append("    println(addr + \",\" + TS + \",\" + TR);");
        sb.append("}");
        return sb.toString();
    }

    private String getDateString(Date date) {
        DateFormat dfDate = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        return dfDate.format(date);
    }

    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        // TODO Auto-generated method stub

    }

    @Override
    public Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
        // TODO Auto-generated method stub

    }
}
