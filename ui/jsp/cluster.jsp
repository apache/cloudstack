<%@ page import="java.util.*" %>
<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<div class="main_title" id="right_panel_header">
   
    <div class="main_titleicon">
        <img src="images/title_clustericon.gif"/></div>
   
    <h1>Cluster
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top:15px;">
        <div class="content_tabs on">
            <%=t.t("Details")%></div>        
    </div>    
    <div id="tab_content_details">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    Loading &hellip;</p>
            </div>
        </div>   
        <div id="tab_container">
		    <div class="grid_container">
		        <div class="grid_header">
	                <div id="grid_header_title" class="grid_header_title">
	                    (title)</div>
	                <div class="grid_actionbox" id="action_link"><p>Actions</p>
	                    <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
	                        <ul class="actionsdropdown_boxlist" id="action_list">
	                            <li>
	                                <%=t.t("no.available.actions")%></li>
	                        </ul>
	                    </div>
	                </div>
	                <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
	                    display: none;">
	                    <div class="gridheader_loader" id="icon">
	                    </div>
	                    <p id="description">
	                        Waiting &hellip;</p>
	                </div>
	            </div>
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
		                    <%=t.t("name")%>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="name">
		                </div>
		            </div>
		        </div>
		        <div class="grid_rows odd">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("zone")%>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="zonename">
		                    </div>
		            </div>
		        </div>
		        <div class="grid_rows even">
		            <div class="grid_row_cell" style="width: 20%;">
		                <div class="row_celltitles">
		                    <%=t.t("pod")%>:</div>
		            </div>
		            <div class="grid_row_cell" style="width: 79%;">
		                <div class="row_celltitles" id="podname">
		                </div>
		            </div>
		        </div>		        
		    </div>
		</div>    
    </div>     
</div>

<!--  top buttons (begin) -->
<div id="top_buttons"> 
    <div class="actionpanel_button_wrapper" id="add_host_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Host" /></div>
            <div class="actionpanel_button_links">
                Add Host
            </div>
        </div>
    </div>
    <div class="actionpanel_button_wrapper" id="add_primarystorage_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Primary Storage" /></div>
            <div class="actionpanel_button_links">
                Add Primary Storage
            </div>
        </div>
    </div>
</div>
<!--  top buttons (end) -->

<!-- Add Host Dialog -->
<div id="dialog_add_host" title="Add Host" style="display: none">
    <p>
        Add a host for zone <b><span id="zone_name"></span></b>, pod <b><span id="pod_name">
        </span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>    
            <li>
            	<label for="host_hypervisor">Hypervisor:</label>
                <select class="select" id="host_hypervisor">
                    <option value="XenServer" SELECTED>Xen Server</option>		
                    <option value="KVM">KVM</option>										
                    <option value="VmWare">VMware</option>										
                </select>
            </li>            
            <li>
                <label>
                    Cluster:</label>
                <select class="select" id="cluster_select">
                </select>
                <div id="cluster_select_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>                
            <li input_group="general">
                <label for="host_hostname">
                    Host name:</label>
                <input class="text" type="text" name="host_hostname" id="host_hostname" />
                <div id="host_hostname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="general">
                <label for="user_name">
                    User name:</label>
                <input class="text" type="text" name="host_username" id="host_username" />
                <div id="host_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="general">
                <label for="user_name">
                    Password:</label>
                <input class="text" type="password" name="host_password" id="host_password" autocomplete="off" />
                <div id="host_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware">
                <label for="host_vcenter_address">
                    vCenter Address:</label>
                <input class="text" type="text" name="host_vcenter_address" id="host_vcenter_address" />
                <div id="host_vcenter_address_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware">
                <label for="host_vcenter_username">
                    vCenter User:</label>
                <input class="text" type="text" name="host_vcenter_username" id="host_vcenter_username" />
                <div id="host_vcenter_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware">
                <label for="host_vcenter_password">
                    vCenter Password:</label>
                <input class="text" type="password" name="host_vcenter_password" id="host_vcenter_password" autocomplete="off" />
                <div id="host_vcenter_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware">
                <label for="host_vcenter_dc">
                    vCenter Datacenter:</label>
                <input class="text" type="text" name="host_vcenter_dc" id="host_vcenter_dc" />
                <div id="host_vcenter_dc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmware">
                <label for="host_vcenter_host">
                    vCenter Host:</label>
                <input class="text" type="text" name="host_vcenter_host" id="host_vcenter_host" />
                <div id="host_vcenter_host_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>            
        </ol>
        </form>
    </div>
    <!--Loading box-->
    <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display: none;">
        <div class="ui_dialog_loader">
        </div>
        <p>
            Adding....</p>
    </div>
    <!--Confirmation msg box-->
    <!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
    <div id="info_container" class="ui_dialog_messagebox error" style="display: none;">
        <div id="icon" class="ui_dialog_msgicon error">
        </div>
        <div id="info" class="ui_dialog_messagebox_text error">
            (info)</div>
    </div>
