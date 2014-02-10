package org.apache.cloudstack.storage.datastore.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.cloudstack.storage.datastore.client.ElastiCenterClient;
import org.apache.cloudstack.storage.datastore.command.AddQosGroupCmd;
import org.apache.cloudstack.storage.datastore.command.CreateVolumeCmd;
import org.apache.cloudstack.storage.datastore.command.DeleteTsmCmd;
import org.apache.cloudstack.storage.datastore.command.DeleteVolumeCmd;
import org.apache.cloudstack.storage.datastore.command.ListTsmCmd;
import org.apache.cloudstack.storage.datastore.command.QueryAsyncJobResultCmd;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.datastore.response.AddQosGroupCmdResponse;
import org.apache.cloudstack.storage.datastore.response.CreateVolumeCmdResponse;
import org.apache.cloudstack.storage.datastore.response.DeleteTsmResponse;
import org.apache.cloudstack.storage.datastore.response.DeleteVolumeResponse;
import org.apache.cloudstack.storage.datastore.response.ListTsmsResponse;
import org.apache.cloudstack.storage.datastore.response.QueryAsyncJobResultResponse;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;

import com.cloud.storage.VolumeVO;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * The util class for elastistor's storage plugin codebase.
 *
 * @author amit.das@cloudbyte.com
 * @author punith.s@cloudbyte.com
 *
 */
public class ElastistorUtil {

    /**
     * Elastistor REST API Connection Info
     */
    private static final String REST_PROTOCOL = "https";
    private static final String REST_CONTEXT_PATH = "client/api";
    private static final String REST_VALUE_RESPONSE = "json";

    /**
     * Elastistor REST API Commands. Refer ElastistorUtil.RESTApi
     */

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
    public static String esip = "";
    public static String esapikey = "";
    public static String esaccountid = "";
    public static String espoolid = "";

    /**
     * Error messages
     */
    private static final String PRESETUP_VOL_NOT_AVAILABLE = "A presetup volume is not available for account [%s].";
    private static final String FILESYSTEM_ID_NOT_AVAILABLE = "File system id is not available for this operation [%s].";
    private static final String INVALID_ARGUMENTS = "Invalid or null arguments were provided for this operation [%s] on account [%s].";
    private static final String DESERIALIZATION_ERROR = "Error while deserializing json to [%s] using key [%s].";
    public static final String ELASTISTOR_ACCOUNT_MISSING = "Elastistor does not have any account named [%s].";

    /**
     * Private constructor s.t. its never instantiated.
     */
    private ElastistorUtil() {

    }

    public static void setElastistorApiKey(String value) {
        esapikey = value;
    }

    public static void setElastistorManagementIp(String value) {
        esip = value;
    }

    public static void setElastistorPoolId(String value) {
        espoolid = value;
    }

    public static void setElastistorAccountId(String value) {
        esaccountid = value;
    }

    public enum ElastistorStoragePoolType {
    }

    private static ElastistorFilesystem deleteVolume(String esFilesystemId,
            ElastistorConnectionInfo esConnectionInfo) {
        // prepare REST arguments
        Map<String, String> restArgs = new HashMap<String, String>();
        restArgs.put(REST_PARAM_ID, esFilesystemId);
        restArgs.put(REST_PARAM_QUOTA_SIZE, "1M");
        restArgs.put(REST_PARAM_READONLY, "false");

        String strElastistorResultJson = executeHttpReq(esConnectionInfo,
                RESTApi.DEL_ES_VOLUME.setPairs(restArgs));

        return (ElastistorFilesystem) deserializeJsonToJava(
                strElastistorResultJson, "filesystem",
                ElastistorFilesystem.class);

    }

    private static ElastistorFilesystem createVolume(long volSize,
            ElastistorFilesystem esFilesystem,
            ElastistorConnectionInfo esConnectionInfo) {

        // prepare REST arguments
        Map<String, String> restArgs = new HashMap<String, String>();
        restArgs.put(REST_PARAM_ID, esFilesystem.getId());
        String size = String.valueOf(volSize) + "M";
        restArgs.put(REST_PARAM_QUOTA_SIZE, size);
        restArgs.put(REST_PARAM_READONLY, "false");

        String strElastistorFilesystemJson = executeHttpReq(esConnectionInfo,
                RESTApi.POST_ES_VOLUME.setPairs(restArgs));

        return (ElastistorFilesystem) deserializeJsonToJava(
                strElastistorFilesystemJson, "filesystem",
                ElastistorFilesystem.class);
    }

