package com.ljuangbminecraft.tfcchannelcasting.common.blocks;

import java.util.Locale;

import net.minecraft.util.StringRepresentable;

public enum ExtraFluid implements StringRepresentable
{
    MILK_CHOCOLATE(0xFF59420A),
    WHITE_CHOCOLATE(0xFFF8F3E9),
    DARK_CHOCOLATE(0xFF3C321A),
    ;

    private final String id;
    private final int color;

    ExtraFluid(int color)
    {
        this.id = name().toLowerCase(Locale.ROOT);
        this.color = color;
    }

    @Override
    public String getSerializedName()
    {
        return id;
    }

    public int getColor()
    {
        return color;
    }
}