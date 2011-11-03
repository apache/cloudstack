<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<c:if test="${!empty cookie.lang}">
	<fmt:setLocale value="${cookie.lang.value}" />
</c:if>
<fmt:setBundle basename="resources/messages"/>

<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_dashboardicon.gif"/></div>
    <h1>
        <fmt:message key="label.menu.dashboard"/>
    </h1>
</div>

<!--Dashboard Admin (begin)-->
<div id="dashboard_admin" style="display: none">
    <div class="contentbox">
        <div class="rightpanel_mainloaderbox" style="display: none;">
            <div class="rightpanel_mainloader_animatedicon">
            </div>
            <p>
                <fmt:message key="label.loading"/> &hellip;
            </p>
        </div>
        <div class="grid_container" id="system_wide_capacity_container">
            <div class="grid_header">
                <div class="grid_header_cell" style="width: 40%; border: none;">
                    <div class="grid_header_title">
                        <fmt:message key="label.system.capacity"/></div>
                </div>
                <div class="grid_header_cell" style="width: 60%; border: none;">
                    <div class="grid_header_formbox">
                        <label for="zone" class="label">
                            <fmt:message key="label.zone"/>:</label>
                        <select id="capacity_zone_select" class="select" style="width: 110px;">
                        </select>
                        <label for="pod" class="label">
                            <fmt:message key="label.pod"/>:</label>
                        <select id="capacity_pod_select" class="select" style="width: 110px;">
                        </select>
                    </div>
                </div>
            </div>
            <div class="dbrow even" id="public_ip_address">
                <div class="dbrow_cell" style="width: 35%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            <fmt:message key="label.public.ips"/></h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                <fmt:message key="label.allocated"/>: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 52%; border: none;">
                    <div class="db_barbox low" id="bar_chart">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
            <div class="dbrow odd" id="private_ip_address">
                <div class="dbrow_cell" style="width: 35%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            <fmt:message key="label.private.ips"/></h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                <fmt:message key="label.allocated"/>: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 52%; border: none;">
                    <div class="db_barbox low" id="bar_chart">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
            <div class="dbrow even" id="memory_allocated">
                <div class="dbrow_cell" style="width: 35%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            <fmt:message key="label.memory.allocated"/></h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                <fmt:message key="label.allocated"/>: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 52%; border: none;">
                    <div class="db_barbox mid" id="bar_chart">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
            <div class="dbrow odd" id="cpu">
                <div class="dbrow_cell" style="width: 35%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            <fmt:message key="label.cpu.allocated"/></h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                <fmt:message key="label.allocated"/>: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 52%; border: none;">
                    <div class="db_barbox high" id="bar_chart">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
            <div class="dbrow even" id="primary_storage_allocated">
                <div class="dbrow_cell" style="width: 35%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            <fmt:message key="label.primary.allocated"/></h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                <fmt:message key="label.allocated"/>: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 52%; border: none;">
                    <div class="db_barbox low" id="bar_chart">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
            <div class="dbrow odd" id="primary_storage_used">
                <div class="dbrow_cell" style="width: 35%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            <fmt:message key="label.primary.used"/></h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                <fmt:message key="label.used"/>: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 52%; border: none;">
                    <div class="db_barbox low" id="bar_chart">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
            <div class="dbrow even" id="secondary_storage_used">
                <div class="dbrow_cell" style="width: 35%;">
                    <div class="dbgraph_titlebox">
                        <h2>
                            <fmt:message key="label.secondary.used"/></h2>
                        <div class="dbgraph_title_usedbox">
                            <p>
                                <fmt:message key="label.used"/>: <span id="capacityused">N</span>/<span id="capacitytotal">A</span>
                            </p>
                        </div>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 52%; border: none;">
                    <div class="db_barbox low" id="bar_chart">
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 12%; border: none;">
                    <div class="db_totaltitle" id="percentused">
                    </div>
                </div>
            </div>
        </div>
        <!--General Alerts-->
        <div class="grid_container" id="general_alerts" style="width: 49%; margin-top: 15px;">
            <div class="grid_header">
                <div class="grid_header_cell" style="width: 60%; border: none;">
                    <div class="grid_header_title">
                        <fmt:message key="label.general.alerts"/></div>
                </div>
                <div class="grid_header_cell" style="width: 40%; border: none;">
                    <div class="grid_header_formbox">
                        <div class="gridheader_morebutt" id="more_icon">
                        </div>
                    </div>
                </div>
            </div>
            <div id="alert_grid_content">
                <div style="height: 310px; text-align: center;">
                    <i><fmt:message key="label.no.alerts"/></i>
                </div>
            </div>
        </div>
        <!--Hosts Alerts-->
        <div class="grid_container" id="hosts_alerts" style="width: 48%; margin-top: 15px; float: right;">
            <div class="grid_header">
                <div class="grid_header_cell" style="width: 60%; border: none;">
                    <div class="grid_header_title">
                        <fmt:message key="label.host.alerts"/></div>
                </div>
                <div class="grid_header_cell" style="width: 40%; border: none;">
                    <div class="grid_header_formbox">
                        <!--  
                        <div class="gridheader_morebutt" id="more_icon">
                        </div>
                        -->
                    </div>
                </div>
            </div>
            <div id="host_alert_grid_content">
                <div style="height: 310px; text-align: center;">
                    <i><fmt:message key="label.no.alerts"/></i>
                </div>
            </div>
        </div>
    </div>
