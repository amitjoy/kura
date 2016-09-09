package org.eclipse.kura.web.shared.model;

import java.io.Serializable;

import org.eclipse.kura.web.client.util.KuraBaseModel;

public class GwtChannelInfo extends KuraBaseModel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2188123225288791202L;

	public void setId(String id){
		super.set("id", id);
	}
	
	public void setName(String name){
		super.set("name", name);
	}
	
	public void setType(String type){
		super.set("type", type);
	}
	
	public void setValueType(String valueType){
		super.set("valueType", valueType);
	}
	
	public String getId(){
		return super.get("id");
	}
	
	public String getName(){
		return super.get("name");
	}
	
	public String getType(){
		return super.get("type");
	}
	
	public String getValueType(){
		return super.get("valueType");
	}

	@Override
	public void set(String name, Object value) {
		super.set("driver."+name, value);
	}

	@Override
	public <X> X get(String key) {
		return super.get("driver."+key);
	}
	
}
