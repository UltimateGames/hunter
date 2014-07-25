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
