package fi.iki.express;

import java.util.ArrayList;
import java.util.Map;
import android.content.Context;


/**
 * Created by James on 6/8/2015.
 */
public abstract class NanoAndroidExpress extends NanoExpress {

    protected final Context applicationContext;
    /**
     * Constructs an HTTP server on given port.
     *
     * @param port
     */
    public NanoAndroidExpress(int port, Context applicationContext) {
        super(port);
        this.applicationContext = applicationContext;
    }

    public NanoAndroidExpress(int port, Context applicationContext, ArrayList<String> routePriority, Map<String, Router> routerArray) {
        super(port, routePriority, routerArray);
        this.applicationContext = applicationContext;
    }

    /**
     * User Define the mechanism of how he/she wants to load the mapping.
     * Externally
     */
    @Override
    public abstract void loadMappings();

     public synchronized void addMappings(String path, AndroidRouter route) {
        route.setApplicationContext(this.applicationContext);
        super.addMappings(path, route);
    }
}
