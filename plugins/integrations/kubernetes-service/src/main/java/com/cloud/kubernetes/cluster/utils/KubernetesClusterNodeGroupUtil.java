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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.kubernetes.cluster.KubernetesClusterNodeGroup;

/**
 * Helpers to validate and (de)serialize the name, Kubernetes node labels and taints of a node group.
 * Labels are stored as comma separated key=value pairs and taints as comma separated key[=value]:effect
 * entries, which is the format expected by the kubelet --node-labels and --register-with-taints arguments.
 */
public class KubernetesClusterNodeGroupUtil {

    public static final int MAX_NODE_GROUP_NAME_LENGTH = 20;
    public static final Set<String> TAINT_EFFECTS = Set.of("NoSchedule", "PreferNoSchedule", "NoExecute");

    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String EFFECT = "effect";

    private static final Pattern NODE_GROUP_NAME_PATTERN = Pattern.compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?$");
    private static final Pattern LABEL_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9]([-A-Za-z0-9_.]*[A-Za-z0-9])?$");
    private static final Pattern DNS_SUBDOMAIN_PATTERN = Pattern.compile("^[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*$");
    private static final int MAX_LABEL_NAME_LENGTH = 63;
    private static final int MAX_LABEL_PREFIX_LENGTH = 253;

    public static class Taint {
        private final String key;
        private final String value;
        private final String effect;

        public Taint(String key, String value, String effect) {
            this.key = key;
            this.value = StringUtils.defaultString(value);
            this.effect = effect;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

        public String getEffect() {
            return effect;
        }

        @Override
        public String toString() {
            return StringUtils.isEmpty(value) ? String.format("%s:%s", key, effect) : String.format("%s=%s:%s", key, value, effect);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Taint)) {
                return false;
            }
            Taint taint = (Taint) o;
            return key.equals(taint.key) && value.equals(taint.value) && effect.equals(taint.effect);
        }

        @Override
        public int hashCode() {
            return Objects.hash(key, value, effect);
        }
    }

    public static void validateNodeGroupName(String name) {
        if (StringUtils.isBlank(name) || name.length() > MAX_NODE_GROUP_NAME_LENGTH || !NODE_GROUP_NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidParameterValueException(String.format("Invalid node group name: %s. It must consist of at most %d lower case " +
                    "alphanumeric characters or '-', and must start and end with an alphanumeric character", name, MAX_NODE_GROUP_NAME_LENGTH));
        }
    }

    /**
     * Validates a Kubernetes label or taint key: an optional DNS subdomain prefix followed by '/' and a name.
     * Keys in the kubernetes.io and k8s.io namespaces cannot be set by the kubelet and the node group label is reserved
     */
    protected static void validateKey(String key, String type) {
        if (StringUtils.isBlank(key)) {
            throw new InvalidParameterValueException(String.format("Empty %s key", type));
        }
        String prefix = null;
        String name = key;
        int separatorIndex = key.indexOf('/');
        if (separatorIndex >= 0) {
            prefix = key.substring(0, separatorIndex);
            name = key.substring(separatorIndex + 1);
            if (prefix.isEmpty() || prefix.length() > MAX_LABEL_PREFIX_LENGTH || !DNS_SUBDOMAIN_PATTERN.matcher(prefix).matches()) {
                throw new InvalidParameterValueException(String.format("Invalid prefix of the %s key: %s", type, key));
            }
        }
        if (name.length() > MAX_LABEL_NAME_LENGTH || !LABEL_NAME_PATTERN.matcher(name).matches()) {
            throw new InvalidParameterValueException(String.format("Invalid %s key: %s", type, key));
        }
        if (prefix != null && (isRestrictedNamespace(prefix) || KubernetesClusterNodeGroup.NODE_GROUP_LABEL.equals(key))) {
            throw new InvalidParameterValueException(String.format("The %s key %s uses a reserved namespace", type, key));
        }
    }

    private static boolean isRestrictedNamespace(String prefix) {
        for (String namespace : List.of("kubernetes.io", "k8s.io")) {
            if (prefix.equals(namespace) || prefix.endsWith("." + namespace)) {
                return true;
            }
        }
        return false;
    }

    protected static void validateValue(String value, String key, String type) {
        if (StringUtils.isEmpty(value)) {
            return;
        }
        if (value.length() > MAX_LABEL_NAME_LENGTH || !LABEL_NAME_PATTERN.matcher(value).matches()) {
            throw new InvalidParameterValueException(String.format("Invalid value of the %s %s: %s", type, key, value));
        }
    }

    /**
     * Converts and validates the labels passed to an API as a map parameter
     * (labels[0].key=k&amp;labels[0].value=v) into an ordered key-value map
     */
    public static Map<String, String> getLabelsFromApiMap(Map<String, Map<String, String>> labelsMap) {
        Map<String, String> labels = new LinkedHashMap<>();
        if (MapUtils.isEmpty(labelsMap)) {
            return labels;
        }
        for (Map<String, String> entry : labelsMap.values()) {
            String key = StringUtils.trim(entry.get(KEY));
            String value = StringUtils.trimToEmpty(entry.get(VALUE));
            validateKey(key, "label");
            validateValue(value, key, "label");
            if (labels.containsKey(key)) {
                throw new InvalidParameterValueException(String.format("Duplicate label key: %s", key));
            }
            labels.put(key, value);
        }
        return labels;
    }

    /**
     * Converts and validates the taints passed to an API as a map parameter
     * (taints[0].key=k&amp;taints[0].value=v&amp;taints[0].effect=NoSchedule)
     */
    public static List<Taint> getTaintsFromApiMap(Map<String, Map<String, String>> taintsMap) {
        List<Taint> taints = new ArrayList<>();
        if (MapUtils.isEmpty(taintsMap)) {
            return taints;
        }
        for (Map<String, String> entry : taintsMap.values()) {
            String key = StringUtils.trim(entry.get(KEY));
            String value = StringUtils.trimToEmpty(entry.get(VALUE));
            String effect = StringUtils.trim(entry.get(EFFECT));
            validateKey(key, "taint");
            validateValue(value, key, "taint");
            if (!TAINT_EFFECTS.contains(effect)) {
                throw new InvalidParameterValueException(String.format("Invalid effect of the taint %s: %s. Valid values are: %s",
                        key, effect, String.join(", ", TAINT_EFFECTS)));
            }
            if (taints.stream().anyMatch(t -> t.getKey().equals(key) && t.getEffect().equals(effect))) {
                throw new InvalidParameterValueException(String.format("Duplicate taint with key %s and effect %s", key, effect));
            }
            taints.add(new Taint(key, value, effect));
        }
        return taints;
    }

    /**
     * @return the labels of a node: the labels of its node group and its own labels, which override the labels of the
     * node group with the same key
     */
    public static String mergeLabels(String nodeGroupLabels, String nodeLabels) {
        Map<String, String> labels = labelsFromString(nodeGroupLabels);
        labels.putAll(labelsFromString(nodeLabels));
        return labelsToString(labels);
    }

    /**
     * @return the taints of a node: the taints of its node group and its own taints, which override the taints of the
     * node group with the same key and effect
     */
    public static String mergeTaints(String nodeGroupTaints, String nodeTaints) {
        List<Taint> taints = taintsFromString(nodeGroupTaints);
        for (Taint taint : taintsFromString(nodeTaints)) {
            taints.removeIf(t -> t.getKey().equals(taint.getKey()) && t.getEffect().equals(taint.getEffect()));
            taints.add(taint);
        }
        return taintsToString(taints);
    }

    /**
     * @return whether some of the taints prevent the pods without matching tolerations from running on the nodes,
     * which is not allowed for the default node group as it has to accept the system pods
     */
    public static boolean hasSchedulingTaints(Collection<Taint> taints) {
        return taints != null && taints.stream().anyMatch(taint -> !"PreferNoSchedule".equals(taint.getEffect()));
    }

        public static String labelsToString(Map<String, String> labels) {
        if (MapUtils.isEmpty(labels)) {
            return null;
        }
        return labels.entrySet().stream().map(e -> String.format("%s=%s", e.getKey(), e.getValue())).collect(Collectors.joining(","));
    }

    public static Map<String, String> labelsFromString(String labels) {
        Map<String, String> map = new LinkedHashMap<>();
        if (StringUtils.isBlank(labels)) {
            return map;
        }
        for (String label : labels.split(",")) {
            String[] keyValue = label.split("=", 2);
            map.put(keyValue[0], keyValue.length > 1 ? keyValue[1] : "");
        }
        return map;
    }

    public static String taintsToString(Collection<Taint> taints) {
        if (taints == null || taints.isEmpty()) {
            return null;
        }
        return taints.stream().map(Taint::toString).collect(Collectors.joining(","));
    }

    public static List<Taint> taintsFromString(String taints) {
        List<Taint> list = new ArrayList<>();
        if (StringUtils.isBlank(taints)) {
            return list;
        }
        for (String taint : taints.split(",")) {
            int effectIndex = taint.lastIndexOf(':');
            String keyValue = taint.substring(0, effectIndex);
            String effect = taint.substring(effectIndex + 1);
            String[] parts = keyValue.split("=", 2);
            list.add(new Taint(parts[0], parts.length > 1 ? parts[1] : "", effect));
        }
        return list;
    }

    /**
     * @return the kubelet arguments registering the node group nodes with the node group label,
     * the user defined labels and the taints of the node group
     */
    public static String getKubeletExtraArgs(KubernetesClusterNodeGroup nodeGroup) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(KubernetesClusterNodeGroup.NODE_GROUP_LABEL, nodeGroup.getName());
        labels.putAll(labelsFromString(nodeGroup.getLabels()));
        String args = String.format("--node-labels=%s", labelsToString(labels));
        if (StringUtils.isNotBlank(nodeGroup.getTaints())) {
            args += String.format(" --register-with-taints=%s", nodeGroup.getTaints());
        }
        return args;
    }

    /**
     * @return the kubectl commands to run against a node of the node group to bring its labels and taints
     * to the new ones: labels and taints which are not present anymore are removed and all the new ones
     * as well as the node group label are (re)applied, so the commands are idempotent and can be retried
     */
    public static List<String> getLabelAndTaintUpdateCommands(String nodeName, String nodeGroupName, String oldLabels, String newLabels,
                                                              String oldTaints, String newTaints) {
        List<String> commands = new ArrayList<>();
        Map<String, String> oldLabelsMap = labelsFromString(oldLabels);
        // The node group label is (re)applied too, nodes added before the node group existed do not have it
        Map<String, String> newLabelsMap = new LinkedHashMap<>();
        newLabelsMap.put(KubernetesClusterNodeGroup.NODE_GROUP_LABEL, nodeGroupName);
        newLabelsMap.putAll(labelsFromString(newLabels));
        List<String> labelArgs = new ArrayList<>();
        for (String key : oldLabelsMap.keySet()) {
            if (!newLabelsMap.containsKey(key)) {
                labelArgs.add(key + "-");
            }
        }
        for (Map.Entry<String, String> label : newLabelsMap.entrySet()) {
            labelArgs.add(String.format("%s=%s", label.getKey(), label.getValue()));
        }
        if (!labelArgs.isEmpty()) {
            commands.add(String.format("sudo /opt/bin/kubectl label node %s %s --overwrite", nodeName, String.join(" ", labelArgs)));
        }

        List<Taint> oldTaintsList = taintsFromString(oldTaints);
        List<Taint> newTaintsList = taintsFromString(newTaints);
        List<String> removedTaintArgs = new ArrayList<>();
        for (Taint taint : oldTaintsList) {
            boolean keptWithSameEffect = newTaintsList.stream().anyMatch(t -> t.getKey().equals(taint.getKey()) && t.getEffect().equals(taint.getEffect()));
            if (!keptWithSameEffect) {
                removedTaintArgs.add(String.format("%s:%s-", taint.getKey(), taint.getEffect()));
            }
        }
        if (!removedTaintArgs.isEmpty()) {
            // kubectl fails to remove a taint which is not present, e.g. already removed by a previous attempt
            for (String removedTaintArg : removedTaintArgs) {
                commands.add(String.format("sudo /opt/bin/kubectl taint node %s %s || true", nodeName, removedTaintArg));
            }
        }
        if (!newTaintsList.isEmpty()) {
            commands.add(String.format("sudo /opt/bin/kubectl taint node %s %s --overwrite", nodeName,
                    newTaintsList.stream().map(Taint::toString).collect(Collectors.joining(" "))));
        }
        return commands;
    }
}
