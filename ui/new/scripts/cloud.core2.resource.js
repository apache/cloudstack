function afterLoadResourceJSP() {
    //***** switch between different tabs (begin) ********************************************************************
    var tabArray = ["tab_details", "tab_network", "tab_secondary_storage"];
    var tabContentArray = ["tab_content_details", "tab_content_network", "tab_content_secondary_storage"];
    switchBetweenDifferentTabs(tabArray, tabContentArray);       
    //***** switch between different tabs (end) **********************************************************************
        
}