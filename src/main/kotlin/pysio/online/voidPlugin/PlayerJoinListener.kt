import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.json.JSONObject
import java.io.File
import org.bukkit.plugin.java.JavaPlugin

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

        if (mainWorld == null || voidWorld == null) {
            plugin.logger.warning("主世界或虚空世界未正确加载")
            return
        }

        val playerLocation = player.location
        val mainWorldChunk = mainWorld.getChunkAt(playerLocation)

        // 确保主世界的区块已加载
        if (!mainWorldChunk.isLoaded) {
            mainWorldChunk.load()
            mainWorldChunk.load(true) // 强制加载区块
        }

        // 确保 voidWorld 的区块已加载
        val voidWorldChunk = voidWorld.getChunkAt(playerLocation)
        if (!voidWorldChunk.isLoaded) {
            voidWorldChunk.load()
        }

        val playerChunkX = playerLocation.blockX shr 4
        val playerChunkZ = playerLocation.blockZ shr 4

        // 复制3x3的区块范围
        for (cx in -1..1) {
            for (cz in -1..1) {
                val mainWorldChunk = mainWorld.getChunkAt(playerChunkX + cx, playerChunkZ + cz)

                // 确保主世界的区块已加载
                if (!mainWorldChunk.isLoaded) {
                    mainWorldChunk.load()
                }

                // 确保 voidWorld 的区块已加载
                val voidWorldChunk = voidWorld.getChunkAt(playerChunkX + cx, playerChunkZ + cz)
                if (!voidWorldChunk.isLoaded) {
                    voidWorldChunk.load()
                }

                // 复制区块
                for (x in 0..15) {
                    for (z in 0..15) {
                        for (y in 0 until mainWorld.maxHeight) {
                            val block = mainWorldChunk.getBlock(x, y, z)
                            val voidBlock = voidWorld.getBlockAt((playerChunkX + cx) * 16 + x, y, (playerChunkZ + cz) * 16 + z)
                            voidBlock.type = block.type
                        }
                    }
                }
            }
        }

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

    private fun isPlayerFirstTime(playerName: String): Boolean {
        return !playerData.has(playerName)
    }

    private fun markPlayerAsVisited(playerName: String) {
        playerData.put(playerName, true)
        playerDataFile.writeText(playerData.toString())
    }
}

class VoidPlugin : JavaPlugin() {

    override fun onEnable() {
        // 注册事件监听器
        server.pluginManager.registerEvents(PlayerJoinListener(this), this)
    }
}