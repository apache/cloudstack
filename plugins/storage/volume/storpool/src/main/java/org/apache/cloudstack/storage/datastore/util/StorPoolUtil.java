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

import com.cloud.hypervisor.kvm.storage.StorPoolStorageAdaptor;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.OutputInterpreter;
import com.cloud.utils.script.Script;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import org.apache.cloudstack.storage.datastore.db.PrimaryDataStoreDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailVO;
import org.apache.cloudstack.storage.datastore.db.StoragePoolDetailsDao;
import org.apache.cloudstack.storage.datastore.db.StoragePoolVO;
import org.apache.cloudstack.storage.snapshot.StorPoolConfigurationManager;
import org.apache.cloudstack.storage.to.VolumeObjectTO;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StorPoolUtil {
    protected static Logger LOGGER = LogManager.getLogger(StorPoolUtil.class);

    private static final File spLogFile = new File(
            Files.exists(Paths.get("/var/log/cloudstack/management/")) ?
                    "/var/log/cloudstack/management/storpool-plugin.log" :
                    "/tmp/storpool-plugin.log");
    private static PrintWriter spLogPrinterWriter = spLogFileInitialize();

    private static PrintWriter spLogFileInitialize() {
        try {
            LOGGER.info("INITIALIZE SP-LOGGER_FILE");
            if (spLogFile.exists()) {
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
                final Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                final File spLogFileRename = new File(spLogFile + "-" + sdf.format(timestamp));
                final boolean ret = spLogFile.renameTo(spLogFileRename);
                if (!ret) {
                    LOGGER.warn("Unable to rename" + spLogFile + " to " + spLogFileRename);
                } else {
                    LOGGER.debug("Renamed " + spLogFile + " to " + spLogFileRename);
                }
            } else {
                spLogFile.getParentFile().mkdirs();
            }
            return new PrintWriter(spLogFile);
        } catch (Exception e) {
            LOGGER.info("INITIALIZE SP-LOGGER_FILE: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static void spLog(String fmt, Object... args) {
        String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,ms").format(Calendar.getInstance().getTime());
        spLogPrinterWriter.println(String.format(timeStamp + " " + fmt, args));
        spLogPrinterWriter.flush();
        if (spLogFile.length() > 107374182400L) {
            spLogPrinterWriter.close();
            spLogPrinterWriter = spLogFileInitialize();
        }
    }

    public static final String SP_PROVIDER_NAME = "StorPool";
    public static final String SP_DEV_PATH = "/dev/storpool-byid/";
    public static final String SP_OLD_PATH = "/dev/storpool/";
    public static final String SP_VC_POLICY = "vc-policy";
    public static final String GLOBAL_ID = "snapshotGlobalId";
    public static final String UPDATED_DETAIL = "renamed";
    public static final String SP_STORAGE_POOL_ID = "spStoragePoolId";

    public static final String SP_HOST_PORT = "SP_API_HTTP_HOST";

    public static final String SP_TEMPLATE = "SP_TEMPLATE";

    public static final String SP_AUTH_TOKEN = "SP_AUTH_TOKEN";

    public static final String SP_VOLUME_ON_CLUSTER = "SP_VOLUME_ON_CLUSTER";

    public static enum StorpoolRights {
        RO("ro"), RW("rw"), DETACH("detach");

        private final String name;

        private StorpoolRights(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    public static final class SpApiError {
        private String name;
        private String descr;

        public SpApiError() {
        }

        public String getName() {
            return this.name;
        }

        public String getDescr() {
            return this.descr;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setDescr(String descr) {
            this.descr = descr;
        }

        public String toString() {
            return String.format("%s: %s", name, descr);
        }
    }

    public static class SpConnectionDesc {
        private String hostPort;
        private String authToken;
        private String templateName;

        public SpConnectionDesc(String url) {
            String[] urlSplit = url.split(";");
            if (urlSplit.length == 1 && !urlSplit[0].contains("=")) {
                this.templateName = url;

                Script sc = new Script("storpool_confget", 0, LOGGER);
                OutputInterpreter.AllLinesParser parser = new OutputInterpreter.AllLinesParser();

                final String err = sc.execute(parser);
                if (err != null) {
                    final String errMsg = String.format("Could not execute storpool_confget. Error: %s", err);
                    LOGGER.warn(errMsg);
                    throw new CloudRuntimeException(errMsg);
                }

                String SP_API_HOST = null;
                String SP_API_PORT = null;

                for (String line : parser.getLines().split("\n")) {
                    String[] toks = line.split("=");
                    if (toks.length != 2) {
                        continue;
                    }

                    switch (toks[0]) {
                    case "SP_API_HTTP_HOST":
                        SP_API_HOST = toks[1];
                        break;

                    case "SP_API_HTTP_PORT":
                        SP_API_PORT = toks[1];
                        break;

                    case "SP_AUTH_TOKEN":
                        this.authToken = toks[1];
                        break;
                    }
                }

                if (SP_API_HOST == null)
                    throw new CloudRuntimeException("Invalid StorPool config. Missing SP_API_HTTP_HOST");
                if (SP_API_PORT == null)
                    throw new CloudRuntimeException("Invalid StorPool config. Missing SP_API_HTTP_PORT");
                if (this.authToken == null)
                    throw new CloudRuntimeException("Invalid StorPool config. Missing SP_AUTH_TOKEN");

                this.hostPort = SP_API_HOST + ":" + SP_API_PORT;
            } else {
                for (String kv : urlSplit) {
                    String[] toks = kv.split("=");
                    if (toks.length != 2)
                        continue;
                    switch (toks[0]) {
                    case "SP_API_HTTP":
                        this.hostPort = toks[1];
                        break;

                    case "SP_AUTH_TOKEN":
                        this.authToken = toks[1];
                        break;

                    case "SP_TEMPLATE":
                        this.templateName = toks[1];
                        break;
                    }
                }
            }
        }

        public SpConnectionDesc(String host, String authToken2, String templateName2) {
            this.hostPort = host;
            this.authToken = authToken2;
            this.templateName = templateName2;
        }

        public String getHostPort() {
            return this.hostPort;
        }

        public String getAuthToken() {
            return this.authToken;
        }

        public String getTemplateName() {
            return this.templateName;
        }
    }

    public static SpConnectionDesc getSpConnection(String url, long poolId, StoragePoolDetailsDao poolDetails,
            PrimaryDataStoreDao storagePool) {
        boolean isAlternateEndpointEnabled = StorPoolConfigurationManager.AlternativeEndPointEnabled.valueIn(poolId);
        if (isAlternateEndpointEnabled) {
            String alternateEndpoint = StorPoolConfigurationManager.AlternativeEndpoint.valueIn(poolId);
            if (StringUtils.isNotEmpty(alternateEndpoint)) {
                return new SpConnectionDesc(alternateEndpoint);
            } else {
                throw new CloudRuntimeException(String.format("Using an alternative endpoint of StorPool primary storage with id [%s] is enabled but no endpoint URL is provided", poolId));
            }
        }
        List<StoragePoolDetailVO> details = poolDetails.listDetails(poolId);
        String host = null;
        String authToken = null;
        String templateName = null;
        for (StoragePoolDetailVO storagePoolDetailVO : details) {
            switch (storagePoolDetailVO.getName()) {
            case SP_HOST_PORT:
                host = storagePoolDetailVO.getValue();
                break;
            case SP_AUTH_TOKEN:
                authToken = storagePoolDetailVO.getValue();
                break;
            case SP_TEMPLATE:
                templateName = storagePoolDetailVO.getValue();
                break;
            }
        }
        if (host != null && authToken != null && templateName != null) {
            return new SpConnectionDesc(host, authToken, templateName);
        } else {
            return updateStorageAndStorageDetails(url, poolId, poolDetails, storagePool);
        }
    }

    private static SpConnectionDesc updateStorageAndStorageDetails(String url, long poolId,
            StoragePoolDetailsDao poolDetails, PrimaryDataStoreDao storagePool) {
        SpConnectionDesc conn = new SpConnectionDesc(url);
        poolDetails.persist(new StoragePoolDetailVO(poolId, SP_HOST_PORT, conn.getHostPort(), false));
        poolDetails.persist(new StoragePoolDetailVO(poolId, SP_AUTH_TOKEN, conn.getAuthToken(), false));
        poolDetails.persist(new StoragePoolDetailVO(poolId, SP_TEMPLATE, conn.getTemplateName(), false));
        StoragePoolVO pool = storagePool.findById(poolId);
        pool.setUuid(conn.getTemplateName() + ";" + UUID.randomUUID().toString());
        storagePool.update(poolId, pool);
        StorPoolUtil.spLog(
                "Storage pool with id=%s and template's name=%s was updated and its connection details are hidden from UI.",
                pool.getId(), conn.getTemplateName());
        return conn;
    }

    public static class SpApiResponse {
        private SpApiError error;
        public JsonElement fullJson;

        public SpApiResponse() {
        }

        public SpApiError getError() {
            return this.error;
        }

        public void setError(SpApiError error) {
            this.error = error;
        }
    }

    public static String devPath(final String name) {
        return String.format("%s%s", SP_DEV_PATH, name);
    }

    private static SpApiResponse spApiRequest(HttpRequestBase req, String query, SpConnectionDesc conn) {

        if (conn == null)
            conn = new SpConnectionDesc("");

        if (conn.getHostPort() == null) {
            throw new CloudRuntimeException("Invalid StorPool config. Missing SP_API_HTTP_HOST");
        }

        if (conn.getAuthToken() == null) {
            throw new CloudRuntimeException("Invalid StorPool config. Missing SP_AUTH_TOKEN");
        }

        try (CloseableHttpClient httpclient = HttpClientBuilder.create().build()) {
            final String qry = String.format("http://%s/ctrl/1.0/%s", conn.getHostPort(), query);
            final URI uri = new URI(qry);

            req.setURI(uri);
            req.addHeader("Authorization", String.format("Storpool v1:%s", conn.getAuthToken()));

            final HttpResponse resp = httpclient.execute(req);

            Gson gson = new Gson();
            BufferedReader br = new BufferedReader(new InputStreamReader(resp.getEntity().getContent()));

            JsonElement el = new JsonParser().parse(br);

            SpApiResponse apiResp = gson.fromJson(el, SpApiResponse.class);
            apiResp.fullJson = el;
            return apiResp;
        } catch (UnsupportedEncodingException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (ClientProtocolException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (IOException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        } catch (URISyntaxException ex) {
            throw new CloudRuntimeException(ex.getMessage());
        }
    }

    private static SpApiResponse GET(String query, SpConnectionDesc conn) {
        return spApiRequest(new HttpGet(), query, conn);
    }

    private static SpApiResponse POST(String query, Object json, SpConnectionDesc conn) {
        HttpPost req = new HttpPost();
        if (json != null) {
            Gson gson = new Gson();
            String js = gson.toJson(json);
            StringEntity input = new StringEntity(js, ContentType.APPLICATION_JSON);
            LOGGER.info("Request:" + js);
            req.setEntity(input);
        }

        return spApiRequest(req, query, conn);
    }

    public static boolean templateExists(SpConnectionDesc conn) {
        SpApiResponse resp = GET("VolumeTemplateDescribe/" + conn.getTemplateName(), conn);
        return resp.getError() == null ? true : objectExists(resp.getError());
    }

    public static boolean snapshotExists(final String name, SpConnectionDesc conn) {
        SpApiResponse resp = GET("MultiCluster/Snapshot/" + name, conn);
        return resp.getError() == null ? true : objectExists(resp.getError());
    }

    public static JsonArray snapshotsList(SpConnectionDesc conn) {
        SpApiResponse resp = GET("MultiCluster/SnapshotsList", conn);
        JsonObject obj = resp.fullJson.getAsJsonObject();
        JsonArray data = obj.getAsJsonArray("data");
        return data;
    }

    public static JsonArray volumesList(SpConnectionDesc conn) {
        SpApiResponse resp = GET("MultiCluster/VolumesList", conn);
        JsonObject obj = resp.fullJson.getAsJsonObject();
        JsonArray data = obj.getAsJsonArray("data");
        return data;
    }

    public static JsonArray volumesSpace(SpConnectionDesc conn) {
        SpApiResponse resp = GET("MultiCluster/AllClusters/VolumesSpace", conn);
        JsonObject obj = resp.fullJson.getAsJsonObject();
        return obj.getAsJsonObject("data").getAsJsonArray("clusters");
    }

    public static JsonArray templatesStats(SpConnectionDesc conn) {
        SpApiResponse resp = GET("MultiCluster/AllClusters/VolumeTemplatesStatus", conn);
        JsonObject obj = resp.fullJson.getAsJsonObject();
        return obj.getAsJsonObject("data").getAsJsonArray("clusters");
    }

    private static boolean objectExists(SpApiError err) {
        if (!err.getName().equals("objectDoesNotExist")) {
            throw new CloudRuntimeException(err.getDescr());
        }
        return false;
    }

    public static Long snapshotSize(final String name, SpConnectionDesc conn) {
        SpApiResponse resp = GET("MultiCluster/Snapshot/" + name, conn);
        JsonObject obj = resp.fullJson.getAsJsonObject();

        if (resp.getError() != null && !objectExists(resp.getError())) {
            return null;
        }
        JsonObject data = obj.getAsJsonArray("data").get(0).getAsJsonObject();
        return data.getAsJsonPrimitive("size").getAsLong();
    }

    public static String getSnapshotClusterID(String name, SpConnectionDesc conn) {
        SpApiResponse resp = GET("MultiCluster/Snapshot/" + name, conn);
        JsonObject obj = resp.fullJson.getAsJsonObject();

        JsonObject data = obj.getAsJsonArray("data").get(0).getAsJsonObject();
        JsonPrimitive clusterId = data.getAsJsonPrimitive("clusterId");
        return clusterId != null ? clusterId.getAsString() : null;
    }

    public static String getVolumeClusterID(String name, SpConnectionDesc conn) {
        SpApiResponse resp = GET("MultiCluster/Volume/" + name, conn);
        JsonObject obj = resp.fullJson.getAsJsonObject();

        JsonObject data = obj.getAsJsonArray("data").get(0).getAsJsonObject();
        JsonPrimitive clusterId = data.getAsJsonPrimitive("clusterId");
        return clusterId != null ? clusterId.getAsString() : null;
    }

    public static SpApiResponse volumeCreate(final String name, final String parentName, final Long size, String vmUuid,
            String vcPolicy, String csTag, Long iops, SpConnectionDesc conn) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", "");
        json.put("iops", iops);
        json.put("parent", parentName);
        json.put("size", size);
        json.put("template", conn.getTemplateName());
        Map<String, String> tags = StorPoolHelper.addStorPoolTags(name, vmUuid, csTag, vcPolicy);
        json.put("tags", tags);
        return POST("MultiCluster/VolumeCreate", json, conn);
    }

    public static SpApiResponse volumeCreate(SpConnectionDesc conn) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", "");
        json.put("size", 512);
        json.put("template", conn.getTemplateName());
        Map<String, String> tags = new HashMap<>();
        tags.put("cs", "check-volume-is-on-host");
        json.put("tags", tags);
        return POST("MultiCluster/VolumeCreate", json, conn);
    }

    public static SpApiResponse volumeCopy(final String name, final String baseOn, String csTag, Long iops, String cvmTag, String vcPolicyTag,
            SpConnectionDesc conn) {
        Map<String, Object> json = new HashMap<>();
        json.put("baseOn", baseOn);
        if (iops != null) {
            json.put("iops", iops);
        }
        json.put("template", conn.getTemplateName());
        Map<String, String> tags = StorPoolHelper.addStorPoolTags(name, cvmTag, csTag, vcPolicyTag);
        json.put("tags", tags);
        return POST("MultiCluster/VolumeCreate", json, conn);
    }

    public static SpApiResponse volumeUpdateRename(final String name, String newName, String uuid,
            SpConnectionDesc conn) {
        Map<String, Object> json = new HashMap<>();
        json.put("rename", newName);
        Map<String, String> tags = new HashMap<>();
        tags.put("uuid", uuid);
        json.put("tags", tags);

        return POST("MultiCluster/VolumeUpdate/" + name, json, conn);
    }

    public static SpApiResponse volumeUpdate(final String name, final Long newSize, final Boolean shrinkOk, Long iops,
            SpConnectionDesc conn) {
        Map<String, Object> json = new HashMap<>();
        json.put("iops", iops);
        json.put("size", newSize);
        json.put("shrinkOk", shrinkOk);

        return POST("MultiCluster/VolumeUpdate/" + name, json, conn);
    }

    public static SpApiResponse volumeUpdateTags(final String name, final String uuid, Long iops,
            SpConnectionDesc conn, String vcPolicy) {
        Map<String, Object> json = new HashMap<>();
        Map<String, String> tags = StorPoolHelper.addStorPoolTags(null, uuid, null, vcPolicy);
        json.put("iops", iops);
        json.put("tags", tags);
        return POST("MultiCluster/VolumeUpdate/" + name, json, conn);
    }

    public static SpApiResponse volumeUpdateCvmTags(final String name, final String uuid, SpConnectionDesc conn) {
        Map<String, Object> json = new HashMap<>();
        Map<String, String> tags = StorPoolHelper.addStorPoolTags(null, uuid, null, null);
        json.put("tags", tags);
        return POST("MultiCluster/VolumeUpdate/" + name, json, conn);
    }

    public static SpApiResponse volumeUpdateVCTags(final String name, SpConnectionDesc conn, String vcPolicy) {
        Map<String, Object> json = new HashMap<>();
        Map<String, String> tags = StorPoolHelper.addStorPoolTags(null, null, null, vcPolicy);
        json.put("tags", tags);
        return POST("MultiCluster/VolumeUpdate/" + name, json, conn);
    }

    public static SpApiResponse volumeUpdateTemplate(final String name, SpConnectionDesc conn) {
        Map<String, Object> json = new HashMap<>();
        json.put("template", conn.getTemplateName());
        return POST("MultiCluster/VolumeUpdate/" + name, json, conn);
    }

    public static SpApiResponse volumeSnapshot(final String volumeName, final String snapshotName, String vmUuid,
            String csTag, String vcPolicy, SpConnectionDesc conn) {
        Map<String, Object> json = new HashMap<>();
        Map<String, String> tags = StorPoolHelper.addStorPoolTags(snapshotName, vmUuid, csTag, vcPolicy);
        json.put("name", "");
        json.put("tags", tags);

        return POST("MultiCluster/VolumeSnapshot/" + volumeName, json, conn);
    }

    public static SpApiResponse volumesGroupSnapshot(final List<VolumeObjectTO> volumeTOs, final String vmUuid,
            final String snapshotName, String csTag, SpConnectionDesc conn) {
        Map<String, Object> json = new LinkedHashMap<>();
        Map<String, String> tags = StorPoolHelper.addStorPoolTags(snapshotName, vmUuid, csTag, null);
        List<Map<String, Object>> volumes = new ArrayList<>();
        for (VolumeObjectTO volumeTO : volumeTOs) {
            Map<String, Object> vol = new LinkedHashMap<>();
            String name = StorPoolStorageAdaptor.getVolumeNameFromPath(volumeTO.getPath(), true);
            vol.put("name", "");
            vol.put("volume", name);
            volumes.add(vol);
        }
        json.put("tags", tags);
        json.put("volumes", volumes);
        LOGGER.info("json:" + json);
        return POST("MultiCluster/VolumesGroupSnapshot", json, conn);
    }

    public static SpApiResponse volumeRevert(final String name, final String snapshotName, SpConnectionDesc conn) {
        Map<String, Object> json = new HashMap<>();
        json.put("toSnapshot", snapshotName);
        return POST("MultiCluster/VolumeRevert/" + name, json, conn);
    }

    public static SpApiResponse volumeFreeze(final String volumeName, SpConnectionDesc conn) {
        return POST("MultiCluster/VolumeFreeze/" + volumeName, null, conn);
    }

    public static SpApiResponse volumeAcquire(final String volumeName, SpConnectionDesc conn) {
        Map<String, Object> json = new HashMap<>();
        json.put("onRemoteAttached", "detachForce");
        return POST("MultiCluster/VolumeAcquire/" + volumeName, json, conn);
    }

    public static SpApiResponse volumeDelete(final String name, SpConnectionDesc conn) {
        Map<String, Object> json = new HashMap<>();
        json.put("onAttached", "detachForce");
        return POST("MultiCluster/VolumeDelete/" + name, json, conn);
    }

    public static SpApiResponse snapshotDelete(final String name, SpConnectionDesc conn) {
        SpApiResponse resp = detachAllForced(name, true, conn);
        return resp.getError() == null ? POST("MultiCluster/SnapshotDelete/" + name, null, conn) : resp;
    }

    public static SpApiResponse detachAllForced(final String name, final boolean snapshot, SpConnectionDesc conn) {
        final String type = snapshot ? "snapshot" : "volume";
        List<Map<String, Object>> json = new ArrayList<>();
        Map<String, Object> reassignDesc = new HashMap<>();
        reassignDesc.put(type, name);
        reassignDesc.put("detach", "all");
        reassignDesc.put("force", true);
        json.add(reassignDesc);

        return POST("MultiCluster/VolumesReassign", json, conn);
    }

    public static String getSnapshotNameFromResponse(SpApiResponse resp, boolean tildeNeeded, String globalIdOrRemote) {
        JsonObject obj = resp.fullJson.getAsJsonObject();
        JsonPrimitive data = obj.getAsJsonObject("data").getAsJsonPrimitive(globalIdOrRemote);
        String name = data != null ? data.getAsString() : null;
        name = name != null ? !tildeNeeded ? name : "~" + name : name;
        return name;
    }

    public static String getNameFromResponse(SpApiResponse resp, boolean tildeNeeded) {
        JsonObject obj = resp.fullJson.getAsJsonObject();
        JsonPrimitive data = obj.getAsJsonObject("data").getAsJsonPrimitive("name");
        String name = data != null ? data.getAsString() : null;
        name = name != null ? name.startsWith("~") && !tildeNeeded ? name.split("~")[1] : name : name;
        return name;
    }
}
