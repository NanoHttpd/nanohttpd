package fi.iki.elonen;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import fi.iki.elonen.NanoHTTPD.Cookie;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public class NanoSession {

	private HashMap<String, SessionHandler.Session> sessions = new HashMap<>();
	private final String COOKIE_NAME = "nano-session";
	private Timer timer = new Timer(COOKIE_NAME);

	public SessionHandler serve(IHTTPSession http) {
		return new SessionHandler(http);
	}

	public class SessionHandler implements ISession {

		private Session session;
		private String id;
		private IHTTPSession http;

		public SessionHandler(IHTTPSession http) {
			this.http = http;
			id = http.getCookies().read(COOKIE_NAME);
			if (id != null) {
				session = sessions.get(id);
			} else {
				http.getCookies().delete(COOKIE_NAME);
			}
		}

		public String generateId() {
			return UUID.randomUUID().toString();
		}

		private void create() {
			if (session == null) {
				session = new Session();
				id = generateId();

				sessions.put(id, session);
				timer.schedule(session.task, session.DEFAULT_TIME);

				Cookie cookie = new Cookie(COOKIE_NAME, id, "");
				http.getCookies().set(cookie);
			}
		}

		@Override
		public String get(String key) {
			if (session == null) {
				create();
			}
			return session.get(key);
		}

		@Override
		public void set(String key, String value) {
			if (session == null) {
				create();
			}
			session.put(key, value);
		}

		@Override
		public void clear() {
			if (session != null) {
				session.task.cancel();
				session = null;
				sessions.remove(id);
				http.getCookies().delete(COOKIE_NAME);
			}
		}

		public class Task extends TimerTask {
			@Override
			public void run() {
				if (session.isValidToExpier()) {
					clear();
				} else {
					session.task = new Task();
					timer.schedule(session.task, session.getExtraTime());
				}
			}
		}

		public class Session {
			private HashMap<String, String> values;
			public long DEFAULT_TIME = 30 * 60 * 1000;
			public long time;

			public TimerTask task;

			public Session() {
				task = new Task();
				time = System.currentTimeMillis();
				values = new HashMap<>();
			}

			public void put(String key, String value) {
				time = System.currentTimeMillis();
				values.put(key, value);
			}

			public String get(String key) {
				time = System.currentTimeMillis();
				return values.get(key);
			}

			public boolean isValidToExpier() {
				if (DEFAULT_TIME + time > System.currentTimeMillis()) {
					return false;
				}
				return true;
			}

			public long getExtraTime() {
				return DEFAULT_TIME + time - System.currentTimeMillis();
			}
		}

		@Override
		public boolean isExits() {
			return session != null;
		}

		@Override
		public void setExpireTime(int minute) {
			if (session == null) {
				create();

			}
			Cookie cookie = new Cookie(COOKIE_NAME, id, getHTTPTime(minute));
			http.getCookies().set(cookie);
			session.time = 60 * 1000 * minute;
			session.task.cancel();
			session.task = new Task();
			timer.schedule(session.task, minute * 60 * 1000);

		}

		public String getHTTPTime(int minute) {
			Calendar calendar = Calendar.getInstance();
			SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			calendar.add(Calendar.MINUTE, minute);
			return dateFormat.format(calendar.getTime());
		}
	}

	public interface ISession {
		public String get(String key);//get session, if first time then session will be created

		public void set(String key, String value);//set session, if first time then session will be created

		public void clear();// clear session and delete from memory

		public boolean isExits();// check session is already created or not

		public void setExpireTime(int minute);// set expire time for session in minute, default 30 minute
		
		//session will be clear automatically after expire time, if before expire time access session then time start from 0 
	}

}
