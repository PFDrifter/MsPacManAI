import java.util.*;

import controllers.osc.OSCPacMan;

public class AIControllerPacMan extends OSCPacMan
{
	/***********************************************************************************************************************/
	public AIControllerPacMan(String[] args)
	{
		super(args);
	}

	/**
	 * This class stores directional information from any desired ghost
	 * 
	 * @author Jennifer
	 *
	 */
	public class Ghost implements Comparable<Ghost>
	{
		private float distance;

		private int direction;

		private int isApproaching;

		private int containsJunctions;

		public Ghost()
		{

		}

		public Ghost(float distance, int direction, int isApproaching,
				int containsJunctions)
		{
			super();
			this.distance = distance;
			this.direction = direction;
			this.isApproaching = isApproaching;
			this.containsJunctions = containsJunctions;
		}

		public float getDistance()
		{
			return distance;
		}

		public void setDistance(float distance)
		{
			this.distance = distance;
		}

		public int getDirection()
		{
			return direction;
		}

		public void setDirection(int direction)
		{
			this.direction = direction;
		}

		public int getIsApproaching()
		{
			return isApproaching;
		}

		public void setIsApproaching(int isApproaching)
		{
			this.isApproaching = isApproaching;
		}

		public int getContainsJunctions()
		{
			return containsJunctions;
		}

		public void setContainsJunctions(int containsJunctions)
		{
			this.containsJunctions = containsJunctions;
		}

		/**
		 * Used to be able to sort an ArrayList of Ghosts according to their distance from Mrs. PacMan
		 */
		@Override
		public int compareTo(Ghost o)
		{
			if (this.distance < o.distance)
			{
				return -1;
			} else if (this.distance > o.distance)
			{
				return 1;
			} else
			{
				return 0;
			}
		}

	}

	/**
	 * Stores the states that can be invoked within the AI logic
	 * 
	 * @author Jennifer
	 *
	 */
	public enum states
	{
		None,

		Gathering,

		Running,

		Chasing,

		Powerpill,

		Lure
	}

	public states currentState;

	public ArrayList<ArrayList<Ghost>> ghostInformation;

	/**
	 * Distance tolerance that the ghosts are allowed to reach before Mrs. PacMan runs away
	 */
	public float ghostDistanceTolerance = 0.2f;

	/**
	 * Used to calculate the total score of X number of Mrs. PacMan games
	 */
	public static float totalScore = 0;

	@Override
	public void startGame()
	{ // runs once at the start of each game
		currentState = states.None;
	}

	@Override
	public void endGame()
	{ // runs once at the end of each game
	}

