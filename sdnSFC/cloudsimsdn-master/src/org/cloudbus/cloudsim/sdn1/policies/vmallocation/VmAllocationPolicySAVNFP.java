/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2015, The University of Melbourne, Australia
 */
package org.cloudbus.cloudsim.sdn.policies.vmallocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.lang.Math.sqrt;
import java.util.Collections;
import java.util.Random;


import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.sdn.monitor.power.PowerUtilizationMaxHostInterface;

/**
 * Modifyin Hill Climbing algorithm for vnf allocation to 
 * compute power and network bandwidth.
 * @author Jungmin Son
 * Fatemeh msy
 * @since CloudSimSDN 1.0
 */
public class VmAllocationPolicySAVNFP extends VmAllocationPolicy implements PowerUtilizationMaxHostInterface {

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
	private double alpha = 0.99993;
    
    	private double t_criteria = 0.00001;
    
    	private double i_criteria =100000;

	/**
	 * Creates the new VmAllocationPolicySimple object.
	 * 
	 * @param list the list
	 * @pre $none
	 * @post $none
	 */
	public VmAllocationPolicySAVNFP(List<? extends Host> list) {
		super(list);
		//x=Collections.shuffle(list);
		setFreePes(new ArrayList<Integer>());
		setFreeMips(new ArrayList<Long>());
		setFreeBw(new ArrayList<Long>());
		
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
	 * @return $true if the host could be allocated; $false otherwise
	 * @pre $none
	 * @post $none
	 */
	@Override
	public boolean allocateHostForVm(Vm vm) {
		//return allocateHostForVm(vm);

		if (getVmTable().containsKey(vm.getUid())) { // if this vm was not created
			return false;
		}
		
	
		
		int numHosts = getHostList().size();;
		// 1. Find/Order the best host for this VM by comparing a metric
		int requiredPes = vm.getNumberOfPes();
		double requiredMips = vm.getCurrentRequestedTotalMips();
		long requiredBw = vm.getCurrentRequestedBw();
		
		boolean result = false;
		
		double[] freeResources = new double[numHosts];
		
		for (int i = 0; i < numHosts; i++) {
			double mipsFreePercent = (double)getFreeMips().get(i) / this.hostTotalMips; 
			double bwFreePercent = (double)getFreeBw().get(i) / this.hostTotalBw;
			
			freeResources[i] = this.convertWeightedMetric(mipsFreePercent, bwFreePercent);
			
		}

		for(int idIndex = 0; result == false && idIndex < numHosts; idIndex++) {// we still trying until we find a host or until we try all of them

			double currentFitness=freeResources[idIndex];
				
			double bestFitness = currentFitness;
			float temp = (float) sqrt(numHosts);
			//https://www.geeksforgeeks.org/simulated-annealing/
			int numberOfIterator = 10;
			while (temp>= t_criteria && numberOfIterator<i_criteria){
				for( int i=0; i<numHosts; i++){
								
					//Collections.shuffle(hostss);			
					double candidateFitness=freeResources[i];
								
					if (candidateFitness>currentFitness) {
						double feasibleNeighbor = candidateFitness;
						double p = Math.exp(-Math.abs(feasibleNeighbor-currentFitness) / temp);
						if (Math.random() < p) {	
					    		currentFitness = feasibleNeighbor;
							idIndex=i;
						}
						
					}
					temp *= alpha;
					numberOfIterator++;
				}
			
			}
			Host host = getHostList().get(idIndex);
			// Check whether the host can hold this VM or not.
			if( getFreeMips().get(idIndex) < requiredMips) {
				System.err.println("not enough MIPS:"+getFreeMips().get(idIndex)+", req="+requiredMips);
				//Cannot host the VM
				continue;
			}
			if( getFreeBw().get(idIndex) < requiredBw) {
				System.err.println("not enough BW:"+getFreeBw().get(idIndex)+", req="+requiredBw);
				//Cannot host the VM
				//continue;
			}
				
			result = host.vmCreate(vm);
			
			if (result) { // if vm were succesfully created in the host
				getVmTable().put(vm.getUid(), host);
				getUsedPes().put(vm.getUid(), requiredPes);
				getFreePes().set(idIndex, getFreePes().get(idIndex) - requiredPes);
					
				getUsedMips().put(vm.getUid(), (long) requiredMips);
				getFreeMips().set(idIndex,  (long) (getFreeMips().get(idIndex) - requiredMips));

				getUsedBw().put(vm.getUid(), (long) requiredBw);
				getFreeBw().set(idIndex,  (long) (getFreeBw().get(idIndex) - requiredBw));
				result = true;
				break;
			}
				//idIndex++;
		}
			
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
			
			Long mips = getUsedMips().remove(vm.getUid());
			getFreeMips().set(idx, getFreeMips().get(idx) + mips);
			
			Long bw = getUsedBw().remove(vm.getUid());
			getFreeBw().set(idx, getFreeBw().get(idx) + bw);
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
