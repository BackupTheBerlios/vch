<#include "header.ftl">

    ${NAVIGATION}
    <h1>${TITLE}</h1>
    
    <#include "status_messages.ftl">
    
    <form action="${ACTION}" method="post">
        <input type="text" name="channel" style="width: 50%" value="${I18N_NEW_FEED}"/>
        <input type="hidden" name="action" value="add_channel"/>
        <input type="submit" value="${I18N_ADD}"/>
    </form>
    
    <br/><br/>
    
    <form action="${ACTION}" method="post" id="feed_list">
        <select name="channel" size="30">
            <#list CHANNELS as channel>
                <option value="${channel.link}">${channel.title}</option>                
            </#list>
        </select>
        <br/><br/>
        <input type="submit" name="edit_channel" value="${I18N_EDIT}"/>
        <input type="submit" name="delete_channel" value="${I18N_DELETE}"/>
    </form>        
    
<#include "footer.ftl">