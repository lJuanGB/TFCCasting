package com.ljuangbminecraft.tfcchannelcasting.common.blocks;

import static com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting.MOD_ID;
import static net.dries007.tfc.common.fluids.TFCFluids.*;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ljuangbminecraft.tfcchannelcasting.common.items.TFCCCItems;

import net.dries007.tfc.common.fluids.ExtendedFluidType;
import net.dries007.tfc.common.fluids.FluidRegistryObject;
import net.dries007.tfc.common.fluids.FluidTypeClientProperties;
import net.dries007.tfc.common.fluids.MixingFluid;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.registry.RegistrationHelpers;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class TFCCCFluids 
{
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, MOD_ID);

    public static final Map<ExtraFluid, FluidRegistryObject<ForgeFlowingFluid>> EXTRA_FLUIDS = Helpers.mapOfKeys(ExtraFluid.class, fluid -> register(
        fluid.getSerializedName(),
        properties -> properties.block(TFCCCBlocks.EXTRA_FLUIDS.get(fluid)).bucket(TFCCCItems.EXTRA_FLUID_BUCKETS.get(fluid)),
            waterLike().descriptionId("fluid.tfcchannelcasting." + fluid.getSerializedName()).canConvertToSource(false),
            new FluidTypeClientProperties(fluid.getColor(), MOLTEN_STILL, MOLTEN_FLOW, null, null),
        MixingFluid.Source::new,
        MixingFluid.Flowing::new
    ));

    private static <F extends FlowingFluid> FluidRegistryObject<F> register(String name, Consumer<ForgeFlowingFluid.Properties> builder, FluidType.Properties typeProperties, FluidTypeClientProperties clientProperties, Function<ForgeFlowingFluid.Properties, F> sourceFactory, Function<ForgeFlowingFluid.Properties, F> flowingFactory)
    {
        // Names `metal/foo` to `metal/flowing_foo`
        final int index = name.lastIndexOf('/');
        final String flowingName = index == -1 ? "flowing_" + name : name.substring(0, index) + "/flowing_" + name.substring(index + 1);

        return RegistrationHelpers.registerFluid(FLUID_TYPES, FLUIDS, name, name, flowingName, builder, () -> new ExtendedFluidType(typeProperties, clientProperties), sourceFactory, flowingFactory);
    }

    private static FluidType.Properties waterLike()
    {
        return FluidType.Properties.create()
                .adjacentPathType(BlockPathTypes.WATER)
                // .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                // .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .canConvertToSource(false)
                .canDrown(true)
                .canExtinguish(true)
                .canHydrate(true)
                .canPushEntity(true)
                .canSwim(true)
                .supportsBoating(true);
    }
}
