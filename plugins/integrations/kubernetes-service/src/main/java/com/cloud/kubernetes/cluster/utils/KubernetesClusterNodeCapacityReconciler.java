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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cloudstack.utils.bytescale.ByteScaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.kubernetes.cluster.KubernetesCluster;
import com.cloud.offering.ServiceOffering;
import com.cloud.uservm.UserVm;
import com.cloud.utils.Pair;
import com.cloud.utils.ssh.SshHelper;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Brings the guest and Kubernetes views of a CKS node's CPU and memory up to date after a live VM resize,
 * using kubectl on the control plane and commands on the node itself.
 */
public class KubernetesClusterNodeCapacityReconciler {
    protected Logger logger = LogManager.getLogger(getClass());

    static final long KUBERNETES_MEMORY_REPORTING_TOLERANCE_BYTES = 16L * ByteScaleUtils.MiB;
    static final int MAX_NODE_UPDATE_ATTEMPTS = 3;
    static final long POLL_INTERVAL_MS = 5000L;
    static final String GET_NODE_COMMAND = "sudo /opt/bin/kubectl get node %s -o json --request-timeout=20s";
    // CPUs online, MemTotal in KiB, memory block size in hex bytes, and the number of online memory blocks.
    static final String GUEST_RESOURCES_COMMAND = "getconf _NPROCESSORS_ONLN; awk '/^MemTotal:/ {print $2}' /proc/meminfo; "
            + "cat /sys/devices/system/memory/block_size_bytes; cat /sys/devices/system/memory/memory*/state | grep -cx online";
    static final String RESTART_KUBELET_COMMAND = "sudo systemctl restart kubelet";
    // is-active exits non-zero for every state except active, so print the state instead of failing.
    static final String KUBELET_STATE_COMMAND = "systemctl is-active kubelet 2>/dev/null || true";
    static final String ONLINE_HOTPLUGGED_RESOURCES = "sudo sh -c 'for cpu in /sys/devices/system/cpu/cpu[0-9]*; do "
            + "[ -e \"$cpu/online\" ] || continue; echo 1 > \"$cpu/online\"; done; "
            + "for memory in /sys/devices/system/memory/memory[0-9]*; do "
            + "[ -e \"$memory/online\" ] || continue; echo online > \"$memory/online\"; done'";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int COMMAND_TIMEOUT_MS = 30000;
    private static final Pattern NODE_NAME = Pattern.compile("[a-z0-9][a-z0-9.-]{0,252}");
    private static final Pattern PATCH_VALUE = Pattern.compile("[A-Za-z0-9-]{1,64}");
    private static final Pattern GUEST_VALUE = Pattern.compile("[1-9]\\d{0,17}");
    private static final Pattern GUEST_HEX_VALUE = Pattern.compile("[0-9a-fA-F]{1,15}");
    private static final Pattern CPU_QUANTITY = Pattern.compile("(\\d{1,18})(m)?");
    private static final Pattern MEMORY_QUANTITY = Pattern.compile("(\\d{1,18})(Ki|Mi|Gi|Ti|K|M|G|T)?");
    private static final String READ_GUEST = "READ_GUEST";
    private static final String METADATA = "metadata";
    private static final String STATUS = "status";

    /** Whether the target offering changes the CPU count or the memory of the current one. */
    public static boolean capacityChanged(ServiceOffering currentOffering, ServiceOffering targetOffering) {
        return !Objects.equals(currentOffering.getCpu(), targetOffering.getCpu())
                || !Objects.equals(currentOffering.getRamSize(), targetOffering.getRamSize());
    }

    /** Whether the guest has more CPUs online, or at least one memory block more, than the target offering provides. */
    public static boolean isGuestAboveTarget(GuestResources guest, ServiceOffering target) {
        long targetMemory = target.getRamSize() * ByteScaleUtils.MiB;
        return guest.getOnlineCpus() > target.getCpu() || guest.getOnlineMemoryBytes() - targetMemory >= guest.getMemoryBlockBytes();
    }

