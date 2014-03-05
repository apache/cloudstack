package org.apache.cloudstack.storage.datastore.util;

import java.security.InvalidParameterException;

import javax.naming.ServiceUnavailableException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.cloudstack.storage.datastore.client.ElastiCenterClient;
import org.apache.cloudstack.storage.datastore.command.AddQosGroupCmd;
import org.apache.cloudstack.storage.datastore.command.CreateTsmCmd;
import org.apache.cloudstack.storage.datastore.command.CreateVolumeCmd;
import org.apache.cloudstack.storage.datastore.command.DeleteTsmCmd;
import org.apache.cloudstack.storage.datastore.command.DeleteVolumeCmd;
import org.apache.cloudstack.storage.datastore.command.ListHAPoolCmd;
import org.apache.cloudstack.storage.datastore.command.ListTsmCmd;
import org.apache.cloudstack.storage.datastore.command.QueryAsyncJobResultCmd;
import org.apache.cloudstack.storage.datastore.command.UpdateFileSystemCmd;
import org.apache.cloudstack.storage.datastore.command.UpdateQosGroupCmd;
import org.apache.cloudstack.storage.datastore.command.UpdateStorageCmd;
import org.apache.cloudstack.storage.datastore.command.UpdateTsmCmd;
import org.apache.cloudstack.storage.datastore.response.AddQosGroupCmdResponse;
import org.apache.cloudstack.storage.datastore.response.CreateTsmCmdResponse;
import org.apache.cloudstack.storage.datastore.response.CreateVolumeCmdResponse;
import org.apache.cloudstack.storage.datastore.response.DeleteTsmResponse;
import org.apache.cloudstack.storage.datastore.response.DeleteVolumeResponse;
import org.apache.cloudstack.storage.datastore.response.ListHAPoolResponse;
import org.apache.cloudstack.storage.datastore.response.ListTsmsResponse;
import org.apache.cloudstack.storage.datastore.response.QueryAsyncJobResultResponse;
import org.apache.cloudstack.storage.datastore.response.UpdateFileSystemCmdResponse;
import org.apache.cloudstack.storage.datastore.response.UpdateQosGroupCmdResponse;
import org.apache.cloudstack.storage.datastore.response.UpdateStorageCmdResponse;
import org.apache.cloudstack.storage.datastore.response.UpdateTsmCmdResponse;
import org.apache.cloudstack.storage.volume.VolumeObject;
import org.apache.http.auth.InvalidCredentialsException;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.storage.Storage.StoragePoolType;
import com.cloud.storage.StoragePool;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * The util class for elastistor's storage plugin codebase.
 * 
 * @author amit.das@cloudbyte.com
 * @author punith.s@cloudbyte.com
 * 
 */
public class ElastistorUtil {
    
    private static final Logger s_logger = Logger.getLogger(ElastistorUtil.class);

    /**
     * Elastistor restclient for http rest calls
     */

    public static ElastiCenterClient restclient = null;
    
    
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
    public static String ES_IP_VAL = "";
    public static String ES_API_KEY_VAL = "";
    public static String ES_ACCOUNT_ID_VAL = "";
    public static String ES_POOL_ID_VAL = "";
    public static String ES_SUBNET_VAL = "";
    public static String ES_INTERFACE_VAL = "";
    public static String ES_GATEWAY_VAL = "";

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
    
