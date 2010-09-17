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
   
    <h1>Dashboard
    </h1>
</div>

<!--Dashboard-->
<div class="contentbox" id="right_panel_content">
    <div class="info_detailbox errorbox" id="after_action_info_container" style="display:none">
        <p id="after_action_info"></p>
    </div>
    
    <div class="grid_container">
       <div class="grid_header">
       		<div class="grid_header_cell" style="width:60%; border:none;">
            	<div class="grid_header_title">System Wide Capacity</div>
            </div>
            <div class="grid_header_cell" style="width:40%; border:none;">
            	<div class="grid_header_formbox">
                    <select class="select" style="width:70px; "><option value="opt1">Op1</option> <option value="opt2">Op2</option></select>
                    <select class="select" style="width:70px;"><option value="opt1">All</option> <option value="opt2">Op2</option></select>
                    </div>
            </div>
       </div>
       <div class="dbrow even">
       		<div class="dbrow_cell" style="width:40%;">
            	<div class="dbgraph_titlebox">
                	<h2>Public IP Addresses</h2>
                    <div class="dbgraph_title_usedbox">
                    	<p>Used: <span> 2 / 11 </span></p>
                    </div>
                </div>
            </div>
            <div class="dbrow_cell" style="width:50%; border:none;"></div>
            <div class="dbrow_cell" style="width:9%; border:none;"></div>
       </div>
       <div class="dbrow odd"></div>
    </div>
</div>


