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
package org.terasology.kcomputers.events;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.Event;
import org.terasology.kallisti.base.component.ComponentContext;
import org.terasology.kallisti.base.component.Machine;
import org.terasology.kcomputers.TerasologyEntityContext;
import org.terasology.network.ServerEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Event for attaching Kallisti components to a virtual machine.
 *
 * Send this Event to a given EntityRef to gather all of the components
 * it provides.
 *
 * Receive this Event and use addComponent() to add your own components.
 *
 * @see org.terasology.kcomputers.KComputersUtil
 */
public class KallistiAttachComponentsEvent implements Event {
    private transient final Map<ComponentContext, Object> components = new HashMap<>();
    private transient final Set<Object> addedObjects = Collections.newSetFromMap(new IdentityHashMap<>());
    private int id = 0;

    public KallistiAttachComponentsEvent() {
    }

    /**
     * Add a component to be attached.
     * @param ref The reference to the entity containing the component.
     * @param o The object, which is a Kallisti-compatible component.
     */
    public void addComponent(EntityRef ref, Object o) {
        if (!addedObjects.contains(o)) {
            components.put(new TerasologyEntityContext(ref.getId(), id++), o);
            addedObjects.add(o);
        }
    }

    /**
     * Get the map of component context to their Kallisti objects.
     * @return The map.
     */
    public Map<ComponentContext,Object> getComponentMap() {
        return Collections.unmodifiableMap(components);
    }
}
