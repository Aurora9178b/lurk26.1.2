package meteordevelopment.meteorclient.renderer.text.ttf;

public record TtfGlyphDescriptor(TtfGlyphAtlas atlas, TtfGlyphAtlas.GlyphUV uv, int width, int height, int xOffset, int yOffset, int advance) {
}
