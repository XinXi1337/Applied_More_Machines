package com.appliedmoremachines.client;

import java.util.List;
import java.util.function.Function;
import javax.annotation.ParametersAreNonnullByDefault;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/** Connected casing faces, using only our existing base and 2x2 connection sprites. */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FactoryCasingModel extends BakedModelWrapper<BakedModel> {
    public static final IGeometryLoader<Geometry> LOADER = (json, context) -> new Geometry();
    private static final ModelProperty<Long> CONNECTIONS = new ModelProperty<>();
    // Clockwise, starting above the face: U, UR, R, DR, D, DL, L, UL.
    private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
    private static final int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};
    private final List<BakedQuad>[][] faces;

    @SuppressWarnings("unchecked")
    private FactoryCasingModel(BakedModel original, TextureAtlasSprite base, TextureAtlasSprite connected) {
        super(original);
        faces = (List<BakedQuad>[][]) new List<?>[6][256];
        var bakery = new FaceBakery();
        for (Direction face : Direction.values()) {
            BakedQuad[][] parts = new BakedQuad[4][5];
            for (int corner = 0; corner < 4; corner++) {
                for (int tile = 0; tile < 5; tile++) {
                    int x = corner & 1, y = corner >> 1;
                    TextureAtlasSprite sprite = tile == 0 ? base : connected;
                    // Connection sprite tiles: interior, vertical border, horizontal border, inner corner.
                    float u = tile == 0 ? x * 8 : x * 4 + (tile == 2 || tile == 4 ? 8 : 0);
                    float v = tile == 0 ? y * 8 : y * 4 + (tile == 3 || tile == 4 ? 8 : 0);
                    float size = tile == 0 ? 8 : 4;
                    parts[corner][tile] = bakeFace(bakery, face, x * 8, y * 8, x * 8 + 8, y * 8 + 8,
                            sprite, new float[]{u, v, u + size, v + size});
                }
            }
            for (int mask = 0; mask < 256; mask++) {
                faces[face.ordinal()][mask] = mask == 0
                        ? original.getQuads(null, face, RandomSource.create(0), ModelData.EMPTY, null)
                        : List.of(parts[0][tile(mask, 0)], parts[1][tile(mask, 1)],
                                  parts[2][tile(mask, 2)], parts[3][tile(mask, 3)]);
            }
        }
    }

    // Each corner depends on its two edges and the diagonal between them.
    static int tile(int mask, int corner) {
        boolean horizontal = (mask & (1 << ((corner & 1) == 0 ? 6 : 2))) != 0;
        boolean vertical = (mask & (1 << ((corner & 2) == 0 ? 0 : 4))) != 0;
        if (!horizontal && !vertical) return 0;
        if (!horizontal) return 2;
        if (!vertical) return 3;
        int diagonal = switch (corner) { case 0 -> 7; case 1 -> 1; case 2 -> 5; default -> 3; };
        return (mask & (1 << diagonal)) != 0 ? 1 : 4;
    }

    static Direction right(Direction face) {
        return switch (face) {
            case NORTH -> Direction.WEST;
            case SOUTH, UP, DOWN -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            case EAST -> Direction.NORTH;
        };
    }

    static Direction up(Direction face) {
        return switch (face) {
            case UP -> Direction.NORTH;
            case DOWN -> Direction.SOUTH;
            default -> Direction.UP;
        };
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
        long connections = 0;
        for (Direction face : Direction.values()) {
            int mask = 0;
            for (int i = 0; i < 8; i++) {
                BlockPos neighbor = pos.relative(right(face), DX[i]).relative(up(face), -DY[i]);
                // Facing is intentionally irrelevant: all casing orientations join one another.
                if (level.getBlockState(neighbor).is(state.getBlock())) mask |= 1 << i;
            }
            connections |= (long) mask << (face.ordinal() * 8);
        }
        return data.derive().with(CONNECTIONS, connections).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        Long connections = data.get(CONNECTIONS);
        if (side == null || connections == null) return originalModel.getQuads(state, side, random, data, renderType);
        return faces[side.ordinal()][(int) (connections >>> (side.ordinal() * 8)) & 255];
    }

    private static BakedQuad bakeFace(FaceBakery bakery, Direction face, float u0, float v0, float u1, float v1,
                                      TextureAtlasSprite sprite, float[] uv) {
        Vector3f a = point(face, u0, v0), b = point(face, u1, v1);
        Vector3f from = new Vector3f(a).min(b), to = new Vector3f(a).max(b);
        return bakery.bakeQuad(from, to, new BlockElementFace(face, -1, "", new BlockFaceUV(uv, 0)),
                sprite, face, BlockModelRotation.X0_Y0, null, true);
    }

    private static Vector3f point(Direction face, float u, float v) {
        return switch (face) {
            case NORTH -> new Vector3f(16 - u, 16 - v, 0);
            case SOUTH -> new Vector3f(u, 16 - v, 16);
            case WEST -> new Vector3f(0, 16 - v, u);
            case EAST -> new Vector3f(16, 16 - v, 16 - u);
            case UP -> new Vector3f(u, 16, v);
            case DOWN -> new Vector3f(u, 0, 16 - v);
        };
    }

    public static final class Geometry implements IUnbakedGeometry<Geometry> {
        @Override
        public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
                               Function<Material, TextureAtlasSprite> sprites, ModelState state, ItemOverrides overrides) {
            TextureAtlasSprite base = sprites.apply(context.getMaterial("all"));
            TextureAtlasSprite connected = sprites.apply(context.getMaterial("connected"));
            var builder = new SimpleBakedModel.Builder(context.useAmbientOcclusion(), context.useBlockLight(),
                    context.isGui3d(), context.getTransforms(), overrides).particle(base);
            var bakery = new FaceBakery();
            // Casing has no directional artwork; use world-aligned faces for every saved facing value.
            for (Direction face : Direction.values()) {
                builder.addCulledFace(face, bakeFace(bakery, face, 0, 0, 16, 16, base, new float[]{0, 0, 16, 16}));
            }
            var renderTypes = context.getRenderTypeHint() == null ? RenderTypeGroup.EMPTY
                    : context.getRenderType(context.getRenderTypeHint());
            return new FactoryCasingModel(builder.build(renderTypes), base, connected);
        }
    }
}
