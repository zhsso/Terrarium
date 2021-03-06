package net.gegy1000.terrarium.server.map.glob.generator

import net.gegy1000.terrarium.server.map.glob.GlobGenerator
import net.gegy1000.terrarium.server.map.glob.GlobType
import net.gegy1000.terrarium.server.map.glob.generator.layer.SelectionSeedLayer
import net.minecraft.block.BlockDirt
import net.minecraft.block.BlockTallGrass
import net.minecraft.init.Blocks
import net.minecraft.world.chunk.ChunkPrimer
import net.minecraft.world.gen.layer.GenLayer
import net.minecraft.world.gen.layer.GenLayerFuzzyZoom
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom
import java.util.Random

class Shrubland : GlobGenerator(GlobType.SHRUBLAND) {
    companion object {
        private const val LAYER_DIRT = 0
        private const val LAYER_SAND = 1

        private val SAND = Blocks.SAND.defaultState
        private val DIRT = Blocks.DIRT.defaultState.withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT)

        private val TALL_GRASS = Blocks.TALLGRASS.defaultState.withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS)
        private val DEAD_BUSH = Blocks.TALLGRASS.defaultState.withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.DEAD_BUSH)

        private val BUSH = Blocks.LEAVES.defaultState
    }

    lateinit var coverSelector: GenLayer
    lateinit var grassSelector: GenLayer

    override fun createLayers() {
        var cover: GenLayer = SelectionSeedLayer(2, 1)
        cover = GenLayerVoronoiZoom(1000, cover)
        cover = GenLayerFuzzyZoom(3000, cover)

        this.coverSelector = cover
        this.coverSelector.initWorldGenSeed(this.seed)

        var grass: GenLayer = SelectionSeedLayer(3, 3000)
        grass = GenLayerVoronoiZoom(1000, grass)
        grass = GenLayerFuzzyZoom(2000, grass)

        this.grassSelector = grass
        this.grassSelector.initWorldGenSeed(this.seed)
    }

    override fun coverDecorate(primer: ChunkPrimer, random: Random, x: Int, z: Int) {
        val grassLayer = this.sampleChunk(this.grassSelector, x, z)

        this.iterate { localX: Int, localZ: Int ->
            val bufferIndex = localX + localZ * 16
            val grassType = grassLayer[bufferIndex]

            if (grassType != 0 && random.nextInt(4) == 0) {
                val y = this.heightBuffer[bufferIndex]

                if (grassType == 1) {
                    if (random.nextInt(8) == 0) {
                        primer.setBlockState(localX, y + 1, localZ, Shrubland.DEAD_BUSH)
                    } else {
                        primer.setBlockState(localX, y + 1, localZ, Shrubland.TALL_GRASS)
                    }
                } else {
                    if (random.nextInt(4) == 0) {
                        primer.setBlockState(localX, y + 1, localZ, Shrubland.BUSH)
                    }
                }
            }
        }
    }

    override fun getCover(x: Int, z: Int, random: Random) {
        this.coverLayer(this.coverBuffer, x, z, this.coverSelector) {
            when (it) {
                Shrubland.LAYER_SAND -> Shrubland.SAND
                Shrubland.LAYER_DIRT -> Shrubland.DIRT
                else -> Shrubland.SAND
            }
        }
    }
}
