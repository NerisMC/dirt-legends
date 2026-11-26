package com.neoncoast.economy;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Everyone's bank balance, saved with the world. */
public class MoneyState extends PersistentState {
    public static final PersistentState.Type<MoneyState> TYPE = new PersistentState.Type<>(MoneyState::new, MoneyState::fromNbt, null);
    final Map<UUID, Long> balances = new HashMap<>();

    public static MoneyState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        MoneyState state = new MoneyState();
        NbtCompound b = nbt.getCompound("Balances");
        for (String key : b.getKeys()) {
            state.balances.put(UUID.fromString(key), b.getLong(key));
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtCompound b = new NbtCompound();
        balances.forEach((uuid, amount) -> b.putLong(uuid.toString(), amount));
        nbt.put("Balances", b);
        return nbt;
    }
}
