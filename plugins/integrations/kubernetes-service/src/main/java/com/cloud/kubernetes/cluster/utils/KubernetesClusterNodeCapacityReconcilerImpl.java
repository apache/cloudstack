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

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.kubernetes.cluster.KubernetesServiceHelper.KubernetesClusterNodeType;
import com.cloud.offering.ServiceOffering;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.ssh.SshHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * SSH-backed implementation for a rolling CKS live-resize reconciliation.
 *
 * Component scanning makes this implementation available to the scale worker, which obtains it
 * through {@code ComponentContext}.
 */
@Component
public class KubernetesClusterNodeCapacityReconcilerImpl implements KubernetesClusterNodeCapacityReconciler {
    static final long MINIMUM_MEMORY_OVERHEAD_BYTES = 128L * 1024L * 1024L;
    static final long KUBERNETES_MEMORY_REPORTING_TOLERANCE_BYTES = 16L * 1024L * 1024L;
    private static final int COMMAND_TIMEOUT_MS = 30000;
    private static final int POLL_INTERVAL_MS = 5000;
    private static final Pattern NODE_NAME = Pattern.compile("[A-Za-z0-9][A-Za-z0-9.-]{0,252}");
    private static final Pattern MEMORY_QUANTITY = Pattern.compile("([0-9]+)(Ki|Mi|Gi|Ti|K|M|G|T)?");
    private static final String RESIZE_ANNOTATION = "cloudstack.apache.org/cks-live-resize";

    @Override
    public boolean requiresKubeletRefresh(ServiceOffering oldOffering, ServiceOffering newOffering,
            KubernetesClusterNodeType nodeType, KubernetesCluster.State originalState) {
        return KubernetesCluster.State.Running == originalState
                && (KubernetesClusterNodeType.WORKER == nodeType || KubernetesClusterNodeType.CONTROL == nodeType)
                && capacityChanged(oldOffering, newOffering);
    }

    @Override
    public NodeCapacitySnapshot captureBefore(KubernetesCluster cluster, UserVm vm, NodeAccess access) throws Exception {
        return nodeSnapshot(cluster, vm, access, guestResources(access));
    }

    @Override
    public void cordonIfNeeded(KubernetesCluster cluster, UserVm vm, NodeCapacitySnapshot before, NodeAccess access, long deadline) throws Exception {
        if (before.isUnschedulable() && !before.isCloudStackResizeCordon()) return;
        String node = nodeName(vm);
        executeControl(access, "sudo /opt/bin/kubectl annotate node " + node + " " + RESIZE_ANNOTATION + "=" + cluster.getUuid() + " --overwrite");
        executeControl(access, "sudo /opt/bin/kubectl cordon " + node);
        while (System.currentTimeMillis() < deadline) {
            if (nodeSnapshot(cluster, vm, access, null).isUnschedulable()) return;
            sleep();
        }
        throw failure("CORDON", vm, "Kubernetes node did not become unschedulable");
    }

    @Override
    public void verifyGuestResources(UserVm vm, ServiceOffering target, NodeCapacitySnapshot before, NodeAccess access, long deadline) throws Exception {
        while (System.currentTimeMillis() < deadline) {
            if (guestMatchesTarget(guestResources(access), target)) return;
            sleep();
        }
        throw failure("GUEST_VERIFY", vm, "guest CPU or memory did not reach the target offering");
    }

    @Override
    public boolean isKubernetesResourcesCurrent(NodeCapacitySnapshot snapshot, ServiceOffering target) {
        GuestResources guest = new GuestResources(snapshot.getGuestOnlineCpuCount(), snapshot.getGuestMemoryKiB());
        return guestMatchesTarget(guest, target) && kubernetesMatchesGuest(snapshot, snapshot);
    }

    @Override
    public void restartKubelet(UserVm vm, NodeAccess access, long deadline) throws Exception {
        executeNode(access, "sudo systemctl restart kubelet");
        while (System.currentTimeMillis() < deadline) {
            if ("active".equals(executeNode(access, "sudo systemctl is-active kubelet").trim())) return;
            sleep();
        }
        throw failure("KUBELET_RESTART", vm, "kubelet did not become active");
    }

