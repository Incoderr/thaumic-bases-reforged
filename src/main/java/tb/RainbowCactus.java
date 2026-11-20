package tb;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.CrucibleRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

final class RainbowCactus extends CactusBlock {
    static final String ID = "rainbow_cactus";
    private static final List<String> DYES = List.of("red", "green", "purple", "green", "cyan", "light_gray",
        "green", "gray", "pink", "lime", "green", "yellow", "light_blue", "magenta", "orange", "green");

    RainbowCactus(Properties properties) {
        super(properties);
    }

    static void register(PortRegistration.Blocks blocks, PortRegistration.Items items) {
        items.registerSimpleBlockItem(blocks.registerBlock(ID, RainbowCactus::new,
            p -> p.strength(0.4F).randomTicks().noOcclusion().sound(SoundType.WOOL)));
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {}

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var above = pos.above();
        if (!level.isEmptyBlock(above) || level.getBlockState(pos.below()).is(this)
            && level.getBlockState(pos.below(2)).is(this)) return;
        if (!net.neoforged.neoforge.common.CommonHooks.canCropGrow(level, above, state, true)) return;
        int age = state.getValue(AGE);
        if (age == 15 && defaultBlockState().canSurvive(level, above)) {
            level.setBlockAndUpdate(above, defaultBlockState());
            level.setBlockAndUpdate(pos, defaultBlockState());
        } else if (age < 15) {
            level.setBlock(pos, state.setValue(AGE, age + 1), UPDATE_CLIENTS);
        }
        net.neoforged.neoforge.common.CommonHooks.fireCropGrowPost(level, pos, state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        var level = params.getLevel();
        if (!level.getBlockState(pos.below()).is(this)) return List.of(new ItemStack(this));
        var random = level.getRandom();
        var drops = new ArrayList<ItemStack>();
        for (int i = 0; i < 3 + random.nextInt(8); i++) {
            String color = DYES.get(random.nextInt(DYES.size()));
            drops.add(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color + "_dye"))));
        }
        return drops;
    }

    static void research() {
        String key = "TB.RainbowCacti";
        String id = Bases.id(ID).toString();
        var icon = TcStack.of(id);
        TcRecipes.register(new CrucibleRecipe(id, key, icon, TcIngredient.item("minecraft:cactus"),
            new AspectList().add(Aspect.EXCHANGE, 24).add(Aspect.SENSES, 6)));
        var research = new AspectList().add(Aspect.LIFE, 6).add(Aspect.MAGIC, 6).add(Aspect.EXCHANGE, 6)
            .add(Aspect.CROP, 6).add(Aspect.ENTROPY, 6).add(Aspect.SENSES, 8);
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, research, -2, 4, 2, icon)
            .setParents("TB.Metalleat")
            .setPages(ResearchPage.text("tc.research_page.TB.RainbowCacti.1"), ResearchPage.crucible(id)));
    }
}
