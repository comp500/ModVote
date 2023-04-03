package link.infra.modvote.scan;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import link.infra.modvote.ModVote;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ModScanner {
	public static Path modsRoot = QuiltLoader.getGameDir().resolve("modvotemods");

	public static List<Result> scan() {
		try {
			return doScan();
		} catch (IOException e) {
			ModVote.LOGGER.error("Failed to scan mods", e);
			return List.of();
		}
	}

	private static List<Result> doScan() throws IOException {
		List<Result> results = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(modsRoot, "*.{jar}")) {
			for (Path file : stream) {
				String modid = null;
				String name = null;
				// Attempt to open zip
				try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(file))) {
					ZipEntry entry;
					while ((entry = zis.getNextEntry()) != null) {
						if (entry.getName().equals("fabric.mod.json")) {
							try (BufferedReader br = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8))) {
								JsonObject el = new Gson().fromJson(br, JsonObject.class);
								modid = el.get("id").getAsString();
								if (el.has("name")) {
									name = el.get("name").getAsString();
								} else {
									name = modid;
								}
							}
							break;
						} else if (entry.getName().equals("quilt.mod.json")) {
							try (BufferedReader br = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8))) {
								JsonObject el = new Gson().fromJson(br, JsonObject.class);
								JsonObject qloader = el.getAsJsonObject("quilt_loader");
								modid = qloader.get("id").getAsString();
								if (qloader.has("metadata") && qloader.getAsJsonObject("metadata").has("name")) {
									name = qloader.getAsJsonObject("metadata").get("name").getAsString();
								} else {
									name = modid;
								}
							}
							break;
						}
					}
				}

				if (modid == null || name == null) {
					throw new RuntimeException("Failed to find mod metadata in jar " + file);
				}
				results.add(new Result(modid, name, file, QuiltLoader.isModLoaded(modid)));
			}
		}
		return results;
	}

	public record Result(String id, String name, Path path, boolean loaded) { }
}