    public static void setElastistorRestClient(String managementIp , String apiKey) {
        try {
            
            restclient = new ElastiCenterClient(managementIp, apiKey);
            
            s_logger.info("ELASTICENTER REST CLIENT INTIALIZED");
            
        } catch (InvalidCredentialsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (InvalidParameterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SSLHandshakeException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (ServiceUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void setElastistorApiKey(String value) {
        ES_API_KEY_VAL = value;
    }

    public static void setElastistorManagementIp(String value) {
        ES_IP_VAL = value;
    }

    public static void setElastistorPoolId(String value) {
        ES_POOL_ID_VAL = value;
    }

    public static void setElastistorAccountId(String value) {
        ES_ACCOUNT_ID_VAL = value;
    }
    
    public static void setElastistorGateway(String value) {
        ES_GATEWAY_VAL = value;
    }

    public static void setElastistorInterface(String value) {
        ES_INTERFACE_VAL = value;
    }

    public static void setElastistorSubnet(String value) {
        ES_SUBNET_VAL = value;
    }

    

    public static CreateTsmCmdResponse createElastistorTsm(String storagePoolName, String storageIp, Long capacityBytes, Long capacityIops) throws Throwable {
        
           s_logger.info("creation of elastistor plugin started");
        
            String quotasize; 
            String totalthroughput = String.valueOf(capacityIops*4);
            String totaliops = String.valueOf(capacityIops);
        
           if((1099511627776L)>capacityBytes &&(capacityBytes>(1073741824))){
                
                quotasize =(String.valueOf(capacityBytes/(1024*1024*1024))+"G");
            }
            else
            {
                int temp1 = (int) (capacityBytes/(1024*1024*1024));
                int temp2  = temp1/1024;
                
                quotasize =(String.valueOf(temp2)+"T");
                
            }          
        
          CreateTsmCmd cmd = new CreateTsmCmd();
        
    
          if ( null != ElastistorUtil.ES_ACCOUNT_ID_VAL ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_ACCOUNTID, ElastistorUtil.ES_ACCOUNT_ID_VAL);
          if ( null != totalthroughput ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_TSM_THROUGHPUT, totalthroughput);
          if ( null != ElastistorUtil.ES_POOL_ID_VAL ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_POOLID, ElastistorUtil.ES_POOL_ID_VAL);
          if ( null != storagePoolName ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_NAME, "TSM"+storagePoolName);
          if ( null != quotasize ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_QUOTA_SIZE, quotasize);
          if ( null != storageIp ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_IPADDRESS, storageIp);
          if ( null != ElastistorUtil.ES_SUBNET_VAL ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_SUBNET, ElastistorUtil.ES_SUBNET_VAL);
          if ( null != ElastistorUtil.ES_GATEWAY_VAL ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_GATEWAY, ElastistorUtil.ES_GATEWAY_VAL);
          if ( null != ElastistorUtil.ES_INTERFACE_VAL ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_INTERFACE, ElastistorUtil.ES_INTERFACE_VAL);           
          if ( null != ElastistorUtil.ES_NOOFCOPIES_VAL ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_NOOFCOPIES, ElastistorUtil.ES_NOOFCOPIES_VAL);           
          if ( null != ElastistorUtil.ES_BLOCKSIZE_VAL ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_RECORDSIZE, ElastistorUtil.ES_BLOCKSIZE_VAL);      
          if ( null != totaliops ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_TOTALIOPS, totaliops);
          if ( null != ElastistorUtil.ES_LATENCY_VAL ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_LATENCY, ElastistorUtil.ES_LATENCY_VAL);
          if ( null != ElastistorUtil.ES_BLOCKSIZE_VAL ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_BLOCKSIZE, ElastistorUtil.ES_BLOCKSIZE_VAL);
          if ( null != ElastistorUtil.ES_GRACEALLOWED_VAL ) cmd.putCommandParameter(ElastistorUtil.REST_PARAM_GRACEALLOWED, ElastistorUtil.ES_GRACEALLOWED_VAL);
          
        CreateTsmCmdResponse cmdResponse;
        
    try {
        cmdResponse = (CreateTsmCmdResponse) ElastistorUtil.restclient.executeCommand(cmd);
        
        if ( cmdResponse.getTsm().getUuid() == null  ){
             
             throw new CloudRuntimeException("tsm creation failed , contact elatistor admin");
         }
        
        return cmdResponse;
        
    } catch (Exception e) {
        e.printStackTrace();
        s_logger.error("tsm creation failed");
        throw new CloudRuntimeException("tsm creation failed , contact elatistor admin");       
    }
        
    }
    
    
    public static CreateVolumeCmdResponse createElastistorVolume(String storagePoolName, CreateTsmCmdResponse cmdResponse, Long capacityBytes, Long capacityIops,String protocoltype, String mountpoint) throws Throwable {

        String datasetid;
        String tsmid;
        String qosgroupid;
        String VolumeName = storagePoolName;
        String totaliops = String.valueOf(capacityIops);
        String totalthroughput = String.valueOf(capacityIops*4);

        String quotasize; 
            
        if((1099511627776L)>capacityBytes &&(capacityBytes>(1073741824))){
                quotasize =(String.valueOf(capacityBytes/(1024*1024*1024))+"G");
            }
            else
            {
                int temp1 = (int) (capacityBytes/(1024*1024*1024));
                int temp2  = temp1/1024;                
                quotasize =(String.valueOf(temp2)+"T");
                
            }
        
           
           
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

        AddQosGroupCmdResponse addQosGroupCmdResponse = (AddQosGroupCmdResponse) ElastistorUtil.restclient.executeCommand(addQosGroupCmd);

        if (addQosGroupCmdResponse.getQoSGroup().getUuid() == null) {

            throw new CloudRuntimeException("adding qos group failed , contact elatistor admin");

        }

        else {

            CreateVolumeCmd createVolumeCmd = new CreateVolumeCmd();

            qosgroupid = addQosGroupCmdResponse.getQoSGroup().getUuid();

            if (null != ElastistorUtil.ES_ACCOUNT_ID_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ACCOUNTID,ElastistorUtil.ES_ACCOUNT_ID_VAL);
            if (null != qosgroupid)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_QOSGROUPID, qosgroupid);
            if (null != tsmid)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_TSMID, tsmid);
            if (null != ElastistorUtil.ES_POOL_ID_VAL)createVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_POOLID,ElastistorUtil.ES_POOL_ID_VAL);
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
                createVolumeCmdResponse = (CreateVolumeCmdResponse) ElastistorUtil.restclient.executeCommand(createVolumeCmd);
                
                if (createVolumeCmdResponse.getFileSystem().getUuid() == null) {

                    throw new CloudRuntimeException("creating volume failed , contact elatistor admin");

                } else {
                    
                    return createVolumeCmdResponse;
                }
                
                
            } catch (Exception e) {
                e.printStackTrace();
                throw new CloudRuntimeException("creating volume failed , contact elatistor admin");
            }           

        }

    }

