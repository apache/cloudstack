<% long now = System.currentTimeMillis(); %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta http-equiv='cache-control' content='no-cache'>
    <meta http-equiv='expires' content='0'>
    <meta http-equiv='pragma' content='no-cache'>
	<link rel="stylesheet" href="css/main.css" type="text/css" />

	<!-- Common libraries -->
	<script type="text/javascript" src="scripts/json2.js"></script>
    <script type="text/javascript" src="../scripts/jquery.min.js"></script>
    <script type="text/javascript" src="../scripts/jquery-ui.custom.min.js"></script>
    <script type="text/javascript" src="../scripts/date.js"></script>
    <script type="text/javascript" src="../scripts/jquery.cookies.js"></script>
    <script type="text/javascript" src="../scripts/jquery.timers.js"></script>
    <script type="text/javascript" src="../scripts/jquery.md5.js"></script>

    <!-- cloud.com scripts -->
	<script type="text/javascript" src="scripts/cloudkit.js?t=<%=now%>"></script>
	<script type="text/javascript" src="scripts/cloudkit.hosts.js?t=<%=now%>"></script>
	
	<!-- Favicon -->
	<link rel="shortcut icon" href="favicon.ico" type="image/x-icon" />

    <title>myCloud</title>
</head>
<body>
	<!-- Host template -->
	<div class="db_gridrows" id="host_template" style="display:none">
		<div class="db_gridcolumns" style="width:20%;">
			<div class="db_gridcelltitles" id="hostname"></div>
		</div>
		<div class="db_gridcolumns" style="width:15%;">
		  <div class="db_gridcelltitles green" id="state"></div>
	    </div>
		<div class="db_gridcolumns" style="width:15%;">
		  <div class="db_gridcelltitles" id="ip"></div>
	    </div>
		<div class="db_gridcolumns" style="width:20%;">
		  <div class="db_gridcelltitles" id="version"></div>
	    </div>
		<div class="db_gridcolumns" style="width:20%;">
		  <div class="db_gridcelltitles" id="disconnected"></div>
	    </div>
		<div class="db_gridcolumns" style="width:10%;">
			<a class="db_statistics_icon" href="#" id="host_details"></a>
			<a class="db_delete_icon" style="margin-left:25px" href="#" id="host_delete"></a>
		</div>
	</div>
	<!-- End Host template -->

	<div id="main" style="display:none">
    	<div id="dialog_overlay" style="display:none;">
            <div class="overlay_black"></div>
            
			<!-- Statitics overlay starts here-->
            <div class="overlay_dialogbox" id="dialog_host_details" style="display:none;">
                    <div class="overlay_dialogbox_contentarea">
                        <div class="overlay_dialogbox_titlearea"><h2>Host Details</h2></div>
                        <div class="overlay_dialogbox_content">
                            <div class="db_stats_gridbox">
								<div class="db_stats_gridrow ">
                                    <div class="db_stats_gridcolumns" style="width:48%;">
                                        <div class="db_stats_gridcelltitles">Host GUID: </div>
                                    </div>
                                    <div class="db_stats_gridcolumns" style="width:50%;">
                                        <div class="db_stats_gridcelltitles"><strong id="host_id"></strong></div>
                                    </div>
                                </div>
								<div class="db_stats_gridrow ">
                                    <div class="db_stats_gridcolumns" style="width:48%;">
                                        <div class="db_stats_gridcelltitles">Added Date: </div>
                                    </div>
                                    <div class="db_stats_gridcolumns" style="width:50%;">
                                        <div class="db_stats_gridcelltitles"><strong id="host_added"></strong></div>
                                    </div>
                                </div>
                                <div class="db_stats_gridrow ">
                                    <div class="db_stats_gridcolumns" style="width:48%;">
                                        <div class="db_stats_gridcelltitles">CPU Total: </div>
                                    </div>
                                    <div class="db_stats_gridcolumns" style="width:50%;">
                                        <div class="db_stats_gridcelltitles"><strong id="host_cpu_total">4 x 2.40 GHZ</strong></div>
                                    </div>
                                </div>
								
								<div class="db_stats_gridrow ">
                                    <div class="db_stats_gridcolumns" style="width:48%;">
                                        <div class="db_stats_gridcelltitles">CPU Allocated:</div>
                                    </div>
                                    <div class="db_stats_gridcolumns" style="width:50%;">
                                        <div class="db_stats_gridcelltitles"><strong id="host_cpu_allocated">20.83%</strong></div>
                                    </div>
                                </div>
                                
                                <div class="db_stats_gridrow ">
                                    <div class="db_stats_gridcolumns" style="width:48%;">
                                        <div class="db_stats_gridcelltitles">CPU Used: </div>
                                    </div>
                                    <div class="db_stats_gridcolumns" style="width:50%;">
                                        <div class="db_stats_gridcelltitles"><strong id="host_cpu_used">0.04%</strong></div>
                                    </div>
                                </div>
                                
                                <div class="db_stats_gridrow ">
                                    <div class="db_stats_gridcolumns" style="width:48%;">
                                        <div class="db_stats_gridcelltitles">Memory Total:</div>
                                    </div>
                                    <div class="db_stats_gridcolumns" style="width:50%;">
                                        <div class="db_stats_gridcelltitles"><strong id="host_mem_total">3.09 GB</strong></div>
                                    </div>
                                </div>
                                
                                <div class="db_stats_gridrow ">
                                    <div class="db_stats_gridcolumns" style="width:48%;">
                                        <div class="db_stats_gridcelltitles">Memory Allocated:</div>
                                    </div>
                                    <div class="db_stats_gridcolumns" style="width:50%;">
                                        <div class="db_stats_gridcelltitles"><strong id="host_mem_allocated">2.63 GB</strong></div>
                                    </div>
                                </div>
                                
                                <div class="db_stats_gridrow ">
                                    <div class="db_stats_gridcolumns" style="width:48%;">
                                        <div class="db_stats_gridcelltitles">Memory Used:</div>
                                    </div>
                                    <div class="db_stats_gridcolumns" style="width:50%;">
                                        <div class="db_stats_gridcelltitles"><strong id="host_mem_used">2.63 GB</strong></div>
                                    </div>
                                </div>
                                
                                <div class="db_stats_gridrow ">
                                    <div class="db_stats_gridcolumns" style="width:48%;">
                                        <div class="db_stats_gridcelltitles">Network Read:</div>
                                    </div>
                                    <div class="db_stats_gridcolumns" style="width:50%;">
                                        <div class="db_stats_gridcelltitles"><strong id="host_net_read">4338950879.03 TB</strong></div>
                                    </div>
                                </div>
                                
                                <div class="db_stats_gridrow ">
                                    <div class="db_stats_gridcolumns" style="width:48%;">
                                        <div class="db_stats_gridcelltitles">Network Write:</div>
                                    </div>
                                    <div class="db_stats_gridcolumns" style="width:50%;">
                                        <div class="db_stats_gridcelltitles"><strong id="host_net_sent">4352955092.25 TB</strong></div>
                                    </div>
                                </div>
                                
                                
                                
                            </div>
                        </div>
                        
                        <div class="overlay_dialogbox_confirmationbox">
                            <div class="overlay_dialogbox_confirmationbuttonbox">
                                <div class="overlay_dialog_button" id="dialog_ok">OK</div>
                            </div>
                        </div>
                    </div>
            </div>
            <!-- Statitics overlay ends here-->
           
            <!-- Delete overlay starts here-->
            <div class="overlay_dialogbox" style="display:none;" id="dialog_delete_host">
				<div class="overlay_dialogbox_contentarea">
					<div class="overlay_dialogbox_titlearea"><h2>Confirmation</h2></div>
					<div class="overlay_dialogbox_content">
						<p>Please confirm that you want to remove the host, <strong id="hostname"></strong>, from your cloud.</p>
					</div>
					
					<div class="overlay_dialogbox_confirmationbox">
						<div class="overlay_dialogbox_confirmationbuttonbox">
							<a href="#" id="dialog_cancel">Cancel</a>
							<div class="overlay_dialog_button" id="dialog_confirm">Confirm</div>
						</div>
					</div>
				</div>
            </div>
            <!-- Delete overlay ends here-->
        </div>
    	<div id="header">
        	<div class="logo"></div>
			<div class="user_links"><p><span id="header_username"></span></p><p><a href="#" id="header_logout">Logout</a></p></div>
        </div>
        <div class="main_contentbg">
            <div class="db_gridcontainer">
            	<div class="db_gridcontainer_topbox">
                	<div class="db_gridcontainer_topbox_left">
                    	<h2>myCloud</h2>
                        <div id="search_panel" class="db_grid_searchbox">
                        	<div class="db_grid_searchicon"></div>
                            <input id="search_input" class="text" type="text" />                            
                            <a id="clear_search_button" class="db_grid_search_closeicon" href="#" style="display:none;"></a>                            
                        </div>                       
                        <div id="refresh_button" class="db_gridcontainer_refreshbox"><a href="#"> Refresh </a></div>                        
                    </div>
                    <div class="db_gridcontainer_topbox_right">
                    	<div class="db_grid_tabbox">
							<div class="db_grid_tabs on" id="tab_hosts">Hosts</div>
                        </div>
                        <div class="db_grid_tabbox">
                        	<div class="db_grid_tabs off" id="tab_docs">Getting Started</div>
                        </div>
                    </div>
                </div>
                <div class="db_tabcontent" id="tab_hosts_content" style="display:block;">
                    <div class="db_gridbox">
                        <div class="db_gridrows header">
                            <div class="db_gridcolumns header" style="width:20%;">
                                <div class="db_gridcelltitles header">Name</div>
                            </div>
                            <div class="db_gridcolumns header" style="width:15%;">
                                <div class="db_gridcelltitles header">State</div>
                            </div>
                            <div class="db_gridcolumns header" style="width:15%;">
                                <div class="db_gridcelltitles header">IP Address</div>
                            </div>
                            <div class="db_gridcolumns header" style="width:20%;">
                                <div class="db_gridcelltitles header">Version</div>
                            </div>
                            <div class="db_gridcolumns header" style="width:20%;">
                                <div class="db_gridcelltitles header">Last Disconnected</div>
                            </div>
                            <div class="db_gridcolumns header" style="width:10%;">
                                <div class="db_gridcelltitles header">Actions</div>
                            </div>
                        </div>
                
                        <div class="db_maingrid">
							<!--Reminder for completing registrtaion starts here-->
							<div class="db_gridmsgbox" style="display:none;" id="registration_complete_container">
								<div class="db_gridmsgbox_content">
									<p>
										You have successfully added your first compute node.  Please complete your registration process by clicking here: <a id="registration_complete_link" href='#'>Complete Registration</a>
									</p>
								</div>
							</div>
							<!--Reminder for completing registrtaion ends here-->
							<div id="host_container">
							</div>							
						</div>
						
						<div class="db_grid_navigationpanel">
						    <div class="db_gridb_paginationbox"><p>Page <span id="page_number">N</span></p></div>
							<div class="db_gridb_navbox">
							    <a id="prev_page_button" href="#">Prev</a> 
							    <a id="next_page_button" href="#">Next</a> 
							</div>
						</div>
						
					</div>
				</div>
              
				<div class="db_tabcontent" id="tab_docs_content" style="display:none;">
                    <div class="db_gridbox">
                        <div class="db_gridrows header">
                            <div class="db_gridcolumns header" style="width:70%;">
                                <div class="db_gridcelltitles header"></div>
                            </div>
						</div>
						<div class="db_maingrid">
							<div class="dbinstruction_contentarea">
								<div class="dbinstruction_submenubox">
									<div class="dbinstruction_submenubox_content">
										<ul>
											<li><a href="#gettingstarted">Getting Started</a></li>
											<li><a href="#managehost">Managing Your Hosts</a></li>
										</ul>
									</div>
								</div>
                            
								<div id="getting_started"><a name="gettingstarted"></a>
									<h3>Getting Started</h3>
									<div class="db_downlaodbox">
										<p>Your zone token is: <strong id="zone_token"></strong></p>
										<a class="db_instructiondownlaodbutton" href="#">Download Installer</a>
									</div>
									
									<h4>Contents</h4>
									<p>Cloud.com's <a href="http://cloud.com/products/cloud-computing-software">CloudStack</a> agent + RightScale's <a href="http://support.rightscale.com/06-FAQs/FAQ_0178_-_What_is_inside_of_a_RightImage%3F">RightImage</a> + README</p>
									
									<h4>System requirements</h4>
									<div class="dbinstruction_bulletbox">
										<ul>
											<li>One or more computer hosts capable of running the software. See <a href="http://linuxhcl.com/">Linux Hardware Compatibility List</a>.</li>
											<li>Ubuntu 10.04 LTS installed on each machine. Available at <a href="http://releases.ubuntu.com/">Ubuntu Releases</a>.</li>											
										</ul>
									</div>
									
									<h4>Adding a New Host</h4>
									<p>To add a new host, install the CloudStack agent software on it.</p>
									<div class="dbinstruction_bulletbox">
										<ol>
											<li>Click the Download button to get the CloudKit installation bundle.</li>
											<li>Log in as root to the host machine where you want to install the software.
												The machine must have <a href="http://releases.ubuntu.com/">Ubuntu 10.04 LTS</a> installed.                                    
											</li>
											<li>Place the CloudKit installation bundle on the host and untar.
												<div class="dbinstruction_bulletbox_codebox"># tar xzf &lt;Installer file name&gt;.tar.gz</div>
												</li>
											<li>Run the installer.
												<div class="dbinstruction_bulletbox_codebox"># cd &lt;Directory of extracted installer&gt;</div>
												<div class="dbinstruction_bulletbox_codebox"># sudo ./install.sh</div>
											</li>
											<li>At the prompt, type A to install the agent. Installation can take a few minutes.
											    When installation is finished, a message like "Agent installation is completed" appears.</li>
											<li>After the agent installation finishes, run the setup script.</li>
												<div class="dbinstruction_bulletbox_codebox"># sudo mycloud-setup-agent</div>											
											<li>For Management Server address, use the default if it is not localhost, or input the IP address provided to you by
											    Cloud.com. Currently the only Management Server IP being provided is 192.168.130.223.</li>
											<li>When prompted, paste in the zone token for your cloud. You can find it at the top of this page.
												The zone token is a unique code that identifies your cloud. You will need it throughout the lifetime of your cloud.
											</li>
											<li>Provide the name of the network interface you want to use, or (typically) accept the default.
											</li>                                    
											<li>When the "Installation complete!" message appears, confirm that the new host appears in the Hosts tab of the <a href="http://216.38.159.3:8080/client/cloudkit/login.jsp ">CloudKit administration UI</a>.
												You can also run this command on the host to be sure the cloud-agent software is running:
												<div class="dbinstruction_bulletbox_codebox"># sudo service cloud-agent status</div>
											 </li>                                 	
											 <li>Click <a id="registration_complete_doc_link" href='#'>Complete Registration</a>.
												The new host is automatically registered with RightScale as part of your CloudKit cloud.</p>
											</li>
										</ol>
									</div>
									<p><strong>You're ready to start using your new host!</strong></p>	
									<h4>More Information</h4>
									 <p style="margin-top:5px;"><a href="#">RightScale's CoudKit docs (title and URL: TBD)</a></p>
								</div>
                            
								<div id="managing_host">
									
									<h3>Managing Your Hosts</h3>
									<a name="managehost"></a>
									<p>Use the Hosts tab of the CloudKit administration UI to view host status or delete hosts.</p>
									<p>(Want to do more than view or delete? To add a host, click the Installer tab. For other cloud management tasks, <a href="#">RightScale UI</a>.)</p>


									<div class="dbinstruction_bulletbox">
									   <ul>
											<li><strong>Host Name</strong>: IP address or fully-qualified DNS name of the host</li>
											<li><strong>State:</strong></li>
											<li style="margin-left:15px; display:inline;">Connecting: Request to connect to the cloud has been received and the host is attempting to comply</li>
											<li style="margin-left:15px; display:inline;">Up: Host is connected and operational in the cloud</li>
											<li style="margin-left:15px; display:inline;">Down: Host is connected but not operational</li>
											<li style="margin-left:15px; display:inline;">Disconnected: Host is not connected to the cloud</li>
											<li style="margin-left:15px; display:inline;">Updating: State of the host is in transition</li>
											<li style="margin-left:15px; display:inline;">PrepareForMaintenance: Request to put the host into maintenance mode has been received</li>
											<li style="margin-left:15px; display:inline;">ErrorInMaintenance: Action taken during maintenance mode has failed</li>
											<li style="margin-left:15px; display:inline;">CancelMaintenance: Request to abandon maintenance activity and restore host to previous condition has been received</li>
											<li style="margin-left:15px; display:inline;">Maintenance: Host is in maintenance mode and is not available for new VM instances</li>
											<li style="margin-left:15px; display:inline;">Alert: Error condition exists on the host and requires attention</li>
											<li style="margin-left:15px; display:inline;">Removed: Request to delete the host has been received</li>
											<li><strong>Version</strong>: Version of CloudKit software installed on the host</li>
											<li><strong>Last Disconnected</strong>: Timestamp recording any prior disconnection of the host from the cloud</li>
											<li><strong>Action</strong></li>
											<li style="margin-left:15px; display:inline;"><a class="db_statistics_icon" style="margin:-5px 5px 0 0;"></a> Statistic Icon: Click to display resource usage statistics for troubleshooting and tuning</li>
											<li style="margin-left:15px; display:inline;"><a class="db_delete_icon" style="margin:-5px 5px 0 0;"></a>Delete Icon: Click to delete host; a confirmation dialog will be displayed</li>
										</ul>
									</div>
									
									<h4>Adding a New Host</h4>
									<p style="margin-top:5px;">To add a new host, install the CloudStack agent software on it. Click the Getting Started tab and follow the <a href="#gettingstarted">installation steps</a>.</p> 
									
									<h4>More Information</h4>
									<p style="margin-top:5px;"><a href="#">RightScale's CoudKit docs (title and URL: TBD)</a></p>
								</div>
							</div>   
                        </div>
					</div>   
				</div>
            </div>
        </div>
        <div id="footer">
        	<div class="footer_left"><p>&copy; 2006-2011 RightScale, Inc. All rights reserved. RightScale is a registered trademark of RightScale, Inc. </p></div>
            <div class="footer_right">
            	<a class="poweredby" href="http://cloud.com/"></a>
            </div>
        </div>
    </div>
</body>
</html>
