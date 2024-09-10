import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.entity.Snowball
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin

class SnowballCommand(private val plugin: JavaPlugin) : CommandExecutor, Listener {
    init {
        registerCommand("specialsnowballs")
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("只有玩家可以执行此命令。")
            return true
        }

        val player = sender as Player
        giveSpecialSnowballs(player)
        return true
    }

    private fun giveSpecialSnowballs(player: Player) {
        val snowball1 = ItemStack(Material.SNOWBALL, 1)
        val snowball2 = ItemStack(Material.SNOWBALL, 1)
        val snowball3 = ItemStack(Material.SNOWBALL, 1)

        val meta1 = snowball1.itemMeta
        meta1?.setDisplayName("deleteblock")
        snowball1.itemMeta = meta1

        val meta2 = snowball2.itemMeta
        meta2?.setDisplayName("copyblock")
        snowball2.itemMeta = meta2

        val meta3 = snowball3.itemMeta
        meta3?.setDisplayName("resetblock")
        snowball3.itemMeta = meta3

        player.inventory.addItem(snowball1, snowball2, snowball3)
        player.sendMessage("你已经收到三个特殊的雪球。")
    }

    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        val entity = event.entity
        if (entity is Snowball) {
            val snowball = entity
            val shooter = snowball.shooter
            if (shooter is Player) {
                val player = shooter
                val name = snowball.item?.itemMeta?.displayName
                when (name) {
                    "deleteblock" -> player.performCommand("deleteblock")
                    "copyblock" -> player.performCommand("copyblock")
                    "resetblock" -> player.performCommand("resetblock")
                }
            }
        }
    }

    private fun registerCommand(commandName: String) {
        try {
            val commandMapField = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
            commandMapField.isAccessible = true
            val commandMap = commandMapField.get(Bukkit.getServer()) as CommandMap

            val command = object : Command(commandName) {
                override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
                    return onCommand(sender, this, commandLabel, args)
                }
            }

            commandMap.register(plugin.description.name, command)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}