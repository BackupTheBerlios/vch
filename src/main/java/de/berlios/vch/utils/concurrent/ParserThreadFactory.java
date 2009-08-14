package de.berlios.vch.utils.concurrent;

import java.util.concurrent.ThreadFactory;

public class ParserThreadFactory implements ThreadFactory {

    private String threadNamePrefix;
    
    public ParserThreadFactory(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }
    
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setPriority(Thread.MIN_PRIORITY);
        t.setName(threadNamePrefix + " " + t.getName());
        return t;
    }

}
