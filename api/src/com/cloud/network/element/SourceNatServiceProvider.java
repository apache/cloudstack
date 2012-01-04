package com.cloud.network.element;

import com.cloud.network.Network;

public interface SourceNatServiceProvider extends NetworkElement {
    IpDeployer getIpDeployer(Network network);
}
