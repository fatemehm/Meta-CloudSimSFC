/*
 * Title:        CloudSimSDN
 * Description:  SDN extension for CloudSim
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2017, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.sdn.nos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.sdn.CloudSimTagsSDN;
import org.cloudbus.cloudsim.sdn.policies.vmallocation.VmGroup;
import org.cloudbus.cloudsim.sdn.sfc.ServiceFunctionChainPolicy;
import org.cloudbus.cloudsim.sdn.virtualcomponents.FlowConfig;
import org.cloudbus.cloudsim.sdn.virtualcomponents.SDNVm;

public class NetworkOperatingSystemGroupAware extends NetworkOperatingSystemSimple {

	public NetworkOperatingSystemGroupAware() {
		super();
	}

	protected List<VmGroup> createVmGroup(Collection<Vm> vms, Collection<FlowConfig> links) {
		// Put VMs into separate group according to their connections 
		List<VmGroup> groups = new ArrayList<VmGroup>();
		List<Vm> vmPool = new ArrayList<Vm>(vms);
		List<FlowConfig> arcPool = new ArrayList<FlowConfig>(links);
		
		// Sort links by their bandwidth 
		Collections.sort(arcPool, new Comparator<FlowConfig>() {
		    public int compare(FlowConfig o1, FlowConfig o2) {
		        return (int) (o1.getBw() - o2.getBw());
		    }
		});
		
		// Separate groups by link bandwidth order
		VmGroup.classifyGroupByArcList(arcPool, vmPool, groups);
		
		// Put all other VMs into separate group
		VmGroup.putEachVmIntoEachGroup(vmPool, groups);
		
		// Sort VmGroup by their mips request
		Collections.sort(groups);
		
		return groups;
	}
	/*
	private Collection<Vm> getConnectedVms(Vm vm, Collection<Arc> flowList) {
		Set<Integer> connectedVmIds = new HashSet<Integer>();

		for(Arc arc:flowList) {
			if(arc.getSrcId() == vm.getId()) {
				connectedVmIds.add(arc.getDstId());
			}
			else if(arc.getDstId() == vm.getId()) {
				connectedVmIds.add(arc.getSrcId());
			}
		}
		
		Collection<Vm> vms = new ArrayList<Vm>();
		for(int vmId:connectedVmIds) {
			vms.add(vmList.get(vmId));
		}
		
		return vms;
	}
*/
	
	@Override
	public boolean deployApplication(List<Vm> vms, Collection<FlowConfig> links, List<ServiceFunctionChainPolicy> sfcPolicy) {
		Log.printLine(CloudSim.clock() + ": " + getName() + ": Starting deploying application..");
		List<VmGroup> sortedGroups = createVmGroup(vms, links);
		
		for(VmGroup group:sortedGroups)
		{
			for(Vm vm:group.getVms()) {
				SDNVm tvm = (SDNVm) vm;
				Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId()
						+ " in " + datacenter.getName() + ", (" + tvm.getStartTime() + "~" +tvm.getFinishTime() + ")");
				
				List<Object> params = new ArrayList<Object>();
				params.add(tvm); 	// obj.get(0)
				params.add(group);	// obj.get(1)

//				send(datacenter.getId(), tvm.getStartTime(), CloudSimTags.VM_CREATE_ACK, vm);
				send(datacenter.getId(), tvm.getStartTime(), CloudSimTagsSDN.SDN_VM_CREATE_IN_GROUP_ACK, params);
				
				if(tvm.getFinishTime() != Double.POSITIVE_INFINITY) {
					send(datacenter.getId(), tvm.getFinishTime(), CloudSimTags.VM_DESTROY, vm);
					send(this.getId(), tvm.getFinishTime(), CloudSimTags.VM_DESTROY, vm);
				}
			}
		}
		return true;
	}
	
}
