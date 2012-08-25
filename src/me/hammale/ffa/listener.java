package me.hammale.ffa;

import java.util.Map.Entry;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

import com.massivecraft.factions.P;

public class listener implements Listener
{
	public static FFA ffa = null;

	public listener(FFA plugin)
	{
		ffa = plugin;
	}
	
	@EventHandler
	public void onCommandPreprocess(PlayerCommandPreprocessEvent e){
		if(ffa.active.contains(e.getPlayer())){
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatColor.RED + "No commands while in FFA please!");
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e){
		if(e.getAction() == Action.RIGHT_CLICK_BLOCK){		
			if(e.getClickedBlock().getState() instanceof Sign){
				Sign sign = (Sign) e.getClickedBlock().getState();
				if(sign.getLine(0).contains("[FFA]")){
					e.getPlayer().sendMessage(ChatColor.GREEN
							+ "You are about to enter the arena, please wait " + ffa.tpDelay + " seconds");
					if (e.getPlayer().getGameMode() == GameMode.CREATIVE)
						e.getPlayer().sendMessage("Switching you to Survival!");
					e.getPlayer().setGameMode(GameMode.SURVIVAL);
					ffa.removeEffects(e.getPlayer());
					ffa.rTeleport(e.getPlayer());
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerTP(PlayerTeleportEvent e){
		if(ffa.ignore){
			return;
		}
		if(ffa.active.contains(e.getPlayer())){
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatColor.RED + "No tp'ing while in FFA please!");
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityDamage(EntityDamageEvent event)
	{
		if (event.isCancelled()) return;

		if (event instanceof EntityDamageByEntityEvent)
		{
			EntityDamageByEntityEvent sub = (EntityDamageByEntityEvent) event;
			
			if (sub.getEntity() instanceof Player)
			{
				Player attacker = null;
				if(sub.getDamager() instanceof Player)
					attacker = (Player) sub.getDamager();
				if(sub.getDamager() instanceof Arrow && ((Arrow) sub.getDamager()).getShooter() instanceof Player)
					attacker = (Player) ((Arrow) sub.getDamager()).getShooter();

				if(attacker != null)
				{
					if (ffa.saveItems.containsKey(attacker) && ffa.saveItems.containsKey((Player) sub.getEntity()))
					{
						return;
					}
				}
			}

			if (!P.p.entityListener.canDamagerHurtDamagee(sub, true))
			{
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event)
	{
		Entity e = event.getEntity();
		
		if ((e instanceof Player) && ((LivingEntity) e).getKiller() instanceof Player)
		{
			Player killer = ((Player) e).getKiller();
			if(ffa.playerKills.get(killer) == null){
				return;
			}
			event.getDrops().removeAll(event.getDrops());
			if (killer != null)
			{
				int x = (ffa.playerKills.get(killer)).intValue();
				int newx = x + 1;
				ffa.playerKills.remove(killer);
				ffa.playerKills.put(killer, Integer.valueOf(newx));
				if(newx == 3
						|| newx == 5
						|| newx == 10
						|| newx == 15
						|| newx == 20
						|| newx == 30
						|| newx == 50){
					ffa.getServer().broadcastMessage(ChatColor.GREEN + "[NexusFFA] " + killer.getName() + " is on a " + newx + " killstreak!");
				}
				killer.getInventory().addItem(
						new ItemStack[] { new ItemStack(Material.ROTTEN_FLESH,
								16) });
				if (newx == 1 && !killer.hasPermission("ffa.vip"))
				{
					killer.getInventory().getItem(0)
							.setType(Material.DIAMOND_SWORD);
					killer.getInventory().getItem(2)
							.setType(Material.DIAMOND_AXE);
				}
				else if (newx == 2)
				{
					killer.getInventory().addItem(
							new ItemStack[] { new ItemStack(Material.BOW) });
					killer.getInventory()
							.addItem(
									new ItemStack[] { new ItemStack(
											Material.ARROW, 32) });
				}
				else if(newx >= ffa.startKills){
					if(newx == ffa.startKills){
						killer.sendMessage(ChatColor.GREEN + "You have won " + ffa.initialAmnt + "!");
						ffa.getServer().dispatchCommand(
								ffa.getServer().getConsoleSender(),
								"eco give " + killer.getName() + " " + ffa.initialAmnt);
					}else{
						int amnt = ffa.initialAmnt+(ffa.increase*(newx-ffa.startKills));
						killer.sendMessage(ChatColor.GREEN + "You have won " + amnt + "!");
						ffa.getServer().dispatchCommand(
								ffa.getServer().getConsoleSender(),
								"eco give " + killer.getName() + " " + amnt);
					}
				}
				else if ((newx == 3) || (newx == 6) || (newx == 9)
						|| (newx == 12) || (newx == 15) || (newx == 18)
						|| (newx == 21) || (newx == 24))
				{
					killer.getInventory()
							.addItem(
									new ItemStack[] { new ItemStack(
											Material.ARROW, 32) });
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	@EventHandler
	public void respawn(PlayerRespawnEvent event)
	{
		Player p = event.getPlayer().getPlayer();
		if (ffa.saveItems.containsKey(p))
		{
			Location l = ffa.saveLocation.get(p);
			ffa.active.remove(event.getPlayer());
			p.teleport(l);
			ffa.saveLocation.remove(p);
			Entry<Player, ItemStack[]>[] entries = (Entry<Player, ItemStack[]>[]) ffa.saveItems.entrySet().toArray(new Entry[0]);
			for (int i = 0;i<ffa.saveItems.entrySet().size();i++)
			{
				Entry<Player, ItemStack[]> entry = entries[i];
				
				final Player pl = (Player) entry.getKey();
				ItemStack[] a = (ItemStack[]) entry.getValue();
				if (pl == p)
				{
				
				
					pl.getInventory().clear();
					pl.getInventory().setContents(a);
					ffa.saveItems.remove(pl);
					ffa.playerKills.remove(pl);
					ffa.saveArmor.get(event.getPlayer()).equip();
					pl.sendMessage(ChatColor.GREEN + "Your inventory has been restored!");				
				}
			}
		}
	}

	@EventHandler
	public void inventoryDrop(PlayerDropItemEvent event)
	{
		Player p = event.getPlayer();
		if (ffa.active.contains(p))
		{
			event.setCancelled(true);
			p.sendMessage(ChatColor.RED + "You are not allowed to drop items in FFA.");
		}
	}

	@SuppressWarnings("unchecked")
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onQuit(PlayerQuitEvent event)
	{
		if(ffa.active.contains(event.getPlayer())){	
			Player p = event.getPlayer();
			p.setHealth(20);
			Location l = ffa.saveLocation.get(p);
			ffa.active.remove(p);
			p.teleport(l);
			ffa.saveLocation.remove(p);
			if (ffa.saveItems.containsKey(p))
			{
				Entry<Player, ItemStack[]>[] entries = (Entry<Player, ItemStack[]>[]) ffa.saveItems.entrySet().toArray(new Entry[0]);
				for (int i = 0;i<ffa.saveItems.entrySet().size();i++)
				{
					Entry<Player, ItemStack[]> entry = entries[i];
					Player pl = (Player) entry.getKey();
					ItemStack[] a = (ItemStack[]) entry.getValue();
					if (pl == p)
					{
						pl.getInventory().clear();
						pl.getInventory().setBoots(new ItemStack(Material.AIR));
						pl.getInventory().setLeggings(new ItemStack(Material.AIR));
						pl.getInventory().setChestplate(new ItemStack(Material.AIR));
						pl.getInventory().setHelmet(new ItemStack(Material.AIR));
						pl.getInventory().setContents(a);
						ffa.saveItems.remove(pl);
						ffa.playerKills.remove(pl);
						ffa.saveArmor.get(event.getPlayer()).equip();
						pl.sendMessage(ChatColor.GREEN + "Your inventory has been restored!");					
					}
				}
			}			
		}
	}
}