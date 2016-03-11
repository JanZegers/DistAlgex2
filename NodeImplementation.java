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

public class NodeImplementation extends UnicastRemoteObject implements NodeInterface {
	private static final long serialVersionUID = 1L;
	private int nodePort;
	private int expectedNetworkSize;
	private Registry registry;
	int nodesJoined;
	int roundNumber;
	int id = 7;
	boolean stillAlive = true;
	int bestID = -1;
	int bestLevel = -1;

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
			id = nodesJoined - 2; // the node own id is the number of nodes
									// joind -2, because it starts at 0
			bestID = id;
			System.out.println("My nodeID is: " + id); // extra print for
														// debugging
			if (nodesJoined - 1 == expectedNetworkSize)
				startAlgorithm(); // algorithm is started if number of nodes
									// joined is reached
		} catch (Exception e) {
			System.out.println("Kaboom: " + e);
		}
	}

	private void startAlgorithm() throws Exception {
		// The test algorithm does the following:
		// 1. Gets the list of all the registered nodes.
		// 2. Iterates through the list and sends a 'Hello' message to each
		// node.
		final NodeImplementation currentNode = this;
		bestID = id;
		if (this.id == 0)
			while (true) {
				String[] connectedNodes = this.registry.list();
				for (String nodeName : connectedNodes) {
					NodeInterface remoteNode = getRemoteNode(nodeName);
					remoteNode.round();
				}
				Thread.sleep(1000);
			}
	}

	private NodeInterface getRemoteNode(String nodeStringId)
			throws AccessException, RemoteException, NotBoundException {
		NodeInterface remoteNode = (NodeInterface) this.registry.lookup(nodeStringId);
		return remoteNode;
	}

	public void newNodeJoined() throws Exception {
		// Increase the counter of the nodes already in the network.
		nodesJoined++;
		// Start the algorithm if enough nodes have joined the network.
		if (nodesJoined - 1 == expectedNetworkSize)
			startAlgorithm(); // algorithm is started if the last node joined
	}

	ArrayList<int[]> messages = new ArrayList<int[]>(); // arraylist of messages
	int acksReceived; // number of acknoledgments received

	// if ID is me then we received an acknowledgement else we will store
	// message for later
	public void passMessage(int level, int id) {
		System.out.println("passed message: " + id);
		if (id == this.id) {
			acksReceived++;
			System.out.println("I was acknowledged!!!");
		} else
			messages.add(new int[] { level, id });
	}

	ArrayList<Integer> nodedIDs; // Arraylist with al the nodeID's

	// fills the ArrayList nodeIDs and shuffles it
	// so we have a random list of the nodes to send meassages to
	private void prepareList() {
		nodedIDs = new ArrayList<Integer>();
		for (int i = 0; i < expectedNetworkSize; i++) {
			if (id != i)
				nodedIDs.add(i);

		}
		Collections.shuffle(nodedIDs);
		System.out.println("will send in order  " + nodedIDs.toString());
	}

	// first half round of the algorithm
	// send m messages nodes not already messages
	// and still alive
	int alreadySend = 0; // number wich keep track of where we are in NodeIDs

	public void evenRound() throws Exception {
		System.out.println("received: " + acksReceived + " acks" + " and alive = " + stillAlive);
		// This checks if this node is elected, because then I would have
		// received all
		// the acknolegements.
		// BUT WHY the -1?
		if (acksReceived == expectedNetworkSize - 1) {
			stillAlive = false;
			System.out.print("I am elected!!");
		}

		// If this node did not receive an acknowledgement last round, it is
		// killed
		if (alreadySend != acksReceived)
			stillAlive = false;
		// If it is killed, it returns
		if (!stillAlive)
			return;
		// if nodeIDs does not exist, it makes a nodeIDs list
		if (nodedIDs == null)
			prepareList();
		// this filles a list ids with the ids from nodeIDs, from the
		// already send + 1 till the 2^roundnumber/2 id in this list.
		ArrayList<Integer> ids = new ArrayList<Integer>();
		for (int i = 0; i < Math.pow(2, roundNumber / 2); i++) {
			try {
				ids.add(nodedIDs.get(alreadySend));
			} catch (Exception e) {
				System.out.println("reached end of list");
			}
			alreadySend++;
		}
		Integer[] a = new Integer[ids.size()];
		System.out.println("send claim to " + Arrays.toString(a));
		ids.toArray(a);
		sendMessages(roundNumber, id, a); // sends a message with roundnumber,
											// own id , and the list of nodes.

	}

	// Second half round of the algorithm
	// Recieve messages if messages send are not equal of received, kill oneself

	public void oddRound() throws Exception {
		boolean newbest = false;
		bestID = Math.max(bestID, id); // own id is best id
		ArrayList<int[]> readMessages = new ArrayList<int[]>();
		for (int[] message : messages) {
			readMessages.add(message);
			/*
			 * if (roundNumber < message[0]) { stillAlive = false; roundNumber =
			 * message[0] + 1; bestID = message[1]; newbest = true; } else {
			 */
			System.out.println("reading " + Arrays.toString(message));
			// selecting the best id, if it is lower then current best id.
			if (bestID < message[1]) {// && roundNumber - 1 == message[0]) {
				bestID = message[1];
				newbest = true;
			}

		}
		if (newbest) {
			sendMessages(roundNumber, bestID, new Integer[] { bestID });
			stillAlive = false;
			System.out.println("ack send to " + bestID);
		}
		System.out.println("bestID = " + bestID);
		for (int[] mes : readMessages) {
			messages.remove(mes);
		}
		if (messages.size() != 0) {
			throw new Exception("sync error");
		}
	}

	// een tweede poging
	// public void oddRound() throws Exception {
	// boolean newbest = false;
	//
	// if (stillAlive) {
	// bestID = Math.max(bestID, id); // iff still alive, o
	// // own id is compared with best ide
	// }
	// for (int[] message : messages) {
	// /*
	// * if (roundNumber < message[0]) { stillAlive = false; roundNumber =
	// * message[0] + 1; bestID = message[1]; newbest = true; } else {
	// */
	// System.out.println("reading " + Arrays.toString(message));
	// // selecting the best id, if it is lower then current best id.
	// if (message[0] > newLevel) {
	// newLevel = message[0];
	// bestID = message[1];
	// newbest = true;
	// }
	//
	// }
	// if (newbest) {
	// sendMessages(roundNumber, bestID, new Integer[] { bestID });
	// stillAlive = false;
	// System.out.println("ack send to " + bestID);
	// }
	// messages = new ArrayList<int[]>();
	// System.out.println("bestID = " + bestID);
	// }

	// Execute even or odd round depending on round number
	public void round() throws Exception {
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println("round number: " + roundNumber);

		if (roundNumber % 2 == 0) {
			evenRound();
		} else {
			oddRound();
		}
		roundNumber++;
	}

	// Sends a message with level and number to every Node in IDList
	public void sendMessages(int level, int number, Integer[] IDList) throws Exception {
		final NodeImplementation currentNode = this;
		System.out.println(
				"own id is: " + id + " level " + level + " number " + number + " IDList " + Arrays.toString(IDList));
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