    private static List<ElastistorFilesystem> selectIscsiFileSystems(
            ElastistorAccount esAccount) {
        List<ElastistorFilesystem> esFilesystems = esAccount
                .getFilesystemslist();
        if (null == esFilesystems || 0 == esFilesystems.size()) {
            return null;
        }

        List<ElastistorFilesystem> selectedEsFilesystems = (List<ElastistorFilesystem>) Collections2
                .filter(esFilesystems, new Predicate<ElastistorFilesystem>() {
                    @Override
                    public boolean apply(ElastistorFilesystem filesystem) {
                        return filesystem.isIscsi();
                    }
                });

        return selectedEsFilesystems;

    }

    private static List<ElastistorFilesystem> filterFileSystemsOf1MB(
            List<ElastistorFilesystem> esFilesystems) {

        if (null == esFilesystems || 0 == esFilesystems.size()) {
            return null;
        }

        List<ElastistorFilesystem> filteredEsFilesystems = (List<ElastistorFilesystem>) Collections2
                .filter(esFilesystems, new Predicate<ElastistorFilesystem>() {
                    @Override
                    public boolean apply(ElastistorFilesystem filesystem) {
                        return filesystem.quota.contains("1M")
                                || filesystem.quota.contains("1MB");
                    }
                });

        return filteredEsFilesystems;
    }

    private static List<ElastistorTSM> filterTsmsBySize(final long size,
            List<ElastistorTSM> tsms) {

        if (null == tsms || 0 == tsms.size()) {
            return null;
        }

        List<ElastistorTSM> filteredTsms = (List<ElastistorTSM>) Collections2
                .filter(tsms, new Predicate<ElastistorTSM>() {
                    @Override
                    public boolean apply(ElastistorTSM tsm) {
                        return tsm.getAvailspace() > size;
                    }
                });

        return filteredTsms;
    }

    private static List<ElastistorFilesystem> filterFilesystemsByTsms(
            List<ElastistorTSM> tsms, List<ElastistorFilesystem> filesystems) {

        if (null == filesystems || 0 == filesystems.size()) {
            return null;
        }

        List<ElastistorFilesystem> filteredFilesystems = new ArrayList<ElastistorUtil.ElastistorFilesystem>();
        for (ElastistorTSM tsm : tsms) {
            filteredFilesystems
                    .addAll(filterFilesystemsByTsm(tsm, filesystems));
        }

        return filteredFilesystems;
    }

    private static List<ElastistorFilesystem> filterFilesystemsByTsm(
            ElastistorTSM tsm, List<ElastistorFilesystem> esFilesystems) {

        if (null == esFilesystems || 0 == esFilesystems.size()) {
            return null;
        }

        final String tsmId = tsm.getId();

        List<ElastistorFilesystem> selectedEsFilesystems = (List<ElastistorFilesystem>) Collections2
                .filter(esFilesystems, new Predicate<ElastistorFilesystem>() {
                    @Override
                    public boolean apply(ElastistorFilesystem filesystem) {
                        if (null != filesystem.getTsmid()) {
                            return filesystem.getTsmid().equals(tsmId);
                        } else {
                            return false;
                        }
                    }
                });

        return selectedEsFilesystems;
    }

    /**
     * This class holds the connection details to elastistor .
     *
     */
    public static final class ElastistorConnectionInfo {
        private final String _managementIP;
        private final int _managementPort;
        private final String _apiKey;

        public ElastistorConnectionInfo(String managementIP,int managementPort, String apiKey) {
            _managementIP = managementIP;
            _managementPort = managementPort;
            _apiKey = apiKey;
        }

        public String getManagementIP() {
            return _managementIP;
        }

        public int getManagementPort() {
            return _managementPort;
        }

        public String getApiKey() {
            return _apiKey;
        }

    }

    /**
     * This class is the representation of an elastistor's generic REST
     * response. The normal naming conventions are not followed here s.t. gson
     * can de-serialize the rest response to this bean easily.
     *
     */
    public static final class ElastistorRestResponse {
        private int count;
        private boolean success;
        private String jobid;

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getJobid() {
            return jobid;
        }

        public void setJobid(String jobid) {
            this.jobid = jobid;
        }

    }

