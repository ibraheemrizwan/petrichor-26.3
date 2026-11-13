package com.pockyl.petrichor.client.render;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import net.minecraft.client.renderer.rendertype.RenderType;
/** Retains a static native vertex buffer and an immutable CPU snapshot for shader-pack submit rendering. */
public final class WeatherMesh implements AutoCloseable {
    private GpuBuffer vertices;
    private GpuBuffer indices;
    private boolean ownsIndices;
    private MeshData.DrawState state;
    private CapturedMesh snapshot;
    public void upload(MeshData mesh) {
        close();
        var device = RenderSystem.getDevice();
        vertices = device.createBuffer(() -> "Petrichor surface", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer());
        state = mesh.drawState();
        ownsIndices = mesh.indexBuffer() != null;
        if (ownsIndices) indices = device.createBuffer(() -> "Petrichor indices", GpuBuffer.USAGE_INDEX, mesh.indexBuffer());
        else {
            var sequential = RenderSystem.getSequentialBuffer(state.primitiveTopology());
            indices = sequential.getBuffer(state.indexCount());
            state = new MeshData.DrawState(state.format(),state.vertexCount(),state.indexCount(),state.primitiveTopology(),sequential.type());
        }
        snapshot = new CapturedMesh(mesh);
    }
    public void draw(WeatherShader shader) {
        if (vertices == null) return;
        if (!ownsIndices) {
            var sequential=RenderSystem.getSequentialBuffer(state.primitiveTopology());
            indices=sequential.getBuffer(state.indexCount());
            state=new MeshData.DrawState(state.format(),state.vertexCount(),state.indexCount(),state.primitiveTopology(),sequential.type());
        }
        shader.draw(vertices,indices,state);
    }
    public CapturedMesh.Submission at(RenderType type,double x,double y,double z) { return snapshot.at(type,x,y,z); }
    @Override public void close() {
        if (vertices != null) { vertices.close(); vertices=null; }
        if (ownsIndices && indices != null) indices.close();
        indices=null;
        if (snapshot != null) { snapshot.close(); snapshot=null; }
    }
}
