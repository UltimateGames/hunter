package me.ampayne2.Hunter;

import java.util.ArrayList;

import me.ampayne2.UltimateGames.UltimateGames;
import me.ampayne2.UltimateGames.Arenas.Arena;
import me.ampayne2.UltimateGames.Enums.ArenaStatus;

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
			ArrayList<String> hunters = hunter.getHunters(arena);
			ArrayList<String> civilians = hunter.getCivilians(arena);
			if (hunters != null & !hunters.isEmpty() && civilians != null && !civilians.isEmpty()) {
				for (String hunterName : hunters) {
					ultimateGames.getUtils().radarScan(hunterName, civilians);
				}
				new Radar(ultimateGames, arena, hunter);
			}
		}
	}

}
