<#include "header.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>
<h2>${MESSAGE}</h2>

<#if STACKTRACE??>
  <br/><br/>
  ${I18N_CAUSE}:<br/>
  <pre>
    <code>${STACKTRACE}<code>
  </pre>
</#if>

<#include "footer.ftl">