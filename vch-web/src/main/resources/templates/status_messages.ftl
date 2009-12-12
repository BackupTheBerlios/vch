<#if NOTIFY_MESSAGES??>
    <script type="text/javascript">
    <#list NOTIFY_MESSAGES as msg>
        <#if msg.type == "INFO">
            $.notify({text:'${msg.message}', title:'${I18N_INFO}', icon:'/notify/dialog-information.png'});
        <#elseif msg.type == "WARNING">
            $.notify({text:'${msg.message}', title:'${I18N_WARNING}', icon:'/notify/dialog-warning.png'});
        <#elseif msg.type == "ERROR">
            <#if msg.exception?? >
                $.notify({text:'${msg.message}<br/>${msg.stackTrace}', title:'${I18N_ERROR}', icon:'/notify/dialog-error.png'});
            <#else>
                $.notify({text:'${msg.message}', title:'${I18N_ERROR}', icon:'/notify/dialog-error.png'});
            </#if>            
        </#if>
    </#list>
    </script>
</#if>