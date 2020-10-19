package com.mdomurad.TeleportToPlayer;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {

    public Inventory inv;
    public boolean canTeleport = false;
    private BukkitTask task;
    private Player teleporter;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.getServer().getPluginManager().registerEvents(this, this);
        if (this.getConfig().getBoolean("teleportPearl")) {
            Bukkit.addRecipe(getTeleportPearlRecipe());
        }
        getLogger().info("TeleportToPlayer has been successfuly ON!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TeleportToPlayer has been successfuly OFF!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("ttp")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You cannot do this!");
                return true;
            }
            Player player = (Player) sender;
            createInv();
            player.openInventory(inv);

            return true;
        }

        return false;
    }

    public ShapedRecipe getTeleportPearlRecipe() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<String>();

        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Teleport Pearl");
        lore.add(ChatColor.YELLOW + "RMB to use!");
        meta.setLore(lore);
        meta.addEnchant(Enchantment.KNOCKBACK, 2, true);

        item.setItemMeta(meta);

        NamespacedKey key = new NamespacedKey(this, "teleport_pearl");

        ShapedRecipe recipe = new ShapedRecipe(key, item);

        recipe.shape(" E ", "EPE", " E ");

        recipe.setIngredient('E', Material.EMERALD);
        recipe.setIngredient('P', Material.ENDER_PEARL);

        return recipe;
    }

    @EventHandler()
    public void onClick(InventoryClickEvent event) {
        if (!event.getInventory().equals(inv)) return;
        if (event.getCurrentItem() == null) return;
        if (event.getCurrentItem().getItemMeta() == null) return;
        if (event.getCurrentItem().getItemMeta().getDisplayName() == null) return;

        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        this.teleporter = player;

        Player teleportTo = getServer().getPlayer(event.getCurrentItem().getItemMeta().getDisplayName());

        if (event.getSlot() != 8) {
            ItemStack itemStack = player.getInventory().getItemInMainHand();
            Material itemConfig = Material.matchMaterial(this.getConfig().getString("item"));
            int countConfig = this.getConfig().getInt("count");
            if (this.getConfig().getBoolean("teleportPearl")) {
                if (itemStack.getType() == Material.ENDER_PEARL && itemStack.containsEnchantment(Enchantment.KNOCKBACK)) {
                    if (itemStack.getAmount() >= 1) {
                        this.canTeleport = true;
                    } else {
                        player.sendMessage(ChatColor.RED + "You do not have enough teleport pearls! You steel need 1 teleport pearl!");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You must have 1 teleport pearl in your hand to be able to teleport!");
                }
            } else {
                if (itemStack.getType() == itemConfig) {
                    if (itemStack.getAmount() >= countConfig) {
                        this.canTeleport = true;
                    } else {
                        int missingAmount = countConfig - itemStack.getAmount();
                        player.sendMessage(ChatColor.RED + "You do not have enough " + itemConfig.toString().toLowerCase() + "s! You steel need " + missingAmount + " " + itemConfig.toString().toLowerCase() + "s!");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "You must have " + countConfig + " " + itemConfig.toString().toLowerCase() + "s in your hand to be able to teleport!");
                }
            }
            if (this.canTeleport) {
                player.closeInventory();
                Location location = teleportTo.getLocation();
                World world = teleportTo.getWorld();

                float x = location.getBlockX();
                float y = location.getBlockY();
                float z = location.getBlockZ();

                Location newLocation = new Location(world, x, y, z);

                player.sendMessage(ChatColor.GOLD + "Do not move for 5 seconds! Teleportation is in progress...");
                player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 9999999, 1));

                BukkitRunnable teleporting = new BukkitRunnable() {

                    @Override
                    public void run() {
                        canTeleport = false;
                        if (getConfig().getBoolean("teleportPearl")) {
                            itemStack.setAmount(itemStack.getAmount() - 1);
                        } else {
                            itemStack.setAmount(itemStack.getAmount() - countConfig);
                        }
                        player.teleport(newLocation);
                        player.removePotionEffect(PotionEffectType.CONFUSION);
                        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You have been teleported!");
                    }
                };
                this.task = teleporting.runTaskLater(this, 100);
            } else {
                player.closeInventory();
            }
        } else {
            player.closeInventory();
        }

        return;
    }

    public void createInv() {
        inv = Bukkit.createInventory(null, 9, "Teleport to someone");

        List<String> lore = new ArrayList<String>();

        int i = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            lore.clear();
            ItemStack item = getPlayerHead(player.getDisplayName());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(player.getDisplayName());
            lore.add(ChatColor.GRAY + "Click to teleport!");
            meta.setLore(lore);
            item.setItemMeta(meta);
            inv.setItem(i, item);
            i++;
        }

        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();

        // close button
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "Close Menu");
        lore.clear();
        meta.setLore(lore);
        item.setItemMeta(meta);
        inv.setItem(8, item);
    }

    @SuppressWarnings("deprecation")
    public ItemStack getPlayerHead(String player) {
        boolean isNewVersion = Arrays.stream(Material.values())
                .map(Material::name).collect(Collectors.toList()).contains("PLAYER_HEAD");

        Material type = Material.matchMaterial(isNewVersion ? "PLAYER_HEAD" : "SKULL_ITEM");
        ItemStack item = new ItemStack(type, 1);

        if (!isNewVersion)
            item.setDurability((short) 3);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(player);

        item.setItemMeta(meta);

        return item;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!this.canTeleport) return;
        if (this.teleporter != e.getPlayer()) return;
        if (e.getTo().getBlockX() == e.getFrom().getBlockX() && e.getTo().getBlockY() == e.getFrom().getBlockY() && e.getTo().getBlockZ() == e.getFrom().getBlockZ()) return;
        this.task.cancel();
        this.canTeleport = false;
        e.getPlayer().removePotionEffect(PotionEffectType.CONFUSION);
        e.getPlayer().sendMessage(ChatColor.RED + "You have moved! Teleportation has been canceled.");
    }

    @EventHandler
    public void onPlayerClicks(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        ItemStack item = event.getItem();

        if ( this.getConfig().getBoolean("teleportPearl") ) {
            if ( action.equals( Action.RIGHT_CLICK_AIR ) || action.equals( Action.RIGHT_CLICK_BLOCK ) ) {
                if ( item != null && item.getType() == Material.ENDER_PEARL && item.containsEnchantment(Enchantment.KNOCKBACK) ) {
                    event.setCancelled(true);
                    createInv();
                    player.openInventory(inv);
                }
            }
        }

    }
}
