$(document).ready(function() {
    $("img[src='next.png']").attr("src", "icons/next.png").removeAttr("width").removeAttr("height");
    $("img[src='next_g.png']").attr("src", "icons/next_g.png").removeAttr("width").removeAttr("height");
    
    $("img[src='prev.png']").attr("src", "icons/prev.png").removeAttr("width").removeAttr("height");
    $("img[src='prev_g.png']").attr("src", "icons/prev_g.png").removeAttr("width").removeAttr("height");
    
    $("img[src='up.png']").attr("src", "icons/up.png").removeAttr("width").removeAttr("height");
    $("img[src='up_g.png']").attr("src", "icons/up_g.png").removeAttr("width").removeAttr("height");
    
    $("img[alt='contents']").attr("src", "icons/contents.png").removeAttr("width").removeAttr("height");

    $("div[class='navigation'] br:lt(1)").after("<br>");

    $("div[class='navigation'] br:eq(2)").before("&nbsp;&nbsp;&nbsp;<a href=\"main.pdf\">PDF</a>");
});