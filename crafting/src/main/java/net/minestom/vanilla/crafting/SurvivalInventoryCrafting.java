package net.minestom.vanilla.crafting;

import dev.goldenstack.window.InventoryView;
import dev.goldenstack.window.Views;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.inventory.InventoryClickEvent;
import net.minestom.server.inventory.PlayerInventory;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import net.minestom.vanilla.VanillaReimplementation;
import net.minestom.vanilla.datapack.Datapack;
import net.minestom.vanilla.datapack.recipe.Recipe;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public record SurvivalInventoryCrafting(Datapack datapack, VanillaReimplementation vri) {

    public void init() {
        EventNode<Event> node = EventNode.all("vri:survival-inventory-crafting");

        node.addListener(InventoryClickEvent.class, event -> {
            Player player = event.getPlayer();
            PlayerInventory inv = player.getInventory();
            int slot = event.getSlot();
            if (event.getInventory() != null) return; // we only are dealing with player inventories

            var crafting = Views.player().crafting();

            InventoryView input = crafting.input();
            InventoryView.Singular output = crafting.output();

            // if we are clicking on the output slot, remove one from all of the input slots
            if (output.isValidExternal(slot)) {
                for (int i = 0; i < 4; i++) {
                    input.set(inv, i, input.get(inv, i).withAmount(prev -> prev - 1));
                }
                return;
            }

            Material topLeft = input.get(inv, 0).material();
            Material topRight = input.get(inv, 1).material();
            Material bottomLeft = input.get(inv, 2).material();
            Material bottomRight = input.get(inv, 3).material();

            ItemStack result = tryRecipe(topLeft, topRight, bottomLeft, bottomRight);

            output.set(inv, Objects.requireNonNullElse(result, ItemStack.AIR));
        });

        vri.process().eventHandler().addChild(node);
    }

    private @Nullable ItemStack tryRecipe(Material topLeft, Material topRight, Material bottomLeft, Material bottomRight) {
        CraftingUtils utils = new CraftingUtils(datapack);

        for (var entry : datapack.namespacedData().entrySet()) {
            Datapack.NamespacedData data = entry.getValue();

            for (String file : data.recipes().files()) {
                Recipe recipe = data.recipes().file(file);

                if (recipe.type().equals(NamespaceID.from("minecraft:crafting_shapeless"))) {
                    Recipe.Shapeless shapeless = (Recipe.Shapeless) recipe;
                    if (utils.recipeMatchesShapeless(shapeless, List.of(topLeft, topRight, bottomLeft, bottomRight))) {
                        return ItemStack.of(shapeless.result().item(), shapeless.result().count() == null ? 1 : shapeless.result().count());
                    }
                }

                if (recipe.type().equals(NamespaceID.from("minecraft:crafting_shaped"))) {
                    Recipe.Shaped shaped = (Recipe.Shaped) recipe;

                    if (utils.recipeMatchesShaped2x2(shaped, List.of(topLeft, topRight, bottomLeft, bottomRight))) {
                        return ItemStack.of(shaped.result().item(), shaped.result().count() == null ? 1 : shaped.result().count());
                    }
                }
            }
        }
        return null;
    }
}
