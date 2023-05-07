package com.ljuangbminecraft.tfcchannelcasting.common.blocks;

import static com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting.MOD_ID;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.eerussianguy.firmalife.common.blocks.FLBlocks;
import com.eerussianguy.firmalife.common.items.FLItems;
import com.ljuangbminecraft.tfcchannelcasting.common.items.TFCCCItems;

import net.dries007.tfc.common.fluids.FlowingFluidRegistryObject;
import net.dries007.tfc.common.fluids.MixingFluid;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.registry.RegistrationHelpers;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import static net.dries007.tfc.common.fluids.TFCFluids.MOLTEN_STILL;
import static net.dries007.tfc.common.fluids.TFCFluids.MOLTEN_FLOW;

public class TFCCCFluids 
{
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, MOD_ID);

    public static final Map<ExtraFluid, FlowingFluidRegistryObject<ForgeFlowingFluid>> EXTRA_FLUIDS = Helpers.mapOfKeys(ExtraFluid.class, fluid -> register(
        fluid.getSerializedName(),
        "flowing_" + fluid.getSerializedName(),
        properties -> properties.block(TFCCCBlocks.EXTRA_FLUIDS.get(fluid)).bucket(TFCCCItems.EXTRA_FLUID_BUCKETS.get(fluid)),
        FluidAttributes.builder(MOLTEN_STILL, MOLTEN_FLOW)
            .translationKey("fluid.tfcchannelcasting." + fluid.getSerializedName())
            .color(fluid.getColor())
            .sound(SoundEvents.BUCKET_FILL, SoundEvents.BUCKET_EMPTY),
        MixingFluid.Source::new,
        MixingFluid.Flowing::new
    ));

    private static <F extends FlowingFluid> FlowingFluidRegistryObject<F> register(String sourceName, String flowingName, Consumer<ForgeFlowingFluid.Properties> builder, FluidAttributes.Builder attributes, Function<ForgeFlowingFluid.Properties, F> sourceFactory, Function<ForgeFlowingFluid.Properties, F> flowingFactory)
    {
        return RegistrationHelpers.registerFluid(FLUIDS, sourceName, flowingName, builder, attributes, sourceFactory, flowingFactory);
    }    
}
