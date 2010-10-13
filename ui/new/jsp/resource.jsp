<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>
<!-- domain detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <!--  
    <div class="main_titleicon">
        <img src="images/title_snapshoticon.gif" alt="Instance" /></div>
    -->
    <h1>
        Resources
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div id="zone_page">
        <div class="tabbox" style="margin-top: 15px;">
            <div class="content_tabs on" id="tab_details">
                <%=t.t("details")%></div>
            <div class="content_tabs off" id="tab_network">
                <%=t.t("network")%></div>
            <div class="content_tabs off" id="tab_secondary_storage">
                <%=t.t("secondary.storage")%></div>
        </div>
        <!-- Details tab (start)-->
        <div id="tab_content_details">
            <div class="grid_container">
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("ID")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="id">
                        </div>
                    </div>
                </div>
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("Name")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="name">
                        </div>
                    </div>
                </div>
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("Accounts")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="redirect_to_account_page">
                        </div>
                    </div>
                </div>
                <div class="grid_rows even">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("Instances")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="redirect_to_instance_page">
                        </div>
                    </div>
                </div>
                <div class="grid_rows odd">
                    <div class="grid_row_cell" style="width: 20%;">
                        <div class="row_celltitles">
                            <%=t.t("Volume")%>:</div>
                    </div>
                    <div class="grid_row_cell" style="width: 79%;">
                        <div class="row_celltitles" id="redirect_to_volume_page">
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!-- Details tab (end)-->
        <!-- Network tab (start)-->
        <div style="display: none;" id="tab_content_network">
            <div class="text_container">
                <div class="network_dgbox">
                    <div class="networkdg_zonepanel">
                        <div class="networkdg_zonebox">
                            <div class="networkdg_zonecloud">
                                <p>
                                    Zone 1</p>
                            </div>
                            <div class="networkdg_zoneconnect">
                            </div>
                        </div>
                        <div class="networkswitchpanel">
                            <div class="networkswitch_titlebox">
                                <p>
                                    Guest VLAN 1000 -1001</p>
                            </div>
                            <div class="networkswitch_top">
                            </div>
                            <div class="networkswitch_midpanel">
                                <div class="networkswitch_vlanpanel">
                                    <div class="networkswitch_vlanconnect">
                                        <div class="networkswitch_vlan_infoicon">
                                        </div>
                                        <div class="networkswitch_vlan_detailsbox">
                                            <div class="networkswitch_vlan_detailsbox_textbox">
                                                <div class="networkswitch_vlan_detailsbox_textbox_label">
                                                    VM Group:</div>
                                                <span>VLAN 10</span>
                                            </div>
                                            <div class="networkswitch_vlan_detailsbox_textbox">
                                                <div class="networkswitch_vlan_detailsbox_textbox_label">
                                                    IP:</div>
                                                <span>10.101.24.231</span>
                                            </div>
                                        </div>
                                        <div class="networkswitch_typeicon direct">
                                        </div>
                                    </div>
                                </div>
                                <div class="networkswitch_vlanpanel">
                                    <div class="networkswitch_vlanconnect">
                                        <div class="networkswitch_vlan_infoicon">
                                        </div>
                                        <div class="networkswitch_vlan_detailsbox">
                                            <div class="networkswitch_vlan_detailsbox_textbox">
                                                <div class="networkswitch_vlan_detailsbox_textbox_label">
                                                    VM Group:</div>
                                                <span>VLAN 10</span>
                                            </div>
                                            <div class="networkswitch_vlan_detailsbox_textbox">
                                                <div class="networkswitch_vlan_detailsbox_textbox_label">
                                                    IP:</div>
                                                <span>10.101.24.231</span>
                                            </div>
                                        </div>
                                        <div class="networkswitch_typeicon virtual">
                                        </div>
                                    </div>
                                </div>
                            </div>
                            <div class="networkswitch_end">
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <!-- Network tab (end)-->
        <!-- Secondary Storage tab (start)-->
        <div id="tab_content_secondary_storage" style="display: none">
            Secondary Storage
        </div>
        <!-- Secondary Storage tab (end)-->
    </div>
    <div id="pod_page" style="display:none">
        Pod Page
    </div>
    <div id="cluster_page" style="display:none">
        Cluster Page
    </div>
    <div id="host_page" style="display:none">
        Host page
    </div>
    <div id="primarystorage_page" style="display:none">
        Primary Storage Page
    </div>
    <div id="systemvm_page" style="display:none">
        System VM Page
    </div>    
</div>
<!-- domain detail panel (end) -->
<!-- treenode template (begin) -->
<div id="treenode_template" class="tree_levelspanel" style="display: none">
    <div class="tree_levelsbox" style="margin-left: 20px;">
        <div id="domain_title_container" class="tree_levels">
            <div id="domain_expand_icon" class="zonetree_closedarrows">
            </div>
            <div id="domain_name" class="tree_links">
                Domain Name</div>
        </div>
        <div id="domain_children_container" style="display: none">
        </div>
    </div>