    /**
     * This class is the representation of an elastistor's account as received
     * from a REST response.
     *
     * Note - The code naming conventions are not followed here s.t. gson can
     * de-serialize the rest response to this bean easily.
     *
     */
    public static final class ElastistorAccount {

        private String id;
        private String name;
        private long availIOPS;
        private long totaliops;
        private long usedIOPS;
        private long currentUsedSpace;
        private long currentAvailableSpace;
        private List<ElastistorVolume> volumes;
        private List<ElastistorTSM> tsms;
        private List<ElastistorFilesystem> filesystemslist;

        public List<ElastistorFilesystem> getFilesystemslist() {
            return filesystemslist;
        }

        public void setFilesystemslist(
                List<ElastistorFilesystem> filesystemslist) {
            this.filesystemslist = filesystemslist;
        }

        public List<ElastistorVolume> getVolumes() {
            return volumes;
        }

        public void setVolumes(List<ElastistorVolume> volumes) {
            this.volumes = volumes;
        }

        public List<ElastistorTSM> getTsms() {
            return tsms;
        }

        public void setTsms(List<ElastistorTSM> tsms) {
            this.tsms = tsms;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getAvailIOPS() {
            return availIOPS;
        }

        public void setAvailIOPS(long availIOPS) {
            this.availIOPS = availIOPS;
        }

        public long getTotaliops() {
            return totaliops;
        }

        public void setTotaliops(long totaliops) {
            this.totaliops = totaliops;
        }

        public long getUsedIOPS() {
            return usedIOPS;
        }

        public void setUsedIOPS(long usedIOPS) {
            this.usedIOPS = usedIOPS;
        }

        public long getCurrentUsedSpace() {
            return currentUsedSpace;
        }

        public void setCurrentUsedSpace(long currentUsedSpace) {
            this.currentUsedSpace = currentUsedSpace;
        }

        public long getCurrentAvailableSpace() {
            return currentAvailableSpace;
        }

        public void setCurrentAvailableSpace(long currentAvailableSpace) {
            this.currentAvailableSpace = currentAvailableSpace;
        }

    }

    /**
     * This class represents an elastistor TSM as received from a REST response.
     * This is specific to elastistor & probably should be removed in future
     * plugin implementations when elastistor API has improved provisioning
     * capability.
     *
     * Note - The code naming conventions are not followed here s.t. gson can
     * de-serialize the rest response to this bean easily.
     *
     * @author amit.das@cloudbyte.com
     *
     */
    public static final class ElastistorTSM {

        private String id;
        private String name;
        private String ipaddress;
        private long availiops;
        private long availthroughput;
        private long availspace;
        private String datasetid;

        public String getDatasetid() {
            return datasetid;
        }

        public void setDatasetid(String datasetid) {
            this.datasetid = datasetid;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getIpaddress() {
            return ipaddress;
        }

        public void setIpaddress(String ipaddress) {
            this.ipaddress = ipaddress;
        }

        public long getAvailiops() {
            return availiops;
        }

        public void setAvailiops(long availiops) {
            this.availiops = availiops;
        }

        public long getAvailthroughput() {
            return availthroughput;
        }

        public void setAvailthroughput(long availthroughput) {
            this.availthroughput = availthroughput;
        }

        public long getAvailspace() {
            return availspace;
        }

        public void setAvailspace(long availspace) {
            this.availspace = availspace;
        }

    }

    /**
     * This class is the representation of an elastistor's volume as received
     * from a REST response.
     *
     * Note - The code naming conventions are not followed here s.t. gson can
     * de-serialize the rest response to this bean easily.
     *
     */
    public static final class ElastistorVolume {

        private String name;
        private String type;
        private String path;
        private String accountid;
        private String accountname;
        private String mountpoint;
        private String ipaddress;
        private boolean nfs;
        private boolean cifs;
        private boolean iscsi;
        private boolean fc;
        private String id;
        private String groupId;
        private String tsmId;
        private long iops;
        private long totalspace;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getAccountid() {
            return accountid;
        }

        public void setAccountid(String accountid) {
            this.accountid = accountid;
        }

        public String getAccountname() {
            return accountname;
        }

        public void setAccountname(String accountname) {
            this.accountname = accountname;
        }

        public String getMountpoint() {
            return mountpoint;
        }

        public void setMountpoint(String mountpoint) {
            this.mountpoint = mountpoint;
        }

