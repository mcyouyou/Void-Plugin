import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.java.JavaPlugin
import org.json.JSONObject
import java.io.File

class PlayerJoinListener(private val plugin: JavaPlugin) : Listener {
    private val playerDataFile = File("plugins/VoidWorld/playerData.json")
    private val playerData: JSONObject

    init {
        val parentDir = playerDataFile.parentFile
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }
        if (!playerDataFile.exists()) {
            playerDataFile.createNewFile()
            playerDataFile.writeText("{}")
        }
        playerData = JSONObject(playerDataFile.readText())
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        val playerName = player.name

        if (!isPlayerFirstTime(playerName)) {
            return
        }

        val mainWorld = Bukkit.getWorld("world")
        val voidWorld = Bukkit.getWorld("void_world")
        val netherWorld = Bukkit.getWorld("world_nether")
        val voidNetherWorld = Bukkit.getWorld("void_nether_world")

        if (mainWorld == null || voidWorld == null || netherWorld == null || voidNetherWorld == null) {
            plugin.logger.warning("主世界、虚空世界或地狱未正确加载")
            return
        }

        val playerLocation = player.location
        val playerChunkX = playerLocation.blockX shr 4
        val playerChunkZ = playerLocation.blockZ shr 4

        // 复制主世界的区块到虚空世界
        copyChunks(mainWorld, voidWorld, playerChunkX, playerChunkZ)

        // 复制地狱的区块到虚空地狱世界
        copyChunks(netherWorld, voidNetherWorld, playerChunkX, playerChunkZ)

        // 传送玩家到虚空世界的最高方块上
        val highestBlockY = voidWorld.getHighestBlockYAt(playerLocation)
        player.teleport(Location(voidWorld, playerLocation.x, highestBlockY.toDouble(), playerLocation.z))

        markPlayerAsVisited(playerName)
    }

    @EventHandler
    fun onPlayerRespawn(event: PlayerRespawnEvent) {
        val player = event.player
        val voidWorld = Bukkit.getWorld("void_world")

        if (voidWorld == null) {
            plugin.logger.warning("虚空世界未正确加载")
            return
        }

        // 获取虚空世界的重生位置
        val respawnLocation = Location(voidWorld, 0.0, voidWorld.getHighestBlockYAt(0, 0).toDouble(), 0.0)

        // 设置玩家的重生位置为虚空世界
        event.respawnLocation = respawnLocation

        plugin.logger.info("${player.name} 已被传送到虚空世界的重生点")
    }

    @EventHandler
    fun onPlayerPortal(event: PlayerPortalEvent) {
        val player = event.player
        val currentWorld = player.world
        val targetWorld: World? = when (currentWorld.environment) {
            World.Environment.NETHER -> Bukkit.getWorld("void_world")
            World.Environment.NORMAL -> Bukkit.getWorld("void_nether_world")
            else -> null
        }

        if (targetWorld != null) {
            val targetLocation = Location(targetWorld, player.location.x, player.location.y, player.location.z)
            event.to = targetLocation
            plugin.logger.info("${player.name} 通过传送门被传送到 ${targetWorld.name}")
        }
    }

    private fun copyChunks(sourceWorld: World, targetWorld: World, playerChunkX: Int, playerChunkZ: Int) {
        for (cx in -1..1) {
            for (cz in -1..1) {
                val sourceChunk = sourceWorld.getChunkAt(playerChunkX + cx, playerChunkZ + cz)

                // 确保源世界的区块已加载
                if (!sourceChunk.isLoaded) {
                    sourceChunk.load()
                }

                // 确保目标世界的区块已加载
                val targetChunk = targetWorld.getChunkAt(playerChunkX + cx, playerChunkZ + cz)
                if (!targetChunk.isLoaded) {
                    targetChunk.load()
                }

                // 复制区块
                for (x in 0..15) {
                    for (z in 0..15) {
                        for (y in 0 until sourceWorld.maxHeight) {
                            val block = sourceChunk.getBlock(x, y, z)
                            val targetBlock = targetWorld.getBlockAt((playerChunkX + cx) * 16 + x, y, (playerChunkZ + cz) * 16 + z)
                            targetBlock.type = block.type
                        }
                    }
                }
            }
        }
    }

    private fun isPlayerFirstTime(playerName: String): Boolean {
        return !playerData.has(playerName)
    }

    private fun markPlayerAsVisited(playerName: String) {
        playerData.put(playerName, true)
        playerDataFile.writeText(playerData.toString())
    }
}