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
package org.terasology.kcomputers.components;

import com.google.common.primitives.UnsignedBytes;
import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.kallisti.base.interfaces.FrameBuffer;
import org.terasology.kallisti.base.interfaces.Synchronizable;
import org.terasology.kallisti.base.util.Dimension;
import org.terasology.kallisti.base.util.KallistiFileUtils;
import org.terasology.kallisti.oc.OCFont;
import org.terasology.kallisti.oc.OCGPURenderer;
import org.terasology.kallisti.oc.OCTextRenderer;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3f;
import org.terasology.rendering.assets.material.Material;
import org.terasology.rendering.assets.material.MaterialData;
import org.terasology.rendering.assets.mesh.MeshBuilder;
import org.terasology.rendering.assets.texture.Texture;
import org.terasology.rendering.assets.texture.TextureData;
import org.terasology.rendering.assets.texture.TextureUtil;
import org.terasology.rendering.logic.MeshComponent;
import org.terasology.rendering.nui.Color;
import org.terasology.utilities.Assets;
import org.terasology.world.block.BlockPart;
import org.terasology.world.block.shapes.BlockMeshPart;
import org.terasology.world.block.shapes.BlockShape;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Collections;

public class KallistiDisplayComponent implements Component, FrameBuffer, Synchronizable.Receiver, KallistiComponentContainer {
	private transient Synchronizable source;
	private transient Renderer renderer;
	private transient EntityManager entityManager;
	private transient Vector3f location;
	private transient EntityRef self;
	private transient MeshRenderComponent mesh;

	public void setMeshRenderComponent(EntityManager entityManager, Vector3f location, EntityRef self, MeshRenderComponent mesh) {
		this.entityManager = entityManager;
		this.location = location;
		this.self = self;
		this.mesh = mesh;
	}

	public Synchronizable getSource() {
		return source;
	}

	@Override
	public void bind(Synchronizable source, Renderer renderer) {
		this.source = source;
		this.renderer = renderer;
	}

	@Override
	public Dimension aspectRatio() {
		return new Dimension(1, 1);
	}

	private transient ByteBuffer dataBB;
	private transient int i;

	@Override
	public void finalize() {
		dataBB.clear();
	}

	@Override
	public void blit(Image image) {
		String key = "tmp";
		MeshComponent component;

		if (dataBB == null || dataBB.capacity() != 4 * image.size().getX() * image.size().getY()) {
			if (dataBB != null) {
				dataBB.clear();
			}
			dataBB = ByteBuffer.allocateDirect(4 * image.size().getX() * image.size().getY());
		}

		for (int argb : image.data()) {
			int r = (argb >> 16) & 0xFF;
			int g = (argb >> 8) & 0xFF;
			int b = argb & 0xFF;
			dataBB.put(UnsignedBytes.checkedCast(r));
			dataBB.put(UnsignedBytes.checkedCast(g));
			dataBB.put(UnsignedBytes.checkedCast(b));
			dataBB.put((byte) 0xFF);
		}

		dataBB.rewind();

		Texture texture = Assets.generateAsset(new TextureData(image.size().getX(), image.size().getY(),
			new ByteBuffer[]{ dataBB }, Texture.WrapMode.REPEAT, Texture.FilterMode.NEAREST), Texture.class);

		MaterialData terrainMatData = new MaterialData(Assets.getShader("engine:genericMeshMaterial").get());
		terrainMatData.setParam("diffuse", texture);
		terrainMatData.setParam("colorOffset", new float[]{1, 1, 1});
		terrainMatData.setParam("textured", true);
		Material material = Assets.generateAsset(terrainMatData, Material.class);

		component = mesh.get(key);
		if (component != null) {
			component.material.dispose();
			component.material = material;
		} else {
			component = new MeshComponent();
			component.material = material;

			MeshBuilder meshBuilder = new MeshBuilder();
			BlockShape blockShape = Assets.get("engine:cube", BlockShape.class).get();
			BlockMeshPart meshPart = blockShape.getMeshPart(BlockPart.fromSide(Side.TOP));

			for (int i = 0; i < meshPart.indicesSize(); i++) {
				meshBuilder.addIndices(meshPart.getIndex(i));
			}

			for (int i = 0; i < meshPart.size(); i++) {
				meshBuilder.addVertex(new Vector3f(meshPart.getVertex(i)).add(0, 0, 0));
				meshBuilder.addColor(Color.WHITE);
				meshBuilder.addTexCoord(meshPart.getTexCoord(i));
			}

			component.mesh = meshBuilder.build();
			component.translucent = false;
			component.hideFromOwner = false;
			component.color = Color.WHITE;

			mesh.add(entityManager, key, new Vector3f(location).add(0, 1f, 0), component);
		}

		self.saveComponent(mesh);
	}

	private void initRenderer() {
		if (renderer == null) {
			try {
				renderer = new OCGPURenderer(
						new OCTextRenderer(
								new OCFont(
										KallistiFileUtils.readString(
												new File("/home/asie/Kallisti/funscii-16.hex")
										),
										16
								)
						)
				);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void render() {
		initRenderer();
		renderer.render(this);
	}

	@Override
	public Collection<Object> getKallistiComponents() {
		return Collections.singleton(this);
	}

	@Override
	public void update(InputStream stream) throws IOException {
		initRenderer();

		if (renderer != null) {
			renderer.update(stream);
			render();
		}
	}
}