</div>
<!-- Add Primary Storage Dialog -->
<div id="dialog_add_pool" title="Add Primary Storage" style="display: none">
    <p>
        Add a new Primary Storage for zone <b><span id="zone_name"></span></b>, pod <b><span
            id="pod_name"></span></b>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li id="pool_cluster_container">
                <label for="pool_cluster">
                    Cluster:</label>
                <select class="select" id="pool_cluster">
                </select>
                <div id="pool_cluster_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    Name:</label>
                <input class="text" type="text" name="add_pool_name" id="add_pool_name" />
                <div id="add_pool_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_pool_protocol">
                    Protocol:</label>
                <select class="select" id="add_pool_protocol">
                    <option value="nfs">NFS</option>
                    <option value="iscsi">ISCSI</option>
                    <option value="vmfs">VMFS</option>
                </select>
            </li>
            <li id="add_pool_server_container">
                <label for="add_pool_nfs_server">
                    Server:</label>
                <input class="text" type="text" name="add_pool_nfs_server" id="add_pool_nfs_server" />
                <div id="add_pool_nfs_server_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_path_container" input_group="nfs">
                <label for="add_pool_path">
                    Path:</label>
                <input class="text" type="text" name="add_pool_path" id="add_pool_path" />
                <div id="add_pool_path_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_iqn_container" style="display: none" input_group="iscsi">
                <label for="add_pool_iqn">
                    Target IQN:</label>
                <input class="text" type="text" name="add_pool_iqn" id="add_pool_iqn" />
                <div id="add_pool_iqn_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_lun_container" style="display: none" input_group="iscsi">
                <label for="add_pool_lun">
                    LUN #:</label>
                <input class="text" type="text" name="add_pool_lun" id="add_pool_lun" />
                <div id="add_pool_lun_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmfs">
                <label for="add_pool_vmfs_dc">
                    vCenter Datacenter:</label>
                <input class="text" type="text" name="add_pool_vmfs_dc" id="add_pool_vmfs_dc" />
                <div id="add_pool_vmfs_dc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li input_group="vmfs">
                <label for="add_pool_vmfs_ds">
                    vCenter Datastore:</label>
                <input class="text" type="text" name="add_pool_vmfs_ds" id="add_pool_vmfs_ds" />
                <div id="add_pool_vmfs_ds_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_tags_container">
                <label for="add_pool_tags">
                    Tags:</label>
                <input class="text" type="text" id="add_pool_tags" />
                <div id="add_pool_tags_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
    <!--Loading box-->
    <div id="spinning_wheel" class="ui_dialog_loaderbox" style="display: none;">
        <div class="ui_dialog_loader">
        </div>
        <p>
            Adding....</p>
    </div>
    <!--Confirmation msg box-->
    <!--Note: for error msg, just have to add error besides everything for eg. add error(class) next to ui_dialog_messagebox error, ui_dialog_msgicon error, ui_dialog_messagebox_text error.  -->
    <div id="info_container" class="ui_dialog_messagebox error" style="display: none;">
        <div id="icon" class="ui_dialog_msgicon error">
        </div>
        <div id="info" class="ui_dialog_messagebox_text error">
            (info)</div>
    </div>
</div>