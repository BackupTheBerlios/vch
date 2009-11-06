<#include "header.ftl">

${NAVIGATION}
<h1>${TITLE}</h1>
<#include "status_messages.ftl">
<form action="${MEMBER_CONFIG_PAGE}" method="get">
	<select name="group_name">
	    <#list GROUPLIST as group>
        <option>${group.name}</option>
	    </#list>
    </select>
	<input type="hidden" name="action" value="edit"/>
	<input type="submit" value="${I18N_EDIT}"/>
</form>
<br/>
<form name="delete" action="${ACTION}" method="get">
	<select name="group_name">
        <#list GROUPLIST as group>
        <option>${group.name}</option>
        </#list>
    </select>
	<input type="hidden" name="action" value="delete"/>
	<input type="hidden" name="delete_entries" value="false"/>
	<input id="delete_button" type="submit" value="${I18N_DELETE}"/>
</form>
<br/>
<form action="${ACTION}" method="get">
	<input name="group_name" type="text" size="30" maxlength="30"/>
	<input type="hidden" name="action" value="addgroup"/>
	<input type="submit" value="${I18N_ADD}"/>
</form>

<script type="text/javascript"><!--
<#if AJAX_ENABLED>
$('form[name=delete]').submit(
    function() {
        var group_name = document.delete.group_name.value;
        var groupCount = 0;
        $.ajax({
            url: '${ACTION}',
            async: false,
            type: 'GET',
            data: {action:'group_count', group_name:group_name},
            processData: true,
            dataType: 'text',
            timeout: 30000,
            error: function(){
                alert("Error");
            },
            success: function(count){
                groupCount = count;
            }
        });
        
        if(groupCount > 0) {
            var ok = confirm('${I18N_GROUP_NOT_EMPTY}');
            document.delete.delete_entries.value = ok;
            return ok;
        } else {
            return true;
        }
    }
);
</#if>

--></script>

<#include "footer.ftl">