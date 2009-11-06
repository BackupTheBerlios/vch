<#include "header.ftl">

${NAVIGATION}
<h1>${TITLE}</h1>
<table border="1">
    <tr>
        <th>${I18N_TH_URL}</th>
        <th>${I18N_TH_DESC}</th>
    </tr>
    <#list HANDLER_LIST as handler>
    <tr>
        <td><a href="${handler.url}">${handler.url}</a></td>
        <td>${handler.description}</td>
    </tr>
    </#list>            
</table>
    
<#include "footer.ftl">