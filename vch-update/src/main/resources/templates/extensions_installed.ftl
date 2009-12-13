<form id="installed_form" action="${ACTION}" method="get">
<div style="overflow: hidden">
    <select id="installed" name="installed" size="18" style="width: 100%" multiple="multiple">
        <#list INSTALLED as bundle>
            <#if bundle.state == 1>
                <#assign cls="stopped">
                <#assign state=I18N_UNINSTALLED>
            <#elseif bundle.state == 2>
                <#assign cls="stopped">
                <#assign state=I18N_INSTALLED>
            <#elseif bundle.state == 4>
                <#assign cls="stopped">
                <#assign state=I18N_RESOLVED>
            <#elseif bundle.state == 8>
                <#assign cls="stopped">
                <#assign state=I18N_STARTING>
            <#elseif bundle.state == 16>
                <#assign cls="started">
                <#assign state=I18N_STOPPING>
            <#elseif bundle.state == 32>
                <#assign cls="started">
                <#assign state=I18N_ACTIVE>
            </#if>
            <option vch:bundle-symbolicname="${bundle.symbolicName}" vch:bundle-version="${bundle.version}" class="${cls}" value="${bundle.bundleId}" onclick="showDetails('${bundle.name}', '${bundle.author}', '${bundle.version}', '${state}', '${bundle.description}')">
                ${bundle.name} (${bundle.version})
            </option> 
        </#list>
    </select>
</div>
<br/>
<input id="button_update" type="submit" name="submit_update" value="${I18N_UPDATE}"/>
<input id="button_start" type="submit" name="submit_start" value="${I18N_START}"/>
<input id="button_stop" type="submit" name="submit_stop" value="${I18N_STOP}"/>
<input id="button_uninstall" type="submit" name="submit_uninstall" value="${I18N_UNINSTALL}"/>
</form>

<script type="text/javascript">
    $(document).ready(function() {
        $.notify({text:'${I18N_INFO}', title:'${I18N_UPDATES_SEARCHING}', icon:'/notify/dialog-information.png'});
        $.ajax({
            type: "GET",
            url: "${ACTION}",
            data: "updates",
            dataType: "json",
            success: function(data) {
                var updates_available=false;
                for(var i=0; i<data.length; i++) {
                    var res = data[i];
                    var name = res.symbolicName;
                    var version = res.version;
                    var options = $('#installed option');
                    for(var j=0; j<options.length; j++) {
                        var option = options[j];
                        if($(option).attr('vch:bundle-symbolicname') == name) {
                            if($(option).attr('vch:bundle-version') != version) {
                                $(option).addClass('update_available');
                                $(option).text($(option).text() + ' -- ${I18N_UPDATES_AVAILABLE} - ${I18N_VERSION}:' + version);
                                updates_available = true;
                            } 
                        }
                    }
                    
                }
                if(updates_available) {
                    $.notify({text:'${I18N_UPDATES_AVAILABLE_TEXT}', title:'${I18N_UPDATES_AVAILABLE}', icon:'/notify/dialog-information.png'});
                }
            },
            error: function(request, textStatus, exception){
                console.log(request);
                $.notify({text:request.responseText, title:request.statusText, icon:'/notify/dialog-error.png'});
            }
        });
    });
</script>