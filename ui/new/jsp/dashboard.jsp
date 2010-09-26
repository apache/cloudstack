<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>
<!-- event detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_dashboardicon.gif" alt="Event" /></div>
    <h1>
        Dashboard
    </h1>
</div>
<!--Dashboard-->
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="grid_container">
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
        <div class="dbrow even">
            <div class="dbrow_cell" style="width: 29%;">
                <div class="dbgraph_titlebox">
                    <h2>
                        Public IP Addresses</h2>
                    <div class="dbgraph_title_usedbox">
                        <p>
                            Used: <span>2 / 11 </span>
                        </p>
                    </div>
                </div>
            </div>
            <div class="dbrow_cell" style="width: 58%; border: none;">
                <div class="db_barbox low" style="width: 18%;">
                </div>
            </div>
            <div class="dbrow_cell" style="width: 12%; border: none;">
                <div class="db_totaltitle">
                    18.2 %</div>
            </div>
        </div>
        <div class="dbrow odd">
            <div class="dbrow_cell" style="width: 29%;">
                <div class="dbgraph_titlebox">
                    <h2>
                        Private IP Addresses</h2>
                    <div class="dbgraph_title_usedbox">
                        <p>
                            Used: <span>2 / 10 </span>
                        </p>
                    </div>
                </div>
            </div>
            <div class="dbrow_cell" style="width: 58%; border: none;">
                <div class="db_barbox low" style="width: 20%;">
                </div>
            </div>
            <div class="dbrow_cell" style="width: 12%; border: none;">
                <div class="db_totaltitle">
                    20 %</div>
            </div>
        </div>
        <div class="dbrow even">
            <div class="dbrow_cell" style="width: 29%;">
                <div class="dbgraph_titlebox">
                    <h2>
                        Memory Allocated</h2>
                    <div class="dbgraph_title_usedbox">
                        <p>
                            Used: <span>2.12 GB / 3.22 GB </span>
                        </p>
                    </div>
                </div>
            </div>
            <div class="dbrow_cell" style="width: 58%; border: none;">
                <div class="db_barbox mid" style="width: 66%;">
                </div>
            </div>
            <div class="dbrow_cell" style="width: 12%; border: none;">
                <div class="db_totaltitle">
                    65.8 %</div>
            </div>
        </div>
        <div class="dbrow odd">
            <div class="dbrow_cell" style="width: 29%;">
                <div class="dbgraph_titlebox">
                    <h2>
                        CPU</h2>
                    <div class="dbgraph_title_usedbox">
                        <p>
                            Used: <span>8 MHZ / 9.60 MHZ </span>
                        </p>
                    </div>
                </div>
            </div>
            <div class="dbrow_cell" style="width: 58%; border: none;">
                <div class="db_barbox high" style="width: 83%;">
                </div>
            </div>
            <div class="dbrow_cell" style="width: 12%; border: none;">
                <div class="db_totaltitle">
                    83.3 %</div>
            </div>
        </div>
        <div class="dbrow even">
            <div class="dbrow_cell" style="width: 29%;">
                <div class="dbgraph_titlebox">
                    <h2>
                        Primary Storage Allocated</h2>
                    <div class="dbgraph_title_usedbox">
                        <p>
                            Used: <span>Used: 4.00 GB / 3.50 TB </span>
                        </p>
                    </div>
                </div>
            </div>
            <div class="dbrow_cell" style="width: 58%; border: none;">
                <div class="db_barbox low" style="width: 15%;">
                </div>
            </div>
            <div class="dbrow_cell" style="width: 12%; border: none;">
                <div class="db_totaltitle">
                    15.2 %</div>
            </div>
        </div>
        <div class="dbrow odd">
            <div class="dbrow_cell" style="width: 29%;">
                <div class="dbgraph_titlebox">
                    <h2>
                        Primary Storage Used</h2>
                    <div class="dbgraph_title_usedbox">
                        <p>
                            Used: <span>999.17 GB / 1.75 TB </span>
                        </p>
                    </div>
                </div>
            </div>
            <div class="dbrow_cell" style="width: 58%; border: none;">
                <div class="db_barbox low" style="width: 40%;">
                </div>
            </div>
            <div class="dbrow_cell" style="width: 12%; border: none;">
                <div class="db_totaltitle">
                    40.12 %</div>
            </div>
        </div>
        <div class="dbrow even">
            <div class="dbrow_cell" style="width: 29%;">
                <div class="dbgraph_titlebox">
                    <h2>
                        Secondary Storage Used</h2>
                    <div class="dbgraph_title_usedbox">
                        <p>
                            Used: <span>599.17 GB / 1.75 TB </span>
                        </p>
                    </div>
                </div>
            </div>
            <div class="dbrow_cell" style="width: 58%; border: none;">
                <div class="db_barbox low" style="width: 20%;">
                </div>
            </div>
            <div class="dbrow_cell" style="width: 12%; border: none;">
                <div class="db_totaltitle">
                    20.12 %</div>
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
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert">
                    Alerts name</div>
                <div class="row_celltitles alertdetails">
                    Details about the alert will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles">
                    09/17/2010 14:33:49</div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert">
                    Alerts name</div>
                <div class="row_celltitles alertdetails">
                    Details about the alert will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles">
                    09/17/2010 14:33:49</div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert">
                    Alerts name</div>
                <div class="row_celltitles alertdetails">
                    Details about the alert will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles">
                    09/17/2010 14:33:49</div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert">
                    Alerts name</div>
                <div class="row_celltitles alertdetails">
                    Details about the alert will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles">
                    09/17/2010 14:33:49</div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert">
                    Alerts name</div>
                <div class="row_celltitles alertdetails">
                    Details about the alert will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles">
                    09/17/2010 14:33:49</div>
            </div>
        </div>
    </div>
    <!--Host Alerts-->
    <div class="grid_container" style="width: 48%; margin-top: 15px; float: right;">
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
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert">
                    Alerts name</div>
                <div class="row_celltitles alertdetails">
                    Details about the alert will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles">
                    09/17/2010 14:33:49</div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert">
                    Alerts name</div>
                <div class="row_celltitles alertdetails">
                    Details about the alert will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles">
                    09/17/2010 14:33:49</div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert">
                    Alerts name</div>
                <div class="row_celltitles alertdetails">
                    Details about the alert will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles">
                    09/17/2010 14:33:49</div>
            </div>
        </div>
        <div class="grid_rows odd">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert">
                    Alerts name</div>
                <div class="row_celltitles alertdetails">
                    Details about the alert will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles">
                    09/17/2010 14:33:49</div>
            </div>
        </div>
        <div class="grid_rows even">
            <div class="grid_row_cell" style="width: 10%;">
                <div class="row_celltitles">
                    <img src="images/alert_icon.png" /></div>
            </div>
            <div class="grid_row_cell" style="width: 70%;">
                <div class="row_celltitles alert">
                    Alerts name</div>
                <div class="row_celltitles alertdetails">
                    Details about the alert will appear here</div>
            </div>
            <div class="grid_row_cell" style="width: 19%;">
                <div class="row_celltitles">
                    09/17/2010 14:33:49</div>
            </div>
        </div>
    </div>
</div>
