package net.ewant.jmqttd.utils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

public class ReflectUtil {

	public static Method getReadMethod(Class<?> clazz,String field) throws Exception {
		PropertyDescriptor descriptor = new PropertyDescriptor(field, clazz);
		Method writeMethod = descriptor.getReadMethod();
		return writeMethod;
	}
	
	public static Method getWriteMethod(Class<?> clazz,String field) throws Exception {
		PropertyDescriptor descriptor = new PropertyDescriptor(field, clazz);
		Method writeMethod = descriptor.getWriteMethod();
		return writeMethod;
	}
	
	/**
	 * 获取obj对象fieldName的Field
	 * @param obj
	 * @param fieldName
	 * @return
	 */
	public static Field getFieldByFieldName(Object obj, String fieldName) {
		if (obj == null || fieldName == null) {
			return null;
		}
		for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass
				.getSuperclass()) {
			try {
				return superClass.getDeclaredField(fieldName);
			} catch (Exception e) {
			}
		}
		return null;
	}

	/**
	 * 获取obj对象指定类型的Field
	 * @param obj
	 * @param fieldType
	 * @return
	 */
	public static Field getFieldByFieldType(Object obj, Class<?> fieldType) {
		if (obj == null || fieldType == null) {
			return null;
		}
		for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass
				.getSuperclass()) {
			try {
				Field[] fields = superClass.getDeclaredFields();
				for (Field field : fields){
					if(field.getType().isAssignableFrom(fieldType)){
						return field;
					}
				}
			} catch (Exception e) {
			}
		}
		return null;
	}

	/**
	 * 获取obj对象fieldName的属性值
	 * @param obj
	 * @param fieldName
	 * @return
	 */
	public static Object getValueByFieldName(Object obj, String fieldName) {
		Object value = null;
		try {
			Field field = getFieldByFieldName(obj, fieldName);
			if (field != null) {
				if (field.isAccessible()) {
					value = field.get(obj);
				} else {
					field.setAccessible(true);
					value = field.get(obj);
					field.setAccessible(false);
				}
			}
		} catch (Exception e) {
		}
		return value;
	}

	/**
	 * 获取obj对象fieldName的属性值
	 * @param obj
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getValueByFieldType(Object obj, Class<T> fieldType) {
		Object value = null;
		for (Class<?> superClass = obj.getClass(); superClass != Object.class; superClass = superClass
				.getSuperclass()) {
			try {
				Field[] fields = superClass.getDeclaredFields();
				for (Field f : fields) {
					if (fieldType.isAssignableFrom(f.getType())) {
						if (f.isAccessible()) {
							value = f.get(obj);
							break;
						} else {
							f.setAccessible(true);
							value = f.get(obj);
							f.setAccessible(false);
							break;
						}
					}
				}
				if (value != null) {
					break;
				}
			} catch (Exception e) {
			}
		}
		return (T) value;
	}

	/**
	 * 设置obj对象fieldName的属性值
	 * @param obj
	 * @param fieldName
	 * @param value
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	public static boolean setValueByFieldName(Object obj, String fieldName,
			Object value) {
		try {
			//java.lang.Class.getDeclaredField()方法用法实例教程 - 方法返回一个Field对象，它反映此Class对象所表示的类或接口的指定已声明字段。
			//此方法返回这个类中的指定字段的Field对象
			Field field = obj.getClass().getDeclaredField(fieldName);
		  /**
			* public void setAccessible(boolean flag)
            *       throws SecurityException将此对象的 accessible 标志设置为指示的布尔值。值为 true 则指示反射的对象在使用时应该取消 Java 语言访问检查。值为 false 则指示反射的对象应该实施 Java 语言访问检查。 
			* 	首先，如果存在安全管理器，则在 ReflectPermission("suppressAccessChecks") 权限下调用 checkPermission 方法。 
			* 	如果 flag 为 true，并且不能更改此对象的可访问性（例如，如果此元素对象是 Class 类的 Constructor 对象），则会引发 SecurityException。 
			* 	如果此对象是 java.lang.Class 类的 Constructor 对象，并且 flag 为 true，则会引发 SecurityException。 
			* 	参数：
			* 	flag - accessible 标志的新值 
 			* 	抛出： 
			* 	SecurityException - 如果请求被拒绝。
			*/
			if (field.isAccessible()) {//获取此对象的 accessible 标志的值。 
				field.set(obj, value);//将指定对象变量上此 Field 对象表示的字段设置为指定的新值
			} else {
				field.setAccessible(true);
				field.set(obj, value);
				field.setAccessible(false);
			}
			return true;
		} catch (Exception e) {
		}
		return false;
	}
	
	/**  
	  * 判断一个类是否为基本数据类型。  
	  * @param clazz 要判断的类。  
	  * @return true 表示为基本数据类型。  
	  */ 
	 public static boolean isBaseDataType(Class<?> clazz) {   
	     return (  
 		 clazz.isPrimitive() ||
	         clazz.equals(String.class) ||
	         clazz.equals(Integer.class)||   
	         clazz.equals(Long.class) ||   
	         clazz.equals(Double.class) ||   
	         clazz.equals(Float.class) ||   
	         clazz.equals(Boolean.class) ||   
	         clazz.equals(Character.class) ||  
	         clazz.equals(Short.class) || 
	         clazz.equals(Byte.class) ||
	         clazz.equals(BigDecimal.class) ||   
	         clazz.equals(BigInteger.class)   
	     ); 
	}

	public static boolean isSimpleInstance(Class<?> clazz) {
		return !(Modifier.isInterface(clazz.getModifiers()) ||
				Modifier.isAbstract(clazz.getModifiers()) ||
				Object.class.equals(clazz) ||
				clazz.isArray() ||
				Map.class.isAssignableFrom(clazz) ||
				Collection.class.isAssignableFrom(clazz) ||
				clazz.isEnum() || isBaseDataType(clazz)
		);
	}
	 
	public static <T> T getEnum(Class<T> enumType, String name) throws Exception {  
		T[] enumConstants = enumType.getEnumConstants();
		for (T t : enumConstants) {
			if(t.toString().equals(name)){
				return t;
			}
		}
		for (T t : enumConstants) {
			Field[] fields = t.getClass().getDeclaredFields();
			for (Field field : fields) {
				Object value = null;
				if (field.isAccessible()) {
					value  = field.get(t);
				} else {
					field.setAccessible(true);
					value = field.get(t);
					field.setAccessible(false);
				}
				if(value != null && value.toString().equals(name)){
					return t;
				}
			}
		}
		return null;
	} 
	/**
	 * 根据位置（在枚举类中的定义顺序，0开始）获取指定枚举值
	 * @param enumType
	 * @param ordinal
	 * @return
	 * @throws Exception
	 */
	public static <T> T getEnum(Class<T> enumType, int ordinal) throws Exception {  
		return enumType.getEnumConstants()[ordinal];
	}

	public static Object getValueForType(Class<?> parameterType, String value) throws Exception{
		try {
			if(parameterType.isPrimitive()){
                if(value == null){
                    value = "0";
                }
                if(int.class.isAssignableFrom(parameterType)){
                    return Integer.valueOf(value).intValue();
                }else if(long.class.isAssignableFrom(parameterType)){
                    return Long.valueOf(value).longValue();
                }else if(float.class.isAssignableFrom(parameterType)){
                    return Float.valueOf(value).floatValue();
                }else if(double.class.isAssignableFrom(parameterType)){
                    return Double.valueOf(value).doubleValue();
                }else if(boolean.class.isAssignableFrom(parameterType)){
                    String bol = value.equals("0") ? "false" : value.equals("1") ? "true" : value;
                    return Boolean.valueOf(bol).booleanValue();
                }else if(char.class.isAssignableFrom(parameterType)){
                    if(value.length() > 1){
                        return null;
                    }
                    return value.charAt(0);
                }else if(byte.class.isAssignableFrom(parameterType)){
                    return Byte.valueOf(value).byteValue();
                }else if(short.class.isAssignableFrom(parameterType)){
                    return Short.valueOf(value).shortValue();
                }
            }else{
                if(value == null){
                    return null;
                }else if(value.length() == 0 && !String.class.isAssignableFrom(parameterType) && !parameterType.isEnum()){
                    return null;
                }
                if(parameterType.isEnum()){
                    return getEnum(parameterType, value);
                }
                Constructor<?> constructor = null;
                if(Character.class.isAssignableFrom(parameterType)){
                    if(value.length() > 1){
                        return null;
                    }
                    constructor = parameterType.getConstructor(char.class);
                    return constructor.newInstance(value.charAt(0));
                }else{
                    String arg = value;
                    if(Boolean.class.isAssignableFrom(parameterType)){
                        arg = arg.equals("0") ? "false" : arg.equals("1") ? "true" : arg;
                    }
                    constructor = parameterType.getConstructor(String.class);
                    return constructor.newInstance(arg);
                }
            }
		} catch (Exception e) {
			throw e;
		}
		throw new IllegalArgumentException("cat not resolver [" + value + "] for type [" + parameterType.getName() +"], support base data type and enum only");
	}

	public static StackTraceElement getAvailableStack(Throwable cause){
		String name = ReflectUtil.class.getPackage().getName();
		String pname = name.substring(0, name.lastIndexOf("."));
		for(StackTraceElement stackTraceElement: cause.getStackTrace()){
			if(stackTraceElement.getClassName().contains(pname)){
				return stackTraceElement;
			}
		}
		return cause.getStackTrace()[0];
	}
}
