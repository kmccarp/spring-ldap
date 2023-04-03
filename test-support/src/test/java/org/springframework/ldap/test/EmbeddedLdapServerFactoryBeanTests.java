package org.springframework.ldap.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import javax.naming.directory.Attributes;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQueryBuilder;

public class EmbeddedLdapServerFactoryBeanTests {

	@Test
	public void testServerStartup() throws Exception {
		ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("/applicationContext.xml");
		LdapTemplate ldapTemplate = ctx.getBean(LdapTemplate.class);
		assertNotNull(ldapTemplate);

		List<String> list = ldapTemplate.search(LdapQueryBuilder.query().where("objectclass").is("person"),
	attrs -> (String) attrs.get("cn").get());
		assertEquals(5, list.size());
	}

}