    @Override
    public NodeCapacitySnapshot waitForKubernetesResources(KubernetesCluster cluster, UserVm vm, ServiceOffering target,
            NodeCapacitySnapshot before, NodeAccess access, long deadline) throws Exception {
        while (System.currentTimeMillis() < deadline) {
            GuestResources guest = guestResources(access);
            NodeCapacitySnapshot observed = nodeSnapshot(cluster, vm, access, guest);
            if (guestMatchesTarget(guest, target) && kubernetesMatchesGuest(observed, before)) return observed;
            sleep();
        }
        throw failure("CAPACITY_VERIFY", vm, "Kubernetes capacity did not match the resized guest");
    }

    @Override
    public void restoreSchedulability(KubernetesCluster cluster, UserVm vm, NodeCapacitySnapshot before, NodeAccess access, long deadline) throws Exception {
        if (before.isUnschedulable() && !before.isCloudStackResizeCordon()) return;
        String node = nodeName(vm);
        executeControl(access, "sudo /opt/bin/kubectl uncordon " + node);
        executeControl(access, "sudo /opt/bin/kubectl annotate node " + node + " " + RESIZE_ANNOTATION + "-");
        while (System.currentTimeMillis() < deadline) {
            if (!nodeSnapshot(cluster, vm, access, null).isUnschedulable()) return;
            sleep();
        }
        throw failure("RESTORE_SCHEDULABILITY", vm, "Kubernetes node remained unschedulable");
    }

    public static boolean capacityChanged(ServiceOffering oldOffering, ServiceOffering targetOffering) {
        return oldOffering != null && targetOffering != null
                && (!Objects.equals(oldOffering.getCpu(), targetOffering.getCpu())
                || !Objects.equals(oldOffering.getRamSize(), targetOffering.getRamSize()));
    }

    static long parseCpuMillis(String value) { return value.endsWith("m") ? Long.parseLong(value.substring(0, value.length() - 1)) : Long.parseLong(value) * 1000L; }
    static long parseMemoryBytes(String value) {
        Matcher matcher = MEMORY_QUANTITY.matcher(value);
        if (!matcher.matches()) throw new IllegalArgumentException("Unsupported Kubernetes memory quantity");
        long number = Long.parseLong(matcher.group(1)); String unit = matcher.group(2);
        if (unit == null) return number;
        switch (unit) {
            case "Ki": return number * 1024L; case "Mi": return number * 1024L * 1024L; case "Gi": return number * 1024L * 1024L * 1024L;
            case "Ti": return number * 1024L * 1024L * 1024L * 1024L; case "K": return number * 1000L; case "M": return number * 1000L * 1000L;
            case "G": return number * 1000L * 1000L * 1000L; case "T": return number * 1000L * 1000L * 1000L * 1000L;
            default: throw new IllegalArgumentException("Unsupported Kubernetes memory quantity");
        }
    }

    private NodeCapacitySnapshot nodeSnapshot(KubernetesCluster cluster, UserVm vm, NodeAccess access, GuestResources guest) throws Exception {
        JsonObject node = new JsonParser().parse(executeControl(access, "sudo /opt/bin/kubectl get node " + nodeName(vm) + " -o json")).getAsJsonObject();
        JsonObject annotations = node.getAsJsonObject("metadata").has("annotations") ? node.getAsJsonObject("metadata").getAsJsonObject("annotations") : null;
        boolean cksCordon = annotations != null && annotations.has(RESIZE_ANNOTATION) && cluster.getUuid().equals(annotations.get(RESIZE_ANNOTATION).getAsString());
        JsonObject spec = node.has("spec") ? node.getAsJsonObject("spec") : new JsonObject(); JsonObject status = node.getAsJsonObject("status");
        JsonObject capacity = status.getAsJsonObject("capacity"); JsonObject allocatable = status.getAsJsonObject("allocatable");
        return new NodeCapacitySnapshot(spec.has("unschedulable") && spec.get("unschedulable").getAsBoolean(), cksCordon,
                guest == null ? 0 : guest.cpu, guest == null ? 0 : guest.memoryKiB, parseCpuMillis(capacity.get("cpu").getAsString()),
                parseMemoryBytes(capacity.get("memory").getAsString()), parseCpuMillis(allocatable.get("cpu").getAsString()),
                parseMemoryBytes(allocatable.get("memory").getAsString()), isReady(status));
    }

