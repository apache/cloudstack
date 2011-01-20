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

package com.cloud.agent.api;

public class PingTestCommand extends Command {

    String _computingHostIp = null;
    String _routerIp = null;
    String _privateIp = null;

    public PingTestCommand() {}

    public PingTestCommand(String computingHostIp) {
        _computingHostIp = computingHostIp;
    }

    public PingTestCommand(String routerIp, String privateIp) {
        _routerIp = routerIp;
        _privateIp = privateIp;
    }

    public String getComputingHostIp() {
        return _computingHostIp;
    }

    public String getRouterIp() {
        return _routerIp;
    }

    public String getPrivateIp() {
        return _privateIp;
    }

    @Override
    public boolean executeInSequence() {
        return false;
    }
}
