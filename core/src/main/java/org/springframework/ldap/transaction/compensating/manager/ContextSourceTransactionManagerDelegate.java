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

package org.springframework.ldap.transaction.compensating.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ldap.core.ContextSource;
import org.springframework.ldap.core.support.AbstractContextSource;
import org.springframework.ldap.transaction.compensating.LdapCompensatingTransactionOperationFactory;
import org.springframework.ldap.transaction.compensating.TempEntryRenamingStrategy;
import org.springframework.ldap.transaction.compensating.support.DefaultTempEntryRenamingStrategy;
import org.springframework.transaction.compensating.support.AbstractCompensatingTransactionManagerDelegate;
import org.springframework.transaction.compensating.support.CompensatingTransactionHolderSupport;
import org.springframework.transaction.compensating.support.DefaultCompensatingTransactionOperationManager;
import org.springframework.util.Assert;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;

/**
 * This delegate performs all the work for the
 * {@link ContextSourceTransactionManager}. The work is delegated in order to
 * be able to perform the exact same work for the LDAP part in
 * {@link ContextSourceAndDataSourceTransactionManager}.
 * 
 * @author Mattias Hellborg Arthursson
 * @see ContextSourceTransactionManager
 * @see ContextSourceAndDataSourceTransactionManager
 * @since 1.2
 */
public class ContextSourceTransactionManagerDelegate extends
		AbstractCompensatingTransactionManagerDelegate {

	private static final Logger LOG = LoggerFactory.getLogger(ContextSourceTransactionManagerDelegate.class);

	private ContextSource contextSource;

	private TempEntryRenamingStrategy renamingStrategy;

	/**
	 * Set the ContextSource to work on. Even though the actual ContextSource
	 * sent to the LdapTemplate instance should be a
	 * {@link TransactionAwareContextSourceProxy}, the one sent to this method
	 * should be the target of that proxy. If it is not, the target will be
	 * extracted and used instead.
	 * 
	 * @param contextSource
	 *			the ContextSource to work on.
	 */
	public void setContextSource(ContextSource contextSource) {
		if (contextSource instanceof TransactionAwareContextSourceProxy) {
			TransactionAwareContextSourceProxy proxy = (TransactionAwareContextSourceProxy) contextSource;
			this.contextSource = proxy.getTarget();
		} else {
			this.contextSource = contextSource;
		}

		if (contextSource instanceof AbstractContextSource) {
			AbstractContextSource abstractContextSource = (AbstractContextSource) contextSource;
			if(abstractContextSource.isAnonymousReadOnly()) {
				throw new IllegalArgumentException(
						"Compensating LDAP transactions cannot be used when context-source is anonymous-read-only");
			}
		}
	}

	public ContextSource getContextSource() {
		return contextSource;
	}

	/*
	 * @see org.springframework.transaction.compensating.support.AbstractCompensatingTransactionManagerDelegate#getTransactionSynchronizationKey()
	 */
	protected Object getTransactionSynchronizationKey() {
		return getContextSource();
	}

	/*
	 * @see org.springframework.transaction.compensating.support.AbstractCompensatingTransactionManagerDelegate#getNewHolder()
	 */
	protected CompensatingTransactionHolderSupport getNewHolder() {
		DirContext newCtx = getContextSource().getReadWriteContext();
		return new DirContextHolder(
				new DefaultCompensatingTransactionOperationManager(
						new LdapCompensatingTransactionOperationFactory(
								renamingStrategy)), newCtx);
	}

	/*
	 * @see org.springframework.transaction.compensating.support.AbstractCompensatingTransactionManagerDelegate#closeTargetResource(org.springframework.transaction.compensating.support.CompensatingTransactionHolderSupport)
	 */
	protected void closeTargetResource(
			CompensatingTransactionHolderSupport transactionHolderSupport) {
		DirContextHolder contextHolder = (DirContextHolder) transactionHolderSupport;
		DirContext ctx = contextHolder.getCtx();

		try {
			LOG.debug("Closing target context");
			ctx.close();
		} catch (NamingException e) {
			LOG.warn("Failed to close target context", e);
		}
	}

	/**
	 * Set the {@link TempEntryRenamingStrategy} to be used when renaming
	 * temporary entries in unbind and rebind operations. Default value is a
	 * {@link DefaultTempEntryRenamingStrategy}.
	 * 
	 * @param renamingStrategy
	 *			the {@link TempEntryRenamingStrategy} to use.
	 */
	public void setRenamingStrategy(TempEntryRenamingStrategy renamingStrategy) {
		this.renamingStrategy = renamingStrategy;
	}

	void checkRenamingStrategy() {
		Assert.notNull(renamingStrategy, "RenamingStrategy must be specified");
	}
}
