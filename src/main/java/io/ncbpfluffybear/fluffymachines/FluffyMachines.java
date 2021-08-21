package io.ncbpfluffybear.fluffymachines;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.ncbpfluffybear.fluffymachines.utils.Constants;
import io.ncbpfluffybear.fluffymachines.utils.Events;
import io.ncbpfluffybear.fluffymachines.utils.FluffyItems;
import io.ncbpfluffybear.fluffymachines.utils.GlowEnchant;
import io.ncbpfluffybear.fluffymachines.utils.McMMOEvents;
import io.ncbpfluffybear.fluffymachines.utils.Utils;
import lombok.SneakyThrows;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.cscorelib2.collections.Pair;
import me.mrCookieSlime.Slimefun.cscorelib2.config.Config;
import me.mrCookieSlime.Slimefun.cscorelib2.updater.GitHubBuildsUpdater;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.RayTraceResult;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;

public class FluffyMachines extends JavaPlugin implements SlimefunAddon {

    private static FluffyMachines instance;
    public static final HashMap<ItemStack, List<Pair<ItemStack, List<RecipeChoice>>>> shapedVanillaRecipes = new HashMap<>();
    public static final HashMap<ItemStack, List<Pair<ItemStack, List<RecipeChoice>>>> shapelessVanillaRecipes =
        new HashMap<>();

    @SneakyThrows
    @Override
    public void onEnable() {
        instance = this;
        // Read something from your config.yml
        Config cfg = new Config(this);

        if (cfg.getBoolean("options.auto-update") && getDescription().getVersion().startsWith("DEV - ")) {
            new GitHubBuildsUpdater(this, getFile(), "NCBPFluffyBear/FluffyMachines/master/").start();
        }

        // Register Glow

        try {
            if (!Enchantment.isAcceptingRegistrations()) {
                Field accepting = Enchantment.class.getDeclaredField("acceptingNew");
                accepting.setAccessible(true);
                accepting.set(null, true);
            }
        } catch (IllegalAccessException | NoSuchFieldException ignored) {
            getLogger().warning("Failed to register enchantment.");
        }

        registerGlow();

        // Register ACT Recipes
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe r = recipeIterator.next();

            if (r instanceof ShapedRecipe) {
                ShapedRecipe sr = (ShapedRecipe) r;
                List<RecipeChoice> rc = new ArrayList<>();
                ItemStack key = new ItemStack(sr.getResult().getType(), 1);

                // Convert the recipe to a list
                for (Map.Entry<Character, RecipeChoice> choice : sr.getChoiceMap().entrySet()) {
                    if (choice.getValue() != null) {
                        rc.add(choice.getValue());
                    }
                }

                if (!shapedVanillaRecipes.containsKey(key)) {
                    shapedVanillaRecipes.put(key,
                        new ArrayList<>(Collections.singletonList(new Pair<>(sr.getResult(), rc))));
                }else {
                    shapedVanillaRecipes.get(key).add(new Pair<>(sr.getResult(), rc));
                }

            } else if (r instanceof ShapelessRecipe) {
                ShapelessRecipe slr = (ShapelessRecipe) r;
                ItemStack key = new ItemStack(slr.getResult().getType(), 1);

                // Key has a list of recipe options
                if (!shapelessVanillaRecipes.containsKey(key)) {
                    shapelessVanillaRecipes.put(key,
                        new ArrayList<>(Collections.singletonList(new Pair<>(slr.getResult(), slr.getChoiceList()))));
                } else {
                    shapelessVanillaRecipes.get(key).add(new Pair<>(slr.getResult(), slr.getChoiceList()));
                }
            }
        }

        // Register McMMO Events
        if (getServer().getPluginManager().isPluginEnabled("McMMO")) {
            Bukkit.getLogger().log(Level.INFO, "McMMO found!");
            getServer().getPluginManager().registerEvents(new McMMOEvents(), this);
        }

        // Get Slimefun Numerical Version
        try {
            Matcher matcher = Constants.VERSION_PATTERN.matcher(Constants.SLIMEFUN_VERSION);
            if (matcher.find()) {
                int parsedVersion = Integer.parseInt(matcher.group(2));
                if (parsedVersion < 844) {
                    getLogger().log(Level.INFO, ChatColor.YELLOW + "您运行的是DEV 844之前的Slimefun版本. " +
                        "FluffyMachines要求您更新Slimefun版本，以便让储存箱子保持正常工作. " +
                        "请更换210415后续版本，否则玩家可能会遇到蓬松机器的各种问题." +
                        "如果您未更改，作者将不负责蓬松机器.");
                } else {
                    Constants.SLIMEFUN_UPDATED = true;
                }
            } else {
                getLogger().log(Level.INFO, ChatColor.YELLOW + "您运行的是RC版本的Slimefun版本." +
                    "FluffyMachines要求您更新Slimefun版本，以便让储存箱子保持正常工作." +
                    "请更换210415后续版本，否则玩家可能会遇到蓬松机器的各种问题." +
                    "如果您未更改，作者将不负责蓬松机器.");
            }
        } catch (NumberFormatException e) {
            return;
        }

