import org.bukkit.Bukkit
import org.bukkit.Material
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

class ResetBlockCommand(private val plugin: JavaPlugin) : BukkitCommand("resetblock") {

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

        // 删除区块
        for (x in 0..15) {
            for (z in 0..15) {
                for (y in 0 until player.world.maxHeight) {
                    val block = playerChunk.getBlock(x, y, z)
                    block.type = Material.AIR
                }
            }
        }
        player.sendMessage("已删除你脚下的区块，将在5秒后重置。")

        // 5秒后从主世界重新拷贝区块
        Bukkit.getScheduler().runTaskLater(plugin, Runnable {
            val targetWorldName = when (player.world.environment) {
                World.Environment.NETHER -> "world_nether"
                else -> "world"
            }

            val targetWorld = plugin.server.getWorld(targetWorldName)
            if (targetWorld == null) {
                player.sendMessage("目标世界未正确加载。")
                return@Runnable
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
            player.sendMessage("区块已从 $targetWorldName 重置。")
        }, 100L) // 100L = 5秒

        return true
    }
}