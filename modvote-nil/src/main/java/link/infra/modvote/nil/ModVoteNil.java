package link.infra.modvote.nil;

import nilloader.NilLoader;
import nilloader.api.ClassTransformer;
import nilloader.api.ModRemapper;
import nilloader.api.NilLogger;
import nilloader.impl.log.Log4j2LogImpl;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.LogManager;

// All entrypoint classes must implement Runnable.
public class ModVoteNil implements Runnable {
	public static final NilLogger log = NilLogger.get("ModVoteNil");

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
			shutdownHook = new Thread(ModVoteNil::cleanupProcess);
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		}
	}

	public static boolean fork(String[] mainArgs) {
		cleanupProcess();
		setupShutdownHook();

		log.info("VERY NORMAL NOTHING TO SEE HERE DEFINITELY NOT CREATING A NEW PROCESS");
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
			log.warn("Failed to create argfile, passing classpath directly");
			args.add(System.getProperty("java.class.path"));
		}
		args.add("org.quiltmc.loader.impl.launch.knot.KnotClient");
		args.addAll(Arrays.asList(mainArgs));

		KillLog4j.bye();
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
		}, "ModVoteNil-ParentExitWatchdog");
		t.setDaemon(true);
		t.start();
	}
	
	@Override
	public void run() {
		ModRemapper.setTargetMapping("none"); // Mini vomits on unmapped stuff, we don't need mappings anyway

		if (!"true".equals(System.getProperty("modvote.forked"))) {
			log.info("preparing to invoke great evils");
			ClassTransformer.register(new QuiltMainHook());
			return;
		}

		log.info("my metamorphosis begins...");
		Optional<ProcessHandle> parentProcess = ProcessHandle.current().parent();
		if (parentProcess.isEmpty()) {
			log.warn("failed to get parent process: will keep running if a launcher terminates the game!");
		} else {
			parentExitWatchdog(parentProcess.get());
		}

		ClassTransformer.register(new PluginHook());
	}

}
