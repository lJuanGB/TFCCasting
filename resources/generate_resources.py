import copy
from enum import Enum, auto
import json
import sys
from typing import *

from mcresources import RecipeContext, ResourceManager, utils
from mcresources.type_definitions import Json

POTTERY_MELT = 1400 - 1
POTTERY_HEAT_CAPACITY = 1.2  # Heat Capacity # Useful comment


class Category(Enum):
    fruit = auto()
    vegetable = auto()
    grain = auto()
    bread = auto()
    dairy = auto()
    meat = auto()
    cooked_meat = auto()
    other = auto()


class Size(Enum):
    tiny = auto()
    very_small = auto()
    small = auto()
    normal = auto()
    large = auto()
    very_large = auto()
    huge = auto()


class Weight(Enum):
    very_light = auto()
    light = auto()
    medium = auto()
    heavy = auto()
    very_heavy = auto()


class Metal(NamedTuple):
    tier: int
    types: Set[str]
    heat_capacity_base: float  # Do not access directly, use one of specific or ingot heat capacity.
    melt_temperature: float
    melt_metal: Optional[str]

    def specific_heat_capacity(self) -> float:
        return round(300 / self.heat_capacity_base) / 100_000

    def ingot_heat_capacity(self) -> float:
        return 1 / self.heat_capacity_base


MOLDS = (
    "INGOT",
    "PICKAXE_HEAD",
    "PROPICK_HEAD",
    "AXE_HEAD",
    "SHOVEL_HEAD",
    "HOE_HEAD",
    "CHISEL_HEAD",
    "HAMMER_HEAD",
    "SAW_BLADE",
    "JAVELIN_HEAD",
    "SWORD_BLADE",
    "MACE_HEAD",
    "KNIFE_BLADE",
    "SCYTHE_BLADE",
    "FIRE_INGOT",
    "BELL",
)

CUSTOM_MOLDS = ("HEART",)

CHOCOLATES = ("dark_chocolate", "milk_chocolate", "white_chocolate")

CHOCOLATES_METALS = {
    chocolate: Metal(0, set(), 0.8, 300, None) for chocolate in CHOCOLATES
}

LANG = ("en_US",)

NORMAL_CHOCOLATE_FOOD_DATA = {
    "hunger": 4,
    "saturation": 1,
    "decay_modifier": 0.3,
    "grain": 0.5,
    "dairy": 0.5,
}

SPECIAL_CHOCOLATE_FOOD_DATA = {
    "hunger": 4,
    "saturation": 1,
    "decay_modifier": 0.3,
    "grain": 1,
    "dairy": 1,
}

JAM_CHOCOLATE_FOOD_DATA = {"fruit": 0.75}

FIRMALIFE_CONDITIONS = [{"type": "forge:mod_loaded", "modid": "firmalife"}]


def not_rotten(ingredient: Json) -> Json:
    return {"type": "tfc:not_rotten", "ingredient": utils.ingredient(ingredient)}


def lang(key: str, *args) -> str:
    return (
        ((key % args) if len(args) > 0 else key)
        .replace("_", " ")
        .replace("/", " ")
        .title()
    )


def fluid_ingredient(data_in: Json) -> Json:
    if isinstance(data_in, dict):
        return data_in
    elif isinstance(data_in, List):
        return [*utils.flatten_list([fluid_ingredient(e) for e in data_in])]
    else:
        fluid, tag, amount, _ = utils.parse_item_stack(data_in, False)
        if tag:
            return {"tag": fluid}
        else:
            return


def item_stack_ingredient(data_in: Json):
    if isinstance(data_in, dict):
        if "type" in data_in:
            return item_stack_ingredient({"ingredient": data_in})
        return {
            "ingredient": utils.ingredient(data_in["ingredient"]),
            "count": data_in["count"] if data_in.get("count") is not None else None,
        }
    if pair := utils.maybe_unordered_pair(data_in, int, object):
        count, item = pair
        return {"ingredient": fluid_ingredient(item), "count": count}
    item, tag, count, _ = utils.parse_item_stack(data_in, False)
    if tag:
        return {"ingredient": {"tag": item}, "count": count}
    else:
        return {"ingredient": {"item": item}, "count": count}


