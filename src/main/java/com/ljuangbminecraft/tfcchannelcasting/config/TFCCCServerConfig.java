package com.ljuangbminecraft.tfcchannelcasting.config;

import static com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting.MOD_ID;
import java.util.function.Function;

import net.dries007.tfc.util.Alloy;
import net.minecraftforge.common.ForgeConfigSpec.*;

public class TFCCCServerConfig
{
    public final IntValue moldHeartCapacity;

    TFCCCServerConfig(Builder innerBuilder)
    {
        Function<String, Builder> builder = name -> innerBuilder.translation(MOD_ID + ".config.server." + name);

        innerBuilder.push("molds");

        moldHeartCapacity = builder.apply("moldHeartCapacity").comment("Tank capacity of a Pickaxe Head mold (in mB).").defineInRange("moldHeartCapacity", 100, 0, Alloy.MAX_ALLOY);
        
        innerBuilder.pop();
    }
}