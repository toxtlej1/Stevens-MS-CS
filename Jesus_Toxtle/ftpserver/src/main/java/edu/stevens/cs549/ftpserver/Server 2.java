package edu.stevens.cs549.ftpserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.server.ServerNotActiveException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Enumeration;
import java.util.Stack;
import java.util.logging.Logger;

import edu.stevens.cs549.ftpinterface.IServer;

/**
 *
 * @author jtoxtle
 */
public class Server extends UnicastRemoteObject
        implements IServer {
	
	static final long serialVersionUID = 0L;
	
	public static Logger log = Logger.getLogger("edu.stevens.cs.cs549.ftpserver");
    
	/*
	 * For multi-homed hosts, must specify IP address on which to 
	 * bind a server socket for file transfers.  See the constructor
	 * for ServerSocket that allows an explicit IP address as one
	 * of its arguments.
	 */
	private InetAddress host;
	
	final static int BACKLOG_LENGTH = 5;
	
	/*
	 *********************************************************************************************
	 * Current working directory.
	 */
    static final int MAX_PATH_LEN = 1024;
    private Stack<String> cwd = new Stack<String>();
    
    /*
     *********************************************************************************************
     * Data connection.
     */
    
    enum Mode { NONE, PASSIVE, ACTIVE };
    
    private Mode mode = Mode.NONE;
    
    /*
     * If passive mode, remember the server socket.
     */
    
    private ServerSocket dataChan = null;
    
    private int makePassive () throws IOException {
    	dataChan = new ServerSocket(0, BACKLOG_LENGTH, host);
    	mode = Mode.PASSIVE;
//    	return (InetSocketAddress)(dataChan.getLocalSocketAddress());
    	return dataChan.getLocalPort();
    }
    
    /*
     * If active mode, remember the client socket address.
     */
    private InetSocketAddress clientSocket = null;
    
    private void makeActive (int clientPort) {
//    	clientSocket = s;
    	try {
			clientSocket = InetSocketAddress.createUnresolved(getClientHost(), clientPort);
		} catch (ServerNotActiveException e) {
			throw new IllegalStateException ("Make active", e);
		}
    	mode = Mode.ACTIVE;
    }
    
    /*
     **********************************************************************************************
     */
            
    /*
     * The server can be initialized to only provide subdirectories
     * of a directory specified at start-up.
     */
    private final String pathPrefix;

    public Server(InetAddress host, int port, String prefix) throws RemoteException {
    	super(port);
    	this.host = host;
    	this.pathPrefix = prefix + "/";
        log.info("A client has bound to a server instance.");
    }
    
    public Server(InetAddress host, int port) throws RemoteException {
        this(host, port, "/");
    }
    
    private boolean valid (String s) {
        // File names should not contain "/".
        return (s.indexOf('/')<0);
    }
    
    private static class GetThread implements Runnable {
    	private ServerSocket dataChan = null;
    	private InputStream file = null;
    	public GetThread (ServerSocket s, InputStream f) { dataChan = s; file = f; }
    	public void run () {
    		/*
    		 * TODO: Process a client request to transfer a file. - DONE
    		 */
    		
    		try {
				Socket xfer = dataChan.accept();
				
	    		BufferedInputStream in = new BufferedInputStream(file);
	    		BufferedOutputStream out = new BufferedOutputStream(xfer.getOutputStream());
	    		
	    		byte[] buffer = new byte[1024];
	    		int len = 0;
	    		
	    		while ((len = in.read(buffer)) != -1) 
	    		{
	    			out.write(buffer, 0, len);
	    		}
	    		
	    		if (in != null && out != null) {
	    			out.close();
	    			in.close();
	    		}
	    		
	    		file.close();
	    		
			} catch (IOException e) {
				e.printStackTrace();
			}

    		
    	}
    }
    
    public void get (String file) throws IOException, FileNotFoundException, RemoteException {
        if (!valid(file)) {
            throw new IOException("Bad file name: " + file);
        } else if (mode == Mode.ACTIVE) {
        	log.info("Server connecting to client at address "+clientSocket.getHostName());
        	Socket xfer = new Socket (clientSocket.getHostName(), clientSocket.getPort());
        	/*
        	 * TODO: connect to client socket to transfer file. - REDO
        	 */
        	InputStream in = new FileInputStream(path() + file);
        	BufferedInputStream bufIn = new BufferedInputStream(in);
        	
        	BufferedOutputStream out = new BufferedOutputStream(xfer.getOutputStream());
        	
        	byte[] buffer = new byte[1024];
        	int len = 0;
        	
        	while ((len = bufIn.read(buffer)) != -1) 
        	{
        		out.write(buffer, 0, len);
        	}
        	
        	if (bufIn != null && out != null) {
        		bufIn.close();
        		out.close();
        	}
        	
        	in.close();
        	
        	/*
			 * End
			 */
        } else if (mode == Mode.PASSIVE) {
        	
            InputStream f = new BufferedInputStream(new FileInputStream(path()+file));
            new Thread (new GetThread(dataChan, f)).start();
            
        }
    }
    
    private static class CreateThreadForPut implements Runnable {
    	private ServerSocket dataChan = null;
    	private FileOutputStream file = null;
    	
    	public CreateThreadForPut(ServerSocket s, FileOutputStream f) { dataChan = s; file = f; }
    	
    	public void run() {
    		/*
    		 * TODO: Process a client request to transfer a file. - DONE
    		 */
    		
    		try {
    			Socket xfer = dataChan.accept();
    			
    			BufferedInputStream in = new BufferedInputStream(xfer.getInputStream());
    			FileOutputStream f = file;
    			
    			byte[] buffer = new byte[1024];
    			int len = 0;
    			
    			while ((len = in.read(buffer)) != -1) 
    			{
    				f.write(buffer, 0, len);
    			}
    			
    			f.close();
    			
    			if (in != null) {
    				in.close();
    			}
    			
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    	}
    }
    
    public void put (String file) throws IOException, FileNotFoundException, RemoteException {
    	/*
    	 * TODO: Finish put (both ACTIVE and PASSIVE). - DONE
    	 */
    	if (mode == Mode.ACTIVE) 
    	{
    		Socket xfer = new Socket(clientSocket.getHostName(), clientSocket.getPort());
    		
    		BufferedInputStream in = new BufferedInputStream(xfer.getInputStream());
    		FileOutputStream f = new FileOutputStream(path() + file);
    		
    		byte[] buffer = new byte[1024];
    		int len = 0;
    		
    		while ((len = in.read(buffer)) != -1) 
    		{
    			f.write(buffer, 0, len);
    			
    		}
    		
    		f.close();
    		if (in != null) { in.close(); }
    		
    	} 
    	else if (mode == Mode.PASSIVE) 
    	{
    		FileOutputStream f = new FileOutputStream(path() + file);
    		new Thread(new CreateThreadForPut(dataChan, f)).start();
    	}
    }
    
    public String[] dir () throws RemoteException {
        // List the contents of the current directory.
        return new File(path()).list();
    }

	public void cd(String dir) throws IOException, RemoteException {
		// Change current working directory (".." is parent directory)
		if (!valid(dir)) {
			throw new IOException("Bad file name: " + dir);
		} else {
			if ("..".equals(dir)) {
				if (cwd.size() > 0)
					cwd.pop();
				else
					throw new IOException("Already in root directory!");
			} else if (".".equals(dir)) {
				;
			} else {
				File f = new File(path());
				if (!f.exists())
					throw new IOException("Directory does not exist: " + dir);
				else if (!f.isDirectory())
					throw new IOException("Not a directory: " + dir);
				else
					cwd.push(dir);
			}
		}
	}

    public String pwd () throws RemoteException {
        // List the current working directory.
        String p = "/";
        for (Enumeration<String> e = cwd.elements(); e.hasMoreElements(); ) {
            p = p + e.nextElement() + "/";
        }
        return p;
    }
    
    private String path () throws RemoteException {
    	return pathPrefix+pwd();
    }
    
    public void port (int clientPort) {
    	makeActive(clientPort);
    }
    
    public int pasv () throws IOException {
    	return makePassive();
    }

}
 