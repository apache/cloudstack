<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_clustericon.gif" /></div>
    <h1>
        Cluster
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on" id="tab_details">
            <%=t.t("details")%></div>
    </div>
    <!-- Details tab (start)-->
    <div id="tab_content_details">
        <div class="rightpanel_mainloader_panel" style="display: none;">
            <div class="rightpanel_mainloaderbox">
                <div class="rightpanel_mainloader_animatedicon">
                </div>
                <p>
                    Loading &hellip;</p>
            </div>
        </div>
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
    <!-- Details tab (end)-->
</div>

<!-- Add Host Dialog (begin) -->
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
                    <option value="">Auto</option>									
                </select>
            </li>
            <li>
                <label for="host_hostname">
                    Host name:</label>
                <input class="text" type="text" name="host_hostname" id="host_hostname" />
                <div id="host_hostname_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    User name:</label>
                <input class="text" type="text" name="host_username" id="host_username" />
                <div id="host_username_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
                    Password:</label>
                <input class="text" type="password" name="host_password" id="host_password" autocomplete="off" />
                <div id="host_password_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="cluster_options_container">
                <label>
                    &nbsp;</label><span><u>Cluster Options</u></span> </li>
            <li id="new_cluster_radio_container">
                <label>
                    <input type="radio" name="cluster" value="new_cluster_radio" checked />&nbsp;New
                    cluster:</label>
                <input class="text" type="text" id="new_cluster_name" />
                <div id="new_cluster_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="existing_cluster_radio_container">
                <label>
                    <input type="radio" name="cluster" value="existing_cluster_radio" />&nbsp;Join cluster:</label>
                <select class="select" id="cluster_select">
                </select>
            </li>
            <li id="no_cluster_radio_container">
                <label>
                    <input type="radio" name="cluster" value="no_cluster_radio" />&nbsp;Standalone</label>
                <span style="padding-left: 20px"></span></li>
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
<!-- Add Host Dialog (end) -->


<!-- Add Primary Storage Dialog (begin) -->
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
            <li>
                <label for="add_pool_nfs_server">
                    Server:</label>
                <input class="text" type="text" name="add_pool_nfs_server" id="add_pool_nfs_server" />
                <div id="add_pool_nfs_server_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_path_container">
                <label for="add_pool_path">
                    Path:</label>
                <input class="text" type="text" name="add_pool_path" id="add_pool_path" />
                <div id="add_pool_path_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_iqn_container" style="display: none">
                <label for="add_pool_iqn">
                    Target IQN:</label>
                <input class="text" type="text" name="add_pool_iqn" id="add_pool_iqn" />
                <div id="add_pool_iqn_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li id="add_pool_lun_container" style="display: none">
                <label for="add_pool_lun">
                    LUN #:</label>
                <input class="text" type="text" name="add_pool_lun" id="add_pool_lun" />
                <div id="add_pool_lun_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
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
<!-- Add Primary Storage Dialog (end) -->
