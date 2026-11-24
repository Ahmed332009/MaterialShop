package dev.materialshop;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One item for sale, with 1-4 alternative payment options. */
public final class Listing {
    private final UUID id;
    private ItemStack item;
    private final List<PaymentOption> options = new ArrayList<>();

    public Listing(UUID id, ItemStack item) {
        this.id = id;
        ItemStack copy = item.clone();
        copy.setAmount(1);
        this.item = copy;
    }

    public UUID id() {
        return id;
    }

    public ItemStack item() {
        return item.clone();
    }

    public void item(ItemStack source) {
        ItemStack copy = source.clone();
        copy.setAmount(1);
        this.item = copy;
    }

    public List<PaymentOption> options() {
        List<PaymentOption> out = new ArrayList<>();
        for (PaymentOption option : options) {
            out.add(option.copy());
        }
        return out;
    }

    public PaymentOption option(int index) {
        return index >= 0 && index < options.size() ? options.get(index).copy() : null;
    }

    public boolean addOption(PaymentOption option) {
        if (options.size() >= 4) {
            return false;
        }
        options.add(option.copy());
        return true;
    }

    public boolean removeOption(int index) {
        if (index < 0 || index >= options.size()) {
            return false;
        }
        options.remove(index);
        return true;
    }

    public boolean setOptionAmount(int index, int amount) {
        if (index < 0 || index >= options.size()) {
            return false;
        }
        options.get(index).amount(amount);
        return true;
    }

    public int optionCount() {
        return options.size();
    }
}
