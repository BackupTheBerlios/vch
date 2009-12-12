<#if NOTIFY_MESSAGES??>
    <#assign delay="10000">
    <script type="text/javascript">
    <#list NOTIFY_MESSAGES as msg>
        <#if msg.type == "INFO">
            $.notify({text:'${msg.message}', title:'${I18N_INFO}', icon:'/notify/dialog-information.png', delay:${delay}});
        <#elseif msg.type == "WARNING">
            $.notify({text:'${msg.message}', title:'${I18N_WARNING}', icon:'/notify/dialog-warning.png', delay:${delay}});
        <#elseif msg.type == "ERROR">
            <#if msg.exception?? >
                $.notify({text:'${msg.message}<br/>${msg.stackTrace}', title:'${I18N_ERROR}', icon:'/notify/dialog-error.png', delay:${delay}});
            <#else>
                $.notify({text:'${msg.message}', title:'${I18N_ERROR}', icon:'/notify/dialog-error.png', delay:${delay}});
            </#if>            
        </#if>
    </#list>
    </script>
</#if>