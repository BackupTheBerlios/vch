<#include "header.ftl">
<#include "status_messages.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>
<#include "status_messages.ftl">

<div id="search_bar">
    <form action="${ACTION}">
        <input type="text" id="search_input" name="q" class="ui-widget ui-widget-content ui-corner-all" <#if Q??>value="${Q}"</#if> />
        <input type="hidden" name="action" value="search" />
        <input type="submit" name="submit" value="${I18N_DO_SEARCH}" class="ui-button" />
    </form>
    <#if COUNT??>
        <small>${I18N_HITS}: ${COUNT}</small>
    </#if>
</div>

<div id="search_container">
    <#if RESULTS?? && (RESULTS.pages?size > 0)>
    <div id="provider_list">
        <ul>
        <#list RESULTS.pages as provider>
        <li>
            <a href="#" onclick="$('#results div').hide(0, function() {$('#provider_${provider_index}').show(0);});">${provider.title} (${provider.pages?size})</a>
        </li>
        </#list>    
        </ul>
    </div>
    <div id="results">
        <#list RESULTS.pages as provider>
        <div id="provider_${provider_index}" style="display:none;" >
            <h2>${provider.title}</h2>
            <ul>
            <#list provider.pages as result>
                <li>
                    <#if result.thumbnail??>
                        <img alt="Preview" src="${result.thumbnail}" />
                    </#if>
                    <h4><a href="#">${result.title}</a></h4>
                    <#if result.publishDate??>
                        <p>${result.publishDate.time?date?string.short}
                        <#if result.duration?? && (result.duration > 0)>
                            <#assign minutes=result.duration/60 />
                            <#assign seconds=result.duration%60 />
                            - ${minutes?string("00")}:${seconds?string("00")} min
                        </#if>
                        </p>
                    </#if>
                    <#if result.description??>
                        <p>
                        <#assign maxlength=800>
                        <#if (result.description?length > maxlength)>
                            ${result.description?substring(0,maxlength)}...
                        <#else>
                            ${result.description}
                        </#if>
                        </p>
                    </#if>
                </li>
            </#list>
            </ul>
        </div>
        </#list>
    </div>
    </#if>
</div>
<#include "footer.ftl">