        public String getIpaddress() {
            return ipaddress;
        }

        public void setIpaddress(String ipaddress) {
            this.ipaddress = ipaddress;
        }

        public boolean isNfs() {
            return nfs;
        }

        public void setNfs(boolean nfs) {
            this.nfs = nfs;
        }

        public boolean isCifs() {
            return cifs;
        }

        public void setCifs(boolean cifs) {
            this.cifs = cifs;
        }

        public boolean isIscsi() {
            return iscsi;
        }

        public void setIscsi(boolean iscsi) {
            this.iscsi = iscsi;
        }

        public boolean isFc() {
            return fc;
        }

        public void setFc(boolean fc) {
            this.fc = fc;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getTsmId() {
            return tsmId;
        }

        public void setTsmId(String tsmId) {
            this.tsmId = tsmId;
        }

        public long getIops() {
            return iops;
        }

        public void setIops(long iops) {
            this.iops = iops;
        }

        public long getTotalspace() {
            return totalspace;
        }

        public void setTotalspace(long totalspace) {
            this.totalspace = totalspace;
        }

    }

    public static class ElastistorFilesystem {
        private String id;
        private String name;
        private String type;
        private String path;
        private String tsmid;
        private String quota;
        private long availspace;
        private boolean cifs;
        private boolean nfs;
        private boolean fc;
        private boolean iscsi;
        private long iops;
        private long throughput;
        private String mountpoint;

        public String getMountpoint() {
            return mountpoint;
        }

        public void setMountpoint(String mountpoint) {
            this.mountpoint = mountpoint;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getTsmid() {
            return tsmid;
        }

        public void setTsmid(String tsmid) {
            this.tsmid = tsmid;
        }

        public String getQuota() {
            return quota;
        }

        public void setQuota(String quota) {
            this.quota = quota;
        }

        public long getAvailspace() {
            return availspace;
        }

        public void setAvailspace(long availspace) {
            this.availspace = availspace;
        }

        public boolean isCifs() {
            return cifs;
        }

        public void setCifs(boolean cifs) {
            this.cifs = cifs;
        }

        public boolean isNfs() {
            return nfs;
        }

        public void setNfs(boolean nfs) {
            this.nfs = nfs;
        }

        public boolean isFc() {
            return fc;
        }

        public void setFc(boolean fc) {
            this.fc = fc;
        }

        public boolean isIscsi() {
            return iscsi;
        }

        public void setIscsi(boolean iscsi) {
            this.iscsi = iscsi;
        }

        public long getIops() {
            return iops;
        }

        public void setIops(long iops) {
            this.iops = iops;
        }

        public long getThroughput() {
            return throughput;
        }

        public void setThroughput(long throughput) {
            this.throughput = throughput;
        }
    }

    /**
     * Enum to represent all the REST supported commands supported at
     * elastistor.
     *
     */
    private static enum RESTApi {
        GET_ES_ACCOUNT("listAccount2"), DEL_ES_VOLUME("updateFileSystem"), POST_ES_VOLUME(
                "updateFileSystem");

        private Map<String, String> _pairs;
        private String _restCommandName;

        /**
         * constructor
         *
         */
        private RESTApi(String restCommandName) {
            _restCommandName = restCommandName;
            _pairs = new HashMap<String, String>();
        }

        public Map<String, String> getPairs() {
            return _pairs;
        }

        public RESTApi setPair(String key, String value) {
            _pairs.put(key, value);
            return this;
        }

        public RESTApi setPairs(Map<String, String> args) {
            _pairs.putAll(args);
            return this;
        }

        public String getCommandName() {
            return _restCommandName;
        }
    }

    /**
     * Get the elastistor account details. This should create a new account in
     * elastistor if not present.
     *
     * @param esAccountName
     *            represents the es account name, composed of cs account
     *            properties
     * @param esConnectionInfo
     *            has the connection details to es
     * @return {@link ElastistorAccount} the elastistor account that was queried
     */
    public static ElastistorAccount getElastistorAccountByName(
            ElastistorConnectionInfo esConnectionInfo, String esAccountName) {

        String strElastistorAccountJson = executeHttpReq(esConnectionInfo,
                RESTApi.GET_ES_ACCOUNT.setPair(REST_PARAM_KEYWORD,
                        esAccountName));

        return (ElastistorAccount) deserializeJsonToJava(
                strElastistorAccountJson, "account", ElastistorAccount.class);

    }

