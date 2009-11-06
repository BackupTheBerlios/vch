<#include "header.ftl">

${NAVIGATION}
<h1>${TITLE}</h1>
<#include "status_messages.ftl">
<form action="${ACTION}" method="post" style="display: inline">
    <div id="downloads">
    <table class="download" width="100%" border="1">
        <tr>
            <th width="30%">${I18N_TITLE}</th>
            <th width="15%">${I18N_PROGRESS}</th>
            <th width="05%">${I18N_SPEED}</th>
            <th width="15%">${I18N_STATUS}</th>
            <th>${I18N_OPTIONS}</th>
        </tr>
        <#list DOWNLOADS as download>
            <#if download.status != "FINISHED">
            <#if download_index % 2 == 0>
            <tr class="even" id="tr_${download_index}">
            <#else>
            <tr class="odd" id="tr_${download_index}">
            </#if>
                <td>
                    <a id="download_${download_index}" href="${download.id?url}" rel="tooltip">
                        ${download.item.title}
                    </a>
                    <div id="tooltip_${download_index}" class="tooltip">
                        ${download.item.title}
                        <#if download.item.description??>
                        <br/>
                        ${download.item.description}
                        </#if>
                    </div>
                </td>
                <td>
                    <#if (download.progress >= 0)>
                    <div class="progressbar" style="width: ${download.progress}%">${download.progress}%</div>
                    <#else>
                    ${I18N_N_A}
                    </#if>
                </td>
                <td>
                    <#if (download.speed >= 0)>
                    ${download.speed} KiB/s
                    <#else>
                    ${I18N_N_A}
                    </#if>
                </td>
                <td>
                    ${download.status}
                    <#if download.exception??>
                    <a href="javascript:void(0)">
                        <img id="img_exception_${download_index}" src="${DOCROOT}/icons/tango/dialog-warning.png" alt=""/>
                    </a>
                    </#if>
                </td>
                <td>
                    <#if download.pauseSupported>
                        <#if download.running>
                            <a href="${ACTION}?action=stop&amp;id=${download.id?url}">
                                <img src="${DOCROOT}/icons/tango/process-stop.png" title="${I18N_STOP}" alt="${I18N_STOP}"/>
                            </a>
                        <#else>
                            <#if download.startable && download.status != "FINISHED">
                                <a href="${ACTION}?action=start&amp;id=${download.id?url}">
                                    <img src="${DOCROOT}/icons/tango/media-playback-start.png" title="${I18N_START}" alt="${I18N_START}"/>
                                </a>
                            </#if>
                        </#if> 
                    </#if>
                    <a id="delete_${download_index}" href="${ACTION}?action=delete&amp;id=${download.id?url}">
                        <img src="${DOCROOT}/icons/tango/user-trash-full.png" title="${I18N_DELETE}" alt="${I18N_DELETE}"/>
                    </a>
                    <img id="indicator_delete_${download_index}" src="${DOCROOT}/icons/tango/indicator.gif" alt="" style="display:none"/> 
                </td>
            </tr>
            <#if download.exception??>
            <tr id="exception_${download_index}"><td colspan="5">
                <div class="errors">
                    <pre>
                    ${download.exceptionString}
                    </pre>
                </div>
            </td></tr>
            </#if>
            </#if>
        </#list>    
    </table>
    </div>
    <br/>
    <input type="button" value="${I18N_REFRESH}" onclick="javascript:window.location.href='${ACTION}'"/>
    <#if AJAX_ENABLED>
        <!-- disabled because it prevents other ajax request from working correctly
        <input type="checkbox" id="auto_refresh"/>${I18N_AUTO_REFRESH}
        -->
    </#if>
</form>

<span style="margin: 2em"></span>

<form action="${ACTION}" method="post" style="display: inline">
    <input type="hidden" name="action" value="start_all"/>
    <input type="submit" value="${I18N_START_ALL}"/>
</form>
<form action="${ACTION}" method="post" style="display: inline">
    <input type="hidden" name="action" value="stop_all"/>
    <input type="submit" value="${I18N_STOP_ALL}"/>
</form>

<div style="padding: 2em"></div>

