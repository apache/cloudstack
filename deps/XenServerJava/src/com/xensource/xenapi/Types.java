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

import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.LinkedHashSet;
import java.io.IOException;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.xmlrpc.XmlRpcException;

/**
 * This class holds vital marshalling functions, enum types and exceptions.
 *
 * @author Citrix Systems, Inc.
 */
public class Types
{
    /**
     * Interface for all Record classes
     */
    public static interface Record
    {
        /**
         * Convert a Record to a Map
         */
        Map<String, Object> toMap();
    }

    /**
     * Helper method.
     */
    private static String[] ObjectArrayToStringArray(Object[] objArray)
    {
        String[] result = new String[objArray.length];
        for (int i = 0; i < objArray.length; i++)
        {
            result[i] = (String) objArray[i];
        }
        return result;
    }

    /**
     * Base class for all XenAPI Exceptions
     */
    public static class XenAPIException extends IOException {
        public final String shortDescription;
        public final String[] errorDescription;

        XenAPIException(String shortDescription)
        {
            this.shortDescription = shortDescription;
            this.errorDescription = null;
        }

        XenAPIException(String[] errorDescription)
        {
            this.errorDescription = errorDescription;

            if (errorDescription.length > 0)
            {
                shortDescription = errorDescription[0];
            } else
            {
                shortDescription = "";
            }
        }

        public String toString()
        {
            if (errorDescription == null)
            {
                return shortDescription;
            } else if (errorDescription.length == 0)
            {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < errorDescription.length - 1; i++)
            {
                sb.append(errorDescription[i]);
            }
            sb.append(errorDescription[errorDescription.length - 1]);

            return sb.toString();
        }
    }
    /**
     * Thrown if the response from the server contains an invalid status.
     */
    public static class BadServerResponse extends XenAPIException
    {
        public BadServerResponse(Map response)
        {
            super(ObjectArrayToStringArray((Object[]) response.get("ErrorDescription")));
        }
    }

    public static class BadAsyncResult extends XenAPIException
    {
        public final String result;

        public BadAsyncResult(String result)
        {
            super(result);
            this.result = result;
        }
    }

    /*
     * A call has been made which should not be made against this version of host.
     * Probably the host is out of date and cannot handle this call, or is
     * unable to comply with the details of the call. For instance SR.create
     * on Miami (4.1) hosts takes an smConfig parameter, which must be an empty map
     * when making this call on Rio (4.0) hosts.
     */
    public static class VersionException extends XenAPIException
    {
        public final String result;

        public VersionException(String result)
        {
            super(result);
            this.result = result;
        }
    }

