/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.sdn.policies.vmallocation;

import java.lang.*;
import java.util.*;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.sdn.monitor.power.PowerUtilizationMaxHostInterface;

/**
 * 
 * 
 *    
 *  
 * @author Jungmin Son
 *Fatemeh msy
 * @since CloudSimSDN 1.0
 */
public class VmAllocationPolicyMemVNFP extends VmAllocationPolicy implements PowerUtilizationMaxHostInterface {

	protected final double hostTotalMips;
	protected final double hostTotalBw;
	protected final int hostTotalPes;
	

	/** The vm table. */
	private Map<String, Host> vmTable;

	/** The used pes. */
	private Map<String, Integer> usedPes;

	/** The free pes. */
	private List<Integer> freePes;

	private Map<String, Long> usedMips;
	private List<Long> freeMips;
	private Map<String, Long> usedBw;
	private List<Long> freeBw;

	//Number of algorithm's iteration for allocation 
	private int numberOfIterator=1;

	//One-Dimensional Arrays with 10 memory location 
	//9 location for server and 1 location for fitness
	private int chromoLength=10;

	//[server0, server1, ... server9, fitness]	
	private int fitnessLocation=chromoLength-1;	

	//Population size has to be iqual with number of host in the datacenter
	
	private int popSize=100;

	private String [][] population1 = new String[popSize][chromoLength];	
	private String [][] population2 = new String[popSize][chromoLength];		

	/**
	 * Creates the new VmAllocationPolicySimple object.
	 *
	 * @param list the list
	 *
	 * @pre $none
	 * @post $none
	 */
	public VmAllocationPolicyMemVNFP(List<? extends Host> list) {
		super(list);

		setFreePes(new ArrayList<Integer>());
		setFreeMips(new ArrayList<Long>());
		setFreeBw(new ArrayList<Long>());
		//setFreeRam(new ArrayList<Long>());
		for (Host host : getHostList()) {
			getFreePes().add(host.getNumberOfPes());
			getFreeMips().add((long)host.getTotalMips());
			getFreeBw().add(host.getBw());

		}	
		if(list == null || list.size() == 0)
		{
			hostTotalMips = 0;
			hostTotalBw =  0;
			hostTotalPes =  0;
		} else {
			hostTotalMips = getHostList().get(0).getTotalMips();
			hostTotalBw =  getHostList().get(0).getBw();
			hostTotalPes =  getHostList().get(0).getNumberOfPes();
		}

		setVmTable(new HashMap<String, Host>());
		setUsedPes(new HashMap<String, Integer>());
		setUsedMips(new HashMap<String, Long>());
		setUsedBw(new HashMap<String, Long>());
	}
	
	protected double convertWeightedMetric(double mipsPercent, double bwPercent) {
		double ret = mipsPercent * bwPercent;
		return ret;
	}