</div>
<!-- treenode template (end) -->
<!-- admin account tab template (begin) -->
<div class="grid_container" id="admin_account_tab_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="title">
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("ID")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="id">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Role")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="role">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Account")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="account">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Domain")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="domain">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("VMs")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="vm_total">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("IPs")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="ip_total">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Bytes.Received")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="bytes_received">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("Bytes.Sent")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="bytes_sent">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                <%=t.t("State")%>:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="state">
            </div>
        </div>
    </div>
</div>
<!-- admin account tab template (end) -->
<!-- Zonetree Template (begin) -->
<div class="zonetree_contentbox" id="zonetree" style="display: none">
    <div id="zones_container">
    </div>
</div>
<!-- Zonetree Template (end) -->
<!-- Zone Template (begin) -->
<div id="zone_template" style="display: none">
    <div class="adding_loading" style="height: 25px; display: none" id="loading_container">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Adding a zone &hellip;
        </div>
    </div>
    <div id="row_container">
        <div class="leftmenu_content" id="zone_node">
            <div class="leftmenu_firstindent">
                <div class="zonetree_closedarrows" id="zone_expand">
                </div>
                <div class="leftmenu_list_icons">
                    <img src="images/zone_zoneicon.png" id="zone_icon" alt="Zone" /></div>
                <span id="zone_label">Zone:</span> <strong><span id="zone_name">Zone 1</span> </strong>
            </div>
        </div>
        <div id="zone_content" style="display: none">
            <div id="pods_container">
            </div>
            <div id="systemvms_container">
            </div>
        </div>
    </div>
</div>
<!-- Zone Template (end) -->
<!-- Pod Template (begin) -->
<div id="pod_template" style="display: none">
    <div class="adding_loading" style="height: 25px; display: none" id="loading_container">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Adding a pod &hellip;
        </div>
    </div>
    <div id="row_container">
        <div class="leftmenu_content" id="pod_node">
            <div class="leftmenu_secondindent">
                <div class="zonetree_closedarrows" id="pod_expand">
                </div>
                <div class="leftmenu_list_icons">
                    <img src="images/zone_podicon.png" id="pod_icon" alt="Pod" /></div>
                <span id="pod_label">Pod:</span> <strong><span id="pod_name">Name of the Pod</span>
                </strong>
            </div>
        </div>
        <div id="pod_content" style="display: none">
            <div id="clusters_container">
            </div>
        </div>
    </div>
</div>
<!-- Pod Template (end) -->
<!-- Cluster Template (begin) -->
<div id="cluster_template" style="display: none">
    <div class="adding_loading" style="height: 25px; display: none" id="loading_container">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Adding a cluster &hellip;
        </div>
    </div>
    <div id="row_container">
        <div class="leftmenu_content" id="cluster_node">
            <div class="leftmenu_thirdindent">
                <div class="zonetree_openarrows" id="cluster_expand">
                </div>
                <div class="leftmenu_list_icons">
                    <img src="images/zone_clustericon.png" id="cluster_icon" alt="Cluster" /></div>
                <span id="cluster_label"">Cluster:</span> <strong><span id="cluster_name">(Name of the
                    Cluster)</span> </strong>
            </div>
        </div>
        <div id="cluster_content">
            <div id="hosts_container">
            </div>
            <div id="primarystorages_container">
            </div>
        </div>
    </div>
</div>
<!-- Cluster Template (end) -->
<!-- Host Template (begin) -->
<div id="host_template" style="display: none">
    <div class="adding_loading" style="height: 25px; display: none" id="loading_container">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Adding a host &hellip;
        </div>
    </div>
    <div id="row_container">
        <div class="leftmenu_content" id="host_node">
            <div class="leftmenu_fourthindent">
                <div class="leftmenu_list_icons">
                    <img src="images/zone_hosticon.png" id="host_icon" alt="Host" /></div>
                <span id="host_label">Host:</span> <strong><span id="host_name">(Name of the Host)</span>
                </strong>
            </div>
        </div>
    </div>
</div>
<!-- Host Template (end) -->
<!-- Primary Storage Template (begin) -->
<div id="primarystorage_template" style="display: none">
    <div class="adding_loading" style="height: 25px; display: none" id="loading_container">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Adding a primary storage &hellip;
        </div>
    </div>
    <div id="row_container">
        <div class="leftmenu_content" id="primarystorage_node">
            <div class="leftmenu_fourthindent">
                <div class="leftmenu_list_icons">
                    <img src="images/zone_primarystorageicon.png" id="primarystorage_icon" alt="PrimaryStorage" /></div>
                <span id="primarystorage_label">Storage:</span> <strong><span id="primarystorage_name">
                    (Name of the Primary Storage)</span> </strong>
            </div>
        </div>
    </div>
</div>
<!-- Primary Storage Template (end) -->
<!-- SystemVM Template (begin) -->
<div id="systemvm_template" style="display: none">
    <div id="row_container">
        <div class="leftmenu_content" id="systemvm_node">
            <div class="leftmenu_secondindent">
                <div class="leftmenu_list_icons">
                    <img src="images/zone_systemvmicon.png" id="systemvm_icon" alt="System VM" /></div>
                <span id="systemvm_label">System VM:</span> <strong><span id="systemvm_name">(System
                    VM name)</span> </strong>
            </div>
        </div>
    </div>
</div>
<!-- SystemVM Template (end) -->