    public static VolumeVO createElastistorVolume(StoragePoolVO storagePool,
            VolumeVO volume, String esmanagementip, String esapikey)
            throws Throwable {
        ElastiCenterClient restClient = new ElastiCenterClient(esmanagementip,
                esapikey);

        // AddQosGroup parameters
        String memlimit = "0";
        String networkspeed = "0";
        String datasetid;
        String tsmid;

        // createVolume parameters
        String qosgroupid;
        String deduplication = "off";
        String compression = "off";
        String sync = "always";
        String casesensitivity = "sensitive";
        String readonly = "off";
        String unicode = "off";
        String authnetwork = "all";
        String mapuserstoroot = "yes";

        String VolumeName = volume.getName();
        String totaliops = String.valueOf(volume.getMaxIops());
        String latency = "15";
        String blocksize = "4K";
        String totalthroughput = String.valueOf(volume.getMaxIops() * 4);
        String graceallowed = "false";
        String quotasize = String.valueOf(volume.getSize()
                / (1024 * 1024 * 1024));
        String noofcopies = "1";

        AddQosGroupCmd cmd2 = new AddQosGroupCmd();

        ListTsmsResponse listTsmsResponse = listTsm(restClient,
                storagePool.getHostAddress());

        tsmid = listTsmsResponse.getTsms().getTsm(0).getUuid();
        datasetid = listTsmsResponse.getTsms().getTsm(0).getDatasetid();

        if (null != VolumeName)
            cmd2.putCommandParameter("name", "QOS_" + VolumeName);
        if (null != totaliops)
            cmd2.putCommandParameter("iops", totaliops);
        if (null != latency)
            cmd2.putCommandParameter("latency", latency);
        if (null != blocksize)
            cmd2.putCommandParameter("blocksize", blocksize);
        if (null != totalthroughput)
            cmd2.putCommandParameter("throughput", totalthroughput);
        if (null != memlimit)
            cmd2.putCommandParameter("memlimit", memlimit);
        if (null != networkspeed)
            cmd2.putCommandParameter("networkspeed", networkspeed);
        if (null != tsmid)
            cmd2.putCommandParameter("tsmid", tsmid);
        if (null != datasetid)
            cmd2.putCommandParameter("datasetid", datasetid);
        if (null != graceallowed)
            cmd2.putCommandParameter("graceallowed", graceallowed);

        AddQosGroupCmdResponse cmdResponse2 = (AddQosGroupCmdResponse) restClient
                .executeCommand(cmd2);

        if (cmdResponse2.getQoSGroup().getUuid() == null) {

            // s_logger.error("*************ADD QOS GROUP FAILED *********************");
            throw new CloudRuntimeException(
                    "ADD QOS GROUP FAILED , contact elatistor admin");

        }

        else {

            CreateVolumeCmd cmd3 = new CreateVolumeCmd();

            qosgroupid = cmdResponse2.getQoSGroup().getUuid();

            if (null != ElastistorUtil.esaccountid)
                cmd3.putCommandParameter(ElastistorUtil.REST_PARAM_ACCOUNTID,
                        ElastistorUtil.esaccountid);
            if (null != qosgroupid)
                cmd3.putCommandParameter("qosgroupid", qosgroupid);
            if (null != tsmid)
                cmd3.putCommandParameter("tsmid", tsmid);
            if (null != ElastistorUtil.espoolid)
                cmd3.putCommandParameter(ElastistorUtil.REST_PARAM_POOLID,
                        ElastistorUtil.espoolid);
            if (null != VolumeName)
                cmd3.putCommandParameter("name", VolumeName);
            if (null != quotasize)
                cmd3.putCommandParameter("quotasize", quotasize);
            if (null != blocksize)
                cmd3.putCommandParameter("recordsize", blocksize);
            if (null != deduplication)
                cmd3.putCommandParameter("deduplication", deduplication);
            if (null != sync)
                cmd3.putCommandParameter("sync", sync);
            if (null != compression)
                cmd3.putCommandParameter("compression", compression);
            if (null != noofcopies)
                cmd3.putCommandParameter("noofcopies", noofcopies);
            cmd3.putCommandParameter("mountpoint", "/" + volume.getName());
            if (null != casesensitivity)
                cmd3.putCommandParameter("casesensitivity", casesensitivity);
            if (null != readonly)
                cmd3.putCommandParameter("readonly", readonly);
            if (null != datasetid)
                cmd3.putCommandParameter("datasetid", datasetid);
            if (null != unicode)
                cmd3.putCommandParameter("unicode", unicode);
            cmd3.putCommandParameter("protocoltype", "iscsi");
            if (null != authnetwork)
                cmd3.putCommandParameter("authnetwork", authnetwork);
            if (null != mapuserstoroot)
                cmd3.putCommandParameter("mapuserstoroot", mapuserstoroot);

            CreateVolumeCmdResponse cmdResponse3 = (CreateVolumeCmdResponse) restClient
                    .executeCommand(cmd3);

            if (cmdResponse3.getFileSystem().getUuid() == null) {
                // s_logger.error("*************CREATING VOLUME FAILED *********************");
                throw new CloudRuntimeException(
                        "CREATING VOLUME FAILED , contact elatistor admin");

            } else {
                System.out.println("elastistor volume creation complete");
            }

            volume.set_iScsiName(cmdResponse3.getFileSystem().getIqn());
            volume.setFolder(String.valueOf(cmdResponse3.getFileSystem()
                    .getUuid()));

        }

        return volume;
    }

