package com.pockyl.petrichor.client.render;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.pipeline.*;
import com.mojang.renderpearl.api.textures.*;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryUtil;

/** Named weather parameters backed by a real 26.3 pipeline and a std140 uniform buffer. */
public final class WeatherShader implements AutoCloseable {
    private static WeatherShader current;
    private static Matrix4f projection = new Matrix4f();
    private static FogData fog = new FogData();
    private final String name;
    private final Map<String, Uniform> uniforms = new LinkedHashMap<>();
    private final Map<String, GpuTextureView> textures = new HashMap<>();
    private final Set<String> samplerNames = new HashSet<>();
    private final RenderPipeline pipeline;
    private final RenderPipeline multiplyPipeline;
    private final RenderPipeline additivePipeline;
    private RenderPipeline selected;
    private final ByteBuffer data;
    private GpuBuffer uniformBuffer;
    private boolean explicitMatrices;
    public final Uniform CHUNK_OFFSET;

    public WeatherShader(String name, VertexFormat vertexFormat, boolean fullscreen, BlendFunction blend) {
        this.name = name;
        var resource = WeatherShader.class.getResourceAsStream("/assets/petrichor/shaders/port-layout.json");
        if (resource == null) throw new IllegalStateException("Missing weather uniform layouts");
        try (var reader = new InputStreamReader(resource, java.nio.charset.StandardCharsets.UTF_8)) {
            var layout = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject(name);
            data = MemoryUtil.memCalloc(layout.get("size").getAsInt()).order(ByteOrder.nativeOrder());
            var bindings = BindGroupLayout.builder().withUniform("WeatherUniforms", UniformType.UNIFORM_BUFFER);
            for (var item : layout.getAsJsonArray("uniforms")) {
                var value = item.getAsJsonObject();
                var uniform = new Uniform(value.get("offset").getAsInt(), value.get("type").getAsString().equals("int"), value.get("count").getAsInt());
                uniforms.put(value.get("name").getAsString(), uniform);
                var defaults = value.getAsJsonArray("values");
                float[] numbers = new float[defaults.size()];
                for (int i = 0; i < numbers.length; i++) numbers[i] = defaults.get(i).getAsFloat();
                uniform.set(numbers);
            }
            for (var item : layout.getAsJsonArray("samplers")) {
                samplerNames.add(item.getAsString());
                bindings.withUniform(item.getAsString(), UniformType.COMBINED_IMAGE_SAMPLER);
            }
            var builder = RenderPipeline.builder().withLocation(Identifier.fromNamespaceAndPath("petrichor", "pipeline/" + name))
                    .withVertexShader(Identifier.fromNamespaceAndPath("petrichor", "core/" + name))
                    .withFragmentShader(Identifier.fromNamespaceAndPath("petrichor", "core/" + name))
                    .withBindGroupLayout(bindings.build()).withVertexBinding(0, vertexFormat).withPrimitiveTopology(PrimitiveTopology.QUADS)
                    .withCull(false).withColorTargetState(new ColorTargetState(blend));
            if (fullscreen) builder.withDepthStencilState(Optional.empty());
            else builder.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false));
            pipeline = RenderPipelines.register(builder.build());
            if (name.endsWith("grade")) {
                multiplyPipeline = RenderPipelines.register(builder.withLocation(Identifier.fromNamespaceAndPath("petrichor", "pipeline/" + name + "_multiply"))
                        .withColorTargetState(new ColorTargetState(new BlendFunction(BlendFactor.DST_COLOR, BlendFactor.ZERO))).build());
                additivePipeline = RenderPipelines.register(builder.withLocation(Identifier.fromNamespaceAndPath("petrichor", "pipeline/" + name + "_additive"))
                        .withColorTargetState(new ColorTargetState(BlendFunction.ADDITIVE)).build());
            } else {
                multiplyPipeline = pipeline;
                additivePipeline = name.equals("petrichor_rain") ? RenderPipelines.register(builder
                        .withLocation(Identifier.fromNamespaceAndPath("petrichor", "pipeline/" + name + "_additive"))
                        .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING)).build()) : pipeline;
            }
            selected = pipeline;
        } catch (java.io.IOException ex) { throw new IllegalStateException("Loading shader layout " + name, ex); }
        CHUNK_OFFSET = getUniform("ChunkOffset");
    }

    public static void frame(Matrix4fc viewProjection, FogData fogData) { projection.set(viewProjection); fog = fogData; }
    public static Matrix4f projection() { return new Matrix4f(projection); }
    public static Matrix4f modelView() { return RenderSystem.getModelViewMatrixCopy(); }
    public static void use(WeatherShader shader) { current = shader; shader.explicitMatrices = false; shader.selected = shader.pipeline; }
    public void multiplyBlend() { selected = multiplyPipeline; }
    public void additiveBlend() { selected = additivePipeline; }
    public static float[] fogColor() { return new float[] {fog.color.x, fog.color.y, fog.color.z, fog.color.w}; }
    public static float fogEnd() { return Math.min(fog.environmentalEnd, fog.renderDistanceEnd); }
    public static WeatherShader current() { return current; }
    public Uniform getUniform(String name) { return uniforms.get(name); }
    public Uniform safeGetUniform(String name) { return uniforms.getOrDefault(name, new Uniform(-1, false, 0)); }
    public void setSampler(String name, Object texture) {
        if (texture instanceof Supplier<?> supplier) texture = supplier.get();
        if (texture instanceof AbstractTexture abstractTexture) texture = abstractTexture.getTextureView();
        if (texture instanceof GpuTextureView view) textures.put(name, view);
        else throw new IllegalArgumentException("Sampler " + name + " needs a texture view");
    }
    public static void texture(int unit, Identifier id) {
        current.setSampler("Sampler" + unit, Minecraft.getInstance().getTextureManager().getTexture(id));
    }
    public static void texture(int unit, GpuTextureView view) { current.setSampler("Sampler" + unit, view); }
    public void setDefaultUniforms(Object mode, Matrix4fc modelView, Matrix4fc projectionMatrix, Object window) {
        safeGetUniform("ModelViewMat").set(modelView);
        safeGetUniform("ProjMat").set(projectionMatrix);
        explicitMatrices = true;
    }
    public void apply() { current = this; }
    public void clear() { if (current == this) current = null; }

    public static void drawCurrent(MeshData mesh) {
        if (mesh == null) return;
        if (current == null) { mesh.close(); throw new IllegalStateException("No active weather shader"); }
        current.draw(mesh);
    }
    public void draw(MeshData mesh) {
        try (mesh; GpuBuffer vertices = RenderSystem.getDevice().createBuffer(() -> name + " vertices", GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer())) {
            if (mesh.indexBuffer() != null) {
                try (GpuBuffer indices = RenderSystem.getDevice().createBuffer(() -> name + " indices", GpuBuffer.USAGE_INDEX, mesh.indexBuffer())) {
                    draw(vertices, indices, mesh.drawState());
                }
            } else {
                var indices = RenderSystem.getSequentialBuffer(mesh.drawState().primitiveTopology());
                draw(vertices, indices.getBuffer(mesh.drawState().indexCount()), new MeshData.DrawState(mesh.drawState().format(), mesh.drawState().vertexCount(), mesh.drawState().indexCount(), mesh.drawState().primitiveTopology(), indices.type()));
            }
        }
    }
    public void draw(GpuBuffer vertices, GpuBuffer indices, MeshData.DrawState drawState) {
        if (!explicitMatrices) {
            safeGetUniform("ModelViewMat").set(modelView());
            safeGetUniform("ProjMat").set(projection);
        }
        safeGetUniform("FogStart").set(fog.environmentalStart);
        safeGetUniform("FogEnd").set(Math.min(fog.environmentalEnd, fog.renderDistanceEnd));
        safeGetUniform("FogColor").set(fog.color.x, fog.color.y, fog.color.z, fog.color.w);
        safeGetUniform("FogShape").set(0);
        var client = Minecraft.getInstance();
        textures.put("Sampler2", client.gameRenderer.levelLightmap());
        var device = RenderSystem.getDevice();
        if (uniformBuffer == null) uniformBuffer = device.createBuffer(() -> name + " uniforms", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, data.capacity());
        data.position(0).limit(data.capacity());
        var encoder = device.createCommandEncoder();
        encoder.writeToBuffer(uniformBuffer.slice(), data);
        var target = client.gameRenderer.mainRenderTarget();
        try (var pass = encoder.createRenderPass(() -> name, target.getColorTextureView(), Optional.empty(), target.getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(RenderSystem.getCompiledPipeline(selected));
            pass.setUniform("WeatherUniforms", uniformBuffer);
            for (var entry : textures.entrySet()) if (samplerNames.contains(entry.getKey())) pass.setUniform(entry.getKey(), entry.getValue(), RenderSystem.getSamplerCache().getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, false));
            pass.setVertexBuffer(0, vertices.slice());
            pass.setIndexBuffer(indices, drawState.indexType());
            pass.drawIndexed(drawState.indexCount(), 1, 0, 0, 0);
        }
    }
    @Override public void close() { if (uniformBuffer != null) uniformBuffer.close(); MemoryUtil.memFree(data); }

    public final class Uniform {
        private final int offset;
        private final boolean integer;
        private final int count;
        private Uniform(int offset, boolean integer, int count) { this.offset = offset; this.integer = integer; this.count = count; }
        public void set(float... values) {
            if (offset < 0) return;
            if (values.length != count && values.length != 1) throw new IllegalArgumentException("Expected " + count + " values, got " + values.length);
            for (int i = 0; i < count; i++) {
                float value = values[Math.min(i, values.length - 1)];
                if (integer) data.putInt(offset + i * 4, (int) value); else data.putFloat(offset + i * 4, value);
            }
        }
        public void set(int value) { set((float) value); }
        public void set(Matrix4fc matrix) { if (offset >= 0) matrix.get(offset, data); }
        public void upload() { /* The owning pass uploads one coherent parameter snapshot before drawing. */ }
    }
}