    private static String parseResult(String result) throws BadAsyncResult
    {
        Pattern pattern = Pattern.compile("<value>(.*)</value>");
        Matcher matcher = pattern.matcher(result);
        matcher.find();

        if (matcher.groupCount() != 1)
        {
            throw new Types.BadAsyncResult("Can't interpret: " + result);
        }

        return matcher.group(1);
    }
      /**
     * Checks the provided server response was successful. If the call failed, throws a XenAPIException. If the server
     * returned an invalid response, throws a BadServerResponse. Otherwise, returns the server response as passed in.
     */
    static Map checkResponse(Map response) throws XenAPIException, BadServerResponse
    {
        if (response.get("Status").equals("Success"))
        {
            return response;
        }

        if (response.get("Status").equals("Failure"))
        {
            String[] ErrorDescription = ObjectArrayToStringArray((Object[]) response.get("ErrorDescription"));

            if (ErrorDescription[0].equals("RESTORE_TARGET_MISSING_DEVICE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.RestoreTargetMissingDevice(p1);
            }
            if (ErrorDescription[0].equals("WLB_TIMEOUT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.WlbTimeout(p1);
            }
            if (ErrorDescription[0].equals("MAC_DOES_NOT_EXIST"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.MacDoesNotExist(p1);
            }
            if (ErrorDescription[0].equals("HANDLE_INVALID"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.HandleInvalid(p1, p2);
            }
            if (ErrorDescription[0].equals("DEVICE_ALREADY_ATTACHED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.DeviceAlreadyAttached(p1);
            }
            if (ErrorDescription[0].equals("INVALID_IP_ADDRESS_SPECIFIED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.InvalidIpAddressSpecified(p1);
            }
            if (ErrorDescription[0].equals("SR_NOT_EMPTY"))
            {
                throw new Types.SrNotEmpty();
            }
            if (ErrorDescription[0].equals("VM_HVM_REQUIRED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmHvmRequired(p1);
            }
            if (ErrorDescription[0].equals("GPU_GROUP_CONTAINS_PGPU"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.GpuGroupContainsPgpu(p1);
            }
            if (ErrorDescription[0].equals("PIF_TUNNEL_STILL_EXISTS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PifTunnelStillExists(p1);
            }
            if (ErrorDescription[0].equals("PIF_BOND_NEEDS_MORE_MEMBERS"))
            {
                throw new Types.PifBondNeedsMoreMembers();
            }
            if (ErrorDescription[0].equals("PIF_ALREADY_BONDED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PifAlreadyBonded(p1);
            }
            if (ErrorDescription[0].equals("CANNOT_DESTROY_DISASTER_RECOVERY_TASK"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CannotDestroyDisasterRecoveryTask(p1);
            }
            if (ErrorDescription[0].equals("VLAN_TAG_INVALID"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VlanTagInvalid(p1);
            }
            if (ErrorDescription[0].equals("HOST_IS_SLAVE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HostIsSlave(p1);
            }
            if (ErrorDescription[0].equals("SR_HAS_MULTIPLE_PBDS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SrHasMultiplePbds(p1);
            }
            if (ErrorDescription[0].equals("POOL_AUTH_ENABLE_FAILED_INVALID_OU"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.PoolAuthEnableFailedInvalidOu(p1, p2);
            }
            if (ErrorDescription[0].equals("IMPORT_ERROR_SOME_CHECKSUMS_FAILED"))
            {
                throw new Types.ImportErrorSomeChecksumsFailed();
            }
            if (ErrorDescription[0].equals("OPENVSWITCH_NOT_ACTIVE"))
            {
                throw new Types.OpenvswitchNotActive();
            }
            if (ErrorDescription[0].equals("CANNOT_FIND_OEM_BACKUP_PARTITION"))
            {
                throw new Types.CannotFindOemBackupPartition();
            }
            if (ErrorDescription[0].equals("PIF_DEVICE_NOT_FOUND"))
            {
                throw new Types.PifDeviceNotFound();
            }
            if (ErrorDescription[0].equals("DOMAIN_BUILDER_ERROR"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.DomainBuilderError(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("PATCH_PRECHECK_FAILED_VM_RUNNING"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PatchPrecheckFailedVmRunning(p1);
            }
            if (ErrorDescription[0].equals("VM_REQUIRES_IOMMU"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmRequiresIommu(p1);
            }
            if (ErrorDescription[0].equals("HA_HOST_CANNOT_SEE_PEERS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.HaHostCannotSeePeers(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("POOL_AUTH_DISABLE_FAILED_PERMISSION_DENIED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.PoolAuthDisableFailedPermissionDenied(p1, p2);
            }
            if (ErrorDescription[0].equals("PERMISSION_DENIED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PermissionDenied(p1);
            }
            if (ErrorDescription[0].equals("SSL_VERIFY_ERROR"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SslVerifyError(p1);
            }
            if (ErrorDescription[0].equals("SR_ATTACH_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SrAttachFailed(p1);
            }
            if (ErrorDescription[0].equals("SUBJECT_ALREADY_EXISTS"))
            {
                throw new Types.SubjectAlreadyExists();
            }
            if (ErrorDescription[0].equals("HA_LOST_STATEFILE"))
            {
                throw new Types.HaLostStatefile();
            }
            if (ErrorDescription[0].equals("HA_NOT_ENABLED"))
            {
                throw new Types.HaNotEnabled();
            }
            if (ErrorDescription[0].equals("HA_HEARTBEAT_DAEMON_STARTUP_FAILED"))
            {
                throw new Types.HaHeartbeatDaemonStartupFailed();
            }
            if (ErrorDescription[0].equals("SESSION_NOT_REGISTERED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SessionNotRegistered(p1);
            }
            if (ErrorDescription[0].equals("VM_NO_SUSPEND_SR"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmNoSuspendSr(p1);
            }
            if (ErrorDescription[0].equals("VM_HAS_TOO_MANY_SNAPSHOTS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmHasTooManySnapshots(p1);
            }
            if (ErrorDescription[0].equals("PATCH_APPLY_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PatchApplyFailed(p1);
            }
            if (ErrorDescription[0].equals("VDI_READONLY"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VdiReadonly(p1);
            }
            if (ErrorDescription[0].equals("SR_FULL"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.SrFull(p1, p2);
            }
            if (ErrorDescription[0].equals("VM_REQUIRES_GPU"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VmRequiresGpu(p1, p2);
            }
            if (ErrorDescription[0].equals("VDI_NOT_AVAILABLE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VdiNotAvailable(p1);
            }
            if (ErrorDescription[0].equals("XMLRPC_UNMARSHAL_FAILURE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.XmlrpcUnmarshalFailure(p1, p2);
            }
            if (ErrorDescription[0].equals("CRL_ALREADY_EXISTS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CrlAlreadyExists(p1);
            }
            if (ErrorDescription[0].equals("HOST_MASTER_CANNOT_TALK_BACK"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HostMasterCannotTalkBack(p1);
            }
            if (ErrorDescription[0].equals("XAPI_HOOK_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                String p4 = ErrorDescription.length > 4 ? ErrorDescription[4] : "";
                throw new Types.XapiHookFailed(p1, p2, p3, p4);
            }
            if (ErrorDescription[0].equals("IMPORT_INCOMPATIBLE_VERSION"))
            {
                throw new Types.ImportIncompatibleVersion();
            }
            if (ErrorDescription[0].equals("UNKNOWN_BOOTLOADER"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.UnknownBootloader(p1, p2);
            }
            if (ErrorDescription[0].equals("XEN_VSS_REQ_ERROR_PROV_NOT_LOADED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.XenVssReqErrorProvNotLoaded(p1, p2);
            }
            if (ErrorDescription[0].equals("FEATURE_REQUIRES_HVM"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.FeatureRequiresHvm(p1);
            }
            if (ErrorDescription[0].equals("SR_VDI_LOCKING_FAILED"))
            {
                throw new Types.SrVdiLockingFailed();
            }
            if (ErrorDescription[0].equals("PIF_IS_PHYSICAL"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PifIsPhysical(p1);
            }
            if (ErrorDescription[0].equals("MAP_DUPLICATE_KEY"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                String p4 = ErrorDescription.length > 4 ? ErrorDescription[4] : "";
                throw new Types.MapDuplicateKey(p1, p2, p3, p4);
            }
            if (ErrorDescription[0].equals("MISSING_CONNECTION_DETAILS"))
            {
                throw new Types.MissingConnectionDetails();
            }
            if (ErrorDescription[0].equals("XEN_VSS_REQ_ERROR_CREATING_SNAPSHOT_XML_STRING"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.XenVssReqErrorCreatingSnapshotXmlString(p1, p2);
            }
            if (ErrorDescription[0].equals("BOOTLOADER_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.BootloaderFailed(p1, p2);
            }
            if (ErrorDescription[0].equals("WLB_XENSERVER_MALFORMED_RESPONSE"))
            {
                throw new Types.WlbXenserverMalformedResponse();
            }
            if (ErrorDescription[0].equals("GPU_GROUP_CONTAINS_VGPU"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.GpuGroupContainsVgpu(p1);
            }
            if (ErrorDescription[0].equals("POOL_AUTH_ENABLE_FAILED_DUPLICATE_HOSTNAME"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.PoolAuthEnableFailedDuplicateHostname(p1, p2);
            }
            if (ErrorDescription[0].equals("SYSTEM_STATUS_RETRIEVAL_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SystemStatusRetrievalFailed(p1);
            }
            if (ErrorDescription[0].equals("VDI_IN_USE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VdiInUse(p1, p2);
            }
            if (ErrorDescription[0].equals("HOST_NOT_LIVE"))
            {
                throw new Types.HostNotLive();
            }
            if (ErrorDescription[0].equals("CERTIFICATE_ALREADY_EXISTS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CertificateAlreadyExists(p1);
            }
            if (ErrorDescription[0].equals("SR_HAS_NO_PBDS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SrHasNoPbds(p1);
            }
            if (ErrorDescription[0].equals("CANNOT_ADD_TUNNEL_TO_BOND_SLAVE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CannotAddTunnelToBondSlave(p1);
            }
            if (ErrorDescription[0].equals("INVALID_PATCH"))
            {
                throw new Types.InvalidPatch();
            }
            if (ErrorDescription[0].equals("SR_INDESTRUCTIBLE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SrIndestructible(p1);
            }
            if (ErrorDescription[0].equals("HA_ABORT_NEW_MASTER"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HaAbortNewMaster(p1);
            }
            if (ErrorDescription[0].equals("WLB_MALFORMED_RESPONSE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.WlbMalformedResponse(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("POOL_JOINING_HOST_MUST_HAVE_PHYSICAL_MANAGEMENT_NIC"))
            {
                throw new Types.PoolJoiningHostMustHavePhysicalManagementNic();
            }
            if (ErrorDescription[0].equals("PIF_HAS_NO_V6_NETWORK_CONFIGURATION"))
            {
                throw new Types.PifHasNoV6NetworkConfiguration();
            }
            if (ErrorDescription[0].equals("VM_IS_PART_OF_AN_APPLIANCE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VmIsPartOfAnAppliance(p1, p2);
            }
            if (ErrorDescription[0].equals("WLB_XENSERVER_AUTHENTICATION_FAILED"))
            {
                throw new Types.WlbXenserverAuthenticationFailed();
            }
            if (ErrorDescription[0].equals("CANNOT_RESET_CONTROL_DOMAIN"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CannotResetControlDomain(p1);
            }
            if (ErrorDescription[0].equals("PATCH_PRECHECK_FAILED_UNKNOWN_ERROR"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.PatchPrecheckFailedUnknownError(p1, p2);
            }
            if (ErrorDescription[0].equals("HOST_CANNOT_ATTACH_NETWORK"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.HostCannotAttachNetwork(p1, p2);
            }
            if (ErrorDescription[0].equals("WLB_URL_INVALID"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.WlbUrlInvalid(p1);
            }
            if (ErrorDescription[0].equals("DUPLICATE_VM"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.DuplicateVm(p1);
            }
            if (ErrorDescription[0].equals("HOST_CANNOT_DESTROY_SELF"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HostCannotDestroySelf(p1);
            }
            if (ErrorDescription[0].equals("HOST_BROKEN"))
            {
                throw new Types.HostBroken();
            }
            if (ErrorDescription[0].equals("VM_CHECKPOINT_RESUME_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmCheckpointResumeFailed(p1);
            }
            if (ErrorDescription[0].equals("VM_TOO_MANY_VCPUS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmTooManyVcpus(p1);
            }
            if (ErrorDescription[0].equals("HOST_IS_LIVE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HostIsLive(p1);
            }
            if (ErrorDescription[0].equals("IMPORT_ERROR_ATTACHED_DISKS_NOT_FOUND"))
            {
                throw new Types.ImportErrorAttachedDisksNotFound();
            }
            if (ErrorDescription[0].equals("VBD_NOT_UNPLUGGABLE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VbdNotUnpluggable(p1);
            }
            if (ErrorDescription[0].equals("XEN_VSS_REQ_ERROR_CREATING_SNAPSHOT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.XenVssReqErrorCreatingSnapshot(p1, p2);
            }
            if (ErrorDescription[0].equals("CANNOT_ENABLE_REDO_LOG"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CannotEnableRedoLog(p1);
            }
            if (ErrorDescription[0].equals("CANNOT_EVACUATE_HOST"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CannotEvacuateHost(p1);
            }
            if (ErrorDescription[0].equals("NO_HOSTS_AVAILABLE"))
            {
                throw new Types.NoHostsAvailable();
            }
            if (ErrorDescription[0].equals("DEVICE_ATTACH_TIMEOUT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.DeviceAttachTimeout(p1, p2);
            }
            if (ErrorDescription[0].equals("INVALID_DEVICE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.InvalidDevice(p1);
            }
            if (ErrorDescription[0].equals("PBD_EXISTS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.PbdExists(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("WLB_XENSERVER_CONNECTION_REFUSED"))
            {
                throw new Types.WlbXenserverConnectionRefused();
            }
            if (ErrorDescription[0].equals("HOST_CANNOT_READ_METRICS"))
            {
                throw new Types.HostCannotReadMetrics();
            }
            if (ErrorDescription[0].equals("VM_INCOMPATIBLE_WITH_THIS_HOST"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.VmIncompatibleWithThisHost(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("NO_MORE_REDO_LOGS_ALLOWED"))
            {
                throw new Types.NoMoreRedoLogsAllowed();
            }
            if (ErrorDescription[0].equals("POOL_AUTH_ENABLE_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.PoolAuthEnableFailed(p1, p2);
            }
            if (ErrorDescription[0].equals("VM_SNAPSHOT_WITH_QUIESCE_NOT_SUPPORTED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VmSnapshotWithQuiesceNotSupported(p1, p2);
            }
            if (ErrorDescription[0].equals("LICENSE_DOES_NOT_SUPPORT_POOLING"))
            {
                throw new Types.LicenseDoesNotSupportPooling();
            }
            if (ErrorDescription[0].equals("HOST_UNKNOWN_TO_MASTER"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HostUnknownToMaster(p1);
            }
            if (ErrorDescription[0].equals("WLB_CONNECTION_REFUSED"))
            {
                throw new Types.WlbConnectionRefused();
            }
            if (ErrorDescription[0].equals("VM_SNAPSHOT_WITH_QUIESCE_PLUGIN_DEOS_NOT_RESPOND"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmSnapshotWithQuiescePluginDeosNotRespond(p1);
            }
            if (ErrorDescription[0].equals("VM_REQUIRES_SR"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VmRequiresSr(p1, p2);
            }
            if (ErrorDescription[0].equals("VM_NO_CRASHDUMP_SR"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmNoCrashdumpSr(p1);
            }
            if (ErrorDescription[0].equals("HA_NOT_INSTALLED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HaNotInstalled(p1);
            }
            if (ErrorDescription[0].equals("DUPLICATE_PIF_DEVICE_NAME"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.DuplicatePifDeviceName(p1);
            }
            if (ErrorDescription[0].equals("VM_BAD_POWER_STATE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.VmBadPowerState(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("WLB_DISABLED"))
            {
                throw new Types.WlbDisabled();
            }
            if (ErrorDescription[0].equals("VM_HOST_INCOMPATIBLE_VERSION"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VmHostIncompatibleVersion(p1, p2);
            }
            if (ErrorDescription[0].equals("POOL_JOINING_EXTERNAL_AUTH_MISMATCH"))
            {
                throw new Types.PoolJoiningExternalAuthMismatch();
            }
            if (ErrorDescription[0].equals("DISK_VBD_MUST_BE_READWRITE_FOR_HVM"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.DiskVbdMustBeReadwriteForHvm(p1);
            }
            if (ErrorDescription[0].equals("VM_BIOS_STRINGS_ALREADY_SET"))
            {
                throw new Types.VmBiosStringsAlreadySet();
            }
            if (ErrorDescription[0].equals("WLB_XENSERVER_UNKNOWN_HOST"))
            {
                throw new Types.WlbXenserverUnknownHost();
            }
            if (ErrorDescription[0].equals("HA_HOST_CANNOT_ACCESS_STATEFILE"))
            {
                throw new Types.HaHostCannotAccessStatefile();
            }
            if (ErrorDescription[0].equals("VM_FAILED_SHUTDOWN_ACKNOWLEDGMENT"))
            {
                throw new Types.VmFailedShutdownAcknowledgment();
            }
            if (ErrorDescription[0].equals("AUTH_SERVICE_ERROR"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.AuthServiceError(p1);
            }
            if (ErrorDescription[0].equals("HOST_IN_EMERGENCY_MODE"))
            {
                throw new Types.HostInEmergencyMode();
            }
            if (ErrorDescription[0].equals("HOST_DISABLED_UNTIL_REBOOT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HostDisabledUntilReboot(p1);
            }
            if (ErrorDescription[0].equals("DEFAULT_SR_NOT_FOUND"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.DefaultSrNotFound(p1);
            }
            if (ErrorDescription[0].equals("DEVICE_ALREADY_EXISTS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.DeviceAlreadyExists(p1);
            }
            if (ErrorDescription[0].equals("SR_NOT_SHARABLE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.SrNotSharable(p1, p2);
            }
            if (ErrorDescription[0].equals("VM_HAS_CHECKPOINT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmHasCheckpoint(p1);
            }
            if (ErrorDescription[0].equals("SM_PLUGIN_COMMUNICATION_FAILURE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SmPluginCommunicationFailure(p1);
            }
            if (ErrorDescription[0].equals("VM_ASSIGNED_TO_PROTECTION_POLICY"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VmAssignedToProtectionPolicy(p1, p2);
            }
            if (ErrorDescription[0].equals("RBAC_PERMISSION_DENIED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.RbacPermissionDenied(p1, p2);
            }
            if (ErrorDescription[0].equals("AUTH_DISABLE_FAILED_PERMISSION_DENIED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.AuthDisableFailedPermissionDenied(p1);
            }
            if (ErrorDescription[0].equals("LICENSE_CANNOT_DOWNGRADE_WHILE_IN_POOL"))
            {
                throw new Types.LicenseCannotDowngradeWhileInPool();
            }
            if (ErrorDescription[0].equals("TOO_MANY_PENDING_TASKS"))
            {
                throw new Types.TooManyPendingTasks();
            }
            if (ErrorDescription[0].equals("VM_SNAPSHOT_WITH_QUIESCE_TIMEOUT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmSnapshotWithQuiesceTimeout(p1);
            }
            if (ErrorDescription[0].equals("HA_CANNOT_CHANGE_BOND_STATUS_OF_MGMT_IFACE"))
            {
                throw new Types.HaCannotChangeBondStatusOfMgmtIface();
            }
            if (ErrorDescription[0].equals("PATCH_ALREADY_APPLIED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PatchAlreadyApplied(p1);
            }
            if (ErrorDescription[0].equals("SR_UUID_EXISTS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SrUuidExists(p1);
            }
            if (ErrorDescription[0].equals("AUTH_ENABLE_FAILED_DOMAIN_LOOKUP_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.AuthEnableFailedDomainLookupFailed(p1);
            }
            if (ErrorDescription[0].equals("PATCH_PRECHECK_FAILED_WRONG_SERVER_BUILD"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.PatchPrecheckFailedWrongServerBuild(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("INVALID_FEATURE_STRING"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.InvalidFeatureString(p1);
            }
            if (ErrorDescription[0].equals("WLB_NOT_INITIALIZED"))
            {
                throw new Types.WlbNotInitialized();
            }
            if (ErrorDescription[0].equals("OPERATION_BLOCKED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.OperationBlocked(p1, p2);
            }
            if (ErrorDescription[0].equals("PROVISION_ONLY_ALLOWED_ON_TEMPLATE"))
            {
                throw new Types.ProvisionOnlyAllowedOnTemplate();
            }
            if (ErrorDescription[0].equals("VM_SHUTDOWN_TIMEOUT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VmShutdownTimeout(p1, p2);
            }
            if (ErrorDescription[0].equals("ROLE_ALREADY_EXISTS"))
            {
                throw new Types.RoleAlreadyExists();
            }
            if (ErrorDescription[0].equals("NETWORK_CONTAINS_PIF"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.NetworkContainsPif(p1);
            }
            if (ErrorDescription[0].equals("COULD_NOT_FIND_NETWORK_INTERFACE_WITH_SPECIFIED_DEVICE_NAME_AND_MAC_ADDRESS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.CouldNotFindNetworkInterfaceWithSpecifiedDeviceNameAndMacAddress(p1, p2);
            }
            if (ErrorDescription[0].equals("JOINING_HOST_SERVICE_FAILED"))
            {
                throw new Types.JoiningHostServiceFailed();
            }
            if (ErrorDescription[0].equals("VDI_MISSING"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VdiMissing(p1, p2);
            }
            if (ErrorDescription[0].equals("VBD_TRAY_LOCKED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VbdTrayLocked(p1);
            }
            if (ErrorDescription[0].equals("POOL_AUTH_ENABLE_FAILED_PERMISSION_DENIED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.PoolAuthEnableFailedPermissionDenied(p1, p2);
            }
            if (ErrorDescription[0].equals("UUID_INVALID"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.UuidInvalid(p1, p2);
            }
            if (ErrorDescription[0].equals("LICENCE_RESTRICTION"))
            {
                throw new Types.LicenceRestriction();
            }
            if (ErrorDescription[0].equals("VIF_IN_USE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VifInUse(p1, p2);
            }
            if (ErrorDescription[0].equals("ONLY_ALLOWED_ON_OEM_EDITION"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.OnlyAllowedOnOemEdition(p1);
            }
            if (ErrorDescription[0].equals("VDI_IS_A_PHYSICAL_DEVICE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VdiIsAPhysicalDevice(p1);
            }
            if (ErrorDescription[0].equals("LICENSE_PROCESSING_ERROR"))
            {
                throw new Types.LicenseProcessingError();
            }
            if (ErrorDescription[0].equals("ILLEGAL_VBD_DEVICE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.IllegalVbdDevice(p1, p2);
            }
            if (ErrorDescription[0].equals("CRL_DOES_NOT_EXIST"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CrlDoesNotExist(p1);
            }
            if (ErrorDescription[0].equals("TASK_CANCELLED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.TaskCancelled(p1);
            }
            if (ErrorDescription[0].equals("VM_CRASHED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmCrashed(p1);
            }
            if (ErrorDescription[0].equals("POOL_AUTH_ENABLE_FAILED_DOMAIN_LOOKUP_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.PoolAuthEnableFailedDomainLookupFailed(p1, p2);
            }
            if (ErrorDescription[0].equals("HA_SHOULD_BE_FENCED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HaShouldBeFenced(p1);
            }
            if (ErrorDescription[0].equals("VM_UNSAFE_BOOT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmUnsafeBoot(p1);
            }
            if (ErrorDescription[0].equals("PIF_HAS_NO_NETWORK_CONFIGURATION"))
            {
                throw new Types.PifHasNoNetworkConfiguration();
            }
            if (ErrorDescription[0].equals("TOO_BUSY"))
            {
                throw new Types.TooBusy();
            }
            if (ErrorDescription[0].equals("VALUE_NOT_SUPPORTED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.ValueNotSupported(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("SESSION_INVALID"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SessionInvalid(p1);
            }
            if (ErrorDescription[0].equals("HA_CONSTRAINT_VIOLATION_NETWORK_NOT_SHARED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HaConstraintViolationNetworkNotShared(p1);
            }
            if (ErrorDescription[0].equals("HA_FAILED_TO_FORM_LIVESET"))
            {
                throw new Types.HaFailedToFormLiveset();
            }
            if (ErrorDescription[0].equals("PIF_CANNOT_BOND_CROSS_HOST"))
            {
                throw new Types.PifCannotBondCrossHost();
            }
            if (ErrorDescription[0].equals("EVENT_FROM_TOKEN_PARSE_FAILURE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.EventFromTokenParseFailure(p1);
            }
            if (ErrorDescription[0].equals("SR_REQUIRES_UPGRADE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SrRequiresUpgrade(p1);
            }
            if (ErrorDescription[0].equals("CERTIFICATE_DOES_NOT_EXIST"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CertificateDoesNotExist(p1);
            }
            if (ErrorDescription[0].equals("HA_OPERATION_WOULD_BREAK_FAILOVER_PLAN"))
            {
                throw new Types.HaOperationWouldBreakFailoverPlan();
            }
            if (ErrorDescription[0].equals("CANNOT_FETCH_PATCH"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CannotFetchPatch(p1);
            }
            if (ErrorDescription[0].equals("CANNOT_FIND_PATCH"))
            {
                throw new Types.CannotFindPatch();
            }
            if (ErrorDescription[0].equals("DB_UNIQUENESS_CONSTRAINT_VIOLATION"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.DbUniquenessConstraintViolation(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("VM_REQUIRES_NETWORK"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VmRequiresNetwork(p1, p2);
            }
            if (ErrorDescription[0].equals("VBD_NOT_EMPTY"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VbdNotEmpty(p1);
            }
            if (ErrorDescription[0].equals("HOST_NOT_ENOUGH_FREE_MEMORY"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.HostNotEnoughFreeMemory(p1, p2);
            }
            if (ErrorDescription[0].equals("VM_MIGRATE_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                String p4 = ErrorDescription.length > 4 ? ErrorDescription[4] : "";
                throw new Types.VmMigrateFailed(p1, p2, p3, p4);
            }
            if (ErrorDescription[0].equals("SR_OPERATION_NOT_SUPPORTED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SrOperationNotSupported(p1);
            }
            if (ErrorDescription[0].equals("DEVICE_NOT_ATTACHED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.DeviceNotAttached(p1);
            }
            if (ErrorDescription[0].equals("HOST_DISABLED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HostDisabled(p1);
            }
            if (ErrorDescription[0].equals("SYSTEM_STATUS_MUST_USE_TAR_ON_OEM"))
            {
                throw new Types.SystemStatusMustUseTarOnOem();
            }
            if (ErrorDescription[0].equals("XEN_VSS_REQ_ERROR_PREPARING_WRITERS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.XenVssReqErrorPreparingWriters(p1, p2);
            }
            if (ErrorDescription[0].equals("AUTH_ENABLE_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.AuthEnableFailed(p1);
            }
            if (ErrorDescription[0].equals("JOINING_HOST_CANNOT_CONTAIN_SHARED_SRS"))
            {
                throw new Types.JoiningHostCannotContainSharedSrs();
            }
            if (ErrorDescription[0].equals("VM_NO_VCPUS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmNoVcpus(p1);
            }
            if (ErrorDescription[0].equals("INVALID_PATCH_WITH_LOG"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.InvalidPatchWithLog(p1);
            }
            if (ErrorDescription[0].equals("SR_DEVICE_IN_USE"))
            {
                throw new Types.SrDeviceInUse();
            }
            if (ErrorDescription[0].equals("HOST_CD_DRIVE_EMPTY"))
            {
                throw new Types.HostCdDriveEmpty();
            }
            if (ErrorDescription[0].equals("HA_HOST_IS_ARMED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HaHostIsArmed(p1);
            }
            if (ErrorDescription[0].equals("EVENT_SUBSCRIPTION_PARSE_FAILURE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.EventSubscriptionParseFailure(p1);
            }
            if (ErrorDescription[0].equals("LICENSE_EXPIRED"))
            {
                throw new Types.LicenseExpired();
            }
            if (ErrorDescription[0].equals("SESSION_AUTHENTICATION_FAILED"))
            {
                throw new Types.SessionAuthenticationFailed();
            }
            if (ErrorDescription[0].equals("PIF_IS_VLAN"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PifIsVlan(p1);
            }
            if (ErrorDescription[0].equals("VMPP_ARCHIVE_MORE_FREQUENT_THAN_BACKUP"))
            {
                throw new Types.VmppArchiveMoreFrequentThanBackup();
            }
            if (ErrorDescription[0].equals("V6D_FAILURE"))
            {
                throw new Types.V6dFailure();
            }
            if (ErrorDescription[0].equals("JOINING_HOST_CANNOT_BE_MASTER_OF_OTHER_HOSTS"))
            {
                throw new Types.JoiningHostCannotBeMasterOfOtherHosts();
            }
            if (ErrorDescription[0].equals("HOST_HAS_RESIDENT_VMS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HostHasResidentVms(p1);
            }
            if (ErrorDescription[0].equals("VM_CHECKPOINT_SUSPEND_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmCheckpointSuspendFailed(p1);
            }
            if (ErrorDescription[0].equals("PIF_IS_MANAGEMENT_INTERFACE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PifIsManagementInterface(p1);
            }
            if (ErrorDescription[0].equals("MAC_INVALID"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.MacInvalid(p1);
            }
            if (ErrorDescription[0].equals("XEN_VSS_REQ_ERROR_START_SNAPSHOT_SET_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.XenVssReqErrorStartSnapshotSetFailed(p1, p2);
            }
            if (ErrorDescription[0].equals("VBD_IS_EMPTY"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VbdIsEmpty(p1);
            }
            if (ErrorDescription[0].equals("PATCH_PRECHECK_FAILED_WRONG_SERVER_VERSION"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.PatchPrecheckFailedWrongServerVersion(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("CANNOT_FIND_STATE_PARTITION"))
            {
                throw new Types.CannotFindStatePartition();
            }
            if (ErrorDescription[0].equals("WLB_AUTHENTICATION_FAILED"))
            {
                throw new Types.WlbAuthenticationFailed();
            }
            if (ErrorDescription[0].equals("AUTH_UNKNOWN_TYPE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.AuthUnknownType(p1);
            }
            if (ErrorDescription[0].equals("NOT_IN_EMERGENCY_MODE"))
            {
                throw new Types.NotInEmergencyMode();
            }
            if (ErrorDescription[0].equals("AUTH_DISABLE_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.AuthDisableFailed(p1);
            }
            if (ErrorDescription[0].equals("NETWORK_ALREADY_CONNECTED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.NetworkAlreadyConnected(p1, p2);
            }
            if (ErrorDescription[0].equals("VDI_INCOMPATIBLE_TYPE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VdiIncompatibleType(p1, p2);
            }
            if (ErrorDescription[0].equals("WLB_UNKNOWN_HOST"))
            {
                throw new Types.WlbUnknownHost();
            }
            if (ErrorDescription[0].equals("IMPORT_ERROR"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.ImportError(p1);
            }
            if (ErrorDescription[0].equals("SR_UNKNOWN_DRIVER"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SrUnknownDriver(p1);
            }
            if (ErrorDescription[0].equals("AUTH_DISABLE_FAILED_WRONG_CREDENTIALS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.AuthDisableFailedWrongCredentials(p1);
            }
            if (ErrorDescription[0].equals("VM_HALTED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmHalted(p1);
            }
            if (ErrorDescription[0].equals("FEATURE_RESTRICTED"))
            {
                throw new Types.FeatureRestricted();
            }
            if (ErrorDescription[0].equals("VDI_CONTAINS_METADATA_OF_THIS_POOL"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VdiContainsMetadataOfThisPool(p1, p2);
            }
            if (ErrorDescription[0].equals("CRL_NAME_INVALID"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CrlNameInvalid(p1);
            }
            if (ErrorDescription[0].equals("HOST_POWER_ON_MODE_DISABLED"))
            {
                throw new Types.HostPowerOnModeDisabled();
            }
            if (ErrorDescription[0].equals("ACTIVATION_WHILE_NOT_FREE"))
            {
                throw new Types.ActivationWhileNotFree();
            }
            if (ErrorDescription[0].equals("XENAPI_PLUGIN_FAILURE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.XenapiPluginFailure(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("MAC_STILL_EXISTS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.MacStillExists(p1);
            }
            if (ErrorDescription[0].equals("HOST_IN_USE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.HostInUse(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("HA_TOO_FEW_HOSTS"))
            {
                throw new Types.HaTooFewHosts();
            }
            if (ErrorDescription[0].equals("WLB_CONNECTION_RESET"))
            {
                throw new Types.WlbConnectionReset();
            }
            if (ErrorDescription[0].equals("POOL_AUTH_ENABLE_FAILED_WRONG_CREDENTIALS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.PoolAuthEnableFailedWrongCredentials(p1, p2);
            }
            if (ErrorDescription[0].equals("PATCH_IS_APPLIED"))
            {
                throw new Types.PatchIsApplied();
            }
            if (ErrorDescription[0].equals("SR_HAS_PBD"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.SrHasPbd(p1);
            }
            if (ErrorDescription[0].equals("OPERATION_PARTIALLY_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.OperationPartiallyFailed(p1);
            }
            if (ErrorDescription[0].equals("WLB_MALFORMED_REQUEST"))
            {
                throw new Types.WlbMalformedRequest();
            }
            if (ErrorDescription[0].equals("HOST_STILL_BOOTING"))
            {
                throw new Types.HostStillBooting();
            }
            if (ErrorDescription[0].equals("CANNOT_DESTROY_SYSTEM_NETWORK"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CannotDestroySystemNetwork(p1);
            }
            if (ErrorDescription[0].equals("OBJECT_NOLONGER_EXISTS"))
            {
                throw new Types.ObjectNolongerExists();
            }
            if (ErrorDescription[0].equals("VDI_NOT_IN_MAP"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VdiNotInMap(p1);
            }
            if (ErrorDescription[0].equals("HOSTS_NOT_HOMOGENEOUS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HostsNotHomogeneous(p1);
            }
            if (ErrorDescription[0].equals("POOL_JOINING_HOST_MUST_HAVE_SAME_PRODUCT_VERSION"))
            {
                throw new Types.PoolJoiningHostMustHaveSameProductVersion();
            }
            if (ErrorDescription[0].equals("PIF_VLAN_EXISTS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PifVlanExists(p1);
            }
            if (ErrorDescription[0].equals("LICENSE_CHECKOUT_ERROR"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.LicenseCheckoutError(p1);
            }
            if (ErrorDescription[0].equals("CERTIFICATE_LIBRARY_CORRUPT"))
            {
                throw new Types.CertificateLibraryCorrupt();
            }
            if (ErrorDescription[0].equals("VDI_NOT_MANAGED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VdiNotManaged(p1);
            }
            if (ErrorDescription[0].equals("INVALID_EDITION"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.InvalidEdition(p1);
            }
            if (ErrorDescription[0].equals("PATCH_ALREADY_EXISTS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PatchAlreadyExists(p1);
            }
            if (ErrorDescription[0].equals("OUT_OF_SPACE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.OutOfSpace(p1);
            }
            if (ErrorDescription[0].equals("IMPORT_ERROR_PREMATURE_EOF"))
            {
                throw new Types.ImportErrorPrematureEof();
            }
            if (ErrorDescription[0].equals("NOT_SYSTEM_DOMAIN"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.NotSystemDomain(p1);
            }
            if (ErrorDescription[0].equals("VM_MEMORY_SIZE_TOO_LOW"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmMemorySizeTooLow(p1);
            }
            if (ErrorDescription[0].equals("VMPP_HAS_VM"))
            {
                throw new Types.VmppHasVm();
            }
            if (ErrorDescription[0].equals("HOST_NOT_DISABLED"))
            {
                throw new Types.HostNotDisabled();
            }
            if (ErrorDescription[0].equals("FIELD_TYPE_ERROR"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.FieldTypeError(p1);
            }
            if (ErrorDescription[0].equals("SLAVE_REQUIRES_MANAGEMENT_INTERFACE"))
            {
                throw new Types.SlaveRequiresManagementInterface();
            }
            if (ErrorDescription[0].equals("VM_IS_TEMPLATE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmIsTemplate(p1);
            }
            if (ErrorDescription[0].equals("VM_IS_PROTECTED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmIsProtected(p1);
            }
            if (ErrorDescription[0].equals("JOINING_HOST_CANNOT_HAVE_RUNNING_VMS"))
            {
                throw new Types.JoiningHostCannotHaveRunningVms();
            }
            if (ErrorDescription[0].equals("VM_REQUIRES_VDI"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VmRequiresVdi(p1, p2);
            }
            if (ErrorDescription[0].equals("VBD_CDS_MUST_BE_READONLY"))
            {
                throw new Types.VbdCdsMustBeReadonly();
            }
            if (ErrorDescription[0].equals("LICENSE_FILE_DEPRECATED"))
            {
                throw new Types.LicenseFileDeprecated();
            }
            if (ErrorDescription[0].equals("CANNOT_CREATE_STATE_FILE"))
            {
                throw new Types.CannotCreateStateFile();
            }
            if (ErrorDescription[0].equals("JOINING_HOST_CANNOT_HAVE_VMS_WITH_CURRENT_OPERATIONS"))
            {
                throw new Types.JoiningHostCannotHaveVmsWithCurrentOperations();
            }
            if (ErrorDescription[0].equals("MESSAGE_PARAMETER_COUNT_MISMATCH"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.MessageParameterCountMismatch(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("POOL_AUTH_ALREADY_ENABLED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PoolAuthAlreadyEnabled(p1);
            }
            if (ErrorDescription[0].equals("RESTORE_INCOMPATIBLE_VERSION"))
            {
                throw new Types.RestoreIncompatibleVersion();
            }
            if (ErrorDescription[0].equals("DEVICE_DETACH_REJECTED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.DeviceDetachRejected(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("AUTH_IS_DISABLED"))
            {
                throw new Types.AuthIsDisabled();
            }
            if (ErrorDescription[0].equals("JOINING_HOST_CANNOT_HAVE_RUNNING_OR_SUSPENDED_VMS"))
            {
                throw new Types.JoiningHostCannotHaveRunningOrSuspendedVms();
            }
            if (ErrorDescription[0].equals("PATCH_PRECHECK_FAILED_PREREQUISITE_MISSING"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.PatchPrecheckFailedPrerequisiteMissing(p1, p2);
            }
            if (ErrorDescription[0].equals("VM_HAS_PCI_ATTACHED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmHasPciAttached(p1);
            }
            if (ErrorDescription[0].equals("MIRROR_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.MirrorFailed(p1);
            }
            if (ErrorDescription[0].equals("WLB_XENSERVER_TIMEOUT"))
            {
                throw new Types.WlbXenserverTimeout();
            }
            if (ErrorDescription[0].equals("POOL_AUTH_DISABLE_FAILED_WRONG_CREDENTIALS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.PoolAuthDisableFailedWrongCredentials(p1, p2);
            }
            if (ErrorDescription[0].equals("VM_SNAPSHOT_WITH_QUIESCE_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmSnapshotWithQuiesceFailed(p1);
            }
            if (ErrorDescription[0].equals("CERTIFICATE_CORRUPT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CertificateCorrupt(p1);
            }
            if (ErrorDescription[0].equals("WLB_INTERNAL_ERROR"))
            {
                throw new Types.WlbInternalError();
            }
            if (ErrorDescription[0].equals("VM_REBOOTED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmRebooted(p1);
            }
            if (ErrorDescription[0].equals("CANNOT_CONTACT_HOST"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CannotContactHost(p1);
            }
            if (ErrorDescription[0].equals("XEN_VSS_REQ_ERROR_NO_VOLUMES_SUPPORTED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.XenVssReqErrorNoVolumesSupported(p1, p2);
            }
            if (ErrorDescription[0].equals("HOST_ITS_OWN_SLAVE"))
            {
                throw new Types.HostItsOwnSlave();
            }
            if (ErrorDescription[0].equals("CANNOT_ADD_VLAN_TO_BOND_SLAVE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CannotAddVlanToBondSlave(p1);
            }
            if (ErrorDescription[0].equals("REDO_LOG_IS_ENABLED"))
            {
                throw new Types.RedoLogIsEnabled();
            }
            if (ErrorDescription[0].equals("VM_MISSING_PV_DRIVERS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmMissingPvDrivers(p1);
            }
            if (ErrorDescription[0].equals("CERTIFICATE_NAME_INVALID"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CertificateNameInvalid(p1);
            }
            if (ErrorDescription[0].equals("IMPORT_ERROR_FAILED_TO_FIND_OBJECT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.ImportErrorFailedToFindObject(p1);
            }
            if (ErrorDescription[0].equals("VDI_LOCATION_MISSING"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VdiLocationMissing(p1, p2);
            }
            if (ErrorDescription[0].equals("AUTH_ENABLE_FAILED_PERMISSION_DENIED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.AuthEnableFailedPermissionDenied(p1);
            }
            if (ErrorDescription[0].equals("PIF_VLAN_STILL_EXISTS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PifVlanStillExists(p1);
            }
            if (ErrorDescription[0].equals("VMS_FAILED_TO_COOPERATE"))
            {
                throw new Types.VmsFailedToCooperate();
            }
            if (ErrorDescription[0].equals("TOO_MANY_STORAGE_MIGRATES"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.TooManyStorageMigrates(p1);
            }
            if (ErrorDescription[0].equals("NETWORK_CONTAINS_VIF"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.NetworkContainsVif(p1);
            }
            if (ErrorDescription[0].equals("INVALID_VALUE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.InvalidValue(p1, p2);
            }
            if (ErrorDescription[0].equals("XENAPI_MISSING_PLUGIN"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.XenapiMissingPlugin(p1);
            }
            if (ErrorDescription[0].equals("RESTORE_TARGET_MGMT_IF_NOT_IN_BACKUP"))
            {
                throw new Types.RestoreTargetMgmtIfNotInBackup();
            }
            if (ErrorDescription[0].equals("IS_TUNNEL_ACCESS_PIF"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.IsTunnelAccessPif(p1);
            }
            if (ErrorDescription[0].equals("JOINING_HOST_CONNECTION_FAILED"))
            {
                throw new Types.JoiningHostConnectionFailed();
            }
            if (ErrorDescription[0].equals("SUBJECT_CANNOT_BE_RESOLVED"))
            {
                throw new Types.SubjectCannotBeResolved();
            }
            if (ErrorDescription[0].equals("XEN_VSS_REQ_ERROR_ADDING_VOLUME_TO_SNAPSET_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.XenVssReqErrorAddingVolumeToSnapsetFailed(p1, p2);
            }
            if (ErrorDescription[0].equals("PROVISION_FAILED_OUT_OF_SPACE"))
            {
                throw new Types.ProvisionFailedOutOfSpace();
            }
            if (ErrorDescription[0].equals("VDI_NEEDS_VM_FOR_MIGRATE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VdiNeedsVmForMigrate(p1);
            }
            if (ErrorDescription[0].equals("COULD_NOT_IMPORT_DATABASE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CouldNotImportDatabase(p1);
            }
            if (ErrorDescription[0].equals("VDI_IS_NOT_ISO"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VdiIsNotIso(p1, p2);
            }
            if (ErrorDescription[0].equals("MESSAGE_METHOD_UNKNOWN"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.MessageMethodUnknown(p1);
            }
            if (ErrorDescription[0].equals("VM_CANNOT_DELETE_DEFAULT_TEMPLATE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmCannotDeleteDefaultTemplate(p1);
            }
            if (ErrorDescription[0].equals("ROLE_NOT_FOUND"))
            {
                throw new Types.RoleNotFound();
            }
            if (ErrorDescription[0].equals("NOT_ALLOWED_ON_OEM_EDITION"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.NotAllowedOnOemEdition(p1);
            }
            if (ErrorDescription[0].equals("RESTORE_SCRIPT_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.RestoreScriptFailed(p1);
            }
            if (ErrorDescription[0].equals("INTERNAL_ERROR"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.InternalError(p1);
            }
            if (ErrorDescription[0].equals("LICENSE_DOES_NOT_SUPPORT_XHA"))
            {
                throw new Types.LicenseDoesNotSupportXha();
            }
            if (ErrorDescription[0].equals("PIF_INCOMPATIBLE_PRIMARY_ADDRESS_TYPE"))
            {
                throw new Types.PifIncompatiblePrimaryAddressType();
            }
            if (ErrorDescription[0].equals("DEVICE_ALREADY_DETACHED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.DeviceAlreadyDetached(p1);
            }
            if (ErrorDescription[0].equals("AUTH_ENABLE_FAILED_UNAVAILABLE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.AuthEnableFailedUnavailable(p1);
            }
            if (ErrorDescription[0].equals("VBD_NOT_REMOVABLE_MEDIA"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VbdNotRemovableMedia(p1);
            }
            if (ErrorDescription[0].equals("LOCATION_NOT_UNIQUE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.LocationNotUnique(p1, p2);
            }
            if (ErrorDescription[0].equals("NOT_IMPLEMENTED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.NotImplemented(p1);
            }
            if (ErrorDescription[0].equals("CANNOT_PLUG_VIF"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CannotPlugVif(p1);
            }
            if (ErrorDescription[0].equals("USER_IS_NOT_LOCAL_SUPERUSER"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.UserIsNotLocalSuperuser(p1);
            }
            if (ErrorDescription[0].equals("BACKUP_SCRIPT_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.BackupScriptFailed(p1);
            }
            if (ErrorDescription[0].equals("IMPORT_ERROR_UNEXPECTED_FILE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.ImportErrorUnexpectedFile(p1, p2);
            }
            if (ErrorDescription[0].equals("AUTH_ALREADY_ENABLED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.AuthAlreadyEnabled(p1, p2);
            }
            if (ErrorDescription[0].equals("OPERATION_NOT_ALLOWED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.OperationNotAllowed(p1);
            }
            if (ErrorDescription[0].equals("HA_NO_PLAN"))
            {
                throw new Types.HaNoPlan();
            }
            if (ErrorDescription[0].equals("EVENTS_LOST"))
            {
                throw new Types.EventsLost();
            }
            if (ErrorDescription[0].equals("SR_BACKEND_FAILURE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.SrBackendFailure(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("DEVICE_DETACH_TIMEOUT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.DeviceDetachTimeout(p1, p2);
            }
            if (ErrorDescription[0].equals("VM_DUPLICATE_VBD_DEVICE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.VmDuplicateVbdDevice(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("CANNOT_PLUG_BOND_SLAVE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CannotPlugBondSlave(p1);
            }
            if (ErrorDescription[0].equals("VM_TO_IMPORT_IS_NOT_NEWER_VERSION"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.VmToImportIsNotNewerVersion(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("CRL_CORRUPT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CrlCorrupt(p1);
            }
            if (ErrorDescription[0].equals("VM_OLD_PV_DRIVERS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                String p3 = ErrorDescription.length > 3 ? ErrorDescription[3] : "";
                throw new Types.VmOldPvDrivers(p1, p2, p3);
            }
            if (ErrorDescription[0].equals("PIF_DOES_NOT_ALLOW_UNPLUG"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.PifDoesNotAllowUnplug(p1);
            }
            if (ErrorDescription[0].equals("CHANGE_PASSWORD_REJECTED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.ChangePasswordRejected(p1);
            }
            if (ErrorDescription[0].equals("OTHER_OPERATION_IN_PROGRESS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.OtherOperationInProgress(p1, p2);
            }
            if (ErrorDescription[0].equals("XEN_VSS_REQ_ERROR_INIT_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.XenVssReqErrorInitFailed(p1, p2);
            }
            if (ErrorDescription[0].equals("CPU_FEATURE_MASKING_NOT_SUPPORTED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.CpuFeatureMaskingNotSupported(p1);
            }
            if (ErrorDescription[0].equals("VM_NOT_RESIDENT_HERE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VmNotResidentHere(p1, p2);
            }
            if (ErrorDescription[0].equals("HOST_OFFLINE"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HostOffline(p1);
            }
            if (ErrorDescription[0].equals("POOL_AUTH_DISABLE_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.PoolAuthDisableFailed(p1, p2);
            }
            if (ErrorDescription[0].equals("HOST_HAS_NO_MANAGEMENT_IP"))
            {
                throw new Types.HostHasNoManagementIp();
            }
            if (ErrorDescription[0].equals("TRANSPORT_PIF_NOT_CONFIGURED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.TransportPifNotConfigured(p1);
            }
            if (ErrorDescription[0].equals("HA_IS_ENABLED"))
            {
                throw new Types.HaIsEnabled();
            }
            if (ErrorDescription[0].equals("VM_REVERT_FAILED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.VmRevertFailed(p1, p2);
            }
            if (ErrorDescription[0].equals("HOST_NAME_INVALID"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HostNameInvalid(p1);
            }
            if (ErrorDescription[0].equals("DOMAIN_EXISTS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.DomainExists(p1, p2);
            }
            if (ErrorDescription[0].equals("HA_POOL_IS_ENABLED_BUT_HOST_IS_DISABLED"))
            {
                throw new Types.HaPoolIsEnabledButHostIsDisabled();
            }
            if (ErrorDescription[0].equals("MESSAGE_DEPRECATED"))
            {
                throw new Types.MessageDeprecated();
            }
            if (ErrorDescription[0].equals("HA_CONSTRAINT_VIOLATION_SR_NOT_SHARED"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.HaConstraintViolationSrNotShared(p1);
            }
            if (ErrorDescription[0].equals("IMPORT_ERROR_CANNOT_HANDLE_CHUNKED"))
            {
                throw new Types.ImportErrorCannotHandleChunked();
            }
            if (ErrorDescription[0].equals("VM_ATTACHED_TO_MORE_THAN_ONE_VDI_WITH_TIMEOFFSET_MARKED_AS_RESET_ON_BOOT"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.VmAttachedToMoreThanOneVdiWithTimeoffsetMarkedAsResetOnBoot(p1);
            }
            if (ErrorDescription[0].equals("NOT_SUPPORTED_DURING_UPGRADE"))
            {
                throw new Types.NotSupportedDuringUpgrade();
            }
            if (ErrorDescription[0].equals("PIF_CONFIGURATION_ERROR"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                String p2 = ErrorDescription.length > 2 ? ErrorDescription[2] : "";
                throw new Types.PifConfigurationError(p1, p2);
            }
            if (ErrorDescription[0].equals("INTERFACE_HAS_NO_IP"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.InterfaceHasNoIp(p1);
            }
            if (ErrorDescription[0].equals("HOSTS_NOT_COMPATIBLE"))
            {
                throw new Types.HostsNotCompatible();
            }
            if (ErrorDescription[0].equals("AUTH_ENABLE_FAILED_WRONG_CREDENTIALS"))
            {
                String p1 = ErrorDescription.length > 1 ? ErrorDescription[1] : "";
                throw new Types.AuthEnableFailedWrongCredentials(p1);
            }

            // An unknown error occurred
            throw new Types.XenAPIException(ErrorDescription);
        }

        throw new BadServerResponse(response);
    }

    public enum VdiOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Scanning backends for new or deleted VDIs
         */
        SCAN,
        /**
         * Cloning the VDI
         */
        CLONE,
        /**
         * Copying the VDI
         */
        COPY,
        /**
         * Resizing the VDI
         */
        RESIZE,
        /**
         * Resizing the VDI which may or may not be online
         */
        RESIZE_ONLINE,
        /**
         * Snapshotting the VDI
         */
        SNAPSHOT,
        /**
         * Destroying the VDI
         */
        DESTROY,
        /**
         * Forget about the VDI
         */
        FORGET,
        /**
         * Refreshing the fields of the VDI
         */
        UPDATE,
        /**
         * Forcibly unlocking the VDI
         */
        FORCE_UNLOCK,
        /**
         * Generating static configuration
         */
        GENERATE_CONFIG,
        /**
         * Operations on this VDI are temporarily blocked
         */
        BLOCKED;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == SCAN) return "scan";
            if (this == CLONE) return "clone";
            if (this == COPY) return "copy";
            if (this == RESIZE) return "resize";
            if (this == RESIZE_ONLINE) return "resize_online";
            if (this == SNAPSHOT) return "snapshot";
            if (this == DESTROY) return "destroy";
            if (this == FORGET) return "forget";
            if (this == UPDATE) return "update";
            if (this == FORCE_UNLOCK) return "force_unlock";
            if (this == GENERATE_CONFIG) return "generate_config";
            if (this == BLOCKED) return "blocked";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum Cls {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * VM
         */
        VM,
        /**
         * Host
         */
        HOST,
        /**
         * SR
         */
        SR,
        /**
         * Pool
         */
        POOL,
        /**
         * VMPP
         */
        VMPP;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == VM) return "VM";
            if (this == HOST) return "Host";
            if (this == SR) return "SR";
            if (this == POOL) return "Pool";
            if (this == VMPP) return "VMPP";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VdiType {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * a disk that may be replaced on upgrade
         */
        SYSTEM,
        /**
         * a disk that is always preserved on upgrade
         */
        USER,
        /**
         * a disk that may be reformatted on upgrade
         */
        EPHEMERAL,
        /**
         * a disk that stores a suspend image
         */
        SUSPEND,
        /**
         * a disk that stores VM crashdump information
         */
        CRASHDUMP,
        /**
         * a disk used for HA storage heartbeating
         */
        HA_STATEFILE,
        /**
         * a disk used for HA Pool metadata
         */
        METADATA,
        /**
         * a disk used for a general metadata redo-log
         */
        REDO_LOG;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == SYSTEM) return "system";
            if (this == USER) return "user";
            if (this == EPHEMERAL) return "ephemeral";
            if (this == SUSPEND) return "suspend";
            if (this == CRASHDUMP) return "crashdump";
            if (this == HA_STATEFILE) return "ha_statefile";
            if (this == METADATA) return "metadata";
            if (this == REDO_LOG) return "redo_log";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum AfterApplyGuidance {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * This patch requires HVM guests to be restarted once applied.
         */
        RESTARTHVM,
        /**
         * This patch requires PV guests to be restarted once applied.
         */
        RESTARTPV,
        /**
         * This patch requires the host to be restarted once applied.
         */
        RESTARTHOST,
        /**
         * This patch requires XAPI to be restarted once applied.
         */
        RESTARTXAPI;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == RESTARTHVM) return "restartHVM";
            if (this == RESTARTPV) return "restartPV";
            if (this == RESTARTHOST) return "restartHost";
            if (this == RESTARTXAPI) return "restartXAPI";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum EventOperation {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * An object has been created
         */
        ADD,
        /**
         * An object has been deleted
         */
        DEL,
        /**
         * An object has been modified
         */
        MOD;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == ADD) return "add";
            if (this == DEL) return "del";
            if (this == MOD) return "mod";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum PrimaryAddressType {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Primary address is the IPv4 address
         */
        IPV4,
        /**
         * Primary address is the IPv6 address
         */
        IPV6;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == IPV4) return "IPv4";
            if (this == IPV6) return "IPv6";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum TaskAllowedOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * refers to the operation "cancel"
         */
        CANCEL;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == CANCEL) return "cancel";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum TaskStatusType {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * task is in progress
         */
        PENDING,
        /**
         * task was completed successfully
         */
        SUCCESS,
        /**
         * task has failed
         */
        FAILURE,
        /**
         * task is being cancelled
         */
        CANCELLING,
        /**
         * task has been cancelled
         */
        CANCELLED;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == PENDING) return "pending";
            if (this == SUCCESS) return "success";
            if (this == FAILURE) return "failure";
            if (this == CANCELLING) return "cancelling";
            if (this == CANCELLED) return "cancelled";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum NetworkOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Indicates this network is attaching to a VIF or PIF
         */
        ATTACHING;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == ATTACHING) return "attaching";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum ConsoleProtocol {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * VT100 terminal
         */
        VT100,
        /**
         * Remote FrameBuffer protocol (as used in VNC)
         */
        RFB,
        /**
         * Remote Desktop Protocol
         */
        RDP;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == VT100) return "vt100";
            if (this == RFB) return "rfb";
            if (this == RDP) return "rdp";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum OnCrashBehaviour {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * destroy the VM state
         */
        DESTROY,
        /**
         * record a coredump and then destroy the VM state
         */
        COREDUMP_AND_DESTROY,
        /**
         * restart the VM
         */
        RESTART,
        /**
         * record a coredump and then restart the VM
         */
        COREDUMP_AND_RESTART,
        /**
         * leave the crashed VM paused
         */
        PRESERVE,
        /**
         * rename the crashed VM and start a new copy
         */
        RENAME_RESTART;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == DESTROY) return "destroy";
            if (this == COREDUMP_AND_DESTROY) return "coredump_and_destroy";
            if (this == RESTART) return "restart";
            if (this == COREDUMP_AND_RESTART) return "coredump_and_restart";
            if (this == PRESERVE) return "preserve";
            if (this == RENAME_RESTART) return "rename_restart";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VmppBackupType {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * The backup is a snapshot
         */
        SNAPSHOT,
        /**
         * The backup is a checkpoint
         */
        CHECKPOINT;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == SNAPSHOT) return "snapshot";
            if (this == CHECKPOINT) return "checkpoint";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum OnNormalExit {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * destroy the VM state
         */
        DESTROY,
        /**
         * restart the VM
         */
        RESTART;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == DESTROY) return "destroy";
            if (this == RESTART) return "restart";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VifOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Attempting to attach this VIF to a VM
         */
        ATTACH,
        /**
         * Attempting to hotplug this VIF
         */
        PLUG,
        /**
         * Attempting to hot unplug this VIF
         */
        UNPLUG;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == ATTACH) return "attach";
            if (this == PLUG) return "plug";
            if (this == UNPLUG) return "unplug";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum XenAPIObjects {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * A session
         */
        SESSION,
        /**
         * Management of remote authentication services
         */
        AUTH,
        /**
         * A user or group that can log in xapi
         */
        SUBJECT,
        /**
         * A set of permissions associated with a subject
         */
        ROLE,
        /**
         * A long-running asynchronous task
         */
        TASK,
        /**
         * Asynchronous event registration and handling
         */
        EVENT,
        /**
         * Pool-wide information
         */
        POOL,
        /**
         * Pool-wide patches
         */
        POOL_PATCH,
        /**
         * A virtual machine (or 'guest').
         */
        VM,
        /**
         * The metrics associated with a VM
         */
        VM_METRICS,
        /**
         * The metrics reported by the guest (as opposed to inferred from outside)
         */
        VM_GUEST_METRICS,
        /**
         * VM Protection Policy
         */
        VMPP,
        /**
         * VM appliance
         */
        VM_APPLIANCE,
        /**
         * DR task
         */
        DR_TASK,
        /**
         * A physical host
         */
        HOST,
        /**
         * Represents a host crash dump
         */
        HOST_CRASHDUMP,
        /**
         * Represents a patch stored on a server
         */
        HOST_PATCH,
        /**
         * The metrics associated with a host
         */
        HOST_METRICS,
        /**
         * A physical CPU
         */
        HOST_CPU,
        /**
         * A virtual network
         */
        NETWORK,
        /**
         * A virtual network interface
         */
        VIF,
        /**
         * The metrics associated with a virtual network device
         */
        VIF_METRICS,
        /**
         * A physical network interface (note separate VLANs are represented as several PIFs)
         */
        PIF,
        /**
         * The metrics associated with a physical network interface
         */
        PIF_METRICS,
        /**
         *
         */
        BOND,
        /**
         * A VLAN mux/demux
         */
        VLAN,
        /**
         * A storage manager plugin
         */
        SM,
        /**
         * A storage repository
         */
        SR,
        /**
         * A virtual disk image
         */
        VDI,
        /**
         * A virtual block device
         */
        VBD,
        /**
         * The metrics associated with a virtual block device
         */
        VBD_METRICS,
        /**
         * The physical block devices through which hosts access SRs
         */
        PBD,
        /**
         * A VM crashdump
         */
        CRASHDUMP,
        /**
         * A virtual TPM device
         */
        VTPM,
        /**
         * A console
         */
        CONSOLE,
        /**
         * A user of the system
         */
        USER,
        /**
         * Data sources for logging in RRDs
         */
        DATA_SOURCE,
        /**
         * A placeholder for a binary blob
         */
        BLOB,
        /**
         * An message for the attention of the administrator
         */
        MESSAGE,
        /**
         * A secret
         */
        SECRET,
        /**
         * A tunnel for network traffic
         */
        TUNNEL,
        /**
         * A PCI device
         */
        PCI,
        /**
         * A physical GPU (pGPU)
         */
        PGPU,
        /**
         * A group of compatible GPUs across the resource pool
         */
        GPU_GROUP,
        /**
         * A virtual GPU (vGPU)
         */
        VGPU;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == SESSION) return "session";
            if (this == AUTH) return "auth";
            if (this == SUBJECT) return "subject";
            if (this == ROLE) return "role";
            if (this == TASK) return "task";
            if (this == EVENT) return "event";
            if (this == POOL) return "pool";
            if (this == POOL_PATCH) return "pool_patch";
            if (this == VM) return "VM";
            if (this == VM_METRICS) return "VM_metrics";
            if (this == VM_GUEST_METRICS) return "VM_guest_metrics";
            if (this == VMPP) return "VMPP";
            if (this == VM_APPLIANCE) return "VM_appliance";
            if (this == DR_TASK) return "DR_task";
            if (this == HOST) return "host";
            if (this == HOST_CRASHDUMP) return "host_crashdump";
            if (this == HOST_PATCH) return "host_patch";
            if (this == HOST_METRICS) return "host_metrics";
            if (this == HOST_CPU) return "host_cpu";
            if (this == NETWORK) return "network";
            if (this == VIF) return "VIF";
            if (this == VIF_METRICS) return "VIF_metrics";
            if (this == PIF) return "PIF";
            if (this == PIF_METRICS) return "PIF_metrics";
            if (this == BOND) return "Bond";
            if (this == VLAN) return "VLAN";
            if (this == SM) return "SM";
            if (this == SR) return "SR";
            if (this == VDI) return "VDI";
            if (this == VBD) return "VBD";
            if (this == VBD_METRICS) return "VBD_metrics";
            if (this == PBD) return "PBD";
            if (this == CRASHDUMP) return "crashdump";
            if (this == VTPM) return "VTPM";
            if (this == CONSOLE) return "console";
            if (this == USER) return "user";
            if (this == DATA_SOURCE) return "data_source";
            if (this == BLOB) return "blob";
            if (this == MESSAGE) return "message";
            if (this == SECRET) return "secret";
            if (this == TUNNEL) return "tunnel";
            if (this == PCI) return "PCI";
            if (this == PGPU) return "PGPU";
            if (this == GPU_GROUP) return "GPU_group";
            if (this == VGPU) return "VGPU";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum HostAllowedOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Indicates this host is able to provision another VM
         */
        PROVISION,
        /**
         * Indicates this host is evacuating
         */
        EVACUATE,
        /**
         * Indicates this host is in the process of shutting itself down
         */
        SHUTDOWN,
        /**
         * Indicates this host is in the process of rebooting
         */
        REBOOT,
        /**
         * Indicates this host is in the process of being powered on
         */
        POWER_ON,
        /**
         * This host is starting a VM
         */
        VM_START,
        /**
         * This host is resuming a VM
         */
        VM_RESUME,
        /**
         * This host is the migration target of a VM
         */
        VM_MIGRATE;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == PROVISION) return "provision";
            if (this == EVACUATE) return "evacuate";
            if (this == SHUTDOWN) return "shutdown";
            if (this == REBOOT) return "reboot";
            if (this == POWER_ON) return "power_on";
            if (this == VM_START) return "vm_start";
            if (this == VM_RESUME) return "vm_resume";
            if (this == VM_MIGRATE) return "vm_migrate";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VmppArchiveFrequency {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Never archive
         */
        NEVER,
        /**
         * Archive after backup
         */
        ALWAYS_AFTER_BACKUP,
        /**
         * Daily archives
         */
        DAILY,
        /**
         * Weekly backups
         */
        WEEKLY;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == NEVER) return "never";
            if (this == ALWAYS_AFTER_BACKUP) return "always_after_backup";
            if (this == DAILY) return "daily";
            if (this == WEEKLY) return "weekly";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VmppArchiveTargetType {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * No target config
         */
        NONE,
        /**
         * CIFS target config
         */
        CIFS,
        /**
         * NFS target config
         */
        NFS;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == NONE) return "none";
            if (this == CIFS) return "cifs";
            if (this == NFS) return "nfs";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VbdMode {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * only read-only access will be allowed
         */
        RO,
        /**
         * read-write access will be allowed
         */
        RW;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == RO) return "RO";
            if (this == RW) return "RW";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum Ipv6ConfigurationMode {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Do not acquire an IPv6 address
         */
        NONE,
        /**
         * Acquire an IPv6 address by DHCP
         */
        DHCP,
        /**
         * Static IPv6 address configuration
         */
        STATIC,
        /**
         * Router assigned prefix delegation IPv6 allocation
         */
        AUTOCONF;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == NONE) return "None";
            if (this == DHCP) return "DHCP";
            if (this == STATIC) return "Static";
            if (this == AUTOCONF) return "Autoconf";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VbdType {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * VBD will appear to guest as CD
         */
        CD,
        /**
         * VBD will appear to guest as disk
         */
        DISK;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == CD) return "CD";
            if (this == DISK) return "Disk";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum OnBoot {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * When a VM containing this VDI is started, the contents of the VDI are reset to the state they were in when this flag was last set.
         */
        RESET,
        /**
         * Standard behaviour.
         */
        PERSIST;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == RESET) return "reset";
            if (this == PERSIST) return "persist";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VmApplianceOperation {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Start
         */
        START,
        /**
         * Clean shutdown
         */
        CLEAN_SHUTDOWN,
        /**
         * Hard shutdown
         */
        HARD_SHUTDOWN,
        /**
         * Shutdown
         */
        SHUTDOWN;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == START) return "start";
            if (this == CLEAN_SHUTDOWN) return "clean_shutdown";
            if (this == HARD_SHUTDOWN) return "hard_shutdown";
            if (this == SHUTDOWN) return "shutdown";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VbdOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Attempting to attach this VBD to a VM
         */
        ATTACH,
        /**
         * Attempting to eject the media from this VBD
         */
        EJECT,
        /**
         * Attempting to insert new media into this VBD
         */
        INSERT,
        /**
         * Attempting to hotplug this VBD
         */
        PLUG,
        /**
         * Attempting to hot unplug this VBD
         */
        UNPLUG,
        /**
         * Attempting to forcibly unplug this VBD
         */
        UNPLUG_FORCE,
        /**
         * Attempting to pause a block device backend
         */
        PAUSE,
        /**
         * Attempting to unpause a block device backend
         */
        UNPAUSE;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == ATTACH) return "attach";
            if (this == EJECT) return "eject";
            if (this == INSERT) return "insert";
            if (this == PLUG) return "plug";
            if (this == UNPLUG) return "unplug";
            if (this == UNPLUG_FORCE) return "unplug_force";
            if (this == PAUSE) return "pause";
            if (this == UNPAUSE) return "unpause";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VmppBackupFrequency {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Hourly backups
         */
        HOURLY,
        /**
         * Daily backups
         */
        DAILY,
        /**
         * Weekly backups
         */
        WEEKLY;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == HOURLY) return "hourly";
            if (this == DAILY) return "daily";
            if (this == WEEKLY) return "weekly";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum NetworkDefaultLockingMode {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Treat all VIFs on this network with locking_mode = 'default' as if they have locking_mode = 'unlocked'
         */
        UNLOCKED,
        /**
         * Treat all VIFs on this network with locking_mode = 'default' as if they have locking_mode = 'disabled'
         */
        DISABLED;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == UNLOCKED) return "unlocked";
            if (this == DISABLED) return "disabled";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VmPowerState {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * VM is offline and not using any resources
         */
        HALTED,
        /**
         * All resources have been allocated but the VM itself is paused and its vCPUs are not running
         */
        PAUSED,
        /**
         * Running
         */
        RUNNING,
        /**
         * VM state has been saved to disk and it is nolonger running. Note that disks remain in-use while the VM is suspended.
         */
        SUSPENDED;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == HALTED) return "Halted";
            if (this == PAUSED) return "Paused";
            if (this == RUNNING) return "Running";
            if (this == SUSPENDED) return "Suspended";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VmOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * refers to the operation "snapshot"
         */
        SNAPSHOT,
        /**
         * refers to the operation "clone"
         */
        CLONE,
        /**
         * refers to the operation "copy"
         */
        COPY,
        /**
         * refers to the operation "create_template"
         */
        CREATE_TEMPLATE,
        /**
         * refers to the operation "revert"
         */
        REVERT,
        /**
         * refers to the operation "checkpoint"
         */
        CHECKPOINT,
        /**
         * refers to the operation "snapshot_with_quiesce"
         */
        SNAPSHOT_WITH_QUIESCE,
        /**
         * refers to the operation "provision"
         */
        PROVISION,
        /**
         * refers to the operation "start"
         */
        START,
        /**
         * refers to the operation "start_on"
         */
        START_ON,
        /**
         * refers to the operation "pause"
         */
        PAUSE,
        /**
         * refers to the operation "unpause"
         */
        UNPAUSE,
        /**
         * refers to the operation "clean_shutdown"
         */
        CLEAN_SHUTDOWN,
        /**
         * refers to the operation "clean_reboot"
         */
        CLEAN_REBOOT,
        /**
         * refers to the operation "hard_shutdown"
         */
        HARD_SHUTDOWN,
        /**
         * refers to the operation "power_state_reset"
         */
        POWER_STATE_RESET,
        /**
         * refers to the operation "hard_reboot"
         */
        HARD_REBOOT,
        /**
         * refers to the operation "suspend"
         */
        SUSPEND,
        /**
         * refers to the operation "csvm"
         */
        CSVM,
        /**
         * refers to the operation "resume"
         */
        RESUME,
        /**
         * refers to the operation "resume_on"
         */
        RESUME_ON,
        /**
         * refers to the operation "pool_migrate"
         */
        POOL_MIGRATE,
        /**
         * refers to the operation "migrate_send"
         */
        MIGRATE_SEND,
        /**
         * refers to the operation "get_boot_record"
         */
        GET_BOOT_RECORD,
        /**
         * refers to the operation "send_sysrq"
         */
        SEND_SYSRQ,
        /**
         * refers to the operation "send_trigger"
         */
        SEND_TRIGGER,
        /**
         * refers to the operation "query_services"
         */
        QUERY_SERVICES,
        /**
         * Changing the memory settings
         */
        CHANGING_MEMORY_LIVE,
        /**
         * Waiting for the memory settings to change
         */
        AWAITING_MEMORY_LIVE,
        /**
         * Changing the memory dynamic range
         */
        CHANGING_DYNAMIC_RANGE,
        /**
         * Changing the memory static range
         */
        CHANGING_STATIC_RANGE,
        /**
         * Changing the memory limits
         */
        CHANGING_MEMORY_LIMITS,
        /**
         * Changing the shadow memory for a halted VM.
         */
        CHANGING_SHADOW_MEMORY,
        /**
         * Changing the shadow memory for a running VM.
         */
        CHANGING_SHADOW_MEMORY_LIVE,
        /**
         * Changing VCPU settings for a halted VM.
         */
        CHANGING_VCPUS,
        /**
         * Changing VCPU settings for a running VM.
         */
        CHANGING_VCPUS_LIVE,
        /**
         *
         */
        ASSERT_OPERATION_VALID,
        /**
         * Add, remove, query or list data sources
         */
        DATA_SOURCE_OP,
        /**
         *
         */
        UPDATE_ALLOWED_OPERATIONS,
        /**
         * Turning this VM into a template
         */
        MAKE_INTO_TEMPLATE,
        /**
         * importing a VM from a network stream
         */
        IMPORT,
        /**
         * exporting a VM to a network stream
         */
        EXPORT,
        /**
         * exporting VM metadata to a network stream
         */
        METADATA_EXPORT,
        /**
         * Reverting the VM to a previous snapshotted state
         */
        REVERTING,
        /**
         * refers to the act of uninstalling the VM
         */
        DESTROY;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == SNAPSHOT) return "snapshot";
            if (this == CLONE) return "clone";
            if (this == COPY) return "copy";
            if (this == CREATE_TEMPLATE) return "create_template";
            if (this == REVERT) return "revert";
            if (this == CHECKPOINT) return "checkpoint";
            if (this == SNAPSHOT_WITH_QUIESCE) return "snapshot_with_quiesce";
            if (this == PROVISION) return "provision";
            if (this == START) return "start";
            if (this == START_ON) return "start_on";
            if (this == PAUSE) return "pause";
            if (this == UNPAUSE) return "unpause";
            if (this == CLEAN_SHUTDOWN) return "clean_shutdown";
            if (this == CLEAN_REBOOT) return "clean_reboot";
            if (this == HARD_SHUTDOWN) return "hard_shutdown";
            if (this == POWER_STATE_RESET) return "power_state_reset";
            if (this == HARD_REBOOT) return "hard_reboot";
            if (this == SUSPEND) return "suspend";
            if (this == CSVM) return "csvm";
            if (this == RESUME) return "resume";
            if (this == RESUME_ON) return "resume_on";
            if (this == POOL_MIGRATE) return "pool_migrate";
            if (this == MIGRATE_SEND) return "migrate_send";
            if (this == GET_BOOT_RECORD) return "get_boot_record";
            if (this == SEND_SYSRQ) return "send_sysrq";
            if (this == SEND_TRIGGER) return "send_trigger";
            if (this == QUERY_SERVICES) return "query_services";
            if (this == CHANGING_MEMORY_LIVE) return "changing_memory_live";
            if (this == AWAITING_MEMORY_LIVE) return "awaiting_memory_live";
            if (this == CHANGING_DYNAMIC_RANGE) return "changing_dynamic_range";
            if (this == CHANGING_STATIC_RANGE) return "changing_static_range";
            if (this == CHANGING_MEMORY_LIMITS) return "changing_memory_limits";
            if (this == CHANGING_SHADOW_MEMORY) return "changing_shadow_memory";
            if (this == CHANGING_SHADOW_MEMORY_LIVE) return "changing_shadow_memory_live";
            if (this == CHANGING_VCPUS) return "changing_VCPUs";
            if (this == CHANGING_VCPUS_LIVE) return "changing_VCPUs_live";
            if (this == ASSERT_OPERATION_VALID) return "assert_operation_valid";
            if (this == DATA_SOURCE_OP) return "data_source_op";
            if (this == UPDATE_ALLOWED_OPERATIONS) return "update_allowed_operations";
            if (this == MAKE_INTO_TEMPLATE) return "make_into_template";
            if (this == IMPORT) return "import";
            if (this == EXPORT) return "export";
            if (this == METADATA_EXPORT) return "metadata_export";
            if (this == REVERTING) return "reverting";
            if (this == DESTROY) return "destroy";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum BondMode {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Source-level balancing
         */
        BALANCE_SLB,
        /**
         * Active/passive bonding: only one NIC is carrying traffic
         */
        ACTIVE_BACKUP,
        /**
         * Link aggregation control protocol
         */
        LACP;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == BALANCE_SLB) return "balance-slb";
            if (this == ACTIVE_BACKUP) return "active-backup";
            if (this == LACP) return "lacp";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum IpConfigurationMode {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Do not acquire an IP address
         */
        NONE,
        /**
         * Acquire an IP address by DHCP
         */
        DHCP,
        /**
         * Static IP address configuration
         */
        STATIC;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == NONE) return "None";
            if (this == DHCP) return "DHCP";
            if (this == STATIC) return "Static";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum StorageOperations {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * Scanning backends for new or deleted VDIs
         */
        SCAN,
        /**
         * Destroying the SR
         */
        DESTROY,
        /**
         * Forgetting about SR
         */
        FORGET,
        /**
         * Plugging a PBD into this SR
         */
        PLUG,
        /**
         * Unplugging a PBD from this SR
         */
        UNPLUG,
        /**
         * Refresh the fields on the SR
         */
        UPDATE,
        /**
         * Creating a new VDI
         */
        VDI_CREATE,
        /**
         * Introducing a new VDI
         */
        VDI_INTRODUCE,
        /**
         * Destroying a VDI
         */
        VDI_DESTROY,
        /**
         * Resizing a VDI
         */
        VDI_RESIZE,
        /**
         * Cloneing a VDI
         */
        VDI_CLONE,
        /**
         * Snapshotting a VDI
         */
        VDI_SNAPSHOT,
        /**
         * Creating a PBD for this SR
         */
        PBD_CREATE,
        /**
         * Destroying one of this SR's PBDs
         */
        PBD_DESTROY;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == SCAN) return "scan";
            if (this == DESTROY) return "destroy";
            if (this == FORGET) return "forget";
            if (this == PLUG) return "plug";
            if (this == UNPLUG) return "unplug";
            if (this == UPDATE) return "update";
            if (this == VDI_CREATE) return "vdi_create";
            if (this == VDI_INTRODUCE) return "vdi_introduce";
            if (this == VDI_DESTROY) return "vdi_destroy";
            if (this == VDI_RESIZE) return "vdi_resize";
            if (this == VDI_CLONE) return "vdi_clone";
            if (this == VDI_SNAPSHOT) return "vdi_snapshot";
            if (this == PBD_CREATE) return "pbd_create";
            if (this == PBD_DESTROY) return "pbd_destroy";
        /* This can never be reached */
        return "illegal enum";
        }

    };

    public enum VifLockingMode {
        /**
         * The value does not belong to this enumeration
         */
        UNRECOGNIZED,
        /**
         * No specific configuration set - default network policy applies
         */
        NETWORK_DEFAULT,
        /**
         * Only traffic to a specific MAC and a list of IPv4 or IPv6 addresses is permitted
         */
        LOCKED,
        /**
         * All traffic is permitted
         */
        UNLOCKED,
        /**
         * No traffic is permitted
         */
        DISABLED;
        public String toString() {
            if (this == UNRECOGNIZED) return "UNRECOGNIZED";
            if (this == NETWORK_DEFAULT) return "network_default";
            if (this == LOCKED) return "locked";
            if (this == UNLOCKED) return "unlocked";
            if (this == DISABLED) return "disabled";
        /* This can never be reached */
        return "illegal enum";
        }

    };


    /**
     * The restore could not be performed because a network interface is missing
     */
    public static class RestoreTargetMissingDevice extends XenAPIException {
        public final String device;

        /**
         * Create a new RestoreTargetMissingDevice
         *
         * @param device
         */
        public RestoreTargetMissingDevice(String device) {
            super("The restore could not be performed because a network interface is missing");
            this.device = device;
        }

    }

    /**
     * The communication with the WLB server timed out.
     */
    public static class WlbTimeout extends XenAPIException {
        public final String configuredTimeout;

        /**
         * Create a new WlbTimeout
         *
         * @param configuredTimeout
         */
        public WlbTimeout(String configuredTimeout) {
            super("The communication with the WLB server timed out.");
            this.configuredTimeout = configuredTimeout;
        }

    }

    /**
     * The MAC address specified doesn't exist on this host.
     */
    public static class MacDoesNotExist extends XenAPIException {
        public final String MAC;

        /**
         * Create a new MacDoesNotExist
         *
         * @param MAC
         */
        public MacDoesNotExist(String MAC) {
            super("The MAC address specified doesn't exist on this host.");
            this.MAC = MAC;
        }

    }

    /**
     * You gave an invalid object reference.  The object may have recently been deleted.  The class parameter gives the type of reference given, and the handle parameter echoes the bad value given.
     */
    public static class HandleInvalid extends XenAPIException {
        public final String clazz;
        public final String handle;

        /**
         * Create a new HandleInvalid
         *
         * @param clazz
         * @param handle
         */
        public HandleInvalid(String clazz, String handle) {
            super("You gave an invalid object reference.  The object may have recently been deleted.  The class parameter gives the type of reference given, and the handle parameter echoes the bad value given.");
            this.clazz = clazz;
            this.handle = handle;
        }

    }

    /**
     * The device is already attached to a VM
     */
    public static class DeviceAlreadyAttached extends XenAPIException {
        public final String device;

        /**
         * Create a new DeviceAlreadyAttached
         *
         * @param device
         */
        public DeviceAlreadyAttached(String device) {
            super("The device is already attached to a VM");
            this.device = device;
        }

    }

    /**
     * A required parameter contained an invalid IP address
     */
    public static class InvalidIpAddressSpecified extends XenAPIException {
        public final String parameter;

        /**
         * Create a new InvalidIpAddressSpecified
         *
         * @param parameter
         */
        public InvalidIpAddressSpecified(String parameter) {
            super("A required parameter contained an invalid IP address");
            this.parameter = parameter;
        }

    }

    /**
     * The SR operation cannot be performed because the SR is not empty.
     */
    public static class SrNotEmpty extends XenAPIException {

        /**
         * Create a new SrNotEmpty
         */
        public SrNotEmpty() {
            super("The SR operation cannot be performed because the SR is not empty.");
        }

    }

    /**
     * HVM is required for this operation
     */
    public static class VmHvmRequired extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmHvmRequired
         *
         * @param vm
         */
        public VmHvmRequired(String vm) {
            super("HVM is required for this operation");
            this.vm = vm;
        }

    }

    /**
     * The GPU group contains active PGPUs and cannot be deleted.
     */
    public static class GpuGroupContainsPgpu extends XenAPIException {
        public final String pgpus;

        /**
         * Create a new GpuGroupContainsPgpu
         *
         * @param pgpus
         */
        public GpuGroupContainsPgpu(String pgpus) {
            super("The GPU group contains active PGPUs and cannot be deleted.");
            this.pgpus = pgpus;
        }

    }

    /**
     * Operation cannot proceed while a tunnel exists on this interface.
     */
    public static class PifTunnelStillExists extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifTunnelStillExists
         *
         * @param PIF
         */
        public PifTunnelStillExists(String PIF) {
            super("Operation cannot proceed while a tunnel exists on this interface.");
            this.PIF = PIF;
        }

    }

    /**
     * A bond must consist of at least two member interfaces
     */
    public static class PifBondNeedsMoreMembers extends XenAPIException {

        /**
         * Create a new PifBondNeedsMoreMembers
         */
        public PifBondNeedsMoreMembers() {
            super("A bond must consist of at least two member interfaces");
        }

    }

    /**
     * This operation cannot be performed because the pif is bonded.
     */
    public static class PifAlreadyBonded extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifAlreadyBonded
         *
         * @param PIF
         */
        public PifAlreadyBonded(String PIF) {
            super("This operation cannot be performed because the pif is bonded.");
            this.PIF = PIF;
        }

    }

    /**
     * The disaster recovery task could not be cleanly destroyed.
     */
    public static class CannotDestroyDisasterRecoveryTask extends XenAPIException {
        public final String reason;

        /**
         * Create a new CannotDestroyDisasterRecoveryTask
         *
         * @param reason
         */
        public CannotDestroyDisasterRecoveryTask(String reason) {
            super("The disaster recovery task could not be cleanly destroyed.");
            this.reason = reason;
        }

    }

    /**
     * You tried to create a VLAN, but the tag you gave was invalid -- it must be between 0 and 4094.  The parameter echoes the VLAN tag you gave.
     */
    public static class VlanTagInvalid extends XenAPIException {
        public final String VLAN;

        /**
         * Create a new VlanTagInvalid
         *
         * @param VLAN
         */
        public VlanTagInvalid(String VLAN) {
            super("You tried to create a VLAN, but the tag you gave was invalid -- it must be between 0 and 4094.  The parameter echoes the VLAN tag you gave.");
            this.VLAN = VLAN;
        }

    }

    /**
     * You cannot make regular API calls directly on a slave. Please pass API calls via the master host.
     */
    public static class HostIsSlave extends XenAPIException {
        public final String masterIPAddress;

        /**
         * Create a new HostIsSlave
         *
         * @param masterIPAddress
         */
        public HostIsSlave(String masterIPAddress) {
            super("You cannot make regular API calls directly on a slave. Please pass API calls via the master host.");
            this.masterIPAddress = masterIPAddress;
        }

    }

    /**
     * The SR.shared flag cannot be set to false while the SR remains connected to multiple hosts
     */
    public static class SrHasMultiplePbds extends XenAPIException {
        public final String PBD;

        /**
         * Create a new SrHasMultiplePbds
         *
         * @param PBD
         */
        public SrHasMultiplePbds(String PBD) {
            super("The SR.shared flag cannot be set to false while the SR remains connected to multiple hosts");
            this.PBD = PBD;
        }

    }

    /**
     * The pool failed to enable external authentication.
     */
    public static class PoolAuthEnableFailedInvalidOu extends XenAPIException {
        public final String host;
        public final String message;

        /**
         * Create a new PoolAuthEnableFailedInvalidOu
         *
         * @param host
         * @param message
         */
        public PoolAuthEnableFailedInvalidOu(String host, String message) {
            super("The pool failed to enable external authentication.");
            this.host = host;
            this.message = message;
        }

    }

    /**
     * Some data checksums were incorrect; the VM may be corrupt.
     */
    public static class ImportErrorSomeChecksumsFailed extends XenAPIException {

        /**
         * Create a new ImportErrorSomeChecksumsFailed
         */
        public ImportErrorSomeChecksumsFailed() {
            super("Some data checksums were incorrect; the VM may be corrupt.");
        }

    }

    /**
     * This operation needs the OpenVSwitch networking backend to be enabled on all hosts in the pool.
     */
    public static class OpenvswitchNotActive extends XenAPIException {

        /**
         * Create a new OpenvswitchNotActive
         */
        public OpenvswitchNotActive() {
            super("This operation needs the OpenVSwitch networking backend to be enabled on all hosts in the pool.");
        }

    }

    /**
     * The backup partition to stream the updat to cannot be found
     */
    public static class CannotFindOemBackupPartition extends XenAPIException {

        /**
         * Create a new CannotFindOemBackupPartition
         */
        public CannotFindOemBackupPartition() {
            super("The backup partition to stream the updat to cannot be found");
        }

    }

    /**
     * The specified device was not found.
     */
    public static class PifDeviceNotFound extends XenAPIException {

        /**
         * Create a new PifDeviceNotFound
         */
        public PifDeviceNotFound() {
            super("The specified device was not found.");
        }

    }

    /**
     * An internal error generated by the domain builder.
     */
    public static class DomainBuilderError extends XenAPIException {
        public final String function;
        public final String code;
        public final String message;

        /**
         * Create a new DomainBuilderError
         *
         * @param function
         * @param code
         * @param message
         */
        public DomainBuilderError(String function, String code, String message) {
            super("An internal error generated by the domain builder.");
            this.function = function;
            this.code = code;
            this.message = message;
        }

    }

    /**
     * The patch precheck stage failed: there are one or more VMs still running on the server.  All VMs must be suspended before the patch can be applied.
     */
    public static class PatchPrecheckFailedVmRunning extends XenAPIException {
        public final String patch;

        /**
         * Create a new PatchPrecheckFailedVmRunning
         *
         * @param patch
         */
        public PatchPrecheckFailedVmRunning(String patch) {
            super("The patch precheck stage failed: there are one or more VMs still running on the server.  All VMs must be suspended before the patch can be applied.");
            this.patch = patch;
        }

    }

    /**
     * You attempted to run a VM on a host which doesn't have I/O virtualisation (IOMMU/VT-d) enabled, which is needed by the VM.
     */
    public static class VmRequiresIommu extends XenAPIException {
        public final String host;

        /**
         * Create a new VmRequiresIommu
         *
         * @param host
         */
        public VmRequiresIommu(String host) {
            super("You attempted to run a VM on a host which doesn't have I/O virtualisation (IOMMU/VT-d) enabled, which is needed by the VM.");
            this.host = host;
        }

    }

    /**
     * The operation failed because the HA software on the specified host could not see a subset of other hosts. Check your network connectivity.
     */
    public static class HaHostCannotSeePeers extends XenAPIException {
        public final String host;
        public final String all;
        public final String subset;

        /**
         * Create a new HaHostCannotSeePeers
         *
         * @param host
         * @param all
         * @param subset
         */
        public HaHostCannotSeePeers(String host, String all, String subset) {
            super("The operation failed because the HA software on the specified host could not see a subset of other hosts. Check your network connectivity.");
            this.host = host;
            this.all = all;
            this.subset = subset;
        }

    }

    /**
     * The pool failed to disable the external authentication of at least one host.
     */
    public static class PoolAuthDisableFailedPermissionDenied extends XenAPIException {
        public final String host;
        public final String message;

        /**
         * Create a new PoolAuthDisableFailedPermissionDenied
         *
         * @param host
         * @param message
         */
        public PoolAuthDisableFailedPermissionDenied(String host, String message) {
            super("The pool failed to disable the external authentication of at least one host.");
            this.host = host;
            this.message = message;
        }

    }

    /**
     * Caller not allowed to perform this operation.
     */
    public static class PermissionDenied extends XenAPIException {
        public final String message;

        /**
         * Create a new PermissionDenied
         *
         * @param message
         */
        public PermissionDenied(String message) {
            super("Caller not allowed to perform this operation.");
            this.message = message;
        }

    }

    /**
     * The remote system's SSL certificate failed to verify against our certificate library.
     */
    public static class SslVerifyError extends XenAPIException {
        public final String reason;

        /**
         * Create a new SslVerifyError
         *
         * @param reason
         */
        public SslVerifyError(String reason) {
            super("The remote system's SSL certificate failed to verify against our certificate library.");
            this.reason = reason;
        }

    }

    /**
     * Attaching this SR failed.
     */
    public static class SrAttachFailed extends XenAPIException {
        public final String sr;

        /**
         * Create a new SrAttachFailed
         *
         * @param sr
         */
        public SrAttachFailed(String sr) {
            super("Attaching this SR failed.");
            this.sr = sr;
        }

    }

    /**
     * Subject already exists.
     */
    public static class SubjectAlreadyExists extends XenAPIException {

        /**
         * Create a new SubjectAlreadyExists
         */
        public SubjectAlreadyExists() {
            super("Subject already exists.");
        }

    }

    /**
     * This host lost access to the HA statefile.
     */
    public static class HaLostStatefile extends XenAPIException {

        /**
         * Create a new HaLostStatefile
         */
        public HaLostStatefile() {
            super("This host lost access to the HA statefile.");
        }

    }

    /**
     * The operation could not be performed because HA is not enabled on the Pool
     */
    public static class HaNotEnabled extends XenAPIException {

        /**
         * Create a new HaNotEnabled
         */
        public HaNotEnabled() {
            super("The operation could not be performed because HA is not enabled on the Pool");
        }

    }

    /**
     * The host could not join the liveset because the HA daemon failed to start.
     */
    public static class HaHeartbeatDaemonStartupFailed extends XenAPIException {

        /**
         * Create a new HaHeartbeatDaemonStartupFailed
         */
        public HaHeartbeatDaemonStartupFailed() {
            super("The host could not join the liveset because the HA daemon failed to start.");
        }

    }

    /**
     * This session is not registered to receive events.  You must call event.register before event.next.  The session handle you are using is echoed.
     */
    public static class SessionNotRegistered extends XenAPIException {
        public final String handle;

        /**
         * Create a new SessionNotRegistered
         *
         * @param handle
         */
        public SessionNotRegistered(String handle) {
            super("This session is not registered to receive events.  You must call event.register before event.next.  The session handle you are using is echoed.");
            this.handle = handle;
        }

    }

    /**
     * This VM does not have a suspend SR specified.
     */
    public static class VmNoSuspendSr extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmNoSuspendSr
         *
         * @param vm
         */
        public VmNoSuspendSr(String vm) {
            super("This VM does not have a suspend SR specified.");
            this.vm = vm;
        }

    }

    /**
     * You attempted to migrate a VM with more than one snapshot.
     */
    public static class VmHasTooManySnapshots extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmHasTooManySnapshots
         *
         * @param vm
         */
        public VmHasTooManySnapshots(String vm) {
            super("You attempted to migrate a VM with more than one snapshot.");
            this.vm = vm;
        }

    }

    /**
     * The patch apply failed.  Please see attached output.
     */
    public static class PatchApplyFailed extends XenAPIException {
        public final String output;

        /**
         * Create a new PatchApplyFailed
         *
         * @param output
         */
        public PatchApplyFailed(String output) {
            super("The patch apply failed.  Please see attached output.");
            this.output = output;
        }

    }

    /**
     * The operation required write access but this VDI is read-only
     */
    public static class VdiReadonly extends XenAPIException {
        public final String vdi;

        /**
         * Create a new VdiReadonly
         *
         * @param vdi
         */
        public VdiReadonly(String vdi) {
            super("The operation required write access but this VDI is read-only");
            this.vdi = vdi;
        }

    }

    /**
     * The SR is full. Requested new size exceeds the maximum size
     */
    public static class SrFull extends XenAPIException {
        public final String requested;
        public final String maximum;

        /**
         * Create a new SrFull
         *
         * @param requested
         * @param maximum
         */
        public SrFull(String requested, String maximum) {
            super("The SR is full. Requested new size exceeds the maximum size");
            this.requested = requested;
            this.maximum = maximum;
        }

    }

    /**
     * You attempted to run a VM on a host which doesn't have a pGPU available in the GPU group needed by the VM. The VM has a vGPU attached to this GPU group.
     */
    public static class VmRequiresGpu extends XenAPIException {
        public final String vm;
        public final String GPUGroup;

        /**
         * Create a new VmRequiresGpu
         *
         * @param vm
         * @param GPUGroup
         */
        public VmRequiresGpu(String vm, String GPUGroup) {
            super("You attempted to run a VM on a host which doesn't have a pGPU available in the GPU group needed by the VM. The VM has a vGPU attached to this GPU group.");
            this.vm = vm;
            this.GPUGroup = GPUGroup;
        }

    }

    /**
     * This operation cannot be performed because this VDI could not be properly attached to the VM.
     */
    public static class VdiNotAvailable extends XenAPIException {
        public final String vdi;

        /**
         * Create a new VdiNotAvailable
         *
         * @param vdi
         */
        public VdiNotAvailable(String vdi) {
            super("This operation cannot be performed because this VDI could not be properly attached to the VM.");
            this.vdi = vdi;
        }

    }

    /**
     * The server failed to unmarshal the XMLRPC message; it was expecting one element and received something else.
     */
    public static class XmlrpcUnmarshalFailure extends XenAPIException {
        public final String expected;
        public final String received;

        /**
         * Create a new XmlrpcUnmarshalFailure
         *
         * @param expected
         * @param received
         */
        public XmlrpcUnmarshalFailure(String expected, String received) {
            super("The server failed to unmarshal the XMLRPC message; it was expecting one element and received something else.");
            this.expected = expected;
            this.received = received;
        }

    }

    /**
     * A CRL already exists with the specified name.
     */
    public static class CrlAlreadyExists extends XenAPIException {
        public final String name;

        /**
         * Create a new CrlAlreadyExists
         *
         * @param name
         */
        public CrlAlreadyExists(String name) {
            super("A CRL already exists with the specified name.");
            this.name = name;
        }

    }

    /**
     * The master reports that it cannot talk back to the slave on the supplied management IP address.
     */
    public static class HostMasterCannotTalkBack extends XenAPIException {
        public final String ip;

        /**
         * Create a new HostMasterCannotTalkBack
         *
         * @param ip
         */
        public HostMasterCannotTalkBack(String ip) {
            super("The master reports that it cannot talk back to the slave on the supplied management IP address.");
            this.ip = ip;
        }

    }

    /**
     * 3rd party xapi hook failed
     */
    public static class XapiHookFailed extends XenAPIException {
        public final String hookName;
        public final String reason;
        public final String stdout;
        public final String exitCode;

        /**
         * Create a new XapiHookFailed
         *
         * @param hookName
         * @param reason
         * @param stdout
         * @param exitCode
         */
        public XapiHookFailed(String hookName, String reason, String stdout, String exitCode) {
            super("3rd party xapi hook failed");
            this.hookName = hookName;
            this.reason = reason;
            this.stdout = stdout;
            this.exitCode = exitCode;
        }

    }

    /**
     * The import failed because this export has been created by a different (incompatible) product version
     */
    public static class ImportIncompatibleVersion extends XenAPIException {

        /**
         * Create a new ImportIncompatibleVersion
         */
        public ImportIncompatibleVersion() {
            super("The import failed because this export has been created by a different (incompatible) product version");
        }

    }

    /**
     * The requested bootloader is unknown
     */
    public static class UnknownBootloader extends XenAPIException {
        public final String vm;
        public final String bootloader;

        /**
         * Create a new UnknownBootloader
         *
         * @param vm
         * @param bootloader
         */
        public UnknownBootloader(String vm, String bootloader) {
            super("The requested bootloader is unknown");
            this.vm = vm;
            this.bootloader = bootloader;
        }

    }

    /**
     * The Citrix XenServer Vss Provider is not loaded
     */
    public static class XenVssReqErrorProvNotLoaded extends XenAPIException {
        public final String vm;
        public final String errorCode;

        /**
         * Create a new XenVssReqErrorProvNotLoaded
         *
         * @param vm
         * @param errorCode
         */
        public XenVssReqErrorProvNotLoaded(String vm, String errorCode) {
            super("The Citrix XenServer Vss Provider is not loaded");
            this.vm = vm;
            this.errorCode = errorCode;
        }

    }

    /**
     * The VM is set up to use a feature that requires it to boot as HVM.
     */
    public static class FeatureRequiresHvm extends XenAPIException {
        public final String details;

        /**
         * Create a new FeatureRequiresHvm
         *
         * @param details
         */
        public FeatureRequiresHvm(String details) {
            super("The VM is set up to use a feature that requires it to boot as HVM.");
            this.details = details;
        }

    }

    /**
     * The operation could not proceed because necessary VDIs were already locked at the storage level.
     */
    public static class SrVdiLockingFailed extends XenAPIException {

        /**
         * Create a new SrVdiLockingFailed
         */
        public SrVdiLockingFailed() {
            super("The operation could not proceed because necessary VDIs were already locked at the storage level.");
        }

    }

    /**
     * You tried to destroy a PIF, but it represents an aspect of the physical host configuration, and so cannot be destroyed.  The parameter echoes the PIF handle you gave.
     */
    public static class PifIsPhysical extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifIsPhysical
         *
         * @param PIF
         */
        public PifIsPhysical(String PIF) {
            super("You tried to destroy a PIF, but it represents an aspect of the physical host configuration, and so cannot be destroyed.  The parameter echoes the PIF handle you gave.");
            this.PIF = PIF;
        }

    }

    /**
     * You tried to add a key-value pair to a map, but that key is already there.
     */
    public static class MapDuplicateKey extends XenAPIException {
        public final String type;
        public final String paramName;
        public final String uuid;
        public final String key;

        /**
         * Create a new MapDuplicateKey
         *
         * @param type
         * @param paramName
         * @param uuid
         * @param key
         */
        public MapDuplicateKey(String type, String paramName, String uuid, String key) {
            super("You tried to add a key-value pair to a map, but that key is already there.");
            this.type = type;
            this.paramName = paramName;
            this.uuid = uuid;
            this.key = key;
        }

    }

    /**
     * The license-server connection details (address or port) were missing or incomplete.
     */
    public static class MissingConnectionDetails extends XenAPIException {

        /**
         * Create a new MissingConnectionDetails
         */
        public MissingConnectionDetails() {
            super("The license-server connection details (address or port) were missing or incomplete.");
        }

    }

    /**
     * Could not create the XML string generated by the transportable snapshot
     */
    public static class XenVssReqErrorCreatingSnapshotXmlString extends XenAPIException {
        public final String vm;
        public final String errorCode;

        /**
         * Create a new XenVssReqErrorCreatingSnapshotXmlString
         *
         * @param vm
         * @param errorCode
         */
        public XenVssReqErrorCreatingSnapshotXmlString(String vm, String errorCode) {
            super("Could not create the XML string generated by the transportable snapshot");
            this.vm = vm;
            this.errorCode = errorCode;
        }

    }

    /**
     * The bootloader returned an error
     */
    public static class BootloaderFailed extends XenAPIException {
        public final String vm;
        public final String msg;

        /**
         * Create a new BootloaderFailed
         *
         * @param vm
         * @param msg
         */
        public BootloaderFailed(String vm, String msg) {
            super("The bootloader returned an error");
            this.vm = vm;
            this.msg = msg;
        }

    }

    /**
     * The WLB server reported that XenServer said something to it that WLB wasn't expecting or didn't understand.
     */
    public static class WlbXenserverMalformedResponse extends XenAPIException {

        /**
         * Create a new WlbXenserverMalformedResponse
         */
        public WlbXenserverMalformedResponse() {
            super("The WLB server reported that XenServer said something to it that WLB wasn't expecting or didn't understand.");
        }

    }

    /**
     * The GPU group contains active VGPUs and cannot be deleted.
     */
    public static class GpuGroupContainsVgpu extends XenAPIException {
        public final String vgpus;

        /**
         * Create a new GpuGroupContainsVgpu
         *
         * @param vgpus
         */
        public GpuGroupContainsVgpu(String vgpus) {
            super("The GPU group contains active VGPUs and cannot be deleted.");
            this.vgpus = vgpus;
        }

    }

    /**
     * The pool failed to enable external authentication.
     */
    public static class PoolAuthEnableFailedDuplicateHostname extends XenAPIException {
        public final String host;
        public final String message;

        /**
         * Create a new PoolAuthEnableFailedDuplicateHostname
         *
         * @param host
         * @param message
         */
        public PoolAuthEnableFailedDuplicateHostname(String host, String message) {
            super("The pool failed to enable external authentication.");
            this.host = host;
            this.message = message;
        }

    }

    /**
     * Retrieving system status from the host failed.  A diagnostic reason suitable for support organisations is also returned.
     */
    public static class SystemStatusRetrievalFailed extends XenAPIException {
        public final String reason;

        /**
         * Create a new SystemStatusRetrievalFailed
         *
         * @param reason
         */
        public SystemStatusRetrievalFailed(String reason) {
            super("Retrieving system status from the host failed.  A diagnostic reason suitable for support organisations is also returned.");
            this.reason = reason;
        }

    }

    /**
     * This operation cannot be performed because this VDI is in use by some other operation
     */
    public static class VdiInUse extends XenAPIException {
        public final String vdi;
        public final String operation;

        /**
         * Create a new VdiInUse
         *
         * @param vdi
         * @param operation
         */
        public VdiInUse(String vdi, String operation) {
            super("This operation cannot be performed because this VDI is in use by some other operation");
            this.vdi = vdi;
            this.operation = operation;
        }

    }

    /**
     * This operation cannot be completed as the host is not live.
     */
    public static class HostNotLive extends XenAPIException {

        /**
         * Create a new HostNotLive
         */
        public HostNotLive() {
            super("This operation cannot be completed as the host is not live.");
        }

    }

    /**
     * A certificate already exists with the specified name.
     */
    public static class CertificateAlreadyExists extends XenAPIException {
        public final String name;

        /**
         * Create a new CertificateAlreadyExists
         *
         * @param name
         */
        public CertificateAlreadyExists(String name) {
            super("A certificate already exists with the specified name.");
            this.name = name;
        }

    }

    /**
     * The SR has no attached PBDs
     */
    public static class SrHasNoPbds extends XenAPIException {
        public final String sr;

        /**
         * Create a new SrHasNoPbds
         *
         * @param sr
         */
        public SrHasNoPbds(String sr) {
            super("The SR has no attached PBDs");
            this.sr = sr;
        }

    }

    /**
     * This PIF is a bond slave and cannot have a tunnel on it.
     */
    public static class CannotAddTunnelToBondSlave extends XenAPIException {
        public final String PIF;

        /**
         * Create a new CannotAddTunnelToBondSlave
         *
         * @param PIF
         */
        public CannotAddTunnelToBondSlave(String PIF) {
            super("This PIF is a bond slave and cannot have a tunnel on it.");
            this.PIF = PIF;
        }

    }

    /**
     * The uploaded patch file is invalid
     */
    public static class InvalidPatch extends XenAPIException {

        /**
         * Create a new InvalidPatch
         */
        public InvalidPatch() {
            super("The uploaded patch file is invalid");
        }

    }

    /**
     * The SR could not be destroyed, as the 'indestructible' flag was set on it.
     */
    public static class SrIndestructible extends XenAPIException {
        public final String sr;

        /**
         * Create a new SrIndestructible
         *
         * @param sr
         */
        public SrIndestructible(String sr) {
            super("The SR could not be destroyed, as the 'indestructible' flag was set on it.");
            this.sr = sr;
        }

    }

    /**
     * This host cannot accept the proposed new master setting at this time.
     */
    public static class HaAbortNewMaster extends XenAPIException {
        public final String reason;

        /**
         * Create a new HaAbortNewMaster
         *
         * @param reason
         */
        public HaAbortNewMaster(String reason) {
            super("This host cannot accept the proposed new master setting at this time.");
            this.reason = reason;
        }

    }

    /**
     * The WLB server said something that XenServer wasn't expecting or didn't understand.  The method called on the WLB server, a diagnostic reason, and the response from WLB are returned.
     */
    public static class WlbMalformedResponse extends XenAPIException {
        public final String method;
        public final String reason;
        public final String response;

        /**
         * Create a new WlbMalformedResponse
         *
         * @param method
         * @param reason
         * @param response
         */
        public WlbMalformedResponse(String method, String reason, String response) {
            super("The WLB server said something that XenServer wasn't expecting or didn't understand.  The method called on the WLB server, a diagnostic reason, and the response from WLB are returned.");
            this.method = method;
            this.reason = reason;
            this.response = response;
        }

    }

    /**
     * The host joining the pool must have a physical management NIC (i.e. the management NIC must not be on a VLAN or bonded PIF).
     */
    public static class PoolJoiningHostMustHavePhysicalManagementNic extends XenAPIException {

        /**
         * Create a new PoolJoiningHostMustHavePhysicalManagementNic
         */
        public PoolJoiningHostMustHavePhysicalManagementNic() {
            super("The host joining the pool must have a physical management NIC (i.e. the management NIC must not be on a VLAN or bonded PIF).");
        }

    }

    /**
     * PIF has no IPv6 configuration (mode curently set to 'none')
     */
    public static class PifHasNoV6NetworkConfiguration extends XenAPIException {

        /**
         * Create a new PifHasNoV6NetworkConfiguration
         */
        public PifHasNoV6NetworkConfiguration() {
            super("PIF has no IPv6 configuration (mode curently set to 'none')");
        }

    }

    /**
     * This operation is not allowed as the VM is part of an appliance.
     */
    public static class VmIsPartOfAnAppliance extends XenAPIException {
        public final String vm;
        public final String appliance;

        /**
         * Create a new VmIsPartOfAnAppliance
         *
         * @param vm
         * @param appliance
         */
        public VmIsPartOfAnAppliance(String vm, String appliance) {
            super("This operation is not allowed as the VM is part of an appliance.");
            this.vm = vm;
            this.appliance = appliance;
        }

    }

    /**
     * The WLB server reported that XenServer rejected its configured authentication details.
     */
    public static class WlbXenserverAuthenticationFailed extends XenAPIException {

        /**
         * Create a new WlbXenserverAuthenticationFailed
         */
        public WlbXenserverAuthenticationFailed() {
            super("The WLB server reported that XenServer rejected its configured authentication details.");
        }

    }

    /**
     * The power-state of a control domain cannot be reset.
     */
    public static class CannotResetControlDomain extends XenAPIException {
        public final String vm;

        /**
         * Create a new CannotResetControlDomain
         *
         * @param vm
         */
        public CannotResetControlDomain(String vm) {
            super("The power-state of a control domain cannot be reset.");
            this.vm = vm;
        }

    }

    /**
     * The patch precheck stage failed with an unknown error.  See attached info for more details.
     */
    public static class PatchPrecheckFailedUnknownError extends XenAPIException {
        public final String patch;
        public final String info;

        /**
         * Create a new PatchPrecheckFailedUnknownError
         *
         * @param patch
         * @param info
         */
        public PatchPrecheckFailedUnknownError(String patch, String info) {
            super("The patch precheck stage failed with an unknown error.  See attached info for more details.");
            this.patch = patch;
            this.info = info;
        }

    }

    /**
     * Host cannot attach network (in the case of NIC bonding, this may be because attaching the network on this host would require other networks [that are currently active] to be taken down).
     */
    public static class HostCannotAttachNetwork extends XenAPIException {
        public final String host;
        public final String network;

        /**
         * Create a new HostCannotAttachNetwork
         *
         * @param host
         * @param network
         */
        public HostCannotAttachNetwork(String host, String network) {
            super("Host cannot attach network (in the case of NIC bonding, this may be because attaching the network on this host would require other networks [that are currently active] to be taken down).");
            this.host = host;
            this.network = network;
        }

    }

    /**
     * The WLB URL is invalid. Ensure it is in format: <ipaddress>:<port>.  The configured/given URL is returned.
     */
    public static class WlbUrlInvalid extends XenAPIException {
        public final String url;

        /**
         * Create a new WlbUrlInvalid
         *
         * @param url
         */
        public WlbUrlInvalid(String url) {
            super("The WLB URL is invalid. Ensure it is in format: <ipaddress>:<port>.  The configured/given URL is returned.");
            this.url = url;
        }

    }

    /**
     * Cannot restore this VM because it would create a duplicate
     */
    public static class DuplicateVm extends XenAPIException {
        public final String vm;

        /**
         * Create a new DuplicateVm
         *
         * @param vm
         */
        public DuplicateVm(String vm) {
            super("Cannot restore this VM because it would create a duplicate");
            this.vm = vm;
        }

    }

    /**
     * The pool master host cannot be removed.
     */
    public static class HostCannotDestroySelf extends XenAPIException {
        public final String host;

        /**
         * Create a new HostCannotDestroySelf
         *
         * @param host
         */
        public HostCannotDestroySelf(String host) {
            super("The pool master host cannot be removed.");
            this.host = host;
        }

    }

    /**
     * This host failed in the middle of an automatic failover operation and needs to retry the failover action
     */
    public static class HostBroken extends XenAPIException {

        /**
         * Create a new HostBroken
         */
        public HostBroken() {
            super("This host failed in the middle of an automatic failover operation and needs to retry the failover action");
        }

    }

    /**
     * An error occured while restoring the memory image of the specified virtual machine
     */
    public static class VmCheckpointResumeFailed extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmCheckpointResumeFailed
         *
         * @param vm
         */
        public VmCheckpointResumeFailed(String vm) {
            super("An error occured while restoring the memory image of the specified virtual machine");
            this.vm = vm;
        }

    }

    /**
     * Too many VCPUs to start this VM
     */
    public static class VmTooManyVcpus extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmTooManyVcpus
         *
         * @param vm
         */
        public VmTooManyVcpus(String vm) {
            super("Too many VCPUs to start this VM");
            this.vm = vm;
        }

    }

    /**
     * This operation cannot be completed as the host is still live.
     */
    public static class HostIsLive extends XenAPIException {
        public final String host;

        /**
         * Create a new HostIsLive
         *
         * @param host
         */
        public HostIsLive(String host) {
            super("This operation cannot be completed as the host is still live.");
            this.host = host;
        }

    }

    /**
     * The VM could not be imported because attached disks could not be found.
     */
    public static class ImportErrorAttachedDisksNotFound extends XenAPIException {

        /**
         * Create a new ImportErrorAttachedDisksNotFound
         */
        public ImportErrorAttachedDisksNotFound() {
            super("The VM could not be imported because attached disks could not be found.");
        }

    }

    /**
     * Drive could not be hot-unplugged because it is not marked as unpluggable
     */
    public static class VbdNotUnpluggable extends XenAPIException {
        public final String vbd;

        /**
         * Create a new VbdNotUnpluggable
         *
         * @param vbd
         */
        public VbdNotUnpluggable(String vbd) {
            super("Drive could not be hot-unplugged because it is not marked as unpluggable");
            this.vbd = vbd;
        }

    }

    /**
     * An attempt to create the snapshots failed
     */
    public static class XenVssReqErrorCreatingSnapshot extends XenAPIException {
        public final String vm;
        public final String errorCode;

        /**
         * Create a new XenVssReqErrorCreatingSnapshot
         *
         * @param vm
         * @param errorCode
         */
        public XenVssReqErrorCreatingSnapshot(String vm, String errorCode) {
            super("An attempt to create the snapshots failed");
            this.vm = vm;
            this.errorCode = errorCode;
        }

    }

    /**
     * Could not enable redo log.
     */
    public static class CannotEnableRedoLog extends XenAPIException {
        public final String reason;

        /**
         * Create a new CannotEnableRedoLog
         *
         * @param reason
         */
        public CannotEnableRedoLog(String reason) {
            super("Could not enable redo log.");
            this.reason = reason;
        }

    }

    /**
     * This host cannot be evacuated.
     */
    public static class CannotEvacuateHost extends XenAPIException {
        public final String errors;

        /**
         * Create a new CannotEvacuateHost
         *
         * @param errors
         */
        public CannotEvacuateHost(String errors) {
            super("This host cannot be evacuated.");
            this.errors = errors;
        }

    }

    /**
     * There were no hosts available to complete the specified operation.
     */
    public static class NoHostsAvailable extends XenAPIException {

        /**
         * Create a new NoHostsAvailable
         */
        public NoHostsAvailable() {
            super("There were no hosts available to complete the specified operation.");
        }

    }

    /**
     * A timeout happened while attempting to attach a device to a VM.
     */
    public static class DeviceAttachTimeout extends XenAPIException {
        public final String type;
        public final String ref;

        /**
         * Create a new DeviceAttachTimeout
         *
         * @param type
         * @param ref
         */
        public DeviceAttachTimeout(String type, String ref) {
            super("A timeout happened while attempting to attach a device to a VM.");
            this.type = type;
            this.ref = ref;
        }

    }

    /**
     * The device name is invalid
     */
    public static class InvalidDevice extends XenAPIException {
        public final String device;

        /**
         * Create a new InvalidDevice
         *
         * @param device
         */
        public InvalidDevice(String device) {
            super("The device name is invalid");
            this.device = device;
        }

    }

    /**
     * A PBD already exists connecting the SR to the host
     */
    public static class PbdExists extends XenAPIException {
        public final String sr;
        public final String host;
        public final String pbd;

        /**
         * Create a new PbdExists
         *
         * @param sr
         * @param host
         * @param pbd
         */
        public PbdExists(String sr, String host, String pbd) {
            super("A PBD already exists connecting the SR to the host");
            this.sr = sr;
            this.host = host;
            this.pbd = pbd;
        }

    }

    /**
     * The WLB server reported that XenServer refused it a connection (even though we're connecting perfectly fine in the other direction).
     */
    public static class WlbXenserverConnectionRefused extends XenAPIException {

        /**
         * Create a new WlbXenserverConnectionRefused
         */
        public WlbXenserverConnectionRefused() {
            super("The WLB server reported that XenServer refused it a connection (even though we're connecting perfectly fine in the other direction).");
        }

    }

    /**
     * The metrics of this host could not be read.
     */
    public static class HostCannotReadMetrics extends XenAPIException {

        /**
         * Create a new HostCannotReadMetrics
         */
        public HostCannotReadMetrics() {
            super("The metrics of this host could not be read.");
        }

    }

    /**
     * The VM is incompatible with the CPU features of this host.
     */
    public static class VmIncompatibleWithThisHost extends XenAPIException {
        public final String vm;
        public final String host;
        public final String reason;

        /**
         * Create a new VmIncompatibleWithThisHost
         *
         * @param vm
         * @param host
         * @param reason
         */
        public VmIncompatibleWithThisHost(String vm, String host, String reason) {
            super("The VM is incompatible with the CPU features of this host.");
            this.vm = vm;
            this.host = host;
            this.reason = reason;
        }

    }

    /**
     * The upper limit of active redo log instances was reached.
     */
    public static class NoMoreRedoLogsAllowed extends XenAPIException {

        /**
         * Create a new NoMoreRedoLogsAllowed
         */
        public NoMoreRedoLogsAllowed() {
            super("The upper limit of active redo log instances was reached.");
        }

    }

    /**
     * The pool failed to enable external authentication.
     */
    public static class PoolAuthEnableFailed extends XenAPIException {
        public final String host;
        public final String message;

        /**
         * Create a new PoolAuthEnableFailed
         *
         * @param host
         * @param message
         */
        public PoolAuthEnableFailed(String host, String message) {
            super("The pool failed to enable external authentication.");
            this.host = host;
            this.message = message;
        }

    }

    /**
     * The VSS plug-in is not installed on this virtual machine
     */
    public static class VmSnapshotWithQuiesceNotSupported extends XenAPIException {
        public final String vm;
        public final String error;

        /**
         * Create a new VmSnapshotWithQuiesceNotSupported
         *
         * @param vm
         * @param error
         */
        public VmSnapshotWithQuiesceNotSupported(String vm, String error) {
            super("The VSS plug-in is not installed on this virtual machine");
            this.vm = vm;
            this.error = error;
        }

    }

    /**
     * This host cannot join a pool because it's license does not support pooling
     */
    public static class LicenseDoesNotSupportPooling extends XenAPIException {

        /**
         * Create a new LicenseDoesNotSupportPooling
         */
        public LicenseDoesNotSupportPooling() {
            super("This host cannot join a pool because it's license does not support pooling");
        }

    }

    /**
     * The master says the host is not known to it. Perhaps the Host was deleted from the master's database? Perhaps the slave is pointing to the wrong master?
     */
    public static class HostUnknownToMaster extends XenAPIException {
        public final String host;

        /**
         * Create a new HostUnknownToMaster
         *
         * @param host
         */
        public HostUnknownToMaster(String host) {
            super("The master says the host is not known to it. Perhaps the Host was deleted from the master's database? Perhaps the slave is pointing to the wrong master?");
            this.host = host;
        }

    }

    /**
     * The WLB server refused a connection to XenServer.
     */
    public static class WlbConnectionRefused extends XenAPIException {

        /**
         * Create a new WlbConnectionRefused
         */
        public WlbConnectionRefused() {
            super("The WLB server refused a connection to XenServer.");
        }

    }

    /**
     * The VSS plug-in cannot be contacted
     */
    public static class VmSnapshotWithQuiescePluginDeosNotRespond extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmSnapshotWithQuiescePluginDeosNotRespond
         *
         * @param vm
         */
        public VmSnapshotWithQuiescePluginDeosNotRespond(String vm) {
            super("The VSS plug-in cannot be contacted");
            this.vm = vm;
        }

    }

    /**
     * You attempted to run a VM on a host which doesn't have access to an SR needed by the VM. The VM has at least one VBD attached to a VDI in the SR.
     */
    public static class VmRequiresSr extends XenAPIException {
        public final String vm;
        public final String sr;

        /**
         * Create a new VmRequiresSr
         *
         * @param vm
         * @param sr
         */
        public VmRequiresSr(String vm, String sr) {
            super("You attempted to run a VM on a host which doesn't have access to an SR needed by the VM. The VM has at least one VBD attached to a VDI in the SR.");
            this.vm = vm;
            this.sr = sr;
        }

    }

    /**
     * This VM does not have a crashdump SR specified.
     */
    public static class VmNoCrashdumpSr extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmNoCrashdumpSr
         *
         * @param vm
         */
        public VmNoCrashdumpSr(String vm) {
            super("This VM does not have a crashdump SR specified.");
            this.vm = vm;
        }

    }

    /**
     * The operation could not be performed because the HA software is not installed on this host.
     */
    public static class HaNotInstalled extends XenAPIException {
        public final String host;

        /**
         * Create a new HaNotInstalled
         *
         * @param host
         */
        public HaNotInstalled(String host) {
            super("The operation could not be performed because the HA software is not installed on this host.");
            this.host = host;
        }

    }

    /**
     * A PIF with this specified device name already exists.
     */
    public static class DuplicatePifDeviceName extends XenAPIException {
        public final String device;

        /**
         * Create a new DuplicatePifDeviceName
         *
         * @param device
         */
        public DuplicatePifDeviceName(String device) {
            super("A PIF with this specified device name already exists.");
            this.device = device;
        }

    }

    /**
     * You attempted an operation on a VM that was not in an appropriate power state at the time; for example, you attempted to start a VM that was already running.  The parameters returned are the VM's handle, and the expected and actual VM state at the time of the call.
     */
    public static class VmBadPowerState extends XenAPIException {
        public final String vm;
        public final String expected;
        public final String actual;

        /**
         * Create a new VmBadPowerState
         *
         * @param vm
         * @param expected
         * @param actual
         */
        public VmBadPowerState(String vm, String expected, String actual) {
            super("You attempted an operation on a VM that was not in an appropriate power state at the time; for example, you attempted to start a VM that was already running.  The parameters returned are the VM's handle, and the expected and actual VM state at the time of the call.");
            this.vm = vm;
            this.expected = expected;
            this.actual = actual;
        }

    }

    /**
     * This pool has wlb-enabled set to false.
     */
    public static class WlbDisabled extends XenAPIException {

        /**
         * Create a new WlbDisabled
         */
        public WlbDisabled() {
            super("This pool has wlb-enabled set to false.");
        }

    }

    /**
     * This VM operation cannot be performed on an older-versioned host during an upgrade.
     */
    public static class VmHostIncompatibleVersion extends XenAPIException {
        public final String host;
        public final String vm;

        /**
         * Create a new VmHostIncompatibleVersion
         *
         * @param host
         * @param vm
         */
        public VmHostIncompatibleVersion(String host, String vm) {
            super("This VM operation cannot be performed on an older-versioned host during an upgrade.");
            this.host = host;
            this.vm = vm;
        }

    }

    /**
     * Cannot join pool whose external authentication configuration is different.
     */
    public static class PoolJoiningExternalAuthMismatch extends XenAPIException {

        /**
         * Create a new PoolJoiningExternalAuthMismatch
         */
        public PoolJoiningExternalAuthMismatch() {
            super("Cannot join pool whose external authentication configuration is different.");
        }

    }

    /**
     * All VBDs of type 'disk' must be read/write for HVM guests
     */
    public static class DiskVbdMustBeReadwriteForHvm extends XenAPIException {
        public final String vbd;

        /**
         * Create a new DiskVbdMustBeReadwriteForHvm
         *
         * @param vbd
         */
        public DiskVbdMustBeReadwriteForHvm(String vbd) {
            super("All VBDs of type 'disk' must be read/write for HVM guests");
            this.vbd = vbd;
        }

    }

    /**
     * The BIOS strings for this VM have already been set and cannot be changed anymore.
     */
    public static class VmBiosStringsAlreadySet extends XenAPIException {

        /**
         * Create a new VmBiosStringsAlreadySet
         */
        public VmBiosStringsAlreadySet() {
            super("The BIOS strings for this VM have already been set and cannot be changed anymore.");
        }

    }

    /**
     * The WLB server reported that its configured server name for this XenServer instance failed to resolve in DNS.
     */
    public static class WlbXenserverUnknownHost extends XenAPIException {

        /**
         * Create a new WlbXenserverUnknownHost
         */
        public WlbXenserverUnknownHost() {
            super("The WLB server reported that its configured server name for this XenServer instance failed to resolve in DNS.");
        }

    }

    /**
     * The host could not join the liveset because the HA daemon could not access the heartbeat disk.
     */
    public static class HaHostCannotAccessStatefile extends XenAPIException {

        /**
         * Create a new HaHostCannotAccessStatefile
         */
        public HaHostCannotAccessStatefile() {
            super("The host could not join the liveset because the HA daemon could not access the heartbeat disk.");
        }

    }

    /**
     * VM didn't acknowledge the need to shutdown.
     */
    public static class VmFailedShutdownAcknowledgment extends XenAPIException {

        /**
         * Create a new VmFailedShutdownAcknowledgment
         */
        public VmFailedShutdownAcknowledgment() {
            super("VM didn't acknowledge the need to shutdown.");
        }

    }

    /**
     * Error querying the external directory service.
     */
    public static class AuthServiceError extends XenAPIException {
        public final String message;

        /**
         * Create a new AuthServiceError
         *
         * @param message
         */
        public AuthServiceError(String message) {
            super("Error querying the external directory service.");
            this.message = message;
        }

    }

    /**
     * Cannot perform operation as the host is running in emergency mode.
     */
    public static class HostInEmergencyMode extends XenAPIException {

        /**
         * Create a new HostInEmergencyMode
         */
        public HostInEmergencyMode() {
            super("Cannot perform operation as the host is running in emergency mode.");
        }

    }

    /**
     * The specified host is disabled and cannot be re-enabled until after it has rebooted.
     */
    public static class HostDisabledUntilReboot extends XenAPIException {
        public final String host;

        /**
         * Create a new HostDisabledUntilReboot
         *
         * @param host
         */
        public HostDisabledUntilReboot(String host) {
            super("The specified host is disabled and cannot be re-enabled until after it has rebooted.");
            this.host = host;
        }

    }

    /**
     * The default SR reference does not point to a valid SR
     */
    public static class DefaultSrNotFound extends XenAPIException {
        public final String sr;

        /**
         * Create a new DefaultSrNotFound
         *
         * @param sr
         */
        public DefaultSrNotFound(String sr) {
            super("The default SR reference does not point to a valid SR");
            this.sr = sr;
        }

    }

    /**
     * A device with the name given already exists on the selected VM
     */
    public static class DeviceAlreadyExists extends XenAPIException {
        public final String device;

        /**
         * Create a new DeviceAlreadyExists
         *
         * @param device
         */
        public DeviceAlreadyExists(String device) {
            super("A device with the name given already exists on the selected VM");
            this.device = device;
        }

    }

    /**
     * The PBD could not be plugged because the SR is in use by another host and is not marked as sharable.
     */
    public static class SrNotSharable extends XenAPIException {
        public final String sr;
        public final String host;

        /**
         * Create a new SrNotSharable
         *
         * @param sr
         * @param host
         */
        public SrNotSharable(String sr, String host) {
            super("The PBD could not be plugged because the SR is in use by another host and is not marked as sharable.");
            this.sr = sr;
            this.host = host;
        }

    }

    /**
     * You attempted to migrate a VM which has a checkpoint.
     */
    public static class VmHasCheckpoint extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmHasCheckpoint
         *
         * @param vm
         */
        public VmHasCheckpoint(String vm) {
            super("You attempted to migrate a VM which has a checkpoint.");
            this.vm = vm;
        }

    }

    /**
     * The SM plugin did not respond to a query.
     */
    public static class SmPluginCommunicationFailure extends XenAPIException {
        public final String sm;

        /**
         * Create a new SmPluginCommunicationFailure
         *
         * @param sm
         */
        public SmPluginCommunicationFailure(String sm) {
            super("The SM plugin did not respond to a query.");
            this.sm = sm;
        }

    }

    /**
     * This VM is assigned to a protection policy.
     */
    public static class VmAssignedToProtectionPolicy extends XenAPIException {
        public final String vm;
        public final String vmpp;

        /**
         * Create a new VmAssignedToProtectionPolicy
         *
         * @param vm
         * @param vmpp
         */
        public VmAssignedToProtectionPolicy(String vm, String vmpp) {
            super("This VM is assigned to a protection policy.");
            this.vm = vm;
            this.vmpp = vmpp;
        }

    }

    /**
     * RBAC permission denied.
     */
    public static class RbacPermissionDenied extends XenAPIException {
        public final String permission;
        public final String message;

        /**
         * Create a new RbacPermissionDenied
         *
         * @param permission
         * @param message
         */
        public RbacPermissionDenied(String permission, String message) {
            super("RBAC permission denied.");
            this.permission = permission;
            this.message = message;
        }

    }

    /**
     * The host failed to disable external authentication.
     */
    public static class AuthDisableFailedPermissionDenied extends XenAPIException {
        public final String message;

        /**
         * Create a new AuthDisableFailedPermissionDenied
         *
         * @param message
         */
        public AuthDisableFailedPermissionDenied(String message) {
            super("The host failed to disable external authentication.");
            this.message = message;
        }

    }

    /**
     * Cannot downgrade license while in pool. Please disband the pool first, then downgrade licenses on hosts separately.
     */
    public static class LicenseCannotDowngradeWhileInPool extends XenAPIException {

        /**
         * Create a new LicenseCannotDowngradeWhileInPool
         */
        public LicenseCannotDowngradeWhileInPool() {
            super("Cannot downgrade license while in pool. Please disband the pool first, then downgrade licenses on hosts separately.");
        }

    }

    /**
     * The request was rejected because there are too many pending tasks on the server.
     */
    public static class TooManyPendingTasks extends XenAPIException {

        /**
         * Create a new TooManyPendingTasks
         */
        public TooManyPendingTasks() {
            super("The request was rejected because there are too many pending tasks on the server.");
        }

    }

    /**
     * The VSS plug-in has timed out
     */
    public static class VmSnapshotWithQuiesceTimeout extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmSnapshotWithQuiesceTimeout
         *
         * @param vm
         */
        public VmSnapshotWithQuiesceTimeout(String vm) {
            super("The VSS plug-in has timed out");
            this.vm = vm;
        }

    }

    /**
     * This operation cannot be performed because creating or deleting a bond involving the management interface is not allowed while HA is on. In order to do that, disable HA, create or delete the bond then re-enable HA.
     */
    public static class HaCannotChangeBondStatusOfMgmtIface extends XenAPIException {

        /**
         * Create a new HaCannotChangeBondStatusOfMgmtIface
         */
        public HaCannotChangeBondStatusOfMgmtIface() {
            super("This operation cannot be performed because creating or deleting a bond involving the management interface is not allowed while HA is on. In order to do that, disable HA, create or delete the bond then re-enable HA.");
        }

    }

    /**
     * This patch has already been applied
     */
    public static class PatchAlreadyApplied extends XenAPIException {
        public final String patch;

        /**
         * Create a new PatchAlreadyApplied
         *
         * @param patch
         */
        public PatchAlreadyApplied(String patch) {
            super("This patch has already been applied");
            this.patch = patch;
        }

    }

    /**
     * An SR with that uuid already exists.
     */
    public static class SrUuidExists extends XenAPIException {
        public final String uuid;

        /**
         * Create a new SrUuidExists
         *
         * @param uuid
         */
        public SrUuidExists(String uuid) {
            super("An SR with that uuid already exists.");
            this.uuid = uuid;
        }

    }

    /**
     * The host failed to enable external authentication.
     */
    public static class AuthEnableFailedDomainLookupFailed extends XenAPIException {
        public final String message;

        /**
         * Create a new AuthEnableFailedDomainLookupFailed
         *
         * @param message
         */
        public AuthEnableFailedDomainLookupFailed(String message) {
            super("The host failed to enable external authentication.");
            this.message = message;
        }

    }

    /**
     * The patch precheck stage failed: the server is of an incorrect build.
     */
    public static class PatchPrecheckFailedWrongServerBuild extends XenAPIException {
        public final String patch;
        public final String foundBuild;
        public final String requiredBuild;

        /**
         * Create a new PatchPrecheckFailedWrongServerBuild
         *
         * @param patch
         * @param foundBuild
         * @param requiredBuild
         */
        public PatchPrecheckFailedWrongServerBuild(String patch, String foundBuild, String requiredBuild) {
            super("The patch precheck stage failed: the server is of an incorrect build.");
            this.patch = patch;
            this.foundBuild = foundBuild;
            this.requiredBuild = requiredBuild;
        }

    }

    /**
     * The given feature string is not valid.
     */
    public static class InvalidFeatureString extends XenAPIException {
        public final String details;

        /**
         * Create a new InvalidFeatureString
         *
         * @param details
         */
        public InvalidFeatureString(String details) {
            super("The given feature string is not valid.");
            this.details = details;
        }

    }

    /**
     * No WLB connection is configured.
     */
    public static class WlbNotInitialized extends XenAPIException {

        /**
         * Create a new WlbNotInitialized
         */
        public WlbNotInitialized() {
            super("No WLB connection is configured.");
        }

    }

    /**
     * You attempted an operation that was explicitly blocked (see the blocked_operations field of the given object).
     */
    public static class OperationBlocked extends XenAPIException {
        public final String ref;
        public final String code;

        /**
         * Create a new OperationBlocked
         *
         * @param ref
         * @param code
         */
        public OperationBlocked(String ref, String code) {
            super("You attempted an operation that was explicitly blocked (see the blocked_operations field of the given object).");
            this.ref = ref;
            this.code = code;
        }

    }

    /**
     * The provision call can only be invoked on templates, not regular VMs.
     */
    public static class ProvisionOnlyAllowedOnTemplate extends XenAPIException {

        /**
         * Create a new ProvisionOnlyAllowedOnTemplate
         */
        public ProvisionOnlyAllowedOnTemplate() {
            super("The provision call can only be invoked on templates, not regular VMs.");
        }

    }

    /**
     * VM failed to shutdown before the timeout expired
     */
    public static class VmShutdownTimeout extends XenAPIException {
        public final String vm;
        public final String timeout;

        /**
         * Create a new VmShutdownTimeout
         *
         * @param vm
         * @param timeout
         */
        public VmShutdownTimeout(String vm, String timeout) {
            super("VM failed to shutdown before the timeout expired");
            this.vm = vm;
            this.timeout = timeout;
        }

    }

    /**
     * Role already exists.
     */
    public static class RoleAlreadyExists extends XenAPIException {

        /**
         * Create a new RoleAlreadyExists
         */
        public RoleAlreadyExists() {
            super("Role already exists.");
        }

    }

    /**
     * The network contains active PIFs and cannot be deleted.
     */
    public static class NetworkContainsPif extends XenAPIException {
        public final String pifs;

        /**
         * Create a new NetworkContainsPif
         *
         * @param pifs
         */
        public NetworkContainsPif(String pifs) {
            super("The network contains active PIFs and cannot be deleted.");
            this.pifs = pifs;
        }

    }

    /**
     * Could not find a network interface with the specified device name and MAC address.
     */
    public static class CouldNotFindNetworkInterfaceWithSpecifiedDeviceNameAndMacAddress extends XenAPIException {
        public final String device;
        public final String mac;

        /**
         * Create a new CouldNotFindNetworkInterfaceWithSpecifiedDeviceNameAndMacAddress
         *
         * @param device
         * @param mac
         */
        public CouldNotFindNetworkInterfaceWithSpecifiedDeviceNameAndMacAddress(String device, String mac) {
            super("Could not find a network interface with the specified device name and MAC address.");
            this.device = device;
            this.mac = mac;
        }

    }

    /**
     * There was an error connecting to the host. the service contacted didn't reply properly.
     */
    public static class JoiningHostServiceFailed extends XenAPIException {

        /**
         * Create a new JoiningHostServiceFailed
         */
        public JoiningHostServiceFailed() {
            super("There was an error connecting to the host. the service contacted didn't reply properly.");
        }

    }

    /**
     * This operation cannot be performed because the specified VDI could not be found on the storage substrate
     */
    public static class VdiMissing extends XenAPIException {
        public final String sr;
        public final String vdi;

        /**
         * Create a new VdiMissing
         *
         * @param sr
         * @param vdi
         */
        public VdiMissing(String sr, String vdi) {
            super("This operation cannot be performed because the specified VDI could not be found on the storage substrate");
            this.sr = sr;
            this.vdi = vdi;
        }

    }

    /**
     * This VM has locked the DVD drive tray, so the disk cannot be ejected
     */
    public static class VbdTrayLocked extends XenAPIException {
        public final String vbd;

        /**
         * Create a new VbdTrayLocked
         *
         * @param vbd
         */
        public VbdTrayLocked(String vbd) {
            super("This VM has locked the DVD drive tray, so the disk cannot be ejected");
            this.vbd = vbd;
        }

    }

    /**
     * The pool failed to enable external authentication.
     */
    public static class PoolAuthEnableFailedPermissionDenied extends XenAPIException {
        public final String host;
        public final String message;

        /**
         * Create a new PoolAuthEnableFailedPermissionDenied
         *
         * @param host
         * @param message
         */
        public PoolAuthEnableFailedPermissionDenied(String host, String message) {
            super("The pool failed to enable external authentication.");
            this.host = host;
            this.message = message;
        }

    }

    /**
     * The uuid you supplied was invalid.
     */
    public static class UuidInvalid extends XenAPIException {
        public final String type;
        public final String uuid;

        /**
         * Create a new UuidInvalid
         *
         * @param type
         * @param uuid
         */
        public UuidInvalid(String type, String uuid) {
            super("The uuid you supplied was invalid.");
            this.type = type;
            this.uuid = uuid;
        }

    }

    /**
     * This operation is not allowed under your license.  Please contact your support representative.
     */
    public static class LicenceRestriction extends XenAPIException {

        /**
         * Create a new LicenceRestriction
         */
        public LicenceRestriction() {
            super("This operation is not allowed under your license.  Please contact your support representative.");
        }

    }

    /**
     * Network has active VIFs
     */
    public static class VifInUse extends XenAPIException {
        public final String network;
        public final String VIF;

        /**
         * Create a new VifInUse
         *
         * @param network
         * @param VIF
         */
        public VifInUse(String network, String VIF) {
            super("Network has active VIFs");
            this.network = network;
            this.VIF = VIF;
        }

    }

    /**
     * This command is only allowed on the OEM edition.
     */
    public static class OnlyAllowedOnOemEdition extends XenAPIException {
        public final String command;

        /**
         * Create a new OnlyAllowedOnOemEdition
         *
         * @param command
         */
        public OnlyAllowedOnOemEdition(String command) {
            super("This command is only allowed on the OEM edition.");
            this.command = command;
        }

    }

    /**
     * The operation cannot be performed on physical device
     */
    public static class VdiIsAPhysicalDevice extends XenAPIException {
        public final String vdi;

        /**
         * Create a new VdiIsAPhysicalDevice
         *
         * @param vdi
         */
        public VdiIsAPhysicalDevice(String vdi) {
            super("The operation cannot be performed on physical device");
            this.vdi = vdi;
        }

    }

    /**
     * There was an error processing your license.  Please contact your support representative.
     */
    public static class LicenseProcessingError extends XenAPIException {

        /**
         * Create a new LicenseProcessingError
         */
        public LicenseProcessingError() {
            super("There was an error processing your license.  Please contact your support representative.");
        }

    }

    /**
     * The specified VBD device is not recognised: please use a non-negative integer
     */
    public static class IllegalVbdDevice extends XenAPIException {
        public final String vbd;
        public final String device;

        /**
         * Create a new IllegalVbdDevice
         *
         * @param vbd
         * @param device
         */
        public IllegalVbdDevice(String vbd, String device) {
            super("The specified VBD device is not recognised: please use a non-negative integer");
            this.vbd = vbd;
            this.device = device;
        }

    }

    /**
     * The specified CRL does not exist.
     */
    public static class CrlDoesNotExist extends XenAPIException {
        public final String name;

        /**
         * Create a new CrlDoesNotExist
         *
         * @param name
         */
        public CrlDoesNotExist(String name) {
            super("The specified CRL does not exist.");
            this.name = name;
        }

    }

    /**
     * The request was asynchronously cancelled.
     */
    public static class TaskCancelled extends XenAPIException {
        public final String task;

        /**
         * Create a new TaskCancelled
         *
         * @param task
         */
        public TaskCancelled(String task) {
            super("The request was asynchronously cancelled.");
            this.task = task;
        }

    }

    /**
     * The VM crashed
     */
    public static class VmCrashed extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmCrashed
         *
         * @param vm
         */
        public VmCrashed(String vm) {
            super("The VM crashed");
            this.vm = vm;
        }

    }

    /**
     * The pool failed to enable external authentication.
     */
    public static class PoolAuthEnableFailedDomainLookupFailed extends XenAPIException {
        public final String host;
        public final String message;

        /**
         * Create a new PoolAuthEnableFailedDomainLookupFailed
         *
         * @param host
         * @param message
         */
        public PoolAuthEnableFailedDomainLookupFailed(String host, String message) {
            super("The pool failed to enable external authentication.");
            this.host = host;
            this.message = message;
        }

    }

    /**
     * Host cannot rejoin pool because it should have fenced (it is not in the master's partition)
     */
    public static class HaShouldBeFenced extends XenAPIException {
        public final String host;

        /**
         * Create a new HaShouldBeFenced
         *
         * @param host
         */
        public HaShouldBeFenced(String host) {
            super("Host cannot rejoin pool because it should have fenced (it is not in the master's partition)");
            this.host = host;
        }

    }

    /**
     * You attempted an operation on a VM that was judged to be unsafe by the server. This can happen if the VM would run on a CPU that has a potentially incompatible set of feature flags to those the VM requires. If you want to override this warning then use the 'force' option.
     */
    public static class VmUnsafeBoot extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmUnsafeBoot
         *
         * @param vm
         */
        public VmUnsafeBoot(String vm) {
            super("You attempted an operation on a VM that was judged to be unsafe by the server. This can happen if the VM would run on a CPU that has a potentially incompatible set of feature flags to those the VM requires. If you want to override this warning then use the 'force' option.");
            this.vm = vm;
        }

    }

    /**
     * PIF has no IP configuration (mode curently set to 'none')
     */
    public static class PifHasNoNetworkConfiguration extends XenAPIException {

        /**
         * Create a new PifHasNoNetworkConfiguration
         */
        public PifHasNoNetworkConfiguration() {
            super("PIF has no IP configuration (mode curently set to 'none')");
        }

    }

    /**
     * The request was rejected because the server is too busy.
     */
    public static class TooBusy extends XenAPIException {

        /**
         * Create a new TooBusy
         */
        public TooBusy() {
            super("The request was rejected because the server is too busy.");
        }

    }

    /**
     * You attempted to set a value that is not supported by this implementation.  The fully-qualified field name and the value that you tried to set are returned.  Also returned is a developer-only diagnostic reason.
     */
    public static class ValueNotSupported extends XenAPIException {
        public final String field;
        public final String value;
        public final String reason;

        /**
         * Create a new ValueNotSupported
         *
         * @param field
         * @param value
         * @param reason
         */
        public ValueNotSupported(String field, String value, String reason) {
            super("You attempted to set a value that is not supported by this implementation.  The fully-qualified field name and the value that you tried to set are returned.  Also returned is a developer-only diagnostic reason.");
            this.field = field;
            this.value = value;
            this.reason = reason;
        }

    }

    /**
     * You gave an invalid session reference.  It may have been invalidated by a server restart, or timed out.  You should get a new session handle, using one of the session.login_ calls.  This error does not invalidate the current connection.  The handle parameter echoes the bad value given.
     */
    public static class SessionInvalid extends XenAPIException {
        public final String handle;

        /**
         * Create a new SessionInvalid
         *
         * @param handle
         */
        public SessionInvalid(String handle) {
            super("You gave an invalid session reference.  It may have been invalidated by a server restart, or timed out.  You should get a new session handle, using one of the session.login_ calls.  This error does not invalidate the current connection.  The handle parameter echoes the bad value given.");
            this.handle = handle;
        }

    }

    /**
     * This operation cannot be performed because the referenced network is not properly shared. The network must either be entirely virtual or must be physically present via a currently_attached PIF on every host.
     */
    public static class HaConstraintViolationNetworkNotShared extends XenAPIException {
        public final String network;

        /**
         * Create a new HaConstraintViolationNetworkNotShared
         *
         * @param network
         */
        public HaConstraintViolationNetworkNotShared(String network) {
            super("This operation cannot be performed because the referenced network is not properly shared. The network must either be entirely virtual or must be physically present via a currently_attached PIF on every host.");
            this.network = network;
        }

    }

    /**
     * HA could not be enabled on the Pool because a liveset could not be formed: check storage and network heartbeat paths.
     */
    public static class HaFailedToFormLiveset extends XenAPIException {

        /**
         * Create a new HaFailedToFormLiveset
         */
        public HaFailedToFormLiveset() {
            super("HA could not be enabled on the Pool because a liveset could not be formed: check storage and network heartbeat paths.");
        }

    }

    /**
     * You cannot bond interfaces across different hosts.
     */
    public static class PifCannotBondCrossHost extends XenAPIException {

        /**
         * Create a new PifCannotBondCrossHost
         */
        public PifCannotBondCrossHost() {
            super("You cannot bond interfaces across different hosts.");
        }

    }

    /**
     * The event.from token could not be parsed. Valid values include: '', and a value returned from a previous event.from call.
     */
    public static class EventFromTokenParseFailure extends XenAPIException {
        public final String token;

        /**
         * Create a new EventFromTokenParseFailure
         *
         * @param token
         */
        public EventFromTokenParseFailure(String token) {
            super("The event.from token could not be parsed. Valid values include: '', and a value returned from a previous event.from call.");
            this.token = token;
        }

    }

    /**
     * The operation cannot be performed until the SR has been upgraded
     */
    public static class SrRequiresUpgrade extends XenAPIException {
        public final String SR;

        /**
         * Create a new SrRequiresUpgrade
         *
         * @param SR
         */
        public SrRequiresUpgrade(String SR) {
            super("The operation cannot be performed until the SR has been upgraded");
            this.SR = SR;
        }

    }

    /**
     * The specified certificate does not exist.
     */
    public static class CertificateDoesNotExist extends XenAPIException {
        public final String name;

        /**
         * Create a new CertificateDoesNotExist
         *
         * @param name
         */
        public CertificateDoesNotExist(String name) {
            super("The specified certificate does not exist.");
            this.name = name;
        }

    }

    /**
     * This operation cannot be performed because it would invalidate VM failover planning such that the system would be unable to guarantee to restart protected VMs after a Host failure.
     */
    public static class HaOperationWouldBreakFailoverPlan extends XenAPIException {

        /**
         * Create a new HaOperationWouldBreakFailoverPlan
         */
        public HaOperationWouldBreakFailoverPlan() {
            super("This operation cannot be performed because it would invalidate VM failover planning such that the system would be unable to guarantee to restart protected VMs after a Host failure.");
        }

    }

    /**
     * The requested update could to be obtained from the master.
     */
    public static class CannotFetchPatch extends XenAPIException {
        public final String uuid;

        /**
         * Create a new CannotFetchPatch
         *
         * @param uuid
         */
        public CannotFetchPatch(String uuid) {
            super("The requested update could to be obtained from the master.");
            this.uuid = uuid;
        }

    }

    /**
     * The requested update could not be found.  This can occur when you designate a new master or xe patch-clean.  Please upload the update again
     */
    public static class CannotFindPatch extends XenAPIException {

        /**
         * Create a new CannotFindPatch
         */
        public CannotFindPatch() {
            super("The requested update could not be found.  This can occur when you designate a new master or xe patch-clean.  Please upload the update again");
        }

    }

    /**
     * You attempted an operation which would have resulted in duplicate keys in the database.
     */
    public static class DbUniquenessConstraintViolation extends XenAPIException {
        public final String table;
        public final String field;
        public final String value;

        /**
         * Create a new DbUniquenessConstraintViolation
         *
         * @param table
         * @param field
         * @param value
         */
        public DbUniquenessConstraintViolation(String table, String field, String value) {
            super("You attempted an operation which would have resulted in duplicate keys in the database.");
            this.table = table;
            this.field = field;
            this.value = value;
        }

    }

    /**
     * You attempted to run a VM on a host which doesn't have a PIF on a Network needed by the VM. The VM has at least one VIF attached to the Network.
     */
    public static class VmRequiresNetwork extends XenAPIException {
        public final String vm;
        public final String network;

        /**
         * Create a new VmRequiresNetwork
         *
         * @param vm
         * @param network
         */
        public VmRequiresNetwork(String vm, String network) {
            super("You attempted to run a VM on a host which doesn't have a PIF on a Network needed by the VM. The VM has at least one VIF attached to the Network.");
            this.vm = vm;
            this.network = network;
        }

    }

    /**
     * Operation could not be performed because the drive is not empty
     */
    public static class VbdNotEmpty extends XenAPIException {
        public final String vbd;

        /**
         * Create a new VbdNotEmpty
         *
         * @param vbd
         */
        public VbdNotEmpty(String vbd) {
            super("Operation could not be performed because the drive is not empty");
            this.vbd = vbd;
        }

    }

    /**
     * Not enough host memory is available to perform this operation
     */
    public static class HostNotEnoughFreeMemory extends XenAPIException {
        public final String needed;
        public final String available;

        /**
         * Create a new HostNotEnoughFreeMemory
         *
         * @param needed
         * @param available
         */
        public HostNotEnoughFreeMemory(String needed, String available) {
            super("Not enough host memory is available to perform this operation");
            this.needed = needed;
            this.available = available;
        }

    }

    /**
     * An error occurred during the migration process.
     */
    public static class VmMigrateFailed extends XenAPIException {
        public final String vm;
        public final String source;
        public final String destination;
        public final String msg;

        /**
         * Create a new VmMigrateFailed
         *
         * @param vm
         * @param source
         * @param destination
         * @param msg
         */
        public VmMigrateFailed(String vm, String source, String destination, String msg) {
            super("An error occurred during the migration process.");
            this.vm = vm;
            this.source = source;
            this.destination = destination;
            this.msg = msg;
        }

    }

    /**
     * The SR backend does not support the operation (check the SR's allowed operations)
     */
    public static class SrOperationNotSupported extends XenAPIException {
        public final String sr;

        /**
         * Create a new SrOperationNotSupported
         *
         * @param sr
         */
        public SrOperationNotSupported(String sr) {
            super("The SR backend does not support the operation (check the SR's allowed operations)");
            this.sr = sr;
        }

    }

    /**
     * The operation could not be performed because the VBD was not connected to the VM.
     */
    public static class DeviceNotAttached extends XenAPIException {
        public final String VBD;

        /**
         * Create a new DeviceNotAttached
         *
         * @param VBD
         */
        public DeviceNotAttached(String VBD) {
            super("The operation could not be performed because the VBD was not connected to the VM.");
            this.VBD = VBD;
        }

    }

    /**
     * The specified host is disabled.
     */
    public static class HostDisabled extends XenAPIException {
        public final String host;

        /**
         * Create a new HostDisabled
         *
         * @param host
         */
        public HostDisabled(String host) {
            super("The specified host is disabled.");
            this.host = host;
        }

    }

    /**
     * You must use tar output to retrieve system status from an OEM host.
     */
    public static class SystemStatusMustUseTarOnOem extends XenAPIException {

        /**
         * Create a new SystemStatusMustUseTarOnOem
         */
        public SystemStatusMustUseTarOnOem() {
            super("You must use tar output to retrieve system status from an OEM host.");
        }

    }

    /**
     * An attempt to prepare VSS writers for the snapshot failed
     */
    public static class XenVssReqErrorPreparingWriters extends XenAPIException {
        public final String vm;
        public final String errorCode;

        /**
         * Create a new XenVssReqErrorPreparingWriters
         *
         * @param vm
         * @param errorCode
         */
        public XenVssReqErrorPreparingWriters(String vm, String errorCode) {
            super("An attempt to prepare VSS writers for the snapshot failed");
            this.vm = vm;
            this.errorCode = errorCode;
        }

    }

    /**
     * The host failed to enable external authentication.
     */
    public static class AuthEnableFailed extends XenAPIException {
        public final String message;

        /**
         * Create a new AuthEnableFailed
         *
         * @param message
         */
        public AuthEnableFailed(String message) {
            super("The host failed to enable external authentication.");
            this.message = message;
        }

    }

    /**
     * The host joining the pool cannot contain any shared storage.
     */
    public static class JoiningHostCannotContainSharedSrs extends XenAPIException {

        /**
         * Create a new JoiningHostCannotContainSharedSrs
         */
        public JoiningHostCannotContainSharedSrs() {
            super("The host joining the pool cannot contain any shared storage.");
        }

    }

    /**
     * You need at least 1 VCPU to start a VM
     */
    public static class VmNoVcpus extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmNoVcpus
         *
         * @param vm
         */
        public VmNoVcpus(String vm) {
            super("You need at least 1 VCPU to start a VM");
            this.vm = vm;
        }

    }

    /**
     * The uploaded patch file is invalid.  See attached log for more details.
     */
    public static class InvalidPatchWithLog extends XenAPIException {
        public final String log;

        /**
         * Create a new InvalidPatchWithLog
         *
         * @param log
         */
        public InvalidPatchWithLog(String log) {
            super("The uploaded patch file is invalid.  See attached log for more details.");
            this.log = log;
        }

    }

    /**
     * The SR operation cannot be performed because a device underlying the SR is in use by the host.
     */
    public static class SrDeviceInUse extends XenAPIException {

        /**
         * Create a new SrDeviceInUse
         */
        public SrDeviceInUse() {
            super("The SR operation cannot be performed because a device underlying the SR is in use by the host.");
        }

    }

    /**
     * The host CDROM drive does not contain a valid CD
     */
    public static class HostCdDriveEmpty extends XenAPIException {

        /**
         * Create a new HostCdDriveEmpty
         */
        public HostCdDriveEmpty() {
            super("The host CDROM drive does not contain a valid CD");
        }

    }

    /**
     * The operation could not be performed while the host is still armed; it must be disarmed first
     */
    public static class HaHostIsArmed extends XenAPIException {
        public final String host;

        /**
         * Create a new HaHostIsArmed
         *
         * @param host
         */
        public HaHostIsArmed(String host) {
            super("The operation could not be performed while the host is still armed; it must be disarmed first");
            this.host = host;
        }

    }

    /**
     * The server failed to parse your event subscription. Valid values include: *, class-name, class-name/object-reference.
     */
    public static class EventSubscriptionParseFailure extends XenAPIException {
        public final String subscription;

        /**
         * Create a new EventSubscriptionParseFailure
         *
         * @param subscription
         */
        public EventSubscriptionParseFailure(String subscription) {
            super("The server failed to parse your event subscription. Valid values include: *, class-name, class-name/object-reference.");
            this.subscription = subscription;
        }

    }

    /**
     * Your license has expired.  Please contact your support representative.
     */
    public static class LicenseExpired extends XenAPIException {

        /**
         * Create a new LicenseExpired
         */
        public LicenseExpired() {
            super("Your license has expired.  Please contact your support representative.");
        }

    }

    /**
     * The credentials given by the user are incorrect, so access has been denied, and you have not been issued a session handle.
     */
    public static class SessionAuthenticationFailed extends XenAPIException {

        /**
         * Create a new SessionAuthenticationFailed
         */
        public SessionAuthenticationFailed() {
            super("The credentials given by the user are incorrect, so access has been denied, and you have not been issued a session handle.");
        }

    }

    /**
     * You tried to create a VLAN on top of another VLAN - use the underlying physical PIF/bond instead
     */
    public static class PifIsVlan extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifIsVlan
         *
         * @param PIF
         */
        public PifIsVlan(String PIF) {
            super("You tried to create a VLAN on top of another VLAN - use the underlying physical PIF/bond instead");
            this.PIF = PIF;
        }

    }

    /**
     * Archive more frequent than backup.
     */
    public static class VmppArchiveMoreFrequentThanBackup extends XenAPIException {

        /**
         * Create a new VmppArchiveMoreFrequentThanBackup
         */
        public VmppArchiveMoreFrequentThanBackup() {
            super("Archive more frequent than backup.");
        }

    }

    /**
     * There was a problem with the license daemon (v6d). Is it running?
     */
    public static class V6dFailure extends XenAPIException {

        /**
         * Create a new V6dFailure
         */
        public V6dFailure() {
            super("There was a problem with the license daemon (v6d). Is it running?");
        }

    }

    /**
     * The host joining the pool cannot already be a master of another pool.
     */
    public static class JoiningHostCannotBeMasterOfOtherHosts extends XenAPIException {

        /**
         * Create a new JoiningHostCannotBeMasterOfOtherHosts
         */
        public JoiningHostCannotBeMasterOfOtherHosts() {
            super("The host joining the pool cannot already be a master of another pool.");
        }

    }

    /**
     * This host can not be forgotten because there are some user VMs still running
     */
    public static class HostHasResidentVms extends XenAPIException {
        public final String host;

        /**
         * Create a new HostHasResidentVms
         *
         * @param host
         */
        public HostHasResidentVms(String host) {
            super("This host can not be forgotten because there are some user VMs still running");
            this.host = host;
        }

    }

    /**
     * An error occured while saving the memory image of the specified virtual machine
     */
    public static class VmCheckpointSuspendFailed extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmCheckpointSuspendFailed
         *
         * @param vm
         */
        public VmCheckpointSuspendFailed(String vm) {
            super("An error occured while saving the memory image of the specified virtual machine");
            this.vm = vm;
        }

    }

    /**
     * The operation you requested cannot be performed because the specified PIF is the management interface.
     */
    public static class PifIsManagementInterface extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifIsManagementInterface
         *
         * @param PIF
         */
        public PifIsManagementInterface(String PIF) {
            super("The operation you requested cannot be performed because the specified PIF is the management interface.");
            this.PIF = PIF;
        }

    }

    /**
     * The MAC address specified is not valid.
     */
    public static class MacInvalid extends XenAPIException {
        public final String MAC;

        /**
         * Create a new MacInvalid
         *
         * @param MAC
         */
        public MacInvalid(String MAC) {
            super("The MAC address specified is not valid.");
            this.MAC = MAC;
        }

    }

    /**
     * An attempt to start a new VSS snapshot failed
     */
    public static class XenVssReqErrorStartSnapshotSetFailed extends XenAPIException {
        public final String vm;
        public final String errorCode;

        /**
         * Create a new XenVssReqErrorStartSnapshotSetFailed
         *
         * @param vm
         * @param errorCode
         */
        public XenVssReqErrorStartSnapshotSetFailed(String vm, String errorCode) {
            super("An attempt to start a new VSS snapshot failed");
            this.vm = vm;
            this.errorCode = errorCode;
        }

    }

    /**
     * Operation could not be performed because the drive is empty
     */
    public static class VbdIsEmpty extends XenAPIException {
        public final String vbd;

        /**
         * Create a new VbdIsEmpty
         *
         * @param vbd
         */
        public VbdIsEmpty(String vbd) {
            super("Operation could not be performed because the drive is empty");
            this.vbd = vbd;
        }

    }

    /**
     * The patch precheck stage failed: the server is of an incorrect version.
     */
    public static class PatchPrecheckFailedWrongServerVersion extends XenAPIException {
        public final String patch;
        public final String foundVersion;
        public final String requiredVersion;

        /**
         * Create a new PatchPrecheckFailedWrongServerVersion
         *
         * @param patch
         * @param foundVersion
         * @param requiredVersion
         */
        public PatchPrecheckFailedWrongServerVersion(String patch, String foundVersion, String requiredVersion) {
            super("The patch precheck stage failed: the server is of an incorrect version.");
            this.patch = patch;
            this.foundVersion = foundVersion;
            this.requiredVersion = requiredVersion;
        }

    }

    /**
     * This operation could not be performed because the state partition could not be found
     */
    public static class CannotFindStatePartition extends XenAPIException {

        /**
         * Create a new CannotFindStatePartition
         */
        public CannotFindStatePartition() {
            super("This operation could not be performed because the state partition could not be found");
        }

    }

    /**
     * The WLB server rejected our configured authentication details.
     */
    public static class WlbAuthenticationFailed extends XenAPIException {

        /**
         * Create a new WlbAuthenticationFailed
         */
        public WlbAuthenticationFailed() {
            super("The WLB server rejected our configured authentication details.");
        }

    }

    /**
     * Unknown type of external authentication.
     */
    public static class AuthUnknownType extends XenAPIException {
        public final String type;

        /**
         * Create a new AuthUnknownType
         *
         * @param type
         */
        public AuthUnknownType(String type) {
            super("Unknown type of external authentication.");
            this.type = type;
        }

    }

    /**
     * This pool is not in emergency mode.
     */
    public static class NotInEmergencyMode extends XenAPIException {

        /**
         * Create a new NotInEmergencyMode
         */
        public NotInEmergencyMode() {
            super("This pool is not in emergency mode.");
        }

    }

    /**
     * The host failed to disable external authentication.
     */
    public static class AuthDisableFailed extends XenAPIException {
        public final String message;

        /**
         * Create a new AuthDisableFailed
         *
         * @param message
         */
        public AuthDisableFailed(String message) {
            super("The host failed to disable external authentication.");
            this.message = message;
        }

    }

    /**
     * You tried to create a PIF, but the network you tried to attach it to is already attached to some other PIF, and so the creation failed.
     */
    public static class NetworkAlreadyConnected extends XenAPIException {
        public final String network;
        public final String connectedPIF;

        /**
         * Create a new NetworkAlreadyConnected
         *
         * @param network
         * @param connectedPIF
         */
        public NetworkAlreadyConnected(String network, String connectedPIF) {
            super("You tried to create a PIF, but the network you tried to attach it to is already attached to some other PIF, and so the creation failed.");
            this.network = network;
            this.connectedPIF = connectedPIF;
        }

    }

    /**
     * This operation cannot be performed because the specified VDI is of an incompatible type (eg: an HA statefile cannot be attached to a guest)
     */
    public static class VdiIncompatibleType extends XenAPIException {
        public final String vdi;
        public final String type;

        /**
         * Create a new VdiIncompatibleType
         *
         * @param vdi
         * @param type
         */
        public VdiIncompatibleType(String vdi, String type) {
            super("This operation cannot be performed because the specified VDI is of an incompatible type (eg: an HA statefile cannot be attached to a guest)");
            this.vdi = vdi;
            this.type = type;
        }

    }

    /**
     * The configured WLB server name failed to resolve in DNS.
     */
    public static class WlbUnknownHost extends XenAPIException {

        /**
         * Create a new WlbUnknownHost
         */
        public WlbUnknownHost() {
            super("The configured WLB server name failed to resolve in DNS.");
        }

    }

    /**
     * The VM could not be imported.
     */
    public static class ImportError extends XenAPIException {
        public final String msg;

        /**
         * Create a new ImportError
         *
         * @param msg
         */
        public ImportError(String msg) {
            super("The VM could not be imported.");
            this.msg = msg;
        }

    }

    /**
     * The SR could not be connected because the driver was not recognised.
     */
    public static class SrUnknownDriver extends XenAPIException {
        public final String driver;

        /**
         * Create a new SrUnknownDriver
         *
         * @param driver
         */
        public SrUnknownDriver(String driver) {
            super("The SR could not be connected because the driver was not recognised.");
            this.driver = driver;
        }

    }

    /**
     * The host failed to disable external authentication.
     */
    public static class AuthDisableFailedWrongCredentials extends XenAPIException {
        public final String message;

        /**
         * Create a new AuthDisableFailedWrongCredentials
         *
         * @param message
         */
        public AuthDisableFailedWrongCredentials(String message) {
            super("The host failed to disable external authentication.");
            this.message = message;
        }

    }

    /**
     * The VM unexpectedly halted
     */
    public static class VmHalted extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmHalted
         *
         * @param vm
         */
        public VmHalted(String vm) {
            super("The VM unexpectedly halted");
            this.vm = vm;
        }

    }

    /**
     * The use of this feature is restricted.
     */
    public static class FeatureRestricted extends XenAPIException {

        /**
         * Create a new FeatureRestricted
         */
        public FeatureRestricted() {
            super("The use of this feature is restricted.");
        }

    }

    /**
     * The VDI could not be opened for metadata recovery as it contains the current pool's metadata.
     */
    public static class VdiContainsMetadataOfThisPool extends XenAPIException {
        public final String vdi;
        public final String pool;

        /**
         * Create a new VdiContainsMetadataOfThisPool
         *
         * @param vdi
         * @param pool
         */
        public VdiContainsMetadataOfThisPool(String vdi, String pool) {
            super("The VDI could not be opened for metadata recovery as it contains the current pool's metadata.");
            this.vdi = vdi;
            this.pool = pool;
        }

    }

    /**
     * The specified CRL name is invalid.
     */
    public static class CrlNameInvalid extends XenAPIException {
        public final String name;

        /**
         * Create a new CrlNameInvalid
         *
         * @param name
         */
        public CrlNameInvalid(String name) {
            super("The specified CRL name is invalid.");
            this.name = name;
        }

    }

    /**
     * This operation cannot be completed as the host power on mode is disabled.
     */
    public static class HostPowerOnModeDisabled extends XenAPIException {

        /**
         * Create a new HostPowerOnModeDisabled
         */
        public HostPowerOnModeDisabled() {
            super("This operation cannot be completed as the host power on mode is disabled.");
        }

    }

    /**
     * An activation key can only be applied when the edition is set to 'free'.
     */
    public static class ActivationWhileNotFree extends XenAPIException {

        /**
         * Create a new ActivationWhileNotFree
         */
        public ActivationWhileNotFree() {
            super("An activation key can only be applied when the edition is set to 'free'.");
        }

    }

    /**
     * There was a failure communicating with the plugin.
     */
    public static class XenapiPluginFailure extends XenAPIException {
        public final String status;
        public final String stdout;
        public final String stderr;

        /**
         * Create a new XenapiPluginFailure
         *
         * @param status
         * @param stdout
         * @param stderr
         */
        public XenapiPluginFailure(String status, String stdout, String stderr) {
            super("There was a failure communicating with the plugin.");
            this.status = status;
            this.stdout = stdout;
            this.stderr = stderr;
        }

    }

    /**
     * The MAC address specified still exists on this host.
     */
    public static class MacStillExists extends XenAPIException {
        public final String MAC;

        /**
         * Create a new MacStillExists
         *
         * @param MAC
         */
        public MacStillExists(String MAC) {
            super("The MAC address specified still exists on this host.");
            this.MAC = MAC;
        }

    }

    /**
     * This operation cannot be completed as the host is in use by (at least) the object of type and ref echoed below.
     */
    public static class HostInUse extends XenAPIException {
        public final String host;
        public final String type;
        public final String ref;

        /**
         * Create a new HostInUse
         *
         * @param host
         * @param type
         * @param ref
         */
        public HostInUse(String host, String type, String ref) {
            super("This operation cannot be completed as the host is in use by (at least) the object of type and ref echoed below.");
            this.host = host;
            this.type = type;
            this.ref = ref;
        }

    }

    /**
     * HA can only be enabled for 2 hosts or more. Note that 2 hosts requires a pre-configured quorum tiebreak script.
     */
    public static class HaTooFewHosts extends XenAPIException {

        /**
         * Create a new HaTooFewHosts
         */
        public HaTooFewHosts() {
            super("HA can only be enabled for 2 hosts or more. Note that 2 hosts requires a pre-configured quorum tiebreak script.");
        }

    }

    /**
     * The connection to the WLB server was reset.
     */
    public static class WlbConnectionReset extends XenAPIException {

        /**
         * Create a new WlbConnectionReset
         */
        public WlbConnectionReset() {
            super("The connection to the WLB server was reset.");
        }

    }

    /**
     * The pool failed to enable external authentication.
     */
    public static class PoolAuthEnableFailedWrongCredentials extends XenAPIException {
        public final String host;
        public final String message;

        /**
         * Create a new PoolAuthEnableFailedWrongCredentials
         *
         * @param host
         * @param message
         */
        public PoolAuthEnableFailedWrongCredentials(String host, String message) {
            super("The pool failed to enable external authentication.");
            this.host = host;
            this.message = message;
        }

    }

    /**
     * The specified patch is applied and cannot be destroyed.
     */
    public static class PatchIsApplied extends XenAPIException {

        /**
         * Create a new PatchIsApplied
         */
        public PatchIsApplied() {
            super("The specified patch is applied and cannot be destroyed.");
        }

    }

    /**
     * The SR is still connected to a host via a PBD. It cannot be destroyed or forgotten.
     */
    public static class SrHasPbd extends XenAPIException {
        public final String sr;

        /**
         * Create a new SrHasPbd
         *
         * @param sr
         */
        public SrHasPbd(String sr) {
            super("The SR is still connected to a host via a PBD. It cannot be destroyed or forgotten.");
            this.sr = sr;
        }

    }

    /**
     * Some VMs belonging to the appliance threw an exception while carrying out the specified operation
     */
    public static class OperationPartiallyFailed extends XenAPIException {
        public final String operation;

        /**
         * Create a new OperationPartiallyFailed
         *
         * @param operation
         */
        public OperationPartiallyFailed(String operation) {
            super("Some VMs belonging to the appliance threw an exception while carrying out the specified operation");
            this.operation = operation;
        }

    }

    /**
     * The WLB server rejected XenServer's request as malformed.
     */
    public static class WlbMalformedRequest extends XenAPIException {

        /**
         * Create a new WlbMalformedRequest
         */
        public WlbMalformedRequest() {
            super("The WLB server rejected XenServer's request as malformed.");
        }

    }

    /**
     * The host toolstack is still initialising. Please wait.
     */
    public static class HostStillBooting extends XenAPIException {

        /**
         * Create a new HostStillBooting
         */
        public HostStillBooting() {
            super("The host toolstack is still initialising. Please wait.");
        }

    }

    /**
     * You tried to destroy a system network: these cannot be destroyed.
     */
    public static class CannotDestroySystemNetwork extends XenAPIException {
        public final String network;

        /**
         * Create a new CannotDestroySystemNetwork
         *
         * @param network
         */
        public CannotDestroySystemNetwork(String network) {
            super("You tried to destroy a system network: these cannot be destroyed.");
            this.network = network;
        }

    }

    /**
     * The specified object no longer exists.
     */
    public static class ObjectNolongerExists extends XenAPIException {

        /**
         * Create a new ObjectNolongerExists
         */
        public ObjectNolongerExists() {
            super("The specified object no longer exists.");
        }

    }

    /**
     * This VDI was not mapped to a destination SR in VM.migrate_send operation
     */
    public static class VdiNotInMap extends XenAPIException {
        public final String vdi;

        /**
         * Create a new VdiNotInMap
         *
         * @param vdi
         */
        public VdiNotInMap(String vdi) {
            super("This VDI was not mapped to a destination SR in VM.migrate_send operation");
            this.vdi = vdi;
        }

    }

    /**
     * The hosts in this pool are not homogeneous.
     */
    public static class HostsNotHomogeneous extends XenAPIException {
        public final String reason;

        /**
         * Create a new HostsNotHomogeneous
         *
         * @param reason
         */
        public HostsNotHomogeneous(String reason) {
            super("The hosts in this pool are not homogeneous.");
            this.reason = reason;
        }

    }

    /**
     * The host joining the pool must have the same product version as the pool master.
     */
    public static class PoolJoiningHostMustHaveSameProductVersion extends XenAPIException {

        /**
         * Create a new PoolJoiningHostMustHaveSameProductVersion
         */
        public PoolJoiningHostMustHaveSameProductVersion() {
            super("The host joining the pool must have the same product version as the pool master.");
        }

    }

    /**
     * You tried to create a PIF, but it already exists.
     */
    public static class PifVlanExists extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifVlanExists
         *
         * @param PIF
         */
        public PifVlanExists(String PIF) {
            super("You tried to create a PIF, but it already exists.");
            this.PIF = PIF;
        }

    }

    /**
     * The license for the edition you requested is not available.
     */
    public static class LicenseCheckoutError extends XenAPIException {
        public final String reason;

        /**
         * Create a new LicenseCheckoutError
         *
         * @param reason
         */
        public LicenseCheckoutError(String reason) {
            super("The license for the edition you requested is not available.");
            this.reason = reason;
        }

    }

    /**
     * The certificate library is corrupt or unreadable.
     */
    public static class CertificateLibraryCorrupt extends XenAPIException {

        /**
         * Create a new CertificateLibraryCorrupt
         */
        public CertificateLibraryCorrupt() {
            super("The certificate library is corrupt or unreadable.");
        }

    }

    /**
     * This operation cannot be performed because the system does not manage this VDI
     */
    public static class VdiNotManaged extends XenAPIException {
        public final String vdi;

        /**
         * Create a new VdiNotManaged
         *
         * @param vdi
         */
        public VdiNotManaged(String vdi) {
            super("This operation cannot be performed because the system does not manage this VDI");
            this.vdi = vdi;
        }

    }

    /**
     * The edition you supplied is invalid.
     */
    public static class InvalidEdition extends XenAPIException {
        public final String edition;

        /**
         * Create a new InvalidEdition
         *
         * @param edition
         */
        public InvalidEdition(String edition) {
            super("The edition you supplied is invalid.");
            this.edition = edition;
        }

    }

    /**
     * The uploaded patch file already exists
     */
    public static class PatchAlreadyExists extends XenAPIException {
        public final String uuid;

        /**
         * Create a new PatchAlreadyExists
         *
         * @param uuid
         */
        public PatchAlreadyExists(String uuid) {
            super("The uploaded patch file already exists");
            this.uuid = uuid;
        }

    }

    /**
     * There is not enough space to upload the update
     */
    public static class OutOfSpace extends XenAPIException {
        public final String location;

        /**
         * Create a new OutOfSpace
         *
         * @param location
         */
        public OutOfSpace(String location) {
            super("There is not enough space to upload the update");
            this.location = location;
        }

    }

    /**
     * The VM could not be imported; the end of the file was reached prematurely.
     */
    public static class ImportErrorPrematureEof extends XenAPIException {

        /**
         * Create a new ImportErrorPrematureEof
         */
        public ImportErrorPrematureEof() {
            super("The VM could not be imported; the end of the file was reached prematurely.");
        }

    }

    /**
     * The given VM is not registered as a system domain. This operation can only be performed on a registered system domain.
     */
    public static class NotSystemDomain extends XenAPIException {
        public final String vm;

        /**
         * Create a new NotSystemDomain
         *
         * @param vm
         */
        public NotSystemDomain(String vm) {
            super("The given VM is not registered as a system domain. This operation can only be performed on a registered system domain.");
            this.vm = vm;
        }

    }

    /**
     * The specified VM has too little memory to be started.
     */
    public static class VmMemorySizeTooLow extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmMemorySizeTooLow
         *
         * @param vm
         */
        public VmMemorySizeTooLow(String vm) {
            super("The specified VM has too little memory to be started.");
            this.vm = vm;
        }

    }

    /**
     * There is at least one VM assigned to this protection policy.
     */
    public static class VmppHasVm extends XenAPIException {

        /**
         * Create a new VmppHasVm
         */
        public VmppHasVm() {
            super("There is at least one VM assigned to this protection policy.");
        }

    }

    /**
     * This operation cannot be performed because the host is not disabled. Please disable the host and then try again.
     */
    public static class HostNotDisabled extends XenAPIException {

        /**
         * Create a new HostNotDisabled
         */
        public HostNotDisabled() {
            super("This operation cannot be performed because the host is not disabled. Please disable the host and then try again.");
        }

    }

    /**
     * The value specified is of the wrong type
     */
    public static class FieldTypeError extends XenAPIException {
        public final String field;

        /**
         * Create a new FieldTypeError
         *
         * @param field
         */
        public FieldTypeError(String field) {
            super("The value specified is of the wrong type");
            this.field = field;
        }

    }

    /**
     * The management interface on a slave cannot be disabled because the slave would enter emergency mode.
     */
    public static class SlaveRequiresManagementInterface extends XenAPIException {

        /**
         * Create a new SlaveRequiresManagementInterface
         */
        public SlaveRequiresManagementInterface() {
            super("The management interface on a slave cannot be disabled because the slave would enter emergency mode.");
        }

    }

    /**
     * The operation attempted is not valid for a template VM
     */
    public static class VmIsTemplate extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmIsTemplate
         *
         * @param vm
         */
        public VmIsTemplate(String vm) {
            super("The operation attempted is not valid for a template VM");
            this.vm = vm;
        }

    }

    /**
     * This operation cannot be performed because the specified VM is protected by xHA
     */
    public static class VmIsProtected extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmIsProtected
         *
         * @param vm
         */
        public VmIsProtected(String vm) {
            super("This operation cannot be performed because the specified VM is protected by xHA");
            this.vm = vm;
        }

    }

    /**
     * The host joining the pool cannot have any running VMs.
     */
    public static class JoiningHostCannotHaveRunningVms extends XenAPIException {

        /**
         * Create a new JoiningHostCannotHaveRunningVms
         */
        public JoiningHostCannotHaveRunningVms() {
            super("The host joining the pool cannot have any running VMs.");
        }

    }

    /**
     * VM cannot be started because it requires a VDI which cannot be attached
     */
    public static class VmRequiresVdi extends XenAPIException {
        public final String vm;
        public final String vdi;

        /**
         * Create a new VmRequiresVdi
         *
         * @param vm
         * @param vdi
         */
        public VmRequiresVdi(String vm, String vdi) {
            super("VM cannot be started because it requires a VDI which cannot be attached");
            this.vm = vm;
            this.vdi = vdi;
        }

    }

    /**
     * Read/write CDs are not supported
     */
    public static class VbdCdsMustBeReadonly extends XenAPIException {

        /**
         * Create a new VbdCdsMustBeReadonly
         */
        public VbdCdsMustBeReadonly() {
            super("Read/write CDs are not supported");
        }

    }

    /**
     * This license file is no longer accepted. Please upgrade to the new licensing system.
     */
    public static class LicenseFileDeprecated extends XenAPIException {

        /**
         * Create a new LicenseFileDeprecated
         */
        public LicenseFileDeprecated() {
            super("This license file is no longer accepted. Please upgrade to the new licensing system.");
        }

    }

    /**
     * An HA statefile could not be created, perhaps because no SR with the appropriate capability was found.
     */
    public static class CannotCreateStateFile extends XenAPIException {

        /**
         * Create a new CannotCreateStateFile
         */
        public CannotCreateStateFile() {
            super("An HA statefile could not be created, perhaps because no SR with the appropriate capability was found.");
        }

    }

    /**
     * The host joining the pool cannot have any VMs with active tasks.
     */
    public static class JoiningHostCannotHaveVmsWithCurrentOperations extends XenAPIException {

        /**
         * Create a new JoiningHostCannotHaveVmsWithCurrentOperations
         */
        public JoiningHostCannotHaveVmsWithCurrentOperations() {
            super("The host joining the pool cannot have any VMs with active tasks.");
        }

    }

    /**
     * You tried to call a method with the incorrect number of parameters.  The fully-qualified method name that you used, and the number of received and expected parameters are returned.
     */
    public static class MessageParameterCountMismatch extends XenAPIException {
        public final String method;
        public final String expected;
        public final String received;

        /**
         * Create a new MessageParameterCountMismatch
         *
         * @param method
         * @param expected
         * @param received
         */
        public MessageParameterCountMismatch(String method, String expected, String received) {
            super("You tried to call a method with the incorrect number of parameters.  The fully-qualified method name that you used, and the number of received and expected parameters are returned.");
            this.method = method;
            this.expected = expected;
            this.received = received;
        }

    }

    /**
     * External authentication in this pool is already enabled for at least one host.
     */
    public static class PoolAuthAlreadyEnabled extends XenAPIException {
        public final String host;

        /**
         * Create a new PoolAuthAlreadyEnabled
         *
         * @param host
         */
        public PoolAuthAlreadyEnabled(String host) {
            super("External authentication in this pool is already enabled for at least one host.");
            this.host = host;
        }

    }

    /**
     * The restore could not be performed because this backup has been created by a different (incompatible) product version
     */
    public static class RestoreIncompatibleVersion extends XenAPIException {

        /**
         * Create a new RestoreIncompatibleVersion
         */
        public RestoreIncompatibleVersion() {
            super("The restore could not be performed because this backup has been created by a different (incompatible) product version");
        }

    }

    /**
     * The VM rejected the attempt to detach the device.
     */
    public static class DeviceDetachRejected extends XenAPIException {
        public final String type;
        public final String ref;
        public final String msg;

        /**
         * Create a new DeviceDetachRejected
         *
         * @param type
         * @param ref
         * @param msg
         */
        public DeviceDetachRejected(String type, String ref, String msg) {
            super("The VM rejected the attempt to detach the device.");
            this.type = type;
            this.ref = ref;
            this.msg = msg;
        }

    }

    /**
     * External authentication is disabled, unable to resolve subject name.
     */
    public static class AuthIsDisabled extends XenAPIException {

        /**
         * Create a new AuthIsDisabled
         */
        public AuthIsDisabled() {
            super("External authentication is disabled, unable to resolve subject name.");
        }

    }

    /**
     * The host joining the pool cannot have any running or suspended VMs.
     */
    public static class JoiningHostCannotHaveRunningOrSuspendedVms extends XenAPIException {

        /**
         * Create a new JoiningHostCannotHaveRunningOrSuspendedVms
         */
        public JoiningHostCannotHaveRunningOrSuspendedVms() {
            super("The host joining the pool cannot have any running or suspended VMs.");
        }

    }

    /**
     * The patch precheck stage failed: prerequisite patches are missing.
     */
    public static class PatchPrecheckFailedPrerequisiteMissing extends XenAPIException {
        public final String patch;
        public final String prerequisitePatchUuidList;

        /**
         * Create a new PatchPrecheckFailedPrerequisiteMissing
         *
         * @param patch
         * @param prerequisitePatchUuidList
         */
        public PatchPrecheckFailedPrerequisiteMissing(String patch, String prerequisitePatchUuidList) {
            super("The patch precheck stage failed: prerequisite patches are missing.");
            this.patch = patch;
            this.prerequisitePatchUuidList = prerequisitePatchUuidList;
        }

    }

    /**
     * This operation could not be performed, because the VM has one or more PCI devices passed through.
     */
    public static class VmHasPciAttached extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmHasPciAttached
         *
         * @param vm
         */
        public VmHasPciAttached(String vm) {
            super("This operation could not be performed, because the VM has one or more PCI devices passed through.");
            this.vm = vm;
        }

    }

    /**
     * The VDI mirroring cannot be performed
     */
    public static class MirrorFailed extends XenAPIException {
        public final String vdi;

        /**
         * Create a new MirrorFailed
         *
         * @param vdi
         */
        public MirrorFailed(String vdi) {
            super("The VDI mirroring cannot be performed");
            this.vdi = vdi;
        }

    }

    /**
     * The WLB server reported that communication with XenServer timed out.
     */
    public static class WlbXenserverTimeout extends XenAPIException {

        /**
         * Create a new WlbXenserverTimeout
         */
        public WlbXenserverTimeout() {
            super("The WLB server reported that communication with XenServer timed out.");
        }

    }

    /**
     * The pool failed to disable the external authentication of at least one host.
     */
    public static class PoolAuthDisableFailedWrongCredentials extends XenAPIException {
        public final String host;
        public final String message;

        /**
         * Create a new PoolAuthDisableFailedWrongCredentials
         *
         * @param host
         * @param message
         */
        public PoolAuthDisableFailedWrongCredentials(String host, String message) {
            super("The pool failed to disable the external authentication of at least one host.");
            this.host = host;
            this.message = message;
        }

    }

    /**
     * The quiesced-snapshot operation failed for an unexpected reason
     */
    public static class VmSnapshotWithQuiesceFailed extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmSnapshotWithQuiesceFailed
         *
         * @param vm
         */
        public VmSnapshotWithQuiesceFailed(String vm) {
            super("The quiesced-snapshot operation failed for an unexpected reason");
            this.vm = vm;
        }

    }

    /**
     * The specified certificate is corrupt or unreadable.
     */
    public static class CertificateCorrupt extends XenAPIException {
        public final String name;

        /**
         * Create a new CertificateCorrupt
         *
         * @param name
         */
        public CertificateCorrupt(String name) {
            super("The specified certificate is corrupt or unreadable.");
            this.name = name;
        }

    }

    /**
     * The WLB server reported an internal error.
     */
    public static class WlbInternalError extends XenAPIException {

        /**
         * Create a new WlbInternalError
         */
        public WlbInternalError() {
            super("The WLB server reported an internal error.");
        }

    }

    /**
     * The VM unexpectedly rebooted
     */
    public static class VmRebooted extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmRebooted
         *
         * @param vm
         */
        public VmRebooted(String vm) {
            super("The VM unexpectedly rebooted");
            this.vm = vm;
        }

    }

    /**
     * Cannot forward messages because the host cannot be contacted.  The host may be switched off or there may be network connectivity problems.
     */
    public static class CannotContactHost extends XenAPIException {
        public final String host;

        /**
         * Create a new CannotContactHost
         *
         * @param host
         */
        public CannotContactHost(String host) {
            super("Cannot forward messages because the host cannot be contacted.  The host may be switched off or there may be network connectivity problems.");
            this.host = host;
        }

    }

    /**
     * Could not find any volumes supported by the Citrix XenServer Vss Provider
     */
    public static class XenVssReqErrorNoVolumesSupported extends XenAPIException {
        public final String vm;
        public final String errorCode;

        /**
         * Create a new XenVssReqErrorNoVolumesSupported
         *
         * @param vm
         * @param errorCode
         */
        public XenVssReqErrorNoVolumesSupported(String vm, String errorCode) {
            super("Could not find any volumes supported by the Citrix XenServer Vss Provider");
            this.vm = vm;
            this.errorCode = errorCode;
        }

    }

    /**
     * The host is its own slave. Please use pool-emergency-transition-to-master or pool-emergency-reset-master.
     */
    public static class HostItsOwnSlave extends XenAPIException {

        /**
         * Create a new HostItsOwnSlave
         */
        public HostItsOwnSlave() {
            super("The host is its own slave. Please use pool-emergency-transition-to-master or pool-emergency-reset-master.");
        }

    }

    /**
     * This PIF is a bond slave and cannot have a VLAN on it.
     */
    public static class CannotAddVlanToBondSlave extends XenAPIException {
        public final String PIF;

        /**
         * Create a new CannotAddVlanToBondSlave
         *
         * @param PIF
         */
        public CannotAddVlanToBondSlave(String PIF) {
            super("This PIF is a bond slave and cannot have a VLAN on it.");
            this.PIF = PIF;
        }

    }

    /**
     * The operation could not be performed because a redo log is enabled on the Pool.
     */
    public static class RedoLogIsEnabled extends XenAPIException {

        /**
         * Create a new RedoLogIsEnabled
         */
        public RedoLogIsEnabled() {
            super("The operation could not be performed because a redo log is enabled on the Pool.");
        }

    }

    /**
     * You attempted an operation on a VM which requires PV drivers to be installed but the drivers were not detected.
     */
    public static class VmMissingPvDrivers extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmMissingPvDrivers
         *
         * @param vm
         */
        public VmMissingPvDrivers(String vm) {
            super("You attempted an operation on a VM which requires PV drivers to be installed but the drivers were not detected.");
            this.vm = vm;
        }

    }

    /**
     * The specified certificate name is invalid.
     */
    public static class CertificateNameInvalid extends XenAPIException {
        public final String name;

        /**
         * Create a new CertificateNameInvalid
         *
         * @param name
         */
        public CertificateNameInvalid(String name) {
            super("The specified certificate name is invalid.");
            this.name = name;
        }

    }

    /**
     * The VM could not be imported because a required object could not be found.
     */
    public static class ImportErrorFailedToFindObject extends XenAPIException {
        public final String id;

        /**
         * Create a new ImportErrorFailedToFindObject
         *
         * @param id
         */
        public ImportErrorFailedToFindObject(String id) {
            super("The VM could not be imported because a required object could not be found.");
            this.id = id;
        }

    }

    /**
     * This operation cannot be performed because the specified VDI could not be found in the specified SR
     */
    public static class VdiLocationMissing extends XenAPIException {
        public final String sr;
        public final String location;

        /**
         * Create a new VdiLocationMissing
         *
         * @param sr
         * @param location
         */
        public VdiLocationMissing(String sr, String location) {
            super("This operation cannot be performed because the specified VDI could not be found in the specified SR");
            this.sr = sr;
            this.location = location;
        }

    }

    /**
     * The host failed to enable external authentication.
     */
    public static class AuthEnableFailedPermissionDenied extends XenAPIException {
        public final String message;

        /**
         * Create a new AuthEnableFailedPermissionDenied
         *
         * @param message
         */
        public AuthEnableFailedPermissionDenied(String message) {
            super("The host failed to enable external authentication.");
            this.message = message;
        }

    }

    /**
     * Operation cannot proceed while a VLAN exists on this interface.
     */
    public static class PifVlanStillExists extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifVlanStillExists
         *
         * @param PIF
         */
        public PifVlanStillExists(String PIF) {
            super("Operation cannot proceed while a VLAN exists on this interface.");
            this.PIF = PIF;
        }

    }

    /**
     * The given VMs failed to release memory when instructed to do so
     */
    public static class VmsFailedToCooperate extends XenAPIException {

        /**
         * Create a new VmsFailedToCooperate
         */
        public VmsFailedToCooperate() {
            super("The given VMs failed to release memory when instructed to do so");
        }

    }

    /**
     * You reached the maximal number of concurrently migrating VMs.
     */
    public static class TooManyStorageMigrates extends XenAPIException {
        public final String number;

        /**
         * Create a new TooManyStorageMigrates
         *
         * @param number
         */
        public TooManyStorageMigrates(String number) {
            super("You reached the maximal number of concurrently migrating VMs.");
            this.number = number;
        }

    }

    /**
     * The network contains active VIFs and cannot be deleted.
     */
    public static class NetworkContainsVif extends XenAPIException {
        public final String vifs;

        /**
         * Create a new NetworkContainsVif
         *
         * @param vifs
         */
        public NetworkContainsVif(String vifs) {
            super("The network contains active VIFs and cannot be deleted.");
            this.vifs = vifs;
        }

    }

    /**
     * The value given is invalid
     */
    public static class InvalidValue extends XenAPIException {
        public final String field;
        public final String value;

        /**
         * Create a new InvalidValue
         *
         * @param field
         * @param value
         */
        public InvalidValue(String field, String value) {
            super("The value given is invalid");
            this.field = field;
            this.value = value;
        }

    }

    /**
     * The requested plugin could not be found.
     */
    public static class XenapiMissingPlugin extends XenAPIException {
        public final String name;

        /**
         * Create a new XenapiMissingPlugin
         *
         * @param name
         */
        public XenapiMissingPlugin(String name) {
            super("The requested plugin could not be found.");
            this.name = name;
        }

    }

    /**
     * The restore could not be performed because the host's current management interface is not in the backup. The interfaces mentioned in the backup are:
     */
    public static class RestoreTargetMgmtIfNotInBackup extends XenAPIException {

        /**
         * Create a new RestoreTargetMgmtIfNotInBackup
         */
        public RestoreTargetMgmtIfNotInBackup() {
            super("The restore could not be performed because the host's current management interface is not in the backup. The interfaces mentioned in the backup are:");
        }

    }

    /**
     * You tried to create a VLAN or tunnel on top of a tunnel access PIF - use the underlying transport PIF instead.
     */
    public static class IsTunnelAccessPif extends XenAPIException {
        public final String PIF;

        /**
         * Create a new IsTunnelAccessPif
         *
         * @param PIF
         */
        public IsTunnelAccessPif(String PIF) {
            super("You tried to create a VLAN or tunnel on top of a tunnel access PIF - use the underlying transport PIF instead.");
            this.PIF = PIF;
        }

    }

    /**
     * There was an error connecting to the host while joining it in the pool.
     */
    public static class JoiningHostConnectionFailed extends XenAPIException {

        /**
         * Create a new JoiningHostConnectionFailed
         */
        public JoiningHostConnectionFailed() {
            super("There was an error connecting to the host while joining it in the pool.");
        }

    }

    /**
     * Subject cannot be resolved by the external directory service.
     */
    public static class SubjectCannotBeResolved extends XenAPIException {

        /**
         * Create a new SubjectCannotBeResolved
         */
        public SubjectCannotBeResolved() {
            super("Subject cannot be resolved by the external directory service.");
        }

    }

    /**
     * Some volumes to be snapshot could not be added to the VSS snapshot set
     */
    public static class XenVssReqErrorAddingVolumeToSnapsetFailed extends XenAPIException {
        public final String vm;
        public final String errorCode;

        /**
         * Create a new XenVssReqErrorAddingVolumeToSnapsetFailed
         *
         * @param vm
         * @param errorCode
         */
        public XenVssReqErrorAddingVolumeToSnapsetFailed(String vm, String errorCode) {
            super("Some volumes to be snapshot could not be added to the VSS snapshot set");
            this.vm = vm;
            this.errorCode = errorCode;
        }

    }

    /**
     * The provision call failed because it ran out of space.
     */
    public static class ProvisionFailedOutOfSpace extends XenAPIException {

        /**
         * Create a new ProvisionFailedOutOfSpace
         */
        public ProvisionFailedOutOfSpace() {
            super("The provision call failed because it ran out of space.");
        }

    }

    /**
     * You attempted to migrate a VDI which is not attached to a runnning VM.
     */
    public static class VdiNeedsVmForMigrate extends XenAPIException {
        public final String vdi;

        /**
         * Create a new VdiNeedsVmForMigrate
         *
         * @param vdi
         */
        public VdiNeedsVmForMigrate(String vdi) {
            super("You attempted to migrate a VDI which is not attached to a runnning VM.");
            this.vdi = vdi;
        }

    }

    /**
     * An error occurred while attempting to import a database from a metadata VDI
     */
    public static class CouldNotImportDatabase extends XenAPIException {
        public final String reason;

        /**
         * Create a new CouldNotImportDatabase
         *
         * @param reason
         */
        public CouldNotImportDatabase(String reason) {
            super("An error occurred while attempting to import a database from a metadata VDI");
            this.reason = reason;
        }

    }

    /**
     * This operation can only be performed on CD VDIs (iso files or CDROM drives)
     */
    public static class VdiIsNotIso extends XenAPIException {
        public final String vdi;
        public final String type;

        /**
         * Create a new VdiIsNotIso
         *
         * @param vdi
         * @param type
         */
        public VdiIsNotIso(String vdi, String type) {
            super("This operation can only be performed on CD VDIs (iso files or CDROM drives)");
            this.vdi = vdi;
            this.type = type;
        }

    }

    /**
     * You tried to call a method that does not exist.  The method name that you used is echoed.
     */
    public static class MessageMethodUnknown extends XenAPIException {
        public final String method;

        /**
         * Create a new MessageMethodUnknown
         *
         * @param method
         */
        public MessageMethodUnknown(String method) {
            super("You tried to call a method that does not exist.  The method name that you used is echoed.");
            this.method = method;
        }

    }

    /**
     * You cannot delete the specified default template.
     */
    public static class VmCannotDeleteDefaultTemplate extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmCannotDeleteDefaultTemplate
         *
         * @param vm
         */
        public VmCannotDeleteDefaultTemplate(String vm) {
            super("You cannot delete the specified default template.");
            this.vm = vm;
        }

    }

    /**
     * Role cannot be found.
     */
    public static class RoleNotFound extends XenAPIException {

        /**
         * Create a new RoleNotFound
         */
        public RoleNotFound() {
            super("Role cannot be found.");
        }

    }

    /**
     * This command is not allowed on the OEM edition.
     */
    public static class NotAllowedOnOemEdition extends XenAPIException {
        public final String command;

        /**
         * Create a new NotAllowedOnOemEdition
         *
         * @param command
         */
        public NotAllowedOnOemEdition(String command) {
            super("This command is not allowed on the OEM edition.");
            this.command = command;
        }

    }

    /**
     * The restore could not be performed because the restore script failed.  Is the file corrupt?
     */
    public static class RestoreScriptFailed extends XenAPIException {
        public final String log;

        /**
         * Create a new RestoreScriptFailed
         *
         * @param log
         */
        public RestoreScriptFailed(String log) {
            super("The restore could not be performed because the restore script failed.  Is the file corrupt?");
            this.log = log;
        }

    }

    /**
     * The server failed to handle your request, due to an internal error.  The given message may give details useful for debugging the problem.
     */
    public static class InternalError extends XenAPIException {
        public final String message;

        /**
         * Create a new InternalError
         *
         * @param message
         */
        public InternalError(String message) {
            super("The server failed to handle your request, due to an internal error.  The given message may give details useful for debugging the problem.");
            this.message = message;
        }

    }

    /**
     * XHA cannot be enabled because this host's license does not allow it
     */
    public static class LicenseDoesNotSupportXha extends XenAPIException {

        /**
         * Create a new LicenseDoesNotSupportXha
         */
        public LicenseDoesNotSupportXha() {
            super("XHA cannot be enabled because this host's license does not allow it");
        }

    }

    /**
     * The primary address types are not compatible
     */
    public static class PifIncompatiblePrimaryAddressType extends XenAPIException {

        /**
         * Create a new PifIncompatiblePrimaryAddressType
         */
        public PifIncompatiblePrimaryAddressType() {
            super("The primary address types are not compatible");
        }

    }

    /**
     * The device is not currently attached
     */
    public static class DeviceAlreadyDetached extends XenAPIException {
        public final String device;

        /**
         * Create a new DeviceAlreadyDetached
         *
         * @param device
         */
        public DeviceAlreadyDetached(String device) {
            super("The device is not currently attached");
            this.device = device;
        }

    }

    /**
     * The host failed to enable external authentication.
     */
    public static class AuthEnableFailedUnavailable extends XenAPIException {
        public final String message;

        /**
         * Create a new AuthEnableFailedUnavailable
         *
         * @param message
         */
        public AuthEnableFailedUnavailable(String message) {
            super("The host failed to enable external authentication.");
            this.message = message;
        }

    }

    /**
     * Media could not be ejected because it is not removable
     */
    public static class VbdNotRemovableMedia extends XenAPIException {
        public final String vbd;

        /**
         * Create a new VbdNotRemovableMedia
         *
         * @param vbd
         */
        public VbdNotRemovableMedia(String vbd) {
            super("Media could not be ejected because it is not removable");
            this.vbd = vbd;
        }

    }

    /**
     * A VDI with the specified location already exists within the SR
     */
    public static class LocationNotUnique extends XenAPIException {
        public final String SR;
        public final String location;

        /**
         * Create a new LocationNotUnique
         *
         * @param SR
         * @param location
         */
        public LocationNotUnique(String SR, String location) {
            super("A VDI with the specified location already exists within the SR");
            this.SR = SR;
            this.location = location;
        }

    }

    /**
     * The function is not implemented
     */
    public static class NotImplemented extends XenAPIException {
        public final String function;

        /**
         * Create a new NotImplemented
         *
         * @param function
         */
        public NotImplemented(String function) {
            super("The function is not implemented");
            this.function = function;
        }

    }

    /**
     * Cannot plug VIF
     */
    public static class CannotPlugVif extends XenAPIException {
        public final String VIF;

        /**
         * Create a new CannotPlugVif
         *
         * @param VIF
         */
        public CannotPlugVif(String VIF) {
            super("Cannot plug VIF");
            this.VIF = VIF;
        }

    }

    /**
     * Only the local superuser can execute this operation
     */
    public static class UserIsNotLocalSuperuser extends XenAPIException {
        public final String msg;

        /**
         * Create a new UserIsNotLocalSuperuser
         *
         * @param msg
         */
        public UserIsNotLocalSuperuser(String msg) {
            super("Only the local superuser can execute this operation");
            this.msg = msg;
        }

    }

    /**
     * The backup could not be performed because the backup script failed.
     */
    public static class BackupScriptFailed extends XenAPIException {
        public final String log;

        /**
         * Create a new BackupScriptFailed
         *
         * @param log
         */
        public BackupScriptFailed(String log) {
            super("The backup could not be performed because the backup script failed.");
            this.log = log;
        }

    }

    /**
     * The VM could not be imported because the XVA file is invalid: an unexpected file was encountered.
     */
    public static class ImportErrorUnexpectedFile extends XenAPIException {
        public final String filenameExpected;
        public final String filenameFound;

        /**
         * Create a new ImportErrorUnexpectedFile
         *
         * @param filenameExpected
         * @param filenameFound
         */
        public ImportErrorUnexpectedFile(String filenameExpected, String filenameFound) {
            super("The VM could not be imported because the XVA file is invalid: an unexpected file was encountered.");
            this.filenameExpected = filenameExpected;
            this.filenameFound = filenameFound;
        }

    }

    /**
     * External authentication for this host is already enabled.
     */
    public static class AuthAlreadyEnabled extends XenAPIException {
        public final String currentAuthType;
        public final String currentServiceName;

        /**
         * Create a new AuthAlreadyEnabled
         *
         * @param currentAuthType
         * @param currentServiceName
         */
        public AuthAlreadyEnabled(String currentAuthType, String currentServiceName) {
            super("External authentication for this host is already enabled.");
            this.currentAuthType = currentAuthType;
            this.currentServiceName = currentServiceName;
        }

    }

    /**
     * You attempted an operation that was not allowed.
     */
    public static class OperationNotAllowed extends XenAPIException {
        public final String reason;

        /**
         * Create a new OperationNotAllowed
         *
         * @param reason
         */
        public OperationNotAllowed(String reason) {
            super("You attempted an operation that was not allowed.");
            this.reason = reason;
        }

    }

    /**
     * Cannot find a plan for placement of VMs as there are no other hosts available.
     */
    public static class HaNoPlan extends XenAPIException {

        /**
         * Create a new HaNoPlan
         */
        public HaNoPlan() {
            super("Cannot find a plan for placement of VMs as there are no other hosts available.");
        }

    }

    /**
     * Some events have been lost from the queue and cannot be retrieved.
     */
    public static class EventsLost extends XenAPIException {

        /**
         * Create a new EventsLost
         */
        public EventsLost() {
            super("Some events have been lost from the queue and cannot be retrieved.");
        }

    }

    /**
     * There was an SR backend failure.
     */
    public static class SrBackendFailure extends XenAPIException {
        public final String status;
        public final String stdout;
        public final String stderr;

        /**
         * Create a new SrBackendFailure
         *
         * @param status
         * @param stdout
         * @param stderr
         */
        public SrBackendFailure(String status, String stdout, String stderr) {
            super("There was an SR backend failure.");
            this.status = status;
            this.stdout = stdout;
            this.stderr = stderr;
        }

    }

    /**
     * A timeout happened while attempting to detach a device from a VM.
     */
    public static class DeviceDetachTimeout extends XenAPIException {
        public final String type;
        public final String ref;

        /**
         * Create a new DeviceDetachTimeout
         *
         * @param type
         * @param ref
         */
        public DeviceDetachTimeout(String type, String ref) {
            super("A timeout happened while attempting to detach a device from a VM.");
            this.type = type;
            this.ref = ref;
        }

    }

    /**
     * The specified VM has a duplicate VBD device and cannot be started.
     */
    public static class VmDuplicateVbdDevice extends XenAPIException {
        public final String vm;
        public final String vbd;
        public final String device;

        /**
         * Create a new VmDuplicateVbdDevice
         *
         * @param vm
         * @param vbd
         * @param device
         */
        public VmDuplicateVbdDevice(String vm, String vbd, String device) {
            super("The specified VM has a duplicate VBD device and cannot be started.");
            this.vm = vm;
            this.vbd = vbd;
            this.device = device;
        }

    }

    /**
     * This PIF is a bond slave and cannot be plugged.
     */
    public static class CannotPlugBondSlave extends XenAPIException {
        public final String PIF;

        /**
         * Create a new CannotPlugBondSlave
         *
         * @param PIF
         */
        public CannotPlugBondSlave(String PIF) {
            super("This PIF is a bond slave and cannot be plugged.");
            this.PIF = PIF;
        }

    }

    /**
     * The VM cannot be imported unforced because it is either the same version or an older version of an existing VM.
     */
    public static class VmToImportIsNotNewerVersion extends XenAPIException {
        public final String vm;
        public final String existingVersion;
        public final String versionToImport;

        /**
         * Create a new VmToImportIsNotNewerVersion
         *
         * @param vm
         * @param existingVersion
         * @param versionToImport
         */
        public VmToImportIsNotNewerVersion(String vm, String existingVersion, String versionToImport) {
            super("The VM cannot be imported unforced because it is either the same version or an older version of an existing VM.");
            this.vm = vm;
            this.existingVersion = existingVersion;
            this.versionToImport = versionToImport;
        }

    }

    /**
     * The specified CRL is corrupt or unreadable.
     */
    public static class CrlCorrupt extends XenAPIException {
        public final String name;

        /**
         * Create a new CrlCorrupt
         *
         * @param name
         */
        public CrlCorrupt(String name) {
            super("The specified CRL is corrupt or unreadable.");
            this.name = name;
        }

    }

    /**
     * You attempted an operation on a VM which requires a more recent version of the PV drivers. Please upgrade your PV drivers.
     */
    public static class VmOldPvDrivers extends XenAPIException {
        public final String vm;
        public final String major;
        public final String minor;

        /**
         * Create a new VmOldPvDrivers
         *
         * @param vm
         * @param major
         * @param minor
         */
        public VmOldPvDrivers(String vm, String major, String minor) {
            super("You attempted an operation on a VM which requires a more recent version of the PV drivers. Please upgrade your PV drivers.");
            this.vm = vm;
            this.major = major;
            this.minor = minor;
        }

    }

    /**
     * The operation you requested cannot be performed because the specified PIF does not allow unplug.
     */
    public static class PifDoesNotAllowUnplug extends XenAPIException {
        public final String PIF;

        /**
         * Create a new PifDoesNotAllowUnplug
         *
         * @param PIF
         */
        public PifDoesNotAllowUnplug(String PIF) {
            super("The operation you requested cannot be performed because the specified PIF does not allow unplug.");
            this.PIF = PIF;
        }

    }

    /**
     * The system rejected the password change request; perhaps the new password was too short?
     */
    public static class ChangePasswordRejected extends XenAPIException {
        public final String msg;

        /**
         * Create a new ChangePasswordRejected
         *
         * @param msg
         */
        public ChangePasswordRejected(String msg) {
            super("The system rejected the password change request; perhaps the new password was too short?");
            this.msg = msg;
        }

    }

    /**
     * Another operation involving the object is currently in progress
     */
    public static class OtherOperationInProgress extends XenAPIException {
        public final String clazz;
        public final String object;

        /**
         * Create a new OtherOperationInProgress
         *
         * @param clazz
         * @param object
         */
        public OtherOperationInProgress(String clazz, String object) {
            super("Another operation involving the object is currently in progress");
            this.clazz = clazz;
            this.object = object;
        }

    }

    /**
     * Initialization of the VSS requestor failed
     */
    public static class XenVssReqErrorInitFailed extends XenAPIException {
        public final String vm;
        public final String errorCode;

        /**
         * Create a new XenVssReqErrorInitFailed
         *
         * @param vm
         * @param errorCode
         */
        public XenVssReqErrorInitFailed(String vm, String errorCode) {
            super("Initialization of the VSS requestor failed");
            this.vm = vm;
            this.errorCode = errorCode;
        }

    }

    /**
     * The CPU does not support masking of features.
     */
    public static class CpuFeatureMaskingNotSupported extends XenAPIException {
        public final String details;

        /**
         * Create a new CpuFeatureMaskingNotSupported
         *
         * @param details
         */
        public CpuFeatureMaskingNotSupported(String details) {
            super("The CPU does not support masking of features.");
            this.details = details;
        }

    }

    /**
     * The specified VM is not currently resident on the specified host.
     */
    public static class VmNotResidentHere extends XenAPIException {
        public final String vm;
        public final String host;

        /**
         * Create a new VmNotResidentHere
         *
         * @param vm
         * @param host
         */
        public VmNotResidentHere(String vm, String host) {
            super("The specified VM is not currently resident on the specified host.");
            this.vm = vm;
            this.host = host;
        }

    }

    /**
     * You attempted an operation which involves a host which could not be contacted.
     */
    public static class HostOffline extends XenAPIException {
        public final String host;

        /**
         * Create a new HostOffline
         *
         * @param host
         */
        public HostOffline(String host) {
            super("You attempted an operation which involves a host which could not be contacted.");
            this.host = host;
        }

    }

    /**
     * The pool failed to disable the external authentication of at least one host.
     */
    public static class PoolAuthDisableFailed extends XenAPIException {
        public final String host;
        public final String message;

        /**
         * Create a new PoolAuthDisableFailed
         *
         * @param host
         * @param message
         */
        public PoolAuthDisableFailed(String host, String message) {
            super("The pool failed to disable the external authentication of at least one host.");
            this.host = host;
            this.message = message;
        }

    }

    /**
     * The host failed to acquire an IP address on its management interface and therefore cannot contact the master.
     */
    public static class HostHasNoManagementIp extends XenAPIException {

        /**
         * Create a new HostHasNoManagementIp
         */
        public HostHasNoManagementIp() {
            super("The host failed to acquire an IP address on its management interface and therefore cannot contact the master.");
        }

    }

    /**
     * The tunnel transport PIF has no IP configuration set.
     */
    public static class TransportPifNotConfigured extends XenAPIException {
        public final String PIF;

        /**
         * Create a new TransportPifNotConfigured
         *
         * @param PIF
         */
        public TransportPifNotConfigured(String PIF) {
            super("The tunnel transport PIF has no IP configuration set.");
            this.PIF = PIF;
        }

    }

    /**
     * The operation could not be performed because HA is enabled on the Pool
     */
    public static class HaIsEnabled extends XenAPIException {

        /**
         * Create a new HaIsEnabled
         */
        public HaIsEnabled() {
            super("The operation could not be performed because HA is enabled on the Pool");
        }

    }

    /**
     * An error occured while reverting the specified virtual machine to the specified snapshot
     */
    public static class VmRevertFailed extends XenAPIException {
        public final String vm;
        public final String snapshot;

        /**
         * Create a new VmRevertFailed
         *
         * @param vm
         * @param snapshot
         */
        public VmRevertFailed(String vm, String snapshot) {
            super("An error occured while reverting the specified virtual machine to the specified snapshot");
            this.vm = vm;
            this.snapshot = snapshot;
        }

    }

    /**
     * The host name is invalid.
     */
    public static class HostNameInvalid extends XenAPIException {
        public final String reason;

        /**
         * Create a new HostNameInvalid
         *
         * @param reason
         */
        public HostNameInvalid(String reason) {
            super("The host name is invalid.");
            this.reason = reason;
        }

    }

    /**
     * The operation could not be performed because a domain still exists for the specified VM.
     */
    public static class DomainExists extends XenAPIException {
        public final String vm;
        public final String domid;

        /**
         * Create a new DomainExists
         *
         * @param vm
         * @param domid
         */
        public DomainExists(String vm, String domid) {
            super("The operation could not be performed because a domain still exists for the specified VM.");
            this.vm = vm;
            this.domid = domid;
        }

    }

    /**
     * This host cannot join the pool because the pool has HA enabled but this host has HA disabled.
     */
    public static class HaPoolIsEnabledButHostIsDisabled extends XenAPIException {

        /**
         * Create a new HaPoolIsEnabledButHostIsDisabled
         */
        public HaPoolIsEnabledButHostIsDisabled() {
            super("This host cannot join the pool because the pool has HA enabled but this host has HA disabled.");
        }

    }

    /**
     * This message has been deprecated.
     */
    public static class MessageDeprecated extends XenAPIException {

        /**
         * Create a new MessageDeprecated
         */
        public MessageDeprecated() {
            super("This message has been deprecated.");
        }

    }

    /**
     * This operation cannot be performed because the referenced SR is not properly shared. The SR must both be marked as shared and a currently_attached PBD must exist for each host.
     */
    public static class HaConstraintViolationSrNotShared extends XenAPIException {
        public final String SR;

        /**
         * Create a new HaConstraintViolationSrNotShared
         *
         * @param SR
         */
        public HaConstraintViolationSrNotShared(String SR) {
            super("This operation cannot be performed because the referenced SR is not properly shared. The SR must both be marked as shared and a currently_attached PBD must exist for each host.");
            this.SR = SR;
        }

    }

    /**
     * Cannot import VM using chunked encoding.
     */
    public static class ImportErrorCannotHandleChunked extends XenAPIException {

        /**
         * Create a new ImportErrorCannotHandleChunked
         */
        public ImportErrorCannotHandleChunked() {
            super("Cannot import VM using chunked encoding.");
        }

    }

    /**
     * You attempted to start a VM that's attached to more than one VDI with a timeoffset marked as reset-on-boot.
     */
    public static class VmAttachedToMoreThanOneVdiWithTimeoffsetMarkedAsResetOnBoot extends XenAPIException {
        public final String vm;

        /**
         * Create a new VmAttachedToMoreThanOneVdiWithTimeoffsetMarkedAsResetOnBoot
         *
         * @param vm
         */
        public VmAttachedToMoreThanOneVdiWithTimeoffsetMarkedAsResetOnBoot(String vm) {
            super("You attempted to start a VM that's attached to more than one VDI with a timeoffset marked as reset-on-boot.");
            this.vm = vm;
        }

    }

    /**
     * This operation is not supported during an upgrade.
     */
    public static class NotSupportedDuringUpgrade extends XenAPIException {

        /**
         * Create a new NotSupportedDuringUpgrade
         */
        public NotSupportedDuringUpgrade() {
            super("This operation is not supported during an upgrade.");
        }

    }

    /**
     * An unknown error occurred while attempting to configure an interface.
     */
    public static class PifConfigurationError extends XenAPIException {
        public final String PIF;
        public final String msg;

        /**
         * Create a new PifConfigurationError
         *
         * @param PIF
         * @param msg
         */
        public PifConfigurationError(String PIF, String msg) {
            super("An unknown error occurred while attempting to configure an interface.");
            this.PIF = PIF;
            this.msg = msg;
        }

    }

    /**
     * The specified interface cannot be used because it has no IP address
     */
    public static class InterfaceHasNoIp extends XenAPIException {
        public final String iface;

        /**
         * Create a new InterfaceHasNoIp
         *
         * @param iface
         */
        public InterfaceHasNoIp(String iface) {
            super("The specified interface cannot be used because it has no IP address");
            this.iface = iface;
        }

    }

    /**
     * The hosts in this pool are not compatible.
     */
    public static class HostsNotCompatible extends XenAPIException {

        /**
         * Create a new HostsNotCompatible
         */
        public HostsNotCompatible() {
            super("The hosts in this pool are not compatible.");
        }

    }

    /**
     * The host failed to enable external authentication.
     */
    public static class AuthEnableFailedWrongCredentials extends XenAPIException {
        public final String message;

        /**
         * Create a new AuthEnableFailedWrongCredentials
         *
         * @param message
         */
        public AuthEnableFailedWrongCredentials(String message) {
            super("The host failed to enable external authentication.");
            this.message = message;
        }

    }


    public static String toString(Object object) {
        if (object == null) {
            return null;
        }
        return (String) object;
    }

    public static Long toLong(Object object) {
        if (object == null) {
            return null;
        }
        return Long.valueOf((String) object);
    }

    public static Double toDouble(Object object) {
        if (object == null) {
            return null;
        }
        return (Double) object;
    }

    public static Boolean toBoolean(Object object) {
        if (object == null) {
            return null;
        }
        return (Boolean) object;
    }

    public static Date toDate(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return (Date) object;
        } catch (ClassCastException e){
            //Occasionally the date comes back as an ocaml float rather than
            //in the xmlrpc format! Catch this and convert.
            return (new Date((long) (1000*Double.parseDouble((String) object))));
        }
    }

    public static Types.XenAPIObjects toXenAPIObjects(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return XenAPIObjects.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return XenAPIObjects.UNRECOGNIZED;
        }
    }

    public static Types.AfterApplyGuidance toAfterApplyGuidance(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return AfterApplyGuidance.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return AfterApplyGuidance.UNRECOGNIZED;
        }
    }

    public static Types.BondMode toBondMode(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return BondMode.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return BondMode.UNRECOGNIZED;
        }
    }

    public static Types.Cls toCls(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return Cls.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return Cls.UNRECOGNIZED;
        }
    }

    public static Types.ConsoleProtocol toConsoleProtocol(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return ConsoleProtocol.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return ConsoleProtocol.UNRECOGNIZED;
        }
    }

    public static Types.EventOperation toEventOperation(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return EventOperation.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return EventOperation.UNRECOGNIZED;
        }
    }

    public static Types.HostAllowedOperations toHostAllowedOperations(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return HostAllowedOperations.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return HostAllowedOperations.UNRECOGNIZED;
        }
    }

    public static Types.IpConfigurationMode toIpConfigurationMode(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return IpConfigurationMode.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return IpConfigurationMode.UNRECOGNIZED;
        }
    }

    public static Types.Ipv6ConfigurationMode toIpv6ConfigurationMode(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return Ipv6ConfigurationMode.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return Ipv6ConfigurationMode.UNRECOGNIZED;
        }
    }

    public static Types.NetworkDefaultLockingMode toNetworkDefaultLockingMode(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return NetworkDefaultLockingMode.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return NetworkDefaultLockingMode.UNRECOGNIZED;
        }
    }

    public static Types.NetworkOperations toNetworkOperations(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return NetworkOperations.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return NetworkOperations.UNRECOGNIZED;
        }
    }

    public static Types.OnBoot toOnBoot(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OnBoot.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return OnBoot.UNRECOGNIZED;
        }
    }

    public static Types.OnCrashBehaviour toOnCrashBehaviour(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OnCrashBehaviour.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return OnCrashBehaviour.UNRECOGNIZED;
        }
    }

    public static Types.OnNormalExit toOnNormalExit(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OnNormalExit.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return OnNormalExit.UNRECOGNIZED;
        }
    }

    public static Types.PrimaryAddressType toPrimaryAddressType(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return PrimaryAddressType.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return PrimaryAddressType.UNRECOGNIZED;
        }
    }

    public static Types.StorageOperations toStorageOperations(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return StorageOperations.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return StorageOperations.UNRECOGNIZED;
        }
    }

    public static Types.TaskAllowedOperations toTaskAllowedOperations(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return TaskAllowedOperations.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return TaskAllowedOperations.UNRECOGNIZED;
        }
    }

    public static Types.TaskStatusType toTaskStatusType(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return TaskStatusType.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return TaskStatusType.UNRECOGNIZED;
        }
    }

    public static Types.VbdMode toVbdMode(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VbdMode.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VbdMode.UNRECOGNIZED;
        }
    }

    public static Types.VbdOperations toVbdOperations(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VbdOperations.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VbdOperations.UNRECOGNIZED;
        }
    }

    public static Types.VbdType toVbdType(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VbdType.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VbdType.UNRECOGNIZED;
        }
    }

    public static Types.VdiOperations toVdiOperations(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VdiOperations.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VdiOperations.UNRECOGNIZED;
        }
    }

    public static Types.VdiType toVdiType(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VdiType.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VdiType.UNRECOGNIZED;
        }
    }

    public static Types.VifLockingMode toVifLockingMode(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VifLockingMode.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VifLockingMode.UNRECOGNIZED;
        }
    }

    public static Types.VifOperations toVifOperations(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VifOperations.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VifOperations.UNRECOGNIZED;
        }
    }

    public static Types.VmApplianceOperation toVmApplianceOperation(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VmApplianceOperation.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VmApplianceOperation.UNRECOGNIZED;
        }
    }

    public static Types.VmOperations toVmOperations(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VmOperations.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VmOperations.UNRECOGNIZED;
        }
    }

    public static Types.VmPowerState toVmPowerState(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VmPowerState.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VmPowerState.UNRECOGNIZED;
        }
    }

    public static Types.VmppArchiveFrequency toVmppArchiveFrequency(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VmppArchiveFrequency.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VmppArchiveFrequency.UNRECOGNIZED;
        }
    }

    public static Types.VmppArchiveTargetType toVmppArchiveTargetType(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VmppArchiveTargetType.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VmppArchiveTargetType.UNRECOGNIZED;
        }
    }

    public static Types.VmppBackupFrequency toVmppBackupFrequency(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VmppBackupFrequency.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VmppBackupFrequency.UNRECOGNIZED;
        }
    }

    public static Types.VmppBackupType toVmppBackupType(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return VmppBackupType.valueOf(((String) object).toUpperCase().replace('-','_'));
        } catch (IllegalArgumentException ex) {
            return VmppBackupType.UNRECOGNIZED;
        }
    }

    public static Set<String> toSetOfString(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<String> result = new LinkedHashSet<String>();
        for(Object item: items) {
            String typed = toString(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Types.AfterApplyGuidance> toSetOfAfterApplyGuidance(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Types.AfterApplyGuidance> result = new LinkedHashSet<Types.AfterApplyGuidance>();
        for(Object item: items) {
            Types.AfterApplyGuidance typed = toAfterApplyGuidance(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Types.HostAllowedOperations> toSetOfHostAllowedOperations(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Types.HostAllowedOperations> result = new LinkedHashSet<Types.HostAllowedOperations>();
        for(Object item: items) {
            Types.HostAllowedOperations typed = toHostAllowedOperations(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Types.NetworkOperations> toSetOfNetworkOperations(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Types.NetworkOperations> result = new LinkedHashSet<Types.NetworkOperations>();
        for(Object item: items) {
            Types.NetworkOperations typed = toNetworkOperations(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Types.StorageOperations> toSetOfStorageOperations(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Types.StorageOperations> result = new LinkedHashSet<Types.StorageOperations>();
        for(Object item: items) {
            Types.StorageOperations typed = toStorageOperations(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Types.TaskAllowedOperations> toSetOfTaskAllowedOperations(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Types.TaskAllowedOperations> result = new LinkedHashSet<Types.TaskAllowedOperations>();
        for(Object item: items) {
            Types.TaskAllowedOperations typed = toTaskAllowedOperations(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Types.VbdOperations> toSetOfVbdOperations(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Types.VbdOperations> result = new LinkedHashSet<Types.VbdOperations>();
        for(Object item: items) {
            Types.VbdOperations typed = toVbdOperations(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Types.VdiOperations> toSetOfVdiOperations(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Types.VdiOperations> result = new LinkedHashSet<Types.VdiOperations>();
        for(Object item: items) {
            Types.VdiOperations typed = toVdiOperations(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Types.VifOperations> toSetOfVifOperations(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Types.VifOperations> result = new LinkedHashSet<Types.VifOperations>();
        for(Object item: items) {
            Types.VifOperations typed = toVifOperations(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Types.VmApplianceOperation> toSetOfVmApplianceOperation(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Types.VmApplianceOperation> result = new LinkedHashSet<Types.VmApplianceOperation>();
        for(Object item: items) {
            Types.VmApplianceOperation typed = toVmApplianceOperation(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Types.VmOperations> toSetOfVmOperations(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Types.VmOperations> result = new LinkedHashSet<Types.VmOperations>();
        for(Object item: items) {
            Types.VmOperations typed = toVmOperations(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Bond> toSetOfBond(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Bond> result = new LinkedHashSet<Bond>();
        for(Object item: items) {
            Bond typed = toBond(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<DRTask> toSetOfDRTask(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<DRTask> result = new LinkedHashSet<DRTask>();
        for(Object item: items) {
            DRTask typed = toDRTask(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<GPUGroup> toSetOfGPUGroup(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<GPUGroup> result = new LinkedHashSet<GPUGroup>();
        for(Object item: items) {
            GPUGroup typed = toGPUGroup(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<PBD> toSetOfPBD(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<PBD> result = new LinkedHashSet<PBD>();
        for(Object item: items) {
            PBD typed = toPBD(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<PCI> toSetOfPCI(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<PCI> result = new LinkedHashSet<PCI>();
        for(Object item: items) {
            PCI typed = toPCI(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<PGPU> toSetOfPGPU(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<PGPU> result = new LinkedHashSet<PGPU>();
        for(Object item: items) {
            PGPU typed = toPGPU(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<PIF> toSetOfPIF(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<PIF> result = new LinkedHashSet<PIF>();
        for(Object item: items) {
            PIF typed = toPIF(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<PIFMetrics> toSetOfPIFMetrics(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<PIFMetrics> result = new LinkedHashSet<PIFMetrics>();
        for(Object item: items) {
            PIFMetrics typed = toPIFMetrics(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<SM> toSetOfSM(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<SM> result = new LinkedHashSet<SM>();
        for(Object item: items) {
            SM typed = toSM(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<SR> toSetOfSR(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<SR> result = new LinkedHashSet<SR>();
        for(Object item: items) {
            SR typed = toSR(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VBD> toSetOfVBD(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VBD> result = new LinkedHashSet<VBD>();
        for(Object item: items) {
            VBD typed = toVBD(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VBDMetrics> toSetOfVBDMetrics(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VBDMetrics> result = new LinkedHashSet<VBDMetrics>();
        for(Object item: items) {
            VBDMetrics typed = toVBDMetrics(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VDI> toSetOfVDI(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VDI> result = new LinkedHashSet<VDI>();
        for(Object item: items) {
            VDI typed = toVDI(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VGPU> toSetOfVGPU(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VGPU> result = new LinkedHashSet<VGPU>();
        for(Object item: items) {
            VGPU typed = toVGPU(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VIF> toSetOfVIF(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VIF> result = new LinkedHashSet<VIF>();
        for(Object item: items) {
            VIF typed = toVIF(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VIFMetrics> toSetOfVIFMetrics(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VIFMetrics> result = new LinkedHashSet<VIFMetrics>();
        for(Object item: items) {
            VIFMetrics typed = toVIFMetrics(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VLAN> toSetOfVLAN(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VLAN> result = new LinkedHashSet<VLAN>();
        for(Object item: items) {
            VLAN typed = toVLAN(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VM> toSetOfVM(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VM> result = new LinkedHashSet<VM>();
        for(Object item: items) {
            VM typed = toVM(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VMPP> toSetOfVMPP(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VMPP> result = new LinkedHashSet<VMPP>();
        for(Object item: items) {
            VMPP typed = toVMPP(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VMAppliance> toSetOfVMAppliance(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VMAppliance> result = new LinkedHashSet<VMAppliance>();
        for(Object item: items) {
            VMAppliance typed = toVMAppliance(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VMGuestMetrics> toSetOfVMGuestMetrics(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VMGuestMetrics> result = new LinkedHashSet<VMGuestMetrics>();
        for(Object item: items) {
            VMGuestMetrics typed = toVMGuestMetrics(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VMMetrics> toSetOfVMMetrics(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VMMetrics> result = new LinkedHashSet<VMMetrics>();
        for(Object item: items) {
            VMMetrics typed = toVMMetrics(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<VTPM> toSetOfVTPM(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<VTPM> result = new LinkedHashSet<VTPM>();
        for(Object item: items) {
            VTPM typed = toVTPM(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Blob> toSetOfBlob(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Blob> result = new LinkedHashSet<Blob>();
        for(Object item: items) {
            Blob typed = toBlob(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Console> toSetOfConsole(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Console> result = new LinkedHashSet<Console>();
        for(Object item: items) {
            Console typed = toConsole(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Crashdump> toSetOfCrashdump(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Crashdump> result = new LinkedHashSet<Crashdump>();
        for(Object item: items) {
            Crashdump typed = toCrashdump(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Host> toSetOfHost(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Host> result = new LinkedHashSet<Host>();
        for(Object item: items) {
            Host typed = toHost(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<HostCpu> toSetOfHostCpu(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<HostCpu> result = new LinkedHashSet<HostCpu>();
        for(Object item: items) {
            HostCpu typed = toHostCpu(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<HostCrashdump> toSetOfHostCrashdump(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<HostCrashdump> result = new LinkedHashSet<HostCrashdump>();
        for(Object item: items) {
            HostCrashdump typed = toHostCrashdump(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<HostMetrics> toSetOfHostMetrics(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<HostMetrics> result = new LinkedHashSet<HostMetrics>();
        for(Object item: items) {
            HostMetrics typed = toHostMetrics(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<HostPatch> toSetOfHostPatch(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<HostPatch> result = new LinkedHashSet<HostPatch>();
        for(Object item: items) {
            HostPatch typed = toHostPatch(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Message> toSetOfMessage(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Message> result = new LinkedHashSet<Message>();
        for(Object item: items) {
            Message typed = toMessage(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Network> toSetOfNetwork(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Network> result = new LinkedHashSet<Network>();
        for(Object item: items) {
            Network typed = toNetwork(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Pool> toSetOfPool(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Pool> result = new LinkedHashSet<Pool>();
        for(Object item: items) {
            Pool typed = toPool(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<PoolPatch> toSetOfPoolPatch(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<PoolPatch> result = new LinkedHashSet<PoolPatch>();
        for(Object item: items) {
            PoolPatch typed = toPoolPatch(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Role> toSetOfRole(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Role> result = new LinkedHashSet<Role>();
        for(Object item: items) {
            Role typed = toRole(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Secret> toSetOfSecret(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Secret> result = new LinkedHashSet<Secret>();
        for(Object item: items) {
            Secret typed = toSecret(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Subject> toSetOfSubject(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Subject> result = new LinkedHashSet<Subject>();
        for(Object item: items) {
            Subject typed = toSubject(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Task> toSetOfTask(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Task> result = new LinkedHashSet<Task>();
        for(Object item: items) {
            Task typed = toTask(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Tunnel> toSetOfTunnel(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Tunnel> result = new LinkedHashSet<Tunnel>();
        for(Object item: items) {
            Tunnel typed = toTunnel(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<DataSource.Record> toSetOfDataSourceRecord(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<DataSource.Record> result = new LinkedHashSet<DataSource.Record>();
        for(Object item: items) {
            DataSource.Record typed = toDataSourceRecord(item);
            result.add(typed);
        }
        return result;
    }

    public static Set<Event.Record> toSetOfEventRecord(Object object) {
        if (object == null) {
            return null;
        }
        Object[] items = (Object[]) object;
        Set<Event.Record> result = new LinkedHashSet<Event.Record>();
        for(Object item: items) {
            Event.Record typed = toEventRecord(item);
            result.add(typed);
        }
        return result;
    }

    public static Map<String, String> toMapOfStringString(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<String,String> result = new HashMap<String,String>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            String key = toString(entry.getKey());
            String value = toString(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<String, Types.HostAllowedOperations> toMapOfStringHostAllowedOperations(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<String,Types.HostAllowedOperations> result = new HashMap<String,Types.HostAllowedOperations>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            String key = toString(entry.getKey());
            Types.HostAllowedOperations value = toHostAllowedOperations(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<String, Types.NetworkOperations> toMapOfStringNetworkOperations(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<String,Types.NetworkOperations> result = new HashMap<String,Types.NetworkOperations>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            String key = toString(entry.getKey());
            Types.NetworkOperations value = toNetworkOperations(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<String, Types.StorageOperations> toMapOfStringStorageOperations(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<String,Types.StorageOperations> result = new HashMap<String,Types.StorageOperations>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            String key = toString(entry.getKey());
            Types.StorageOperations value = toStorageOperations(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<String, Types.TaskAllowedOperations> toMapOfStringTaskAllowedOperations(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<String,Types.TaskAllowedOperations> result = new HashMap<String,Types.TaskAllowedOperations>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            String key = toString(entry.getKey());
            Types.TaskAllowedOperations value = toTaskAllowedOperations(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<String, Types.VbdOperations> toMapOfStringVbdOperations(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<String,Types.VbdOperations> result = new HashMap<String,Types.VbdOperations>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            String key = toString(entry.getKey());
            Types.VbdOperations value = toVbdOperations(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<String, Types.VdiOperations> toMapOfStringVdiOperations(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<String,Types.VdiOperations> result = new HashMap<String,Types.VdiOperations>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            String key = toString(entry.getKey());
            Types.VdiOperations value = toVdiOperations(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<String, Types.VifOperations> toMapOfStringVifOperations(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<String,Types.VifOperations> result = new HashMap<String,Types.VifOperations>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            String key = toString(entry.getKey());
            Types.VifOperations value = toVifOperations(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<String, Types.VmApplianceOperation> toMapOfStringVmApplianceOperation(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<String,Types.VmApplianceOperation> result = new HashMap<String,Types.VmApplianceOperation>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            String key = toString(entry.getKey());
            Types.VmApplianceOperation value = toVmApplianceOperation(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<String, Types.VmOperations> toMapOfStringVmOperations(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<String,Types.VmOperations> result = new HashMap<String,Types.VmOperations>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            String key = toString(entry.getKey());
            Types.VmOperations value = toVmOperations(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<String, Blob> toMapOfStringBlob(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<String,Blob> result = new HashMap<String,Blob>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            String key = toString(entry.getKey());
            Blob value = toBlob(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Long, Long> toMapOfLongLong(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Long,Long> result = new HashMap<Long,Long>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Long key = toLong(entry.getKey());
            Long value = toLong(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Long, Double> toMapOfLongDouble(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Long,Double> result = new HashMap<Long,Double>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Long key = toLong(entry.getKey());
            Double value = toDouble(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Long, Set<String>> toMapOfLongSetOfString(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Long,Set<String>> result = new HashMap<Long,Set<String>>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Long key = toLong(entry.getKey());
            Set<String> value = toSetOfString(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Types.VmOperations, String> toMapOfVmOperationsString(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Types.VmOperations,String> result = new HashMap<Types.VmOperations,String>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Types.VmOperations key = toVmOperations(entry.getKey());
            String value = toString(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Bond, Bond.Record> toMapOfBondBondRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Bond,Bond.Record> result = new HashMap<Bond,Bond.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Bond key = toBond(entry.getKey());
            Bond.Record value = toBondRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<DRTask, DRTask.Record> toMapOfDRTaskDRTaskRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<DRTask,DRTask.Record> result = new HashMap<DRTask,DRTask.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            DRTask key = toDRTask(entry.getKey());
            DRTask.Record value = toDRTaskRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<GPUGroup, GPUGroup.Record> toMapOfGPUGroupGPUGroupRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<GPUGroup,GPUGroup.Record> result = new HashMap<GPUGroup,GPUGroup.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            GPUGroup key = toGPUGroup(entry.getKey());
            GPUGroup.Record value = toGPUGroupRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<PBD, PBD.Record> toMapOfPBDPBDRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<PBD,PBD.Record> result = new HashMap<PBD,PBD.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            PBD key = toPBD(entry.getKey());
            PBD.Record value = toPBDRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<PCI, PCI.Record> toMapOfPCIPCIRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<PCI,PCI.Record> result = new HashMap<PCI,PCI.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            PCI key = toPCI(entry.getKey());
            PCI.Record value = toPCIRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<PGPU, PGPU.Record> toMapOfPGPUPGPURecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<PGPU,PGPU.Record> result = new HashMap<PGPU,PGPU.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            PGPU key = toPGPU(entry.getKey());
            PGPU.Record value = toPGPURecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<PIF, PIF.Record> toMapOfPIFPIFRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<PIF,PIF.Record> result = new HashMap<PIF,PIF.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            PIF key = toPIF(entry.getKey());
            PIF.Record value = toPIFRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<PIFMetrics, PIFMetrics.Record> toMapOfPIFMetricsPIFMetricsRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<PIFMetrics,PIFMetrics.Record> result = new HashMap<PIFMetrics,PIFMetrics.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            PIFMetrics key = toPIFMetrics(entry.getKey());
            PIFMetrics.Record value = toPIFMetricsRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<SM, SM.Record> toMapOfSMSMRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<SM,SM.Record> result = new HashMap<SM,SM.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            SM key = toSM(entry.getKey());
            SM.Record value = toSMRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<SR, SR.Record> toMapOfSRSRRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<SR,SR.Record> result = new HashMap<SR,SR.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            SR key = toSR(entry.getKey());
            SR.Record value = toSRRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VBD, VBD.Record> toMapOfVBDVBDRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VBD,VBD.Record> result = new HashMap<VBD,VBD.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VBD key = toVBD(entry.getKey());
            VBD.Record value = toVBDRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VBDMetrics, VBDMetrics.Record> toMapOfVBDMetricsVBDMetricsRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VBDMetrics,VBDMetrics.Record> result = new HashMap<VBDMetrics,VBDMetrics.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VBDMetrics key = toVBDMetrics(entry.getKey());
            VBDMetrics.Record value = toVBDMetricsRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VDI, SR> toMapOfVDISR(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VDI,SR> result = new HashMap<VDI,SR>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VDI key = toVDI(entry.getKey());
            SR value = toSR(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VDI, VDI.Record> toMapOfVDIVDIRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VDI,VDI.Record> result = new HashMap<VDI,VDI.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VDI key = toVDI(entry.getKey());
            VDI.Record value = toVDIRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VGPU, VGPU.Record> toMapOfVGPUVGPURecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VGPU,VGPU.Record> result = new HashMap<VGPU,VGPU.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VGPU key = toVGPU(entry.getKey());
            VGPU.Record value = toVGPURecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VIF, Network> toMapOfVIFNetwork(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VIF,Network> result = new HashMap<VIF,Network>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VIF key = toVIF(entry.getKey());
            Network value = toNetwork(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VIF, VIF.Record> toMapOfVIFVIFRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VIF,VIF.Record> result = new HashMap<VIF,VIF.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VIF key = toVIF(entry.getKey());
            VIF.Record value = toVIFRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VIFMetrics, VIFMetrics.Record> toMapOfVIFMetricsVIFMetricsRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VIFMetrics,VIFMetrics.Record> result = new HashMap<VIFMetrics,VIFMetrics.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VIFMetrics key = toVIFMetrics(entry.getKey());
            VIFMetrics.Record value = toVIFMetricsRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VLAN, VLAN.Record> toMapOfVLANVLANRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VLAN,VLAN.Record> result = new HashMap<VLAN,VLAN.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VLAN key = toVLAN(entry.getKey());
            VLAN.Record value = toVLANRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VM, String> toMapOfVMString(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VM,String> result = new HashMap<VM,String>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VM key = toVM(entry.getKey());
            String value = toString(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VM, Set<String>> toMapOfVMSetOfString(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VM,Set<String>> result = new HashMap<VM,Set<String>>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VM key = toVM(entry.getKey());
            Set<String> value = toSetOfString(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VM, Map<String, String>> toMapOfVMMapOfStringString(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VM,Map<String, String>> result = new HashMap<VM,Map<String, String>>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VM key = toVM(entry.getKey());
            Map<String, String> value = toMapOfStringString(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VM, VM.Record> toMapOfVMVMRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VM,VM.Record> result = new HashMap<VM,VM.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VM key = toVM(entry.getKey());
            VM.Record value = toVMRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VMPP, VMPP.Record> toMapOfVMPPVMPPRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VMPP,VMPP.Record> result = new HashMap<VMPP,VMPP.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VMPP key = toVMPP(entry.getKey());
            VMPP.Record value = toVMPPRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VMAppliance, VMAppliance.Record> toMapOfVMApplianceVMApplianceRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VMAppliance,VMAppliance.Record> result = new HashMap<VMAppliance,VMAppliance.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VMAppliance key = toVMAppliance(entry.getKey());
            VMAppliance.Record value = toVMApplianceRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VMGuestMetrics, VMGuestMetrics.Record> toMapOfVMGuestMetricsVMGuestMetricsRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VMGuestMetrics,VMGuestMetrics.Record> result = new HashMap<VMGuestMetrics,VMGuestMetrics.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VMGuestMetrics key = toVMGuestMetrics(entry.getKey());
            VMGuestMetrics.Record value = toVMGuestMetricsRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<VMMetrics, VMMetrics.Record> toMapOfVMMetricsVMMetricsRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<VMMetrics,VMMetrics.Record> result = new HashMap<VMMetrics,VMMetrics.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            VMMetrics key = toVMMetrics(entry.getKey());
            VMMetrics.Record value = toVMMetricsRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Blob, Blob.Record> toMapOfBlobBlobRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Blob,Blob.Record> result = new HashMap<Blob,Blob.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Blob key = toBlob(entry.getKey());
            Blob.Record value = toBlobRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Console, Console.Record> toMapOfConsoleConsoleRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Console,Console.Record> result = new HashMap<Console,Console.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Console key = toConsole(entry.getKey());
            Console.Record value = toConsoleRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Crashdump, Crashdump.Record> toMapOfCrashdumpCrashdumpRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Crashdump,Crashdump.Record> result = new HashMap<Crashdump,Crashdump.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Crashdump key = toCrashdump(entry.getKey());
            Crashdump.Record value = toCrashdumpRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Host, Set<String>> toMapOfHostSetOfString(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Host,Set<String>> result = new HashMap<Host,Set<String>>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Host key = toHost(entry.getKey());
            Set<String> value = toSetOfString(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Host, Host.Record> toMapOfHostHostRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Host,Host.Record> result = new HashMap<Host,Host.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Host key = toHost(entry.getKey());
            Host.Record value = toHostRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<HostCpu, HostCpu.Record> toMapOfHostCpuHostCpuRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<HostCpu,HostCpu.Record> result = new HashMap<HostCpu,HostCpu.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            HostCpu key = toHostCpu(entry.getKey());
            HostCpu.Record value = toHostCpuRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<HostCrashdump, HostCrashdump.Record> toMapOfHostCrashdumpHostCrashdumpRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<HostCrashdump,HostCrashdump.Record> result = new HashMap<HostCrashdump,HostCrashdump.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            HostCrashdump key = toHostCrashdump(entry.getKey());
            HostCrashdump.Record value = toHostCrashdumpRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<HostMetrics, HostMetrics.Record> toMapOfHostMetricsHostMetricsRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<HostMetrics,HostMetrics.Record> result = new HashMap<HostMetrics,HostMetrics.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            HostMetrics key = toHostMetrics(entry.getKey());
            HostMetrics.Record value = toHostMetricsRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<HostPatch, HostPatch.Record> toMapOfHostPatchHostPatchRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<HostPatch,HostPatch.Record> result = new HashMap<HostPatch,HostPatch.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            HostPatch key = toHostPatch(entry.getKey());
            HostPatch.Record value = toHostPatchRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Message, Message.Record> toMapOfMessageMessageRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Message,Message.Record> result = new HashMap<Message,Message.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Message key = toMessage(entry.getKey());
            Message.Record value = toMessageRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Network, Network.Record> toMapOfNetworkNetworkRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Network,Network.Record> result = new HashMap<Network,Network.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Network key = toNetwork(entry.getKey());
            Network.Record value = toNetworkRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Pool, Pool.Record> toMapOfPoolPoolRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Pool,Pool.Record> result = new HashMap<Pool,Pool.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Pool key = toPool(entry.getKey());
            Pool.Record value = toPoolRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<PoolPatch, PoolPatch.Record> toMapOfPoolPatchPoolPatchRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<PoolPatch,PoolPatch.Record> result = new HashMap<PoolPatch,PoolPatch.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            PoolPatch key = toPoolPatch(entry.getKey());
            PoolPatch.Record value = toPoolPatchRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Role, Role.Record> toMapOfRoleRoleRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Role,Role.Record> result = new HashMap<Role,Role.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Role key = toRole(entry.getKey());
            Role.Record value = toRoleRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Secret, Secret.Record> toMapOfSecretSecretRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Secret,Secret.Record> result = new HashMap<Secret,Secret.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Secret key = toSecret(entry.getKey());
            Secret.Record value = toSecretRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Subject, Subject.Record> toMapOfSubjectSubjectRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Subject,Subject.Record> result = new HashMap<Subject,Subject.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Subject key = toSubject(entry.getKey());
            Subject.Record value = toSubjectRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Task, Task.Record> toMapOfTaskTaskRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Task,Task.Record> result = new HashMap<Task,Task.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Task key = toTask(entry.getKey());
            Task.Record value = toTaskRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Map<Tunnel, Tunnel.Record> toMapOfTunnelTunnelRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map map = (Map) object;
        Map<Tunnel,Tunnel.Record> result = new HashMap<Tunnel,Tunnel.Record>();
        Set<Map.Entry> entries = map.entrySet();
        for(Map.Entry entry: entries) {
            Tunnel key = toTunnel(entry.getKey());
            Tunnel.Record value = toTunnelRecord(entry.getValue());
            result.put(key, value);
        }
        return result;
    }

    public static Bond toBond(Object object) {
        if (object == null) {
            return null;
        }
        return new Bond((String) object);
    }

    public static DRTask toDRTask(Object object) {
        if (object == null) {
            return null;
        }
        return new DRTask((String) object);
    }

    public static GPUGroup toGPUGroup(Object object) {
        if (object == null) {
            return null;
        }
        return new GPUGroup((String) object);
    }

    public static PBD toPBD(Object object) {
        if (object == null) {
            return null;
        }
        return new PBD((String) object);
    }

    public static PCI toPCI(Object object) {
        if (object == null) {
            return null;
        }
        return new PCI((String) object);
    }

    public static PGPU toPGPU(Object object) {
        if (object == null) {
            return null;
        }
        return new PGPU((String) object);
    }

    public static PIF toPIF(Object object) {
        if (object == null) {
            return null;
        }
        return new PIF((String) object);
    }

    public static PIFMetrics toPIFMetrics(Object object) {
        if (object == null) {
            return null;
        }
        return new PIFMetrics((String) object);
    }

    public static SM toSM(Object object) {
        if (object == null) {
            return null;
        }
        return new SM((String) object);
    }

    public static SR toSR(Object object) {
        if (object == null) {
            return null;
        }
        return new SR((String) object);
    }

    public static VBD toVBD(Object object) {
        if (object == null) {
            return null;
        }
        return new VBD((String) object);
    }

    public static VBDMetrics toVBDMetrics(Object object) {
        if (object == null) {
            return null;
        }
        return new VBDMetrics((String) object);
    }

    public static VDI toVDI(Object object) {
        if (object == null) {
            return null;
        }
        return new VDI((String) object);
    }

    public static VGPU toVGPU(Object object) {
        if (object == null) {
            return null;
        }
        return new VGPU((String) object);
    }

    public static VIF toVIF(Object object) {
        if (object == null) {
            return null;
        }
        return new VIF((String) object);
    }

    public static VIFMetrics toVIFMetrics(Object object) {
        if (object == null) {
            return null;
        }
        return new VIFMetrics((String) object);
    }

    public static VLAN toVLAN(Object object) {
        if (object == null) {
            return null;
        }
        return new VLAN((String) object);
    }

    public static VM toVM(Object object) {
        if (object == null) {
            return null;
        }
        return new VM((String) object);
    }

    public static VMPP toVMPP(Object object) {
        if (object == null) {
            return null;
        }
        return new VMPP((String) object);
    }

    public static VMAppliance toVMAppliance(Object object) {
        if (object == null) {
            return null;
        }
        return new VMAppliance((String) object);
    }

    public static VMGuestMetrics toVMGuestMetrics(Object object) {
        if (object == null) {
            return null;
        }
        return new VMGuestMetrics((String) object);
    }

    public static VMMetrics toVMMetrics(Object object) {
        if (object == null) {
            return null;
        }
        return new VMMetrics((String) object);
    }

    public static VTPM toVTPM(Object object) {
        if (object == null) {
            return null;
        }
        return new VTPM((String) object);
    }

    public static Blob toBlob(Object object) {
        if (object == null) {
            return null;
        }
        return new Blob((String) object);
    }

    public static Console toConsole(Object object) {
        if (object == null) {
            return null;
        }
        return new Console((String) object);
    }

    public static Crashdump toCrashdump(Object object) {
        if (object == null) {
            return null;
        }
        return new Crashdump((String) object);
    }

    public static Host toHost(Object object) {
        if (object == null) {
            return null;
        }
        return new Host((String) object);
    }

    public static HostCpu toHostCpu(Object object) {
        if (object == null) {
            return null;
        }
        return new HostCpu((String) object);
    }

    public static HostCrashdump toHostCrashdump(Object object) {
        if (object == null) {
            return null;
        }
        return new HostCrashdump((String) object);
    }

    public static HostMetrics toHostMetrics(Object object) {
        if (object == null) {
            return null;
        }
        return new HostMetrics((String) object);
    }

    public static HostPatch toHostPatch(Object object) {
        if (object == null) {
            return null;
        }
        return new HostPatch((String) object);
    }

    public static Message toMessage(Object object) {
        if (object == null) {
            return null;
        }
        return new Message((String) object);
    }

    public static Network toNetwork(Object object) {
        if (object == null) {
            return null;
        }
        return new Network((String) object);
    }

    public static Pool toPool(Object object) {
        if (object == null) {
            return null;
        }
        return new Pool((String) object);
    }

    public static PoolPatch toPoolPatch(Object object) {
        if (object == null) {
            return null;
        }
        return new PoolPatch((String) object);
    }

    public static Role toRole(Object object) {
        if (object == null) {
            return null;
        }
        return new Role((String) object);
    }

    public static Secret toSecret(Object object) {
        if (object == null) {
            return null;
        }
        return new Secret((String) object);
    }

    public static Session toSession(Object object) {
        if (object == null) {
            return null;
        }
        return new Session((String) object);
    }

    public static Subject toSubject(Object object) {
        if (object == null) {
            return null;
        }
        return new Subject((String) object);
    }

    public static Task toTask(Object object) {
        if (object == null) {
            return null;
        }
        return new Task((String) object);
    }

    public static Tunnel toTunnel(Object object) {
        if (object == null) {
            return null;
        }
        return new Tunnel((String) object);
    }

    public static User toUser(Object object) {
        if (object == null) {
            return null;
        }
        return new User((String) object);
    }

    public static Bond.Record toBondRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Bond.Record record = new Bond.Record();
            record.uuid = toString(map.get("uuid"));
            record.master = toPIF(map.get("master"));
            record.slaves = toSetOfPIF(map.get("slaves"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.primarySlave = toPIF(map.get("primary_slave"));
            record.mode = toBondMode(map.get("mode"));
            record.properties = toMapOfStringString(map.get("properties"));
            record.linksUp = toLong(map.get("links_up"));
        return record;
    }

    public static DRTask.Record toDRTaskRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        DRTask.Record record = new DRTask.Record();
            record.uuid = toString(map.get("uuid"));
            record.introducedSRs = toSetOfSR(map.get("introduced_SRs"));
        return record;
    }

    public static GPUGroup.Record toGPUGroupRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        GPUGroup.Record record = new GPUGroup.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.PGPUs = toSetOfPGPU(map.get("PGPUs"));
            record.VGPUs = toSetOfVGPU(map.get("VGPUs"));
            record.GPUTypes = toSetOfString(map.get("GPU_types"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static PBD.Record toPBDRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        PBD.Record record = new PBD.Record();
            record.uuid = toString(map.get("uuid"));
            record.host = toHost(map.get("host"));
            record.SR = toSR(map.get("SR"));
            record.deviceConfig = toMapOfStringString(map.get("device_config"));
            record.currentlyAttached = toBoolean(map.get("currently_attached"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static PCI.Record toPCIRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        PCI.Record record = new PCI.Record();
            record.uuid = toString(map.get("uuid"));
            record.clazzName = toString(map.get("class_name"));
            record.vendorName = toString(map.get("vendor_name"));
            record.deviceName = toString(map.get("device_name"));
            record.host = toHost(map.get("host"));
            record.pciId = toString(map.get("pci_id"));
            record.dependencies = toSetOfPCI(map.get("dependencies"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static PGPU.Record toPGPURecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        PGPU.Record record = new PGPU.Record();
            record.uuid = toString(map.get("uuid"));
            record.PCI = toPCI(map.get("PCI"));
            record.GPUGroup = toGPUGroup(map.get("GPU_group"));
            record.host = toHost(map.get("host"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static PIF.Record toPIFRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        PIF.Record record = new PIF.Record();
            record.uuid = toString(map.get("uuid"));
            record.device = toString(map.get("device"));
            record.network = toNetwork(map.get("network"));
            record.host = toHost(map.get("host"));
            record.MAC = toString(map.get("MAC"));
            record.MTU = toLong(map.get("MTU"));
            record.VLAN = toLong(map.get("VLAN"));
            record.metrics = toPIFMetrics(map.get("metrics"));
            record.physical = toBoolean(map.get("physical"));
            record.currentlyAttached = toBoolean(map.get("currently_attached"));
            record.ipConfigurationMode = toIpConfigurationMode(map.get("ip_configuration_mode"));
            record.IP = toString(map.get("IP"));
            record.netmask = toString(map.get("netmask"));
            record.gateway = toString(map.get("gateway"));
            record.DNS = toString(map.get("DNS"));
            record.bondSlaveOf = toBond(map.get("bond_slave_of"));
            record.bondMasterOf = toSetOfBond(map.get("bond_master_of"));
            record.VLANMasterOf = toVLAN(map.get("VLAN_master_of"));
            record.VLANSlaveOf = toSetOfVLAN(map.get("VLAN_slave_of"));
            record.management = toBoolean(map.get("management"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.disallowUnplug = toBoolean(map.get("disallow_unplug"));
            record.tunnelAccessPIFOf = toSetOfTunnel(map.get("tunnel_access_PIF_of"));
            record.tunnelTransportPIFOf = toSetOfTunnel(map.get("tunnel_transport_PIF_of"));
            record.ipv6ConfigurationMode = toIpv6ConfigurationMode(map.get("ipv6_configuration_mode"));
            record.IPv6 = toSetOfString(map.get("IPv6"));
            record.ipv6Gateway = toString(map.get("ipv6_gateway"));
            record.primaryAddressType = toPrimaryAddressType(map.get("primary_address_type"));
        return record;
    }

    public static PIFMetrics.Record toPIFMetricsRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        PIFMetrics.Record record = new PIFMetrics.Record();
            record.uuid = toString(map.get("uuid"));
            record.ioReadKbs = toDouble(map.get("io_read_kbs"));
            record.ioWriteKbs = toDouble(map.get("io_write_kbs"));
            record.carrier = toBoolean(map.get("carrier"));
            record.vendorId = toString(map.get("vendor_id"));
            record.vendorName = toString(map.get("vendor_name"));
            record.deviceId = toString(map.get("device_id"));
            record.deviceName = toString(map.get("device_name"));
            record.speed = toLong(map.get("speed"));
            record.duplex = toBoolean(map.get("duplex"));
            record.pciBusPath = toString(map.get("pci_bus_path"));
            record.lastUpdated = toDate(map.get("last_updated"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static SM.Record toSMRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        SM.Record record = new SM.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.type = toString(map.get("type"));
            record.vendor = toString(map.get("vendor"));
            record.copyright = toString(map.get("copyright"));
            record.version = toString(map.get("version"));
            record.requiredApiVersion = toString(map.get("required_api_version"));
            record.configuration = toMapOfStringString(map.get("configuration"));
            record.capabilities = toSetOfString(map.get("capabilities"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.driverFilename = toString(map.get("driver_filename"));
        return record;
    }

    public static SR.Record toSRRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        SR.Record record = new SR.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.allowedOperations = toSetOfStorageOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringStorageOperations(map.get("current_operations"));
            record.VDIs = toSetOfVDI(map.get("VDIs"));
            record.PBDs = toSetOfPBD(map.get("PBDs"));
            record.virtualAllocation = toLong(map.get("virtual_allocation"));
            record.physicalUtilisation = toLong(map.get("physical_utilisation"));
            record.physicalSize = toLong(map.get("physical_size"));
            record.type = toString(map.get("type"));
            record.contentType = toString(map.get("content_type"));
            record.shared = toBoolean(map.get("shared"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.tags = toSetOfString(map.get("tags"));
            record.smConfig = toMapOfStringString(map.get("sm_config"));
            record.blobs = toMapOfStringBlob(map.get("blobs"));
            record.localCacheEnabled = toBoolean(map.get("local_cache_enabled"));
            record.introducedBy = toDRTask(map.get("introduced_by"));
        return record;
    }

    public static VBD.Record toVBDRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VBD.Record record = new VBD.Record();
            record.uuid = toString(map.get("uuid"));
            record.allowedOperations = toSetOfVbdOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringVbdOperations(map.get("current_operations"));
            record.VM = toVM(map.get("VM"));
            record.VDI = toVDI(map.get("VDI"));
            record.device = toString(map.get("device"));
            record.userdevice = toString(map.get("userdevice"));
            record.bootable = toBoolean(map.get("bootable"));
            record.mode = toVbdMode(map.get("mode"));
            record.type = toVbdType(map.get("type"));
            record.unpluggable = toBoolean(map.get("unpluggable"));
            record.storageLock = toBoolean(map.get("storage_lock"));
            record.empty = toBoolean(map.get("empty"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.currentlyAttached = toBoolean(map.get("currently_attached"));
            record.statusCode = toLong(map.get("status_code"));
            record.statusDetail = toString(map.get("status_detail"));
            record.runtimeProperties = toMapOfStringString(map.get("runtime_properties"));
            record.qosAlgorithmType = toString(map.get("qos_algorithm_type"));
            record.qosAlgorithmParams = toMapOfStringString(map.get("qos_algorithm_params"));
            record.qosSupportedAlgorithms = toSetOfString(map.get("qos_supported_algorithms"));
            record.metrics = toVBDMetrics(map.get("metrics"));
        return record;
    }

    public static VBDMetrics.Record toVBDMetricsRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VBDMetrics.Record record = new VBDMetrics.Record();
            record.uuid = toString(map.get("uuid"));
            record.ioReadKbs = toDouble(map.get("io_read_kbs"));
            record.ioWriteKbs = toDouble(map.get("io_write_kbs"));
            record.lastUpdated = toDate(map.get("last_updated"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static VDI.Record toVDIRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VDI.Record record = new VDI.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.allowedOperations = toSetOfVdiOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringVdiOperations(map.get("current_operations"));
            record.SR = toSR(map.get("SR"));
            record.VBDs = toSetOfVBD(map.get("VBDs"));
            record.crashDumps = toSetOfCrashdump(map.get("crash_dumps"));
            record.virtualSize = toLong(map.get("virtual_size"));
            record.physicalUtilisation = toLong(map.get("physical_utilisation"));
            record.type = toVdiType(map.get("type"));
            record.sharable = toBoolean(map.get("sharable"));
            record.readOnly = toBoolean(map.get("read_only"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.storageLock = toBoolean(map.get("storage_lock"));
            record.location = toString(map.get("location"));
            record.managed = toBoolean(map.get("managed"));
            record.missing = toBoolean(map.get("missing"));
            record.parent = toVDI(map.get("parent"));
            record.xenstoreData = toMapOfStringString(map.get("xenstore_data"));
            record.smConfig = toMapOfStringString(map.get("sm_config"));
            record.isASnapshot = toBoolean(map.get("is_a_snapshot"));
            record.snapshotOf = toVDI(map.get("snapshot_of"));
            record.snapshots = toSetOfVDI(map.get("snapshots"));
            record.snapshotTime = toDate(map.get("snapshot_time"));
            record.tags = toSetOfString(map.get("tags"));
            record.allowCaching = toBoolean(map.get("allow_caching"));
            record.onBoot = toOnBoot(map.get("on_boot"));
            record.metadataOfPool = toPool(map.get("metadata_of_pool"));
            record.metadataLatest = toBoolean(map.get("metadata_latest"));
        return record;
    }

    public static VGPU.Record toVGPURecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VGPU.Record record = new VGPU.Record();
            record.uuid = toString(map.get("uuid"));
            record.VM = toVM(map.get("VM"));
            record.GPUGroup = toGPUGroup(map.get("GPU_group"));
            record.device = toString(map.get("device"));
            record.currentlyAttached = toBoolean(map.get("currently_attached"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static VIF.Record toVIFRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VIF.Record record = new VIF.Record();
            record.uuid = toString(map.get("uuid"));
            record.allowedOperations = toSetOfVifOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringVifOperations(map.get("current_operations"));
            record.device = toString(map.get("device"));
            record.network = toNetwork(map.get("network"));
            record.VM = toVM(map.get("VM"));
            record.MAC = toString(map.get("MAC"));
            record.MTU = toLong(map.get("MTU"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.currentlyAttached = toBoolean(map.get("currently_attached"));
            record.statusCode = toLong(map.get("status_code"));
            record.statusDetail = toString(map.get("status_detail"));
            record.runtimeProperties = toMapOfStringString(map.get("runtime_properties"));
            record.qosAlgorithmType = toString(map.get("qos_algorithm_type"));
            record.qosAlgorithmParams = toMapOfStringString(map.get("qos_algorithm_params"));
            record.qosSupportedAlgorithms = toSetOfString(map.get("qos_supported_algorithms"));
            record.metrics = toVIFMetrics(map.get("metrics"));
            record.MACAutogenerated = toBoolean(map.get("MAC_autogenerated"));
            record.lockingMode = toVifLockingMode(map.get("locking_mode"));
            record.ipv4Allowed = toSetOfString(map.get("ipv4_allowed"));
            record.ipv6Allowed = toSetOfString(map.get("ipv6_allowed"));
        return record;
    }

    public static VIFMetrics.Record toVIFMetricsRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VIFMetrics.Record record = new VIFMetrics.Record();
            record.uuid = toString(map.get("uuid"));
            record.ioReadKbs = toDouble(map.get("io_read_kbs"));
            record.ioWriteKbs = toDouble(map.get("io_write_kbs"));
            record.lastUpdated = toDate(map.get("last_updated"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static VLAN.Record toVLANRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VLAN.Record record = new VLAN.Record();
            record.uuid = toString(map.get("uuid"));
            record.taggedPIF = toPIF(map.get("tagged_PIF"));
            record.untaggedPIF = toPIF(map.get("untagged_PIF"));
            record.tag = toLong(map.get("tag"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static VM.Record toVMRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VM.Record record = new VM.Record();
            record.uuid = toString(map.get("uuid"));
            record.allowedOperations = toSetOfVmOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringVmOperations(map.get("current_operations"));
            record.powerState = toVmPowerState(map.get("power_state"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.userVersion = toLong(map.get("user_version"));
            record.isATemplate = toBoolean(map.get("is_a_template"));
            record.suspendVDI = toVDI(map.get("suspend_VDI"));
            record.residentOn = toHost(map.get("resident_on"));
            record.affinity = toHost(map.get("affinity"));
            record.memoryOverhead = toLong(map.get("memory_overhead"));
            record.memoryTarget = toLong(map.get("memory_target"));
            record.memoryStaticMax = toLong(map.get("memory_static_max"));
            record.memoryDynamicMax = toLong(map.get("memory_dynamic_max"));
            record.memoryDynamicMin = toLong(map.get("memory_dynamic_min"));
            record.memoryStaticMin = toLong(map.get("memory_static_min"));
            record.VCPUsParams = toMapOfStringString(map.get("VCPUs_params"));
            record.VCPUsMax = toLong(map.get("VCPUs_max"));
            record.VCPUsAtStartup = toLong(map.get("VCPUs_at_startup"));
            record.actionsAfterShutdown = toOnNormalExit(map.get("actions_after_shutdown"));
            record.actionsAfterReboot = toOnNormalExit(map.get("actions_after_reboot"));
            record.actionsAfterCrash = toOnCrashBehaviour(map.get("actions_after_crash"));
            record.consoles = toSetOfConsole(map.get("consoles"));
            record.VIFs = toSetOfVIF(map.get("VIFs"));
            record.VBDs = toSetOfVBD(map.get("VBDs"));
            record.crashDumps = toSetOfCrashdump(map.get("crash_dumps"));
            record.VTPMs = toSetOfVTPM(map.get("VTPMs"));
            record.PVBootloader = toString(map.get("PV_bootloader"));
            record.PVKernel = toString(map.get("PV_kernel"));
            record.PVRamdisk = toString(map.get("PV_ramdisk"));
            record.PVArgs = toString(map.get("PV_args"));
            record.PVBootloaderArgs = toString(map.get("PV_bootloader_args"));
            record.PVLegacyArgs = toString(map.get("PV_legacy_args"));
            record.HVMBootPolicy = toString(map.get("HVM_boot_policy"));
            record.HVMBootParams = toMapOfStringString(map.get("HVM_boot_params"));
            record.HVMShadowMultiplier = toDouble(map.get("HVM_shadow_multiplier"));
            record.platform = toMapOfStringString(map.get("platform"));
            record.PCIBus = toString(map.get("PCI_bus"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.domid = toLong(map.get("domid"));
            record.domarch = toString(map.get("domarch"));
            record.lastBootCPUFlags = toMapOfStringString(map.get("last_boot_CPU_flags"));
            record.isControlDomain = toBoolean(map.get("is_control_domain"));
            record.metrics = toVMMetrics(map.get("metrics"));
            record.guestMetrics = toVMGuestMetrics(map.get("guest_metrics"));
            record.lastBootedRecord = toString(map.get("last_booted_record"));
            record.recommendations = toString(map.get("recommendations"));
            record.xenstoreData = toMapOfStringString(map.get("xenstore_data"));
            record.haAlwaysRun = toBoolean(map.get("ha_always_run"));
            record.haRestartPriority = toString(map.get("ha_restart_priority"));
            record.isASnapshot = toBoolean(map.get("is_a_snapshot"));
            record.snapshotOf = toVM(map.get("snapshot_of"));
            record.snapshots = toSetOfVM(map.get("snapshots"));
            record.snapshotTime = toDate(map.get("snapshot_time"));
            record.transportableSnapshotId = toString(map.get("transportable_snapshot_id"));
            record.blobs = toMapOfStringBlob(map.get("blobs"));
            record.tags = toSetOfString(map.get("tags"));
            record.blockedOperations = toMapOfVmOperationsString(map.get("blocked_operations"));
            record.snapshotInfo = toMapOfStringString(map.get("snapshot_info"));
            record.snapshotMetadata = toString(map.get("snapshot_metadata"));
            record.parent = toVM(map.get("parent"));
            record.children = toSetOfVM(map.get("children"));
            record.biosStrings = toMapOfStringString(map.get("bios_strings"));
            record.protectionPolicy = toVMPP(map.get("protection_policy"));
            record.isSnapshotFromVmpp = toBoolean(map.get("is_snapshot_from_vmpp"));
            record.appliance = toVMAppliance(map.get("appliance"));
            record.startDelay = toLong(map.get("start_delay"));
            record.shutdownDelay = toLong(map.get("shutdown_delay"));
            record.order = toLong(map.get("order"));
            record.VGPUs = toSetOfVGPU(map.get("VGPUs"));
            record.attachedPCIs = toSetOfPCI(map.get("attached_PCIs"));
            record.suspendSR = toSR(map.get("suspend_SR"));
            record.version = toLong(map.get("version"));
        return record;
    }

    public static VMPP.Record toVMPPRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VMPP.Record record = new VMPP.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.isPolicyEnabled = toBoolean(map.get("is_policy_enabled"));
            record.backupType = toVmppBackupType(map.get("backup_type"));
            record.backupRetentionValue = toLong(map.get("backup_retention_value"));
            record.backupFrequency = toVmppBackupFrequency(map.get("backup_frequency"));
            record.backupSchedule = toMapOfStringString(map.get("backup_schedule"));
            record.isBackupRunning = toBoolean(map.get("is_backup_running"));
            record.backupLastRunTime = toDate(map.get("backup_last_run_time"));
            record.archiveTargetType = toVmppArchiveTargetType(map.get("archive_target_type"));
            record.archiveTargetConfig = toMapOfStringString(map.get("archive_target_config"));
            record.archiveFrequency = toVmppArchiveFrequency(map.get("archive_frequency"));
            record.archiveSchedule = toMapOfStringString(map.get("archive_schedule"));
            record.isArchiveRunning = toBoolean(map.get("is_archive_running"));
            record.archiveLastRunTime = toDate(map.get("archive_last_run_time"));
            record.VMs = toSetOfVM(map.get("VMs"));
            record.isAlarmEnabled = toBoolean(map.get("is_alarm_enabled"));
            record.alarmConfig = toMapOfStringString(map.get("alarm_config"));
            record.recentAlerts = toSetOfString(map.get("recent_alerts"));
        return record;
    }

    public static VMAppliance.Record toVMApplianceRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VMAppliance.Record record = new VMAppliance.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.allowedOperations = toSetOfVmApplianceOperation(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringVmApplianceOperation(map.get("current_operations"));
            record.VMs = toSetOfVM(map.get("VMs"));
        return record;
    }

    public static VMGuestMetrics.Record toVMGuestMetricsRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VMGuestMetrics.Record record = new VMGuestMetrics.Record();
            record.uuid = toString(map.get("uuid"));
            record.osVersion = toMapOfStringString(map.get("os_version"));
            record.PVDriversVersion = toMapOfStringString(map.get("PV_drivers_version"));
            record.PVDriversUpToDate = toBoolean(map.get("PV_drivers_up_to_date"));
            record.memory = toMapOfStringString(map.get("memory"));
            record.disks = toMapOfStringString(map.get("disks"));
            record.networks = toMapOfStringString(map.get("networks"));
            record.other = toMapOfStringString(map.get("other"));
            record.lastUpdated = toDate(map.get("last_updated"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.live = toBoolean(map.get("live"));
        return record;
    }

    public static VMMetrics.Record toVMMetricsRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VMMetrics.Record record = new VMMetrics.Record();
            record.uuid = toString(map.get("uuid"));
            record.memoryActual = toLong(map.get("memory_actual"));
            record.VCPUsNumber = toLong(map.get("VCPUs_number"));
            record.VCPUsUtilisation = toMapOfLongDouble(map.get("VCPUs_utilisation"));
            record.VCPUsCPU = toMapOfLongLong(map.get("VCPUs_CPU"));
            record.VCPUsParams = toMapOfStringString(map.get("VCPUs_params"));
            record.VCPUsFlags = toMapOfLongSetOfString(map.get("VCPUs_flags"));
            record.state = toSetOfString(map.get("state"));
            record.startTime = toDate(map.get("start_time"));
            record.installTime = toDate(map.get("install_time"));
            record.lastUpdated = toDate(map.get("last_updated"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static VTPM.Record toVTPMRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        VTPM.Record record = new VTPM.Record();
            record.uuid = toString(map.get("uuid"));
            record.VM = toVM(map.get("VM"));
            record.backend = toVM(map.get("backend"));
        return record;
    }

    public static Blob.Record toBlobRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Blob.Record record = new Blob.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.size = toLong(map.get("size"));
            record._public = toBoolean(map.get("public"));
            record.lastUpdated = toDate(map.get("last_updated"));
            record.mimeType = toString(map.get("mime_type"));
        return record;
    }

    public static Console.Record toConsoleRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Console.Record record = new Console.Record();
            record.uuid = toString(map.get("uuid"));
            record.protocol = toConsoleProtocol(map.get("protocol"));
            record.location = toString(map.get("location"));
            record.VM = toVM(map.get("VM"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static Crashdump.Record toCrashdumpRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Crashdump.Record record = new Crashdump.Record();
            record.uuid = toString(map.get("uuid"));
            record.VM = toVM(map.get("VM"));
            record.VDI = toVDI(map.get("VDI"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static DataSource.Record toDataSourceRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        DataSource.Record record = new DataSource.Record();
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.enabled = toBoolean(map.get("enabled"));
            record.standard = toBoolean(map.get("standard"));
            record.units = toString(map.get("units"));
            record.min = toDouble(map.get("min"));
            record.max = toDouble(map.get("max"));
            record.value = toDouble(map.get("value"));
        return record;
    }

    public static Event.Record toEventRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Event.Record record = new Event.Record();
            record.id = toLong(map.get("id"));
            record.timestamp = toDate(map.get("timestamp"));
            record.clazz = toString(map.get("class"));
            record.operation = toEventOperation(map.get("operation"));
            record.ref = toString(map.get("ref"));
            record.objUuid = toString(map.get("obj_uuid"));


        Object a,b;
        a=map.get("snapshot");
        switch(toXenAPIObjects(record.clazz))
        {
                case           SESSION: b =           toSessionRecord(a); break;
                case           SUBJECT: b =           toSubjectRecord(a); break;
                case              ROLE: b =              toRoleRecord(a); break;
                case              TASK: b =              toTaskRecord(a); break;
                case             EVENT: b =             toEventRecord(a); break;
                case              POOL: b =              toPoolRecord(a); break;
                case        POOL_PATCH: b =         toPoolPatchRecord(a); break;
                case                VM: b =                toVMRecord(a); break;
                case        VM_METRICS: b =         toVMMetricsRecord(a); break;
                case  VM_GUEST_METRICS: b =    toVMGuestMetricsRecord(a); break;
                case              VMPP: b =              toVMPPRecord(a); break;
                case      VM_APPLIANCE: b =       toVMApplianceRecord(a); break;
                case           DR_TASK: b =            toDRTaskRecord(a); break;
                case              HOST: b =              toHostRecord(a); break;
                case    HOST_CRASHDUMP: b =     toHostCrashdumpRecord(a); break;
                case        HOST_PATCH: b =         toHostPatchRecord(a); break;
                case      HOST_METRICS: b =       toHostMetricsRecord(a); break;
                case          HOST_CPU: b =           toHostCpuRecord(a); break;
                case           NETWORK: b =           toNetworkRecord(a); break;
                case               VIF: b =               toVIFRecord(a); break;
                case       VIF_METRICS: b =        toVIFMetricsRecord(a); break;
                case               PIF: b =               toPIFRecord(a); break;
                case       PIF_METRICS: b =        toPIFMetricsRecord(a); break;
                case              BOND: b =              toBondRecord(a); break;
                case              VLAN: b =              toVLANRecord(a); break;
                case                SM: b =                toSMRecord(a); break;
                case                SR: b =                toSRRecord(a); break;
                case               VDI: b =               toVDIRecord(a); break;
                case               VBD: b =               toVBDRecord(a); break;
                case       VBD_METRICS: b =        toVBDMetricsRecord(a); break;
                case               PBD: b =               toPBDRecord(a); break;
                case         CRASHDUMP: b =         toCrashdumpRecord(a); break;
                case              VTPM: b =              toVTPMRecord(a); break;
                case           CONSOLE: b =           toConsoleRecord(a); break;
                case              USER: b =              toUserRecord(a); break;
                case       DATA_SOURCE: b =        toDataSourceRecord(a); break;
                case              BLOB: b =              toBlobRecord(a); break;
                case           MESSAGE: b =           toMessageRecord(a); break;
                case            SECRET: b =            toSecretRecord(a); break;
                case            TUNNEL: b =            toTunnelRecord(a); break;
                case               PCI: b =               toPCIRecord(a); break;
                case              PGPU: b =              toPGPURecord(a); break;
                case         GPU_GROUP: b =          toGPUGroupRecord(a); break;
                case              VGPU: b =              toVGPURecord(a); break;
                default: throw new RuntimeException("Internal error in auto-generated code whilst unmarshalling event snapshot");
        }
        record.snapshot = b;
        return record;
    }

    public static Host.Record toHostRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Host.Record record = new Host.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.memoryOverhead = toLong(map.get("memory_overhead"));
            record.allowedOperations = toSetOfHostAllowedOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringHostAllowedOperations(map.get("current_operations"));
            record.APIVersionMajor = toLong(map.get("API_version_major"));
            record.APIVersionMinor = toLong(map.get("API_version_minor"));
            record.APIVersionVendor = toString(map.get("API_version_vendor"));
            record.APIVersionVendorImplementation = toMapOfStringString(map.get("API_version_vendor_implementation"));
            record.enabled = toBoolean(map.get("enabled"));
            record.softwareVersion = toMapOfStringString(map.get("software_version"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.capabilities = toSetOfString(map.get("capabilities"));
            record.cpuConfiguration = toMapOfStringString(map.get("cpu_configuration"));
            record.schedPolicy = toString(map.get("sched_policy"));
            record.supportedBootloaders = toSetOfString(map.get("supported_bootloaders"));
            record.residentVMs = toSetOfVM(map.get("resident_VMs"));
            record.logging = toMapOfStringString(map.get("logging"));
            record.PIFs = toSetOfPIF(map.get("PIFs"));
            record.suspendImageSr = toSR(map.get("suspend_image_sr"));
            record.crashDumpSr = toSR(map.get("crash_dump_sr"));
            record.crashdumps = toSetOfHostCrashdump(map.get("crashdumps"));
            record.patches = toSetOfHostPatch(map.get("patches"));
            record.PBDs = toSetOfPBD(map.get("PBDs"));
            record.hostCPUs = toSetOfHostCpu(map.get("host_CPUs"));
            record.cpuInfo = toMapOfStringString(map.get("cpu_info"));
            record.hostname = toString(map.get("hostname"));
            record.address = toString(map.get("address"));
            record.metrics = toHostMetrics(map.get("metrics"));
            record.licenseParams = toMapOfStringString(map.get("license_params"));
            record.haStatefiles = toSetOfString(map.get("ha_statefiles"));
            record.haNetworkPeers = toSetOfString(map.get("ha_network_peers"));
            record.blobs = toMapOfStringBlob(map.get("blobs"));
            record.tags = toSetOfString(map.get("tags"));
            record.externalAuthType = toString(map.get("external_auth_type"));
            record.externalAuthServiceName = toString(map.get("external_auth_service_name"));
            record.externalAuthConfiguration = toMapOfStringString(map.get("external_auth_configuration"));
            record.edition = toString(map.get("edition"));
            record.licenseServer = toMapOfStringString(map.get("license_server"));
            record.biosStrings = toMapOfStringString(map.get("bios_strings"));
            record.powerOnMode = toString(map.get("power_on_mode"));
            record.powerOnConfig = toMapOfStringString(map.get("power_on_config"));
            record.localCacheSr = toSR(map.get("local_cache_sr"));
            record.chipsetInfo = toMapOfStringString(map.get("chipset_info"));
            record.PCIs = toSetOfPCI(map.get("PCIs"));
            record.PGPUs = toSetOfPGPU(map.get("PGPUs"));
        return record;
    }

    public static HostCpu.Record toHostCpuRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        HostCpu.Record record = new HostCpu.Record();
            record.uuid = toString(map.get("uuid"));
            record.host = toHost(map.get("host"));
            record.number = toLong(map.get("number"));
            record.vendor = toString(map.get("vendor"));
            record.speed = toLong(map.get("speed"));
            record.modelname = toString(map.get("modelname"));
            record.family = toLong(map.get("family"));
            record.model = toLong(map.get("model"));
            record.stepping = toString(map.get("stepping"));
            record.flags = toString(map.get("flags"));
            record.features = toString(map.get("features"));
            record.utilisation = toDouble(map.get("utilisation"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static HostCrashdump.Record toHostCrashdumpRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        HostCrashdump.Record record = new HostCrashdump.Record();
            record.uuid = toString(map.get("uuid"));
            record.host = toHost(map.get("host"));
            record.timestamp = toDate(map.get("timestamp"));
            record.size = toLong(map.get("size"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static HostMetrics.Record toHostMetricsRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        HostMetrics.Record record = new HostMetrics.Record();
            record.uuid = toString(map.get("uuid"));
            record.memoryTotal = toLong(map.get("memory_total"));
            record.memoryFree = toLong(map.get("memory_free"));
            record.live = toBoolean(map.get("live"));
            record.lastUpdated = toDate(map.get("last_updated"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static HostPatch.Record toHostPatchRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        HostPatch.Record record = new HostPatch.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.version = toString(map.get("version"));
            record.host = toHost(map.get("host"));
            record.applied = toBoolean(map.get("applied"));
            record.timestampApplied = toDate(map.get("timestamp_applied"));
            record.size = toLong(map.get("size"));
            record.poolPatch = toPoolPatch(map.get("pool_patch"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static Message.Record toMessageRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Message.Record record = new Message.Record();
            record.uuid = toString(map.get("uuid"));
            record.name = toString(map.get("name"));
            record.priority = toLong(map.get("priority"));
            record.cls = toCls(map.get("cls"));
            record.objUuid = toString(map.get("obj_uuid"));
            record.timestamp = toDate(map.get("timestamp"));
            record.body = toString(map.get("body"));
        return record;
    }

    public static Network.Record toNetworkRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Network.Record record = new Network.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.allowedOperations = toSetOfNetworkOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringNetworkOperations(map.get("current_operations"));
            record.VIFs = toSetOfVIF(map.get("VIFs"));
            record.PIFs = toSetOfPIF(map.get("PIFs"));
            record.MTU = toLong(map.get("MTU"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.bridge = toString(map.get("bridge"));
            record.blobs = toMapOfStringBlob(map.get("blobs"));
            record.tags = toSetOfString(map.get("tags"));
            record.defaultLockingMode = toNetworkDefaultLockingMode(map.get("default_locking_mode"));
        return record;
    }

    public static Pool.Record toPoolRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Pool.Record record = new Pool.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.master = toHost(map.get("master"));
            record.defaultSR = toSR(map.get("default_SR"));
            record.suspendImageSR = toSR(map.get("suspend_image_SR"));
            record.crashDumpSR = toSR(map.get("crash_dump_SR"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.haEnabled = toBoolean(map.get("ha_enabled"));
            record.haConfiguration = toMapOfStringString(map.get("ha_configuration"));
            record.haStatefiles = toSetOfString(map.get("ha_statefiles"));
            record.haHostFailuresToTolerate = toLong(map.get("ha_host_failures_to_tolerate"));
            record.haPlanExistsFor = toLong(map.get("ha_plan_exists_for"));
            record.haAllowOvercommit = toBoolean(map.get("ha_allow_overcommit"));
            record.haOvercommitted = toBoolean(map.get("ha_overcommitted"));
            record.blobs = toMapOfStringBlob(map.get("blobs"));
            record.tags = toSetOfString(map.get("tags"));
            record.guiConfig = toMapOfStringString(map.get("gui_config"));
            record.wlbUrl = toString(map.get("wlb_url"));
            record.wlbUsername = toString(map.get("wlb_username"));
            record.wlbEnabled = toBoolean(map.get("wlb_enabled"));
            record.wlbVerifyCert = toBoolean(map.get("wlb_verify_cert"));
            record.redoLogEnabled = toBoolean(map.get("redo_log_enabled"));
            record.redoLogVdi = toVDI(map.get("redo_log_vdi"));
            record.vswitchController = toString(map.get("vswitch_controller"));
            record.restrictions = toMapOfStringString(map.get("restrictions"));
            record.metadataVDIs = toSetOfVDI(map.get("metadata_VDIs"));
        return record;
    }

    public static PoolPatch.Record toPoolPatchRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        PoolPatch.Record record = new PoolPatch.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.version = toString(map.get("version"));
            record.size = toLong(map.get("size"));
            record.poolApplied = toBoolean(map.get("pool_applied"));
            record.hostPatches = toSetOfHostPatch(map.get("host_patches"));
            record.afterApplyGuidance = toSetOfAfterApplyGuidance(map.get("after_apply_guidance"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static Role.Record toRoleRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Role.Record record = new Role.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.subroles = toSetOfRole(map.get("subroles"));
        return record;
    }

    public static Secret.Record toSecretRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Secret.Record record = new Secret.Record();
            record.uuid = toString(map.get("uuid"));
            record.value = toString(map.get("value"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static Session.Record toSessionRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Session.Record record = new Session.Record();
            record.uuid = toString(map.get("uuid"));
            record.thisHost = toHost(map.get("this_host"));
            record.thisUser = toUser(map.get("this_user"));
            record.lastActive = toDate(map.get("last_active"));
            record.pool = toBoolean(map.get("pool"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.isLocalSuperuser = toBoolean(map.get("is_local_superuser"));
            record.subject = toSubject(map.get("subject"));
            record.validationTime = toDate(map.get("validation_time"));
            record.authUserSid = toString(map.get("auth_user_sid"));
            record.authUserName = toString(map.get("auth_user_name"));
            record.rbacPermissions = toSetOfString(map.get("rbac_permissions"));
            record.tasks = toSetOfTask(map.get("tasks"));
            record.parent = toSession(map.get("parent"));
        return record;
    }

    public static Subject.Record toSubjectRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Subject.Record record = new Subject.Record();
            record.uuid = toString(map.get("uuid"));
            record.subjectIdentifier = toString(map.get("subject_identifier"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.roles = toSetOfRole(map.get("roles"));
        return record;
    }

    public static Task.Record toTaskRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Task.Record record = new Task.Record();
            record.uuid = toString(map.get("uuid"));
            record.nameLabel = toString(map.get("name_label"));
            record.nameDescription = toString(map.get("name_description"));
            record.allowedOperations = toSetOfTaskAllowedOperations(map.get("allowed_operations"));
            record.currentOperations = toMapOfStringTaskAllowedOperations(map.get("current_operations"));
            record.created = toDate(map.get("created"));
            record.finished = toDate(map.get("finished"));
            record.status = toTaskStatusType(map.get("status"));
            record.residentOn = toHost(map.get("resident_on"));
            record.progress = toDouble(map.get("progress"));
            record.type = toString(map.get("type"));
            record.result = toString(map.get("result"));
            record.errorInfo = toSetOfString(map.get("error_info"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
            record.subtaskOf = toTask(map.get("subtask_of"));
            record.subtasks = toSetOfTask(map.get("subtasks"));
        return record;
    }

    public static Tunnel.Record toTunnelRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        Tunnel.Record record = new Tunnel.Record();
            record.uuid = toString(map.get("uuid"));
            record.accessPIF = toPIF(map.get("access_PIF"));
            record.transportPIF = toPIF(map.get("transport_PIF"));
            record.status = toMapOfStringString(map.get("status"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }

    public static User.Record toUserRecord(Object object) {
        if (object == null) {
            return null;
        }
        Map<String,Object> map = (Map<String,Object>) object;
        User.Record record = new User.Record();
            record.uuid = toString(map.get("uuid"));
            record.shortName = toString(map.get("short_name"));
            record.fullname = toString(map.get("fullname"));
            record.otherConfig = toMapOfStringString(map.get("other_config"));
        return record;
    }


   public static Bond toBond(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toBond(parseResult(task.getResult(connection)));
    }

   public static DRTask toDRTask(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toDRTask(parseResult(task.getResult(connection)));
    }

   public static GPUGroup toGPUGroup(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toGPUGroup(parseResult(task.getResult(connection)));
    }

   public static PBD toPBD(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toPBD(parseResult(task.getResult(connection)));
    }

   public static PCI toPCI(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toPCI(parseResult(task.getResult(connection)));
    }

   public static PGPU toPGPU(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toPGPU(parseResult(task.getResult(connection)));
    }

   public static PIF toPIF(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toPIF(parseResult(task.getResult(connection)));
    }

   public static PIFMetrics toPIFMetrics(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toPIFMetrics(parseResult(task.getResult(connection)));
    }

   public static SM toSM(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toSM(parseResult(task.getResult(connection)));
    }

   public static SR toSR(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toSR(parseResult(task.getResult(connection)));
    }

   public static VBD toVBD(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVBD(parseResult(task.getResult(connection)));
    }

   public static VBDMetrics toVBDMetrics(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVBDMetrics(parseResult(task.getResult(connection)));
    }

   public static VDI toVDI(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVDI(parseResult(task.getResult(connection)));
    }

   public static VGPU toVGPU(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVGPU(parseResult(task.getResult(connection)));
    }

   public static VIF toVIF(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVIF(parseResult(task.getResult(connection)));
    }

   public static VIFMetrics toVIFMetrics(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVIFMetrics(parseResult(task.getResult(connection)));
    }

   public static VLAN toVLAN(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVLAN(parseResult(task.getResult(connection)));
    }

   public static VM toVM(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVM(parseResult(task.getResult(connection)));
    }

   public static VMPP toVMPP(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVMPP(parseResult(task.getResult(connection)));
    }

   public static VMAppliance toVMAppliance(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVMAppliance(parseResult(task.getResult(connection)));
    }

   public static VMGuestMetrics toVMGuestMetrics(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVMGuestMetrics(parseResult(task.getResult(connection)));
    }

   public static VMMetrics toVMMetrics(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVMMetrics(parseResult(task.getResult(connection)));
    }

   public static VTPM toVTPM(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toVTPM(parseResult(task.getResult(connection)));
    }

   public static Blob toBlob(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toBlob(parseResult(task.getResult(connection)));
    }

   public static Console toConsole(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toConsole(parseResult(task.getResult(connection)));
    }

   public static Crashdump toCrashdump(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toCrashdump(parseResult(task.getResult(connection)));
    }

   public static Host toHost(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toHost(parseResult(task.getResult(connection)));
    }

   public static HostCpu toHostCpu(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toHostCpu(parseResult(task.getResult(connection)));
    }

   public static HostCrashdump toHostCrashdump(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toHostCrashdump(parseResult(task.getResult(connection)));
    }

   public static HostMetrics toHostMetrics(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toHostMetrics(parseResult(task.getResult(connection)));
    }

   public static HostPatch toHostPatch(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toHostPatch(parseResult(task.getResult(connection)));
    }

   public static Message toMessage(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toMessage(parseResult(task.getResult(connection)));
    }

   public static Network toNetwork(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toNetwork(parseResult(task.getResult(connection)));
    }

   public static Pool toPool(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toPool(parseResult(task.getResult(connection)));
    }

   public static PoolPatch toPoolPatch(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toPoolPatch(parseResult(task.getResult(connection)));
    }

   public static Role toRole(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toRole(parseResult(task.getResult(connection)));
    }

   public static Secret toSecret(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toSecret(parseResult(task.getResult(connection)));
    }

   public static Session toSession(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toSession(parseResult(task.getResult(connection)));
    }

   public static Subject toSubject(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toSubject(parseResult(task.getResult(connection)));
    }

   public static Task toTask(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toTask(parseResult(task.getResult(connection)));
    }

   public static Tunnel toTunnel(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toTunnel(parseResult(task.getResult(connection)));
    }

   public static User toUser(Task task, Connection connection) throws XenAPIException, BadServerResponse, XmlRpcException, BadAsyncResult{
               return Types.toUser(parseResult(task.getResult(connection)));
    }

}
