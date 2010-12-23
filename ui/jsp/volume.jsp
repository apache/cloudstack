<%@ page import="java.util.*" %>

<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>




<!-- volume detail panel (begin) -->
<div class="main_title" id="right_panel_header">
  
    <div class="main_titleicon">
        <img src="images/title_volumeicons.gif" /></div>
    
    <h1>
        Volume
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
        <div class="content_tabs off" id="tab_snapshot">
            <%=t.t("Snapshot")%></div>
    </div>
    <!--Details tab (start)-->
    <div id="tab_content_details">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">
	        <div class="grid_container">
	        	<div class="grid_header">
	            	<div class="grid_header_title">Title</div>
	                    <div class="grid_actionbox" id="action_link"><p>Actions</p>
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
	                        <%=t.t("Type")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="type">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Zone")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="zonename">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Instance.Name")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="vm_name">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Device.ID")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="device_id">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Size")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="size">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("State")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="state">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Created")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="created">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows even">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Storage")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="storage">
	                    </div>
	                </div>
	            </div>
	            <div class="grid_rows odd">
	                <div class="grid_row_cell" style="width: 20%;">
	                    <div class="row_celltitles">
	                        <%=t.t("Account")%>:</div>
	                </div>
	                <div class="grid_row_cell" style="width: 79%;">
	                    <div class="row_celltitles" id="account">
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
	        </div>
	    </div>    
    </div>
    <!--Details tab (end)-->
    <!--Snapshot tab (start)-->
    <div style="display: none;" id="tab_content_snapshot">
    	<div id="tab_spinning_wheel" class="rightpanel_mainloader_panel" style="display:none;">
              <div class="rightpanel_mainloaderbox">
                   <div class="rightpanel_mainloader_animatedicon"></div>
                   <p>Loading &hellip;</p>    
              </div>               
        </div>
        <div id="tab_container">
        </div>
    </div> 
    <!--Snapshot tab (end)-->
</div>

<!--  top buttons (begin) -->
<div id="top_buttons">
    <div class="actionpanel_button_wrapper" id="add_volume_button">
        <div class="actionpanel_button">
            <div class="actionpanel_button_icons">
                <img src="images/addvm_actionicon.png" alt="Add Volume" /></div>
            <div class="actionpanel_button_links">
                Add Volume
            </div>
        </div>
    </div>
</div>
<!--  top buttons (end) -->

<!-- Create Template Dialog -->
<div id="dialog_create_template" title="Create Template" style="display: none">
    <p>
        Please specify the following information before creating a template of your disk
        volume: <b><span id="volume_name"></span></b>. Creating a template could take up
        to several hours depending on the size of your disk volume.</p>
    <div class="dialog_formcontent">
        <form action="#" method="post" id="form_acquire">
        <ol>
            <li>
                <label for="user_name">
                    Name:</label>
                <input class="text" type="text" name="create_template_name" id="create_template_name" />
                <div id="create_template_name_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
            <li>
                <label for="user_name">
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
                <label for="user_name">
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

<!-- Download Volume Dialog (begin) -->
<div id="dialog_download_volume" title="Download Volume" style="display: none">    
  <!--Loading box-->
  <div id="spinning_wheel" class="ui_dialog_loaderbox">
      <div class="ui_dialog_loader">
      </div>
      <p>
          Generating URL....</p>
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
<!-- Download Volume Dialog (end) -->

<!-- Create Snapshot Dialog (begin) -->
<div id="dialog_create_snapshot" title="Create Snapshot" style="display: none">
    <p>
        Please confirm you want to create snapshot for this volume.</p>
</div>
<!-- Create Snapshot Dialog (end) -->

<!-- snapshot confirmation dialog (begin) -->
<div id="dialog_confirmation_delete_snapshot" title="Confirmation" style="display:none">
    <p>Please confirm you want to delete the snapshot.</p>   
</div>
<!-- snapshot confirmation dialog (end) -->

