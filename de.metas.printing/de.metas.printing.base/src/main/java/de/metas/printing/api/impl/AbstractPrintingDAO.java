package de.metas.printing.api.impl;

/*
 * #%L
 * de.metas.printing.base
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


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.print.attribute.standard.MediaSize;

import org.adempiere.ad.dao.IQueryBL;
import org.adempiere.ad.dao.IQueryBuilder;
import org.adempiere.ad.persistence.ModelDynAttributeAccessor;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.model.IContextAware;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;

import de.metas.printing.api.IPrintingDAO;
import de.metas.printing.model.I_AD_Print_Clients;
import de.metas.printing.model.I_AD_Printer;
import de.metas.printing.model.I_AD_PrinterHW;
import de.metas.printing.model.I_AD_PrinterHW_Calibration;
import de.metas.printing.model.I_AD_PrinterHW_MediaSize;
import de.metas.printing.model.I_AD_PrinterHW_MediaTray;
import de.metas.printing.model.I_AD_PrinterRouting;
import de.metas.printing.model.I_AD_PrinterTray_Matching;
import de.metas.printing.model.I_AD_Printer_Config;
import de.metas.printing.model.I_AD_Printer_Matching;
import de.metas.printing.model.I_C_Print_Job;
import de.metas.printing.model.I_C_Print_Job_Detail;
import de.metas.printing.model.I_C_Print_Job_Instructions;
import de.metas.printing.model.I_C_Print_Job_Line;
import de.metas.printing.model.I_C_Printing_Queue;
import de.metas.printing.model.I_C_Printing_Queue_Recipient;

public abstract class AbstractPrintingDAO implements IPrintingDAO
{
	/** Flag used to indicate if a change on {@link I_C_Printing_Queue_Recipient} shall NOT automatically trigger an update to {@link I_C_Printing_Queue#setPrintingQueueAggregationKey(String)} */
	private static final ModelDynAttributeAccessor<I_C_Printing_Queue_Recipient, Boolean> DYNATTR_DisableAggregationKeyUpdate = new ModelDynAttributeAccessor<>("DisableAggregationKeyUpdate", Boolean.class);
	
	@Override
	public Iterator<I_C_Print_Job_Line> retrievePrintJobLines(final I_C_Print_Job job)
	{
		return retrievePrintJobLines(job, SEQNO_First, SEQNO_Last);
	}

	@Override
	public Iterator<I_C_Print_Job_Line> retrievePrintJobLines(final I_C_Print_Job job, final int fromSeqNo, final int toSeqNo)
	{
		Check.assume(fromSeqNo == SEQNO_First || fromSeqNo > 0, "Valid fromSeqNo: {}", fromSeqNo);
		Check.assume(toSeqNo == SEQNO_Last || fromSeqNo > 0, "Valid toSeqNo: {}", toSeqNo);
		if (fromSeqNo != SEQNO_First && toSeqNo != SEQNO_Last)
		{
			Check.assume(fromSeqNo <= toSeqNo, "fromSeqNo({}) < toSeqNo({})", fromSeqNo, toSeqNo);
		}
		return retrievePrintJobLines0(job, fromSeqNo, toSeqNo);
	}

	protected abstract Iterator<I_C_Print_Job_Line> retrievePrintJobLines0(final I_C_Print_Job job, final int fromSeqNo, final int toSeqNo);

	@Override
	public I_C_Print_Job_Line retrievePrintJobLine(final I_C_Print_Job job, final int seqNo)
	{
		final int seqNoReal = resolveSeqNo(job, seqNo);
		Check.assume(seqNoReal > 0, "seqNo > 0");

		final Iterator<I_C_Print_Job_Line> lines = retrievePrintJobLines0(job, seqNoReal, seqNoReal);
		if (!lines.hasNext())
		{
			throw new AdempiereException("@NotFound@ @C_Print_Job_Line_ID@ (@SeqNo@:" + seqNoReal + ")");
		}

		final I_C_Print_Job_Line line = lines.next();
		if (lines.hasNext())
		{
			// shall not happen
			throw new AdempiereException("More then one line found for seqNo=" + seqNoReal + ": " + lines);
		}

		return line;
	}

	protected abstract int resolveSeqNo(final I_C_Print_Job job, final int seqNo);

	@Override
	public Iterator<I_C_Print_Job_Line> retrievePrintJobLines(final I_C_Print_Job_Instructions jobInstructions)
	{
		final I_C_Print_Job job = jobInstructions.getC_Print_Job();
		final int fromSeqNo = jobInstructions.getC_PrintJob_Line_From().getSeqNo();
		final int toSeqNo = jobInstructions.getC_PrintJob_Line_To().getSeqNo();

		return retrievePrintJobLines(job, fromSeqNo, toSeqNo);
	}

	@Override
	public List<I_C_Print_Job_Detail> retrievePrintJobDetails(final I_C_Print_Job_Line jobLine)
	{
		final List<I_C_Print_Job_Detail> details = retrievePrintJobDetailsIfAny(jobLine);

		Check.errorIf(details.isEmpty(), "Couldn't retrieve print job details for {}", jobLine);

		return details;
	}

	@Override
	public final List<I_C_Print_Job_Detail> retrievePrintJobDetailsIfAny(final I_C_Print_Job_Line jobLine)
	{
		final IQueryBL queryBL = Services.get(IQueryBL.class);

		final IQueryBuilder<I_C_Print_Job_Detail> queryBuilder = queryBL.createQueryBuilder(I_C_Print_Job_Detail.class, jobLine)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_C_Print_Job_Detail.COLUMNNAME_C_Print_Job_Line_ID, jobLine.getC_Print_Job_Line_ID());

		queryBuilder.orderBy()
				.addColumn(I_C_Print_Job_Detail.COLUMNNAME_C_Print_Job_Detail_ID);

		return queryBuilder
				.create()
				.list(I_C_Print_Job_Detail.class);
	}

	@Override
	public void removeMediaSizes(final List<I_AD_PrinterHW_MediaSize> sizes)
	{
		for (final I_AD_PrinterHW_MediaSize si : sizes)
		{
			InterfaceWrapperHelper.delete(si);
		}
	}

	@Override
	public void removeCalibrations(final List<I_AD_PrinterHW_Calibration> calibrations)
	{
		for (final I_AD_PrinterHW_Calibration cal : calibrations)
		{
			InterfaceWrapperHelper.delete(cal);
		}
	}

	@Override
	public void removeMediaTrays(final List<I_AD_PrinterHW_MediaTray> trays)
	{
		for (final I_AD_PrinterHW_MediaTray tr : trays)
		{
			InterfaceWrapperHelper.delete(tr);
		}
	}

	@Override
	public List<I_AD_Printer> retrievePrintersOrNull(final I_AD_PrinterHW printerHW)
	{
		final List<I_AD_Printer_Matching> matchings = retrievePrinterMatchings(printerHW);
		if (matchings == null)
		{
			return null;
		}

		final List<I_AD_Printer> printers = new ArrayList<I_AD_Printer>();
		for (I_AD_Printer_Matching matching : matchings)
		{
			printers.add(matching.getAD_Printer());
		}

		return printers;

	}

	@Override
	public I_AD_Printer_Config retrievePrinterConfig(final IContextAware ctx, String hostKey, int userToPrintId)
	{
		final IQueryBuilder<I_AD_Printer_Config> queryBuilder = Services.get(IQueryBL.class).createQueryBuilder(I_AD_Printer_Config.class, ctx)
				.addOnlyActiveRecordsFilter();

		if (!Check.isEmpty(hostKey, true))
		{
			queryBuilder.addEqualsFilter(I_AD_Printer_Config.COLUMN_HostKey, hostKey);
		}
		else
		{
			Check.errorIf(userToPrintId <= 0, "If the 'hostKey' param is empty, then the 'userToPrintId has to be > 0");
			queryBuilder.addEqualsFilter(I_AD_Printer_Config.COLUMN_CreatedBy, userToPrintId);
		}
		return queryBuilder
				.create()
				.firstOnly(I_AD_Printer_Config.class);
	}

	@Override
	public I_AD_Printer_Matching retrievePrinterMatching(final String hostKey, final I_AD_PrinterRouting routing)
	{
		final I_AD_Printer_Matching matching = retrievePrinterMatchingOrNull(hostKey, InterfaceWrapperHelper.create(routing.getAD_Printer(), I_AD_Printer.class));
		if (matching == null)
		{
			throw new AdempiereException("Couldn't retrieve printer matching for routing " + routing);
		}

		return matching;
	}

	@Override
	public I_AD_PrinterTray_Matching retrievePrinterTrayMatching(final I_AD_Printer_Matching matching, final I_AD_PrinterRouting routing, final boolean throwExIfMissing)
	{
		final I_AD_PrinterTray_Matching trayMatching = retrievePrinterTrayMatchingOrNull(matching, routing.getAD_Printer_Tray_ID());
		if (trayMatching == null && throwExIfMissing)
		{
			throw new AdempiereException("Couldn't retrieve printer tray matching for printer " + routing.getAD_Printer_ID());
		}

		return trayMatching;
	}

	@Override
	public List<Integer> retrievePrintingQueueRecipientIDs(final I_C_Printing_Queue printingQueue)
	{
		final List<Map<String, Object>> listDistinct = retrievePrintingQueueRecipientsQuery(printingQueue)
				.addOnlyActiveRecordsFilter()
				.create()
				.listDistinct(I_C_Printing_Queue_Recipient.COLUMNNAME_AD_User_ToPrint_ID);
		final List<Integer> result = new ArrayList<Integer>();
		for (Map<String, Object> distinct : listDistinct)
		{
			result.add((Integer)distinct.get(I_C_Printing_Queue_Recipient.COLUMNNAME_AD_User_ToPrint_ID));
		}
		return result;
	}

	private IQueryBuilder<I_C_Printing_Queue_Recipient> retrievePrintingQueueRecipientsQuery(final I_C_Printing_Queue item)
	{
		final IQueryBL queryBL = Services.get(IQueryBL.class);
		return queryBL.createQueryBuilder(I_C_Printing_Queue_Recipient.class, item)
				// .addOnlyActiveRecordsFilter() // we must retrieve ALL; the caller shall decide
				.addEqualsFilter(I_C_Printing_Queue_Recipient.COLUMN_C_Printing_Queue_ID, item.getC_Printing_Queue_ID())
				//
				.orderBy()
				.addColumn(I_C_Printing_Queue_Recipient.COLUMNNAME_C_Printing_Queue_Recipient_ID)
				.endOrderBy();

	}

	@Override
	public List<I_C_Printing_Queue_Recipient> retrieveAllPrintingQueueRecipients(final I_C_Printing_Queue item)
	{
		return retrievePrintingQueueRecipientsQuery(item)
				.create()
				.list(I_C_Printing_Queue_Recipient.class);
	}

	@Override
	public void deletePrintingQueueRecipients(final I_C_Printing_Queue item)
	{
		for (final I_C_Printing_Queue_Recipient recipient : retrieveAllPrintingQueueRecipients(item))
		{
			setDisableAggregationKeyUpdate(recipient);
			InterfaceWrapperHelper.delete(recipient);
		}
	}

	@Override
	public boolean isUpdatePrintingQueueAggregationKey(final I_C_Printing_Queue_Recipient recipient)
	{
		final Boolean disabled = DYNATTR_DisableAggregationKeyUpdate.getValue(recipient);
		return disabled != null && disabled ? false : true;
	}
	
	@Override
	public void setDisableAggregationKeyUpdate(final I_C_Printing_Queue_Recipient recipient)
	{
		DYNATTR_DisableAggregationKeyUpdate.setValue(recipient, true);
	}

	@Override
	public final I_AD_Print_Clients retrievePrintClientsEntry(final Properties ctx, final String hostKey)
	{
		return Services.get(IQueryBL.class).createQueryBuilder(I_AD_Print_Clients.class, ctx, ITrx.TRXNAME_None)
				// .addOnlyContextClient(ctx) // task 08022: AD_Print_Clients always have AD_Client_ID=0
				.addEqualsFilter(I_AD_Print_Clients.COLUMNNAME_HostKey, hostKey)
				.addOnlyActiveRecordsFilter()
				.create()
				.firstOnly(I_AD_Print_Clients.class);
	}

	@Override
	public final I_AD_PrinterHW_MediaSize retrieveMediaSize(final I_AD_PrinterHW hwPrinter,
			final MediaSize mediaSize,
			final boolean createIfNotExists)
	{
		Check.assume(hwPrinter != null, "Param 'hwPrinter' is not null");
		Check.assume(mediaSize != null, "Param 'mediaSize' is not null");

		final String mediaSizeName = mediaSize.getMediaSizeName().toString();
		final I_AD_PrinterHW_MediaSize result = Services.get(IQueryBL.class).createQueryBuilder(I_AD_PrinterHW_MediaSize.class, hwPrinter)
				.addOnlyActiveRecordsFilter()
				.addEqualsFilter(I_AD_PrinterHW_MediaSize.COLUMN_AD_PrinterHW_ID, hwPrinter.getAD_PrinterHW_ID())
				.addEqualsFilter(I_AD_PrinterHW_MediaSize.COLUMN_Name, mediaSizeName)
				.create()
				.setClient_ID()
				.firstOnly(I_AD_PrinterHW_MediaSize.class);

		if (result == null && createIfNotExists)
		{
			// task 08458
			final I_AD_PrinterHW_MediaSize newInstance = InterfaceWrapperHelper.newInstance(I_AD_PrinterHW_MediaSize.class, hwPrinter);
			newInstance.setAD_PrinterHW(hwPrinter);
			newInstance.setName(mediaSizeName);
			InterfaceWrapperHelper.save(newInstance);

			return newInstance;
		}

		Check.errorUnless(result != null, "Missing AD_PrinterHW_MediaSize with name {} for printer {}",
				mediaSizeName,
				hwPrinter.getName());
		return result;
	}
}
