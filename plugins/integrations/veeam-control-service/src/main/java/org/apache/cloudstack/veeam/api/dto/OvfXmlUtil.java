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

package org.apache.cloudstack.veeam.api.dto;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.cloud.api.query.vo.UserVmJoinVO;

public class OvfXmlUtil {

    private static final String NS_OVF = "http://schemas.dmtf.org/ovf/envelope/1/";
    private static final String NS_RASD = "http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_ResourceAllocationSettingData";
    private static final String NS_VSSD = "http://schemas.dmtf.org/wbem/wscim/1/cim-schema/2/CIM_VirtualSystemSettingData";
    private static final String NS_XSI = "http://www.w3.org/2001/XMLSchema-instance";

    private static final String ZERO_UUID = "00000000-0000-0000-0000-000000000000";
    private static final TimeZone UTC = TimeZone.getTimeZone("Etc/GMT");

    private static final ThreadLocal<SimpleDateFormat> OVIRT_DTF = ThreadLocal.withInitial(() -> {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.ROOT);
        sdf.setTimeZone(UTC);
        return sdf;
    });

    protected enum MemoryAllocationUnit {
        Bytes("byte", 1),
        Kilobytes("byte * 2^10", 1024),
        Megabytes("byte * 2^20", 1024 * 1024),
        Gigabytes("byte * 2^30", 1024 * 1024 * 1024);

        final String allocationUnitsToken;
        final long bytesMultiplier;

        MemoryAllocationUnit(String allocationUnitsToken, long bytesMultiplier) {
            this.allocationUnitsToken = allocationUnitsToken;
            this.bytesMultiplier = bytesMultiplier;
        }

        public String getAllocationUnitsToken() {
            return allocationUnitsToken;
        }

        public long getBytesMultiplier() {
            return bytesMultiplier;
        }

        public static MemoryAllocationUnit fromString(String value) {
            for (MemoryAllocationUnit unit : MemoryAllocationUnit.values()) {
                if (StringUtils.isNotBlank(value) &&
                        (unit.getAllocationUnitsToken().equalsIgnoreCase(value) || unit.name().equalsIgnoreCase(value))) {
                    return unit;
                }
            }
            return null;
        }
    }

    public static String toXml(final Vm vm, final UserVmJoinVO vo) {
        final String vmId = vm.getId();
        final String vmName = vm.getName();
        final String vmDesc = defaultString(vm.getDescription());

        final long creationMillis = vm.getCreationTime();
        final String creationDate = formatDate(creationMillis);
        final String exportDate = formatDate(System.currentTimeMillis());
        final String stopTime = vm.getStopTime() != null ? formatDate(vm.getStopTime()) : creationDate;
        final String bootTime = vm.getStartTime() != null ? formatDate(vm.getStartTime()) : creationDate;

        // Memory: Vm.memory is bytes (string)
        final long memBytes = parseLong(vm.getMemory(), 1024L * 1024L * 1024L);
        final long memMb = Math.max(128, memBytes / (1024L * 1024L));

        // CPU: topology cores/sockets/threads. We default sockets=1 threads=1.
        final int vcpu = Math.max(1, Integer.parseInt(vm.getCpu().getTopology().getCores()));
        final int sockets = Math.max(1, Integer.parseInt(vm.getCpu().getTopology().getSockets()));
        final int threads = Math.max(1, Integer.parseInt(vm.getCpu().getTopology().getThreads()));
        final int cpuPerSocket = Math.max(1, vcpu / sockets);
        final int maxVcpu = vcpu;

        // Template
        final Ref template = vm.getTemplate();
        final String templateId = template != null && StringUtils.isNotBlank(template.getId()) ? template.getId() : ZERO_UUID;
        final String templateName = template != null ? defaultString(template.getId()) : "Blank";

        // Snapshot id (stable per VM id)
        final String snapshotId = UUID.nameUUIDFromBytes(("ovf-snap-" + vmId).getBytes(StandardCharsets.UTF_8)).toString();

        final StringBuilder sb = new StringBuilder(16_384);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<ovf:Envelope ");
        sb.append("xmlns:ovf=\"").append(NS_OVF).append("\" ");
        sb.append("xmlns:rasd=\"").append(NS_RASD).append("\" ");
        sb.append("xmlns:vssd=\"").append(NS_VSSD).append("\" ");
        sb.append("xmlns:xsi=\"").append(NS_XSI).append("\" ovf:version=\"4.4.0.0\">");

        // --- References (from disks) ---
        sb.append("<References>");
        for (DiskAttachment da : diskAttachments(vm)) {
            if (da == null || da.getDisk() == null || StringUtils.isBlank(da.getDisk().getId())) {
                continue;
            }
            final String diskId = da.getDisk().getId();
            final String storageDomainId = firstStorageDomainId(da.getDisk());
            final String href = storageDomainId + "/" + diskId;
            sb.append("<File ovf:href=\"").append(escapeAttr(href))
                    .append("\" ovf:id=\"").append(escapeAttr(diskId))
                    .append("\" ovf:size=\"4096\" ovf:description=\"Active VM\" ovf:disk_storage_type=\"IMAGE\" ovf:cinder_volume_type=\"\"></File>");
        }
        sb.append("</References>");

        // --- NetworkSection ---
        sb.append("<NetworkSection>");
        sb.append("<Info>List of networks</Info>");
        // oVirt often lists networks, but can also be empty. We'll include known names if we can.
        for (Nic nic : nics(vm)) {
            if (nic == null) {
                continue;
            }
            final String netName = inferNetworkName(nic);
            if (StringUtils.isBlank(netName)) {
                continue;
            }
            sb.append("<Network ovf:name=\"").append(escapeAttr(netName)).append("\">");
            sb.append("<Description>").append(escapeText(defaultString(nic.getDescription()))).append("</Description>");
            sb.append("</Network>");
        }
        sb.append("</NetworkSection>");

        // --- DiskSection ---
        sb.append("<Section xsi:type=\"ovf:DiskSection_Type\">");
        sb.append("<Info>List of Virtual Disks</Info>");
        for (DiskAttachment da : diskAttachments(vm)) {
            if (da == null || da.getDisk() == null || StringUtils.isBlank(da.getDisk().getId())) {
                continue;
            }
            final org.apache.cloudstack.veeam.api.dto.Disk d = da.getDisk();
            final String diskId = d.getId();
            final String storageDomainId = firstStorageDomainId(d);
            final String href = storageDomainId + "/" + diskId;
            final long provBytes = parseLong(d.getProvisionedSize(), 0);
            final long actualBytes = parseLong(d.getActualSize(), 0);
            final long provGiB = bytesToGibCeil(provBytes);
            final long actualGiB = bytesToGibCeil(actualBytes);
            final String diskInterface = mapDiskInterface(da.getIface());

            sb.append("<Disk");
            sb.append(" ovf:diskId=\"").append(escapeAttr(diskId)).append("\"");
            sb.append(" ovf:size=\"").append(provGiB > 0 ? provGiB : 1).append("\"");
            sb.append(" ovf:actual_size=\"").append(actualGiB > 0 ? actualGiB : 1).append("\"");
            sb.append(" ovf:vm_snapshot_id=\"").append(escapeAttr(snapshotId)).append("\"");
            sb.append(" ovf:parentRef=\"\"");
            sb.append(" ovf:fileRef=\"").append(escapeAttr(href)).append("\"");
            sb.append(" ovf:format=\"").append(escapeAttr(mapOvfDiskFormat(d.getFormat(), d.getSparse()))).append("\"");
            sb.append(" ovf:volume-format=\"").append(escapeAttr(mapVolumeFormat(d.getFormat()))).append("\"");
            sb.append(" ovf:volume-type=\"").append(escapeAttr(mapVolumeType(d.getSparse()))).append("\"");
            sb.append(" ovf:disk-interface=\"").append(escapeAttr(diskInterface)).append("\"");
            sb.append(" ovf:read-only=\"").append(escapeAttr(booleanString(da.getReadOnly(), "false"))).append("\"");
            sb.append(" ovf:shareable=\"").append(escapeAttr(booleanString(d.getShareable(), "false"))).append("\"");
            sb.append(" ovf:boot=\"").append(escapeAttr(booleanString(da.getBootable(), "false"))).append("\"");
            sb.append(" ovf:pass-discard=\"").append(escapeAttr(booleanString(da.getPassDiscard(), "false"))).append("\"");
            sb.append(" ovf:incremental-backup=\"false\"");
            sb.append(" ovf:disk-alias=\"").append(escapeAttr(defaultString(d.getAlias()))).append("\"");
            sb.append(" ovf:disk-description=\"").append(escapeAttr(defaultString(d.getDescription()))).append("\"");
            sb.append(" ovf:wipe-after-delete=\"").append(escapeAttr(booleanString(d.getWipeAfterDelete(), "false"))).append("\"");
            sb.append("></Disk>");
        }
        sb.append("</Section>");

        if (vo != null) {
            // -- Add a section for CloudStack-specific metadata that some consumers might look for (e.g. for import back into CloudStack) ---
            // Add CloudStack-specific metadata section
            sb.append("<Section xsi:type=\"ovf:CloudStackMetadata_Type\">");
            sb.append("<Info>CloudStack specific metadata</Info>");
            sb.append("<CloudStack>");
            sb.append("<AccountId>").append(vo.getAccountUuid()).append("</AccountId>");
            sb.append("<DomainId>").append(vo.getDomainUuid()).append("</DomainId>");
            sb.append("<ProjectId>").append(escapeText(vo.getProjectUuid())).append("</ProjectId>");
            if (vm.getCpuProfile() != null && StringUtils.isNotBlank(vm.getCpuProfile().getId())) {
                sb.append("<ServiceOfferingId>").append(vm.getCpuProfile().getId()).append("</ServiceOfferingId>");
            }
            sb.append("<DataDiskOfferingIdMap>");
            for (DiskAttachment da : diskAttachments(vm)) {
                if (da == null || da.getDisk() == null || StringUtils.isBlank(da.getDisk().getId())) {
                    continue;
                }
                final Disk d = da.getDisk();
                sb.append("<Entry>");
                sb.append("<DiskId>").append(escapeText(d.getId())).append("</DiskId>");
                sb.append("<OfferingId>").append(d.getDiskProfile().getId()).append("</OfferingId>");
                sb.append("</Entry>");
            }
            sb.append("</DataDiskOfferingIdMap>");
            if (MapUtils.isNotEmpty(vm.getDetails())) {
                sb.append("<Details>");
                for (Map.Entry<String, String> entry : vm.getDetails().entrySet()) {
                    sb.append("<Detail>");
                    sb.append("<Key>").append(escapeText(entry.getKey())).append("</Key>");
                    sb.append("<Value>").append(escapeText(entry.getValue())).append("</Value>");
                    sb.append("</Detail>");
                }
                sb.append("</Details>");
            }
            if (vo.getUserDataId() != null) {
                sb.append("<UserDataId>").append(escapeText(vo.getUserDataUuid())).append("</UserDataId>");
            }
            if (vo.getAffinityGroupId() != null) {
                sb.append("<AffinityGroupId>").append(escapeText(vo.getAffinityGroupUuid())).append("</AffinityGroupId>");
            }
            sb.append("</CloudStack>");
            sb.append("</Section>");
        }

        // --- Content / VirtualSystem ---
        sb.append("<Content ovf:id=\"out\" xsi:type=\"ovf:VirtualSystem_Type\">");
        sb.append("<Name>").append(escapeText(vmName)).append("</Name>");
        sb.append("<Description>").append(escapeText(vmDesc)).append("</Description>");
        sb.append("<Comment></Comment>");
        sb.append("<CreationDate>").append(creationDate).append("</CreationDate>");
        sb.append("<ExportDate>").append(exportDate).append("</ExportDate>");
        sb.append("<DeleteProtected>false</DeleteProtected>");
        sb.append("<SsoMethod>guest_agent</SsoMethod>");
        sb.append("<IsSmartcardEnabled>false</IsSmartcardEnabled>");
        sb.append("<NumOfIoThreads>1</NumOfIoThreads>");
        sb.append("<TimeZone>Etc/GMT</TimeZone>");
        sb.append("<default_boot_sequence>0</default_boot_sequence>");
        sb.append("<Generation>11</Generation>");
        sb.append("<ClusterCompatibilityVersion>4.8</ClusterCompatibilityVersion>");
        sb.append("<VmType>1</VmType>");
        sb.append("<ResumeBehavior>AUTO_RESUME</ResumeBehavior>");
        sb.append("<MinAllocatedMem>").append(memMb).append("</MinAllocatedMem>");
        sb.append("<IsStateless>").append(escapeText(booleanString(vm.getStateless(), "false"))).append("</IsStateless>");
        sb.append("<IsRunAndPause>false</IsRunAndPause>");
        sb.append("<AutoStartup>false</AutoStartup>");
        sb.append("<Priority>0</Priority>");
        sb.append("<CreatedByUserId>").append(vo.getAccountUuid()).append("</CreatedByUserId>");
        sb.append("<MigrationSupport>0</MigrationSupport>");
        sb.append("<IsBootMenuEnabled>").append(escapeText(booleanString(vm.getBios() != null && vm.getBios().getBootMenu() != null ? vm.getBios().getBootMenu().getEnabled() : null, "false"))).append("</IsBootMenuEnabled>");
        sb.append("<IsSpiceFileTransferEnabled>true</IsSpiceFileTransferEnabled>");
        sb.append("<IsSpiceCopyPasteEnabled>true</IsSpiceCopyPasteEnabled>");
        sb.append("<AllowConsoleReconnect>false</AllowConsoleReconnect>");
        sb.append("<ConsoleDisconnectAction>LOCK_SCREEN</ConsoleDisconnectAction>");
        sb.append("<ConsoleDisconnectActionDelay>0</ConsoleDisconnectActionDelay>");
        sb.append("<CustomEmulatedMachine></CustomEmulatedMachine>");
        sb.append("<BiosType>").append(vm.getBios() != null ? vm.getBios().getTypeOrdinal() : 1).append("</BiosType>");
        sb.append("<CustomCpuName></CustomCpuName>");
        sb.append("<PredefinedProperties></PredefinedProperties>");
        sb.append("<UserDefinedProperties></UserDefinedProperties>");
        sb.append("<MaxMemorySizeMb>").append(memMb).append("</MaxMemorySizeMb>");
        sb.append("<MultiQueuesEnabled>true</MultiQueuesEnabled>");
        sb.append("<VirtioScsiMultiQueuesEnabled>false</VirtioScsiMultiQueuesEnabled>");
        sb.append("<UseHostCpu>false</UseHostCpu>");
        sb.append("<BalloonEnabled>").append(mapBalloonEnabled(vm)).append("</BalloonEnabled>");
        sb.append("<CpuPinningPolicy>0</CpuPinningPolicy>");
        sb.append("<ClusterName></ClusterName>");
        sb.append("<TemplateId>").append(escapeText(templateId)).append("</TemplateId>");
        sb.append("<TemplateName>").append(escapeText(templateName)).append("</TemplateName>");
        sb.append("<IsInitilized>true</IsInitilized>");
        sb.append("<Origin>3</Origin>");
        sb.append("<quota_id>").append(ZERO_UUID).append("</quota_id>");
        sb.append("<DefaultDisplayType>2</DefaultDisplayType>");
        sb.append("<TrustedService>false</TrustedService>");
        sb.append("<OriginalTemplateId>").append(escapeText(templateId)).append("</OriginalTemplateId>");
        sb.append("<OriginalTemplateName>").append(escapeText(templateName)).append("</OriginalTemplateName>");
        sb.append("<UseLatestVersion>false</UseLatestVersion>");
        sb.append("<StopTime>").append(stopTime).append("</StopTime>");
        sb.append("<BootTime>").append(bootTime).append("</BootTime>");
        sb.append("<Downtime>0</Downtime>");

        // --- Operating system section ---
        sb.append("<Section ovf:id=\"").append(escapeAttr(vmId)).append("\" ovf:required=\"false\" xsi:type=\"ovf:OperatingSystemSection_Type\">");
        sb.append("<Info>Guest Operating System</Info>");
        sb.append("<Description>").append(escapeText(inferOsDescription(vm))).append("</Description>");
        sb.append("</Section>");

        // --- Virtual hardware section ---
        sb.append("<Section xsi:type=\"ovf:VirtualHardwareSection_Type\">");
        sb.append("<Info>").append(vcpu).append(" CPU, ").append(memMb).append(" Memory</Info>");
        sb.append("<System>");
        sb.append("<vssd:VirtualSystemType>ENGINE 4.4.0.0</vssd:VirtualSystemType>");
        sb.append("</System>");

        // CPU
        sb.append("<Item>");
        sb.append("<rasd:Caption>").append(vcpu).append(" virtual cpu</rasd:Caption>");
        sb.append("<rasd:Description>Number of virtual CPU</rasd:Description>");
        sb.append("<rasd:InstanceId>1</rasd:InstanceId>");
        sb.append("<rasd:ResourceType>3</rasd:ResourceType>");
        sb.append("<rasd:num_of_sockets>").append(sockets).append("</rasd:num_of_sockets>");
        sb.append("<rasd:cpu_per_socket>").append(cpuPerSocket).append("</rasd:cpu_per_socket>");
        sb.append("<rasd:threads_per_cpu>").append(threads).append("</rasd:threads_per_cpu>");
        sb.append("<rasd:max_num_of_vcpus>").append(maxVcpu).append("</rasd:max_num_of_vcpus>");
        sb.append("<rasd:VirtualQuantity>").append(vcpu).append("</rasd:VirtualQuantity>");
        sb.append("</Item>");

        // Memory
        sb.append("<Item>");
        sb.append("<rasd:Caption>").append(memMb).append(" MB of memory</rasd:Caption>");
        sb.append("<rasd:Description>Memory Size</rasd:Description>");
        sb.append("<rasd:InstanceId>2</rasd:InstanceId>");
        sb.append("<rasd:ResourceType>4</rasd:ResourceType>");
        sb.append("<rasd:AllocationUnits>").append(MemoryAllocationUnit.Megabytes.getAllocationUnitsToken()).append("</rasd:AllocationUnits>");
        sb.append("<rasd:VirtualQuantity>").append(memMb).append("</rasd:VirtualQuantity>");
        sb.append("</Item>");

        // Disks as Items
        int diskUnit = 0;
        for (DiskAttachment da : diskAttachments(vm)) {
            if (da == null || da.getDisk() == null || StringUtils.isBlank(da.getDisk().getId())) {
                continue;
            }
            final org.apache.cloudstack.veeam.api.dto.Disk d = da.getDisk();
            final String diskId = d.getId();
            final String storageDomainId = firstStorageDomainId(d);
            final String href = storageDomainId + "/" + diskId;

            sb.append("<Item>");
            sb.append("<rasd:Caption>").append(escapeText(defaultString(d.getAlias()))).append("</rasd:Caption>");
            sb.append("<rasd:InstanceId>").append(escapeText(diskId)).append("</rasd:InstanceId>");
            sb.append("<rasd:ResourceType>17</rasd:ResourceType>");
            sb.append("<rasd:HostResource>").append(escapeText(href)).append("</rasd:HostResource>");
            sb.append("<rasd:Parent>").append(ZERO_UUID).append("</rasd:Parent>");
            sb.append("<rasd:Template>").append(escapeText(templateId)).append("</rasd:Template>");
            sb.append("<rasd:ApplicationList></rasd:ApplicationList>");
            sb.append("<rasd:StorageId>").append(escapeText(storageDomainId)).append("</rasd:StorageId>");
            sb.append("<rasd:StoragePoolId>").append(ZERO_UUID).append("</rasd:StoragePoolId>");
            sb.append("<rasd:CreationDate>").append(creationDate).append("</rasd:CreationDate>");
            sb.append("<rasd:LastModified>").append(exportDate).append("</rasd:LastModified>");
            sb.append("<rasd:last_modified_date>").append(exportDate).append("</rasd:last_modified_date>");
            sb.append("<Type>disk</Type>");
            sb.append("<Device>disk</Device>");
            sb.append("<rasd:Address>").append(escapeText("{type=drive, bus=0, controller=0, target=0, unit=" + diskUnit + "}")).append("</rasd:Address>");
            sb.append("<BootOrder>").append("true".equalsIgnoreCase(da.getBootable()) ? 1 : 0).append("</BootOrder>");
            sb.append("<IsPlugged>true</IsPlugged>");
            sb.append("<IsReadOnly>").append("true".equalsIgnoreCase(da.getReadOnly())).append("</IsReadOnly>");
            sb.append("<Alias>").append(escapeText("ua-" + href)).append("</Alias>");
            sb.append("</Item>");
            diskUnit++;
        }

        // NICs as Items
        int nicSlot = 0;
        for (Nic nic : nics(vm)) {
            if (nic == null) {
                continue;
            }
            final String nicId = firstNonBlank(nic.getId(), UUID.nameUUIDFromBytes(("nic-" + vmId + "-" + nicSlot).getBytes(StandardCharsets.UTF_8)).toString());
            final String nicName = firstNonBlank(nic.getName(), "nic" + (nicSlot + 1));
            final String mac = nic.getMac() != null ? defaultString(nic.getMac().getAddress()) : "";

            sb.append("<Item>");
            sb.append("<rasd:Caption>Ethernet adapter on [No Network]</rasd:Caption>");
            sb.append("<rasd:InstanceId>").append(escapeText(nicId)).append("</rasd:InstanceId>");
            sb.append("<rasd:ResourceType>10</rasd:ResourceType>");
            sb.append("<rasd:OtherResourceType></rasd:OtherResourceType>");
            sb.append("<rasd:ResourceSubType>").append(mapNicResourceSubType(nic.getInterfaceType())).append("</rasd:ResourceSubType>");
            sb.append("<rasd:Connection>").append(escapeText(defaultString(inferNetworkName(nic)))).append("</rasd:Connection>");
            sb.append("<rasd:Linked>").append(escapeText(booleanString(nic.getLinked(), "true"))).append("</rasd:Linked>");
            sb.append("<rasd:Name>").append(escapeText(nicName)).append("</rasd:Name>");
            sb.append("<rasd:ElementName>").append(escapeText(nicName)).append("</rasd:ElementName>");
            sb.append("<rasd:MACAddress>").append(escapeText(mac)).append("</rasd:MACAddress>");
            sb.append("<rasd:speed>10000</rasd:speed>");
            sb.append("<Type>interface</Type>");
            sb.append("<Device>bridge</Device>");
            sb.append("<rasd:Address>").append(escapeText("{type=pci, slot=0x" + String.format("%02x", nicSlot) + ", bus=0x01, domain=0x0000, function=0x0}")).append("</rasd:Address>");
            sb.append("<BootOrder>0</BootOrder>");
            sb.append("<IsPlugged>").append(escapeText(booleanString(nic.getPlugged(), "true"))).append("</IsPlugged>");
            sb.append("<IsReadOnly>false</IsReadOnly>");
            sb.append("<Alias>").append(escapeText("ua-" + nicId)).append("</Alias>");
            sb.append("</Item>");
            nicSlot++;
        }

        // A few common devices that some consumers expect to exist (kept minimal)
        // USB controller
        sb.append("<Item>");
        sb.append("<rasd:Caption>USB Controller</rasd:Caption>");
        sb.append("<rasd:InstanceId>3</rasd:InstanceId>");
        sb.append("<rasd:ResourceType>23</rasd:ResourceType>");
        sb.append("<rasd:UsbPolicy>DISABLED</rasd:UsbPolicy>");
        sb.append("</Item>");

        // RNG device
        sb.append("<Item>");
        sb.append("<rasd:ResourceType>0</rasd:ResourceType>");
        sb.append("<rasd:InstanceId>").append(UUID.nameUUIDFromBytes(("rng-" + vmId).getBytes(StandardCharsets.UTF_8))).append("</rasd:InstanceId>");
        sb.append("<Type>rng</Type>");
        sb.append("<Device>virtio</Device>");
        sb.append("<rasd:Address>{type=pci, slot=0x00, bus=0x06, domain=0x0000, function=0x0}</rasd:Address>");
        sb.append("<BootOrder>0</BootOrder>");
        sb.append("<IsPlugged>true</IsPlugged>");
        sb.append("<IsReadOnly>false</IsReadOnly>");
        sb.append("<Alias></Alias>");
        sb.append("<SpecParams><source>urandom</source></SpecParams>");
        sb.append("</Item>");

        sb.append("</Section>");
        sb.append("</Content>");
        sb.append("</ovf:Envelope>");

        return sb.toString();
    }

    public static void updateFromConfiguration(Vm vm) {
        Vm.Initialization initialization = vm.getInitialization();
        if (initialization == null) {
            return;
        }
        Vm.Initialization.Configuration configuration = vm.getInitialization().getConfiguration();
        if (configuration == null) {
            return;
        }
        OvfXmlUtil.updateFromXml(vm, configuration.getData());
    }

    protected static void updateFromXml(Vm vm, String ovfXml) {
        if (vm == null || StringUtils.isBlank(ovfXml)) {
            return;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(ovfXml.getBytes(StandardCharsets.UTF_8)));

            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();

            // Register namespace context for XPath
            xpath.setNamespaceContext(new OvfNamespaceContext());


            Node contentNode = (Node) xpath.evaluate(
                    "//*[local-name()='Content']",
                    doc,
                    XPathConstants.NODE
            );
            updateFromXmlContentNode(vm, contentNode, xpath);

            Node hwSection = (Node) xpath.evaluate(
                "//*[local-name()='Section' and @*[local-name()='type']='ovf:VirtualHardwareSection_Type']",
                doc,
                XPathConstants.NODE
            );
            updateFromXmlHardwareSection(vm, hwSection, xpath);

            Node metadataSection = (Node) xpath.evaluate(
                "//*[local-name()='Section' and @*[local-name()='type']='ovf:CloudStackMetadata_Type']",
                doc,
                XPathConstants.NODE
            );
            updateFromXmlCloudStackMetadataSection(vm, metadataSection, xpath);
        } catch (Exception e) {
            // Ignore parsing errors and keep original VM configuration
        }
    }

    private static void updateFromXmlContentNode(Vm vm, Node contentNode, XPath xpath) {
        if (contentNode == null) {
            return;
        }
        String userId = xpathString(xpath, contentNode, "./*[local-name()='CreatedByUserId']/text()");
        if (StringUtils.isNotBlank(userId)) {
            vm.setAccountId(userId);
        }
        String templateId = xpathString(xpath, contentNode, "./*[local-name()='TemplateId']/text()");
        if (StringUtils.isNotBlank(templateId)) {
            vm.setTemplate(Ref.of("", templateId));
        }
        String biosType = xpathString(xpath, contentNode, "./*[local-name()='BiosType']/text()");
        Vm.Bios bios = Vm.Bios.getBiosFromOrdinal(biosType);
        vm.setBios(bios);
    }

    private static void updateFromXmlHardwareSection(Vm vm, Node hwSection, XPath xpath) throws XPathExpressionException {
        if (hwSection == null) {
            return;
        }
        // Memory
        NodeList memItems = (NodeList) xpath.evaluate(
            ".//*[local-name()='Item'][*[local-name()='ResourceType' and text()='4']]",
                hwSection,
            XPathConstants.NODESET
        );
        if (memItems != null && memItems.getLength() > 0) {
            Node memItem = memItems.item(0);
            String memStr = childText(memItem, "VirtualQuantity");
            String memAllocationUnitsStr = childText(memItem, "AllocationUnits");
            updateVmMemory(vm, memStr, memAllocationUnitsStr);
        }

        // CPU
        NodeList cpuItems = (NodeList) xpath.evaluate(
            ".//*[local-name()='Item'][*[local-name()='ResourceType' and text()='3']]",
                hwSection,
            XPathConstants.NODESET
        );
        if (cpuItems != null && cpuItems.getLength() > 0) {
            Node cpuItem = cpuItems.item(0);
            String socketsStr = childText(cpuItem, "num_of_sockets");
            String coresStr = childText(cpuItem, "cpu_per_socket");
            String threadsStr = childText(cpuItem, "threads_per_cpu");

            if (vm.getCpu() == null) {
                vm.setCpu(new Cpu());
            }
            if (vm.getCpu().getTopology() == null) {
                vm.getCpu().setTopology(new Topology());
            }

            if (StringUtils.isNotBlank(socketsStr)) {
                vm.getCpu().getTopology().setSockets(socketsStr);
            }
            if (StringUtils.isNotBlank(coresStr)) {
                vm.getCpu().getTopology().setCores(coresStr);
            }
            if (StringUtils.isNotBlank(threadsStr)) {
                vm.getCpu().getTopology().setThreads(threadsStr);
            }
        }
    }

    private static void updateVmMemory(Vm vm, String memStr, String memAllocationUnitsStr) {
        if (StringUtils.isAnyBlank(memStr, memAllocationUnitsStr)) {
            return;
        }
        MemoryAllocationUnit memoryAllocationUnit = MemoryAllocationUnit.fromString(memAllocationUnitsStr);
        if (memoryAllocationUnit == null) {
            return;
        }
        long memory = parseLong(memStr, 0);
        if (memory == 0) {
            return;
        }
        vm.setMemory(String.valueOf(memory * memoryAllocationUnit.getBytesMultiplier()));
    }

    private static void updateFromXmlCloudStackMetadataSection(Vm vm, Node metadataSection, XPath xpath) {
        if (metadataSection == null) {
            return;
        }
        String serviceOfferingId = xpathString(xpath, metadataSection, ".//*[local-name()='ServiceOfferingId']/text()");
        if (StringUtils.isNotBlank(serviceOfferingId)) {
            vm.setCpuProfile(Ref.of("", serviceOfferingId));
        }
        String affinityGroupId = xpathString(xpath, metadataSection, ".//*[local-name()='AffinityGroupId']/text()");
        if (StringUtils.isNotBlank(affinityGroupId)) {
            vm.setAffinityGroupId(affinityGroupId);
        }
        String userDataId = xpathString(xpath, metadataSection, ".//*[local-name()='UserDataId']/text()");
        if (StringUtils.isNotBlank(userDataId)) {
            vm.setUserDataId(userDataId);
        }
        final Map<String, String> details = new HashMap<>();
        try {
            NodeList detailNodes = (NodeList) xpath.evaluate(
                    ".//*[local-name()='Details']/*[local-name()='Detail']",
                    metadataSection,
                    XPathConstants.NODESET
            );

            for (int i = 0; i < detailNodes.getLength(); i++) {
                Node detailNode = detailNodes.item(i);
                String key = xpathString(xpath, detailNode, "./*[local-name()='Key']/text()");
                if (StringUtils.isBlank(key)) {
                    continue;
                }
                String value = xpathString(xpath, detailNode, "./*[local-name()='Value']/text()");
                details.put(key, defaultString(value));
            }
        } catch (XPathExpressionException ignored) {
        }
        if (!details.isEmpty()) {
            vm.setDetails(details);
        }
    }

    private static String xpathString(XPath xpath, Node node, String expression) {
        if (node == null) {
            return null;
        }
        try {
            String value = (String) xpath.evaluate(expression, node, XPathConstants.STRING);
            return StringUtils.isBlank(value) ? null : value.trim();
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    private static String childText(Node parent, String localName) {
        if (parent == null || StringUtils.isBlank(localName)) {
            return null;
        }
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String ln = child.getLocalName();
            if (StringUtils.isBlank(ln)) {
                ln = child.getNodeName();
            }
            if (localName.equalsIgnoreCase(ln)) {
                return StringUtils.trim(child.getTextContent());
            }
        }
        return null;
    }

    private static List<DiskAttachment> diskAttachments(Vm vm) {
        if (vm.getDiskAttachments() == null) {
            return List.of();
        }
        return vm.getDiskAttachments().getItems();
    }

    private static List<Nic> nics(Vm vm) {
        if (vm.getNics() == null) {
            return List.of();
        }
        return vm.getNics().getItems();
    }

    private static String inferOsDescription(Vm vm) {
        if (vm.getOs() == null) {
            return "other";
        }
        String t = vm.getOs().getType();
        if (StringUtils.isBlank(t)) {
            return "other";
        }
        if (t.toLowerCase(Locale.ROOT).contains("win")) {
            return "windows";
        }
        if (t.toLowerCase(Locale.ROOT).contains("linux")) {
            return "linux";
        }
        return t;
    }

    private static String inferNetworkName(Nic nic) {
        return "Network-" + nic.getId();
    }

    private static String firstStorageDomainId(Disk d) {
        if (ObjectUtils.allNotNull(d, d.getStorageDomains()) && CollectionUtils.isNotEmpty(d.getStorageDomains().getItems())) {
            return d.getStorageDomains().getItems().get(0).getId();
        }
        return UUID.randomUUID().toString();
    }

    private static String mapDiskInterface(String iface) {
        if (StringUtils.isBlank(iface)) {
            return "VirtIO_SCSI";
        }
        String v = iface.toLowerCase(Locale.ROOT);
        if (v.contains("virtio") && v.contains("scsi")) {
            return "VirtIO_SCSI";
        }
        if (v.contains("virtio")) {
            return "VirtIO";
        }
        if (v.contains("ide")) {
            return "IDE";
        }
        if (v.contains("sata")) {
            return "SATA";
        }
        return iface;
    }

    private static String mapOvfDiskFormat(String format, String sparse) {
        if ("true".equalsIgnoreCase(sparse)) {
            return "http://www.vmware.com/specifications/vmdk.html#sparse";
        }
        return "http://www.vmware.com/specifications/vmdk.html#sparse";
    }

    private static String mapVolumeFormat(String format) {
        if (StringUtils.isBlank(format)) {
            return "RAW";
        }
        String f = format.toLowerCase(Locale.ROOT);
        if (f.contains("cow") || f.contains("qcow")) {
            return "COW";
        }
        if (f.contains("raw")) {
            return "RAW";
        }
        return format.toUpperCase(Locale.ROOT);
    }

    private static String mapVolumeType(String sparse) {
        return "true".equalsIgnoreCase(sparse) ? "Sparse" : "Preallocated";
    }

    private static String mapBalloonEnabled(Vm vm) {
        if (vm.getMemoryPolicy() == null || vm.getMemoryPolicy().getBallooning() == null) {
            return "true";
        }
        return "true".equalsIgnoreCase(vm.getMemoryPolicy().getBallooning()) ? "true" : "false";
    }

    private static int mapNicResourceSubType(String iface) {
        if (StringUtils.isBlank(iface)) {
            return 3;
        }
        String v = iface.toLowerCase(Locale.ROOT);
        if (v.contains("virtio")) {
            return 3;
        }
        return 3;
    }

    private static String booleanString(String v, String def) {
        if (StringUtils.isBlank(v)) {
            return def;
        }
        if ("true".equalsIgnoreCase(v)) {
            return "true";
        }
        if ("false".equalsIgnoreCase(v)) {
            return "false";
        }
        return def;
    }

    private static String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (StringUtils.isNotBlank(v)) {
                return v;
            }
        }
        return "";
    }

    private static String defaultString(String s) {
        return s == null ? "" : s;
    }

    private static long parseLong(String s, long def) {
        if (StringUtils.isBlank(s)) {
            return def;
        }
        try {
            return Long.parseLong(s);
        } catch (Exception ignored) {
            return def;
        }
    }

    private static long bytesToGibCeil(long bytes) {
        if (bytes <= 0) {
            return 0;
        }
        final long gib = 1024L * 1024L * 1024L;
        return (bytes + gib - 1) / gib;
    }

    private static String formatDate(long epochMillis) {
        return OVIRT_DTF.get().format(new Date(epochMillis));
    }

    private static String escapeText(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String escapeAttr(String s) {
        return escapeText(s);
    }

    protected static class OvfNamespaceContext implements NamespaceContext {
        @Override
        public String getNamespaceURI(String prefix) {
            if ("ovf".equals(prefix)) return NS_OVF;
            if ("rasd".equals(prefix)) return NS_RASD;
            if ("vssd".equals(prefix)) return NS_VSSD;
            if ("xsi".equals(prefix)) return NS_XSI;
            return XMLConstants.NULL_NS_URI;
        }
        @Override
        public String getPrefix(String namespaceURI) {
            return null;
        }
        @Override
        public java.util.Iterator<String> getPrefixes(String namespaceURI) {
            return null;
        }
    }
}
