//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//

package org.apache.cloudstack.storage.datastore.util;

import com.cloud.agent.api.Answer;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.utils.security.SSLUtils;
import org.apache.cloudstack.utils.security.SecureSSLSocketFactory;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.log4j.Logger;

import javax.naming.ServiceUnavailableException;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import java.net.ConnectException;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;

public class ElastistorUtil {

    private static final Logger s_logger = Logger.getLogger(ElastistorUtil.class);

    private static ConfigurationDao configurationDao;

    public static ConfigurationDao getConfigurationDao() {
        return configurationDao;
    }

    public static void setConfigurationDao(ConfigurationDao configurationDao) {
        ElastistorUtil.configurationDao = configurationDao;
    }

    /**
     * Elastistor REST API Param Keys. These should match exactly with the
     * elastistor API commands' params.
     */
    public static final String REST_PARAM_COMMAND = "command";
    public static final String REST_PARAM_APIKEY = "apikey";
    public static final String REST_PARAM_KEYWORD = "keyword";
    public static final String REST_PARAM_ID = "id";
    public static final String REST_PARAM_QUOTA_SIZE = "quotasize";
    public static final String REST_PARAM_READONLY = "readonly";
    public static final String REST_PARAM_RESPONSE = "response";
    public static final String REST_PARAM_POOLID = "poolid";
    public static final String REST_PARAM_ACCOUNTID = "accountid";
    public static final String REST_PARAM_GATEWAY = "router";
    public static final String REST_PARAM_SUBNET = "subnet";
    public static final String REST_PARAM_INTERFACE = "tntinterface";
    public static final String REST_PARAM_IPADDRESS = "ipaddress";
    public static final String REST_PARAM_JOBID = "jobId";
    public static final String REST_PARAM_FORECEDELETE = "forcedelete";
    public static final String REST_PARAM_TSM_THROUGHPUT = "totalthroughput";
    public static final String REST_PARAM_NAME = "name";
    public static final String REST_PARAM_NOOFCOPIES = "noofcopies";
    public static final String REST_PARAM_RECORDSIZE = "recordsize";
    public static final String REST_PARAM_TOTALIOPS = "totaliops";
    public static final String REST_PARAM_LATENCY = "latency";
    public static final String REST_PARAM_BLOCKSIZE = "blocksize";
    public static final String REST_PARAM_GRACEALLOWED = "graceallowed";
    public static final String REST_PARAM_IOPS = "iops";
    public static final String REST_PARAM_THROUGHPUT = "throughput";
    public static final String REST_PARAM_MEMLIMIT = "memlimit";
    public static final String REST_PARAM_NETWORKSPEED = "networkspeed";
    public static final String REST_PARAM_TSMID = "tsmid";
    public static final String REST_PARAM_DATASETID = "datasetid";
    public static final String REST_PARAM_QOSGROUPID = "qosgroupid";
    public static final String REST_PARAM_DEDUPLICATION = "deduplication";
    public static final String REST_PARAM_COMPRESSION = "compression";
    public static final String REST_PARAM_SYNC = "sync";
    public static final String REST_PARAM_MOUNTPOINT = "mountpoint";
    public static final String REST_PARAM_CASESENSITIVITY = "casesensitivity";
    public static final String REST_PARAM_UNICODE = "unicode";
    public static final String REST_PARAM_PROTOCOLTYPE = "protocoltype";
    public static final String REST_PARAM_AUTHNETWORK = "authnetwork";
    public static final String REST_PARAM_MAPUSERSTOROOT = "mapuserstoroot";
    public static final String REST_PARAM_STORAGEID = "storageid";
    public static final String REST_PARAM_TPCONTROL = "tpcontrol";
    public static final String REST_PARAM_IOPSCONTROL = "iopscontrol";

    /**
     * Constants related to elastistor which are persisted in cloudstack
     * databases as keys.
     */
    public static final String ES_SUBNET = "essubnet";
    public static final String ES_INTERFACE = "estntinterface";
    public static final String ES_GATEWAY = "esdefaultgateway";
    public static final String ES_PROVIDER_NAME = "CloudByte";
    public static final String ES_ACCOUNT_ID = "esAccountId";
    public static final String ES_POOL_ID = "esPoolId";
    public static final String ES_ACCOUNT_NAME = "esAccountName";
    public static final String ES_STORAGE_IP = "esStorageIp";
    public static final String ES_STORAGE_PORT = "esStoragePort";
    public static final String ES_STORAGE_TYPE = "esStorageType";
    public static final String ES_MANAGEMENT_IP = "esMgmtIp";
    public static final String ES_MANAGEMENT_PORT = "esMgmtPort";
    public static final String ES_API_KEY = "esApiKey";
    public static final String ES_VOLUME_ID = "esVolumeId";
    public static final String ES_VOLUME_GROUP_ID = "esVolumeGroupId";
    public static final String ES_FILE_SYSTEM_ID = "esFilesystemId";

    /**
     * Values from configuration that are required for every invocation of
     * ElastiCenter API. These might in turn be saved as DB updates along with
     * above keys.
     */
    public static String s_esIPVAL = "";
    public static String s_esAPIKEYVAL = "";
    public static String s_esACCOUNTIDVAL = "";
    public static String s_esPOOLIDVAL = "";
    public static String s_esSUBNETVAL = "";
    public static String s_esINTERFACEVAL = "";
    public static String s_esGATEWAYVAL = "";

    /**
     * hardcoded constants for elastistor api calls.
     */
    private static final String ES_NOOFCOPIES_VAL = "1";
    private static final String ES_BLOCKSIZE_VAL = "4K";
    private static final String ES_LATENCY_VAL = "15";
    private static final String ES_GRACEALLOWED_VAL = "false";
    private static final String ES_MEMLIMIT_VAL = "0";
    private static final String ES_NETWORKSPEED_VAL = "0";
    private static final String ES_DEDUPLICATION_VAL = "off";
    private static final String ES_COMPRESSION_VAL = "off";
    private static final String ES_CASESENSITIVITY_VAL = "sensitive";
    private static final String ES_READONLY_VAL = "off";
    private static final String ES_UNICODE_VAL = "off";
    private static final String ES_AUTHNETWORK_VAL = "all";
    private static final String ES_MAPUSERSTOROOT_VAL = "yes";
    private static final String ES_SYNC_VAL = "always";
    private static final String ES_TPCONTROL_VAL = "false";
    private static final String ES_IOPSCONTROL_VAL = "true";

    /**
     * Private constructor s.t. its never instantiated.
     */
    private ElastistorUtil() {

    }

    /**
     * This intializes a new jersey restclient for http call with elasticenter
     */
    public static ElastiCenterClient getElastistorRestClient() {
        ElastiCenterClient restclient = null;
        try {
            String ip = getConfigurationDao().getValue("cloudbyte.management.ip");
            String apikey = getConfigurationDao().getValue("cloudbyte.management.apikey");

            if (ip == null) {
                throw new CloudRuntimeException("set the value of cloudbyte.management.ip in global settings");
            }
            if (apikey == null) {
                throw new CloudRuntimeException("set the value of cloudbyte.management.apikey in global settings");
            }

            restclient = new ElastiCenterClient(ip, apikey);

        } catch (InvalidCredentialsException e) {
            throw new CloudRuntimeException("InvalidCredentialsException:" + e.getMessage(), e);
        } catch (InvalidParameterException e) {
            throw new CloudRuntimeException("InvalidParameterException:" + e.getMessage(), e);
        } catch (SSLHandshakeException e) {
            throw new CloudRuntimeException("SSLHandshakeException:" + e.getMessage(), e);
        } catch (ServiceUnavailableException e) {
            throw new CloudRuntimeException("ServiceUnavailableException:" + e.getMessage(), e);
        }
        return restclient;
    }

    public static void setElastistorApiKey(String value) {
        s_esAPIKEYVAL = value;
    }

    public static void setElastistorManagementIp(String value) {
        s_esIPVAL = value;
    }

    public static void setElastistorPoolId(String value) {
        s_esPOOLIDVAL = value;
    }

    public static void setElastistorAccountId(String value) {
        s_esACCOUNTIDVAL = value;
    }

    public static void setElastistorGateway(String value) {
        s_esGATEWAYVAL = value;
    }

    public static void setElastistorInterface(String value) {
        s_esINTERFACEVAL = value;
    }

    public static void setElastistorSubnet(String value) {
        s_esSUBNETVAL = value;
    }

    /**
     * This creates a new Account in Elasticenter for the given Domain Name.
     *
     * @return
     */
    public static String getElastistorAccountId(String domainName) throws Throwable {

        ListAccountResponse listAccountResponse = ListElastistorAccounts();

        if (listAccountResponse.getAccounts().getCount() != 0) {
            int i;
            // check weather a account in elasticenter with given Domain name is
            // already present in the list of accounts
            for (i = 0; i < listAccountResponse.getAccounts().getCount(); i++) {
                if (domainName.equals(listAccountResponse.getAccounts().getAccount(i).getName())) {
                    return listAccountResponse.getAccounts().getAccount(i).getUuid();
                }
            }

            // if no account matches the give Domain Name , create one with the
            // Domain name
            CreateAccountResponse createAccountResponse = createElastistorAccount(domainName);
            return createAccountResponse.getAccount().getUuid();

        } else {
            // if no account is present in the elasticenter create one
            CreateAccountResponse createAccountResponse = createElastistorAccount(domainName);
            return createAccountResponse.getAccount().getUuid();
        }
    }

