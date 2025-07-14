package me.mathiasdevmc.dailyrewarder

import LoginListener
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bson.Document
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.Villager
import org.bukkit.plugin.java.JavaPlugin
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

class DailyRewarder : JavaPlugin() {

    var rewardNPC: Villager? = null
        private set

    lateinit var mongoClient: MongoClient
    lateinit var database: MongoDatabase
    lateinit var collection: MongoCollection<Document>

    val mini = MiniMessage.miniMessage()

    companion object {
        const val NPC_NAME = "§6Belohnungs-NPC"

        val dateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("d. MMMM yyyy").withZone(ZoneId.of("Europe/Berlin"))

        fun formatDate(millis: Long): String {
            return if (millis == 0L) "Nie"
            else dateFormatter.format(Instant.ofEpochMilli(millis))
        }
    }

    override fun onEnable() {
        removeOldNPC()
        logger.info("DailyRewarder geladen!")

        mongoClient = MongoClients.create("mongodb://localhost:27017")
        database = mongoClient.getDatabase("dailyrewarder")
        collection = database.getCollection("users")

        getCommand("npc")?.setExecutor(NPCCommand(this))
        server.pluginManager.registerEvents(RewardListener(this), this)
        server.pluginManager.registerEvents(LoginListener(this), this)

        val adminCommand = AdminRewardCommand(this)
        getCommand("adminreward")?.setExecutor(adminCommand)
        server.pluginManager.registerEvents(adminCommand, this)
    }

    private fun removeOldNPC() {
        Bukkit.getWorlds().forEach { world ->
            world.entities.filter { it is Villager && it.customName == NPC_NAME }.forEach {
                it.remove()
                logger.info("Alten Belohnungs-NPC entfernt: ${it.uniqueId}")
            }
        }
    }

    override fun onDisable() {
        logger.info("DailyRewarder deaktiviert.")
        rewardNPC?.remove()
        mongoClient.close()
    }

    fun spawnRewardNPC(world: World) {
        rewardNPC?.remove()

        val loc = Location(world, -4312.00, 63.00, 699.00)

        rewardNPC = world.spawn(loc, Villager::class.java).apply {
            customName = NPC_NAME
            isCustomNameVisible = true
            isInvulnerable = true
            setAI(false)
            setCanPickupItems(false)
            profession = Villager.Profession.FISHERMAN
        }

        logger.info("Belohnungs-NPC gespawnt bei ${loc.blockX}, ${loc.blockY}, ${loc.blockZ}")
    }

    fun removeRewardNPC() {
        rewardNPC?.remove()
        rewardNPC = null
        logger.info("Belohnungs-NPC wurde entfernt.")
    }

    fun isOnCooldownNormal(uuid: UUID): Boolean {
        val doc = try {
            collection.find(Document("_id", uuid.toString())).first()
        } catch (e: Exception) {
            logger.warning("MongoDB Fehler bei isOnCooldownNormal: ${e.message}")
            return false
        } ?: return false

        val lastClaimNormal = doc.getLong("lastClaimNormal") ?: return false
        val now = System.currentTimeMillis()
        val diff = now - lastClaimNormal
        return diff < TimeUnit.SECONDS.toMillis(24)
    }

    fun isOnCooldownSub(uuid: UUID): Boolean {
        val doc = try {
            collection.find(Document("_id", uuid.toString())).first()
        } catch (e: Exception) {
            logger.warning("MongoDB Fehler bei isOnCooldownSub: ${e.message}")
            return false
        } ?: return false

        val lastClaimSub = doc.getLong("lastClaimSub") ?: return false
        val now = System.currentTimeMillis()
        val diff = now - lastClaimSub
        return diff < TimeUnit.SECONDS.toMillis(24)
    }

    fun recordClaimNormal(uuid: UUID) {
        val now = System.currentTimeMillis()
        val doc = collection.find(Document("_id", uuid.toString())).first()
        if (doc == null) {
            val newDoc = Document("_id", uuid.toString())
                .append("lastClaimNormal", now)
                .append("normalClaimCount", 1)
                .append("subClaimCount", 0)
            collection.insertOne(newDoc)
        } else {
            val count = doc.getInteger("normalClaimCount", 0) + 1
            collection.updateOne(
                Document("_id", uuid.toString()),
                Document("\$set", Document("lastClaimNormal", now).append("normalClaimCount", count))
            )
        }
    }

    fun recordClaimSub(uuid: UUID) {
        val now = System.currentTimeMillis()
        val doc = collection.find(Document("_id", uuid.toString())).first()
        if (doc == null) {
            val newDoc = Document("_id", uuid.toString())
                .append("lastClaimSub", now)
                .append("subClaimCount", 1)
                .append("normalClaimCount", 0)
            collection.insertOne(newDoc)
        } else {
            val count = doc.getInteger("subClaimCount", 0) + 1
            collection.updateOne(
                Document("_id", uuid.toString()),
                Document("\$set", Document("lastClaimSub", now).append("subClaimCount", count))
            )
        }
    }

    fun getClaimCount(uuid: UUID): Int {
        val doc = try {
            collection.find(Document("_id", uuid.toString())).first()
        } catch (e: Exception) {
            logger.warning("MongoDB Fehler bei getClaimCount: ${e.message}")
            null
        } ?: return 0

        val normal = doc.getInteger("normalClaimCount", 0)
        val sub = doc.getInteger("subClaimCount", 0)
        return normal + sub
    }


    fun mm(text: String): Component {
        return mini.deserialize(text)
    }
}
