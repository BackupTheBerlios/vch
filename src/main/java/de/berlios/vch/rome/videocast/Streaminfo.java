package de.berlios.vch.rome.videocast;

public class Streaminfo implements Cloneable {
    private String length;
    
    private String subtitle;

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
        this.length = length;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
    
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