	/**
	 * Allocates a host for a given VM.
	 *
	 * @param vm VM specification
	 *
	 * @return $true if the host could be allocated; $false otherwise
	 *
	 * @pre $none
	 * @post $none
	 */
	@Override
	public boolean allocateHostForVm(Vm vm) {
		//if (getVmTable().containsKey(vm.getUid())) { // if this vm was not created
			//return false;
		//}
		
		int numHosts = getHostList().size();
		int requiredPes = vm.getNumberOfPes(); 
		double requiredMips = vm.getCurrentRequestedTotalMips();
		long requiredBw = vm.getCurrentRequestedBw();

		int tries = 0;
		boolean result = false;
		List<Double> freeResources = new ArrayList<Double>(numHosts);
		
		for (int i = 0; i < numHosts; i++) {
		//for (Integer freePes : getFreePes()) {
			//freePesTmp.add(freePes);
			double mipsFreePercent = (double)getFreeMips().get(i) / this.hostTotalMips; 
			double bwFreePercent = (double)getFreeBw().get(i) / this.hostTotalBw;
			freeResources.add(this.convertWeightedMetric(mipsFreePercent, bwFreePercent));
		}
		if (!getVmTable().containsKey(vm.getUid())) { //if this vm was not created

			do {//we still trying until we find a host or until we try all of them
				double lessFree = Double.POSITIVE_INFINITY;
				int idx = -1;

			
		//for(int tries = 0; result == false && tries < numHosts; tries++) {// we still trying until we find a host or until we try all of them
		
				//double lessFree = Double.POSITIVE_INFINITY;
				//int idx = -1;

				//Initializes a population based on the number of  
				//hosts that have a free CPUs
				
				initialization(freeResources);
				try {

					//System.out.println("---");
					//					Thread t = new Thread();			
					//					t.sleep(5000);
					
				} catch (Exception e){}

				
				int i=1;				
				while (i<=numberOfIterator){

					//Selection parents to produce an individul with greater fitness to 						//allocate
					
					selection();

					//Reproduce new population
					reproduction();

					//selection parents for mutation and crossover
					//variation();

					
					//Evaluate each individual which has a greater fitness
					
					idx=evaluate(freeResources);

					//change parents with individuals which have a greater fitness
					replacePopulation();


					i=i+1;
				}//End while
				try {
					System.out.println("Host is selected: " + idx);
					//					Thread t = new Thread();			
					//					t.sleep(5000);
				} catch (Exception e){}

				if (idx!=-1){
									
					Host host = getHostList().get(idx);
					result = host.vmCreate(vm);

				/*freeResources[idx] = Double.POSITIVE_INFINITY;
				Host host = getHostList().get(idx);
			

				// Check whether the host can hold this VM or not.
				if( getFreeMips().get(idx) < requiredMips) {
					System.err.println("not enough MIPS:"+getFreeMips().get(idx)+", req="+requiredMips);
					//Cannot host the VM
					continue;
				}
				if( getFreeBw().get(idx) < requiredBw) {
					System.err.println("not enough BW:"+getFreeBw().get(idx)+", req="+requiredBw);
					//Cannot host the VM
					//continue;
				}
			
				result = host.vmCreate(vm);*/

					if (result) { // if vm were succesfully created in the host
						Log.printLine("VmAllocationPolicyMA: VM #"+vm.getId()+ " Chosen host: #"+host.getId()+" idx:"+idx);
						Log.printLine("---Chosen host: #"+host.getId()+" idx:"+idx);
						try {
						} catch(Exception exc){
						}
						getVmTable().put(vm.getUid(), host);
						getUsedPes().put(vm.getUid(), requiredPes);
						getFreePes().set(idx, getFreePes().get(idx) - requiredPes);
						result = true;
					/*getUsedMips().put(vm.getUid(), (long) requiredMips);
					getFreeMips().set(idx,  (long) (getFreeMips().get(idx) - requiredMips));

					getUsedBw().put(vm.getUid(), (long) requiredBw);
					getFreeBw().set(idx,  (long) (getFreeBw().get(idx) - requiredBw));*/

						break;
					} else {
						freeResources.set(idx, Double.POSITIVE_INFINITY);
					}
				}//End if
				tries++;
				} while (!result && tries < getFreePes().size());			
		
			}
			
	//return result;
		if(!result) {
			System.err.println("VmAllocationPolicy: WARNING:: Cannot create VM!!!!");
		}
		logMaxNumHostsUsed();
		return result;
	}
	
	protected int maxNumHostsUsed=0;
	public void logMaxNumHostsUsed() {
		// Get how many are used
		int numHostsUsed=0;
		for(int freePes:getFreePes()) {
			if(freePes < hostTotalPes) {
				numHostsUsed++;
			}
		}
		if(maxNumHostsUsed < numHostsUsed)
			maxNumHostsUsed = numHostsUsed;
		System.out.println("Number of online hosts:"+numHostsUsed + ", max was ="+maxNumHostsUsed);
	}
	public int getMaxNumHostsUsed() { return maxNumHostsUsed;}

	public void printPopulation(String [][] population1){

		int i=0;
		int j=0;
		while (i<population1.length){

			while (j<chromoLength){
				//population1[i][j]="0";

				System.out.print(population1[i][j] + " ");
				j++;

			}

			System.out.println();

			i++;
			j=0;
		}

	}

	
	public void replacePopulation(){

		int i=0;
		int j=0;
		while (i<popSize){
			j=0;
			while (j<chromoLength){
				population1[i][j]=population2[i][j];
				j=j+1;
			}//End while
			i=i+1;
		}//End while

	}//End population
		

