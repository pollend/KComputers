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
package org.terasology.kcomputers.rendering.nui.layers;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.kcomputers.components.KallistiComputerComponent;
import org.terasology.kcomputers.events.KallistiToggleComputerEvent;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.registry.In;
import org.terasology.rendering.nui.CoreScreenLayer;
import org.terasology.rendering.nui.UIWidget;
import org.terasology.rendering.nui.databinding.ReadOnlyBinding;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryGrid;
import org.terasology.rendering.nui.widgets.ActivateEventListener;
import org.terasology.rendering.nui.widgets.UIButton;

public class ComputerContainerScreen extends CoreScreenLayer {
    @In
    private LocalPlayer localPlayer;

    private InventoryGrid containerInventory;

    @Override
    public void initialise() {
        InventoryGrid inventory = find("inventory", InventoryGrid.class);
        inventory.bindTargetEntity(new ReadOnlyBinding<EntityRef>() {
            @Override
            public EntityRef get() {
                return localPlayer.getCharacterEntity();
            }
        });
        inventory.setCellOffset(10);

        containerInventory = find("container", InventoryGrid.class);
        containerInventory.bindTargetEntity(new ReadOnlyBinding<EntityRef>() {
            @Override
            public EntityRef get() {
                EntityRef characterEntity = localPlayer.getCharacterEntity();
                CharacterComponent characterComponent = characterEntity.getComponent(CharacterComponent.class);
                return characterComponent.predictedInteractionTarget;
            }
        });

        UIButton buttonOn = find("buttonOn", UIButton.class);
        buttonOn.subscribe(widget -> {
            EntityRef characterEntity = localPlayer.getCharacterEntity();
            CharacterComponent characterComponent = characterEntity.getComponent(CharacterComponent.class);
            EntityRef target = characterComponent.predictedInteractionTarget;
            if (target.hasComponent(KallistiComputerComponent.class)) {
                target.send(new KallistiToggleComputerEvent(true));
            }
        });

        UIButton buttonOff = find("buttonOff", UIButton.class);
        buttonOff.subscribe(widget -> {
            EntityRef characterEntity = localPlayer.getCharacterEntity();
            CharacterComponent characterComponent = characterEntity.getComponent(CharacterComponent.class);
            EntityRef target = characterComponent.predictedInteractionTarget;
            if (target.hasComponent(KallistiComputerComponent.class)) {
                target.send(new KallistiToggleComputerEvent(false));
            }
        });
    }

    @Override
    public boolean isModal() {
        return false;
    }
}