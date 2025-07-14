package me.mathiasdevmc.dailyrewarder

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

class RewardListener(private val plugin: DailyRewarder) : Listener {

    private val mm = MiniMessage.miniMessage()

    @EventHandler
    fun onPlayerInteractNPC(event: PlayerInteractEntityEvent) {
        val player = event.player
        val clicked = event.rightClicked

        if (clicked.type != EntityType.VILLAGER) return
        if (clicked.customName != DailyRewarder.NPC_NAME) return

        event.isCancelled = true

        val gui: Inventory = Bukkit.createInventory(null, 27, "§6Tägliche Belohnung")

        val glass = ItemStack(Material.GRAY_STAINED_GLASS_PANE).apply {
            itemMeta = itemMeta?.apply { setDisplayName(" ") }
        }
        for (i in 0 until 27) gui.setItem(i, glass)

        val normalItem = ItemStack(Material.GOLD_INGOT).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName("§eTägliche Belohnung")
                lore = listOf(
                    "§7Belohnung für alle Spieler",
                    "§7100 Coins",
                    "",
                    "§aKlicke hier, um sie zu erhalten!"
                )
            }
        }

        val subItem = ItemStack(Material.DIAMOND).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName("§bBonus für Unterstützer")
                lore = listOf(
                    "§7Extrabonus für §6Sub§7-Spieler",
                    "§7125 Coins",
                    "",
                    "§aKlicke hier, um sie zu erhalten!"
                )
            }
        }

        val streakCount = plugin.getClaimCount(player.uniqueId)
        val streakItem = ItemStack(Material.BOOK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName("§6Deine Streak")
                lore = listOf(
                    "§7Du hast die Belohnung bisher",
                    "§e$streakCount §7Mal abgeholt.",
                    "",
                    "§7Sammle täglich, um deine Streak zu erhöhen!"
                )
            }
        }

        gui.setItem(12, normalItem)
        gui.setItem(14, subItem)
        gui.setItem(18, streakItem)

        player.openInventory(gui)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        if (event.view.title != "§6Tägliche Belohnung") return

        event.isCancelled = true

        val isSub = player.hasPermission("dailybonus.sub") || player.isOp
        val uuid = player.uniqueId

        when (event.rawSlot) {
            12 -> {
                if (plugin.isOnCooldownNormal(uuid)) {
                    player.sendMessage(mm.deserialize("<red>Du kannst die normale Belohnung nur alle 24 Stunden abholen!"))
                    player.closeInventory()
                    return
                }
                val coins = 100
                player.sendMessage(mm.deserialize("<green>Normale Belohnung erhalten: <yellow>$coins Coins!"))
                plugin.recordClaimNormal(uuid)
            }
            14 -> {
                if (!isSub) {
                    player.sendMessage(mm.deserialize("<red>Du benötigst den <gold>Sub-Rang<red>, um diese Belohnung zu erhalten!"))
                    return
                }
                if (plugin.isOnCooldownSub(uuid)) {
                    player.sendMessage(mm.deserialize("<red>Du kannst die Sub-Belohnung nur alle 24 Stunden abholen!"))
                    player.closeInventory()
                    return
                }
                val coins = (100 * 1.25).toInt()
                player.sendMessage(mm.deserialize("<aqua>Sub-Belohnung erhalten: <yellow>$coins Coins!"))
                plugin.recordClaimSub(uuid)
            }
            else -> return
        }

        val claimCount = plugin.getClaimCount(uuid)
        player.sendMessage(mm.deserialize("<gray>Täglicher Claim-Zähler: <yellow>$claimCount"))
        player.world.spawnParticle(Particle.HAPPY_VILLAGER, player.location.add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5)
        player.playSound(player.location, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
        player.closeInventory()
    }

    @EventHandler
    fun onEntityDamage(event: EntityDamageByEntityEvent) {
        if (event.entity.type == EntityType.VILLAGER && event.entity.customName == DailyRewarder.NPC_NAME) {
            event.isCancelled = true
        }
    }
}
