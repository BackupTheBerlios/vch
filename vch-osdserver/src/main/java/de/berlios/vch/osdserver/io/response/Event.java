package de.berlios.vch.osdserver.io.response;

import de.berlios.vch.osdserver.osd.OsdObject;


public class Event extends Response {
    
    private String type;
    
    private String sourceId;
    
    private OsdObject source;
    
    private String modifier;

    public Event(int code, String message) {
        super(code, message);
        String[] msg = message.split(" ");
        sourceId = msg[0];
        if(msg.length > 1) {
            type = msg[1];
            if(type.contains("|")) {
                String[] tmp = type.split("\\|"); 
                type = tmp[0];
                if(tmp.length == 2) {
                    modifier = tmp[1];
                } else {
                    modifier = tmp[1]+"|"+tmp[2];
                }
            }
        }
    }

    public String getSourceId() {
        return sourceId;
    }
    
    public String getType() {
        return type;
    }
    
    public String getModifier() {
        return modifier;
    }
    
    @Override
    public String toString() {
        return "Event";
    }

    public OsdObject getSource() {
        return source;
    }
    
    public void setSource(OsdObject source) {
        this.source = source;
    }
    
    public static final String KEY_UP           = "keyUp";
    public static final String KEY_DOWN         = "keyDown";
    public static final String KEY_MENU         = "keyMenu";
    public static final String KEY_OK           = "keyOk";
    public static final String KEY_BACK         = "keyBack";
    public static final String KEY_LEFT         = "keyLeft";
    public static final String KEY_RIGHT        = "keyRight";
    public static final String KEY_RED          = "keyRed";
    public static final String KEY_GREEN        = "keyGreen";
    public static final String KEY_YELLOW       = "keyYellow";
    public static final String KEY_BLUE         = "keyBlue";
    public static final String KEY_0            = "key0";
    public static final String KEY_1            = "key1";
    public static final String KEY_2            = "key2";
    public static final String KEY_3            = "key3";
    public static final String KEY_4            = "key4";
    public static final String KEY_5            = "key5";
    public static final String KEY_6            = "key6";
    public static final String KEY_7            = "key7";
    public static final String KEY_8            = "key8";
    public static final String KEY_9            = "key9";
    public static final String KEY_INFO         = "keyInfo";
    public static final String KEY_PLAY         = "keyPlay";
    public static final String KEY_PAUSE        = "keyPause";
    public static final String KEY_STOP         = "keyStop";
    public static final String KEY_RECORD       = "keyRecord";
    public static final String KEY_FASTFWD      = "keyFastFwd";
    public static final String KEY_FASTREW      = "keyFastRew";
    public static final String KEY_NEXT         = "keyNext";
    public static final String KEY_PREV         = "keyPrev";
    public static final String KEY_POWER        = "keyPower";
    public static final String KEY_CHANNELPLUS  = "keyChannel+";
    public static final String KEY_CHANNELMINUS = "keyChannel-";
    public static final String KEY_PREVCHANNEL  = "keyPrevChannel";
    public static final String KEY_VOLUMEPLUS   = "keyVolume+";
    public static final String KEY_VOLUMEMINUS  = "keyVolume-";
    public static final String KEY_MUTE         = "keyMute";
    public static final String KEY_AUDIO        = "keyAudio";
    public static final String KEY_SCHEDULE     = "keySchedule";
    public static final String KEY_CHANNELS     = "keyChannels";
    public static final String KEY_TIMERS       = "keyTimers";
    public static final String KEY_RECORDINGS   = "keyRecordings";
    public static final String KEY_SETUP        = "keySetup";
    public static final String KEY_COMMANDS     = "keyCommands";
    public static final String KEY_USER1        = "keyUser1";
    public static final String KEY_USER2        = "keyUser2";
    public static final String KEY_USER3        = "keyUser3";
    public static final String KEY_USER4        = "keyUser4";
    public static final String KEY_USER5        = "keyUser5";
    public static final String KEY_USER6        = "keyUser6";
    public static final String KEY_USER7        = "keyUser7";
    public static final String KEY_USER8        = "keyUser8";
    public static final String KEY_USER9        = "keyUser9";
    public static final String KEY_NONE         = "keyNone";
    public static final String CLOSE            = "close";
    public static final String EDIT             = "edit";
    public static final String FOCUS            = "focus";
    public static final String BLUR             = "blur";

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((modifier == null) ? 0 : modifier.hashCode());
        result = prime * result + ((sourceId == null) ? 0 : sourceId.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Event other = (Event) obj;
        if (modifier == null) {
            if (other.modifier != null)
                return false;
        } else if (!modifier.equals(other.modifier))
            return false;
        if (sourceId == null) {
            if (other.sourceId != null)
                return false;
        } else if (!sourceId.equals(other.sourceId))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }
}
