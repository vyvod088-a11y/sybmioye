package com.symbiote.evolution;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class MeteoriteBlock extends Block {
    public MeteoriteBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                              Hand hand, BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (world.isClient) return ActionResult.SUCCESS;

        if (player instanceof ServerPlayerEntity serverPlayer) {
            SymbioteData data = SymbioteData.get(serverPlayer.getServerWorld());
            if (data.isBonded(player.getUuid())) {
                player.sendMessage(Text.literal("§7У тебя уже есть связь с симбиотом."), false);
                return ActionResult.CONSUME;
            }
        }

        SymbioteEntity symbiote = SymbioteEvolution.SYMBIOTE.create(world);
        if (symbiote != null) {
            symbiote.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 1.0,
                    pos.getZ() + 0.5, world.random.nextFloat() * 360.0F, 0.0F);
            symbiote.setPersistent();
            symbiote.setCustomName(Text.literal("Дикий симбиот"));
            world.spawnEntity(symbiote);
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            player.sendMessage(Text.literal("§5Из метеорита выползает живой симбиот... Подойди и нажми ПКМ."), false);
        }
        return ActionResult.CONSUME;
    }
}