	/*public void setPopSize(int newPop){
		popSize=newPop;
	}*/

	public void initialization(List<Double> freeResources){
		 
		//Number of hosts in the datacenter
		//population1 = new String[popSize][chromoLength];
		//population2 = new String[popSize][chromoLength];
		int numHosts=freeResources.size();
		System.out.println("Population Size:" + popSize);
		//servers are a sequences of chromosome and each chromosome is an individual of the population
		// and each population has a random part of the search space 
		// a random part of the search space
		// chromosome = [server1, server2 ..., fitness of the search space]		
		int i=0;
		int j = 0;
		int server=1;
		
		while(i<popSize) {

			int host=1;
			
			j=0;
			while (j<chromoLength-1){
				host = server;
				
				population1[i][j]=host + "";
				j++;
				// Check if we reached to the end of the search space
				if (server==numHosts)
				// back to the start point
					server=1;
				else
					server=server+1;
					
			}//End while
			

			//update the fitness of the individual
			population1[i][fitnessLocation]="0";
			updateFitnessPopulation(freeResources,i);

			//next individual
			i++;

		}//End while		

	}//End initialization
	
	// update fitness value of the population1
	public void updateFitnessPopulation(List<Double> freeResources, int chromoIndex){

		int [] evaluated = new int[chromoLength];
		int i=0;
		while (i<chromoLength){
			evaluated[i]=0;
			i=i+1;
		}//End while

		//each chromosome is a list of server
		//and each server has a special fitness value
		// need list of servers that is evaluated in order to avoid a repeat        

		
		int j=0;
		int host=1;	
		boolean tried=false;
		//find index of servers that is evaluated
		int k=0;
		while (j<chromoLength-1){
			//put index of the server on the chromosome
			host = Integer.parseInt(population1[chromoIndex][j]); 
			// if the index of the server is not in the evaluted list 
			//Calculate fitness 

			tried = checkRepetition(host,evaluated);
			if (!tried){
				population1[chromoIndex][fitnessLocation]=(Double.parseDouble(population1[chromoIndex][fitnessLocation])+calcFitness(host,freeResources))+"";

				//add on the evaluated list
				evaluated[k]=host;
				k=k+1;
			}//End if

			//evalute next host 
			j=j+1;
		}//End while

	}//End updateFitnessPopulation

	public void updateFitnessPopulation2(List<Double> freeResources, int chromoIndex){

		int [] evaluated = new int[chromoLength];
		int i=0;
		while (i<chromoLength){
			evaluated[i]=0;
			i=i+1;
		}//End while
	
		int j=0;
		int host=1;	
		boolean tried=false;
		//find index of servers that is evaluated
		int k=0;
		while (j<chromoLength-1){
			//put index of the server on the chromosome
			host = Integer.parseInt(population2[chromoIndex][j]); 

			// if the index of the server is not in the evaluted list 
			//calculate fitness 
//v_posString =  v_posString.replace(",",".");
			tried = checkRepetition(host,evaluated);
			if (!tried){
				//population2[chromoIndex][fitnessLocation]=(population2[chromoIndex][fitnessLocation]).replace(",",".");
				population2[chromoIndex][fitnessLocation]=(Double.parseDouble(population2[chromoIndex][fitnessLocation])+calcFitness(host,freeResources))+"";

				//add on the evaluated list
				evaluated[k]=host;
				k=k+1;
			}//End if

			//next individual
			j=j+1;
		}//End while

	}//updateFitnessPopulation2
	
	//Calculate fitness
	
	

	public boolean checkRepetition(int host, int [] evaluated){

		boolean tried=false;
		int i=1;		
		while (evaluated[i]!=0 & i<=chromoLength-1 & tried==false){
			if (evaluated[i]!=host) 
				i=i+1;
			else
				tried=true;
		}//End while

		return tried;

	}//End of checkRepetition
	
	public double calcFitness(int host, List<Double> freeResources){

		double fitnessValue = 0;
		// Checks if CPUs are available on selected host
		//-1 (list of hosts start with 0 --> [host0, host1,..., hostn])
		fitnessValue = freeResources.get(host-1) + 1 ; 
		return fitnessValue;

	}//End of calcFitness