def fluid_stack_ingredient(data_in: Json) -> Json:
    if isinstance(data_in, dict):
        return {
            "ingredient": fluid_ingredient(data_in["ingredient"]),
            "amount": data_in["amount"],
        }
    if pair := utils.maybe_unordered_pair(data_in, int, object):
        amount, fluid = pair
        return {"ingredient": fluid_ingredient(fluid), "amount": amount}
    fluid, tag, amount, _ = utils.parse_item_stack(data_in, False)
    if tag:
        return {"ingredient": {"tag": fluid}, "amount": amount}
    else:
        return {"ingredient": fluid, "amount": amount}


def fluid_item_ingredient(fluid: Json, delegate: Json = None):
    return {
        "type": "tfc:fluid_item",
        "ingredient": delegate,
        "fluid_ingredient": fluid_stack_ingredient(fluid),
    }


def food_item(
    rm: ResourceManager,
    name_parts: utils.ResourceIdentifier,
    ingredient: utils.Json,
    category: Category,
    hunger: int,
    saturation: float,
    water: int,
    decay: float,
    fruit: Optional[float] = None,
    veg: Optional[float] = None,
    protein: Optional[float] = None,
    grain: Optional[float] = None,
    dairy: Optional[float] = None,
):
    rm.item_tag("tfc:foods", ingredient)
    rm.data(
        ("tfc", "food_items", name_parts),
        {
            "ingredient": utils.ingredient(ingredient),
            "category": category.name,
            "hunger": hunger,
            "saturation": saturation,
            "water": water if water != 0 else None,
            "decay_modifier": decay,
            "fruit": fruit,
            "vegetables": veg,
            "protein": protein,
            "grain": grain,
            "dairy": dairy,
        },
    )
    if category in (Category.fruit, Category.vegetable):
        rm.item_tag("tfc:foods/%ss" % category.name.lower(), ingredient)
    if category in (Category.meat, Category.cooked_meat):
        rm.item_tag("tfc:foods/meats", ingredient)
        if category == Category.cooked_meat:
            rm.item_tag("tfc:foods/cooked_meats", ingredient)
        else:
            rm.item_tag("tfc:foods/raw_meats", ingredient)
    if category == Category.dairy:
        rm.item_tag("tfc:foods/dairy", ingredient)


def casting_recipe(
    rm: ResourceManager,
    name_parts: utils.ResourceIdentifier,
    mold: str,
    metal: str,
    amount: int,
    result: Json,
    break_chance: float,
    conditions: Json = None,
):
    rm.recipe(
        ("casting", name_parts),
        "tfc:casting",
        {
            "mold": {"item": mold},
            "fluid": fluid_stack_ingredient(
                "%d tfcchannelcasting:%s" % (amount, metal)
            ),
            "result": utils.item_stack(result),
            "break_chance": break_chance,
        },
        conditions=conditions,
    )


def advanced_shapeless(
    rm: ResourceManager,
    name_parts: utils.ResourceIdentifier,
    ingredients: Json,
    result: Json,
    primary_ingredient: Json = None,
    group: Optional[str] = None,
    conditions: Optional[Json] = None,
) -> RecipeContext:
    res = utils.resource_location(rm.domain, name_parts)
    rm.write(
        (*rm.resource_dir, "data", res.domain, "recipes", res.path),
        {
            "type": "tfc:advanced_shapeless_crafting",
            "group": group,
            "ingredients": utils.item_stack_list(ingredients),
            "result": result,
            "primary_ingredient": None
            if primary_ingredient is None
            else utils.ingredient(primary_ingredient),
            "conditions": utils.recipe_condition(conditions),
        },
    )
    return RecipeContext(rm, res)


