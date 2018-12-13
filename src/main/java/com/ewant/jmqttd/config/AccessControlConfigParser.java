package com.ewant.jmqttd.config;

import com.ewant.jmqttd.config.impl.AclPermissionAccess;

public class AccessControlConfigParser extends ConfigParser<AccessControlConfig> {

    @Override
    protected AccessControlConfig generateTypeObject() throws Exception {
        return AccessControlConfig.INSTANCE;
    }

    @Override
    protected Object createConfigObject(AccessControlConfig config, String nodeName) throws ConfigParseException {

        AclPermissionAccess.AclPermission aclPermision = AclPermissionAccess.AclPermission.valueOf(nodeName.toUpperCase());
        AclPermissionAccess permisionAccess = new AclPermissionAccess();
        permisionAccess.setPermission(aclPermision);

        config.addPermission(permisionAccess);

        return permisionAccess;
    }

}
