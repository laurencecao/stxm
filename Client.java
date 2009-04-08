package socket;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * @author (石头小马)
 * 2009-4-6 下午05:20:27
 */
public class Client{

	public static void main(String args[]){
		ClientManager cm = new ClientManager("127.0.0.1",2008);
		try {
			cm.start();
			String tmpStr = null;
			String regStr = null;
			boolean sendStatus = false;
			StringBuffer bfMsg = null;
			while(true){
				if(!cm.regStatus){
					System.out.print("请输入您的昵称：");
					tmpStr = getReqMsg();
					if(!tmpStr.isEmpty()){
						regStr = "-f -u:" + tmpStr.trim();
						cm.setSendMsg(regStr);
						cm.regStatus = true;
						regStr = null;
						continue;
					}else{
						System.out.print("\n昵称不能为空，请重新输入：");
					}
				}
				
				tmpStr = getReqMsg();
				//System.out.println(tmpStr);
				if(tmpStr.startsWith("-u")){
					sendStatus = true;
					bfMsg = new StringBuffer();
					String newStr = tmpStr.substring(tmpStr.indexOf("-u")+2);
					
					bfMsg.append("-u{")
					.append(newStr.trim())
					.append("}");
					newStr = null;
					System.out.print("\n请输入您要发送的信息：");					
					continue;
				}
				if(sendStatus){
					bfMsg.append(tmpStr);
					cm.setSendMsg(bfMsg.toString());
					sendStatus = false;
					continue;
				}
				/* 默认发送 */
				cm.setSendMsg(tmpStr);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String getReqMsg() throws IOException{
		BufferedReader inStr = new BufferedReader(new InputStreamReader(System.in));
		return inStr.readLine();
	}
	
}

class ClientManager  implements Runnable{
    private Thread receiverThread = null;
    private Thread senderThread   = null;
    private String sIp = null;
    private int port; 
    private Socket sk;
    private DataOutputStream dos;
    private DataInputStream dis;
    private String sendMsg = null;
    private List<String> reqMsg;
    public boolean regStatus;
    
    public ClientManager(){}
    public ClientManager(String ip, int port){
    	this.sIp = ip;
    	this.port = port;
    }
    
	public void setSendMsg(String sendMsg) {
		this.sendMsg = sendMsg;
	}
	
	private boolean getConn() throws IOException{
    	sk = new Socket();
		InetSocketAddress server = new InetSocketAddress(sIp, port);
		sk.connect(server, 1000);// 建立连接超时
		dos = new DataOutputStream(sk.getOutputStream());
		dis = new DataInputStream(sk.getInputStream());
    	return true;
    }
    
    public void start() throws IOException{
    	reqMsg = new ArrayList<String>();
    	if(!getConn()){
    		System.out.println("Connect false!");
    		return;
    	}
    	
        // 起发送线程
        if ((senderThread == null) || !senderThread.isAlive()) {
            senderThread = new Thread(this, "TransmitSenderThread");
            senderThread.setDaemon(true);
            senderThread.start();
        }
        
        // 起接收线程
        if ((receiverThread == null) || !receiverThread.isAlive()) {
            receiverThread = new Thread(this, "TransmitReceiverThread");
            receiverThread.setDaemon(true);
            receiverThread.start();
        }  	
    }
    
	public void run() {
		while ((receiverThread != null) && Thread.currentThread().equals(receiverThread)) {
			try{
				String msg = dis.readUTF();
				System.out.println(msg);
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		while ((senderThread != null) && Thread.currentThread().equals(senderThread)) {
			try{
				
                while (sendMsg == null) {
                	senderThread.sleep(1500);
                }
                
                synchronized(sendMsg){
        			dos.writeUTF(sendMsg);
        			dos.flush();
        			sendMsg = null;
                }
                
			}catch(Exception e){
				//System.out.println(e.toString());
				e.printStackTrace();
			}			
		}
	}
}