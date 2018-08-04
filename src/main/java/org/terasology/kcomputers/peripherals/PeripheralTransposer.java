/*
 * Copyright 2018 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.kcomputers.peripherals;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.jnlua.LuaValueProxy;
import org.terasology.kallisti.base.component.ComponentMethod;
import org.terasology.kallisti.base.component.Peripheral;
import org.terasology.logic.inventory.InventoryAccessComponent;
import org.terasology.logic.inventory.InventoryComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.InventoryUtils;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.nameTags.NameTagComponent;
import org.terasology.math.geom.Vector3i;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.BlockComponent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class PeripheralTransposer implements Peripheral {
    private final WorldProvider provider;
    private final BlockEntityRegistry registry;
    private final EntityRef self;
    private final BlockComponent block;
    private final InventoryManager inventoryManager;

    public PeripheralTransposer(WorldProvider provider, BlockEntityRegistry registry, EntityRef self, BlockComponent block, InventoryManager inventoryManager) {
        this.provider = provider;
        this.registry = registry;
        this.self = self;
        this.block = block;
        this.inventoryManager = inventoryManager;
    }

    @ComponentMethod(returnsMultipleArguments = true)
    public Object[] getInventorySize(Number side) {
        return getInventory(side.intValue(), (ref) -> new Object[] { ref.getComponent(InventoryComponent.class).itemSlots.size() });
    }

    @ComponentMethod(returnsMultipleArguments = true)
    public Object[] transferItem(Number sourceSide, Number sinkSide, Number count, Number sourceSlot, Number sinkSlot) {
        if (count.intValue() <= 0) {
            return new Object[] { null, "invalid count" };
        }

        return getInventory(sourceSide.intValue(), (sourceRef) -> getInventory(sinkSide.intValue(), (sinkRef) -> {
            int sourceSize = sourceRef.getComponent(InventoryComponent.class).itemSlots.size();
            int sinkSize = sinkRef.getComponent(InventoryComponent.class).itemSlots.size();

            if (sourceSlot.intValue() < 0 || sourceSlot.intValue() >= sourceSize) {
                return new Object[] { null, "invalid source slot" };
            }

            if (sinkSlot.intValue() < 0 || sinkSlot.intValue() >= sinkSize) {
                return new Object[] { null, "invalid sink slot" };
            }

            if (inventoryManager.moveItem(sourceRef, self, sourceSlot.intValue(), sinkRef, sinkSlot.intValue(), count.intValue())) {
                return new Object[] { count.intValue() };
            } else {
                return new Object[] { null, "item move failure" };
            }
        }));
    }

    @ComponentMethod(returnsMultipleArguments = true)
    public Object[] getSlotStackSize(Number side, Number slot) {
        return getItem(side.intValue(), slot.intValue(), (ref) -> new Object[] { PeripheralUtils.convertItem(ref).getOrDefault("size", 99) });
    }

    @ComponentMethod(returnsMultipleArguments = true)
    public Object[] getSlotMaxStackSize(Number side, Number slot) {
        return getItem(side.intValue(), slot.intValue(), (ref) -> new Object[] { PeripheralUtils.convertItem(ref).getOrDefault("maxSize", 99) });
    }

    @ComponentMethod(returnsMultipleArguments = true)
    public Object[] getStackInSlot(Number side, Number slot) {
        return getItem(side.intValue(), slot.intValue(), (ref) -> new Object[] { PeripheralUtils.convertItem(ref) });
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @ComponentMethod(returnsMultipleArguments = true)
    public Object[] compareStacks(Number side, Number slotA, Number slotB, Optional<Boolean> checkTag) {
        // ignore checkTag, we're not

        return getItem(side.intValue(), slotA.intValue(), (refA) -> getItem(side.intValue(), slotB.intValue(), (refB) -> {
            return new Object[] { refA.getComponent(ItemComponent.class).stackId.equals(refB.getComponent(ItemComponent.class).stackId) };
        }));
    }

    @ComponentMethod(returnsMultipleArguments = true)
    public Object[] areStacksEquivalent(Number side, Number slotA, Number slotB) {
        return compareStacks(side, slotA, slotB, Optional.empty());
    }

    protected Object[] getItem(int side, int slot, Function<EntityRef, Object[]> result) {
        return getInventory(side, (ref) -> {
            InventoryComponent component = ref.getComponent(InventoryComponent.class);
            int slotI = slot;
            if (slotI < 0 || slotI >= component.itemSlots.size()) {
                return new Object[]{ null, "invalid slot number" };
            } else {
                EntityRef item = component.itemSlots.get(slotI);
                return result.apply(item);
            }
        });
    }

    protected Object[] getInventory(int side, Function<EntityRef, Object[]> result) {
        Vector3i pos = getNeighborPos(side);
        if (pos != null && provider.isBlockRelevant(pos) && registry.hasPermanentBlockEntity(pos)) {
            EntityRef ref = registry.getBlockEntityAt(pos);
            if (ref != null && ref.exists() && ref.hasComponent(InventoryComponent.class)) {
                return result.apply(ref);
            }
        }

        return new Object[]{null, "no inventory"};
    }

    @Nullable
    protected Vector3i getNeighborPos(int side) {
        Vector3i pos = new Vector3i(block.getPosition());
        switch (side) {
            case 0:
                return pos.add(0, -1, 0);
            case 1:
                return pos.add(0, 1, 0);
            case 2:
                return pos.add(0, 0, -1);
            case 3:
                return pos.add(0, 0, 1);
            case 4:
                return pos.add(-1, 0, 0);
            case 5:
                return pos.add(1, 0, 0);
            default:
                return null;
        }
    }

    @Override
    public String type() {
        return "transposer";
    }
}