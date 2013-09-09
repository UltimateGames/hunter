package me.ampayne2.hunter;

import java.util.List;
import java.util.Random;

import me.ampayne2.hunter.classes.CivilianClass;
import me.ampayne2.hunter.classes.HunterClass;
import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.api.GamePlugin;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.arenas.SpawnPoint;
import me.ampayne2.ultimategames.classes.ClassManager;
import me.ampayne2.ultimategames.classes.GameClass;
import me.ampayne2.ultimategames.enums.ArenaStatus;
import me.ampayne2.ultimategames.games.Game;
import me.ampayne2.ultimategames.scoreboards.ArenaScoreboard;
import me.ampayne2.ultimategames.teams.Team;
import me.ampayne2.ultimategames.teams.TeamManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class Hunter extends GamePlugin {

    private UltimateGames ultimateGames;
    private Game game;
    private HunterClass hunter;
    private CivilianClass civilian;

    @Override
    public Boolean loadGame(UltimateGames ultimateGames, Game game) {
        this.ultimateGames = ultimateGames;
        this.game = game;
        ClassManager classManager = ultimateGames.getClassManager();
        hunter = new HunterClass(ultimateGames, game, "Hunter", false);
        civilian = new CivilianClass(ultimateGames, game, "Civilian", false);
        classManager.addGameClass(hunter);
        classManager.addGameClass(civilian);
        return true;
    }

    @Override
    public void unloadGame() {

    }

    @Override
    public Boolean reloadGame() {
        return true;
    }

    @Override
    public Boolean stopGame() {
        return true;
    }

    @Override
    public Boolean loadArena(Arena arena) {
        TeamManager teamManager = ultimateGames.getTeamManager();
        teamManager.addTeam(new Team(ultimateGames, arena, ChatColor.DARK_RED, "Hunter", false));
        teamManager.addTeam(new Team(ultimateGames, arena, ChatColor.GREEN, "Civilian", false));
        ultimateGames.addAPIHandler("/" + game.getName() + "/" + arena.getName(), new WebKillHandler(ultimateGames, arena));
        return true;
    }

    @Override
    public Boolean unloadArena(Arena arena) {
        ultimateGames.getTeamManager().removeTeamsOfArena(arena);
        return true;
    }

    @Override
    public Boolean isStartPossible(Arena arena) {
        return arena.getStatus() == ArenaStatus.OPEN;
    }

    @Override
    public Boolean startArena(Arena arena) {
        return true;
    }

    @Override
    public Boolean beginArena(Arena arena) {
        // Create the ending countdown
        ultimateGames.getCountdownManager().createEndingCountdown(arena, 300, true);

        ArenaScoreboard scoreBoard = ultimateGames.getScoreboardManager().createArenaScoreboard(arena, game.getName());

        // Unlock all spawnpoints
        for (SpawnPoint spawnPoint : ultimateGames.getSpawnpointManager().getSpawnPointsOfArena(arena)) {
            spawnPoint.lock(false);
        }

        // Picks a hunter, adds it to the scoreboard, sets its color to red, spawns the player, and sends it a message
        Random generator = new Random();
        String hunterName = arena.getPlayers().get(generator.nextInt(arena.getPlayers().size()));
        Player theHunter = Bukkit.getPlayerExact(hunterName);
        scoreBoard.addPlayer(theHunter);
        scoreBoard.setPlayerColor(theHunter, ChatColor.DARK_RED);
        TeamManager teamManager = ultimateGames.getTeamManager();
        teamManager.getTeam(arena, "Hunter").addPlayer(theHunter);
        ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0).teleportPlayer(theHunter);
        hunter.addPlayerToClass(theHunter);
        ultimateGames.getMessageManager().sendGameMessage(game, theHunter, "hunter");

        // Makes the rest of the players civilians, adding them to the scoreboard, setting their color to green, spawning the, and sending them a message.
        for (String playerName : arena.getPlayers()) {
            if (!playerName.equals(hunterName)) {
                Player player = Bukkit.getPlayerExact(playerName);
                scoreBoard.addPlayer(player);
                teamManager.getTeam(arena, "Civilian").addPlayer(player);
                ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1).teleportPlayer(player);
                civilian.addPlayerToClass(player);
                ultimateGames.getMessageManager().sendGameMessage(game, player, "civilian");
            }
        }

        // Set the score of Hunters and Civilians on the scoreboard
        scoreBoard.setScore(ChatColor.DARK_RED + "Hunters", 1);
        scoreBoard.setScore(ChatColor.GREEN + "Civilians", teamManager.getTeam(arena, "Civilian").getPlayers().size());
        scoreBoard.setVisible(true);

        // Starts the hunter radar
        new Radar(ultimateGames, arena);

        return true;
    }

    @Override
    public void endArena(Arena arena) {
        if (ultimateGames.getTeamManager().getTeam(arena, "Civilian").getPlayers().size() == 0) {
            ultimateGames.getMessageManager().broadcastGameMessage(game, "hunterswin");
        } else {
            ultimateGames.getMessageManager().broadcastGameMessage(game, "huntedwin");
        }
    }

    @Override
    public Boolean resetArena(Arena arena) {
        return true;
    }

    @Override
    public Boolean openArena(Arena arena) {
        return true;
    }

    @Override
    public Boolean stopArena(Arena arena) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Boolean addPlayer(Player player, Arena arena) {
        if (arena.getStatus() == ArenaStatus.OPEN || arena.getStatus() == ArenaStatus.STARTING) {
            if (arena.getStatus() == ArenaStatus.OPEN && arena.getPlayers().size() >= arena.getMinPlayers() && !ultimateGames.getCountdownManager().isStartingCountdownEnabled(arena)) {
                ultimateGames.getCountdownManager().createStartingCountdown(arena, 30);
            }
            SpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
            spawnPoint.lock(false);
            spawnPoint.teleportPlayer(player);
            player.setHealth(20.0);
            player.setFoodLevel(20);
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().addItem(ultimateGames.getUtils().createInstructionBook(game));
            player.updateInventory();
        }
        return true;
    }

    @Override
    public void removePlayer(Player player, Arena arena) {
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            String playerName = player.getName();
            TeamManager teamManager = ultimateGames.getTeamManager();
            Team team = teamManager.getPlayerTeam(playerName);
            if (team.getPlayers().size() == 1) {
                List<String> queuePlayer = ultimateGames.getQueueManager().getNextPlayers(1, arena);
                if (!queuePlayer.isEmpty()) {
                    String newPlayerName = queuePlayer.get(0);
                    Player newPlayer = Bukkit.getPlayerExact(newPlayerName);
                    ultimateGames.getPlayerManager().addPlayerToArena(newPlayer, arena, true);
                    team.addPlayer(newPlayer);
                    if (team.getName().equals("Hunter")) {
                        ultimateGames.getMessageManager().sendGameMessage(game, newPlayer, "hunter");
                        SpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0);
                        spawnPoint.lock(false);
                        spawnPoint.teleportPlayer(newPlayer);
                        hunter.addPlayerToClass(newPlayer, true);
                    } else if (team.getName().equals("Civilian")) {
                        ultimateGames.getMessageManager().sendGameMessage(game, newPlayer, "civilian");
                        SpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
                        spawnPoint.lock(false);
                        spawnPoint.teleportPlayer(player);
                        civilian.addPlayerToClass(newPlayer, true);
                    }
                } else {
                    ultimateGames.getArenaManager().endArena(arena);
                }
            }
            ArenaScoreboard scoreBoard = ultimateGames.getScoreboardManager().getArenaScoreboard(arena);
            if (scoreBoard != null) {
                scoreBoard.setScore(ChatColor.DARK_RED + "Hunters", teamManager.getTeam(arena, "Hunter").getPlayers().size());
                scoreBoard.setScore(ChatColor.GREEN + "Civilians", teamManager.getTeam(arena, "Civilian").getPlayers().size());
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Boolean addSpectator(Player player, Arena arena) {
        SpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
        spawnPoint.lock(false);
        spawnPoint.teleportPlayer(player);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().addItem(ultimateGames.getUtils().createInstructionBook(game));
        player.updateInventory();
        return true;
    }

    @Override
    public void removeSpectator(Player player, Arena arena) {

    }

    @Override
    public void onPlayerDeath(Arena arena, PlayerDeathEvent event) {
        Player player = event.getEntity();
        String playerName = player.getName();
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            event.getDrops().clear();
            TeamManager teamManager = ultimateGames.getTeamManager();
            Team civilians = teamManager.getTeam(arena, "Civilian");
            if (civilians.hasPlayer(playerName)) {
                civilians.removePlayer(playerName);
                Team hunters = teamManager.getTeam(arena, "Hunter");
                hunters.addPlayer(player);
                hunter.addPlayerToClass(player);
                ultimateGames.getMessageManager().sendGameMessage(game, player, "hunter");

                ArenaScoreboard scoreBoard = ultimateGames.getScoreboardManager().getArenaScoreboard(arena);
                if (scoreBoard != null) {
                    scoreBoard.setScore(ChatColor.DARK_RED + "Hunters", hunters.getPlayers().size());
                    scoreBoard.setScore(ChatColor.GREEN + "Civilians", civilians.getPlayers().size());
                }
                ultimateGames.getMessageManager().broadcastReplacedGameMessageToArena(game, arena, "killed", playerName);
                if (civilians.getPlayers().size() == 0) {
                    ultimateGames.getArenaManager().endArena(arena);
                }
            }
        } else {
            SpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
            spawnPoint.lock(false);
            spawnPoint.teleportPlayer(player);
        }
        ultimateGames.getUtils().autoRespawn(player);
    }

    @Override
    public void onPlayerRespawn(Arena arena, PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            Team civilians = ultimateGames.getTeamManager().getTeam(arena, "Civilian");
            if (civilians.getPlayers().size() > 0) {
                event.setRespawnLocation(ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0).getLocation());
                GameClass gameClass = ultimateGames.getClassManager().getPlayerClass(game, playerName);
                if (gameClass != null) {
                    gameClass.resetInventory(player);
                }
            }
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
            if (event.getDamager() instanceof Arrow) {
                event.setCancelled(true);
                player.setHealth((player.getHealth() - event.getDamage()) > 0.0 ? player.getHealth() - event.getDamage() : 0.0);
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

}
