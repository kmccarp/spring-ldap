/*
 * Copyright 2005-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.ldap.itest.core.simple;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.ldap.LdapName;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.NameNotFoundException;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.DirContextProcessor;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.itest.AbstractLdapTemplateIntegrationTests;
import org.springframework.ldap.itest.NoAdTests;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@ContextConfiguration(locations = { "/conf/simpleLdapTemplateTestContext.xml" })
public class SimpleLdapTemplateITests extends AbstractLdapTemplateIntegrationTests {

	private static String dnString = "cn=Some Person4,ou=company1,ou=Sweden";

	private static LdapName dn = LdapUtils.newLdapName("cn=Some Person4,ou=company1,ou=Sweden");

	@Autowired
	private LdapTemplate ldapTemplate;

	@Test
	public void testLookup() {
		String result = ldapTemplate.lookup("cn=Some Person,ou=company1,ou=Sweden", new CnContextMapper());
		assertThat(result).isEqualTo("Some Person");
	}

	@Test
	public void testLookupName() {
		String result = ldapTemplate.lookup(LdapUtils.newLdapName("cn=Some Person,ou=company1,ou=Sweden"),
				new CnContextMapper());
		assertThat(result).isEqualTo("Some Person");
	}

	@Test
	public void testSearch() {
		List<String> cns = ldapTemplate.search("", "(&(objectclass=person)(sn=Person3))", new CnContextMapper());

		assertThat(cns).hasSize(1);
		assertThat(cns.get(0)).isEqualTo("Some Person3");
	}

	@Test
	public void testSearchForObject() {
		String cn = ldapTemplate.searchForObject("", "(&(objectclass=person)(sn=Person3))", new CnContextMapper());
		assertThat(cn).isEqualTo("Some Person3");
	}

	@Test
	public void testSearchProcessor() {
		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		DummyDirContextProcessor processor = new DummyDirContextProcessor();

		List<String> cns = ldapTemplate.search("", "(&(objectclass=person)(sn=Person3))", searchControls,
				new CnContextMapper(), processor);

		assertThat(cns).hasSize(1);
		assertThat(cns.get(0)).isEqualTo("Some Person3");
		assertThat(processor.isPreProcessCalled()).isTrue();
		assertThat(processor.isPostProcessCalled()).isTrue();
	}

	@Test
	public void testSearchProcessorName() {
		SearchControls searchControls = new SearchControls();
		searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
		DummyDirContextProcessor processor = new DummyDirContextProcessor();

		List<String> cns = ldapTemplate.search(LdapUtils.emptyLdapName(), "(&(objectclass=person)(sn=Person3))",
				searchControls, new CnContextMapper(), processor);

		assertThat(cns).hasSize(1);
		assertThat(cns.get(0)).isEqualTo("Some Person3");
		assertThat(processor.isPreProcessCalled()).isTrue();
		assertThat(processor.isPostProcessCalled()).isTrue();
	}

	@Test
	public void testSearchName() {
		List<String> cns = ldapTemplate.search(LdapUtils.emptyLdapName(), "(&(objectclass=person)(sn=Person3))",
				new CnContextMapper());

		assertThat(cns).hasSize(1);
		assertThat(cns.get(0)).isEqualTo("Some Person3");
	}

	@Test
	public void testModifyAttributes() {
		DirContextOperations ctx = ldapTemplate.lookupContext("cn=Some Person,ou=company1,ou=Sweden");

		ctx.setAttributeValue("description", "updated description");
		ctx.setAttributeValue("telephoneNumber", "0000001");

		ldapTemplate.modifyAttributes(ctx);

		// verify that the data was properly updated.
		ldapTemplate.lookup("cn=Some Person,ou=company1,ou=Sweden", new ContextMapper<Object>() {
			public Object mapFromContext(Object ctx) {
				DirContextAdapter adapter = (DirContextAdapter) ctx;
				assertThat(adapter.getStringAttribute("description")).isEqualTo("updated description");
				assertThat(adapter.getStringAttribute("telephoneNumber")).isEqualTo("0000001");
				return null;
			}
		});
	}

	@Test
	public void testModifyAttributesName() {
		DirContextOperations ctx = ldapTemplate
				.lookupContext(LdapUtils.newLdapName("cn=Some Person,ou=company1,ou=Sweden"));

		ctx.setAttributeValue("description", "updated description");
		ctx.setAttributeValue("telephoneNumber", "0000001");

		ldapTemplate.modifyAttributes(ctx);

		// verify that the data was properly updated.
		ldapTemplate.lookup("cn=Some Person,ou=company1,ou=Sweden", ctx -> {
			DirContextAdapter adapter = (DirContextAdapter) ctx;
			assertThat(adapter.getStringAttribute("description")).isEqualTo("updated description");
			assertThat(adapter.getStringAttribute("telephoneNumber")).isEqualTo("0000001");
			return null;
		});
	}

	@Test
	public void testBindAndUnbind() {
		DirContextAdapter adapter = new DirContextAdapter();
		adapter.setAttributeValues("objectclass", new String[] { "top", "person" });
		adapter.setAttributeValue("cn", "Some Person4");
		adapter.setAttributeValue("sn", "Person4");

		ldapTemplate.bind(dnString, adapter, null);
		verifyBoundCorrectData();
		ldapTemplate.unbind(dnString);
		verifyCleanup();
	}

	@Test
	public void testBindAndUnbindName() {
		DirContextAdapter adapter = new DirContextAdapter();
		adapter.setAttributeValues("objectclass", new String[] { "top", "person" });
		adapter.setAttributeValue("cn", "Some Person4");
		adapter.setAttributeValue("sn", "Person4");

		ldapTemplate.bind(dn, adapter, null);
		verifyBoundCorrectData();
		ldapTemplate.unbind(dn);
		verifyCleanup();
	}

	@Test
	public void testBindAndUnbindWithDirContextAdapter() {
		DirContextAdapter adapter = new DirContextAdapter(dn);
		adapter.setAttributeValues("objectclass", new String[] { "top", "person" });
		adapter.setAttributeValue("cn", "Some Person4");
		adapter.setAttributeValue("sn", "Person4");

		ldapTemplate.bind(adapter);
		verifyBoundCorrectData();
		ldapTemplate.unbind(dn);
		verifyCleanup();
	}

	@Test
	@Category(NoAdTests.class)
	public void testAuthenticate() {
		AndFilter filter = new AndFilter();
		filter.and(new EqualsFilter("objectclass", "person")).and(new EqualsFilter("uid", "some.person3"));
		assertThat(ldapTemplate.authenticate("", filter.toString(), "password")).isTrue();
	}

	private void verifyBoundCorrectData() {
		DirContextOperations result = ldapTemplate.lookupContext(dnString);
		assertThat(result.getStringAttribute("cn")).isEqualTo("Some Person4");
		assertThat(result.getStringAttribute("sn")).isEqualTo("Person4");
	}

	private void verifyCleanup() {
		try {
			ldapTemplate.lookupContext(dnString);
			fail("NameNotFoundException expected");
		}
		catch (NameNotFoundException expected) {
			assertThat(true).isTrue();
		}
	}

	private static final class CnContextMapper implements ContextMapper<String> {

		public String mapFromContext(Object ctx) {
			DirContextAdapter adapter = (DirContextAdapter) ctx;

			return adapter.getStringAttribute("cn");
		}

	}

	private static final class DummyDirContextProcessor implements DirContextProcessor {

		private boolean preProcessCalled;

		private boolean postProcessCalled;

		public boolean isPreProcessCalled() {
			return preProcessCalled;
		}

		public boolean isPostProcessCalled() {
			return postProcessCalled;
		}

		public void postProcess(DirContext ctx) throws NamingException {
			preProcessCalled = true;
		}

		public void preProcess(DirContext ctx) throws NamingException {
			postProcessCalled = true;
		}

	}

}
