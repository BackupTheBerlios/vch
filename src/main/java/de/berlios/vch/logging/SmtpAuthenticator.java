package de.berlios.vch.logging;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class SmtpAuthenticator extends Authenticator {
    private String user = null;
    private String pwd  = null;

    public SmtpAuthenticator(String user, String pwd) {
        this.user = user;
        this.pwd = pwd;
    }

    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(this.user, this.pwd);
    }
}