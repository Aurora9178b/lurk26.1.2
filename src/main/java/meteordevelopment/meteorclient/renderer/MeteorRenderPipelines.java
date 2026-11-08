package meteordevelopment.meteorclient.renderer;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;

public abstract class MeteorRenderPipelines {
   private static final List<RenderPipeline> PIPELINES = new ArrayList<>();
   private static final Snippet MESH_UNIFORMS = RenderPipeline.builder().withUniform("MeshData", UniformType.UNIFORM_BUFFER).buildSnippet();
   private static final Snippet MATRICES_PROJECTION_SNIPPET = RenderPipeline.builder()
      .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
      .withUniform("Projection", UniformType.UNIFORM_BUFFER)
      .buildSnippet();
   public static final RenderPipeline WORLD_COLORED = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/world_colored"))
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/pos_color.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/pos_color.frag"))
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline WORLD_COLORED_LINES = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLineSmooth()
         .withLocation(MeteorClient.identifier("pipeline/world_colored_lines"))
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.DEBUG_LINES)
         .withVertexShader(MeteorClient.identifier("shaders/pos_color.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/pos_color.frag"))
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline WORLD_COLORED_DEPTH = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/world_colored_depth"))
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/pos_color.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/pos_color.frag"))
         .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline WORLD_COLORED_LINES_DEPTH = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLineSmooth()
         .withLocation(MeteorClient.identifier("pipeline/world_colored_lines_depth"))
         .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, Mode.DEBUG_LINES)
         .withVertexShader(MeteorClient.identifier("shaders/pos_color.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/pos_color.frag"))
         .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline WORLD_TEXTURED = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/world_textured"))
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/pos_tex_color.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/pos_tex_color.frag"))
         .withSampler("u_Texture")
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline WORLD_TEXTURED_LIGHTNING = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/world_textured_lightning"))
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/pos_tex_color.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/pos_tex_color.frag"))
         .withSampler("u_Texture")
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline WORLD_TEXTURED_LIGHTNING_DEPTH = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/world_textured_lightning_depth"))
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/pos_tex_color.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/pos_tex_color.frag"))
         .withSampler("u_Texture")
         .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline UI_COLORED = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/ui_colored"))
         .withVertexFormat(MeteorVertexFormats.POS2_COLOR, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/pos_color.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/pos_color.frag"))
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(true)
         .build()
   );
   public static final RenderPipeline UI_COLORED_LINES = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/ui_colored_lines"))
         .withVertexFormat(MeteorVertexFormats.POS2_COLOR, Mode.DEBUG_LINES)
         .withVertexShader(MeteorClient.identifier("shaders/pos_color.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/pos_color.frag"))
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(true)
         .build()
   );
   public static final RenderPipeline UI_TEXTURED = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/ui_textured"))
         .withVertexFormat(MeteorVertexFormats.POS2_TEXTURE_COLOR, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/pos_tex_color.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/pos_tex_color.frag"))
         .withSampler("u_Texture")
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(true)
         .build()
   );
   public static final RenderPipeline UI_TEXT = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/ui_text"))
         .withVertexFormat(MeteorVertexFormats.POS2_TEXTURE_COLOR, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/text.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/text.frag"))
         .withSampler("u_Texture")
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(true)
         .build()
   );
   public static final RenderPipeline TTF_FONT_AA = add(
      new ExtendedRenderPipelineBuilder(MATRICES_PROJECTION_SNIPPET)
         .withLocation(MeteorClient.identifier("pipeline/ttf_font_aa"))
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, Mode.QUADS)
         .withVertexShader(MeteorClient.identifier("shaders/ttf_font.vsh"))
         .withFragmentShader(MeteorClient.identifier("shaders/ttf_font_aa.fsh"))
         .withSampler("Sampler0")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline TTF_FONT_NO_AA = add(
      new ExtendedRenderPipelineBuilder(MATRICES_PROJECTION_SNIPPET)
         .withLocation(MeteorClient.identifier("pipeline/ttf_font_no_aa"))
         .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, Mode.QUADS)
         .withVertexShader(MeteorClient.identifier("shaders/ttf_font.vsh"))
         .withFragmentShader(MeteorClient.identifier("shaders/ttf_font_no_aa.fsh"))
         .withSampler("Sampler0")
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline POST_OUTLINE = add(
      new ExtendedRenderPipelineBuilder()
         .withLocation(MeteorClient.identifier("pipeline/post/outline"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/post-process/base.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/post-process/outline.frag"))
         .withSampler("u_Texture")
         .withUniform("PostData", UniformType.UNIFORM_BUFFER)
         .withUniform("OutlineData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline POST_IMAGE = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/post/image"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/post-process/base.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/post-process/image.frag"))
         .withSampler("u_Texture")
         .withSampler("u_TextureI")
         .withUniform("PostData", UniformType.UNIFORM_BUFFER)
         .withUniform("ImageData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline BLUR_DOWN = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/blur/down"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/blur.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/blur_down.frag"))
         .withSampler("u_Texture")
         .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline BLUR_UP = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/blur/up"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/blur.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/blur_up.frag"))
         .withSampler("u_Texture")
         .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline BLUR_PASSTHROUGH = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/blur/up"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/passthrough.frag"))
         .withSampler("u_Texture")
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline BLUR_ARC = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/blur/arc"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/blur_arc.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/blur_arc.frag"))
         .withSampler("u_Texture")
         .withUniform("BlurRectData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline MOTION_BLUR = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/post/motion_blur"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/motion_blur.frag"))
         .withSampler("u_Texture")
         .withUniform("BlurData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline BLUR_ROUNDED = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/blur/rounded"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/rounded_blur.frag"))
         .withSampler("u_Texture")
         .withUniform("RoundedData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline TEXTURE_ROUNDED = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/ui/texture_rounded"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/texture_rounded.frag"))
         .withSampler("u_Texture")
         .withUniform("RectData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline RECT_ROUNDED = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/ui/rounded"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/rect_rounded.frag"))
         .withUniform("RectData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline RECT_ROUNDED_CORNERS = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/ui/rounded_corners"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/rect_rounded_corners.frag"))
         .withUniform("RectCornersData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline RECT_ACID_GLOW = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/ui/acid_glow"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/rect_acid_glow.frag"))
         .withUniform("AcidGlowData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline RECT_ROUNDED_OUTLINE = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/ui/rounded_outline"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/rect_rounded_outline.frag"))
         .withUniform("RectData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline RECT_SHADOW = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/ui/shadow"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/rect_shadow.frag"))
         .withUniform("RectData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline MAIN_MENU_BACKGROUND = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/ui/main_menu_background"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/main_menu_background.frag"))
         .withUniform("MainMenuData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline LIQUID_GLASS_CLEAR = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/liquid_glass/clear"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/liquid_glass_clear.frag"))
         .withSampler("u_Texture")
         .withUniform("LiquidGlassData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline LIQUID_GLASS_TINTED = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/liquid_glass/tinted"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/liquid_glass_tinted.frag"))
         .withSampler("u_Texture")
         .withUniform("LiquidGlassData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
         .withCull(false)
         .build()
   );
   public static final RenderPipeline LIQUID_GLASS_BLUR = add(
      new ExtendedRenderPipelineBuilder(MESH_UNIFORMS)
         .withLocation(MeteorClient.identifier("pipeline/liquid_glass/blur"))
         .withVertexFormat(MeteorVertexFormats.POS2, Mode.TRIANGLES)
         .withVertexShader(MeteorClient.identifier("shaders/passthrough.vert"))
         .withFragmentShader(MeteorClient.identifier("shaders/liquid_glass_blur.frag"))
         .withSampler("u_Texture")
         .withUniform("LiquidGlassBlurData", UniformType.UNIFORM_BUFFER)
         .withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, false))
         .withCull(false)
         .build()
   );

   private static RenderPipeline add(RenderPipeline pipeline) {
      PIPELINES.add(pipeline);
      return pipeline;
   }

   public static void precompile(ShaderManager shaderManager) {
      GpuDevice device = RenderSystem.getDevice();
      ResourceManager resources = Minecraft.getInstance().getResourceManager();

      for (RenderPipeline pipeline : PIPELINES) {
         device.precompilePipeline(pipeline, (identifier, shaderType) -> loadShaderSource(shaderManager, resources, identifier, shaderType));
      }
   }

   private static String loadShaderSource(ShaderManager shaderManager, ResourceManager resources, Identifier identifier, ShaderType shaderType) {
      ShaderType resourceType = ShaderType.byLocation(identifier);
      if (resourceType != null) {
         return shaderManager.getShader(resourceType.idConverter().fileToId(identifier), shaderType);
      }

      Optional<Resource> resourceOpt = resources.getResource(identifier);
      if (resourceOpt.isEmpty()) {
         throw new RuntimeException("Shader resource not found: " + identifier);
      }

      try (InputStream in = resourceOpt.get().open()) {
         return IOUtils.toString(in, StandardCharsets.UTF_8);
      } catch (IOException e) {
         throw new RuntimeException("Failed to load shader: " + identifier, e);
      }
   }

   private MeteorRenderPipelines() {
   }
}
