package link.infra.modvote;

import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModVote implements PreLaunchEntrypoint {
	public static final Logger LOGGER = LoggerFactory.getLogger("Mod Vote");

	@Override
	public void onPreLaunch(ModContainer mod) {
		if (!"true".equals(System.getProperty("modvote.active"))) {
			throw new RuntimeException("Failed to load loader plugin: are you using an incompatible version of quilt loader? Ensure you have installed ModVoteNil correctly.");
		}
		LOGGER.info("Successfully battled quilt loader demons, mod vote is alive!");
	}

	public static void queueGameRestart() {
		// Need to start a new thread, because mojang's shutdown hook blocks waiting for the server thread to clean up
		new Thread(() -> {
			System.exit(1337);
		}).start();
	}
}