    /**
     * Whether the guest has exactly the target CPUs online and its online memory blocks add up to the target
     * memory. MemTotal is not compared with the offering: it leaves out the memory the kernel keeps for
     * itself, which grows with the memory the VM booted with. The memory blocks add up to the RAM of the VM,
     * rounded to a whole block, so a memory step smaller than one block (128 MiB on most VMs) is not seen.
     */
    static boolean guestMatchesTarget(GuestResources guest, ServiceOffering target) {
        long targetMemory = target.getRamSize() * ByteScaleUtils.MiB;
        return guest.getOnlineCpus() == target.getCpu()
                && Math.abs(guest.getOnlineMemoryBytes() - targetMemory) < guest.getMemoryBlockBytes();
    }

    /**
     * Whether the Kubernetes node already reports the guest's current CPUs and memory. Kubelet computes
     * allocatable as capacity minus its reservations in the same status update, so a Ready node whose
     * capacity matches the guest and whose allocatable lies between zero and its capacity is up to date.
     */
    public boolean isKubernetesNodeAlreadyResized(NodeCapacitySnapshot node, GuestResources guest) {
        NodeResources capacity = node.getCapacity();
        NodeResources allocatable = node.getAllocatable();
        return node.isReady()
                && capacity.getCpuMillis() == guest.getOnlineCpus() * 1000L
                && Math.abs(capacity.getMemoryBytes() - guest.getMemoryBytes()) <= KUBERNETES_MEMORY_REPORTING_TOLERANCE_BYTES
                && allocatable.getCpuMillis() >= 0 && allocatable.getCpuMillis() <= capacity.getCpuMillis()
                && allocatable.getMemoryBytes() >= 0 && allocatable.getMemoryBytes() <= capacity.getMemoryBytes();
    }

    /** Reads the Kubernetes Node of a VM through the control plane. */
    public NodeCapacitySnapshot readNode(UserVm vm, NodeAccess access) {
        String output = executeControl(access, String.format(GET_NODE_COMMAND, nodeName(vm)));
        try {
            return parseNode(output);
        } catch (JsonParseException | IllegalArgumentException | ArithmeticException e) {
            throw failure("READ_NODE", vm, "unexpected kubectl output: " + e.getMessage(), e);
        }
    }

    /** Reads the CPUs online, MemTotal and the online memory blocks inside the VM. */
    public GuestResources readGuest(UserVm vm, NodeAccess access) {
        String[] values = executeNode(access, GUEST_RESOURCES_COMMAND).trim().split("\\s+");
        if (values.length != 4 || !GUEST_VALUE.matcher(values[0]).matches() || !GUEST_VALUE.matcher(values[1]).matches()
                || !GUEST_HEX_VALUE.matcher(values[2]).matches() || !GUEST_VALUE.matcher(values[3]).matches()) {
            throw failure(READ_GUEST, vm, "unexpected guest CPU and memory output");
        }
        long memoryBlockBytes = Long.parseLong(values[2], 16);
        if (memoryBlockBytes == 0) {
            throw failure(READ_GUEST, vm, "unexpected guest memory block size");
        }
        try {
            return new GuestResources(Long.parseLong(values[0]),
                    Long.parseLong(values[1]),
                    Math.multiplyExact(Long.parseLong(values[3]), memoryBlockBytes),
                    memoryBlockBytes);
        } catch (ArithmeticException e) {
            throw failure(READ_GUEST, vm, "unexpected guest memory size", e);
        }
    }

    public void onlineGuestResources(NodeAccess access) {
        executeNode(access, ONLINE_HOTPLUGGED_RESOURCES);
    }

    public boolean isKubeletActive(NodeAccess access) {
        return "active".equals(executeNode(access, KUBELET_STATE_COMMAND).trim());
    }

    /**
     * Marks the node as having unverified resize work and, unless it is already cordoned, cordons it and
     * records that CKS owns the cordon. An operator's cordon is left as it is.
     */
    public NodeCapacitySnapshot startResize(KubernetesCluster cluster, UserVm vm, NodeAccess access, String nodeUid) {
        String owner = patchValue(cluster.getUuid(), vm);
        return updateNode("START", vm, access, nodeUid, node -> {
            if (node.hasForeignMarker(owner)) {
                throw failure("START", vm, "the Kubernetes node carries live resize markers of another cluster");
            }
            if (node.isPendingFor(owner) && node.isUnschedulable()) {
                return null;
            }
            JsonObject annotations = new JsonObject();
            annotations.addProperty(NodeCapacitySnapshot.PENDING_ANNOTATION, owner);
            if (node.isUnschedulable()) {
                return nodePatch(vm, node, annotations, null);
            }
            annotations.addProperty(NodeCapacitySnapshot.CORDON_ANNOTATION, owner);
            return nodePatch(vm, node, annotations, true);
        });
    }

