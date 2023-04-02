package link.infra.modvote.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quiltmc.loader.api.LoaderValue;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.plugin.ModLocation;
import org.quiltmc.loader.api.plugin.QuiltLoaderPlugin;
import org.quiltmc.loader.api.plugin.QuiltPluginContext;
import org.quiltmc.loader.api.plugin.gui.PluginGuiTreeNode;
import org.quiltmc.loader.api.plugin.solver.LoadOption;
import org.quiltmc.loader.api.plugin.solver.ModLoadOption;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ModVotePlugin implements QuiltLoaderPlugin {
	private static final Logger LOGGER = LogManager.getLogger("ModVotePlugin");

	public void parentExitWatchdog(ProcessHandle handle) {
		Thread t = new Thread(() -> {
			while (true) {
				if (!handle.isAlive()) {
					System.exit(1);
				}
				try {
					handle.onExit().get(1, TimeUnit.SECONDS);
				} catch (InterruptedException | ExecutionException ex) {
					break;
				} catch (TimeoutException ignored) {}
			}
		}, "ModVotePlugin-ParentExitWatchdog");
		t.setDaemon(true);
		t.start();
	}

	private static volatile Process process = null;
	private static Thread shutdownHook = null;

	private static void cleanupProcess() {
		Process p = process; // Could be run concurrently!
		if (p != null) {
			p.destroy();
			try {
				p.onExit().get(1, TimeUnit.SECONDS);
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException("Failed to terminate forked process", e);
			} catch (TimeoutException ignored) {}
			p.destroyForcibly();
			process = null;
		}
	}

	private static void setupShutdownHook() {
		if (shutdownHook == null) {
			// Note: this isn't run when the JVM forcibly terminates (i.e. from a "kill" button in a launcher)
			//       The parent exit watchdog is a workaround for this issue (though it only works if the child process isn't hung)
			shutdownHook = new Thread(ModVotePlugin::cleanupProcess);
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		}
	}

	public static boolean fork(String[] mainArgs) {
		cleanupProcess();
		setupShutdownHook();

		LOGGER.info("VERY NORMAL NOTHING TO SEE HERE DEFINITELY NOT CREATING A NEW PROCESS");
		List<String> args = new ArrayList<>();

		String javaExec = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
		args.add(javaExec);
		args.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
		args.add("-Dmodvote.forked=true");
		if (System.getProperty("modvote.forkarg") != null) {
			args.add(System.getProperty("modvote.forkarg"));
		}
		args.add("-cp");
		// Dev envs have a biiig class path, so they stick it in an argfile
		try {
			Path tempFile = Files.createTempFile("modvote", ".txt");
			Files.writeString(tempFile, System.getProperty("java.class.path"));
			args.add("@" + tempFile);
		} catch (IOException ex) {
			LOGGER.warn("Failed to create argfile, passing classpath directly");
			args.add(System.getProperty("java.class.path"));
		}
		// TODO: server support?
		args.add("org.quiltmc.loader.impl.launch.knot.KnotClient");
		args.addAll(Arrays.asList(mainArgs));

		LogManager.shutdown();
		ProcessBuilder pb = new ProcessBuilder(args);
		pb.inheritIO();
		try {
			process = pb.start();
		} catch (IOException e) {
			throw new RuntimeException("Failed to fork self", e);
		}
		try {
			if (process == null) {
				return false;
			}
			int exitCode = process.waitFor();
			if (exitCode == 1337) {
				return true;
			}
			System.exit(exitCode);
			return false;
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted when waiting for process", e);
		}
	}

	@Override
	public void load(QuiltPluginContext context, Map<String, LoaderValue> previousData) {
		if (!"true".equals(System.getProperty("modvote.forked"))) {
			LOGGER.info("preparing to invoke great evils");
			while (fork(QuiltLoader.getLaunchArguments(false))) {}
			System.exit(1);
			return;
		}

		Optional<ProcessHandle> parentProcess = ProcessHandle.current().parent();
		if (parentProcess.isEmpty()) {
			LOGGER.warn("Failed to get parent process: will keep running if a launcher terminates the game!");
		} else {
			parentExitWatchdog(parentProcess.get());
		}

		Path submods = Paths.get("modvotemods");
		try {
			Files.createDirectories(submods);
			context.addFolderToScan(submods);
			// TODO: enable/disable mods
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
