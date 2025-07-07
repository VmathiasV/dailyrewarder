package me.mathiasdevmc.dailyrewarder

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta

class AdminRewardCommand(private val plugin: DailyRewarder) : CommandExecutor, Listener {

    companion object {
        private const val ADMIN_GUI_TITLE = "§6Admin Reward Dashboard"
        private const val PLAYER_LIST_TITLE = "§6Spieler Übersicht"
        private const val PLAYER_STATS_TITLE = "§6Spieler Statistiken"
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Nur Spieler können diesen Befehl verwenden!")
            return true
        }
        if (!sender.isOp) {
            sender.sendMessage("§cNur OPs dürfen diesen Befehl benutzen!")
            return true
        }

        openAdminDashboard(sender)
        return true
    }

    private fun openAdminDashboard(player: Player) {
        val gui: Inventory = Bukkit.createInventory(null, 9, ADMIN_GUI_TITLE)

        gui.setItem(0, createGuiItem(Material.PAPER, "§aLetzte Claims anzeigen", listOf("Klick, um Spieler zu sehen")))
        gui.setItem(1, createGuiItem(Material.BARRIER, "§cClaim für Spieler zurücksetzen", listOf("Klick, um Spieler zu sehen")))
        gui.setItem(2, createGuiItem(Material.VILLAGER_SPAWN_EGG, "§6NPC neu spawnen", listOf("Klick, um NPC neu zu spawnen")))

        player.openInventory(gui)
    }

    private fun createGuiItem(material: Material, name: String, lore: List<String>): ItemStack {
        val item = ItemStack(material)
        val meta = item.itemMeta
        meta?.setDisplayName(name)
        meta?.lore = lore
        item.itemMeta = meta
        return item
    }

    private fun openPlayerList(player: Player, forReset: Boolean) {
        val onlinePlayers = Bukkit.getOnlinePlayers().toList()
        val size = ((onlinePlayers.size + 8) / 9) * 9

        val gui: Inventory = Bukkit.createInventory(null, size, PLAYER_LIST_TITLE + if (forReset) " §c(Claim zurücksetzen)" else "")

        for ((index, p) in onlinePlayers.withIndex()) {
            val skull = createPlayerHead(p)
            val doc = plugin.collection.find(org.bson.Document("_id", p.uniqueId.toString())).first()
            val normalClaims = doc?.getInteger("normalClaimCount", 0) ?: 0
            val subClaims = doc?.getInteger("subClaimCount", 0) ?: 0
            val totalClaims = normalClaims + subClaims

            val lastJoinMillis = p.lastPlayed
            val lastJoinFormatted = if (lastJoinMillis > 0) {
                DailyRewarder.formatDate(lastJoinMillis)
            } else {
                "Unbekannt"
            }

            val meta = skull.itemMeta as SkullMeta
            meta.setDisplayName("§e${p.name}")
            meta.lore = listOf(
                "§7Gesamt Claims: §6$totalClaims",
                "§7Letzter Login: §f$lastJoinFormatted"
            )
            skull.itemMeta = meta

            if (forReset) {
                val loreWithReset = meta.lore?.toMutableList() ?: mutableListOf()
                loreWithReset.add("§cKlick, um Claims zurückzusetzen")
                meta.lore = loreWithReset
                skull.itemMeta = meta
            }

            gui.setItem(index, skull)
        }

        player.openInventory(gui)
    }



    private fun createPlayerHead(player: Player): ItemStack {
        val skull = ItemStack(Material.PLAYER_HEAD)
        val meta = skull.itemMeta as SkullMeta
        meta.owningPlayer = player
        skull.itemMeta = meta
        return skull
    }

    private fun openPlayerStats(player: Player, target: Player) {
        val gui: Inventory = Bukkit.createInventory(null, 27, PLAYER_STATS_TITLE)

        val lastClaimDoc = plugin.collection.find(org.bson.Document("_id", target.uniqueId.toString())).first()

        val normalClaims = lastClaimDoc?.getInteger("normalClaimCount", 0) ?: 0
        val subClaims = lastClaimDoc?.getInteger("subClaimCount", 0) ?: 0
        val totalClaims = normalClaims + subClaims

        val isSub = target.hasPermission("dailybonus.sub") || target.isOp

        val lastClaimNormal = lastClaimDoc?.getLong("lastClaimNormal") ?: 0L
        val lastClaimSub = lastClaimDoc?.getLong("lastClaimSub") ?: 0L

        val skull = createPlayerHead(target).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName("§e${target.name}")
                lore = listOf("§7Rang: Spieler")
            }
        }
        gui.setItem(13, skull)

        val claimsItem = ItemStack(Material.PAPER).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName("§aClaims")
                lore = listOf(
                    "§7Gesamt Claims: §6$totalClaims",
                    "§7Normale Claims: §6$normalClaims",
                    "§7Sub Claims: §6$subClaims",
                    "§7Letzter Normal Claim: ${DailyRewarder.formatDate(lastClaimNormal)}",
                    "§7Letzter Sub Claim: ${DailyRewarder.formatDate(lastClaimSub)}",
                    "§7Sub-Bonus: ${if (isSub) "§aJa" else "§cNein"}"
                )
            }
        }
        gui.setItem(11, claimsItem)

        val playtimeItem = ItemStack(Material.CLOCK).apply {
            itemMeta = itemMeta?.apply {
                setDisplayName("§6Spielzeit")
                lore = listOf(
                    "§7Spielzeit:",
                    "§7Muss ich noch einbauen"
                )
            }
        }
        gui.setItem(15, playtimeItem)

        player.openInventory(gui)
    }



    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        val title = event.view.title

        if (title == ADMIN_GUI_TITLE) {
            event.isCancelled = true
            when (event.rawSlot) {
                0 -> openPlayerList(player, false)
                1 -> openPlayerList(player, true)
                2 -> {
                    plugin.spawnRewardNPC(player.world)
                    player.sendMessage("§aNPC wurde neu gespawnt!")
                }
            }
            return
        }

        if (title.startsWith(PLAYER_LIST_TITLE)) {
            event.isCancelled = true
            val clickedItem = event.currentItem ?: return
            if (clickedItem.type != Material.PLAYER_HEAD) return

            val forReset = title.contains("(Claim zurücksetzen)")
            val meta = clickedItem.itemMeta as? SkullMeta ?: return
            val targetName = meta.owningPlayer?.name ?: return
            val targetPlayer = Bukkit.getPlayerExact(targetName) ?: return

            if (forReset) {
                plugin.collection.updateOne(
                    org.bson.Document("_id", targetPlayer.uniqueId.toString()),
                    org.bson.Document("\$unset", org.bson.Document("lastClaimNormal", "").append("lastClaimSub", ""))
                        .append("\$set", org.bson.Document("claimCount", 0))
                )
                player.sendMessage("§cClaims von §e$targetName §cwurden zurückgesetzt!")
                player.closeInventory()
            } else {
                openPlayerStats(player, targetPlayer)
            }
            return
        }

        if (title == PLAYER_STATS_TITLE) {
            event.isCancelled = true
        }
    }
}
