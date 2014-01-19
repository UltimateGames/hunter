package me.ampayne2.hunter;

import me.ampayne2.hunter.classes.CivilianClass;
import me.ampayne2.hunter.classes.HunterClass;
import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.api.GamePlugin;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.arenas.ArenaStatus;
import me.ampayne2.ultimategames.arenas.scoreboards.ArenaScoreboard;
import me.ampayne2.ultimategames.arenas.spawnpoints.PlayerSpawnPoint;
import me.ampayne2.ultimategames.games.Game;
import me.ampayne2.ultimategames.players.classes.GameClass;
import me.ampayne2.ultimategames.players.classes.GameClassManager;
import me.ampayne2.ultimategames.players.teams.Team;
import me.ampayne2.ultimategames.players.teams.TeamManager;
import me.ampayne2.ultimategames.players.trackers.compass.ClosestPlayerInTeamCompassTracker;
import me.ampayne2.ultimategames.utils.UGUtils;
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
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Random;

public class Hunter extends GamePlugin {
    private UltimateGames ultimateGames;
    private Game game;
    private static final Random RANDOM = new Random();

    @Override
    public boolean loadGame(UltimateGames ultimateGames, Game game) {
        this.ultimateGames = ultimateGames;
        this.game = game;

        ultimateGames.getGameClassManager()
                .registerGameClass(new HunterClass(ultimateGames, game, "Hunter", false))
                .registerGameClass(new CivilianClass(ultimateGames, game, "Civilian", false));

        return true;
    }

    @Override
    public void unloadGame() {

    }

    @Override
    public boolean reloadGame() {
        return true;
    }

    @Override
    public boolean stopGame() {
        return true;
    }

    @Override
    public boolean loadArena(Arena arena) {
        TeamManager teamManager = ultimateGames.getTeamManager();
        teamManager.addTeam(new Team(ultimateGames, "Hunters", arena, ChatColor.DARK_RED, false));
        teamManager.addTeam(new Team(ultimateGames, "Civilians", arena, ChatColor.GREEN, false));
        ultimateGames.addAPIHandler("/" + game.getName() + "/" + arena.getName(), new HunterWebHandler(ultimateGames, arena));
        return true;
    }

    @Override
    public boolean unloadArena(Arena arena) {
        ultimateGames.getTeamManager().removeTeamsOfArena(arena);
        return true;
    }

    @Override
    public boolean isStartPossible(Arena arena) {
        return arena.getStatus() == ArenaStatus.OPEN;
    }

    @Override
    public boolean startArena(Arena arena) {
        return true;
    }

    @Override
    public boolean beginArena(Arena arena) {
        // Create the ending countdown
        ultimateGames.getCountdownManager().createEndingCountdown(arena, ultimateGames.getConfigManager().getGameConfig(game).getInt("CustomValues.GameTime"), true);

        ArenaScoreboard scoreBoard = ultimateGames.getScoreboardManager().createScoreboard(arena, game.getName());

        // Unlock all spawnpoints
        for (PlayerSpawnPoint spawnPoint : ultimateGames.getSpawnpointManager().getSpawnPointsOfArena(arena)) {
            spawnPoint.lock(false);
        }

        // Picks a hunter, adds it to the scoreboard, sets its color to red, spawns the player, and sends it a message
        String hunterName = arena.getPlayers().get(RANDOM.nextInt(arena.getPlayers().size()));
        Player theHunter = Bukkit.getPlayerExact(hunterName);
        scoreBoard.addPlayer(theHunter);
        scoreBoard.setPlayerColor(theHunter, ChatColor.DARK_RED);
        TeamManager teamManager = ultimateGames.getTeamManager();
        GameClassManager gameClassManager = ultimateGames.getGameClassManager();
        Team hunters = teamManager.getTeam(arena, "Hunters");
        hunters.addPlayer(theHunter);
        ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0).teleportPlayer(theHunter);
        gameClassManager.getGameClass(game, "Hunter").addPlayer(theHunter);
        ultimateGames.getMessenger().sendGameMessage(theHunter, game, "hunter");

