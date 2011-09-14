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

import com.cloud.agent.api.to.LoadBalancerTO;

/**
 * LoadBalancerConfigCommand sends the load balancer configuration
 * to the load balancer.
 */
public class LoadBalancerConfigCommand extends NetworkElementCommand {
    LoadBalancerTO[] loadBalancers;
    public String lbStatsVisibility = "guest-network";
    public String lbStatsPublicIP; /* load balancer listen on this ips for stats */
    public String lbStatsPrivateIP; /* load balancer listen on this ips for stats */
    public String lbStatsGuestIP; /* load balancer listen on this ips for stats */
    public String lbStatsPort = "8081"; /*load balancer listen on this port for stats */
    public String lbStatsSrcCidrs = "0/0" ; /* TODO : currently there is no filtering based on the source ip */
    public String lbStatsAuth = "admin1:AdMiN123";
    public String lbStatsUri = "/admin?stats";  
    
    protected LoadBalancerConfigCommand() {
    }
    
    public LoadBalancerConfigCommand(LoadBalancerTO[] loadBalancers,String PublicIp,String GuestIp,String PrivateIp) {
    	this.loadBalancers = loadBalancers;
    	this.lbStatsPublicIP = PublicIp;
    	this.lbStatsPrivateIP = PrivateIp;
    	this.lbStatsGuestIP = GuestIp;
    }
    
   
	public LoadBalancerTO[] getLoadBalancers() {
        return loadBalancers;
    }
}
