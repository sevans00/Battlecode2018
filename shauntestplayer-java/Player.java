// import the API.
// See xxx for the javadocs.
import java.util.ArrayList;
import java.util.Random;

import bc.*;

public class Player {
	public static GameController gc;
	
	public static Team friendlyTeam = Team.Red;
	public static Team enemyTeam = Team.Blue;
	
    public static void main(String[] args) {
        // MapLocation is a data structure you'll use a lot.
        MapLocation loc = new MapLocation(Planet.Earth, 10, 20);
        System.out.println("loc: "+loc+", one step to the Northwest: "+loc.add(Direction.Northwest));
        System.out.println("loc x: "+loc.getX());
        
        // One slightly weird thing: some methods are currently static methods on a static class called bc.
        // This will eventually be fixed :/
        System.out.println("Opposite of " + Direction.North + ": " + bc.bcDirectionOpposite(Direction.North));
        
        // Connect to the manager, starting the game
        gc = new GameController();
        
        // Direction is a normal java enum.
        Direction[] directions = Direction.values();
        
        while (true) {
            System.out.println("Current round: "+gc.round());
            // VecUnit is a class that you can think of as similar to ArrayList<Unit>, but immutable.
            VecUnit units = gc.myUnits();
            /*
            //Set up team:
            Unit firstUnit = units.get(0);
            if ( firstUnit.team() == Team.Blue)
            {
            	friendlyTeam = Team.Red;
            	enemyTeam = Team.Blue;
            }
            */
            
            ArrayList<Unit> factories = new ArrayList<Unit>();
            ArrayList<Unit> healers = new ArrayList<Unit>();
            ArrayList<Unit> knights = new ArrayList<Unit>();
            ArrayList<Unit> mages = new ArrayList<Unit>();
            ArrayList<Unit> rangers = new ArrayList<Unit>();
            ArrayList<Unit> rockets = new ArrayList<Unit>();
            ArrayList<Unit> workers = new ArrayList<Unit>();
            
            for (int ii = 0; ii < units.size(); ii++) 
            {
                Unit unit = units.get(ii);
                switch (unit.unitType()) {
				case Factory:
					factories.add(unit);
					break;
				case Healer:
					healers.add(unit);
					break;
				case Knight:
					knights.add(unit);
					break;
				case Mage:
					mages.add(unit);
					break;
				case Ranger:
					rangers.add(unit);
					break;
				case Rocket:
					rockets.add(unit);
					break;
				case Worker:
					workers.add(unit);
				default:
					break;
				}
            }
            //End units arraylist population
            
            //Sense enemies:
            //VecUnit enemies = gc.senseNearbyUnitsByTeam(firstUnit.location().mapLocation(), 9999, enemyTeam);
            
            
            //Factories:
            for (Unit unit : factories) 
            {
				VecUnitID garrison = unit.structureGarrison();
				if ( garrison.size() > 0 )
				{
					Direction freeDirection = getNextUnloadDirection(unit);
					if ( freeDirection != null )
					{
						System.out.println("Unloaded a unit!");
						gc.unload(unit.id(), freeDirection);
						continue;
					}
				}
				//Build:
				if ( gc.canProduceRobot(unit.id(), UnitType.Knight))
				{
					gc.produceRobot(unit.id(), UnitType.Knight);
                    System.out.println("produced a knight!");
                    continue;
				}
			}
            
            //Workers:
            for (Unit unit : workers) 
            {
            	Location location = unit.location();
            	if ( location.isOnMap() )
            	{
            		VecUnit nearbyUnits = gc.senseNearbyUnitsByTeam(location.mapLocation(), unit.abilityRange(), unit.team());
            		for (int ii = 0; ii < nearbyUnits.size(); ii++) {
            			Unit nearbyUnit = nearbyUnits.get(ii);
						if ( gc.canBuild(unit.id(), nearbyUnit.id()))
						{
							gc.build(unit.id(), nearbyUnit.id());
							continue;
						}
					}
            		
            		Direction direction = getCanBlueprintDirection(unit, UnitType.Factory);
            		if ( 	direction != null 
            				&& gc.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Factory) 
            				&& gc.canBlueprint(unit.id(), UnitType.Factory, direction) )
            		{
            			gc.blueprint(unit.id(), UnitType.Factory, direction);
            			continue;
            		}
            		
            		DoRandomMove(unit);
            	}
            }
            
            //Knights:
            for (Unit unit : knights) {
            	if ( gc.isAttackReady(unit.id()))
            	{
					VecUnit nearbyUnits = gc.senseNearbyUnits(unit.location().mapLocation(), unit.attackRange() );
					for (int ii = 0; ii < nearbyUnits.size(); ii++) {
            			Unit other = nearbyUnits.get(ii);
						if ( other.team() != unit.team() && gc.canAttack(unit.id(), other.id()) )
						{
							gc.attack(unit.id(), other.id());
							continue;
						}
					}
            	}
            	
            	VecUnit nearbyUnits = gc.senseNearbyUnits(unit.location().mapLocation(), unit.attackRange() );
            	for (int ii = 0; ii < nearbyUnits.size(); ii++) {
        			Unit other = nearbyUnits.get(ii);
            		if ( other.team() != unit.team() )
            		{
            			Direction direction = unit.location().mapLocation().directionTo(other.location().mapLocation());
            			DoMoveTowards(unit, direction);
            			continue;
            		}
            	}
            	
            	DoRandomMove(unit);
			}
            
            /*
                Unit unit = units.get(ii);
                
                if ( unit.unitType() == UnitType.Factory )
                {
                	garrison = unit.structureGarrison();
                	
                }
                
                
                if ( unit.unitType() == UnitType.Worker )
                {
                	
                }
                
                
                // Most methods on gc take unit IDs, instead of the unit objects themselves.
                if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), Direction.Southeast)) {
                	if (Math.random() > 0.5f)
                		gc.moveRobot(unit.id(), Direction.Southeast);
                	else
                		gc.moveRobot(unit.id(), Direction.Northwest);
                	
                }
            }*/
            // Submit the actions we've done, and wait for our next turn.
            gc.nextTurn();
        }
    }
    
    public static boolean DoMoveTowards(Unit unit, Direction direction)
    {
    	return DoMoveTowards(unit, direction, 0);
    }
    public static boolean DoMoveTowards(Unit unit, Direction direction, int iterations)
    {
    	if ( iterations > 7 )
    		return false;
    	if ( gc.isMoveReady(unit.id()) ) {
    		if ( gc.canMove(unit.id(), direction)){
    			gc.moveRobot(unit.id(), direction);
    		}
    		else {
    			Direction newDirection = bc.bcDirectionRotateLeft(direction);
    			DoMoveTowards(unit, newDirection, iterations++);
    		}
    		return true;
    	}
    	return false;
    }
    
    public static boolean DoRandomMove(Unit unit)
    {
    	Direction direction = RandomDirection();
    	if ( gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), direction) ){
    		gc.moveRobot(unit.id(), direction);
    		return true;
    	}
    	return false;
    }
    
    public static Direction RandomDirection()
    {
    	Direction[] directions = Direction.values();
    	int rand = new Random().nextInt(directions.length);
    	return directions[rand];
    }
    
    public static Direction getNextUnloadDirection (Unit unit)
    {
    	for (Direction direction : Direction.values())
			if ( gc.canUnload(unit.id(), direction) )
				return direction;
    	return null;
    }
    
    public static Direction getCanBlueprintDirection(Unit unit, UnitType unitType)
    {
    	for (Direction direction : Direction.values())
    		if ( gc.canBlueprint(unit.id(), unitType, direction) )
    			return direction;
    	return null;
    }
    
}