	public void selection(){
	//Select Parents
			
		 	

		String [] roulette = new String[popSize];
		//need a matrice which has a value of the 
		//roulette, so we need to generate the roulette
		generateRoulette(roulette);

		int i=0;
		int j=0;
		double p=0;
		int bestIndividual=0;
		while (i<popSize){
			//probability of each chromosome that is selected
			p = (double) Math.random();

			bestIndividual = chromoRoulette(roulette,p);

			//Create array with selectioned chromosome for reproduction
			j=0; 
			while (j<chromoLength){
				population2[i][j] = population1[bestIndividual][j];
				j++;
			}//End while

			i++;
		}//End while

	}//End of selection

	public void reproduction(){

				

		//for example, consider the crossover point to be 3
		//population1 = | 0 | 0 | 0 | 0 | 0 | 0 |... | fitness |
		//    		| 1 | 1 | 1 | 1 | 1 | 1 |... | fitness |
		//...
		//offspring would create by exchanging the genes of parents until 
		//the crossover point is reached.
		//
		//population2 = | 1 | 1 | 1 | 0 | 0 | 0 |... | fitness |
		//    		| 0 | 0 | 0 | 1 | 1 | 1 |... | fitness |

		//we have to consider the index of the chromosome that is started from zero and the final index is equal 			   	with the chromosome length
		int firstIndex = 0;
		int lastIndex = chromoLength;

		int crossoverPoint=0;
		
		int i=0;
		int j=0;
		String temp1="0";
		String temp2="0";
		while (i<population2.length) {
			//a crossover point is chosen at random from within the genes.
			crossoverPoint = (int) Math.round(firstIndex + Math.random() * lastIndex);
			while (crossoverPoint < lastIndex){

				temp1 = population2[i][crossoverPoint];
				temp2 = population2[i+1][crossoverPoint];

				population2[i][crossoverPoint] = temp2;
				population2[i+1][crossoverPoint] = temp1;

				crossoverPoint++;
			}//End while
			i+=2;
		}//End while

	}//End reproduction

	/*public void variation(){
		int point1=0;
		int point2=0;
		String tmp1 ="0";
		String tmp2 ="0";
		int firstIndex = 0;
		int lastIndex = chromoLength;
		for ( int i =0; i < population2.length; i++){
			point1 = (int) Math.round(firstIndex + Math.random() * lastIndex);
			point2 = (int) Math.round(firstIndex + Math.random() * lastIndex);

			tmp1 = population2[i][point1];
			tmp2 = population2[i][point2];
			if (tmp1 == "0")
				tmp1="1";
			else
				tmp1="0";
			if (tmp2 =="0")
				tmp2="1";
			else
				tmp2="0";
			population2[i][point1]=tmp2+"";
			population2[i][point2]=tmp1+"";
		}//End for
	}//End variation*/
	

	public int evaluate(List<Double> freeResources){

		//index of the host that is selected
		int idx=-1;
		//update the fitness of population2
		int i=0;
		while (i<population2.length){			
			population2[i][fitnessLocation]="0";
			updateFitnessPopulation2(freeResources,i);
			i=i+1;
		}//End while

		sortPopulation();

		i= 0;
		int host=i;
		boolean indvIsFound=false;
		while ( i< popSize && indvIsFound==false) {
			sortIndividual(i,freeResources);

			int j = 0;
			double lessFree = Double.POSITIVE_INFINITY;
			idx = -1;

			while (j<chromoLength-1){
				host = Integer.parseInt(population2[i][j]) -1;
				if (freeResources.get(host) < lessFree) {
					lessFree = freeResources.get(host);
					idx = host;
				} //End if
				j = j+1;
			}//End while
			i = i+1;
		}//End while
		return idx;

	}//End of evaluate

	public void generateRoulette(String [] roulette){

		double sum=0;
		double strength=0;

		int i=0;
		while (i<popSize){
			
			strength = Double.parseDouble(population1[i][fitnessLocation]);
			sum += strength;

			i++;
		}//End while

		double prev=0;
		double probability=0;

		i=0;
		while (i<popSize){
			population1[i][fitnessLocation]= (population1[i][fitnessLocation]).replace(",",".");
			//v_posString =  v_posString.replace(",",".");
//int v_CurrentPosX = Math.round(Float.parseFloat(v_posString));
			strength = Double.parseDouble(population1[i][fitnessLocation]);
			probability = prev + strength/sum;
			roulette[i] = probability + "";
			prev = probability;
			i++;

		}//End while

	}//End generateRoulette

