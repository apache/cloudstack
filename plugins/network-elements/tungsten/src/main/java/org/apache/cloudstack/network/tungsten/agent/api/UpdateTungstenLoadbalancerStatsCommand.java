package org.apache.cloudstack.network.tungsten.agent.api;

import com.cloud.agent.api.Command;

public class UpdateTungstenLoadbalancerStatsCommand extends Command {
    private final String lbUuid;
    private final String lbStatsPort;
    private final String lbStatsUri;
    private final String lbStatsAuth;

    public UpdateTungstenLoadbalancerStatsCommand(final String lbUuid, final String lbStatsPort,
        final String lbStatsUri, final String lbStatsAuth) {
        this.lbUuid = lbUuid;
        this.lbStatsPort = lbStatsPort;
        this.lbStatsUri = lbStatsUri;
        this.lbStatsAuth = lbStatsAuth;
    }

    public String getLbUuid() {
        return lbUuid;
    }

    public String getLbStatsPort() {
        return lbStatsPort;
    }

    public String getLbStatsUri() {
        return lbStatsUri;
    }

    public String getLbStatsAuth() {
        return lbStatsAuth;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
