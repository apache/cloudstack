/*
 * Copyright (c) Citrix Systems, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   1) Redistributions of source code must retain the above copyright
 *      notice, this list of conditions and the following disclaimer.
 *
 *   2) Redistributions in binary form must reproduce the above
 *      copyright notice, this list of conditions and the following
 *      disclaimer in the documentation and/or other materials
 *      provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.xensource.xenapi;

import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.VersionException;
import com.xensource.xenapi.Types.XenAPIException;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.xmlrpc.XmlRpcException;

/**
 * A virtual machine (or 'guest').
 *
 * @author Citrix Systems, Inc.
 */
public class VM extends XenAPIObject {

    /**
     * The XenAPI reference (OpaqueRef) to this object.
     */
    protected final String ref;

    /**
     * For internal use only.
     */
    VM(String ref) {
       this.ref = ref;
    }

    /**
     * @return The XenAPI reference (OpaqueRef) to this object.
     */
    public String toWireString() {
       return this.ref;
    }

    /**
     * If obj is a VM, compares XenAPI references for equality.
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof VM)
        {
            VM other = (VM) obj;
            return other.ref.equals(this.ref);
        } else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        return ref.hashCode();
    }

    /**
     * Represents all the fields in a VM
     */
    public static class Record implements Types.Record {
        public String toString() {
            StringWriter writer = new StringWriter();
            PrintWriter print = new PrintWriter(writer);
            print.printf("%1$20s: %2$s\n", "uuid", this.uuid);
            print.printf("%1$20s: %2$s\n", "allowedOperations", this.allowedOperations);
            print.printf("%1$20s: %2$s\n", "currentOperations", this.currentOperations);
            print.printf("%1$20s: %2$s\n", "powerState", this.powerState);
            print.printf("%1$20s: %2$s\n", "nameLabel", this.nameLabel);
            print.printf("%1$20s: %2$s\n", "nameDescription", this.nameDescription);
            print.printf("%1$20s: %2$s\n", "userVersion", this.userVersion);
            print.printf("%1$20s: %2$s\n", "isATemplate", this.isATemplate);
            print.printf("%1$20s: %2$s\n", "suspendVDI", this.suspendVDI);
            print.printf("%1$20s: %2$s\n", "residentOn", this.residentOn);
            print.printf("%1$20s: %2$s\n", "affinity", this.affinity);
            print.printf("%1$20s: %2$s\n", "memoryOverhead", this.memoryOverhead);
            print.printf("%1$20s: %2$s\n", "memoryTarget", this.memoryTarget);
            print.printf("%1$20s: %2$s\n", "memoryStaticMax", this.memoryStaticMax);
            print.printf("%1$20s: %2$s\n", "memoryDynamicMax", this.memoryDynamicMax);
            print.printf("%1$20s: %2$s\n", "memoryDynamicMin", this.memoryDynamicMin);
            print.printf("%1$20s: %2$s\n", "memoryStaticMin", this.memoryStaticMin);
            print.printf("%1$20s: %2$s\n", "VCPUsParams", this.VCPUsParams);
            print.printf("%1$20s: %2$s\n", "VCPUsMax", this.VCPUsMax);
            print.printf("%1$20s: %2$s\n", "VCPUsAtStartup", this.VCPUsAtStartup);
            print.printf("%1$20s: %2$s\n", "actionsAfterShutdown", this.actionsAfterShutdown);
            print.printf("%1$20s: %2$s\n", "actionsAfterReboot", this.actionsAfterReboot);
            print.printf("%1$20s: %2$s\n", "actionsAfterCrash", this.actionsAfterCrash);
            print.printf("%1$20s: %2$s\n", "consoles", this.consoles);
            print.printf("%1$20s: %2$s\n", "VIFs", this.VIFs);
            print.printf("%1$20s: %2$s\n", "VBDs", this.VBDs);
            print.printf("%1$20s: %2$s\n", "crashDumps", this.crashDumps);
            print.printf("%1$20s: %2$s\n", "VTPMs", this.VTPMs);
            print.printf("%1$20s: %2$s\n", "PVBootloader", this.PVBootloader);
            print.printf("%1$20s: %2$s\n", "PVKernel", this.PVKernel);
            print.printf("%1$20s: %2$s\n", "PVRamdisk", this.PVRamdisk);
            print.printf("%1$20s: %2$s\n", "PVArgs", this.PVArgs);
            print.printf("%1$20s: %2$s\n", "PVBootloaderArgs", this.PVBootloaderArgs);
            print.printf("%1$20s: %2$s\n", "PVLegacyArgs", this.PVLegacyArgs);
            print.printf("%1$20s: %2$s\n", "HVMBootPolicy", this.HVMBootPolicy);
            print.printf("%1$20s: %2$s\n", "HVMBootParams", this.HVMBootParams);
            print.printf("%1$20s: %2$s\n", "HVMShadowMultiplier", this.HVMShadowMultiplier);
            print.printf("%1$20s: %2$s\n", "platform", this.platform);
            print.printf("%1$20s: %2$s\n", "PCIBus", this.PCIBus);
            print.printf("%1$20s: %2$s\n", "otherConfig", this.otherConfig);
            print.printf("%1$20s: %2$s\n", "domid", this.domid);
            print.printf("%1$20s: %2$s\n", "domarch", this.domarch);
            print.printf("%1$20s: %2$s\n", "lastBootCPUFlags", this.lastBootCPUFlags);
            print.printf("%1$20s: %2$s\n", "isControlDomain", this.isControlDomain);
            print.printf("%1$20s: %2$s\n", "metrics", this.metrics);
            print.printf("%1$20s: %2$s\n", "guestMetrics", this.guestMetrics);
            print.printf("%1$20s: %2$s\n", "lastBootedRecord", this.lastBootedRecord);
            print.printf("%1$20s: %2$s\n", "recommendations", this.recommendations);
            print.printf("%1$20s: %2$s\n", "xenstoreData", this.xenstoreData);
            print.printf("%1$20s: %2$s\n", "haAlwaysRun", this.haAlwaysRun);
            print.printf("%1$20s: %2$s\n", "haRestartPriority", this.haRestartPriority);
            print.printf("%1$20s: %2$s\n", "isASnapshot", this.isASnapshot);
            print.printf("%1$20s: %2$s\n", "snapshotOf", this.snapshotOf);
            print.printf("%1$20s: %2$s\n", "snapshots", this.snapshots);
            print.printf("%1$20s: %2$s\n", "snapshotTime", this.snapshotTime);
            print.printf("%1$20s: %2$s\n", "transportableSnapshotId", this.transportableSnapshotId);
            print.printf("%1$20s: %2$s\n", "blobs", this.blobs);
            print.printf("%1$20s: %2$s\n", "tags", this.tags);
            print.printf("%1$20s: %2$s\n", "blockedOperations", this.blockedOperations);
            print.printf("%1$20s: %2$s\n", "snapshotInfo", this.snapshotInfo);
            print.printf("%1$20s: %2$s\n", "snapshotMetadata", this.snapshotMetadata);
            print.printf("%1$20s: %2$s\n", "parent", this.parent);
            print.printf("%1$20s: %2$s\n", "children", this.children);
            print.printf("%1$20s: %2$s\n", "biosStrings", this.biosStrings);
            print.printf("%1$20s: %2$s\n", "protectionPolicy", this.protectionPolicy);
            print.printf("%1$20s: %2$s\n", "isSnapshotFromVmpp", this.isSnapshotFromVmpp);
            print.printf("%1$20s: %2$s\n", "appliance", this.appliance);
            print.printf("%1$20s: %2$s\n", "startDelay", this.startDelay);
            print.printf("%1$20s: %2$s\n", "shutdownDelay", this.shutdownDelay);
            print.printf("%1$20s: %2$s\n", "order", this.order);
            print.printf("%1$20s: %2$s\n", "VGPUs", this.VGPUs);
            print.printf("%1$20s: %2$s\n", "attachedPCIs", this.attachedPCIs);
            print.printf("%1$20s: %2$s\n", "suspendSR", this.suspendSR);
            print.printf("%1$20s: %2$s\n", "version", this.version);
            return writer.toString();
        }

