<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>


<!-- IP Address detail panel (begin) -->
<div class="main_title" id="right_panel_header">
    <div class="main_titleicon">
        <img src="images/title_ipicon.gif" alt="IP Address" /></div>
    <h1>
        IP Address
    </h1>
</div>
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container_on_top" style="display: none">
        <p id="after_action_info">
        </p>
    </div>
    <div class="tabbox" style="margin-top: 15px;">
        <div class="content_tabs on" id="tab_details">
            <%=t.t("Details")%></div>
        <div class="content_tabs off" id="tab_port_forwarding">
            <%=t.t("Port Forwarding")%></div>
        <div class="content_tabs off" id="tab_load_balancer">
            <%=t.t("Load Balancer")%></div>
    </div>  
    <div id="tab_content_details">
    	<div class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div>

        <div class="grid_container">
        	<div class="grid_header">
            	<div id="grid_header_title" class="grid_header_title">(title)</div>
                <div class="grid_actionbox" id="action_link">
                    <div class="grid_actionsdropdown_box" id="action_menu" style="display: none;">
                        <ul class="actionsdropdown_boxlist" id="action_list">
                            <li><%=t.t("no.available.actions")%></li>
                        </ul>
                    </div>
                </div>
                <div class="gridheader_loaderbox" id="spinning_wheel" style="border: 1px solid #999;
                display: none;">
                    <div class="gridheader_loader" id="icon">
                    </div>
                    <p id="description">
                        Detaching Disk &hellip;</p>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        IP:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="ipaddress">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        Zone:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="zonename">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        VLAN:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="vlanname">
                    </div>
                </div>
            </div>           
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        Source NAT:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="source_nat">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        Network Type:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="network_type">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        Domain:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="domain">
                    </div>
                </div>
            </div>
            <div class="grid_rows odd">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        Account:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="account">
                    </div>
                </div>
            </div>
            <div class="grid_rows even">
                <div class="grid_row_cell" style="width: 20%;">
                    <div class="row_celltitles">
                        Allocated:</div>
                </div>
                <div class="grid_row_cell" style="width: 79%;">
                    <div class="row_celltitles" id="allocated">
                    </div>
                </div>
            </div>
        </div>
    </div>
    <!-- Details ends here-->
    <!-- Port Forwarding start here-->
    <div id="tab_content_port_forwarding" style="display:none">
    	<div class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div>
        <div class="grid_container" id="grid_container">
            <div class="grid_header">
                <div class="grid_header_cell" style="width: 15%">
                    <div class="grid_header_title">
                        Public Port</div>
                </div>
                <div class="grid_header_cell" style="width: 15%">
                    <div class="grid_header_title">
                        Private Port</div>
                </div>
                <div class="grid_header_cell" style="width: 15%">
                    <div class="grid_header_title">
                        Protocol</div>
                </div>
                <div class="grid_header_cell" style="width: 39%; border: none;">
                    <div class="grid_header_title">
                        Instance</div>
                </div>
                <div class="grid_header_cell" style="width: 15%">
                    <div class="grid_header_title">
                        Action</div>
                </div>
            </div>
            <div class="grid_rows even" id="create_port_forwarding_row">
                <div class="grid_row_cell" style="width: 15%;">
                    <input id="public_port" class="text" style="width: 70%;" type="text" />
                    <div id="public_port_errormsg" class="errormsg" style="display: none;">Error msg will appear here</div>
                </div>
                <div class="grid_row_cell" style="width: 15%;">
                    <input id="private_port" class="text" style="width: 70%;" type="text" />
                    <div id="private_port_errormsg" class="errormsg" style="display: none;">Error msg will appear here</div>
                </div>
                <div class="grid_row_cell" style="width: 15%;">  
                   <select class="select" id="protocol" style="width:70%;">
                       <option value="TCP">TCP</option>
                       <option value="UDP">UDP</option>
                   </select>
                </div>
                <div class="grid_row_cell" style="width: 39%;">                   
                    <select class="select" id="vm">
                    </select>
                </div>
                <div class="grid_row_cell" style="width: 15%;">
                    <div class="row_celltitles">
                        <a id="add_link" href="#">Add</a></div>
                </div>
            </div>              
            <div id="grid_content">
            </div>            
        </div>
    </div>
    <!-- Port Forwarding ends here-->
    <!-- Load Balancer start here-->
    <div id="tab_content_load_balancer" style="display:none">
    	<div class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div>
        <div class="grid_container">
            <div class="grid_header">
                <div class="grid_header_cell" style="width: 25%">
                    <div class="grid_header_title">
                        Name</div>
                </div>
                <div class="grid_header_cell" style="width: 15%">
                    <div class="grid_header_title">
                        Public Port</div>
                </div>
                <div class="grid_header_cell" style="width: 15%">
                    <div class="grid_header_title">
                        Private Port</div>
                </div>
                <div class="grid_header_cell" style="width: 15%; border: none;">
                    <div class="grid_header_title">
                        Algorithm</div>
                </div>
                <div class="grid_header_cell" style="width: 29%">
                    <div class="grid_header_title">
                        Action</div>
                </div>
            </div>
            <div class="grid_rows even" id="create_load_balancer_row">
                <div class="grid_row_cell" style="width: 25%;">
                    <input id="name" class="text" style="width: 70%;" type="text" />
                    <div id="name_errormsg" class="errormsg" style="display: none;">Error msg will appear here</div>
                </div>
                <div class="grid_row_cell" style="width: 15%;">
                    <input id="public_port" class="text" style="width: 70%;" type="text" />
                    <div id="public_port_errormsg" class="errormsg" style="display: none;">Error msg will appear here</div>
                </div>
                <div class="grid_row_cell" style="width: 15%;">
                    <input id="private_port" class="text" style="width: 70%;" type="text" />
                    <div id="private_port_errormsg" class="errormsg" style="display: none;">Error msg will appear here</div>
                </div>
                <div class="grid_row_cell" style="width: 15%;">
                    <select id="algorithm_select" class="select" style="width: 70%;">                       
						<option value="roundrobin">roundrobin</option>
                        <option value="leastconn">leastconn</option>
                        <option value="source">source</option>
                    </select>
                </div>
                <div class="grid_row_cell" style="width: 29%;">
                    <div class="row_celltitles">
                        <a id="add_link" href="#">Add</a></div>
                </div>
            </div>
            <div id="grid_content">
            </div>
        </div>
    </div>
    <!-- Load Balancer ends here-->
