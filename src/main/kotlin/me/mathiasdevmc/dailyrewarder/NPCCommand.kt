package me.mathiasdevmc.dailyrewarder

import net.kyori.adventure.text.minimessage.MiniMessage

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class NPCCommand(private val plugin: DailyRewarder) : CommandExecutor {

    private val mini = MiniMessage.miniMessage()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage(mini.deserialize("<red>Nur Spieler können diesen Befehl nutzen!</red>"))
            return true
        }

        if (args.isEmpty()) {
            sender.sendMessage(mini.deserialize("<red>Bitte benutze: </red><gray>/$label <spawn|remove></gray>"))
            return true
        }

        when (args[0].lowercase()) {
            "spawn" -> {
                val world = sender.world
                plugin.spawnRewardNPC(world)
                sender.sendMessage(mini.deserialize("<green>Belohnungs-NPC wurde gespawnt!</green>"))
            }
            "remove" -> {
                plugin.removeRewardNPC()
                sender.sendMessage(mini.deserialize("<red>Belohnungs-NPC wurde entfernt!</red>"))
            }
            else -> {
                sender.sendMessage(mini.deserialize("<red>Unbekannter Befehlsteil.</red> <gray>Benutze <yellow>spawn</yellow> oder <yellow>remove</yellow>.</gray>"))
            }
        }

        return true
    }
}
