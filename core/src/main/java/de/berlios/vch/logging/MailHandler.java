package de.berlios.vch.logging;

import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class MailHandler extends Handler {

    private Formatter defaultFormatter = new SimpleFormatter();
    
    private Executor exec;
    
    private String[] recip;
    private String from;
    private String subject;
    private String username;
    private String password;
    private String host;
    
    public MailHandler() {
        exec = Executors.newSingleThreadExecutor();
        
        String cname = getClass().getName();
        from = LogManager.getLogManager().getProperty(cname + ".sender");
        recip = new String[] {LogManager.getLogManager().getProperty(cname + ".recipient")};
        subject = LogManager.getLogManager().getProperty(cname + ".subject");
        
        username = LogManager.getLogManager().getProperty(cname + ".auth.user");
        password = LogManager.getLogManager().getProperty(cname + ".auth.pass");
        host = LogManager.getLogManager().getProperty(cname + ".auth.host");
    }
    
    @Override
    public void publish(LogRecord record) {
        if(isLoggable(record)) {
            Formatter formatter = getFormatter() != null ? getFormatter() : defaultFormatter;
            String message = formatter.format(record);
            postMail(recip, subject, message, from);
        }
    }
    
    public void postMail(final String recipients[], final String subject, final String message, final String from) {
        Runnable sender = new Runnable() {
            @Override
            public void run() {
                try {
                    // Get system properties
                    Properties props = System.getProperties();

                    // Setup mail server
                    props.put("mail.smtp.host", host);
                    props.put("mail.smtp.auth", "true");

                    // Get session
                    Session session = Session.getDefaultInstance(props, new SmtpAuthenticator(username, password));

                    // Pop Authenticate yourself
                    // Store store = session.getStore("pop3s");
                    // store.connect(popHost, username, password);

                    // Define message
                    MimeMessage msg = new MimeMessage(session);
                    msg.setFrom(new InternetAddress(from));
                    for (int i = 0; i < recipients.length; i++) {
                        msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipients[i]));
                    }
                    msg.setSubject(subject);
                    msg.setText(message);

                    // Send message
                    Transport.send(msg);
                } catch (MessagingException e) {
                    reportError("Couldn't send log message with MailHandler", e, ErrorManager.GENERIC_FAILURE);
                }
            }
        };
        exec.execute(sender);
    }
    
    @Override
    public void close() throws SecurityException {}

    @Override
    public void flush() {}
}