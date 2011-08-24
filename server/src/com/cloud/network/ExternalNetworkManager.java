/**
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */

package com.cloud.network;

import java.util.List;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.dc.DataCenter;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.component.Manager;
import com.cloud.utils.net.NetUtils;
import com.cloud.vm.NicVO;

public interface ExternalNetworkManager extends Manager {
		
}
