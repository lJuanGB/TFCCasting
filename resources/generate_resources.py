import copy
import json
import sys
from typing import *

import mcresources

LANG = ("en_US", )

def generate_blocks(mngr: mcresources.ResourceManager):
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
    
def load_lang(mngr: mcresources.ResourceManager):
    for lang in LANG:
        with open(f"./resources/lang/{lang}.json") as f:
            mngr.lang(json.load(f))

def main():
    mngr = mcresources.ResourceManager(domain="tfcchannelcasting", resource_dir='./src/main/resources')
    generate_blocks(mngr)
    load_lang(mngr)
    mngr.flush()

if __name__ == "__main__":
    main()