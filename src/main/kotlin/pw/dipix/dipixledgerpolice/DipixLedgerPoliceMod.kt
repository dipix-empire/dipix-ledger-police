package pw.dipix.dipixledgerpolice

import com.github.quiltservertools.ledger.api.CommandExtension
import com.github.quiltservertools.ledger.api.ExtensionManager
import com.github.quiltservertools.ledger.commands.BuildableCommand
import com.github.quiltservertools.ledger.commands.CommandConsts
import com.github.quiltservertools.libs.com.uchuhimo.konf.ConfigSpec
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import me.lucko.fabric.api.permissions.v0.Permissions
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.command.argument.BlockPosArgumentType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.command.CommandManager.argument
import net.minecraft.server.command.CommandManager.literal
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.util.ActionResult
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.world.World

class DipixLedgerPoliceMod : DedicatedServerModInitializer {
    /**
     * Runs the mod initializer.
     */

    override fun onInitializeServer() {
        ExtensionManager.registerExtension(DipixLedgerPoliceExtension)
        ServerLifecycleEvents.SERVER_STARTING.register {
            AttackBlockCallback.EVENT.register(DipixLedgerPolicePlayerListeners::onBlockAttack)
            UseBlockCallback.EVENT.register(DipixLedgerPolicePlayerListeners::onUseBlock)
        }
    }
}

object DipixLedgerPolicePlayerListeners {
    fun onUseBlock(
        player: PlayerEntity,
        world: World,
        hand: Hand,
        blockHitResult: BlockHitResult
    ): ActionResult {
        if (player.isPolicing() && hand == Hand.MAIN_HAND) {
            player.commandSource.policeBlock(blockHitResult.blockPos.offset(blockHitResult.side))
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

    fun onBlockAttack(
        player: PlayerEntity,
        world: World,
        hand: Hand,
        pos: BlockPos,
        direction: Direction
    ): ActionResult {
        if (world.isClient) return ActionResult.PASS

        if (player.isPolicing()) {
            player.commandSource.policeBlock(pos)
            return ActionResult.SUCCESS
        }

        return ActionResult.PASS
    }

}

object DipixLedgerPoliceExtension : CommandExtension {
    override fun registerSubcommands(): List<BuildableCommand> = listOf(DipixLedgerPoliceSubcommand)

    override fun getConfigSpecs(): List<ConfigSpec> = listOf(DipixLedgerPoliceConfig)

    override fun getIdentifier(): Identifier = Identifier.of("dipix-ledger-police", "dipix-ledger-police-extension")!!
}

object DipixLedgerPoliceConfig : ConfigSpec("dipix-police") {
    val fingerprintMaxAge by required<Long>()
}

object DipixLedgerPoliceSubcommand : BuildableCommand {
    override fun build(): LiteralCommandNode<ServerCommandSource> =
        literal("police")
            .requires(Permissions.require("ledger.commands.police", CommandConsts.PERMISSION_LEVEL))
            .executes { togglePolice(it) }
            .then(
                literal("on")
                    .executes { it.source.playerOrThrow.policeOn() }
            )
            .then(
                literal("off")
                    .executes { it.source.playerOrThrow.policeOff() }
            )
            .then(
                argument("pos", BlockPosArgumentType.blockPos())
                    .executes { policeBlock(it, BlockPosArgumentType.getBlockPos(it, "pos")) }
            )
            .build()

    private fun togglePolice(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val player = source.playerOrThrow

        return if (player.isPolicing()) {
            player.policeOff()
        } else {
            player.policeOn()
        }
    }

    private fun policeBlock(context: CommandContext<ServerCommandSource>, pos: BlockPos): Int {
        val source = context.source

        source.policeBlock(pos)
        return 1
    }
}