</div>
<!-- IP Address detail panel (end) -->



<!-- Load Balancer Template (begin) -->
<div class="grid_rows odd" id="load_balancer_template" style="display:none">
    <div id="row_container">
        <div class="grid_row_cell" style="width: 25%;">
            <div class="row_celltitles" id="name">
                LB#1</div>
        </div>
        <div class="grid_row_cell" style="width: 15%;">
            <div class="row_celltitles" id="public_port">
                8080</div>
        </div>
        <div class="grid_row_cell" style="width: 15%;">
            <div class="row_celltitles" id="private_port">
                80</div>
        </div>
        <div class="grid_row_cell" style="width: 15%;">
            <div class="row_celltitles" id="algorithm">
                (algorithm)</div>
        </div>
        <div class="grid_row_cell" style="width: 29%;">
            <div class="row_celltitles">
                <a id="manage_link" href="#">Manage</a></div>
            <div class="row_celltitles">
                <a id="delete_link" href="#">Delete</a></div>
            <div class="row_celltitles">
                <a id="edit_link" href="#">Edit</a></div>
        </div>       
        <div class="gridrow_loaderbox" style="display: none;" id="spinning_wheel">
            <div class="gridrow_loader">
            </div>
            <p id="description">
                Waiting &hellip;
            </p>
        </div>
    </div> 
    <div class="grid_rows odd" id="row_container_edit" style="display:none">
        <div class="grid_row_cell" style="width: 25%;">
            <input id="name" class="text" style="width: 90%;" type="text" />
            <div id="name_errormsg" class="errormsg" style="display: none;">Error msg will appear here</div>
        </div>
        <div class="grid_row_cell" style="width: 15%;">
            <div class="row_celltitles" id="public_port">8080</div>
        </div>
        <div class="grid_row_cell" style="width: 15%;">
            <input id="private_port" class="text" style="width: 90%;" type="text" />
            <div id="private_port_errormsg" class="errormsg" style="display: none;">Error msg will appear here</div>
        </div>
        <div class="grid_row_cell" style="width: 15%;">
            <select id="algorithm_select" class="select" style="width: 90%;">                       
				<option value="roundrobin">roundrobin</option>
                <option value="leastconn">leastconn</option>
                <option value="source">source</option>
            </select>
        </div>
        <div class="grid_row_cell" style="width: 29%;">
            <div class="row_celltitles">
                <a id="save_link" href="#">Save</a>
            </div>
            <div class="row_celltitles">
                <a id="cancel_link" href="#">Cancel</a>
            </div>
        </div>
        <div class="gridrow_loaderbox" style="display: none;" id="spinning_wheel">
            <div class="gridrow_loader">
            </div>
            <p id="description">
                Waiting &hellip;
            </p>
        </div>
    </div>  
    <div class="grid_detailspanel" id="management_area" style="display: none;">
        <div class="grid_details_pointer">
        </div>
        <div class="grid_detailsbox">
            <div class="grid_details_row odd" id="add_vm_to_lb_row">
                <div class="grid_row_cell" style="width: 9%;">
                    <div class="row_celltitles">
                        <img src="images/network_managevmicon.gif" /></div>
                </div>
                <div class="grid_row_cell" style="width: 60%;">
                    <select id="vm_select" class="select" style="width: 90%;">                      
                    </select>
                </div>
                <div class="grid_row_cell" style="width: 30%;">
                    <div class="row_celltitles">
                        <a id="assign_link" href="#">Assign</a></div>
                </div>
                <div id="spinning_wheel" class="gridrow_loaderbox" style="display: none;">
                    <div class="gridrow_loader">
                    </div>
                    <p>
                        Assigning instance to load balancer rule &hellip;</p>
                </div>
            </div>
            <div id="subgrid_content" class="ip_description_managearea">
            </div>
        </div>
    </div>
