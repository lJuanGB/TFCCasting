package com.ljuangbminecraft.tfcchannelcasting.common.recipes.outputs;

import com.google.gson.JsonObject;

import net.dries007.tfc.util.JsonHelpers;
import net.dries007.tfc.util.calendar.Calendars;
import net.dries007.tfc.util.calendar.Month;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record DateRangeModifyCondition(int minCalendarDayOfMonth, Month minCalendarMonthOfYear,
        int maxCalendarDayOfMonth, Month maxCalendarMonthOfYear) implements ModifyCondition {

    @Override
    public boolean shouldApply(ItemStack stack, ItemStack input) {
        int TOTAL_DAYS_IN_YEAR = Calendars.SERVER.getCalendarDaysInMonth() * Month.values().length;

        int minDay = getDayInt(minCalendarDayOfMonth, minCalendarMonthOfYear);
        int maxDay = getDayInt(maxCalendarDayOfMonth, maxCalendarMonthOfYear);

        if (maxDay < minDay) {
            minDay += TOTAL_DAYS_IN_YEAR;
        }

        int currentDay = getDayInt(Calendars.SERVER.getCalendarDayOfMonth(), Calendars.SERVER.getCalendarMonthOfYear());

        return (minDay <= currentDay && currentDay <= maxDay)
                || (minDay <= currentDay + TOTAL_DAYS_IN_YEAR && currentDay + TOTAL_DAYS_IN_YEAR <= maxDay);
    }

    private static int getDayInt(int calendarDayOfMonth, Month calendarMonthOfYear) {
        return calendarDayOfMonth
                + (calendarMonthOfYear.ordinal() - 1) * Calendars.SERVER.getCalendarDaysInMonth();
    }

    @Override
    public Serializer serializer() {
        return Serializer.INSTANCE;
    }

    public enum Serializer implements ModifyCondition.Serializer<DateRangeModifyCondition> {
        INSTANCE;

        @Override
        public DateRangeModifyCondition fromJson(JsonObject json) {
            final int minDay = JsonHelpers.getAsInt(json, "min_day");
            final int minMonth = JsonHelpers.getAsInt(json, "min_month");
            final int maxDay = JsonHelpers.getAsInt(json, "max_day");
            final int maxMonth = JsonHelpers.getAsInt(json, "max_month");

            assert minDay >= 1;
            assert maxDay >= 1;
            assert minMonth >= 1 && minMonth <= 12;
            assert maxMonth >= 1 && maxMonth <= 12;

            return new DateRangeModifyCondition(minDay, Month.valueOf(minMonth - 1), maxDay,
                    Month.valueOf(maxMonth - 1));
        }

        @Override
        public DateRangeModifyCondition fromNetwork(FriendlyByteBuf buffer) {
            final int minDay = buffer.readInt();
            final int minMonth = buffer.readInt();
            final int maxDay = buffer.readInt();
            final int maxMonth = buffer.readInt();
            return new DateRangeModifyCondition(minDay, Month.valueOf(minMonth), maxDay,
                    Month.valueOf(maxMonth));
        }

        @Override
        public void toNetwork(DateRangeModifyCondition modifier, FriendlyByteBuf buffer) {
            buffer.writeInt(modifier.minCalendarDayOfMonth);
            buffer.writeInt(modifier.minCalendarMonthOfYear.ordinal());
            buffer.writeInt(modifier.maxCalendarDayOfMonth);
            buffer.writeInt(modifier.maxCalendarMonthOfYear.ordinal());
        }
    }

}
