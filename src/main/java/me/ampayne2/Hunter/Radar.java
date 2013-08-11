package me.ampayne2.Hunter;

import java.util.List;

import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.enums.ArenaStatus;

import org.bukkit.scheduler.BukkitRunnable;

public class Radar extends BukkitRunnable{

	private UltimateGames ultimateGames;
	private Arena arena;
	private Hunter hunter;
	
	public Radar(UltimateGames ultimateGames, Arena arena, Hunter hunter) {
		this.ultimateGames = ultimateGames;
		this.arena = arena;
		this.hunter = hunter;
		this.runTaskLater(ultimateGames, 1L);
	}
	
	@Override
	public void run() {
		if (arena.getStatus() == ArenaStatus.RUNNING) {
			List<String> hunters = hunter.getHunters(arena);
			List<String> civilians = hunter.getCivilians(arena);
			if (hunters != null & !hunters.isEmpty() && civilians != null && !civilians.isEmpty()) {
				for (String hunterName : hunters) {
					ultimateGames.getUtils().radarScan(hunterName, civilians);
				}
				new Radar(ultimateGames, arena, hunter);
			}
		}
	}

}
