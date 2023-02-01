package com.ljuangbminecraft.tfcchannelcasting.common.blockentities;

import java.util.Optional;

import com.ljuangbminecraft.tfcchannelcasting.common.blocks.ChannelBlock;

import net.dries007.tfc.common.blockentities.CrucibleBlockEntity;
import net.dries007.tfc.common.blockentities.TFCBlockEntities;
import net.dries007.tfc.common.blockentities.TFCBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

public class ChannelBlockEntity extends TFCBlockEntity
{
    protected ChannelBlockEntity(BlockPos pos, BlockState state) {
        super(TFCCCBlockEntities.CHANNEL.get(), pos, state);
    }
    
    /*** The direction where the flow is coming from 
     * 
     * Empty if the channel does not have a flow currently.
    */
    private Optional<Direction> flowSource = Optional.empty();

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

    /*** Color of the flow to render */
    private int color = 0;
    /*** Texture of the flow to render */
    private ResourceLocation texture = new ResourceLocation("");

    public boolean shouldRender()
    {
        return flowSource.isPresent();
    }

    public int getColor() 
    {
        return color;
    }

    public ResourceLocation getTexture() 
    {
        return texture;
    }

    public Direction getFlowSource()
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

    public void setLinkProperties(Direction flowSource, boolean isConnectedToAnotherChannel, int nFlows, int color, ResourceLocation texture)
    {
        this.flowSource = Optional.of(flowSource);
        this.isConnectedToAnotherChannel = isConnectedToAnotherChannel;
        this.nFlows = nFlows;
        this.color = color;
        this.texture = texture;

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
                    worldPosition.relative(fs), TFCCCBlockEntities.CHANNEL.get()
                ).ifPresent(
                    channel -> channel.notifyBrokenLink(linksBroken)
                )
            );
        }
            
        if (nFlows <= 0)
        {
            flowSource = Optional.empty();
            color = 0;
            texture = new ResourceLocation("");
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
        if (isConnectedToAnotherChannel)
        {
            Optional<ChannelBlockEntity> bEntity = level.getBlockEntity(
                worldPosition.relative(flowSource.get()), TFCCCBlockEntities.CHANNEL.get()
            );

            if (bEntity.isEmpty()) return true;

            return bEntity.get().isLinkBroken();
        }
        else // If expecting a crucible, then set broken only if crucible is not there
        {
            Optional<CrucibleBlockEntity> bEntity = level.getBlockEntity(
                worldPosition.relative(flowSource.get()), TFCBlockEntities.CRUCIBLE.get()
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
        flowSource = flowSourceByte != NO_FLOW_BYTE ? Optional.of(Direction.values()[flowSourceByte]) : Optional.empty();
        texture = new ResourceLocation(nbt.getString("texture"));
        color = nbt.getInt("color");
    }

    @Override
    public void saveAdditional(CompoundTag nbt)
    {
        nbt.putByte("nFlowsOut", (byte) nFlows);
        nbt.putBoolean("useLongRenderBox", isConnectedToAnotherChannel);
        nbt.putByte("flowSource", flowSource.isPresent() ? (byte) flowSource.get().ordinal() : NO_FLOW_BYTE);
        nbt.putString("texture", texture.toString());
        nbt.putInt("color", color);
    }
}
