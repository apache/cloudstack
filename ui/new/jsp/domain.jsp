<%@ page import="java.util.*" %>
<%@ page import="com.cloud.utils.*" %>

<%
    Locale browserLocale = request.getLocale();
    CloudResourceBundle t = CloudResourceBundle.getBundle("resources/resource", browserLocale);
%>

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