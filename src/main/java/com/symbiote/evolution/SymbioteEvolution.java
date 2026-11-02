package com.symbiote.evolution;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemGroups;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static net.minecraft.server.command.CommandManager.literal;

/** Main entry point for Symbiote Evolution. */
public class SymbioteEvolution implements ModInitializer {
    public static final String MOD_ID = "symbiote_evolution";
    public static final Identifier TOGGLE_PACKET = id("toggle_symbiosis");
    public static final Identifier FORM_PACKET = id("cycle_form");
    public static final Identifier TALK_PACKET = id("talk");
    public static final Identifier ABILITY_PACKET = id("use_ability");

    public static final EntityType<SymbioteEntity> SYMBIOTE = Registry.register(
            Registries.ENTITY_TYPE,
            id("symbiote"),
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, SymbioteEntity::new)
                    .dimensions(EntityDimensions.fixed(0.8F, 1.8F))
                    .trackRangeBlocks(12)
                    .trackedUpdateRate(2)
                    .build()
    );

    public static final Block METEORITE = Registry.register(
            Registries.BLOCK,
            id("meteorite"),
            new MeteoriteBlock(Block.Settings.copy(Blocks.OBSIDIAN).strength(7.0F, 1200.0F))
    );
    public static final BlockItem METEORITE_ITEM = Registry.register(
            Registries.ITEM, id("meteorite"), new BlockItem(METEORITE, new net.minecraft.item.Item.Settings())
    );

    static final UUID ATTACK_MODIFIER = UUID.fromString("dd5d7b5c-1f8c-45d8-b5e5-74c5343d1c01");
    static final UUID SPEED_MODIFIER = UUID.fromString("dd5d7b5c-1f8c-45d8-b5e5-74c5343d1c02");
    static final UUID ARMOR_MODIFIER = UUID.fromString("dd5d7b5c-1f8c-45d8-b5e5-74c5343d1c03");
    static final UUID HEALTH_MODIFIER = UUID.fromString("dd5d7b5c-1f8c-45d8-b5e5-74c5343d1c04");

    private static final String[] FORM_NAMES = {"Боевая", "Скоростная", "Защитная", "Паучья"};
    private static final int ABILITY_COOLDOWN_TICKS = 20 * 20;
    private final Map<UUID, Integer> abilityReadyAt = new ConcurrentHashMap<>();

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register(entries -> entries.add(METEORITE_ITEM));
        FabricDefaultAttributeRegistry.register(SYMBIOTE, SymbioteEntity.createAttributes());

        ServerPlayNetworking.registerGlobalReceiver(TOGGLE_PACKET, (server, player, handler, buf, responseSender) ->
                server.execute(() -> toggleSymbiosis(player)));
        ServerPlayNetworking.registerGlobalReceiver(FORM_PACKET, (server, player, handler, buf, responseSender) ->
                server.execute(() -> cycleForm(player)));
        ServerPlayNetworking.registerGlobalReceiver(TALK_PACKET, (server, player, handler, buf, responseSender) ->
                server.execute(() -> talk(player)));
        ServerPlayNetworking.registerGlobalReceiver(ABILITY_PACKET, (server, player, handler, buf, responseSender) ->
                server.execute(() -> useAbility(player)));

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            SymbioteData data = SymbioteData.get(player.getServerWorld());
            if (data.isBonded(player.getUuid()) && data.isActive(player.getUuid())) {
                applyForm(player, data.getForm(player.getUuid()));
            }
        });

        ServerTickEvents.END_WORLD_TICK.register(this::trySpawnMeteorite);
        ServerTickEvents.END_SERVER_TICK.register(this::tickPlayers);
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
    }

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    private void trySpawnMeteorite(ServerWorld world) {
        if (world.getRegistryKey() != World.OVERWORLD || world.getPlayers().isEmpty()) return;
        // Approximately one meteorite attempt per in-game day across the Overworld.
        if (world.random.nextInt(24000) != 0) return;

        ServerPlayerEntity player = world.getPlayers().get(world.random.nextInt(world.getPlayers().size()));
        int x = player.getBlockX() + world.random.nextInt(49) - 24;
        int z = player.getBlockZ() + world.random.nextInt(49) - 24;
        int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        if (y < world.getBottomY() + 2 || !world.getBlockState(pos).isAir()) return;

        if (world.setBlockState(pos, METEORITE.getDefaultState())) {
            player.sendMessage(Text.literal("§5Где-то неподалёку упал метеорит. Ищите фиолетовое свечение."), false);
        }
    }

    private void tickPlayers(MinecraftServer server) {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            SymbioteData data = SymbioteData.get(player.getServerWorld());
            if (!data.isBonded(player.getUuid())) continue;

            long time = player.getServerWorld().getTime();
            if (data.isActive(player.getUuid()) && time % 10 == 0) {
                player.getServerWorld().spawnParticles(ParticleTypes.DRAGON_BREATH,
                        player.getX(), player.getY() + 1.0, player.getZ(), 2,
                        0.25, 0.45, 0.25, 0.005);
            }
            // Ambient comments are intentionally occasional so they do not flood chat.
            if (time % 1800 == 0 && player.getRandom().nextInt(4) == 0) {
                player.sendMessage(Text.literal("§5Симбиот: §f" + randomLine(
                        data.getPersonality(player.getUuid()), data.getTrust(player.getUuid()))), false);
            }
        }
    }

    private void toggleSymbiosis(ServerPlayerEntity player) {
        SymbioteData data = SymbioteData.get(player.getServerWorld());
        if (!data.isBonded(player.getUuid())) {
            player.sendMessage(Text.literal("§7Симбиот ещё не связан с вами. Найдите метеорит."), false);
            return;
        }

        boolean active = data.toggleActive(player.getUuid());
        if (active) {
            applyForm(player, data.getForm(player.getUuid()));
            player.getServerWorld().spawnParticles(ParticleTypes.REVERSE_PORTAL,
                    player.getX(), player.getY() + 1.0, player.getZ(), 22,
                    0.45, 0.75, 0.45, 0.06);
            player.sendMessage(Text.literal("§5Слияние началось. §f" + randomLine(
                    data.getPersonality(player.getUuid()), data.getTrust(player.getUuid()))), false);
        } else {
            removeModifiers(player);
            player.getServerWorld().spawnParticles(ParticleTypes.POOF,
                    player.getX(), player.getY() + 1.0, player.getZ(), 12,
                    0.35, 0.45, 0.35, 0.02);
            player.sendMessage(Text.literal("§7Симбиот отступает под кожу."), false);
        }
    }

    private void cycleForm(ServerPlayerEntity player) {
        SymbioteData data = SymbioteData.get(player.getServerWorld());
        if (!data.isBonded(player.getUuid())) {
            player.sendMessage(Text.literal("§7Сначала найдите симбиота."), false);
            return;
        }
        int form = data.cycleForm(player.getUuid());
        if (data.isActive(player.getUuid())) applyForm(player, form);
        player.sendMessage(Text.literal("§5Форма симбиота: §f" + FORM_NAMES[form]), false);
    }

    private void talk(ServerPlayerEntity player) {
        SymbioteData data = SymbioteData.get(player.getServerWorld());
        if (!data.isBonded(player.getUuid())) {
            player.sendMessage(Text.literal("§7У вас пока нет симбиота."), false);
            return;
        }
        player.sendMessage(Text.literal("§5Симбиот: §f" + randomLine(
                data.getPersonality(player.getUuid()), data.getTrust(player.getUuid()))), false);
    }

    private void useAbility(ServerPlayerEntity player) {
        SymbioteData data = SymbioteData.get(player.getServerWorld());
        if (!data.isBonded(player.getUuid()) || !data.isActive(player.getUuid())) {
            player.sendMessage(Text.literal("§7Для способности нужно установить связь и включить слияние."), false);
            return;
        }

        int readyAt = abilityReadyAt.getOrDefault(player.getUuid(), 0);
        if (player.age < readyAt) {
            int seconds = Math.max(1, (readyAt - player.age + 19) / 20);
            player.sendMessage(Text.literal("§8Способность восстановится через §f" + seconds + " §8сек."), true);
            return;
        }

        switch (data.getForm(player.getUuid())) {
            case 0 -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 20 * 8, 1));
                player.sendMessage(Text.literal("§5Боевая форма: §fсила пробуждается!"), true);
            }
            case 1 -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 8, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 20 * 8, 1));
                player.sendMessage(Text.literal("§5Скоростная форма: §fдвигайся быстрее!"), true);
            }
            case 2 -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 20 * 7, 1));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 20 * 7, 1));
                player.sendMessage(Text.literal("§5Защитная форма: §fсимбиот укрепляет оболочку!"), true);
            }
            case 3 -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 20 * 10, 2));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 20 * 10, 0));
                player.sendMessage(Text.literal("§5Паучья форма: §fпрыгай выше и мягче приземляйся!"), true);
            }
            default -> { }
        }

        abilityReadyAt.put(player.getUuid(), player.age + ABILITY_COOLDOWN_TICKS);
        player.getServerWorld().spawnParticles(ParticleTypes.REVERSE_PORTAL,
                player.getX(), player.getY() + 1.0, player.getZ(), 18,
                0.35, 0.6, 0.35, 0.04);
        data.adjustTrust(player.getUuid(), 1);
    }

    private static void applyForm(ServerPlayerEntity player, int form) {
        removeModifiers(player);
        switch (form) {
            case 0 -> addModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK_MODIFIER,
                    "Symbiote combat power", 5.0);
            case 1 -> addModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_MODIFIER,
                    "Symbiote speed", 0.12);
            case 2 -> addModifier(player, EntityAttributes.GENERIC_ARMOR, ARMOR_MODIFIER,
                    "Symbiote armor", 8.0);
            case 3 -> {
                addModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_MODIFIER,
                        "Symbiote spider speed", 0.06);
                addModifier(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_MODIFIER,
                        "Symbiote vitality", 4.0);
                player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 4.0F));
            }
            default -> { }
        }
    }

    private static void addModifier(ServerPlayerEntity player, EntityAttribute attribute, UUID id,
                                    String name, double amount) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null) {
            instance.removeModifier(id);
            instance.addTemporaryModifier(new EntityAttributeModifier(id, name, amount,
                    EntityAttributeModifier.Operation.ADDITION));
        }
    }

    private static void removeModifiers(ServerPlayerEntity player) {
        removeModifier(player, EntityAttributes.GENERIC_ATTACK_DAMAGE, ATTACK_MODIFIER);
        removeModifier(player, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_MODIFIER);
        removeModifier(player, EntityAttributes.GENERIC_ARMOR, ARMOR_MODIFIER);
        removeModifier(player, EntityAttributes.GENERIC_MAX_HEALTH, HEALTH_MODIFIER);
    }

    private static void removeModifier(ServerPlayerEntity player, EntityAttribute attribute, UUID id) {
        EntityAttributeInstance instance = player.getAttributeInstance(attribute);
        if (instance != null) instance.removeModifier(id);
    }

    private static String randomLine(int personality, int trust) {
        List<String> lines;
        if (trust < 25) {
            lines = List.of(
                    "Не думай, что я обязан выполнять каждый твой приказ.",
                    "Наша связь становится слабее. Нам нужно научиться доверять друг другу.",
                    "Я помогу тебе, но не забывай: у меня тоже есть воля.",
                    "Ты зовёшь меня лишь тогда, когда тебе что-то нужно.",
                    "Мне нужно время, чтобы снова тебе доверять."
            );
        } else if (trust > 80) {
            lines = switch (personality) {
                case 1 -> List.of("Мы непобедимая команда. Ну… почти.", "Ха! Вот это приключение. Что дальше?", "Я прикрою тебя. Только не привыкай.");
                case 2 -> List.of("Я чувствую опасность раньше, чем она появляется. Доверься мне.", "Мы стали сильнее. Намного сильнее.", "Пусть этот мир попробует нас остановить.");
                case 3 -> List.of("Я рад, что мы нашли друг друга.", "Пока мы вместе, тебе не нужно бояться.", "Мне нравится это тихое место. Давай побудем здесь ещё немного.");
                default -> List.of("Мы теперь действительно одно целое.", "Я всегда рядом, хозяин.", "Твоя цель — моя цель.");
            };
        } else {
            lines = switch (personality) {
                case 1 -> List.of("Ну что, хозяин, куда теперь? Только не говори, что опять в шахту.",
                        "Ха. Я знал, что ты справишься.", "Мы отличная команда. Обычно.",
                        "Надеюсь, сегодня будет что-нибудь интереснее прогулки.");
                case 2 -> List.of("Не стой на месте. Я чувствую опасность.",
                        "Дай мне больше свободы, и мы станем сильнее.", "Этот мир слишком хрупкий.",
                        "Смотри по сторонам. Я не могу следить за всем сразу.");
                case 3 -> List.of("Не волнуйся. Я рядом.", "Давай немного исследуем мир.",
                        "Я запомнил это место. Здесь спокойно.", "Ты сегодня хорошо справляешься.");
                default -> List.of("Я чувствую, что рядом что-то есть.",
                        "Не забывай: мы теперь одно целое.", "Хозяин, я наблюдаю за всем вокруг.",
                        "Дай знать, когда понадоблюсь.");
            };
        }
        return lines.get((int) (Math.random() * lines.size()));
    }

    private void registerCommands(CommandDispatcher<net.minecraft.server.command.ServerCommandSource> dispatcher) {
        dispatcher.register(literal("symbiote")
                .then(literal("talk").executes(context -> {
                    talk(context.getSource().getPlayerOrThrow());
                    return 1;
                }))
                .then(literal("status").executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    SymbioteData data = SymbioteData.get(player.getServerWorld());
                    if (!data.isBonded(player.getUuid())) {
                        player.sendMessage(Text.literal("§7Связь с симбиотом не установлена."), false);
                    } else {
                        player.sendMessage(Text.literal("§5Доверие: §f" + data.getTrust(player.getUuid())
                                + " / 100  §5Характер: §f" + personalityName(data.getPersonality(player.getUuid()))
                                + "  §5Форма: §f" + FORM_NAMES[data.getForm(player.getUuid())]
                                + "  §5Слияние: §f" + (data.isActive(player.getUuid()) ? "включено" : "выключено")), false);
                    }
                    return 1;
                }))
                .then(literal("praise").executes(context -> {
                    adjustRelationship(context.getSource().getPlayerOrThrow(), 5,
                            "§aСимбиот доволен твоими словами. Доверие выросло.");
                    return 1;
                }))
                .then(literal("scold").executes(context -> {
                    adjustRelationship(context.getSource().getPlayerOrThrow(), -7,
                            "§cСимбиот обиделся. Доверие снизилось.");
                    return 1;
                }))
                .then(literal("meteorite").executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
                    BlockPos pos = player.getBlockPos().offset(player.getHorizontalFacing(), 2).up();
                    if (!player.getServerWorld().getBlockState(pos).isAir()) {
                        player.sendMessage(Text.literal("§7Перед тобой недостаточно места для метеорита."), false);
                        return 0;
                    }
                    player.getServerWorld().setBlockState(pos, METEORITE.getDefaultState());
                    player.sendMessage(Text.literal("§5Тестовый метеорит появился впереди. Нажми ПКМ по нему."), false);
                    return 1;
                }))
        );
    }

    private static void adjustRelationship(ServerPlayerEntity player, int amount, String message) {
        SymbioteData data = SymbioteData.get(player.getServerWorld());
        if (!data.isBonded(player.getUuid())) {
            player.sendMessage(Text.literal("§7У вас пока нет симбиота."), false);
            return;
        }
        data.adjustTrust(player.getUuid(), amount);
        player.sendMessage(Text.literal(message), false);
    }

    private static String personalityName(int personality) {
        return switch (personality) {
            case 1 -> "Саркастичный";
            case 2 -> "Агрессивный";
            case 3 -> "Заботливый";
            default -> "Спокойный";
        };
    }
}
