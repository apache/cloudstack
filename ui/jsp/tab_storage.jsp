<%@ page import="java.util.Date" %>
<%
long milliseconds = new Date().getTime();
%>
<script type="text/javascript" src="scripts/cloud.core.storage.js?t=<%=milliseconds%>"></script>
	
<div class="submenu_links">
    <div class="submenu_links_on" id="submenu_pool">
        Primary Storage</div>
    <div class="submenu_links_off" id="submenu_storage">
        Secondary Storage</div>
    <div class="submenu_links_off" id="submenu_volume">
        Volumes</div>
    <div class="submenu_links_off" id="submenu_snapshot" style="display:none">
        Snapshots</div>
</div>
<!-- *** Primary Storage (begin) *** -->
<div class="maincontent" id="submenu_content_pool" style="display: none">
    <div id="maincontent_title">
        <div class="maintitle_icon">
            <img src="images/storagetitle_icons.gif" title="Primary Storage" />
        </div>
        <h1>
            Primary Storage</h1>
        <a class="add_storagepoolbutton" id="storage_action_new_pool" href="#"></a>
        <div class="search_formarea">
            <form action="#" method="post">
            <ol>
                <li>
                    <input class="text" type="text" name="search_input" id="search_input" /></li>
            </ol>
            </form>
            <a class="search_button" id="search_button" href="#"></a>
            <div id="advanced_search_link" class="advsearch_link">
                Advanced</div>
            <div id="advanced_search" class="adv_searchpopup" style="display: none;">
                <div class="adv_searchformbox">
                    <h3>
                        Advance Search</h3>
                         <a id="advanced_search_close" href="#">Close </a>
                    <form action="#" method="post">
                    <ol>
                        <li>
                            <label for="filter">
                                Name:</label>
                            <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
                        </li>
                        <li>
                            <label for="filter">
                                Zone:</label>
                            <select class="select" id="adv_search_zone">
                            </select>
                        </li>
                        <li><label for="filter" id="adv_search_pod_label">Pod:</label>
                        	<select class="select" id="adv_search_pod">
                            </select>
                        </li>
                        <li>
                            <label for="filter">
                                IP:</label>
                            <input class="text" type="text" name="adv_search_ip" id="adv_search_ip" />
                        </li>
                        <li>
                            <label for="filter">
                                Path:</label>
                            <input class="text" type="text" name="adv_search_path" id="adv_search_path" />
                        </li>
                    </ol>
                    </form>
                    <div class="adv_search_actionbox">
                    	<div class="adv_searchpopup_button" id="adv_search_button"></div>
					</div>
                </div>
            </div>
        </div>
    </div>
    <div class="filter_actionbox">
        <div class="selection_formarea" style="display: none;">
            <form action="#" method="post">
            <label for="filter">
                Filters:</label>
            <select class="select" id="Select1">
                <option value="true">Public</option>
                <option value="false">Private</option>
            </select>
            </form>
        </div>
    </div>
    <div class="grid_container">
        <div id="loading_gridtable" class="loading_gridtable">
            <div class="loading_gridanimation">
            </div>
            <p>
                Loading...</p>
        </div>
        <div class="grid_header">
            <div class="grid_genheader_cell" style="width: 3%;">
                <div class="grid_headertitles">
                    ID</div>
            </div>
            <div class="grid_genheader_cell" style="width: 8%;">
                <div class="grid_headertitles">
                    Zone</div>
            </div>
            <div class="grid_genheader_cell" style="width: 8%;">
                <div class="grid_headertitles">
                    Pod</div>
            </div>
            <div class="grid_genheader_cell" style="width: 6%;">
                <div class="grid_headertitles">
                    Cluster</div>
            </div>
            <div class="grid_genheader_cell" style="width: 8%;">
                <div class="grid_headertitles">
                    Name</div>
            </div>
            <div class="grid_genheader_cell" style="width: 6%;">
                <div class="grid_headertitles">
                    Type</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    IP/FQDN</div>
            </div>
            <div class="grid_genheader_cell" style="width: 15%;">
                <div class="grid_headertitles">
                    Path</div>
            </div>
            <div class="grid_genheader_cell" style="width: 20%;">
                <div class="grid_headertitles">
                    Statistics</div>
            </div>
            <div class="grid_genheader_cell" style="width: 9%;">
                <div class="grid_headertitles">
                    Tags</div>
            </div>
			<div class="grid_genheader_cell" style="width:5%;">
				<div class="grid_headertitles">Actions</div>
			</div>
        </div>
        <div id="grid_content">
        </div>
    </div>
    <div id="pagination_panel" class="pagination_panel" style="display: none">
        <p id="grid_rows_total" />
        <div class="pagination_actionbox">
            <div class="pagination_actions">
                <div class="pagination_actionicon">
                    <img src="images/pagination_refresh.gif" title="refresh" /></div>
                <a id="refresh" href="#">Refresh</a>
            </div>
            <div class="pagination_actions" id="prevPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_previcon.gif" title="prev" /></div>
                <a id="prevPage" href="#">Prev</a>
            </div>
            <div class="pagination_actions" id="nextPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_nexticon.gif" title="next" /></div>
                <a id="nextPage" href="#">Next</a>
            </div>
        </div>
    </div>
</div>
<!-- Pool Template (begin) -->
<div id="pool_template" style="display: none">
    <div class="adding_loading" style="height: 25px; display: none;">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Waiting &hellip;
        </div>
    </div>
    <div id="row_container">
        <div class="grid_smallgenrow_cell" style="width: 3%;">
            <div class="netgrid_celltitles" id="pool_id">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 8%;">
            <div class="netgrid_celltitles" id="pool_zone">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 8%;">
            <div class="netgrid_celltitles" id="pool_pod">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 6%;">
            <div class="netgrid_celltitles" id="pool_cluster">
            </div>
        </div>        
        <div class="grid_smallgenrow_cell" style="width: 8%;">
            <div class="netgrid_celltitles" id="pool_name">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 6%;">
            <div class="netgrid_celltitles" id="pool_type">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 10%;">
            <div class="netgrid_celltitles" id="pool_ip">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 15%;">
            <div class="netgrid_celltitles" id="pool_path">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 20%;">
            <div class="netgrid_celltitles" id="pool_statistics">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 9%;">
            <div class="netgrid_celltitles" id="pool_tags">
            </div>
        </div>
		<div class="grid_smallgenrow_cell" style="width:5%;">
		    <div class="netgrid_celltitles">			   
			    <span><a href="#" id="delete_link">Delete</a></span>               
		    </div>
	    </div>
    </div>
