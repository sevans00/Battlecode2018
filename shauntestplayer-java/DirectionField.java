// import the API.
// See xxx for the javadocs.
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

import bc.*;



public class DirectionField {
	
	public class Entry
	{
		public int distance = -1;
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
	
	public DirectionField(PlanetMap map)
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
	
	public void SetupDirectionFieldTowardsUnits( ArrayList<Unit> units)
	{
		ArrayList<MapLocation> targets = new ArrayList<MapLocation>();
		for ( Unit unit : units)
		{
			targets.add(unit.location().mapLocation());
		}
		SetupDirectionFieldTowards(targets);
	}
	
	public void SetupDirectionFieldTowards( ArrayList<MapLocation> targets)
	{
		Queue<Entry> locationStack = new LinkedList<Entry>();
		for(MapLocation target : targets)
		{
			Entry entry = entries[target.getX()][target.getY()];
			entry.distance = 0;
			locationStack.add(entry);
		}
		
		Entry target;
		while(!locationStack.isEmpty())
		{
			target = locationStack.poll();
			
			int newDistance = target.distance + 1;
			
			//Evaluate all locations adjacent to the target:
			for (Direction direction : Direction.values())
			{
				Direction directionToTarget = bc.bcDirectionOpposite(direction);
				MapLocation testLocation = target.mapLocation.add(direction);
				
				if ( !map.onMap(testLocation) || map.isPassableTerrainAt(testLocation) == 0) //We've gone off the map!
					continue;
				
				Entry testEntry = entries[testLocation.getX()][testLocation.getY()];
				
				if ( testEntry.distance == -1 ) //We haven't tested this square!
				{
					testEntry.direction = directionToTarget;
					testEntry.distance = newDistance;
					locationStack.add(testEntry);
				}
				if ( testEntry.distance > newDistance ) //We've evaluated this one already, but we found a shortcut!
				{
					testEntry.distance = newDistance;
					testEntry.direction = directionToTarget;
					locationStack.add(testEntry);
				}
			}
		}
	}
	
	public Entry getEntryAt(Unit unit)
	{
		if ( !unit.location().isOnMap() )
			return null;
		return entries[unit.location().mapLocation().getX()][unit.location().mapLocation().getY()];
	}
	
	public ArrayList<Entry> getAdjacentEntriesTo(Unit unit)
	{
		ArrayList<Entry> adjacentEntries = new ArrayList<Entry>();
		for (Direction direction : Direction.values())
		{
			MapLocation testLocation = unit.location().mapLocation().add(direction);
			if ( !map.onMap(testLocation) || map.isPassableTerrainAt(testLocation) == 0) //We've gone off the map!
				continue;
			adjacentEntries.add(entries[testLocation.getX()][testLocation.getY()]);
		}
		return adjacentEntries;
	}
	
	public void DebugPrintDistances()
	{
		System.out.println();
		for(int jj = height - 1; jj >= 0; jj-- )
		{
			for(int ii = 0; ii < width; ii++ )
			{
				System.out.print( String.format("%1$4s",entries[ii][jj].distance) );
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