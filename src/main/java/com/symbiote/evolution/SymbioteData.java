package com.symbiote.evolution;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** World-persistent bond data. One bonded symbiote is stored per player. */
public final class SymbioteData extends PersistentState {
    private static final String DATA_ID = "symbiote_evolution_data";
    private final Map<UUID, Bond> bonds = new HashMap<>();

    public static SymbioteData get(ServerWorld world) {
        // Store the bond in the Overworld save so it follows the player between dimensions.
        ServerWorld saveWorld = world.getServer().getOverworld();
        return saveWorld.getPersistentStateManager().getOrCreate(
                SymbioteData::fromNbt,
                SymbioteData::new,
                DATA_ID
        );
    }

    public boolean isBonded(UUID playerId) {
        return bonds.containsKey(playerId);
    }

    public boolean isActive(UUID playerId) {
        Bond bond = bonds.get(playerId);
        return bond != null && bond.active;
    }

    public int getForm(UUID playerId) {
        Bond bond = bonds.get(playerId);
        return bond == null ? 0 : bond.form;
    }

    public int getTrust(UUID playerId) {
        Bond bond = bonds.get(playerId);
        return bond == null ? 0 : bond.trust;
    }

    public int getPersonality(UUID playerId) {
        Bond bond = bonds.get(playerId);
        return bond == null ? 0 : bond.personality;
    }

    public void createBond(UUID playerId, int personality) {
        bonds.put(playerId, new Bond(false, 0, 50, personality));
        markDirty();
    }

    public boolean toggleActive(UUID playerId) {
        Bond bond = bonds.get(playerId);
        if (bond == null) return false;
        bond.active = !bond.active;
        bond.trust = Math.max(0, Math.min(100, bond.trust + (bond.active ? 2 : -3)));
        markDirty();
        return bond.active;
    }

    public void adjustTrust(UUID playerId, int amount) {
        Bond bond = bonds.get(playerId);
        if (bond == null) return;
        bond.trust = Math.max(0, Math.min(100, bond.trust + amount));
        markDirty();
    }

    public int cycleForm(UUID playerId) {
        Bond bond = bonds.get(playerId);
        if (bond == null) return 0;
        bond.form = (bond.form + 1) % 4;
        bond.trust = Math.min(100, bond.trust + 1);
        markDirty();
        return bond.form;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Bond> entry : bonds.entrySet()) {
            NbtCompound row = new NbtCompound();
            row.putUuid("player", entry.getKey());
            row.putBoolean("active", entry.getValue().active);
            row.putInt("form", entry.getValue().form);
            row.putInt("trust", entry.getValue().trust);
            row.putInt("personality", entry.getValue().personality);
            list.add(row);
        }
        nbt.put("bonds", list);
        return nbt;
    }

    public static SymbioteData fromNbt(NbtCompound nbt) {
        SymbioteData data = new SymbioteData();
        NbtList list = nbt.getList("bonds", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound row = list.getCompound(i);
            if (!row.containsUuid("player")) continue;
            UUID id = row.getUuid("player");
            data.bonds.put(id, new Bond(
                    row.getBoolean("active"),
                    Math.floorMod(row.getInt("form"), 4),
                    Math.max(0, Math.min(100, row.getInt("trust"))),
                    Math.floorMod(row.getInt("personality"), 4)
            ));
        }
        return data;
    }

    private static final class Bond {
        private boolean active;
        private int form;
        private int trust;
        private int personality;

        private Bond(boolean active, int form, int trust, int personality) {
            this.active = active;
            this.form = form;
            this.trust = trust;
            this.personality = personality;
        }
    }
}
