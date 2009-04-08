package socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author TieSheng
 *
 * 2009-4-3 下午04:26:08
 */
public class Server {
	private Map<String, ClientManager> clientMap = new HashMap<String, ClientManager>();
	private ServerSocket ss;
	private Socket sk;
	private int port;
	private ClientManager cm;
	
	public Server() {}
	
	public Server(int port){
		this.port = port;
	}

	public void start(){
		try {
			ss = new ServerSocket(port);
		} catch (IOException e) {
			e.printStackTrace();
		}
		while (true) {
			try {
				sk = ss.accept();
				cm = new ClientManager(sk);
				new Thread(cm).start();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}		
	}

	public void close() throws IOException{
		ClientManager cm;
		for(Map.Entry<String, ClientManager> entry:clientMap.entrySet()){
			cm = entry.getValue();
			cm.close();
		}
		
		try {
			if (sk != null) sk.close();
			if (ss != null) ss.close();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	public static void main(String args[]){
		Server server = new Server(2008);
		server.start();
	}
	
	
	private class ClientManager implements Runnable{
		private Socket sk = null;
		private DataInputStream dis = null;
		private DataOutputStream dos = null;
		private boolean status = true;
		private String userName;
		
		public ClientManager(Socket sk) {
			this.sk = sk;
			try {
				dis = new DataInputStream(sk.getInputStream());
				dos = new DataOutputStream(sk.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void run() {
			while(status){
				try {
					String cStrInfo = dis.readUTF();
					if(cStrInfo.startsWith("-f")){
						/* 首次登录要在服务端注册 */
						//clientMap.put(cm.userName,cm);
						register(cStrInfo);
					}else if(cStrInfo.startsWith("-t")){
						/* 连接测试 */
						sendTest(cStrInfo);
					}else if(cStrInfo.startsWith("-u")){
						/* 私聊 */
						send2User(cStrInfo);
					}else if(cStrInfo.startsWith("-l")){
						/* 获取用户列表 */
						sendUserList();
					}else{
						/* 公聊 */
						send2All(cStrInfo);
					}
				}catch(Exception e){
					
				}
			}
		}
		/**
		 * 公聊
		 * @throws IOException 
		 */
		private void send2All(String infoStr) throws IOException{
			
			String tmpMsg = null;
			
			for(Map.Entry<String, ClientManager> entry:clientMap.entrySet()){
				cm = entry.getValue();
				tmpMsg = this.userName + "->All: " + infoStr;
				cm.sendInfo(tmpMsg);
				tmpMsg = null;
			}
		}
		/**
		 * 私聊
		 * @throws IOException 
		 */
		private void send2User(String infoStr) throws IOException{
			String uname = infoStr.substring(infoStr.indexOf(123)+1, infoStr.indexOf(125));
			String info = infoStr.substring(infoStr.indexOf(125)+1);
			StringBuffer bf = new StringBuffer();
			ClientManager tmpCM =  clientMap.get(uname.trim());
			
			bf.append(this.userName)
			  .append("->")
			  .append(tmpCM.userName)
			  .append(": ")
			  .append(info);
			  
			tmpCM.sendInfo(bf.toString());
		}
		/**
		 * 测试连接
		 * @throws Exception 
		 */
		private void sendTest(String infoStr) throws Exception{
			this.sendInfo("连接正常");
		}
		/**
		 * 登记注册
		 * @param infoStr
		 * @throws IOException 
		 */
		private void register(String infoStr) throws IOException{
			int u = infoStr.indexOf(":");
			String u_name = infoStr.substring(u+1);
			this.userName = u_name.trim();
			clientMap.put(this.userName,this);
			this.sendInfo(this.userName+"注册成功，您现在可以参与聊天");
		}		
		/**
		 * 发送消息
		 * @param info
		 * @throws IOException
		 */
		private void sendInfo(String info) throws IOException{
			dos.writeUTF(info);
			dos.flush();
		}
		/**
		 * 发送用户列表
		 * @throws IOException 
		 */
		private void sendUserList() throws IOException{
			StringBuffer bf = new StringBuffer();
			for(Map.Entry<String, ClientManager> entry:clientMap.entrySet()){
				bf.append(entry.getKey())
				.append("、 ");
			}
			bf.append("\n");
			this.sendInfo(bf.toString());
		}
		/**
		 * 关闭当前客户端
		 */
		public void close() throws IOException {
			if(status) status = false;
			if (sk != null) sk.close();
			if (dis != null) dis.close();
			if (dos != null) dos.close();
		}		
	}
}

