package com.snatik.matches.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.LinearLayout;

import com.snatik.matches.R;
import com.snatik.matches.common.Shared;
import com.snatik.matches.events.ui.FlipCardEvent;
import com.snatik.matches.model.Board;
import com.snatik.matches.model.BoardArrangment;

public class BoardView extends LinearLayout {

	private LinearLayout.LayoutParams mRowLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
	private LinearLayout.LayoutParams mTileLayoutParams;
	private int mScreenWidth;
	private int mScreenHeight;
	private Board mBoard;
	private BoardArrangment mBoardArrangment;
	private Map<Integer, TileView> mViewReference;
	private List<Integer> flippedUp = new ArrayList<Integer>();
	private boolean mLocked = false;

	public BoardView(Context context) {
		this(context, null);
	}

	public BoardView(Context context, AttributeSet attributeSet) {
		super(context, attributeSet);
		setOrientation(LinearLayout.VERTICAL);
		setGravity(Gravity.CENTER);
		mScreenHeight = getResources().getDisplayMetrics().heightPixels;
		mScreenWidth = getResources().getDisplayMetrics().widthPixels;
		mViewReference = new HashMap<Integer, TileView>();
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
	}

	public static BoardView fromXml(Context context, ViewGroup parent) {
		return (BoardView) LayoutInflater.from(context).inflate(R.layout.board_view, parent, false);
	}
	
	public void setBoard(Board board, BoardArrangment boardArrangment) {
		mBoard = board;
		mBoardArrangment = boardArrangment;
		// calc prefered tiles in width and height
		int singleMargin = getResources().getDimensionPixelSize(R.dimen.card_margin);
		float density = getResources().getDisplayMetrics().density;
		singleMargin = Math.max((int) (1 * density), (int) (singleMargin - board.difficulty * 2 * density));
		int sumMargin = 0;
		for (int row = 0; row < board.numRows; row++) {
			sumMargin += singleMargin * 2;
		}
		int tilesHeight = (mScreenHeight - sumMargin) / board.numRows;
		int tilesWidth = (mScreenWidth - sumMargin) / board.numTilesInRow;
		int size = Math.min(tilesHeight, tilesWidth);

		mTileLayoutParams = new LinearLayout.LayoutParams(size, size);
		mTileLayoutParams.setMargins(singleMargin, singleMargin, singleMargin, singleMargin);

		// build the ui
		buildBoard();
	}

	/**
	 * Build the board
	 */
	private void buildBoard() {

		for (int row = 0; row < mBoard.numRows; row++) {
			// add row
			addBoardRow(row);
		}

	}

	private void addBoardRow(int rowNum) {

		LinearLayout linearLayout = new LinearLayout(getContext());
		linearLayout.setOrientation(LinearLayout.HORIZONTAL);
		linearLayout.setGravity(Gravity.CENTER);

		for (int tile = 0; tile < mBoard.numTilesInRow; tile++) {
			addTile(rowNum * mBoard.numTilesInRow + tile, linearLayout);
		}

		// add to this view
		addView(linearLayout, mRowLayoutParams);
	}

	private void addTile(final int id, ViewGroup parent) {
		final TileView tileView = TileView.fromXml(getContext(), parent);
		tileView.setLayoutParams(mTileLayoutParams);
		parent.addView(tileView);
		mViewReference.put(id,  tileView);

		tileView.setTileImage(mBoardArrangment.getTileBitmap(id));
		tileView.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!mLocked && tileView.isFlippedDown()) {
					tileView.flipUp();
					flippedUp.add(id);
					if (flippedUp.size() == 2) {
						mLocked = true;
					}
					Shared.eventBus.notify(new FlipCardEvent(id));
				} 
			}
		});
		
		ObjectAnimator alphaAnimator = ObjectAnimator.ofFloat(tileView, "alpha", 0f, 1f);
		ObjectAnimator scaleXAnimator = ObjectAnimator.ofFloat(tileView, "scaleX", 0.8f, 1f);
		scaleXAnimator.setInterpolator(new BounceInterpolator());
		ObjectAnimator scaleYAnimator = ObjectAnimator.ofFloat(tileView, "scaleY", 0.8f, 1f);
		scaleYAnimator.setInterpolator(new BounceInterpolator());
		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playTogether(alphaAnimator, scaleXAnimator, scaleYAnimator);
		animatorSet.setDuration(500);
		animatorSet.start();
	}

	public void flipDownAll() {
		for (Integer id : flippedUp) {
			mViewReference.get(id).flipDown();
		}
		flippedUp.clear();
		mLocked = false; 
	}

	public void hideCards(int id1, int id2) {
		animateHide(mViewReference.get(id1));
		animateHide(mViewReference.get(id2));
		flippedUp.clear();
		mLocked = false;
	}

	protected void animateHide(final TileView v) {
		ObjectAnimator animator = ObjectAnimator.ofFloat(v, "alpha", 0f);
		animator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				v.setLayerType(View.LAYER_TYPE_NONE, null);
				v.setVisibility(View.INVISIBLE);
			}
		});
		animator.setDuration(100);
		v.setLayerType(View.LAYER_TYPE_HARDWARE, null);
		animator.start();
	}
	
}