</div>
<!-- Pool Template (end) -->
<!-- Add Primary Storage Dialog -->
<div id="dialog_add_pool" title="Add Primary Storage" style="display: none">
    <p>
        Please fill in the following data to add a new Primary Storage.</p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="user_name">
                    Availability Zone:</label>
                <select class="select" name="pool_zone" id="pool_zone">
                    <option value="default">Please wait...</option>
                </select>
            </li>
            <li>
                <label for="user_name">
                    Pod:</label>
                <select class="select" name="pool_pod" id="pool_pod">
                    <option value="default">Please wait...</option>
                </select>
            </li>
            <li id="pool_cluster_container">
                <label for="pool_cluster">
                    Cluster:</label>
                <select class="select" id="pool_cluster">                    
                </select>
                <div id="pool_cluster_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">Name:</label>
                <input class="text" type="text" name="add_pool_name" id="add_pool_name" />
                <div id="add_pool_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_pool_protocol">Protocol:</label>
				<select class="select" id="add_pool_protocol">
                    <option value="nfs">NFS</option>
					<option value="iscsi">ISCSI</option>
                </select>
			</li>
			<li>
				<label for="add_pool_nfs_server">Server:</label>
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
			<li id="add_pool_iqn_container" style="display:none">
                <label for="add_pool_iqn">
                    Target IQN:</label>
                <input class="text" type="text" name="add_pool_iqn" id="add_pool_iqn" />
                <div id="add_pool_iqn_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
			<li id="add_pool_lun_container" style="display:none">
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
</div>
<!-- *** Primary Storage (end) *** -->
<!-- *** Secondary Storage (begin) *** -->
<!-- Secondary Storage Content Panel (begin) -->
<div class="maincontent" id="submenu_content_storage" style="display: none;">
    <div id="maincontent_title">
        <div class="maintitle_icon">
            <img src="images/storagetitle_icons.gif" title="Secondary Storage" />
        </div>
        <h1>
            Secondary Storage</h1>
        <a class="add_secondary_storagebutton" id="storage_action_new_host" href="#"></a>
        <div class="search_formarea">
            <form action="#" method="post">
            <ol>
                <li>
                    <input class="text" type="text" name="search_input" id="search_input" /></li>
            </ol>
            </form>
            <a class="search_button" id="search_button" href="#"></a>
            <div id="advanced_search_link" class="advsearch_link">
                Advanced</div>
            <div id="advanced_search" class="adv_searchpopup" style="display: none;">
                <div class="adv_searchformbox">
                    <h3>
                        Advance Search</h3>
                         <a id="advanced_search_close" href="#">Close </a>
                    <form action="#" method="post">
                    <ol>
                        <li>
                            <label for="filter">
                                Name:</label>
                            <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
                        </li>
                        <li>
                            <label for="filter">
                                Zone:</label>
                            <select class="select" id="adv_search_zone">
                            </select>
                        </li>
                        <li><label for="filter">Status:</label>
                        	<select class="select" id="adv_search_state">
								<option value=""></option>
								<option value="Up">Up</option>
								<option value="Down">Down</option>
								<option value="Disconnected">Disconnected</option>
								<option value="Updating">Updating</option>
								<option value="Alert">Alert</option>
								<option value="PrepareForMaintenance">PrepareForMaintenance</option>
								<option value="Maintenance">Maintenance</option>
								<option value="ErrorInMaintenance">ErrorInMaintenance</option>
                            </select>
                        </li>
                        <li id="adv_search_domain_li" style="display: none;">
                            <label for="filter">Domain:</label>
                            <select class="select" id="adv_search_domain">
                            </select>
                        </li>
                    </ol>
                    </form>
                    <div class="adv_search_actionbox">
                    	<div class="adv_searchpopup_button" id="adv_search_button"></div>
					</div>
                </div>
            </div>
        </div>
    </div>
    <div class="filter_actionbox">
        <div class="selection_formarea" style="display: none;">
            <form action="#" method="post">
            <label for="filter">
                Filters:</label>
            <select class="select" id="template_type">
                <option value="true">Public</option>
                <option value="false">Private</option>
            </select>
            </form>
        </div>
    </div>
    <div class="grid_container">
        <div id="loading_gridtable" class="loading_gridtable">
            <div class="loading_gridanimation">
            </div>
            <p>
                Loading...</p>
        </div>
        <div class="grid_header">
			<div class="grid_genheader_cell" style="width: 6%;">
			    <div class="grid_headertitles">
			        Status</div>
			</div>			
            <div class="grid_genheader_cell" style="width: 8%;">
                <div class="grid_headertitles">
                    Type</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Zone</div>
            </div>           
            <div class="grid_genheader_cell" style="width: 35%;">
                <div class="grid_headertitles">
                    Name</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    IP</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Version</div>
            </div>
            <div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Last Disconnected</div>
            </div>
            <div class="grid_genheader_cell" style="width:10%;">
				<div class="grid_headertitles">Actions</div>
			</div>
        </div>
        <div id="grid_content">
        </div>
    </div>
    <div id="pagination_panel" class="pagination_panel" style="display: none;">
        <p id="grid_rows_total" />
        <div class="pagination_actionbox">
            <div class="pagination_actions">
                <div class="pagination_actionicon">
                    <img src="images/pagination_refresh.gif" title="refresh" /></div>
                <a id="refresh" href="#">Refresh</a>
            </div>
            <div class="pagination_actions" id="prevPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_previcon.gif" title="prev" /></div>
                <a id="prevPage" href="#">Prev</a>
            </div>
            <div class="pagination_actions" id="nextPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_nexticon.gif" title="next" /></div>
                <a id="nextPage" href="#">Next</a>
            </div>
        </div>
    </div>
</div>
<!-- Secondary Storage Content Panel (end) -->
<!-- Secondary Storage Template (begin) -->
<div id="storage_template" style="display: none">
    <div class="adding_loading" style="height: 25px; display: none;">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Waiting &hellip;
        </div>
    </div>
    <div id="row_container">
		<div class="grid_smallgenrow_cell" style="width: 6%;">
			<div class="netgrid_celltitles" id="storage_status">
			</div>
		</div>
		
        <div class="grid_smallgenrow_cell" style="width: 8%;">
            <div class="netgrid_celltitles" id="storage_type">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 10%;">
            <div class="netgrid_celltitles" id="storage_zone">
            </div>
        </div>       
        <div class="grid_smallgenrow_cell" style="width: 35%;">
            <div class="netgrid_celltitles" id="storage_name">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 10%;">
            <div class="netgrid_celltitles" id="storage_ip">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 10%;">
            <div class="netgrid_celltitles" id="storage_version">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 10%;">
            <div class="netgrid_celltitles" id="storage_disconnected">
            </div>
        </div>        
        <div class="grid_smallgenrow_cell" style="width:10%;">
		    <div class="netgrid_celltitles">			   
			    <span><a href="#" id="delete_link">Delete</a></span>               
		    </div>
	    </div>        
    </div>
