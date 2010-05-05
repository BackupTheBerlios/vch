<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" lang="en" xml:lang="en">


<head>
    <meta http-equiv="Content-Type" content="text/html; charset=${ENCODING}" />
    <title>${TITLE}</title>
    
    <!-- jquery core and ui -->
    <script type="text/javascript" src="/static/jquery-1.4.2.min.js"></script>
    <script type="text/javascript" src="/static/jquery-ui-1.8.custom.min.js"></script>
    
    <!-- jquery menu "droppy" -->
    <link rel="stylesheet" href="/static/droppy/droppy.css" type="text/css" />
    <script type="text/javascript" src="/static/droppy/jquery.droppy.js"></script>

    <!-- default theme -->
    <link rel="stylesheet" type="text/css" href="/static/themeroller/start/jquery-ui-1.8.custom.css" />
    <link rel="stylesheet" type="text/css" href="/static/vch.css" />
    
    <!-- themeroller theme switcher -->
    <script type="text/javascript" src="/static/themeroller/themeswitchertool.js"></script>
    
    <!-- favicon -->
    <link rel="shortcut icon" href="/static/icons/icon.ico" type="image/x-icon" />

    <!-- jquery notify plugin -->
    <link rel="stylesheet" href="/static/pnotify/jquery.pnotify.default.css" type="text/css" media="screen" />
    <script type="text/javascript" src="/static/pnotify/jquery.pnotify.min.js"></script>
    
    <!-- additional javascript and css incudes, which come from other bundles -->
    ${JAVASCRIPT}
    ${CSS}
    
    <script type="text/javascript">
        $(function() {
            $.pnotify.defaults.pnotify_width = "400px";
        });
        
        // enable button hover and click effect
        $(function() {
            $('.ui-button').button();
            $('.ui-button').removeClass('ui-widget');
        });
    </script>
</head>
<body>
<div class="ui-widget-content ui-corner-all ui-helper-clearfix" style="padding:10px">
<noscript>
<h1 class="nojs">${I18N_ENABLE_JAVASCRIPT}</h1>
</noscript>