package com.cloud.agent.api;

import java.util.List;

import com.cloud.utils.Ternary;

public class PrepareOCFS2NodesCommand extends Command {
    List<Ternary<Integer, String, String>> nodes;
    String clusterName;
    
    @Override
    public boolean executeInSequence() {
        return true;
    }

    public PrepareOCFS2NodesCommand(String clusterName, List<Ternary<Integer, String, String>> nodes) {
        this.nodes = nodes;
        this.clusterName = clusterName;
    }
    
    public List<Ternary<Integer, String, String>> getNodes() {
        return nodes;
    }
    
    public String getClusterName() {
        return clusterName;
    }
}
