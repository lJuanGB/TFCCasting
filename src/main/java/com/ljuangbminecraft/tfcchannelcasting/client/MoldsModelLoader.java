package com.ljuangbminecraft.tfcchannelcasting.client;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.joml.Vector3f;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.ljuangbminecraft.tfcchannelcasting.TFCChannelCasting;

import net.dries007.tfc.util.Helpers;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.ElementsModel;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;

public class MoldsModelLoader implements IGeometryLoader<ElementsModel> {
    
    public static final ResourceLocation MOLD_LOADER = new ResourceLocation(TFCChannelCasting.MOD_ID, "mold");

    public static void register(ModelEvent.RegisterGeometryLoaders event) {
        event.register(MOLD_LOADER.getPath(), new MoldsModelLoader());
    }

    @Override
    public ElementsModel read(JsonObject json, JsonDeserializationContext deserializationContext) throws JsonParseException {    
        final JsonArray pattern = json.getAsJsonArray("pattern");
        
        final int height = pattern.size();
        if (height != 14) throw new JsonSyntaxException("Invalid pattern: must 14 rows (has " + height + ")");

        boolean[][] full = new boolean[14][14];

        for (int r = 0; r < 14; ++r)
        {
            String row = GsonHelper.convertToString(pattern.get(r), "pattern[" + r + "]");
            final int width = row.length();
            if (width != 14) throw new JsonSyntaxException("Invalid pattern: must 14 columns (has " + width + " in row "+ r +")");

            for (int c = 0; c < 14; c++)
            {
                full[r][c] = row.charAt(c) != ' ';
            }
        }

        return new ElementsModel(generateBlockElementsFromPattern(full));
    }

    public static @Nonnull List<BlockElement> generateBlockElementsFromPattern(boolean[][] pattern)
    {
        ArrayList<BlockElement> elements = new ArrayList<>();

        int from_y = 1;
        int to_y = 2;
        for (int r = 0; r < 14; ++r)
        {
            int from_x = r+1;
            int to_x = r+2;
            for (int c = 0; c < 14; c++)
            {
                if (!pattern[r][c]) continue;

                int from_z = c+1;
                int to_z = c+2;

                Vector3f from = new Vector3f(from_x, from_y, from_z);
                Vector3f to = new Vector3f(to_x, to_y, to_z);

                elements.add( new BlockElement(
                    from, to,
                    Helpers.mapOfKeys(Direction.class, 
                        direction -> new BlockElementFace(
                            null, -1, "#0", 
                            new BlockFaceUV(autoRelativeUV(direction, from, to), 0)
                        )
                    ),
                    null,
                    true,
                    net.minecraftforge.client.model.ForgeFaceData.DEFAULT
                ));
            }
        }
        return elements;
    }

    // Algorithm copied from BlockBench
    private static float[] autoRelativeUV(Direction direction, Vector3f from, Vector3f to)
    {
        switch (direction) {
            case NORTH:
                return new float[]{16-to.x, 16-to.y, 16-from.x, 16-from.y};
            case SOUTH:
                return new float[]{from.x, 16-to.y, to.x, 16-from.y};
            case WEST:
                return new float[]{from.z, 16-to.y, to.z, 16-from.y};
            case EAST:
                return new float[]{16-to.z, 16-to.y, 16-from.z, 16-from.y};
            case UP:
                return new float[]{from.x, from.z, to.x, to.z};
            case DOWN:
                return new float[]{from.x, 16-to.z, to.x, 16-from.z};
        }

        return new float[]{};
    }
}
