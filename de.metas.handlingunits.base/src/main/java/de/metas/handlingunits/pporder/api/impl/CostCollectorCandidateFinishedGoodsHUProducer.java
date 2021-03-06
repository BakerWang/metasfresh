package de.metas.handlingunits.pporder.api.impl;

/*
 * #%L
 * de.metas.handlingunits.base
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


import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;

import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_Product;
import org.eevolution.api.IReceiptCostCollectorCandidate;
import org.eevolution.model.I_PP_Order;

import de.metas.handlingunits.IHUAssignmentBL;
import de.metas.handlingunits.IHUContext;
import de.metas.handlingunits.allocation.IAllocationRequest;
import de.metas.handlingunits.allocation.IAllocationSource;
import de.metas.handlingunits.allocation.impl.AllocationUtils;
import de.metas.handlingunits.impl.IDocumentLUTUConfigurationManager;
import de.metas.handlingunits.model.I_M_HU;

/**
 * TODO: short description
 *
 * <ul>
 * <li>Input: TODO
 * <li>Output: TODO
 * </ul>
 *
 * @author RC
 *
 */
public class CostCollectorCandidateFinishedGoodsHUProducer extends AbstractPPOrderReceiptHUProducer
{

	private final IReceiptCostCollectorCandidate _costCollectorCandidate;

	private final I_PP_Order _ppOrder;

	public CostCollectorCandidateFinishedGoodsHUProducer(final IReceiptCostCollectorCandidate cand)
	{
		super(cand);

		Check.assumeNotNull(cand, "Cost collector candidate not null");

		_costCollectorCandidate = cand;

		final I_PP_Order ppOrder = cand.getPP_Order();

		Check.assumeNotNull(ppOrder, "PP Order not null");

		_ppOrder = ppOrder;
	}

	protected final I_PP_Order getPP_Order()
	{
		return _ppOrder;
	}

	protected final IReceiptCostCollectorCandidate getReceiptCostCollectorCandidate()
	{
		return _costCollectorCandidate;
	}

	@Override
	protected IAllocationSource createAllocationSource()
	{
		final de.metas.handlingunits.model.I_PP_Order ppOrder = InterfaceWrapperHelper.create(getPP_Order(), de.metas.handlingunits.model.I_PP_Order.class);

		final IAllocationSource allocationSource = huPPOrderBL.createAllocationSourceForPPOrder(ppOrder);

		return allocationSource;

	}

	@Override
	protected IDocumentLUTUConfigurationManager createReceiptLUTUConfigurationManager()
	{
		final I_PP_Order ppOrder = getPP_Order();
		return huPPOrderBL.createReceiptLUTUConfigurationManager(ppOrder);
	}

	@Override
	protected IAllocationRequest createAllocationRequest(final IHUContext huContext)
	{
		final IReceiptCostCollectorCandidate candidate = getReceiptCostCollectorCandidate();
		final I_C_UOM uom = candidate.getC_UOM();
		final BigDecimal qtyReceived = candidate.getQtyToReceive();
		final Timestamp date = candidate.getMovementDate();
		final I_M_Product product = candidate.getM_Product();

		final IAllocationRequest allocationRequest = AllocationUtils.createQtyRequest(huContext,
				product, // product
				qtyReceived, // the quantity we received
				uom,
				date, // transaction date
				getPP_Order() // referenced model
				);

		return allocationRequest;
	}

	@Override
	protected void setAssignedHUs(final Collection<I_M_HU> hus, final String trxName)
	{
		final I_PP_Order ppOrder = getPP_Order();

		Services.get(IHUAssignmentBL.class).setAssignedHandlingUnits(ppOrder, hus, trxName);

	}
}
