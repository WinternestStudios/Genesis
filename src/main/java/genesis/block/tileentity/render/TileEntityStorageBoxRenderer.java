package genesis.block.tileentity.render;

import org.lwjgl.opengl.GL11;

import genesis.block.tileentity.BlockStorageBox;
import genesis.block.tileentity.TileEntityStorageBox;
import genesis.util.*;
import genesis.util.render.*;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class TileEntityStorageBoxRenderer extends TileEntitySpecialRenderer<TileEntityStorageBox>
{
	public static class ModelStorageBox extends ModelBase
	{
		public BlockAsEntityPart lid = new BlockAsEntityPart(this);
		
		public ModelStorageBox()
		{
			lid.setAmbientOcclusion(false);
			lid.setDefaultState();
		}

		public void renderAll()
		{
			lid.render(0.0625F);
		}
	}
	
	public static final ResourceLocation LID = new ResourceLocation(Constants.MOD_ID, "storage_box_lid");
	
	ModelStorageBox model = new ModelStorageBox();
	
	public TileEntityStorageBoxRenderer(final BlockStorageBox block)
	{
		ModelHelpers.forceModelLoading(block, LID);
	}
	
	@Override
	public void renderTileEntityAt(TileEntityStorageBox box, double x, double y, double z, float partialTick, int destroyStage)
	{
		// Get data about the block in the world.
		World world = box.getWorld();
		BlockPos pos = box.getPos();
		IBlockState state = box.getBlockType().getActualState(world.getBlockState(pos), world, pos);
		
		float px = 0.0625F;
		
		// Get animation values.
		final float rotateTime = 0.6F;
		final float stopTime = -0.15F;
		final float translateTime = 1 - rotateTime - stopTime;
		
		float open = box.getOpenAnimation(partialTick);
		float rotateAmt = MathHelper.clamp_float(open / rotateTime, 0, 1);
		float translateAmt = MathHelper.clamp_float((open - (1 - translateTime)) / translateTime, 0, 1);
		translateAmt = (float) Math.pow(translateAmt, 1.5);
		
		// Reset model state.
		model.lid.resetState();
		
		// Set model state before rendering.
		String props = ModelHelpers.getPropertyString(state);
		model.lid.setModel(ModelHelpers.getLocationWithProperties(LID, props), world, pos);
		
		EnumFacing openDirection = box.getOpenDirection();
		
		float pointY = 14 * px;
		float pointHoriz = 7 * px;
		
		float rotation = rotateAmt * 90;
		float translation = translateAmt * 13 * px;
		
		model.lid.rotationPointY += pointY;
		model.lid.rotationPointX += pointHoriz * openDirection.getFrontOffsetX();
		model.lid.rotationPointZ += pointHoriz * openDirection.getFrontOffsetZ();
		
		model.lid.offsetY -= translation;
		
		switch (openDirection)
		{
		case NORTH:
			rotation *= -1;
		case SOUTH:
			model.lid.rotateAngleX += rotation;
			break;
		case EAST:
			rotation *= -1;
		case WEST:
			model.lid.rotateAngleZ += rotation;
			break;
		default:
			break;
		}
		
		// Translate to the proper coordinates.
		GlStateManager.pushMatrix();
		GlStateManager.translate(x, y, z);
		
		ModelHelpers.bindAtlasTexture();
		
		// If we're rendering a destroy stage, set the breaking texture.
		if (destroyStage >= 0)
		{
			Tessellator tess = Tessellator.getInstance();
			VertexBuffer buff = tess.getBuffer();
			
			RenderHelper.disableStandardItemLighting();
			Minecraft.getMinecraft().entityRenderer.disableLightmap();
			
			TextureAtlasSprite breakTexture = ModelHelpers.getDestroyBlockIcon(destroyStage);
			
			buff.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
			buff.noColor();
			buff.setTranslation(-pos.getX(), -pos.getY(), -pos.getZ());
			ModelHelpers.getBlockRenderer().renderModelFlat(world,
					ModelHelpers.getCubeProjectedBakedModel(state, ModelHelpers.getBakedBlockModel(state, world, pos), breakTexture, pos),
					state, pos, buff, true, MathHelper.getPositionRandom(pos));
			buff.setTranslation(0, 0, 0);
			tess.draw();
			
			model.lid.setTextureNoColor(breakTexture);
		}
		
		// Render model.
		model.renderAll();
		
		if (destroyStage >= 0)
		{
			RenderHelper.enableStandardItemLighting();
			Minecraft.getMinecraft().entityRenderer.enableLightmap();
		}
		
		// Pop the matrix to clear all our transforms in here.
		GlStateManager.popMatrix();
	}
}
