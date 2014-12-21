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
package me.ampayne2.hunter.classes;

import me.ampayne2.ultimategames.api.UltimateGames;
import me.ampayne2.ultimategames.api.arenas.Arena;
import me.ampayne2.ultimategames.api.games.Game;
import me.ampayne2.ultimategames.api.players.classes.GameClass;
import me.ampayne2.ultimategames.api.utils.UGUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public class HunterClass extends GameClass {
    private UltimateGames ultimateGames;
    private Game game;
    private static final ItemStack BOW;
    private static final ItemStack ARROW = new ItemStack(Material.ARROW);
    private static final ItemStack COMPASS = new ItemStack(Material.COMPASS);

    public HunterClass(UltimateGames ultimateGames, Game game, boolean canSwitchToWithoutDeath) {
        super(ultimateGames, game, "Hunter", canSwitchToWithoutDeath);
        this.ultimateGames = ultimateGames;
        this.game = game;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void resetInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().addItem(BOW, ARROW, COMPASS, UGUtils.createInstructionBook(game));
        player.updateInventory();
        final String playerName = player.getName();
        Bukkit.getScheduler().scheduleSyncDelayedTask(ultimateGames.getPlugin(), new Runnable() {
            @Override
            public void run() {
                if (ultimateGames.getPlayerManager().isPlayerInArena(playerName)) {
                    Arena arena = ultimateGames.getPlayerManager().getPlayerArena(playerName);
                    if (arena.getGame().equals(game) && ultimateGames.getTeamManager().getTeam(arena, "Hunters").hasPlayer(playerName)) {
                        Player player = Bukkit.getPlayerExact(playerName);
                        UGUtils.removePotionEffect(player, PotionEffectType.SPEED);
                        UGUtils.removePotionEffect(player, PotionEffectType.JUMP);
                        UGUtils.increasePotionEffect(player, PotionEffectType.SPEED, 2);
                        UGUtils.increasePotionEffect(player, PotionEffectType.JUMP, 2);
                    }
                }
            }
        }, 40L);
    }

    static {
        BOW = new ItemStack(Material.BOW);
        BOW.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 10);
        BOW.addEnchantment(Enchantment.ARROW_INFINITE, 1);
        BOW.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
    }
}
