package net.ewant.jmqttd.config.impl;

import java.util.Arrays;

public class AclPermissionAccess {

    public enum AclPermission {
        ALLOW,DENY
    }

    public enum AclType{
        USER,IP,CLIENT,ALL
    }

    public enum AclAction{
        PUB,SUB,PUBSUB,CONN
    }

    private AclPermission permission;
    private AclType type;
    private String typeValues;
    private AclAction action;
    private String[] topics;
    private String msgFilter;


    public AclPermission getPermission() {
        return permission;
    }

    public void setPermission(AclPermission permission) {
        this.permission = permission;
    }

    public AclType getType() {
        return type;
    }

    public void setType(AclType type) {
        this.type = type;
    }

    public String getTypeValues() {
        return typeValues;
    }

    public void setTypeValues(String typeValues) {
        this.typeValues = typeValues;
    }

    public AclAction getAction() {
        return action;
    }

    public void setAction(AclAction action) {
        this.action = action;
    }

    public String[] getTopics() {
        return topics;
    }

    public void setTopics(String[] topics) {
        this.topics = topics;
    }

    public String getMsgFilter() {
        return msgFilter;
    }

    public void setMsgFilter(String msgFilter) {
        this.msgFilter = msgFilter;
    }

	@Override
	public String toString() {
		return "AclPermissionAccess [permission=" + permission + ", type=" + type + ", typeValues=" + typeValues
				+ ", action=" + action + ", topics=" + Arrays.toString(topics) + ", msgFilter=" + msgFilter + "]";
	}
    
}