        // Makes the rest of the players civilians, adding them to the scoreboard, setting their color to green, spawning the, and sending them a message.
        Team civilians = teamManager.getTeam(arena, "Civilians");
        for (String playerName : arena.getPlayers()) {
            if (!playerName.equals(hunterName)) {
                Player player = Bukkit.getPlayerExact(playerName);
                scoreBoard.addPlayer(player);
                civilians.addPlayer(player);
                ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1).teleportPlayer(player);
                ultimateGames.getMessenger().sendGameMessage(player, game, "civilian");
            }
        }

        // Set the score of Hunters and Civilians on the scoreboard
        scoreBoard.setScore(hunters, 1);
        scoreBoard.setScore(civilians, civilians.getPlayers().size());
        scoreBoard.setVisible(true);

        // Starts the hunter radar
        new ClosestPlayerInTeamCompassTracker(ultimateGames, theHunter, arena, civilians);

        return true;
    }

    @Override
    public void endArena(Arena arena) {
        if (ultimateGames.getTeamManager().getTeam(arena, "Civilians").getPlayers().size() == 0) {
            ultimateGames.getMessenger().sendGameMessage(ultimateGames.getServer(), game, "hunterswin");
            for (String player : ultimateGames.getTeamManager().getTeam(arena, "Civilians").getPlayers()) {
                ultimateGames.getPointManager().addPoint(game, player, "store", 10);
                ultimateGames.getPointManager().addPoint(game, player, "win", 1);
            }
        } else {
            ultimateGames.getMessenger().sendGameMessage(ultimateGames.getServer(), game, "huntedwin");
            for (String player : ultimateGames.getTeamManager().getTeam(arena, "Hunters").getPlayers()) {
                ultimateGames.getPointManager().addPoint(game, player, "store", 10);
                ultimateGames.getPointManager().addPoint(game, player, "win", 1);
            }
        }
    }

    @Override
    public boolean resetArena(Arena arena) {
        return true;
    }

    @Override
    public boolean openArena(Arena arena) {
        return true;
    }

    @Override
    public boolean stopArena(Arena arena) {
        return true;
    }

    @Override
    public boolean addPlayer(Player player, Arena arena) {
        if (arena.getStatus() == ArenaStatus.OPEN || arena.getStatus() == ArenaStatus.STARTING) {
            if (arena.getStatus() == ArenaStatus.OPEN && arena.getPlayers().size() >= arena.getMinPlayers() && !ultimateGames.getCountdownManager().hasStartingCountdown(arena)) {
                ultimateGames.getCountdownManager().createStartingCountdown(arena, ultimateGames.getConfigManager().getGameConfig(game).getInt("CustomValues.StartWaitTime"));
            }
            PlayerSpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
            spawnPoint.lock(false);
            spawnPoint.teleportPlayer(player);
            for (PotionEffect potionEffect : player.getActivePotionEffects()) {
                player.removePotionEffect(potionEffect.getType());
            }
            player.setHealth(20.0);
            player.setFoodLevel(20);
            ultimateGames.getGameClassManager().getGameClass(game, "Civilian").addPlayer(player, true, false);
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
                    if (team.getName().equals("Hunters")) {
                        ultimateGames.getMessenger().sendGameMessage(newPlayer, game, "hunter");
                        PlayerSpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0);
                        spawnPoint.lock(false);
                        spawnPoint.teleportPlayer(newPlayer);
                        ultimateGames.getGameClassManager().getGameClass(game, "Hunter").addPlayer(newPlayer, true);
                    } else if (team.getName().equals("Civilians")) {
                        ultimateGames.getMessenger().sendGameMessage(newPlayer, game, "civilian");
                        PlayerSpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
                        spawnPoint.lock(false);
                        spawnPoint.teleportPlayer(player);
                        ultimateGames.getGameClassManager().getGameClass(game, "Civilian").addPlayer(newPlayer, true);
                    }
                } else {
                    ultimateGames.getArenaManager().endArena(arena);
                }
            }
            ArenaScoreboard scoreBoard = ultimateGames.getScoreboardManager().getScoreboard(arena);
            Team hunters = teamManager.getTeam(arena, "Hunters");
            Team civilians = teamManager.getTeam(arena, "Civilians");
            if (scoreBoard != null) {
                scoreBoard.setScore(hunters, hunters.getPlayers().size());
                scoreBoard.setScore(civilians, civilians.getPlayers().size());
            }
            if (hunters.getPlayers().size() <= 0 || civilians.getPlayers().size() <= 0) {
                ultimateGames.getArenaManager().endArena(arena);
            }
        }
        if (arena.getStatus() == ArenaStatus.STARTING && arena.getPlayers().size() < arena.getMinPlayers() && ultimateGames.getCountdownManager().hasStartingCountdown(arena)) {
            ultimateGames.getCountdownManager().stopStartingCountdown(arena);
            arena.setStatus(ArenaStatus.OPEN);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean addSpectator(Player player, Arena arena) {
        ultimateGames.getSpawnpointManager().getSpectatorSpawnPoint(arena).teleportPlayer(player);
        for (PotionEffect potionEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(potionEffect.getType());
        }
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getInventory().addItem(UGUtils.createInstructionBook(game));
        player.getInventory().setArmorContents(null);
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
            Team civilians = teamManager.getTeam(arena, "Civilians");
            if (civilians.hasPlayer(playerName)) {
                civilians.removePlayer(playerName);
                Team hunters = teamManager.getTeam(arena, "Hunters");
                hunters.addPlayer(player);
                ultimateGames.getGameClassManager().getGameClass(game, "Hunter").addPlayer(player);
                ultimateGames.getMessenger().sendGameMessage(player, game, "hunter");
                new ClosestPlayerInTeamCompassTracker(ultimateGames, player, arena, civilians);

                ArenaScoreboard scoreBoard = ultimateGames.getScoreboardManager().getScoreboard(arena);
                if (scoreBoard != null) {
                    scoreBoard.setScore(hunters, hunters.getPlayers().size());
                    scoreBoard.setScore(civilians, civilians.getPlayers().size());
                }
                ultimateGames.getMessenger().sendGameMessage(arena, game, "killed", playerName);
                if (civilians.getPlayers().size() == 0) {
                    ultimateGames.getArenaManager().endArena(arena);
                }
            }
        } else {
            PlayerSpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
            spawnPoint.lock(false);
            spawnPoint.teleportPlayer(player);
        }
        UGUtils.autoRespawn(player);
    }

    @Override
    public void onPlayerRespawn(Arena arena, PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();
        if (arena.getStatus() == ArenaStatus.RUNNING) {
            Team civilians = ultimateGames.getTeamManager().getTeam(arena, "Civilians");
            if (civilians.getPlayers().size() > 0) {
                event.setRespawnLocation(ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0).getLocation());
                GameClass gameClass = ultimateGames.getGameClassManager().getPlayerClass(game, playerName);
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
                ultimateGames.getPointManager().addPoint(game, ((Player) ((Arrow) event.getDamager()).getShooter()).getName(), "store", 1);
                ultimateGames.getPointManager().addPoint(game, ((Player) ((Arrow) event.getDamager()).getShooter()).getName(), "kill", 1);
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
