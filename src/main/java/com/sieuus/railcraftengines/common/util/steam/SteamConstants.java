/*
 * Portions of this file are derived from the Railcraft project.
 *
 * Original Railcraft project by CovertJaguar:
 * https://github.com/Railcraft/Railcraft
 *
 * Adapted for Minecraft 1.21.1 / NeoForge.
 */

package com.sieuus.railcraftengines.common.util.steam;

public final class SteamConstants {

    public static final float COLD_TEMP = 20.0F;
    public static final float BOILING_POINT = 100.0F;
    public static final float SUPER_HEATED = 300.0F;

    public static final float MAX_HEAT_LOW = 500.0F;
    public static final float MAX_HEAT_HIGH = 1000.0F;

    public static final float HEAT_STEP = 0.05F;

    public static final float FUEL_PER_BOILER_CYCLE = 8.0F;
    public static final float FUEL_HEAT_INEFFICIENCY = 0.8F;
    public static final float FUEL_PRESSURE_INEFFICIENCY = 4.0F;

    public static final int STEAM_PER_UNIT_WATER = 160;
    public static final int STEAM_PER_10RF = 5;

    private SteamConstants() {
    }
}