<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_dashboardicon.gif" alt="Dashboard" /></div>
    <h1>
        Dashboard
    </h1>
</div>

<!--Dashboard Admin (start)-->
<div id="dashboard_admin" style="display: none">
    <div class="contentbox">
        <div class="grid_container" id="system_wide_capacity_container">
            <div class="grid_header">
                <div class="grid_header_cell" style="width: 60%; border: none;">
                    <div class="grid_header_title">
                        System Wide Capacity</div>
                </div>
                <div class="grid_header_cell" style="width: 40%; border: none;">
                    <div class="grid_header_formbox">
                        <select id="capacity_zone_select" class="select" style="width: 70px;">
                        </select>
                        <select id="capacity_pod_select" class="select" style="width: 70px;">
                        </select>
                    </div>
                </div>
            </div>
            <div class="dbrow even" id="public_ip_address">
                <div class="dbrow_cell" style="width: 29%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            Public IP Addresses</h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                Used: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 58%; border: none;">
                    <div class="db_barbox low" id="bar_chart" style="width: 18%;">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
            <div class="dbrow odd" id="private_ip_address">
                <div class="dbrow_cell" style="width: 29%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            Private IP Addresses</h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                Used: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 58%; border: none;">
                    <div class="db_barbox low" id="bar_chart" style="width: 20%;">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
            <div class="dbrow even" id="memory_allocated">
                <div class="dbrow_cell" style="width: 29%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            Memory Allocated</h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                Used: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 58%; border: none;">
                    <div class="db_barbox mid" id="bar_chart" style="width: 66%;">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
            <div class="dbrow odd" id="cpu">
                <div class="dbrow_cell" style="width: 29%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            CPU</h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                Used: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 58%; border: none;">
                    <div class="db_barbox high" id="bar_chart" style="width: 83%;">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
            <div class="dbrow even" id="primary_storage_allocated">
                <div class="dbrow_cell" style="width: 29%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            Primary Storage Allocated</h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                Used: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 58%; border: none;">
                    <div class="db_barbox low" id="bar_chart" style="width: 15%;">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
            <div class="dbrow odd" id="primary_storage_used">
                <div class="dbrow_cell" style="width: 29%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            Primary Storage Used</h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                Used: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 58%; border: none;">
                    <div class="db_barbox low" id="bar_chart" style="width: 40%;">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
            <div class="dbrow even" id="secondary_storage_used">
                <div class="dbrow_cell" style="width: 29%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            Secondary Storage Used</h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                Used: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 58%; border: none;">
                    <div class="db_barbox low" id="bar_chart" style="width: 20%;">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
        </div>
        <!--General Alerts-->
        <div class="grid_container" style="width: 49%; margin-top: 15px;">
            <div class="grid_header">
                <div class="grid_header_cell" style="width: 60%; border: none;">
                    <div class="grid_header_title">
                        General Alerts</div>
                </div>
                <div class="grid_header_cell" style="width: 40%; border: none;">
                    <div class="grid_header_formbox">
                        <div class="gridheader_morebutt">
                        </div>
                    </div>
                </div>
            </div>
            <div id="alert_grid_content">
                <div style="height: 310px; text-align: center;">
                    <i>No Recent Alerts</i>
                </div>
            </div>
        </div>
        <!--Hosts Alerts-->
        <div class="grid_container" style="width: 48%; margin-top: 15px; float: right;">
            <div class="grid_header">
                <div class="grid_header_cell" style="width: 60%; border: none;">
                    <div class="grid_header_title">
                        Hosts Alerts</div>
                </div>
                <div class="grid_header_cell" style="width: 40%; border: none;">
                    <div class="grid_header_formbox">
                        <div class="gridheader_morebutt">
                        </div>
                    </div>
                </div>
            </div>
            <div id="host_alert_grid_content">
                <div style="height: 310px; text-align: center;">
                    <i>No Recent Alerts</i>
                </div>
            </div>
        </div>
    </div>
    <div class="grid_rows" id="alert_template" style="display: none">
        <div class="grid_row_cell" style="width: 10%;">
            <div class="row_celltitles">
                <img src="images/alert_icon.png" /></div>
        </div>
        <div class="grid_row_cell" style="width: 70%;">
            <div class="row_celltitles alert" id="type">
            </div>
            <div class="row_celltitles alertdetails" id="description">
            </div>
        </div>
        <div class="grid_row_cell" style="width: 19%;">
            <div class="row_celltitles" id="date">
            </div>
        </div>
    </div>
</div>
<!--Dashboard Admin (end)-->

<!--Dashboard DomainAdmin (start)-->
<div id="dashboard_domainadmin" style="display: none">
</div>
<!--Dashboard DomainAdmin (end) here-->

