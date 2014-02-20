package me.ampayne2.hunter.classes;

import me.ampayne2.ultimategames.api.UltimateGames;
import me.ampayne2.ultimategames.api.games.Game;
import me.ampayne2.ultimategames.api.players.classes.GameClass;
import me.ampayne2.ultimategames.api.utils.UGUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CivilianClass extends GameClass {
    private Game game;
    private static final ItemStack SWORD;
    private static final ItemStack APPLE = new ItemStack(Material.GOLDEN_APPLE);

    public CivilianClass(UltimateGames ultimateGames, Game game, boolean canSwitchToWithoutDeath) {
        super(ultimateGames, game, "Civilian", canSwitchToWithoutDeath);
        this.game = game;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void resetInventory(Player player) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().addItem(SWORD, APPLE, UGUtils.createInstructionBook(game));
        player.updateInventory();
    }

    static {
        SWORD = new ItemStack(Material.DIAMOND_SWORD);
        SWORD.addEnchantment(Enchantment.DAMAGE_ALL, 5);
    }
}