</div>
<!-- Load Balancer Template (end) -->

<!-- Load Balancer's VM subgrid template (begin) -->
<div id="load_balancer_vm_template" class="grid_details_row odd" style="display:none">
    <div class="grid_row_cell" style="width: 9%;">
        <div class="row_celltitles">
            <img src="images/network_managevmicon.gif" /></div>
    </div>
    <div class="grid_row_cell" style="width: 30%;">
        <div class="row_celltitles" id="vm_name"></div>
    </div>
    <div class="grid_row_cell" style="width: 30%;">
        <div class="row_celltitles" id="vm_private_ip"></div>
    </div>
    <div class="grid_row_cell" style="width: 30%;">
        <div class="row_celltitles">
            <a id="remove_link" href="#">Remove</a></div>
    </div>
    <div id="spinning_wheel" class="gridrow_loaderbox" style="display: none;">
        <div class="gridrow_loader">
        </div>
        <p>
            Removing instance from load balancer rule &hellip;</p>
    </div>
</div>
<!-- Load Balancer's VM subgrid template (end) -->

<!-- Port Forwarding template (begin) -->
<div class="grid_rows odd" id="port_forwarding_template" style="display: none">    
    <div id="row_container">
        <div class="grid_row_cell" style="width: 15%;">
            <div class="row_celltitles" id="public_port"></div>
        </div>
        <div class="grid_row_cell" style="width: 15%;">
            <div class="row_celltitles" id="private_port"></div>
        </div>
        <div class="grid_row_cell" style="width: 15%;">
            <div class="row_celltitles" id="protocol"></div>
        </div>
        <div class="grid_row_cell" style="width: 39%;">
            <div class="row_celltitles" id="vm_name"></div>
        </div>       
        <div class="grid_row_cell" style="width: 15%;">
            <div class="row_celltitles">
                <a id="edit_link" href="#">Edit</a>
            </div>
            <div class="row_celltitles">
                <a id="delete_link" href="#">Delete</a>
            </div>
        </div>
        <div class="gridrow_loaderbox" style="display: none;" id="spinning_wheel">
            <div class="gridrow_loader">
            </div>
            <p id="description">
                Waiting &hellip;
            </p>
        </div>
    </div>
    <div id="row_container_edit" style="display:none">
        <div class="grid_row_cell" style="width: 15%;">
            <div class="row_celltitles" id="public_port"></div>
        </div>
        <div class="grid_row_cell" style="width: 15%;">
            <input id="private_port" class="text" style="width: 90%;" type="text" />
            <div id="private_port_errormsg" class="errormsg" style="display: none;">
                Error msg will appear here</div>
        </div>
        <div class="grid_row_cell" style="width: 15%;">
            <div class="row_celltitles" id="protocol"></div>
        </div>
        <div class="grid_row_cell" style="width: 39%;">
            <select class="select" style="width: 104px;" id="vm">
            </select>
        </div>
        <div class="grid_row_cell" style="width: 15%;">
            <div class="row_celltitles">
                <a id="save_link" href="#">Save</a>
            </div>
            <div class="row_celltitles">
                <a id="cancel_link" href="#">Cancel</a>
            </div>
        </div>
        <div class="gridrow_loaderbox" style="display: none;" id="spinning_wheel">
            <div class="gridrow_loader">
            </div>
            <p id="description">
                Waiting &hellip;
            </p>
        </div>
    </div>
</div>
<!-- Port Forwarding template (end) -->

<!--  dialogs (begin) -->
<div id="dialog_confirmation_release_ip" title="Confirmation" style="display:none">
    <p>
        <%=t.t("please.confirm.you.want.to.release.this.IP.address")%>
    </p>
</div>

<div id="dialog_acquire_public_ip" title="Acquire New IP" style="display: none">
    <p> 
        <%=t.t("please.select.an.available.zone.to.associate.your.new.ip.with..acquiring.additional.ip.may.cost.you.an.additional.dollars.per.month.")%>
    </p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form1">
        <ol>
            <li>
                <label>
                    <%=t.t("zone")%>:</label>
                <select class="select" name="acquire_zone" id="acquire_zone">
                    <option value="default"><%=t.t("please.wait")%>....</option>
                </select>
            </li>
        </ol>
        </form>
    </div>
</div>
<!--  dialogs (end) -->
