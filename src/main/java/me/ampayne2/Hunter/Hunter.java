package me.ampayne2.Hunter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import me.ampayne2.UltimateGames.UltimateGames;
import me.ampayne2.UltimateGames.API.ArenaScoreboard;
import me.ampayne2.UltimateGames.API.GamePlugin;
import me.ampayne2.UltimateGames.Arenas.Arena;
import me.ampayne2.UltimateGames.Enums.ArenaStatus;
import me.ampayne2.UltimateGames.Games.Game;
import me.ampayne2.UltimateGames.Players.SpawnPoint;

public class Hunter extends GamePlugin implements Listener {

	private UltimateGames ultimateGames;
	private Game game;
	private HashMap<Arena, ArrayList<String>> hunters = new HashMap<Arena, ArrayList<String>>();
	private HashMap<Arena, ArrayList<String>> civilians = new HashMap<Arena, ArrayList<String>>();

	public Boolean loadGame(UltimateGames ultimateGames, Game game) {
		this.ultimateGames = ultimateGames;
		this.game = game;
		return true;
	}

	public Boolean unloadGame() {
		return true;
	}

	public Boolean stopGame() {
		return true;
	}

	public Boolean loadArena(Arena arena) {
		return true;
	}

	public Boolean unloadArena(Arena arena) {
		return true;
	}

	public Boolean isStartPossible(Arena arena) {
		if (arena.getStatus() == ArenaStatus.OPEN) {
			return true;
		} else {
			return false;
		}
	}

	public Boolean startArena(Arena arena) {
		return true;
	}

	public Boolean beginArena(Arena arena) {
		ultimateGames.getCountdownManager().createEndingCountdown(arena, 300, true);

		if (!hunters.isEmpty() && hunters.containsKey(arena)) {
			hunters.remove(arena);
		}
		if (!civilians.isEmpty() && civilians.containsKey(arena)) {
			civilians.remove(arena);
		}
		
		for (SpawnPoint spawnPoint : ultimateGames.getSpawnpointManager().getSpawnPointsOfArena(arena)) {
			spawnPoint.lock(false);
		}

		ArrayList<String> hunter = new ArrayList<String>();
		Random generator = new Random();
		String hunterName = arena.getPlayers().get(generator.nextInt(arena.getPlayers().size()));
		Player theHunter = Bukkit.getPlayer(hunterName);
		hunter.add(hunterName);
		hunters.put(arena, hunter);
		ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0).teleportPlayer(hunterName);
		theHunter.getInventory().clear();
		equipHunter(theHunter);
		theHunter.setHealth(20.0);
		theHunter.setFoodLevel(20);
		ultimateGames.getMessageManager().sendGameMessage(game, hunterName, "starthunter");

