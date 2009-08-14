<#if ERRORS??>
    <#list ERRORS as error>
        <div id="errors" class="errors">
            <ul>
                <li>
                    <pre>${error}</pre>
                </li>
            </ul>
        </div>
    </#list>
</#if>
<#if MESSAGES??>
    <div id="messages" class="messages">
        <ul>
        <#list MESSAGES as msg>
            <li>
                ${msg}  
            </li>
        </#list>
        </ul>
    </div>
</#if>

<!-- fade out -->
<script type="text/javascript">
    $("#messages").fadeOut(5000);
    //$("#errors").fadeOut(3000);
</script>
