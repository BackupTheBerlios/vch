<#include "header.ftl">
<#include "status_messages.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>

<form action="${ACTION}" method="post">
<table>
<tr>
  <td>${I18N_PARSE_THE_LAST}</td><td><input type="text" name="max_videos" value="${max_videos}" /><td>
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