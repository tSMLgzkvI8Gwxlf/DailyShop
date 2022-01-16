package io.github.divios.lib.dLib.synchronizedGui.singleGui;

import io.github.divios.core_lib.events.Events;
import io.github.divios.core_lib.events.Subscription;
import io.github.divios.core_lib.itemutils.ItemBuilder;
import io.github.divios.core_lib.itemutils.ItemUtils;
import io.github.divios.core_lib.misc.WeightedRandom;
import io.github.divios.dailyShop.DailyShop;
import io.github.divios.dailyShop.events.updateItemEvent;
import io.github.divios.dailyShop.files.Messages;
import io.github.divios.dailyShop.lorestategy.shopItemsLore;
import io.github.divios.dailyShop.utils.DebugLog;
import io.github.divios.dailyShop.utils.Utils;
import io.github.divios.lib.dLib.dItem;
import io.github.divios.lib.dLib.dShop;
import io.github.divios.lib.dLib.dTransaction.SingleTransaction;
import io.github.divios.lib.dLib.dTransaction.Transactions;
import io.github.divios.lib.dLib.synchronizedGui.taskPool.updatePool;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Class that holds a {@link dInventory} for a unique player and
 * also its base.
 * <p>
 * Subscribes to the updatePool to update placeholders
 */

public class singleGuiImpl implements singleGui, Cloneable {

    protected static final DailyShop plugin = DailyShop.get();

    protected boolean isDestroyed = false;

    protected Player p;
    private final dShop shop;
    private dInventory own;
    private Set<Subscription> events = new HashSet<>();

    protected singleGuiImpl(Player p, dShop shop, singleGui base) {
        this(p, shop, base.getInventory());
    }

    protected singleGuiImpl(Player p, dShop shop, dInventory base) {
        this.p = p;
        this.shop = shop;
        this.own = base.clone();

        if (p != null) {
            //updateTask();
            updatePool.subscribe(this);
            this.own.openInventory(p);
        } else
            ready();
    }

    private void ready() {
        events.add(
                Events.subscribe(TransactionEvent.class)
                        .filter(o -> o.getCaller() == own)
                        .handler(o -> {
                            if (o.getType() == SingleTransaction.Type.BUY) {

                                double basePrice = o.getItem().getPlayerBuyPrice(p, shop) / o.getItem().getItem().getAmount();
                                if (!o.getItem().getEcon().hasMoney(p, basePrice)) {
                                    Messages.MSG_NOT_MONEY.send(p);
                                    return;
                                }

                                Transactions.BuyTransaction()
                                        .withShop(shop)
                                        .withBuyer(o.getPlayer())
                                        .withItem(o.getItem())
                                        .execute();
                            } else if (o.getType() == SingleTransaction.Type.SELL)
                                Transactions.SellTransaction()
                                        .withShop(shop)
                                        .withVendor(o.getPlayer())
                                        .withItem(o.getItem())
                                        .execute();
                        })
        );

    }

    @Override
    public void updateItem(updateItemEvent o) {
        updateItemEvent.type type = o.getType();
        dItem toUpdateItem = shop.getItem(o.getUuid());

        switch (type) {
            case UPDATE_ITEM:
                if (toUpdateItem == null) return;
                DebugLog.info("Updated item from singleGui of id: " + toUpdateItem.getID() + " with player " + (p == null ? "null" : p.getName()));
                own.updateDailyItem(toUpdateItem);
                updateTask();
                break;
            case NEXT_AMOUNT:
                if (toUpdateItem == null) return;
                DebugLog.info("Decrement stock from singleGui of id: " + toUpdateItem.getID() + " with player " + (p == null ? "null" : p.getName()));
                dItem buttonItem = own.buttons.get(toUpdateItem.getUUID());
                buttonItem.decrementStock(o.getPlayer(), o.getAmount());
                updateTask();
                break;
            case DELETE_ITEM:
                own.removeButton(o.getUuid());
                DebugLog.info("Deleted item from singleGui of id: " + o.getUuid() + " with player " + (p == null ? "null" : p.getName()));
                break;
            default:
                throw new UnsupportedOperationException("Invalid updateItemEvent type");
        }
    }