    @SuppressWarnings("null")
    public static boolean deleteElastistorVolume(String poolip,
            String esmanagementip, String esapikey) throws Throwable {
        ElastiCenterClient restClient = new ElastiCenterClient(esmanagementip,
                esapikey);

        String esvolumeid;
        String estsmid;
        /*
         * ListTsmCmd listTsmCmd = new ListTsmCmd();
         *
         * listTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_IPADDRESS,
         * poolip);
         *
         * ListTsmsResponse listTsmsResponse = (ListTsmsResponse)
         * restClient.executeCommand( listTsmCmd );
         */

        ListTsmsResponse listTsmsResponse = listTsm(restClient, poolip);

        if (listTsmsResponse.getTsmsCount() != 0) {
            // getting cloudbyte volume id and tsm id

            estsmid = listTsmsResponse.getTsms().getTsm(0).getUuid();

            if (listTsmsResponse.getTsms().getTsm(0).checkvolume()) {
                esvolumeid = listTsmsResponse.getTsms().getTsm(0)
                        .getVolumeProperties(0).getid();
                /*
                 * DeleteVolumeCmd deleteVolumeCmd = new DeleteVolumeCmd();
                 *
                 * deleteVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ID
                 * , esvolumeid); DeleteVolumeResponse deleteVolumeResponse =
                 * (DeleteVolumeResponse) restClient.executeCommand(
                 * deleteVolumeCmd );
                 */

                DeleteVolumeResponse deleteVolumeResponse = deleteVolume(
                        restClient, esvolumeid, null);

                if (deleteVolumeResponse != null) {

                    String jobid = deleteVolumeResponse.getJobId();

                    /*
                     * QueryAsyncJobResultCmd asyncJobResultCmd = new
                     * QueryAsyncJobResultCmd();
                     *
                     * asyncJobResultCmd.putCommandParameter(ElastistorUtil.
                     * REST_PARAM_JOBID, jobid);
                     *
                     * QueryAsyncJobResultResponse asyncJobResultResponse =
                     * (QueryAsyncJobResultResponse) restClient.executeCommand(
                     * asyncJobResultCmd );
                     *
                     * if(asyncJobResultResponse != null) { int jobstatus =
                     * asyncJobResultResponse.getAsync().getJobStatus();
                     *
                     * while(jobstatus == 0){
                     *
                     * QueryAsyncJobResultResponse jobResultResponse =
                     * (QueryAsyncJobResultResponse) restClient.executeCommand(
                     * asyncJobResultCmd );
                     *
                     * jobstatus = jobResultResponse.getAsync().getJobStatus();
                     * }
                     */

                    int jobstatus = queryAsyncJobResult(restClient, jobid);

                    if (jobstatus == 1) {
                        System.out.println(" elastistor volume successfully deleted");

                    } else {
                        System.out
                                .println(" an error occurred in deleting elastistor volume, now force deleting the elastistor volume");

                        while (jobstatus != 1) {
                            DeleteVolumeResponse deleteVolumeResponse1 = deleteVolume(
                                    restClient, esvolumeid, "true");

                            if (deleteVolumeResponse1 != null) {

                                String jobid1 = deleteVolumeResponse1
                                        .getJobId();

                                jobstatus = queryAsyncJobResult(restClient,
                                        jobid1);

                            }

                        }

                        System.out.println(" elastistor volume successfully deleted");
                    }

                }

            } else {
                System.out.println(" no volume present in on the given TSM");

            }

            System.out.println("now trying to delete elastistor TSM");

            if (estsmid != null) {

                DeleteTsmCmd deleteTsmCmd = new DeleteTsmCmd();
                deleteTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ID,
                        estsmid);
                DeleteTsmResponse deleteTsmResponse = (DeleteTsmResponse) restClient
                        .executeCommand(deleteTsmCmd);

                if (deleteTsmResponse != null) {
                    String jobstatus = deleteTsmResponse.getJobStatus();

                    if (jobstatus.equalsIgnoreCase("true")) {
                        System.out.println(" delete elastistor tsm successful");
                        return true;
                    } else {
                        System.out.println("failed to delete elastistor tsm ");
                        return false;
                    }

                } else {
                    System.out.println("elastistor tsm id not present");

                }
            }

            else {
                System.out.println("no volume is present in the tsm");
            }

        } else {
            System.out
                    .println("List tsm failed, no tsm present in the eastistor for the given IP ");
            return false;
        }
        return false;

    }

