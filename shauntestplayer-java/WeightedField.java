// import the API.
// See xxx for the javadocs.
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

import bc.*;



public class WeightedField {
	
	public class Entry
	{
		public int distance = Integer.MAX_VALUE;
		public Direction direction = Direction.Center;
		public MapLocation mapLocation = null;
		
		public Entry(MapLocation mapLocation)
		{
			this.mapLocation = mapLocation;
		}
	}
	
	public PlanetMap map;
	public int width;
	public int height;
	public Entry[][] entries;
	
	public WeightedField(PlanetMap map)
	{
		this.map = map;
		this.width = (int) map.getWidth();
		this.height = (int) map.getHeight();
		
		entries = new Entry[width][height];
		
		MapLocation mapLocation = null;
		for(int ii = 0; ii < width; ii++ )
		{
			for(int jj = 0; jj < height; jj++ )
			{
				mapLocation = new MapLocation(map.getPlanet(), ii, jj);
				entries[ii][jj] = new Entry(mapLocation);
			}
		}
	}
	
	public void SetupDirectionFieldTowardsKarbomite(GameController gc)
	{
		Queue<Entry> locationStack = new LinkedList<Entry>();
		for(int ii = 0; ii < width; ii++ )
		{
			for(int jj = 0; jj < height; jj++ )
			{
				Entry entry = entries[ii][jj]; 
				MapLocation location = entry.mapLocation;
				if ( map.initialKarboniteAt(location) > 0)
				{
					boolean needsUpdating = false;
					long karbonite;
					long initialKarbonite = map.initialKarboniteAt(location);
					karbonite = initialKarbonite;
					if ( gc.canSenseLocation(location))
					{
						long sensedKarbomite = gc.karboniteAt(location);
						karbonite = sensedKarbomite;
					}
					if ( karbonite < entry.distance ) {
						entry.distance = (int) karbonite;
						needsUpdating = true;
					}
					if ( needsUpdating )
						locationStack.add(entry);
				}
			}
		}
		
		
		Entry target;
		while(!locationStack.isEmpty())
		{
			target = locationStack.poll();
			
			int newDistance = target.distance - 1;
			
			//Evaluate all locations adjacent to the target:
			for (Direction direction : Direction.values())
			{
				Direction directionToTarget = bc.bcDirectionOpposite(direction);
				MapLocation testLocation = target.mapLocation.add(direction);
				
				newDistance = (int) (target.distance - target.mapLocation.distanceSquaredTo(testLocation));
				
				if ( !map.onMap(testLocation) || map.isPassableTerrainAt(testLocation) == 0) //We've gone off the map!
					continue;
				
				Entry testEntry = entries[testLocation.getX()][testLocation.getY()];
				
				if ( testEntry.distance == Integer.MAX_VALUE ) //We haven't tested this square!
				{
					testEntry.direction = directionToTarget;
					testEntry.distance = newDistance;
					locationStack.add(testEntry);
				}
				if ( testEntry.distance < newDistance ) //We've evaluated this one already, but we found a shortcut!
				{
					testEntry.distance = newDistance;
					testEntry.direction = directionToTarget;
					locationStack.add(testEntry);
				}
			}
		}
	}
	
	public void DebugPrintDistances()
	{
		System.out.println();
		for(int jj = height - 1; jj >= 0; jj-- )
		{
			for(int ii = 0; ii < width; ii++ )
			{
				if ( entries[ii][jj].distance == Integer.MAX_VALUE)
					System.out.print( String.format("%1$4s", 999) );
				else
					System.out.print( String.format("%1$4s", entries[ii][jj].distance) );
			}
			System.out.println();
		}
	}
	
	public void DebugPrintDirections()
	{
		System.out.println();
		for(int jj = height - 1; jj >= 0; jj-- )
		{
			for(int ii = 0; ii < width; ii++ )
			{
				
				System.out.print( DirectionToString(entries[ii][jj].direction ));
			}
			System.out.println();
		}
	}
	
	private String DirectionToString(Direction direction)
	{
		switch (direction)
		{
		case North:
			return " ^ ";
		case South:
			return " . ";
		case East:
			return " ->";
		case West:
			return "<- ";
		case Northeast:
			return " / ";
		case Northwest:
			return " \\ ";
		case Southeast:
			return " \\.";
		case Southwest:
			return "./ ";
		default:
			break;
		}
		return "   ";
	}
}