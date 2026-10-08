package com.sieuus.railcraftengines.integration.railcraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public final class RailcraftFluids {

    private static final Identifier STEAM_ID =
            Identifier.fromNamespaceAndPath("railcraft", "steam");

    private RailcraftFluids() {
    }

    public static Fluid getSteam() {
        return BuiltInRegistries.FLUID
                .getOptional(STEAM_ID)
                .orElse(Fluids.EMPTY);
    }

    public static boolean isSteam(FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Fluid steam = getSteam();
        return steam != Fluids.EMPTY && stack.getFluid() == steam;
    }
}