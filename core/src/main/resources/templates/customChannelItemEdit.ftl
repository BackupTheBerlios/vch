<#include "header.ftl">

    ${NAVIGATION}
    <h1>${TITLE}</h1>
    <#include "status_messages.ftl">
    <h2>${I18N_ITEM}: ${item.title}</h2>
    
    <form action="${ACTION}" method="post">
        <input type="hidden" name="action" value="save_item"/>
        <input type="hidden" name="guid" value="${item.guid?html}"/>
        <table>
            <tr>
                <td><label for="title">${I18N_TITLE}:</label></td>
                <td><input type="text" id="title" name="title" size="100"value="${item.title}"/></td>
            </tr>
            <tr>
                <td><label for="desc">${I18N_DESCRIPTION}:</label></td>
                <td><input type="text" id="desc" name="desc" size="100" value="${(item.description!"")?html}"/></td>
            </tr>
            <tr>
                <td><label for="enc_link">${I18N_ENCLOSURE_LINK}:</label></td>
                <td><input type="text" id="enc_link" name="enc_link" size="100"value="${item.enclosureKey}"/></td>
            </tr>
            <tr>
                <td></td>
                <td><input type="submit" value="${I18N_SAVE}"/></td>
            </tr>
        </table>
    </form>

<#include "footer.ftl">