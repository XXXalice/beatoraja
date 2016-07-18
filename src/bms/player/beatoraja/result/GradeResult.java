package bms.player.beatoraja.result;

import java.util.*;

import bms.player.beatoraja.select.MusicSelector;
import com.badlogic.gdx.math.Rectangle;
import java.util.logging.Logger;

import bms.player.beatoraja.skin.SkinImage;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

import bms.model.BMSModel;
import bms.player.beatoraja.*;
import bms.player.beatoraja.gauge.GrooveGauge;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class GradeResult extends MainState {

	private static final String[] LAMP = { "000000", "808080", "800080", "ff00ff", "40ff40", "f0c000", "ffffff",
			"ffff88", "88ffff", "ff8888", "ff0000" };
	private static final String[] CLEAR = { "NO PLAY", "FAILED", "ASSIST CLEAR", "L-ASSIST CLEAR", "EASY CLEAR",
			"CLEAR", "HARD CLEAR", "EX-HARD CLEAR", "FULL COMBO", "PERFECT", "MAX" };

	private MainController main;

	private BitmapFont titlefont;
	private String title;

	private PlayerResource resource;

	private int oldclear;
	private int oldexscore;
	private int oldmisscount;
	private int oldcombo;

	private MusicResultSkin skin;

	private boolean saveReplay = false;

	private GaugeGraphRenderer gaugegraph;

	public GradeResult(MainController main) {
		this.main = main;
	}

	public void create(PlayerResource resource) {
		this.resource = resource;
		skin = new MusicResultSkin(main.RESOLUTION[resource.getConfig().getResolution()]);
		this.setSkin(skin);
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("skin/VL-Gothic-Regular.ttf"));
		FreeTypeFontParameter parameter = new FreeTypeFontParameter();
		parameter.size = 24;
		title = "result";
		parameter.characters = title + parameter.characters + "段位認定 " + resource.getCoursetitle() + "不合格";
		titlefont = generator.generateFont(parameter);
		updateScoreDatabase();

		if (resource.getAutoplay() == 0 && resource.getCourseScoreData() != null
				&& resource.getCourseScoreData().getClear() >= GrooveGauge.CLEARTYPE_EASY
				&& !main.getPlayDataAccessor().existsReplayData(resource.getCourseBMSModels(), resource.getConfig().getLnmode(), 0, resource.getConstraint())) {
			saveReplayData(0);
		}

		gaugegraph = new GaugeGraphRenderer();
	}

	public void render() {
		int time = getNowTime();
		final SpriteBatch sprite = main.getSpriteBatch();
		final ShapeRenderer shape = main.getShapeRenderer();
		IRScoreData score = resource.getCourseScoreData();

		if (score != null) {
			if (score.getClear() > GrooveGauge.CLEARTYPE_FAILED) {
				Gdx.gl.glClearColor(0, 0, 0.4f, 1);
			} else {
				Gdx.gl.glClearColor(0.4f, 0, 0, 1);
			}
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

			final float w = 1280;
			final float h = 720;

			// ゲージグラフ描画
			final List<List<Float>> coursegauge = resource.getCourseGauge();
			final int cg = resource.getCourseBMSModels().length;
			for (int i = 0; i < cg; i++) {
				Rectangle graph = new Rectangle(40 + i * (1200 / cg), 500, 1200 / cg, 200);
				if (coursegauge.size() <= i) {
					shape.begin(ShapeRenderer.ShapeType.Filled);
					shape.setColor(Color.DARK_GRAY);
					shape.rect(graph.x, graph.y, graph.width, graph.height);
					shape.end();
					Gdx.gl.glLineWidth(4);
					shape.begin(ShapeRenderer.ShapeType.Line);
					shape.setColor(Color.WHITE);
					shape.rect(graph.x, graph.y, graph.width, graph.height);
					shape.end();
					Gdx.gl.glLineWidth(1);
				} else {
					gaugegraph.render(shape, time, resource, graph, coursegauge.get(i));
				}
			}

			sprite.begin();
			titlefont.setColor(Color.WHITE);
			titlefont.draw(sprite, resource.getCoursetitle()
					+ (score.getClear() > GrooveGauge.CLEARTYPE_FAILED ? "  合格" : "  不合格"), w * 3 / 4, h / 2);
			if (saveReplay) {
				titlefont.draw(sprite, "Replay Saved", w * 3 / 4, h / 4);
			}

			titlefont.draw(sprite, "FAST / SLOW  :  ", 100, 100);
			sprite.end();
		}
		boolean[] keystate = main.getInputProcessor().getKeystate();
		long[] keytime = main.getInputProcessor().getTime();
		if (score == null
				|| ((System.currentTimeMillis() > time + 500 && (keystate[0] || keystate[2] || keystate[4] || keystate[6])))) {
			keytime[0] = keytime[2] = keytime[4] = keytime[6] = 0;
			main.changeState(MainController.STATE_SELECTMUSIC);
		}

		for(int i = 0;i < MusicSelector.REPLAY;i++) {
			if (resource.getAutoplay() == 0 && main.getInputProcessor().getNumberState()[i + 1]) {
				saveReplayData(i);
				break;
			}
		}
	}

	public void updateScoreDatabase() {
		saveReplay = false;
		BMSModel[] models = resource.getCourseBMSModels();
		IRScoreData newscore = resource.getCourseScoreData();
		if (newscore == null) {
			return;
		}
		boolean ln = false;
		boolean dp = false;
		for (BMSModel model : models) {
			ln |= model.getTotalNotes(BMSModel.TOTALNOTES_LONG_KEY)
					+ model.getTotalNotes(BMSModel.TOTALNOTES_LONG_SCRATCH) > 0;
			dp |= (model.getUseKeys() == 10 || model.getUseKeys() == 14);
		}
		newscore.setCombo(resource.getMaxcombo());
		int random = 0;
		if (resource.getConfig().getRandom() > 0 || (dp && (resource.getConfig().getRandom2() > 0
				|| resource.getConfig().getDoubleoption() > 0))) {
			random = 2;
		}
		if (resource.getConfig().getRandom() == 1 && (!dp || (resource.getConfig().getRandom2() == 1
				&& resource.getConfig().getDoubleoption() == 1))) {
			random = 1;
		}
		IRScoreData score = main.getPlayDataAccessor().readScoreData(models, resource.getConfig().getLnmode(), random, resource.getConstraint());
		if (score == null) {
			score = new IRScoreData();
		}
		oldclear = score.getClear();
		oldexscore = score.getExscore();
		oldmisscount = score.getMinbp();
		oldcombo = score.getCombo();

		main.getPlayDataAccessor().writeScoreDara(newscore, models, resource.getConfig().getLnmode(), random,resource.getConstraint(),
				resource.isUpdateScore());

		Logger.getGlobal().info("スコアデータベース更新完了 ");
	}

	public int getJudgeCount(int judge, boolean fast) {
		IRScoreData score = resource.getCourseScoreData();
		if (score != null) {
			switch (judge) {
			case 0:
				return fast ? score.getEpg() : score.getLpg();
			case 1:
				return fast ? score.getEgr() : score.getLgr();
			case 2:
				return fast ? score.getEgd() : score.getLgd();
			case 3:
				return fast ? score.getEbd() : score.getLbd();
			case 4:
				return fast ? score.getEpr() : score.getLpr();
			case 5:
				return fast ? score.getEms() : score.getLms();
			}
		}
		return 0;
	}

	@Override
	public int getClear() {
		if(resource.getCourseScoreData() != null) {
			return resource.getCourseScoreData().getClear();
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getTargetClear() {
		return oldclear;
	}

	@Override
	public int getScore() {
		if (resource.getCourseScoreData() != null) {
			return resource.getCourseScoreData().getExscore();
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getTargetScore() {
		return oldexscore;
	}

	@Override
	public int getMaxcombo() {
		if (resource.getCourseScoreData() != null) {
			return resource.getCourseScoreData().getCombo();
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getTargetMaxcombo() {
		if (oldcombo > 0) {
			return oldcombo;
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getMisscount() {
		if (resource.getCourseScoreData() != null) {
			return resource.getCourseScoreData().getMinbp();
		}
		return Integer.MIN_VALUE;
	}

	@Override
	public int getTargetMisscount() {
		return oldmisscount;
	}

	public int getTotalNotes() {
		int notes = 0;
		for(BMSModel model : resource.getCourseBMSModels()) {
			notes += model.getTotalNotes();
		}
		return notes;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	private void saveReplayData(int index) {
		if (resource.getCourseScoreData() != null) {
			if (!saveReplay && resource.isUpdateScore()) {
				// 保存されているリプレイデータがない場合は、EASY以上で自動保存
				ReplayData[] rd = resource.getCourseReplay();
				main.getPlayDataAccessor().wrireReplayData(rd, resource.getCourseBMSModels(),
						resource.getConfig().getLnmode(), index, resource.getConstraint());
				saveReplay = true;
			}
		}
	}
}