	public int chromoRoulette(String [] roulette, double p){
		double prev=0;
		boolean isFound=false;
		int bestIndividual=0;

		int i=0;
		while (i<roulette.length && !isFound){
			population1[i][fitnessLocation]= (population1[i][fitnessLocation]).replace(",",".");

			if (p>=prev && p <= Double.parseDouble(roulette[i])){
				bestIndividual = i;
				isFound=true;
			} else
				prev = Double.parseDouble(roulette[i]);

			i++;
		}//End while

		return bestIndividual;
		

	}//End chromoRoulette

//--------------------------------SORT-----------------------------------
	public void sortPopulation() {
		for (int t=1; t<popSize; t++) 
			for (int i=0; i<popSize-1; i++) 		
				if (Double.parseDouble(population2[i][fitnessLocation]) < Double.parseDouble(population2[i+1][fitnessLocation]))
					copyPop(i,i+1);
		/*int k=0;
		int j=0;
		//Selection sort
		for(int i=0; i<popSize-1; i++){
			k=i;
			for(j=i+1; j<popsize; j++){
				//float.parsfloat to retrn the float value
				if(Float.parseFloat(newPopulation[j][fitnessLocation])>Float.parseFloat(population2[k][fitnessLocation])){
					k=j;
				}
				copyPop(population2[i], tempPop);
				copyPop(population2[k], population2[i]);
				copyPop(tempPop, population2[k]);
			}*/

		
	}//End sortPopulation

	public void copyPop(int i1, int i2){

		String [] aux = new String[chromoLength];

		int i=0;
		while(i<chromoLength){
			aux[i]=population2[i1][i];
			i++;
		}//End while

		i=0;
		while(i<chromoLength){
			population2[i1][i] = population2[i2][i];
			i++;
		}//End while

		i=0;
		while(i<chromoLength){
			population2[i2][i] = aux[i];
			i++;
		}//End while

	}//End copyPop

	/*public void copyPop(int m, int n){

		String [] tmp = new String[chromoLength];

		//int i=0;
		for(int i = 0; i<chromoLength; i++){
			tmp[i]=population2[m][i];
		}

		for(int i = 0; i<chromoLength; i++){
			population2[m][i] = population2[n][i];
		}

		//int i=0;
		for(int i = 0; i<chromoLength; i++){
			population2[n][i] = tmp[i];
		}

	}// end copyPop */

	public void sortIndividual(int chromoIndex,List<Double> freeResources){

		int host1=1;
		int host2=1;
		for (int t=1; t<chromoLength-1; t++) 
			for (int i=0; i<chromoLength-2; i++){
				host1=Integer.parseInt(population2[chromoIndex][i])-1; 
				host2=Integer.parseInt(population2[chromoIndex][i+1])-1; 
				
				if (freeResources.get(host1) < freeResources.get(host2))
					changeLocal(chromoIndex,i,i+1);

			}//End for

	}//End sortIndividual

	public void changeLocal(int chromoIndex, int i1, int i2){

		String aux = new String();

		aux=population2[chromoIndex][i1];

		population2[chromoIndex][i1] = population2[chromoIndex][i2];

		population2[chromoIndex][i2]=aux;		

	}//End changeLocal

	
	/**
	 * Releases the host used by a VM.
	 * 
	 * @param vm the vm
	 * @pre $none
	 * @post none
	 */
	@Override
	public void deallocateHostForVm(Vm vm) {
		Host host = getVmTable().remove(vm.getUid());
		if (host != null) {
			int idx = getHostList().indexOf(host);
			host.vmDestroy(vm);
			
			Integer pes = getUsedPes().remove(vm.getUid());
			getFreePes().set(idx, getFreePes().get(idx) + pes);
			
			//Long mips = getUsedMips().remove(vm.getUid());
			//getFreeMips().set(idx, getFreeMips().get(idx) + mips);
			
			Long bw = getUsedBw().remove(vm.getUid());
			//getFreeBw().set(idx, getFreeBw().get(idx) + bw);
		}
	}

