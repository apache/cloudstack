<!-- Content Panel -->
<div id="loading_gridtable" class="loading_gridtable">
    <div class="loading_gridanimation">
    </div>
    <p>
        Loading...</p>
</div>
<!-- Dashboard-User -->
<div class="maincontent" id="tab_dashboard_user" style="display: none;">
    <div id="maincontent_title">
        <div class="maintitle_icon">
            <img src="images/dashboardtitle_icons.gif" title="dashboard" />
        </div>
        <h1>
            Dashboard</h1>
    </div>
   
    <div class="small_gridconatiner_left">
        <div class="grid_header">
            <div class="grid_headertitles">
                Public IPs</div>
        </div>
        <div class="dbsmallrow_odd" style="height: 33px;">
            <div class="smallgrid_cell1">
                <div class="grid_celltitles">
                    Available Public IPs:</div>
            </div>
            <div class="smallgrid_cell2">
                <div class="grid_celltitles" id="db_available_public_ips">
                    3</div>
            </div>
        </div>
        <div class="dbsmallrow_even" style="height: 33px;">
            <div class="smallgrid_cell1">
                <div class="grid_celltitles">
                    Owned Public IPs:</div>
            </div>
            <div class="smallgrid_cell2">
                <div class="grid_celltitles" id="db_owned_public_ips">
                    2</div>
            </div>
        </div>
    </div>    
    
    <div class="small_gridconatiner_right" id="network_bandwidth_panel">
        <div class="grid_header">
            <div class="grid_headertitles">
                Network Bandwidth</div>
        </div>
        <div class="dbsmallrow_odd" style="height: 33px;">
            <div class="smallgrid_cell1">
                <div class="grid_celltitles">
                    Traffic Sent:</div>
            </div>
            <div class="smallgrid_cell2">
                <div class="bytes_icon">
                    <img src="images/bytes_in.gif" alt="#" /></div>
                <div class="grid_celltitles" id="db_sent">
                </div>
            </div>
        </div>
        <div class="dbsmallrow_even" style="height: 33px;">
            <div class="smallgrid_cell1">
                <div class="grid_celltitles">
                    Traffic Received:</div>
            </div>
            <div class="smallgrid_cell2">
                <div class="bytes_icon">
                    <img src="images/bytes_out.gif" alt="#" /></div>
                <div class="grid_celltitles" id="db_received">
                </div>
            </div>
        </div>
    </div>    <div class="grid_container">
        <div class="grid_header">
            <div class="grid_headertitles">
                Resources</div>
        </div>
        <div class="db_vmcontainer">
            <div class="db_vmcontainerleft">
            </div>
            <div class="db_vmcontainermid">
                <div class="db_vmcontainermidleft">
                    <div class="db_vmicons">
                        <img src="images/working_vm.gif" alt="#" /></div>
                    <div class="grid_statusicons">
                        <img src="images/running_icon.gif" alt="#" /></div>
                </div>
                <div class="db_vmcontainermidright">
                    <p>
                        Running VMs: <span id="db_running_vms"></span>
                    </p>
                </div>
            </div>
        </div>
        <div class="db_vmcontainerright">
        </div>
        <div class="db_blank">
        </div>
        <div class="db_vmcontainer">
            <div class="db_vmcontainerleft">
            </div>
            <div class="db_vmcontainermid">
                <div class="db_vmcontainermidleft">
                    <div class="db_vmicons">
                        <img src="images/stopped_vm.gif" alt="#" /></div>
                    <div class="grid_statusicons">
                        <img src="images/stopped_icon.gif" alt="#" /></div>
                </div>
                <div class="db_vmcontainermidright">
                    <p>
                        Stopped VMs: <span style="color: #ad0000;" id="db_stopped_vms"></span>
                    </p>
                </div>
            </div>
        </div>
        <div class="db_vmcontainerright">
        </div>
        <div class="db_blank">
        </div>
        <div class="db_vmcontainer">
            <div class="db_vmyellowcontainerleft">
            </div>
            <div class="db_vmyellowcontainermid">
                <div class="db_vmcontainermidleft">
                    <div class="db_vmicons">
                        <img src="images/total_vm.gif" alt="#" /></div>
                </div>
                <div class="db_vmcontainermidright">
                    <p>
                        Total VMs: <span style="color: #0078be" id="db_total_vms"></span>
                        <br />
                        Available VMs: <span style="color: #0078be" id="db_avail_vms"></span>
                    </p>
                </div>
            </div>
        </div>
        <div class="db_vmyellowcontainerright">
        </div>
    </div>
    <div class="small_gridconatiner_left" id="db_recent_errors">
        <div class="grid_header">
            <div class="grid_headertitles">
                Recent Errors!</div>
        </div>
        <div id="error_grid_content">
            <div style="height: 236px; text-align: center;">
                <i>No Recent Errors</i></div>
        </div>
    </div>
    <div class="small_gridconatiner_right">
        <div class="grid_header">
            <div class="grid_headertitles">
                My Accounts</div>
        </div>
        <div class="account_box">
            <div class="pin_icon">
            </div>
            <div class="account_info">
                <p>
                    Account Id: <span id="db_account_id"></span>
                </p>
                <p>
                    Account: <span id="db_account"></span>
                </p>
                <p>
                    Type: <span id="db_type"></span>
                </p>
                <p>
                    Domain: <span id="db_domain"></span>
                </p>
            </div>
        </div>
        <div class="account_boxshadow">
        </div>
    </div>