</div>
<!-- Secondary Storage Template (end) -->
<!-- Add Secondary Storage Dialog (begin) -->
<div id="dialog_add_host" title="Add Secondary Storage" style="display: none">
    <p>
        Please fill in the following data to add a new storage.</p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form1">
        <ol>
            <li>
                <label for="user_name">
                    Availability Zone:</label>
                <select class="select" name="storage_zone" id="storage_zone">
                    <option value="default">Please wait...</option>
                </select>
            </li>
            <li>
                <label for="add_storage_nfs_server">
                    NFS Server:</label>
                <input class="text" type="text" name="add_storage_nfs_server" id="add_storage_nfs_server" />
                <div id="add_storage_nfs_server_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="add_storage_path">
                    Path:</label>
                <input class="text" type="text" name="add_storage_path" id="add_storage_path" />
                <div id="add_storage_path_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Add Secondary Storage Dialog (end) -->
<!--*** Secondary Storage (end) *** -->
<!-- *** Volume (begin) *** -->
<!-- Volume Content Panel (begin) -->
<div class="maincontent" id="submenu_content_volume" style="display: none;">
    <div id="maincontent_title">
        <div class="maintitle_icon">
            <img src="images/storagetitle_icons.gif" title="volume" />
        </div>
        <h1>
            Volumes</h1>
        <a class="add_volumebutton" id="storage_action_new_volume" href="#"></a>
        <div class="search_formarea">
            <form action="#" method="post">
            <ol>
                <li>
                    <input class="text" type="text" name="search_input" id="search_input" /></li>
            </ol>
            </form>
            <a class="search_button" id="search_button" href="#"></a>
            <div id="advanced_search_link" class="advsearch_link">
                Advanced</div>
            <div id="advanced_search" class="adv_searchpopup" style="display: none;">
                <div class="adv_searchformbox">
                    <h3>
                        Advance Search</h3>
                         <a id="advanced_search_close" href="#">Close </a>
                    <form action="#" method="post">
                    <ol>
                        <li>
                            <label for="filter">
                                Name:</label>
                            <input class="text" type="text" name="adv_search_name" id="adv_search_name" />
                        </li>                       
                        <li>
                            <label for="filter">
                                Zone:</label>
                            <select class="select" id="adv_search_zone">
                            </select>
                        </li>
                        <li id="adv_search_pod_li" style="display: none;">
                            <label for="filter" id="adv_search_pod_label">
                                Pod:</label>
                            <select class="select" id="adv_search_pod">
                            </select>
                        </li>
                        <li id="adv_search_domain_li" style="display: none;">
                            <label for="filter">Domain:</label>
                            <select class="select" id="adv_search_domain">
                            </select>
                        </li>
                        <li id="adv_search_account_li" style="display: none;">
                            <label for="filter">
                                Account:</label>
                            <input class="text" type="text" id="adv_search_account" />  
                        </li>
                    </ol>
                    </form>
                    <div class="adv_search_actionbox">
                    	<div class="adv_searchpopup_button" id="adv_search_button"></div>
					</div>
                </div>
            </div>
        </div>
    </div>
    <div class="filter_actionbox">
        <div class="selection_formarea" style="display: none;">
            <form action="#" method="post">
            <label for="filter">
                Filters:</label>
            <select class="select" id="Select2">
                <option value="true">Public</option>
                <option value="false">Private</option>
            </select>
            </form>
        </div>
    </div>
    <div class="grid_container">
        <div id="loading_gridtable" class="loading_gridtable">
            <div class="loading_gridanimation">
            </div>
            <p>
                Loading...</p>
        </div>
        <div class="grid_header">
            <div class="grid_genheader_cell" style="width: 4%;">
                <div class="grid_headertitles">
                    ID</div>
            </div>
            <div class="grid_genheader_cell" style="width: 12%;">
                <div class="grid_headertitles">
                    Name</div>
            </div>
            <div class="grid_genheader_cell" style="width: 15%;">
                <div class="grid_headertitles">
                    Type</div>
            </div>
			<div class="grid_genheader_cell" style="width: 10%;">
                <div class="grid_headertitles">
                    Zone</div>
            </div>
            <div class="grid_genheader_cell" style="width: 14%;">
                <div class="grid_headertitles">
                    Instance Name</div>
            </div>
            <div class="grid_genheader_cell" style="width: 5%;">
                <div class="grid_headertitles">
                    Device ID</div>
            </div>
            <div class="grid_genheader_cell" style="width: 7%;">
                <div class="grid_headertitles">
                    Size</div>
            </div>
             <div class="grid_genheader_cell" style="width: 5%;">
                <div class="grid_headertitles">
                    State</div>
            </div>
            <div class="grid_genheader_cell" id="volume_created_header" style="width: 8%;">
                <div class="grid_headertitles">
                    Created</div>
            </div>
            <div class="grid_genheader_cell" id="volume_hostname_header" style="display: none;
                width: 10%;">
                <div class="grid_headertitles">
                    Storage</div>
            </div>
            <div class="grid_genheader_cell" id="volume_account_header" style="display: none;
                width: 11%; border:0;">
                <div class="grid_headertitles">
                    Account</div>
            </div>
            
        </div>
        <div id="grid_content">
        </div>
    </div>
    <div id="pagination_panel" class="pagination_panel" style="display: none;">
        <p id="grid_rows_total" />
        <div class="pagination_actionbox">
            <div class="pagination_actions">
                <div class="pagination_actionicon">
                    <img src="images/pagination_refresh.gif" title="refresh" />
                </div>
                <a id="refresh" href="#">Refresh</a>
            </div>
            <div class="pagination_actions" id="prevPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_previcon.gif" title="prev" />
                </div>
                <a id="prevPage" href="#">Prev</a>
            </div>
            <div class="pagination_actions" id="nextPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_nexticon.gif" title="next" />
                </div>
                <a id="nextPage" href="#">Next</a>
            </div>
        </div>
    </div>
