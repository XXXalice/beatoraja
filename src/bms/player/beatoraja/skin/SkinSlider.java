package bms.player.beatoraja.skin;

import bms.player.beatoraja.MainState;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;

public class SkinSlider extends SkinObject {

	/**
	 * イメージ
	 */
	private TextureRegion[] image;
	private int cycle;

	private int timing;
	private int[] option = new int[3];

	private int muki;
	private int range = 100;
	private int type;

	public SkinSlider(TextureRegion[] image, int cycle, int muki, int range, int type) {
		this.image = image;
		this.cycle = cycle;
		this.muki = muki;
		this.range = range;
		this.type = type;
	}

	public TextureRegion[] getImage() {
		return image;
	}

	public TextureRegion getImage(long time) {
		if (cycle == 0) {
			return image[0];
		}
		final int index = (int) ((time / (cycle / image.length))) % image.length;
		// System.out.println(index + " / " + image.length);
		return image[index];
	}

	public void setImage(TextureRegion[] image, int cycle) {
		this.image = image;
		this.cycle = cycle;
	}

	public int getTiming() {
		return timing;
	}

	public void setTiming(int timing) {
		this.timing = timing;
	}

	public int[] getOption() {
		return option;
	}

	public void setOption(int[] option) {
		this.option = option;
	}

	public void draw(SpriteBatch sprite, long time, MainState state) {
		if (image == null) {
			return;
		}
		Rectangle r = this.getDestination(time,state);
		if (r != null) {
			TextureRegion image = getImage(time);
			draw(sprite, image, r.x
					+ (muki == 1 ? state.getSliderValue(type) * range : (muki == 3 ? -state.getSliderValue(type) * range : 0)), r.y
					+ (muki == 0 ? state.getSliderValue(type) * range : (muki == 2 ? -state.getSliderValue(type) * range : 0)),
					r.width, r.height, getColor(time));
		}
	}

	public void dispose() {
		if (image != null) {
			for (TextureRegion tr : image) {
				tr.getTexture().dispose();
			}
			image = null;
		}
	}
}