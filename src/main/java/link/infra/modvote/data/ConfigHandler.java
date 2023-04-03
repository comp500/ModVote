package link.infra.modvote.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
			new Gson().toJson(enabledModIds, bw);
		}
	}

	public static HashSet<String> read() throws IOException {
		try (BufferedReader br = Files.newBufferedReader(QuiltLoader.getConfigDir().resolve("modvote-loaded.txt"), StandardCharsets.UTF_8)) {
			return new Gson().fromJson(br, new TypeToken<>() {});
		}
	}
}
