package com.cavetale.fam.session;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import static com.cavetale.fam.FamPlugin.plugin;

public final class Sessions implements Listener {
    protected static Sessions instance;
    private final Map<UUID, Session> sessionsMap = new HashMap<>();

    public Sessions() {
        instance = this;
    }

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin());
        for (Player player : Bukkit.getOnlinePlayers()) {
            Session session = new Session(player);
            sessionsMap.put(session.uuid, session);
            session.enable();
        }
    }

    public void disable() {
        for (Session session : sessionsMap.values()) {
            session.disable();
        }
        sessionsMap.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private void onPlayerJoin(PlayerJoinEvent event) {
        Session session = new Session(event.getPlayer());
        sessionsMap.put(session.uuid, session);
        session.enable();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private void onPlayerQuit(PlayerQuitEvent event) {
        Session session = sessionsMap.remove(event.getPlayer().getUniqueId());
        if (session != null) session.disable();
    }


    public static Sessions sessions() {
        return instance;
    }

    public static Session sessionOf(Player player) {
        return instance.sessionsMap.get(player.getUniqueId());
    }

    public static Session sessionOf(UUID uuid) {
        return instance.sessionsMap.get(uuid);
    }
}
