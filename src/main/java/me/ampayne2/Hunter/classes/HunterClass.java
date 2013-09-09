package me.ampayne2.hunter.classes;

import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.arenas.Arena;
import me.ampayne2.ultimategames.classes.GameClass;
import me.ampayne2.ultimategames.games.Game;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class HunterClass extends GameClass {
    
    private UltimateGames ultimateGames;
    private Game game;
    
    public HunterClass(UltimateGames ultimateGames, Game game, String name, boolean canSwitchToWithoutDeath) {
        super(ultimateGames, game, name, canSwitchToWithoutDeath);
        this.ultimateGames = ultimateGames;
        this.game = game;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void resetInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        ItemStack bow = new ItemStack(Material.BOW, 1);
        bow.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 10);
        bow.addEnchantment(Enchantment.ARROW_INFINITE, 1);
        bow.addUnsafeEnchantment(Enchantment.DURABILITY, 10);
        player.getInventory().addItem(bow, new ItemStack(Material.ARROW, 1), ultimateGames.getUtils().createInstructionBook(game));
        player.updateInventory();
        final String playerName = player.getName();
        Bukkit.getScheduler().scheduleSyncDelayedTask(ultimateGames, new Runnable() {
            @Override
            public void run() {
                if (ultimateGames.getPlayerManager().isPlayerInArena(playerName)) {
                    Arena arena = ultimateGames.getPlayerManager().getPlayerArena(playerName);
                    if (arena.getGame().equals(game) && ultimateGames.getTeamManager().getTeam(arena, "Hunter").hasPlayer(playerName)) {
                        Player player = Bukkit.getPlayerExact(playerName);
                        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 6000, 2));
                        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 6000, 2));
                    }
                }
            }
        }, 40L);
    }

}
