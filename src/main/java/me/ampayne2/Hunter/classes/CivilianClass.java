package me.ampayne2.hunter.classes;

import me.ampayne2.ultimategames.UltimateGames;
import me.ampayne2.ultimategames.classes.GameClass;
import me.ampayne2.ultimategames.games.Game;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CivilianClass extends GameClass {

    private UltimateGames ultimateGames;
    private Game game;

    public CivilianClass(UltimateGames ultimateGames, Game game, String name, boolean canSwitchToWithoutDeath) {
        super(ultimateGames, game, name, canSwitchToWithoutDeath);
        this.ultimateGames = ultimateGames;
        this.game = game;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void resetInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        ItemStack sword = new ItemStack(Material.DIAMOND_SWORD, 1);
        sword.addEnchantment(Enchantment.DAMAGE_ALL, 5);
        player.getInventory().addItem(sword, new ItemStack(Material.GOLDEN_APPLE, 1), ultimateGames.getUtils().createInstructionBook(game));
        player.updateInventory();
    }

}
