package net.gegy1000.terrarium.server.world.generator

import net.gegy1000.terrarium.server.capability.TerrariumCapabilities
import net.gegy1000.terrarium.server.map.glob.GlobType
import net.minecraft.init.Biomes
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.biome.Biome
import net.minecraft.world.biome.BiomeCache
import net.minecraft.world.biome.BiomeProvider
import net.minecraft.world.gen.layer.IntCache
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.util.ArrayList
import java.util.Random

class EarthBiomeProvider(val world: World) : BiomeProvider() {
    val handler by lazy { world.getCapability(TerrariumCapabilities.worldDataCapability, null)!!.generationHandler }

    val biomeCache = BiomeCache(this)
    val spawnBiomes = ArrayList(allowedBiomes)

    var globBuffer = Array(256, { GlobType.NO_DATA })

    override fun getBiomesToSpawnIn() = this.spawnBiomes

    override fun getBiome(pos: BlockPos, defaultBiome: Biome?): Biome = this.biomeCache.getBiome(pos.x, pos.z, defaultBiome)

    @SideOnly(Side.CLIENT)
    override fun getTemperatureAtHeight(biomeTemperature: Float, height: Int) = biomeTemperature

    override fun getBiomesForGeneration(biomes: Array<Biome?>?, x: Int, z: Int, width: Int, height: Int): Array<Biome?>? {
        if (biomes == null || biomes.size < width * height) {
            return Array(256, { Biomes.OCEAN })
        }
        biomes.fill(Biomes.OCEAN)
        return biomes
    }

    override fun getBiomes(biomes: Array<Biome?>?, x: Int, z: Int, width: Int, length: Int, cache: Boolean): Array<Biome?> {
        var newBiomes = biomes
        IntCache.resetIntCache()
        if (newBiomes == null || newBiomes.size < width * length) {
            newBiomes = arrayOfNulls<Biome>(width * length)
        }
        val singleChunk = width == 16 && length == 16 && x and 15 == 0 && z and 15 == 0
        if (cache && singleChunk) {
            System.arraycopy(this.biomeCache.getCachedBiomes(x, z), 0, newBiomes, 0, width * length)
            return newBiomes
        } else {
            if (singleChunk) {
                this.handler.populateGlobRegion(this.globBuffer, x shr 4, z shr 4)
                for (i in 0..globBuffer.lastIndex) {
                    newBiomes[i] = this.globBuffer[i].biome
                }
            } else {
                val chunkMinX = x shr 4
                val chunkMinZ = z shr 4
                val chunkMaxX = (x + width) shr 4
                val chunkMaxZ = (z + length) shr 4
                for (chunkZ in chunkMinZ..chunkMaxZ) {
                    for (chunkX in chunkMinX..chunkMaxX) {
                        this.handler.populateGlobRegion(this.globBuffer, chunkZ, chunkX)
                        for (localZ in Math.max(chunkZ shl 4, z)..Math.min(chunkZ + 1 shl 4, z + length)) {
                            for (localX in Math.max(chunkX shl 4, x)..Math.min(chunkX + 1 shl 4, x + width)) {
                                val index = (localX - x) + (localZ - z) * 16
                                newBiomes[index] = this.globBuffer[index].biome
                            }
                        }
                    }
                }
            }
            return newBiomes
        }
    }

    override fun areBiomesViable(x: Int, z: Int, radius: Int, allowed: List<Biome?>) = true

    // TODO: Implement properly
    override fun findBiomePosition(x: Int, z: Int, radius: Int, biomes: List<Biome>, random: Random) = BlockPos(x, 0, z)

    override fun cleanupCache() = this.biomeCache.cleanupCache()
}