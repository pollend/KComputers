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
import org.terasology.kallisti.base.interfaces.Synchronizable;
import org.terasology.network.OwnerEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@OwnerEvent
public class KallistiSyncInitialEvent implements Event {
	private EntityRef entity;
	private byte[] data;

	public KallistiSyncInitialEvent() {

	}

	public KallistiSyncInitialEvent(EntityRef entity, Synchronizable sync) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		sync.writeSyncPacket(Synchronizable.Type.INITIAL, stream);

		this.entity = entity;
		this.data = stream.toByteArray();
	}

	public EntityRef getSyncEntity() {
		return entity;
	}

	public byte[] getData() {
		return data;
	}
}
