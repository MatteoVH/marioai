package competition.cig.robinbaumgarten.astar;

import ch.idsia.mario.engine.sprites.Mario;
import competition.cig.robinbaumgarten.astar.level.Level;
import java.lang.Math;

import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.Math.random;
import static java.lang.Math.sqrt;


public class NNSimulator {

	public LevelScene levelScene;
	public LevelScene workScene;
	public SearchNode bestPosition;
	public SearchNode furthestPosition;
	float currentSearchStartingMarioXPos;
	ArrayList<SearchNode> posPool;
	ArrayList<int[]> visitedStates = new ArrayList<int[]>();
	private float maxMarioSpeed = 10.9090909f;
	private boolean requireReplanning = false;
	public int debugPos = 0;

	public int targetX = 100;
	public int targetY = 100;
	public int timeBudget = 20; // ms
	public static final int visitedListPenalty = 1500;



	/* SEBAS CODE */

	// centroids for game states derived from SOM

	private static double[][] centroids = {



	};

	// contains values for state action relationships
	private static float[][] qtable;

	// contains a list of recent actions, used to modify qtable
	private static ArrayList<Integer[]> recent_actions;

	// learning constant
	private static float gamma;

	// translates col in q table to actual button presses
	static boolean[][] possible_actions = {
			// move lefts
			{true, false, false, false, false},
			{true, false, false, false, true},
			{true, false, false, true, false},
			{true, false, false, true, true},

			// move rights
			{false, true, false, false, false},
			{false, true, false, false, true},
			{false, true, false, true, false},
			{false, true, false, true, true},

			// move not
			{false, false, false, false, false},
			{false, false, false, false, true},
			{false, false, false, true, false},
			{false, false, false, true, true},
	};

	// static variable used to keep tabs of current move at all times, updated when requested
	static boolean[] action = {false, false, false, false, false};
	// 1. left
	// 2. right
	// 3. down
	// 4. jump
	// 5. speed

	// passed current game snapshot as argument,
	// compares against centroids to determine matching state
	public static int determineState(double[] game_snap)
	{
		double min_dist = -1;
		int best_state  = -1;

		for (int i = 0; i < centroids.length; i++)
		{
			double running_sum = 0;
			for (int j = 0; j < 305; j++)
			{
				running_sum += Math.pow((game_snap[j] - centroids[i][j]), 2);
			}
			running_sum = sqrt(running_sum);
			if (min_dist == -1)
			{
				min_dist   = running_sum;
				best_state = i;
			}
			else if (running_sum < min_dist)
			{
				min_dist   = running_sum;
				best_state = i;
			}
		}
		return best_state;
	}

	/* SEBAS CODE */



    //private int visitedClashes = 0;
    //private LevelScene lsCopy;

    //private int searchResolution = 10;

    private ArrayList<boolean[]> currentActionPlan;
    int ticksBeforeReplanning = 0;


	private class SearchNode
	{
		private int timeElapsed = 0;
		public float remainingTimeEstimated = 0;
		private float remainingTime = 0;

		public SearchNode parentPos = null;
		public SearchNode chosenChild = null;
		public LevelScene sceneSnapshot = null;
		public int distanceFromOrigin = 0;
		public boolean hasBeenHurt = false;
		public boolean isInVisitedList = false;

		boolean[] action;
		int repetitions = 1;


		public float calcRemainingTime(float marioX, float marioXA)
		{
			return (100000 - (maxForwardMovement(marioXA, 1000) + marioX))
				/ maxMarioSpeed - 1000;
		}

		public float getRemainingTime()
		{
			if (remainingTime > 0)
				return remainingTime;
			else
				return remainingTimeEstimated;
		}

		public float estimateRemainingTimeChild(boolean[] action, int repetitions)
		{
			float[] childbehaviorDistanceAndSpeed = estimateMaximumForwardMovement(
					levelScene.mario.xa, action, repetitions);
			return calcRemainingTime(levelScene.mario.x + childbehaviorDistanceAndSpeed[0],
					childbehaviorDistanceAndSpeed[1]);
		}