<!--Dashboard User (start)-->
<div id="dashboard_user" style="display: none">
    <div class="grid_container">
        <div class="grid_header">
            <div class="grid_header_cell" style="width: 60%; border: none;">
                <div class="grid_header_title">
                    Resources</div>
            </div>
        </div>
        <div class="grid_rows" style="padding: 0; border: none;">
            <div class="dbrow_cell" style="width: 33%; height: 137px; background: url(images/db_runningbg.gif) repeat-x top left;
                border: none;">
                <div class="db_resourcebox">
                    <div class="db_resourcebox_top">
                        <div class="db_resourcebox_iconbox">
                            <div class="db_resourcebox_icons">
                                <img src="images/db_runninicon.gif" alt="Running" /></div>
                        </div>
                        <div class="db_resourcebox_textbox">
                            <p>
                            Running VMs:</div>
                    </div>
                    <div class="db_resourcebox_bot">
                        <div class="db_resourcebox_VMnumber running">
                            <span id="db_running_vms">?</span> VM(s)</div>
                    </div>
                </div>
            </div>
            <div class="dbrow_cell" style="width: 33%; height: 137px; background: url(images/db_stoppedbg.gif) repeat-x top left;
                border: none;">
                <div class="db_resourcebox">
                    <div class="db_resourcebox_top">
                        <div class="db_resourcebox_iconbox">
                            <div class="db_resourcebox_icons">
                                <img src="images/db_stoppedicon.gif" alt="Running" /></div>
                        </div>
                        <div class="db_resourcebox_textbox">
                            <p>
                            Stopped VMs:</div>
                    </div>
                    <div class="db_resourcebox_bot">
                        <div class="db_resourcebox_VMnumber stopped">
                            <span id="db_stopped_vms">?</span> VM(s)</div>
                    </div>
                </div>
            </div>
            <div class="dbrow_cell" style="width: 34%; height: 137px; background: url(images/db_totalbg.gif) repeat-x top left;
                border: none;">
                <div class="db_resourcebox">
                    <div class="db_resourcebox_top">
                        <div class="db_resourcebox_iconbox">
                            <div class="db_resourcebox_icons">
                                <img src="images/db_totalicon.gif" alt="Running" /></div>
                        </div>
                        <div class="db_resourcebox_textbox">
                            <p>
                            Total VMs:</div>
                    </div>
                    <div class="db_resourcebox_bot">
                        <div class="db_resourcebox_VMnumber total">
                            <span id="db_total_vms">?</span> VM(s)</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <!--Public Ips-->
    <div class="grid_container" style="margin-top: 15px;" style="display: none; "">
        <div class="grid_header">
            <div class="grid_header_cell" style="width: 60%; border: none;">
                <div class="grid_header_title">
                    Public IPs</div>
            </div>
        </div>
        <div class="grid_rows even" style="width: 50%;">
            <div class="grid_row_cell" style="width: 40%; border: none;">
                <div class="row_celltitles">
                    Available Public IPs:
                </div>
            </div>
            <div class="grid_row_cell" style="width: 59%; border: none;">
                <div class="row_celltitles">
                    <strong>Unlimited</strong></div>
            </div>
        </div>
        <div class="grid_rows odd" style="width: 50%;">
            <div class="grid_row_cell" style="width: 40%; border: none;">
                <div class="row_celltitles">
                    Owned Public IPs:
                </div>
            </div>
            <div class="grid_row_cell" style="width: 59%; border: none;">
                <div class="row_celltitles">
                    <strong>5</strong></div>
            </div>
        </div>
    </div>
    <!--Recent errors-->
    <div class="grid_container" style="width: 49%; margin-top: 15px;" style="display: none;
        "">
        <div class="grid_header">
            <div class="grid_header_cell" style="width: 60%; border: none;">
                <div class="grid_header_title">
                    Recent Errors</div>
            </div>
            <div class="grid_header_cell" style="width: 40%; border: none;">
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert" id="type">
                    Error's Name</div>
                <div class="row_celltitles alertdetails" id="description">
                    Error Description will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles" id="date">
                    09/29/2010 15:20:10</div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert" id="type">
                    Error's Name</div>
                <div class="row_celltitles alertdetails" id="description">
                    Error Description will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles" id="date">
                    09/29/2010 15:20:10</div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert" id="type">
                    Error's Name</div>
                <div class="row_celltitles alertdetails" id="description">
                    Error Description will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles" id="date">
                    09/29/2010 15:20:10</div>
            </div>
        </div>
    </div>
    <!--Accounts-->
    <div class="grid_container" style="width: 49%; margin-top: 15px; float: right; background: #fff8c8 repeat top left;"
        style="display: none; "">
        <div class="grid_header" style="background: url(images/dbaccounts_headerbg.gif) repeat-x top left;">
            <div class="grid_header_cell" style="width: 5%; border: none;">
                <div class="grid_header_title">
                    <img src="images/db_accounticon.png" alt="Account" /></div>
            </div>
            <div class="grid_header_cell" style="width: 60%; border: none;">
                <div class="grid_header_title" style="color: #FFF;">
                    My Account</div>
            </div>
        </div>
        <div class="dbaccounts_rows">
            <div class="grid_row_cell" style="width: 30%;">
                <div class="row_celltitles">
                    Account ID</div>
            </div>
            <div class="grid_row_cell" style="width: 60%; border: none;">
                <div class="row_celltitles">
                    <strong id="db_account_id"></strong></div>
            </div>
        </div>
        <div class="dbaccounts_rows">
            <div class="grid_row_cell" style="width: 30%;">
                <div class="row_celltitles">
                    Account</div>
            </div>
            <div class="grid_row_cell" style="width: 60%; border: none;">
                <div class="row_celltitles">
                    <strong id="db_account"></strong></div>
            </div>
        </div>
        <div class="dbaccounts_rows">
            <div class="grid_row_cell" style="width: 30%;">
                <div class="row_celltitles">
                    Type</div>
            </div>
            <div class="grid_row_cell" style="width: 60%; border: none;">
                <div class="row_celltitles">
                    <strong id="db_type"></strong></div>
            </div>
        </div>
        <div class="dbaccounts_rows">
            <div class="grid_row_cell" style="width: 30%;">
                <div class="row_celltitles">
                    Domain</div>
            </div>
            <div class="grid_row_cell" style="width: 60%; border: none;">
                <div class="row_celltitles">
                    <strong id="db_domain"></strong></div>
            </div>
        </div>
    </div>
</div>
<!--Dashboard User (end)-->
