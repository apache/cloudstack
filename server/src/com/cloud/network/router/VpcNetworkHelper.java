package com.cloud.network.router;

import com.cloud.network.Network;
import com.cloud.network.vpc.VpcGateway;
import com.cloud.vm.NicProfile;

public interface VpcNetworkHelper extends NetworkHelper {

    public abstract NicProfile createPrivateNicProfileForGateway(
            VpcGateway privateGateway);

    public abstract NicProfile createGuestNicProfileForVpcRouter(
            Network guestNetwork);

}