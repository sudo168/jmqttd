package net.ewant.jmqttd.config;

import net.ewant.jmqttd.utils.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class ConfigParser<T> {

    private static final Logger logger = LoggerFactory.getLogger(ConfigParser.class);

    protected ConfigParseResult<T> configParseResult = new ConfigParseResult<>();

    protected ConfigLoader configLoader;

    public ConfigParser(){
        this.configLoader = new ClassPathConfigLoader();
    }

    public ConfigParser(ConfigLoader configLoader){
        this.configLoader = configLoader;
    }

    class ConfigNode{
        private String name;
        public ConfigNode(String name){
            this.name = name;
        }
        public String getName() {
            return name;
        }
    }

    public ConfigParseResult<T> parse(String configPath) throws ConfigParseException {
        try {
            InputStream input = configLoader.load(configPath);
            if(input == null){
                logger.warn("config file [" + configPath + "] not found in classpath. try to find in filesystem...");
                return parse(new File(configPath));
            }
            URL url = getClass().getClassLoader().getResource(configPath);
            logger.info("using config : " + url);
            return parse(input);
        } catch (Exception e) {
            configParseResult.addCause(e);
            return configParseResult;
        }
    }

    public ConfigParseResult<T> parse(File configFile) throws ConfigParseException {
        try {
            if(!configFile.exists()){
                configParseResult.addCause(new IOException("unavailable to read a not exist config: " + configFile.toURI()));
                return configParseResult;
            }
            logger.info("using config : " + configFile.toURI());
            return parse(new FileInputStream(configFile));
        } catch (Exception e) {
            configParseResult.addCause(e);
            return configParseResult;
        }
    }

    public ConfigParseResult<T> parse(InputStream config) throws ConfigParseException {
        if(config == null){
            configParseResult.addCause(new IOException("config inputStream is NULL."));
            return configParseResult;
        }
        T configuration = null;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(config));
            Map<ConfigNode, List<String>> nodeMap = new LinkedHashMap<>();
            List<String> propertyList = null;

            String line;
            while ((line = reader.readLine()) != null){
                line = line.trim();
                if(line.startsWith("#") || line.isEmpty()){
                    continue;
                }
                // line = line.split("#")[0];
                if(line.startsWith("[") && line.endsWith("]")){
                    String node = line.substring(1, line.length() - 1);
                    propertyList = new ArrayList<>();
                    nodeMap.put(new ConfigNode(node), propertyList);
                    continue;
                }
                propertyList.add(line);
            }

            configuration = generateTypeObject();
            configParseResult.addResult(configuration);

            Object configNode = null;
            for (ConfigNode nodeKey : nodeMap.keySet()) {
                try {
                    if(configNode == null){
                        configNode = createConfigObject(configuration, nodeKey.getName());
                    }
                } catch (Exception e) {
                    configParseResult.addCause(new ConfigParseException("can not create config node by name ["+nodeKey.getName()+"]. " + e.getMessage(), e));
                    continue;
                }
                for (String nv : nodeMap.get(nodeKey)) {
                    setPropertyValue(configNode, nv);
                }
                if(configNode instanceof InitializingConfig){
                	((InitializingConfig) configNode).init();
                }
                configNode = null;
            }
            if(configuration instanceof InitializingConfig){
            	((InitializingConfig) configuration).init();
            }
        } catch (Exception e) {
            configParseResult.addCause(e);
        }

        return configParseResult;
    }

    protected abstract T generateTypeObject() throws Exception;

    protected abstract Object createConfigObject(T config, String nodeName) throws ConfigParseException;

    private void setPropertyValue(Object configNode, String nv) {
    	if(configNode == null || nv == null || nv.length() == 0){
            return;
        }
        String nameValue = nv.split("#")[0];
        int nameValueSpliterIndex = nameValue.indexOf('=');
        if(nameValueSpliterIndex == -1){
            throw new IllegalArgumentException("invalid setting [" + nv + "]");
        }
        Object current = configNode;
        String name = parsePropertyName(nameValue.substring(0, nameValueSpliterIndex));
        String[] ns = name.split("\\.");
        for (int i = 0; i < ns.length; i++) {
            Field field = ReflectUtil.getFieldByFieldName(current, ns[i]);
            if (field == null) {
            	return;
			}
            String value = parsePropertyValue(nameValue.substring(nameValueSpliterIndex + 1));
            current = setValue(current, field, value, i + 1 == ns.length);
        }
    }

    private Object setValue(Object configNode, Field field, String value, boolean canSet) {
        Class<?> parameterType = field.getType();
        Object v = null;
        try {
            if(InputStream.class.isAssignableFrom(parameterType)){
                v = new FileInputStream(value);
            }else if(File.class.isAssignableFrom(parameterType)){
                File file = new File(value);// from file system
                if(!file.exists()){
                    URL resource = getClass().getClassLoader().getResource(value);// form classpath
                    file = new File(resource.toURI());
                }
                if(!file.exists()){
                    logger.error("while apply property setting [{}.{}] file [{}] not exist!", configNode.getClass().getName(), field.getName(), file.toURI());
                    file = null;
                }
                v = file;
            }else if(ReflectUtil.isBaseDataType(parameterType) || parameterType.isEnum()){// base type & enum
                v = ReflectUtil.getValueForType(parameterType, value);
            }else if(parameterType.isArray()){// array
                Class<?> componentType = parameterType.getComponentType();
                String[] attrs = value.replaceAll(" ", "").split(",");
                if(String.class.isAssignableFrom(componentType)){
                    v = attrs;
                }
            }else if(List.class.isAssignableFrom(parameterType)){// list
                Type genericType = field.getGenericType();
                String[] attrs = value.replaceAll(" ", "").split(",");
                if(String.class.isAssignableFrom((Class<?>) genericType)){
                    v = Arrays.asList(attrs);
                }
            }else if(Collection.class.isAssignableFrom(parameterType)){// set & other
            }else if(Map.class.isAssignableFrom(parameterType)){// map
            }else{// bean
            	if(!field.isAccessible()){
                    field.setAccessible(true);
                }
                Object o = field.get(configNode);
                if(o == null){
                    o = field.getType().newInstance();
                    field.set(configNode, o);
                }
                return o;
            }
            if(v != null && canSet){
                if(!field.isAccessible()){
                    field.setAccessible(true);
                }
                field.set(configNode, v);
            }
        } catch (Exception e) {
            logger.error("while apply property setting [{}.{}] occur: {} [{}] , at {}", configNode.getClass().getName(), field.getName(), e.getClass().getName(), e.getMessage(), ReflectUtil.getAvailableStack(e));
        }
        return configNode;
    }

    /**
     * some-filed ==> someFiled , some_filed ==> someFiled
     * @param name
     * @return
     */
    private String parsePropertyName(String name) {
        String[] ns = name.contains("-") ? name.split("-") : name.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ns.length; i++) {
            String n = ns[i].trim();
            if(i == 0){
                sb.append(n);
            }else{
                sb.append(n.substring(0,1).toUpperCase());
                sb.append(n.substring(1));
            }

        }
        return sb.toString();
    }

    /**
     * ignore comment message (#)
     * @param value
     * @return
     */
    private String parsePropertyValue(String value) {
        if(value.contains("#")){
            return value.split("#")[0].trim();
        }
        return value.trim();
    }

}
