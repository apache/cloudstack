package com.cloud.utils.db;


public class JoinBuilder<T> {
	
	public enum JoinType {
	     INNER ("INNER JOIN"),
	     LEFT ("LEFT JOIN"),
	     RIGHT ("RIGHT JOIN"),
	     RIGHTOUTER ("RIGHT OUTER JOIN"),
	     LEFTOUTER ("LEFT OUTER JOIN");
	     
	     private final String _name;
	     
	     JoinType(String name) {
	            _name = name;
	     }
	     
	     public String getName() { return _name; }
	}

	
	private T t;
	private JoinType type;
	private Attribute firstAttribute;
	private Attribute secondAttribute;
	
	public JoinBuilder(T t, Attribute firstAttribute,
			Attribute secondAttribute, JoinType type) {
		this.t = t;
		this.firstAttribute = firstAttribute;
		this.secondAttribute = secondAttribute;
		this.type = type;
	}
	
	public T getT() {
		return t;
	}
	public void setT(T t) {
		this.t = t;
	}
	public JoinType getType() {
		return type;
	}
	public void setType(JoinType type) {
		this.type = type;
	}
	public Attribute getFirstAttribute() {
		return firstAttribute;
	}
	public void setFirstAttribute(Attribute firstAttribute) {
		this.firstAttribute = firstAttribute;
	}
	public Attribute getSecondAttribute() {
		return secondAttribute;
	}
	public void setSecondAttribute(Attribute secondAttribute) {
		this.secondAttribute = secondAttribute;
	}

}