    /**
     * This creates a new tenant storage machine(TSM) for the given storagepool
     * ip in elastistor.
     *
     * @param domainName
     *            TODO
     */
    public static Tsm createElastistorTsm(String storagePoolName, String storageIp, Long capacityBytes, Long capacityIops, String domainName) throws Throwable {

        String totalthroughput = String.valueOf(capacityIops * 4);
        String totaliops = String.valueOf(capacityIops);

        String quotasize = convertCapacityBytes(capacityBytes);

        CreateTsmCmd createTsmCmd = new CreateTsmCmd();

        if (null != ElastistorUtil.s_esACCOUNTIDVAL)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ACCOUNTID, domainName);
        if (null != totalthroughput)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_TSM_THROUGHPUT, totalthroughput);
        if (null != ElastistorUtil.s_esPOOLIDVAL)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_POOLID, ElastistorUtil.s_esPOOLIDVAL);
        if (null != storagePoolName)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_NAME, storagePoolName);
        if (null != quotasize)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_QUOTA_SIZE, quotasize);
        if (null != storageIp)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_IPADDRESS, storageIp);
        if (null != ElastistorUtil.s_esSUBNETVAL)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_SUBNET, ElastistorUtil.s_esSUBNETVAL);
        if (null != ElastistorUtil.s_esGATEWAYVAL)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_GATEWAY, ElastistorUtil.s_esGATEWAYVAL);
        if (null != ElastistorUtil.s_esINTERFACEVAL)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_INTERFACE, ElastistorUtil.s_esINTERFACEVAL);
        if (null != ElastistorUtil.ES_NOOFCOPIES_VAL)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_NOOFCOPIES, ElastistorUtil.ES_NOOFCOPIES_VAL);
        if (null != ElastistorUtil.ES_BLOCKSIZE_VAL)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_RECORDSIZE, ElastistorUtil.ES_BLOCKSIZE_VAL);
        if (null != totaliops)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_TOTALIOPS, totaliops);
        if (null != ElastistorUtil.ES_LATENCY_VAL)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_LATENCY, ElastistorUtil.ES_LATENCY_VAL);
        if (null != ElastistorUtil.ES_BLOCKSIZE_VAL)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_BLOCKSIZE, ElastistorUtil.ES_BLOCKSIZE_VAL);
        if (null != ElastistorUtil.ES_GRACEALLOWED_VAL)
            createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_GRACEALLOWED, ElastistorUtil.ES_GRACEALLOWED_VAL);

        CreateTsmCmdResponse createTsmCmdResponse;
        Tsm tsm = null;
        try {
            createTsmCmdResponse = (CreateTsmCmdResponse) getElastistorRestClient().executeCommand(createTsmCmd);

            if (createTsmCmdResponse.getJobid() == null) {
                throw new CloudRuntimeException("tsm creation failed , contact elatistor admin");
            } else {
                tsm = queryAsyncTsmJobResult(createTsmCmdResponse.getJobid());
                if (tsm == null) {
                    throw new CloudRuntimeException("tsm queryAsync failed , contact elatistor admin");
                }
            }
            return tsm;
        } catch (Exception e) {
            throw new CloudRuntimeException("tsm creation failed , contact elatistor admin" + e.toString());
        }

    }

    /**
     * This creates the specified volume on the created tsm.
     */
    public static FileSystem createElastistorVolume(String volumeName, String tsmid, Long capacityBytes, Long capacityIops, String protocoltype, String mountpoint) throws Throwable {

        String datasetid;
        String qosgroupid;
        String VolumeName = volumeName;
        String totaliops = String.valueOf(capacityIops);
        //String totalthroughput = String.valueOf(capacityIops * 4);
        String totalthroughput = "0";

        String quotasize = convertCapacityBytes(capacityBytes);

        AddQosGroupCmd addQosGroupCmd = new AddQosGroupCmd();

        ListTsmsResponse listTsmsResponse = listTsm(tsmid);

        tsmid = listTsmsResponse.getTsms().getTsm(0).getUuid();
        datasetid = listTsmsResponse.getTsms().getTsm(0).getDatasetid();

        if (null != VolumeName)
            addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_NAME, "QOS_" + VolumeName);
        if (null != totaliops)
            addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_IOPS, totaliops);
        if (null != ElastistorUtil.ES_LATENCY_VAL)
            addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_LATENCY, ElastistorUtil.ES_LATENCY_VAL);
        if (null != totalthroughput)
            addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_THROUGHPUT, totalthroughput);
        if (null != ElastistorUtil.ES_MEMLIMIT_VAL)
            addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_MEMLIMIT, ElastistorUtil.ES_MEMLIMIT_VAL);
        if (null != ElastistorUtil.ES_NETWORKSPEED_VAL)
            addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_NETWORKSPEED, ElastistorUtil.ES_NETWORKSPEED_VAL);
        if (null != tsmid)
            addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_TSMID, tsmid);
        if (null != datasetid)
            addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_DATASETID, datasetid);
        if (null != ElastistorUtil.ES_GRACEALLOWED_VAL)
            addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_GRACEALLOWED, ElastistorUtil.ES_GRACEALLOWED_VAL);
        if (null != ElastistorUtil.ES_IOPSCONTROL_VAL)
            addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_IOPSCONTROL, ElastistorUtil.ES_IOPSCONTROL_VAL);
        if (null != ElastistorUtil.ES_TPCONTROL_VAL)
            addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_TPCONTROL, ElastistorUtil.ES_TPCONTROL_VAL);

        AddQosGroupCmdResponse addQosGroupCmdResponse = (AddQosGroupCmdResponse) getElastistorRestClient().executeCommand(addQosGroupCmd);

        if (addQosGroupCmdResponse.getQoSGroup().getUuid() == null) {

            throw new CloudRuntimeException("adding qos group failed , contact elatistor admin");

        }

        else {

            CreateVolumeCmd createVolumeCmd = new CreateVolumeCmd();

            qosgroupid = addQosGroupCmdResponse.getQoSGroup().getUuid();

            // if (null !=
            // ElastistorUtil.s_esACCOUNTIDVAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ACCOUNTID,ElastistorUtil.s_esACCOUNTIDVAL);
            if (null != qosgroupid)
                createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_QOSGROUPID, qosgroupid);
            if (null != tsmid)
                createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_TSMID, tsmid);
            // if (null !=
            // ElastistorUtil.s_esPOOLIDVAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_POOLID,ElastistorUtil.s_esPOOLIDVAL);
            if (null != VolumeName)
                createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_NAME, VolumeName);
            if (null != quotasize)
                createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_QUOTA_SIZE, quotasize);
            if (protocoltype.equalsIgnoreCase("nfs")) {
                if (null != ElastistorUtil.ES_BLOCKSIZE_VAL)
                    createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_BLOCKSIZE, ElastistorUtil.ES_BLOCKSIZE_VAL);
                if (null != ElastistorUtil.ES_BLOCKSIZE_VAL)
                    createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_RECORDSIZE, ElastistorUtil.ES_BLOCKSIZE_VAL);
            } else {
                if (null != ElastistorUtil.ES_BLOCKSIZE_VAL)
                    createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_BLOCKSIZE, "512B");
                if (null != ElastistorUtil.ES_BLOCKSIZE_VAL)
                    createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_RECORDSIZE, "512B");
            }
            if (null != ElastistorUtil.ES_DEDUPLICATION_VAL)
                createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_DEDUPLICATION, ElastistorUtil.ES_DEDUPLICATION_VAL);
            if (null != ElastistorUtil.ES_SYNC_VAL)
                createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_SYNC, ElastistorUtil.ES_SYNC_VAL);
            if (null != ElastistorUtil.ES_COMPRESSION_VAL)
                createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_COMPRESSION, ElastistorUtil.ES_COMPRESSION_VAL);
            // if (null !=
            // ElastistorUtil.ES_NOOFCOPIES_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_NOOFCOPIES,
            // ElastistorUtil.ES_NOOFCOPIES_VAL);
            createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_MOUNTPOINT, mountpoint);
            // if (null !=
            // ElastistorUtil.ES_CASESENSITIVITY_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_CASESENSITIVITY,
            // ElastistorUtil.ES_CASESENSITIVITY_VAL);
            // if (null !=
            // ElastistorUtil.ES_READONLY_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_READONLY,
            // ElastistorUtil.ES_READONLY_VAL);
            if (null != datasetid)
                createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_DATASETID, datasetid);
            // if (null !=
            // ElastistorUtil.ES_UNICODE_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_UNICODE,
            // ElastistorUtil.ES_UNICODE_VAL);
            createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_PROTOCOLTYPE, protocoltype);
            // if (null !=
            // ElastistorUtil.ES_AUTHNETWORK_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_AUTHNETWORK,
            // ElastistorUtil.ES_AUTHNETWORK_VAL);
            // if (null !=
            // ElastistorUtil.ES_MAPUSERSTOROOT_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_MAPUSERSTOROOT,
            // ElastistorUtil.ES_MAPUSERSTOROOT_VAL);

            CreateVolumeCmdResponse createVolumeCmdResponse;
            FileSystem volume = null;
            FileSystem fileSystem = null;

            try {
                createVolumeCmdResponse = (CreateVolumeCmdResponse) getElastistorRestClient().executeCommand(createVolumeCmd);

                if (createVolumeCmdResponse.getJobid() == null) {

                    throw new CloudRuntimeException("creating volume failed , contact elatistor admin");

                } else {
                    volume = queryAsyncVolumeJobResult(createVolumeCmdResponse.getJobid());
                    if (volume == null) {
                        throw new CloudRuntimeException("tsm queryAsync failed , contact elatistor admin");
                    } else {
                        if (protocoltype.equalsIgnoreCase("nfs")) {
                            fileSystem = updateNfsService(volume.getUuid());

                        } else {
                            fileSystem = updateIscsiService(volume.getUuid());
                        }
                    }
                }
                return fileSystem;
            } catch (Exception e) {
                throw new CloudRuntimeException("creating volume failed , contact elatistor admin", e);
            }

        }

    }

    public static FileSystem updateNfsService(String volumeid) throws Throwable {

        FileSystem fileSystem = null;

        String datasetid = updateElastistorNfsVolume(volumeid);

        if (datasetid == null) {
            throw new CloudRuntimeException("Updating Nfs Volume Failed");
        } else {

            fileSystem = listVolume(datasetid);
            if (fileSystem == null) {
                throw new CloudRuntimeException("Volume Creation failed : List Filesystem failed");
            }
        }
        return fileSystem;

    }

    public static FileSystem updateIscsiService(String volumeid) throws Throwable {

        Volumeiscsioptions volumeiscsioptions = null;
        FileSystem fileSystem = null;
        String accountId;

        fileSystem = listVolume(volumeid);

        accountId = fileSystem.getAccountid();

        volumeiscsioptions = updateElastistorIscsiVolume(volumeid, accountId);

        if (volumeiscsioptions == null) {
            throw new CloudRuntimeException("Updating Iscsi Volume Failed");
        } else {

            fileSystem = listVolume(volumeiscsioptions.getVolumeid());
            if (fileSystem == null) {
                throw new CloudRuntimeException("Volume Creation failed : List Filesystem failed");
            }
        }
        return fileSystem;

    }

    public static String updateElastistorNfsVolume(String volumeid) throws Throwable {

        NfsServiceCmd nfsServiceCmd = new NfsServiceCmd();

        nfsServiceCmd.putCommandParameter("datasetid", volumeid);
        nfsServiceCmd.putCommandParameter("authnetwork", "all");
        nfsServiceCmd.putCommandParameter("managedstate", "true");
        nfsServiceCmd.putCommandParameter("alldirs", "yes");
        nfsServiceCmd.putCommandParameter("mapuserstoroot", "yes");
        nfsServiceCmd.putCommandParameter("readonly", "no");

        NfsServiceResponse nfsServiceResponse = (NfsServiceResponse) getElastistorRestClient().executeCommand(nfsServiceCmd);

        if (nfsServiceResponse.getNfsService().getUuid() != null) {

            UpdateControllerCmd controllerCmd = new UpdateControllerCmd();

            controllerCmd.putCommandParameter("nfsid", nfsServiceResponse.getNfsService().getUuid());
            controllerCmd.putCommandParameter("type", "configurenfs");
            controllerCmd.putCommandParameter("id", nfsServiceResponse.getNfsService().getControllerid());

            UpdateControllerResponse controllerResponse = (UpdateControllerResponse) getElastistorRestClient().executeCommand(controllerCmd);

            if (controllerResponse.getController().getUuid() != null) {
                s_logger.info("updated nfs service to ALL");
                return nfsServiceResponse.getNfsService().getDatasetid();
            } else {
                throw new CloudRuntimeException("Updating Nfs Volume Failed");
            }

        }
        return null;
    }

    public static Volumeiscsioptions updateElastistorIscsiVolume(String volumeid, String accountid) throws Throwable {

        // now listing the iscsi volume service group to get iscsi id

        ListVolumeiSCSIServiceCmd listVolumeiSCSIServiceCmd = new ListVolumeiSCSIServiceCmd();

        if (null != volumeid)
            listVolumeiSCSIServiceCmd.putCommandParameter(ElastistorUtil.REST_PARAM_STORAGEID, volumeid);

        ListVolumeiSCSIServiceResponse volumeiSCSIServiceResponse = (ListVolumeiSCSIServiceResponse) getElastistorRestClient().executeCommand(listVolumeiSCSIServiceCmd);

        String iscsiId = volumeiSCSIServiceResponse.getIscsiVolume().getIscsiVolume(0).getUuid();
        String AG_Id = volumeiSCSIServiceResponse.getIscsiVolume().getIscsiVolume(0).getAg_id();

        // now listing the initiator group to get initiator id

        ListiSCSIInitiatorCmd initiatorCmd = new ListiSCSIInitiatorCmd();

        if (null != volumeid)
            initiatorCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ACCOUNTID, accountid);

        ListiSCSIInitiatorResponse initiatorResponse = (ListiSCSIInitiatorResponse) getElastistorRestClient().executeCommand(initiatorCmd);

        String IG_Id;
        if (initiatorResponse.getIInitiator().getInterface(0).getInitiatorgroup().equalsIgnoreCase("ALL")) {
            IG_Id = initiatorResponse.getIInitiator().getInterface(0).getUuid();
        } else {
            IG_Id = initiatorResponse.getIInitiator().getInterface(1).getUuid();
        }

        if (iscsiId != null) {

            UpdateVolumeiSCSIServiceCmd updateVolumeiSCSIServiceCmd = new UpdateVolumeiSCSIServiceCmd();

            if (null != volumeid)
                updateVolumeiSCSIServiceCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ID, iscsiId);
            if (null != volumeid)
                updateVolumeiSCSIServiceCmd.putCommandParameter("status", "true");
            if (null != volumeid)
                updateVolumeiSCSIServiceCmd.putCommandParameter("igid", IG_Id);
            if (null != volumeid)
                updateVolumeiSCSIServiceCmd.putCommandParameter("authgroupid", AG_Id);
            if (null != volumeid)
                updateVolumeiSCSIServiceCmd.putCommandParameter("initialdigest", "Auto");
            if (null != volumeid)
                updateVolumeiSCSIServiceCmd.putCommandParameter("queuedepth", "32");

            UpdateVolumeiSCSIServiceCmdResponse cmdResponse = (UpdateVolumeiSCSIServiceCmdResponse) getElastistorRestClient().executeCommand(updateVolumeiSCSIServiceCmd);

            if (cmdResponse.getVolumeiscsioptions().getVolumeid() == null) {
                throw new CloudRuntimeException("Updating Iscsi Volume Failed");
            }
            return cmdResponse.getVolumeiscsioptions();
        }
        return null;

    }

    /**
     * This deletes both the volume and the tsm in elastistor.
     */
    public static boolean deleteElastistorTsm(String tsmid, boolean managed) throws Throwable {

        if (!managed) {

            s_logger.info("elastistor pool is NOT a managed storage , hence deleting the volume then tsm");

            String esvolumeid = null;
            ListTsmsResponse listTsmsResponse = listTsm(tsmid);

            if (listTsmsResponse.getTsmsCount() != 0) {

                if (listTsmsResponse.getTsms().getTsm(0).checkvolume()) {
                    esvolumeid = listTsmsResponse.getTsms().getTsm(0).getVolumeProperties(0).getid();
                    DeleteVolumeResponse deleteVolumeResponse = deleteVolume(esvolumeid, null);

                    if (deleteVolumeResponse != null) {
                        String jobid = deleteVolumeResponse.getJobId();
                        int jobstatus = queryAsyncJobResult(jobid);

                        if (jobstatus == 1) {
                            s_logger.info("elastistor volume successfully deleted");
                        } else {
                            s_logger.info("now farce deleting the volume");

                            while (jobstatus != 1) {
                                DeleteVolumeResponse deleteVolumeResponse1 = deleteVolume(esvolumeid, "true");

                                if (deleteVolumeResponse1 != null) {
                                    String jobid1 = deleteVolumeResponse1.getJobId();
                                    jobstatus = queryAsyncJobResult(jobid1);
                                }
                            }
                            s_logger.info("elastistor volume successfully deleted");
                        }
                    }
                } else {
                    s_logger.info("no volume present in on the given tsm");
                }

            }
        }

        s_logger.info("now trying to delete elastistor tsm");

        if (tsmid != null) {
            DeleteTsmCmd deleteTsmCmd = new DeleteTsmCmd();
            deleteTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ID, tsmid);
            DeleteTsmResponse deleteTsmResponse = (DeleteTsmResponse) getElastistorRestClient().executeCommand(deleteTsmCmd);

            if (deleteTsmResponse != null) {
                String jobstatus = deleteTsmResponse.getJobStatus();

                if (jobstatus.equalsIgnoreCase("true")) {
                    s_logger.info("deletion of elastistor tsm successful");
                    return true;
                } else {
                    s_logger.info("failed to delete elastistor tsm");
                    return false;
                }
            } else {
                s_logger.info("elastistor tsm id not present");
            }
        }
        s_logger.info("tsm id is null");
        return false;

        /*
         * else { s_logger.error("no volume is present in the tsm"); } } else {
         * s_logger.error(
         * "List tsm failed, no tsm present in the eastistor for the given IP "
         * ); return false; } return false;
         */

    }

    public static boolean deleteElastistorVolume(String esvolumeid) throws Throwable {

        FileSystem fileSystem = listVolume(esvolumeid);

        if (fileSystem != null) {
            DeleteVolumeResponse deleteVolumeResponse = deleteVolume(esvolumeid, null);

            if (deleteVolumeResponse != null) {
                String jobid = deleteVolumeResponse.getJobId();
                int jobstatus = queryAsyncJobResult(jobid);

                if (jobstatus == 1) {
                    s_logger.info("elastistor volume successfully deleted");
                    return true;
                } else {
                    s_logger.info("now force deleting the volume");

                    while (jobstatus != 1) {
                        DeleteVolumeResponse deleteVolumeResponse1 = deleteVolume(esvolumeid, "true");

                        if (deleteVolumeResponse1 != null) {
                            String jobid1 = deleteVolumeResponse1.getJobId();
                            jobstatus = queryAsyncJobResult(jobid1);
                        }
                    }
                    s_logger.info("elastistor volume successfully deleted");
                    return true;
                }
            } else {
                s_logger.info("the given volume is not present on elastistor, datasetrespone is NULL");
                return false;
            }
        } else {
            s_logger.info("the given volume is not present on elastistor");
            return false;
        }

    }

    /**
     * This give a json response containing the list of Interface's in
     * elastistor.
     */

    public static ListInterfacesResponse ListElastistorInterfaces(String controllerid) throws Throwable {

        ListInterfacesCmd listInterfacesCmd = new ListInterfacesCmd();
        listInterfacesCmd.putCommandParameter("controllerid", controllerid);

        ListInterfacesResponse interfacesResponse = (ListInterfacesResponse) getElastistorRestClient().executeCommand(listInterfacesCmd);

        if (interfacesResponse != null && interfacesResponse.getInterfaces() != null) {
            return interfacesResponse;
        } else {
            throw new CloudRuntimeException("There are no elastistor interfaces.");
        }
    }

    /**
     * This give a json response containing the list of Accounts's in
     * elastistor.
     */

    public static CreateAccountResponse createElastistorAccount(String domainName) throws Throwable {

        CreateAccountCmd createAccountCmd = new CreateAccountCmd();

        createAccountCmd.putCommandParameter("name", domainName);
        CreateAccountResponse createAccountResponse = (CreateAccountResponse) getElastistorRestClient().executeCommand(createAccountCmd);

        if (createAccountResponse != null) {
            return createAccountResponse;
        } else {
            throw new CloudRuntimeException("Creating Elastistor Account failed");
        }

    }

    /**
     * This give a json response containing the list of Accounts's in
     * elastistor.
     */

    public static ListAccountResponse ListElastistorAccounts() throws Throwable {

        ListAccountsCmd listAccountsCmd = new ListAccountsCmd();

        ListAccountResponse accountResponse = (ListAccountResponse) getElastistorRestClient().executeCommand(listAccountsCmd);

        if (accountResponse != null) {
            return accountResponse;
        } else {
            throw new CloudRuntimeException("List Elastistor Account failed");
        }

    }

    /**
     * This give a json response containing the list of Pool's in elastistor.
     */

    public static ListPoolsResponse ListElastistorPools() throws Throwable {

        ListPoolsCmd listPoolsCmd = new ListPoolsCmd();

        ListPoolsResponse listPoolsResponse = (ListPoolsResponse) getElastistorRestClient().executeCommand(listPoolsCmd);

        if (listPoolsResponse != null) {
            return listPoolsResponse;
        } else {
            throw new CloudRuntimeException("List Elastistor pool failed");
        }

    }

    /**
     * This give a json response containing the list of tsm's in elastistor.
     */
    private static ListTsmsResponse listTsm(String uuid) throws Throwable {

        ListTsmCmd listTsmCmd = new ListTsmCmd();

        listTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ID, uuid);

        ListTsmsResponse listTsmsResponse = (ListTsmsResponse) getElastistorRestClient().executeCommand(listTsmCmd);

        return listTsmsResponse;
    }

    /**
     * This give a json response containing the list of Volume in elastistor.
     */
    public static FileSystem listVolume(String uuid) throws Throwable {

        ListFileSystemCmd listFileSystemCmd = new ListFileSystemCmd();

        listFileSystemCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ID, uuid);

        ListFileSystemResponse listFileSystemResponse = (ListFileSystemResponse) getElastistorRestClient().executeCommand(listFileSystemCmd);

        return listFileSystemResponse.getFilesystems().getFileSystem(0);
    }

    private static DeleteVolumeResponse deleteVolume(String esvolumeid, String forcedelete) throws Throwable {

        DeleteVolumeCmd deleteVolumeCmd = new DeleteVolumeCmd();

        deleteVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ID, esvolumeid);
        deleteVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_FORECEDELETE, forcedelete);

        DeleteVolumeResponse deleteVolumeResponse = (DeleteVolumeResponse) getElastistorRestClient().executeCommand(deleteVolumeCmd);

        return deleteVolumeResponse;
    }

    private static int queryAsyncJobResult(String jobid) throws Throwable {

        QueryAsyncJobResultCmd asyncJobResultCmd = new QueryAsyncJobResultCmd();
        ElastiCenterClient restclient = getElastistorRestClient();

        asyncJobResultCmd.putCommandParameter(ElastistorUtil.REST_PARAM_JOBID, jobid);

        QueryAsyncJobResultResponse asyncJobResultResponse = (QueryAsyncJobResultResponse) restclient.executeCommand(asyncJobResultCmd);

        if (asyncJobResultResponse != null) {
            int jobstatus = asyncJobResultResponse.getAsync().getJobStatus();

            while (jobstatus == 0) {

                QueryAsyncJobResultResponse jobResultResponse = (QueryAsyncJobResultResponse) restclient.executeCommand(asyncJobResultCmd);

                jobstatus = jobResultResponse.getAsync().getJobStatus();
            }
            return jobstatus;
        }
        return 0;

    }

    private static Tsm queryAsyncTsmJobResult(String jobid) throws Throwable {

        QueryAsyncJobResultCmd asyncJobResultCmd = new QueryAsyncJobResultCmd();
        ElastiCenterClient restclient = getElastistorRestClient();

        asyncJobResultCmd.putCommandParameter(ElastistorUtil.REST_PARAM_JOBID, jobid);

        QueryAsyncJobResultResponse asyncJobResultResponse = (QueryAsyncJobResultResponse) restclient.executeCommand(asyncJobResultCmd);

        if (asyncJobResultResponse != null) {
            int jobstatus = asyncJobResultResponse.getAsync().getJobStatus();
            Tsm tsm = null;
            while (jobstatus == 0) {

                asyncJobResultResponse = (QueryAsyncJobResultResponse) restclient.executeCommand(asyncJobResultCmd);

                jobstatus = asyncJobResultResponse.getAsync().getJobStatus();

            }
            if (jobstatus == 1) {
                tsm = asyncJobResultResponse.getAsync().getJobResult().getTsm();
                return tsm;
            }
        }
        return null;

    }

    private static FileSystem queryAsyncVolumeJobResult(String jobid) throws Throwable {

        QueryAsyncJobResultCmd asyncJobResultCmd = new QueryAsyncJobResultCmd();
        ElastiCenterClient restclient = getElastistorRestClient();

        asyncJobResultCmd.putCommandParameter(ElastistorUtil.REST_PARAM_JOBID, jobid);

        QueryAsyncJobResultResponse asyncJobResultResponse = (QueryAsyncJobResultResponse) restclient.executeCommand(asyncJobResultCmd);

        if (asyncJobResultResponse != null) {
            int jobstatus = asyncJobResultResponse.getAsync().getJobStatus();
            FileSystem volume = null;
            while (jobstatus == 0) {

                asyncJobResultResponse = (QueryAsyncJobResultResponse) restclient.executeCommand(asyncJobResultCmd);

                jobstatus = asyncJobResultResponse.getAsync().getJobStatus();

            }
            if (jobstatus == 1) {
                volume = asyncJobResultResponse.getAsync().getJobResult().getVolume();
                return volume;
            }
        }
        return null;

    }

    /**
     * this method converts the long capacitybytes to string format, which is
     * feasible for elastistor rest api 214748364800 = 200G.
     */
    private static String convertCapacityBytes(Long capacityBytes) {

        if ((1099511627776L) > capacityBytes && (capacityBytes > (1073741824))) {
            return (String.valueOf(capacityBytes / (1024 * 1024 * 1024)) + "G");
        } else {
            int temp1 = (int) (capacityBytes / (1024 * 1024 * 1024));
            int temp2 = temp1 / 1024;
            return (String.valueOf(temp2) + "T");
        }
    }

    static interface ElastiCenterCommand {

        /*
         * Returns the command string to be sent to the ElastiCenter
         */
        public String getCommandName();

        /*
         * Utility method to allow the client to validate the input parameters
         * before sending to the ElastiCenter.
         *
         * This command will be executed by the ElastiCenterClient only this
         * method returns true.
         */
        public boolean validate();

        /*
         * Returns the query parameters that have to be passed to execute the
         * command.
         *
         * Returns null if there are query parameters associated with the
         * command
         */
        public MultivaluedMap<String, String> getCommandParameters();

        /*
         * Adds new key-value pair to the query paramters lists.
         */
        public void putCommandParameter(String key, String value);

        /*
         * Return an instance of the Response Object Type.
         *
         * Return null if no response is expected.
         */
        public Object getResponseObject();
    }

    private static class BaseCommand implements ElastiCenterCommand {

        private String commandName = null;
        private MultivaluedMap<String, String> commandParameters = null;
        private Object responseObject = null;

        /*
         * Enforce the Commands to be initialized with command name and optional
         * response object
         */
        protected BaseCommand(String cmdName, Object responseObj) {
            commandName = cmdName;
            responseObject = responseObj;
        }

        @Override
        public String getCommandName() {
            return commandName;
        }

        @Override
        public boolean validate() {
            // TODO This method can be extended to do some generic
            // validations.
            return true;
        }

        @Override
        public MultivaluedMap<String, String> getCommandParameters() {
            return commandParameters;
        }

        @Override
        public void putCommandParameter(String key, String value) {
            if (null == commandParameters) {
                commandParameters = new MultivaluedMapImpl();
            }
            commandParameters.add(key, value);
        }

        @Override
        public Object getResponseObject() {
            return responseObject;
        }

    }

    /**
     * this is a rest client which is used to call the http rest calls to
     * elastistor
     *
     * @author punith
     *
     */
    private static final class ElastiCenterClient {

        public static boolean debug = false;

        private boolean initialized = false;

        private String apiKey = null;
        private String elastiCenterAddress = null;
        private String responseType = "json";
        private boolean ignoreSSLCertificate = false;

        private String restprotocol = "https://";
        private String restpath = "/client/api";
        private String restdefaultcommand = "listCapabilities";

        private String queryparamcommand = "command";
        private String queryparamapikey = "apikey";
        private String queryparamresponse = "response";

        public ElastiCenterClient(String address, String key) throws InvalidCredentialsException, InvalidParameterException, SSLHandshakeException, ServiceUnavailableException {
            elastiCenterAddress = address;
            apiKey = key;
            initialize();
        }

        public void initialize() throws InvalidParameterException, SSLHandshakeException, InvalidCredentialsException, ServiceUnavailableException {

            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new InvalidParameterException("Unable to initialize. Please specify a valid API Key.");
            }

            if (elastiCenterAddress == null || elastiCenterAddress.trim().isEmpty()) {
                // TODO : Validate the format, like valid IP address or
                // hostname.
                throw new InvalidParameterException("Unable to initialize. Please specify a valid ElastiCenter IP Address or Hostname.");
            }

            if (ignoreSSLCertificate) {
                // Create a trust manager that does not validate certificate
                // chains
                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                } };

                HostnameVerifier hv = new HostnameVerifier() {
                    @Override
                    public boolean verify(String urlHostName, SSLSession session) {
                        return true;
                    }
                };

                // Install the all-trusting trust manager
                try {
                    SSLContext sc = SSLUtils.getSSLContext();
                    sc.init(null, trustAllCerts, new SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(new SecureSSLSocketFactory(sc));
                    HttpsURLConnection.setDefaultHostnameVerifier(hv);
                } catch (Exception e) {
                    ;
                }
            }

            ListCapabilitiesResponse listCapabilitiesResponse = null;
            try {
                initialized = true;
                listCapabilitiesResponse = (ListCapabilitiesResponse) executeCommand(restdefaultcommand, null, new ListCapabilitiesResponse());

            } catch (Throwable t) {
                initialized = false;
                if (t instanceof InvalidCredentialsException) {
                    throw (InvalidCredentialsException) t;
                } else if (t instanceof ServiceUnavailableException) {
                    throw (ServiceUnavailableException) t;
                } else if (t.getCause() instanceof SSLHandshakeException) {
                    throw new SSLHandshakeException("Unable to initialize. An untrusted SSL Certificate was received from " + elastiCenterAddress
                            + ". Please verify your truststore or configure ElastiCenterClient to skip the SSL Validation. ");
                } else if (t.getCause() instanceof ConnectException) {
                    throw new ServiceUnavailableException("Unable to initialize. Failed to connect to " + elastiCenterAddress
                            + ". Please verify the IP Address, Network Connectivity and ensure that Services are running on the ElastiCenter Server. ");
                }
                throw new ServiceUnavailableException("Unable to initialize. Please contact your ElastiCenter Administrator. Exception " + t.getMessage());
            }

            if (null == listCapabilitiesResponse || null == listCapabilitiesResponse.getCapabilities() || null == listCapabilitiesResponse.getCapabilities().getVersion()) {
                initialized = false;
                throw new ServiceUnavailableException("Unable to execute command on the server");
            }

        }

        public Object executeCommand(ElastiCenterCommand cmd) throws Throwable {
            return executeCommand(cmd.getCommandName(), cmd.getCommandParameters(), cmd.getResponseObject());
        }

        public Object executeCommand(String command, MultivaluedMap<String, String> params, Object responeObj) throws Throwable {

            if (!initialized) {
                throw new IllegalStateException("Error : ElastiCenterClient is not initialized.");
            }

            if (command == null || command.trim().isEmpty()) {
                throw new InvalidParameterException("No command to execute.");
            }

            try {
                ClientConfig config = new DefaultClientConfig();
                Client client = Client.create(config);
                WebResource webResource = client.resource(UriBuilder.fromUri(restprotocol + elastiCenterAddress + restpath).build());

                MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
                queryParams.add(queryparamapikey, apiKey);
                queryParams.add(queryparamresponse, responseType);

                queryParams.add(queryparamcommand, command);

                if (null != params) {
                    for (String key : params.keySet()) {
                        queryParams.add(key, params.getFirst(key));
                    }
                }
                if (debug) {
                    System.out.println("Command Sent " + command + " : " + queryParams);
                }
                ClientResponse response = webResource.queryParams(queryParams).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);

                if (response.getStatus() >= 300) {
                    if (debug)
                        System.out.println("ElastiCenter returned error code : " + response.getStatus());
                    if (401 == response.getStatus()) {
                        throw new InvalidCredentialsException("Please specify a valid API Key.");
                    } else if (431 == response.getStatus()) {
                        throw new InvalidParameterException(response.getHeaders().getFirst("X-Description"));
                    } else if (432 == response.getStatus()) {
                        throw new InvalidParameterException(command + " does not exist on the ElastiCenter server.  Please specify a valid command or contact your ElastiCenter Administrator.");
                    } else {
                        throw new ServiceUnavailableException("Internal Error. Please contact your ElastiCenter Administrator.");
                    }
                } else if (null != responeObj) {
                    String jsonResponse = response.getEntity(String.class);
                    if (debug) {
                        System.out.println("Command Response : " + jsonResponse);
                    }
                    Gson gson = new Gson();
                    return gson.fromJson(jsonResponse, responeObj.getClass());
                } else {
                    return "Success";
                }
            } catch (Throwable t) {
                throw t;
            }
        }
    }

    /**
     * these are the list of Elastistor rest commands being called from the
     * plugin.
     */
    private static final class CreateTsmCmd extends BaseCommand {

        public CreateTsmCmd() {
            super("createTsm", new CreateTsmCmdResponse());

        }

    }

    private static final class AddQosGroupCmd extends BaseCommand {

        public AddQosGroupCmd() {

            super("addQosGroup", new AddQosGroupCmdResponse());

        }

    }

    private static final class CreateVolumeCmd extends BaseCommand {

        public CreateVolumeCmd() {
            super("createVolume", new CreateVolumeCmdResponse());

        }

    }

    private static final class ListVolumeiSCSIServiceCmd extends BaseCommand {

        public ListVolumeiSCSIServiceCmd() {
            super("listVolumeiSCSIService", new ListVolumeiSCSIServiceResponse());

        }

    }

    private static final class ListiSCSIInitiatorCmd extends BaseCommand {

        public ListiSCSIInitiatorCmd() {
            super("listiSCSIInitiator", new ListiSCSIInitiatorResponse());

        }

    }

    private static final class NfsServiceCmd extends BaseCommand {

        public NfsServiceCmd() {
            super("nfsService", new NfsServiceResponse());

        }

    }

    private static final class UpdateControllerCmd extends BaseCommand {

        public UpdateControllerCmd() {
            super("updateController", new UpdateControllerResponse());

        }

    }

    private static final class UpdateVolumeiSCSIServiceCmd extends BaseCommand {

        public UpdateVolumeiSCSIServiceCmd() {
            super("updateVolumeiSCSIService", new UpdateVolumeiSCSIServiceCmdResponse());

        }

    }

    private static final class DeleteTsmCmd extends BaseCommand {

        public DeleteTsmCmd() {
            super("deleteTsm", new DeleteTsmResponse());
        }

    }

    private static final class DeleteVolumeCmd extends BaseCommand {

        public DeleteVolumeCmd() {
            super("deleteFileSystem", new DeleteVolumeResponse());
        }

    }

    private static final class QueryAsyncJobResultCmd extends BaseCommand {

        public QueryAsyncJobResultCmd() {
            super("queryAsyncJobResult", new QueryAsyncJobResultResponse());
        }

    }

    private static final class ListTsmCmd extends BaseCommand {

        public ListTsmCmd() {
            super("listTsm", new ListTsmsResponse());
        }

    }

    private static final class ListFileSystemCmd extends BaseCommand {

        public ListFileSystemCmd() {
            super("listFileSystem", new ListFileSystemResponse());
        }

    }

    private static final class ListAccountsCmd extends BaseCommand {

        public ListAccountsCmd() {
            super("listAccount", new ListAccountResponse());
        }

    }

    private static final class CreateAccountCmd extends BaseCommand {

        public CreateAccountCmd() {
            super("createAccount", new CreateAccountResponse());
        }

    }

    private static final class ListInterfacesCmd extends BaseCommand {

        public ListInterfacesCmd() {
            super("listSharedNICs", new ListInterfacesResponse());
        }

    }

    private static final class ListPoolsCmd extends BaseCommand {

        public ListPoolsCmd() {
            super("listHAPool", new ListPoolsResponse());
        }

    }

    /**
     * these are the list of Elastistor rest json response classes for parsing
     * the json response sent by elastistor.
     *
     */
    public static final class CreateTsmCmdResponse {

        @SerializedName("addTsmResponse")
        private JobId jobId;

        public String getJobid() {
            return jobId.getJobid();
        }

        public String getJobStatus() {
            return jobId.getJobStatus();
        }

        @SerializedName("createTsmResponse")
        private TsmWrapper tsmWrapper;

        public Tsm getTsm() {
            return tsmWrapper.getTsm();
        }

    }

    public static final class Tsm {

        @SerializedName("id")
        private String uuid;

        @SerializedName("name")
        private String name;

        @SerializedName("datasetid")
        private String datasetid;

        @SerializedName("ipaddress")
        private String ipaddress;

        @SerializedName("volumes")
        private VolumeProperties[] volumeProperties;

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public String getIpaddress() {
            return ipaddress;
        }

        public String getDatasetid() {
            return datasetid;
        }

        public boolean checkvolume() {

            if (volumeProperties != null) {
                return true;
            } else {
                return false;
            }

        }

        public VolumeProperties getVolumeProperties(int i) {
            return volumeProperties[i];
        }

    }

    public static final class VolumeProperties {

        @SerializedName("id")
        private String id;

        @SerializedName("groupid")
        private String groupid;

        @SerializedName("iops")
        private String iops;

        @SerializedName("name")
        private String name;

        public String getid() {
            return id;
        }

        public String getQosgroupid() {
            return groupid;
        }

        public String getName() {
            return name;
        }

        public String getIops() {
            return iops;
        }
    }

    public static final class TsmWrapper {

        @SerializedName("tsm")
        private Tsm tsm;

        public Tsm getTsm() {
            return tsm;
        }

    }

    public static final class AddQosGroupCmdResponse {

        @SerializedName("addqosgroupresponse")
        private QoSGroupWrapper qosGroupWrapper;

        public QoSGroup getQoSGroup() {
            return qosGroupWrapper.getQosGroup();
        }
    }

    public static final class QoSGroupWrapper {

        @SerializedName("qosgroup")
        private QoSGroup qoSGroup;

        public QoSGroup getQosGroup() {

            return qoSGroup;
        }

    }

    public static final class QoSGroup {

        @SerializedName("id")
        private String uuid;

        @SerializedName("name")
        private String name;

        @SerializedName("qosgroupproperties")
        private HashMap<String, String> qosGroupProperties;

        public String getName() {
            return name;
        }

        public String getUuid() {
            return uuid;
        }

        public String getIops() {
            return qosGroupProperties.get("iops");
        }

        public String getThroughput() {
            return qosGroupProperties.get("throughput");
        }

        public String getLatency() {
            return qosGroupProperties.get("latency");
        }
    }

    public static final class UpdateVolumeiSCSIServiceCmdResponse {

        @SerializedName("updatingvolumeiscsidetails")
        private VolumeiSCSIServiceWrapper volumeiSCSIServiceWrapper;

        public Volumeiscsioptions getVolumeiscsioptions() {

            return volumeiSCSIServiceWrapper.getVolumeiscsioptions();
        }

    }

    public static final class VolumeiSCSIServiceWrapper {

        @SerializedName("viscsioptions")
        private Volumeiscsioptions viscsioptions;

        public Volumeiscsioptions getVolumeiscsioptions() {
            return viscsioptions;
        }

    }

    public static final class Volumeiscsioptions {

        @SerializedName("id")
        private String uuid;

        @SerializedName("volume_id")
        private String volumeid;

        @SerializedName("iqnname")
        private String iqnname;

        public String getUuid() {
            return uuid;
        }

        public String getVolumeid() {
            return volumeid;
        }

        public String getIqn() {
            return iqnname;
        }

    }

    public static final class NfsServiceResponse {

        @SerializedName("nfsserviceprotocolresponse")
        private NfsServiceWrapper nfsServiceWrapper;

        public NfsService getNfsService() {

            return nfsServiceWrapper.getNfsservice();
        }

    }

    public static final class NfsServiceWrapper {

        @SerializedName("nfs")
        private NfsService nfsService;

        public NfsService getNfsservice() {
            return nfsService;
        }

    }

    public static final class NfsService {

        @SerializedName("id")
        private String uuid;

        @SerializedName("STORAGEID")
        private String datasetid;

        @SerializedName("controllerid")
        private String controllerid;

        @SerializedName("authnetwork")
        private String authnetwork;

        public String getUuid() {
            return uuid;
        }

        public String getDatasetid() {
            return datasetid;
        }

        public String getControllerid() {
            return controllerid;
        }

        public String getAuthnetwork() {
            return authnetwork;
        }

    }

    public static final class UpdateControllerResponse {

        @SerializedName("updateControllerResponse")
        private UpdateControllerWrapper controllerWrapper;

        public Controller getController() {

            return controllerWrapper.getController();
        }

    }

    public static final class UpdateControllerWrapper {

        @SerializedName("controller")
        private Controller controller;

        public Controller getController() {
            return controller;
        }

    }

    public static final class Controller {

        @SerializedName("id")
        private String uuid;

        public String getUuid() {
            return uuid;
        }

    }

    public static final class CreateVolumeCmdResponse {

        @SerializedName("createvolumeresponse")
        private JobId jobId;

        public String getJobid() {
            return jobId.getJobid();
        }

        @SerializedName("adddatasetresponse")
        private FileSystemWrapper fileSystemWrapper;

        public FileSystem getFileSystem() {

            return fileSystemWrapper.getFileSystem();
        }

    }

    public static final class FileSystemWrapper {

        @SerializedName("filesystem")
        private FileSystem fileSystem;

        public FileSystem getFileSystem() {
            return fileSystem;
        }

    }

    public static final class FileSystem {

        @SerializedName("id")
        private String uuid;

        @SerializedName("name")
        private String name;

        @SerializedName("quota")
        private String quota;

        @SerializedName("accountid")
        private String accountid;

        @SerializedName("iqnname")
        private String iqnname;

        @SerializedName("nfsenabled")
        private String nfsenabled;

        @SerializedName("iscsienabled")
        private String iscsienabled;

        @SerializedName("path")
        private String path;

        @SerializedName("groupid")
        private String groupid;

        @SerializedName("compression")
        private String compression;

        @SerializedName("sync")
        private String sync;

        @SerializedName("deduplication")
        private String deduplication;

        @SerializedName("graceallowed")
        private String graceallowed;

        public String getCompression() {
            return compression;
        }

        public String getSync() {
            return sync;
        }

        public String getDeduplication() {
            return deduplication;
        }

        public String getGraceallowed() {
            return graceallowed;
        }

        public String getUuid() {
            return uuid;
        }

        public String getQosGroupid() {
            return groupid;
        }

        public String getName() {
            return name;
        }

        public String getNfsenabled() {
            return nfsenabled;
        }

        public String getIscsienabled() {
            return iscsienabled;
        }

        public String getPath() {
            return path;
        }

        public String getIqn() {
            return iqnname;
        }

        public String getQuota() {
            return quota;
        }

        public String getAccountid() {
            return accountid;
        }

    }

    public static final class DeleteTsmResponse {

        @SerializedName("deleteTsmResponse")
        private JobId jobId;

        public String getJobStatus() {
            return jobId.getJobStatus();
        }

    }

    public static final class JobId {

        @SerializedName("jobid")
        private String jobid;

        @SerializedName("success")
        private String jobStatus;

        @SerializedName("jobresult")
        private JobResult jobresult;

        @SerializedName("tsm")
        private Tsm tsm;

        @SerializedName("storage")
        private FileSystem volume;

        public Tsm getTsm() {
            return tsm;
        }

        public FileSystem getVolume() {
            return volume;
        }

        public JobResult getJobResult() {
            return jobresult;
        }

        public String getJobid() {
            return jobid;
        }

        public String getJobStatus() {
            return jobStatus;
        }

    }

    public static final class JobResult {

        @SerializedName("tsm")
        private Tsm tsm;

        @SerializedName("storage")
        private FileSystem volume;

        public Tsm getTsm() {
            return tsm;
        }

        public FileSystem getVolume() {
            return volume;
        }

    }

    public static final class DeleteVolumeResponse {

        @SerializedName("deleteFileSystemResponse")
        private JobId jobId;

        public String getJobId() {
            return jobId.getJobid();
        }

    }

    public static final class ListCapabilitiesResponse {

        @SerializedName("listcapabilitiesresponse")
        private Capabilities capabilities;

        public Capabilities getCapabilities() {
            return capabilities;
        }
    }

    public static final class ListFileSystemResponse {

        @SerializedName("listFilesystemResponse")
        private Filesystems filesystems;

        public int getFilesystemCount() {
            return filesystems.getCount();
        }

        public Filesystems getFilesystems() {
            return filesystems;
        }
    }

    public static final class Filesystems {

        @SerializedName("count")
        private int count;

        @SerializedName("filesystem")
        private FileSystem[] fileSystems;

        public int getCount() {
            return count;
        }

        public FileSystem getFileSystem(int i) {
            return fileSystems[i];
        }
    }

    public static final class ListPoolsResponse {

        @SerializedName("listHAPoolResponse")
        private Pools pools;

        public Pools getPools() {
            return pools;
        }
    }

    public static final class Pools {

        @SerializedName("hapool")
        private Pool[] pool;

        @SerializedName("count")
        private int count;

        public Pool getPool(int i) {
            return pool[i];
        }

        public int getCount() {
            return count;
        }
    }

    public static final class Pool {

        @SerializedName("id")
        private String uuid;

        @SerializedName("name")
        private String name;

        @SerializedName("currentAvailableSpace")
        private String currentAvailableSpace;

        @SerializedName("availIOPS")
        private String availIOPS;

        @SerializedName("status")
        private String state;

        @SerializedName("controllerid")
        private String controllerid;

        @SerializedName("gateway")
        private String gateway;

        public String getControllerid() {
            return controllerid;
        }

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public String getAvailableSpace() {
            return currentAvailableSpace;
        }

        public String getAvailIOPS() {
            return availIOPS;
        }

        public String getState() {
            return state;
        }

        public String getGateway() {
            return gateway;
        }
    }

    public static final class ListInterfacesResponse {

        @SerializedName("listSharedNICsResponse")
        private Interfaces interfaces;

        public Interfaces getInterfaces() {
            return interfaces;
        }
    }

    public static final class Interfaces {

        @SerializedName("nic")
        private Interface[] interfaces;

        @SerializedName("count")
        private int count;

        public Interface getInterface(int i) {
            return interfaces[i];
        }

        public int getCount() {
            return count;
        }
    }

    public static final class Interface {

        @SerializedName("id")
        private String uuid;

        @SerializedName("name")
        private String name;

        @SerializedName("status")
        private String status;

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }
    }

    public static final class ListiSCSIInitiatorResponse {

        @SerializedName("listInitiatorsResponse")
        private Initiators initiators;

        public Initiators getIInitiator() {
            return initiators;
        }
    }

    public static final class Initiators {

        @SerializedName("initiator")
        private Initiator[] initiators;

        @SerializedName("count")
        private int count;

        public Initiator getInterface(int i) {
            return initiators[i];
        }

        public int getCount() {
            return count;
        }
    }

    public static final class Initiator {

        @SerializedName("id")
        private String uuid;

        @SerializedName("name")
        private String name;

        @SerializedName("initiatorgroup")
        private String initiatorgroup;

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }

        public String getInitiatorgroup() {
            return initiatorgroup;
        }
    }

    public static final class ListAccountResponse {

        @SerializedName("listAccountResponse")
        private Accounts accounts;

        public Accounts getAccounts() {
            return accounts;
        }
    }

    public static final class Accounts {

        @SerializedName("account")
        private Account[] Accounts;

        @SerializedName("count")
        private int count;

        public Account getAccount(int i) {
            return Accounts[i];
        }

        public int getCount() {
            return count;
        }
    }

    public static final class CreateAccountResponse {

        @SerializedName("createaccountresponse")
        private Accounts2 accounts;

        public Account getAccount() {
            return accounts.getAccount();
        }
    }

    public static final class Accounts2 {

        @SerializedName("account2")
        private Account Account;

        @SerializedName("count")
        private int count;

        public Account getAccount() {
            return Account;
        }

        public int getCount() {
            return count;
        }
    }

    public static final class Account {

        @SerializedName("id")
        private String uuid;

        @SerializedName("name")
        private String name;

        public String getUuid() {
            return uuid;
        }

        public String getName() {
            return name;
        }
    }

    public static final class ListTsmsResponse {

        @SerializedName("listTsmResponse")
        private Tsms tsms;

        public int getTsmsCount() {
            return tsms.getCount();
        }

        public Tsms getTsms() {
            return tsms;
        }
    }

    public static final class Tsms {

        @SerializedName("count")
        private int count;

        @SerializedName("listTsm")
        private Tsm[] tsms;

        public int getCount() {
            return count;
        }

        public Tsm getTsm(int i) {
            return tsms[i];
        }
    }

    public static final class ListVolumeiSCSIServiceResponse {

        @SerializedName("listVolumeiSCSIServiceResponse")
        private IscsiVolumeService iscsiVolumes;

        public int getVolumeCount() {
            return iscsiVolumes.getCount();
        }

        public IscsiVolumeService getIscsiVolume() {
            return iscsiVolumes;
        }
    }

    public static final class IscsiVolumeService {

        @SerializedName("count")
        private int count;

        @SerializedName("iSCSIService")
        private IscsiVolume[] iscsiVolumes;

        public int getCount() {
            return count;
        }

        public IscsiVolume getIscsiVolume(int i) {
            return iscsiVolumes[i];
        }
    }

    public static final class IscsiVolume {

        @SerializedName("id")
        private String uuid;

        @SerializedName("ag_id")
        private String agid;

        @SerializedName("ig_id")
        private String igid;

        public String getAg_id() {
            return agid;
        }

        public String getUuid() {
            return uuid;
        }

        public String getIg_id() {
            return igid;
        }
    }

    public static final class QueryAsyncJobResultResponse {

        @SerializedName("queryasyncjobresultresponse")
        private Async async;

        public Async getAsync() {
            return async;
        }
    }

    public static final class Async {

        @SerializedName("jobstatus")
        private int jobstatus;

        @SerializedName("jobresult")
        private JobId jobresult;

        @SerializedName("cmd")
        private String cmd;

        public int getJobStatus() {
            return jobstatus;
        }

        public JobId getJobResult() {
            return jobresult;
        }

        public String getCmd() {
            return cmd;
        }

    }

    public static final class Capabilities {

        @SerializedName("capability")
        private HashMap<String, String> capabilites;

        public String getVersion() {
            return capabilites.get("cloudByteVersion");
        }
    }

    /*
     *
     * change Volume IOPS
     */

    public static Answer updateElastistorVolumeQosGroup(String volumeId, Long newIOPS, String graceAllowed) throws Throwable {

        FileSystem fileSystem = listVolume(volumeId);

        String qosid = fileSystem.getQosGroupid();

        if (qosid != null) {

            UpdateQosGroupCmdResponse qosGroupCmdResponse = updateQosGroupVolume(newIOPS.toString(), qosid, graceAllowed);

            if (qosGroupCmdResponse.getQoSGroup(0).uuid != null) {
                return new Answer(null, true, null);
            } else {
                return new Answer(null, false, "Update Qos Group Failed");
            }
        } else {
            return new Answer(null, false, "Qos Group id is NULL");
        }

    }

    private static UpdateQosGroupCmdResponse updateQosGroupVolume(String iops, String qosgroupid, String graceAllowed) throws Throwable {

        UpdateQosGroupCmd updateQosGroupCmd = new UpdateQosGroupCmd();
        updateQosGroupCmd.putCommandParameter("id", qosgroupid);
        updateQosGroupCmd.putCommandParameter("iops", iops);
        updateQosGroupCmd.putCommandParameter("graceallowed", graceAllowed);

        UpdateQosGroupCmdResponse updateQosGroupCmdResponse = (UpdateQosGroupCmdResponse) getElastistorRestClient().executeCommand(updateQosGroupCmd);

        return updateQosGroupCmdResponse;
    }

    private static final class UpdateQosGroupCmd extends BaseCommand {

        public UpdateQosGroupCmd() {
            super("updateQosGroup", new UpdateQosGroupCmdResponse());
        }

    }

    public static final class UpdateQosGroupCmdResponse {

        @SerializedName("updateqosresponse")
        private QoSGroupWrapperChangeVolumeIops qosGroupWrapper;

        public QoSGroup getQoSGroup(int i) {
            return qosGroupWrapper.getQosGroup(i);
        }
    }

    public static final class QoSGroupWrapperChangeVolumeIops {

        @SerializedName("qosgroup")
        private QoSGroup qoSGroup[];

        public QoSGroup getQosGroup(int i) {

            return qoSGroup[i];
        }

    }

    /*
     * resize volume
     */

    public static Boolean updateElastistorVolumeSize(String volumeId, Long newSize) throws Throwable {

        Boolean status = false;

        String quotasize = (String.valueOf(newSize / (1024 * 1024 * 1024)) + "G");

        UpdateFileSystemCmdResponse fileSystemCmdResponse = updateFileSystem(quotasize, volumeId, null, null, null);

        if (fileSystemCmdResponse.getFileSystem(0).uuid != null) {
            status = true;
            return status;
        }

        return status;
    }

    public static UpdateFileSystemCmdResponse updateFileSystem(String quotasize, String esvolumeid, String dedeplication, String compression, String sync) throws Throwable {

        UpdateFileSystemCmd fileSystemCmd = new UpdateFileSystemCmd();

        fileSystemCmd.putCommandParameter("id", esvolumeid);
        if (null != quotasize)
            fileSystemCmd.putCommandParameter("quotasize", quotasize);
        if (null != dedeplication)
            fileSystemCmd.putCommandParameter("deduplication", dedeplication);
        if (null != compression)
            fileSystemCmd.putCommandParameter("compression", compression);
        if (null != sync)
            fileSystemCmd.putCommandParameter("sync", sync);

        UpdateFileSystemCmdResponse fileSystemCmdResponse = (UpdateFileSystemCmdResponse) getElastistorRestClient().executeCommand(fileSystemCmd);

        return fileSystemCmdResponse;
    }

    private static final class UpdateFileSystemCmd extends BaseCommand {

        public UpdateFileSystemCmd() {
            super("updateFileSystem", new UpdateFileSystemCmdResponse());
        }

    }

    private static final class UpdateFileSystemCmdResponse {

        @SerializedName("updatefilesystemresponse")
        private UpdateFileSystemWrapper fileSystemWrapper;

        public FileSystem getFileSystem(int i) {

            return fileSystemWrapper.getFileSystem(i);
        }
    }

    public class UpdateFileSystemWrapper {

        @SerializedName("filesystem")
        private FileSystem fileSystem[];

        public FileSystem getFileSystem(int i) {
            return fileSystem[i];
        }

    }
    /*
     * create snapshot
     */

     public static Answer createElastistorVolumeSnapshot(String volumeId, String snapshotName) throws Throwable{

         CreateStorageSnapshotCmd snapshotCmd = new CreateStorageSnapshotCmd();

         snapshotCmd.putCommandParameter("id", volumeId);
         snapshotCmd.putCommandParameter("name", snapshotName);

         CreateStorageSnapshotCmdResponse snapshotCmdResponse = (CreateStorageSnapshotCmdResponse) getElastistorRestClient().executeCommand(snapshotCmd);

         if(snapshotCmdResponse.getStorageSnapshot().getId() != null){
             return new Answer(null, true, snapshotCmdResponse.getStorageSnapshot().getId());
         }else{
             return new Answer(null, false, "snapshot failed");
         }
     }

     private static final class CreateStorageSnapshotCmd extends BaseCommand {

            public CreateStorageSnapshotCmd() {
                super("createStorageSnapshot", new CreateStorageSnapshotCmdResponse() );
            }

        }

     private static final class CreateStorageSnapshotCmdResponse {

            @SerializedName("createStorageSnapshotResponse")
             private StorageSnapshotWrapper StorageSnapshot;

            public StorageSnapshot getStorageSnapshot() {
                return StorageSnapshot.getStorageSnapshot();
            }
        }

     public static final class StorageSnapshotWrapper {

            @SerializedName("StorageSnapshot")
            private StorageSnapshot snapshot;



            public StorageSnapshot getStorageSnapshot() {
                return snapshot;
            }

        }

     public static final class StorageSnapshot {

         @SerializedName("id")
         private String uuid;

         @SerializedName("name")
         private String name;

         public String getId(){
             return uuid;
         }

         public String getName(){
             return name;
         }
     }

     // update the TSM storage
     public static UpdateTsmStorageCmdResponse updateElastistorTsmStorage(String capacityBytes,String uuid) throws Throwable{

         Long size = (Long.parseLong(capacityBytes)/(1024 * 1024 * 1024));

         String quotasize = null;

         if(size > 1024){
            quotasize = (String.valueOf(Long.parseLong(capacityBytes)/(1024)) + "T");
         }else{
            quotasize = String.valueOf(quotasize) + "G";
         }
         s_logger.info("elastistor tsm storage is updating to " + quotasize);
         UpdateTsmStorageCmd updateTsmStorageCmd = new UpdateTsmStorageCmd();

         updateTsmStorageCmd.putCommandParameter("id", uuid);
         updateTsmStorageCmd.putCommandParameter("quotasize", quotasize);

         UpdateTsmStorageCmdResponse updateTsmStorageCmdResponse = (UpdateTsmStorageCmdResponse) getElastistorRestClient().executeCommand(updateTsmStorageCmd);

       return updateTsmStorageCmdResponse;
     }

     private static final class UpdateTsmStorageCmd extends BaseCommand {

         public UpdateTsmStorageCmd() {
             super("updateStorage", new UpdateTsmStorageCmdResponse());
         }

     }

     public static final class UpdateTsmStorageCmdResponse {

         @SerializedName("updatedatasetresponse")
          private StorageWrapper storageWrapper;

         public Storage getStorage() {
             return storageWrapper.getStorage();
         }
     }

     public static final class StorageWrapper {

         @SerializedName("storage")
         private Storage storage;

         public Storage getStorage() {
             return storage;
         }

     }

     public static final class Storage {

         @SerializedName("id")
         private String uuid;

         @SerializedName("name")
         private String name;

         @SerializedName("quota")
         private String quota;

         public String getId(){
             return uuid;
         }

         public String getName(){
             return name;
         }

         public String getsize(){
             return quota;
         }
     }

  // update the TSM IOPS
     public static UpdateTsmCmdResponse updateElastistorTsmIOPS(String capacityIOPs,String uuid) throws Throwable{

         s_logger.info("elastistor tsm IOPS is updating to " + capacityIOPs);
         UpdateTsmCmd updateTsmCmd = new UpdateTsmCmd();
         String throughput = String.valueOf(Long.parseLong(capacityIOPs)*4);

         updateTsmCmd.putCommandParameter("id", uuid);
         updateTsmCmd.putCommandParameter("iops", capacityIOPs);
         updateTsmCmd.putCommandParameter("throughput", throughput);

         UpdateTsmCmdResponse updateTsmStorageCmdResponse = (UpdateTsmCmdResponse) getElastistorRestClient().executeCommand(updateTsmCmd);

       return updateTsmStorageCmdResponse;
     }

     private static final class UpdateTsmCmd extends BaseCommand {

         public UpdateTsmCmd() {
             super("updateTsm", new UpdateTsmCmdResponse());
         }

     }

     public static final class UpdateTsmCmdResponse {

         @SerializedName("updateTsmResponse")
          private UpdateTsmWrapper tsmWrapper;

         public Tsm getTsm(int i) {
             return tsmWrapper.getTsm(i);
         }
     }

     public static final class UpdateTsmWrapper {

         @SerializedName("count")
         private int count;

         @SerializedName("tsm")
         private Tsm[] tsms;

         public int getCount() {
             return count;
         }

         public Tsm getTsm(int i) {
             return tsms[i];
         }
     }

}
