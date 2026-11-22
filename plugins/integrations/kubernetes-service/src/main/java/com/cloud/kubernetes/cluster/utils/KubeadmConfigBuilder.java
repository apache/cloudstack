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
package com.cloud.kubernetes.cluster.utils;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.kubernetes.version.KubernetesVersionManagerImpl;
import com.cloud.utils.compression.CompressionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.MalformedJsonException;

/**
 * Validates the user supplied kube-apiserver and kubelet settings of a CKS cluster and renders
 * them, together with the cluster topology, into the kubeadm init configuration file.
 * <p>
 * The documents are written as JSON, which kubeadm reads as YAML. User values never reach a
 * shell, and the character set is limited so they cannot open a cloud-init jinja expression.
 */
public final class KubeadmConfigBuilder {

    public static final String MIN_KUBERNETES_VERSION = "1.22.0";
    private static final String MIN_KUBERNETES_VERSION_V1BETA4 = "1.31.0";
    public static final String AUDIT_POLICY_PATH = "/etc/kubernetes/audit-policy.yaml";
    public static final String AUDIT_LOG_DIR = "/var/log/kubernetes/audit";
    public static final int MAX_KUBELET_CONFIG_LENGTH = 4096;
    public static final int MAX_AUDIT_POLICY_LENGTH = 10240;
    private static final int MAX_API_SERVER_ARGS_LENGTH = 10240;
    private static final String KUBELET_API_VERSION = "kubelet.config.k8s.io/v1beta1";
    private static final String CRI_SOCKET = "/run/containerd/containerd.sock";
    private static final int MAX_VALUE_LENGTH = 1024;
    private static final Pattern ARG_NAME = Pattern.compile("[a-z0-9][a-z0-9-]*");
    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9._/-]+");
    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9._:/%+=,@~*-]*");
    private static final Set<String> MANAGED_API_SERVER_ARGS = Set.of("audit-policy-file", "audit-log-path",
            "advertise-address", "bind-address", "secure-port", "authorization-mode", "service-cluster-ip-range", "client-ca-file",
            "tls-cert-file", "tls-private-key-file", "etcd-servers", "etcd-cafile", "etcd-certfile", "etcd-keyfile",
            "kubelet-client-certificate", "kubelet-client-key", "service-account-key-file", "service-account-signing-key-file",
            "anonymous-auth", "enable-bootstrap-token-auth", "allow-privileged", "disable-admission-plugins", "admission-control");
    private static final Set<String> MANAGED_KUBELET_FIELDS = Set.of("apiVersion", "kind",
            "authentication", "authorization", "cgroupDriver", "clusterDNS", "clusterDomain", "staticPodPath",
            "staticPodURL", "providerID", "containerRuntimeEndpoint");
    private static final Map<String, String> AUDIT_LOG_DEFAULTS = Map.of(
            "audit-log-maxage", "30",
            "audit-log-maxbackup", "3",
            "audit-log-maxsize", "100");

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private KubeadmConfigBuilder() {
    }

    public static void validateApiServerArgs(Map<String, String> args) {
        if (MapUtils.isEmpty(args)) {
            return;
        }
        for (Map.Entry<String, String> arg : args.entrySet()) {
            String name = arg.getKey();
            if (name == null || !ARG_NAME.matcher(name).matches()) {
                throw new InvalidParameterValueException(String.format("Invalid kube-apiserver argument name: %s. Use the flag name without the leading dashes", name));
            }
            if (MANAGED_API_SERVER_ARGS.contains(name) || name.startsWith("requestheader-")) {
                throw new InvalidParameterValueException(String.format("kube-apiserver argument %s is managed by CloudStack", name));
            }
            if (!isSafeValue(arg.getValue())) {
                throw new InvalidParameterValueException(String.format("Invalid value for kube-apiserver argument %s, allowed characters are letters, digits and ._:/%%+=,@~*-", name));
            }
        }
        if (apiServerArgsToJson(args).length() > MAX_API_SERVER_ARGS_LENGTH) {
            throw new InvalidParameterValueException(String.format("kube-apiserver arguments can not be longer than %d characters in total", MAX_API_SERVER_ARGS_LENGTH));
        }
    }

    public static String apiServerArgsToJson(Map<String, String> args) {
        return GSON.toJson(new TreeMap<>(args));
    }

    public static Map<String, String> apiServerArgsFromJson(String json) {
        if (StringUtils.isEmpty(json)) {
            return new HashMap<>();
        }
        return GSON.fromJson(json, new TypeToken<Map<String, String>>() { }.getType());
    }

    /**
     * @return the kubelet configuration as compact JSON, or null if none was given
     */
    public static String normalizeKubeletConfig(String kubeletConfig) {
        if (StringUtils.isBlank(kubeletConfig)) {
            return null;
        }
        if (kubeletConfig.length() > MAX_KUBELET_CONFIG_LENGTH) {
            throw new InvalidParameterValueException(String.format("Kubelet configuration can not be longer than %d characters", MAX_KUBELET_CONFIG_LENGTH));
        }
        JsonElement config;
        try (JsonReader reader = new JsonReader(new StringReader(kubeletConfig))) {
            config = GSON.getAdapter(JsonElement.class).read(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new MalformedJsonException("unexpected data after the JSON object");
            }
        } catch (IOException | IllegalStateException | JsonParseException e) {
            throw new InvalidParameterValueException("Kubelet configuration must be a JSON object: " + e.getMessage());
        }
        if (!config.isJsonObject() || config.getAsJsonObject().size() == 0) {
            throw new InvalidParameterValueException("Kubelet configuration must be a non-empty JSON object of KubeletConfiguration fields");
        }
        JsonObject object = config.getAsJsonObject();
        for (String field : MANAGED_KUBELET_FIELDS) {
            if (object.has(field)) {
                throw new InvalidParameterValueException(String.format("Kubelet configuration field %s is managed by CloudStack", field));
            }
        }
        validateKubeletConfigElement(object);
        return GSON.toJson(object);
    }

    public static void validateAuditPolicy(String auditPolicy) {
        if (auditPolicy == null) {
            return;
        }
        if (StringUtils.isBlank(auditPolicy)) {
            throw new InvalidParameterValueException("Audit policy can not be empty");
        }
        if (auditPolicy.length() > MAX_AUDIT_POLICY_LENGTH) {
            throw new InvalidParameterValueException(String.format("Audit policy can not be longer than %d characters", MAX_AUDIT_POLICY_LENGTH));
        }
        if (auditPolicy.codePoints().anyMatch(Character::isSupplementaryCodePoint)) {
            throw new InvalidParameterValueException("Audit policy can not contain characters outside the Unicode basic multilingual plane");
        }
    }

    /**
     * @param controlNodeIp the control node's own IP on the cluster network
     * @param serverIp the cluster's API endpoint, e.g. its public IP
     * @param etcdEndpoints external etcd client URLs, empty for stacked etcd
     * @param kubeletConfig normalized kubelet configuration JSON, or null
     */
    public static String build(String kubernetesVersion, String controlNodeIp, String serverIp, int apiServerPort, List<String> etcdEndpoints,
            String token, String tokenTtl, String certificateKey, boolean haSupported,
            Map<String, String> apiServerArgs, boolean auditPolicy, String kubeletConfig) {
        boolean externalEtcd = !etcdEndpoints.isEmpty();
        boolean v1beta4;
        try {
            v1beta4 = KubernetesVersionManagerImpl.compareSemanticVersions(kubernetesVersion, MIN_KUBERNETES_VERSION_V1BETA4) >= 0;
        } catch (IllegalArgumentException e) {
            v1beta4 = false;
        }
        String kubeadmApiVersion = v1beta4 ? "kubeadm.k8s.io/v1beta4" : "kubeadm.k8s.io/v1beta3";

        Map<String, Object> apiServer = new LinkedHashMap<>();
        apiServer.put("certSANs", controlNodeIp.equals(serverIp) ? List.of(serverIp) : List.of(controlNodeIp, serverIp));
        Map<String, String> extraArgs = new LinkedHashMap<>();
        if (auditPolicy) {
            extraArgs.put("audit-policy-file", AUDIT_POLICY_PATH);
            extraArgs.put("audit-log-path", AUDIT_LOG_DIR + "/audit.log");
            extraArgs.putAll(new TreeMap<>(AUDIT_LOG_DEFAULTS));
            apiServer.put("extraVolumes", List.of(
                    hostPathMount("audit-policy", AUDIT_POLICY_PATH, true, "File"),
                    hostPathMount("audit-log", AUDIT_LOG_DIR, false, "DirectoryOrCreate")));
        }
        if (MapUtils.isNotEmpty(apiServerArgs)) {
            extraArgs.putAll(new TreeMap<>(apiServerArgs));
            String admissionPlugins = extraArgs.get("enable-admission-plugins");
            if (admissionPlugins != null && !List.of(admissionPlugins.split(",")).contains("NodeRestriction")) {
                extraArgs.put("enable-admission-plugins", admissionPlugins.isEmpty() ? "NodeRestriction" : admissionPlugins + ",NodeRestriction");
            }
        }
        if (!extraArgs.isEmpty()) {
            apiServer.put("extraArgs", v1beta4 ? toArgList(extraArgs) : extraArgs);
        }

        Map<String, Object> clusterConfiguration = document(kubeadmApiVersion, "ClusterConfiguration");
        clusterConfiguration.put("kubernetesVersion", kubernetesVersion);
        if (haSupported || externalEtcd) {
            clusterConfiguration.put("controlPlaneEndpoint", String.format("%s:%d", serverIp, apiServerPort));
        }
        clusterConfiguration.put("apiServer", apiServer);
        if (externalEtcd) {
            clusterConfiguration.put("etcd", Map.of("external", Map.of("endpoints", etcdEndpoints)));
        }

        Map<String, Object> initConfiguration = document(kubeadmApiVersion, "InitConfiguration");
        Map<String, String> bootstrapToken = new LinkedHashMap<>();
        bootstrapToken.put("token", token);
        bootstrapToken.put("ttl", tokenTtl);
        initConfiguration.put("bootstrapTokens", List.of(bootstrapToken));
        initConfiguration.put("nodeRegistration", Map.of("criSocket", CRI_SOCKET));
        if (externalEtcd) {
            Map<String, Object> localApiEndpoint = new LinkedHashMap<>();
            localApiEndpoint.put("advertiseAddress", controlNodeIp);
            localApiEndpoint.put("bindPort", apiServerPort);
            initConfiguration.put("localAPIEndpoint", localApiEndpoint);
        }
        initConfiguration.put("certificateKey", certificateKey);

        StringBuilder config = new StringBuilder();
        config.append(GSON.toJson(clusterConfiguration)).append("\n---\n").append(GSON.toJson(initConfiguration));
        if (kubeletConfig != null) {
            JsonObject kubelet = new JsonObject();
            kubelet.addProperty("apiVersion", KUBELET_API_VERSION);
            kubelet.addProperty("kind", "KubeletConfiguration");
            JsonParser.parseString(kubeletConfig).getAsJsonObject().entrySet().forEach(e -> kubelet.add(e.getKey(), e.getValue()));
            config.append("\n---\n").append(GSON.toJson(kubelet));
        }
        return config.toString();
    }

    /**
     * @return a cloud-config write_files entry for the audit policy, or an empty string
     */
    public static String auditPolicyWriteFile(String auditPolicy) {
        if (StringUtils.isEmpty(auditPolicy)) {
            return "";
        }
        byte[] compressed;
        try {
            compressed = new CompressionUtil().compressString(auditPolicy);
        } catch (IOException e) {
            throw new CloudRuntimeException("Unable to compress the audit policy", e);
        }
        return String.format("- path: %s\n    permissions: '0600'\n    owner: root:root\n    encoding: gz+b64\n    content: %s",
                AUDIT_POLICY_PATH, Base64.getEncoder().encodeToString(compressed));
    }

    private static void validateKubeletConfigElement(JsonElement element) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                if (!SAFE_KEY.matcher(entry.getKey()).matches()) {
                    throw new InvalidParameterValueException(String.format("Invalid kubelet configuration key: %s", entry.getKey()));
                }
                validateKubeletConfigElement(entry.getValue());
            }
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(KubeadmConfigBuilder::validateKubeletConfigElement);
        } else if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            if (primitive.isString() && !isSafeValue(primitive.getAsString())) {
                throw new InvalidParameterValueException(String.format("Invalid kubelet configuration value: %s, allowed characters are letters, digits and ._:/%%+=,@~*-", primitive.getAsString()));
            }
        } else if (element.isJsonNull()) {
            throw new InvalidParameterValueException("Kubelet configuration values can not be null, leave the field out instead");
        }
    }

    private static List<Map<String, String>> toArgList(Map<String, String> args) {
        List<Map<String, String>> list = new ArrayList<>();
        for (Map.Entry<String, String> arg : args.entrySet()) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("name", arg.getKey());
            entry.put("value", arg.getValue());
            list.add(entry);
        }
        return list;
    }

    private static boolean isSafeValue(String value) {
        return value != null && value.length() <= MAX_VALUE_LENGTH && SAFE_VALUE.matcher(value).matches();
    }

    private static Map<String, Object> document(String apiVersion, String kind) {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("apiVersion", apiVersion);
        document.put("kind", kind);
        return document;
    }

    private static Map<String, Object> hostPathMount(String name, String path, boolean readOnly, String pathType) {
        Map<String, Object> mount = new LinkedHashMap<>();
        mount.put("name", name);
        mount.put("hostPath", path);
        mount.put("mountPath", path);
        mount.put("readOnly", readOnly);
        mount.put("pathType", pathType);
        return mount;
    }
}
