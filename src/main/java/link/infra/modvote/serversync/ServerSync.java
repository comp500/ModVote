package link.infra.modvote.serversync;

import io.netty.buffer.Unpooled;
import link.infra.modvote.rules.RulesManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.quiltmc.loader.api.QuiltLoader;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

// Ensures mods on server but not on client are synced before registry sync
public class ServerSync implements ModInitializer, ClientModInitializer {
	private static final ResourceLocation ID_SYNC_CHANNEL = new ResourceLocation("modvote", "id_sync");

	public void onInitializeClient() {
		if (QuiltLoader.isModLoaded("fabric-networking-api-v1")) {
			ClientLoginNetworking.registerGlobalReceiver(ID_SYNC_CHANNEL, (client, handler, buf, listenerAdder) -> {
				int numIds = buf.readInt();
				Set<String> ids = new HashSet<>();
				for (; numIds > 0; numIds--) {
					ids.add(buf.readUtf());
				}

				RulesManager.INSTANCE.clientJoinSync(ids);

				return CompletableFuture.completedFuture(PacketByteBufs.empty());
			});
		}
	}

	public void onInitialize() {
		if (QuiltLoader.isModLoaded("fabric-networking-api-v1")) {
			ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
				FriendlyByteBuf syncBuf = new FriendlyByteBuf(Unpooled.buffer());
				Set<String> approvedIds = RulesManager.INSTANCE.votedModsRule.getApprovedModIds();
				syncBuf.writeInt(approvedIds.size());
				for (String id : approvedIds) {
					syncBuf.writeUtf(id);
				}
				sender.sendPacket(ID_SYNC_CHANNEL, syncBuf);
			});
			ServerLoginNetworking.registerGlobalReceiver(ID_SYNC_CHANNEL, (server, handler, understood, buf, synchronizer, responseSender) -> {
				// Do nothing (just needs to be handled)
			});
		}
	}
}
