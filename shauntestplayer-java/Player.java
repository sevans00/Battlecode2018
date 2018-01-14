// import the API.
// See xxx for the javadocs.
import java.util.ArrayList;
import java.util.Random;

import bc.*;



public class Player {
	
	
	public static GameController gc;
	
	public static Team friendlyTeam = Team.Red;
	public static Team enemyTeam = Team.Blue;
	
	public static ArrayList<Unit> factories = new ArrayList<Unit>();
	public static ArrayList<Unit> factoriesToHeal = new ArrayList<Unit>();
	public static ArrayList<Unit> healers = new ArrayList<Unit>();
	public static ArrayList<Unit> knights = new ArrayList<Unit>();
	public static ArrayList<Unit> mages = new ArrayList<Unit>();
	public static ArrayList<Unit> rangers = new ArrayList<Unit>();
	public static ArrayList<Unit> rockets = new ArrayList<Unit>();
	public static ArrayList<Unit> workers = new ArrayList<Unit>();
	
	public static ArrayList<Unit> enemies = new ArrayList<Unit>();
	
	public static MapLocation someMapLoc = null;
    
	
	public static DirectionField karbomiteDirectionField;
	
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
        
        friendlyTeam = gc.team();
		if (friendlyTeam == Team.Blue)
		    enemyTeam = Team.Red;
        
		
        
        while (true) {
            System.out.println("Current round: "+gc.round());
            // VecUnit is a class that you can think of as similar to ArrayList<Unit>, but immutable.
            VecUnit units = gc.myUnits();
            
        	factories = new ArrayList<Unit>();
        	factoriesToHeal = new ArrayList<Unit>();
        	healers = new ArrayList<Unit>();
        	knights = new ArrayList<Unit>();
        	mages = new ArrayList<Unit>();
        	rangers = new ArrayList<Unit>();
        	rockets = new ArrayList<Unit>();
        	workers = new ArrayList<Unit>();
        	
        	enemies = new ArrayList<Unit>();
            
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

            	if ( someMapLoc == null && unit.location().isOnMap() )
            		someMapLoc = unit.location().mapLocation();
            }
            //End units arraylist population
            
            //Sense enemies:
            if ( someMapLoc != null )
            {
            	VecUnit enemyVecUnit = gc.units();
            	System.out.println("Enemies sensed:" + enemyVecUnit.size());
                for ( int ii = 0; ii < enemyVecUnit.size(); ii++ )
                {
                	Unit enemy = enemyVecUnit.get(ii);
                	if ( enemy.team() == enemyTeam && enemy.location().isOnMap() )
                		enemies.add(enemy);
                }
            }
            
            
            
            //Do catchup section:
            
            //Minimum number of workers:
            if ( workers.size() < 5 && gc.karbonite() > bc.bcUnitTypeReplicateCost(UnitType.Worker) )
            {
            	for (Unit unit : workers)
            	{
            		if ( DoWorkerReplicate(unit) )
            			break;
            	}
            }
            
