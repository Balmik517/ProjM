package com.pma.spring.web.legacy.runtime;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Legacy runtime module with classloader and timer leak patterns.
 */
@Component
public class LegacyClassLoaderLeakModule {

    private static final Logger logger = Logger.getLogger(LegacyClassLoaderLeakModule.class);

    private static final Vector<ClassLoader> LEAKED_LOADERS = new Vector<ClassLoader>();
    private static final Vector<Timer> LEAKED_TIMERS = new Vector<Timer>();

    @PostConstruct
    public void init() {
        try {
            URLClassLoader child = new URLClassLoader(new URL[0], Thread.currentThread().getContextClassLoader());
            LEAKED_LOADERS.add(child);

            Timer timer = new Timer("LegacyLeakTimer-" + System.currentTimeMillis(), true);
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    // intentionally keep reference chain alive
                    logger.debug("legacy leak timer tick: loaders=" + LEAKED_LOADERS.size());
                }
            }, 1000, 60000);
            LEAKED_TIMERS.add(timer);
        } catch (Exception e) {
            logger.warn("Leak module init failed", e);
        }
    }

    public int leakedLoaderCount() {
        return LEAKED_LOADERS.size();
    }

    public int leakedTimerCount() {
        return LEAKED_TIMERS.size();
    }
}
