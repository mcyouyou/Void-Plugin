package pysio.online.voidPlugin

import org.bukkit.WorldCreator
import org.bukkit.generator.ChunkGenerator
import java.util.Random

object VoidWorldManager {
    fun createVoidWorld() {
        val worldCreator = WorldCreator("void_world")
        worldCreator.generator(VoidChunkGenerator())
        worldCreator.createWorld()
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