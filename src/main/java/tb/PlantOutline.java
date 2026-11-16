package tb;

import java.util.Arrays;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

final class PlantOutline {
    static final VoxelShape[] PLAX = stages(5, 4, 4, 6, 8, 10, 14, 15, 15);
    static final VoxelShape[] METALLEAT = stages(5, 3, 4, 6, 10, 10, 13, 13, 13);
    static final VoxelShape[] LUCRITE = stages(4, 2, 2, 2, 4, 7, 9, 12, 14);
    static final VoxelShape[] LAZULLIA = stages(5, 4, 4, 5, 6, 7, 9, 10, 11);
    static final VoxelShape[] GLIEONIA = stages(4, 4, 4, 7, 7, 13, 13, 13, 13);
    static final VoxelShape[] SWEED = stages(5, 4, 7, 12, 16);
    static final VoxelShape[] ASHROOM = stages(5, 6, 8, 10, 12);
    static final VoxelShape FLAXIUM = Block.column(8, 0, 14);

    private static VoxelShape[] stages(int halfWidth, int... heights) {
        return Arrays.stream(heights).mapToObj(height -> Block.column(halfWidth * 2, 0, height))
            .toArray(VoxelShape[]::new);
    }

    static VoxelShape at(VoxelShape[] shapes, int age) {
        return shapes[Math.min(age, shapes.length - 1)];
    }
}