</div>
<!-- Volume Content Panel (end) -->
<!-- Volume Template (begin) -->
<div id="volume_template" style="display: none">
    <div class="adding_loading" style="height: 45px; display: none;" id="loading_container">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Creating &hellip;
        </div>
    </div>
    <div class="result_loading" style="height: 45px; display: none;" id="created_successfully">
        <p>
            Created Successfully</p>
        <div class="result_closebutton" id="close_button">
        </div>
    </div>
    <div id="row_container">
        <div class="grid_smallgenrow_cell" style="width: 4%;">
            <div class="netgrid_celltitles" id="volume_id">
                ID</div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 12%;">
            <div class="netgrid_celltitles" id="volume_name">
                Name</div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 15%;">
            <div class="netgrid_celltitles" id="volume_type">
                Type</div>
        </div>
		<div class="grid_smallgenrow_cell" style="width: 10%;">
            <div class="netgrid_celltitles" id="volume_zone">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 14%;">
            <div class="netgrid_celltitles" id="volume_vmname">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 5%;">
            <div class="netgrid_celltitles" id="volume_deviceid">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 7%;">
            <div class="netgrid_celltitles" id="volume_size">
                size</div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 5%;">
            <div class="netgrid_celltitles" id="volume_state">
                state</div>
        </div>
        <div class="grid_smallgenrow_cell" id="volume_created_container" style="width: 8%;">
            <div class="netgrid_celltitles" id="volume_created">
                Created</div>
        </div>
        <div class="grid_smallgenrow_cell" id="volume_hostname_container" style="width: 10%;
            display: none;">
            <div class="netgrid_celltitles" id="volume_hostname">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" id="volume_account_container" style="width: 11%;
            display: none; border:0;">
            <div class="netgrid_celltitles" id="volume_account">
                account</div>
        </div>
        <div id="grid_links_container" style="display: block">
            <div class="grid_links" style="margin:5px 0 0 5px; display:inline;">
                <div id="volume_action_snapshot_grid" class="vm_botactionslinks_down" style="display:none">
                    snapshots
                </div>                
                <div style="float: left;display:none" id="volume_action_take_snapshot_container" >
                    <a id="volume_action_take_snapshot" href="#">take snapshot</a> |
                </div>
                <div style="float: left;display:none" id="volume_action_recurring_snapshot_container">
                    <a id="volume_action_recurring_snapshot" href="#">recurring snapshot</a> |
                </div>
                <div style="display: none; float: left" id="volume_action_detach_span">
                    <a id="volume_action_detach" href="#">detach</a> |
                </div>
                <div style="display: none; float: left" id="volume_action_attach_span">
                    <a id="volume_action_attach" href="#">attach</a> |
                </div>
                <div style="display: none; float: left" id="volume_action_delete_span">
                    <a id="volume_action_delete" href="#">delete</a> |
                </div>
                <div style="display: none; float: left" id="volume_action_create_template_span">
                    <a id="volume_action_create_template" href="#">create template</a> 
                </div>
            </div>
        </div>
    </div>   
    <div class="hostadmin_showdetails_panel" id="volume_snapshot_detail_panel" style="display: none;">
        <div class="hostadmin_showdetails_grid">
            <div class="hostadmin_showdetailsheader">
                <div class="hostadmin_showdetailsheader_cell" style="width: 5%">
                    <div class="grid_headertitles">
                        ID</div>
                </div>
                <div class="hostadmin_showdetailsheader_cell" style="width: 20%">
                    <div class="grid_headertitles">
                        Name</div>
                </div>     
                <div class="hostadmin_showdetailsheader_cell" style="width: 10%">
                    <div class="grid_headertitles">
                        Volume</div>
                </div>   
                <div class="hostadmin_showdetailsheader_cell" style="width: 8%">
                    <div class="grid_headertitles">
                        Interval Type</div>
                </div>                          
                <div class="hostadmin_showdetailsheader_cell" style="width: 10%">
                    <div class="grid_headertitles">
                        Created</div>
                </div>          
                
                <div class="hostadmin_showdetailsheader_cell" style="width: 10%" id="volume_snapshot_account_header">
                    <div class="grid_headertitles">
                        Account</div>
                </div>
                <div class="hostadmin_showdetailsheader_cell" style="width: 10%" id="volume_snapshot_domain_header">
                    <div class="grid_headertitles">
                        Domain</div>
                </div>                         
                <div class="hostadmin_showdetailsheader_cell" style="width: 22%">
                    <div class="grid_headertitles">
                        Actions
                    </div>
                </div>                
            </div>
            <div id="volume_snapshot_grid">
                <div class="hostadmin_showdetails_row_odd">
                    <div class="hostadmin_showdetailsrow_cell" style="width: 100%">
                        <div class="netgrid_celltitles">
                            No Snapshot
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>    
</div>
<!-- Volume Template (end) -->
<!-- Volume's Snapshot Template (begin) -->
<div id="volume_snapshot_detail_template" style="display: none">
    <div class="adding_loading" style="height: 25px; display: none;">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Detaching &hellip;
        </div>
    </div>
    <div id="row_container">
        <div class="hostadmin_showdetailsrow_cell" style="width: 5%">
            <div class="netgrid_celltitles" id="id">
            </div>
        </div>
        <div class="hostadmin_showdetailsrow_cell" style="width: 20%">
            <div class="netgrid_celltitles" id="name">
            </div>
        </div>      
        <div class="hostadmin_showdetailsrow_cell" style="width: 10%">
            <div class="netgrid_celltitles" id="volume">
            </div>
        </div>     
        <div class="hostadmin_showdetailsrow_cell" style="width: 8%">
            <div class="netgrid_celltitles" id="interval_type">
            </div>
        </div>      
        <div class="hostadmin_showdetailsrow_cell" style="width: 10%">
            <div class="netgrid_celltitles" id="created">
            </div>
        </div>   
        <div class="hostadmin_showdetailsrow_cell" style="width: 10%" id="volume_snapshot_account_container">
            <div class="netgrid_celltitles" id="account">
            </div>
        </div>
        <div class="hostadmin_showdetailsrow_cell" style="width: 10%" id="volume_snapshot_domain_container">
            <div class="netgrid_celltitles" id="domain">
            </div>
        </div>   
        <div class="hostadmin_showdetailsrow_cell" style="width: 22%">
            <div class="netgrid_celltitles">
                <span id="volume_snapshot_action_create_volume_container">
                    <a id="volume_snapshot_action_create_volume" href="#">Create Volume</a>
                </span>
                <span id="volume_snapshot_action_delete_snapshot_container">
                    <span>&nbsp;&nbsp;|</span>
                    <a id="volume_snapshot_action_delete_snapshot" href="#">Delete</a>
                </span> 
                <span id="volume_snapshot_action_create_template_container">
                    <span>&nbsp;&nbsp;|</span>
                    <a id="volume_snapshot_action_create_template" href="#">Create Template</a> 
                </span> 
            </div>
        </div>             
    </div>
