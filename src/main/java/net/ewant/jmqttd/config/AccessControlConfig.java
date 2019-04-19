package net.ewant.jmqttd.config;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ewant.jmqttd.config.impl.AclPermissionAccess;
import net.ewant.jmqttd.interceptor.AccessControlInterceptor;
import net.ewant.jmqttd.interceptor.ConnectionAuthInterceptor;
import net.ewant.jmqttd.interceptor.Interceptor;
import net.ewant.jmqttd.interceptor.MessageFilterInterceptor;
import net.ewant.jmqttd.interceptor.PatternAccessControlInterceptor;
import net.ewant.jmqttd.interceptor.PatternConnectionAuthInterceptor;
import net.ewant.jmqttd.interceptor.SimpleAccessControlInterceptor;
import net.ewant.jmqttd.interceptor.SimpleConnectionAuthInterceptor;
import net.ewant.jmqttd.utils.ReflectUtil;

public class AccessControlConfig {

    static final AccessControlConfig INSTANCE = new AccessControlConfig();

    private static final String PLUGIN_FLAG = "plugin:";
    private static final String PATTERN_FLAG = "pattern:";

    private List<AccessControlInterceptor> accessControlInterceptors = new ArrayList<>();
    private List<MessageFilterInterceptor> messageFilterInterceptors = new ArrayList<>();
    private List<ConnectionAuthInterceptor> connectionAuthInterceptors = new ArrayList<>();

    private Map<Interceptor, AclPermissionAccess> interceptorPermissionAccess = new HashMap<>();

    private List<AclPermissionAccess> permissions = new ArrayList<>();
    
    private AclPermissionAccess defaultConnectPermissionAccess;

    private AccessControlConfig(){
    	this.defaultConnectPermissionAccess = new AclPermissionAccess();
		this.defaultConnectPermissionAccess.setPermission(AclPermissionAccess.AclPermission.ALLOW);
		this.defaultConnectPermissionAccess.setType(AclPermissionAccess.AclType.ALL);
		this.defaultConnectPermissionAccess.setAction(AclPermissionAccess.AclAction.CONN);
    }

    public void addPermission(AclPermissionAccess permissionAccess) {
        permissions.add(permissionAccess);
    }
    
    public AclPermissionAccess getPermission(Interceptor interceptor) {
    	return interceptorPermissionAccess.get(interceptor);
    }

    public List<AccessControlInterceptor> getAccessControlInterceptors() {
        return accessControlInterceptors;
    }

    public List<MessageFilterInterceptor> getMessageFilterInterceptors() {
        return messageFilterInterceptors;
    }

    public List<ConnectionAuthInterceptor> getConnectionAuthInterceptors() {
        return connectionAuthInterceptors;
    }

    public Map<Interceptor, AclPermissionAccess> getInterceptorPermissionAccess() {
        return interceptorPermissionAccess;
    }
    
    public AclPermissionAccess getDefaultConnectPermissionAccess() {
		return defaultConnectPermissionAccess;
	}