</div>
<!--Dashboard Admin (end)-->
<!--Dashboard DomainAdmin (begin)-->
<div id="dashboard_domainadmin" style="display: none">
    <div class="contentbox">
        <div class="rightpanel_mainloaderbox" style="display: none;">
            <div class="rightpanel_mainloader_animatedicon">
            </div>
            <p>
                <fmt:message key="label.loading"/> &hellip;
            </p>
        </div>
        <div class="grid_container" style="width: 49%; border: none;">
            <div class="dbrow even" style="border: 1px solid #CCC;">
                <div class="dbrow_cell" style="width: 74%; border: none;">
                    <div class="resource_titlebox">
                        <div class="domain_dbicons">
                            <img src="images/instance_dbdomain.png" /></div>
                        <h2 style="width:60%;">
                            <fmt:message key="label.instance"/></h2>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background: #cacaca repeat top left;">
                    <div class="domain_resources_totalbg">
                        <p id="instance_total">
                            0</p>
                    </div>
                </div>
            </div>
            <div class="dbrow odd" style="margin-top: 8px; border: 1px solid #CCC;">
                <div class="dbrow_cell" style="width: 74%; border: none;">
                    <div class="resource_titlebox">
                        <div class="domain_dbicons">
                            <img src="images/diskvolume_dbdomain.png" /></div>
                       <h2 style="width:60%;">
                            <fmt:message key="label.disk.volume"/></h2>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background: #cacaca repeat top left;">
                    <div class="domain_resources_totalbg">
                        <p id="volume_total">
                            0</p>
                    </div>
                </div>
            </div>
            <div class="dbrow even" style="margin-top: 8px; border: 1px solid #CCC;">
                <div class="dbrow_cell" style="width: 74%; border: none;">
                    <div class="resource_titlebox">
                        <div class="domain_dbicons">
                            <img src="images/snapshots_dbdomain.png" /></div>
                        <h2 style="width:60%;">
                            <fmt:message key="label.snapshots"/></h2>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background: #cacaca repeat top left;">
                    <div class="domain_resources_totalbg">
                        <p id="snapshot_total">
                            0</p>
                    </div>
                </div>
            </div>
            <div class="dbrow odd" style="margin-top: 8px; border: 1px solid #CCC;">
                <div class="dbrow_cell" style="width: 74%; border: none;">
                    <div class="resource_titlebox">
                        <div class="domain_dbicons">
                            <img src="images/users_dbdomain.png" /></div>
                        <h2 style="width:60%;">
                            <fmt:message key="label.accounts"/></h2>
                    </div>
                </div>
                <div class="dbrow_cell" style="width: 25%; border: none; background: #cacaca repeat top left;">
                    <div class="domain_resources_totalbg">
                        <p id="account_total">
                            0</p>
                    </div>
                </div>
            </div>
        </div>
                
        <!--Recent errors-->
	    <div class="grid_container" style="width: 49%; float: right;">
	        <div class="grid_header">
	            <div class="grid_header_cell" style="width: 60%; border: none;">
	                <div class="grid_header_title">
	                    <fmt:message key="label.recent.errors"/></div>
	            </div>
	            <div class="grid_header_cell" style="width: 40%; border: none;">
	            </div>
	        </div>
	        <div id="alert_grid_content">
	            <div style="height: 310px; text-align: center;">
	                <i><fmt:message key="label.no.errors"/></i>
	            </div>
	        </div>
	    </div>    
	    
    </div>
</div>
<!--Dashboard DomainAdmin (end) here-->

