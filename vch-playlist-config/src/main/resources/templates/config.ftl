<#include "header.ftl">
<#include "status_messages.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>

<form action="${ACTION}" method="post">
<table>
<tr><td>SVDRP</td></tr>
<tr>
  <td>${I18N_HOST}</td><td><input type="text" name="svdrp_host" value="${svdrp_host}" class="ui-widget ui-widget-content ui-corner-all"/><td>
</tr>
<tr>
  <td>${I18N_PORT}</td><td><input type="text" name="svdrp_port" value="${svdrp_port}" class="ui-widget ui-widget-content ui-corner-all"/><td>
</tr>
<tr>
  <td>&nbsp;</td>
  <td>
    <input type="submit" name="save_config" value="${I18N_SAVE}" class="ui-button" />
  </td>
</tr>
</table>
</form>
<#include "footer.ftl">