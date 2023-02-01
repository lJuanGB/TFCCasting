import copy
from enum import Enum, auto
import json
import sys
from typing import *

from mcresources import RecipeContext, ResourceManager, utils
from mcresources.type_definitions import Json

POTTERY_MELT = 1400 - 1
POTTERY_HEAT_CAPACITY = 1.2  # Heat Capacity # Useful comment

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
)
LANG = ("en_US", )

def fire_clay_knapping(rm: ResourceManager, name_parts, pattern: List[str], result, outside_slot_required: bool = None):
    knapping_recipe(rm, 'fire_clay_knapping', name_parts, pattern, result, outside_slot_required)

def knapping_recipe(rm: ResourceManager, knapping_type: str, name_parts, pattern: List[str], result, outside_slot_required: bool = None):
    rm.recipe((knapping_type, name_parts), 'tfc:%s' % knapping_type, {
        'outside_slot_required': outside_slot_required,
        'pattern': pattern,
        'result': utils.item_stack(result)
    })

def heat_recipe(rm: ResourceManager, name_parts, ingredient, temperature: float, result_item: Optional[Union[str, Json]] = None, use_durability: Optional[bool] = None) -> RecipeContext:
    result_item = utils.item_stack(result_item) if isinstance(result_item, str) else result_item
    return rm.recipe(('heating', name_parts), 'tfc:heating', {
        'ingredient': utils.ingredient(ingredient),
        'result_item': result_item,
        'temperature': temperature,
        'use_durability': use_durability if use_durability else None,
    })

def item_heat(rm: ResourceManager, name_parts: utils.ResourceIdentifier, ingredient: utils.Json, heat_capacity: float, melt_temperature: Optional[float] = None, mb: Optional[int] = None):
    if melt_temperature is not None:
        forging_temperature = round(melt_temperature * 0.6)
        welding_temperature = round(melt_temperature * 0.8)
    else:
        forging_temperature = welding_temperature = None
    if mb is not None:
        # Interpret heat capacity as a specific heat capacity - so we need to scale by the mB present. Baseline is 100 mB (an ingot)
        # Higher mB = higher heat capacity = heats and cools slower = consumes proportionally more fuel
        heat_capacity = round(10 * heat_capacity * mb) / 1000
    rm.data(('tfc', 'item_heats', name_parts), {
        'ingredient': utils.ingredient(ingredient),
        'heat_capacity': heat_capacity,
        'forging_temperature': forging_temperature,
        'welding_temperature': welding_temperature
    })

def item_size(rm: ResourceManager, name_parts: utils.ResourceIdentifier, ingredient: utils.Json, size: Size, weight: Weight):
    rm.data(('tfc', 'item_sizes', name_parts), {
        'ingredient': utils.ingredient(ingredient),
        'size': size.name,
        'weight': weight.name
    })    

def generate_blocks(mngr: ResourceManager):
    # Channel
    rots = {
        "north": 270,
        "east": 0,
        "south": 90,
        "west": 180
    }

    connection = "tfcchannelcasting:block/channel_connection"
    stop = "tfcchannelcasting:block/channel_stop"
    base = "tfcchannelcasting:block/channel_base"
    bottom = "tfcchannelcasting:block/channel_bottom"

    parts = [
        ({"model": base}),
        (({"down": False}, {'model': bottom}))
    ]
    for rot_name, rot_val in rots.items():
        parts.append(({rot_name: True}, {'model': connection, 'y': rot_val}))
        parts.append(({rot_name: False}, {'model': stop, 'y': rot_val}))

    mngr.blockstate_multipart("channel",
        *parts
    ).with_lang('Casting Channel').with_block_loot('tfcchannelcasting:channel')

    # Mold
    stop = "tfcchannelcasting:block/mold_table_stop"
    base = "tfcchannelcasting:block/mold_table_base"

    parts = [
        ({"model": base}),
    ]
    for rot_name, rot_val in rots.items():
        parts.append(({rot_name: False}, {'model': stop, 'y': rot_val}))

    mngr.blockstate_multipart("mold_table",
        *parts
    ).with_lang('Mold Table').with_block_loot('tfcchannelcasting:mold_table')

    # Crucible
    base = "tfc:block/crucible"
    connection = "tfcchannelcasting:block/crucible_connection"

    parts = [
        ({"model": base}),
    ]
    for rot_name, rot_val in rots.items():
        parts.append(({rot_name: True}, {'model': connection, 'y': rot_val}))

    mngr.blockstate_multipart("tfc:crucible",
        *parts
    )
    
def load_lang(mngr: ResourceManager):
    for lang in LANG:
        with open(f"./resources/lang/{lang}.json") as f:
            mngr.lang(json.load(f))

def generate_items(mngr: ResourceManager):
    [
        mngr.item("mold/" + mold.lower()).with_lang(mold + " Render Item")
        for mold in MOLDS
    ]

    mngr.item("unfired_channel").with_lang("Unfired Casting Channel").with_tag(
        'tfc:unfired_pottery', False
    ).with_item_model(
        {"0": "tfc:block/fire_clay_block"}, 
        parent="tfcchannelcasting:item/channel"
    )
    item_heat(mngr, "unfired_channel", "tfcchannelcasting:unfired_channel", POTTERY_HEAT_CAPACITY)

    mngr.item("unfired_mold_table").with_lang("Unfired Mold Table").with_tag(
        'tfc:unfired_pottery', False
    ).with_item_model(
        {"0": "tfc:block/fire_clay_block", "1": "tfc:block/fire_clay_block"},
        parent="tfcchannelcasting:item/mold_table"
    )
    item_heat(mngr, "unfired_mold_table", "tfcchannelcasting:unfired_mold_table", POTTERY_HEAT_CAPACITY)

    item_size(mngr, 'unfired_mold_table', 'tfcchannelcasting:unfired_mold_table', Size.huge, Weight.heavy)
    item_size(mngr, 'mold_table', 'tfcchannelcasting:mold_table', Size.huge, Weight.heavy)

def generate_tags(mngr: ResourceManager):
    mngr.item_tag("accepted_in_mold_table", *["tfc:ceramic/" + mold.lower() + "_mold" for mold in MOLDS])

def generate_recipes(mngr: ResourceManager):
    fire_clay_knapping(mngr, 'unfired_channel_2', [
        'X   X', 
        ' XXX ',
        ], 
        (2, 'tfcchannelcasting:unfired_channel'), False)


    fire_clay_knapping(mngr, 'unfired_channel_4', [
        'X   X', 
        ' XXX ', 
        '     ', 
        'X   X', 
        ' XXX '], 
        (4, 'tfcchannelcasting:unfired_channel'))

    fire_clay_knapping(mngr, 'unfired_mold_table', [
        'XXXXX', 
        'X   X', 
        'X   X', 
        'X   X', 
        'XXXXX'], 
    'tfcchannelcasting:unfired_mold_table')

    heat_recipe(mngr, 'channel', 'tfcchannelcasting:unfired_channel', POTTERY_MELT, result_item='tfcchannelcasting:channel')
    heat_recipe(mngr, 'mold_table', 'tfcchannelcasting:unfired_mold_table', POTTERY_MELT, result_item='tfcchannelcasting:mold_table')

def generate_mold_models(mngr):
    pass

def main():
    mngr = ResourceManager(domain="tfcchannelcasting", resource_dir='./src/main/resources')
    generate_blocks(mngr)
    generate_tags(mngr)
    generate_items(mngr)
    generate_recipes(mngr)
    load_lang(mngr)
    mngr.flush()

if __name__ == "__main__":
    main()