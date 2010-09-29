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
        Domain
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on" id="tab_details">
            <%=t.t("Details")%></div>
        <div class="content_tabs off" id="tab_admin_account">
            <%=t.t("Admin.Accounts")%></div>
        <div class="content_tabs off" id="tab_resource_limits">
            <%=t.t("Resource.Limits")%></div>
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
            <div class="grid_rows even">
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
    
    <!-- Admin Account tab (start)-->
    <div style="display: none;" id="tab_content_admin_account">
    </div>
    <!-- Admin Account tab (end)-->
    
    <!-- Resource Limits tab (start)-->
    <div id="tab_content_resource_limits" style="display:none">
        <div class="grid_actionpanel">            
            <div class="grid_editbox" id="edit_button">
            </div>
            <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
                display: none;">
                <div class="gridheader_loader" id="Div1">
                </div>
                <p id="description">
                    Updating Resource Limits.... &hellip;</p>
            </div>               
        </div>    
        <div class="grid_container">           
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("Instance.Limit")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="limits_vm">
                    </div>
                    <input class="text" id="limits_vm_edit" value="-1" style="width: 200px; display: none;" type="text" />
                    <div id="limits_vm_edit_errormsg" style="display:none"></div>
                </div>
            </div>            
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("Public.IP.Limit")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="limits_ip">
                    </div>
                    <input class="text" id="limits_ip_edit" value="-1" style="width: 200px; display: none;" type="text" />
                    <div id="limits_ip_edit_errormsg" style="display:none"></div>
                </div>
            </div>            
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("Disk.Volume.Limit")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="limits_volume">
                    </div>
                    <input class="text" id="limits_volume_edit" value="-1" style="width: 200px; display: none;" type="text" />
                    <div id="limits_volume_edit_errormsg" style="display:none"></div>
                </div>
            </div>            
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("Snapshot.Limit")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="limits_snapshot">
                    </div>
                    <input class="text" id="limits_snapshot_edit" value="-1" style="width: 200px; display: none;" type="text" />
                    <div id="limits_snapshot_edit_errormsg" style="display:none"></div>
                </div>
            </div>            
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        <%=t.t("Template.Limit")%>:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="limits_template">
                    </div>
                    <input class="text" id="limits_template_edit" value="-1" style="width: 200px; display: none;" type="text" />
                    <div id="limits_template_edit_errormsg" style="display:none"></div>
                </div>
            </div>
        </div>
        <div class="grid_botactionpanel">
        	<div class="gridbot_buttons" id="save_button" style="display:none;">Save</div>
            <div class="gridbot_buttons" id="cancel_button" style="display:none;">Cancel</div>
        </div>
    </div>
    <!-- Resource Limits tab (start)-->    
</div>
<!-- domain detail panel (end) -->

<!-- treenode template (begin) -->
<div id="treenode_template" class="tree_levelspanel" style="display:none">
	<div class="tree_levelsbox" style="margin-left:20px;">
        <div id="domain_title_container" class="tree_levels">
            <div id="domain_expand_icon" class="zonetree_closedarrows"></div>
            <div id="domain_name" class="tree_links">Domain Name</div>
        </div>
		<div id="domain_children_container" style="display:none">
		</div>   
    </div>
</div>
<!-- treenode template (end) -->

<!-- admin account tab template (begin) -->
<div class="grid_container" id="admin_account_tab_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="name">
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