package tb;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.block.InfusionStabiliser;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;

public final class MagicalAnvil extends AnvilBlock implements InfusionStabiliser {
    private static final String THAUMIUM = "thaumium_anvil";
    private static final String VOID = "void_anvil";
    private static final String THAUMIUM_RESEARCH = "TB.ThaumicAnvil";
    private static final String VOID_RESEARCH = "TB.VoidAnvil";
    private static final List<String> DAMAGE_PREFIXES = List.of("", "chipped_", "damaged_");

    private final String material;
    private final int damage;

    private MagicalAnvil(Properties properties, String material, int damage) {
        super(properties);
        this.material = material;
        this.damage = damage;
    }

    static void register(DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        for (String material : List.of(THAUMIUM, VOID)) {
            for (int stage = 0; stage < DAMAGE_PREFIXES.size(); stage++) {
                int wear = stage;
                items.registerSimpleBlockItem(blocks.registerBlock(DAMAGE_PREFIXES.get(stage) + material,
                    properties -> new MagicalAnvil(properties, material, wear),
                    properties -> {
                        Properties anvil = properties.strength(material.equals(VOID) ? 12 : 5,
                            1200).sound(SoundType.ANVIL).requiresCorrectToolForDrops();
                        return material.equals(VOID) && wear > 0 ? anvil.randomTicks() : anvil;
                    }));
            }
        }
    }

    public boolean isVoid() {
        return material.equals(VOID);
    }

    public BlockState damagedState(BlockState state) {
        return damage == DAMAGE_PREFIXES.size() - 1 ? null : stateAtWear(state, damage + 1);
    }

    private BlockState stateAtWear(BlockState state, int stage) {
        var block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(Bases.id(DAMAGE_PREFIXES.get(stage) + material));
        return block.defaultBlockState().setValue(FACING, state.getValue(FACING));
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
            (id, inventory, player) -> new Menu(id, inventory, ContainerLevelAccess.create(level, pos), isVoid()),
            Component.translatable(getDescriptionId()));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        if (level.getBlockState(pos).is(this)) heal(state, level, pos);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        heal(state, level, pos);
    }

    private void heal(BlockState state, ServerLevel level, BlockPos pos) {
        if (isVoid() && damage > 0) level.setBlockAndUpdate(pos, stateAtWear(state, damage - 1));
    }

    @Override
    public boolean canStabiliseInfusion(Level level, BlockPos pos) {
        return true;
    }

    public static final class Menu extends AnvilMenu {
        private final boolean voidMetal;

        Menu(int id, Inventory inventory, ContainerLevelAccess access, boolean voidMetal) {
            super(id, inventory, access);
            this.voidMetal = voidMetal;
        }

        public int exclusiveCostLimit() {
            return voidMetal ? Integer.MAX_VALUE : 60;
        }

        @Override
        protected void createResultInternal() {
            super.createResultInternal();
            var result = resultSlots.getItem(0);
            if (!result.isEmpty()) {
                result.remove(DataComponents.REPAIR_COST);
                if (voidMetal) setCost(7);
            }
        }
    }

    static void research() {
        String thaumium = Bases.id(THAUMIUM).toString();
        var thaumiumResult = TcStack.of(thaumium);
        var plate = TcIngredient.item("thaumcraft:thaumium_block");
        var ingot = TcIngredient.item("thaumcraft:thaumium_ingot");
        var vis = new AspectList();
        for (Aspect aspect : Aspect.getPrimalAspects()) vis.add(aspect, 25);
        TcRecipes.register(new ShapedArcaneRecipe(thaumium, THAUMIUM_RESEARCH, thaumiumResult, vis, 3, 3,
            List.of(plate, plate, plate, TcIngredient.EMPTY, ingot, TcIngredient.EMPTY, ingot, ingot, ingot)));
        ResearchCategories.addResearch(new BasesResearch(THAUMIUM_RESEARCH, Bases.ID,
            new AspectList().add(Aspect.CRAFT, 4).add(Aspect.TOOL, 3), 9, 4, 2, thaumiumResult)
            .setParents("THAUMIUM").setPages(ResearchPage.text("tc.research_page." + THAUMIUM_RESEARCH + ".1"),
                ResearchPage.arcane(thaumium)));

        String voidAnvil = Bases.id(VOID).toString();
        var voidResult = TcStack.of(voidAnvil);
        var voidBlock = TcIngredient.item(Bases.id("void_metal_block").toString());
        TcRecipes.register(new InfusionRecipe(voidAnvil, VOID_RESEARCH, voidResult, 3,
            new AspectList().add(Aspect.CRAFT, 32).add(Aspect.TOOL, 24).add(Aspect.WEAPON, 24).add(Aspect.ORDER, 48),
            TcIngredient.item(thaumium), List.of(voidBlock, voidBlock, voidBlock, voidBlock, voidBlock)));
        ResearchCategories.addResearch(new BasesResearch(VOID_RESEARCH, Bases.ID,
            new AspectList().add(Aspect.CRAFT, 4).add(Aspect.TOOL, 3).add(Aspect.ORDER, 4).add(Aspect.WEAPON, 4),
            12, 4, 2, voidResult).setParents(THAUMIUM_RESEARCH, "VOIDMETAL")
            .setPages(ResearchPage.text("tc.research_page." + VOID_RESEARCH + ".1"), ResearchPage.infusion(voidAnvil)));
    }
}
