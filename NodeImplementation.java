import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;


public class NodeImplementation extends UnicastRemoteObject implements NodeInterface {
	private static final long serialVersionUID = 1L;
	private int nodePort;
	private int expectedNetworkSize;
	private Registry registry;
	int nodesJoined;
	
	protected NodeImplementation(int registryPort, int nodePort, int expectedNetworkSize) throws RemoteException {
		super();
		this.nodePort = nodePort;
		this.expectedNetworkSize = expectedNetworkSize;
		this.registry = LocateRegistry.getRegistry(registryPort);
		// Registration in the registry.
		try {
			this.registry.bind(Integer.toString(this.nodePort), this);
		} catch (AlreadyBoundException e) {
			System.out.println("Daboom: " + e);
		}
	}
	
	// Notify other nodes that you have joined the network.
	public void notifyOthers() {		
		try
		{
			// Inform the other nodes that you have joined the network by calling their newNodeJoined remote method.
			String[] connectedNodes = this.registry.list();
			for (String nodeName : connectedNodes) {
				NodeInterface remoteNode = getRemoteNode(nodeName);
				remoteNode.newNodeJoined();
				System.out.println("Notified node: " + nodeName);
				nodesJoined++;
			}
		} catch (Exception e) {
			System.out.println("Kaboom: " + e);
		}
	}
	
	private void startAlgorithm() {
		// The test algorithm does the following:
		// 1. Gets the list of all the registered nodes.
		// 2. Iterates through the list and sends a 'Hello' message to each node.
		final NodeImplementation currentNode = this;
		
		new Thread(new Runnable() {
		    public void run() {
	    		try {
	    			// Get the list of registered nodes.
			    	String[] remoteIds = currentNode.registry.list();
			    	// Send the messages until death.
			    	while (true) {
				    	for (String nodeStringId : remoteIds) {
							NodeInterface remoteNode = currentNode.getRemoteNode(nodeStringId);
							remoteNode.passMessage("Hello there!", Integer.toString(currentNode.nodePort));
				    	}
				    	// Sleep a bit.
				    	Thread.sleep(500);
			    	}
				} catch (Exception e) {
					System.out.println("Waboom: " + e);
				}
    		}
	    }).start();
	}
	
	private NodeInterface getRemoteNode(String nodeStringId) throws AccessException, RemoteException, NotBoundException {
		NodeInterface remoteNode = (NodeInterface) this.registry.lookup(nodeStringId);
		return remoteNode;
	}
	
	public void newNodeJoined() {
		// Increase the counter of the nodes already in the network.
		nodesJoined++;
		// Start the algorithm if enough nodes have joined the network.
		if (nodesJoined - 1 == expectedNetworkSize)
			startAlgorithm();
	}
	
	public void passMessage(String message, String nodeId) {
		System.out.println(String.format("Node %s says \"%s\".", nodeId, message));
	}
}
