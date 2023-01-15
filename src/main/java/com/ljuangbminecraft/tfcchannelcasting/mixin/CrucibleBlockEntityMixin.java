package com.ljuangbminecraft.tfcchannelcasting.mixin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting.LOGGER;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ljuangbminecraft.tfcchannelcasting.common.ChannelFlow;
import com.ljuangbminecraft.tfcchannelcasting.common.WithChannelFlows;
import com.ljuangbminecraft.tfcchannelcasting.common.blockentities.MoldBlockEntity;

import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.capabilities.Capabilities;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;
import net.dries007.tfc.common.capabilities.heat.IHeatBlock;
import net.dries007.tfc.util.Helpers;
import net.dries007.tfc.util.Metal;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

@Mixin(CrucibleBlockEntity.class)
public abstract class CrucibleBlockEntityMixin extends BlockEntity implements WithChannelFlows
{
    public CrucibleBlockEntityMixin(BlockEntityType<?> pType, BlockPos pWorldPosition, BlockState pBlockState) {
        super(pType, pWorldPosition, pBlockState);
    }

    private Map<Direction, ChannelFlow> channelFlows = new HashMap<>();

    public Map<Direction, ChannelFlow> getChannelFlows()
    {
        return channelFlows;
    }

    public void generateNewChannelFlow(Direction dir)
    {
        channelFlows.put(dir, ChannelFlow.fromSource(level, worldPosition.relative(dir)));
    }

    @Inject(method = "serverTick", at = @At("RETURN"), cancellable = true, remap = false)
    private static void onServerTick(Level level,BlockPos pos, BlockState state, CrucibleBlockEntity crucible, CallbackInfo ci)
    {
        WithChannelFlows withChannelFlows = (WithChannelFlows) crucible;

        IFluidHandler crucibleFluidHandler = crucible.getCapability(Capabilities.FLUID).resolve().get();
        IHeatBlock crucibleHeatHandler = crucible.getCapability(HeatCapability.BLOCK_CAPABILITY).resolve().get();

        Metal metal = Metal.get(crucibleFluidHandler.getFluidInTank(0).getFluid());

        if (metal == null) return;

        if (metal.getMeltTemperature() < crucibleHeatHandler.getTemperature())
        {
            List<Direction> dirsFinished = new ArrayList<>();

            for (Direction dir : withChannelFlows.getChannelFlows().keySet())
            {
                ChannelFlow flow = withChannelFlows.getChannelFlows().get(dir);
                LOGGER.debug("Handling flow %s of crucible at %s".formatted(dir.toString(), crucible.getBlockPos()));

                flow.getMolds(level).forEach(
                    mold -> {
                        final FluidStack outputDrop = crucibleFluidHandler.drain(1, IFluidHandler.FluidAction.SIMULATE);
                        final FluidStack outputRemainder = Helpers.mergeOutputFluidIntoSlot(mold.getInventory(), outputDrop, crucible.getTemperature(), MoldBlockEntity.MOLD_SLOT);
                        if (outputRemainder.isEmpty())
                        {
                            LOGGER.debug("    Dumping in mold at " + mold.getBlockPos());

                            // Remainder was emptied, so do the extraction for real
                            crucibleFluidHandler.drain(1, IFluidHandler.FluidAction.EXECUTE);
                        }
                        else
                        {
                            LOGGER.debug("    Finished dumping in mold at " + mold.getBlockPos());

                            // Could not fill any longer, so remove the mold from the ChannelFlow
                            flow.removeMold(mold.getBlockPos());
                        }
                        crucible.markForSync();
                    }
                );

                // Flows with no molds left are finished
                if (flow.isFlowFinished())
                {
                    dirsFinished.add(dir);
                }
            }

            dirsFinished.stream().forEach(withChannelFlows.getChannelFlows()::remove);
        }
    }
}
