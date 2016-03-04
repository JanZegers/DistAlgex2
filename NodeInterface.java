import java.rmi.Remote;


public interface NodeInterface extends Remote {
	public void passMessage(String message, String nodeId) throws Exception;
	public void newNodeJoined() throws Exception;
}
