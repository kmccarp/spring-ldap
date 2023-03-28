/*
 * Copyright 2005-2016 the original author or authors.
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

package org.springframework.ldap.itest.control;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.itest.AbstractLdapTemplateIntegrationTest;
import org.springframework.ldap.itest.NoAdTest;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.test.context.ContextConfiguration;

import javax.naming.Name;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Provides tests that verify that the server supports certain controls.
 * 
 * @author Ulrik Sandberg
 */
@ContextConfiguration(locations = {"/conf/rootContextSourceTestContext.xml"})
public class SupportedControlsITest extends AbstractLdapTemplateIntegrationTest {
	/** must use a context source that has no base set */
	@Autowired
	private LdapTemplate tested;

	private static final String SUPPORTED_CONTROL = "supportedcontrol";

	@Override
	protected Name getRoot() {
		return LdapUtils.newLdapName(base);
	}
	
	@Test
	@Category(NoAdTest.class)
	public void testExpectedControlsSupported() throws Exception {
		/**
		 * Maps the 'supportedcontrol' attribute to a string array.
		 */
		ContextMapper mapper = ctx -> {
			DirContextAdapter adapter = (DirContextAdapter) ctx;
			return adapter.getStringAttributes(SUPPORTED_CONTROL);
		};

		String[] controls = (String[]) tested.lookup("", new String[] { SUPPORTED_CONTROL }, mapper);
		System.out.println(Arrays.toString(controls));

		HashSet<String> controlsSet = new HashSet<>(Arrays.asList(controls));

		assertThat(controlsSet.contains("1.3.6.1.4.1.4203.1.10.1")).as("Entry Change Notification LDAPv3 control,").isTrue();
		assertThat(controlsSet.contains("1.3.6.1.4.1.4203.1.10.1")).as("Subentries Control,").isTrue();
		assertThat(controlsSet.contains("2.16.840.1.113730.3.4.2")).as("Manage DSA IT LDAPv3 control,").isTrue();
	}
}
