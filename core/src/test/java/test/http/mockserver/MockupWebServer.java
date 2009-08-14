package test.http.mockserver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class MockupWebServer {

    private ServerSocket httpd = null;
    
    private boolean running = false;

    private static MockupWebServer instance;
    
    private MockupWebServer() {
    }

    public void startServer() throws IOException {
        httpd = new ServerSocket(8081);
        running = true;
        while (running) {
            try {
                Socket socket = httpd.accept();
                (new BrowserClientThread(socket)).start();
            } catch (Exception e) {}
        }
    }

    public void stopServer() throws IOException {
        running = false;
        if (httpd != null) {
            httpd.close();
        }
    }

    public static synchronized MockupWebServer getInstance() {
        if (instance == null) {
            instance = new MockupWebServer();
        }
        return instance;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public static void main(String[] args) throws IOException {
        MockupWebServer.getInstance().startServer();
    }
}

class BrowserClientThread extends Thread {

    private Socket socket;

    private PrintStream out;

    private InputStream in;

    private String cmd;

    private String url;

    public BrowserClientThread(Socket socket) {
        this.socket = socket;
    }

    public void run() {
        try {
            out = new PrintStream(socket.getOutputStream());
            in = socket.getInputStream();
            readRequest();
            createResponse();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readRequest() throws IOException {
        // Request-Zeilen lesen
        Vector<String> request = new Vector<String>(10);
        StringBuffer sb = new StringBuffer(100);
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\r') {
                // ignore
            } else if (c == '\n') { // line terminator
                if (sb.length() <= 0) {
                    break;
                } else {
                    request.addElement(sb.toString());
                    sb = new StringBuffer(100);
                }
            } else {
                sb.append((char) c);
            }
        }
        
        // Kommando, URL und HTTP-Version extrahieren
        String s = request.elementAt(0);
        cmd = "";
        url = "";
        int pos = s.indexOf(' ');
        if (pos != -1) {
            cmd = s.substring(0, pos).toUpperCase();
            s = s.substring(pos + 1);
            // URL
            pos = s.indexOf(' ');
            if (pos != -1) {
                url = s.substring(0, pos);
                s = s.substring(pos + 1);
            } else {
                url = s;
            }
        }
    }

    /**
     * Request bearbeiten und Antwort erzeugen.
     */
    private void createResponse() {
        if (cmd.equals("GET")) {
            if (!url.startsWith("/")) {
                httpError(400, "Bad Request");
            } else {
                String fsep = System.getProperty("file.separator", "/");
                StringBuffer sb = new StringBuffer(url.length());
                for (int i = 1; i < url.length(); ++i) {
                    char c = url.charAt(i);
                    if (c == '/') {
                        sb.append(fsep);
                    } else {
                        sb.append(c);
                    }
                }
                try {
                    InputStream is = MockupWebServer.class.getResourceAsStream("/" + sb.toString());
                    out.print("HTTP/1.0 200 OK\r\n\r\n");

                    byte[] buf = new byte[256];
                    int len;
                    while ((len = is.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    is.close();
                } catch (FileNotFoundException e) {
                    httpError(404, "Error Reading File");
                } catch (IOException e) {
                    httpError(404, "Not Found");
                } catch (Exception e) {
                    httpError(404, "Unknown exception");
                }
            }
        } else {
            httpError(501, "Not implemented");
        }
    }

    private void httpError(int code, String description) {
        out.print("HTTP/1.0 " + code + " " + description + "\r\n");
        out.print("Content-type: text/html\r\n\r\n");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>MockupWebServer-Error</title>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>HTTP/1.0 " + code + "</h1>");
        out.println("<h3>" + description + "</h3>");
        out.println("</body>");
        out.println("</html>");
    }
}