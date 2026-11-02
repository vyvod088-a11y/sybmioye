package com.symbiote.evolution.client;

import com.symbiote.evolution.SymbioteEntity;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;

/** A custom creature model with four independently animated back tendrils. */
public class SymbioteModel extends EntityModel<SymbioteEntity> {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart tendrilLeft;
    private final ModelPart tendrilRight;
    private final ModelPart tendrilLeftOuter;
    private final ModelPart tendrilRightOuter;

    public SymbioteModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        this.tendrilLeft = root.getChild("tendril_left");
        this.tendrilRight = root.getChild("tendril_right");
        this.tendrilLeftOuter = root.getChild("tendril_left_outer");
        this.tendrilRightOuter = root.getChild("tendril_right_outer");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();

        root.addChild("body", ModelPartBuilder.create().uv(16, 16)
                .cuboid(-4.0F, -12.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                ModelTransform.pivot(0.0F, 12.0F, 0.0F));

        ModelPartData head = root.addChild("head", ModelPartBuilder.create().uv(0, 0)
                .cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), ModelTransform.NONE);
        head.addChild("left_eye", ModelPartBuilder.create().uv(32, 0)
                .cuboid(-3.2F, -5.4F, -4.45F, 2.7F, 1.5F, 0.7F), ModelTransform.NONE);
        head.addChild("right_eye", ModelPartBuilder.create().uv(32, 3)
                .cuboid(0.5F, -5.4F, -4.45F, 2.7F, 1.5F, 0.7F), ModelTransform.NONE);
        head.addChild("jaw", ModelPartBuilder.create().uv(16, 0)
                .cuboid(-2.7F, -1.9F, -4.35F, 5.4F, 2.2F, 0.8F), ModelTransform.NONE);

        root.addChild("right_arm", ModelPartBuilder.create().uv(40, 16)
                .cuboid(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), ModelTransform.pivot(-5.0F, 2.0F, 0.0F));
        root.addChild("left_arm", ModelPartBuilder.create().uv(40, 16)
                .cuboid(-2.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F), ModelTransform.pivot(5.0F, 2.0F, 0.0F));
        root.addChild("right_leg", ModelPartBuilder.create().uv(0, 32)
                .cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), ModelTransform.pivot(-2.0F, 12.0F, 0.0F));
        root.addChild("left_leg", ModelPartBuilder.create().uv(0, 32)
                .cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F), ModelTransform.pivot(2.0F, 12.0F, 0.0F));

        root.addChild("tendril_left", ModelPartBuilder.create().uv(48, 16)
                .cuboid(-0.5F, -1.0F, 0.0F, 1.0F, 12.0F, 1.0F), ModelTransform.pivot(2.5F, 4.0F, 3.0F));
        root.addChild("tendril_right", ModelPartBuilder.create().uv(48, 16)
                .cuboid(-0.5F, -1.0F, 0.0F, 1.0F, 12.0F, 1.0F), ModelTransform.pivot(-2.5F, 4.0F, 3.0F));
        root.addChild("tendril_left_outer", ModelPartBuilder.create().uv(52, 16)
                .cuboid(-0.5F, -1.0F, 0.0F, 1.0F, 10.0F, 1.0F), ModelTransform.pivot(4.0F, 2.0F, 2.0F));
        root.addChild("tendril_right_outer", ModelPartBuilder.create().uv(52, 16)
                .cuboid(-0.5F, -1.0F, 0.0F, 1.0F, 10.0F, 1.0F), ModelTransform.pivot(-4.0F, 2.0F, 2.0F));

        return TexturedModelData.of(data, 64, 64);
    }

    @Override
    public void setAngles(SymbioteEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        head.yaw = headYaw * 0.017453292F;
        head.pitch = headPitch * 0.017453292F;
        rightArm.pitch = (float) Math.cos(limbAngle * 0.6662F + Math.PI) * 2.0F * limbDistance * 0.5F;
        leftArm.pitch = (float) Math.cos(limbAngle * 0.6662F) * 2.0F * limbDistance * 0.5F;
        rightLeg.pitch = (float) Math.cos(limbAngle * 0.6662F) * 1.4F * limbDistance;
        leftLeg.pitch = (float) Math.cos(limbAngle * 0.6662F + Math.PI) * 1.4F * limbDistance;

        float idle = animationProgress * 0.085F;
        tendrilLeft.roll = (float) Math.sin(idle) * 0.35F;
        tendrilRight.roll = (float) Math.cos(idle) * 0.35F;
        tendrilLeft.pitch = (float) Math.sin(idle * 0.8F) * 0.18F;
        tendrilRight.pitch = (float) Math.cos(idle * 0.8F) * 0.18F;
        tendrilLeftOuter.roll = (float) Math.cos(idle * 0.7F + 0.8F) * 0.42F;
        tendrilRightOuter.roll = (float) Math.sin(idle * 0.7F + 0.8F) * 0.42F;
        tendrilLeftOuter.yaw = (float) Math.sin(idle * 0.6F) * 0.22F;
        tendrilRightOuter.yaw = (float) Math.cos(idle * 0.6F) * 0.22F;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay,
                       float red, float green, float blue, float alpha) {
        root.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}