            //Minimum number of factories:
            if (gc.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Factory)
            		&& (factories.size() < 4 || gc.karbonite() > 300 ) )
            {
                for (Unit unit : workers)
                {
            		Direction direction = getCanBlueprintDirection(unit, UnitType.Factory);
            		if ( 	direction != null 
            				&& gc.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Factory) 
            				&& gc.canBlueprint(unit.id(), UnitType.Factory, direction) )
            		{
            			gc.blueprint(unit.id(), UnitType.Factory, direction);
            			break; //No reason why we can't build more
            		}
                }
            }
            
            
            //Factories:
            for (Unit unit : factories) 
            {
            	if ( unit.health() < unit.maxHealth() )
            		factoriesToHeal.add(unit);
            	
            	//Ungarrison:
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
				if ( Math.random() < 0.5f ){
					if ( gc.canProduceRobot(unit.id(), UnitType.Knight))
					{
						gc.produceRobot(unit.id(), UnitType.Knight);
	                    System.out.println("produced a knight!");
	                    continue;
					}
				} else {
					if ( gc.canProduceRobot(unit.id(), UnitType.Ranger))
					{
						gc.produceRobot(unit.id(), UnitType.Ranger);
	                    System.out.println("produced a ranger!");
	                    continue;
					}
				}
			}
            
            
            
            //Workers:
            for (Unit unit : workers) 
            {
            	DoWorker(unit);
            }
            
            //Knights:
            for (Unit unit : knights) {
            	DoKnight(unit);
			}
            for ( Unit unit : rangers) {
            	DoRanger(unit);
            }
            
            
            // Submit the actions we've done, and wait for our next turn.
            gc.nextTurn();
        }
    }
    
    //Do one Knight
    public static boolean DoKnight(Unit unit)
    {
    	if ( !unit.location().isOnMap() )
    		return false;
    	
    	Unit enemy = GetClosestUnitFrom(unit, enemies);
    	if ( enemy == null )
    	{
    		return DoMoveTowardsEnemySpawn(unit);
    	}
    	
    	if ( unit.location().mapLocation().isWithinRange(unit.attackRange(), enemy.location().mapLocation() ) )
    	{
    		if ( gc.isAttackReady(unit.id()) && gc.canSenseUnit(enemy.id()) && gc.canAttack(unit.id(), enemy.id()) )
    		{
    			System.out.println("Attack!!!! "+unit +" "+enemy);
				gc.attack(unit.id(), enemy.id());
    		}
    	}
    	
    	TryMoveTowards(unit, enemy);
    	
    	//TODO: add in complicated code that backs away after firing
    	return DoMoveTowardsEnemySpawn(unit);
    }
    
    public static boolean DoRanger(Unit unit)
    {
    	if ( !unit.location().isOnMap() )
    		return false;
    	
    	Unit enemy = GetClosestUnitFrom(unit, enemies);
    	if ( enemy == null )
    	{
    		return DoMoveTowardsEnemySpawn(unit);
    	}
    	
    	MapLocation unitLocation = unit.location().mapLocation();
    	MapLocation enemyLocation = enemy.location().mapLocation();
    	long distanceSquaredTo = unitLocation.distanceSquaredTo(enemyLocation);
    	Direction directionTo = unitLocation.directionTo(enemyLocation);
    	if ( distanceSquaredTo <= unit.rangerCannotAttackRange() )
    	{
    		TryMoveLoose(unit, bc.bcDirectionOpposite(directionTo), 2);
    	}
    	
    	if ( unit.location().mapLocation().isWithinRange(unit.attackRange(), enemy.location().mapLocation() ) )
    	{
    		if ( gc.isAttackReady(unit.id()) && gc.canSenseUnit(enemy.id()) && gc.canAttack(unit.id(), enemy.id()) )
    		{
    			System.out.println("Attack!!!! "+unit +" "+enemy);
				gc.attack(unit.id(), enemy.id());
    		}
    		return false;
    	}
    	
    	return TryMoveTowards(unit, enemy);
    }
    
    
    //Do one Worker
    public static boolean DoWorker(Unit unit)
    {
    	if ( !unit.location().isOnMap() )
    		return false;
    	
    	//Is there a factory we need to build?
    	Unit closestFactoryNeedsHealing = GetClosestUnitFrom(unit, factoriesToHeal);
    	if ( closestFactoryNeedsHealing != null )
    	{
    		long distanceToClosestFactory = unit.location().mapLocation().distanceSquaredTo(closestFactoryNeedsHealing.location().mapLocation());
    		if ( unit.location().mapLocation().isWithinRange(unit.abilityRange(), closestFactoryNeedsHealing.location().mapLocation()) )
    		{
    			DoWorkerRepairBuild(unit, closestFactoryNeedsHealing);
    		}
    		else
    		{
    			TryMoveTowards(unit, closestFactoryNeedsHealing);
    			DoWorkerRepairBuild(unit, closestFactoryNeedsHealing);
    		}
    	}
    	DoWorkerHarvest(unit);
    	
    	DoRandomMove(unit);
    	
    	return false;
    }
    
    public static boolean DoWorkerHarvest(Unit unit)
    {
    	for ( Direction direction : Direction.values())
    	{
    		if ( gc.canHarvest(unit.id(), direction))
    		{
    			gc.harvest(unit.id(), direction);
    			return true;
    		}
    	}
    	return false;
    }
    
    public static boolean DoWorkerRepairBuild(Unit unit, Unit building)
    {
    	if ( gc.canBuild(unit.id(), building.id()) )
    	{
    		gc.build(unit.id(), building.id());
    		return true;
    	}
    	if ( gc.canRepair(unit.id(), building.id()) )
    	{
    		gc.repair(unit.id(), building.id());
    		return true;
    	}
    	return false;
    }
    
    
    public static boolean DoWorkerReplicate(Unit unit)
    {
    	Direction replicateDirection = GetWorkerReplicateDirection(unit);
		if ( replicateDirection != null )
		{
			gc.replicate(unit.id(), replicateDirection);
			return true;
		}
		return false;
    }
    
    
    public static Unit GetClosestUnitFrom(Unit unit, ArrayList<Unit> units)
    {
    	if ( units.size() <= 0 )
    		return null;
    	
    	Unit closestUnit = units.get(0);
    	long closestDistance = unit.location().mapLocation().distanceSquaredTo(closestUnit.location().mapLocation());
    	Unit testUnit;
    	long testDistance;
    	for( int ii = 1; ii < units.size(); ii++ )
    	{
    		testUnit = units.get(ii);
    		testDistance = unit.location().mapLocation().distanceSquaredTo(testUnit.location().mapLocation());
    		if ( testDistance < closestDistance )
    		{
    			closestUnit = testUnit;
    			closestDistance = testDistance;
    		}
    	}
    	return closestUnit;
    }
    

    public static Direction GetWorkerReplicateDirection(Unit unit)
    {
    	for (Direction direction : Direction.values())
    		if ( gc.canReplicate(unit.id(), direction) )
    			return direction;
    	return null;
    }
    
    
    public static boolean TryMoveTowards(Unit unit, Unit target)
    {
    	Direction direction = unit.location().mapLocation().directionTo(target.location().mapLocation());
    	return TryBugMove(unit, direction);
    }
    
    public static boolean TryMoveLoose(Unit unit, Direction direction, int tolerance)
    {
    	if ( !gc.isMoveReady(unit.id()))
    		return false;
    	if ( TryMoveStrict(unit, direction))
    		return false;
    	Direction left = direction;
    	Direction right = direction;
    	for ( int ii = 1; ii < tolerance; ii++)
    	{
    		left = bc.bcDirectionRotateLeft(left);
    		right = bc.bcDirectionRotateRight(right);
    		if ( TryMoveStrict(unit, left) || TryMoveStrict(unit, right))
    			return true;
    	}
    	return false;
    }
    
    public static boolean TryMoveStrict(Unit unit, Direction direction)
    {
    	if ( gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), direction)) {
    		gc.moveRobot(unit.id(), direction);
    		return true;
    	}
    	return false;
    }
    
    
    public static boolean TryBugMove(Unit unit, Direction direction)
    {
    	if ( !gc.isMoveReady(unit.id()) )
    		return false;
    	
    	for ( int iterations = 0; iterations < 7; iterations++ )
    	{
			if ( gc.canMove(unit.id(), direction))
			{
				gc.moveRobot(unit.id(), direction);
				return true;
			}
			else 
			{
				direction = bc.bcDirectionRotateLeft(direction);
			}
    	}
    	return false;
    }
    
    public static boolean DoMoveTowardsEnemySpawn(Unit unit)
    {
    	PlanetMap map = gc.startingMap(Planet.Earth);
    	VecUnit initialUnits = map.getInitial_units();
    	Unit startingEnemy = null;
    	for(int ii = 0; ii < initialUnits.size(); ii ++)
    	{
    		Unit initialUnit = initialUnits.get(ii);
    		if ( initialUnit.team() == enemyTeam )
    		{
    			startingEnemy = initialUnit;
    			break;
    		}
    	}
    	Direction direction = unit.location().mapLocation().directionTo(startingEnemy.location().mapLocation());
    	if ( TryMoveLoose(unit, direction, 2) )
    	{
    		return true;
    	}
    	return DoRandomMove(unit);
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