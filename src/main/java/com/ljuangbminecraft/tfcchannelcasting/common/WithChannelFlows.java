package com.ljuangbminecraft.tfcchannelcasting.common;

import java.util.Map;

import net.minecraft.core.Direction;

public interface WithChannelFlows 
{
    public Map<Direction, ChannelFlow> getChannelFlows();

    public void generateNewChannelFlow(Direction dir);
}
