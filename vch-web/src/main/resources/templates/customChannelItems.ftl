<#include "header.ftl">
    
    ${NAVIGATION}
    <h1>${TITLE}</h1>
    <#include "status_messages.ftl">
    <h2>${I18N_FEED}: ${CHANNEL_TITLE}</h2>
    <#if CHANNEL_IMAGE?? && (CHANNEL_IMAGE?length > 0)>
        <img id="channel_img" src="${CHANNEL_IMAGE?html}" alt="Logo"/>
    </#if>
    
    <fieldset><legend>${I18N_ADD_NEW_ITEM}</legend>
    <form action="${ACTION}" method="post">
        <input type="hidden" name="action" value="add_item"/>
        <input type="hidden" name="channel" value="${CHANNEL_LINK}"/>
        <table>
            <tr>
                <td><label for="title">${I18N_TITLE}:</label></td>
                <td><input type="text" id="title" name="title" size="100"value="${I18N_NEW_ITEM}"/></td>
            </tr>
            <tr>
                <td><label for="desc">${I18N_DESCRIPTION}:</label></td>
                <td><input type="text" id="desc" name="desc" size="100"value="${I18N_NEW_DESCRIPTION}"/></td>
            </tr>
            <tr>
                <td><label for="enc_link">${I18N_ENCLOSURE_LINK}:</label></td>
                <td><input type="text" id="enc_link" name="enc_link" size="100"value="${I18N_NEW_VIDEO_URL}"/></td>
            </tr>
            <tr>
                <td></td>
                <td><input type="submit" value="${I18N_ADD}"/></td>
            </tr>
        </table>
    </form>
    </fieldset>
    
    <br/><br/>
    
    <fieldset id="item_table"><legend>${I18N_ITEMS}</legend>
    <table>
        <tr>
            <th width="5%"></th>
            <th width="20%">${I18N_TITLE}</th>
            <th width="*">${I18N_DESCRIPTION}</th>
            <th width="20%" style="overflow: hidden">${I18N_ENCLOSURE_LINK}</th>
            <th width="10%">${I18N_OPTIONS}</th>
        </tr>
        <#list ITEMS as item>
            <#if (item_index % 2 == 0)>
                <tr class="even">
            <#else> 
                <tr class="odd">
            </#if>
                <td>
                    <#if (item.thumbnail)?? >
                        <a href="${item.thumbnail}" class="thickbox">
                            <img alt="${item.title}" src="${item.thumbnail}" height="60"/>
                        </a>
                    </#if>
                </td>
                <td>
                    <strong>${item.title}</strong><br/><br/>
                    ${item.pubDate?date?string.medium}
                    <#if (item.enclosure.duration > 0)>
                        <br/>${(item.enclosure.duration/60)?string("00")}:${(item.enclosure.duration%60)?string("00")} min
                    </#if>
                </td>
                <td>${item.description!""}</td>
                <td>
                    <a href="${item.enclosureKey?html}">${item.enclosureKey?right_pad(50)?substring(0, 50)}</a>
                </td>
                <td>
                    <a href="${ACTION}?action=edit_item&amp;guid=${item.guid?url}">${I18N_EDIT}</a><br/>
                    <a href="${ACTION}?action=delete_item&amp;guid=${item.guid?url}">${I18N_DELETE}</a><br/>
                    <a href="${DOWNLOAD_ACTION}?action=download_item&amp;channel=${CHANNEL_LINK?url}&amp;guid=${item.guid?url}">${I18N_DOWNLOAD}</a>
                </td>
            </tr>
        </#list> 
    </table>
    </fieldset>

<#include "footer.ftl">