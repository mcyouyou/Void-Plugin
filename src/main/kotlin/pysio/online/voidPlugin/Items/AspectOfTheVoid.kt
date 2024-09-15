import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

class AspectOfTheVoid(private val plugin: JavaPlugin) : CommandExecutor, Listener {
    init {
        // 注册命令和事件监听器
        registerCommand("getaspect")
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("只有玩家可以使用这个命令。")
            return true
        }
        val player = sender as Player

        if (!player.isOp) {
            player.sendMessage("你没有权限执行此命令。")
            return true
        }

        val shovel = ItemStack(Material.DIAMOND_SHOVEL, 1)
        val meta = plugin.server.itemFactory.getItemMeta(Material.DIAMOND_SHOVEL) ?: return true
        meta.setDisplayName("Aspect Of The Void")
        val key = NamespacedKey(plugin, "aspect_of_the_void")
        meta.persistentDataContainer.set(key, PersistentDataType.INTEGER, 1)
        shovel.itemMeta = meta
        player.inventory.addItem(shovel)
        player.sendMessage("你已经获得了Aspect Of The Void。")
        return true
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = player.inventory.itemInMainHand
        val key = NamespacedKey(plugin, "aspect_of_the_void")
        if (item.type == Material.DIAMOND_SHOVEL && item.itemMeta?.persistentDataContainer?.has(key, PersistentDataType.INTEGER) == true) {
            val direction = player.location.direction
            val targetBlock = player.getTargetBlockExact(10)
            val location = when {
                event.action == Action.RIGHT_CLICK_AIR -> {
                    if (targetBlock != null) {
                        // 如果前方有方块，传送到方块旁边
                        targetBlock.location.add(direction.multiply(-1))
                    } else {
                        // 如果前方没有方块，传送到前方10格的位置
                        player.location.add(direction.multiply(10))
                    }
                }
                event.action == Action.LEFT_CLICK_AIR && player.isSneaking -> {
                    // Shift+左键传送到指向方向的方块正上方，无距离限制(才怪)
                    val block = player.getTargetBlockExact(256) // 256格的最大距离
                    block?.location?.add(0.0, 1.0, 0.0) ?: player.location
                }
                else -> null
            }
            location?.let {                       //保持玩家视角不变
                val newLocation = it.clone()
                newLocation.pitch = player.location.pitch
                newLocation.yaw = player.location.yaw
                player.teleport(newLocation)
            }
        }
    }

    private fun registerCommand(commandName: String) {
        val commandMapField = plugin.server.javaClass.getDeclaredField("commandMap")
        commandMapField.isAccessible = true
        val commandMap = commandMapField.get(plugin.server) as CommandMap

        val command = object : Command(commandName) {
            override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
                return onCommand(sender, this, commandLabel, args)
            }
        }
        commandMap.register(plugin.description.name, command)
    }
}