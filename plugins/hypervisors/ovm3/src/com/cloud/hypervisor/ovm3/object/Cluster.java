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
public class Cluster extends OvmObject {

    public Cluster(Connection c) {
        client = c;
    }

    /*
     * leave_cluster, <class 'agent.api.cluster.o2cb.ClusterO2CB'> argument:
     * self - default: None argument: poolfsUuid - default: None
     */
    public Boolean leaveCluster(String poolfsUuid) throws XmlRpcException {
        Object x = callWrapper("leave_cluster", poolfsUuid);
        if (x == null)
            return true;

        return false;
    }

    /*
     * configure_server_for_cluster, <class
     * 'agent.api.cluster.o2cb.ClusterO2CB'> argument: self - default: None <( ?
     * argument: o2cb_conf - default: None <( ? argument: clusterConf -
     * default: None <( ? argument: poolfs_type - default: None argument:
     * poolfs_target - default: None argument: poolfsUuid - default: None
     * argument: poolfs_nfsbase_uuid - default: None
     */
    public Boolean configureServerForCluster(String poolfsUuid)
            throws XmlRpcException {
        Object x = callWrapper("configure_server_for_cluster", poolfsUuid);
        if (x == null)
            return true;

        return false;
    }

    /*
     * deconfigure_server_for_cluster, <class
     * 'agent.api.cluster.o2cb.ClusterO2CB'> argument: self - default: None
     * argument: poolfsUuid - default: None
     */
    public Boolean deconfigureServerForCluster(String poolfsUuid)
            throws XmlRpcException {
        Object x = callWrapper("deconfigure_server_for_cluster", poolfsUuid);
        if (x == null)
            return true;

        return false;
    }

    /*
     * join_cluster, <class 'agent.api.cluster.o2cb.ClusterO2CB'> argument: self
     * - default: None argument: poolfsUuid - default: None
     */
    public Boolean joinCLuster(String poolfsUuid) throws XmlRpcException {
        Object x = callWrapper("join_cluster", poolfsUuid);
        if (x == null)
            return true;

        return false;
    }

    /*
     * discover_cluster, <class 'agent.api.cluster.o2cb.ClusterO2CB'> argument:
     * self - default: None
     */
    /*
     * <Discover_Cluster_Result>< <O2CB_Config>
     * <O2CB_HEARTBEAT_THRESHOLD>61</O2CB_HEARTBEAT_THRESHOLD>
     * <O2CB_RECONNECT_DELAY_MS>2000</O2CB_RECONNECT_DELAY_MS>
     * <O2CB_KEEPALIVE_DELAY_MS>2000</O2CB_KEEPALIVE_DELAY_MS>
     * <O2CB_BOOTCLUSTER>ba9aaf00ae5e2d73</O2CB_BOOTCLUSTER>
     * <O2CB_IDLE_TIMEOUT_MS>60000</O2CB_IDLE_TIMEOUT_MS>
     * <O2CB_ENABLED>true</O2CB_ENABLED> <O2CB_STACK>o2cb</O2CB_STACK>
     * </O2CB_Config> <Cluster_Information> <Stored> <Clusters> <Cluster>
     * <Name>ba9aaf00ae5e2d73</Name> <Node_Count>1</Node_Count>
     * <Heartbeat_Mode>global</Heartbeat_Mode> </Cluster> </Clusters>
     * <Heartbeats> <Heartbeat>
     * <Region>0004FB0000050000E70FBDDEB802208F</Region>
     * <Cluster>ba9aaf00ae5e2d73</Cluster> </Heartbeat> </Heartbeats> <Nodes>
     * <Node> <Number>0</Number> <IP_Port>7777</IP_Port>
     * <IP_Address>192.168.1.64</IP_Address> <Name>ovm-1</Name>
     * <Cluster_Name>ba9aaf00ae5e2d73</Cluster_Name> </Node> </Nodes> </Stored>
     * </Cluster_Information> </Discover_Cluster_Result>
     */
    /* returns xml - sigh */
    public Boolean discoverCluster() throws XmlRpcException {
        Object x = callWrapper("discover_cluster");
        // System.out.println(x);

        return false;
    }

    /*
     * update_clusterConfiguration, <class
     * 'agent.api.cluster.o2cb.ClusterO2CB'> argument: self - default: None
     * argument: cluster_conf - default: None <( ? cluster_conf can be a "dict"
     * or a plain file: print master.update_clusterConfiguration(
     * "heartbeat:\n\tregion = 0004FB0000050000E70FBDDEB802208F\n\tcluster = ba9aaf00ae5e2d72\n\nnode:\n\tip_port = 7777\n\tip_address = 192.168.1.64\n\tnumber = 0\n\tname = ovm-1\n\tcluster = ba9aaf00ae5e2d72\n\nnode:\n\tip_port = 7777\n\tip_address = 192.168.1.65\n\tnumber = 1\n\tname = ovm-2\n\tcluster = ba9aaf00ae5e2d72\n\ncluster:\n\tnode_count = 2\n\theartbeat_mode = global\n\tname = ba9aaf00ae5e2d72\n"
     * )
     */
    public Boolean updateClusterConfiguration(String clusterConf)
            throws XmlRpcException {
        Object x = callWrapper("update_clusterConfiguration", clusterConf);
        if (x == null)
            return true;

        return false;
    }

    /*
     * destroy_cluster, <class 'agent.api.cluster.o2cb.ClusterO2CB'> argument:
     * self - default: None argument: poolfsUuid - default: None
     */
    public Boolean destroyCluster(String poolfsUuid) throws XmlRpcException {
        Object x = callWrapper("destroy_cluster", poolfsUuid);
        if (x == null)
            return true;

        return false;
    }

    /*
     * is_cluster_online, <class 'agent.api.cluster.o2cb.ClusterO2CB'> argument:
     * self - default: None
     */
    public Boolean isClusterOnline() throws XmlRpcException {
        Object x = callWrapper("is_cluster_online");
        return Boolean.valueOf(x.toString());
    }

    /*
     * create_cluster, <class 'agent.api.cluster.o2cb.ClusterO2CB'> argument:
     * self - default: None argument: poolfsUuid - default: None
     */
    public Boolean createCluster(String poolfsUuid) throws XmlRpcException {
        Object x = callWrapper("create_cluster", poolfsUuid);
        if (x == null)
            return true;

        return false;
    }
}
