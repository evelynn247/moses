package com.biyao.moses.util;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.FatalBeanException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 增加方法，只复制当前字段值不为空的数据
 * 
 * @author monkey
 * @date 2018年9月8日
 */
public class MyBeanUtil extends BeanUtils {

	public static void copyNotNullProperties(Object source, Object target) throws BeansException {
		copyNotNullProperties(source, target, null, (String[]) null);
	}

	private static void copyNotNullProperties(Object source, Object target, @Nullable Class<?> editable,
			@Nullable String... ignoreProperties) throws BeansException {

		Assert.notNull(source, "Source must not be null");
		Assert.notNull(target, "Target must not be null");

		Class<?> actualEditable = target.getClass();
		if (editable != null) {
			if (!editable.isInstance(target)) {
				throw new IllegalArgumentException("Target class [" + target.getClass().getName()
						+ "] not assignable to Editable class [" + editable.getName() + "]");
			}
			actualEditable = editable;
		}
		PropertyDescriptor[] targetPds = getPropertyDescriptors(actualEditable);
		List<String> ignoreList = (ignoreProperties != null ? Arrays.asList(ignoreProperties) : null);

		for (PropertyDescriptor targetPd : targetPds) {
			Method writeMethod = targetPd.getWriteMethod();
			if (writeMethod != null && (ignoreList == null || !ignoreList.contains(targetPd.getName()))) {

				Method trm = targetPd.getReadMethod();
				if (trm != null) {
					if (!Modifier.isPublic(trm.getDeclaringClass().getModifiers())) {
						trm.setAccessible(true);
					}
					try {
						Object tarValue = trm.invoke(target);
						if (tarValue == null) {
							PropertyDescriptor sourcePd = getPropertyDescriptor(source.getClass(), targetPd.getName());
							if (sourcePd != null) {
								Method readMethod = sourcePd.getReadMethod();
								if (readMethod != null && ClassUtils.isAssignable(writeMethod.getParameterTypes()[0],
										readMethod.getReturnType())) {
									try {
										if (!Modifier.isPublic(readMethod.getDeclaringClass().getModifiers())) {
											readMethod.setAccessible(true);
										}
										Object value = readMethod.invoke(source);
										if (!Modifier.isPublic(writeMethod.getDeclaringClass().getModifiers())) {
											writeMethod.setAccessible(true);
										}
										writeMethod.invoke(target, value);
									} catch (Throwable ex) {
										throw new FatalBeanException("Could not copy property '" + targetPd.getName()
												+ "' from source to target", ex);
									}
								}
							}
						}
					} catch (Throwable ex) {
						throw new FatalBeanException(
								"Could not get property '" + targetPd.getName() + "' from source to target", ex);
					}
				}

			}
		}
	}

}
