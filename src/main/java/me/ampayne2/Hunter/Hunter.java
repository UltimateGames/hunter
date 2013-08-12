package me.ampayne2.Hunter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.api.GamePlugin;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.arenas.SpawnPoint;
import me.ampayne2.ultimategames.enums.ArenaStatus;
import me.ampayne2.ultimategames.games.Game;
import me.ampayne2.ultimategames.scoreboards.ArenaScoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class Hunter extends GamePlugin {

    private UltimateGames ultimateGames;
    private Game game;
    private Map<Arena, List<String>> hunters = new HashMap<Arena, List<String>>();
    private Map<Arena, List<String>> civilians = new HashMap<Arena, List<String>>();

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
        hunters.put(arena, new ArrayList<String>());
        civilians.put(arena, new ArrayList<String>());
        ultimateGames.addAPIHandler("/" + game.getGameDescription().getName() + "/" + arena.getName(), new WebKillHandler(this, arena));
        return true;
    }

    public Boolean unloadArena(Arena arena) {
        hunters.remove(arena);
        civilians.remove(arena);
        return true;
    }

    public Boolean isStartPossible(Arena arena) {
        return arena.getStatus() == ArenaStatus.OPEN;
    }

    public Boolean startArena(Arena arena) {
        return true;
    }

    public Boolean beginArena(Arena arena) {
        // Create the ending countdown
        ultimateGames.getCountdownManager().createEndingCountdown(arena, 300, true);
        
        ArenaScoreboard scoreBoard = ultimateGames.getScoreboardManager().createArenaScoreboard(arena, game.getGameDescription().getName());

        // Unlock all spawnpoints
        for (SpawnPoint spawnPoint : ultimateGames.getSpawnpointManager().getSpawnPointsOfArena(arena)) {
            spawnPoint.lock(false);
        }

        // Picks a hunter, adds it to the scoreboard, sets its color to red, spawns the player, and sends it a message
        List<String> hunter = new ArrayList<String>();
        Random generator = new Random();
        String hunterName = arena.getPlayers().get(generator.nextInt(arena.getPlayers().size()));
        scoreBoard.addPlayer(hunterName);
        scoreBoard.setPlayerColor(hunterName, ChatColor.DARK_RED);
        Player theHunter = Bukkit.getPlayerExact(hunterName);
        hunter.add(hunterName);
        hunters.put(arena, hunter);
        ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0).teleportPlayer(hunterName);
        equipHunter(theHunter);
        ultimateGames.getMessageManager().sendGameMessage(game, hunterName, "starthunter");

        // Makes the rest of the players civilians, adding them to the scoreboard, setting their color to green, spawning the, and sending them a message.
        List<String> civilian = new ArrayList<String>();
        for (String playerName : arena.getPlayers()) {
            if (!playerName.equals(hunterName)) {
                scoreBoard.addPlayer(playerName);
                scoreBoard.setPlayerColor(playerName, ChatColor.GREEN);
                civilian.add(playerName);
                ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1).teleportPlayer(playerName);
                equipCivilian(Bukkit.getPlayerExact(playerName));
                ultimateGames.getMessageManager().sendGameMessage(game, playerName, "starthunted");
            }
        }
        civilians.put(arena, civilian);

        // Set the score of Hunters and Civilians on the scoreboard
        scoreBoard.setScore(ChatColor.DARK_RED + "Hunters", 1);
        scoreBoard.setScore(ChatColor.GREEN + "Civilians", civilians.get(arena).size());
        scoreBoard.setVisible(true);

        // Starts the hunter radar
        new Radar(ultimateGames, arena, this);

        return true;
    }

    public void endArena(Arena arena) {
        if (civilians.get(arena).size() == 0) {
            ultimateGames.getMessageManager().broadcastGameMessage(game, "hunterswin");
        } else {
            ultimateGames.getMessageManager().broadcastGameMessage(game, "huntedwin");
        }
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
        // Starts the arena if there's enough players
        if (arena.getStatus() == ArenaStatus.OPEN && arena.getPlayers().size() >= arena.getMinPlayers() && !ultimateGames.getCountdownManager().isStartingCountdownEnabled(arena)) {
            ultimateGames.getCountdownManager().createStartingCountdown(arena, 30);
        }
        // Teleports the player to a random spawnpoint, gives them instructions, and replenishes their food/health
        SpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
        spawnPoint.lock(false);
        spawnPoint.teleportPlayer(playerName);
        Player player = Bukkit.getPlayerExact(playerName);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().addItem(ultimateGames.getUtils().createInstructionBook(game));
        player.updateInventory();
        return true;
    }

    public Boolean removePlayer(Arena arena, String playerName) {
        // Detects if there are 0 hunters or civilians left and if so ends the arena
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            if (hunters.containsKey(arena) && hunters.get(arena).contains(playerName)) {
                hunters.get(arena).remove(playerName);
                if (hunters.get(arena).size() == 0) {
                    ultimateGames.getArenaManager().endArena(arena);
                }
            } else if (civilians.containsKey(arena) && civilians.get(arena).contains(playerName)) {
                civilians.get(arena).remove(playerName);
                if (civilians.get(arena).size() == 0) {
                    ultimateGames.getArenaManager().endArena(arena);
                }
            }
            // Updates the arena scoreboard
            for (ArenaScoreboard scoreBoard : ultimateGames.getScoreboardManager().getArenaScoreboards(arena)) {
                if (scoreBoard.getName().equals(game.getGameDescription())) {
                    scoreBoard.setScore(ChatColor.DARK_RED + "Hunters", hunters.get(arena).size());
                    scoreBoard.setScore(ChatColor.GREEN + "Civilians", civilians.get(arena).size());
                }
            }
        }
        return true;
    }

    @Override
    public void onPlayerDeath(Arena arena, PlayerDeathEvent event) {
        Player player = event.getEntity();
        String playerName = player.getName();
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            event.getDrops().clear();
            if (isPlayerCivilian(arena, playerName)) {
                civilians.get(arena).remove(playerName);
                hunters.get(arena).add(playerName);
                if (civilians.get(arena).size() == 0) {
                    ultimateGames.getArenaManager().endArena(arena);
                } else {
                    ultimateGames.getMessageManager().sendGameMessage(game, playerName, "hunter");
                    for (ArenaScoreboard scoreBoard : ultimateGames.getScoreboardManager().getArenaScoreboards(arena)) {
                        if (scoreBoard.getName().equals(game.getGameDescription().getName())) {
                            scoreBoard.setScore(ChatColor.DARK_RED + "Hunters", hunters.get(arena).size());
                            scoreBoard.setScore(ChatColor.GREEN + "Civilians", civilians.get(arena).size());
                            scoreBoard.setPlayerColor(playerName, ChatColor.DARK_RED);
                        }
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

    @Override
    public void onPlayerRespawn(Arena arena, PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            event.setRespawnLocation(ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0).getLocation());
            equipHunter(player);
        }
    }

    @Override
    public void onEntityDamage(Arena arena, EntityDamageEvent event) {
        if (arena.getStatus() != ArenaStatus.RUNNING) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onEntityDamageByEntity(Arena arena, EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Player damager = null;
            if (event.getDamager() instanceof Player) {
                damager = (Player) event.getDamager();
            } else if (event.getDamager() instanceof Arrow && ((Arrow) event.getDamager()).getShooter() instanceof Player) {
                damager = (Player) ((Arrow) event.getDamager()).getShooter();
            } else {
                return;
            }
            String playerName = player.getName();
            String damagerName = damager.getName();
            if (!ultimateGames.getPlayerManager().isPlayerInArena(damagerName) || !ultimateGames.getPlayerManager().getPlayerArena(damagerName).equals(arena)) {
                event.setCancelled(true);
                return;
            }
            if (isPlayerHunter(arena, damagerName) && isPlayerCivilian(arena, playerName)) {
                if (event.getDamager() instanceof Arrow) {
                    event.setCancelled(true);
                    player.setHealth((player.getHealth() - event.getDamage()) > 0.0 ? player.getHealth() - event.getDamage() : 0.0);
                }
                return;
            }
        }
    }

    @Override
    public void onPlayerFoodLevelChange(Arena arena, FoodLevelChangeEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onItemPickup(Arena arena, PlayerPickupItemEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onItemDrop(Arena arena, PlayerDropItemEvent event) {
        event.setCancelled(true);
    }

    public List<String> getHunters(Arena arena) {
        if (hunters.containsKey(arena)) {
            return hunters.get(arena);
        } else {
            return new ArrayList<String>();
        }
    }

    public List<String> getCivilians(Arena arena) {
        if (civilians.containsKey(arena)) {
            return civilians.get(arena);
        } else {
            return new ArrayList<String>();
        }
    }

    public Boolean isPlayerHunter(Arena arena, String playerName) {
        return hunters.containsKey(arena) && hunters.get(arena).contains(playerName);
    }

    public Boolean isPlayerCivilian(Arena arena, String playerName) {
        return civilians.containsKey(arena) && civilians.get(arena).contains(playerName);
    }

    public void equipHunter(final Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        ItemStack bow = new ItemStack(Material.BOW, 1);
        bow.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 10);
        bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
        bow.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
        player.getInventory().addItem(bow, new ItemStack(Material.ARROW, 1), ultimateGames.getUtils().createInstructionBook(game));
        Bukkit.getScheduler().scheduleSyncDelayedTask(ultimateGames, new Runnable() {
            @Override
            public void run() {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6000, 2));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 6000, 2));
            }
        }, 40L);
    }

    public void equipCivilian(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);
        sword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
        player.getInventory().addItem(sword, new ItemStack(Material.GOLDEN_APPLE, 1), ultimateGames.getUtils().createInstructionBook(game));
    }

}
