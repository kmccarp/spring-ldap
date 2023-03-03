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
package org.springframework.ldap.transaction.compensating;

import org.junit.Before;
import org.junit.Test;
import org.springframework.ldap.core.LdapOperations;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.transaction.compensating.CompensatingTransactionOperationExecutor;

import javax.naming.ldap.LdapName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnbindOperationRecorderTest {
	private LdapOperations ldapOperationsMock;

	private TempEntryRenamingStrategy renamingStrategyMock;

	@Before
	public void setUp() throws Exception {
		ldapOperationsMock = mock(LdapOperations.class);

		renamingStrategyMock = mock(TempEntryRenamingStrategy.class);
	}

	@Test
	public void testRecordOperation() {
		final LdapName expectedTempName = LdapUtils.newLdapName(
				"cn=john doe_temp");
		final LdapName expectedDn = LdapUtils.newLdapName(
				"cn=john doe");
		UnbindOperationRecorder tested = new UnbindOperationRecorder(
				ldapOperationsMock, renamingStrategyMock);

		when(renamingStrategyMock.getTemporaryName(expectedDn))
				.thenReturn(expectedTempName);

		// Perform test
		CompensatingTransactionOperationExecutor operation = tested
				.recordOperation(new Object[] { expectedDn });

		// Verify result
		assertThat(operation instanceof UnbindOperationExecutor).isTrue();
		UnbindOperationExecutor rollbackOperation = (UnbindOperationExecutor) operation;
		assertThat(rollbackOperation.getLdapOperations()).isSameAs(ldapOperationsMock);
		assertThat(rollbackOperation.getOriginalDn()).isSameAs(expectedDn);
		assertThat(rollbackOperation.getTemporaryDn()).isSameAs(expectedTempName);
	}

}
