<#include "header.ftl">
<#include "status_messages.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>

<form action="${ACTION}" method="post">
<table>
<tr>
  <td>${I18N_DATA_DIR}</td><td><input type="text" name="data_dir" value="${data_dir}" /><td>
</tr>
<tr>
  <td>${I18N_CONCURRENT_DOWNLOADS}</td><td><input type="text" name="concurrent_downloads" value="${concurrent_downloads}" /><td>
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