package link.infra.modvote.scan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModScanner {
	public static List<ModScanResult> scan() {
		// TODO
		return List.of(new ModScanResult("sodium", "Sodium", Path.of(""), true));
	}

	public record ModScanResult(String id, String name, Path path, boolean enabled) {
		public boolean disabled() {
			return !enabled;
		}
	}
}
