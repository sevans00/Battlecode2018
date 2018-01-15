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
        
        friendlyTeam = gc.team();
		if (friendlyTeam == Team.Blue)
		    enemyTeam = Team.Red;
        

		//Setup Direction Fields:
		PlanetMap earthMap = gc.startingMap(Planet.Earth);
		VecUnit initialUnits = earthMap.getInitial_units();
		ArrayList<MapLocation> initialUnitLocations = new ArrayList<MapLocation>();
		for(int ii = 0; ii < initialUnits.size(); ii ++)
    	{
			if (initialUnits.get(ii).team() == enemyTeam) {
				initialUnitLocations.add(initialUnits.get(ii).location().mapLocation());
			}
    	}
		
		DirectionField startingUnitLocationField = new DirectionField(earthMap);
		startingUnitLocationField.SetupDirectionFieldTowards(initialUnitLocations);
		
		WeightedField initialKarbomiteField = new WeightedField(earthMap);
		initialKarbomiteField.SetupDirectionFieldTowardsKarbomite();
        
		
		gc.queueResearch(UnitType.Rocket);
		
		
		//Main Turn Loop:
        while (true) 
        {
        	
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
        	
        	ArrayList<Unit> friendlyUnitsOnMap = new ArrayList<Unit>();
        	ArrayList<Unit> friendlyBuildings = new ArrayList<Unit>();
        	
        	enemies = new ArrayList<Unit>();
            
            for (int ii = 0; ii < units.size(); ii++) 
            {
                Unit unit = units.get(ii);
                if ( unit.location().isOnMap() )
                	friendlyUnitsOnMap.add(unit);
                
            	if ( !unit.location().isOnMap() )
            		continue;
                
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
            //End basic units arraylist population
            
            //Sense enemies:
        	VecUnit enemyVecUnit = gc.units();
        	System.out.println("Enemies sensed:" + enemyVecUnit.size());
            for ( int ii = 0; ii < enemyVecUnit.size(); ii++ )
            {
            	Unit enemy = enemyVecUnit.get(ii);
            	if ( enemy.team() == enemyTeam && enemy.location().isOnMap() )
            		enemies.add(enemy);
            }
            
            //Enemy Field:
            DirectionField enemyField = new DirectionField(earthMap);
            enemyField.SetupDirectionFieldTowardsUnits(enemies);
            
            
            //Buildings:
            friendlyBuildings.addAll(factories);
            friendlyBuildings.addAll(rockets);
            //Building field:
            DirectionField friendlyBuildingField = new DirectionField(earthMap);
            friendlyBuildingField.SetupDirectionFieldTowardsUnits(friendlyBuildings);
            
            
            
            //Do catchup / initial section: (to be replaced by better stuff later)
            
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
				if ( workers.size() <= 1 && gc.canProduceRobot(unit.id(), UnitType.Worker) )
				{
					gc.produceRobot(unit.id(), UnitType.Worker);
                    System.out.println("produced an emergency worker!");
                    continue;
				}
				
				if ( Math.random() < 0.5f ){
					if ( gc.canProduceRobot(unit.id(), UnitType.Ranger))
					{
						gc.produceRobot(unit.id(), UnitType.Ranger);
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
            
            /*
            //Regular aggression:
            
            //Knights:
            for (Unit unit : knights) {
            	DoKnight(unit);
			}
            for ( Unit unit : rangers) {
            	DoRanger(unit);
            }
            */
            
            //Defensive:
            ArrayList<Unit> defensiveKnights = new ArrayList<Unit>();
            for( Unit unit : rangers )
            {
            	if ( unit.location().isOnMap() )
            		continue;
            	defensiveKnights.add(unit);
            }
            
            int distanceToKeepFromBuildings = 5;
            for ( Unit unit : defensiveKnights ) 
            {
            	Unit enemy = GetClosestUnitFrom(unit, enemies);
	        	if ( enemy == null )
	        	{
	        		//
	        		TryMoveToFieldDistance(unit, friendlyBuildingField, distanceToKeepFromBuildings);
	        		continue;
	        	}
	        	
	        	//How close is the enemy?
	        	int enemyDistance = (int) unit.location().mapLocation().distanceSquaredTo(enemy.location().mapLocation());
	        	if ( enemyDistance <= 50 + 2 || enemyDistance <= unit.rangerCannotAttackRange() )
	        	{
	        		//Charge!
		        	TryMoveTowards(unit, enemy); //TODO: Calculate this for all
	        	}
	        	else 
	        	{
	        		TryMoveToFieldDistance(unit, friendlyBuildingField, distanceToKeepFromBuildings);
	        	}
	        	
	        	if ( unit.location().mapLocation().isWithinRange(unit.attackRange(), enemy.location().mapLocation() ) )
	        	{
	        		if ( gc.isAttackReady(unit.id()) && gc.canSenseUnit(enemy.id()) && gc.canAttack(unit.id(), enemy.id()) )
	        		{
	        			System.out.println("Attack!!!! "+unit +" "+enemy);
	    				gc.attack(unit.id(), enemy.id());
	        		}
	        	}
	        	
            	
            }
            for ( Unit unit : rangers )
            {
            	
            }
            
            // Submit the actions we've done, and wait for our next turn.
            gc.nextTurn();
        }
    }
    
    public static boolean TryMoveToFieldDistance(Unit unit, DirectionField friendlyBuildingField, int distanceToKeepFromBuildings)
    {
    	MapLocation location = unit.location().mapLocation();
    	DirectionField.Entry entry = friendlyBuildingField.entries[location.getX()][location.getY()];
    	if ( entry.distance > distanceToKeepFromBuildings )
    		return TryBugMove(unit, entry.direction);
    	if ( entry.distance < distanceToKeepFromBuildings )
    		return TryBugMove(unit, bc.bcDirectionOpposite(entry.direction));
    	
    	return false;
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
    
    
    //Do miners
    public static void DoWorkers_Miners(ArrayList<Unit> workers_miners, WeightedField initialKarbomiteField)
    {
    	for ( Unit unit : workers_miners )
    	{
        	if ( !unit.location().isOnMap() )
        		continue;
    		
        	MapLocation location = unit.location().mapLocation();
        	WeightedField.Entry entry = initialKarbomiteField.entries[location.getX()][location.getY()];
        	if ( entry.direction != Direction.Center)
        	{
        		TryMoveLoose(unit, entry.direction, 2);
        	}
        	DoWorkerHarvest(unit);
    	}
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
    	//Might as well!
    	DoWorkerHarvest(unit);
    	
    	DoRandomMove(unit);
    	
    	return false;
    }
    
    public static boolean DoWorkerHarvest(Unit unit)
    {
    	if ( gc.canHarvest(unit.id(), Direction.Center))
		{
			gc.harvest(unit.id(), Direction.Center);
			return true;
		}
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