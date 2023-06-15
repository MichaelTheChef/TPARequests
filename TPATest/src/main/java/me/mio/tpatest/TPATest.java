package me.mio.tpatest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TPATest extends JavaPlugin implements Listener, CommandExecutor {
    private final Map<UUID, UUID> teleportRequests = new HashMap<>();
    private final Map<UUID, BukkitRunnable> teleportTimers = new HashMap<>();
    private final CooldownMap<Player> cooldownMap = new CooldownMap<>();
    private final int TELEPORT_DELAY_SECONDS = 5;
    private final int REQUEST_EXPIRATION_SECONDS = 60;
    private final int TPA_COOLDOWN_SECONDS = 10;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("tpa").setExecutor(this);
        getCommand("tpaccept").setExecutor(this);
        getCommand("tpdeny").setExecutor(this);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        if (teleportTimers.containsKey(player.getUniqueId())) {
            BukkitRunnable runnable = teleportTimers.get(player.getUniqueId());
            runnable.cancel();

            teleportTimers.remove(player.getUniqueId());
            teleportRequests.remove(player.getUniqueId());

            player.sendMessage(ChatColor.RED + "Teleport request denied because you moved.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("tpa")) {
            if (args.length != 1) {
                sender.sendMessage(ChatColor.RED + "Usage: /tpa <player>");
                return true;
            }

            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }

            Player requester = (Player) sender;
            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Player not found.");
                return true;
            }

            if (cooldownMap.hasCooldown(requester)) {
                sender.sendMessage(ChatColor.RED + "You must wait before sending another teleport request.");
                return true;
            }

            sendTeleportRequest(requester, target);
            cooldownMap.setCooldown(requester, TPA_COOLDOWN_SECONDS * 1000L);
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpaccept")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }

            Player player = (Player) sender;
            Player requester = Bukkit.getPlayer(teleportRequests.get(player.getUniqueId()));

            if (requester == null) {
                sender.sendMessage(ChatColor.RED + "There are no pending teleport requests.");
                return true;
            }

            acceptTeleportRequest(requester, player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("tpdeny")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }

            Player player = (Player) sender;
            Player requester = Bukkit.getPlayer(teleportRequests.get(player.getUniqueId()));

            if (requester == null) {
                sender.sendMessage(ChatColor.RED + "There are no pending teleport requests.");
                return true;
            }

            denyTeleportRequest(requester, player);
            return true;
        }

        return false;
    }

    public void sendTeleportRequest(Player requester, Player target) {
        UUID targetUUID = target.getUniqueId();
        teleportRequests.put(targetUUID, requester.getUniqueId());

        requester.sendMessage(ChatColor.YELLOW + "Teleport request sent to " + target.getName() + ".");
        target.sendMessage(ChatColor.GREEN + requester.getName() +
                ChatColor.YELLOW + " has sent you a teleport request. Type " +
                ChatColor.GREEN + "/tpaccept" + ChatColor.YELLOW + " or " +
                ChatColor.RED + "/tpdeny" + ChatColor.YELLOW + " to respond to the request. The request will expire in 60 seconds.");

        BukkitRunnable expirationTimer = new BukkitRunnable() {
            @Override
            public void run() {
                if (teleportRequests.containsKey(targetUUID) && teleportRequests.get(targetUUID).equals(requester.getUniqueId())) {
                    teleportRequests.remove(targetUUID);
                    requester.sendMessage(ChatColor.RED + "Teleport request to " + target.getName() + " expired.");
                    target.sendMessage(ChatColor.RED + "Teleport request from " + requester.getName() + " expired.");
                }
            }
        };
        expirationTimer.runTaskLater(this, REQUEST_EXPIRATION_SECONDS * 20L);
    }

    public void acceptTeleportRequest(Player requester, Player target) {
        UUID targetUUID = target.getUniqueId();

        if (!teleportRequests.containsKey(targetUUID) || !teleportRequests.get(targetUUID).equals(requester.getUniqueId())) {
            requester.sendMessage(ChatColor.RED + "The teleport request is no longer valid.");
            return;
        }

        teleportRequests.remove(targetUUID);

        requester.sendMessage(ChatColor.GREEN + "Teleport request accepted.");
        target.sendMessage(ChatColor.GREEN + "You accepted the teleport request from " + ChatColor.YELLOW + requester.getName() + ".");

        Location targetLocation = requester.getLocation();

        BukkitRunnable countdownTimer = new BukkitRunnable() {
            int count = TELEPORT_DELAY_SECONDS;

            @Override
            public void run() {
                if (count > 0) {
                    target.sendActionBar(ChatColor.YELLOW + "Teleporting in " + count + " seconds...");
                    count--;
                } else {
                    target.teleport(targetLocation);
                    teleportTimers.remove(targetUUID);
                    cancel();
                }
            }
        };
        countdownTimer.runTaskTimer(this, 0L, 20L);
        teleportTimers.put(targetUUID, countdownTimer);
    }

    public void denyTeleportRequest(Player requester, Player target) {
        teleportRequests.remove(target.getUniqueId());

        requester.sendMessage(ChatColor.RED + "Teleport request denied.");
        target.sendMessage(ChatColor.RED + requester.getName() + " denied your teleport request.");
    }
}
