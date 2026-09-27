package com.sieuus.railcraftengines.common.util.steam;

public interface IFuelProvider {

    double burnFuelUnit();

    default double getThermalEnergyLevel() {
        return 1.0D;
    }

    default boolean needsFuel() {
        return false;
    }
}