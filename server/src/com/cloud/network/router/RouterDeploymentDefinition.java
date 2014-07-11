package com.cloud.network.router;

import java.util.Map;

import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.network.Network;
import com.cloud.network.vpc.Vpc;
import com.cloud.user.Account;
import com.cloud.vm.VirtualMachineProfile.Param;

public class RouterDeploymentDefinition {

    protected Vpc vpc;
    protected Network guestNetwork;
    protected DeployDestination dest;
    protected Account owner;
    protected Map<Param, Object> params;
    protected boolean isRedundant;
    protected DeploymentPlan plan;

    public RouterDeploymentDefinition(Vpc vpc, DeployDestination dest,
            Account owner, Map<Param, Object> params,
            boolean isRedundant) {

        this.vpc = vpc;
        this.dest = dest;
        this.owner = owner;
        this.params = params;
        this.isRedundant = isRedundant;
    }

    public RouterDeploymentDefinition(Network guestNetwork, DeployDestination dest,
            Account owner, Map<Param, Object> params, boolean isRedundant) {

        this.guestNetwork = guestNetwork;
        this.dest = dest;
        this.owner = owner;
        this.params = params;
        this.isRedundant = isRedundant;
    }

    public Vpc getVpc() {
        return vpc;
    }
    public void setVpc(Vpc vpc) {
        this.vpc = vpc;
    }
    public Network getGuestNetwork() {
        return guestNetwork;
    }
    public void setGuestNetwork(Network guestNetwork) {
        this.guestNetwork = guestNetwork;
    }
    public DeployDestination getDest() {
        return dest;
    }
    public void setDest(DeployDestination dest) {
        this.dest = dest;
    }
    public Account getOwner() {
        return owner;
    }
    public void setOwner(Account owner) {
        this.owner = owner;
    }
    public Map<Param, Object> getParams() {
        return params;
    }
    public void setParams(Map<Param, Object> params) {
        this.params = params;
    }
    public boolean isRedundant() {
        return isRedundant;
    }
    public void setRedundant(boolean isRedundant) {
        this.isRedundant = isRedundant;
    }
    public DeploymentPlan getPlan() {
        return plan;
    }
    public void setPlan(DeploymentPlan plan) {
        this.plan = plan;
    }

    public boolean isVpcRouter() {
        return this.vpc != null;
    }
    public Long getPodId() {
        return this.plan.getPodId();
    }

}
