package link.infra.modvote.nil;

import org.apache.logging.log4j.LogManager;

public class KillLog4j {
	public static void bye() {
		try {
			LogManager.shutdown();
		} catch (Throwable ignored) { }
	}
}
