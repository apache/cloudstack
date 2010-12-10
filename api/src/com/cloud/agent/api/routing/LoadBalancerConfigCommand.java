/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.agent.api.routing;

import java.util.List;

import com.cloud.network.rules.LoadBalancer;
import com.cloud.network.rules.LoadBalancer.Destination;

/**
 * LoadBalancerConfigCommand sends the load balancer configuration
 * to the load balancer.  Isn't that kinda obvious?
 */
public class LoadBalancerConfigCommand extends RoutingCommand {
    String srcIp;
    int srcPort;
    String protocol;
    String algorithm;    
    boolean revoked;
    boolean alreadyAdded;
    DestinationTO[] destinations;
    
    public LoadBalancerConfigCommand(String srcIp, int srcPort, String protocol, String algorithm, boolean revoked, boolean alreadyAdded, List<? extends Destination> destinations) {
    	this.srcIp = srcIp;
    	this.srcPort = srcPort;
    	this.protocol = protocol;
    	this.algorithm = algorithm;    	
    	this.revoked = revoked;
    	this.alreadyAdded = alreadyAdded;
    	this.destinations = new DestinationTO[destinations.size()];
    	int i = 0;
    	for (Destination destination : destinations) {
    		this.destinations[i++] = new DestinationTO(destination.getIpAddress(), destination.getDestinationPortStart(), destination.getRevoked(), destination.getAlreadyAdded());
    	}
    }
    
    public String getSrcIp() {
        return srcIp;
    }

    public int getSrcPort() {
        return srcPort;
    }
    
    public String getAlgorithm() {
    	return algorithm;
    }

    public String getProtocol() {
        return protocol;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public boolean isAlreadyAdded() {
        return alreadyAdded;
    }
    
    public DestinationTO[] getDestinations() {
    	return destinations;
    }

    public class DestinationTO {
        String destIp;
        int destPort;
        boolean revoked;
        boolean alreadyAdded;
        public DestinationTO(String destIp, int destPort, boolean revoked, boolean alreadyAdded) {
            this.destIp = destIp;
            this.destPort = destPort;
            this.revoked = revoked;
            this.alreadyAdded = alreadyAdded;
        }
        
        public String getDestIp() {
            return destIp;
        }
        
        public int getDestPort() {
            return destPort;
        }
        
        public boolean isRevoked() {
            return revoked;
        }

        public boolean isAlreadyAdded() {
            return alreadyAdded;
        }
    }
}
