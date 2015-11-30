package de.metas.materialtracking.qualityBasedInvoicing.ic.spi.impl;

import java.util.Iterator;
import java.util.Properties;

import org.adempiere.ad.modelvalidator.DocTimingType;
import org.adempiere.model.InterfaceWrapperHelper;

import de.metas.materialtracking.model.I_PP_Order;

/*
 * #%L
 * de.metas.materialtracking
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

public class PP_Order_MaterialTracking_Handler extends AbstractQualityInspectionHandler
{
	private final PP_Order_MaterialTracking_HandlerDAO dao = new PP_Order_MaterialTracking_HandlerDAO();

	@Override
	public String getSourceTable()
	{
		return org.eevolution.model.I_PP_Order.Table_Name;
	}

	@Override
	public DocTimingType getAutomaticallyCreateMissingCandidatesDocTiming()
	{
		return DocTimingType.AFTER_CLOSE;
	}

	/**
	 * @param ctx
	 * @param limit how many quality orders to retrieve
	 * @param trxName
	 * @return all PP_Orders which are suitable for invoices and there are no invoice candidates created yet.
	 */
	@Override
	public Iterator<I_PP_Order> retrieveAllModelsWithMissingCandidates(final Properties ctx, final int limit, final String trxName)
	{
		return dao.retrievePPOrdersWithMissingICs(ctx, limit, trxName);
	}

	@Override
	boolean isInvoiceable(final Object model)
	{
		final I_PP_Order ppOrder = InterfaceWrapperHelper.create(model, I_PP_Order.class);
		return dao.isInvoiceable(ppOrder);
	}
}