		ArrayList<String> civilian = new ArrayList<String>();
		for (String playerName : arena.getPlayers()) {
			if (!playerName.equals(hunterName)) {
				civilian.add(playerName);
				ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1).teleportPlayer(playerName);
				Player aCivilian = Bukkit.getPlayer(playerName);
				aCivilian.getInventory().clear();
				equipCivilian(aCivilian);
				aCivilian.setHealth(20.0);
				aCivilian.setFoodLevel(20);
				ultimateGames.getMessageManager().sendGameMessage(game, playerName, "starthunted");
			}
		}
		civilians.put(arena, civilian);
		
		new Radar(ultimateGames, arena, this);
		
		for (ArenaScoreboard scoreBoard : ultimateGames.getScoreboardManager().getArenaScoreboards(arena)) {
			ultimateGames.getScoreboardManager().removeArenaScoreboard(arena, scoreBoard.getName());
		}
		ArenaScoreboard scoreBoard = ultimateGames.getScoreboardManager().createArenaScoreboard(arena, game.getGameDescription().getName());
		for (String playerName : arena.getPlayers()) {
			scoreBoard.addPlayer(playerName);
		}
		scoreBoard.setScore(ChatColor.DARK_RED + "Hunters", 1);
		scoreBoard.setScore(ChatColor.GREEN + "Civilians", civilians.get(arena).size());
		scoreBoard.setVisible(true);
		
		return true;
	}

	public Boolean endArena(Arena arena) {
		if (ultimateGames.getCountdownManager().isStartingCountdownEnabled(arena)) {
			ultimateGames.getCountdownManager().stopStartingCountdown(arena);
		}
		if (ultimateGames.getCountdownManager().isEndingCountdownEnabled(arena)) {
			ultimateGames.getCountdownManager().stopEndingCountdown(arena);
		}
		if (hunters.get(arena).size() > 0 && civilians.get(arena).size() == 0) {
			ultimateGames.getMessageManager().broadcastGameMessageToArena(game, arena, "hunterswin");
		} else {
			ultimateGames.getMessageManager().broadcastGameMessageToArena(game, arena, "huntedwin");
		}
		if (hunters.get(arena).size() > 0) {
			ArrayList<String> removeHunters = new ArrayList<String>();
			for (String hunterName : hunters.get(arena)) {
				removeHunters.add(hunterName);
			}
			for (String hunterName : removeHunters) {
				Bukkit.getPlayer(hunterName).getInventory().clear();
				ultimateGames.getPlayerManager().removePlayerFromArena(hunterName, arena, false);
			}
		}
		if (civilians.get(arena).size() > 0) {
			ArrayList<String> removeCivilians = new ArrayList<String>();
			for (String civilianName : civilians.get(arena)) {
				removeCivilians.add(civilianName);
			}
			for (String civilianName : removeCivilians) {
				Bukkit.getPlayer(civilianName).getInventory().clear();
				ultimateGames.getPlayerManager().removePlayerFromArena(civilianName, arena, false);
			}
		}
		ultimateGames.getScoreboardManager().removeArenaScoreboard(arena, game.getGameDescription().getName());
		ultimateGames.getArenaManager().openArena(arena);
		return true;
	}

	public Boolean resetArena(Arena arena) {
		return true;
	}

	public Boolean openArena(Arena arena) {
		return true;
	}

	public Boolean stopArena(Arena arena) {
		return true;
	}

	@SuppressWarnings("deprecation")
	public Boolean addPlayer(Arena arena, String playerName) {
		if (arena.getPlayers().size() >= arena.getMinPlayers() && !ultimateGames.getCountdownManager().isStartingCountdownEnabled(arena) && arena.getStatus() == ArenaStatus.OPEN) {
			ultimateGames.getCountdownManager().createStartingCountdown(arena, 30);
		}
		SpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
		spawnPoint.lock(false);
		spawnPoint.teleportPlayer(playerName);
		Player player = Bukkit.getPlayer(playerName);
		player.getInventory().addItem(ultimateGames.getUtils().createInstructionBook(arena.getGame()));
		player.updateInventory();
		return true;
	}

	public Boolean removePlayer(Arena arena, String playerName) {
		Player player = Bukkit.getPlayer(playerName);
		player.setLevel(0);
		if (player.hasPotionEffect(PotionEffectType.SPEED)) {
			player.removePotionEffect(PotionEffectType.SPEED);
		}
		if (player.hasPotionEffect(PotionEffectType.JUMP)) {
			player.removePotionEffect(PotionEffectType.JUMP);
		}
		if (hunters.containsKey(arena) && hunters.get(arena).contains(playerName)) {
			hunters.get(arena).remove(playerName);
			if (hunters.get(arena).size() == 0) {
				ultimateGames.getCountdownManager().stopEndingCountdown(arena);
				ultimateGames.getArenaManager().endArena(arena);
			}
		} else if (civilians.containsKey(arena) && civilians.get(arena).contains(playerName)) {
			civilians.get(arena).remove(playerName);
			if (civilians.get(arena).size() == 0) {
				ultimateGames.getCountdownManager().stopEndingCountdown(arena);
				ultimateGames.getArenaManager().endArena(arena);
			}
		}
		for (ArenaScoreboard scoreBoard : ultimateGames.getScoreboardManager().getArenaScoreboards(arena)) {
			if (scoreBoard.getName().equals(game.getGameDescription())) {
				scoreBoard.setScore(ChatColor.DARK_RED + "Hunters", hunters.get(arena).size());
				scoreBoard.setScore(ChatColor.GREEN + "Civilians", civilians.get(arena).size());
			}
		}
		if (arena.getPlayers().size() < arena.getMinPlayers() && ultimateGames.getCountdownManager().isStartingCountdownEnabled(arena)) {
			ultimateGames.getCountdownManager().stopStartingCountdown(arena);
		}
		return true;
	}

	public Boolean onArenaCommand(Arena arena, String command, CommandSender sender, String[] args) {
		return true;
	}

	public void handleInputSignCreate(Arena arena, Sign sign, String label) {

	}

	public void handleInputSignClick(Arena arena, Sign sign, String label, PlayerInteractEvent event) {

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		Player damaged = (Player) event.getEntity();
		Player damager;
		if (event.getDamager() instanceof Player) {
			damager = (Player) event.getDamager();
		} else if (event.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow) event.getDamager();
			if (arrow.getShooter() instanceof Player) {
				damager = (Player) arrow.getShooter();
			} else {
				return;
			}
		} else {
			return;
		}
		String damagerName = damager.getName();
		String damagedName = damaged.getName();
		if (!ultimateGames.getPlayerManager().isPlayerInArena(damagerName).equals(ultimateGames.getPlayerManager().isPlayerInArena(damagedName))) {
			event.setCancelled(true);
			return;
		} else if (!ultimateGames.getPlayerManager().isPlayerInArena(damagerName)) {
			return;
		}
		Arena damagerArena = ultimateGames.getPlayerManager().getPlayerArena(damagerName);
		Arena damagedArena = ultimateGames.getPlayerManager().getPlayerArena(damagedName);
		if (!damagerArena.equals(damagedArena)) {
			return;
		} else if (!hunters.containsKey(damagerArena) || !hunters.containsKey(damagerArena)) {
			return;
		} else if (damagerArena.getStatus() != ArenaStatus.RUNNING) {
			event.setCancelled(true);
			return;
		} else if (isPlayerHunter(damagerArena, damagerName) && isPlayerCivilian(damagerArena, damagedName)) {
			if (event.getDamager() instanceof Arrow) {
				event.setCancelled(true);
				damaged.setHealth((damaged.getHealth() - event.getDamage()) > 0.0 ? damaged.getHealth() - event.getDamage() : 0.0);
			}
			return;
		} else if (isPlayerCivilian(damagerArena, damagerName) && isPlayerHunter(damagerArena, damagedName)) {
			if (event.getDamager() instanceof Arrow) {
				event.setCancelled(true);
				damaged.setHealth((damaged.getHealth() - event.getDamage()) > 0.0 ? damaged.getHealth() - event.getDamage() : 0.0);
			}
			return;
		} else {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDamage(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			String playerName = player.getName();
			if (ultimateGames.getPlayerManager().isPlayerInArena(playerName) && ultimateGames.getPlayerManager().getPlayerArena(playerName).getStatus() != ArenaStatus.RUNNING) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		if (ultimateGames.getPlayerManager().isPlayerInArena(playerName)) {
			Arena arena = ultimateGames.getPlayerManager().getPlayerArena(playerName);
			if (arena.getGame().equals(game) && arena.getStatus() == ArenaStatus.RUNNING) {
				event.setRespawnLocation(ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0).getLocation());
				equipHunter(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDeath(PlayerDeathEvent event) {
		final Player player = event.getEntity();
		String playerName = player.getName();
		if (ultimateGames.getPlayerManager().isPlayerInArena(playerName)) {
			Arena arena = ultimateGames.getPlayerManager().getPlayerArena(playerName);
			if (arena.getGame().equals(game)) {
				if (arena.getStatus() == ArenaStatus.RUNNING) {
					event.getDrops().clear();
					if (civilians.containsKey(arena) && civilians.get(arena).contains(playerName)) {
						civilians.get(arena).remove(playerName);
						if (hunters.containsKey(arena)) {
							hunters.get(arena).add(playerName);
							ultimateGames.getMessageManager().sendGameMessage(game, playerName, "hunter");
						}
						if (civilians.get(arena).size() == 0) {
							ultimateGames.getArenaManager().endArena(arena);
						}
						for (ArenaScoreboard scoreBoard : ultimateGames.getScoreboardManager().getArenaScoreboards(arena)) {
							if (scoreBoard.getName().equals(game.getGameDescription())) {
								scoreBoard.setScore(ChatColor.DARK_RED + "Hunters", hunters.get(arena).size());
								scoreBoard.setScore(ChatColor.GREEN + "Civilians", civilians.get(arena).size());
							}
						}
						ultimateGames.getMessageManager().broadcastReplacedGameMessageToArena(game, arena, "killed", playerName);
					}
				} else {
					SpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
					spawnPoint.lock(false);
					spawnPoint.teleportPlayer(playerName);
				}
				ultimateGames.getUtils().autoRespawn(player);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPickupItem(PlayerPickupItemEvent event) {
		String playerName = event.getPlayer().getName();
		if (ultimateGames.getPlayerManager().isPlayerInArena(playerName)) {
			Arena arena = ultimateGames.getPlayerManager().getPlayerArena(playerName);
			if (arena.getGame().equals(game)) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onDropItem(PlayerDropItemEvent event) {
		String playerName = event.getPlayer().getName();
		if (ultimateGames.getPlayerManager().isPlayerInArena(playerName)) {
			Arena arena = ultimateGames.getPlayerManager().getPlayerArena(playerName);
			if (arena.getGame().equals(game)) {
				event.setCancelled(true);
			}
		}
	}
	
	public ArrayList<String> getHunters(Arena arena) {
		if (hunters.containsKey(arena)) {
			return hunters.get(arena);
		} else {
			return null;
		}
	}
	
	public ArrayList<String> getCivilians(Arena arena) {
		if (civilians.containsKey(arena)) {
			return civilians.get(arena);
		} else {
			return null;
		}
	}
	
	public Boolean isPlayerHunter(Arena arena, String playerName) {
		if (hunters.containsKey(arena) && hunters.get(arena).contains(playerName)) {
			return true;
		} else {
			return false;
		}
	}
	
	public Boolean isPlayerCivilian(Arena arena, String playerName) {
		if (civilians.containsKey(arena) && civilians.get(arena).contains(playerName)) {
			return true;
		} else {
			return false;
		}
	}
	
	public void equipHunter(final Player player) {
		ItemStack bow = new ItemStack(Material.BOW, 1);
		bow.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 10);
		bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
		bow.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
		ItemStack arrow = new ItemStack(Material.ARROW, 1);
		ItemStack food = new ItemStack(Material.COOKED_BEEF, 8);
		player.getInventory().addItem(bow, arrow, food);
		Bukkit.getScheduler().scheduleSyncDelayedTask(ultimateGames, new Runnable() {
			@Override
			public void run() {
				player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6000, 2));
				player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 6000, 2));
			}
		}, 40L);
	}
	
	public void equipCivilian(Player player) {
		ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);
		sword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
		ItemStack apple = new ItemStack(Material.GOLDEN_APPLE, 1);
		ItemStack food = new ItemStack(Material.COOKED_BEEF, 8);
		player.getInventory().addItem(sword, apple, food);
	}

}
