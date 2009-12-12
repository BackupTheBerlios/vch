<form id="installed_form" action="${ACTION}" method="get">
<div style="overflow: hidden">
    <select name="installed" size="18" style="width: 100%" multiple="multiple">
        <#list INSTALLED as bundle>
            <#if bundle.state == 1>
                <#assign cls="stopped">
                <#assign state=I18N_UNINSTALLED>
            <#elseif bundle.state == 2>
                <#assign cls="stopped">
                <#assign state=I18N_INSTALLED>
            <#elseif bundle.state == 4>
                <#assign cls="stopped">
                <#assign state=I18N_RESOLVED>
            <#elseif bundle.state == 8>
                <#assign cls="stopped">
                <#assign state=I18N_STARTING>
            <#elseif bundle.state == 16>
                <#assign cls="started">
                <#assign state=I18N_STOPPING>
            <#elseif bundle.state == 32>
                <#assign cls="started">
                <#assign state=I18N_ACTIVE>
            </#if>
            <option class="${cls}" value="${bundle.bundleId}" onclick="showDetails('${bundle.name}', '${bundle.author}', '${bundle.version}', '${state}', '${bundle.description}')">
                ${bundle.name} (${bundle.version})
            </option> 
        </#list>
    </select>
</div>
<br/>
<input id="button_start" type="submit" name="submit_start" value="${I18N_START}"/>
<input id="button_stop" type="submit" name="submit_stop" value="${I18N_STOP}"/>
<input id="button_uninstall" type="submit" name="submit_uninstall" value="${I18N_UNINSTALL}"/>
</form>