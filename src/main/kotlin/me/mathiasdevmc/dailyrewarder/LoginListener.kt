import me.mathiasdevmc.dailyrewarder.DailyRewarder
import org.bson.Document
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class LoginListener(private val plugin: DailyRewarder) : Listener {

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val uuid = event.player.uniqueId
        val now = System.currentTimeMillis()

        plugin.collection.updateOne(
            Document("_id", uuid.toString()),
            Document("\$set", Document("lastLogin", now)),
            com.mongodb.client.model.UpdateOptions().upsert(true)
        )
    }
}
