package pysio.online.voidPlugin

import BlockCommandHandler
import PlayerJoinListener
import SnowballCommand
import AspectOfTheVoid
import org.bukkit.plugin.java.JavaPlugin

class VoidPlugin : JavaPlugin() {

    override fun onEnable() {
        // Creat Void World
        server.scheduler.runTaskLater(this, Runnable {
            VoidWorldManager.createVoidWorld()
        }, 20L) //Sleep（20 tick）
        server.scheduler.runTaskLater(this, Runnable {
            VoidWorldManager.createVoidNetherWorld()
        }, 20L) //Sleep（20 tick）
        // PlayerJoinListener
        server.pluginManager.registerEvents(PlayerJoinListener(this), this)
        //Command
        BlockCommandHandler(this)
        SnowballCommand(this)
        //Items
        AspectOfTheVoid(this)
        //EventManager
        EventManager.getInstance().enable(this)
    }

    override fun onDisable() {
        //EventManager
        EventManager.getInstance().disable()
    }
}
