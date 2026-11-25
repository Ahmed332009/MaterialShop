package com.banglaverse.materialshop.hook;

import com.banglaverse.materialshop.MaterialShop;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/** Small hook: a simulated break lets land-claim plugins veto shop creation. */
public final class ClaimHook {
    private final MaterialShop plugin;

    public ClaimHook(MaterialShop plugin) {
        this.plugin = plugin;
    }

    /** True when the player may modify this block according to other plugins. */
    public boolean canModify(Player player, Block block) {
        BlockBreakEvent simulated = new BlockBreakEvent(block, player);
        plugin.getServer().getPluginManager().callEvent(simulated);
        return !simulated.isCancelled();
    }
}