        // Registering Items
        FluffyItemSetup.setup(this);

        // Register Events Class
        getServer().getPluginManager().registerEvents(new Events(), this);

        final Metrics metrics = new Metrics(this, 8927);

        getLogger().log(Level.INFO, ChatColor.GREEN + "你好！要与共享服务器到" +
            "Slimefun社区吗?");
        getLogger().log(Level.INFO, ChatColor.GREEN + "加入官方Slimefun服务器" +
            "https://discord.gg/slimefun");
    }

    @Override
    public void onDisable() {
        // Logic for disabling the plugin...
    }

    @Override
    public boolean onCommand(@Nonnull CommandSender sender, @Nonnull Command cmd, @Nonnull String label,
                             String[] args) {

        if (args.length == 0) {
            sender.sendMessage("&eFluffyMachines > &c成功");
            return true;
        }
        if (args[0].equalsIgnoreCase("replace") && sender instanceof Player) {
            Player p = ((Player) sender);
            ItemStack item = p.getInventory().getItemInMainHand();
            if (SlimefunItem.getByItem(item) != null) {
                if (SlimefunItem.getByItem(item) == FluffyItems.WATERING_CAN.getItem()) {
                    p.getInventory().setItemInMainHand(FluffyItems.WATERING_CAN.clone());
                }
            }
            return true;
        } else if (args[0].equalsIgnoreCase("debug") && sender.hasPermission("fluffymachines.admin")) {

            return true;
        } else if (args[0].equalsIgnoreCase("save") && sender.hasPermission("fluffymachines.admin")) {
            saveAllPlayers();
            return true;

        } else if (args[0].equalsIgnoreCase("meta") && sender instanceof Player) {
            Player p = (Player) sender;
            Utils.send(p, String.valueOf(p.getInventory().getItemInMainHand().getItemMeta()));
            return true;

        } else if (args[0].equalsIgnoreCase("rawmeta") && sender instanceof Player) {
            Player p = (Player) sender;
            p.sendMessage(String.valueOf(p.getInventory().getItemInMainHand().getItemMeta()).replace("§", "&"));
            return true;

        } else if (args[0].equalsIgnoreCase("addinfo") && sender.hasPermission("fluffymachines.admin")
            && sender instanceof Player) {
            Player p = (Player) sender;

            if (args.length != 3) {
                Utils.send(p, "&c请指定密钥和数据");

            } else {
                RayTraceResult rayResult = p.rayTraceBlocks(5d);
                if (rayResult != null && rayResult.getHitBlock() != null
                    && BlockStorage.hasBlockInfo(rayResult.getHitBlock())) {

                    BlockStorage.addBlockInfo(rayResult.getHitBlock(), args[1], args[2]);
                    Utils.send(p, "&a已添加.");

                } else {
                    Utils.send(p, "&c你必须瞄准一个SF物品");
                }
            }
            return true;

        }
        return false;
    }

    private void saveAllPlayers() {
        Iterator<PlayerProfile> iterator = PlayerProfile.iterator();
        int players = 0;

        while (iterator.hasNext()) {
            PlayerProfile profile = iterator.next();

            profile.save();
            players++;
        }

        if (players > 0) {
            Bukkit.getLogger().log(Level.INFO, "Auto-saved all player data for {0} player(s)!", players);
        }
    }

    private void registerGlow() {
        Enchantment glowEnchantment = new GlowEnchant(Constants.GLOW_ENCHANT, new String[] {
            "SMALL_PORTABLE_CHARGER", "MEDIUM_PORTABLE_CHARGER", "BIG_PORTABLE_CHARGER",
            "LARGE_PORTABLE_CHARGER", "CARBONADO_PORTABLE_CHARGER", "PAXEL"
        });

        // Prevent double-registration errors
        if (Enchantment.getByKey(glowEnchantment.getKey()) == null) {
            Enchantment.registerEnchantment(glowEnchantment);
        }
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/NCBPFluffyBear/FluffyMachines/issues";
    }

    @Nonnull
    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    public static FluffyMachines getInstance() {
        return instance;
    }

}
