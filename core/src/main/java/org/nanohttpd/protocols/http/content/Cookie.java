package org.nanohttpd.protocols.http.content;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;


/**
 * A simple cookie representation.
 * This is old code and is flawed in many ways.
 * 
 * @author LordFokas
 */
public class Cookie {
    public static String getHTTPTime(int days) {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return dateFormat.format(calendar.getTime());
    }

    private final String n, v, e;

    public Cookie(String name, String value) {
        this(name, value, 30);
    }

    public Cookie(String name, String value, int numDays) {
        this.n = name;
        this.v = value;
        this.e = getHTTPTime(numDays);
    }

    public Cookie(String name, String value, String expires) {
        this.n = name;
        this.v = value;
        this.e = expires;
    }

    public String getHTTPHeader() {
        String fmt = "%s=%s; expires=%s";
        return String.format(fmt, this.n, this.v, this.e);
    }
}