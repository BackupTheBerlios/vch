<#include "header.ftl">
<#include "navigation.ftl">

<h1>${I18N_CONFIGURATION}</h1>
<#include "status_messages.ftl">
<a href="/help/${LOCALE}/index.html">${I18N_HELP}</a><br/><br/>
<form action="${ACTION}" method="post" id="config_form">
    <table width="100%">
        <#list CONFIG_PARAMS.keys as key>
            <tr>
                <td width="30%">${key}</td>
                <#--<td><input style="width: 90%" type="text" name="param_${ram.key}" value="${param.value}"/></td>-->
            </tr>
        </#list>    
        <tr>
            <td></td>
            <td><input type="submit" value="${I18N_SAVE}"/></td>
        </tr>    
    </table>
    <input type="hidden" name="action" value="save"/>
</form>

<#include "footer.ftl">