def barrel_sealed_recipe(
    rm: ResourceManager,
    name_parts: utils.ResourceIdentifier,
    translation: str,
    duration: int,
    input_item: Optional[Json] = None,
    input_fluid: Optional[Json] = None,
    output_item: Optional[Json] = None,
    output_fluid: Optional[Json] = None,
    on_seal: Optional[Json] = None,
    on_unseal: Optional[Json] = None,
    sound: Optional[str] = None,
):
    rm.recipe(
        ("barrel", name_parts),
        "tfc:barrel_sealed",
        {
            "input_item": item_stack_ingredient(input_item)
            if input_item is not None
            else None,
            "input_fluid": fluid_stack_ingredient(input_fluid)
            if input_fluid is not None
            else None,
            "output_item": item_stack_provider(output_item)
            if isinstance(output_item, str)
            else output_item,
            "output_fluid": fluid_stack(output_fluid)
            if output_fluid is not None
            else None,
            "duration": duration,
            "on_seal": on_seal,
            "on_unseal": on_unseal,
            "sound": sound,
        },
    )
    res = utils.resource_location("tfcchannelcasting", name_parts)
    rm.lang(
        "tfc.recipe.barrel." + res.domain + ".barrel." + res.path.replace("/", "."),
        lang(translation),
    )


def item_stack_provider(
    data_in: Json = None,
    copy_input: bool = False,
    add_trait: Optional[str] = None,
    modifiers: list[str | dict] = [],
) -> Json:
    if isinstance(data_in, dict):
        return data_in
    stack = utils.item_stack(data_in) if data_in is not None else None
    modifiers = modifiers.copy()

    if copy_input:
        modifiers.insert(0, "tfc:copy_input")
    if add_trait:
        modifiers.append(
            {
                "type": "tfc:add_trait",
                "trait": add_trait,
            },
        )

    if modifiers:
        return {"stack": stack, "modifiers": modifiers}
    return stack


def water_based_fluid(rm: ResourceManager, name: str):
    rm.blockstate(("fluid", name)).with_block_model(
        {"particle": "minecraft:block/water_still"}, parent=None
    ).with_lang(lang(name))
    rm.fluid_tag(
        name, "tfcchannelcasting:%s" % name, "tfcchannelcasting:flowing_%s" % name
    )
    rm.fluid_tag(
        "minecraft:water",
        "tfcchannelcasting:%s" % name,
        "tfcchannelcasting:flowing_%s" % name,
    )  # Need to use water fluid tag for behavior
    rm.fluid_tag(
        "mixable", "tfcchannelcasting:%s" % name, "tfcchannelcasting:flowing_%s" % name
    )

    item = rm.custom_item_model(
        ("bucket", name),
        "forge:bucket",
        {"parent": "forge:item/bucket", "fluid": "tfcchannelcasting:%s" % name},
    )
    item.with_lang(lang("%s bucket", name))
    rm.lang("fluid.tfcchannelcasting.%s" % name, lang(name))


def fire_clay_knapping(
    rm: ResourceManager,
    name_parts,
    pattern: List[str],
    result,
    outside_slot_required: bool = None,
):
    knapping_recipe(
        rm, "fire_clay_knapping", name_parts, pattern, result, outside_slot_required
    )


def clay_knapping(
    rm: ResourceManager,
    name_parts,
    pattern: List[str],
    result,
    outside_slot_required: bool = None,
):
    knapping_recipe(
        rm, "clay_knapping", name_parts, pattern, result, outside_slot_required
    )


def contained_fluid(
    rm: ResourceManager, name_parts: utils.ResourceIdentifier, base: str, overlay: str
):
    return rm.custom_item_model(
        name_parts,
        "tfc:contained_fluid",
        {"parent": "forge:item/default", "textures": {"base": base, "fluid": overlay}},
    )


def fluid_stack(data_in: Json) -> Json:
    if isinstance(data_in, dict):
        return data_in
    fluid, tag, amount, _ = utils.parse_item_stack(data_in, False)
    assert not tag, "fluid_stack() cannot be a tag"
    return {"fluid": fluid, "amount": amount}


def knapping_recipe(
    rm: ResourceManager,
    knapping_type: str,
    name_parts,
    pattern: List[str],
    result,
    outside_slot_required: bool = None,
):
    rm.recipe(
        (knapping_type, name_parts),
        "tfc:%s" % knapping_type,
        {
            "outside_slot_required": outside_slot_required,
            "pattern": pattern,
            "result": utils.item_stack(result),
        },
    )


