package com.sieuus.railcraftengines.common.util.energy;

/**
 * Energy conversion constants used by the Railcraft Engines port.
 *
 * The classic engines were specified in BuildCraft MJ. For this NeoForge port
 * we use the conventional/default conversion of 1 MJ = 10 FE.
 */
public final class EnergyConstants {

    public static final long FE_PER_MJ = 10L;

    private EnergyConstants() {
    }

    public static long mjToFe(long mj) {
        return mj * FE_PER_MJ;
    }
}
