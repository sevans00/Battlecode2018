// import the API.
// See xxx for the javadocs.
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Stack;

import bc.*;



public class FactoryPlan {
	
	public class Entry
	{
		public int distance = -1;
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
	public MapLocation initialLocation;
	
	public FactoryPlan(PlanetMap map, MapLocation initialLocation, ArrayList<Unit> factories)
	{
		Entry initialEntry = new Entry(initialLocation);
		
		
		
		
		
		
		this.map = map;
		this.width = (int) map.getWidth();
		this.height = (int) map.getHeight();
		this.initialLocation = initialLocation;
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
	
}