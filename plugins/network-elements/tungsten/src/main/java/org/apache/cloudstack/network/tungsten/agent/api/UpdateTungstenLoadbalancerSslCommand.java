package org.apache.cloudstack.network.tungsten.agent.api;

import com.cloud.agent.api.Command;

public class UpdateTungstenLoadbalancerSslCommand extends Command {
    private final String lbUuid;
    private final String sslCertName;
    private final String certificateKey;
    private final String privateKey;
    private final String privateIp;
    private final String port;

    public UpdateTungstenLoadbalancerSslCommand(final String lbUuid, final String sslCertName,
        final String certificateKey, final String privateKey, final String privateIp, final String port) {
        this.lbUuid = lbUuid;
        this.sslCertName = sslCertName;
        this.certificateKey = certificateKey;
        this.privateKey = privateKey;
        this.privateIp = privateIp;
        this.port = port;
    }

    public String getLbUuid() {
        return lbUuid;
    }

    public String getSslCertName() {
        return sslCertName;
    }

    public String getCertificateKey() {
        return certificateKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public String getPrivateIp() {
        return privateIp;
    }

    public String getPort() {
        return port;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
