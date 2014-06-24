/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cloudstack.storage.datastore.util;

import java.net.ConnectException;
import java.security.InvalidParameterException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;

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

import org.apache.http.auth.InvalidCredentialsException;
import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class ElastistorUtil {

    private static final Logger s_logger = Logger.getLogger(ElastistorUtil.class);


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
    public static final String REST_PARAM_MEMLIMIT= "memlimit";
    public static final String REST_PARAM_NETWORKSPEED = "networkspeed";
    public static final String REST_PARAM_TSMID = "tsmid";
    public static final String REST_PARAM_DATASETID = "datasetid";
    public static final String REST_PARAM_QOSGROUPID = "qosgroupid";
    public static final String REST_PARAM_DEDUPLICATION = "deduplication";
    public static final String REST_PARAM_COMPRESSION = "compression";
    public static final String REST_PARAM_SYNC = "sync";
    public static final String REST_PARAM_MOUNTPOINT= "mountpoint";
    public static final String REST_PARAM_CASESENSITIVITY = "casesensitivity";
    public static final String REST_PARAM_UNICODE = "unicode";
    public static final String REST_PARAM_PROTOCOLTYPE= "protocoltype";
    public static final String REST_PARAM_AUTHNETWORK = "authnetwork";
    public static final String REST_PARAM_MAPUSERSTOROOT = "mapuserstoroot";

    /**
     * Constants related to elastistor which are persisted in cloudstack
     * databases as keys.
     */
    public static final String ES_SUBNET = "essubnet";
    public static final String ES_INTERFACE = "estntinterface";
    public static final String ES_GATEWAY = "esdefaultgateway";
    public static final String ES_PROVIDER_NAME = "elastistor";
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


    /**
     * Private constructor s.t. its never instantiated.
     */
    private ElastistorUtil() {

    }

    /**
     * This intializes a new jersey restclient for http call with elasticenter
     */
    public static ElastiCenterClient getElastistorRestClient(String managementIp , String apiKey) {
        ElastiCenterClient restclient = null;
        try {

            restclient = new ElastiCenterClient(managementIp, apiKey);

        } catch (InvalidCredentialsException e) {
            throw new CloudRuntimeException("InvalidCredentialsException", e);
        } catch (InvalidParameterException e) {
            throw new CloudRuntimeException("InvalidParameterException", e);
        } catch (SSLHandshakeException e) {
            throw new CloudRuntimeException("SSLHandshakeException", e);
        } catch (ServiceUnavailableException e) {
            throw new CloudRuntimeException("ServiceUnavailableException", e);
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
     * This creates a new tenant storage machine(TSM) for the given storagepool ip in elastistor.
     */
    public static CreateTsmCmdResponse createElastistorTsm(String storagePoolName, String storageIp, Long capacityBytes, Long capacityIops) throws Throwable {

            String totalthroughput = String.valueOf(capacityIops*4);
            String totaliops = String.valueOf(capacityIops);

            String quotasize = convertCapacityBytes(capacityBytes);

          CreateTsmCmd createTsmCmd = new CreateTsmCmd();

          if ( null != ElastistorUtil.s_esACCOUNTIDVAL ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ACCOUNTID, ElastistorUtil.s_esACCOUNTIDVAL);
          if ( null != totalthroughput ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_TSM_THROUGHPUT, totalthroughput);
          if ( null != ElastistorUtil.s_esPOOLIDVAL ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_POOLID, ElastistorUtil.s_esPOOLIDVAL);
          if ( null != storagePoolName ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_NAME, "TSM"+storagePoolName);
          if ( null != quotasize ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_QUOTA_SIZE, quotasize);
          if ( null != storageIp ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_IPADDRESS, storageIp);
          if ( null != ElastistorUtil.s_esSUBNETVAL ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_SUBNET, ElastistorUtil.s_esSUBNETVAL);
          if ( null != ElastistorUtil.s_esGATEWAYVAL ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_GATEWAY, ElastistorUtil.s_esGATEWAYVAL);
          if ( null != ElastistorUtil.s_esINTERFACEVAL ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_INTERFACE, ElastistorUtil.s_esINTERFACEVAL);
          if ( null != ElastistorUtil.ES_NOOFCOPIES_VAL ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_NOOFCOPIES, ElastistorUtil.ES_NOOFCOPIES_VAL);
          if ( null != ElastistorUtil.ES_BLOCKSIZE_VAL ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_RECORDSIZE, ElastistorUtil.ES_BLOCKSIZE_VAL);
          if ( null != totaliops ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_TOTALIOPS, totaliops);
          if ( null != ElastistorUtil.ES_LATENCY_VAL ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_LATENCY, ElastistorUtil.ES_LATENCY_VAL);
          if ( null != ElastistorUtil.ES_BLOCKSIZE_VAL ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_BLOCKSIZE, ElastistorUtil.ES_BLOCKSIZE_VAL);
          if ( null != ElastistorUtil.ES_GRACEALLOWED_VAL ) createTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_GRACEALLOWED, ElastistorUtil.ES_GRACEALLOWED_VAL);

        CreateTsmCmdResponse cmdResponse;

    try {
        cmdResponse = (CreateTsmCmdResponse) getElastistorRestClient(s_esIPVAL, s_esAPIKEYVAL).executeCommand(createTsmCmd);

        if ( cmdResponse.getTsm().getUuid() == null  ){
             throw new CloudRuntimeException("tsm creation failed , contact elatistor admin");
         }
        return cmdResponse;
    } catch (Exception e) {
        throw new CloudRuntimeException("tsm creation failed , contact elatistor admin" + e.toString());
    }

    }

    /**
     * This creates the specified volume on the created tsm.
     */
    public static CreateVolumeCmdResponse createElastistorVolume(String storagePoolName, CreateTsmCmdResponse cmdResponse, Long capacityBytes, Long capacityIops,String protocoltype, String mountpoint) throws Throwable {

        String datasetid;
        String tsmid;
        String qosgroupid;
        String VolumeName = storagePoolName;
        String totaliops = String.valueOf(capacityIops);
        String totalthroughput = String.valueOf(capacityIops*4);

        String quotasize = convertCapacityBytes(capacityBytes);

        AddQosGroupCmd addQosGroupCmd = new AddQosGroupCmd();


        tsmid = cmdResponse.getTsm().getUuid();
        datasetid = cmdResponse.getTsm().getDatasetid();

        if (null != VolumeName)addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_NAME, "QOS_" + VolumeName);
        if (null != totaliops)addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_IOPS, totaliops);
        if (null != ElastistorUtil.ES_LATENCY_VAL)addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_LATENCY, ElastistorUtil.ES_LATENCY_VAL);
        if (null != ElastistorUtil.ES_BLOCKSIZE_VAL)addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_BLOCKSIZE, ElastistorUtil.ES_BLOCKSIZE_VAL);
        if (null != totalthroughput)addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_THROUGHPUT, totalthroughput);
        if (null != ElastistorUtil.ES_MEMLIMIT_VAL)addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_MEMLIMIT, ElastistorUtil.ES_MEMLIMIT_VAL);
        if (null != ElastistorUtil.ES_NETWORKSPEED_VAL)addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_NETWORKSPEED, ElastistorUtil.ES_NETWORKSPEED_VAL);
        if (null != tsmid)addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_TSMID, tsmid);
        if (null != datasetid)addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_DATASETID, datasetid);
        if (null != ElastistorUtil.ES_GRACEALLOWED_VAL)addQosGroupCmd.putCommandParameter(ElastistorUtil.REST_PARAM_GRACEALLOWED, ElastistorUtil.ES_GRACEALLOWED_VAL);

        AddQosGroupCmdResponse addQosGroupCmdResponse = (AddQosGroupCmdResponse) getElastistorRestClient(s_esIPVAL, s_esAPIKEYVAL).executeCommand(addQosGroupCmd);

        if (addQosGroupCmdResponse.getQoSGroup().getUuid() == null) {

            throw new CloudRuntimeException("adding qos group failed , contact elatistor admin");

        }

        else {

            CreateVolumeCmd createVolumeCmd = new CreateVolumeCmd();

            qosgroupid = addQosGroupCmdResponse.getQoSGroup().getUuid();

            if (null != ElastistorUtil.s_esACCOUNTIDVAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ACCOUNTID,ElastistorUtil.s_esACCOUNTIDVAL);
            if (null != qosgroupid)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_QOSGROUPID, qosgroupid);
            if (null != tsmid)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_TSMID, tsmid);
            if (null != ElastistorUtil.s_esPOOLIDVAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_POOLID,ElastistorUtil.s_esPOOLIDVAL);
            if (null != VolumeName)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_NAME, VolumeName);
            if (null != quotasize)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_QUOTA_SIZE, quotasize);
            if(protocoltype.equalsIgnoreCase("nfs")){
                  if ( null != ElastistorUtil.ES_BLOCKSIZE_VAL ) createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_BLOCKSIZE, ElastistorUtil.ES_BLOCKSIZE_VAL);
                  if ( null != ElastistorUtil.ES_BLOCKSIZE_VAL ) createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_RECORDSIZE, ElastistorUtil.ES_BLOCKSIZE_VAL);
                  }
                  else{
                      if ( null != ElastistorUtil.ES_BLOCKSIZE_VAL ) createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_BLOCKSIZE, "512B");
                      if ( null != ElastistorUtil.ES_BLOCKSIZE_VAL ) createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_RECORDSIZE, "512B");
                  }
            if (null != ElastistorUtil.ES_DEDUPLICATION_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_DEDUPLICATION, ElastistorUtil.ES_DEDUPLICATION_VAL);
            if (null != ElastistorUtil.ES_SYNC_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_SYNC, ElastistorUtil.ES_SYNC_VAL);
            if (null != ElastistorUtil.ES_COMPRESSION_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_COMPRESSION, ElastistorUtil.ES_COMPRESSION_VAL);
            if (null != ElastistorUtil.ES_NOOFCOPIES_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_NOOFCOPIES, ElastistorUtil.ES_NOOFCOPIES_VAL);
            createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_MOUNTPOINT, mountpoint);
            if (null != ElastistorUtil.ES_CASESENSITIVITY_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_CASESENSITIVITY, ElastistorUtil.ES_CASESENSITIVITY_VAL);
            if (null != ElastistorUtil.ES_READONLY_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_READONLY, ElastistorUtil.ES_READONLY_VAL);
            if (null != datasetid)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_DATASETID, datasetid);
            if (null != ElastistorUtil.ES_UNICODE_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_UNICODE, ElastistorUtil.ES_UNICODE_VAL);
            createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_PROTOCOLTYPE, protocoltype);
            if (null != ElastistorUtil.ES_AUTHNETWORK_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_AUTHNETWORK, ElastistorUtil.ES_AUTHNETWORK_VAL);
            if (null != ElastistorUtil.ES_MAPUSERSTOROOT_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_MAPUSERSTOROOT, ElastistorUtil.ES_MAPUSERSTOROOT_VAL);

            CreateVolumeCmdResponse createVolumeCmdResponse;
            try {
                createVolumeCmdResponse = (CreateVolumeCmdResponse) getElastistorRestClient(s_esIPVAL, s_esAPIKEYVAL).executeCommand(createVolumeCmd);

                if (createVolumeCmdResponse.getFileSystem().getUuid() == null) {

                    throw new CloudRuntimeException("creating volume failed , contact elatistor admin");

                } else {
                    return createVolumeCmdResponse;
                }

            } catch (Exception e) {
                throw new CloudRuntimeException("creating volume failed , contact elatistor admin", e);
            }

        }

    }

    /**
     * This deletes both the volume and the tsm in elastistor.
     */
    public static boolean deleteElastistorVolume(String poolip, String esmanagementip, String esapikey) throws Throwable {

        String esvolumeid = null;
        String estsmid = null;

        ListTsmsResponse listTsmsResponse = listTsm(poolip);

        if (listTsmsResponse.getTsmsCount() != 0) {
            int i;

            for (i = 0; i < listTsmsResponse.getTsmsCount(); i++) {
                if (poolip.compareTo(listTsmsResponse.getTsms().getTsm(i).getIpaddress()) == 0) {
                    estsmid = listTsmsResponse.getTsms().getTsm(i).getUuid();
                    break;
                }
            }

            if (listTsmsResponse.getTsms().getTsm(i).checkvolume()) {
                esvolumeid = listTsmsResponse.getTsms().getTsm(i).getVolumeProperties(0).getid();
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
            s_logger.info("now trying to delete elastistor tsm");

            if (estsmid != null) {
                DeleteTsmCmd deleteTsmCmd = new DeleteTsmCmd();
                deleteTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ID,estsmid);
                DeleteTsmResponse deleteTsmResponse = (DeleteTsmResponse) getElastistorRestClient(s_esIPVAL, s_esAPIKEYVAL).executeCommand(deleteTsmCmd);

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
            else {
                s_logger.error("no volume is present in the tsm");
            }
        } else {
            s_logger.error("List tsm failed, no tsm present in the eastistor for the given IP ");
            return false;
        }
        return false;

    }

    /**
     * This give a json response containing the list of tsm's in elastistor.
     */
    private static ListTsmsResponse listTsm(String poolip) throws Throwable {

        ListTsmCmd listTsmCmd = new ListTsmCmd();

        listTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_IPADDRESS,poolip);

        ListTsmsResponse listTsmsResponse = (ListTsmsResponse) getElastistorRestClient(s_esIPVAL, s_esAPIKEYVAL).executeCommand(listTsmCmd);

        return listTsmsResponse;
    }

    private static DeleteVolumeResponse deleteVolume(String esvolumeid, String forcedelete)throws Throwable {

        DeleteVolumeCmd deleteVolumeCmd = new DeleteVolumeCmd();

        deleteVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ID,esvolumeid);
        deleteVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_FORECEDELETE, forcedelete);

        DeleteVolumeResponse deleteVolumeResponse = (DeleteVolumeResponse) getElastistorRestClient(s_esIPVAL, s_esAPIKEYVAL).executeCommand(deleteVolumeCmd);

        return deleteVolumeResponse;
    }

    private static int queryAsyncJobResult(String jobid) throws Throwable {

        QueryAsyncJobResultCmd asyncJobResultCmd = new QueryAsyncJobResultCmd();
        ElastiCenterClient restclient = getElastistorRestClient(s_esIPVAL, s_esAPIKEYVAL);

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

     /**
     * this method converts the long capacitybytes to string format, which is feasible for elastistor rest api
     * 214748364800 = 200G.
     */
    private static String convertCapacityBytes(Long capacityBytes){

        String quotasize;

        if((1099511627776L)>capacityBytes &&(capacityBytes>(1073741824))){
           return quotasize =(String.valueOf(capacityBytes/(1024*1024*1024))+"G");
        }else
        {   int temp1 = (int) (capacityBytes/(1024*1024*1024));
            int temp2  = temp1/1024;
            return  quotasize =(String.valueOf(temp2)+"T");
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
         * This command will be executed by the ElastiCenterClient only this method
         * returns true.
         */
        public boolean validate();

        /*
         * Returns the query parameters that have to be passed to execute the
         * command.
         *
         * Returns null if there are query parameters associated with the command
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
 * this is a rest client which is used to call the http rest calls to elastistor
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

        public ElastiCenterClient(String address, String key)throws InvalidCredentialsException, InvalidParameterException,SSLHandshakeException, ServiceUnavailableException {
            this.elastiCenterAddress = address;
            this.apiKey = key;
            this.initialize();
        }

        public void initialize() throws InvalidParameterException,
                SSLHandshakeException, InvalidCredentialsException,
                ServiceUnavailableException {

            if (apiKey == null || apiKey.trim().isEmpty()) {
                throw new InvalidParameterException("Unable to initialize. Please specify a valid API Key.");
            }

            if (elastiCenterAddress == null || elastiCenterAddress.trim().isEmpty()) {
                // TODO : Validate the format, like valid IP address or hostname.
                throw new InvalidParameterException("Unable to initialize. Please specify a valid ElastiCenter IP Address or Hostname.");
            }

            if (ignoreSSLCertificate) {
                // Create a trust manager that does not validate certificate chains
                TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs,
                            String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs,
                            String authType) {
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
                    SSLContext sc = SSLContext.getInstance("TLS");
                    sc.init(null, trustAllCerts, new SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
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
                    throw new SSLHandshakeException(
                            "Unable to initialize. An untrusted SSL Certificate was received from "
                                    + elastiCenterAddress
                                    + ". Please verify your truststore or configure ElastiCenterClient to skip the SSL Validation. ");
                } else if (t.getCause() instanceof ConnectException) {
                    throw new ServiceUnavailableException(
                            "Unable to initialize. Failed to connect to "
                                    + elastiCenterAddress
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
            return executeCommand(cmd.getCommandName(), cmd.getCommandParameters(),cmd.getResponseObject());
        }

        public Object executeCommand(String command,MultivaluedMap<String, String> params, Object responeObj) throws Throwable {

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
                    System.out.println("Command Sent " + command + " : "+ queryParams);
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
     * these are the list of Elastistor rest commands being called from the plugin.
     *
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

   /**
    * these are the list of Elastistor rest json response classes for parsing the json response sent by elastistor.
    *
    */
   public static final class CreateTsmCmdResponse {

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

            if(volumeProperties != null){
                return true;
            }
            else{
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

    public static final class CreateVolumeCmdResponse {

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

        @SerializedName("timestamp")
        private String timestamp;

        @SerializedName("iqnname")
        private String iqnname;

        @SerializedName("filesystemproperties")
        private HashMap<String, String>[] filesystemproperties;

        public String getUuid() {
          return uuid;
       }

        public String getName() {
          return name;
        }

        public String getIqn() {
          return iqnname;
        }

        public String getQuota() {
          return quota;
        }

        public String getTimestamp() {
          return timestamp;
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

        public String getJobid() {
            return jobid;
        }

        public String getJobStatus() {
            return jobStatus;
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

       @SerializedName("cmd")
       private String cmd;

       public int getJobStatus() {
           return jobstatus;
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
}
