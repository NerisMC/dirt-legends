package com.dirtlegends.npc;

import com.dirtlegends.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Works a stand at the track. Right-click to trade (emeralds, which you win by finishing laps).
 * FOOD vendors run the concession stands; MECHANIC vendors sell fuel, parts and gear in the pits.
 */
public class VendorEntity extends AbstractVillager {
    public static final int FOOD = 0;
    public static final int MECHANIC = 1;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(VendorEntity.class, EntityDataSerializers.INT);
    private int talkCooldown;

    public VendorEntity(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, FOOD);
    }

    public int getKind() {
        return this.entityData.get(KIND);
    }

    public void setup(int kind) {
        this.entityData.set(KIND, kind);
        this.setCustomName(Component.literal(kind == MECHANIC ? "Pit Mechanic" : "Concessions"));
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8.0f));
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("Kind", getKind());
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.entityData.set(KIND, in.getIntOr("Kind", FOOD));
    }

    private static MerchantOffer sell(int emeralds, Item item, int count) {
        return new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack(item, count), 9999, 0, 0.0f);
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers offers = this.getOffers();
        if (getKind() == MECHANIC) {
            offers.add(sell(2, ModItems.FUEL_CAN, 1));
            offers.add(sell(3, ModItems.TIRE, 2));
            offers.add(sell(6, ModItems.ENGINE, 1));
            offers.add(sell(2, ModItems.MUD_BUCKET, 1));
            offers.add(sell(4, ModItems.MX_HELMET, 1));
            offers.add(sell(5, ModItems.MX_JERSEY, 1));
            offers.add(sell(5, ModItems.MX_PANTS, 1));
            offers.add(sell(3, ModItems.MX_BOOTS, 1));
            offers.add(sell(10, ModItems.TRACK_KIT, 1));
        } else {
            offers.add(sell(1, ModItems.HOT_DOG, 3));
            offers.add(sell(1, ModItems.BURGER, 2));
            offers.add(sell(1, ModItems.NACHOS, 2));
            offers.add(sell(1, ModItems.POPCORN, 4));
            offers.add(sell(1, ModItems.SODA, 2));
            offers.add(sell(2, ModItems.ENERGY_DRINK, 1));
        }
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isAlive() || this.isTrading() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!this.level().isClientSide() && player instanceof ServerPlayer sp) {
            if (talkCooldown <= 0) {
                TrackTalk.say(sp, getCustomName() != null ? getCustomName().getString() : "Vendor",
                        TrackTalk.pick(getKind() == MECHANIC ? TrackTalk.VENDOR_PITS : TrackTalk.VENDOR_FOOD, this.random));
                talkCooldown = 100;
            }
            if (!this.getOffers().isEmpty()) {
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();
        if (talkCooldown > 0) talkCooldown--;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
