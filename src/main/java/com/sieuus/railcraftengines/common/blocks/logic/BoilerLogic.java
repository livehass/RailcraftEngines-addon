package com.sieuus.railcraftengines.common.blocks.logic;

import com.sieuus.railcraftengines.common.util.steam.IFuelProvider;
import com.sieuus.railcraftengines.common.util.steam.SteamConstants;
import com.sieuus.railcraftengines.integration.railcraft.RailcraftFluids;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

public final class BoilerLogic {

    private final BoilerData boilerData;
    private final IFuelProvider fuelProvider;
    private final FluidStacksResourceHandler waterTank;
    private final FluidStacksResourceHandler steamTank;
    private final Runnable onChanged;

    private int burnCycle;
    private boolean burning;

    private double partialConversions;
    private double temperature = SteamConstants.COLD_TEMP;

    private double burnTime;
    private double currentItemBurnTime;

    public BoilerLogic(
            BoilerData boilerData,
            FluidStacksResourceHandler steamTank,
            IFuelProvider fuelProvider,
            Runnable onChanged
    ) {
        this.boilerData = boilerData;
        this.steamTank = steamTank;
        this.fuelProvider = fuelProvider;
        this.onChanged = onChanged;

        this.waterTank = new FluidStacksResourceHandler(1, boilerData.waterCapacity()) {
            @Override
            public boolean isValid(int index, FluidResource resource) {
                return index == 0 && !resource.isEmpty()
                        && resource.toStack(1).is(FluidTags.WATER);
            }

            @Override
            protected void onContentsChanged(int index, FluidStack previousContents) {
                onChanged.run();
            }
        };
    }

    public void update() {
        burnCycle++;

        if (burnCycle >= boilerData.ticksPerCycle()) {
            burnCycle = 0;

            double fuelNeeded = getFuelPerCycle();

            while (burnTime < fuelNeeded) {
                if (!addFuel()) {
                    break;
                }
            }

            burning = burnTime >= fuelNeeded;

            if (burning) {
                burnTime -= fuelNeeded;
            }

            convertSteam();
        }

        if (burning) {
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
        currentItemBurnTime = burnTime;
        return true;
    }

    public boolean isBurning() {
        return burning;
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

        int availableWater = Math.min(waterCost, getWaterAmount());
        if (availableWater <= 0) {
            return 0;
        }

        Fluid steamFluid = RailcraftFluids.getSteam();
        if (steamFluid == Fluids.EMPTY) {
            return 0;
        }

        FluidResource steamResource = FluidResource.of(steamFluid);
        int steamPerWater = SteamConstants.STEAM_PER_UNIT_WATER;
        long availableSteamSpace = Math.max(0L,
                steamTank.getCapacityAsLong(0, steamResource)
                        - steamTank.getAmountAsLong(0));
        availableWater = (int) Math.min(availableWater,
                Math.min(availableSteamSpace, Integer.MAX_VALUE) / steamPerWater);
        if (availableWater <= 0) {
            return 0;
        }

        int steamAmount = availableWater * steamPerWater;

        // Commit both changes together; a rejected insertion must not consume water.
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = waterTank.extract(
                    0, waterTank.getResource(0), availableWater, transaction);
            if (extracted != availableWater) {
                return 0;
            }

            int inserted = steamTank.insert(0, steamResource, steamAmount, transaction);
            if (inserted != steamAmount) {
                return 0;
            }

            transaction.commit();
            return inserted;
        }
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

    public FluidStacksResourceHandler getWaterTank() {
        return waterTank;
    }

    public FluidStacksResourceHandler getSteamTank() {
        return steamTank;
    }

    public int getWaterAmount() {
        return waterTank.getAmountAsInt(0);
    }

    public int getWaterCapacity() {
        return boilerData.waterCapacity();
    }

    public boolean needsFuel() {
        if (getWaterAmount()
                < getWaterCapacity() / 3) {
            return true;
        }

        return fuelProvider.needsFuel();
    }

    public void load(ValueInput input) {
        temperature = input.getDoubleOr(
                "BoilerTemperature",
                SteamConstants.COLD_TEMP
        );
        burnTime = input.getDoubleOr("BoilerBurnTime", 0.0D);
        currentItemBurnTime = input.getDoubleOr(
                "BoilerCurrentItemBurnTime",
                0.0D
        );
        partialConversions = input.getDoubleOr(
                "BoilerPartialConversions",
                0.0D
        );
        burnCycle = input.getIntOr("BoilerBurnCycle", 0);
        burning = input.getBooleanOr(
                "BoilerBurning",
                burnTime >= getFuelPerCycle()
        );

        // Preserve the legacy FluidTank save format without depending on that class.
        FluidStack storedWater = input.childOrEmpty("BoilerWaterTank")
                .read("Fluid", FluidStack.CODEC).orElse(FluidStack.EMPTY);
        if (!storedWater.isEmpty() && storedWater.is(FluidTags.WATER)) {
            waterTank.set(0, FluidResource.of(storedWater),
                    Math.min(storedWater.getAmount(), getWaterCapacity()));
        } else {
            waterTank.set(0, FluidResource.EMPTY, 0);
        }
    }

    public void save(ValueOutput output) {
        output.putBoolean("BoilerBurning", burning);
        output.putDouble("BoilerTemperature", temperature);
        output.putDouble("BoilerBurnTime", burnTime);
        output.putDouble("BoilerCurrentItemBurnTime", currentItemBurnTime);
        output.putDouble("BoilerPartialConversions", partialConversions);
        output.putInt("BoilerBurnCycle", burnCycle);

        ValueOutput tankOutput = output.child("BoilerWaterTank");
        if (getWaterAmount() > 0) {
            tankOutput.store("Fluid", FluidStack.CODEC,
                    waterTank.getResource(0).toStack(getWaterAmount()));
        }
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
