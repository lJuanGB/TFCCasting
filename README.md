# TFC Casting with Channels!

It's a TFC addon for casting. With channels! Check it out here: https://www.curseforge.com/minecraft/mc-mods/tfc-casting-with-channels

### For datapack makers
You may wish to add support for molds from other mods to the Mold Table. This is very easy to do. First, you need to add the mold item to the item tag: `tfcchannelcasting:accepted_in_mold_table`. Then, you need to add a model associated with said item. This model needs to placed in the `assets/tfcchannelcasting/models/mold` folder. For example, the model for `tfc:ceramic/ingot_mold` should be placed under: `assets\tfcchannelcasting\models\mold\tfc\ceramic\ingot_mold.json`. Any model format is supported, but the easiest loader to use is `tfcchannelcasting:mold` (checkout one of the default models to understand the format).
