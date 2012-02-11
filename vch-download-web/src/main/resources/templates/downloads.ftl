<#include "header.ftl">
<#include "status_messages.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>
<#include "status_messages.ftl">
<script type="text/javascript">
    var tooltip;
    
    $(function() {
        $('div.progressbar').each(function() {
            var progress = parseInt( $(this).attr('vch:value') );
            $(this).progressbar({ value:progress });
        });
        
        
        // This notice is used as a tooltip.
        tooltip = $.pnotify({
            pnotify_title: "Tooltip",
            pnotify_text: "I'm not in a stack. I'm positioned like a tooltip with JavaScript.",
            pnotify_hide: false,
            pnotify_closer: false,
            pnotify_history: false,
            pnotify_animate_speed: 100,
            pnotify_opacity: .9,
            pnotify_notice_icon: "ui-icon ui-icon-comment",
            // Setting stack to false causes Pines Notify to ignore this notice when positioning.
            pnotify_stack: false,
            pnotify_after_init: function(pnotify){
                // Remove the notice if the user mouses over it.
                pnotify.mouseout(function(){
                    pnotify.pnotify_remove();
                });
            },
            pnotify_before_open: function(pnotify){
                // This prevents the notice from displaying when it's created.
                pnotify.pnotify({
                    pnotify_before_open: null
                });
                return false;
            }
        });
    });
    
    function showTooltip(title, desc) {
        var options = {
            pnotify_title: title,
            pnotify_text: desc
        };        
        tooltip.pnotify(options);
        tooltip.pnotify_display();
    }
</script>

<div id="downloads"  class="ui-widget-content ui-corner-all">
<table class="download" width="100%">
    <tr class="ui-widget-header">
        <th style="width:30%">${I18N_DL_TITLE}</th>
        <th style="width:15%">${I18N_DL_PROGRESS}</th>
        <th style="width:05%">${I18N_DL_SPEED}</th>
        <th style="width:15%">${I18N_DL_STATUS}</th>
        <th>${I18N_DL_OPTIONS}</th>
    </tr>
    <#list DOWNLOADS as download>
        <#-- if download.status != "FINISHED" -->
        <#if download_index % 2 == 0>
        <tr id="tr_${download_index}">
        <#else>
        <tr class="odd" id="tr_${download_index}">
        </#if>
            <td>
                <a id="download_${download_index}" href="${download.id?url}" rel="tooltip" onmouseout="tooltip.pnotify_remove();" 
                    onmousemove="tooltip.css({'top': event.clientY+12, 'left': event.clientX+12});" onmouseover="showTooltip('${download.videoPage.title?js_string?xml}', '${download.videoPage.description?replace('\n',' ','m')?js_string?xml}');">
                    ${download.videoPage.title}
                </a>
            </td>
            <td>
                <#if (download.progress >= 0)>
                <div class="progressbar" vch:value="${download.progress}"></div>
                <#else>
                ${I18N_DL_N_A}
                </#if>
            </td>
            <td>
                <#assign speed=download.speed >
                <#if (speed >= 0)>
                ${speed} KiB/s
                <#else>
                ${I18N_DL_N_A}
                </#if>
            </td>
            <td>
                ${download.status}
                <#if download.exception??>
                <a href="javascript:void(0)">
                    <img id="img_exception_${download_index}" src="${STATIC_PATH}/icons/tango/dialog-warning.png" alt=""/>
                </a>
                </#if>
            </td>
            <td>
                <#if download.pauseSupported>
                    <#if download.running>
                        <button id="stop_${download_index}">${I18N_DL_STOP}</button>
                        <script type="text/javascript">
                            $(function () {
                                $('button#stop_${download_index}').button(
                                    {
                                        icons: { primary: 'ui-icon-stop'},
                                        text: false
                                    }
                                ).click(function() {
                                    window.location.href = '${ACTION}?action=stop&id=${download.id?url}';
                                });
                            });
                        </script>
                    <#else>
                        <#if download.startable && download.status != "FINISHED">
                            <button id="start_${download_index}">${I18N_DL_START}</button>
                            <script type="text/javascript">
                                $(function () {
                                    $('button#start_${download_index}').button(
                                        {
                                            icons: { primary: 'ui-icon-play'},
                                            text: false
                                        }
                                    ).click(function() {
                                        window.location.href = '${ACTION}?action=start&id=${download.id?url}';
                                    });
                                });
                            </script>
                        </#if>
                    </#if> 
                </#if>
                <button id="delete_${download_index}">${I18N_DL_DELETE}</button>
                <script type="text/javascript">
                    $(function () {
                        $('button#delete_${download_index}').button(
                            {
                                icons: { primary: 'ui-icon-trash'},
                                text: false
                            }
                        ).click(function() {
                            var tableRow = $("#" + $(this).attr("id").replace(/delete_/, "tr_") );
                            var exceptionRow = $("#" + $(this).attr("id").replace(/delete_/, "exception_") );
                            
                            // show process indicator and hide delete link
                            var link = $(this);
                            var indicator = $("#indicator_" + $(this).attr("id"));  
                            
                            indicator.css("display", "inline");
                            $(this).css("display", "none");
            
                            // make a ajax get request with the "delete url"
                            $.ajax({
                                url: '${ACTION}?action=delete&id=${download.id?url}',
                                type: 'GET',
                                dataType: 'text',
                                timeout: 30000,
                                error: function(){
                                    // hide process indicator and show delete link
                                    indicator.css("display", "none");
                                    $(this).css("display", "inline");
                                    alert("${I18N_DL_COULDNT_DELETE}");
                                },
                                success: function(html){
                                    tableRow.fadeOut("slow");
                                    exceptionRow.fadeOut("slow");
                                }
                            });
                        });
                    });
                </script>
                <img id="indicator_delete_${download_index}" src="${STATIC_PATH}/icons/tango/indicator.gif" alt="" style="display:none"/> 
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
    </#list>    