</div>
<!-- Volume's Snapshot Template (end) -->
<!-- Add Volume Dialog (begin) -->
<div id="dialog_add_volume" title="Add Volume" style="display: none">
    <p>
        Please fill in the following data to add a new volume.</p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form2">
        <ol>
            <li>
                <label for="add_volume_name">
                    Name:</label>
                <input class="text" type="text" name="add_volume_name" id="add_volume_name" />
                <div id="add_volume_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="volume_zone">
                    Availability Zone:</label>
                <select class="select" name="volume_zone" id="volume_zone">
                    <option value="default">Please wait...</option>
                </select>
            </li>
            <li>
                <label for="volume_diskoffering">
                    Disk Offering:</label>
                <select class="select" name="volume_diskoffering" id="volume_diskoffering">
                    <option value="default">Please wait...</option>
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Add Volume Dialog (end) -->

<!-- Add Volume Dialog from Snapshot (begin) -->
<div id="dialog_add_volume_from_snapshot" title="Add Volume from Snapshot" style="display: none">   
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form5">
        <ol>
            <li>
                <label>Name:</label>
                <input class="text" type="text" id="name" />
                <div id="name_errormsg" class="dialog_formcontent_errormsg" style="display: none;"></div>
            </li>           
        </ol>
        </form>
    </div>
</div>
<!-- Add Volume Dialog from Snapshot (end) -->

