package com.thezorro266.simpleregionmarket.signs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.thezorro266.simpleregionmarket.SimpleRegionMarket;
import com.thezorro266.simpleregionmarket.TokenManager;
import com.thezorro266.simpleregionmarket.Utils;
import com.thezorro266.simpleregionmarket.handlers.LanguageHandler;

/**
 * @author theZorro266
 * 
 */
public abstract class TemplateMain {
	public String id = null;
	protected final SimpleRegionMarket plugin;
	protected final LanguageHandler langHandler;
	protected final TokenManager tokenManager;

	/**
	 * HashMap<Key:String, Value:Object>
	 */
	public HashMap<String, Object> tplOptions = new HashMap<String, Object>();

	/**
	 * HashMap<World:String, HashMap<Region:String, HashMap<Key:String, Value:Object>>>
	 */
	public HashMap<String, HashMap<String, HashMap<String, Object>>> entries = new HashMap<String, HashMap<String, HashMap<String, Object>>>();

	public TemplateMain(SimpleRegionMarket plugin, LanguageHandler langHandler, TokenManager tokenManager) {
		this.plugin = plugin;
		this.langHandler = langHandler;
		this.tokenManager = tokenManager;
	}

	public void load() {
		if (checkTemplate()) {
			final YamlConfiguration configHandle = YamlConfiguration.loadConfiguration(TokenManager.CONFIG_FILE);

			for (final String key : configHandle.getConfigurationSection(id).getKeys(true)) {
				tplOptions.put(key, configHandle.getConfigurationSection(id).get(key));
			}

			final YamlConfiguration tokenLoad = YamlConfiguration.loadConfiguration(new File(SimpleRegionMarket.getPluginDir() + "signs/" + id.toLowerCase()
					+ ".yml"));

			ConfigurationSection path;
			for (final String lWorld : tokenLoad.getKeys(false)) {
				path = tokenLoad.getConfigurationSection(lWorld);
				for (final String lRegion : path.getKeys(false)) {
					path = tokenLoad.getConfigurationSection(lWorld).getConfigurationSection(lRegion);
					for (final String lKey : path.getKeys(false)) {
						if (lKey.equals("signs")) {
							final ConfigurationSection pathSigns = path.getConfigurationSection("signs");
							final ArrayList<Location> signLocations = new ArrayList<Location>();
							for (final String lNr : pathSigns.getKeys(false)) {
								signLocations.add(new Location(Bukkit.getWorld(lWorld), pathSigns.getDouble(lNr + ".X"), pathSigns.getDouble(lNr + ".Y"),
										pathSigns.getDouble(lNr + ".Z")));
							}
							Utils.setEntry(this, lWorld, lRegion, lKey, signLocations);
						} else {
							Utils.setEntry(this, lWorld, lRegion, lKey, path.get(lKey));
						}
					}
				}
			}
		} else {
			Bukkit.getLogger().log(Level.SEVERE, "[SRM] Error loading templates.");
		}
	}

	public void save() {
		if (checkTemplate()) {
			final YamlConfiguration tokenSave = new YamlConfiguration();

			for (final String string : entries.keySet()) {
				final String sWorld = string;
				for (final String string2 : entries.get(sWorld).keySet()) {
					final String sRegion = string2;
					for (final String string3 : entries.get(sWorld).get(sRegion).keySet()) {
						final String sKey = string3;
						if (sKey.equals("signs")) {
							final ArrayList<Location> signLocations = Utils.getSignLocations(this, sWorld, sRegion);
							int counter = 0;
							for (final Location signLoc : signLocations) {
								tokenSave.set(sWorld + "." + sRegion + ".signs." + counter + ".X", signLoc.getX());
								tokenSave.set(sWorld + "." + sRegion + ".signs." + counter + ".Y", signLoc.getY());
								tokenSave.set(sWorld + "." + sRegion + ".signs." + counter + ".Z", signLoc.getZ());
								counter++;
							}
						} else {
							tokenSave.set(sWorld + "." + sRegion + "." + sKey, Utils.getEntry(this, sWorld, sRegion, sKey));
						}
					}
				}
			}

			try {
				tokenSave.save(new File(SimpleRegionMarket.getPluginDir() + "signs/" + id.toLowerCase() + ".yml"));
			} catch (final IOException e) {
				Bukkit.getLogger().log(Level.SEVERE, "[SRM] Error saving token " + id + ".");
				e.printStackTrace();
			}
		}
	}