    @SuppressWarnings("null")
    public static boolean deleteElastistorVolume(String poolip, String esmanagementip, String esapikey) throws Throwable {

        String esvolumeid = null;
        String estsmid = null;

        ListTsmsResponse listTsmsResponse = listTsm(poolip);

        if (listTsmsResponse.getTsmsCount() != 0) {

            int i;

            for (i = 0; i < listTsmsResponse.getTsmsCount(); i++) {
                if (poolip.compareTo(listTsmsResponse.getTsms().getTsm(i)
                        .getIpaddress()) == 0) {
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
                DeleteTsmResponse deleteTsmResponse = (DeleteTsmResponse) restclient.executeCommand(deleteTsmCmd);

                if (deleteTsmResponse != null) {
                    String jobstatus = deleteTsmResponse.getJobStatus();

                    if (jobstatus.equalsIgnoreCase("true")) {
                        s_logger.info("delete elastistor tsm successful");
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
                s_logger.info("no volume is present in the tsm");
            }

        } else {
            s_logger.info("List tsm failed, no tsm present in the eastistor for the given IP ");
            return false;
        }
        return false;

    }

    private static ListHAPoolResponse listPool(String poolid) throws Throwable {

        ListHAPoolCmd listHAPoolCmd = new ListHAPoolCmd();

        listHAPoolCmd.putCommandParameter(ElastistorUtil.REST_PARAM_POOLID,poolid);

        ListHAPoolResponse listHAPoolResponse = (ListHAPoolResponse) restclient.executeCommand(listHAPoolCmd);

        return listHAPoolResponse;
    }

    private static ListTsmsResponse listTsm(String poolip) throws Throwable {

        ListTsmCmd listTsmCmd = new ListTsmCmd();

        listTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_IPADDRESS,poolip);

        ListTsmsResponse listTsmsResponse = (ListTsmsResponse) restclient.executeCommand(listTsmCmd);

        return listTsmsResponse;
    }

    private static DeleteVolumeResponse deleteVolume(String esvolumeid, String forcedelete)throws Throwable {

        DeleteVolumeCmd deleteVolumeCmd = new DeleteVolumeCmd();

        deleteVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ID,esvolumeid);
        deleteVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_FORECEDELETE, forcedelete);
        
        DeleteVolumeResponse deleteVolumeResponse = (DeleteVolumeResponse) restclient.executeCommand(deleteVolumeCmd);

        return deleteVolumeResponse;
    }

