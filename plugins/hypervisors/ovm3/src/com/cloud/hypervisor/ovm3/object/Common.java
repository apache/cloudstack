/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.object;

import org.apache.xmlrpc.XmlRpcException;

/*
 * should become an interface implementation
 */
public class Common extends OvmObject {
    public String apiVersion;

    public Common(Connection c) {
        client = c;
    }

    /*
     * get_api_version, <class 'agent.api.common.Common'>
     */
    public String getApiVersion() throws XmlRpcException {
        return callString("get_api_version");

    }

    /*
     * sleep, <class 'agent.api.common.Common'> argument: secs - default: None
     */
    public String sleep(int seconds) throws XmlRpcException {
        return callString("sleep", seconds);
    }

    /*
     * dispatch, <class 'agent.api.common.Common'> argument: uri - default: None
     * argument: func - default: None
     */
    /*
     * normally used to push commands to other hosts in a cluster: * dispatch
     * function join_server_pool to server
     * https://oracle:******@192.168.1.67:8899/api/3/
     */
    public <T> String dispatch(String url, String function, T... args)
            throws XmlRpcException {
        return callString("dispatch", url, function, args);
    }

    /*
     * echo, <class 'agent.api.common.Common'> argument: msg - default: None
     */
    public String echo(String msg) throws XmlRpcException {
        return callString("echo", msg);
    }

}
