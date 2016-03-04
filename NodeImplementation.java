import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;

public class NodeImplementation extends UnicastRemoteObject implements NodeInterface {
	private static final long serialVersionUID = 1L;
	private int nodePort;
	private int expectedNetworkSize;
	private Registry registry;
	int nodesJoined;
	int roundNumber;
	int id;
	boolean stillAlive = true;

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
		try {
			// Inform the other nodes that you have joined the network by
			// calling their newNodeJoined remote method.
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
		// 2. Iterates through the list and sends a 'Hello' message to each
		// node.
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

	private NodeInterface getRemoteNode(String nodeStringId)
			throws AccessException, RemoteException, NotBoundException {
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

	ArrayList<int[]> messages = new ArrayList<int[]>();
	int acksReceived;

	// if ID is me then we received an ackknowledgement else we will store
	// message for later
	public void passMessage(int level, int id) {

		System.out.println(String.format("Node %s says, I am on level  \"%s\".", id, level));
		if (id == this.id) {
			acksReceived++;
		} else
			messages.add(new int[] { level, id });
	}

	ArrayList<Integer> nodedIDs;

	private void prepareList() {
		nodedIDs = new ArrayList<Integer>();
		for (int i = 0; i < expectedNetworkSize; i++) {
			nodedIDs.add(i);
		}
		Collections.shuffle(nodedIDs);
	}

	// first half round of the algorithm
	// send m messages nodes not already messages
	// and still alive
	int alreadySend = 0;

	public void evenRound() throws Exception {
		if (alreadySend != acksReceived)
			stillAlive = false;
		if (!stillAlive)
			return;
		if (nodedIDs == null)
			prepareList();
		int[] ids = new int[(int) Math.pow(2, roundNumber / 2)];
		for (int i = 0; i < Math.pow(2, roundNumber / 2); i++) {
			ids[i] = nodedIDs.get(alreadySend);
			alreadySend++;
		}
		sendMessages(roundNumber, id, ids);

	}

	// Second half round of the algorithm
	// Recieve messages if messages send are not equal of received, kill oneself
	int bestID = id;

	public void oddRound() throws Exception {

		for (int[] message : messages) {
			if (roundNumber < message[0]) {
				stillAlive = false;
				roundNumber = message[0] + 1;
				bestID = message[1];
			} else {
				if (bestID < message[1] && roundNumber - 1 == message[0]) {
					bestID = message[1];
				}
			}

		}
		if (bestID != id) {
			sendMessages(roundNumber, bestID, new int[] { bestID });
		}
	}

	// Execute even or odd round depending on round number
	public void round() throws Exception {
		if (roundNumber % 2 == 0) {
			evenRound();
		} else {
			oddRound();
		}
		roundNumber++;
	}

	// Sends a message with level and number to every Node in IDList
	public void sendMessages(int level, int number, int[] IDList) throws Exception {
		final NodeImplementation currentNode = this;
		String[] remoteIds = currentNode.registry.list();
		for (int i = 0; i < IDList.length; i++) {
			if (IDList[i] == id)
				continue;
			String nodeStringId = remoteIds[IDList[i]];
			NodeInterface remoteNode = currentNode.getRemoteNode(nodeStringId);
			remoteNode.passMessage(level, number);

		}
	}
}
