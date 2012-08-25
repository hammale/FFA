package me.hammale.ffa;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Map.Entry;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;

public class FFA extends JavaPlugin
{
	public static FFA plugin;
	public final Logger logger = Logger.getLogger("Minecraft");
	FileConfiguration config;
	Random ran = new Random();	
	
	int tpDelay, increase, startKills, initialAmnt;
	String tpMessage;
	
	public HashMap<Player, Integer> wait = new HashMap<Player, Integer>();
	public HashMap<Player, ItemStack[]> saveItems = new HashMap<Player, ItemStack[]>();
	public HashMap<Player, Armor> saveArmor = new HashMap<Player, Armor>();
	public HashMap<Player, Location> saveLocation = new HashMap<Player, Location>();
	public HashMap<Player, Integer> playerKills = new HashMap<Player, Integer>();
	public HashSet<Player> active = new HashSet<Player>();
	
	public boolean ignore;

	public void onDisable()
	{
		PluginDescriptionFile pdfFile = getDescription();
		this.logger.info(pdfFile.getName() + " Has Been Disabled!");
		saveConfig();
	}

	public void onEnable()
	{
		PluginDescriptionFile pdfFile = getDescription();
		this.logger.info(pdfFile.getName() + ", Version "
				+ pdfFile.getVersion() + ", Has Been Enabled!");
		PluginManager pm = getServer().getPluginManager();
		EntityDamageEvent.getHandlerList().unregister(com.massivecraft.factions.P.p.entityListener);
		pm.registerEvents(new listener(this), this);
		plugin = this;
		config = getConfig();
		handleConfig();
	}
	
	public void handleConfig(){
		if (!exists()) {
			config = getConfig();
			config.options().copyDefaults(false);
			config.addDefault("Spawn", "1,1,1");
			config.addDefault("TPMessage", "&eWelcome to the arena!");
			config.addDefault("TPDelay", 5);
			config.addDefault("StartProfitAtKills", 4);
			config.addDefault("InitialProfit", 200);
			config.addDefault("KillProfitIncrease", 20);
			config.options().copyDefaults(true);
			saveConfig();
		}
		readConfig();
	}
	
	public String Colorize(String s) {
	    if (s == null) return null;
	    return s.replaceAll("&([0-9a-f])", "§$1");
	}
	
	public void readConfig(){
		initialAmnt = getConfig().getInt("InitialProfit");
		startKills = getConfig().getInt("StartProfitAtKills");
		increase = getConfig().getInt("KillProfitIncrease");
		tpDelay = getConfig().getInt("TPDelay");
		tpMessage = Colorize(getConfig().getString("TPMessage"));
	}
	
	private boolean exists() {
		try {
			File file = new File("plugins/FFA/config.yml");
			return file.exists();
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
		}
		return true;
	}
	
	public void rTeleport(final Player p)
	{
		int i = Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable()
				{

					public void run()
					{	
						if (plugin.wait.containsKey(p))
						{	
							plugin.saveItems.put(p, p.getInventory().getContents());
							plugin.saveArmor.put(p, new Armor(p));
							p.sendMessage(ChatColor.GREEN
									+ "Your inventory has been saved!");
							plugin.saveLocation.put(p, p.getLocation());
							plugin.wait.remove(p);
							Location l = null;
							String arenaloc = (String) getConfig().get("Spawn");
							String[] arenalocarr = arenaloc.split(",");
							if (arenalocarr.length == 3)
							{
								int x = Integer.parseInt(arenalocarr[0]);
								int y = Integer.parseInt(arenalocarr[1]);
								int z = Integer.parseInt(arenalocarr[2]);
								l = new Location(getServer().getWorld("world"),x, y, z);
							}						
							p.teleport(l);
							p.sendMessage(tpMessage);
							Inventory b = p.getInventory();
							b.clear();
							if(p.hasPermission("ffa.vip")){
								ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
								ItemStack food = new ItemStack(
										Material.ROTTEN_FLESH, 16);
								ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
								b.addItem(new ItemStack[] { sword, food, axe });
								p.getInventory().setChestplate(
										new ItemStack(Material.IRON_CHESTPLATE));
								p.getInventory().setLeggings(
										new ItemStack(Material.IRON_LEGGINGS));
								p.getInventory().setHelmet(
										new ItemStack(Material.IRON_HELMET));
								p.getInventory().setBoots(
										new ItemStack(Material.IRON_BOOTS));
							}else{
								ItemStack sword = new ItemStack(Material.IRON_SWORD);
								ItemStack food = new ItemStack(
										Material.ROTTEN_FLESH, 16);
								ItemStack axe = new ItemStack(Material.IRON_AXE);
								b.addItem(new ItemStack[] { sword, food, axe });
								p.getInventory().setChestplate(
										new ItemStack(Material.IRON_CHESTPLATE));
								p.getInventory().setLeggings(
										new ItemStack(Material.IRON_LEGGINGS));
								p.getInventory().setHelmet(
										new ItemStack(Material.IRON_HELMET));
								p.getInventory().setBoots(
										new ItemStack(Material.IRON_BOOTS));
							}
							plugin.playerKills.put(p, Integer.valueOf(0));
							active.add(p);
						}
					}
				}, tpDelay*20);
		
