import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.generator.ChunkGenerator
import java.util.Random

object VoidWorldManager {
    fun createVoidWorld() {
        val worldCreator = WorldCreator("void_world")
        worldCreator.generator(VoidChunkGenerator())
        worldCreator.createWorld()
    }

    fun createVoidNetherWorld() {
        val netherWorldCreator = WorldCreator("void_nether_world")
        netherWorldCreator.environment(World.Environment.NETHER) // 设置为地狱环境
        netherWorldCreator.generator(VoidChunkGenerator())
        netherWorldCreator.createWorld()
    }
}

class VoidChunkGenerator : ChunkGenerator() {
    override fun generateChunkData(
        world: org.bukkit.World,
        random: Random,
        x: Int,
        z: Int,
        biome: ChunkGenerator.BiomeGrid
    ): ChunkGenerator.ChunkData {
        return createChunkData(world)
    }
}