	public boolean checkTemplate() {
		if (id != null) {
			return true;
		} else {
			Bukkit.getLogger().log(Level.SEVERE, "[SRM] Template misconfiguration.");
		}
		return false;
	}

	public Map<String, String> getReplacementMap(String world, String region) {
		if (checkTemplate()) {
			if (world != null && entries.containsKey(world) && region != null && entries.get(world).containsKey(region)) {
				final World worldWorld = Bukkit.getWorld(world);
				if (worldWorld != null) {
					final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(worldWorld, region);
					if (protectedRegion != null) {
						final HashMap<String, String> replacementMap = new HashMap<String, String>();
						replacementMap.put("id", id);
						replacementMap.put("id_out", Utils.getOptionString(this, "output.id"));
						replacementMap.put("id_taken", Utils.getOptionString(this, "taken.id"));
						replacementMap.put("world", world.toLowerCase());
						replacementMap.put("region", region.toLowerCase());
						if (SimpleRegionMarket.econManager.isEconomy() || Utils.getEntryDouble(this, world, region, "price") == 0) {
							replacementMap.put("price", "FREE");
						} else {
							replacementMap.put("price", SimpleRegionMarket.econManager.econFormat(Utils.getEntryDouble(this, world, region, "price")));
						}
						replacementMap.put("account", Utils.getEntryString(this, world, region, "account"));
						if (Utils.getEntry(this, world, region, "owner") == null || Utils.getEntryString(this, world, region, "owner").isEmpty()) {
							replacementMap.put("player", "No owner");
						} else {
							replacementMap.put("player", Utils.getEntryString(this, world, region, "owner"));
						}
						replacementMap.put("x", Integer.toString(Math.abs((int) protectedRegion.getMaximumPoint().getX()
								- (int) (protectedRegion.getMinimumPoint().getX() - 1))));
						replacementMap.put("y", Integer.toString(Math.abs((int) protectedRegion.getMaximumPoint().getY()
								- (int) (protectedRegion.getMinimumPoint().getY() - 1))));
						replacementMap.put("z", Integer.toString(Math.abs((int) protectedRegion.getMaximumPoint().getZ()
								- (int) (protectedRegion.getMinimumPoint().getZ() - 1))));

						return replacementMap;
					}
				}
			}
		}
		return null;
	}

	public boolean isRegionOwner(Player player, String world, String region) {
		if (Utils.getEntryString(this, world, region, "owner").equalsIgnoreCase(player.getName())) {
			return true;
		}
		return false;
	}

	public void ownerClicksTakenSign(String world, String region) {
		final Player owner = Bukkit.getPlayer(Utils.getEntryString(this, world, region, "owner"));
		langHandler.outputMessage(owner, "REGION_YOURS", null);
	}

	public void ownerClicksSign(Player player, String world, String region) {
		langHandler.outputMessage(player, "REGION_YOURS", null);
	}

	public void otherClicksTakenSign(Player player, String world, String region) {
		final ArrayList<String> args = new ArrayList<String>();
		args.add(Utils.getEntryString(this, world, region, "owner"));
		langHandler.outputMessage(player, "REGION_TAKEN_BY", args);
	}

	public void otherClicksSign(Player player, String world, String region) {
		if (SimpleRegionMarket.econManager.isEconomy()) {
			String account = Utils.getEntryString(this, world, region, "account");
			if (account.isEmpty()) {
				account = null;
			}
			final double price = Utils.getEntryDouble(this, world, region, "price");
			if (SimpleRegionMarket.econManager.moneyTransaction(player.getName(), account, price)) {
				if (account == null) {
					takeRegion(player, world, region);
				}
			}
		} else {
			takeRegion(player, world, region);
		}
	}

