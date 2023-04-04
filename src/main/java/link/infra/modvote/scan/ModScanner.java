package link.infra.modvote.scan;

import link.infra.modvote.logging.PluginLogHandler;
import org.quiltmc.json5.JsonReader;
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
			PluginLogHandler.INSTANCE.error("Failed to scan mods", e);
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
								JsonReader j = JsonReader.json(br);
								j.beginObject();
								while (j.hasNext()) {
									String key = j.nextName();
									switch (key) {
										case "id" -> modid = j.nextString();
										case "name" -> name = j.nextString();
										default -> j.skipValue();
									}
								}
								j.endObject();
							}
							break;
						} else if (entry.getName().equals("quilt.mod.json")) {
							try (BufferedReader br = new BufferedReader(new InputStreamReader(zis, StandardCharsets.UTF_8))) {
								JsonReader j = JsonReader.json(br);
								j.beginObject();
								while (j.hasNext()) {
									String key = j.nextName();
									switch (key) {
										case "quilt_loader" -> {
											j.beginObject();
											while (j.hasNext()) {
												String qKey = j.nextName();
												if ("id".equals(qKey)) {
													modid = j.nextString();
												} else {
													j.skipValue();
												}
											}
											j.endObject();
										}
										case "metadata" -> {
											j.beginObject();
											while (j.hasNext()) {
												String qKey = j.nextName();
												if ("name".equals(qKey)) {
													name = j.nextString();
												} else {
													j.skipValue();
												}
											}
											j.endObject();
										}
										default -> j.skipValue();
									}
								}
								j.endObject();
							}
							break;
						}
					}
				}

				if (modid == null) {
					throw new RuntimeException("Failed to find mod metadata in jar " + file);
				}
				if (name == null) {
					name = modid;
				}
				results.add(new Result(modid, name, file, QuiltLoader.isModLoaded(modid)));
			}
		}
		return results;
	}

	public record Result(String id, String name, Path path, boolean loaded) { }
}
