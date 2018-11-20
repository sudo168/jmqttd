package com.ewant.jmqttd.cluster.ehcache;

import java.io.Serializable;

public class EventBean implements Cachingable {

	private static final long serialVersionUID = -2057750184737842506L;
	
	private Object eventData;
	
	public EventBean(Object eventData) {
		this.eventData = eventData;
	}
	
	public Object getEventData(){
		return this.eventData;
	}

	@Override
	public Serializable cacheName(Serializable... names) {
		return this.eventData.toString();
	}

}
