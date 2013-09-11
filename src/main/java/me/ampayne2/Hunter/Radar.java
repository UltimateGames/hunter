package me.ampayne2.hunter;

import java.util.List;

import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.enums.ArenaStatus;

import org.bukkit.scheduler.BukkitRunnable;

public class Radar extends BukkitRunnable {

    private UltimateGames ultimateGames;
    private Arena arena;

    public Radar(UltimateGames ultimateGames, Arena arena) {
        this.ultimateGames = ultimateGames;
        this.arena = arena;
        this.runTaskLater(ultimateGames, 1L);
    }

    @Override
    public void run() {
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            List<String> hunters = ultimateGames.getTeamManager().getTeam(arena, "Hunter").getPlayers();
            List<String> civilians = ultimateGames.getTeamManager().getTeam(arena, "Civilian").getPlayers();
            if (hunters != null & !hunters.isEmpty() && civilians != null && !civilians.isEmpty()) {
                for (String hunterName : hunters) {
                    ultimateGames.getUtils().radarScan(hunterName, civilians);
                }
                new Radar(ultimateGames, arena);
            }
        }
    }

}