</div>
<!-- Recent Errors Template -->
<div class="dbsmallrow_odd" style="height: 70px; display: none;" id="recent_error_template">
    <div class="alert_box">
        <div class="alert_box_left">
            <div class="alert_icon">
            </div>
        </div>
        <div class="alert_box_mid">
            <p id="db_error_msg">
                <span id="db_error_type"></span>
                <br />
            </p>
        </div>
        <div class="alert_box_right" id="db_error_date">
            <p>
            </p>
        </div>
    </div>
</div>
<!-- Dashboard-Admin -->
<div class="maincontent" id="tab_dashboard_root" style="display: none;">
    <div id="maincontent_title">
        <div class="maintitle_icon">
            <img src="images/dashboardtitle_icons.gif" title="dashboard" />
        </div>
        <h1>
            Dashboard</h1>
    </div>
    <div class="net_gridwrapper" style="height: auto;">
    </div>
    <div class="db_admingrid_leftcontainer">
        <div class="db_admingrid_capacitypanel">
            <div class="db_capacitypanel_top">
                <div class="db_capacitypanel_titlebox">
                    <div class="db_capacitypanel_graphtitleicon">
                    </div>
                    <h2>
                        System Wide Capacity</h2>
                    <div class="dbadmin_selectionformarea">
                        <form action="#" method="post">
                        <select class="select" id="capacity_zone_select">
                        </select>
                        <select class="select" id="capacity_pod_select">
                        </select>
                        </form>
                    </div>
                </div>
            </div>
            <div class="db_capacitypanel_mid">
                <div class="db_capacitypanel_bardgpanel" id="capacity_public_ip">
                    <div class="db_capacitypanel_bardg_bg">
                        <div class="db_capacitypanel_bardg_bgtop">
                            <div class="db_capacitypanel_bardg_titlebg">
                                Public IP Addresses
                            </div>
                        </div>
                        <div class="db_capacitypanel_bardg_bgbot">
                            <div class="db_bargraph_panel">
                                <div class="db_bargraph_barbox">
                                    <div class="db_bargraph_barbox_top">
                                        <div class="db_bargraph_bar_safesmalltext" id="public_ip_used">
                                        </div>
                                    </div>
                                    <div class="db_bargraph_barbox_bot">
                                        <div class="db_bargraph_barbox_bar">
                                            <div class="db_bargraph_barbox_safezone" style="width: 10%;">
                                            </div>
                                            <div class="db_bargraph_barbox_unsafezone" style="width: 10%; display: none">
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="db_bargraph_bar_totaltext" id="public_ip_total">
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="db_capacitypanel_bardgpanel" id="capacity_private_ip">
                    <div class="db_capacitypanel_bardg_bg">
                        <div class="db_capacitypanel_bardg_bgtop">
                            <div class="db_capacitypanel_bardg_titlebg">
                                Private IP Addresses
                            </div>
                        </div>
                        <div class="db_capacitypanel_bardg_bgbot">
                            <div class="db_bargraph_panel">
                                <div class="db_bargraph_barbox">
                                    <div class="db_bargraph_barbox_top">
                                        <div class="db_bargraph_bar_safesmalltext" id="private_ip_used">
                                        </div>
                                    </div>
                                    <div class="db_bargraph_barbox_bot">
                                        <div class="db_bargraph_barbox_bar">
                                            <div class="db_bargraph_barbox_safezone">
                                            </div>
                                            <div class="db_bargraph_barbox_unsafezone" style="width: 10%; display: none">
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="db_bargraph_bar_totaltext" id="private_ip_total">
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="db_capacitypanel_bardgpanel" id="capacity_memory">
                    <div class="db_capacitypanel_bardg_bg">
                        <div class="db_capacitypanel_bardg_bgtop">
                            <div class="db_capacitypanel_bardg_titlebg">
                                Memory Allocated
                            </div>
                        </div>
                        <div class="db_capacitypanel_bardg_bgbot">
                            <div class="db_bargraph_panel">
                                <div class="db_bargraph_barbox">
                                    <div class="db_bargraph_barbox_top">
                                        <div class="db_bargraph_bar_safetext" id="memory_used">
                                        </div>
                                    </div>
                                    <div class="db_bargraph_barbox_bot">
                                        <div class="db_bargraph_barbox_bar">
                                            <div class="db_bargraph_barbox_safezone">
                                            </div>
                                            <div class="db_bargraph_barbox_unsafezone" style="width: 10%; display: none">
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="db_bargraph_bar_totaltext" id="memory_total">
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="db_capacitypanel_bardgpanel" id="capacity_cpu">
                    <div class="db_capacitypanel_bardg_bg">
                        <div class="db_capacitypanel_bardg_bgtop">
                            <div class="db_capacitypanel_bardg_titlebg">
                                CPU
                            </div>
                        </div>
                        <div class="db_capacitypanel_bardg_bgbot">
                            <div class="db_bargraph_panel">
                                <div class="db_bargraph_barbox">
                                    <div class="db_bargraph_barbox_top">
                                        <div class="db_bargraph_bar_safetext" id="cpu_used">
                                        </div>
                                    </div>
                                    <div class="db_bargraph_barbox_bot">
                                        <div class="db_bargraph_barbox_bar">
                                            <div class="db_bargraph_barbox_safezone">
                                            </div>
                                            <div class="db_bargraph_barbox_unsafezone" style="width: 10%; display: none">
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="db_bargraph_bar_totaltext" id="cpu_total">
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="db_capacitypanel_bardgpanel" id="capacity_storage_alloc">
                    <div class="db_capacitypanel_bardg_bg">
                        <div class="db_capacitypanel_bardg_bgtop">
                            <div class="db_capacitypanel_bardg_titlebg">
                                Pri. Storage Allocated
                            </div>
                        </div>
                        <div class="db_capacitypanel_bardg_bgbot">
                            <div class="db_bargraph_panel">
                                <div class="db_bargraph_barbox">
                                    <div class="db_bargraph_barbox_top">
                                        <div class="db_bargraph_bar_safetext" id="storage_alloc">
                                        </div>
                                    </div>
                                    <div class="db_bargraph_barbox_bot">
                                        <div class="db_bargraph_barbox_bar">
                                            <div class="db_bargraph_barbox_safezone">
                                            </div>
                                            <div class="db_bargraph_barbox_unsafezone" style="width: 10%; display: none">
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="db_bargraph_bar_totaltext" id="storage_alloc_total">
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="db_capacitypanel_bardgpanel" id="capacity_storage">
                    <div class="db_capacitypanel_bardg_bg">
                        <div class="db_capacitypanel_bardg_bgtop">
                            <div class="db_capacitypanel_bardg_titlebg">
                                Pri. Storage Used
                            </div>
                        </div>
                        <div class="db_capacitypanel_bardg_bgbot">
                            <div class="db_bargraph_panel">
                                <div class="db_bargraph_barbox">
                                    <div class="db_bargraph_barbox_top">
                                        <div class="db_bargraph_bar_safetext" id="storage_used">
                                        </div>
                                    </div>
                                    <div class="db_bargraph_barbox_bot">
                                        <div class="db_bargraph_barbox_bar">
                                            <div class="db_bargraph_barbox_safezone">
                                            </div>
                                            <div class="db_bargraph_barbox_unsafezone" style="width: 10%; display: none">
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="db_bargraph_bar_totaltext" id="storage_total">
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="db_capacitypanel_bardgpanel" id="capacity_sec_storage">
                    <div class="db_capacitypanel_bardg_bg">
                        <div class="db_capacitypanel_bardg_bgtop">
                            <div class="db_capacitypanel_bardg_titlebg">
                                Sec. Storage Used
                            </div>
                        </div>
                        <div class="db_capacitypanel_bardg_bgbot">
                            <div class="db_bargraph_panel">
                                <div class="db_bargraph_barbox">
                                    <div class="db_bargraph_barbox_top">
                                        <div class="db_bargraph_bar_safetext" id="sec_storage_used">
                                        </div>
                                    </div>
                                    <div class="db_bargraph_barbox_bot">
                                        <div class="db_bargraph_barbox_bar">
                                            <div class="db_bargraph_barbox_safezone">
                                            </div>
                                            <div class="db_bargraph_barbox_unsafezone" style="width: 10%; display: none">
                                            </div>
                                        </div>
                                    </div>
                                </div>
                                <div class="db_bargraph_bar_totaltext" id="sec_storage_total">
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="db_capacitypanel_bot">
            </div>
        </div>
    </div>
    <div class="db_admingrid_rightpanel">
        <div class="dbadmin_grid_container">
            <div class="grid_header">
                <div class="grid_headertitles">
                    General Alerts!</div>
                <div class="gridheader_morebutton" id="alert_more">
                </div>
            </div>
            <div id="alert_grid_content">
                <div style="height: 310px; text-align: center;">
                    <i>No Recent Alerts</i></div>
            </div>
        </div>
        <div class="dbadmin_grid_container">
            <div class="grid_header">
                <div class="grid_headertitles">
                    Hosts Alerts!</div>
                <div class="gridheader_morebutton" id="host_alert_more">
                </div>
            </div>
            <div id="host_alert_grid_content">
                <div style="height: 310px; text-align: center;">
                    <i>No Recent Alerts</i></div>
            </div>
        </div>
    </div>
