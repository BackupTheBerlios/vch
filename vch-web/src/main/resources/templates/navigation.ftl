<#macro naviTree webMenuEntry>
    <li class="yuimenuitem">
        <a class="yuimenuitemlabel" href="${webMenuEntry.linkUri}">${webMenuEntry.title}</a>
	    <#if (webMenuEntry.childs?size > 0)>
            <div class="yuimenu"> 
                <div class="bd">
                	<ul>
                	  <#list webMenuEntry.childs as child>
                	      <@naviTree webMenuEntry=child/>
                	  </#list>
                	</ul>
            	</div>
        	</div>
	    </#if>
    </li>
</#macro>

<div id="navi" class="yuimenubar visible">
	<div class="bd">
        <ul class="first-of-type">
			<#list NAVIGATION.childs as menu>
				<#if menu_index == 0>
				    <li class="yuimenubaritem first-of-type">
				<#else>
				    <li class="yuimenubaritem">
				</#if>
                    <a class="yuimenubaritemlabel" href="${menu.linkUri}">${menu.title}</a>
					<#if menu.childs??>
					<#if (menu.childs?size > 0)>
		        	<div class="yuimenu"> 
		 	        	<div class="bd"> 
				           	<ul>
				           		<#list menu.childs as item>
	            					<@naviTree webMenuEntry=item/>
	            				</#list>
	            			</ul>
            			</div>
        			</div>
        			</#if>
        			</#if>
				</li>
			</#list>
		</ul>
	</div>
</div>

<div id="errors" style="display:none; float:right" class="yuimenubar"></div>

<script type="text/javascript">
	YAHOO.util.Event.onContentReady("navi", function () {
    	var menu = new YAHOO.widget.MenuBar("navi", {autosubmenudisplay: true, showdelay: 0, hidedelay: 750, shadow: true} );
    	menu.render();
  	});
</script>