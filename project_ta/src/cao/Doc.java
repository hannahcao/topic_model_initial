package cao;

import java.util.List;


public class Doc {
	private final int id;
	private final String title;
	/**
	 * follow the order aspect list file
	 */
	private final List<String> text;
	
	public Doc(int id, String title, List<String> text){
		this.id = id;
		this.title = title;
		this.text = text;
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	//	public String getDescription() {
	//	    return text;
	//	}

	public List<String> getText() {
		return text;
	}
	
	public String getFullText(){
		String fullText = "";
		for(String section : text)
			fullText+=" "+section;
		return fullText;
	}

	public String toString(){
		return ("id:"+id+",title:"+title+",desc:omitted");
	}
}
