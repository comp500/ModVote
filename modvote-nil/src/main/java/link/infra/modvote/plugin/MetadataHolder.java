package link.infra.modvote.plugin;

import org.quiltmc.loader.api.plugin.ModMetadataExt;

import java.util.Collection;
import java.util.List;

public class MetadataHolder {
	// See: PluginHook (horrible hack)
	public static final ModMetadataExt.ModPlugin PLUGIN = new ModMetadataExt.ModPlugin() {
		@Override
		public String pluginClass() {
			return "link.infra.modvote.plugin.ModVotePlugin";
		}

		@Override
		public Collection<String> packages() {
			return List.of("link.infra.modvote.plugin");
		}
	};
}