<!-- Recurring Snapshots Dialog (begin) -->
<div id="dialog_recurring_snapshot" title="Recurring Snapshot" style="display:none;">
    <div class="dialog_snapshotcontainer">
        <div class="dialog_snapshotleft" id="dialog_snapshotleft">
            
                <div class="dialog_snapshotleft_list">
                    <div class="dialog_snapshotleft_label">
                        Hourly:</div>
					<div class="dialog_snapshotleft_info" id="dialog_snapshot_hourly_info_unset">                        
						<p><i>&nbsp;Please click <b>'Edit'</b> to set your <b>hourly</b> recurring snapshot schedule</i></p>
                    </div>
                    <div class="dialog_snapshotleft_info" id="dialog_snapshot_hourly_info_set" style="display:none">                        
                        <span id="read_hourly_minute">mm</span></span><span> Minute(s) Past the Hour</span><span id="read_hourly_timezone"></span>            
                    </div>
                    <div class="dialog_snapshotleft_max">
                        <p>Keeping:</p>
                        <span id="read_hourly_max">N/A</span></div>
                    <div class="dialog_snapshotleft_actions">
                        <a id="hourly_edit_link" href="#">Edit</a> | <a id="hourly_delete_link" href="#">&nbsp;Delete</a></div>
                </div>
                <div class="dialog_snapshotleft_list">
                    <div class="dialog_snapshotleft_label">
                        Daily:</div>
					<div class="dialog_snapshotleft_info" id="dialog_snapshot_daily_info_unset">                        
						<p><i>&nbsp;Please click <b>'Edit'</b> to set your  <b>daily</b> recurring snapshot schedule</i></p>
                    </div>
                    <div class="dialog_snapshotleft_info" id="dialog_snapshot_daily_info_set" style="display:none">                       
                        <span id="read_daily_hour">hh</span><span>:</span><span id="read_daily_minute">mm</span>
                        <span id="read_daily_meridiem">AM</span><span id="read_daily_timezone"></span>
                    </div>
                    <div class="dialog_snapshotleft_max">
                        <p>
                            Keeping:</p>
                        <span id="read_daily_max">N/A</span></div>
                    <div class="dialog_snapshotleft_actions">
                        <a id="daily_edit_link" href="#">Edit</a> | <a id="daily_delete_link" href="#">&nbsp;Delete</a></div>
                </div>
                <div class="dialog_snapshotleft_list">
                    <div class="dialog_snapshotleft_label">
                        Weekly:</div>
					<div class="dialog_snapshotleft_info" id="dialog_snapshot_weekly_info_unset">                        
						<p><i>&nbsp;Please click <b>'Edit'</b> to set your  <b>weekly</b> recurring snapshot schedule</i></p>
                    </div>
                    <div class="dialog_snapshotleft_info" id="dialog_snapshot_weekly_info_set" style="display:none">                       
                        <span id="read_weekly_hour">hh</span><span>:</span><span id="read_weekly_minute">mm</span>
                        <span id="read_weekly_meridiem">AM</span><span id="read_weekly_timezone"></span>
                        <span id="read_weekly_day_of_week">day-of-week</span>
                    </div>
                    <div class="dialog_snapshotleft_max">
                        <p>Keeping:</p>
                        <span id="read_weekly_max">N/A</span></div>
                    <div class="dialog_snapshotleft_actions">
                        <a id="weekly_edit_link" href="#">Edit</a> | <a id="weekly_delete_link" href="#">&nbsp;Delete</a></div>
                </div>        
                <div class="dialog_snapshotleft_list">
                    <div class="dialog_snapshotleft_label">
                        Monthly:</div>
					<div class="dialog_snapshotleft_info" id="dialog_snapshot_monthly_info_unset">                        
						<p><i>&nbsp;Please click <b>'Edit'</b> to set your  <b>monthly</b> recurring snapshot schedule</i></p>
                    </div>
                    <div class="dialog_snapshotleft_info" id="dialog_snapshot_monthly_info_set" style="display:none">                       
                        <span id="read_monthly_hour">hh</span><span>:</span><span id="read_monthly_minute">mm</span>
                        <span id="read_monthly_meridiem">AM</span><span id="read_monthly_timezone"></span>
                        <span id="read_monthly_day_of_month">day-of-month</span>
                    </div>
                    <div class="dialog_snapshotleft_max">
                        <p>Keeping:</p>
                        <span id="read_monthly_max">N/A</span></div>
                    <div class="dialog_snapshotleft_actions">
                        <a id="monthly_edit_link" href="#">Edit</a> | <a id="monthly_delete_link" href="#">&nbsp;Delete</a></div>
                </div>             
              </div>
        <div class="dialog_snapshotright" id="dialog_snapshotright">
        	<div class="dialog_snapshotright_infotext" style="display:none"> Cick Edit to Schedule</div>
            <div class="dialog_snapshots_editcontent" style="display:block;">
                <div class="dialog_snapshots_editcontent_title">
                    <p>&nbsp;Edit:</p>
                    <span id="edit_interval_type" style="text-decoration:underline">Interval Type</span>
                </div>
                <div class="dialog_formcontent">
                    <form action="#" method="post" id="form4">
                    <ol>
                        <li>
                            <label for="add_volume_name" style="width:75px">
                                Time:</label>
                            <span id="edit_hardcoding_hour" style="display:none">00</span>
                            <span id="edit_hour_container">
                            	
                                <select class="snapselect" id="edit_hour">
                                    <option value="00">00</option>
                                    <option value="01">01</option>
                                    <option value="02">02</option>
                                    <option value="03">03</option>
                                    <option value="04">04</option>
                                    <option value="05">05</option>
                                    <option value="06">06</option>
                                    <option value="07">07</option>
                                    <option value="08">08</option>
                                    <option value="09">09</option>
                                    <option value="10">10</option>
                                    <option value="11">11</option>                                    
                                </select>                               
                            </span>                            
                            
                            <span id="edit_time_colon">:</span>
                            
                            <span id="edit_minute_container">
                                <select class="snapselect" id="edit_minute">
                                    <option value="00">00</option>
                                    <option value="01">01</option>
                                    <option value="02">02</option>
                                    <option value="03">03</option>
                                    <option value="04">04</option>
                                    <option value="05">05</option>
                                    <option value="06">06</option>
                                    <option value="07">07</option>
                                    <option value="08">08</option>
                                    <option value="09">09</option>                                
                                    <option value="10">10</option>
                                    <option value="11">11</option>
                                    <option value="12">12</option>
                                    <option value="13">13</option>
                                    <option value="14">14</option>
                                    <option value="15">15</option>
                                    <option value="16">16</option>
                                    <option value="17">17</option>
                                    <option value="18">18</option>
                                    <option value="19">19</option>                                
                                    <option value="20">20</option>                                
                                    <option value="21">21</option>
                                    <option value="22">22</option>
                                    <option value="23">23</option>
                                    <option value="24">24</option>
                                    <option value="25">25</option>
                                    <option value="26">26</option>
                                    <option value="27">27</option>
                                    <option value="28">28</option>
                                    <option value="29">29</option>                                
                                    <option value="30">30</option>                                
                                    <option value="31">31</option>
                                    <option value="32">32</option>
                                    <option value="33">33</option>
                                    <option value="34">34</option>
                                    <option value="35">35</option>
                                    <option value="36">36</option>
                                    <option value="37">37</option>
                                    <option value="38">38</option>
                                    <option value="39">39</option>                                
                                    <option value="40">40</option>                                
                                    <option value="41">41</option>
                                    <option value="42">42</option>
                                    <option value="43">43</option>
                                    <option value="44">44</option>
                                    <option value="45">45</option>
                                    <option value="46">46</option>
                                    <option value="47">47</option>
                                    <option value="48">48</option>
                                    <option value="49">49</option>                                
                                    <option value="50">50</option>                                
                                    <option value="51">51</option>
                                    <option value="52">52</option>
                                    <option value="53">53</option>
                                    <option value="54">54</option>
                                    <option value="55">55</option>
                                    <option value="56">56</option>
                                    <option value="57">57</option>
                                    <option value="58">58</option>
                                    <option value="59">59</option>                        
                                </select>                                
                            </span>
							
							<span id="edit_past_the_hour" style="display:none"> Minute(s) Past the Hour</span>
                            
                            <span id="edit_meridiem_container">
                                <select class="snapselect"id="edit_meridiem">                                                                
                                    <option value="AM">AM</option>
                                    <option value="PM">PM</option>                                   
                                </select>
                            </span>                     
                        </li>                     
                        <li style="margin-top:10px;" id="edit_day_of_week_container">
                            <label for="filter" style="width:75px">
                                Day of Week:</label>
                            <select class="snapselect"id="edit_day_of_week">
                                <option value="1">Sunday</option>
                                <option value="2">Monday</option>
                                <option value="3">Tuesday</option>
                                <option value="4">Wednesday</option>
                                <option value="5">Thursday</option>
                                <option value="6">Friday</option>
                                <option value="7">Saturday</option>                                
                            </select>                            
                        </li>
                        <li style="margin-top:10px;" id="edit_day_of_month_container">
                            <label for="filter" style="width:75px">
                                Day of Month:</label>
                            <select class="snapselect" id="edit_day_of_month">
                                <option value="1">1</option>
                                <option value="2">2</option>
                                <option value="3">3</option>
                                <option value="4">4</option>
                                <option value="5">5</option>
                                <option value="6">6</option>
                                <option value="7">7</option>
                                <option value="8">8</option>
                                <option value="9">9</option>
                                <option value="10">10</option>
                                <option value="11">11</option>
                                <option value="12">12</option>
                                <option value="13">13</option>
                                <option value="14">14</option>
                                <option value="15">15</option>
                                <option value="16">16</option>
                                <option value="17">17</option>
                                <option value="18">18</option>
                                <option value="19">19</option>
                                <option value="20">20</option>
                                <option value="21">21</option>
                                <option value="22">22</option>
                                <option value="23">23</option>
                                <option value="24">24</option>
                                <option value="25">25</option>
                                <option value="26">26</option>
                                <option value="27">27</option>
                                <option value="28">28</option>
                            </select>                            
                        </li>
                        <li style="margin-top:10px;">
                            <label for="edit_timezone" style="width:75px">
                                Time Zone:</label>                             
                            <select class="snapselect" id="edit_timezone" style="width:240px">
								<option value='Etc/GMT+12'>[UTC-12:00] GMT-12:00</option>
								<option value='Etc/GMT+11'>[UTC-11:00] GMT-11:00</option>
								<option value='Pacific/Samoa'>[UTC-11:00] Samoa Standard Time</option>
								<option value='Pacific/Honolulu'>[UTC-10:00] Hawaii Standard Time</option>
								<option value='US/Alaska'>[UTC-09:00] Alaska Standard Time</option>
								<option value='America/Los_Angeles'>[UTC-08:00] Pacific Standard Time</option>
								<option value='Mexico/BajaNorte'>[UTC-08:00] Baja California</option>
								<option value='US/Arizona'>[UTC-07:00] Arizona</option>
								<option value='US/Mountain'>[UTC-07:00] Mountain Standard Time</option>
								<option value='America/Chihuahua'>[UTC-07:00] Chihuahua, La Paz</option>
								<option value='America/Chicago'>[UTC-06:00] Central Standard Time</option>
								<option value='America/Costa_Rica'>[UTC-06:00] Central America</option>
								<option value='America/Mexico_City'>[UTC-06:00] Mexico City, Monterrey</option>
								<option value='Canada/Saskatchewan'>[UTC-06:00] Saskatchewan</option>
								<option value='America/Bogota'>[UTC-05:00] Bogota, Lima</option>
								<option value='America/New_York'>[UTC-05:00] Eastern Standard Time</option>
								<option value='America/Caracas'>[UTC-04:00] Venezuela Time</option>
								<option value='America/Asuncion'>[UTC-04:00] Paraguay Time</option>
								<option value='America/Cuiaba'>[UTC-04:00] Amazon Time</option>
								<option value='America/Halifax'>[UTC-04:00] Atlantic Standard Time</option>
								<option value='America/La_Paz'>[UTC-04:00] Bolivia Time</option>
								<option value='America/Santiago'>[UTC-04:00] Chile Time</option>
								<option value='America/St_Johns'>[UTC-03:30] Newfoundland Standard Time</option>
								<option value='America/Araguaina'>[UTC-03:00] Brasilia Time</option>
								<option value='America/Argentina/Buenos_Aires'>[UTC-03:00] Argentine Time</option>
								<option value='America/Cayenne'>[UTC-03:00] French Guiana Time</option>
								<option value='America/Godthab'>[UTC-03:00] Greenland Time</option>
								<option value='America/Montevideo'>[UTC-03:00] Uruguay Time]</option>
								<option value='Etc/GMT+2'>[UTC-02:00] GMT-02:00</option>
								<option value='Atlantic/Azores'>[UTC-01:00] Azores Time</option>
								<option value='Atlantic/Cape_Verde'>[UTC-01:00] Cape Verde Time</option>
								<option value='Africa/Casablanca'>[UTC] Casablanca</option>
								<option value='Etc/UTC'>[UTC] Coordinated Universal Time</option>
								<option value='Atlantic/Reykjavik'>[UTC] Reykjavik</option>
								<option value='Europe/London'>[UTC] Western European Time</option>
								<option value='CET'>[UTC+01:00] Central European Time</option>
								<option value='Europe/Bucharest'>[UTC+02:00] Eastern European Time</option>
								<option value='Africa/Johannesburg'>[UTC+02:00] South Africa Standard Time</option>
								<option value='Asia/Beirut'>[UTC+02:00] Beirut</option>
								<option value='Africa/Cairo'>[UTC+02:00] Cairo</option>
								<option value='Asia/Jerusalem'>[UTC+02:00] Israel Standard Time</option>
								<option value='Europe/Minsk'>[UTC+02:00] Minsk</option>
								<option value='Europe/Moscow'>[UTC+03:00] Moscow Standard Time</option>
								<option value='Africa/Nairobi'>[UTC+03:00] Eastern African Time</option>
								<option value='Asia/Karachi'>[UTC+05:00] Pakistan Time</option>
								<option value='Asia/Kolkata'>[UTC+05:30] India Standard Time</option>
								<option value='Asia/Bangkok'>[UTC+05:30] Indochina Time</option>
								<option value='Asia/Shanghai'>[UTC+08:00] China Standard Time</option>
								<option value='Asia/Kuala_Lumpur'>[UTC+08:00] Malaysia Time</option>
								<option value='Australia/Perth'>[UTC+08:00] Western Standard Time (Australia)</option>
								<option value='Asia/Taipei'>[UTC+08:00] Taiwan</option>
								<option value='Asia/Tokyo'>[UTC+09:00] Japan Standard Time</option>
								<option value='Asia/Seoul'>[UTC+09:00] Korea Standard Time</option>
								<option value='Australia/Adelaide'>[UTC+09:30] Central Standard Time (South Australia)</option>
								<option value='Australia/Darwin'>[UTC+09:30] Central Standard Time (Northern Territory)</option>
								<option value='Australia/Brisbane'>[UTC+10:00] Eastern Standard Time (Queensland)</option>
								<option value='Australia/Canberra'>[UTC+10:00] Eastern Standard Time (New South Wales)</option>
								<option value='Pacific/Guam'>[UTC+10:00] Chamorro Standard Time</option>
								<option value='Pacific/Auckland'>[UTC+12:00] New Zealand Standard Time</option>
         		            </select>                                                          
                        </li>
						<li style="margin-top:10px;">
                            <label for="edit_max" style="width:75px">
                                Keep:</label>
                            <input class="text" style="width: 68px;" type="text" id="edit_max"/>
                            <span>Snapshot(s)</span>
                            <div id="edit_max_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>                             
                        </li>  						
                    </ol>
                    <input class="ui-state-default" type="submit" id="apply_button" value="Apply Schedule" style="background-color:Yellow; width: 150px; height:20px; margin:15px 0 0 0;"/>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
