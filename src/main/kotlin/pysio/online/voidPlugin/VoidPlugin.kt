package pysio.online.voidPlugin

import BlockCommandHandler
import PlayerJoinListener
import org.bukkit.plugin.java.JavaPlugin

class VoidPlugin : JavaPlugin() {

    override fun onEnable() {
        // Creat Void World
        server.scheduler.runTaskLater(this, Runnable {
            VoidWorldManager.createVoidWorld()
        }, 20L) //Sleep（20 tick）
        // PlayerJoinListener
        server.pluginManager.registerEvents(PlayerJoinListener(this), this)
        //Command
        BlockCommandHandler(this)
    }

    override fun onDisable() {

    }
}
