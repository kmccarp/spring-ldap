package org.springframework.ldap.odm.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.springframework.ldap.odm.test.utils.ExecuteRunnable;
import org.springframework.ldap.odm.typeconversion.ConverterManager;
import org.springframework.ldap.odm.typeconversion.impl.Converter;
import org.springframework.ldap.odm.typeconversion.impl.ConverterManagerFactoryBean;

public class TestManagerConverterFactory {
	private static class NullConverter implements Converter {
		public <T> T convert(Object source, Class<T> toClass) throws Exception {
			return null;
		}
	}
	private static final Converter nullConverter=new NullConverter();

	private static final class ConverterConfigTestData {
		private final Class<?>[] fromClasses;
		private final String syntax;
		private final Class<?>[] toClasses;

		private ConverterConfigTestData(Class<?>[] fromClasses, String syntax, Class<?>[] toClasses) {
			this.fromClasses=fromClasses;
			this.syntax=syntax;
			this.toClasses=toClasses;
		}
	}
	
	private static ConverterConfigTestData[] converterConfigTestData=new ConverterConfigTestData[] {
		new ConverterConfigTestData(new Class<?>[] { String.class },						"",	new Class<?>[] { Integer.class }),
		new ConverterConfigTestData(new Class<?>[] { Byte.class, java.lang.Integer.class }, "",	new Class<?>[] { String.class, Long.class }),
		new ConverterConfigTestData(new Class<?>[] { String.class },						"123", new Class<?>[] { java.net.URI.class }),
	};

	private static final class ConverterTestData {
		private final Class<?> fromClass;
		private final String syntax;
		private final Class<?> toClass;
		private final boolean canConvert;
		
		private ConverterTestData(Class<?> fromClass, String syntax, Class<?> toClass, boolean canConvert) {
			this.fromClass=fromClass;
			this.syntax=syntax;
			this.toClass=toClass;
			this.canConvert=canConvert;
		}
	}
   
	private ConverterTestData[] converterTestData=new ConverterTestData[] {
			new ConverterTestData(java.lang.String.class, "", java.lang.Integer.class, true),
			new ConverterTestData(java.lang.Byte.class, "", java.lang.Long.class, true),
			new ConverterTestData(java.lang.Integer.class, "444", java.lang.String.class, true),
			new ConverterTestData(java.lang.String.class, "123", java.net.URI.class, true),
			new ConverterTestData(java.lang.String.class, "123", java.lang.Byte.class, false),
			new ConverterTestData(java.lang.Byte.class, "", java.lang.Integer.class, false)
	};
	
	@Test
	public void testConverterFactory() throws Exception {
		ConverterManagerFactoryBean converterManagerFactory=new ConverterManagerFactoryBean();
		Set<ConverterManagerFactoryBean.ConverterConfig> configList=new HashSet<>();
		for (ConverterConfigTestData config:converterConfigTestData) {
			ConverterManagerFactoryBean.ConverterConfig converterConfig=new ConverterManagerFactoryBean.ConverterConfig();
			converterConfig.setFromClasses(new HashSet<>(Arrays.asList(config.fromClasses)));
			converterConfig.setSyntax(config.syntax);
			converterConfig.setToClasses(new HashSet<>(Arrays.asList(config.toClasses)));
			converterConfig.setConverter(nullConverter);
			configList.add(converterConfig);
		}
		converterManagerFactory.setConverterConfig(configList);
		final ConverterManager converterManager=(ConverterManager)converterManagerFactory.getObject();
		
		new ExecuteRunnable<ConverterTestData>().runTests(testData -> {
			assertEquals(testData.canConvert,
					converterManager.canConvert(testData.fromClass, testData.syntax, testData.toClass));
		}, converterTestData);
	}
}