<!-- Take Snapshots Dialog (end) -->
<!-- Detach Volume Dialog (begin) -->
<div id="dialog_detach_volume" title="Detach Volume" style="display: none">
    <p>
        Please confirm you want to detach the volume. If you are detaching a disk volume
        from a Windows based virtual machine, you will need to reboot the instance for the
        settings to take effect.</p>
</div>
<!-- Detach Volume Dialog (end) -->
<!-- Attach Volume Dialog (begin) -->
<div id="dialog_attach_volume" title="Attach Volume" style="display: none">
    <p>
        Please fill in the following data to attach a new volume. If you are attaching a
        disk volume to a Windows based virtual machine, you will need to reboot the instance
        to see the attached disk.</p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form3">
        <ol>
            <li>
                <label for="volume_vm">
                    Virtual Machine:</label>
                <select class="select" name="volume_vm" id="volume_vm">
                    <option value="default">Please wait...</option>
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Attach Volume Dialog (end) -->
<!-- Delete Volume Dialog (begin) -->
<div id="dialog_delete_volume" title="Delete Volume" style="display: none">
    <p>
        Please confirm you want to delete the volume.</p>
</div>
<!-- Delete Volume Dialog (end) -->
<!-- Create Template Dialog (begin) -->
<div id="dialog_create_template" title="Create Template" style="display:none">
    <p>
        Please specify the following information before creating a template of your disk
        volume: <b><span id="volume_name"></span></b>. Creating a template could take up
        to several hours depending on the size of your disk volume.</p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="create_template_name">
                    Name:</label>
                <input class="text" type="text" name="create_template_name" id="create_template_name" />
                <div id="create_template_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="create_template_desc"">
                    Display Text:</label>
                <input class="text" type="text" name="create_template_desc" id="create_template_desc" />
                <div id="create_template_desc_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="create_template_os_type">
                    OS Type:</label>
                <select class="select" name="create_template_os_type" id="create_template_os_type">
                </select>
            </li>
            <li>
                <label for="create_template_public">
                    Public:</label>
                <select class="select" name="create_template_public" id="create_template_public">                    
                    <option value="false">No</option>
                    <option value="true">Yes</option>
                </select>
            </li>
            <li>
                <label for="create_template_password">
                    Password Enabled?:</label>
                <select class="select" name="create_template_password" id="create_template_password">
                    <option value="false">No</option>
                    <option value="true">Yes</option>                    
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Create Template Dialog (end) -->
<!-- Create Snapshot Dialog (begin) -->
<div id="dialog_create_snapshot" title="Create Snapshot" style="display: none">
    <p>
        Please confirm you want to create snapshot for this volume.</p>
</div>
<!-- Create Snapshot Dialog (end) -->
<!--*** Volume (end) *** -->