	/**
	 * Select a direction for Ms. Pac-Man. Runs once for each timestep of the
	 * game. Must return one int: 0 (UP), 1 (RIGHT), 2 (DOWN), 3 (LEFT)
	 *
	 * Note: to check a binary state variable, use less-than or greater-than: if
	 * (directedState.get(11) > 0); //test if closest ghost is approaching
	 *
	 * @return The direction to move in.
	 */
	@Override
	public int selectMove()
	{
		int move = 0;
		int closeToPowerpill = 0;
		int isEdible = 0;

		Ghost closestGhost = new Ghost();

		ArrayList<Integer> validDirections = new ArrayList<Integer>();
		ghostInformation = new ArrayList<ArrayList<Ghost>>();
		List<Float> globalState = new ArrayList<Float>();

		/**
		 * Removes any directions that are facing a wall from a potential direction that Mrs. PacMan could go
		 */
		for (int direction = 0; direction < NUM_DIR; direction++)
		{
			if (!isTowardWall(direction))
			{
				validDirections.add(direction);
			}
		}

		for (int direction : validDirections)
		{
			globalState = getDirectedState(direction).subList(0, 6);
			isEdible = Math.round(globalState.get(4));

			ArrayList<Float> directedState = getDirectedState(direction);
			closeToPowerpill = Math.round(directedState.get(6));

			ArrayList<Ghost> ghostList = new ArrayList<Ghost>();

			ghostList.add(new Ghost(directedState.get(10), direction, Math
					.round(directedState.get(11)), Math.round(directedState
					.get(12))));
			Collections.sort(ghostList);

			ghostInformation.add(ghostList);
		}
		
		/**
		 * Sorts the Ghost lists in each direction to find the closest ghost in all valid directions
		 */
		Collections.sort(ghostInformation, new Comparator<List<Ghost>>()
		{
			@Override
			public int compare(List<Ghost> o1, List<Ghost> o2)
			{
				return o1.get(0).compareTo(o2.get(0));
			}
		});

		closestGhost = ghostInformation.get(0).get(0);

		/**
		 * If any ghost can be eaten, Mrs. PacMan enters the Chasing state
		 */
		if (isEdible == 1)
		{
			currentState = states.Chasing;
		} 
		else
		{
			/**
			 * If the ghosts are further away from Mrs. PacMan than the ghost tolerance, Mrs. PacMan enters the Gathering
			 * state in order to eat pills
			 */
			if (closestGhost.getDistance() >= ghostDistanceTolerance)
			{
				/**
				 * If miss PacMan is about to each a powerpill but the ghosts are further away than the ghost tolerance
				 * Mrs. PacMan enters the Lure state. She will then wait to eat the powerpill until more ghosts are close
				 */
				if (closeToPowerpill == 1)
				{
					currentState = states.Lure;
				} 
				else
				{
					currentState = states.Gathering;
				}
			} 
			/**
			 * If the ghosts are within the ghost tolerance and Mrs. PacMan is 10 steps away from a powerpill, she
			 * enters the Powerpill state where she heads for the nearest powerpill.
			 */
			else if (closeToPowerpill == 1)
			{
				currentState = states.Powerpill;
			} 
			/**
			 * If the closest ghost is within the ghost tolerance and is appraoching Mrs. PacMan then she enters the Running
			 * state where she runs away from threat ghosts
			 */
			else if (closestGhost.isApproaching == 1)
			{
				currentState = states.Running;
			}
		}

		switch (currentState)
		{
		case Gathering:
		{
			move = gathering(validDirections);
			break;
		}
		case Running:
		{
			move = running(validDirections, closestGhost);
			break;
		}
		case Chasing:
		{
			move = chasing(validDirections, closestGhost);
			break;
		}
		case Powerpill:
		{
			move = powerpill(validDirections);
			break;
		}
		case Lure:
			move = lure(validDirections);
			break;
		}

		return move;
	}

	/**
	 * Mrs. PacMan tries to find the closest pills in any direction and eats them
	 * @param directions valid directions Mrs. PacMan can go
	 * @return the direction Mrs. PacMan should go
	 */
	public int gathering(ArrayList<Integer> directions)
	{
		int move = 0;
		float nearestPillDist = 1.0f;
		int distancesEqual = 0;
		ArrayList<Integer> sameDistanceDirections = new ArrayList<Integer>();

		for (int direction : directions)
		{
			ArrayList<Float> directedState = getDirectedState(direction);
			float dist = directedState.get(7);

			if (dist < nearestPillDist)
			{
				move = direction;
				nearestPillDist = dist;
			}
		}

		/**
		 * Checks to see if more than one pill are the closest distance
		 */
		for (int direction : directions)
		{
			ArrayList<Float> directedState = getDirectedState(direction);
			float dist = directedState.get(7);

			if (dist == nearestPillDist && direction != move)
			{
				sameDistanceDirections.add(direction);
				distancesEqual = 1;
			}

			sameDistanceDirections.add(move);
		}

		/**
		 * Picks a random direction if there are pills with equal closest distance
		 */
		if (distancesEqual == 1)
		{
			move = sameDistanceDirections
					.get((int) (Math.random() * sameDistanceDirections.size()));
		}

		return move;
	}