</div>
<!-- Domain-Admin Dashboard -->
<div class="maincontent" id="tab_dashboard_domain" style="display: none;">
    <div id="maincontent_title">
        <div class="maintitle_icon">
            <img src="images/dashboardtitle_icons.gif" title="dashboard" />
        </div>
        <h1>
            Dashboard</h1>
    </div>
    <div class="net_gridwrapper" style="height: auto;">
    </div>
    <div class="db_admingrid_leftcontainer">
        <div class="db_admingrid_capacitypanel">
            <div class="db_domain_top">
            </div>
            <div class="db_capacitypanel_mid">
                <div class="db_domainbg">
                    <div class="db_domainbg_left">
                        <div class="db_domainicons">
                            <img src="images/domain_dbinstance.gif" alt="#" /></div>
                    </div>
                    <div class="db_domainbg_right">
                        <p>
                            Instances: <span id="dashboard_instances">0</span></p>
                    </div>
                </div>
                <div class="db_domainbg">
                    <div class="db_domainbg_left">
                        <div class="db_domainicons">
                            <img src="images/domain_dbdiskoff.gif" alt="#" /></div>
                    </div>
                    <div class="db_domainbg_right">
                        <p>
                            Disk Volume: <span id="dashboard_volumes">0</span></p>
                    </div>
                </div>
                <div class="db_domainbg">
                    <div class="db_domainbg_left">
                        <div class="db_domainicons">
                            <img src="images/domain_dbsnapshots.gif" alt="#" /></div>
                    </div>
                    <div class="db_domainbg_right">
                        <p>
                            Snapshots: <span id="dashboard_snapshots">0</span></p>
                    </div>
                </div>
                <div class="db_domainbg">
                    <div class="db_domainbg_left">
                        <div class="db_domainicons">
                            <img src="images/domain_dbaccount.gif" alt="#" /></div>
                    </div>
                    <div class="db_domainbg_right">
                        <p>
                            Accounts: <span id="dashboard_accounts">0</span></p>
                    </div>
                </div>
            </div>
        </div>
        <div class="db_capacitypanel_bot">
        </div>
    </div>
    <div class="db_admingrid_rightpanel">
        <div class="dbadmin_grid_container" id="db_recent_errors">
            <div class="grid_header">
                <div class="grid_headertitles">
                    Recent Errors!</div>
            </div>
            <div id="alert_grid_content">
                <div style="height: 450px; text-align: center;">
                    <i>No Recent Errors</i></div>
            </div>
        </div>
    </div>
</div>
