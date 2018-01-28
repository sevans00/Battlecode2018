// import the API.
// See xxx for the javadocs.
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import bc.*;



public class Player {
	
	
	public static GameController gc;
	
	public static Team friendlyTeam = Team.Red;
	public static Team enemyTeam = Team.Blue;
	
	public static Direction[] Direction_Orthogonals;
	
	public static ArrayList<Unit> factories = new ArrayList<Unit>();
	public static ArrayList<Unit> factoriesToHeal = new ArrayList<Unit>();
	public static ArrayList<Unit> healers = new ArrayList<Unit>();
	public static ArrayList<Unit> knights = new ArrayList<Unit>();
	public static ArrayList<Unit> mages = new ArrayList<Unit>();
	public static ArrayList<Unit> rangers = new ArrayList<Unit>();
	public static ArrayList<Unit> rockets = new ArrayList<Unit>();
	public static ArrayList<Unit> workers = new ArrayList<Unit>();
	
	public static ArrayList<Unit> enemies = new ArrayList<Unit>();
	public static ArrayList<MapLocation> enemyLocations = new ArrayList<MapLocation>();
	public static DirectionField enemyField;
	
    public static WeightedField initialKarbomiteField;
	
    public static void main(String[] args) {
    	Direction_Orthogonals = new Direction[] {Direction.North, Direction.East, Direction.South, Direction.West};
    	
    	
        // Connect to the manager, starting the game
        gc = new GameController();
        
        friendlyTeam = gc.team();
		if (friendlyTeam == Team.Blue)
		    enemyTeam = Team.Red;
		System.out.println("Friendly Team "+friendlyTeam + " Enemy: "+enemyTeam);
        
		
		//Setup Direction Fields:
		PlanetMap planetMap = gc.startingMap(gc.planet());
		VecUnit initialUnits = planetMap.getInitial_units();
		ArrayList<MapLocation> initialEnemyUnitLocations = new ArrayList<MapLocation>();
		ArrayList<MapLocation> initialFriendlyUnitLocations = new ArrayList<MapLocation>();
		for(int ii = 0; ii < initialUnits.size(); ii ++)
    	{
			if (initialUnits.get(ii).team() == enemyTeam) {
				initialEnemyUnitLocations.add(initialUnits.get(ii).location().mapLocation());
			} else {
				initialFriendlyUnitLocations.add(initialUnits.get(ii).location().mapLocation());
			}
    	}
		
		DirectionField startingUnitLocationField = new DirectionField(planetMap);
		startingUnitLocationField.SetupDirectionFieldTowards(initialEnemyUnitLocations);
		
		initialKarbomiteField = new WeightedField(planetMap);
		initialKarbomiteField.SetupDirectionFieldTowardsKarbomite(gc);
        
		
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
        	
            
            for (int ii = 0; ii < units.size(); ii++) 
            {
                Unit unit = units.get(ii);
                if ( unit.location().isOnMap() )
                	friendlyUnitsOnMap.add(unit);
                
            	//if ( !unit.location().isOnMap() )
            		//continue;
                
                switch (unit.unitType()) {
				case Factory:
					factories.add(unit);
					if ( unit.health() < unit.maxHealth() )
	            		factoriesToHeal.add(unit);
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
            enemies = new ArrayList<Unit>();
        	enemyLocations = new ArrayList<MapLocation>();
        	VecUnit enemyVecUnit = gc.units();
            for ( int ii = 0; ii < enemyVecUnit.size(); ii++ )
            {
            	Unit enemy = enemyVecUnit.get(ii);
            	if ( enemy.team() == enemyTeam && enemy.location().isOnMap() )
            	{
            		enemies.add(enemy);
            		enemyLocations.add(enemy.location().mapLocation());
            	}
            }
        	System.out.println("Enemies sensed:" + enemyLocations.size());
        	if ( enemyLocations.size() == 0 )
        	{
        		for ( int ii = 0; ii < initialEnemyUnitLocations.size(); ii++ )
        		{
        			enemyLocations.addAll(initialEnemyUnitLocations);
        		}
        	}
            
            //Enemy Field:
            enemyField = new DirectionField(planetMap);
            enemyField.SetupDirectionFieldTowards(enemyLocations);
            
            
            //Buildings:
            friendlyBuildings.addAll(factories);
            friendlyBuildings.addAll(rockets);
            //Building field:
            DirectionField friendlyBuildingField = new DirectionField(planetMap);
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
            
            //*
            //Minimum number of rockets:
            if ( gc.karbonite() > bc.bcUnitTypeBlueprintCost(UnitType.Rocket) && rockets.size() < 2 && gc.planet() == Planet.Earth)
            {
            	for (Unit unit : workers)
                {
            		Direction direction = getCanBlueprintDirection(unit, UnitType.Rocket);
            		if ( 	direction != null  
            				&& gc.canBlueprint(unit.id(), UnitType.Rocket, direction) )
            		{
            			gc.blueprint(unit.id(), UnitType.Rocket, direction);
            			break; //No reason why we can't build more
            		}
                }
            }
            //*/
            //Rockets:
            for ( Unit unit : rockets )
            {
            	if ( unit.health() < unit.maxHealth() )
            		factoriesToHeal.add(unit);
            	
            	if ( gc.planet() == Planet.Earth)
            	{
            		//Launch!
	            	if ( unit.structureGarrison().size() > 0 )
	            	{
	            		System.out.println("GARISONED UNIT!");
	            		MapLocation destination = GetLandingZoneOnMars();
	            		if ( gc.canLaunchRocket(unit.id(), destination) )
	            		{
	            			System.out.println("LAUNCH ROCKET!");
	            			gc.launchRocket(unit.id(), destination);
	            		}
	            	}
	            	else {
	            		//Load a worker
	                	Unit rocket = rockets.get(0);
	                	Unit worker = GetClosestUnitFrom(rocket, workers);
	                	if ( worker != null )
	                	{
	    	            	TryMoveTowards(worker, rocket);
	    	            	if ( gc.canLoad(rocket.id(), worker.id()) )
	    	            	{
	    	            		gc.load(rocket.id(), worker.id());
	    	            	}
	                	}
	            	}
            	} else {
    				VecUnitID garrison = unit.structureGarrison();
    				if ( garrison.size() > 0 )
    				{
    					Direction freeDirection = getNextUnloadDirection(unit);
    					if ( freeDirection != null )
    					{
    						System.out.println("Unloaded a unit ON MARS!");
    						gc.unload(unit.id(), freeDirection);
    					}
    				}
            	}
            }
            
            
            
            

            //Factory Blueprint:
            if (gc.planet() == Planet.Earth) //Can only build factories on earth
            {
	            //Worker Roles:
	            ArrayList<Unit> workers_builders = new ArrayList<Unit>();
	            ArrayList<Unit> workers_miners = new ArrayList<Unit>();
	            if ( factories.size() < 4) //Get 4 up and running ASAP //TODO: More
	            {
	            	for(int ii = 0; ii < workers.size(); ii++)
	            	{
	            		if ( ii < workers.size() / 2) {
	            			workers_builders.add(workers.get(ii));
	            		} else {
	            			workers_miners.add(workers.get(ii));
	            		}
	            	}
	            } else {
	            	for(int ii = 0; ii < workers.size(); ii++)
	            	{
	            		if ( ii <= 2) {
	            			workers_builders.add(workers.get(ii));
	            		} else {
	            			workers_miners.add(workers.get(ii));
	            		}
	            	}
	            }
	            
            	MapLocation factoryStartPoint = initialFriendlyUnitLocations.get(0);
            	
            	DoWorkers_Miners(workers_miners);
            	DoWorkers_Builders(planetMap, workers_builders, factoryStartPoint, factories, rockets, friendlyBuildingField);
            	
            	initialKarbomiteField.DebugPrintDistances();
            	initialKarbomiteField.DebugPrintDirections();
            }
            
            
            
            
            
            //Factories:
            for (Unit unit : factories) 
            {
            	//Ungarrison:
				VecUnitID garrison = unit.structureGarrison();
				if ( garrison.size() > 0 )
				{
					Direction freeDirection = getNextUnloadDirection(unit);
					if ( freeDirection != null )
					{
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
            
            
            /*
            //OLD Workers:
            for (Unit unit : workers) 
            {
            	DoWorker(unit);
            }
            */
            
            //*
            //Aggression:
            
            //Knights:
            for (Unit unit : knights) {
            	Unit enemy = GetClosestUnitFrom(unit, enemies);
            	DoRanger(unit, enemies, enemyField, enemy);
			}
            for ( Unit unit : rangers) {
            	Unit enemy = GetClosestUnitFrom(unit, enemies);
            	DoRanger(unit, enemies, enemyField, enemy); //Target todo
            }
            
            //enemyField.DebugPrintDistances();
            
            
            /*/
            
            
            
            //Defensive:
            ArrayList<Unit> defensiveKnights = new ArrayList<Unit>();
            for( Unit unit : rangers )
            {
            	if ( !unit.location().isOnMap() )
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
	        	TryMoveToFieldDistance(unit, friendlyBuildingField, distanceToKeepFromBuildings);
	        	
	        	
	        	if ( unit.location().mapLocation().isWithinRange(unit.attackRange(), enemy.location().mapLocation() ) )
	        	{
	        		if ( gc.isAttackReady(unit.id()) && gc.canSenseUnit(enemy.id()) && gc.canAttack(unit.id(), enemy.id()) )
	        		{
	    				gc.attack(unit.id(), enemy.id());
	        		}
	        	}
	        	
            	
            }
            for ( Unit unit : rangers )
            {
            	
            }
            */
            
            // Submit the actions we've done, and wait for our next turn.
            gc.nextTurn();
        }
    }
    
    
    //Move along field:
    public static boolean TryMoveAlongField(Unit unit, DirectionField field, Unit target, long minDistance)
    {
    	if ( !unit.location().isOnMap() )
    		return false;
    	if ( target == null )
    		return TryMoveAlongField(unit, field);
    	
    	long distance = unit.location().mapLocation().distanceSquaredTo(target.location().mapLocation());
    	if (distance < minDistance)
    	{
    		DirectionField.Entry entry = field.getEntryAt(unit);
        	if ( TryMoveStrict(unit, bc.bcDirectionOpposite(entry.direction) ) )
        		return true;
        	ArrayList<DirectionField.Entry> entries = field.getAdjacentEntriesTo(unit);
        	for(DirectionField.Entry testEntry : entries )
        	{
        		Direction direction = unit.location().mapLocation().directionTo(testEntry.mapLocation);
        		if ( testEntry.distance >= entry.distance )
        		{
        			boolean result = TryMoveStrict(unit, direction);
        			if ( result )
        				return true;
        		}
        	}
    	}
    	
    	DirectionField.Entry entry = field.getEntryAt(unit);
    	if ( TryMoveStrict(unit, entry.direction) )
    		return true;
    	ArrayList<DirectionField.Entry> entries = field.getAdjacentEntriesTo(unit);
    	for(DirectionField.Entry testEntry : entries )
    	{
    		Direction direction = unit.location().mapLocation().directionTo(testEntry.mapLocation);
    		if ( testEntry.distance <= entry.distance )
    		{
    			boolean result = TryMoveStrict(unit, direction);
    			if ( result )
    				return true;
    		}
    	}
    	
    	/*
    	for(DirectionField.Entry testEntry : entries )
    	{
    		Direction direction = unit.location().mapLocation().directionTo(testEntry.mapLocation);
    		if ( testEntry.distance <= entry.distance )
    		{
    			TryMoveStrict(unit, direction);
    		}
    	}*/
    	return false;
    }
    
    
    public static boolean TryMoveAlongField(Unit unit, DirectionField field)
    {
    	if ( !unit.location().isOnMap() )
    		return false;
    	
    	DirectionField.Entry entry = field.getEntryAt(unit);
    	if ( TryMoveStrict(unit, entry.direction) )
    		return true;
    	ArrayList<DirectionField.Entry> entries = field.getAdjacentEntriesTo(unit);
    	for(DirectionField.Entry testEntry : entries )
    	{
    		Direction direction = unit.location().mapLocation().directionTo(testEntry.mapLocation);
    		if ( testEntry.distance < entry.distance )
    		{
    			TryMoveStrict(unit, direction);
    		}
    	}
    	
    	/*
    	for(DirectionField.Entry testEntry : entries )
    	{
    		Direction direction = unit.location().mapLocation().directionTo(testEntry.mapLocation);
    		if ( testEntry.distance <= entry.distance )
    		{
    			TryMoveStrict(unit, direction);
    		}
    	}*/
    	return false;
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
    
    
    public static boolean DoKnight(Unit unit, ArrayList<Unit> enemies, DirectionField enemyField, Unit enemy)
    {
    	if ( !unit.location().isOnMap() )
    		return false;
    	
    	DirectionField.Entry entry = enemyField.getEntryAt(unit);
    	TryMoveAlongField(unit, enemyField, enemy, 0); //TODO: Tweak distance

    	if ( enemy == null)
    		return false;
    	
    	if ( unit.location().mapLocation().isWithinRange(unit.attackRange(), enemy.location().mapLocation() ) )
    	{
    		if ( gc.isAttackReady(unit.id()) && gc.canSenseUnit(enemy.id()) && gc.canAttack(unit.id(), enemy.id()) )
    		{
				gc.attack(unit.id(), enemy.id());
    		}
    	}
    	
    	return false;
    }

    public static boolean DoRanger(Unit unit, ArrayList<Unit> enemies, DirectionField enemyField, Unit enemy)
    {
    	if ( !unit.location().isOnMap() )
    		return false;
    	
    	if ( enemy != null && unit.location().mapLocation().isWithinRange(unit.attackRange(), enemy.location().mapLocation() ) )
    	{
    		if ( gc.isAttackReady(unit.id()) && gc.canSenseUnit(enemy.id()) && gc.canAttack(unit.id(), enemy.id()) )
    		{
				gc.attack(unit.id(), enemy.id());
				return true;
    		}
    	}
    	TryMoveAlongField(unit, enemyField, enemy, 0); //TODO: Tweak distance
    	
    	return false;
    }
    
    
    
    
    
     //BASIC::::::::::::::::::::
    //Do one Knight
    public static boolean DoBasicKnight(Unit unit)
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
    //Basic
    public static boolean DoBasicRanger(Unit unit)
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
    public static void DoWorkers_Miners(ArrayList<Unit> workers_miners)
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
        	//Rescan:
        	DoRescanOfKarbomite();
    	}
    }
    public static void DoRescanOfKarbomite()
    {
    	initialKarbomiteField.SetupDirectionFieldTowardsKarbomite(gc);
    }
    
    //Do Builders
    public static void DoWorkers_Builders(
    		PlanetMap map,
    		ArrayList<Unit> workers, 
    		MapLocation initialFactoryLocation, 
    		ArrayList<Unit> factories, 
    		ArrayList<Unit> rockets, 
    		DirectionField friendlyBuildingField)
    {
    	for ( Unit unit : workers )
    	{
    		if ( !unit.location().isOnMap() )
    			continue;
    		
    		//Is there a factory we need to build?
        	Unit closestFactoryNeedsHealing = GetClosestUnitFrom(unit, factoriesToHeal);
        	if ( closestFactoryNeedsHealing != null )
        	{
        		if ( unit.location().mapLocation().isWithinRange(unit.abilityRange(), closestFactoryNeedsHealing.location().mapLocation()) )
        		{
        			DoWorkerRepairBuild(unit, closestFactoryNeedsHealing);
        		}
        		else
        		{
        			TryMoveTowards(unit, closestFactoryNeedsHealing);
        			DoWorkerRepairBuild(unit, closestFactoryNeedsHealing);
        		}
        		continue;
        	}
    		
    		MapLocation nextFactoryLocation = GetNextFactoryLocation(map, initialFactoryLocation, factories);
    		if ( nextFactoryLocation != null )
    		{
    			TryMoveNextTo(unit, nextFactoryLocation);
    			Direction direction = unit.location().mapLocation().directionTo(nextFactoryLocation);
    			long distance = unit.location().mapLocation().distanceSquaredTo(nextFactoryLocation);
    			if ( distance == 1 || distance == 2)
    			{
    				if ( gc.canBlueprint(unit.id(), UnitType.Factory, direction) )
    				{
    					gc.blueprint(unit.id(), UnitType.Factory, direction);
    				}
    			}
    		}
    		else
    		{
    			System.out.println("Could not find a next factory location!");
    		}
    	}
    	
    }
    public static MapLocation GetNextFactoryLocation(PlanetMap map, MapLocation initialFactoryLocation, ArrayList<Unit> factories)
    {
    	if ( factories.size() == 0 )
    		return initialFactoryLocation;
    	
    	//Start evaluating locations based on distance:
    	ArrayList<MapLocation> potentialFactoryLocations = new ArrayList<MapLocation>();
    	Unit unitAtTestLocation = null;
    	
    	MapLocation testLocation = null;
    	for ( Unit factory : factories )
    	{
    		for ( Direction direction : Direction_Orthogonals)
    		{
    			testLocation = factory.location().mapLocation().add(direction).add(direction);
    			
    			if (!map.onMap(testLocation) )
    				continue;
    			if ( map.isPassableTerrainAt(testLocation) == 0 )
    				continue;
    			if ( !gc.canSenseLocation(testLocation) )
    				continue;
    			if ( gc.hasUnitAtLocation(testLocation))
    				continue;
    			/*
    			if ( gc.canSenseLocation(testLocation) && gc.hasUnitAtLocation(testLocation) )
    			{
    				System.out.println("\t  unit exists");
	    			unitAtTestLocation = gc.senseUnitAtLocation(testLocation);
	    			if (unitAtTestLocation != null || unitAtTestLocation.unitType() == UnitType.Factory)
	    				continue;
    			}*/
    			return testLocation;
    		}
    	}
    	return null;
    }
    
    
    //Do one Worker - old
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
    	if ( units.size() <= 0 || !unit.location().isOnMap() )
    		return null;
    	
    	Unit closestUnit = null;
    	long closestDistance = Long.MAX_VALUE;
    	Unit testUnit;
    	long testDistance;
    	for( int ii = 0; ii < units.size(); ii++ )
    	{
    		testUnit = units.get(ii);
    		if ( !testUnit.location().isOnMap() )
    			continue;
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
    
    public static boolean TryMoveNextTo(Unit unit, MapLocation target)
    {
    	if ( !unit.location().isOnMap() )
    		return false;
    	Direction direction = unit.location().mapLocation().directionTo(target);
    	long distance = unit.location().mapLocation().distanceSquaredTo(target);
    	if ( distance == 0 )
    	{
    		for( Direction testDirection : Direction.values() )
    		{
    			if ( TryMoveLoose(unit, testDirection, 2) )
    				return true;
    		}
    	}
    	if ( distance == 1 || distance == 2 )
    		return true;
    	TryMoveLoose(unit, direction, 2);
    	return false;
    }
    
    public static boolean TryMoveLoose(Unit unit, Direction direction, int tolerance)
    {
    	if ( !gc.isMoveReady(unit.id()))
    		return false;
    	if ( TryMoveStrict(unit, direction))
    		return false;
    	Direction left = direction;
    	Direction right = direction;
    	for ( int ii = 0; ii < tolerance; ii++)
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
    	PlanetMap map = gc.startingMap(gc.planet());
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
    
    public static MapLocation GetLandingZoneOnMars()
    {
    	PlanetMap mars = gc.startingMap(Planet.Mars);
    	for(int ii = 0; ii < mars.getWidth(); ii++ )
    	{
    		for(int jj = 0; jj < mars.getHeight(); jj++ )
    		{
    			MapLocation location = new MapLocation(Planet.Mars, ii, jj);
    			if ( mars.isPassableTerrainAt(location) == 1 )
    			{
    				return location;
    			}
    		}
    	}
    	return null;
    }
    
    
    
}