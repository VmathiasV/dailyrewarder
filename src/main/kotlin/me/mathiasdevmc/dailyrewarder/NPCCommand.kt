package me.mathiasdevmc.dailyrewarder

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class NPCCommand(private val plugin: DailyRewarder) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("Nur Spieler können diesen Befehl nutzen!")
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage("§cBitte benutze: /$label <spawn|remove>")
            return true
        }

        when (args[0].lowercase()) {
            "spawn" -> {
                val world = sender.world
                plugin.spawnRewardNPC(world)
                sender.sendMessage("§aBelohnungs-NPC wurde gespawnt!")
            }
            "remove" -> {
                plugin.removeRewardNPC()
                sender.sendMessage("§cBelohnungs-NPC wurde entfernt!")
            }
            else -> {
                sender.sendMessage("§cUnbekannter Befehlsteil. Benutze spawn oder remove.")
            }
        }

        return true
    }
}
