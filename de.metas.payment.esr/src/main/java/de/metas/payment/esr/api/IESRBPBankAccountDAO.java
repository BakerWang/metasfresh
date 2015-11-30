package de.metas.payment.esr.api;

/*
 * #%L
 * de.metas.payment.esr
 * %%
 * Copyright (C) 2015 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */


import java.util.List;

import org.adempiere.util.ISingletonService;
import org.compiere.model.I_AD_Org;

import de.metas.payment.esr.model.I_C_BP_BankAccount;

public interface IESRBPBankAccountDAO extends ISingletonService
{
	/**
	 * Search for the ESR bank account(s) of the given org's linked partner.
	 * 
	 * @param org
	 * @return account(s) if exist, throw exception otherwise. If there is more than one ESR account, then the one with IsDefaultESR='Y' is returned first.
	 */
	public List<I_C_BP_BankAccount> fetchOrgEsrAccounts(I_AD_Org org);
	
	/**
	 * @param postAccountNo
	 * @param innerAccountNo
	 * @return {@link I_C_BP_BankAccount} (unique) corresponding to the ESR accountNo and inner-bank accountNo or <code>null</code> if none was found
	 */
	I_C_BP_BankAccount retrieveESRBPBankAccountOrNull(String postAccountNo, String innerAccountNo);
}