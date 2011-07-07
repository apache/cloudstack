package com.cloud.agent.api.baremetal;

import com.cloud.agent.api.Command;

public class IpmiBootorResetCommand extends Command {

    @Override
    public boolean executeInSequence() {
        return true;
    }

}
