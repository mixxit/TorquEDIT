package torquedit.tseditor.util;


public class XmlResource {
	
	private String name;
	private String desc;
	
	public XmlResource(String name, String desc) {
		this.name = name;
		this.desc = desc;
	}
	
	public String getName() {
		return name;
	}
	public String getDescription() {
		return desc;
	}
}