<!-- Recurring Snapshots Dialog (begin) -->
<div id="dialog_recurring_snapshot" title="Recurring Snapshot" style="display:none;">
    <div class="dialog_snapshotcontainer">
        <div class="dialog_snapshotleft" id="dialog_snapshotleft" >
			<p>Your snapshot schedule is currently set to:<br/><br/></p>
        </div>
        <div class="dialog_snapshotright" id="dialog_snapshotright">
            <div class="dialog_snapshots_editcontent" style="display:block;">
                <div class="dialog_snapshots_editcontent_title">
					<div class="dialog_snapshotleft_label" style="width:120px">&nbsp;Snapshot Schedule: &nbsp;&nbsp;</div>
                    <select class="snapselect" id="snapshot_interval">
						<option value="-1">Disabled</option>
						<option value="0">Hourly</option>
						<option value="1">Daily</option>
						<option value="2">Weekly</option>
						<option value="3">Monthly</option>
					</select>
                </div>
                <div class="dialog_formcontent" id="snapshot_form">
                    <form action="#" method="post" id="form4">
                    <ol>
                        <li>
                            <label for="add_volume_name" style="width:75px">
                                Time:</label>
                            <span id="edit_hardcoding_hour" style="display:none">00</span>
                            <span id="edit_hour_container">
                            	
                                <select class="snapselect" id="edit_hour">
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
									<option value="00">12</option>
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
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
<!-- Take Snapshots Dialog (end) -->

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
            <li id="size_container">
                <label>
                    Disk Size (in GB):</label>
                <input class="text" type="text" id="size" />
                <div id="size_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
                </div>
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Add Volume Dialog (end) -->

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
                </select>
                <div id="volume_vm_errormsg" class="dialog_formcontent_errormsg" style="display: none;">
            </li>
        </ol>
        </form>
    </div>
</div>
<!-- Attach Volume Dialog (end) -->

<!--  Snapshot tab template (begin) -->
<div class="grid_container" id="snapshot_tab_template" style="display: none">
    <div class="grid_header">
        <div class="grid_header_title" id="grid_header_title">
        </div>
        <div class="grid_actionbox" id="snapshot_action_link"><p>Actions</p>
            <div class="grid_actionsdropdown_box" id="snapshot_action_menu" style="display: none;">
                <ul class="actionsdropdown_boxlist" id="action_list">
                </ul>
            </div>
        </div>
        <div class="gridheader_loaderbox" id="spinning_wheel" style="display: none; height: 18px;">
            <div class="gridheader_loader" id="icon">
            </div>
            <p id="description">
                Waiting &hellip;
            </p>
        </div>       
    </div>
    
    <div class="grid_rows" id="after_action_info_container" style="display:none">
        <div class="grid_row_cell" style="width: 90%; border: none;">
            <div class="row_celltitles">
                <strong id="after_action_info">Message will appear here</strong></div>
        </div>
    </div>
        
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                ID:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="id">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Snapshot Name:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="name">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Volume Name:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="volumename">
            </div>
        </div>
    </div>
    <div class="grid_rows odd">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Interval Type:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="intervaltype">
            </div>
        </div>
    </div>
    <div class="grid_rows even">
        <div class="grid_row_cell" style="width: 20%;">
            <div class="row_celltitles">
                Created:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="created">
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
                Domain:</div>
        </div>
        <div class="grid_row_cell" style="width: 79%;">
            <div class="row_celltitles" id="domain">
            </div>
        </div>
    </div>    
</div>
<!--  Snapshot tab template (end) -->

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
	                <label for="ispublic">
	                    Public:</label>
	                <select class="select" name="ispublic" id="ispublic">
	                    <option value="false">No</option>
	                    <option value="true">Yes</option>
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

<div id="hidden_container">
    <!-- advanced search popup (begin) -->
    <div class="adv_searchpopup_bg" id="advanced_search_popup" style="display: none;">
        <div class="adv_searchformbox">
            <form action="#" method="post">
            <ol>
                <li>
                    <input class="text textwatermark" type="text" name="adv_search_name" id="adv_search_name"
                        value="by name" />
                </li>
                <li>
                    <select class="select" id="adv_search_zone">
                    </select>
                </li>
                <li id="adv_search_domain_li" style="display: none;">
                    <select class="select" id="adv_search_domain">
                    </select>
                </li>
                <li id="adv_search_account_li" style="display: none;">
                    <input class="text textwatermark" type="text" id="adv_search_account" value="textwatermark" />
                </li>
            </ol>
            </form>
        </div>
    </div>
    <!-- advanced search popup (end) -->
</div>
