import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.World
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.command.defaults.BukkitCommand
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.Field

class BlockCommandHandler(private val plugin: JavaPlugin) {

    init {
        registerCommand("deleteblock", DeleteBlockCommand(plugin))
        registerCommand("copyblock", CopyBlockCommand(plugin))
        registerCommand("resetblock", ResetBlockCommand(plugin)) // 注册新命令
    }

    private fun registerCommand(name: String, command: Command) {
        try {
            val commandMapField: Field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
            commandMapField.isAccessible = true
            val commandMap = commandMapField.get(Bukkit.getServer()) as CommandMap
            commandMap.register(plugin.description.name, command)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class DeleteBlockCommand(private val plugin: JavaPlugin) : BukkitCommand("deleteblock") {

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("该命令只能由玩家执行。")
            return true
        }

        val player = sender

        if (!player.isOp) {
            player.sendMessage("你没有权限执行此命令。")
            return true
        }

        val playerLocation = player.location
        val playerChunk = playerLocation.chunk

        for (x in 0..15) {
            for (z in 0..15) {
                for (y in 0 until player.world.maxHeight) {
                    val block = playerChunk.getBlock(x, y, z)
                    block.type = Material.AIR
                }
            }
        }
        player.sendMessage("已删除你脚下的区块。")
        return true
    }
}

class CopyBlockCommand(private val plugin: JavaPlugin) : BukkitCommand("copyblock") {

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("该命令只能由玩家执行。")
            return true
        }

        val player = sender

        if (!player.isOp) {
            player.sendMessage("你没有权限执行此命令。")
            return true
        }

        val playerLocation = player.location
        val playerChunk = playerLocation.chunk

        val targetWorldName = when (player.world.environment) {
            World.Environment.NETHER -> "world_nether"
            else -> "world"
        }

        val targetWorld = plugin.server.getWorld(targetWorldName)
        if (targetWorld == null) {
            player.sendMessage("目标世界未正确加载。")
            return true
        }

        val targetWorldChunk = targetWorld.getChunkAt(playerLocation)
        if (!targetWorldChunk.isLoaded) {
            targetWorldChunk.load()
        }

        for (x in 0..15) {
            for (z in 0..15) {
                for (y in 0 until targetWorld.maxHeight) {
                    val block = targetWorldChunk.getBlock(x, y, z)
                    val targetBlock = playerChunk.getBlock(x, y, z)
                    targetBlock.type = block.type
                }
            }
        }
        player.sendMessage("已从 $targetWorldName 拷贝区块到你脚下。")
        return true
    }
}

class ResetBlockCommand(private val plugin: JavaPlugin) : BukkitCommand("resetblock") {     //注意 这里有BUG！ 需要站在较高高度才能生成正常高度的区块 直接CV的上面的逻辑不知道为啥有问题 先放在这里了

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("该命令只能由玩家执行。")
            return true
        }

        val player = sender

        if (!player.isOp) {
            player.sendMessage("你没有权限执行此命令。")
            return true
        }

        val playerLocation = player.location
        val playerChunk = playerLocation.chunk
        val world = player.world

        // 检查并加载区块
        if (!playerChunk.isLoaded) {
            playerChunk.load()
        }

        // 显示粒子效果包裹区块边界
        showChunkBoundaryParticles(world, playerChunk.x, playerChunk.z)

        player.sendMessage("已显示区块边界的粒子效果。5秒后将清除区块。")

        // 5秒后清除区块
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            clearChunk(playerChunk)
            player.sendMessage("区块已清除。5秒后将从主世界复制。")

            // 再等待5秒后从主世界复制区块
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                copyChunkFromWorld(player, playerChunk)
                player.sendMessage("区块已从主世界重置。")
            }, 100L) // 100L = 5秒

        }, 100L) // 100L = 5秒

        return true
    }

    private fun showChunkBoundaryParticles(world: World, chunkX: Int, chunkZ: Int) {
        val startX = chunkX * 16
        val startZ = chunkZ * 16
        val endX = startX + 15
        val endZ = startZ + 15
        val maxHeight = world.maxHeight

        val dustOptions = Particle.DustOptions(Color.RED, 1.0f)

        // 每10个tick生成一次粒子，持续5秒
        val task = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            for (y in 0 until maxHeight step 5) {
                for (x in startX..endX step 1) { // 间隔一格
                    world.spawnParticle(Particle.DUST, x.toDouble(), y.toDouble(), startZ.toDouble(), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                    world.spawnParticle(Particle.DUST, x.toDouble(), y.toDouble(), endZ.toDouble(), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                }
                for (z in startZ..endZ step 1) { // 间隔一格
                    world.spawnParticle(Particle.DUST, startX.toDouble(), y.toDouble(), z.toDouble(), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                    world.spawnParticle(Particle.DUST, endX.toDouble(), y.toDouble(), z.toDouble(), 1, 0.0, 0.0, 0.0, 0.0, dustOptions)
                }
            }
        }, 0L, 10L) // 每10 tick 生成一次

        // 5秒后取消粒子效果任务
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            task.cancel()
        }, 100L) // 100L = 5秒
    }

    private fun clearChunk(chunk: org.bukkit.Chunk) {
        for (x in 0..15) {
            for (z in 0..15) {
                for (y in 0 until chunk.world.maxHeight) {
                    val block = chunk.getBlock(x, y, z)
                    block.type = Material.AIR
                }
            }
        }
    }

    private fun copyChunkFromWorld(player: Player, playerChunk: org.bukkit.Chunk) {
        val targetWorldName = when (player.world.environment) {
            World.Environment.NETHER -> "world_nether"
            else -> "world"
        }

        val targetWorld = plugin.server.getWorld(targetWorldName)
        if (targetWorld == null) {
            player.sendMessage("目标世界未正确加载。")
            return
        }

        val targetWorldChunk = targetWorld.getChunkAt(player.location)
        if (!targetWorldChunk.isLoaded) {
            targetWorldChunk.load()
        }

        for (x in 0..15) {
            for (z in 0..15) {
                for (y in 0 until targetWorld.maxHeight) {
                    val block = targetWorldChunk.getBlock(x, y, z)
                    val targetBlock = playerChunk.getBlock(x, y, z)
                    targetBlock.type = block.type
                }
            }
        }
    }
}