    private static ListTsmsResponse listTsm(ElastiCenterClient restClient,
            String poolip) throws Throwable {

        ListTsmCmd listTsmCmd = new ListTsmCmd();

        listTsmCmd.putCommandParameter(ElastistorUtil.REST_PARAM_IPADDRESS,
                poolip);

        ListTsmsResponse listTsmsResponse = (ListTsmsResponse) restClient
                .executeCommand(listTsmCmd);

        return listTsmsResponse;
    }

    private static DeleteVolumeResponse deleteVolume(
            ElastiCenterClient restClient, String esvolumeid, String forcedelete)
            throws Throwable {

        DeleteVolumeCmd deleteVolumeCmd = new DeleteVolumeCmd();

        deleteVolumeCmd.putCommandParameter(ElastistorUtil.REST_PARAM_ID,
                esvolumeid);
        deleteVolumeCmd.putCommandParameter(
                ElastistorUtil.REST_PARAM_FORECEDELETE, forcedelete);
        DeleteVolumeResponse deleteVolumeResponse = (DeleteVolumeResponse) restClient
                .executeCommand(deleteVolumeCmd);

        return deleteVolumeResponse;
    }

    private static int queryAsyncJobResult(ElastiCenterClient restClient,
            String jobid) throws Throwable {

        QueryAsyncJobResultCmd asyncJobResultCmd = new QueryAsyncJobResultCmd();

        asyncJobResultCmd.putCommandParameter(ElastistorUtil.REST_PARAM_JOBID,
                jobid);

        QueryAsyncJobResultResponse asyncJobResultResponse = (QueryAsyncJobResultResponse) restClient
                .executeCommand(asyncJobResultCmd);

        if (asyncJobResultResponse != null) {
            int jobstatus = asyncJobResultResponse.getAsync().getJobStatus();

            while (jobstatus == 0) {

                QueryAsyncJobResultResponse jobResultResponse = (QueryAsyncJobResultResponse) restClient
                        .executeCommand(asyncJobResultCmd);

                jobstatus = jobResultResponse.getAsync().getJobStatus();
            }
            return jobstatus;
        }
        return 0;

    }