    /** Removes this cluster's markers and, only if CKS owned the cordon, makes the node schedulable again. */
    public NodeCapacitySnapshot finishResize(KubernetesCluster cluster, UserVm vm, NodeAccess access, String nodeUid) {
        String owner = cluster.getUuid();
        return updateNode("FINISH", vm, access, nodeUid, node -> {
            boolean pending = node.isPendingFor(owner);
            boolean ownershipMarker = node.hasOwnershipMarker(owner);
            if (!pending && !ownershipMarker) {
                return null;
            }
            JsonObject annotations = new JsonObject();
            if (pending) {
                annotations.add(NodeCapacitySnapshot.PENDING_ANNOTATION, JsonNull.INSTANCE);
            }
            if (ownershipMarker) {
                annotations.add(NodeCapacitySnapshot.CORDON_ANNOTATION, JsonNull.INSTANCE);
            }
            return nodePatch(vm, node, annotations, node.isCordonOwnedBy(owner) ? Boolean.FALSE : null);
        });
    }

    /** Removes a CKS cordon and its ownership marker but keeps the pending marker. */
    public NodeCapacitySnapshot releaseCordon(KubernetesCluster cluster, UserVm vm, NodeAccess access, String nodeUid) {
        String owner = cluster.getUuid();
        return updateNode("RELEASE_CORDON", vm, access, nodeUid, node -> {
            if (!node.hasOwnershipMarker(owner)) {
                return null;
            }
            JsonObject annotations = new JsonObject();
            annotations.add(NodeCapacitySnapshot.CORDON_ANNOTATION, JsonNull.INSTANCE);
            return nodePatch(vm, node, annotations, node.isCordonOwnedBy(owner) ? Boolean.FALSE : null);
        });
    }

    /** Waits until the guest has the target resources online; fails at once if it has more than the target. */
    public GuestResources waitForGuestResources(UserVm vm, ServiceOffering target, NodeAccess access, long deadline) {
        return poll("GUEST_VERIFY", vm, deadline, "guest CPU or memory did not reach the target offering", () -> {
            GuestResources guest = readGuest(vm, access);
            if (isGuestAboveTarget(guest, target)) {
                throw failure("GUEST_VERIFY", vm, String.format("the guest has %d CPUs and %d MiB of memory online, more than the target offering",
                        guest.getOnlineCpus(), guest.getOnlineMemoryBytes() / ByteScaleUtils.MiB));
            }
            return guestMatchesTarget(guest, target) ? guest : null;
        });
    }

    /** Restarts kubelet so it reads the new machine resources, and waits until it is active again. */
    public void restartKubelet(UserVm vm, NodeAccess access, long deadline) {
        if (currentTimeMillis() >= deadline) {
            throw failure("KUBELET_RESTART", vm, "the scale timeout was reached before kubelet was restarted");
        }
        executeNode(access, RESTART_KUBELET_COMMAND);
        poll("KUBELET_RESTART", vm, deadline, "kubelet did not become active", () -> isKubeletActive(access) ? Boolean.TRUE : null);
    }

    public NodeCapacitySnapshot waitForKubernetesResources(UserVm vm, GuestResources guest, NodeAccess access, long deadline) {
        return poll("CAPACITY_VERIFY", vm, deadline, "Kubernetes node capacity did not match the resized guest", () -> {
            NodeCapacitySnapshot node = readNode(vm, access);
            return isKubernetesNodeAlreadyResized(node, guest) ? node : null;
        });
    }

    static NodeCapacitySnapshot parseNode(String output) {
        JsonObject node = requireObject(JsonParser.parseString(output), "node");
        JsonObject metadata = requireObject(node.get(METADATA), METADATA);
        JsonObject status = requireObject(node.get(STATUS), STATUS);
        JsonElement spec = node.get("spec");
        boolean unschedulable = spec != null && spec.isJsonObject() && isTrue(spec.getAsJsonObject().get("unschedulable"));
        return new NodeCapacitySnapshot(requireString(metadata.get("uid"), "metadata.uid"),
                requireString(metadata.get("resourceVersion"), "metadata.resourceVersion"),
                unschedulable,
                readAnnotations(metadata.get("annotations")),
                isReady(status.get("conditions")),
                readResources(status.get("capacity"), "status.capacity"),
                readResources(status.get("allocatable"), "status.allocatable"));
    }

