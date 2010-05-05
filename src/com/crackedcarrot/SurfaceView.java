package com.crackedcarrot;

import com.crackedcarrot.UI.UIHandler;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

public class SurfaceView extends GLSurfaceView {
	
	public GameLoop gameLoop = null;

		// Towertype to build, set by the GUI.
	public int towerType = 0;
	
		// Not very magic, read the comment below for explanation.
	private int magicValue;
	private boolean buildTower = false;
	private UIHandler ui;
	
	@Override
	public boolean onTouchEvent(MotionEvent me) {
		int action = me.getAction();
		int x = (int)me.getX();
		int y = magicValue - (int)me.getY();
		boolean test = false;
		//Log.d("SURFACEVIEW", "onTouchEvent: X " + me.getX() + "  Y " + me.getY());
		
		// We need to do this because Java and our grid counts backwards.
		// 480 - clickedYValue = the correct Y-value, for example, on a 
		// screen with a 480 Y-resolution.
		// we build where the user last touched the screen.
		
		if (action == MotionEvent.ACTION_UP) {

			if(buildTower){
				test = gameLoop.createTower(new Coords(x, y), towerType);
			}
			
			if(ui != null && gameLoop.gridOcupied(x, y) && (me.getEventTime() - me.getDownTime()) < 200){
				
				int[] data = gameLoop.getTowerCoordsAndRange(x, y);
				if(data != null){
					ui.showRangeIndicator(data[0], data[1], data[2], data[3], data[4]);
					test = true;
				}
				else{
					Log.d("SURFACEVIEW","Guru Meditation: Cant get towerdata");
				}
			} if (buildTower &&	!test && ui != null) {
				
				//You are not allowed to place tower here
				ui.blinkRedGrid();
			}
			
			else Log.d("SURFACEVIEW", "The edge of the map, here be dragons! Or maybe road or a snowman, maybe a bush to =)");
			
			return false;
		}
		
		return true;
	}

	public SurfaceView(Context context) {
		super(context);
	}
	
	public SurfaceView(Context context, AttributeSet attrs){
		super(context, attrs);
	}

	public void setScreenHeight(int i) {
		this.magicValue = i;
	}
	
	public void setSimulationRuntime(GameLoop simulationRuntime) {
		this.gameLoop = simulationRuntime;
	}
	
	public void setTowerType(int i) {
		if (i == -1) {
			this.buildTower = false;
		}
		else 
			this.buildTower = true;
		this.towerType = i;
	}

	public void setHUDHandler(UIHandler hudHandler) {
		ui = hudHandler;
	}
}
