package org.apache.cloudstack.network.tungsten.agent.api;

import org.apache.cloudstack.network.tungsten.model.TungstenLoadBalancerMember;

import java.util.List;

public class UpdateTungstenLoadBalancerMemberCommand extends TungstenCommand {
    private final String projectFqn;
    private final String networkUuid;
    private final String lbPoolName;
    private final List<TungstenLoadBalancerMember> listTungstenLoadBalancerMember;

    public UpdateTungstenLoadBalancerMemberCommand(final String projectFqn, final String networkUuid,
        final String lbPoolName, final List<TungstenLoadBalancerMember> listTungstenLoadBalancerMember) {
        this.projectFqn = projectFqn;
        this.networkUuid = networkUuid;
        this.lbPoolName = lbPoolName;
        this.listTungstenLoadBalancerMember = listTungstenLoadBalancerMember;
    }

    public String getProjectFqn() {
        return projectFqn;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public String getLbPoolName() {
        return lbPoolName;
    }

    public List<TungstenLoadBalancerMember> getListTungstenLoadBalancerMember() {
        return listTungstenLoadBalancerMember;
    }
}
