package link.infra.modvote.data;

import org.quiltmc.json5.JsonReader;
import org.quiltmc.json5.JsonWriter;
import org.quiltmc.loader.api.QuiltLoader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class ConfigHandler {
	public static void write(Set<String> enabledModIds) throws IOException {
		try (BufferedWriter bw = Files.newBufferedWriter(QuiltLoader.getConfigDir().resolve("modvote-loaded.txt"), StandardCharsets.UTF_8)) {
			JsonWriter j = JsonWriter.json(bw);
			j.beginArray();
			for (String modid : enabledModIds) {
				j.value(modid);
			}
			j.endArray();
			j.close();
		}
	}

	public static Set<String> read() throws IOException {
		Set<String> enabledModIds = new HashSet<>();
		try (BufferedReader br = Files.newBufferedReader(QuiltLoader.getConfigDir().resolve("modvote-loaded.txt"), StandardCharsets.UTF_8)) {
			JsonReader j = JsonReader.json(br);
			j.beginArray();
			while (j.hasNext()) {
				enabledModIds.add(j.nextString());
			}
			j.endArray();
			j.close();
		}
		return enabledModIds;
	}
}
