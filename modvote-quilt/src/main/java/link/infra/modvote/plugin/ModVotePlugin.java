package link.infra.modvote.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quiltmc.loader.api.LoaderValue;
import org.quiltmc.loader.api.plugin.QuiltLoaderPlugin;
import org.quiltmc.loader.api.plugin.QuiltPluginContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class ModVotePlugin implements QuiltLoaderPlugin {
	private static final Logger LOGGER = LogManager.getLogger("ModVotePlugin");

	@Override
	public void load(QuiltPluginContext context, Map<String, LoaderValue> previousData) {
		Path submods = Paths.get("modvotemods");
		try {
			Files.createDirectories(submods);
			context.addFolderToScan(submods);
			LOGGER.info("hehe :3");
			System.setProperty("modvote.active", "true");
		} catch (IOException e) {
			LOGGER.warn("Failed to set up modvotemods folder", e);
		}
	}

	@Override
	public void unload(Map<String, LoaderValue> data) {
		LOGGER.info("nooooo :(");
	}
}
