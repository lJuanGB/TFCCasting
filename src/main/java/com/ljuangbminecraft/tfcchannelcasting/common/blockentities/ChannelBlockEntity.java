package com.ljuangbminecraft.tfcchannelcasting.common.blockentities;

import com.ljuangbminecraft.tfcchannelcasting.common.blocks.ChannelBlock;
import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;

public class ChannelBlockEntity extends TFCBlockEntity
{
    protected ChannelBlockEntity(BlockPos pos, BlockState state) {
        super(TFCCCBlockEntities.CHANNEL.get(), pos, state);
    }
    
    /*** The direction where the flow is coming from, 
     * as well as the distance (for downwards flow) 
     * 
     * Empty if the channel does not have a flow currently.
    */
    private Optional<Pair<Direction, Byte>> flowSource = Optional.empty();

    /*** True if the flow is coming from another channel,
     * false if it's coming from a crucible.
     */
    private boolean isConnectedToAnotherChannel = false;

    /*** Number of flows that go through this channel.
     * 
     * If a crucible is connected to 3 mold tables, each connection
     * has a path of channels from crucible to mold table. nFlows
     * is a counter of how many connections go through this channel.
     * Once this value reaches 0, then no flow is going through the
     * channel.
     */
    private int nFlows = 0;

    /*** Fluid to render */
    private ResourceLocation fluid = new ResourceLocation("");

    public boolean shouldRender()
    {
        return flowSource.isPresent();
    }

    public ResourceLocation getFluid() 
    {
        return fluid;
    }

    public Pair<Direction, Byte> getFlowSource()
    {
        return flowSource.get();
    }

    public boolean isConnectedToAnotherChannel()
    {
        return isConnectedToAnotherChannel;
    }

    public int getNumberOfFlows()
    {
        return nFlows;
    }

    public void setLinkProperties(Pair<Direction, Byte> flowSource, boolean isConnectedToAnotherChannel, int nFlows, ResourceLocation fluid)
    {
        this.flowSource = Optional.of(flowSource);
        this.isConnectedToAnotherChannel = isConnectedToAnotherChannel;
        this.nFlows = nFlows;
        this.fluid = fluid;

        level.setBlock(worldPosition, getBlockState().setValue(ChannelBlock.WITH_METAL, this.flowSource.isPresent()), 3);

        markForSync();
    }

    /***
     * When a mold table gets disconnected from the crucible (because
     * it filled the mold, it was broken, etc.) it notifies to its
     * source channel that it has finished accepting flow. This
     * notification is propagated upstream the channels.
     * 
     * Every channel that is notified reduces the internal counter of
     * flows going through the channel, and when this counter reaches
     * 0 then all flows through the channel are finished and this 
     * channel can stop rendering the fluid.
     */
    public void notifyBrokenLink(int linksBroken)
    {
        nFlows -= linksBroken;

        if (level != null)
        {
            flowSource.ifPresent(
                fs -> level.getBlockEntity(
                    worldPosition.relative(fs.getLeft(), fs.getRight()), TFCCCBlockEntities.CHANNEL.get()
                ).ifPresent(
                    channel -> channel.notifyBrokenLink(linksBroken)
                )
            );
        }
            
        if (nFlows <= 0)
        {
            flowSource = Optional.empty();
            fluid = new ResourceLocation("");
            level.setBlock(worldPosition, getBlockState().setValue(ChannelBlock.WITH_METAL, false), 3);
            markForSync();
        }   
    }

    /***
     * Returns true iff the link from this channel to the source crucible
     * has been broken.
     */
    public boolean isLinkBroken()
    {
        if (flowSource.isEmpty()) return true;

        assert level != null;

        // If expecting a channel, find the channel block entity
        // If it's not present (the channel was broken, for example),
        // then return true (broken link). Otherwise, recursively
        // call this function for the channel block entity found.
        // Also break if distance from source is >1 and there is no
        // air in between
        if (isConnectedToAnotherChannel)
        {
            Optional<ChannelBlockEntity> bEntity = level.getBlockEntity(
                worldPosition.relative(flowSource.get().getLeft(), flowSource.get().getRight()), 
                TFCCCBlockEntities.CHANNEL.get()
            );

            if (bEntity.isEmpty()) return true;

            for (byte i = 1; i < flowSource.get().getRight(); i++)
            {
                BlockPos rel = worldPosition.relative(flowSource.get().getLeft(), i);
                if (!level.getBlockState(rel).isAir()) return true;
            }

            return bEntity.get().isLinkBroken();
        }
        else // If expecting a crucible, then set broken only if crucible is not there
        {
            Optional<CrucibleBlockEntity> bEntity = level.getBlockEntity(
                worldPosition.relative(flowSource.get().getLeft(), flowSource.get().getRight()), 
                TFCBlockEntities.CRUCIBLE.get()
            );

            return bEntity.isEmpty();
        }
    }

    private static final byte NO_FLOW_BYTE = 99;

    @Override
    public void loadAdditional(CompoundTag nbt)
    {
        nFlows = nbt.getByte("nFlowsOut");
        isConnectedToAnotherChannel = nbt.getBoolean("useLongRenderBox");
        byte flowSourceByte = nbt.getByte("flowSource");
        byte flowSourceDistance = nbt.contains("flowSourceDistance") ? nbt.getByte("flowSourceDistance") : 1;

        flowSource = flowSourceByte != NO_FLOW_BYTE ? 
            Optional.of(Pair.of(Direction.values()[flowSourceByte], flowSourceDistance)) 
            : Optional.empty();
        
        fluid = new ResourceLocation(nbt.getString("texture"));
    }

    @Override
    public void saveAdditional(CompoundTag nbt)
    {
        nbt.putByte("nFlowsOut", (byte) nFlows);
        nbt.putBoolean("useLongRenderBox", isConnectedToAnotherChannel);
        nbt.putByte("flowSource", flowSource.isPresent() ? (byte) flowSource.get().getLeft().ordinal() : NO_FLOW_BYTE);
        nbt.putByte("flowSourceDistance", flowSource.isPresent() ? flowSource.get().getRight() : 1);
        nbt.putString("texture", fluid.toString());
    }

    @Override
    public AABB getRenderBoundingBox()
    {
        if (this.flowSource.isPresent())
        {
            if (this.flowSource.get().getLeft() == Direction.UP)
            {
                return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 1+this.flowSource.get().getRight(), 2));
            }
            else
            {
                return new AABB(worldPosition.offset(-1, 0, -1), worldPosition.offset(2, 2, 2));
            }
        }
        else
        {
            return super.getRenderBoundingBox();
        }
    }
}
