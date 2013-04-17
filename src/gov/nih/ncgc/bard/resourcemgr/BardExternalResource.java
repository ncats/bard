package gov.nih.ncgc.bard.resourcemgr;

public class BardExternalResource {
	
	private String resourceKey;
	private int resourceProtocolType;
	private String resourceServer;
	private String resourcePath;
	private String resourceUserName;
	private String resourcePassword;
	private String fileName;
	private int compressionType;
	
	public BardExternalResource() { }
	
	public String getResourceKey() {
	    return resourceKey;
	}
	public void setResourceKey(String resourceKey) {
	    this.resourceKey = resourceKey;
	}
	public int getResourceProtocolType() {
	    return resourceProtocolType;
	}
	public void setResourceProtocolType(int resourceProtocolType) {
	    this.resourceProtocolType = resourceProtocolType;
	}

	public String getResourceServer() {
	    return resourceServer;
	}

	public void setResourceServer(String resourceServer) {
	    this.resourceServer = resourceServer;
	}

	public String getResourcePath() {
	    return resourcePath;
	}

	public void setResourcePath(String resourcePath) {
	    this.resourcePath = resourcePath;
	}

	public String getFileName() {
	    return fileName;
	}
	public void setFileName(String fileName) {
	    this.fileName = fileName;
	}
	public int getCompressionType() {
	    return compressionType;
	}
	public void setCompressionType(int compressionType) {
	    this.compressionType = compressionType;
	}
	
	
	public String getResourceUserName() {
	    return resourceUserName;
	}

	public void setResourceUserName(String resourceUserName) {
	    this.resourceUserName = resourceUserName;
	}

	public String getResourcePassword() {
	    return resourcePassword;
	}

	public void setResourcePassword(String resourcePassword) {
	    this.resourcePassword = resourcePassword;
	}

	public void dumpExtResourceVals() {
	    System.out.println("EXT_RES_KEY:"+resourceKey);
	    System.out.println("EXT_RES_PROTOCOL:"+resourceProtocolType);
	    System.out.println("EXT_RES_SERVER:"+resourceServer);
	    System.out.println("EXT_RES_UNAME:"+resourceUserName);
	    System.out.println("EXT_RES_PW:"+resourcePassword);	    
	    System.out.println("EXT_RES_PATH:"+resourcePath);
	    System.out.println("EXT_RES_FILE_NAME:"+fileName);
	    System.out.println("EXT_RES_COMPRESSION:"+compressionType);	    	    
	}
}

