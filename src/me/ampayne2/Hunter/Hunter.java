package me.ampayne2.Hunter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import net.minecraft.server.v1_6_R1.Packet205ClientCommand;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_6_R1.entity.CraftPlayer;
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
import me.ampayne2.UltimateGames.API.GamePlugin;
import me.ampayne2.UltimateGames.Arenas.Arena;
import me.ampayne2.UltimateGames.Enums.ArenaStatus;
import me.ampayne2.UltimateGames.Games.Game;
import me.ampayne2.UltimateGames.Players.SpawnPoint;

public class Hunter extends GamePlugin implements Listener{
	
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
		return true;
	}
	
	public Boolean startArena(Arena arena) {
		return true;
	}
	
	public Boolean beginArena(Arena arena) {
		//create countdown
		ultimateGames.getCountdownManager().createEndingCountdown(arena, 300, true);
		
		//reset hunters and civilians list for the arena
		if (!hunters.isEmpty() && hunters.containsKey(arena)) {
			hunters.remove(arena);
		}
		if (!civilians.isEmpty() && civilians.containsKey(arena)) {
			civilians.remove(arena);
		}
		
		//get a random person and make them a hunter
		Random generator = new Random();
		String hunterName = arena.getPlayers().get(generator.nextInt(arena.getPlayers().size()));
		ArrayList<String> hunter = new ArrayList<String>();
		hunter.add(hunterName);
		hunters.put(arena, hunter);
		ultimateGames.getMessageManager().sendGameMessage(game, hunterName, "starthunter");
		
		//get everyone else and make them civilians
		ArrayList<String> civilian = new ArrayList<String>();
		for (String playerName : arena.getPlayers()) {
			if (!playerName.equals(hunterName)) {
				civilian.add(playerName);
				HashMap<String, String> replace2 = new HashMap<String, String>();
				replace2.put(" ", " ");
				ultimateGames.getMessageManager().sendGameMessage(game, playerName, "starthunted");
			}
		}
		civilians.put(arena, civilian);
		
		//unlock all spawnpoints
		for (SpawnPoint spawnPoint : ultimateGames.getSpawnpointManager().getSpawnPointsOfArena(arena)) {
			spawnPoint.lock(false);
		}
		
		//teleport hunter to spawnpoint 0, and civilians to random spawnpoints
		ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0).teleportPlayer(hunterName);
		Player theHunter = Bukkit.getPlayer(hunterName);
		for (String civilianName : civilians.get(arena)) {
			ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1).teleportPlayer(civilianName);
		}
		
		//make hunter a ghost if not already
		/*
		if (!ultimateGames.getGhostFactory().isGhost(theHunter)) {
			ultimateGames.getGhostFactory().addGhost(theHunter);
		}
		*/
		
		//give hunter speed 2, jump boost 2, 1 arrow, and power 5 infinity 1 unbreaking 10 bow, and some food
		theHunter.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6000, 2));
		theHunter.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 6000, 2));
		ItemStack bow = new ItemStack(Material.BOW, 1);
		bow.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 10);
		bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
		bow.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
		ItemStack arrow = new ItemStack(Material.ARROW, 1);
		ItemStack food = new ItemStack(Material.COOKED_BEEF, 8);
		theHunter.getInventory().clear();
		theHunter.getInventory().addItem(bow, arrow, food);
		theHunter.setHealth(20.0);
		theHunter.setFoodLevel(20);
		
		
		//give civilians diamond sword with sharpness 5, 1 golden apple, and some food
		for (String civilianName : civilians.get(arena)) {
			Player aCivilian = Bukkit.getPlayer(civilianName);
			ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);
			sword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
			ItemStack apple = new ItemStack(Material.GOLDEN_APPLE, 1);
			aCivilian.getInventory().clear();
			aCivilian.getInventory().addItem(sword, apple, food);
			aCivilian.setHealth(20.0);
			aCivilian.setFoodLevel(20);
		}
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
				ultimateGames.getPlayerManager().removePlayerFromArena(hunterName, arena, false);
				Bukkit.getPlayer(hunterName).getInventory().clear();
				/*
				if (ultimateGames.getGhostFactory().isGhost(player)) {
					ultimateGames.getGhostFactory().removeGhost(player);
				}
				*/
			}
		}
		if (civilians.get(arena).size() > 0) {
			ArrayList<String> removeCivilians = new ArrayList<String>();
			for (String civilianName : civilians.get(arena)) {
				removeCivilians.add(civilianName);
			}
			for (String civilianName : removeCivilians) {
				ultimateGames.getPlayerManager().removePlayerFromArena(civilianName, arena, false);
				Bukkit.getPlayer(civilianName).getInventory().clear();
			}
		}
		arena.setStatus(ArenaStatus.OPEN);
		ultimateGames.getUGSignManager().updateLobbySignsOfArena(arena);
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
		if (player.hasPotionEffect(PotionEffectType.SPEED)) {
			player.removePotionEffect(PotionEffectType.SPEED);
		}
		if (player.hasPotionEffect(PotionEffectType.JUMP)) {
			player.removePotionEffect(PotionEffectType.JUMP);
		}
		player.setLevel(0);
		/*
		if (ultimateGames.getGhostFactory().isGhost(player)) {
			ultimateGames.getGhostFactory().removeGhost(player);
		}
		*/
		if (hunters.containsKey(arena) && hunters.get(arena).contains(playerName)) {
			hunters.get(arena).remove(playerName);
			if (hunters.get(arena).size() == 0) {
				endArena(arena);
				ultimateGames.getCountdownManager().stopEndingCountdown(arena);
			}
		} else if (civilians.containsKey(arena) && civilians.get(arena).contains(playerName)) {
			civilians.get(arena).remove(playerName);
			if (civilians.get(arena).size() == 0) {
				endArena(arena);
				ultimateGames.getCountdownManager().stopEndingCountdown(arena);
			}
		}
		if (arena.getPlayers().size() < arena.getMinPlayers() && ultimateGames.getCountdownManager().isStartingCountdownEnabled(arena)) {
			ultimateGames.getCountdownManager().stopStartingCountdown(arena);
		}
		return true;
	}
	
	public void onGameCommand(String command, CommandSender sender, String[] args) {
		
	}
	
	public void onArenaCommand(Arena arena, String command, CommandSender sender, String[] args) {
		
	}
	
	public void handleInputSignCreate(Arena arena, Sign sign, String label) {
		
	}
	
	public void handleInputSignClick(Arena arena, Sign sign, String label, PlayerInteractEvent event) {
		
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerDamageByPlayer(EntityDamageByEntityEvent event) {
		Player damager;
		Player damaged;
		if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
			damager = (Player) event.getDamager();
			damaged = (Player) event.getEntity();
		} else if (event.getDamager() instanceof Arrow && event.getEntity() instanceof Player) {
			Arrow arrow = (Arrow) event.getDamager();
			if (arrow.getShooter() instanceof Player) {
				damager = (Player) arrow.getShooter();
				damaged = (Player) event.getEntity();
			} else {
				return;
			}
		} else {
			return;
		}
		String damagerName = damager.getName();
		String damagedName = damaged.getName();
		if (ultimateGames.getPlayerManager().isPlayerInArena(damagerName) && !ultimateGames.getPlayerManager().isPlayerInArena(damagedName)) {
			event.setCancelled(true);
			return;
		} else if (!ultimateGames.getPlayerManager().isPlayerInArena(damagerName) && ultimateGames.getPlayerManager().isPlayerInArena(damagedName)) {
			event.setCancelled(true);
			return;
		} else if (!ultimateGames.getPlayerManager().isPlayerInArena(damagerName) && !ultimateGames.getPlayerManager().isPlayerInArena(damagerName)) {
			return;
		}
		Arena damagerArena = ultimateGames.getPlayerManager().getPlayerArena(damagerName);
		Arena damagedArena = ultimateGames.getPlayerManager().getPlayerArena(damagedName);
		if (!damagerArena.equals(damagedArena) || !damagerArena.getGame().equals(game)) {
			return;
		} else if (damagerArena.getStatus() != ArenaStatus.RUNNING) {
			event.setCancelled(true);
			return;
		} else if (hunters.containsKey(damagerArena) && hunters.get(damagerArena).contains(damagerName) && hunters.get(damagerArena).contains(damagedName)) {
			event.setCancelled(true);
			return;
		} else if (civilians.containsKey(damagerArena) && civilians.get(damagerArena).contains(damagerName) && civilians.get(damagerArena).contains(damagedName)) {
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
		final Player player = event.getPlayer();
		String playerName = player.getName();
		if (ultimateGames.getPlayerManager().isPlayerInArena(playerName)) {
			Arena arena = ultimateGames.getPlayerManager().getPlayerArena(playerName);
			if (arena.getGame().equals(game) && arena.getStatus() == ArenaStatus.RUNNING) {
				event.setRespawnLocation(ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0).getLocation());
				/*
				if (!ultimateGames.getGhostFactory().isGhost(player)) {
					ultimateGames.getGhostFactory().addGhost(player);
				}
				*/
				ItemStack bow = new ItemStack(Material.BOW, 1);
				bow.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 10);
				bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
				bow.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
				ItemStack arrow = new ItemStack(Material.ARROW, 1);
				ItemStack food = new ItemStack(Material.COOKED_BEEF, 8);
				player.getInventory().addItem(bow, arrow, food);
				//new Equip(player.getName()).runTaskLater(ultimateGames, 5L);
				
                Bukkit.getScheduler().scheduleSyncDelayedTask(ultimateGames, new Runnable() {
                    @Override
                    public void run() {
                		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6000, 2));
                		player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 6000, 2));
                    }
                }, 40L);
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
							endArena(arena);
						}
						
					}
				} else {
					SpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
					spawnPoint.lock(false);
					spawnPoint.teleportPlayer(playerName);
				}
                Bukkit.getScheduler().scheduleSyncDelayedTask(ultimateGames, new Runnable() {
                    @Override
                    public void run() {
                        Packet205ClientCommand packet = new Packet205ClientCommand();
                        packet.a = 1;
                        ((CraftPlayer) player).getHandle().playerConnection.a(packet);
                    }
                }, 1L);

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

}
