package link.infra.modvote;

import net.minecraft.server.MinecraftServer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class GracefulTerminator {
	private static final List<WeakReference<MinecraftServer>> servers = new ArrayList<>();

	public static void addServer(MinecraftServer server) {
		servers.add(new WeakReference<>(server));
	}

	public static void gracefullyTerminate() {
		// Need to start a new thread, because mojang's shutdown hook blocks waiting for the server thread to clean up
		// also because we can't block the server thread waiting for the server thread to exit :P
		new Thread(() -> {
			// Try to shutdown all servers before terminating
			Semaphore terminationSem = new Semaphore(0);
			int numServersStopping = 0;
			for (WeakReference<MinecraftServer> ref : servers) {
				var server = ref.get();
				if (server != null) {
					numServersStopping++;
					try {
						new Thread(() -> {
							server.halt(true);
							terminationSem.release();
						}).start();
					} catch (Throwable ignored) {
						terminationSem.release();
					}
				}
			}
			try {
				terminationSem.tryAcquire(numServersStopping, 5, TimeUnit.SECONDS);
			} catch (InterruptedException ignored) {
				// I am inevitable >:)
			}
			System.exit(1337);
		}).start();
	}
}
