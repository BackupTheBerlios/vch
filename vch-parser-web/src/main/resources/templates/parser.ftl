<#include "header.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>

<#if PAGE??>
    <div id="tree"></div>

    <script type="text/javascript">
    <!--
        function showDetails(node) {
            $.ajax({
                url: '${SERVLET_URI}',
                type: 'GET',
                data: {
                    id : '${PARSER}',
                    uri : $(node).attr('id')
                },
                dataType: 'json',
                timeout: 30000,
                error: function(xhr, text, error) {
                    $('#content').html('');
                    $.pnotify( {
                        pnotify_title : '${I18N_ERROR}',
                        pnotify_text : '<strong>' + xhr.status + ' - ' + xhr.statusText + '</strong><br/>' + xhr.responseText,
                        pnotify_type : 'error',
                        pnotify_hide: false
                    });
                },
                success: function(response){
                    var html = '<h1>' + response.data.title + '</h1>';
                    
                    // add a preview, if available
                    if(response.attributes.vchthumb) {
                        html += '<p><img src="'+response.attributes.vchthumb+'" alt="Preview" class="thumb ui-widget-content ui-corner-all"/></p>';
                    }
                    
                    // add the pubdate, if available
                    if(response.attributes.vchpubDate) {
                        var date = new Date();
                        date.setTime(response.attributes.vchpubDate);
                        html += '<p><strong>' + date.toLocaleString();
                    }
                    
                    // add the duration, if available
                    if(response.attributes.vchduration) {
                        html += ' - ';
                        var secs = parseInt(response.attributes.vchduration);
                        if(secs < 60) {
                            html += secs + ' ${I18N_SECONDS}';                        
                        } else {
                            var minutes = Math.floor(secs / 60);
                            if(minutes < 10) minutes = "0"+minutes;
                            var secs = secs % 60;
                            if(secs < 10) secs = "0"+secs;
                            html += minutes+':'+secs + ' ${I18N_MINUTES}'; 
                        }
                    }
                    
                    html += '</strong></p>';
                    
                    // add the description, if available
                    if(response.attributes.vchdesc) {
                        html += '<p>' + response.attributes.vchdesc + '</p>';
                    } 
                    
                    // add the video link, if available
                    if(response.attributes.vchvideo) {
                        html += '<a id="watch" href="'+response.attributes.vchvideo+'">${I18N_WATCH}</a>';
                    }
                    
                    // display the details
                    $('#content').html(html);
                    $('#watch').button({
                        icons: { primary: 'ui-icon-play'},
                    });
                }
            });
        }
        
        
        $(document).ready(function() {
            var stat =  [{ 
                attributes : { 
                    id : "${PAGE.vchUri}" }, 
                data: { 
                    title : "${TITLE}", 
                    icon : "" }, 
                state : "closed"
            }];
            
            $(function () { 
                $("#tree").tree({
                    data : { 
                        type : "json",
                        async : true,
                        opts : {
                            async : true,
                            method : "POST",
                            url : "${SERVLET_URI}"
                        }
                    },
                    callback : { 
                        // Make sure static is not used once the tree has loaded for the first time
                        onload : function (t) { 
                            t.settings.data.opts.static = false; 
                        },
                        // Take care of refresh calls - n will be false only when the whole tree is refreshed or loaded of the first time
                        beforedata : function (n, t) { 
                            if(n == false) { t.settings.data.opts.static = stat; }
                            return {
                                id : "${PARSER}", 
                                uri : $(n).attr("id")
                            };
                        },
                        onselect : function(n, t) { 
                            if($(n).attr("vchisleaf")) {
                                $('#content').html('<h1>${I18N_LOADING}</h1><img src="/static/icons/loadingAnimation.gif" alt=""/>');
                                showDetails(n);
                            }
                        },
                        error : function(text, tree) {
                            if(text.indexOf("DESELECT") >= 0) return;
                            
                            $.pnotify( {
                                pnotify_title : '${I18N_ERROR}',
                                pnotify_text : text,
                                pnotify_type : 'error',
                                pnotify_hide: false
                            });
                        }
                    },
                    ui : {
                        theme_name : "themeroller",
                        dots : false,
                        animation : 500
                    },
                    lang : {
                        loading: '${I18N_LOADING}'
                    },
                    plugins : {
                        themeroller : { }
                    }
                });
            });
        });
    // -->
    </script>
    
    <div id="content"></div>
</#if>
<#include "footer.ftl">