<#include "header.ftl">
<#include "status_messages.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>

<form action="${ACTION}" method="post">
<table>
<tr>
  <td>
    ${I18N_ADD_NEW_FEED}
  </td><td>
    <input type="text" name="feed" style="min-width:600px; max-width:600px" />
  </td><td>
    <input type="submit" name="add_feed" value="${I18N_ADD}" />
  </td>
</tr>
<tr>
  <td valign="top">
    ${I18N_INSTALLED_FEEDS}
  </td><td>
    <select name="feeds" size="20" multiple="multiple" style="min-width:600px; max-width:600px">
        <#list FEEDS as feed>
        <option value="${feed.id}">${feed.title} - ${feed.uri}</option> 
        </#list>
    </select>
  </td><td valign="top">
    <input type="submit" name="remove_feeds" value="${I18N_DELETE_SELECTED}" />
  </td>
</tr>
</table>
</form>
<#include "footer.ftl">