package com.sieuus.railcraftengines.common.blocks.logic;

import com.sieuus.railcraftengines.common.util.steam.IFuelProvider;
import com.sieuus.railcraftengines.common.util.steam.SteamConstants;
import com.sieuus.railcraftengines.integration.railcraft.RailcraftFluids;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public final class BoilerLogic {

    private final BoilerData boilerData;
    private final IFuelProvider fuelProvider;
    private final FluidTank waterTank;
    private final FluidTank steamTank;
    private final Runnable onChanged;

    private int burnCycle;

    private double partialConversions;
    private double temperature = SteamConstants.COLD_TEMP;

    private double burnTime;
    private double currentItemBurnTime;

    public BoilerLogic(
            BoilerData boilerData,
            FluidTank steamTank,
            IFuelProvider fuelProvider,
            Runnable onChanged
    ) {
        this.boilerData = boilerData;
        this.steamTank = steamTank;
        this.fuelProvider = fuelProvider;
        this.onChanged = onChanged;

        this.waterTank = new FluidTank(
                boilerData.waterCapacity(),
                stack -> stack.is(FluidTags.WATER)
        ) {
            @Override
            protected void onContentsChanged() {
                onChanged.run();
            }
        };
    }

    public void update() {
        burnCycle++;

        if (burnCycle >= boilerData.ticksPerCycle()) {
            burnCycle = 0;

            if (isBurning()) {
                burnTime -= getFuelPerCycle();

                if (burnTime < 0) {
                    burnTime = 0;
                }
            }

            while (!isBurning()) {
                if (!addFuel()) {
                    break;
                }
            }

            convertSteam();
        }

        if (isBurning()) {
            increaseTemperature();
        } else {
            reduceTemperature();
        }

        onChanged.run();
    }

    private boolean addFuel() {
        double fuel = fuelProvider.burnFuelUnit();

        if (fuel <= 0) {
            return false;
        }

        burnTime += fuel;
        currentItemBurnTime = fuel;

        return true;
    }

    public double getFuelPerCycle() {
        double fuel = SteamConstants.FUEL_PER_BOILER_CYCLE;

        fuel -= boilerData.numTanks()
                * SteamConstants.FUEL_PER_BOILER_CYCLE
                * 0.0125D;

        fuel += SteamConstants.FUEL_HEAT_INEFFICIENCY
                * getHeatLevel();

        fuel += SteamConstants.FUEL_PRESSURE_INEFFICIENCY
                * (getMaxTemperature()
                / SteamConstants.MAX_HEAT_HIGH);

        fuel *= boilerData.numTanks();
        fuel *= boilerData.efficiency();

        return fuel;
    }

    public int convertSteam() {
        if (!isHot()) {
            return 0;
        }

        partialConversions +=
                boilerData.numTanks() * getHeatLevel();

        int waterCost = (int) partialConversions;

        if (waterCost <= 0) {
            return 0;
        }

        partialConversions -= waterCost;

        int availableWater =
                Math.min(
                        waterCost,
                        waterTank.getFluidAmount()
                );

        if (availableWater <= 0) {
            return 0;
        }

        Fluid steamFluid = RailcraftFluids.getSteam();

        if (steamFluid == Fluids.EMPTY) {
            return 0;
        }

        int steamAmount =
                SteamConstants.STEAM_PER_UNIT_WATER
                        * availableWater;

        int availableSteamSpace =
                steamTank.getCapacity()
                        - steamTank.getFluidAmount();

        int maxWaterForAvailableSteam =
                availableSteamSpace
                        / SteamConstants.STEAM_PER_UNIT_WATER;

        availableWater =
                Math.min(
                        availableWater,
                        maxWaterForAvailableSteam
                );

        if (availableWater <= 0) {
            return 0;
        }

        steamAmount =
                availableWater
                        * SteamConstants.STEAM_PER_UNIT_WATER;

        waterTank.drain(
                availableWater,
                IFluidHandler.FluidAction.EXECUTE
        );

        int filled = steamTank.fill(
                new FluidStack(
                        steamFluid,
                        steamAmount
                ),
                IFluidHandler.FluidAction.EXECUTE
        );

        return filled;
    }

    public void increaseTemperature() {
        double max = getMaxTemperature();

        if (temperature >= max) {
            temperature = max;
            return;
        }

        double step =
                SteamConstants.HEAT_STEP
                        * fuelProvider.getThermalEnergyLevel();

        double change =
                step
                        + (((max - temperature) / max)
                        * step
                        * 3.0D);

        change /= boilerData.numTanks();

        temperature += change;
        temperature = Math.min(temperature, max);
    }

    public void reduceTemperature() {
        if (temperature <= SteamConstants.COLD_TEMP) {
            temperature = SteamConstants.COLD_TEMP;
            return;
        }

        double step = SteamConstants.HEAT_STEP;

        double change =
                step
                        + ((temperature / getMaxTemperature())
                        * step
                        * 3.0D);

        change /= boilerData.numTanks();

        temperature -= change;

        temperature =
                Math.max(
                        temperature,
                        SteamConstants.COLD_TEMP
                );
    }

    public boolean isBurning() {
        return burnTime >= getFuelPerCycle();
    }

    public boolean isHot() {
        return temperature >= SteamConstants.BOILING_POINT;
    }

    public boolean isSuperHeated() {
        return temperature >= SteamConstants.SUPER_HEATED;
    }

    public double getTemperature() {
        return temperature;
    }

    public double getMaxTemperature() {
        return boilerData.maxHeat();
    }

    public double getHeatLevel() {
        return temperature / getMaxTemperature();
    }

    public double getBurnTime() {
        return burnTime;
    }

    public double getCurrentItemBurnTime() {
        return currentItemBurnTime;
    }

    public FluidTank getWaterTank() {
        return waterTank;
    }

    public FluidTank getSteamTank() {
        return steamTank;
    }

    public boolean needsFuel() {
        if (waterTank.getFluidAmount()
                < waterTank.getCapacity() / 3) {
            return true;
        }

        return fuelProvider.needsFuel();
    }

    public void load(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        temperature =
                tag.contains("BoilerTemperature")
                        ? tag.getDouble("BoilerTemperature")
                        : SteamConstants.COLD_TEMP;

        burnTime =
                tag.getDouble("BoilerBurnTime");

        currentItemBurnTime =
                tag.getDouble("BoilerCurrentItemBurnTime");

        partialConversions =
                tag.getDouble("BoilerPartialConversions");

        burnCycle =
                tag.getInt("BoilerBurnCycle");

        if (tag.contains("BoilerWaterTank")) {
            waterTank.readFromNBT(
                    registries,
                    tag.getCompound("BoilerWaterTank")
            );
        }
    }

    public void save(
            CompoundTag tag,
            HolderLookup.Provider registries
    ) {
        tag.putDouble(
                "BoilerTemperature",
                temperature
        );

        tag.putDouble(
                "BoilerBurnTime",
                burnTime
        );

        tag.putDouble(
                "BoilerCurrentItemBurnTime",
                currentItemBurnTime
        );

        tag.putDouble(
                "BoilerPartialConversions",
                partialConversions
        );

        tag.putInt(
                "BoilerBurnCycle",
                burnCycle
        );

        CompoundTag waterTag =
                new CompoundTag();

        waterTank.writeToNBT(
                registries,
                waterTag
        );

        tag.put(
                "BoilerWaterTank",
                waterTag
        );
    }

    public record BoilerData(
            int numTanks,
            int ticksPerCycle,
            double efficiency,
            float maxHeat,
            int waterCapacity,
            int steamCapacity
    ) {
    }
}