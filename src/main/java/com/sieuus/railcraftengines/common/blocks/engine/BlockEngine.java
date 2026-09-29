package com.sieuus.railcraftengines.common.blocks.engine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import java.util.function.BiFunction;
import mods.railcraft.particle.RailcraftParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;

public class BlockEngine extends Block implements EntityBlock {

    public static final DirectionProperty FACING =
            BlockStateProperties.FACING;

    private final BiFunction<BlockPos, BlockState, ? extends TileEngine>
            blockEntityFactory;

    public BlockEngine(
            BlockBehaviour.Properties properties,
            BiFunction<BlockPos, BlockState, ? extends TileEngine> blockEntityFactory
    ) {
        super(properties);

        this.blockEntityFactory = blockEntityFactory;

        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.UP)
        );
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(
            BlockPlaceContext context
    ) {
        return defaultBlockState()
                .setValue(FACING, Direction.UP);
    }

    @Override
    public BlockState rotate(
            BlockState state,
            Rotation rotation
    ) {
        return state.setValue(
                FACING,
                rotation.rotate(state.getValue(FACING))
        );
    }

    @Override
    public BlockState mirror(
            BlockState state,
            Mirror mirror
    ) {
        return state.rotate(
                mirror.getRotation(
                        state.getValue(FACING)
                )
        );
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public BlockEntity newBlockEntity(
            BlockPos pos,
            BlockState state
    ) {
        return blockEntityFactory.apply(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof TileEngine engine) {
                TileEngine.tick(
                        tickLevel,
                        pos,
                        tickState,
                        engine
                );
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer) {

            BlockEntity blockEntity =
                    level.getBlockEntity(pos);

            if (blockEntity instanceof MenuProvider menuProvider) {
                serverPlayer.openMenu(menuProvider);
            }
        }

        return InteractionResult.SUCCESS;
    }
    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston
    ) {
        if (!state.is(newState.getBlock()) && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof TileEngineSteamHobby engine) {
                var inventory = engine.getInventory();

                for (int slot = 0; slot < inventory.getSlots(); slot++) {
                    ItemStack stack = inventory.getStackInSlot(slot).copy();

                    if (!stack.isEmpty()) {
                        inventory.setStackInSlot(slot, ItemStack.EMPTY);
                        Containers.dropItemStack(
                                level,
                                pos.getX(),
                                pos.getY(),
                                pos.getZ(),
                                stack
                        );
                    }
                }

                level.updateNeighbourForOutputSignal(pos, this);
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, Level level,
                            BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof TileEngineSteam engine)) {
            return;
        }

        TileEngine.EnergyStage stage = engine.getEnergyStage();

        if (!engine.isActive() && stage != TileEngine.EnergyStage.OVERHEAT) {
            return;
        }

        int count = switch (stage) {
            case BLUE -> 1;
            case GREEN -> 2;
            case YELLOW -> 3;
            case ORANGE -> 4;
            case RED -> 5;
            case OVERHEAT -> 8;
        };

        for (int i = 0; i < count; i++) {
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(
                    RailcraftParticleTypes.STEAM.get(),
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    random.nextGaussian() * 0.1,
                    random.nextDouble() * 0.01,
                    random.nextGaussian() * 0.1
            );

            if (particle != null) {
                particle.scale(0.15F);
            }
        }
    }
}