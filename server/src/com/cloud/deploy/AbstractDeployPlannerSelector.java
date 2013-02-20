package com.cloud.deploy;

import java.util.Map;

import javax.naming.ConfigurationException;

import com.cloud.vm.UserVmVO;

public abstract class AbstractDeployPlannerSelector implements DeployPlannerSelector {
    protected Map<String, Object>  params;
    protected String name;
    protected int runLevel;
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return params;
    }

    @Override
    public int getRunLevel() {
        return runLevel;
    }

    @Override
    public void setRunLevel(int level) {
        this.runLevel = level;
    }

    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        return true;
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