    /**
     * This method is responsible for making http connection to elastistor REST
     * server & parsing the REST response recevied afterwards.
     *
     * @param esConnectionInfo
     *            has the connection details to make a http connection to
     *            elastistor server
     * @param restApi
     *            represents the REST command & parameters that needs to be
     *            executed at elastistor REST server
     * @return a json string formatted response received from REST server.
     */
    private static String executeHttpReq(
            ElastistorConnectionInfo esConnectionInfo, RESTApi restApi) {

        DefaultHttpClient httpClient = null;
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;

        try {

            String ip = esConnectionInfo.getManagementIP();
            int port = esConnectionInfo.getManagementPort();
            String apiKey = esConnectionInfo.getApiKey();

            // add common params to restApi object
            restApi.setPair(REST_PARAM_APIKEY, apiKey);
            restApi.setPair(REST_PARAM_RESPONSE, REST_VALUE_RESPONSE);

            // prepare url query param
            String queryParam = "?" + REST_PARAM_COMMAND + "="
                    + restApi.getCommandName()
                    + prepareQueryParam(restApi.getPairs());

            URI uri = new URI(REST_PROTOCOL + "://" + ip + ":" + port + "/"
                    + REST_CONTEXT_PATH + queryParam);

            httpClient = getHttpClient(port);

            HttpPost postRequest = new HttpPost(uri);
            HttpResponse response = httpClient.execute(postRequest);

            if (!isHttpSuccess(response.getStatusLine().getStatusCode())) {
                throw new CloudRuntimeException("Failed on ["
                        + restApi.toString()
                        + "] API call. HTTP error code = ["
                        + response.getStatusLine().getStatusCode()
                        + "]. HTTP error desc = ["
                        + response.getStatusLine().getReasonPhrase() + "].");
            }

            reader = new BufferedReader(new InputStreamReader(response
                    .getEntity().getContent()));

            String strOutput;
            sb = new StringBuilder();
            while ((strOutput = reader.readLine()) != null) {
                sb.append(strOutput);
            }

        } catch (UnsupportedEncodingException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (ClientProtocolException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (IOException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (URISyntaxException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } finally {
            if (null != httpClient) {
                try {
                    httpClient.getConnectionManager().shutdown();
                } catch (Exception t) {
                    // TODO log as warning
                    t.printStackTrace();
                }
            }
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // TODO log as warning
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();
    }

    private static DefaultHttpClient getHttpClient(int iPort) {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs,
                        String string) throws CertificateException {
                }

                public void checkServerTrusted(X509Certificate[] xcs,
                        String string) throws CertificateException {
                }

                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            sslContext
                    .init(null, new TrustManager[] { tm }, new SecureRandom());

            SSLSocketFactory socketFactory = new SSLSocketFactory(sslContext,
                    SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            SchemeRegistry registry = new SchemeRegistry();

            registry.register(new Scheme("https", iPort, socketFactory));

            BasicClientConnectionManager mgr = new BasicClientConnectionManager(
                    registry);
            DefaultHttpClient client = new DefaultHttpClient();

            return new DefaultHttpClient(mgr, client.getParams());
        } catch (NoSuchAlgorithmException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (KeyManagementException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
    }

    /**
     * This method determines if the http response is a success or a failure.
     *
     * @param iCode
     *            represents the Http response code
     * @return true for successful http responses
     */
    private static boolean isHttpSuccess(int iCode) {
        return iCode >= 200 && iCode < 300;
    }

    /**
     * This string utility method will return a string of format
     * &keyName=Value&keyName2=Value2 This should be appended to an URI having
     * query parameter.
     *
     * @param pairs
     *            contains the url query parameter name & corresponding value
     * @return url query param formated string
     */
    private static String prepareQueryParam(Map<String, String> pairs) {

        StringBuilder queryParam = new StringBuilder();
        if (null == pairs || 0 == pairs.size()) {
            return "";
        }

        Collection<String> keys = pairs.keySet();
        for (String key : keys) {
            queryParam.append("&").append(key).append("=")
                    .append(pairs.get(key));
        }

        return queryParam.toString();
    }

    /**
     * This deserializes a json string to the corresponding Java object.
     *
     * @param strJson
     *            the json string to be deserialized
     * @param clazz
     *            the Java class to which the json will be deserialized into
     * @return the deserialized Java object
     */
    private static Object deserializeJsonToJava(String strJson, String key,
            Class<?> clazz) {
        try {

            Gson gson = new GsonBuilder().create();

            // Json string to Elastistor Java object transformation
            if (null == key || 0 == key.length()) {
                return gson.fromJson(strJson, clazz);

            } else {
                JsonElement jsonEl = gson.fromJson(strJson, JsonElement.class);
                JsonObject jsonObj = jsonEl.getAsJsonObject();

                // get the required JsonObject now
                jsonObj = jsonObj.getAsJsonObject(key);
                // convert the required JsonObject to particular Java
                // representation
                return gson.fromJson(jsonObj.toString(), clazz);
            }
        } catch (JsonParseException jpEx) {
            throw new CloudRuntimeException(String.format(
                    DESERIALIZATION_ERROR, clazz.getSimpleName(), key), jpEx);
        }
    }

}