		plugin.wait.put(p, i);
	}

	public boolean onCommand(CommandSender sender, Command cmd,
			String commandLabel, String[] args)
	{
		if ((sender instanceof Player))
		{
			Player p = (Player) sender;
			if ((commandLabel.equalsIgnoreCase("ffa"))
					&& (p.hasPermission("FFA.default")))
			{
				if(!this.playerKills.containsKey(p))
				{
				if (args.length == 0)
				{
					p.sendMessage(ChatColor.GREEN
							+ "You are about to enter the arena, please wait " + tpDelay + " seconds");
					if (p.getGameMode() == GameMode.CREATIVE)
						p.sendMessage("Switching you to Survival!");
					p.setGameMode(GameMode.SURVIVAL);
					removeEffects(p);
					rTeleport(p);
				}
				else if (args.length == 1)
				{
					if ((args[0].equalsIgnoreCase("reload"))
							&& (p.hasPermission("FFA.*"))){
						reloadConfig();
					}else if ((args[0].equalsIgnoreCase("setspawn"))
								&& (p.hasPermission("FFA.*"))){
							Location loc = p.getLocation();
							int blockX = loc.getBlockX();
							int blockY = loc.getBlockY();
							int blockZ = loc.getBlockZ();
							getConfig().set("Spawn", blockX + "," + blockY + "," + blockZ);
							p.sendMessage(ChatColor.GOLD + "FFA Spawn has been set succesfully at: " + blockX
									+ ", " + blockY + ", " + blockZ);
							getServer().getPluginManager().disablePlugin(this);
							getServer().getPluginManager().enablePlugin(this);
					}else if ((args[0].equalsIgnoreCase("end"))
							&& (p.hasPermission("FFA.*"))){
						ignore = true;
						for(Player ap : active){
							if (saveItems.containsKey(ap))
							{
								ap.sendMessage(ChatColor.YELLOW + "FFA has been ended by an admin!");
								Location l = saveLocation.get(ap);
								ap.teleport(l);
								saveLocation.remove(ap);
								@SuppressWarnings("unchecked")
								Entry<Player, ItemStack[]>[] entries = (Entry<Player, ItemStack[]>[]) saveItems.entrySet().toArray(new Entry[0]);
								for (int i = 0;i<saveItems.entrySet().size();i++)
								{
									Entry<Player, ItemStack[]> entry = entries[i];
									
									final Player pl = (Player) entry.getKey();
									ItemStack[] a = (ItemStack[]) entry.getValue();
									if (pl == ap)
									{			
										pl.getInventory().clear();
										pl.getInventory().setContents(a);
										saveItems.remove(pl);
										playerKills.remove(pl);
										saveArmor.get(ap).equip();
										pl.sendMessage(ChatColor.GREEN + "Your inventory has been restored!");				
									}
								}
							}
						}
						active.clear();
						ignore = false;
					}

				}
				else
				{
					p.sendMessage(ChatColor.RED + "Invalid command!");
				}
				}
				else
					p.sendMessage(ChatColor.RED
							+ "You can't /ffa while in battle!");
			}
			else
				p.sendMessage(ChatColor.RED
						+ "You don't have permission for this command!");
		}

		return false;
	}

	public void removeEffects(Player p) {
		for (PotionEffect effect : p.getActivePotionEffects()) {
			p.removePotionEffect(effect.getType());
		}
	}
}