	/**
	 * Gets the host that is executing the given VM belonging to the given user.
	 * 
	 * @param vm the vm
	 * @return the Host with the given vmID and userID; $null if not found
	 * @pre $none
	 * @post $none
	 */
	@Override
	public Host getHost(Vm vm) {
		return getVmTable().get(vm.getUid());
	}

	/**
	 * Gets the host that is executing the given VM belonging to the given user.
	 * 
	 * @param vmId the vm id
	 * @param userId the user id
	 * @return the Host with the given vmID and userID; $null if not found
	 * @pre $none
	 * @post $none
	 */
	@Override
	public Host getHost(int vmId, int userId) {
		return getVmTable().get(Vm.getUid(userId, vmId));
	}

	/**
	 * Gets the vm table.
	 * 
	 * @return the vm table
	 */
	public Map<String, Host> getVmTable() {
		return vmTable;
	}

	/**
	 * Sets the vm table.
	 * 
	 * @param vmTable the vm table
	 */
	protected void setVmTable(Map<String, Host> vmTable) {
		this.vmTable = vmTable;
	}

	/**
	 * Gets the used pes.
	 * 
	 * @return the used pes
	 */
	protected Map<String, Integer> getUsedPes() {
		return usedPes;
	}

	/**
	 * Sets the used pes.
	 * 
	 * @param usedPes the used pes
	 */
	protected void setUsedPes(Map<String, Integer> usedPes) {
		this.usedPes = usedPes;
	}

	/**
	 * Gets the free pes.
	 * 
	 * @return the free pes
	 */
	protected List<Integer> getFreePes() {
		return freePes;
	}

	/**
	 * Sets the free pes.
	 * 
	 * @param freePes the new free pes
	 */
	protected void setFreePes(List<Integer> freePes) {
		this.freePes = freePes;
	}

	protected Map<String, Long> getUsedMips() {
		return usedMips;
	}
	protected void setUsedMips(Map<String, Long> usedMips) {
		this.usedMips = usedMips;
	}
	protected Map<String, Long> getUsedBw() {
		return usedBw;
	}
	protected void setUsedBw(Map<String, Long> usedBw) {
		this.usedBw = usedBw;
	}
	protected List<Long> getFreeMips() {
		return this.freeMips;
	}
	protected void setFreeMips(List<Long> freeMips) {
		this.freeMips = freeMips;
	}
	
	protected List<Long> getFreeBw() {
		return this.freeBw;
	}
	protected void setFreeBw(List<Long> freeBw) {
		this.freeBw = freeBw;
	}

	/*
	 * (non-Javadoc)
	 * @see cloudsim.VmAllocationPolicy#optimizeAllocation(double, cloudsim.VmList, double)
	 */
	@Override
	public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vmList) {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.cloudbus.cloudsim.VmAllocationPolicy#allocateHostForVm(org.cloudbus.cloudsim.Vm,
	 * org.cloudbus.cloudsim.Host)
	 */
	@Override
	public boolean allocateHostForVm(Vm vm, Host host) {
		if (host.vmCreate(vm)) { // if vm has been succesfully created in the host
			getVmTable().put(vm.getUid(), host);

			int pe = vm.getNumberOfPes();
			double requiredMips = vm.getCurrentRequestedTotalMips();
			long requiredBw = vm.getCurrentRequestedBw();
			
			int idx = getHostList().indexOf(host);
			
			getUsedPes().put(vm.getUid(), pe);
			getFreePes().set(idx, getFreePes().get(idx) - pe);
			
			getUsedMips().put(vm.getUid(), (long) requiredMips);
			getFreeMips().set(idx,  (long) (getFreeMips().get(idx) - requiredMips));

			getUsedBw().put(vm.getUid(), (long) requiredBw);
			getFreeBw().set(idx, (long) (getFreeBw().get(idx) - requiredBw));

			Log.formatLine(
					"%.2f: VM #" + vm.getId() + " has been allocated to the host #" + host.getId(),
					CloudSim.clock());
			return true;
		}

		return false;
	}	
}

