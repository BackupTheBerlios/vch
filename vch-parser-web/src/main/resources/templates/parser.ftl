<#include "header.ftl">
<#include "navigation.ftl">

<h1>${TITLE}</h1>

<#if PAGE??>
    <div id="tree" style="max-height:500px; max-width:800px; overflow:auto; float:left; margin-right:10px"></div>

    <script type="text/javascript">
        function error(msg) {
            $.notify({text:msg, title:'${I18N_ERROR}', icon:'/notify/dialog-error.png'});
            //$('div#errors').html(msg);
            //$('div#errors').show();
            //$('div#errors').fadeOut(5000);
        }
    
        function showDetails(node) {
            var callback = {
                success: function(oResponse) {
                    YAHOO.log("XHR transaction was successful.", "info");
                    var oResults = eval("(" + oResponse.responseText + ")");
                    if((oResults.ResultSet.Result)) {
                        var node = oResults.ResultSet.Result;
                        var html = '<p><a href=\"'+encodeURI(node.video)+'\">'+node.label+'</a>';
                        if(node.pubDate) {
                            var date = new Date();
                            date.setTime(node.pubDate);
                            html += '<br/>' + date.toLocaleString();
                         }
                        if(node.thumb) html += '<br/>' + node.thumb; 
                        if(node.desc) html += '<br/>' + node.desc;
                        $('#content').html(html + '</p>');
                    }
                },
                
                failure: function(oResponse) {
                    error(oResponse.responseText);
                    YAHOO.log("Failed to process XHR transaction.", "error");
                },
                
                argument: {
                    "node": node
                },
                
                timeout: 30000
            };

            var sUrl = "${SERVLET_URI}/?id=${PARSER}&uri=" + encodeURI(node.href) + "&title=" + encodeURI(node.label);
            for(var item in node.data) {
                var value = node.data[item];
                sUrl += "&node.data." + item + "=" + encodeURI(value);
            }
            YAHOO.util.Connect.asyncRequest('GET', sUrl, callback);
            YAHOO.log("URI " + sUrl);
        }
    
        var tree, currentIconMode;
    
        function changeIconMode() {
            var newVal = parseInt(this.value);
            if (newVal != currentIconMode) {
                currentIconMode = newVal;
            }
            buildTree();
        }
            
    
        function buildTree() {
           //create a new tree:
           tree = new YAHOO.widget.TreeView("tree");
           
           //turn dynamic loading on for entire tree:
           tree.setDynamicLoad(loadNodeData, currentIconMode);
           
           //get root node for tree:
           var root = tree.getRoot();
           
           //add child nodes for tree;
           var pages = [{id:"${PAGE.parser}", label:"${PAGE.title}", href:"${PAGE.uri}", type:"IOverviewPage"}];
           
           for (var i=0, j=pages.length; i<j; i++) {
                var tempNode = new YAHOO.widget.TextNode(pages[i], root, false);
           }
        
           //render tree with these toplevel nodes; all descendants of these nodes
           //will be generated as needed by the dynamic loader.
           tree.draw();
           
           tree.subscribe("labelClick", function(node) {
               if(node.isLeaf) {
                   showDetails(node);
               }
           });
        }
        
        function loadNodeData(node, fnLoadComplete)  {
            //We'll create child nodes based on what we get back when we
            //use Connection Manager to pass the text label of the 
            //expanding node to the Yahoo!
            //Search "related suggestions" API.  Here, we're at the 
            //first part of the request -- we'll make the request to the
            //server.  In our Connection Manager success handler, we'll build our new children
            //and then return fnLoadComplete back to the tree.
            
            //prepare URL for XHR request:
            var sUrl = "${SERVLET_URI}/?id=${PARSER}&uri=" + escape(node.href) + "&title=" + node.label;
            for(var item in node.data) {
                var value = node.data[item];
                sUrl += "&node.data." + item + "=" + escape(value);
            }
            
            //prepare our callback object
            var callback = {
            
                //if our XHR call is successful, we want to make use
                //of the returned data and create child nodes.
                success: function(oResponse) {
                    YAHOO.log("XHR transaction was successful.", "info");
                    var oResults = eval("(" + oResponse.responseText + ")");
                    if((oResults.ResultSet.Result) && (oResults.ResultSet.Result.length)) {
                        //Result is an array if more than one result, string otherwise
                        if(YAHOO.lang.isArray(oResults.ResultSet.Result)) {
                            for (var i=0, j=oResults.ResultSet.Result.length; i<j; i++) {
                                var childNode = oResults.ResultSet.Result[i];
                                var tempNode = new YAHOO.widget.TextNode(childNode, node, false);
                                if(childNode.isLeaf) {
                                    tempNode.isLeaf = true;
                                }
                            }
                        } else {
                            //there is only one result
                            var childNode = oResults.ResultSet.Result;
                            var tempNode = new YAHOO.widget.TextNode(childNode, node, false)
                            tempNode.isLeaf = true;
                        }
                    }
                                        
                    //When we're done creating child nodes, we execute the node's
                    //loadComplete callback method which comes in via the argument
                    //in the response object (we could also access it at node.loadComplete,
                    //if necessary):
                    oResponse.argument.fnLoadComplete();
                },
                
                //if our XHR call is not successful, we want to
                //fire the TreeView callback and let the Tree
                //proceed with its business.
                failure: function(oResponse) {
                    error(oResponse.responseText);
                    YAHOO.log("Failed to process XHR transaction: " + oResponse.responseText, "error");
                    oResponse.argument.fnLoadComplete();
                },
                
                //our handlers for the XHR response will need the same
                //argument information we got to loadNodeData, so
                //we'll pass those along:
                argument: {
                    "node": node,
                    "fnLoadComplete": fnLoadComplete
                },
                
                //timeout -- if more than x seconds go by, we'll abort
                //the transaction and assume there are no children:
                timeout: 30000
            };
            
            //With our callback object ready, it's now time to 
            //make our XHR call using Connection Manager's
            //asyncRequest method:
            YAHOO.util.Connect.asyncRequest('GET', sUrl, callback);
        }
        
        YAHOO.util.Event.on(["mode0", "mode1"], "click", changeIconMode);
        var el = document.getElementById("mode1");
        if (el && el.checked) {
            currentIconMode = parseInt(el.value);
        } else {
            currentIconMode = 0;
        }

        buildTree();

    </script>
    
    <div id="content"></div>
    
    <script type="text/javascript">
        var myContainer = document.body.appendChild(document.createElement("div"));
        $(myContainer).css('float', 'right');
        var myLogReader = new YAHOO.widget.LogReader(myContainer);
    </script>
    
</#if>
<#include "footer.ftl">