<!--Dashboard User (begin)-->
<div id="dashboard_user" style="display: none">
    <div class="grid_container">
        <div class="grid_header">
            <div class="grid_header_cell" style="width: 60%; border: none;">
                <div class="grid_header_title">
                    <fmt:message key="label.resources"/></div>
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
                            <fmt:message key="label.running.vms"/>:</div>
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
                            <fmt:message key="label.stopped.vms"/>:</div>
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
                            <fmt:message key="label.total.vms"/>:</div>
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
    <div class="grid_container" style="margin-top: 15px;">
        <div class="grid_header">
            <div class="grid_header_cell" style="width: 60%; border: none;">
                <div class="grid_header_title">
                    <fmt:message key="label.public.ips"/></div>
            </div>
        </div>
        <div class="grid_rows even" style="width: 50%;">
            <div class="grid_row_cell" style="width: 40%; border: none;">
                <div class="row_celltitles">
                    <fmt:message key="label.available.public.ips"/>:
                </div>
            </div>
            <div class="grid_row_cell" style="width: 59%; border: none;">
                <div class="row_celltitles">
                    <strong id="db_available_public_ips"></strong>
                </div>
            </div>
        </div>
        <div class="grid_rows odd" style="width: 50%;">
            <div class="grid_row_cell" style="width: 40%; border: none;">
                <div class="row_celltitles">
                    <fmt:message key="label.owned.public.ips"/>:
                </div>
            </div>
            <div class="grid_row_cell" style="width: 59%; border: none;">
                <div class="row_celltitles">
                    <strong id="db_owned_public_ips"></strong>
                </div>
            </div>
        </div>
    </div>
    <!--Recent errors-->
    <div class="grid_container" style="width: 49%; margin-top: 15px;">
        <div class="grid_header">
            <div class="grid_header_cell" style="width: 60%; border: none;">
                <div class="grid_header_title">
                    <fmt:message key="label.recent.errors"/></div>
            </div>
            <div class="grid_header_cell" style="width: 40%; border: none;">
            </div>
        </div>
        <div id="alert_grid_content">
            <div style="height: 310px; text-align: center;">
                <i><fmt:message key="label.no.errors"/></i>
            </div>
        </div>
    </div>
    <!--Accounts-->
    <div class="grid_container" style="width: 49%; margin-top: 15px; float: right; background: #fff8c8 repeat top left;">
        <div class="grid_header" style="background: url(images/dbaccounts_headerbg.gif) repeat-x top left;">
            <div class="grid_header_cell" style="width: 5%; border: none;">
                <div class="grid_header_title">
                    <img src="images/db_accounticon.png" alt="Account" /></div>
            </div>
            <div class="grid_header_cell" style="width: 60%; border: none;">
                <div class="grid_header_title" style="color: #FFF;">
                    <fmt:message key="label.my.account"/></div>
            </div>
        </div>
        <div class="dbaccounts_rows">
            <div class="grid_row_cell" style="width: 30%;">
                <div class="row_celltitles">
                    <fmt:message key="label.account.id"/></div>
            </div>
            <div class="grid_row_cell" style="width: 60%; border: none;">
                <div class="row_celltitles">
                    <strong id="db_account_id"></strong>
                </div>
            </div>
        </div>
        <div class="dbaccounts_rows">
            <div class="grid_row_cell" style="width: 30%;">
                <div class="row_celltitles">
                    <fmt:message key="label.account"/></div>
            </div>
            <div class="grid_row_cell" style="width: 60%; border: none;">
                <div class="row_celltitles">
                    <strong id="db_account"></strong>
                </div>
            </div>
        </div>
        <div class="dbaccounts_rows">
            <div class="grid_row_cell" style="width: 30%;">
                <div class="row_celltitles">
                    <fmt:message key="label.type"/></div>
            </div>
            <div class="grid_row_cell" style="width: 60%; border: none;">
                <div class="row_celltitles">
                    <strong id="db_type"></strong>
                </div>
            </div>
        </div>
        <div class="dbaccounts_rows">
            <div class="grid_row_cell" style="width: 30%;">
                <div class="row_celltitles">
                    <fmt:message key="label.domain"/></div>
            </div>
            <div class="grid_row_cell" style="width: 60%; border: none;">
                <div class="row_celltitles">
                    <strong id="db_domain"></strong>
                </div>
            </div>
        </div>
    </div>
</div>
<!--Dashboard User (end)-->

<!-- alert template (begin) -->
<div class="grid_rows" id="alert_template" style="display: none">
    <div class="grid_row_cell" style="width: 10%;">
        <div class="row_celltitles">
            <img src="images/alert_icon.png" /></div>
    </div>
    <div class="grid_row_cell" style="width: 63%;">
        <div class="row_celltitles alert" id="type">
        </div>
        <div class="row_celltitles alertdetails" id="description">
        </div>
    </div>
    <div class="grid_row_cell" style="width: 26%;">
        <div class="row_celltitles" id="date">
        </div>
    </div>
</div>
<!-- alert template (end) -->

