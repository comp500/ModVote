package link.infra.modvote.rules;

import link.infra.modvote.GracefulTerminator;
import link.infra.modvote.ModVote;
import link.infra.modvote.data.ConfigHandler;
import link.infra.modvote.mixin.RulesAccessor;
import link.infra.modvote.plugin.VotedModsRule;
import link.infra.modvote.scan.ModScanner;
import link.infra.modvote.ui.BossEventUpdater;
import net.fabricmc.api.EnvType;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.voting.rules.Rule;
import org.quiltmc.loader.api.minecraft.MinecraftQuiltLoader;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RulesManager {
	public static final RulesManager INSTANCE = new RulesManager();

	public List<ModScanner.Result> scannedMods = ModScanner.scan();

	public void registerRules() {
		Holder.Reference<Rule> reference = Registry.registerForHolder(BuiltInRegistries.RULE,
			new ResourceLocation("modvote", "addmods"), new VotedModsRule());
		RulesAccessor.getBUILDER().add(reference, 10000000); // TODO: make more reasonable
	}

	private int ruleUpdateTimer = -1;
	public static final int UPDATE_DELAY = 20 * 10;
	// Always empty on start - server will sync/load loaded ids, then both sides will update their mod lists to match
	// with a janky artificial delay to ensure all mod updates are done at once
	private final Set<String> enabledModIds = new HashSet<>();

	public boolean pendingOrLoaded(String id) {
		return enabledModIds.contains(id);
	}

	public void queueModUpdate(String id, boolean enabled) {
		ruleUpdateTimer = UPDATE_DELAY;
		boolean idIsAvailable = scannedMods.stream().anyMatch(mod -> mod.id().equals(id));
		if (!idIsAvailable) {
			ModVote.LOGGER.warn("Failed to find voted mod " + id + " (do you have the same mods as the server?)");
			return;
		}

		if (enabled) {
			enabledModIds.add(id);
		} else {
			enabledModIds.remove(id);
		}
	}

	public void tick() {
		if (ruleUpdateTimer > 0) {
			// Don't do anything in first tick (might still be receiving packets or something silly)
			if (ruleUpdateTimer < UPDATE_DELAY) {
				if (MinecraftQuiltLoader.getEnvironmentType() == EnvType.CLIENT) {
					BossEventUpdater.update(ruleUpdateTimer);
				}
			}
			if (ruleUpdateTimer < UPDATE_DELAY && ruleUpdateTimer % 20 == 0) {
				Set<String> currentEnabledModIds = scannedMods.stream()
					.filter(ModScanner.Result::loaded).map(ModScanner.Result::id).collect(Collectors.toSet());
				if (currentEnabledModIds.equals(enabledModIds)) {
					ruleUpdateTimer = -1;
					return;
				} else {
					ModVote.LOGGER.info("Mod Vote enacted! Reloading mods in " + ruleUpdateTimer / 20 + " seconds...");
				}
			}
			ruleUpdateTimer--;
		} else if (ruleUpdateTimer == 0) {
			ruleUpdateTimer = -1;

			// Apply rule updates!
			scannedMods = ModScanner.scan();
			Set<String> currentEnabledModIds = scannedMods.stream()
				.filter(ModScanner.Result::loaded).map(ModScanner.Result::id).collect(Collectors.toSet());
			ModVote.LOGGER.info("Applying updates: " + currentEnabledModIds + " -> " + enabledModIds);
			if (!currentEnabledModIds.equals(enabledModIds)) {
				try {
					ConfigHandler.write(enabledModIds);
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
