package me.hammale.ffa;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Armor {
	
	private Player player;
	private ItemStack helmet,chest,legs,boots;
	
	public Armor(Player p){
		this.player = p;
		this.helmet = p.getInventory().getHelmet();
		this.chest = p.getInventory().getChestplate();
		this.legs = p.getInventory().getLeggings();
		this.boots = p.getInventory().getBoots();	
	}

	public void equip(){
		player.getInventory().setHelmet(helmet);
		player.getInventory().setChestplate(chest);
		player.getInventory().setLeggings(legs);
		player.getInventory().setBoots(boots);
	}
	
}
