<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=${ENCODING}" />
    <title>${I18N_GROUP}</title>
    <link rel="stylesheet" href="${HELP_ROOT}/styles.css" type="text/css" media="screen" />
    <link rel="shortcut icon" href="/files/icons/icon.ico" type="image/x-icon" />
    <script type="text/javascript" src="${DOCROOT}/jquery.js"></script>
    <style type="text/css">
        #channels_available {
            position: relative;
            float: left; 
            width: 40%;
        }
        
        #channels_member {
            position: relative;
            float: left;
            width: 40%;
        }
        
        #button_add {
            float: left;
            position: relative;
            top: 110px;
            margin-right: 10px;
        }
        
        #button_delete {
            float: left;
            position: relative;
            top: 110px;
            margin-left: 10px;
            margin-right: 10px;
        }
        
        #container {
            margin-top: 30px;
        }
    </style>
</head>
<body>
    ${NAVIGATION}
	<h1>${GROUP_NAME}</h1>
	
	<#include "status_messages.ftl">
	
	<form action="${ACTION}" method="get" id="change_desc">
		<input type="hidden" name="action" value="save_desc"/>
		<input type="hidden" name="group_name" value="${GROUP_NAME}"/>
        <label for="desc">${I18N_DESCRIPTION}</label>
		<input type="text" id="desc" name="desc" value="${DESC}" style="width: 50%"/>
		<input type="submit" value="${I18N_SAVE}"/>
	</form>
	
	<div id="container">
        <form id="channel_form" action="${ACTION}" method="get">
        <input type="hidden" name="group_name" value="${GROUP_NAME}"/>
        
        <fieldset id="channels_available"><legend>${I18N_ALL_CHANNEL}</legend>
            <div style="overflow: hidden">
                <select name="channels" size="10" style="width: 100%" multiple="multiple">
                    <#list AVAILCHANNELLIST as channel>
                    <option value="${channel.link}">${channel.title}</option> 
                    </#list>
                </select>
			</div>     
		</fieldset>
		
		<input id="button_delete" type="submit" name="submit_delete" value="<<"/>
        <input id="button_add" type="submit" name="submit_add" value=">>"/>
        
		<fieldset id="channels_member"><legend>${I18N_GROUP_MEMBER}</legend>
            <div style="overflow: hidden">
                <select name="members" size="10" style="width: 100%" multiple="multiple">
                    <#list CHANNELLIST as channel>
                    <option value="${channel.link}">${channel.title}</option> 
                    </#list>
                </select>
			</div>
		</fieldset>
		</form>
	</div>
<#include "footer.ftl">