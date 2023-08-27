package emeraldwater.infernity.dev.interpreter

import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Vec
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.tag.Tag
import kotlin.math.hypot

data class ParseBlockResult(val point: Point, val action: List<Action>)

fun parseDevArea(instance: Instance): List<ActionContainer> {
    val actionContainers = mutableListOf<ActionContainer>()
    for(x in -0 downTo -20 step 3) {
        for(z in 0..0) {
            for(y in 2..255 step 5) {
                val block = instance.getBlock(x, y, z)
                if(block.name() == "minecraft:diamond_block") {
                    val event = instance.getBlock(x-1, y, z).getTag(Tag.String("line2"))
                    if(event != null && playerEventFromString(event) != null) {
                        val (_, actions) = parseBlock(instance, Vec(x.toDouble(), y.toDouble(), z.toDouble()))
                        actionContainers.add(PlayerEventBlock(
                            playerEventFromString(event)!!,
                            actions
                        ))
                    }
                }
                if(block.name() == "minecraft:oak_planks") {
                    val event = instance.getBlock(x-1, y, z).getTag(Tag.String("line2"))
                    if(event != null && ifPlayerFromString(event) != null) {
                        val (_, actions) = parseBlock(instance, Vec(x.toDouble(), y.toDouble(), z.toDouble()))
                        val arguments = mutableListOf<Argument>()
                        for(slot in 0..53) {
                            val item = instance.getBlock(x, y + 1, z).getTag(Tag.ItemStack("barrel.slot$slot"))
                            if(item != null && item != ItemStack.AIR) {
                                if(item.getTag(Tag.String("varitem.id")) == "txt") {
                                    arguments.add(Argument.Text(item.getTag(Tag.String("varitem.value"))))
                                }
                                else if (item.getTag(Tag.String("varitem.id")) == "rtxt") {
                                    arguments.add(Argument.RichText(item.getTag(Tag.Component("varitem.value"))))
                                }
                                else if (item.getTag(Tag.String("varitem.id")) == "num") {
                                    arguments.add(Argument.Number(item.getTag(Tag.Double("varitem.value"))))
                                }
                                else {
                                    arguments.add(Argument.Item(item))
                                }

                            }
                        }
                        actionContainers.add(IfPlayerBlock(
                            ifPlayerFromString(event)!!,
                            actions,
                            arguments
                        ))
                    }
                }
                if(block.name() == "minecraft:lapis_block") {
                    val event = instance.getBlock(x-1, y, z).getTag(Tag.String("line2"))
                    if(event != null) {
                        val (_, actions) = parseBlock(instance, Vec(x.toDouble(), y.toDouble(), z.toDouble()))
                        actionContainers.add(FunctionDefinitionBlock(
                            actions,
                            listOf()
                        ))
                    }
                }
            }
        }
    }
    return actionContainers
}

fun parseBlock(instance: Instance, getPoint: Point): ParseBlockResult {
    var point = getPoint
    val actions = mutableListOf<Action>()
    var counter = 1
    while(true) {
        point = point.withZ(point.z() + 2.0)
        val block = instance.getBlock(point)
        val stone = instance.getBlock(point.withZ(point.z() + 1.0))
        val chest = instance.getBlock(point.withY(point.y() + 1.0))
        val sign = instance.getBlock(point.withX(point.x() - 1.0))
        if(stone == Block.PISTON.withProperty("facing", "north")) counter--
        if(stone == Block.PISTON.withProperty("facing", "south")) counter++
        if(stone == Block.AIR || (counter == 0 && stone == Block.PISTON.withProperty("facing", "north")))
            return ParseBlockResult(point, actions)
        val line2 = sign.getTag(Tag.String("line2"))

        val arguments = mutableListOf<Argument>()
        for(slot in 0..53) {
            val item = chest.getTag(Tag.ItemStack("barrel.slot$slot"))
            if(item != null && item != ItemStack.AIR) {
                if(item.getTag(Tag.String("varitem.id")) == "txt") {
                    arguments.add(Argument.Text(item.getTag(Tag.String("varitem.value"))))
                }
                if(item.getTag(Tag.String("varitem.id")) == "rtxt") {
                    arguments.add(Argument.RichText(item.getTag(Tag.Component("varitem.value"))))
                }
                if(item.getTag(Tag.String("varitem.id")) == "num") {
                    arguments.add(Argument.Number(item.getTag(Tag.Double("varitem.value"))))
                }
            }
        }


        if(block.name() == "minecraft:cobblestone") {
            if(line2 != null && playerActionFromString(line2) != null) {
                actions.add(PlayerActionBlock(
                    playerActionFromString(line2)!!,
                    arguments
                ))
            }
        }
        if(block.name() == "minecraft:iron_block") {
            if(line2 != null && setVariableFromString(line2) != null) {
                actions.add(SetVariableBlock(
                    setVariableFromString(line2)!!,
                    arguments
                ))
            }
        }
        if(block.name() == "minecraft:lapis_block") {
            if(line2 != null) {
                val (point2, actions2) = parseBlock(instance, point)
                val func = FunctionDefinitionBlock(
                    actions2,
                    arguments,
                )
                actions.add(func)
                point = point2
            }
        }
        if(block.name() == "minecraft:target_block") {
            if(line2 != null && setTargetFromString(line2) != null) {
                actions.add(SetTargetBlock(
                    setTargetFromString(line2)!!,
                    arguments
                ))
            }
        }
    }
    return ParseBlockResult(point, actions)

}