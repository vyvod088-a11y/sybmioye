package com.symbiote.evolution;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class SymbioteEntity extends PathAwareEntity {
    protected SymbioteEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 12;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (this.getWorld().isClient) return ActionResult.SUCCESS;

        if (player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            SymbioteData data = SymbioteData.get(serverPlayer.getServerWorld());
            if (!data.isBonded(player.getUuid())) {
                int personality = this.random.nextInt(4);
                data.createBond(player.getUuid(), personality);
                player.sendMessage(Text.literal("§5Симбиот слился с вами. Нажмите G для трансформации и H для смены формы."), false);
                player.sendMessage(Text.literal("§7Связь установлена. Характер симбиота будет раскрываться со временем."), false);
                this.discard();
            } else {
                player.sendMessage(Text.literal("§5У вас уже есть симбиот."), false);
            }
        }
        return ActionResult.SUCCESS;
    }
}
