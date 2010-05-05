<#if NOTIFY_MESSAGES??>
    <script type="text/javascript">
    $.pnotify.defaults.pnotify_width = "400px";
    <#list NOTIFY_MESSAGES as msg>
        <#if msg.type == "INFO">
            $.pnotify( {
                pnotify_title : '${I18N_INFO}',
                pnotify_text : '${msg.message}'
            });
        <#elseif msg.type == "WARNING">
            $.pnotify( {
                pnotify_title : '${I18N_WARNING}',
                pnotify_text : '${msg.message}',
                pnotify_notice_icon : 'ui-icon io-icon-alert'
            });
        <#elseif msg.type == "ERROR">
            <#if msg.exception?? >
                $.pnotify( {
                    pnotify_title : '${I18N_ERROR}',
                    pnotify_text : '${msg.message}<br/>${msg.stackTrace}',
                    pnotify_type : 'error',
                    pnotify_hide: false
                });
            <#else>
                $.pnotify( {
                    pnotify_title : '${I18N_ERROR}',
                    pnotify_text : '${msg.message}',
                    pnotify_type : 'error',
                    pnotify_hide: false
                });
            </#if>            
        </#if>
    </#list>
    </script>
</#if>