<#include "header.ftl">
<#include "status_messages.ftl">
<#include "navigation.ftl">

<h2>${TITLE}</h2>

<img src="${STATIC_PATH}/qrcode.png" alt="QR-Code" style="float: left" />
<div style="position: relative">
<p style="width: 900px; position: relative; top: 1em">
${I18N_SCANNER_HELP}
<br/><br/>
<a href="http://vch.berlios.de/repo/releases/de/berlios/vch/vchr/1.0.0/vchr-1.0.0.apk">Download</a>
</p>
</div>
<p style="clear: both"><br/><br/>${I18N_INSTALL_HELP}</p>
<img src="${STATIC_PATH}/help/settings_root.png" alt="${I18N_ALT_SETTINGS}"/>
<img src="${STATIC_PATH}/help/settings_applications.png" alt="${I18N_ALT_SETTINGS_APPS}"/>

<#include "footer.ftl">