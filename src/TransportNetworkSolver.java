import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import ilog.concert.*;
import ilog.cplex.*;

public class TransportNetworkSolver{

	public static void main(String[] args) {
		
		File Orders = new File("Orders.txt");
		int timespan = 6;
		
		try (Scanner scanner = new Scanner(Orders)) { 
			int nrOfOrders = Integer.parseInt(scanner.nextLine());
			Order[] orderList = new Order[nrOfOrders];
	        for(int i = 0; i < nrOfOrders; i++){
	        	if(scanner.hasNextLine()) {
		           String[] string = scanner.nextLine().trim().split("\\s+");
		           Order order  = (new Order(Integer.parseInt(string[0]), Integer.parseInt(string[1]), Integer.parseInt(string[2]), 
               		               Integer.parseInt(string[3]), Integer.parseInt(string[4]), Integer.parseInt(string[5]),
               		               Integer.parseInt(string[6])));
		           orderList[i] = order;
	        	}
			}
	        int[] nodesList =  {0, 1, 2 ,3};
	        ArrayList<Arc> arcList = new ArrayList<Arc>();
	         
	  	   	// TRUCK arcs
	  	   	arcList.add(new Arc(0, 2, 15, 50, 0, 1, 100));
	  	   	arcList.add(new Arc(0, 3, 15, 50, 0, 1, 100));
	  	   	arcList.add(new Arc(2, 1, 15, 50, 0, 1, 100));
	  	   	arcList.add(new Arc(3, 2, 15, 50, 0, 1, 100));
	  	   	arcList.add(new Arc(1, 3, 15, 50, 0, 1, 100));
	  	   	// TRAIN arcs
	  	   	arcList.add(new Arc(0, 1, 15, 40, 1, 2, 60));
	  	    arcList.add(new Arc(0, 2, 15, 40, 1, 2, 60));
	  	   	arcList.add(new Arc(2, 3, 15, 40, 1, 2, 60));
	  	   	// Barge arcs
	  	   	arcList.add(new Arc(0, 1, 15, 30, 2, 3, 50));
	  	   	arcList.add(new Arc(1, 2, 15, 30, 2, 2, 50));
	  	   	arcList.add(new Arc(2, 3, 15, 30, 2, 2, 50));
	        
	 		solveSynchromodalityNetwork(timespan, orderList, arcList, nodesList); 
       } 
       catch(IOException e) {
    	   e.printStackTrace();
            } catch (NumberFormatException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		
		
		
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////
	
		public static void solveSynchromodalityNetwork(int timespan, Order[] orderList, ArrayList<Arc> arcList, int[] nodesList) {
	    	try{
			
	    		// define a new cplex object
	    		IloCplex cplex = new IloCplex();
	    		cplex.setOut(null);
			
	    		// declare decision variable x_iet, equal to 1 when load/order i is transported by TRUCK on link e at time t equal to 0 otherwise
	    		IloNumVar[][][] x = new IloNumVar[orderList.length][arcList.size()][timespan];
	    		for(int i = 0; i < orderList.length; i++) {
	    			for(int j = 0; j < arcList.size(); j++) {
	    				for(int t = 0; t < timespan; t++) {
	    					x[i][j][t] = cplex.numVar(0, 1, IloNumVarType.Int);
	    				}
	    			}
	    		}
	    					
			
	    		// declare decision variable x_iet, equal to 1 when load/order i is transported by TRUCK on link e at time t equal to 0 otherwise
	    		IloNumVar[][] y = new IloNumVar[arcList.size()][timespan];
	    		for(int j = 0; j < arcList.size(); j++) {
					for(int t = 0; t < timespan; t++) {
						y[j][t] = cplex.numVar(0, 1, IloNumVarType.Int); 
					}
				}
			
	    		// add objective function
	    		IloLinearNumExpr expr1 = cplex.linearNumExpr(); 
	    		for(int i = 0; i < orderList.length; i++) {
	    			for(int j = 0; j < arcList.size(); j++) {
	    				for(int t = 0; t < timespan; t++) {
	    					expr1.addTerm((orderList[i].orderVolume)*(arcList.get(j).variableCost), x[i][j][t]);
	    					expr1.addTerm(arcList.get(j).fixedCost, y[j][t]);
	    				}
	    			}
	    		}
	    		cplex.addMinimize(expr1);
	    		
	    		//constraint every order starts in source node
	    		for(int i = 0; i < orderList.length; i++) {
	    			IloLinearNumExpr expr2 = cplex.linearNumExpr();
	    			for(int j = 0; j < arcList.size(); j ++) {
	    				for(int t = (orderList[i].releaseTime); t <= ((orderList[i].LPA) - (arcList.get(j).duration)); t++) {
	    					if((arcList.get(j).from) == (orderList[i].origin)) {
	    						expr2.addTerm(x[i][j][t], 1.0);	
	    					}
	    				}
	    			}
	    			cplex.addEq(expr2, 1);
	    		}
			
	    		//constraint: every order needs to arrive in its destination, at the latest at its deadline
	    		for(int i = 0; i < orderList.length; i++) {
	    			IloLinearNumExpr expr3 = cplex.linearNumExpr(); 
	    			for(int j = 0; j < arcList.size(); j ++) {
	    				for(int t = (orderList[i].releaseTime); t <= ((orderList[i].LPA) - (arcList.get(j).duration)); t++) {
	    					if(arcList.get(j).to == orderList[i].destination) {
	    						expr3.addTerm(x[i][j][t], 1.0);
	    					}
	    				}
	    			}
	    			cplex.addEq(expr3, 1);
	    		}
			
	    		
				// node constraint: 3.	For every node that is not the source or the destination: 
				// order can only leave from this node at some time if order arrived at this time at the latest in this node and hasn't left yet.
	    	
	    		for(int i = 0; i < orderList.length; i++) {
					for(int n = 0; n < nodesList.length; n++) {
						if(n != orderList[i].origin && n != orderList[i].destination) {
							for(int t = 0; t < timespan; t++) {
								IloLinearNumExpr expr4 = cplex.linearNumExpr();
								IloLinearNumExpr expr5 = cplex.linearNumExpr();
								IloLinearNumExpr expr6 = cplex.linearNumExpr();
								for(int j = 0; j < arcList.size(); j++) {
									if(arcList.get(j).from == n) {
										expr4.addTerm(x[i][j][t], 1.0);
									}
								}
								for(int j = 0; j < arcList.size(); j++) {
									for(int t1 = 0; t1 <= t - arcList.get(j).duration; t1++) {
										if(arcList.get(j).to == n) {
											expr5.addTerm(x[i][j][t1], 1.0);
										}
									}
								} 
								for(int j = 0; j < arcList.size(); j++) {
									for(int t1 = 0; t1 <= t-1; t1++) {
										if(arcList.get(i).from == n ) {
											expr6.addTerm(x[i][j][t1], 1.0);
										}
									}
								}
								cplex.addLe(expr4, cplex.diff(expr5, expr6));
							}
						}
					}
				}
			    
				//linking and capacity constraint
	    		for(int j = 0; j < arcList.size(); j++) {
	    			for(int t = 0; t < timespan; t++) {
					 IloLinearNumExpr expr6 = cplex.linearNumExpr();
					 IloLinearNumExpr expr7 = cplex.linearNumExpr();
					    for(int i = 0; i < orderList.length; i++) {
						 expr6.addTerm(orderList[i].orderVolume, x[i][j][t]);
						 expr7.addTerm(arcList.get(j).capacity, y[j][t]);
					    }
					    cplex.addLe(expr6, expr7);
				    }
				}	 
	    		
	    		//Preferred mode of transportation constraint
	    		/* 
	    		for(int i = 0; i < orderList.length; i++) {
	    			 for(int t = 0; t < timespan; t++) {
	    				 for(int j = 0; j < arcList.size(); j++) {
	    					 IloLinearNumExpr expr8 = cplex.linearNumExpr();
	    					 if(arcList.get(j).mode != orderList[i].preferredMode) {
	    						 expr8.addTerm(x[i][j][t], 1.0);
	    					 }
	    					 cplex.addEq(expr8, 0);
	    				 }
	    			 }
	    		 }
	    		 */
			// solve ILP
			cplex.solve();
			/*for(int j = 0; j < arcList.size(); j++) {
    			for(int t = 0; t < timespan; t++) {
    				System.out.println(cplex.getValue(y[j][t]));
    			}
			}
			*/
			System.out.println( cplex.getObjValue()); 
			// close cplex object      
			cplex.close(); 
			}
		catch (IloException exc) {
			exc.printStackTrace();
		}
		}

	}
		
		

		
		
	     
	








