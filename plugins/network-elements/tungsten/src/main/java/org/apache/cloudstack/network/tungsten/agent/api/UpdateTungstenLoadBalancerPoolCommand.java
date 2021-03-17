package org.apache.cloudstack.network.tungsten.agent.api;

public class UpdateTungstenLoadBalancerPoolCommand extends TungstenCommand {
    private final String projectFqn;
    private final String lbPoolName;
    private final String lbMethod;
    private final String lbSessionPersistence;
    private final String lbPersistenceCookieName;
    private final String lbProtocol;

    public UpdateTungstenLoadBalancerPoolCommand(final String projectFqn, final String lbPoolName,
        final String lbMethod, final String lbSessionPersistence, final String lbPersistenceCookieName,
        final String lbProtocol) {
        this.projectFqn = projectFqn;
        this.lbPoolName = lbPoolName;
        this.lbMethod = lbMethod;
        this.lbSessionPersistence = lbSessionPersistence;
        this.lbPersistenceCookieName = lbPersistenceCookieName;
        this.lbProtocol = lbProtocol;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getLbPoolName() {
        return lbPoolName;
    }

    public String getLbMethod() {
        return lbMethod;
    }

    public String getLbSessionPersistence() {
        return lbSessionPersistence;
    }

    public String getLbPersistenceCookieName() {
        return lbPersistenceCookieName;
    }

    public String getLbProtocol() {
        return lbProtocol;
    }
}
