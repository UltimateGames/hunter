/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2014, UltimateGames Staff <https://github.com/UltimateGames//>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.ampayne2.hunter;

import me.ampayne2.hunter.classes.CivilianClass;
import me.ampayne2.hunter.classes.HunterClass;
import me.ampayne2.ultimategames.api.UltimateGames;
import me.ampayne2.ultimategames.api.arenas.Arena;
import me.ampayne2.ultimategames.api.arenas.ArenaStatus;
import me.ampayne2.ultimategames.api.arenas.scoreboards.Scoreboard;
import me.ampayne2.ultimategames.api.arenas.spawnpoints.PlayerSpawnPoint;
import me.ampayne2.ultimategames.api.games.Game;
import me.ampayne2.ultimategames.api.games.GamePlugin;
import me.ampayne2.ultimategames.api.message.UGMessage;
import me.ampayne2.ultimategames.api.players.classes.GameClass;
import me.ampayne2.ultimategames.api.players.classes.GameClassManager;
import me.ampayne2.ultimategames.api.players.teams.Team;
import me.ampayne2.ultimategames.api.players.teams.TeamManager;
import me.ampayne2.ultimategames.api.players.trackers.compass.ClosestPlayerInTeamCompassTracker;
import me.ampayne2.ultimategames.api.utils.UGUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
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
        game.setMessages(HMessage.class);

        ultimateGames.getGameClassManager()
                .registerGameClass(new HunterClass(ultimateGames, game, false))
                .registerGameClass(new CivilianClass(ultimateGames, game, false));

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
        teamManager.createTeam(ultimateGames, "Hunters", arena, ChatColor.DARK_RED, false, true);
        teamManager.createTeam(ultimateGames, "Civilians", arena, ChatColor.GREEN, false, true);
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

        Scoreboard scoreBoard = ultimateGames.getScoreboardManager().createScoreboard(arena, game.getName());

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
        teamManager.setPlayerTeam(theHunter, hunters);
        ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0).teleportPlayer(theHunter);
        gameClassManager.getGameClass(game, "Hunter").addPlayer(theHunter, true, false);
        ultimateGames.getMessenger().sendGameMessage(theHunter, game, HMessage.HUNTER);

        // Makes the rest of the players civilians, adding them to the scoreboard, setting their color to green, spawning the, and sending them a message.
        Team civilians = teamManager.getTeam(arena, "Civilians");
        for (String playerName : arena.getPlayers()) {
            if (!playerName.equals(hunterName)) {
                Player player = Bukkit.getPlayerExact(playerName);
                scoreBoard.addPlayer(player);
                teamManager.setPlayerTeam(player, civilians);
                ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1).teleportPlayer(player);
                ultimateGames.getMessenger().sendGameMessage(player, game, HMessage.CIVILIAN);
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
            ultimateGames.getMessenger().sendGameMessage(Bukkit.getServer(), game, HMessage.WIN_HUNTERS);
            for (String player : ultimateGames.getTeamManager().getTeam(arena, "Civilians").getPlayers()) {
                ultimateGames.getPointManager().addPoint(game, player, "store", 10);
                ultimateGames.getPointManager().addPoint(game, player, "win", 1);
            }
        } else {
            ultimateGames.getMessenger().sendGameMessage(Bukkit.getServer(), game, HMessage.WIN_CIVILIANS);
            for (String player : ultimateGames.getTeamManager().getTeam(arena, "Hunters").getPlayers()) {
                ultimateGames.getPointManager().addPoint(game, player, "store", 10);
                ultimateGames.getPointManager().addPoint(game, player, "win", 1);
            }
        }
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
                    teamManager.setPlayerTeam(newPlayer, team);
                    if (team.getName().equals("Hunters")) {
                        ultimateGames.getMessenger().sendGameMessage(newPlayer, game, HMessage.HUNTER);
                        PlayerSpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getSpawnPoint(arena, 0);
                        spawnPoint.lock(false);
                        spawnPoint.teleportPlayer(newPlayer);
                        ultimateGames.getGameClassManager().getGameClass(game, "Hunter").addPlayer(newPlayer, true);
                    } else if (team.getName().equals("Civilians")) {
                        ultimateGames.getMessenger().sendGameMessage(newPlayer, game, HMessage.CIVILIAN);
                        PlayerSpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
                        spawnPoint.lock(false);
                        spawnPoint.teleportPlayer(player);
                        ultimateGames.getGameClassManager().getGameClass(game, "Civilian").addPlayer(newPlayer, true);
                    }
                } else {
                    ultimateGames.getArenaManager().endArena(arena);
                }
            }
            Scoreboard scoreBoard = ultimateGames.getScoreboardManager().getScoreboard(arena);
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
                civilians.removePlayer(player);
                Team hunters = teamManager.getTeam(arena, "Hunters");
                teamManager.setPlayerTeam(player, hunters);
                ultimateGames.getGameClassManager().getGameClass(game, "Hunter").addPlayer(player, true, false);
                ultimateGames.getMessenger().sendGameMessage(player, game, HMessage.HUNTER);
                new ClosestPlayerInTeamCompassTracker(ultimateGames, player, arena, civilians);

                Scoreboard scoreBoard = ultimateGames.getScoreboardManager().getScoreboard(arena);
                if (scoreBoard != null) {
                    scoreBoard.setScore(hunters, hunters.getPlayers().size());
                    scoreBoard.setScore(civilians, civilians.getPlayers().size());
                }
                ultimateGames.getMessenger().sendGameMessage(arena, game, HMessage.DEATH, playerName);
                if (civilians.getPlayers().size() == 0) {
                    ultimateGames.getArenaManager().endArena(arena);
                }
            }
        } else {
            PlayerSpawnPoint spawnPoint = ultimateGames.getSpawnpointManager().getRandomSpawnPoint(arena, 1);
            spawnPoint.lock(false);
            spawnPoint.teleportPlayer(player);
        }
        UGUtils.autoRespawn(ultimateGames.getPlugin(), player);
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

    @Override
    public void onArenaCommand(Arena arena, String command, CommandSender sender, String[] args) {
        if (arena.getStatus() == ArenaStatus.RUNNING && (command.equalsIgnoreCase("shout") || command.equalsIgnoreCase("s"))) {
            Player player = (Player) sender;
            String playerName = player.getName();
            ChatColor teamColor = ChatColor.WHITE;
            if (ultimateGames.getTeamManager().isPlayerInTeam(playerName)) {
                teamColor = ultimateGames.getTeamManager().getPlayerTeam(playerName).getColor();
            }
            StringBuilder message = new StringBuilder();
            for (String s : args) {
                message.append(s);
                message.append(" ");
            }
            ultimateGames.getMessenger().sendMessage(arena, UGMessage.CHAT, teamColor + playerName, message.toString());
        }
    }
}
