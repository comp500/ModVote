package link.infra.modvote.scan;

import org.quiltmc.loader.api.QuiltLoader;

import java.nio.file.Path;
import java.util.List;

public class ModScanner {
	public static List<Result> scan() {
		// TODO
		return List.of(new Result("adorn", "Adorn", QuiltLoader.getGameDir()
			.resolve("modvotemods")
			.resolve("Adorn-4.2.0-or+23w13a_or_b-fabric.jar"), QuiltLoader.isModLoaded("adorn")));
	}

	public record Result(String id, String name, Path path, boolean loaded) { }
}
