<#include "header.ftl">
<#include "status_messages.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>
<#include "status_messages.ftl">
<script type="text/javascript">
    $(function() {
        $('div.progressbar').each(function() {
            var progress = parseInt( $(this).attr('vch:value') );
            $(this).progressbar({ value:progress });
        });
    });
</script>
<form action="${ACTION}" method="post" style="display: inline">
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
                    <a id="download_${download_index}" href="${download.id?url}" rel="tooltip">
                        ${download.videoPage.title}
                    </a>
                    <div id="tooltip_${download_index}" class="tooltip">
                        ${download.videoPage.title}
                        <#if download.videoPage.description??>
                        <br/>
                        ${download.videoPage.description}
                        </#if>
                    </div>
                </td>
                <td>
                    <#if (download.progress >= 0)>
                    <div class="progressbar" vch:value="${download.progress}"></div>
                    <#else>
                    ${I18N_DL_N_A}
                    </#if>
                </td>
                <td>
                    <#if (download.speed >= 0)>
                    ${download.speed} KiB/s
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
                            <#if AJAX_ENABLED>
                                var tableRow = $("#" + $(this).attr("id").replace(/delete_/, "tr_") );
                                
                                // show process indicator and hide delete link
                                var link = $(this);
                                var indicator = $("#indicator_" + $(this).attr("id"));  
                                
                                indicator.css("display", "inline");
                                $(this).css("display", "none");
                
                                // make a ajax get request with the "delete url"
                                $(this).queue(function() {
                                    $.ajax({
                                        url: '${ACTION}?action=delete&id=${download.id?url}',
                                        type: 'GET',
                                        dataType: 'html',
                                        timeout: 30000,
                                        error: function(){
                                            // hide process indicator and show delete link
                                            indicator.css("display", "none");
                                            $(this).css("display", "inline");
                                            alert("${I18N_DL_COULDNT_DELETE}");
                                        },
                                        success: function(html){
                                            tableRow.fadeOut("slow");
                                            console.log('success');
                                        }
                                    });
                                });
                            <#else>
                                window.location.href = '${ACTION}?action=delete&id=${download.id?url}';
                            </#if>
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
    <#if AJAX_ENABLED>
        <!-- disabled because it prevents other ajax request from working correctly
        <input type="checkbox" id="auto_refresh"/>${I18N_DL_AUTO_REFRESH}
        -->
    </#if>
</form>

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
<form action="${ACTION}" method="post" style="display: inline">
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
                    <a id="finished_download_${download_index}" href="#" rel="tooltip">
                        ${download.title}
                    </a>
                    <div id="finished_tooltip_${download_index}" class="tooltip">
                        ${download.title}
                        <#if download.description??>
                        <br/>
                        ${download.description}
                        </#if>
                    </div>
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
                            <#if AJAX_ENABLED>
                                var tableRow = $("#" + $(this).attr("id").replace(/delete_/, "tr_") );
                                
                                // show process indicator and hide delete link
                                var link = $(this);
                                var indicator = $("#indicator_" + $(this).attr("id"));  
                                
                                indicator.css("display", "inline");
                                $(this).css("display", "none");
                
                                // make a ajax get request with the "delete url"
                                $(this).queue(function() {
                                    $.ajax({
                                        url: '${ACTION}?action=delete_finished&id=${download.id?url}',
                                        type: 'GET',
                                        dataType: 'html',
                                        timeout: 30000,
                                        error: function(){
                                            // hide process indicator and show delete link
                                            indicator.css("display", "none");
                                            $(this).css("display", "inline");
                                            alert("${I18N_DL_COULDNT_DELETE}");
                                        },
                                        success: function(html){
                                            tableRow.fadeOut("slow");
                                            console.log('success');
                                        }
                                    });
                                });
                            <#else>
                                window.location.href = '${ACTION}?action=delete_finished&id=${download.id?url}';
                            </#if>
                            });
                        });
                    </script>
                    <img id="indicator_delete_finished_${download_index}" src="${STATIC_PATH}/icons/tango/indicator.gif" alt="" style="display:none"/>
                </td>
            </tr>
        </#list>    
    </table>
    </div>
</form>

<script type="text/javascript">

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
        
    // exception tooltip
    $("*[id^='exception_']").css("display","none");               
    $("img[id^='img_exception_']").bind("click", function(e) {
        var id = $(this).attr("id");
        var tooltip = id.replace(/img_exception_/, "exception_");
        $("#" + tooltip).toggle();
    });
</script>

<#include "footer.ftl">