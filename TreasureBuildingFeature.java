package com.example.lokistructures;

import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PillagerEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

/**
 * Two intentionally compact, terrain-checked overworld buildings.
 * Natural worldgen features (not /locate-able vanilla Structure instances).
 */
public final class TreasureBuildingFeature extends Feature<DefaultFeatureConfig> {
    private final boolean hostile;

    public TreasureBuildingFeature(Codec<DefaultFeatureConfig> codec, boolean hostile) {
        super(codec);
        this.hostile = hostile;
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        Random random = context.getRandom();
        BlockPos origin = context.getOrigin();
        int radius = hostile ? 6 : 5;
        int surface = origin.getY() - 1;
        if (surface < 60 || surface > 200) return false;

        // Avoid lakes, caves, steep slopes and unavailable chunk boundaries.
        for (int dx : new int[] {-radius, 0, radius}) {
            for (int dz : new int[] {-radius, 0, radius}) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int top = world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, x, z) - 1;
                BlockPos foot = new BlockPos(x, top, z);
                if (Math.abs(top - surface) > 2 || top < 58) return false;
                if (!world.getFluidState(foot).isEmpty() || !world.getFluidState(foot.up()).isEmpty()) return false;
                if (!world.getBlockState(foot).isOpaque()) return false;
                if (!world.isValidForSetBlock(origin.add(dx, 9, dz))) return false;
            }
        }

        BlockPos base = new BlockPos(origin.getX(), surface, origin.getZ());
        if (hostile) createPillagerFort(world, base, random);
        else createVillagerHouse(world, base, random);
        return true;
    }

    private static void put(StructureWorldAccess world, BlockPos base,
                            int dx, int dy, int dz, Block block) {
        BlockPos position = base.add(dx, dy, dz);
        if (world.isValidForSetBlock(position)) {
            world.setBlockState(position, block.getDefaultState(), 2);
        }
    }

    private static void putState(StructureWorldAccess world, BlockPos base,
                                 int dx, int dy, int dz, BlockState state) {
        BlockPos position = base.add(dx, dy, dz);
        if (world.isValidForSetBlock(position)) world.setBlockState(position, state, 2);
    }

    private static void box(StructureWorldAccess world, BlockPos base,
                            int minX, int minY, int minZ, int maxX, int maxY, int maxZ, Block block) {
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    put(world, base, x, y, z, block);
    }

    private static void clearInterior(StructureWorldAccess world, BlockPos base, int radius, int height) {
        box(world, base, -radius, 1, -radius, radius, height, radius, Blocks.AIR);
    }

    private static void chest(StructureWorldAccess world, BlockPos base, int x, int y, int z,
                              boolean pillager, Random random) {
        BlockPos p = base.add(x, y, z);
        world.setBlockState(p, Blocks.CHEST.getDefaultState(), 2);
        BlockEntity be = world.getBlockEntity(p);
        if (be instanceof LootableContainerBlockEntity container) {
            RegistryKey<LootTable> key = RegistryKey.of(RegistryKeys.LOOT_TABLE,
                    LokiStructuresMod.id(pillager ? "chests/pillager_vault" : "chests/villager_supplies"));
            container.setLootTable(key);
            container.setLootTableSeed(random.nextLong());
            container.markDirty();
        }
    }

    private static void createVillagerHouse(StructureWorldAccess w, BlockPos b, Random random) {
        clearInterior(w, b, 5, 9);
        // Strong stone foundation and wood flooring, not a bare cube.
        box(w, b, -5, -2, -5, 5, -1, 5, Blocks.COBBLESTONE);
        box(w, b, -5, 0, -5, 5, 0, 5, Blocks.SPRUCE_PLANKS);
        for (int y = 1; y <= 4; y++) {
            for (int x = -5; x <= 5; x++) {
                put(w, b, x, y, -5, Blocks.OAK_PLANKS);
                put(w, b, x, y, 5, Blocks.OAK_PLANKS);
            }
            for (int z = -4; z <= 4; z++) {
                put(w, b, -5, y, z, Blocks.OAK_PLANKS);
                put(w, b, 5, y, z, Blocks.OAK_PLANKS);
            }
        }
        for (int x : new int[] {-5, 5})
            for (int z : new int[] {-5, 5})
                box(w, b, x, 1, z, x, 5, z, Blocks.STRIPPED_OAK_LOG);
        // Entrance from the south, leave it clear and reachable.
        box(w, b, -1, 1, 5, 1, 3, 5, Blocks.AIR);
        put(w, b, -1, 4, 5, Blocks.OAK_FENCE);
        put(w, b, 0, 4, 5, Blocks.OAK_FENCE);
        put(w, b, 1, 4, 5, Blocks.OAK_FENCE);
        // Windows
        for (int z : new int[] {-2, 2}) {
            put(w, b, -5, 2, z, Blocks.GLASS_PANE);
            put(w, b, 5, 2, z, Blocks.GLASS_PANE);
        }
        for (int x : new int[] {-2, 2}) put(w, b, x, 2, -5, Blocks.GLASS_PANE);
        // Stepped timber roof
        box(w, b, -6, 5, -6, 6, 5, 6, Blocks.DARK_OAK_PLANKS);
        box(w, b, -4, 6, -5, 4, 6, 5, Blocks.DARK_OAK_PLANKS);
        box(w, b, -2, 7, -4, 2, 7, 4, Blocks.DARK_OAK_PLANKS);
        box(w, b, -1, 8, -3, 1, 8, 3, Blocks.DARK_OAK_SLAB);
        // Small chimney and path
        box(w, b, 3, 6, -3, 3, 8, -3, Blocks.STONE_BRICKS);
        put(w, b, 3, 9, -3, Blocks.CAMPFIRE);
        for (int z = 6; z <= 9; z++) box(w, b, -1, 0, z, 1, 0, z, Blocks.COBBLESTONE);
        // Furniture and two supply chests; loot is rolled on opening.
        put(w, b, -1, 1, -3, Blocks.LECTERN);
        put(w, b, 1, 1, -3, Blocks.COMPOSTER);
        put(w, b, -3, 1, 2, Blocks.CRAFTING_TABLE);
        put(w, b, 3, 1, 2, Blocks.BARREL);
        put(w, b, -3, 1, -3, Blocks.LANTERN);
        chest(w, b, -3, 1, -1, false, random);
        chest(w, b, 3, 1, -1, false, random);
        spawnVillager(w, b.add(-1, 1, 1), random);
        spawnVillager(w, b.add(2, 1, 1), random);
    }

    private static void createPillagerFort(StructureWorldAccess w, BlockPos b, Random random) {
        clearInterior(w, b, 6, 11);
        box(w, b, -6, -2, -6, 6, -1, 6, Blocks.COBBLED_DEEPSLATE);
        box(w, b, -6, 0, -6, 6, 0, 6, Blocks.POLISHED_DEEPSLATE);
        // High dark fort walls with gaps at the entry.
        for (int y = 1; y <= 6; y++) {
            for (int x = -6; x <= 6; x++) {
                put(w, b, x, y, -6, Blocks.DEEPSLATE_BRICKS);
                put(w, b, x, y, 6, Blocks.DEEPSLATE_BRICKS);
            }
            for (int z = -5; z <= 5; z++) {
                put(w, b, -6, y, z, Blocks.DEEPSLATE_BRICKS);
                put(w, b, 6, y, z, Blocks.DEEPSLATE_BRICKS);
            }
        }
        box(w, b, -1, 1, 6, 1, 4, 6, Blocks.AIR);
        for (int x : new int[] {-3, 3}) {
            put(w, b, x, 3, 6, Blocks.IRON_BARS);
            put(w, b, x, 4, 6, Blocks.IRON_BARS);
            put(w, b, x, 3, -6, Blocks.IRON_BARS);
        }
        // Four fortified towers, with crenellations.
        for (int x : new int[] {-6, 6}) for (int z : new int[] {-6, 6}) {
            box(w, b, x, 1, z, x, 9, z, Blocks.POLISHED_BLACKSTONE_BRICKS);
            put(w, b, x, 10, z, Blocks.SOUL_LANTERN);
        }
        for (int x = -6; x <= 6; x += 2) {
            put(w, b, x, 7, -6, Blocks.COBBLED_DEEPSLATE_WALL);
            put(w, b, x, 7, 6, Blocks.COBBLED_DEEPSLATE_WALL);
        }
        for (int z = -4; z <= 4; z += 2) {
            put(w, b, -6, 7, z, Blocks.COBBLED_DEEPSLATE_WALL);
            put(w, b, 6, 7, z, Blocks.COBBLED_DEEPSLATE_WALL);
        }
        // Roof + raised central command cabin.
        box(w, b, -5, 6, -5, 5, 6, 5, Blocks.DARK_OAK_PLANKS);
        box(w, b, -2, 7, -2, 2, 7, 2, Blocks.DEEPSLATE_BRICKS);
        box(w, b, -1, 8, -1, 1, 8, 1, Blocks.POLISHED_BLACKSTONE);
        // Decorations, supplies and two high-value vault chests.
        put(w, b, -4, 1, -4, Blocks.SOUL_LANTERN);
        put(w, b, 4, 1, -4, Blocks.SOUL_LANTERN);
        put(w, b, -4, 1, 3, Blocks.FLETCHING_TABLE);
        put(w, b, 4, 1, 3, Blocks.SMITHING_TABLE);
        put(w, b, 0, 1, -4, Blocks.BLACK_BANNER);
        put(w, b, -2, 1, 4, Blocks.BARREL);
        put(w, b, 2, 1, 4, Blocks.BARREL);
        chest(w, b, -3, 1, -2, true, random);
        chest(w, b, 3, 1, -2, true, random);
        for (int z = 7; z <= 10; z++) box(w, b, -1, 0, z, 1, 0, z, Blocks.COBBLED_DEEPSLATE);
        spawnPillager(w, b.add(-3, 1, 1), random);
        spawnPillager(w, b.add(3, 1, 1), random);
        spawnPillager(w, b.add(0, 1, -2), random);
    }

    private static void spawnVillager(StructureWorldAccess w, BlockPos pos, Random random) {
        VillagerEntity v = EntityType.VILLAGER.create(w.toServerWorld());
        if (v == null) return;
        v.refreshPositionAndAngles(pos.getX() + .5, pos.getY(), pos.getZ() + .5,
                random.nextFloat() * 360, 0);
        v.initialize(w, w.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null);
        v.setPersistent();
        w.spawnEntity(v);
    }

    private static void spawnPillager(StructureWorldAccess w, BlockPos pos, Random random) {
        PillagerEntity mob = EntityType.PILLAGER.create(w.toServerWorld());
        if (mob == null) return;
        mob.refreshPositionAndAngles(pos.getX() + .5, pos.getY(), pos.getZ() + .5,
                random.nextFloat() * 360, 0);
        mob.initialize(w, w.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null);
        mob.setPersistent();
        w.spawnEntity(mob);
    }
}
