// Copyright 2012 Citrix Systems, Inc. Licensed under the
package com.cloud.agent.api.routing;

import com.cloud.agent.api.Answer;

public class SetNetworkACLAnswer extends Answer {
    String[] results;
    
    protected SetNetworkACLAnswer() {
    }
    
    public SetNetworkACLAnswer(SetNetworkACLCommand cmd, boolean success, String[] results) {
        super(cmd, success, null);
        assert (cmd.getRules().length == results.length) : "ACLs and their results should be the same length";
        this.results = results;
    }
    
    public String[] getResults() {
        return results;
    }
}