</table>
</div>
<p style="display:inline"><br/><input class="ui-button" type="button" value="${I18N_DL_REFRESH}" onclick="javascript:window.location.href='${ACTION}'" /></p>

<form action="${ACTION}" method="post" style="display: inline; margin-left: 2em">
    <p style="display:inline">
    <input type="hidden" name="action" value="start_all" />
    <input class="ui-button" type="submit" value="${I18N_DL_START_ALL}" />
    </p>
</form>
<form action="${ACTION}" method="post" style="display: inline">
    <p style="display:inline">
    <input type="hidden" name="action" value="stop_all" />
    <input class="ui-button" type="submit" value="${I18N_DL_STOP_ALL}" />
    </p>
</form>

<div style="padding: 2em"></div>

<h1>${I18N_DL_FINISHED_DOWNLOADS}</h1>
<div id="finished_downloads" class="ui-widget-content ui-corner-all">
<table class="download" width="100%">
    <tr class="ui-widget-header">
        <th style="width:30%">${I18N_DL_TITLE}</th>
        <th style="width:41%">${I18N_DL_FILE}</th>
        <th>${I18N_DL_OPTIONS}</th>
    </tr>
    <#list FINISHED_DOWNLOADS as download>
        <#if download_index % 2 == 0>
        <tr id="tr_finished_${download_index}">
        <#else>
        <tr class="odd" id="tr_finished_${download_index}">
        </#if>
            <td>
                <a id="finished_download_${download_index}" href="#" rel="tooltip" onmouseout="tooltip.pnotify_remove();" 
                    onmousemove="tooltip.css({'top': event.clientY+12, 'left': event.clientX+12});" onmouseover="showTooltip('${download.title?js_string?xml}', '${download.description?replace('\n',' ','m')?js_string?xml}')">
                    ${download.title}
                </a>
            </td>
            <td>
                <#assign pos="${download.videoFile?last_index_of(\"/\")}" >
                <#assign pos = pos?number + 1>
                <a href="${FILE_PATH}/${download.videoFile?substring(pos?number)?url}">${download.videoFile?substring(pos?number)}</a>
            </td>
            <td>
                <button id="delete_finished_${download_index}">${I18N_DL_DELETE}</button>
                <script type="text/javascript">
                    $(function () {
                        $('button#delete_finished_${download_index}').button(
                            {
                                icons: { primary: 'ui-icon-trash'},
                                text: false
                            }
                        ).click(function() {
                            var tableRow = $("#" + $(this).attr("id").replace(/delete_/, "tr_") );

                            // show process indicator and hide delete link
                            var link = $(this);
                            var indicator = $("#indicator_" + $(this).attr("id"));  
                            
                            indicator.css("display", "inline");
                            $(this).css("display", "none");
            
                            // make a ajax get request with the "delete url"
                            $.ajax({
                                url: '${ACTION}?action=delete_finished&id=${download.id?url}',
                                type: 'GET',
                                timeout: 30000,
                                error: function(){
                                    // hide process indicator and show delete link
                                    indicator.css("display", "none");
                                    $(this).css("display", "inline");
                                    alert("${I18N_DL_COULDNT_DELETE}");
                                },
                                success: function(html){
                                    tableRow.fadeOut("slow");
                                }
                            });
                        });
                    });
                </script>
                <img id="indicator_delete_finished_${download_index}" src="${STATIC_PATH}/icons/tango/indicator.gif" alt="" style="display:none"/>
            </td>
        </tr>
    </#list>    
</table>
</div>

<script type="text/javascript">
        
    // exception tooltip
    $("*[id^='exception_']").css("display","none");               
    $("img[id^='img_exception_']").bind("click", function(e) {
        var id = $(this).attr("id");
        var tooltip = id.replace(/img_exception_/, "exception_");
        $("#" + tooltip).toggle();
    });
</script>

<#include "footer.ftl">