    private static int queryAsyncJobResult(String jobid) throws Throwable {

        QueryAsyncJobResultCmd asyncJobResultCmd = new QueryAsyncJobResultCmd();

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

    public static Boolean updateElastistorVolumeSize(VolumeObject vol,StoragePool pool, Long newSize) throws Throwable {

        ElastiCenterClient restClient = new ElastiCenterClient(ES_IP_VAL, ES_API_KEY_VAL);
        Boolean status = false;
        String poolip = pool.getHostAddress();

        ListTsmsResponse listTsmsResponse = listTsm(poolip);
        String esvolumeid = null;
        String esdatasetid = null;
        int i;

        for (i = 0; i < listTsmsResponse.getTsmsCount(); i++) {
            if (poolip.compareTo(listTsmsResponse.getTsms().getTsm(i).getIpaddress()) == 0) {
                esvolumeid = listTsmsResponse.getTsms().getTsm(i).getVolumeProperties(0).getid();
                esdatasetid = listTsmsResponse.getTsms().getTsm(i).getDatasetid();
                break;
            }
        }
        
        String quotasize = (String.valueOf(newSize / (1024 * 1024 * 1024)) + "G");

        String protocol;

        if (pool.getPoolType() == StoragePoolType.IscsiLUN) {
            protocol = "iscsi";
        } else {
            protocol = "nfs";
        }
        
        UpdateStorageCmdResponse updateStorageCmdResponse = updateStorage(quotasize, esdatasetid);
        
        UpdateFileSystemCmdResponse fileSystemCmdResponse = updateFileSystem(quotasize, protocol, esvolumeid);

        status = true;

        return status;
    }

    public static Answer updateElastistorVolumeQosGroup(VolumeObject vol,StoragePool pool, Long newIOPS) throws Throwable {

        String poolip = pool.getHostAddress();
        ListHAPoolResponse listHAPoolResponse = listPool(ES_POOL_ID_VAL);

        if (listHAPoolResponse.getPool().getUuid() != null) {
            String availiops = listHAPoolResponse.getPool().getAvailIOPS();

            Long temp = (long) Integer.parseInt(availiops);

            if (newIOPS > temp) {
                return new Answer(null,false,"elastistor does not have enough iops in the pool, the available IOPS in the pool : " + temp);
            }

        }

        ListTsmsResponse listTsmsResponse = listTsm(poolip);
        String iops = String.valueOf(newIOPS);
        String esQosgroupid = null;
        String estsmid = null;
        String currentiops = null;
        int i;

        for (i = 0; i < listTsmsResponse.getTsmsCount(); i++) {
            if (poolip.compareTo(listTsmsResponse.getTsms().getTsm(i).getIpaddress()) == 0) {
                
                estsmid = listTsmsResponse.getTsms().getTsm(i).getUuid();
                currentiops = listTsmsResponse.getTsms().getTsm(i).getVolumeProperties(0).getIops();
                esQosgroupid = listTsmsResponse.getTsms().getTsm(i).getVolumeProperties(0).getQosgroupid();
                break;
            }
        }

        long ciops = Long.valueOf(currentiops).longValue();

        if (ciops < newIOPS) {
            
            updateQosGroupTsm(iops, estsmid);

            updateQosGroupVolume(iops, esQosgroupid);

        } else {

            updateQosGroupVolume(iops, esQosgroupid);

            updateQosGroupTsm(iops, estsmid);

        }

        return new Answer(null, true, null);
    }

    private static UpdateStorageCmdResponse updateStorage(String quotasize, String esdatasetid)throws Throwable {

        UpdateStorageCmd updateStorageCmd = new UpdateStorageCmd();

        updateStorageCmd.putCommandParameter("id", esdatasetid);
        updateStorageCmd.putCommandParameter("quotasize", quotasize);

        UpdateStorageCmdResponse updateStorageCmdResponse = (UpdateStorageCmdResponse) restclient.executeCommand(updateStorageCmd);

        return updateStorageCmdResponse;
    }

    private static UpdateFileSystemCmdResponse updateFileSystem(String quotasize, String protocol,String esvolumeid) throws Throwable {

        UpdateFileSystemCmd fileSystemCmd = new UpdateFileSystemCmd();

        fileSystemCmd.putCommandParameter("id", esvolumeid);
        fileSystemCmd.putCommandParameter("quotasize", quotasize);
        if (protocol == "nfs") {
            fileSystemCmd.putCommandParameter("recordsize", "4k");
        } else {
            fileSystemCmd.putCommandParameter("blocklength", "512B");
        }
        UpdateFileSystemCmdResponse fileSystemCmdResponse = (UpdateFileSystemCmdResponse) restclient.executeCommand(fileSystemCmd);

        return fileSystemCmdResponse;
    }

    private static UpdateTsmCmdResponse updateQosGroupTsm(String iops, String tsmid)throws Throwable {

        UpdateTsmCmd updateTsmCmd = new UpdateTsmCmd();
        
        updateTsmCmd.putCommandParameter("id", tsmid);
        updateTsmCmd.putCommandParameter("iops", iops);
        int throughtput = Integer.parseInt(iops) * 4;
        updateTsmCmd.putCommandParameter("throughput",String.valueOf(throughtput));

        UpdateTsmCmdResponse updateTsmCmdResponse = (UpdateTsmCmdResponse) restclient.executeCommand(updateTsmCmd);

        return updateTsmCmdResponse;
    }

    private static UpdateQosGroupCmdResponse updateQosGroupVolume(String iops, String qosgroupid)throws Throwable {

        UpdateQosGroupCmd updateQosGroupCmd = new UpdateQosGroupCmd();
        updateQosGroupCmd.putCommandParameter("id", qosgroupid);
        updateQosGroupCmd.putCommandParameter("iops", iops);
        updateQosGroupCmd.putCommandParameter("graceallowed", "false");

        UpdateQosGroupCmdResponse updateQosGroupCmdResponse = (UpdateQosGroupCmdResponse) restclient.executeCommand(updateQosGroupCmd);

        return updateQosGroupCmdResponse;
    }

}
