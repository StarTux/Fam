package com.cavetale.fam.core;

import com.cavetale.core.skin.PlayerSkin;
import com.cavetale.core.skin.PlayerSkinProvider;
import com.cavetale.fam.FamPlugin;
import com.cavetale.fam.sql.SQLPlayerSkin;
import com.cavetale.fam.sql.SQLProfile;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import static com.cavetale.fam.FamPlugin.plugin;

public final class CorePlayerSkinProvider implements PlayerSkinProvider {
    @Override
    public FamPlugin getPlugin() {
        return plugin();
    }

    @Override
    public PlayerSkin getPlayerSkin(UUID uuid) {
        final SQLProfile profile = plugin().getDatabase().find(SQLProfile.class)
            .eq("uuid", uuid)
            .findUnique();
        if (profile == null) return null;
        return plugin().getDatabase().find(SQLPlayerSkin.class)
            .eq("textureUrl", profile.getTextureUrl())
            .findUnique();
    }

    @Override
    public void getPlayerSkinAsync(final UUID uuid, final Consumer<PlayerSkin> callback) {
        plugin().getDatabase().scheduleAsyncTask(() -> {
                final PlayerSkin playerSkin = getPlayerSkin(uuid);
                Bukkit.getScheduler().runTask(plugin(), () -> callback.accept(playerSkin));
            });
    }
}
