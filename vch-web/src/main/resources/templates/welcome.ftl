<#include "header.ftl">
<#include "status_messages.ftl">
<#include "navigation.ftl">

<h2>${TITLE}</h2>

<div id="news">${I18N_LOADING_NEWS} <img src="/static/icons/loadingAnimation.gif" alt=""/></div>
<script type="text/javascript">
<!--
$(document).ready(function() {

$.ajax({
    url: '${SERVLET_URI}',
    type: 'GET',
    data: {
        news : 'show'
    },
    dataType: 'json',
    timeout: 30000,
    success: function(response, status, xhr) {
        $('#news').hide();
        if(response.length == 0) {
            $('#news').html('<p>${I18N_NO_NEWS}</p>').show();
        } else {
            $('#news').html('<ul></ul>');
            for(var i=0; i<response.length; i++) {
                var item = response[i];
    
                var li = document.createElement('li');
                $(li).append('<h3>'+item.title+'</h3>');
                $(li).append(' - '+item.date);
                $(li).append('<p>'+item.text);
                $(li).append(' <a href="'+item.link+'">${I18N_READ_MORE}</a></p>');
                
                $('#news ul').append($(li));    
            }
            $('#news').slideDown('slow');
        }
    },
    error: function(xhr, statusText, error) {
        $('#news').hide();
        $.pnotify( {
            pnotify_title : '${I18N_ERROR}',
            pnotify_text : '${I18N_LOADING_NEWS_FAILED}<br/>' + error,
            pnotify_type : 'error',
            pnotify_hide: false
        });
    }
});

});
-->
</script>

<#include "footer.ftl">