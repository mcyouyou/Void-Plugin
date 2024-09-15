import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.HashMap
import java.util.Random

class EventManager private constructor() {
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val eventMap: MutableMap<String, EventBase> = HashMap()

    init {
        initializeEvents()
    }

    companion object {
        private var instance: EventManager? = null

        fun getInstance(): EventManager {
            if (instance == null) {
                instance = EventManager()
            }
            return instance!!
        }
    }

    fun enable(plugin: JavaPlugin) {
        // 启动定时器
        scheduler.scheduleAtFixedRate({ triggerRandomEvent() }, 0, 1, TimeUnit.HOURS)
        // 注册命令
        getCommandMap(plugin)?.register("event", EventCommand("event", eventMap))
    }

    fun disable() {
        scheduler.shutdownNow()
    }

    private fun initializeEvents() {
        // 初始化事件，添加到eventMap中
        // eventMap["eventA"] = EventA()
        // 可以继续添加更多事件
    }

    private fun triggerRandomEvent() {
        // 随机选择并触发事件
        val events = eventMap.values.toTypedArray()
        val event = events[Random().nextInt(events.size)]
        event.trigger()
    }

    private fun getCommandMap(plugin: JavaPlugin): CommandMap? {
        try {
            val craftServer = plugin.server::class.java
            val commandMapField = craftServer.getDeclaredField("commandMap")
            commandMapField.isAccessible = true
            return commandMapField.get(plugin.server) as CommandMap?
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    abstract class EventBase {
        abstract fun trigger()
    }

    class EventCommand(name: String, private val events: Map<String, EventBase>) : Command(name) {
        override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean {
            if (args.isNotEmpty() && events.containsKey(args[0])) {
                val event = events[args[0]]
                event?.trigger()
                sender.sendMessage("事件 ${args[0]} 已手动触发。")
                return true
            }
            sender.sendMessage("事件不存在。")
            return false
        }
    }
}