package de.metas.materialtracking.process;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;

import org.adempiere.ad.process.ISvrProcessPrecondition;
import org.adempiere.document.service.IDocActionBL;
import org.adempiere.model.IContextAware;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.ILoggable;
import org.adempiere.util.Services;
import org.apache.commons.collections4.IteratorUtils;
import org.compiere.model.GridTab;
import org.compiere.model.I_C_Period;
import org.compiere.model.I_M_InOut;
import org.compiere.process.SvrProcess;

import de.metas.materialtracking.IMaterialTrackingDAO;
import de.metas.materialtracking.IMaterialTrackingPPOrderBL;
import de.metas.materialtracking.IMaterialTrackingReportDAO;
import de.metas.materialtracking.ch.lagerkonf.model.I_M_Material_Tracking_Report;
import de.metas.materialtracking.model.I_M_InOutLine;
import de.metas.materialtracking.model.I_M_Material_Tracking;
import de.metas.materialtracking.model.I_M_Material_Tracking_Ref;
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

public class M_Material_Tracking_Report_Line_Create
		extends SvrProcess
		implements ISvrProcessPrecondition
{

	@Override
	protected void prepare()
	{
		// nothing to do
	}

	@Override
	protected String doIt() throws Exception
	{
		final IMaterialTrackingDAO materialTrackingDAO = Services.get(IMaterialTrackingDAO.class);
		final IMaterialTrackingReportDAO materialTrackingReportDAO = Services.get(IMaterialTrackingReportDAO.class);

		final I_M_Material_Tracking_Report report = getRecord(I_M_Material_Tracking_Report.class);
		final I_C_Period period = report.getC_Period();

		final CreateMaterialTrackingReportLineFromMaterialTrackingRefAggregator workpackageAggregator = new CreateMaterialTrackingReportLineFromMaterialTrackingRefAggregator(report);

		workpackageAggregator.setItemAggregationKeyBuilder(new CreateMaterialTrackingReportLineFromMaterialTrackingRefKeyBuilder());
		workpackageAggregator.setGroupsBufferSize(100);

		// get all the material tracking entries that fit the given period
		final List<I_M_Material_Tracking> materialTrackings = materialTrackingDAO.retrieveMaterialTrackingsForPeriod(period);

		for (final I_M_Material_Tracking materialTracking : materialTrackings)
		{
			final Iterator<I_M_Material_Tracking_Ref> it = materialTrackingReportDAO.retrieveMaterialTrackingRefsForMaterialTracking(materialTracking);

			for (final I_M_Material_Tracking_Ref ref : IteratorUtils.asIterable(it))
			{
				if (isValid(ref, report))
				{
					workpackageAggregator.add(ref);
				}
			}

		}

		workpackageAggregator.closeAllGroups();

		report.setProcessed(true);
		InterfaceWrapperHelper.save(report);

		return "Success";
	}

	private boolean isValid(final I_M_Material_Tracking_Ref ref, final I_M_Material_Tracking_Report report)
	{
		final IDocActionBL docActionBL = Services.get(IDocActionBL.class);
		final IMaterialTrackingPPOrderBL materialTrackingPPOrderBL = Services.get(IMaterialTrackingPPOrderBL.class);

		final I_M_Material_Tracking materialTracking = ref.getM_Material_Tracking();
		final int productID = materialTracking.getM_Product_ID();

		final I_C_Period period = report.getC_Period();
		final Timestamp periodEndDate = period.getEndDate();
		final IContextAware reportCtx = InterfaceWrapperHelper.getContextAware(report);
		final String trxName = InterfaceWrapperHelper.getTrxName(report);

		int table_ID = ref.getAD_Table_ID();

		if (InterfaceWrapperHelper.getTableId(I_PP_Order.class) == table_ID)
		{

			final I_PP_Order ppOrder = InterfaceWrapperHelper.create(reportCtx.getCtx(), ref.getRecord_ID(), I_PP_Order.class, trxName);

			// shall never happen
			Check.assumeNotNull(ppOrder, "PP_Order not null in M_Material_Tracking_Ref: ", ref);

			// only closed pp_Orders are eligible
			if (!docActionBL.isStatusClosed(ppOrder))
			{
				return false; // this is a common and frequent case; don't pollute the loggable with it.
			}

			final Timestamp productionDate = materialTrackingPPOrderBL.getDateOfProduction(ppOrder);

			// Production date must be <= endDate
			if (productionDate.after(periodEndDate))
			{
				return false; // this is a common and frequent case; don't pollute the loggable with it.
			}

			// skip pp_Orders with other material trackings
			if (materialTracking.getM_Material_Tracking_ID() != ppOrder.getM_Material_Tracking_ID())
			{
				// should not happen because that would mean an inconsistent M_Material_Tracking_Ref
				ILoggable.THREADLOCAL.getLoggable().addLog(
						"Skipping {0} because it is referenced via M_Material_Tracking_Ref, but itself does not reference {1}",
						ppOrder, materialTracking);
				return false;
			}
		}

		else if (InterfaceWrapperHelper.getTableId(I_M_InOutLine.class) == table_ID)
		{
			final I_M_InOutLine iol = InterfaceWrapperHelper.create(reportCtx.getCtx(), ref.getRecord_ID(), I_M_InOutLine.class, trxName);

			// shall never happen
			Check.assumeNotNull(iol, "M_InOutLine not null in M_Material_Tracking_Ref: ", ref);

			final I_M_InOut io = iol.getM_InOut();

			// only completed and closed inouts are eligible
			if (!docActionBL.isStatusCompletedOrClosed(io))
			{
				return false;
			}
			final Timestamp movementDate = io.getMovementDate();

			// Movement date must be <= endDate
			if (movementDate.after(periodEndDate))
			{
				return false;
			}

			// skip inoutlines with other products
			if (productID != iol.getM_Product_ID())
			{
				return false;
			}

			// skip inout lines with other material tracking
			if (materialTracking.getM_Material_Tracking_ID() != iol.getM_Material_Tracking_ID())
			{
				return false; // should not happen because that would mean an inconsistent M_Material_Tracking_Ref
			}
		}

		else
		{
			return false;
		}
		return true;
	}

	@Override
	public boolean isPreconditionApplicable(final GridTab gridTab)
	{
		// This process is just for unprocessed reports
		final I_M_Material_Tracking_Report report = InterfaceWrapperHelper.create(gridTab, I_M_Material_Tracking_Report.class);
	
		return !report.isProcessed();
	}

}