// import the API.
// See xxx for the javadocs.
import java.util.ArrayList;
import java.util.Random;
import java.util.Stack;

import bc.*;



public class DirectionField {
	public class Entry
	{
		public int distance = -1;
		public Direction direction = Direction.Center;
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
		
		for(int ii = 0; ii < width; ii++ )
		{
			for(int jj = 0; jj < height; jj++ )
			{
				entries[ii][jj] = new Entry();
			}
		}
	}
	
	public Stack<Entry> locationStack;
	
	public void SetupDirectionField( ArrayList<MapLocation> targets)
	{
		locationStack = new Stack<Entry>();
		for(MapLocation target : targets)
		{
			Entry entry = entries[target.getX()][target.getY()];
			entry.distance = 0;
			locationStack.push(entry);
		}
		
		Entry target;
		while(!locationStack.isEmpty())
		{
			target = locationStack.pop();
			
			//Evaluate all locations adjacent to the target:
			
			
			
		}
		
		
	}
	   
}