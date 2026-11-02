package com.symbiote.evolution.client;

import com.symbiote.evolution.SymbioteEntity;
import com.symbiote.evolution.SymbioteEvolution;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class SymbioteRenderer extends MobEntityRenderer<SymbioteEntity, SymbioteModel> {
    private static final Identifier TEXTURE = new Identifier(SymbioteEvolution.MOD_ID,
            "textures/entity/symbiote.png");

    public SymbioteRenderer(EntityRendererFactory.Context context) {
        super(context, new SymbioteModel(context.getPart(SymbioteClient.MODEL_LAYER)), 0.45F);
    }

    @Override
    public Identifier getTexture(SymbioteEntity entity) {
        return TEXTURE;
    }
}