def heat_recipe(
    rm: ResourceManager,
    name_parts: utils.ResourceIdentifier,
    ingredient: utils.Json,
    temperature: float,
    result_item: Optional[Union[str, Json]] = None,
    result_fluid: Optional[str] = None,
    use_durability: Optional[bool] = None,
    conditions: Json = None,
) -> RecipeContext:
    result_item = (
        utils.item_stack(result_item) if isinstance(result_item, str) else result_item
    )
    result_fluid = None if result_fluid is None else fluid_stack(result_fluid)
    return rm.recipe(
        ("heating", name_parts),
        "tfc:heating",
        {
            "ingredient": utils.ingredient(ingredient),
            "result_item": result_item,
            "result_fluid": result_fluid,
            "temperature": temperature,
            "use_durability": use_durability if use_durability else None,
        },
        conditions=conditions,
    )


def item_heat(
    rm: ResourceManager,
    name_parts: utils.ResourceIdentifier,
    ingredient: utils.Json,
    heat_capacity: float,
    melt_temperature: Optional[float] = None,
    mb: Optional[int] = None,
):
    if melt_temperature is not None:
        forging_temperature = round(melt_temperature * 0.6)
        welding_temperature = round(melt_temperature * 0.8)
    else:
        forging_temperature = welding_temperature = None
    if mb is not None:
        # Interpret heat capacity as a specific heat capacity - so we need to scale by the mB present. Baseline is 100 mB (an ingot)
        # Higher mB = higher heat capacity = heats and cools slower = consumes proportionally more fuel
        heat_capacity = round(10 * heat_capacity * mb) / 1000
    rm.data(
        ("tfc", "item_heats", name_parts),
        {
            "ingredient": utils.ingredient(ingredient),
            "heat_capacity": heat_capacity,
            "forging_temperature": forging_temperature,
            "welding_temperature": welding_temperature,
        },
    )


def item_size(
    rm: ResourceManager,
    name_parts: utils.ResourceIdentifier,
    ingredient: utils.Json,
    size: Size,
    weight: Weight,
):
    rm.data(
        ("tfc", "item_sizes", name_parts),
        {
            "ingredient": utils.ingredient(ingredient),
            "size": size.name,
            "weight": weight.name,
        },
    )


def ingredient_without_traits(ingredient: Json, *trait: str):
    if len(trait) == 0:
        return utils.item_stack(ingredient)

    return {
        "type": "tfc:lacks_trait",
        "trait": trait[0],
        "ingredient": ingredient_without_traits(ingredient, *trait[1:]),
    }


def ingredient_without_filling(ingredient: Json):
    return ingredient_without_traits(
        ingredient,
        *[
            f"tfcchannelcasting:filled_with_{fill}"
            for fill in ("jam", "sweet_liquor", "strong_liquor", "whiskey")
        ],
    )


def generate_blocks(mngr: ResourceManager):
    # Channel
    rots = {"north": 270, "east": 0, "south": 90, "west": 180}

    connection = "tfcchannelcasting:block/channel_connection"
    stop = "tfcchannelcasting:block/channel_stop"
    base = "tfcchannelcasting:block/channel_base"
    bottom = "tfcchannelcasting:block/channel_bottom"

    parts = [({"model": base}), (({"down": False}, {"model": bottom}))]
    for rot_name, rot_val in rots.items():
        parts.append(({rot_name: True}, {"model": connection, "y": rot_val}))
        parts.append(({rot_name: False}, {"model": stop, "y": rot_val}))

    mngr.blockstate_multipart("channel", *parts).with_lang(
        "Casting Channel"
    ).with_block_loot("tfcchannelcasting:channel").with_tag(
        "minecraft:mineable/pickaxe"
    )

    # Mold
    stop = "tfcchannelcasting:block/mold_table_stop"
    base = "tfcchannelcasting:block/mold_table_base"

    parts = [
        ({"model": base}),
    ]
    for rot_name, rot_val in rots.items():
        parts.append(({rot_name: False}, {"model": stop, "y": rot_val}))

    mngr.blockstate_multipart("mold_table", *parts).with_lang(
        "Mold Table"
    ).with_block_loot("tfcchannelcasting:mold_table").with_tag(
        "minecraft:mineable/pickaxe"
    )

    # Crucible
    base = "tfc:block/crucible"
    connection = "tfcchannelcasting:block/crucible_connection"

    parts = [
        ({"model": base}),
    ]
    for rot_name, rot_val in rots.items():
        parts.append(({rot_name: True}, {"model": connection, "y": rot_val}))

    mngr.blockstate_multipart("tfc:crucible", *parts)


