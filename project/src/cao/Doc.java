package cao;

public class Doc {
	private final int id;
	private final String title;
	private final String description;
	
	public Doc(int id, String title, String description){
	    this.id = id;
	    this.title = title;
	    this.description = description;
	}
	
	public int getId() {
	    return id;
	}
	
	public String getTitle() {
	    return title;
	}
	
	public String getDescription() {
	    return description;
	}
	
	public String getText() {
	    return getTitle()+" "+getDescription();
	}
	
	public String toString(){
		return ("id:"+id+",title:"+title+",desc:omitted");
	}
    
}
