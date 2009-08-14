<#include "header.ftl">

${NAVIGATION}
<h1>${TITLE}</h1>
<#include "status_messages.ftl">
<a href="${LINK_HELP}">${I18N_HELP}</a><br/><br/>
<form action="${ACTION}" method="post" id="config_form">
    <table width="100%">
        <#list CONFIG_PARAMS as param>
            <tr>
                <td width="30%">${param.key}</td>
                <td><input style="width: 90%" type="text" name="${param.key}" value="${param.value}"/></td>
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