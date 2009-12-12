<#include "header.ftl">
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
<tr><td colspan="3"><hr style="width:100%" /></td></tr>
<tr>
  <td>
    ${I18N_QUALITY}
  </td><td>
    <select name="quality">
        <#if QUALITY == 34>
            <option value="34" selected="selected">Flash FLV SD (640x360)</option>
        <#else>
            <option value="34">Flash FLV SD (640x360)</option>
        </#if>
        <#if QUALITY == 35>
            <option value="35" selected="selected">Flash FLV HD (854x480)</option>
        <#else>
            <option value="35">Flash FLV HD (854x480)</option>
        </#if>
        <#if QUALITY == 18>
            <option value="18" selected="selected">MP4 SD (480x270)</option>
        <#else>
            <option value="18">MP4 SD (480x270)</option>
        </#if>
        <#if QUALITY == 22>
            <option value="22" selected="selected">MP4 HD (1280x720)</option>
        <#else>
            <option value="22">MP4 HD (1280x720)</option>
        </#if>
    </select>
  <td>
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