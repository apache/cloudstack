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
        <div class="content_tabs on">
            <%=t.t("Details")%></div>
    </div>    
    <div id="tab_content_details">
        <div class="grid_actionpanel">
            <div class="grid_actionbox" id="action_link">
                <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                    <ul class="actionsdropdown_boxlist" id="action_list">
                        <!--  
                    	<li> <a href="#"> Delete </a> </li>
                        <li> <a href="#"> Attach Disk </a> </li>
                        -->
                    </ul>
                </div>
            </div>
            <div class="grid_editbox">
            </div>
            <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
                display: none;">
                <div class="gridheader_loader" id="icon">
                </div>
                <p id="description">
                    Detaching Disk &hellip;</p>
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