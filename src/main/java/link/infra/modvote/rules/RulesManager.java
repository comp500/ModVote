package link.infra.modvote.rules;

import link.infra.modvote.ModVote;
import link.infra.modvote.mixin.RulesAccessor;
import link.infra.modvote.plugin.VotedModsRule;
import link.infra.modvote.scan.ModScanner;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.voting.rules.Rule;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RulesManager {
	public static final RulesManager INSTANCE = new RulesManager();

	public final List<ModScanner.ModScanResult> scannedMods = ModScanner.scan();

	public void registerRules() {
		Holder.Reference<Rule> reference = Registry.registerForHolder(BuiltInRegistries.RULE,
			new ResourceLocation("modvote", "addmods"), new VotedModsRule());
		RulesAccessor.getBUILDER().add(reference, 10000000);
	}

	public int ruleUpdateTimer = -1;
	// Always empty on start - server will sync/load enabled ids, then both sides will update their mod lists to match
	// with a janky artificial delay to ensure all mod updates are done at once
	private final Set<String> enabledModIds = new HashSet<>();

	public void queueModUpdate(String id, boolean enabled) {
		ruleUpdateTimer = 20 * 10;
		if (enabled) {
			enabledModIds.add(id);
		} else {
			enabledModIds.remove(id);
		}
	}

	public void tick() {
		if (ruleUpdateTimer > 0) {
			ruleUpdateTimer--;
		} else if (ruleUpdateTimer == 0) {
			// Apply rule updates!
			ruleUpdateTimer = -1;
			// TODO: write config file to enable/disable mods
			ModVote.queueGameRestart();
		}
	}
}
