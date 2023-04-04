package link.infra.modvote.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract sealed class PluginLogHandler {
	public static final PluginLogHandler INSTANCE;

	public abstract void info(String msg);
	public abstract void warn(String msg);
	public abstract void warn(String msg, Throwable e);
	public abstract void error(String msg, Throwable e);
	public void shutdown() {}

	static {
		PluginLogHandler inst;
		try {
			inst = new Log4j();
		} catch (Throwable ex) {
			inst = new Basic();
		}
		INSTANCE = inst;
	}

	private static final class Log4j extends PluginLogHandler {
		private final Logger LOGGER = LogManager.getLogger("ModVotePlugin");

		@Override
		public void info(String msg) {
			LOGGER.info(msg);
		}

		@Override
		public void warn(String msg) {
			LOGGER.warn(msg);
		}

		@Override
		public void warn(String msg, Throwable e) {
			LOGGER.warn(msg, e);
		}

		@Override
		public void error(String msg, Throwable e) {
			LOGGER.error(msg, e);
		}

		@Override
		public void shutdown() {
			LogManager.shutdown();
		}
	}

	private static final class Basic extends PluginLogHandler {
		@Override
		public void info(String msg) {
			System.out.println("[ModVotePlugin/INFO] " + msg);
		}

		@Override
		public void warn(String msg) {
			System.out.println("[ModVotePlugin/WARN] " + msg);
		}

		@Override
		public void warn(String msg, Throwable e) {
			System.out.println("[ModVotePlugin/WARN] " + msg + ": " + e);
		}

		@Override
		public void error(String msg, Throwable e) {
			System.out.println("[ModVotePlugin/ERROR] " + msg + ": " + e);
		}
	}
}