<h1>${I18N_FINISHED_DOWNLOADS}</h1>
<form action="${ACTION}" method="post" style="display: inline">
    <div id="finished_downloads">
    <table class="download" width="100%" border="1">
        <tr>
            <th width="30%">${I18N_TITLE}</th>
            <th width="41%">${I18N_FILE}</th>
            <th>${I18N_OPTIONS}</th>
        </tr>
        <#list FINISHED_DOWNLOADS as download>
            <#if download_index % 2 == 0>
            <tr class="even" id="tr_finished_${download_index}">
            <#else>
            <tr class="odd" id="tr_finished_${download_index}">
            </#if>
                <td>
                    <a id="finished_download_${download_index}" href="#" rel="tooltip">
                        ${download.item.title}
                    </a>
                    <div id="finished_tooltip_${download_index}" class="tooltip">
                        ${download.item.title}
                        <#if download.item.description??>
                        <br/>
                        ${download.item.description}
                        </#if>
                    </div>
                </td>
                <td>
                    <#assign pos="${download.localFile?last_index_of(\"/\")}" >
                    <#assign pos = pos?number + 1>
                    <a href="${DOCROOT}/${download.localFile?substring(pos?number)?url}">${download.localFile}</a>
                </td>
                <td>
                    <a id="delete_finished_${download_index}" href="${ACTION}?action=delete_finished&amp;id=${download.itemKey?url}">
                        <img src="${DOCROOT}/icons/tango/user-trash-full.png" title="${I18N_DELETE}" alt="${I18N_DELETE}"/>
                    </a>
                    <img id="indicator_delete_finished_${download_index}" src="${DOCROOT}/icons/tango/indicator.gif" alt="" style="display:none"/>
                </td>
            </tr>
        </#list>    
    </table>
    </div>
</form>

<script type="text/javascript"><!--

    $("a[rel='tooltip']").bind("mouseover", function(e) {
        var id = $(this).attr("id");
        var tooltip = id.replace(/download/, "tooltip");
        $("#" + tooltip).fadeIn("slow");
        $("#" + tooltip).css("top", e.pageY+20);
        $("#" + tooltip).css("left", e.pageX+10);
    });
    
    
    $("a[rel='tooltip']").bind("mouseout", function(e) {
        var id = $(this).attr("id");
        var tooltip = id.replace(/download/, "tooltip");
        $("#" + tooltip).fadeOut("slow");
    });
    
    <#if AJAX_ENABLED>
        // manipulate delete links and register listeners
        // for ajax requests
        var ajaxEnabled=true;
        if(ajaxEnabled) {
            var links = $("a[id^='delete_']");
            for(var i=0; i<links.length; i++) {
                links[i].rel = links[i].href;
                links[i].href = "javascript:void(0)";
            }
            $("a[id^='delete_']").click(function() {
                var tableRow = $("#" + $(this).attr("id").replace(/delete_/, "tr_") );
                
                // show process indicator and hide delete link
                var link = $(this);
                var indicator = $("#indicator_" + $(this).attr("id"));  
                
                indicator.css("display", "inline");
                link.css("display", "none");

                $(this).queue(function() {
                    // make a ajax get request with the "delete url"
                    $.ajax({
                        url: $(this).attr("rel"),
                        type: 'GET',
                        dataType: 'html',
                        timeout: 30000,
                        error: function(){
                            // hide process indicator and show delete link
                            indicator.css("display", "none");
                            link.css("display", "inline");
                            alert("${I18N_COULDNT_DELETE}");
                        },
                        success: function(html){
                            tableRow.fadeOut("slow");
                        }
                    });
                });
            });
        }
        
        // ajax auto refresh
        if(ajaxEnabled) {
            $("#auto_refresh").click(function() {
                if($(this).attr("checked")) {
                    window.setInterval("refresh()", 1000);
                }
            });
        }
        
        function refresh() {
            var url = "/downloads";
            if($("#auto_refresh").attr("checked")) {
                jQuery.get(url, function(data){
                    $("#downloads").replaceWith($(data).find("#downloads"));
                });
            } else {
                stopInterval();
            }
            
            return false;
        }
    </#if>
    
    // exception tooltip
    $("*[id^='exception_']").css("display","none");               
    $("img[id^='img_exception_']").bind("click", function(e) {
        var id = $(this).attr("id");
        var tooltip = id.replace(/img_exception_/, "exception_");
        $("#" + tooltip).toggle();
    });
--></script>

<#include "footer.ftl">