    @Override
    public void updateTask() {
        Set<Integer> dailySlots = own.dailyItemsSlots;

        own.buttonsSlot
                .forEach((integer, dItem) -> {
                    if (dItem.isAir()) return;
                    try {
                        ItemStack oldItem;
                        ItemBuilder newItem;
                        if (dailySlots.contains(integer)) {
                            newItem = ItemBuilder.of(shopItemsLore.applyLore(dItem, p, shop));
                        } else {
                            oldItem = dItem.getItemWithId();

                            newItem = ItemBuilder.of(oldItem).setLore(Collections.emptyList());
                            if (ItemUtils.getMetadata(newItem).hasDisplayName())
                                newItem = newItem.setName(Utils.JTEXT_PARSER.parse(ItemUtils.getName(oldItem), p));

                            for (String s : ItemUtils.getLore(oldItem))
                                newItem = newItem.addLore(Utils.JTEXT_PARSER.parse(s, p));
                        }
                        own.getInventory().setItem(integer, newItem);
                    } catch (Exception ignored) {}
                });

    }

    @Override
    public void restock() {
        Set<dItem> newItems = dRandomItemsSelector.fromItems(shop.getItems())
                .roll(own.dailyItemsSlots.size());
        own.restock(newItems);
        updateTask();
    }

    @Override
    public Player getPlayer() {
        return p;
    }

    @Override
    public dInventory getInventory() {
        return own;
    }

    @Override
    public dShop getShop() {
        return shop;
    }

    @Override
    public singleGui copy(Player p) {
        singleGuiImpl clone = clone();
        clone.p = p;
        if (p != null) clone.own.openInventory(p);

        return clone;
    }

    @Override
    public singleGui deepCopy(Player p) {
        singleGuiImpl clone = clone();
        clone.own = own.deepClone();
        clone.p = p;
        if (p != null) clone.own.openInventory(p);

        return clone;
    }

    @Override
    public void destroy() {
        if (isDestroyed) return;
        isDestroyed = true;
        events.forEach(Subscription::unregister);
        own.destroy();
        updatePool.unsubscribe(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        singleGuiImpl singleGui = (singleGuiImpl) o;
        return isDestroyed == singleGui.isDestroyed
                && Objects.equals(p, singleGui.p)
                && Objects.equals(shop, singleGui.shop)
                && Objects.equals(own, singleGui.own);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isDestroyed, p, shop, own);
    }

    @Override
    public singleGuiImpl clone() {
        try {
            singleGuiImpl clone = (singleGuiImpl) super.clone();

            clone.own = own.clone();
            clone.events = new HashSet<>();
            clone.ready();
            updatePool.subscribe(clone);

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    /**
     * Inner utility class to generate the daily Items
     */
    @SuppressWarnings("unused")
    private final static class dRandomItemsSelector {

        private static final Predicate<dItem> filterItems = item ->
                !(item.getBuyPrice() <= 0 && item.getSellPrice() <= 0)
                        || item.getRarity().getWeight() != 0;

        private static final Function<dItem, Integer> getWeights = dItem -> dItem.getRarity().getWeight();

        public static dRandomItemsSelector fromItems(Collection<dItem> items) {
            return new dRandomItemsSelector(items, Function.identity());
        }

        public static dRandomItemsSelector of(Collection<dItem> items, Function<dItem, dItem> action) {
            return new dRandomItemsSelector(items, action);
        }

        private final Map<UUID, dItem> items;
        private final Function<dItem, dItem> action;

        private dRandomItemsSelector(Collection<dItem> items, Function<dItem, dItem> action) {
            this(items.stream().collect(Collectors.toMap(dItem::getUUID, dItem -> dItem)), action);
        }

        private dRandomItemsSelector(Map<UUID, dItem> items, Function<dItem, dItem> action) {
            this.items = items.entrySet().stream()
                    .filter(entry -> filterItems.test(entry.getValue()))
                    .collect(Collectors
                            .toMap(Map.Entry::getKey, Map.Entry::getValue)
                    );
            this.action = action;
        }

        public void add(dItem item) {
            items.put(item.getUUID(), item);
        }

        public dItem remove(String id) {
            return remove(UUID.nameUUIDFromBytes(id.getBytes()));
        }

        public dItem remove(UUID uuid) {
            return items.remove(uuid);
        }

        public Set<dItem> getItems() {
            return Collections.unmodifiableSet(new HashSet<>(items.values()));
        }

        public Set<dItem> roll() {
            return roll(54);
        }

        public Set<dItem> roll(int max) {
            Set<dItem> rolledItems = new LinkedHashSet<>();

            WeightedRandom<dItem> randomSelector = WeightedRandom.fromCollection(items.values(), dItem::clone, getWeights::apply);

            for (int i = 0; i < max; i++) {
                dItem rolledItem = randomSelector.roll();
                if (rolledItem == null) break;

                rolledItem.generateNewBuyPrice();
                rolledItem.generateNewSellPrice();

                randomSelector.remove(rolledItem);
                rolledItems.add(action.apply(rolledItem));
            }

            return rolledItems;
        }
    }


}