		public SearchNode(boolean[] action, int repetitions, SearchNode parent)
		{
	    	this.parentPos = parent;
	    	if (parent != null)
	    	{
	    		this.remainingTimeEstimated = parent.estimateRemainingTimeChild(action, repetitions);
	    		this.distanceFromOrigin = parent.distanceFromOrigin + 1;
	    	}
	    	else
	    		this.remainingTimeEstimated = calcRemainingTime(levelScene.mario.x, 0);
	    	this.action = action;
	    	this.repetitions = repetitions;
	    	if (parent != null)
	    		timeElapsed = parent.timeElapsed + repetitions;
	    	else
	    		timeElapsed = 0;
		}

		public float simulatePos()
		{
	    	// set state to parents scene
			if (parentPos.sceneSnapshot == null)
			{
				System.out.println("DAMN! NO SNAPSHOT!");
			}
			levelScene = parentPos.sceneSnapshot;
			parentPos.sceneSnapshot = backupState();

			int initialDamage = getMarioDamage();
	    	for (int i = 0; i < repetitions; i++)
	    	{
	    		/*if (debugPos < 1000)
	    		{
	    			GlobalOptions.Pos[debugPos][0] = (int) levelScene.mario.x;
	    			GlobalOptions.Pos[debugPos][1] = (int) levelScene.mario.y;
	    			debugPos++;
	    		}*/
	    		advanceStep(action);
	    		/*if (debugPos < 1000)
	    		{
	    			GlobalOptions.Pos[debugPos][0] = (int) levelScene.mario.x;
	    			GlobalOptions.Pos[debugPos][1] = (int) levelScene.mario.y;
	    			debugPos++;
	    		}*/
	    		if (debugPos > 1000)
	    			debugPos = 0;
	    	}
	    	remainingTime = calcRemainingTime(levelScene.mario.x, levelScene.mario.xa)
	    	 	+ (getMarioDamage() - initialDamage) * (1000000 - 100 * distanceFromOrigin);
	    	if (isInVisitedList)
	    		remainingTime += visitedListPenalty;
	    	hasBeenHurt = (getMarioDamage() - initialDamage) != 0;
	    	sceneSnapshot = backupState();

	    	return remainingTime;
		}

		public ArrayList<SearchNode> generateChildren()
		{
			ArrayList<SearchNode> list = new ArrayList<SearchNode>();
			ArrayList<boolean[]> possibleActions = createPossibleActions(this);

			for (boolean[] action: possibleActions)
			{
				list.add(new SearchNode(action, repetitions, this));
			}
			return list;
		}

	}



	/* SEBAS CODE */

    public NNSimulator()
    {
    	initialiseSimulator();

		// initialize arraylist for qlearning actions
		recent_actions = new ArrayList<Integer[]>();

		// initialize qtable to have values between 0 and 100
		qtable = new float[centroids.length][12];
		for (int i = 0; i < centroids.length; i++)
		{
			for (int j = 0; j < 12; j++)
			{
				qtable[i][j] = (float)(random()*1000);
			}
		}
    }
    
    /* SEBAS CODE */



    public boolean canJumpHigher(SearchNode currentPos, boolean checkParent)
    {
    	if (currentPos.parentPos != null && checkParent
    			&& canJumpHigher(currentPos.parentPos, false))
    			return true;
    	return currentPos.sceneSnapshot.mario.mayJump() || (currentPos.sceneSnapshot.mario.jumpTime > 0);
    }
    
    private ArrayList<boolean[]> createPossibleActions(SearchNode currentPos)
    {
    	ArrayList<boolean[]> possibleActions = new ArrayList<boolean[]>();
    	// do nothing
    	//possibleActions.add(createAction(false, false, false, false, false));

    	//possibleActions.add(createAction(false, false, true, false, false));
    	// jump
    	if (canJumpHigher(currentPos, true)) possibleActions.add(createAction(false, false, false, true, false));
    	if (canJumpHigher(currentPos, true)) possibleActions.add(createAction(false, false, false, true, true));
    	
    	// run right
    	possibleActions.add(createAction(false, true, false, false, true));
    	if (canJumpHigher(currentPos, true))  possibleActions.add(createAction(false, true, false, true, true));
    	possibleActions.add(createAction(false, true, false, false, false));
    	if (canJumpHigher(currentPos, true))  possibleActions.add(createAction(false, true, false, true, false));
 	
    	// run left
    	possibleActions.add(createAction(true, false, false, false, false));
    	if (canJumpHigher(currentPos, true))  possibleActions.add(createAction(true, false, false, true, false));
    	possibleActions.add(createAction(true, false, false, false, true));
    	if (canJumpHigher(currentPos, true))  possibleActions.add(createAction(true, false, false, true, true));
  	
    	
    	// duck (probably sucks always)
    	
    	return possibleActions;
    }
    