        /**
         * Convert a VM.Record to a Map
         */
        public Map<String,Object> toMap() {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("uuid", this.uuid == null ? "" : this.uuid);
            map.put("allowed_operations", this.allowedOperations == null ? new LinkedHashSet<Types.VmOperations>() : this.allowedOperations);
            map.put("current_operations", this.currentOperations == null ? new HashMap<String, Types.VmOperations>() : this.currentOperations);
            map.put("power_state", this.powerState == null ? Types.VmPowerState.UNRECOGNIZED : this.powerState);
            map.put("name_label", this.nameLabel == null ? "" : this.nameLabel);
            map.put("name_description", this.nameDescription == null ? "" : this.nameDescription);
            map.put("user_version", this.userVersion == null ? 0 : this.userVersion);
            map.put("is_a_template", this.isATemplate == null ? false : this.isATemplate);
            map.put("suspend_VDI", this.suspendVDI == null ? new VDI("OpaqueRef:NULL") : this.suspendVDI);
            map.put("resident_on", this.residentOn == null ? new Host("OpaqueRef:NULL") : this.residentOn);
            map.put("affinity", this.affinity == null ? new Host("OpaqueRef:NULL") : this.affinity);
            map.put("memory_overhead", this.memoryOverhead == null ? 0 : this.memoryOverhead);
            map.put("memory_target", this.memoryTarget == null ? 0 : this.memoryTarget);
            map.put("memory_static_max", this.memoryStaticMax == null ? 0 : this.memoryStaticMax);
            map.put("memory_dynamic_max", this.memoryDynamicMax == null ? 0 : this.memoryDynamicMax);
            map.put("memory_dynamic_min", this.memoryDynamicMin == null ? 0 : this.memoryDynamicMin);
            map.put("memory_static_min", this.memoryStaticMin == null ? 0 : this.memoryStaticMin);
            map.put("VCPUs_params", this.VCPUsParams == null ? new HashMap<String, String>() : this.VCPUsParams);
            map.put("VCPUs_max", this.VCPUsMax == null ? 0 : this.VCPUsMax);
            map.put("VCPUs_at_startup", this.VCPUsAtStartup == null ? 0 : this.VCPUsAtStartup);
            map.put("actions_after_shutdown", this.actionsAfterShutdown == null ? Types.OnNormalExit.UNRECOGNIZED : this.actionsAfterShutdown);
            map.put("actions_after_reboot", this.actionsAfterReboot == null ? Types.OnNormalExit.UNRECOGNIZED : this.actionsAfterReboot);
            map.put("actions_after_crash", this.actionsAfterCrash == null ? Types.OnCrashBehaviour.UNRECOGNIZED : this.actionsAfterCrash);
            map.put("consoles", this.consoles == null ? new LinkedHashSet<Console>() : this.consoles);
            map.put("VIFs", this.VIFs == null ? new LinkedHashSet<VIF>() : this.VIFs);
            map.put("VBDs", this.VBDs == null ? new LinkedHashSet<VBD>() : this.VBDs);
            map.put("crash_dumps", this.crashDumps == null ? new LinkedHashSet<Crashdump>() : this.crashDumps);
            map.put("VTPMs", this.VTPMs == null ? new LinkedHashSet<VTPM>() : this.VTPMs);
            map.put("PV_bootloader", this.PVBootloader == null ? "" : this.PVBootloader);
            map.put("PV_kernel", this.PVKernel == null ? "" : this.PVKernel);
            map.put("PV_ramdisk", this.PVRamdisk == null ? "" : this.PVRamdisk);
            map.put("PV_args", this.PVArgs == null ? "" : this.PVArgs);
            map.put("PV_bootloader_args", this.PVBootloaderArgs == null ? "" : this.PVBootloaderArgs);
            map.put("PV_legacy_args", this.PVLegacyArgs == null ? "" : this.PVLegacyArgs);
            map.put("HVM_boot_policy", this.HVMBootPolicy == null ? "" : this.HVMBootPolicy);
            map.put("HVM_boot_params", this.HVMBootParams == null ? new HashMap<String, String>() : this.HVMBootParams);
            map.put("HVM_shadow_multiplier", this.HVMShadowMultiplier == null ? 0.0 : this.HVMShadowMultiplier);
            map.put("platform", this.platform == null ? new HashMap<String, String>() : this.platform);
            map.put("PCI_bus", this.PCIBus == null ? "" : this.PCIBus);
            map.put("other_config", this.otherConfig == null ? new HashMap<String, String>() : this.otherConfig);
            map.put("domid", this.domid == null ? 0 : this.domid);
            map.put("domarch", this.domarch == null ? "" : this.domarch);
            map.put("last_boot_CPU_flags", this.lastBootCPUFlags == null ? new HashMap<String, String>() : this.lastBootCPUFlags);
            map.put("is_control_domain", this.isControlDomain == null ? false : this.isControlDomain);
            map.put("metrics", this.metrics == null ? new VMMetrics("OpaqueRef:NULL") : this.metrics);
            map.put("guest_metrics", this.guestMetrics == null ? new VMGuestMetrics("OpaqueRef:NULL") : this.guestMetrics);
            map.put("last_booted_record", this.lastBootedRecord == null ? "" : this.lastBootedRecord);
            map.put("recommendations", this.recommendations == null ? "" : this.recommendations);
            map.put("xenstore_data", this.xenstoreData == null ? new HashMap<String, String>() : this.xenstoreData);
            map.put("ha_always_run", this.haAlwaysRun == null ? false : this.haAlwaysRun);
            map.put("ha_restart_priority", this.haRestartPriority == null ? "" : this.haRestartPriority);
            map.put("is_a_snapshot", this.isASnapshot == null ? false : this.isASnapshot);
            map.put("snapshot_of", this.snapshotOf == null ? new VM("OpaqueRef:NULL") : this.snapshotOf);
            map.put("snapshots", this.snapshots == null ? new LinkedHashSet<VM>() : this.snapshots);
            map.put("snapshot_time", this.snapshotTime == null ? new Date(0) : this.snapshotTime);
            map.put("transportable_snapshot_id", this.transportableSnapshotId == null ? "" : this.transportableSnapshotId);
            map.put("blobs", this.blobs == null ? new HashMap<String, Blob>() : this.blobs);
            map.put("tags", this.tags == null ? new LinkedHashSet<String>() : this.tags);
            map.put("blocked_operations", this.blockedOperations == null ? new HashMap<Types.VmOperations, String>() : this.blockedOperations);
            map.put("snapshot_info", this.snapshotInfo == null ? new HashMap<String, String>() : this.snapshotInfo);
            map.put("snapshot_metadata", this.snapshotMetadata == null ? "" : this.snapshotMetadata);
            map.put("parent", this.parent == null ? new VM("OpaqueRef:NULL") : this.parent);
            map.put("children", this.children == null ? new LinkedHashSet<VM>() : this.children);
            map.put("bios_strings", this.biosStrings == null ? new HashMap<String, String>() : this.biosStrings);
            map.put("protection_policy", this.protectionPolicy == null ? new VMPP("OpaqueRef:NULL") : this.protectionPolicy);
            map.put("is_snapshot_from_vmpp", this.isSnapshotFromVmpp == null ? false : this.isSnapshotFromVmpp);
            map.put("appliance", this.appliance == null ? new VMAppliance("OpaqueRef:NULL") : this.appliance);
            map.put("start_delay", this.startDelay == null ? 0 : this.startDelay);
            map.put("shutdown_delay", this.shutdownDelay == null ? 0 : this.shutdownDelay);
            map.put("order", this.order == null ? 0 : this.order);
            map.put("VGPUs", this.VGPUs == null ? new LinkedHashSet<VGPU>() : this.VGPUs);
            map.put("attached_PCIs", this.attachedPCIs == null ? new LinkedHashSet<PCI>() : this.attachedPCIs);
            map.put("suspend_SR", this.suspendSR == null ? new SR("OpaqueRef:NULL") : this.suspendSR);
            map.put("version", this.version == null ? 0 : this.version);
            return map;
        }

        /**
         * Unique identifier/object reference
         */
        public String uuid;
        /**
         * list of the operations allowed in this state. This list is advisory only and the server state may have changed by the time this field is read by a client.
         */
        public Set<Types.VmOperations> allowedOperations;
        /**
         * links each of the running tasks using this object (by reference) to a current_operation enum which describes the nature of the task.
         */
        public Map<String, Types.VmOperations> currentOperations;
        /**
         * Current power state of the machine
         */
        public Types.VmPowerState powerState;
        /**
         * a human-readable name
         */
        public String nameLabel;
        /**
         * a notes field containing human-readable description
         */
        public String nameDescription;
        /**
         * a user version number for this machine
         */
        public Long userVersion;
        /**
         * true if this is a template. Template VMs can never be started, they are used only for cloning other VMs
         */
        public Boolean isATemplate;
        /**
         * The VDI that a suspend image is stored on. (Only has meaning if VM is currently suspended)
         */
        public VDI suspendVDI;
        /**
         * the host the VM is currently resident on
         */
        public Host residentOn;
        /**
         * a host which the VM has some affinity for (or NULL). This is used as a hint to the start call when it decides where to run the VM. Implementations are free to ignore this field.
         */
        public Host affinity;
        /**
         * Virtualization memory overhead (bytes).
         */
        public Long memoryOverhead;
        /**
         * Dynamically-set memory target (bytes). The value of this field indicates the current target for memory available to this VM.
         */
        public Long memoryTarget;
        /**
         * Statically-set (i.e. absolute) maximum (bytes). The value of this field at VM start time acts as a hard limit of the amount of memory a guest can use. New values only take effect on reboot.
         */
        public Long memoryStaticMax;
        /**
         * Dynamic maximum (bytes)
         */
        public Long memoryDynamicMax;
        /**
         * Dynamic minimum (bytes)
         */
        public Long memoryDynamicMin;
        /**
         * Statically-set (i.e. absolute) mininum (bytes). The value of this field indicates the least amount of memory this VM can boot with without crashing.
         */
        public Long memoryStaticMin;
        /**
         * configuration parameters for the selected VCPU policy
         */
        public Map<String, String> VCPUsParams;
        /**
         * Max number of VCPUs
         */
        public Long VCPUsMax;
        /**
         * Boot number of VCPUs
         */
        public Long VCPUsAtStartup;
        /**
         * action to take after the guest has shutdown itself
         */
        public Types.OnNormalExit actionsAfterShutdown;
        /**
         * action to take after the guest has rebooted itself
         */
        public Types.OnNormalExit actionsAfterReboot;
        /**
         * action to take if the guest crashes
         */
        public Types.OnCrashBehaviour actionsAfterCrash;
        /**
         * virtual console devices
         */
        public Set<Console> consoles;
        /**
         * virtual network interfaces
         */
        public Set<VIF> VIFs;
        /**
         * virtual block devices
         */
        public Set<VBD> VBDs;
        /**
         * crash dumps associated with this VM
         */
        public Set<Crashdump> crashDumps;
        /**
         * virtual TPMs
         */
        public Set<VTPM> VTPMs;
        /**
         * name of or path to bootloader
         */
        public String PVBootloader;
        /**
         * path to the kernel
         */
        public String PVKernel;
        /**
         * path to the initrd
         */
        public String PVRamdisk;
        /**
         * kernel command-line arguments
         */
        public String PVArgs;
        /**
         * miscellaneous arguments for the bootloader
         */
        public String PVBootloaderArgs;
        /**
         * to make Zurich guests boot
         */
        public String PVLegacyArgs;
        /**
         * HVM boot policy
         */
        public String HVMBootPolicy;
        /**
         * HVM boot params
         */
        public Map<String, String> HVMBootParams;
        /**
         * multiplier applied to the amount of shadow that will be made available to the guest
         */
        public Double HVMShadowMultiplier;
        /**
         * platform-specific configuration
         */
        public Map<String, String> platform;
        /**
         * PCI bus path for pass-through devices
         */
        public String PCIBus;
        /**
         * additional configuration
         */
        public Map<String, String> otherConfig;
        /**
         * domain ID (if available, -1 otherwise)
         */
        public Long domid;
        /**
         * Domain architecture (if available, null string otherwise)
         */
        public String domarch;
        /**
         * describes the CPU flags on which the VM was last booted
         */
        public Map<String, String> lastBootCPUFlags;
        /**
         * true if this is a control domain (domain 0 or a driver domain)
         */
        public Boolean isControlDomain;
        /**
         * metrics associated with this VM
         */
        public VMMetrics metrics;
        /**
         * metrics associated with the running guest
         */
        public VMGuestMetrics guestMetrics;
        /**
         * marshalled value containing VM record at time of last boot, updated dynamically to reflect the runtime state of the domain
         */
        public String lastBootedRecord;
        /**
         * An XML specification of recommended values and ranges for properties of this VM
         */
        public String recommendations;
        /**
         * data to be inserted into the xenstore tree (/local/domain/<domid>/vm-data) after the VM is created.
         */
        public Map<String, String> xenstoreData;
        /**
         * if true then the system will attempt to keep the VM running as much as possible.
         */
        public Boolean haAlwaysRun;
        /**
         * has possible values: "best-effort" meaning "try to restart this VM if possible but don't consider the Pool to be overcommitted if this is not possible"; "restart" meaning "this VM should be restarted"; "" meaning "do not try to restart this VM"
         */
        public String haRestartPriority;
        /**
         * true if this is a snapshot. Snapshotted VMs can never be started, they are used only for cloning other VMs
         */
        public Boolean isASnapshot;
        /**
         * Ref pointing to the VM this snapshot is of.
         */
        public VM snapshotOf;
        /**
         * List pointing to all the VM snapshots.
         */
        public Set<VM> snapshots;
        /**
         * Date/time when this snapshot was created.
         */
        public Date snapshotTime;
        /**
         * Transportable ID of the snapshot VM
         */
        public String transportableSnapshotId;
        /**
         * Binary blobs associated with this VM
         */
        public Map<String, Blob> blobs;
        /**
         * user-specified tags for categorization purposes
         */
        public Set<String> tags;
        /**
         * List of operations which have been explicitly blocked and an error code
         */
        public Map<Types.VmOperations, String> blockedOperations;
        /**
         * Human-readable information concerning this snapshot
         */
        public Map<String, String> snapshotInfo;
        /**
         * Encoded information about the VM's metadata this is a snapshot of
         */
        public String snapshotMetadata;
        /**
         * Ref pointing to the parent of this VM
         */
        public VM parent;
        /**
         * List pointing to all the children of this VM
         */
        public Set<VM> children;
        /**
         * BIOS strings
         */
        public Map<String, String> biosStrings;
        /**
         * Ref pointing to a protection policy for this VM
         */
        public VMPP protectionPolicy;
        /**
         * true if this snapshot was created by the protection policy
         */
        public Boolean isSnapshotFromVmpp;
        /**
         * the appliance to which this VM belongs
         */
        public VMAppliance appliance;
        /**
         * The delay to wait before proceeding to the next order in the startup sequence (seconds)
         */
        public Long startDelay;
        /**
         * The delay to wait before proceeding to the next order in the shutdown sequence (seconds)
         */
        public Long shutdownDelay;
        /**
         * The point in the startup or shutdown sequence at which this VM will be started
         */
        public Long order;
        /**
         * Virtual GPUs
         */
        public Set<VGPU> VGPUs;
        /**
         * Currently passed-through PCI devices
         */
        public Set<PCI> attachedPCIs;
        /**
         * The SR on which a suspend image is stored
         */
        public SR suspendSR;
        /**
         * The number of times this VM has been recovered
         */
        public Long version;
    }

