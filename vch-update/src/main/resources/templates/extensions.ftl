<#include "header.ftl">
<#include "navigation.ftl">

<h1>${I18N_EXTENSIONS}</h1>
<#include "status_messages.ftl">
<style type="text/css">
    #bundles_installed {
        position: relative;
        float: left; 
        width: 40%;
    }
    
    #bundles_available {
        position: relative;
        float: left;
        width: 40%;
    }
    
    #button_uninstall {
        float: left;
        position: relative;
        top: 110px;
        margin-right: 10px;
    }
    
    #button_install {
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
    
<div id="container">
    <form id="bundle_form" action="${ACTION}" method="get">
    
    <fieldset id="bundles_installed"><legend>${I18N_BUNDLES_INSTALLED}</legend>
        <div style="overflow: hidden">
            <select name="installed" size="20" style="width: 100%" multiple="multiple">
                <#list INSTALLED as bundle>
                <option value="${bundle.bundleId}">${bundle.name} (${bundle.version})</option> 
                </#list>
            </select>
        </div>
        <#--
        <br/>
        <input id="button_start" type="submit" name="submit_start" value="Start"/>
        <input id="button_stop" type="submit" name="submit_stop" value="Stop"/>
        -->
    </fieldset>
    
    <input id="button_install" type="submit" name="submit_install" value="<<"/>
    <input id="button_uninstall" type="submit" name="submit_uninstall" value=">>"/>
    
    <fieldset id="bundles_available"><legend>${I18N_BUNDLES_AVAILABLE}</legend>
        <div style="overflow: hidden">
            <select name="available" size="20" style="width: 100%" multiple="multiple">
                <#list AVAILABLE as resource>
                <option value="${resource.symbolicName}">${resource.presentationName} (${resource.version})</option> 
                </#list>
            </select>
        </div>
    </fieldset>
    </form>
</div>

<#include "footer.ftl">