    private boolean[] createAction(boolean left, boolean right, boolean down, boolean jump, boolean speed)
    {
    	boolean[] action = new boolean[5];
    	action[Mario.KEY_DOWN] = down;
    	action[Mario.KEY_JUMP] = jump;
    	action[Mario.KEY_LEFT] = left;
    	action[Mario.KEY_RIGHT] = right;
    	action[Mario.KEY_SPEED] = speed;
    	return action;
    }
    
    public float[] estimateMaximumForwardMovement(float currentAccel, boolean[] action, int ticks)
    {
    	float dist = 0;
    	float runningSpeed =  action[Mario.KEY_SPEED] ? 1.2f : 0.6f;
    	int dir = 0;
    	if (action[Mario.KEY_LEFT]) dir = -1;
    	if (action[Mario.KEY_RIGHT]) dir = 1;
    	for (int i = 0; i < ticks; i++)
    	{
    		currentAccel += runningSpeed * dir;
    		dist += currentAccel;
    		//System.out.println("Estimator of Fastforward Speed, Tick "+i+" speed: "+currentAccel);
    		currentAccel *= 0.89f;
    	}    	
    	float[] ret = new float[2];
    	ret[0] = dist;
    	ret[1] = currentAccel;
    	return ret;
    }
    
    // distance covered at maximum acceleration with initialSpeed for ticks timesteps 
    private float maxForwardMovement(float initialSpeed, int ticks)
    {
    	float y = ticks;
    	float s0 = initialSpeed;
    	return (float) (99.17355373 * Math.pow(0.89,y+1)
    	  -9.090909091*s0*Math.pow(0.89,y+1)
    	  +10.90909091*y-88.26446282+9.090909091*s0);
    }
    
