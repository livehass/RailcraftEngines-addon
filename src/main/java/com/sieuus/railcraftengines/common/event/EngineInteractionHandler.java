package com.sieuus.railcraftengines.common.event;

import com.sieuus.railcraftengines.RailcraftEngines;
import com.sieuus.railcraftengines.common.blocks.engine.TileEngine;
import mods.railcraft.api.item.Crowbar;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = RailcraftEngines.MODID)
public final class EngineInteractionHandler {

    private EngineInteractionHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRightClickBlock(
            PlayerInteractEvent.RightClickBlock event) {

        if (event.isCanceled()
                || event.getUseBlock() == TriState.FALSE
                || event.getUseItem() == TriState.FALSE) {
            return;
        }

        var player = event.getEntity();
        var level = event.getLevel();
        var pos = event.getPos();
        var hand = event.getHand();
        var stack = player.getItemInHand(hand);

        if (player.isSpectator()
                || !player.mayBuild()
                || !level.mayInteract(player, pos)) {
            return;
        }

        if (!(stack.getItem() instanceof Crowbar crowbar)) {
            return;
        }

        if (!(level.getBlockEntity(pos) instanceof TileEngine engine)) {
            return;
        }

        if (!crowbar.canWhack(player, hand, stack, pos)) {
            return;
        }

        boolean rotating = player.isShiftKeyDown();
        boolean overheated =
                engine.getEnergyStage() == TileEngine.EnergyStage.OVERHEAT;

        // Prevent the crowbar's default rotation and menu interaction.
        event.setCanceled(true);
        event.setCancellationResult(level.isClientSide()
                ? InteractionResult.SUCCESS
                : InteractionResult.CONSUME);

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (rotating) {
            // Overheated engines must be reset before rotating.
            if (overheated) {
                return;
            }

            engine.setFacing(nextFacing(engine.getFacing()));
            crowbar.onWhack(serverPlayer, hand, stack, pos);
        } else if (overheated) {
            engine.resetEnergyStage();
            crowbar.onWhack(serverPlayer, hand, stack, pos);
        }
    }

    private static Direction nextFacing(Direction facing) {
        return switch (facing) {
            case UP -> Direction.NORTH;
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.DOWN;
            case DOWN -> Direction.UP;
        };
    }
}