<!-- Snapshot (begin) -->
 <div class="maincontent" style="display: none;" id="submenu_content_snapshot">
    <div id="maincontent_title">
        <div class="maintitle_icon">
            <img src="images/accountstitle_icons.gif" title="routers" />
        </div>
        <h1>
            Snapshots</h1>
        <div class="search_formarea">
            <form action="#" method="post">
            <ol>
                <li>
                    <input class="text" type="text" name="search_input" id="search_input" /></li>
            </ol>
            </form>
            <a class="search_button" id="search_button" href="#"></a>
            <div id="advanced_search_link" class="advsearch_link">
                Advanced</div>
            <div id="advanced_search" class="adv_searchpopup" style="display: none;">
                <div class="adv_searchformbox">
                    <h3>
                        Advance Search</h3>
                         <a id="advanced_search_close" href="#">Close </a>
                    <form action="#" method="post">
                    <ol>    
                        <li id="adv_search_domain_li" style="display: none;">
                            <label for="filter">Domain:</label>
                            <select class="select" id="adv_search_domain">
                            </select>
                        </li>                   
                        <li id="adv_search_account_li" style="display: none;">
                            <label for="filter">
                                Account:</label>
                            <input class="text" type="text" id="adv_search_account" />  
                        </li>
                    </ol>
                    </form>
                    <div class="adv_search_actionbox">
                    	<div class="adv_searchpopup_button" id="adv_search_button"></div>
					</div>
                </div>
            </div>
        </div>
    </div>
    <div class="filter_actionbox">
    </div>
    <div class="grid_container">
        <div id="loading_gridtable" class="loading_gridtable">
            <div class="loading_gridanimation">
            </div>
            <p>
                Loading...</p>
        </div>
        <div class="grid_header">
            <div class="hostadmin_showdetailsheader_cell" style="width: 5%">
                <div class="grid_headertitles">
                    ID</div>
            </div>
            <div class="hostadmin_showdetailsheader_cell" style="width: 20%">
                <div class="grid_headertitles">
                    Name</div>
            </div>     
            <div class="hostadmin_showdetailsheader_cell" style="width: 10%">
                <div class="grid_headertitles">
                    Volume</div>
            </div>   
            <div class="hostadmin_showdetailsheader_cell" style="width: 8%">
                <div class="grid_headertitles">
                    Interval Type</div>
            </div>          
            <div class="hostadmin_showdetailsheader_cell" style="width: 10%">
                <div class="grid_headertitles">
                    Created</div>
            </div>
            <div class="hostadmin_showdetailsheader_cell" style="width: 10%" id="snapshot_account_header">
                <div class="grid_headertitles">
                    Account</div>
            </div>
            <div class="hostadmin_showdetailsheader_cell" style="width: 10%" id="snapshot_domain_header">
                <div class="grid_headertitles">
                    Domain</div>
            </div>
            <div class="hostadmin_showdetailsheader_cell" style="width: 22%">
                <div class="grid_headertitles">
                    Actions
                </div>
            </div>
        </div>
        <div id="grid_content">
        </div>
    </div>
    <div id="pagination_panel" class="pagination_panel" style="display: none;">
        <p id="grid_rows_total" />
        <div class="pagination_actionbox">
            <div class="pagination_actions">
                <div class="pagination_actionicon">
                    <img src="images/pagination_refresh.gif" title="refresh" /></div>
                <a id="refresh" href="#">Refresh</a>
            </div>
            <div class="pagination_actions" id="prevPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_previcon.gif" title="prev" /></div>
                <a id="prevPage" href="#">Prev</a>
            </div>
            <div class="pagination_actions" id="nextPage_div">
                <div class="pagination_actionicon">
                    <img src="images/pagination_nexticon.gif" title="next" /></div>
                <a id="nextPage" href="#">Next</a>
            </div>
        </div>
    </div>
</div>

<!-- Snapshot Template (begin) -->
<div id="snapshot_template" style="display: none">
    <div class="adding_loading" style="display: none;">
        <div class="adding_animation">
        </div>
        <div class="adding_text">
            Waiting &hellip;
        </div>
    </div>
    <div id="row_container">
        <div class="grid_smallgenrow_cell" style="width: 5%">
            <div class="netgrid_celltitles" id="id">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 20%">
            <div class="netgrid_celltitles" id="name">
            </div>
        </div>    
        <div class="grid_smallgenrow_cell" style="width: 10%">
            <div class="netgrid_celltitles" id="volume">
            </div>
        </div>    
        <div class="grid_smallgenrow_cell" style="width: 8%">
            <div class="netgrid_celltitles" id="interval_type">
            </div>
        </div>    
       <div class="grid_smallgenrow_cell" style="width: 10%">
            <div class="netgrid_celltitles" id="created">
            </div>
        </div>
       <div class="grid_smallgenrow_cell" style="width: 10%" id="snapshot_account_container">
            <div class="netgrid_celltitles" id="account">
            </div>
        </div>
        <div class="grid_smallgenrow_cell" style="width: 10%" id="snapshot_domain_container">
            <div class="netgrid_celltitles" id="domain">
            </div>
        </div>        
        <div class="grid_smallgenrow_cell" style="width: 22%">
            <div class="netgrid_celltitles" id="detail_action">
                <span>
                    <a id="snapshot_action_create_volume" href="#">Create Volume</a>
                </span>
                <span>   
                    <span>&nbsp;&nbsp;|</span>
                    <a id="snapshot_action_delete" href="#">Delete</a>
                </span> 
                <span id="snapshot_action_create_template_container">   
                    <span>&nbsp;&nbsp;|</span>
                    <a id="snapshot_action_create_template" href="#">Create Template</a>
                </span> 
            </div>
        </div>        
    </div>
</div>
<!-- Snapshot Template (begin) -->
<!--*** Snapshot (end) *** -->


<!-- Create template from snapshot (begin) -->
<div id="dialog_create_template_from_snapshot" title="Create Template from Snapshot" style="display:none">	
	<div class="dialog_formcontent">
		<form action="#" method="post" id="form6">
			<ol>
				<li>
					<label>Name:</label>
					<input class="text" type="text" id="name" style="width:250px"/>
					<div id="name_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>
				<li>
					<label>Display Text:</label>
					<input class="text" type="text" id="display_text" style="width:250px"/>
					<div id="display_text_errormsg" class="dialog_formcontent_errormsg" style="display:none;"></div>
				</li>				
				<li>
					<label>OS Type:</label>
					<select class="select" id="os_type">
					</select>
				</li>		
				<li>
					<label>Password Enabled?:</label>
					<select class="select" id="password">						
						<option value="false">No</option>
						<option value="true">Yes</option>
					</select>
				</li>
			</ol>
		</form>
	</div>
</div>
<!-- Create template from snapshot (end) -->
