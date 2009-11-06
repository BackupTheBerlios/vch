package de.berlios.vch.osdserver.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.osdserver.IEventDispatcher;
import de.berlios.vch.osdserver.io.command.Command;
import de.berlios.vch.osdserver.io.command.GenericCommand;
import de.berlios.vch.osdserver.io.command.Quit;
import de.berlios.vch.osdserver.io.command.Version;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.io.response.GenericResponse;
import de.berlios.vch.osdserver.io.response.Response;
import de.berlios.vch.osdserver.osd.OsdException;

public class Connection {
    private static transient Logger logger = LoggerFactory.getLogger(Connection.class);
    
    /**
     * The socket used to talk to VDR
     */
    private Socket socket;
    
    /**
     * The BufferedWriter to send commands to VDR
     */
    private BufferedWriter out;

    /**
     * Scanner to scan text lines from the socket
     */
    private Scanner scanner;
    
    private IEventDispatcher eventDispatcher;

    /**
     * Creates a new connection to host:port with timeout
     * 
     * @param host
     *            The host name or IP-address of the VDR
     * @param port
     *            The port of the osdserver. Default is 2010
     * @param timeout
     *            The timeout for this connection
     * @throws UnknownHostException
     * @throws IOException
     * @throws OsdException 
     */
    public Connection(String host, int port, int timeout)
            throws UnknownHostException, IOException, OsdException {
        //this(host, port, timeout, Config.getInstance().getProperty("default.encoding"));
        this(host, port, timeout, "UTF-8"); // TODO config param
    }
    
    /**
     * Creates a new connection to host:port with timeout
     * 
     * @param host
     *            The host name or IP-address of the VDR
     * @param port
     *            The port of the osdserver. Default is 2010
     * @param timeout
     *            The timeout for this connection
     * @param encoding
     *            The charset encoding used to talk to VDR
     * @throws UnknownHostException
     * @throws IOException
     * @throws OsdException 
     */
    public Connection(String host, int port, int timeout, String encoding) 
        throws UnknownHostException, IOException, OsdException {
        socket = new Socket();
        InetSocketAddress sa = new InetSocketAddress(host, port);
        socket.connect(sa, timeout);
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), encoding));
        scanner = new Scanner(socket.getInputStream(), encoding);

        // read the welcome message
        readResponse();
        
        // send desired version request
        send(new Version());
    }

    /**
     * 
     * @param host
     *            The host name or IP-address of the VDR
     * @param port
     *            The port of the osdserver. Default is 2010
     * @throws UnknownHostException
     * @throws IOException
     * @throws OsdException 
     */
    public Connection(String host, int port) throws UnknownHostException,
            IOException, OsdException {
        this(host, port, 500);
    }

    /**
     * Sends a command and returns the responses returned by the osdserver
     * @param cmd A command for the osdserver
     * @return A List of Response objects
     * @throws IOException
     * @throws OsdException 
     * @see Command
     * @see Response
     * @see List
     */
    public synchronized List<Response> send(String cmd) throws IOException, OsdException {
        return send(new GenericCommand(cmd));
    }
    
    /**
     * Sends a command to VDR and returns the response from VDR.
     * 
     * @param cmd
     *            The {@link Command}, which should be sent to VDR
     * @return A List of {@link Response} objects
     * @throws IOException
     * @see Command
     * @see Response
     * @see List
     */
    public synchronized List<Response> send(Command cmd) throws IOException, OsdException {
        out.write(cmd.getCommand());
        out.newLine();
        out.flush();
        logger.trace("--> {}", cmd.getCommand());
        

        // read the response
        List<Response> responses = readResponse();

        for (Response response : responses) {
            if(response.isError()) {
                throw new OsdException(response);
            }
        }
        
        // return the response list
        return responses;
    }

    /**
     * Reads the response for a sent command. Used by send()
     * 
     * @return A List of Response objects
     * @see Response
     * @see List
     */
    private List<Response> readResponse() throws IOException {
        List<Response> responses = new ArrayList<Response>();
        Event event = null;
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            int code = -1;
            try {
                code = Integer.parseInt(line.substring(0, 3));
                line = line.substring(4);
            } catch (Exception e) {
                logger.error("Couldn't parse response from osdserver", e);
            }

            
            GenericResponse res = new GenericResponse(code, line);
            responses.add(res);
            
            logger.trace("<-- {} {}", res.getCode(), res.getMessage());
            

            if (code >= 200 && code < 300) {
                // we received an "end of response" line and can stop reading from
                // the socket
                break;
            } else if (code >= 300 && code < 400 && code != 302) {
                // we received an event
                event = new Event(code, line);
            }
        }
        
        // if we received an event, dispatch it to the
        // object, which corresponds to this event
        if(event != null) {
            eventDispatcher.dispatchEvent(event);
        }
        
        return responses;
    }
    

    /**
     * Closes the connection to osdserver. After closing a connection you have to
     * create a new connection to talk to osdserver again.
     * 
     * @throws IOException
     * @throws OsdException 
     */
    public void close() throws IOException, OsdException {
        send(new Quit());
    }

    public IEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public void setEventDispatcher(IEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }
}
