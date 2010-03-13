<#include "header.ftl">
<#include "status_messages.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>

<form action="${ACTION}" method="post">
<table>
<tr><td>osdserver</td></tr>
<tr>
  <td>${I18N_HOST}</td><td><input type="text" name="osdserver_host" value="${osdserver_host}" /><td>
</tr>
<tr>
  <td>${I18N_PORT}</td><td><input type="text" name="osdserver_port" value="${osdserver_port}" /><td>
</tr>
<tr>
  <td>${I18N_ENCODING}</td><td><input type="text" name="osdserver_encoding" value="${osdserver_encoding}" /><td>
</tr>
<tr><td><br/>SVDRP</td></tr>
<tr>
  <td>${I18N_HOST}</td><td><input type="text" name="svdrp_host" value="${svdrp_host}" /><td>
</tr>
<tr>
  <td>${I18N_PORT}</td><td><input type="text" name="svdrp_port" value="${svdrp_port}" /><td>
</tr>
<tr>
  <td>&nbsp;</td>
  <td>
    <input type="submit" name="save_config" value="${I18N_SAVE}" />
  </td>
</tr>
</table>
</form>
<#include "footer.ftl">