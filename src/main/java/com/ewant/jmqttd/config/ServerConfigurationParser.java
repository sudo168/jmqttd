package com.ewant.jmqttd.config;

import com.ewant.jmqttd.config.impl.ServerConfig;
import com.ewant.jmqttd.utils.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;

public class ServerConfigurationParser extends ConfigParser<ServerConfiguration> {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static final String CONFIG_BASE_PACKAGE = ConfigParser.class.getPackage().getName() + ".impl";

    @Override
    protected ServerConfiguration generateTypeObject() throws Exception {
        return ServerConfiguration.INSTANCE;
    }

    @Override
    protected Object createConfigObject(ServerConfiguration configuration, String nodeName) throws ConfigParseException {
        try {
            String configName = (nodeName.substring(0,1).toUpperCase() + nodeName.substring(1)) + "Config";
            Object configObject = Class.forName(CONFIG_BASE_PACKAGE + "." + configName).newInstance();

            Field field = ReflectUtil.getFieldByFieldType(configuration, configObject.getClass());
            if(!field.isAccessible()){
                field.setAccessible(true);
            }
            field.set(configuration, configObject);
            return configObject;
        } catch (Exception e) {
            throw new ConfigParseException(e);
        }
    }

    @Override
    public ConfigParseResult<ServerConfiguration> parse(InputStream config) throws ConfigParseException {
        ConfigParseResult<ServerConfiguration> configParseResult = super.parse(config);
        ServerConfiguration serverConfiguration = configParseResult.getResult();
        ServerConfig serverConfig = serverConfiguration.getServerConfig();
        serverConfiguration.setAclConfig(AccessControlConfig.INSTANCE);
        if(serverConfig.isAclEnable()){
            File aclFile = serverConfig.getAclFile();
            if(aclFile != null && aclFile.exists()){
                AccessControlConfigParser aclConfigParser = new AccessControlConfigParser();
                ConfigParseResult<AccessControlConfig> aclConfigResult = aclConfigParser.parse(aclFile);
                AccessControlConfig accessControlConfig = aclConfigResult.getResult();
                if(accessControlConfig != null){
                    accessControlConfig.init(aclConfigResult);
                    //serverConfiguration.setAclConfig(accessControlConfig);
                }else {
                    logger.warn("not acl config defined.");
                }
                configParseResult.addAllCause(aclConfigResult.getCauses());
            }
        }
        return configParseResult;
    }
}