def load_lang(mngr: ResourceManager):
    for lang in LANG:
        with open(f"./resources/lang/{lang}.json") as f:
            mngr.lang(json.load(f))


def generate_items(mngr: ResourceManager):
    [
        mngr.item("mold/" + mold.lower()).with_lang(mold + " Render Item")
        for mold in list(MOLDS) + list(CUSTOM_MOLDS)
    ]

    mngr.item("unfired_channel").with_lang("Unfired Casting Channel").with_tag(
        "tfc:unfired_pottery", False
    ).with_item_model(
        {"0": "tfc:block/fire_clay_block"}, parent="tfcchannelcasting:item/channel"
    )
    item_heat(
        mngr,
        "unfired_channel",
        "tfcchannelcasting:unfired_channel",
        POTTERY_HEAT_CAPACITY,
    )

    mngr.item("unfired_mold_table").with_lang("Unfired Mold Table").with_tag(
        "tfc:unfired_pottery", False
    ).with_item_model(
        {"0": "tfc:block/fire_clay_block", "1": "tfc:block/fire_clay_block"},
        parent="tfcchannelcasting:item/mold_table",
    )
    item_heat(
        mngr,
        "unfired_mold_table",
        "tfcchannelcasting:unfired_mold_table",
        POTTERY_HEAT_CAPACITY,
    )

    item_size(
        mngr,
        "unfired_mold_table",
        "tfcchannelcasting:unfired_mold_table",
        Size.huge,
        Weight.heavy,
    )
    item_size(
        mngr, "mold_table", "tfcchannelcasting:mold_table", Size.huge, Weight.heavy
    )


def generate_tags(mngr: ResourceManager):
    mngr.item_tag(
        "accepted_in_mold_table",
        *["tfc:ceramic/" + mold.lower() + "_mold" for mold in MOLDS],
    )
    mngr.item_tag(
        "accepted_in_mold_table",
        *["tfcchannelcasting:" + mold.lower() + "_mold" for mold in CUSTOM_MOLDS],
    )


def generate_recipes(mngr: ResourceManager):
    fire_clay_knapping(
        mngr,
        "unfired_channel_2",
        [
            "X   X",
            " XXX ",
        ],
        (2, "tfcchannelcasting:unfired_channel"),
        False,
    )

    fire_clay_knapping(
        mngr,
        "unfired_channel_4",
        ["X   X", " XXX ", "     ", "X   X", " XXX "],
        (4, "tfcchannelcasting:unfired_channel"),
    )

    fire_clay_knapping(
        mngr,
        "unfired_mold_table",
        ["XXXXX", "X   X", "X   X", "X   X", "XXXXX"],
        "tfcchannelcasting:unfired_mold_table",
    )

    clay_knapping(
        mngr,
        "unfired_heart_mold",
        ["X X X", "     ", "     ", "X   X", "XX XX"],
        "tfcchannelcasting:unfired_heart_mold",
    )

    heat_recipe(
        mngr,
        "channel",
        "tfcchannelcasting:unfired_channel",
        POTTERY_MELT,
        result_item="tfcchannelcasting:channel",
    )
    heat_recipe(
        mngr,
        "mold_table",
        "tfcchannelcasting:unfired_mold_table",
        POTTERY_MELT,
        result_item="tfcchannelcasting:mold_table",
    )


