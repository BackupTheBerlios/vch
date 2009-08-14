package de.berlios.vch.osdserver.io.command;

import de.berlios.vch.osdserver.io.StringUtils;

public class OsdMessage extends Command {

    public static final String INFO = "-info";
    public static final String WARN = "-warn";
    public static final String ERROR = "-error";
    public static final String STATUS = "-status";
    public static final String STATUSCLEAR = "-statusclear";
    
    private int seconds = 0;
    
    private String msg;
    private String type;
    
    public OsdMessage(String message, String type) {
        this(message, type, ERROR.equals(type) ? 10 : 0);
    }
    
    public OsdMessage(String message, String type, int seconds) {
        this.msg = StringUtils.escape(message);
        this.type = type;
        this.seconds = seconds;
    }
    
    @Override
    public String getCommand() {
        if(type.equals(STATUSCLEAR)) {
            return new StringBuilder("message ").append(type).toString();
        } else {
            StringBuilder sb = new StringBuilder("message ").append(type);
            if(seconds > 0) {
                sb.append(" -seconds ").append(seconds);
            }
            sb.append(" '").append(msg).append("'");
            
            return sb.toString();
        }
    }

    @Override
    public String toString() {
        return "OsdMessage";
    }

}
