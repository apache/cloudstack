package org.apache.cloudstack.network.tungsten.agent.api;

import com.cloud.agent.api.Command;

public class TungstenCommand extends Command {
    @Override
    public boolean executeInSequence() {
        return false;
    }
}