    private int getMarioDamage()
    {
    	// early damage at gaps: Don't even fall 1 px into them.
    	if (levelScene.level.isGap[(int) (levelScene.mario.x/16)] &&
    			levelScene.mario.y > levelScene.level.gapHeight[(int) (levelScene.mario.x/16)]*16)
    	{
    		//System.out.println("Gap height: "+levelScene.level.gapHeight[(int) (levelScene.mario.x/16)]);
    		levelScene.mario.damage+=5;
    	}
    	return levelScene.mario.damage;
    }
    

    
    private void search(long startTime)
    {
    	SearchNode current = bestPosition;
    	//SearchNode furthest = bestPosition;
    	boolean currentGood = false;
    	int ticks = 0;
    	int maxRight = 176;
    	while(posPool.size() != 0 
    			//&& ((levelScene.mario.x - currentSearchStartingMarioXPos < maxRight) || !currentGood) 
    			&& ((bestPosition.sceneSnapshot.mario.x - currentSearchStartingMarioXPos < maxRight) || !currentGood) 
    			//&& ((System.currentTimeMillis() - startTime < 35) || (ticks < 200)))
    			&& (System.currentTimeMillis() - startTime < Math.min(200,timeBudget/2)))
    			//&& ticks < 200)
    	{
    		ticks++;
    		current = pickBestPos(posPool);
    		currentGood = false;
    		float realRemainingTime = current.simulatePos();
    		
    		//System.out.println("Simulated mariopos: "+ current.sceneSnapshot.mario.x + " " + current.sceneSnapshot.mario.y);
    		if (realRemainingTime < 0)
    		{
    			System.out.print("-");
    			continue;
    		}
    		else if  (!current.isInVisitedList 
    				&& isInVisited((int) current.sceneSnapshot.mario.x, (int) current.sceneSnapshot.mario.y, current.timeElapsed))
	   		{
    			if (levelScene.verbose > 12) System.out.print("V");
    			realRemainingTime += visitedListPenalty;
    			current.isInVisitedList = true;
    			current.remainingTime = realRemainingTime;
    			current.remainingTimeEstimated = realRemainingTime;
	   			/*current.pathQuality -= 100;
	   			realRemainingDist -= 100;
	   			current.remainingDistanceEstimated = realRemainingDist;
    			current.remainingDistance = realRemainingDist;*/
    			posPool.add(current); 
	   		}
    		else if (realRemainingTime - current.remainingTimeEstimated > 0.1)
    		{
    			if (levelScene.verbose > 12) System.out.print("b");
    			// current item is not as good as anticipated. put it back in pool and look for best again
    			current.remainingTimeEstimated = realRemainingTime;
    			posPool.add(current);
    		}
    		else
    		{

    			if (levelScene.verbose > 12) System.out.print("a");
    			//System.out.println("Simulated bestPos accepted! Est remaining time: "
    			//		+current.remainingTimeEstimated+" real: "+ realRemainingTime);
    			currentGood = true;
    			visited((int) current.sceneSnapshot.mario.x, (int) current.sceneSnapshot.mario.y, current.timeElapsed);
    			
    			posPool.addAll(current.generateChildren());    			
    		}
    		if (currentGood) 
    		{
    			bestPosition = current;
    			if (current.sceneSnapshot.mario.x > furthestPosition.sceneSnapshot.mario.x
    					&& !levelScene.level.isGap[(int)(current.sceneSnapshot.mario.x/16)])
    					//&& current.sceneSnapshot.mario.isOnGround())
    				furthestPosition = current;
    		}
    	}
    	if (levelScene.mario.x - currentSearchStartingMarioXPos < maxRight
    			&& furthestPosition.sceneSnapshot.mario.x > bestPosition.sceneSnapshot.mario.x + 20
    			&& (levelScene.mario.fire ||
    					levelScene.level.isGap[(int)(bestPosition.sceneSnapshot.mario.x/16)]))
    	{
    		// Couldnt plan till end of screen, take furthest
    		//System.out.println("Furthest: "+ furthestPosition.sceneSnapshot.mario.x + " best: "+ bestPosition.sceneSnapshot.mario.x);
    		bestPosition = furthestPosition;
    	}
    	
    	//bestPosition = current;
    	if (levelScene.verbose > 1) System.out.println("Search stopped. Remaining pool size: "+ posPool.size() + " Current remaining time: " + current.remainingTime);

    	levelScene = current.sceneSnapshot;
    }
    
    private void startSearch(int repetitions)
    {    	
    	if (levelScene.verbose > 1) System.out.println("Started search.");
    	SearchNode startPos = new SearchNode(null, repetitions, null);
    	startPos.sceneSnapshot = backupState();
    	
    	posPool = new ArrayList<SearchNode>();
    	//visitedStates = new ArrayList<int[]>();
    	visitedStates.clear();
    	posPool.addAll(startPos.generateChildren());
    	currentSearchStartingMarioXPos = levelScene.mario.x; 
    	
    	/*
    	if (bestPosition != null)
    	{
    		LevelScene internal = backupState();
    		// insert old plan
    		SearchNode pos = bestPosition;
    		// rewind
    		int steps = 0;
    		while (pos.parentPos != null)
    		{
    			steps++;
    			pos = pos.parentPos;
    		}
    		if (steps > 3)
    		{
	    		pos = pos.chosenChild.chosenChild.chosenChild;
	    		// go forward
	    		if (pos != null && pos.parentPos != null)
	    		{
		    		pos.parentPos.sceneSnapshot = backupState(); // overwrite previous scenesnapshot
		    		//float previousRemainingTime = pos.remainingTime;
		    		while(pos != null && pos.remainingTime == pos.simulatePos())
		    		{
		    			// same conditions
		    			SearchNode newP = new SearchNode(pos.action, pos.repetitions, pos.parentPos);
		    	    	newP.sceneSnapshot = pos.sceneSnapshot;
		    	    	newP.remainingTime = pos.remainingTime;
		    	    	newP.remainingTimeEstimated = pos.remainingTimeEstimated;
		    	    	posPool.add(newP);
		    	    	//posPool.addAll(newP.generateChildren());
		    	    	pos = pos.chosenChild;
		    	    	
		    	    }
	    		}
	    		restoreState(internal);
    		}
    	}
    	
    	
		for(int i = 0; i < 1000; i++)
		{
			GlobalOptions.Pos[i][0] = 0;
			GlobalOptions.Pos[i][1] = 0;
		}*/
    	debugPos = 0;
    	bestPosition = startPos;
    	furthestPosition = startPos;
    	
    }
    
