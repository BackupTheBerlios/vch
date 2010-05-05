<#macro naviTree webMenuEntry>
    <li>
        <a href="${webMenuEntry.linkUri}">${webMenuEntry.title}</a>
	    <#if (webMenuEntry.childs?size > 0)>
        	<ul>
        	  <#list webMenuEntry.childs as child>
        	      <@naviTree webMenuEntry=child/>
        	  </#list>
        	</ul>
	    </#if>
    </li>
</#macro>


<ul id="navi" class="ui-widget ui-widget-content ui-corner-all ui-state-default">
	<#list NAVIGATION.childs as menu>
	    <li style="margin-top:0px;margin-left:-1px;border-top:none;border-bottom:none;">
            <a href="${menu.linkUri}">${menu.title}</a>
		    <#if menu.childs??>
		    <#if (menu.childs?size > 0)>
	           	<ul>
	           		<#list menu.childs as item>
    					<@naviTree webMenuEntry=item/>
    				</#list>
    			</ul>
    		</#if>
	       	</#if>
	    </li>
	</#list>
</ul>


<div id="errors" style="display:none; float:right"></div>

<script type='text/javascript'>
    $(function() {
        $('#navi').droppy({speed: 150});
    });
</script>