    /**
     * Get a record containing the current state of the given VM.
     *
     * @return all fields from the object
     */
    public VM.Record getRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVMRecord(result);
    }

    /**
     * Get a reference to the VM instance with the specified UUID.
     *
     * @param uuid UUID of object to return
     * @return reference to the object
     */
    public static VM getByUuid(Connection c, String uuid) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_by_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(uuid)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVM(result);
    }

    /**
     * Create a new VM instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return Task
     */
    public static Task createAsync(Connection c, VM.Record record) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a new VM instance, and return its handle.
     *
     * @param record All constructor arguments
     * @return reference to the newly created object
     */
    public static VM create(Connection c, VM.Record record) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.create";
        String session = c.getSessionReference();
        Map<String, Object> record_map = record.toMap();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(record_map)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVM(result);
    }

    /**
     * Destroy the specified VM.  The VM is completely removed from the system.  This function can only be called when the VM is in the Halted State.
     *
     * @return Task
     */
    public Task destroyAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Destroy the specified VM.  The VM is completely removed from the system.  This function can only be called when the VM is in the Halted State.
     *
     */
    public void destroy(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.destroy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Get all the VM instances with the given label.
     *
     * @param label label of object to return
     * @return references to objects with matching names
     */
    public static Set<VM> getByNameLabel(Connection c, String label) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_by_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(label)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVM(result);
    }

    /**
     * Get the uuid field of the given VM.
     *
     * @return value of the field
     */
    public String getUuid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_uuid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the allowed_operations field of the given VM.
     *
     * @return value of the field
     */
    public Set<Types.VmOperations> getAllowedOperations(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_allowed_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVmOperations(result);
    }

    /**
     * Get the current_operations field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, Types.VmOperations> getCurrentOperations(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_current_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringVmOperations(result);
    }

    /**
     * Get the power_state field of the given VM.
     *
     * @return value of the field
     */
    public Types.VmPowerState getPowerState(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_power_state";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVmPowerState(result);
    }

    /**
     * Get the name/label field of the given VM.
     *
     * @return value of the field
     */
    public String getNameLabel(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the name/description field of the given VM.
     *
     * @return value of the field
     */
    public String getNameDescription(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the user_version field of the given VM.
     *
     * @return value of the field
     */
    public Long getUserVersion(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_user_version";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the is_a_template field of the given VM.
     *
     * @return value of the field
     */
    public Boolean getIsATemplate(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_is_a_template";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the suspend_VDI field of the given VM.
     *
     * @return value of the field
     */
    public VDI getSuspendVDI(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_suspend_VDI";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVDI(result);
    }

    /**
     * Get the resident_on field of the given VM.
     *
     * @return value of the field
     */
    public Host getResidentOn(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_resident_on";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toHost(result);
    }

    /**
     * Get the affinity field of the given VM.
     *
     * @return value of the field
     */
    public Host getAffinity(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_affinity";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toHost(result);
    }

    /**
     * Get the memory/overhead field of the given VM.
     *
     * @return value of the field
     */
    public Long getMemoryOverhead(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_memory_overhead";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the memory/target field of the given VM.
     * @deprecated
     *
     * @return value of the field
     */
   @Deprecated public Long getMemoryTarget(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_memory_target";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the memory/static_max field of the given VM.
     *
     * @return value of the field
     */
    public Long getMemoryStaticMax(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_memory_static_max";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the memory/dynamic_max field of the given VM.
     *
     * @return value of the field
     */
    public Long getMemoryDynamicMax(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_memory_dynamic_max";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the memory/dynamic_min field of the given VM.
     *
     * @return value of the field
     */
    public Long getMemoryDynamicMin(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_memory_dynamic_min";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the memory/static_min field of the given VM.
     *
     * @return value of the field
     */
    public Long getMemoryStaticMin(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_memory_static_min";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the VCPUs/params field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getVCPUsParams(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_VCPUs_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the VCPUs/max field of the given VM.
     *
     * @return value of the field
     */
    public Long getVCPUsMax(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_VCPUs_max";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the VCPUs/at_startup field of the given VM.
     *
     * @return value of the field
     */
    public Long getVCPUsAtStartup(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_VCPUs_at_startup";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the actions/after_shutdown field of the given VM.
     *
     * @return value of the field
     */
    public Types.OnNormalExit getActionsAfterShutdown(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_actions_after_shutdown";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toOnNormalExit(result);
    }

    /**
     * Get the actions/after_reboot field of the given VM.
     *
     * @return value of the field
     */
    public Types.OnNormalExit getActionsAfterReboot(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_actions_after_reboot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toOnNormalExit(result);
    }

    /**
     * Get the actions/after_crash field of the given VM.
     *
     * @return value of the field
     */
    public Types.OnCrashBehaviour getActionsAfterCrash(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_actions_after_crash";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toOnCrashBehaviour(result);
    }

    /**
     * Get the consoles field of the given VM.
     *
     * @return value of the field
     */
    public Set<Console> getConsoles(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_consoles";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfConsole(result);
    }

    /**
     * Get the VIFs field of the given VM.
     *
     * @return value of the field
     */
    public Set<VIF> getVIFs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_VIFs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVIF(result);
    }

    /**
     * Get the VBDs field of the given VM.
     *
     * @return value of the field
     */
    public Set<VBD> getVBDs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_VBDs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVBD(result);
    }

    /**
     * Get the crash_dumps field of the given VM.
     *
     * @return value of the field
     */
    public Set<Crashdump> getCrashDumps(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_crash_dumps";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfCrashdump(result);
    }

    /**
     * Get the VTPMs field of the given VM.
     *
     * @return value of the field
     */
    public Set<VTPM> getVTPMs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_VTPMs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVTPM(result);
    }

    /**
     * Get the PV/bootloader field of the given VM.
     *
     * @return value of the field
     */
    public String getPVBootloader(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_PV_bootloader";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the PV/kernel field of the given VM.
     *
     * @return value of the field
     */
    public String getPVKernel(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_PV_kernel";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the PV/ramdisk field of the given VM.
     *
     * @return value of the field
     */
    public String getPVRamdisk(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_PV_ramdisk";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the PV/args field of the given VM.
     *
     * @return value of the field
     */
    public String getPVArgs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_PV_args";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the PV/bootloader_args field of the given VM.
     *
     * @return value of the field
     */
    public String getPVBootloaderArgs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_PV_bootloader_args";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the PV/legacy_args field of the given VM.
     *
     * @return value of the field
     */
    public String getPVLegacyArgs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_PV_legacy_args";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the HVM/boot_policy field of the given VM.
     *
     * @return value of the field
     */
    public String getHVMBootPolicy(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_HVM_boot_policy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the HVM/boot_params field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getHVMBootParams(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_HVM_boot_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the HVM/shadow_multiplier field of the given VM.
     *
     * @return value of the field
     */
    public Double getHVMShadowMultiplier(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_HVM_shadow_multiplier";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toDouble(result);
    }

    /**
     * Get the platform field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getPlatform(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_platform";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the PCI_bus field of the given VM.
     * @deprecated
     *
     * @return value of the field
     */
   @Deprecated public String getPCIBus(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_PCI_bus";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the other_config field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getOtherConfig(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the domid field of the given VM.
     *
     * @return value of the field
     */
    public Long getDomid(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_domid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the domarch field of the given VM.
     *
     * @return value of the field
     */
    public String getDomarch(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_domarch";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the last_boot_CPU_flags field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getLastBootCPUFlags(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_last_boot_CPU_flags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the is_control_domain field of the given VM.
     *
     * @return value of the field
     */
    public Boolean getIsControlDomain(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_is_control_domain";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the metrics field of the given VM.
     *
     * @return value of the field
     */
    public VMMetrics getMetrics(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_metrics";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVMMetrics(result);
    }

    /**
     * Get the guest_metrics field of the given VM.
     *
     * @return value of the field
     */
    public VMGuestMetrics getGuestMetrics(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_guest_metrics";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVMGuestMetrics(result);
    }

    /**
     * Get the last_booted_record field of the given VM.
     *
     * @return value of the field
     */
    public String getLastBootedRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_last_booted_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the recommendations field of the given VM.
     *
     * @return value of the field
     */
    public String getRecommendations(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_recommendations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the xenstore_data field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getXenstoreData(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_xenstore_data";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the ha_always_run field of the given VM.
     * @deprecated
     *
     * @return value of the field
     */
   @Deprecated public Boolean getHaAlwaysRun(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_ha_always_run";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the ha_restart_priority field of the given VM.
     *
     * @return value of the field
     */
    public String getHaRestartPriority(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_ha_restart_priority";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the is_a_snapshot field of the given VM.
     *
     * @return value of the field
     */
    public Boolean getIsASnapshot(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_is_a_snapshot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the snapshot_of field of the given VM.
     *
     * @return value of the field
     */
    public VM getSnapshotOf(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_snapshot_of";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVM(result);
    }

    /**
     * Get the snapshots field of the given VM.
     *
     * @return value of the field
     */
    public Set<VM> getSnapshots(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_snapshots";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVM(result);
    }

    /**
     * Get the snapshot_time field of the given VM.
     *
     * @return value of the field
     */
    public Date getSnapshotTime(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_snapshot_time";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toDate(result);
    }

    /**
     * Get the transportable_snapshot_id field of the given VM.
     *
     * @return value of the field
     */
    public String getTransportableSnapshotId(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_transportable_snapshot_id";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the blobs field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, Blob> getBlobs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_blobs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringBlob(result);
    }

    /**
     * Get the tags field of the given VM.
     *
     * @return value of the field
     */
    public Set<String> getTags(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_tags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Get the blocked_operations field of the given VM.
     *
     * @return value of the field
     */
    public Map<Types.VmOperations, String> getBlockedOperations(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_blocked_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfVmOperationsString(result);
    }

    /**
     * Get the snapshot_info field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getSnapshotInfo(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_snapshot_info";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the snapshot_metadata field of the given VM.
     *
     * @return value of the field
     */
    public String getSnapshotMetadata(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_snapshot_metadata";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toString(result);
    }

    /**
     * Get the parent field of the given VM.
     *
     * @return value of the field
     */
    public VM getParent(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_parent";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVM(result);
    }

    /**
     * Get the children field of the given VM.
     *
     * @return value of the field
     */
    public Set<VM> getChildren(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_children";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVM(result);
    }

    /**
     * Get the bios_strings field of the given VM.
     *
     * @return value of the field
     */
    public Map<String, String> getBiosStrings(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_bios_strings";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Get the protection_policy field of the given VM.
     *
     * @return value of the field
     */
    public VMPP getProtectionPolicy(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_protection_policy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVMPP(result);
    }

    /**
     * Get the is_snapshot_from_vmpp field of the given VM.
     *
     * @return value of the field
     */
    public Boolean getIsSnapshotFromVmpp(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_is_snapshot_from_vmpp";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Get the appliance field of the given VM.
     *
     * @return value of the field
     */
    public VMAppliance getAppliance(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_appliance";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVMAppliance(result);
    }

    /**
     * Get the start_delay field of the given VM.
     *
     * @return value of the field
     */
    public Long getStartDelay(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_start_delay";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the shutdown_delay field of the given VM.
     *
     * @return value of the field
     */
    public Long getShutdownDelay(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_shutdown_delay";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the order field of the given VM.
     *
     * @return value of the field
     */
    public Long getOrder(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_order";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Get the VGPUs field of the given VM.
     *
     * @return value of the field
     */
    public Set<VGPU> getVGPUs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_VGPUs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVGPU(result);
    }

    /**
     * Get the attached_PCIs field of the given VM.
     *
     * @return value of the field
     */
    public Set<PCI> getAttachedPCIs(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_attached_PCIs";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfPCI(result);
    }

    /**
     * Get the suspend_SR field of the given VM.
     *
     * @return value of the field
     */
    public SR getSuspendSR(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_suspend_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSR(result);
    }

    /**
     * Get the version field of the given VM.
     *
     * @return value of the field
     */
    public Long getVersion(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_version";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Set the name/label field of the given VM.
     *
     * @param label New value to set
     */
    public void setNameLabel(Connection c, String label) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_name_label";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(label)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the name/description field of the given VM.
     *
     * @param description New value to set
     */
    public void setNameDescription(Connection c, String description) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_name_description";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(description)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the user_version field of the given VM.
     *
     * @param userVersion New value to set
     */
    public void setUserVersion(Connection c, Long userVersion) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_user_version";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(userVersion)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the is_a_template field of the given VM.
     *
     * @param isATemplate New value to set
     */
    public void setIsATemplate(Connection c, Boolean isATemplate) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_is_a_template";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(isATemplate)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the affinity field of the given VM.
     *
     * @param affinity New value to set
     */
    public void setAffinity(Connection c, Host affinity) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_affinity";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(affinity)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the VCPUs/params field of the given VM.
     *
     * @param params New value to set
     */
    public void setVCPUsParams(Connection c, Map<String, String> params) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_VCPUs_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(params)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the VCPUs/params field of the given VM.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToVCPUsParams(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.add_to_VCPUs_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the VCPUs/params field of the given VM.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromVCPUsParams(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.remove_from_VCPUs_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the actions/after_shutdown field of the given VM.
     *
     * @param afterShutdown New value to set
     */
    public void setActionsAfterShutdown(Connection c, Types.OnNormalExit afterShutdown) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_actions_after_shutdown";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(afterShutdown)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the actions/after_reboot field of the given VM.
     *
     * @param afterReboot New value to set
     */
    public void setActionsAfterReboot(Connection c, Types.OnNormalExit afterReboot) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_actions_after_reboot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(afterReboot)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the actions/after_crash field of the given VM.
     *
     * @param afterCrash New value to set
     */
    public void setActionsAfterCrash(Connection c, Types.OnCrashBehaviour afterCrash) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_actions_after_crash";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(afterCrash)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the PV/bootloader field of the given VM.
     *
     * @param bootloader New value to set
     */
    public void setPVBootloader(Connection c, String bootloader) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_PV_bootloader";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(bootloader)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the PV/kernel field of the given VM.
     *
     * @param kernel New value to set
     */
    public void setPVKernel(Connection c, String kernel) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_PV_kernel";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(kernel)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the PV/ramdisk field of the given VM.
     *
     * @param ramdisk New value to set
     */
    public void setPVRamdisk(Connection c, String ramdisk) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_PV_ramdisk";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(ramdisk)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the PV/args field of the given VM.
     *
     * @param args New value to set
     */
    public void setPVArgs(Connection c, String args) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_PV_args";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(args)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the PV/bootloader_args field of the given VM.
     *
     * @param bootloaderArgs New value to set
     */
    public void setPVBootloaderArgs(Connection c, String bootloaderArgs) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_PV_bootloader_args";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(bootloaderArgs)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the PV/legacy_args field of the given VM.
     *
     * @param legacyArgs New value to set
     */
    public void setPVLegacyArgs(Connection c, String legacyArgs) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_PV_legacy_args";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(legacyArgs)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the HVM/boot_policy field of the given VM.
     *
     * @param bootPolicy New value to set
     */
    public void setHVMBootPolicy(Connection c, String bootPolicy) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_HVM_boot_policy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(bootPolicy)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the HVM/boot_params field of the given VM.
     *
     * @param bootParams New value to set
     */
    public void setHVMBootParams(Connection c, Map<String, String> bootParams) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_HVM_boot_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(bootParams)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the HVM/boot_params field of the given VM.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToHVMBootParams(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.add_to_HVM_boot_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the HVM/boot_params field of the given VM.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromHVMBootParams(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.remove_from_HVM_boot_params";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the platform field of the given VM.
     *
     * @param platform New value to set
     */
    public void setPlatform(Connection c, Map<String, String> platform) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_platform";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(platform)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the platform field of the given VM.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToPlatform(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.add_to_platform";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the platform field of the given VM.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromPlatform(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.remove_from_platform";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the PCI_bus field of the given VM.
     * @deprecated
     *
     * @param PCIBus New value to set
     */
   @Deprecated public void setPCIBus(Connection c, String PCIBus) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_PCI_bus";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(PCIBus)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the other_config field of the given VM.
     *
     * @param otherConfig New value to set
     */
    public void setOtherConfig(Connection c, Map<String, String> otherConfig) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(otherConfig)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the other_config field of the given VM.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToOtherConfig(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.add_to_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the other_config field of the given VM.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromOtherConfig(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.remove_from_other_config";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the recommendations field of the given VM.
     *
     * @param recommendations New value to set
     */
    public void setRecommendations(Connection c, String recommendations) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_recommendations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(recommendations)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the xenstore_data field of the given VM.
     *
     * @param xenstoreData New value to set
     */
    public void setXenstoreData(Connection c, Map<String, String> xenstoreData) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_xenstore_data";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(xenstoreData)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the xenstore_data field of the given VM.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToXenstoreData(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.add_to_xenstore_data";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the xenstore_data field of the given VM.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromXenstoreData(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.remove_from_xenstore_data";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the tags field of the given VM.
     *
     * @param tags New value to set
     */
    public void setTags(Connection c, Set<String> tags) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_tags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(tags)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given value to the tags field of the given VM.  If the value is already in that Set, then do nothing.
     *
     * @param value New value to add
     */
    public void addTags(Connection c, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.add_tags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given value from the tags field of the given VM.  If the value is not in that Set, then do nothing.
     *
     * @param value Value to remove
     */
    public void removeTags(Connection c, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.remove_tags";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the blocked_operations field of the given VM.
     *
     * @param blockedOperations New value to set
     */
    public void setBlockedOperations(Connection c, Map<Types.VmOperations, String> blockedOperations) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_blocked_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(blockedOperations)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to the blocked_operations field of the given VM.
     *
     * @param key Key to add
     * @param value Value to add
     */
    public void addToBlockedOperations(Connection c, Types.VmOperations key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.add_to_blocked_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Remove the given key and its corresponding value from the blocked_operations field of the given VM.  If the key is not in that Map, then do nothing.
     *
     * @param key Key to remove
     */
    public void removeFromBlockedOperations(Connection c, Types.VmOperations key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.remove_from_blocked_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the suspend_SR field of the given VM.
     *
     * @param suspendSR New value to set
     */
    public void setSuspendSR(Connection c, SR suspendSR) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_suspend_SR";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(suspendSR)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Snapshots the specified VM, making a new VM. Snapshot automatically exploits the capabilities of the underlying storage repository in which the VM's disk images are stored (e.g. Copy on Write).
     *
     * @param newName The name of the snapshotted VM
     * @return Task
     */
    public Task snapshotAsync(Connection c, String newName) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed {
        String method_call = "Async.VM.snapshot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(newName)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Snapshots the specified VM, making a new VM. Snapshot automatically exploits the capabilities of the underlying storage repository in which the VM's disk images are stored (e.g. Copy on Write).
     *
     * @param newName The name of the snapshotted VM
     * @return The reference of the newly created VM.
     */
    public VM snapshot(Connection c, String newName) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed {
        String method_call = "VM.snapshot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(newName)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVM(result);
    }

    /**
     * Snapshots the specified VM with quiesce, making a new VM. Snapshot automatically exploits the capabilities of the underlying storage repository in which the VM's disk images are stored (e.g. Copy on Write).
     *
     * @param newName The name of the snapshotted VM
     * @return Task
     */
    public Task snapshotWithQuiesceAsync(Connection c, String newName) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed,
       Types.VmSnapshotWithQuiesceFailed,
       Types.VmSnapshotWithQuiesceTimeout,
       Types.VmSnapshotWithQuiescePluginDeosNotRespond,
       Types.VmSnapshotWithQuiesceNotSupported {
        String method_call = "Async.VM.snapshot_with_quiesce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(newName)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Snapshots the specified VM with quiesce, making a new VM. Snapshot automatically exploits the capabilities of the underlying storage repository in which the VM's disk images are stored (e.g. Copy on Write).
     *
     * @param newName The name of the snapshotted VM
     * @return The reference of the newly created VM.
     */
    public VM snapshotWithQuiesce(Connection c, String newName) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed,
       Types.VmSnapshotWithQuiesceFailed,
       Types.VmSnapshotWithQuiesceTimeout,
       Types.VmSnapshotWithQuiescePluginDeosNotRespond,
       Types.VmSnapshotWithQuiesceNotSupported {
        String method_call = "VM.snapshot_with_quiesce";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(newName)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVM(result);
    }

    /**
     * Clones the specified VM, making a new VM. Clone automatically exploits the capabilities of the underlying storage repository in which the VM's disk images are stored (e.g. Copy on Write).   This function can only be called when the VM is in the Halted State.
     *
     * @param newName The name of the cloned VM
     * @return Task
     */
    public Task createCloneAsync(Connection c, String newName) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed {
        String method_call = "Async.VM.clone";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(newName)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Clones the specified VM, making a new VM. Clone automatically exploits the capabilities of the underlying storage repository in which the VM's disk images are stored (e.g. Copy on Write).   This function can only be called when the VM is in the Halted State.
     *
     * @param newName The name of the cloned VM
     * @return The reference of the newly created VM.
     */
    public VM createClone(Connection c, String newName) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed {
        String method_call = "VM.clone";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(newName)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVM(result);
    }

    /**
     * Copied the specified VM, making a new VM. Unlike clone, copy does not exploits the capabilities of the underlying storage repository in which the VM's disk images are stored. Instead, copy guarantees that the disk images of the newly created VM will be 'full disks' - i.e. not part of a CoW chain.  This function can only be called when the VM is in the Halted State.
     *
     * @param newName The name of the copied VM
     * @param sr An SR to copy all the VM's disks into (if an invalid reference then it uses the existing SRs)
     * @return Task
     */
    public Task copyAsync(Connection c, String newName, SR sr) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed {
        String method_call = "Async.VM.copy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(newName), Marshalling.toXMLRPC(sr)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Copied the specified VM, making a new VM. Unlike clone, copy does not exploits the capabilities of the underlying storage repository in which the VM's disk images are stored. Instead, copy guarantees that the disk images of the newly created VM will be 'full disks' - i.e. not part of a CoW chain.  This function can only be called when the VM is in the Halted State.
     *
     * @param newName The name of the copied VM
     * @param sr An SR to copy all the VM's disks into (if an invalid reference then it uses the existing SRs)
     * @return The reference of the newly created VM.
     */
    public VM copy(Connection c, String newName, SR sr) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed {
        String method_call = "VM.copy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(newName), Marshalling.toXMLRPC(sr)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVM(result);
    }

    /**
     * Reverts the specified VM to a previous state.
     *
     * @return Task
     */
    public Task revertAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OperationNotAllowed,
       Types.SrFull,
       Types.VmRevertFailed {
        String method_call = "Async.VM.revert";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Reverts the specified VM to a previous state.
     *
     */
    public void revert(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OperationNotAllowed,
       Types.SrFull,
       Types.VmRevertFailed {
        String method_call = "VM.revert";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Checkpoints the specified VM, making a new VM. Checkpoint automatically exploits the capabilities of the underlying storage repository in which the VM's disk images are stored (e.g. Copy on Write) and saves the memory image as well.
     *
     * @param newName The name of the checkpointed VM
     * @return Task
     */
    public Task checkpointAsync(Connection c, String newName) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed,
       Types.VmCheckpointSuspendFailed,
       Types.VmCheckpointResumeFailed {
        String method_call = "Async.VM.checkpoint";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(newName)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Checkpoints the specified VM, making a new VM. Checkpoint automatically exploits the capabilities of the underlying storage repository in which the VM's disk images are stored (e.g. Copy on Write) and saves the memory image as well.
     *
     * @param newName The name of the checkpointed VM
     * @return The reference of the newly created VM.
     */
    public VM checkpoint(Connection c, String newName) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed,
       Types.VmCheckpointSuspendFailed,
       Types.VmCheckpointResumeFailed {
        String method_call = "VM.checkpoint";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(newName)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVM(result);
    }

    /**
     * Inspects the disk configuration contained within the VM's other_config, creates VDIs and VBDs and then executes any applicable post-install script.
     *
     * @return Task
     */
    public Task provisionAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed {
        String method_call = "Async.VM.provision";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Inspects the disk configuration contained within the VM's other_config, creates VDIs and VBDs and then executes any applicable post-install script.
     *
     */
    public void provision(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.SrFull,
       Types.OperationNotAllowed {
        String method_call = "VM.provision";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Start the specified VM.  This function can only be called with the VM is in the Halted State.
     *
     * @param startPaused Instantiate VM in paused state if set to true.
     * @param force Attempt to force the VM to start. If this flag is false then the VM may fail pre-boot safety checks (e.g. if the CPU the VM last booted on looks substantially different to the current one)
     * @return Task
     */
    public Task startAsync(Connection c, Boolean startPaused, Boolean force) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.VmHvmRequired,
       Types.VmIsTemplate,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.BootloaderFailed,
       Types.UnknownBootloader,
       Types.NoHostsAvailable,
       Types.LicenceRestriction {
        String method_call = "Async.VM.start";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(startPaused), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Start the specified VM.  This function can only be called with the VM is in the Halted State.
     *
     * @param startPaused Instantiate VM in paused state if set to true.
     * @param force Attempt to force the VM to start. If this flag is false then the VM may fail pre-boot safety checks (e.g. if the CPU the VM last booted on looks substantially different to the current one)
     */
    public void start(Connection c, Boolean startPaused, Boolean force) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.VmHvmRequired,
       Types.VmIsTemplate,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.BootloaderFailed,
       Types.UnknownBootloader,
       Types.NoHostsAvailable,
       Types.LicenceRestriction {
        String method_call = "VM.start";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(startPaused), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Start the specified VM on a particular host.  This function can only be called with the VM is in the Halted State.
     *
     * @param host The Host on which to start the VM
     * @param startPaused Instantiate VM in paused state if set to true.
     * @param force Attempt to force the VM to start. If this flag is false then the VM may fail pre-boot safety checks (e.g. if the CPU the VM last booted on looks substantially different to the current one)
     * @return Task
     */
    public Task startOnAsync(Connection c, Host host, Boolean startPaused, Boolean force) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.VmIsTemplate,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.BootloaderFailed,
       Types.UnknownBootloader {
        String method_call = "Async.VM.start_on";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(startPaused), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Start the specified VM on a particular host.  This function can only be called with the VM is in the Halted State.
     *
     * @param host The Host on which to start the VM
     * @param startPaused Instantiate VM in paused state if set to true.
     * @param force Attempt to force the VM to start. If this flag is false then the VM may fail pre-boot safety checks (e.g. if the CPU the VM last booted on looks substantially different to the current one)
     */
    public void startOn(Connection c, Host host, Boolean startPaused, Boolean force) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.VmIsTemplate,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.BootloaderFailed,
       Types.UnknownBootloader {
        String method_call = "VM.start_on";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(startPaused), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Pause the specified VM. This can only be called when the specified VM is in the Running state.
     *
     * @return Task
     */
    public Task pauseAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "Async.VM.pause";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Pause the specified VM. This can only be called when the specified VM is in the Running state.
     *
     */
    public void pause(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.pause";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Resume the specified VM. This can only be called when the specified VM is in the Paused state.
     *
     * @return Task
     */
    public Task unpauseAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "Async.VM.unpause";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Resume the specified VM. This can only be called when the specified VM is in the Paused state.
     *
     */
    public void unpause(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.unpause";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Attempt to cleanly shutdown the specified VM. (Note: this may not be supported---e.g. if a guest agent is not installed). This can only be called when the specified VM is in the Running state.
     *
     * @return Task
     */
    public Task cleanShutdownAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "Async.VM.clean_shutdown";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Attempt to cleanly shutdown the specified VM. (Note: this may not be supported---e.g. if a guest agent is not installed). This can only be called when the specified VM is in the Running state.
     *
     */
    public void cleanShutdown(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.clean_shutdown";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Attempt to cleanly shutdown the specified VM (Note: this may not be supported---e.g. if a guest agent is not installed). This can only be called when the specified VM is in the Running state.
     *
     * @return Task
     */
    public Task cleanRebootAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "Async.VM.clean_reboot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Attempt to cleanly shutdown the specified VM (Note: this may not be supported---e.g. if a guest agent is not installed). This can only be called when the specified VM is in the Running state.
     *
     */
    public void cleanReboot(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.clean_reboot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Stop executing the specified VM without attempting a clean shutdown.
     *
     * @return Task
     */
    public Task hardShutdownAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "Async.VM.hard_shutdown";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Stop executing the specified VM without attempting a clean shutdown.
     *
     */
    public void hardShutdown(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.hard_shutdown";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Reset the power-state of the VM to halted in the database only. (Used to recover from slave failures in pooling scenarios by resetting the power-states of VMs running on dead slaves to halted.) This is a potentially dangerous operation; use with care.
     *
     * @return Task
     */
    public Task powerStateResetAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.power_state_reset";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Reset the power-state of the VM to halted in the database only. (Used to recover from slave failures in pooling scenarios by resetting the power-states of VMs running on dead slaves to halted.) This is a potentially dangerous operation; use with care.
     *
     */
    public void powerStateReset(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.power_state_reset";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Stop executing the specified VM without attempting a clean shutdown and immediately restart the VM.
     *
     * @return Task
     */
    public Task hardRebootAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "Async.VM.hard_reboot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Stop executing the specified VM without attempting a clean shutdown and immediately restart the VM.
     *
     */
    public void hardReboot(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.hard_reboot";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Suspend the specified VM to disk.  This can only be called when the specified VM is in the Running state.
     *
     * @return Task
     */
    public Task suspendAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "Async.VM.suspend";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Suspend the specified VM to disk.  This can only be called when the specified VM is in the Running state.
     *
     */
    public void suspend(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.suspend";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Awaken the specified VM and resume it.  This can only be called when the specified VM is in the Suspended state.
     *
     * @param startPaused Resume VM in paused state if set to true.
     * @param force Attempt to force the VM to resume. If this flag is false then the VM may fail pre-resume safety checks (e.g. if the CPU the VM was running on looks substantially different to the current one)
     * @return Task
     */
    public Task resumeAsync(Connection c, Boolean startPaused, Boolean force) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "Async.VM.resume";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(startPaused), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Awaken the specified VM and resume it.  This can only be called when the specified VM is in the Suspended state.
     *
     * @param startPaused Resume VM in paused state if set to true.
     * @param force Attempt to force the VM to resume. If this flag is false then the VM may fail pre-resume safety checks (e.g. if the CPU the VM was running on looks substantially different to the current one)
     */
    public void resume(Connection c, Boolean startPaused, Boolean force) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.resume";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(startPaused), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Awaken the specified VM and resume it on a particular Host.  This can only be called when the specified VM is in the Suspended state.
     *
     * @param host The Host on which to resume the VM
     * @param startPaused Resume VM in paused state if set to true.
     * @param force Attempt to force the VM to resume. If this flag is false then the VM may fail pre-resume safety checks (e.g. if the CPU the VM was running on looks substantially different to the current one)
     * @return Task
     */
    public Task resumeOnAsync(Connection c, Host host, Boolean startPaused, Boolean force) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "Async.VM.resume_on";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(startPaused), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Awaken the specified VM and resume it on a particular Host.  This can only be called when the specified VM is in the Suspended state.
     *
     * @param host The Host on which to resume the VM
     * @param startPaused Resume VM in paused state if set to true.
     * @param force Attempt to force the VM to resume. If this flag is false then the VM may fail pre-resume safety checks (e.g. if the CPU the VM was running on looks substantially different to the current one)
     */
    public void resumeOn(Connection c, Host host, Boolean startPaused, Boolean force) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OperationNotAllowed,
       Types.VmIsTemplate {
        String method_call = "VM.resume_on";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(startPaused), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Migrate a VM to another Host. This can only be called when the specified VM is in the Running state.
     *
     * @param host The target host
     * @param options Extra configuration operations
     * @return Task
     */
    public Task poolMigrateAsync(Connection c, Host host, Map<String, String> options) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.VmIsTemplate,
       Types.OperationNotAllowed,
       Types.VmMigrateFailed,
       Types.VmMissingPvDrivers {
        String method_call = "Async.VM.pool_migrate";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(options)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Migrate a VM to another Host. This can only be called when the specified VM is in the Running state.
     *
     * @param host The target host
     * @param options Extra configuration operations
     */
    public void poolMigrate(Connection c, Host host, Map<String, String> options) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState,
       Types.OtherOperationInProgress,
       Types.VmIsTemplate,
       Types.OperationNotAllowed,
       Types.VmMigrateFailed,
       Types.VmMissingPvDrivers {
        String method_call = "VM.pool_migrate";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host), Marshalling.toXMLRPC(options)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the number of VCPUs for a running VM
     *
     * @param nvcpu The number of VCPUs
     * @return Task
     */
    public Task setVCPUsNumberLiveAsync(Connection c, Long nvcpu) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.set_VCPUs_number_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(nvcpu)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set the number of VCPUs for a running VM
     *
     * @param nvcpu The number of VCPUs
     */
    public void setVCPUsNumberLive(Connection c, Long nvcpu) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_VCPUs_number_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(nvcpu)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Add the given key-value pair to VM.VCPUs_params, and apply that value on the running VM
     *
     * @param key The key
     * @param value The value
     * @return Task
     */
    public Task addToVCPUsParamsLiveAsync(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.add_to_VCPUs_params_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Add the given key-value pair to VM.VCPUs_params, and apply that value on the running VM
     *
     * @param key The key
     * @param value The value
     */
    public void addToVCPUsParamsLive(Connection c, String key, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.add_to_VCPUs_params_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the value of the ha_restart_priority field
     *
     * @param value The value
     */
    public void setHaRestartPriority(Connection c, String value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_ha_restart_priority";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the value of the ha_always_run
     * @deprecated
     *
     * @param value The value
     */
   @Deprecated public void setHaAlwaysRun(Connection c, Boolean value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_ha_always_run";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Computes the virtualization memory overhead of a VM.
     *
     * @return Task
     */
    public Task computeMemoryOverheadAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.compute_memory_overhead";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Computes the virtualization memory overhead of a VM.
     *
     * @return the virtualization memory overhead of the VM.
     */
    public Long computeMemoryOverhead(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.compute_memory_overhead";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Set the value of the memory_dynamic_max field
     *
     * @param value The new value of memory_dynamic_max
     */
    public void setMemoryDynamicMax(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_memory_dynamic_max";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the value of the memory_dynamic_min field
     *
     * @param value The new value of memory_dynamic_min
     */
    public void setMemoryDynamicMin(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_memory_dynamic_min";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the minimum and maximum amounts of physical memory the VM is allowed to use.
     *
     * @param min The new minimum value
     * @param max The new maximum value
     * @return Task
     */
    public Task setMemoryDynamicRangeAsync(Connection c, Long min, Long max) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.set_memory_dynamic_range";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(min), Marshalling.toXMLRPC(max)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set the minimum and maximum amounts of physical memory the VM is allowed to use.
     *
     * @param min The new minimum value
     * @param max The new maximum value
     */
    public void setMemoryDynamicRange(Connection c, Long min, Long max) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_memory_dynamic_range";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(min), Marshalling.toXMLRPC(max)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the value of the memory_static_max field
     *
     * @param value The new value of memory_static_max
     */
    public void setMemoryStaticMax(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.HaOperationWouldBreakFailoverPlan {
        String method_call = "VM.set_memory_static_max";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the value of the memory_static_min field
     *
     * @param value The new value of memory_static_min
     */
    public void setMemoryStaticMin(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_memory_static_min";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the static (ie boot-time) range of virtual memory that the VM is allowed to use.
     *
     * @param min The new minimum value
     * @param max The new maximum value
     * @return Task
     */
    public Task setMemoryStaticRangeAsync(Connection c, Long min, Long max) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.set_memory_static_range";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(min), Marshalling.toXMLRPC(max)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set the static (ie boot-time) range of virtual memory that the VM is allowed to use.
     *
     * @param min The new minimum value
     * @param max The new maximum value
     */
    public void setMemoryStaticRange(Connection c, Long min, Long max) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_memory_static_range";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(min), Marshalling.toXMLRPC(max)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the memory limits of this VM.
     *
     * @param staticMin The new value of memory_static_min.
     * @param staticMax The new value of memory_static_max.
     * @param dynamicMin The new value of memory_dynamic_min.
     * @param dynamicMax The new value of memory_dynamic_max.
     * @return Task
     */
    public Task setMemoryLimitsAsync(Connection c, Long staticMin, Long staticMax, Long dynamicMin, Long dynamicMax) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.set_memory_limits";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(staticMin), Marshalling.toXMLRPC(staticMax), Marshalling.toXMLRPC(dynamicMin), Marshalling.toXMLRPC(dynamicMax)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set the memory limits of this VM.
     *
     * @param staticMin The new value of memory_static_min.
     * @param staticMax The new value of memory_static_max.
     * @param dynamicMin The new value of memory_dynamic_min.
     * @param dynamicMax The new value of memory_dynamic_max.
     */
    public void setMemoryLimits(Connection c, Long staticMin, Long staticMax, Long dynamicMin, Long dynamicMax) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_memory_limits";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(staticMin), Marshalling.toXMLRPC(staticMax), Marshalling.toXMLRPC(dynamicMin), Marshalling.toXMLRPC(dynamicMax)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the memory target for a running VM
     * @deprecated
     *
     * @param target The target in bytes
     * @return Task
     */
   @Deprecated public Task setMemoryTargetLiveAsync(Connection c, Long target) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.set_memory_target_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(target)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set the memory target for a running VM
     * @deprecated
     *
     * @param target The target in bytes
     */
   @Deprecated public void setMemoryTargetLive(Connection c, Long target) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_memory_target_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(target)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Wait for a running VM to reach its current memory target
     * @deprecated
     *
     * @return Task
     */
   @Deprecated public Task waitMemoryTargetLiveAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.wait_memory_target_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Wait for a running VM to reach its current memory target
     * @deprecated
     *
     */
   @Deprecated public void waitMemoryTargetLive(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.wait_memory_target_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Return true if the VM is currently 'co-operative' i.e. is expected to reach a balloon target and actually has done
     * @deprecated
     *
     * @return Task
     */
   @Deprecated public Task getCooperativeAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.get_cooperative";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Return true if the VM is currently 'co-operative' i.e. is expected to reach a balloon target and actually has done
     * @deprecated
     *
     * @return true if the VM is currently 'co-operative'; false otherwise
     */
   @Deprecated public Boolean getCooperative(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_cooperative";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBoolean(result);
    }

    /**
     * Set the shadow memory multiplier on a halted VM
     *
     * @param value The new shadow memory multiplier to set
     */
    public void setHVMShadowMultiplier(Connection c, Double value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_HVM_shadow_multiplier";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the shadow memory multiplier on a running VM
     *
     * @param multiplier The new shadow memory multiplier to set
     * @return Task
     */
    public Task setShadowMultiplierLiveAsync(Connection c, Double multiplier) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.set_shadow_multiplier_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(multiplier)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set the shadow memory multiplier on a running VM
     *
     * @param multiplier The new shadow memory multiplier to set
     */
    public void setShadowMultiplierLive(Connection c, Double multiplier) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_shadow_multiplier_live";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(multiplier)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the maximum number of VCPUs for a halted VM
     *
     * @param value The new maximum number of VCPUs
     */
    public void setVCPUsMax(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_VCPUs_max";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the number of startup VCPUs for a halted VM
     *
     * @param value The new maximum number of VCPUs
     */
    public void setVCPUsAtStartup(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_VCPUs_at_startup";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Send the given key as a sysrq to this VM.  The key is specified as a single character (a String of length 1).  This can only be called when the specified VM is in the Running state.
     *
     * @param key The key to send
     * @return Task
     */
    public Task sendSysrqAsync(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState {
        String method_call = "Async.VM.send_sysrq";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Send the given key as a sysrq to this VM.  The key is specified as a single character (a String of length 1).  This can only be called when the specified VM is in the Running state.
     *
     * @param key The key to send
     */
    public void sendSysrq(Connection c, String key) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState {
        String method_call = "VM.send_sysrq";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(key)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Send the named trigger to this VM.  This can only be called when the specified VM is in the Running state.
     *
     * @param trigger The trigger to send
     * @return Task
     */
    public Task sendTriggerAsync(Connection c, String trigger) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState {
        String method_call = "Async.VM.send_trigger";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(trigger)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Send the named trigger to this VM.  This can only be called when the specified VM is in the Running state.
     *
     * @param trigger The trigger to send
     */
    public void sendTrigger(Connection c, String trigger) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState {
        String method_call = "VM.send_trigger";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(trigger)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Returns the maximum amount of guest memory which will fit, together with overheads, in the supplied amount of physical memory. If 'exact' is true then an exact calculation is performed using the VM's current settings. If 'exact' is false then a more conservative approximation is used
     *
     * @param total Total amount of physical RAM to fit within
     * @param approximate If false the limit is calculated with the guest's current exact configuration. Otherwise a more approximate calculation is performed
     * @return Task
     */
    public Task maximiseMemoryAsync(Connection c, Long total, Boolean approximate) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.maximise_memory";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(total), Marshalling.toXMLRPC(approximate)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Returns the maximum amount of guest memory which will fit, together with overheads, in the supplied amount of physical memory. If 'exact' is true then an exact calculation is performed using the VM's current settings. If 'exact' is false then a more conservative approximation is used
     *
     * @param total Total amount of physical RAM to fit within
     * @param approximate If false the limit is calculated with the guest's current exact configuration. Otherwise a more approximate calculation is performed
     * @return The maximum possible static-max
     */
    public Long maximiseMemory(Connection c, Long total, Boolean approximate) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.maximise_memory";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(total), Marshalling.toXMLRPC(approximate)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toLong(result);
    }

    /**
     * Migrate the VM to another host.  This can only be called when the specified VM is in the Running state.
     *
     * @param dest The result of a Host.migrate_receive call.
     * @param live Live migration
     * @param vdiMap Map of source VDI to destination SR
     * @param vifMap Map of source VIF to destination network
     * @param options Other parameters
     * @return Task
     */
    public Task migrateSendAsync(Connection c, Map<String, String> dest, Boolean live, Map<VDI, SR> vdiMap, Map<VIF, Network> vifMap, Map<String, String> options) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState {
        String method_call = "Async.VM.migrate_send";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(dest), Marshalling.toXMLRPC(live), Marshalling.toXMLRPC(vdiMap), Marshalling.toXMLRPC(vifMap), Marshalling.toXMLRPC(options)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Migrate the VM to another host.  This can only be called when the specified VM is in the Running state.
     *
     * @param dest The result of a Host.migrate_receive call.
     * @param live Live migration
     * @param vdiMap Map of source VDI to destination SR
     * @param vifMap Map of source VIF to destination network
     * @param options Other parameters
     */
    public void migrateSend(Connection c, Map<String, String> dest, Boolean live, Map<VDI, SR> vdiMap, Map<VIF, Network> vifMap, Map<String, String> options) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmBadPowerState {
        String method_call = "VM.migrate_send";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(dest), Marshalling.toXMLRPC(live), Marshalling.toXMLRPC(vdiMap), Marshalling.toXMLRPC(vifMap), Marshalling.toXMLRPC(options)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Assert whether a VM can be migrated to the specified destination.
     *
     * @param dest The result of a VM.migrate_receive call.
     * @param live Live migration
     * @param vdiMap Map of source VDI to destination SR
     * @param vifMap Map of source VIF to destination network
     * @param options Other parameters
     * @return Task
     */
    public Task assertCanMigrateAsync(Connection c, Map<String, String> dest, Boolean live, Map<VDI, SR> vdiMap, Map<VIF, Network> vifMap, Map<String, String> options) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.assert_can_migrate";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(dest), Marshalling.toXMLRPC(live), Marshalling.toXMLRPC(vdiMap), Marshalling.toXMLRPC(vifMap), Marshalling.toXMLRPC(options)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Assert whether a VM can be migrated to the specified destination.
     *
     * @param dest The result of a VM.migrate_receive call.
     * @param live Live migration
     * @param vdiMap Map of source VDI to destination SR
     * @param vifMap Map of source VIF to destination network
     * @param options Other parameters
     */
    public void assertCanMigrate(Connection c, Map<String, String> dest, Boolean live, Map<VDI, SR> vdiMap, Map<VIF, Network> vifMap, Map<String, String> options) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.assert_can_migrate";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(dest), Marshalling.toXMLRPC(live), Marshalling.toXMLRPC(vdiMap), Marshalling.toXMLRPC(vifMap), Marshalling.toXMLRPC(options)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Returns a record describing the VM's dynamic state, initialised when the VM boots and updated to reflect runtime configuration changes e.g. CPU hotplug
     *
     * @return A record describing the VM
     */
    public VM.Record getBootRecord(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_boot_record";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toVMRecord(result);
    }

    /**
     *
     *
     * @return A set of data sources
     */
    public Set<DataSource.Record> getDataSources(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_data_sources";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfDataSourceRecord(result);
    }

    /**
     * Start recording the specified data source
     *
     * @param dataSource The data source to record
     */
    public void recordDataSource(Connection c, String dataSource) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.record_data_source";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(dataSource)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Query the latest value of the specified data source
     *
     * @param dataSource The data source to query
     * @return The latest value, averaged over the last 5 seconds
     */
    public Double queryDataSource(Connection c, String dataSource) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.query_data_source";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(dataSource)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toDouble(result);
    }

    /**
     * Forget the recorded statistics related to the specified data source
     *
     * @param dataSource The data source whose archives are to be forgotten
     */
    public void forgetDataSourceArchives(Connection c, String dataSource) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.forget_data_source_archives";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(dataSource)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Check to see whether this operation is acceptable in the current state of the system, raising an error if the operation is invalid for some reason
     *
     * @param op proposed operation
     * @return Task
     */
    public Task assertOperationValidAsync(Connection c, Types.VmOperations op) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.assert_operation_valid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(op)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Check to see whether this operation is acceptable in the current state of the system, raising an error if the operation is invalid for some reason
     *
     * @param op proposed operation
     */
    public void assertOperationValid(Connection c, Types.VmOperations op) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.assert_operation_valid";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(op)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Recomputes the list of acceptable operations
     *
     * @return Task
     */
    public Task updateAllowedOperationsAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.update_allowed_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Recomputes the list of acceptable operations
     *
     */
    public void updateAllowedOperations(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.update_allowed_operations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Returns a list of the allowed values that a VBD device field can take
     *
     * @return The allowed values
     */
    public Set<String> getAllowedVBDDevices(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_allowed_VBD_devices";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Returns a list of the allowed values that a VIF device field can take
     *
     * @return The allowed values
     */
    public Set<String> getAllowedVIFDevices(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_allowed_VIF_devices";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfString(result);
    }

    /**
     * Return the list of hosts on which this VM may run.
     *
     * @return Task
     */
    public Task getPossibleHostsAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.get_possible_hosts";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Return the list of hosts on which this VM may run.
     *
     * @return The possible hosts
     */
    public Set<Host> getPossibleHosts(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_possible_hosts";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfHost(result);
    }

    /**
     * Returns an error if the VM could not boot on this host for some reason
     *
     * @param host The host
     * @return Task
     */
    public Task assertCanBootHereAsync(Connection c, Host host) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.HostNotEnoughFreeMemory,
       Types.VmRequiresSr,
       Types.VmHostIncompatibleVersion {
        String method_call = "Async.VM.assert_can_boot_here";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Returns an error if the VM could not boot on this host for some reason
     *
     * @param host The host
     */
    public void assertCanBootHere(Connection c, Host host) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.HostNotEnoughFreeMemory,
       Types.VmRequiresSr,
       Types.VmHostIncompatibleVersion {
        String method_call = "VM.assert_can_boot_here";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Create a placeholder for a named binary blob of data that is associated with this VM
     *
     * @param name The name associated with the blob
     * @param mimeType The mime type for the data. Empty string translates to application/octet-stream
     * @param _public True if the blob should be publicly available
     * @return Task
     */
    public Task createNewBlobAsync(Connection c, String name, String mimeType, Boolean _public) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.create_new_blob";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(name), Marshalling.toXMLRPC(mimeType), Marshalling.toXMLRPC(_public)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Create a placeholder for a named binary blob of data that is associated with this VM
     *
     * @param name The name associated with the blob
     * @param mimeType The mime type for the data. Empty string translates to application/octet-stream
     * @param _public True if the blob should be publicly available
     * @return The reference of the blob, needed for populating its data
     */
    public Blob createNewBlob(Connection c, String name, String mimeType, Boolean _public) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.create_new_blob";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(name), Marshalling.toXMLRPC(mimeType), Marshalling.toXMLRPC(_public)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toBlob(result);
    }

    /**
     * Returns an error if the VM is not considered agile e.g. because it is tied to a resource local to a host
     *
     * @return Task
     */
    public Task assertAgileAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.assert_agile";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Returns an error if the VM is not considered agile e.g. because it is tied to a resource local to a host
     *
     */
    public void assertAgile(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.assert_agile";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Returns mapping of hosts to ratings, indicating the suitability of starting the VM at that location according to wlb. Rating is replaced with an error if the VM cannot boot there.
     *
     * @return Task
     */
    public Task retrieveWlbRecommendationsAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.retrieve_wlb_recommendations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Returns mapping of hosts to ratings, indicating the suitability of starting the VM at that location according to wlb. Rating is replaced with an error if the VM cannot boot there.
     *
     * @return The potential hosts and their corresponding recommendations or errors
     */
    public Map<Host, Set<String>> retrieveWlbRecommendations(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.retrieve_wlb_recommendations";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfHostSetOfString(result);
    }

    /**
     * Copy the BIOS strings from the given host to this VM
     *
     * @param host The host to copy the BIOS strings from
     * @return Task
     */
    public Task copyBiosStringsAsync(Connection c, Host host) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.copy_bios_strings";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Copy the BIOS strings from the given host to this VM
     *
     * @param host The host to copy the BIOS strings from
     */
    public void copyBiosStrings(Connection c, Host host) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.copy_bios_strings";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(host)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set the value of the protection_policy field
     *
     * @param value The value
     */
    public void setProtectionPolicy(Connection c, VMPP value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_protection_policy";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set this VM's start delay in seconds
     *
     * @param value This VM's start delay in seconds
     * @return Task
     */
    public Task setStartDelayAsync(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.set_start_delay";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set this VM's start delay in seconds
     *
     * @param value This VM's start delay in seconds
     */
    public void setStartDelay(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_start_delay";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set this VM's shutdown delay in seconds
     *
     * @param value This VM's shutdown delay in seconds
     * @return Task
     */
    public Task setShutdownDelayAsync(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.set_shutdown_delay";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set this VM's shutdown delay in seconds
     *
     * @param value This VM's shutdown delay in seconds
     */
    public void setShutdownDelay(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_shutdown_delay";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set this VM's boot order
     *
     * @param value This VM's boot order
     * @return Task
     */
    public Task setOrderAsync(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.set_order";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set this VM's boot order
     *
     * @param value This VM's boot order
     */
    public void setOrder(Connection c, Long value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_order";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Set this VM's suspend VDI, which must be indentical to its current one
     *
     * @param value The suspend VDI uuid
     * @return Task
     */
    public Task setSuspendVDIAsync(Connection c, VDI value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.set_suspend_VDI";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Set this VM's suspend VDI, which must be indentical to its current one
     *
     * @param value The suspend VDI uuid
     */
    public void setSuspendVDI(Connection c, VDI value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_suspend_VDI";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Assert whether all SRs required to recover this VM are available.
     *
     * @param sessionTo The session to which the VM is to be recovered.
     * @return Task
     */
    public Task assertCanBeRecoveredAsync(Connection c, Session sessionTo) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmIsPartOfAnAppliance,
       Types.VmRequiresSr {
        String method_call = "Async.VM.assert_can_be_recovered";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(sessionTo)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Assert whether all SRs required to recover this VM are available.
     *
     * @param sessionTo The session to which the VM is to be recovered.
     */
    public void assertCanBeRecovered(Connection c, Session sessionTo) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException,
       Types.VmIsPartOfAnAppliance,
       Types.VmRequiresSr {
        String method_call = "VM.assert_can_be_recovered";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(sessionTo)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Recover the VM
     *
     * @param sessionTo The session to which the VM is to be recovered.
     * @param force Whether the VM should replace newer versions of itself.
     * @return Task
     */
    public Task recoverAsync(Connection c, Session sessionTo, Boolean force) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.recover";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(sessionTo), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Recover the VM
     *
     * @param sessionTo The session to which the VM is to be recovered.
     * @param force Whether the VM should replace newer versions of itself.
     */
    public void recover(Connection c, Session sessionTo, Boolean force) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.recover";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(sessionTo), Marshalling.toXMLRPC(force)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Import using a conversion service.
     *
     * @param type Type of the conversion
     * @param username Admin username on the host
     * @param password Password on the host
     * @param sr The destination SR
     * @param remoteConfig Remote configuration options
     * @return Task
     */
    public static Task importConvertAsync(Connection c, String type, String username, String password, SR sr, Map<String, String> remoteConfig) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.import_convert";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(username), Marshalling.toXMLRPC(password), Marshalling.toXMLRPC(sr), Marshalling.toXMLRPC(remoteConfig)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Import using a conversion service.
     *
     * @param type Type of the conversion
     * @param username Admin username on the host
     * @param password Password on the host
     * @param sr The destination SR
     * @param remoteConfig Remote configuration options
     */
    public static void importConvert(Connection c, String type, String username, String password, SR sr, Map<String, String> remoteConfig) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.import_convert";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(type), Marshalling.toXMLRPC(username), Marshalling.toXMLRPC(password), Marshalling.toXMLRPC(sr), Marshalling.toXMLRPC(remoteConfig)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Assign this VM to an appliance.
     *
     * @param value The appliance to which this VM should be assigned.
     * @return Task
     */
    public Task setApplianceAsync(Connection c, VMAppliance value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.set_appliance";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Assign this VM to an appliance.
     *
     * @param value The appliance to which this VM should be assigned.
     */
    public void setAppliance(Connection c, VMAppliance value) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.set_appliance";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref), Marshalling.toXMLRPC(value)};
        Map response = c.dispatch(method_call, method_params);
        return;
    }

    /**
     * Query the system services advertised by this VM and register them. This can only be applied to a system domain.
     *
     * @return Task
     */
    public Task queryServicesAsync(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "Async.VM.query_services";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
        return Types.toTask(result);
    }

    /**
     * Query the system services advertised by this VM and register them. This can only be applied to a system domain.
     *
     * @return map of service type to name
     */
    public Map<String, String> queryServices(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.query_services";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session), Marshalling.toXMLRPC(this.ref)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfStringString(result);
    }

    /**
     * Return a list of all the VMs known to the system.
     *
     * @return references to all objects
     */
    public static Set<VM> getAll(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_all";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toSetOfVM(result);
    }

    /**
     * Return a map of VM references to VM records for all VMs known to the system.
     *
     * @return records of all objects
     */
    public static Map<VM, VM.Record> getAllRecords(Connection c) throws
       BadServerResponse,
       XenAPIException,
       XmlRpcException {
        String method_call = "VM.get_all_records";
        String session = c.getSessionReference();
        Object[] method_params = {Marshalling.toXMLRPC(session)};
        Map response = c.dispatch(method_call, method_params);
        Object result = response.get("Value");
            return Types.toMapOfVMVMRecord(result);
    }

}