	/**
	 * Mrs. PacMan attempts to find the safest direction to escape threat ghosts
	 * @param directions Valid directions Mrs. PacMan can go
	 * @param closestGhost Closest ghost information
	 * @return Direction to escape ghosts
	 */
	public int running(ArrayList<Integer> directions, Ghost closestGhost)
	{
		int move = 0;

		int directionClosestGhost = closestGhost.getDirection();

		int oppositeDirection = (directionClosestGhost + 2) % 4;

		float distNearestJunction = 0.0f;
		float distNearestPowerPill = 1.0f;
		int directionNearestPowerPill = 0;
		int distancesEqual = 0;
		
		ArrayList<Integer> sameDistanceDirections = new ArrayList<Integer>();

		ArrayList<Integer> modifiedDirections = directions;

		/**
		 * Mrs. PacMan first attempts to head in the opposite direction of the closest ghost if safe to do so
		 */
		if (directions.contains(oppositeDirection))
		{
			ArrayList<Float> directedState = getDirectedState(move);

			Ghost oppositeGhost = new Ghost(directedState.get(10),
					oppositeDirection, Math.round(directedState.get(11)),
					Math.round(directedState.get(12)));
			distNearestJunction = directedState.get(28);

			/**
			 * Mrs. PacMan can move in the opposite direction in the closest ghost in the opposite direction
			 * is further away than the nearest safe junction
			 */
			if ((oppositeGhost.getDistance() > distNearestJunction))
			{
				move = oppositeDirection;
				return move;
			}
			else
			{
				modifiedDirections.remove(modifiedDirections
						.indexOf(oppositeDirection));
			}
		}

		/**
		 * If Mrs. PacMan cannot go in the opposite direction to the closest ghost, find the best direction
		 * for the safest nearest junction
		 */
		for (int direction : modifiedDirections)
		{
			ArrayList<Float> directedState = getDirectedState(direction);
			float dist = directedState.get(28);
			float powerPill = directedState.get(8);
			
			if (dist > distNearestJunction)
			{
				move = direction;
				distNearestJunction = dist;
			}
			
			if (powerPill < distNearestPowerPill)
			{
				distNearestPowerPill = powerPill;
				directionNearestPowerPill = direction;
			}
		}

		/**
		 * Checks to see if more than one junction are the closest distance
		 */
		for (int direction : modifiedDirections)
		{
			ArrayList<Float> directedState = getDirectedState(direction);
			float dist = directedState.get(28);

			if (dist == distNearestJunction && direction != move)
			{
				sameDistanceDirections.add(direction);
				distancesEqual = 1;
			}

			sameDistanceDirections.add(move);
		}

		/**
		 * Picks a random direction if there are junctions with equal closest distance
		 */
		if (distancesEqual == 1)
		{
			move = sameDistanceDirections
					.get((int) (Math.random() * sameDistanceDirections.size()));
		}

		ArrayList<Float> directedState = getDirectedState(directionNearestPowerPill);
		float distanceNearestGhostPowerpill = directedState.get(10);

		/**
		 * If Mrs. PacMan is closer to a powerpill than the nearest ghost in that direction,
		 * she heads for the powerpill
		 */
		if (distNearestPowerPill < distanceNearestGhostPowerpill)
		{
			currentState = states.Powerpill;
			move = powerpill(directions);
		}

		return move;
	}

	/**
	 * Mrs. PacMan attempts to find the closest ghost to eat when under the affect of a powerpill
	 * @param directions Valid directions Mrs. PacMan can go
	 * @param closestGhost Closest ghost information
	 * @return The direction towards the closest ghost
	 */
	public int chasing(ArrayList<Integer> directions, Ghost closestGhost)
	{
		int move = 0;
		float nearestGhostDist = 1.0f;
		int distancesEqual = 0;
		ArrayList<Integer> sameDistanceDirections = new ArrayList<Integer>();

		/**
		 * Move in the same direction as closest ghost
		 */
		move = closestGhost.getDirection();
		nearestGhostDist = closestGhost.getDistance();

		/**
		 * Checks to see if more than one ghost are the closest distance
		 */
		for (int direction : directions)
		{
			ArrayList<Float> directedState = getDirectedState(direction);
			float dist = directedState.get(10);

			if (dist == nearestGhostDist && direction != move)
			{
				sameDistanceDirections.add(direction);
				distancesEqual = 1;
			}

			sameDistanceDirections.add(move);
		}

		/**
		 * Picks a random direction if there are ghosts with equal closest distance
		 */
		if (distancesEqual == 1)
		{
			move = sameDistanceDirections
					.get((int) (Math.random() * sameDistanceDirections.size()));
		}

		ArrayList<Float> directedState = getDirectedState(move);
		float dist = directedState.get(10);
		float isEdible = directedState.get(13);
		float isApproaching = directedState.get(11);
		float dist2nd = directedState.get(14);
		float isEdible2nd = directedState.get(15);
		float isApproaching2nd = directedState.get(17);
		
		/**
		 * Checks to see if the closest ghost is still edible, if not start to run away from it.
		 */
		if (Math.round(isEdible) == 0.0 && Math.round(isApproaching) == 1
				&& dist < ghostDistanceTolerance)
		{
			currentState = states.Running;
			move = running(directions, closestGhost);
		}
		
		/**
		 * Checks to see if the second closest ghost is still edible, if not start to run away from it.
		 */
		if (Math.round(isEdible2nd) == 0.0 && Math.round(isApproaching2nd) == 1
				&& dist2nd < ghostDistanceTolerance)
		{
			currentState = states.Running;
			move = running(directions, closestGhost);
		}

		return move;
	}

