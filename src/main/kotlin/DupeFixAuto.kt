import net.fabricmc.mappingio.MappingWriter
import net.fabricmc.mappingio.format.MappingFormat
import net.fabricmc.mappingio.tree.MappingTree
import net.fabricmc.mappingio.tree.MemoryMappingTree
import java.io.File


fun main() {
    val oldInput = File("""D:\Downloads\mappings-official-srg-named.tiny2.txt""")
    val old8Input = File("""L:\LightCraftMappings\1.8.9\mappings-official-srg-named.tiny2""")
    val input = File("one_dot_five_merged_one_dot_eight.tiny")
    val output = File("one_dot_five_merged_one_dot_eight_fix.tiny")

    var oldTree = readMappingFromFile(oldInput) as MappingTree
    var old8Tree = readMappingFromFile(old8Input) as MappingTree
    var tree = readMappingFromFile(input)

    val mappingTree = tree as MappingTree
    val clonedClasses = mappingTree.classes.toList()

    val unfixableClones = setOf(
        "updateScreen",
        "updateEntities",
        "unloadQueuedChunks",
        "initGui",
        "entityInit",
        "startServer",
        "onFinishedPotionEffect",
        "updateAllPlayersSleepingFlag",
        "dropFewItems",
        "registerStat",
        "tick",
        "onLivingUpdate",
        "dropEquipment",

        /* Misc */

        "getTabIconItemIndex",
        "renderAsNormalBlock",
        "func_96092_aw",
        "setHealth",
        "openTexturePackFile",
        "isPushedByWater",
        "getUnlocalizedName",
        "func_148210_b",
        "hitByEntity",
        "setEntityHealth",
        "updateFlow",
        "getInternalNameSuffix",
        "isItemValidForSlot",
        "sendPlayerAbilities",
        "getBoundingBox",
        "func_147068_g",
        "setRandomEntity",
        "onInventoryChanged",
        "onNoMoreProgress",
        "getItemStackDisplayName",
        "getFullSlabName",
        "func_82506_l",
        "func_82194_d",
        "saveExtraData",
        "saveLevel",
        "getSourceOfDamage",
        "func_70183_g",
        "fall",
        "doRender",
        "func_82329_d",
        "isStackValidForSlot",
        "getWorldObj",
        "doRenderLiving",
        "updateBlocks",
        "setSpawnLocation",
        "func_148171_c",
        "setDoneWorking",
        "updateEntityActionState",
        "isFullCube",
        "getItemDisplayName",
        "func_70182_d",
        "func_82330_g",
        "func_104112_b",
        "checkForHarden",
        "removeExperienceLevel",
        "func_148224_c",
        "markDirty",
        "func_82143_as",
        "getInaccuracy",
        "isAIDisabled",
        "removeAllEntities",
        "canPlaceTorchOn",
        "getWidthPixels",
        "func_94062_bN",
        "removePlayerFromTeam",
        "func_96521_a",
        "setRandomMinecart",
        "updateDragSplitting",
        "getEntity",
        "moveEntityWithHeading",
        "func_77258_c",
        "registerDispenseBehaviors",
        "isInvisibleToPlayer",
        "func_85031_j",
        "getBlockMetadata",
        "getWorld",
        "getHeightPixels",
        "func_98034_c",
        "swingItem",
        "setNotStationary",
        "closeScreen",
        "getMaxFallHeight",
        "overlayBackground",
        "calculateInitialWeather",
        "executePendingCommands",
        "getVelocity",
        "drawDefaultBackground",
        "alterSquishAmount",
        "func_82204_b",
        "drawActivePotionEffects",
        "getDimensionName",
        "getIconItemDamage",
        "tickBlocksAndAmbiance",
        "requiresUpdates",
        "isIndirectlyPowered",
        "getCollisionBoundingBox",
        "updateRotation",
        "func_70808_l",
        "loadThumbnailImage",
        "getZInt",
        "setInitialSpawnLocation",
        "func_71013_b"
    )

    val srgMapResult = mutableMapOf<String, String>()

    var iterationCount = 0
    var hasModified = true
    while (hasModified) {
        println("Starting iteration $iterationCount")
        hasModified = false
        val methodSet = mutableSetOf<String>()
        //clonedClasses.forEach { clazz ->
            val clonedMethods = clonedClasses.flatMap { it.methods }
            clonedMethods.groupBy { method -> getMethodName(method, tree) + method.srcDesc }.forEach dupe@{
                if (it.value.size <= 1) return@dupe
                it.value.forEach { method ->
                    val clazz = method.owner
                    val oldMethod =
                        oldTree.getClass(clazz.srcName).methods.firstOrNull { it.srcName.equals(method.srcName) && it.srcDesc.equals(method.srcDesc) }
                    val old8Method =
                        old8Tree.getClass(clazz.getName("srg"), oldTree.getNamespaceId("srg"))?.methods?.firstOrNull { it.getName("srg").equals(method.getName("srg")) && it.getDesc("srg").equals(method.getDesc("srg")) }
                    val currentName = method.getName("named")
                    val oldName = oldMethod?.getName("named")
                    if (oldMethod != null) {
                        if (oldName != currentName) {
                            if (!unfixableClones.contains(currentName)) {
                                return@dupe
                            }
                            hasModified = true
                            method.args.clear()
                            oldMethod.args.forEach { arg ->
                                method.addArg(arg)
                            }
                            method.comment = oldMethod.comment
                            method.setDstName(oldMethod.getName("srg"), tree.getNamespaceId("srg"))
                            method.setDstName(oldName, tree.getNamespaceId("named"))
                            println("Replacing duplicate method $currentName in class ${clazz.getName("named")} with $oldName")
                        }
                    } else if (false) {
                        it.value.forEach {
                            if (unfixableClones.contains(it.getName("named"))) {
                                println("Found an unfixable clone of $currentName in class ${clazz.getName("named")}")
                            }
                        }
                        hasModified = true
                        clazz.removeMethod(method.srcName, method.srcDesc)
                        //method.setDstName(currentName + "_Duplicated", tree.getNamespaceId("named"))
                        //method.setDstName(method.getName("srg") + "_Duplicated", tree.getNamespaceId("srg"))
                    }
                }
          //  }

            if (false) clonedMethods.forEach methods@{ method ->
                var dupeCount = 0
                var name = getMethodName(method, tree)
                if (method.getName("named") !in unfixableClones) return@methods
                var key = name + method.srcDesc
                if (methodSet.contains(key)) {
                    val newSrg = method.getDstName(tree.getNamespaceId("named")) + "_${++dupeCount}"
                    srgMapResult[method.getName("srg")] = newSrg
                    method.setDstName(newSrg, tree.getNamespaceId("named"))
                }
                if (srgMapResult.containsKey(method.getName("srg"))) {
                    method.setDstName(srgMapResult[method.getName("srg")]!!, tree.getNamespaceId("srg"))
                }
                key = (name ?: "") + method.getDesc("named")
                methodSet.add(key)
            }

            if (false && methodSet.size != clonedMethods.size) {
                println("Repeated methods found in sus")
            }
        }
        iterationCount++;

    }

    MappingWriter.create(output.toPath(), MappingFormat.TINY_2).use { writer ->
        mappingTree.accept(writer)
    }

}

private fun getMethodName(
    method: MappingTree.MethodMapping,
    tree: MemoryMappingTree
): String? {
    var name = method.getDstName(tree.getNamespaceId("named"))
    if (name == null) name = method.srcName
    return name
}