    private ArrayList<boolean[]> extractPlan()
    {
    	ArrayList<boolean[]> actions = new ArrayList<boolean[]>();
    	
    	// just move forward if no best position exists
    	if (bestPosition == null)
    	{
    		if (levelScene.verbose > 1) System.out.println("NO BESTPOS!");
    		for (int i = 0; i < 10; i++)
    		{
    			actions.add(createAction(false, true, false, false, true));        		
    		}
    		return actions;
    	}
    	if (levelScene.verbose > 2) System.out.print("Extracting plan (reverse order): ");
    	SearchNode current = bestPosition;
    	while (current.parentPos != null)
    	{
    		for (int i = 0; i < current.repetitions; i++)
    			actions.add(0, current.action);
    		if (levelScene.verbose > 2) 
    			System.out.print("[" 
    				+ (current.action[Mario.KEY_DOWN] ? "d" : "") 
    				+ (current.action[Mario.KEY_RIGHT] ? "r" : "")
    				+ (current.action[Mario.KEY_LEFT] ? "l" : "")
    				+ (current.action[Mario.KEY_JUMP] ? "j" : "")
    				+ (current.action[Mario.KEY_SPEED] ? "s" : "") 
    				+ (current.hasBeenHurt ? "-" : "") + "]");
    		if (current.hasBeenHurt)
    		{
    			requireReplanning = true;    			
    		}
    		if (current.parentPos != null)
    			current.parentPos.chosenChild =current;
    		current = current.parentPos;
    	}
    	if (levelScene.verbose > 2) System.out.println();
		return actions;
    }
    
    public String printAction(boolean[] action)
    {
    	String s = "";
    	if (action[Mario.KEY_RIGHT]) s+= "Forward ";
    	if (action[Mario.KEY_LEFT]) s+= "Backward ";
    	if (action[Mario.KEY_SPEED]) s+= "Speed ";
    	if (action[Mario.KEY_JUMP]) s+= "Jump ";
    	if (action[Mario.KEY_DOWN]) s+= "Duck";
    	return s;
    }
    
    private SearchNode pickBestPos(ArrayList<SearchNode> posPool)
    {
    	SearchNode bestPos = null;
    	float bestPosCost = 10000000;
    	//System.out.println("Searching fitnesses.");
    	for (SearchNode current: posPool)
    	{
    		float jumpModifier = 0;    		
    		//if (current.action[Mario.KEY_JUMP]) jumpModifier = -0.0001f;
    		if (current.sceneSnapshot != null)
    		{
    			int marioX = (int) current.sceneSnapshot.mario.x / 16;
    			if (current.sceneSnapshot.level.isGap.length > marioX && current.sceneSnapshot.level.isGap[marioX])
    			{
    				//if (current.action[Mario.KEY_JUMP])
    				//	jumpModifier -= 5f;
    				//if (current.action[Mario.KEY_RIGHT])
        			//	jumpModifier -= 0.5f;
    				//if (current.action[Mario.KEY_SPEED])
        			//	jumpModifier -= 5f;
    			}
    		}
    		
    		//if (current.sceneSnapshot != null && current.sceneSnapshot.mario.y > 200) jumpModifier += 0.001f * (300-current.sceneSnapshot.mario.y);
    		float currentCost = current.getRemainingTime()
    			+ current.timeElapsed * 0.90f + jumpModifier; // slightly bias towards furthest positions 
    		//System.out.println("Looking at pos with elapsed time "+current.timeElapsed+" est time: "
    		//		+ current.getRemainingTime() + " actions: " + printAction(current.action));
    		if (currentCost < bestPosCost)
    		{
    			bestPos = current;
    			bestPosCost = currentCost;
    		}
    	}
    	posPool.remove(bestPos);
    	//System.out.println("Best Pos: elapsed time "+bestPos.timeElapsed+" est time: "
    	//			+ bestPos.getRemainingTime() + " actions: " + printAction(bestPos.action));
    	return bestPos;
    }
        