    public void init(ConfigParseResult<AccessControlConfig> configParseResult){

        List<AclPermissionAccess> aclConfigPermissions = this.permissions;
        if(aclConfigPermissions == null || aclConfigPermissions.isEmpty()){
            configParseResult.addCause(new ConfigParseException("empty acl config rule."));
            return;
        }
        for(AclPermissionAccess permissionAccess : aclConfigPermissions){
            AclPermissionAccess.AclAction accessAction = permissionAccess.getAction();
            switch (accessAction){
                case PUB:
                case SUB:
                case PUBSUB:
                    String typeValues = permissionAccess.getTypeValues();
                    if(typeValues != null && typeValues.trim().length() > 0){
                    	String[] interceptorGroup = typeValues.replaceAll(" ", "").split("\\|");
                    	for(String item : interceptorGroup){
                    		String[] values = item.split(",");
                    		if(item.startsWith(PLUGIN_FLAG)){
                    			values[0] = values[0].replace(PLUGIN_FLAG, "");
                    			List<AccessControlInterceptor> accessControlInterceptorList = this.buildInterceptor(configParseResult, values, AccessControlInterceptor.class);
                    			for (AccessControlInterceptor accessControlInterceptor : accessControlInterceptorList){
                    				accessControlInterceptors.add(accessControlInterceptor);
                    				interceptorPermissionAccess.put(accessControlInterceptor, permissionAccess);
                    			}
                    		}else if(item.startsWith(PATTERN_FLAG)){
                    			values[0] = values[0].replace(PATTERN_FLAG, "");
                    			PatternAccessControlInterceptor patternAccessControlInterceptor = new PatternAccessControlInterceptor(values);
                    			accessControlInterceptors.add(patternAccessControlInterceptor);
                    			interceptorPermissionAccess.put(patternAccessControlInterceptor, permissionAccess);
                    		}else{
                    			SimpleAccessControlInterceptor simpleAccessControlInterceptor = new SimpleAccessControlInterceptor(values);
                    			accessControlInterceptors.add(simpleAccessControlInterceptor);
                    			interceptorPermissionAccess.put(simpleAccessControlInterceptor, permissionAccess);
                    		}
                    	}
                    }
                    if(accessAction != AclPermissionAccess.AclAction.SUB){
                    	String msgFilter = permissionAccess.getMsgFilter();
                    	if(msgFilter != null && msgFilter.trim().length() > 0){
                    		String[] mfs = msgFilter.replace(" ", "").replace(PLUGIN_FLAG, "").split(",");
                    		List<MessageFilterInterceptor> messageFilterInterceptorList = this.buildInterceptor(configParseResult, mfs, MessageFilterInterceptor.class);
                    		for (MessageFilterInterceptor messageFilterInterceptor : messageFilterInterceptorList){
                    			messageFilterInterceptors.add(messageFilterInterceptor);
                    			interceptorPermissionAccess.put(messageFilterInterceptor, permissionAccess);
                    		}
                    	}
                    }
                    break;
                case CONN:
                    typeValues = permissionAccess.getTypeValues();
                    if(typeValues == null || typeValues.trim().length() == 0){
                    	continue;
                    }
                    String[] interceptorGroup = typeValues.replaceAll(" ", "").split("\\|");
                    for(String item : interceptorGroup){
                    	String[] values = item.split(",");
                    	if(item.startsWith(PLUGIN_FLAG)){
                    		values[0] = values[0].replace(PLUGIN_FLAG, "");
                            List<ConnectionAuthInterceptor> connectionAuthInterceptorList = this.buildInterceptor(configParseResult, values, ConnectionAuthInterceptor.class);
                            for (ConnectionAuthInterceptor connectionAuthInterceptor : connectionAuthInterceptorList){
                            	connectionAuthInterceptors.add(connectionAuthInterceptor);
                                interceptorPermissionAccess.put(connectionAuthInterceptor, permissionAccess);
                            }
                        }else if(item.startsWith(PATTERN_FLAG)){
                        	values[0] = values[0].replace(PATTERN_FLAG, "");
                            PatternConnectionAuthInterceptor patternConnectionAuthInterceptor = new PatternConnectionAuthInterceptor(values);
                            connectionAuthInterceptors.add(patternConnectionAuthInterceptor);
                            interceptorPermissionAccess.put(patternConnectionAuthInterceptor, permissionAccess);
                        }else{
                        	SimpleConnectionAuthInterceptor simpleConnectionAuthInterceptor = new SimpleConnectionAuthInterceptor(values);
                            connectionAuthInterceptors.add(simpleConnectionAuthInterceptor);
                            interceptorPermissionAccess.put(simpleConnectionAuthInterceptor, permissionAccess);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        if(!connectionAuthInterceptors.isEmpty()){
        	this.defaultConnectPermissionAccess = null;
        }
    }

    private <E> List<E> buildInterceptor(ConfigParseResult<AccessControlConfig> configParseResult, String[] plugins, Class<E> type){
        List<E> holder = new ArrayList<>();
        if(plugins != null){
            for(String plugin : plugins){
                String pluginName = plugin;
                try {
                    Class<?> pClass = Class.forName(pluginName);
                    if(!ReflectUtil.isSimpleInstance(pClass)){
                        configParseResult.addCause(new ConfigParseException("can not instantiation an abstract plugin : " + pluginName));
                        continue;
                    }else if(!type.isAssignableFrom(pClass)){
                        configParseResult.addCause(new ConfigParseException("plugin : " + pluginName + " must implements " + type.getName()));
                        continue;
                    }
                    try {
                        Constructor<?> constructor = pClass.getConstructor();
                        @SuppressWarnings("unchecked")
						E instance = (E) constructor.newInstance();
                        holder.add(instance);
                    } catch (NoSuchMethodException e) {
                        configParseResult.addCause(new ConfigParseException("can not an empty argument Constructor if plugin : " + pluginName));
                        continue;
                    } catch (Exception e) {
                        configParseResult.addCause(new ConfigParseException("failed to instantiation plugin : " + pluginName, e));
                        continue;
                    }
                } catch (ClassNotFoundException e) {
                    configParseResult.addCause(new ConfigParseException("not plugin class defined : " + pluginName,e));
                    continue;
                }
            }
        }

        return holder;
    }
    
}