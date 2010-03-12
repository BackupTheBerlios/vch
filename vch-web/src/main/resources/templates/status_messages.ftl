<#if NOTIFY_MESSAGES??>
    <#assign delay="10000">
    <#assign errorDelay="15000">
    <script type="text/javascript">
    <#list NOTIFY_MESSAGES as msg>
        <#if msg.type == "INFO">
            $.notify({text:'${msg.message}', title:'${I18N_INFO}', icon:'/static/notify/dialog-information.png', delay:${delay}});
        <#elseif msg.type == "WARNING">
            $.notify({text:'${msg.message}', title:'${I18N_WARNING}', icon:'/static/notify/dialog-warning.png', delay:${delay}});
        <#elseif msg.type == "ERROR">
            <#if msg.exception?? >
                $.notify({text:'${msg.message}<br/>${msg.stackTrace}', title:'${I18N_ERROR}', icon:'/static/notify/dialog-error.png', delay:${errorDelay}});
            <#else>
                $.notify({text:'${msg.message}', title:'${I18N_ERROR}', icon:'/static/notify/dialog-error.png', delay:${errorDelay}});
            </#if>            
        </#if>
    </#list>
    </script>
</#if>