    private boolean isReady(JsonObject status) {
        JsonArray conditions = status.getAsJsonArray("conditions");
        for (JsonElement condition : conditions) { JsonObject item = condition.getAsJsonObject(); if ("Ready".equals(item.get("type").getAsString()) && "True".equals(item.get("status").getAsString())) return true; }
        return false;
    }

    private GuestResources guestResources(NodeAccess access) throws Exception {
        String[] values = executeNode(access, "getconf _NPROCESSORS_ONLN; awk '/^MemTotal:/ {print $2}' /proc/meminfo").trim().split("\\s+");
        if (values.length != 2) throw new CloudRuntimeException("Unable to read guest CPU and memory");
        return new GuestResources(Long.parseLong(values[0]), Long.parseLong(values[1]));
    }

    private boolean guestMatchesTarget(GuestResources observed, ServiceOffering target) {
        long targetMemory = target.getRamSize() * 1024L * 1024L; long observedMemory = observed.memoryKiB * 1024L;
        long overhead = Math.max(targetMemory / 50L, MINIMUM_MEMORY_OVERHEAD_BYTES);
        return observed.cpu == target.getCpu() && observedMemory <= targetMemory && targetMemory - observedMemory <= overhead;
    }

    private boolean kubernetesMatchesGuest(NodeCapacitySnapshot observed, NodeCapacitySnapshot before) {
        return observed.isReady() && observed.getCapacityCpuMillis() == observed.getGuestOnlineCpuCount() * 1000L
                && Math.abs(observed.getCapacityMemoryBytes() - observed.getGuestMemoryKiB() * 1024L) <= KUBERNETES_MEMORY_REPORTING_TOLERANCE_BYTES
                && (observed.getGuestOnlineCpuCount() <= before.getGuestOnlineCpuCount() || observed.getAllocatableCpuMillis() > before.getAllocatableCpuMillis())
                && (observed.getGuestMemoryKiB() <= before.getGuestMemoryKiB() || observed.getAllocatableMemoryBytes() > before.getAllocatableMemoryBytes());
    }

    protected String executeControl(NodeAccess access, String command) throws Exception { return execute(access.getControlAddress(), access.getControlPort(), access, command); }
    protected String executeNode(NodeAccess access, String command) throws Exception { return execute(access.getNodeAddress(), access.getNodePort(), access, command); }
    private String execute(String address, int port, NodeAccess access, String command) throws Exception {
        Pair<Boolean, String> result = SshHelper.sshExecute(address, port, access.getUser(), access.getSshKeyFile(), null, command, 10000, 10000, COMMAND_TIMEOUT_MS);
        if (Boolean.TRUE.equals(result.first())) return StringUtils.defaultString(result.second());
        throw new CloudRuntimeException("CKS live-resize command failed");
    }
    private String nodeName(UserVm vm) { String name = StringUtils.lowerCase(vm.getHostName()); if (!NODE_NAME.matcher(name).matches()) throw new CloudRuntimeException("Invalid Kubernetes node name for live resize"); return name; }
    private CloudRuntimeException failure(String phase, UserVm vm, String message) { return new CloudRuntimeException("CKS live resize " + phase + " failed for VM " + vm.getUuid() + ": " + message); }
    private void sleep() throws InterruptedException { Thread.sleep(POLL_INTERVAL_MS); }
    private static class GuestResources { private final long cpu; private final long memoryKiB; GuestResources(long cpu, long memoryKiB) { this.cpu = cpu; this.memoryKiB = memoryKiB; } }
}