    static long parseCpuMillis(String value) {
        Matcher matcher = CPU_QUANTITY.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("unsupported Kubernetes CPU quantity " + value);
        }
        long number = Long.parseLong(matcher.group(1));
        return matcher.group(2) == null ? Math.multiplyExact(number, 1000L) : number;
    }

    static long parseMemoryBytes(String value) {
        Matcher matcher = MEMORY_QUANTITY.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("unsupported Kubernetes memory quantity " + value);
        }
        return Math.multiplyExact(Long.parseLong(matcher.group(1)), memoryUnitBytes(matcher.group(2)));
    }

    private static long memoryUnitBytes(String unit) {
        if (unit == null) {
            return 1L;
        }
        switch (unit) {
            case "Ki":
                return ByteScaleUtils.KiB;
            case "Mi":
                return ByteScaleUtils.MiB;
            case "Gi":
                return ByteScaleUtils.GiB;
            case "Ti":
                return ByteScaleUtils.GiB * ByteScaleUtils.KiB;
            case "K":
                return 1000L;
            case "M":
                return 1000L * 1000L;
            case "G":
                return 1000L * 1000L * 1000L;
            case "T":
                return 1000L * 1000L * 1000L * 1000L;
            default:
                throw new IllegalArgumentException("unsupported Kubernetes memory unit " + unit);
        }
    }

    private static JsonObject requireObject(JsonElement element, String name) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("missing " + name);
        }
        return element.getAsJsonObject();
    }

    private static String requireString(JsonElement element, String name) {
        if (!isString(element)) {
            throw new IllegalArgumentException("missing " + name);
        }
        return element.getAsString();
    }

    private static boolean isString(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString();
    }

    private static boolean isTrue(JsonElement element) {
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean() && element.getAsBoolean();
    }

    private static Map<String, String> readAnnotations(JsonElement element) {
        Map<String, String> annotations = new HashMap<>();
        if (element == null || !element.isJsonObject()) {
            return annotations;
        }
        for (Map.Entry<String, JsonElement> annotation : element.getAsJsonObject().entrySet()) {
            if (isString(annotation.getValue())) {
                annotations.put(annotation.getKey(), annotation.getValue().getAsString());
            }
        }
        return annotations;
    }

    private static boolean isReady(JsonElement conditions) {
        if (conditions == null || !conditions.isJsonArray()) {
            return false;
        }
        for (JsonElement condition : conditions.getAsJsonArray()) {
            if (!condition.isJsonObject()) {
                continue;
            }
            JsonObject item = condition.getAsJsonObject();
            if (isString(item.get("type")) && "Ready".equals(item.get("type").getAsString())
                    && isString(item.get(STATUS)) && "True".equals(item.get(STATUS).getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static NodeResources readResources(JsonElement element, String name) {
        JsonObject resources = requireObject(element, name);
        return new NodeResources(parseCpuMillis(requireString(resources.get("cpu"), name + ".cpu")),
                parseMemoryBytes(requireString(resources.get("memory"), name + ".memory")));
    }

    /**
     * Applies the patch that patchFor returns for a fresh read of the node, until patchFor finds nothing
     * left to change. Every patch carries the resourceVersion it was based on, so the API server rejects it
     * if the node changed in the meantime. After a conflict, a rejected update or a lost reply the next
     * read shows what was applied.
     */
    private NodeCapacitySnapshot updateNode(String phase, UserVm vm, NodeAccess access, String nodeUid,
                                            Function<NodeCapacitySnapshot, String> patchFor) {
        String nodeName = nodeName(vm);
        for (int attempt = 0; attempt <= MAX_NODE_UPDATE_ATTEMPTS; attempt++) {
            NodeCapacitySnapshot node = readNode(vm, access);
            if (!node.getUid().equals(nodeUid)) {
                throw failure(phase, vm, "the Kubernetes node was replaced during the resize");
            }
            String patch = patchFor.apply(node);
            if (patch == null) {
                return node;
            }
            if (attempt < MAX_NODE_UPDATE_ATTEMPTS) {
                applyPatch(nodeName, patch, access);
            }
        }
        throw failure(phase, vm, String.format("the Kubernetes node could not be updated in %d attempts", MAX_NODE_UPDATE_ATTEMPTS));
    }

    private void applyPatch(String nodeName, String patch, NodeAccess access) {
        try {
            executeControl(access, KubernetesClusterUtil.getKubernetesNodePatchCommand(nodeName, patch));
        } catch (NodeCapacityReconciliationException e) {
            if (Thread.currentThread().isInterrupted()) {
                throw e;
            }
            logger.debug("Updating Kubernetes node {} did not succeed, reading it again: {}", nodeName, e.getMessage());
        }
    }

    private String nodePatch(UserVm vm, NodeCapacitySnapshot node, JsonObject annotations, Boolean unschedulable) {
        JsonObject metadata = new JsonObject();
        metadata.addProperty("resourceVersion", patchValue(node.getResourceVersion(), vm));
        metadata.add("annotations", annotations);
        JsonObject patch = new JsonObject();
        patch.add(METADATA, metadata);
        if (unschedulable != null) {
            JsonObject spec = new JsonObject();
            spec.addProperty("unschedulable", unschedulable);
            patch.add("spec", spec);
        }
        return patch.toString();
    }

    /** Values go into a single-quoted shell argument, so allow only characters that need no quoting. */
    private String patchValue(String value, UserVm vm) {
        if (value == null || !PATCH_VALUE.matcher(value).matches()) {
            throw failure("NODE_UPDATE", vm, "unexpected value in a Kubernetes node update: " + value);
        }
        return value;
    }

    /** Observes once, then keeps observing while time remains; at most one observation follows the deadline. */
    private <T> T poll(String phase, UserVm vm, long deadline, String timeoutMessage, Supplier<T> observation) {
        while (true) {
            T result = observation.get();
            if (result != null) {
                return result;
            }
            long remaining = deadline - currentTimeMillis();
            if (remaining <= 0) {
                throw failure(phase, vm, timeoutMessage);
            }
            sleep(Math.min(POLL_INTERVAL_MS, remaining));
        }
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NodeCapacityReconciliationException("CKS live resize polling was interrupted", e);
        }
    }

    protected String executeControl(NodeAccess access, String command) {
        return execute(access.getControlAddress(), access.getControlPort(), access, command);
    }

    protected String executeNode(NodeAccess access, String command) {
        return execute(access.getNodeAddress(), access.getNodePort(), access, command);
    }

    private String execute(String address, int port, NodeAccess access, String command) {
        Pair<Boolean, String> result;
        try {
            result = SshHelper.sshExecute(address, port, access.getUser(), access.getSshKeyFile(), null, command,
                    CONNECT_TIMEOUT_MS, CONNECT_TIMEOUT_MS, COMMAND_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NodeCapacityReconciliationException(String.format("Command '%s' on %s was interrupted", command, address), e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // SshHelper declares Exception, including connection and authentication failures.
            throw new NodeCapacityReconciliationException(String.format("Unable to run command '%s' on %s: %s", command, address, e.getMessage()), e);
        }
        if (!Boolean.TRUE.equals(result.first())) {
            throw new NodeCapacityReconciliationException(String.format("Command '%s' failed on %s: %s",
                    command, address, StringUtils.abbreviate(StringUtils.trimToEmpty(result.second()), 300)));
        }
        return StringUtils.defaultString(result.second());
    }

    private String nodeName(UserVm vm) {
        String name = KubernetesClusterUtil.getKubernetesNodeName(vm);
        if (StringUtils.isBlank(name)) {
            throw failure("NODE_NAME", vm, "the VM has no host name");
        }
        if (!NODE_NAME.matcher(name).matches()) {
            throw failure("NODE_NAME", vm, "the VM host name is not a valid Kubernetes node name");
        }
        return name;
    }

    private NodeCapacityReconciliationException failure(String phase, UserVm vm, String message) {
        return failure(phase, vm, message, null);
    }

    private NodeCapacityReconciliationException failure(String phase, UserVm vm, String message, Exception cause) {
        return new NodeCapacityReconciliationException(String.format("CKS live resize %s failed for VM %s: %s", phase, vm.getUuid(), message), cause);
    }
}