	public void initialiseSimulator()
	{
		levelScene = new LevelScene();
		levelScene.init();	
		levelScene.level = new Level(1500,15);
	}
	
	public void setLevelPart(byte[][] levelPart, float[] enemies)
	{
    	if (levelScene.setLevelScene(levelPart))
    	{
    		requireReplanning = true;
    	}
    	requireReplanning = levelScene.setEnemies(enemies);
	}
	
	public LevelScene backupState()
	{
		LevelScene sceneCopy = null;
		try
		{
			sceneCopy = (LevelScene) levelScene.clone();
		} catch (CloneNotSupportedException e)
		{
			e.printStackTrace();
		}
		
		return sceneCopy;
	}
	
	
	
	public void restoreState(LevelScene l)
	{
		levelScene = l;
	}
	
	public void advanceStep(boolean[] action)
	{
		levelScene.mario.setKeys(action);
		if (levelScene.verbose > 8) System.out.print("[" 
				+ (action[Mario.KEY_DOWN] ? "d" : "") 
				+ (action[Mario.KEY_RIGHT] ? "r" : "")
				+ (action[Mario.KEY_LEFT] ? "l" : "")
				+ (action[Mario.KEY_JUMP] ? "j" : "")
				+ (action[Mario.KEY_SPEED] ? "s" : "") + "]");
		levelScene.tick();
	}

	/* SEBAS CODE */

	public boolean[] getAction()
	{
        return action;
	}

	public static void updateQtable(float reward)
	{
		float reward_discount = 1;
		while (!recent_actions.isEmpty())
		{
			// add reward value times history discount coefficient
			qtable[recent_actions.get(0)[0]][recent_actions.get(0)[1]] += (reward * reward_discount);

			// remove this action from the list
			recent_actions.remove(0);

			// set discount coefficient for next step
			reward_discount *= gamma;
		}
	}

	public static void updateAction(double[] state, float reward)
	{
		// update qTable
		if (reward != -2500)
		{
			updateQtable(reward);

			// if death state
			if (reward == -1000)
			{
				// ensure recent_actions has been reset
				recent_actions.clear();

				// normalize q table
				float max_val = 0;
				for (int i = 0; i < centroids.length; i++)
				{
					for (int j = 0; j < 12; j++)
					{
						// find largest magnitude in Q-table
						if (abs(qtable[i][j]) > max_val)
						{
							max_val = abs(qtable[i][j]);
						}
					}
				}

				// divide all values by max magnitude
				for (int i = 0; i < centroids.length; i++)
				{
					for (int j = 0; j < 12; j++)
					{
						qtable[i][j] /= max_val;
						qtable[i][j] *= 1000;
					}
				}
			}
		}

		// determine what state current snap matches in qtable
		int current_state = determineState(state);

		// init before max find
		int   best_action = 0;
		float max_action_value = qtable[current_state][0];

		// find action with highest value for state
		for (int i = 1; i < 12; i++)
		{
			if (qtable[current_state][i] > max_action_value)
			{
				best_action = i;
				max_action_value = qtable[current_state][i];
			}
		}

		// update action list
		Integer[] state_action = {current_state, best_action};
		recent_actions.add(0, state_action);

		// update action to new action
		action = possible_actions[best_action];
	}

	/* SEBAS CODE */

	private void visited(int x, int y, int t)
	{
		visitedStates.add(new int[]{x,y,t});
	}
	
	private boolean isInVisited(int x, int y, int t)
	{
		int timeDiff = 5;
		int xDiff = 2;
		int yDiff = 2;
		for(int[] v: visitedStates)
		{
			if (abs(v[0] - x) < xDiff
					&& abs(v[1] - y) < yDiff
					&& abs(v[2] - t) < timeDiff
					&& t >= v[2])
			{
				return true;
			}
		}
		return false;	
		//return visitedStates.contains(new int[]{x,y,t});
	}
}