import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class MenuItemInventoryManager(private val plugin: JavaPlugin) : Listener {

    init {
        plugin.server.pluginManager.registerEvents(this, plugin)
        startPeriodicCheck()
    }

    private fun startPeriodicCheck() {
        object : BukkitRunnable() {
            override fun run() {
                Bukkit.getOnlinePlayers().forEach { player ->
                    ensureSingleMenuItem(player)
                }
            }
        }.runTaskTimer(plugin, 0L, 2000L) // 2000L = 100 seconds
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        ensureSingleMenuItem(player)
    }

    private fun ensureSingleMenuItem(player: Player) {
        // 清除所有名为"菜单"的下书
        player.inventory.contents.filterNotNull().forEach {
            if (it.type == Material.BOOK && it.itemMeta?.displayName == "菜单") {
                player.inventory.remove(it)
            }
        }
        // 添加一个新的下书到快捷栏最后一个位置
        addItem(player)
    }

    private fun addItem(player: Player) {
        val netherStar = ItemStack(Material.BOOK, 1)
        val meta = plugin.server.itemFactory.getItemMeta(Material.BOOK) ?: return
        meta.setDisplayName("菜单")
        netherStar.itemMeta = meta

        player.inventory.setItem(8, netherStar) // 设置到快捷栏的最后一格（索引8）
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val item = event.currentItem
        if (item != null && item.type == Material.BOOK && item.itemMeta?.displayName == "菜单") {
            if (event.slot != 8 || event.clickedInventory?.type != InventoryType.PLAYER) {
                event.isCancelled = true
                ensureSingleMenuItem(event.whoClicked as Player)
            }
        }
    }

    @EventHandler
    fun onPlayerDropItem(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack
        if (item.type == Material.BOOK && item.itemMeta?.displayName == "菜单") {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val item = event.item
        if (item != null && item.type == Material.BOOK && item.itemMeta?.displayName == "菜单") {
            val player = event.player
            player.performCommand("menu") // 执行/menu命令
            event.isCancelled = true // 取消事件，防止其他交互
        }
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        if (event.inventory.type == InventoryType.CHEST) {
            object : BukkitRunnable() {
                override fun run() {
                    ensureSingleMenuItem(event.player as Player)
                }
            }.runTaskTimer(plugin, 0L, 200L) // 200L = 10 second
        }
    }
}