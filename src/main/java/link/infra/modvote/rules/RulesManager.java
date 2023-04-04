package link.infra.modvote.rules;

import link.infra.modvote.GracefulTerminator;
import link.infra.modvote.ModVote;
import link.infra.modvote.data.ConfigHandler;
import link.infra.modvote.mixin.RulesAccessor;
import link.infra.modvote.plugin.VotedModsRule;
import link.infra.modvote.scan.ModScanner;
import link.infra.modvote.ui.BossEventUpdater;
import net.fabricmc.api.EnvType;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.voting.rules.Rule;
import net.minecraft.world.level.Level;
import org.quiltmc.loader.api.QuiltLoader;
import org.quiltmc.loader.api.minecraft.MinecraftQuiltLoader;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RulesManager {
	public static final RulesManager INSTANCE = new RulesManager();

	public List<ModScanner.Result> scannedMods = ModScanner.scan();
	public final VotedModsRule votedModsRule = new VotedModsRule();

	public void registerRules() {
		Holder.Reference<Rule> reference = Registry.registerForHolder(BuiltInRegistries.RULE,
			new ResourceLocation("modvote", "addmods"), votedModsRule);
		RulesAccessor.getBUILDER().add(reference, 10000000); // TODO: make more reasonable
	}

	private int ruleUpdateTimer = -1;
	public static final int UPDATE_DELAY = 20 * 10;

	public void queueModUpdate() {
		ruleUpdateTimer = UPDATE_DELAY;
	}

	public void levelTick(Level level) {
		if (!(level instanceof ServerLevel)) {
			if (!Minecraft.getInstance().isLocalServer()) {
				tick();
			}
		} else {
			tick();
		}
	}

	public void clientJoinSync(Set<String> approvedModIds) {
		// Check that all approved mods are loaded when joining the server
		// otherwise, a registry sync error could be encountered, stopping the join before the rules had a chance to sync
		for (String id : approvedModIds) {
			if (!QuiltLoader.isModLoaded(id)) {
				// TODO: a nicer dialog/error message?
				try {
					ConfigHandler.write(approvedModIds);
					ModVote.LOGGER.info("Restarting game to apply Mod Vote...");
					GracefulTerminator.gracefullyTerminate();
				} catch (IOException e) {
					ModVote.LOGGER.error("Failed to write configuration", e);
				}
				break;
			}
		}
	}

	public void tick() {
		if (ruleUpdateTimer > 0) {
			// Don't do anything in first tick (might still be receiving packets or something silly)
			if (ruleUpdateTimer < UPDATE_DELAY) {
				if (MinecraftQuiltLoader.getEnvironmentType() == EnvType.CLIENT) {
					BossEventUpdater.update(ruleUpdateTimer);
				}
				if (ruleUpdateTimer % 20 == 0) {
					Set<String> loadedModIds = scannedMods.stream()
						.filter(ModScanner.Result::loaded).map(ModScanner.Result::id).collect(Collectors.toSet());
					Set<String> enabledModIds = votedModsRule.getApprovedModIds();
					if (loadedModIds.equals(enabledModIds)) {
						ruleUpdateTimer = -1;
						return;
					} else {
						ModVote.LOGGER.info("Mod Vote enacted! Reloading mods in " + ruleUpdateTimer / 20 + " seconds...");
					}
				}
			}
			ruleUpdateTimer--;
		} else if (ruleUpdateTimer == 0) {
			ruleUpdateTimer = -1;

			// Apply rule updates!
			scannedMods = ModScanner.scan();
			Set<String> loadedModIds = scannedMods.stream()
				.filter(ModScanner.Result::loaded).map(ModScanner.Result::id).collect(Collectors.toSet());
			Set<String> approvedModIds = votedModsRule.getApprovedModIds();
			ModVote.LOGGER.info("Applying updates: " + loadedModIds + " -> " + approvedModIds);
			if (!loadedModIds.equals(approvedModIds)) {
				try {
					ConfigHandler.write(approvedModIds);
					ModVote.LOGGER.info("Restarting game to apply Mod Vote...");
					GracefulTerminator.gracefullyTerminate();
				} catch (IOException e) {
					ModVote.LOGGER.error("Failed to write configuration", e);
				}
			}
		} else if (ruleUpdateTimer == -1) {
			if (MinecraftQuiltLoader.getEnvironmentType() == EnvType.CLIENT) {
				BossEventUpdater.clear();
			}
		}
	}
}
