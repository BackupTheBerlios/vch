package de.berlios.vch;

public class Usage {

    public static void main(String[] args) {

        System.out.println("Usage: java -cp vodcatcherhelper.jar de.berlios.vch.<Module>");
        System.out.println();
        System.out.println("Module: RSSFeedCatcher <http prefix>");
        System.out.println("        Collect RSS Feeds and generate XMLs in current directory.");
        System.out.println("        Prints vodcatchersources.conf to stdout for the generated XMLs including Prefix");
        System.out.println();
        System.out.println("        HTTPServer <Portnumber>");
        System.out.println("        Start a simple WEB Server for the current directory.");

    }

}
