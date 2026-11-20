package tb;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.crafting.ShapedArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.node.NodeBlockEntity;
import thaumcraft.registry.ModBlocks;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchPage;
import thaumcraft.wand.WandInteractable;

final class NodeLinker extends PortEntityBlock implements EntityBlock, WandInteractable {
    private static final int RANGE = 32;
    private static final String NAME = "node_linker";
    private static java.util.function.Supplier<NodeLinker> block;
    private static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Bases.ID);
    private static final java.util.function.Supplier<BlockEntityType<Link>> TYPE = TYPES.register(NAME,
        () -> BlockEntityType.Builder.of(Link::new, block.get()).build(null));
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Bases.ID);
    private static final java.util.function.Supplier<DataComponentType<GlobalPos>> RECEIVER = COMPONENTS.register("node_link_receiver",
        () -> DataComponentType.<GlobalPos>builder().persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC).build());

    private NodeLinker(Properties properties) { super(properties); }

    static void register(PortRegistration.Blocks blocks, PortRegistration.Items items) {
        var linker = blocks.registerBlock(NAME, NodeLinker::new, p -> p.strength(3, 15).noOcclusion());
        block = linker;
        items.registerSimpleBlockItem(linker);
    }

    static void registerTypes(IEventBus bus) {
        TYPES.register(bus);
        COMPONENTS.register(bus);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Link(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != TYPE.get()) return null;
        return (world, pos, blockState, entity) -> ((Link) entity).tick((ServerLevel) world);
    }

    @Override
    public InteractionResult onWandUseOn(Level level, BlockPos pos, Player player, ItemStack wand, Direction side) {
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!(server.getBlockEntity(pos) instanceof Link selected)) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) {
            selected.disconnect(server);
            wand.remove(RECEIVER.get());
            return feedback(player, "cleared");
        }
        if (node(server, pos) == null || selected.partner != null) return feedback(player, "unavailable");
        GlobalPos target = wand.get(RECEIVER.get());
        if (target == null || target.equals(GlobalPos.of(server.dimension(), pos))) {
            wand.set(RECEIVER.get(), GlobalPos.of(server.dimension(), pos));
            return feedback(player, "selected");
        }
        wand.remove(RECEIVER.get());
        if (!target.dimension().equals(server.dimension()) || !target.pos().closerThan(pos, RANGE + 0.01)
            || !server.hasChunkAt(target.pos()) || !server.mayInteract(player, target.pos())
            || !(server.getBlockEntity(target.pos()) instanceof Link receiver) || receiver.partner != null
            || node(server, target.pos()) == null) return feedback(player, "unavailable");
        receiver.setPartner(pos, false);
        selected.setPartner(target.pos(), true);
        return feedback(player, "linked");
    }

    private static InteractionResult feedback(Player player, String key) {
        player.sendSystemMessage(Component.translatable("message.thaumicbases.node_linker." + key));
        return InteractionResult.SUCCESS;
    }

    private static NodeBlockEntity node(Level level, BlockPos linker) {
        BlockPos below = linker.below();
        return level.getBlockState(below).is(ModBlocks.AURA_NODE.get())
            && level.getBlockEntity(below) instanceof NodeBlockEntity aura ? aura : null;
    }

    static final class Link extends BlockEntity implements PortEntityBlock.Removal {
        private BlockPos partner;
        private boolean transmitter;
        private int instability;

        Link(BlockPos pos, BlockState state) { super(TYPE.get(), pos, state); }

        private void setPartner(BlockPos pos, boolean transmitter) {
            partner = pos;
            this.transmitter = transmitter;
            instability = 0;
            setChanged();
        }

        private void disconnect(ServerLevel level) {
            if (partner != null && level.hasChunkAt(partner)
                && level.getBlockEntity(partner) instanceof Link other && worldPosition.equals(other.partner)) {
                other.setPartner(null, false);
            }
            setPartner(null, false);
        }

        void tick(ServerLevel level) {
            if (level.getGameTime() % 10 == 0) transfer(level);
        }

        void transfer(ServerLevel level) {
            if (partner == null || !level.hasChunkAt(partner)) return;
            if (!(level.getBlockEntity(partner) instanceof Link other) || !worldPosition.equals(other.partner)
                || node(level, worldPosition) == null || node(level, partner) == null) {
                disconnect(level);
                return;
            }
            if (!transmitter) return;
            NodeBlockEntity donor = node(level, worldPosition);
            NodeBlockEntity receiver = node(level, partner);
            if (donor.getAspectsBase().visSize() == 0) {
                level.removeBlock(worldPosition.below(), false);
                disconnect(level);
                return;
            }
            var random = level.getRandom();
            boolean base = donor.getAspects().visSize() == 0 || random.nextDouble() < 0.3;
            var source = base ? donor.getAspectsBase() : donor.getAspects();
            var aspects = source.getAspects();
            if (aspects.isEmpty()) return;
            Aspect aspect = aspects.get(random.nextInt(aspects.size()));
            source.reduce(aspect, 1);
            if (base) {
                int overflow = donor.getAspects().getAmount(aspect) - source.getAmount(aspect);
                if (overflow > 0) donor.getAspects().reduce(aspect, overflow);
            }
            boolean disrupted = instability >= 10 && random.nextInt(50) < instability;
            if (disrupted) {
                int outcome = random.nextInt(instability);
                instability = Math.max(0, instability - 8);
                var vent = worldPosition.below().north();
                if (level.getBlockState(vent).isAir()) level.setBlockAndUpdate(vent, ModBlocks.FLUX_GAS.get().defaultBlockState());
                if (outcome >= 49) {
                    level.removeBlock(worldPosition.below(), false);
                    level.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() - 0.5,
                        worldPosition.getZ() + 0.5, 1.3F, Level.ExplosionInteraction.NONE);
                    disconnect(level);
                    return;
                }
            } else if (base) {
                if (random.nextDouble() < 0.7) {
                    receiver.getAspectsBase().add(aspect, 1);
                    receiver.addToContainer(aspect, 1);
                }
            } else {
                receiver.addToContainer(aspect, 1);
            }
            if (base) instability++;
            donor.sync();
            receiver.sync();
            setChanged();
            var from = Vec3.atCenterOf(worldPosition);
            var to = Vec3.atCenterOf(partner);
            for (int step = 1; step <= 8; step++) {
                Vec3 point = from.lerp(to, step / 9.0);
                level.sendParticles(ParticleTypes.ENCHANT, point.x, point.y, point.z, 1, 0, 0, 0, 0);
            }
        }

        @Override
        public void onBlockRemoved(BlockPos pos, BlockState state) {
            if (getLevel() instanceof ServerLevel server) disconnect(server);
        }

        @Override
        protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            var output = new PortNbt.Output(tag, registries);
            if (partner != null) output.store("partner", BlockPos.CODEC, partner);
            output.putBoolean("transmitter", transmitter);
            output.putInt("instability", instability);
        }

        @Override
        protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            var input = new PortNbt.Input(tag, registries);
            partner = input.read("partner", BlockPos.CODEC).orElse(null);
            transmitter = input.getBooleanOr("transmitter", false);
            instability = Math.clamp(input.getIntOr("instability", 0), 0, 1000);
        }
    }

    static void research() {
        String key = "TB.NodeLinker";
        String id = Bases.id(NAME).toString();
        var vis = new AspectList();
        for (Aspect aspect : Aspect.getPrimalAspects()) vis.add(aspect, 15);
        var transducer = TcIngredient.item("thaumcraft:node_transducer");
        var frame = TcIngredient.item("thaumcraft:thaumium_block");
        var base = TcIngredient.item(Bases.id("void_metal_block").toString());
        TcRecipes.register(new ShapedArcaneRecipe(id, key, TcStack.of(id), vis, 3, 3,
            List.of(transducer, TcIngredient.item("thaumcraft:primordial_pearl"), transducer,
                frame, TcIngredient.item("thaumcraft:alumentum"), frame,
                base, TcIngredient.item("minecraft:obsidian"), base)));
        ResearchCategories.addResearch(new BasesResearch(key, Bases.ID, new AspectList()
            .add(Aspect.AURA, 8).add(Aspect.MOTION, 8).add(Aspect.MAGIC, 16).add(Aspect.EXCHANGE, 8),
            4, -23, 3, TcStack.of(id)).setParents("VISPOWER")
            .setPages(ResearchPage.text("tc.research_page." + key + ".1"), ResearchPage.arcane(id)));
    }
}