def generate_chocolate_stuff(mngr: ResourceManager):
    contained_fluid(
        mngr,
        "heart_mold",
        "tfcchannelcasting:item/fired_mold/heart_mold_empty",
        "tfcchannelcasting:item/fired_mold/heart_mold_overlay",
    ).with_lang("Heart Mold").with_tag("tfc:fired_molds", False)

    mngr.item("unfired_heart_mold").with_lang("Unfired Heart Mold").with_tag(
        "tfc:unfired_molds", False
    ).with_item_model()
    item_heat(
        mngr,
        "unfired_heart_mold",
        "tfcchannelcasting:unfired_heart_mold",
        POTTERY_HEAT_CAPACITY,
    )
    heat_recipe(
        mngr,
        "heart_mold",
        "tfcchannelcasting:unfired_heart_mold",
        POTTERY_MELT,
        result_item="tfcchannelcasting:heart_mold",
    )

    for chocolate in CHOCOLATES:
        metal = CHOCOLATES_METALS[chocolate]
        mngr.data(
            ("tfc", "metals", chocolate),
            {
                "tier": metal.tier,
                "fluid": f"tfcchannelcasting:{chocolate}",
                "melt_temperature": metal.melt_temperature,
                "specific_heat_capacity": metal.specific_heat_capacity(),
                "ingots": utils.ingredient(f"#forge:ingots/{chocolate}"),
                "sheets": utils.ingredient(f"#forge:sheets/{chocolate}"),
            },
        )

        mngr.lang(f"metal.tfcchannelcasting.{chocolate}", lang(chocolate))

        mngr.item_tag(
            f"forge:ingots/{chocolate}",
            {"id": f"firmalife:food/{chocolate}", "required": False},
        )
        mngr.item_tag("forge:ingots", f"#forge:ingots/{chocolate}")
        mngr.item_tag("tfc:pileable_ingots", f"#forge:ingots/{chocolate}")

        mngr.item_tag(
            f"forge:sheets/{chocolate}",
        )
        mngr.item_tag("tfc:pileable_sheets", f"#forge:sheets/{chocolate}")

        water_based_fluid(mngr, chocolate)
        for tag in (
            "tfcchannelcasting:usable_in_heart_mold",
            "tfc:usable_in_ingot_mold",
            "tfc:usable_in_tool_head_mold",
            "tfc:usable_in_bell_mold",
        ):
            mngr.fluid_tag(
                tag,
                f"tfcchannelcasting:{chocolate}",
                "tfcchannelcasting:flowing_%s" % chocolate,
            )

        item_heat(
            mngr,
            ("food", chocolate),
            f"firmalife:food/{chocolate}",
            metal.ingot_heat_capacity(),
        )

        heat_recipe(
            mngr,
            ("food", chocolate),
            not_rotten(f"firmalife:food/{chocolate}"),
            300,
            None,
            f"100 tfcchannelcasting:{chocolate}",
            conditions=FIRMALIFE_CONDITIONS,
        )

        casting_recipe(
            mngr,
            chocolate,
            "tfc:ceramic/ingot_mold",
            chocolate,
            100,
            f"firmalife:food/{chocolate}",
            0,
            conditions=FIRMALIFE_CONDITIONS,
        )
        casting_recipe(
            mngr,
            f"{chocolate}_fire_ingot",
            "tfc:ceramic/fire_ingot_mold",
            chocolate,
            100,
            f"firmalife:food/{chocolate}",
            0,
            conditions=FIRMALIFE_CONDITIONS,
        )

        for chocolate_type, trait, month, mold, name in zip(
            ("heart", "bell", "knife"),
            (
                "tfcchannelcasting:romantic",
                "tfcchannelcasting:festive",
                "tfcchannelcasting:scary",
            ),
            (2, 12, 10),
            (
                "tfcchannelcasting:heart_mold",
                "tfc:ceramic/bell_mold",
                "tfc:ceramic/knife_blade_mold",
            ),
            ("Heart", "Christmas Bell", "Candy Knife"),
        ):
            item = f"{chocolate}_{chocolate_type}"
            loc = f"tfcchannelcasting:food/{item}"

            mngr.item_tag(
                ("foods", "chocolate_sweet"),
                f"#tfcchannelcasting:foods/chocolate_{chocolate_type}",
                replace=False,
            )

            mngr.item(("food", item)).with_lang(lang(f"{chocolate} {name}")).with_tag(
                f"tfcchannelcasting:foods/chocolate_{chocolate_type}", False
            ).with_item_model()

            item_heat(
                mngr,
                ("food", item),
                loc,
                metal.ingot_heat_capacity(),
            )

            heat_recipe(
                mngr,
                ("food", item),
                not_rotten(loc),
                300,
                None,
                f"100 tfcchannelcasting:{chocolate}",
            )

            casting_recipe(
                mngr,
                item,
                mold,
                chocolate,
                100,
                item_stack_provider(
                    loc,
                    modifiers=[
                        {
                            "type": "tfcchannelcasting:conditional",
                            "condition": {
                                "type": "tfcchannelcasting:date_range",
                                "min_day": 1,
                                "min_month": month,
                                "max_day": 8,
                                "max_month": month,
                            },
                            "modifiers": [
                                {
                                    "type": "tfcchannelcasting:set_food_data",
                                    **SPECIAL_CHOCOLATE_FOOD_DATA,
                                },
                                {
                                    "type": "tfc:add_trait",
                                    "trait": trait,
                                },
                            ],
                            "else_modifiers": [
                                {
                                    "type": "tfcchannelcasting:set_food_data",
                                    **NORMAL_CHOCOLATE_FOOD_DATA,
                                },
                            ],
                        },
                    ],
                ),
                0,
            )

    mngr.fluid_tag(
        "tfcchannelcasting:sweet_liquor",
        "tfc:cider",
        "tfc:rum",
        replace=False,
    )

    mngr.fluid_tag(
        "tfcchannelcasting:strong_liquor",
        "tfc:sake",
        "tfc:vodka",
        replace=False,
    )

    mngr.fluid_tag(
        "tfcchannelcasting:whiskey",
        "tfc:whiskey",
        "tfc:corn_whiskey",
        "tfc:rye_whiskey",
        replace=False,
    )

    for chocolate_type, trait in zip(
        ("heart", "bell", "knife"),
        (
            "tfcchannelcasting:romantic",
            "tfcchannelcasting:festive",
            "tfcchannelcasting:scary",
        ),
    ):
        input_tag = f"#tfcchannelcasting:foods/chocolate_{chocolate_type}"
        local_input_wout_fill = ingredient_without_filling(input_tag)

        advanced_shapeless(
            mngr,
            f"crafting/{chocolate_type}_add_jam",
            (local_input_wout_fill, "#firmalife:foods/preserves"),
            item_stack_provider(
                copy_input=True,
                add_trait="tfcchannelcasting:filled_with_jam",
                modifiers=[
                    {
                        "type": "tfcchannelcasting:conditional",
                        "condition": {
                            "type": "tfcchannelcasting:has_trait",
                            "trait": trait,
                        },
                        "modifiers": [
                            {
                                "type": "tfcchannelcasting:set_food_data",
                                **SPECIAL_CHOCOLATE_FOOD_DATA,
                                **JAM_CHOCOLATE_FOOD_DATA,
                            },
                        ],
                        "else_modifiers": [
                            {
                                "type": "tfcchannelcasting:set_food_data",
                                **NORMAL_CHOCOLATE_FOOD_DATA,
                                **JAM_CHOCOLATE_FOOD_DATA,
                            },
                        ],
                    },
                ],
            ),
            local_input_wout_fill,
            conditions=FIRMALIFE_CONDITIONS,
        )

    local_input_wout_fill = ingredient_without_filling(
        "#tfcchannelcasting:foods/chocolate_sweet"
    )

    for liq, liq_lan in zip(
        ("sweet_liquor", "strong_liquor", "whiskey"),
        ("Sweet Liquor", "Strong Liquor", "Whiskey"),
    ):
        barrel_sealed_recipe(
            mngr,
            f"fill_with_{liq}",
            f"Filling with {liq_lan}",
            24000,
            local_input_wout_fill,
            f"250 #tfcchannelcasting:{liq}",
            item_stack_provider(
                copy_input=True, add_trait=f"tfcchannelcasting:filled_with_{liq}"
            ),
        )


def generate_mold_models(mngr):
    pass


def main():
    mngr = ResourceManager(
        domain="tfcchannelcasting", resource_dir="./src/main/resources"
    )
    generate_blocks(mngr)
    generate_tags(mngr)
    generate_items(mngr)
    generate_recipes(mngr)
    generate_chocolate_stuff(mngr)
    load_lang(mngr)
    mngr.flush()


if __name__ == "__main__":
    main()