	/**
	 * Mrs. PacMan attempts to find the closest powerpill when within 10 steps of a powerpill and being chased by ghosts
	 * @param directions Valid directions Mrs. PacMan can go
	 * @return Direction towards the closest powerpill
	 */
	public int powerpill(ArrayList<Integer> directions)
	{
		int move = 0;
		float nearestPowerPillDist = 1.0f;
		int distancesEqual = 0;
		ArrayList<Integer> sameDistanceDirections = new ArrayList<Integer>();

		for (int direction : directions)
		{
			ArrayList<Float> directedState = getDirectedState(direction);
			float dist = directedState.get(8);

			if (dist < nearestPowerPillDist)
			{
				move = direction;
				nearestPowerPillDist = dist;
			}
		}

		/**
		 * Checks to see if more than one powerpill are the closest distance
		 */
		for (int direction : directions)
		{
			ArrayList<Float> directedState = getDirectedState(direction);
			float dist = directedState.get(8);

			if (dist == nearestPowerPillDist && direction != move)
			{
				sameDistanceDirections.add(direction);
				distancesEqual = 1;
			}

			sameDistanceDirections.add(move);
		}

		/**
		 * Picks a random direction if there are powerpills with equal closest distance
		 */
		if (distancesEqual == 1)
		{
			move = sameDistanceDirections
					.get((int) (Math.random() * sameDistanceDirections.size()));
		}
		
		return move;
	}

	/**
	 * Mrs. PacMan attempts to lure ghosts closer to her when she is about to eat a powerpill in order to
	 * make it easier to chase them.
	 * @param directions Valid directions Mrs. PacMan can go
	 * @return The direction in order to not eat the powerpill
	 */
	public int lure(ArrayList<Integer> directions)
	{
		int move = 0;
		float nearestPowerPillDist = 1.0f;
		float powerPillDistLimit = 0.015f;
		int oppositeDirection;

		for (int direction : directions)
		{
			ArrayList<Float> directedState = getDirectedState(direction);
			float dist = directedState.get(8);

			if (dist < nearestPowerPillDist)
			{
				move = direction;
				nearestPowerPillDist = dist;
			}
		}

		/**
		 * Head in the opposite direction of the powerpill if to close to the pill
		 */
		if (nearestPowerPillDist <= powerPillDistLimit)
		{
			oppositeDirection = (move + 2) % 4;
			return oppositeDirection;
		}

		return move;
	}

	/**
	 * Prints out the current move of Mrs. Pacman. Helper method for debugging.
	 * @param move The string representation of Mrs. Pacmans current move.
	 */
	public void printMove(int move)
	{
		if (move == 0)
		{
			System.out.println("GO UP");
		} else if (move == 1)
		{
			System.out.println("GO RIGHT");
		} else if (move == 2)
		{
			System.out.println("GO DOWN");
		} else if (move == 3)
		{
			System.out.println("GO LEFT");
		}
	}

	/***********************************************************************************************************************/
	public static void main(String[] args)
	{
		AIControllerPacMan pacman = new AIControllerPacMan(args);
		int gameNum = 0;
		for (int g = 0; g < pacman.numGamesToPlay(); g++)
		{
			pacman.initGame();
			while (!pacman.gameOver())
			{
				try
				{
					Thread.sleep(1);
				} catch (Exception e)
				{
				}
			}
			System.out.println("Game: " + (g + 1) + " Score: "
					+ pacman.gameScore());
			totalScore = totalScore + pacman.gameScore();
			gameNum++;
			System.out.println("Average Score: " + (totalScore / gameNum)
					+ " On Game " + gameNum);
		}
		pacman.exit();
		System.exit(0);
	}
}