	public void takeRegion(Player newOwner, String world, String region) {
		final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(Bukkit.getWorld(world), region);

		if (Utils.getEntryBoolean(this, world, region, "taken")) {
			final Player oldOwner = Bukkit.getPlayer(Utils.getEntryString(this, world, region, "owner"));
			final ArrayList<String> list = new ArrayList<String>();
			list.add(region);
			list.add(newOwner.getName());
			langHandler.outputMessage(oldOwner, "REGION_TAKE", list);
			untakeRegion(world, region);
		} else {
			// Clear Members and Owners
			protectedRegion.setMembers(new DefaultDomain());
			protectedRegion.setOwners(new DefaultDomain());
		}

		if (Utils.getOptionString(this, "buyer").equalsIgnoreCase("member")) {
			protectedRegion.getMembers().addPlayer(SimpleRegionMarket.wgManager.wrapPlayer(newOwner));
		} else {
			if (!Utils.getOptionString(this, "buyer").equalsIgnoreCase("owner")) {
				langHandler.outputConsole(Level.WARNING, "The buyer state " + Utils.getOptionString(this, "buyer") + " is not known.");
			}
			protectedRegion.getMembers().addPlayer(SimpleRegionMarket.wgManager.wrapPlayer(newOwner));
		}
		
		if (Utils.getOptionBoolean(this, "removesigns")) {
			final ArrayList<Location> signLocations = Utils.getSignLocations(this, world, region);
			for (final Location sign : signLocations) {
				sign.getBlock().setType(Material.AIR);
			}
			Utils.setEntry(this, world, region, "signs", null);
		}

		Utils.setEntry(this, world, region, "taken", true);
		Utils.setEntry(this, world, region, "owner", newOwner.getName());

		final ArrayList<String> list = new ArrayList<String>();
		list.add(region);
		langHandler.outputMessage(newOwner, "REGION_TAKEN", list);

		tokenManager.updateSigns(this, world, region);
	}

	public void untakeRegion(String world, String region) {
		final ProtectedRegion protectedRegion = SimpleRegionMarket.wgManager.getProtectedRegion(Bukkit.getWorld(world), region);

		// Clear Members and Owners
		protectedRegion.setMembers(new DefaultDomain());
		protectedRegion.setOwners(new DefaultDomain());

		Utils.setEntry(this, world, region, "taken", false);
		Utils.removeEntry(this, world, region, "owner");

		tokenManager.updateSigns(this, world, region);
	}
	
	public boolean signCreated(Player player, String world, ProtectedRegion protectedRegion, Location signLocation, HashMap<String, String> input, String[] lines) {
		final String region = protectedRegion.getId();
		final ArrayList<Location> signLocations = Utils.getSignLocations(this, world, region);
		
		if (!entries.containsKey(world) || !entries.get(world).containsKey(region)) {
			double price;
			if (SimpleRegionMarket.econManager.isEconomy() && input.get("price") != null) {
				try {
					price = Double.parseDouble((String) input.get("price"));
				} catch (final Exception e) {
					langHandler.outputError(player, "ERR_NO_PRICE", null);
					return false;
				}
			} else {
				price = 0;
			}

			final double priceMin = Utils.getOptionDouble(this, "price.min");
			final double priceMax = Utils.getOptionDouble(this, "price.max");
			if (priceMin > price && (priceMax == -1 || price < priceMax)) {
				final ArrayList<String> list = new ArrayList<String>();
				list.add(String.valueOf(priceMin));
				list.add(String.valueOf(priceMax));
				langHandler.outputError(player, "ERR_PRICE_LIMIT", list);
				return false;
			}

			String account = player.getName();
			if (input.get("account") != null) {
				if (SimpleRegionMarket.permManager.isAdmin(player)) {
					if (((String) input.get("account")).equalsIgnoreCase("none")) {
						account = "";
					} else {
						account = (String) input.get("account");
					}
				}
			}

			Utils.setEntry(this, world, region, "price", price);
			Utils.setEntry(this, world, region, "account", account);
			Utils.setEntry(this, world, region, "taken", false);
			Utils.removeEntry(this, world, region, "owner");
		}
		
		signLocations.add(signLocation);
		Utils.setEntry(this, world, region, "signs", signLocations);

		tokenManager.updateSigns(this, world, region);
		return true;
	}

	public void schedule(String world, String region) {
		// nothing
	}
}
