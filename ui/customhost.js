//
// Use https://realfavicongenerator.net to generate custom host based resources
//
const ippSLDs = ['ippathways', 'adaptivecloud'];
const ippSubDomainPrefixes = ["www", "cloud", "labcloud"]; // Make sure this has all possible IPP subdomain prefixes!
const defaultbrand = 'adaptivecloud';

// See if a given string starts with any of the strings in the array
function arrayHasPrefixFor(prefixArray, needle) {
    for (var idx in prefixArray) {
        if (needle.startsWith(prefixArray[idx])) {
            return true;
        }
    }
    return false;
};

var hostparts = window.location.hostname.split('.');

// Grab the Second-level Domain. If this isn't an IPP SLD, then it is a white-label name that we want to use
var sld = hostparts[hostparts.length - 2];

// Default to the IPP AdaptiveCloud branding
var custombrand = defaultbrand;

// Grab the sub-domain, just in case the SLD is an IP Pathways SLD, we need this to see if this is a white-label
var subdomain = (hostparts.length > 2 ? hostparts[hostparts.length - 3] : '');

// If this part is not one of the IP Pathways SLDs, use it as a white-label brand
if (!ippSLDs.includes(sld)) {
    custombrand = sld;
}
// This is an IPP SLD, see if the sub-domain is one reserved for IPP, if not, use it as a white-label brand
else if (subdomain.length > 0 && !arrayHasPrefixFor(ippSubDomainPrefixes, subdomain)) {
    custombrand = subdomain;
}

function getHostPath(host) {
    return `hosts/${host}`;
}

function applyUpdates(updates) {
    var item;
    for (item of updates.removals) {
        $(item).remove();
    }
    for (item of updates.additions) {
        $(item).appendTo('head');
    }
}

function fetchUpdates(brandpath) {
    var url = `${path}/updates.json`
    $.get(url)
        .done(function(updates) {
            applyUpdates(updates);
        })
    .fail(function() {
    });
}

function customizeBrand(brandpath) {
    // Pull in the custom css
    $('<link rel="stylesheet" type="text/css" rel="stylesheet" href="'+brandpath+'/custom.css" >').appendTo('head');

    fetchUpdates(brandpath);
}

function finishSetup() {
    // Set the domain if we're on the login form (element exists)
    var domainInput = $('div.login.nologo input[name="domain"]')
    if (domainInput.length > 0 && custombrand && (custombrand != defaultbrand)) {
        domainInput.val(custombrand)
    }

    // Set the title
    // This doesn't work, as cloud stack resets the title after this event fires!!! ugh
    $('title').text('AdaptiveCloud')
    //$('title').text(custombrand)
}

$(window).bind('cloudStack.cloudStack.init', function() {
    finishSetup()
});

// Verify that there is a custom css file, if so, then try to inject the resources for it
var path = getHostPath(custombrand);
var url = `${path}/custom.css`
$.get(url)
    .done(function() {
        customizeBrand(path);
    })
    .fail(function() {
        if (custombrand != defaultbrand) {
            // Fall back to the default brand
            custombrand = defaultbrand
            path = getHostPath(custombrand);
            url = `${path}/custom.css`
            $.get(url)
            .done(function() {
                customizeBrand(path);
            });
        }
    });
