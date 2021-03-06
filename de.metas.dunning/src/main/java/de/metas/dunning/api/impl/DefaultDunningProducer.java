package de.metas.dunning.api.impl;

/*
 * #%L
 * de.metas.dunning
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import de.metas.logging.LogManager;

import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.util.Env;
import org.compiere.util.TimeUtil;

import de.metas.document.IDocumentLocationBL;
import de.metas.dunning.api.IDunningBL;
import de.metas.dunning.api.IDunningContext;
import de.metas.dunning.api.IDunningDAO;
import de.metas.dunning.api.IDunningProducer;
import de.metas.dunning.api.IDunningUtil;
import de.metas.dunning.interfaces.I_C_DunningLevel;
import de.metas.dunning.model.I_C_DunningDoc;
import de.metas.dunning.model.I_C_DunningDoc_Line;
import de.metas.dunning.model.I_C_DunningDoc_Line_Source;
import de.metas.dunning.model.I_C_Dunning_Candidate;
import de.metas.dunning.spi.IDunningAggregator;

public class DefaultDunningProducer implements IDunningProducer
{
	private final static transient Logger logger = LogManager.getLogger(DefaultDunningProducer.class);

	private IDunningContext dunningContext;

	private final CompositeDunningAggregator dunningAggregators = new CompositeDunningAggregator();

	private I_C_DunningDoc dunningDoc = null;
	private I_C_DunningDoc_Line dunningDocLine = null;
	private final List<I_C_DunningDoc_Line_Source> dunningDocLineSources = new ArrayList<I_C_DunningDoc_Line_Source>();

	@Override
	public void setDunningContext(IDunningContext context)
	{
		Check.assumeNotNull(context, "context not null");
		this.dunningContext = context;
	}

	public IDunningContext getDunningContext()
	{
		return dunningContext;
	}

	@Override
	public void addCandidate(I_C_Dunning_Candidate candidate)
	{
		if (dunningDoc != null && dunningAggregators.isNewDunningDoc(candidate))
		{
			completeDunningDoc();
			dunningDoc = null;
			dunningDocLine = null;
		}

		if (dunningDoc == null)
		{
			dunningDoc = createDunningDoc(candidate);
			dunningDocLine = null;
		}

		if (dunningDocLine != null && dunningAggregators.isNewDunningDocLine(candidate))
		{
			dunningDocLine = null;
		}

		if (dunningDocLine == null)
		{
			dunningDocLine = createDunningDocLine(candidate);
			createDunningDocLineSource(candidate);
		}
		else
		{
			aggregateDunningDocLine(dunningDocLine, candidate);
		}
	}

	protected I_C_DunningDoc createDunningDoc(final I_C_Dunning_Candidate candidate)
	{
		final IDunningDAO dunningDAO = Services.get(IDunningDAO.class);
		final IDunningContext context = getDunningContext();

		final I_C_DunningDoc doc = dunningDAO.newInstance(getDunningContext(), I_C_DunningDoc.class);
		doc.setAD_Org_ID(candidate.getAD_Org_ID());

		// Validate and set Dunning Level
		final I_C_DunningLevel contextDunningLevel = context.getC_DunningLevel();
		if (contextDunningLevel == null)
		{
			doc.setC_DunningLevel_ID(candidate.getC_DunningLevel_ID());
		}
		else
		{
			if (contextDunningLevel.getC_DunningLevel_ID() != candidate.getC_DunningLevel_ID())
			{
				logger.warn("Candidate {} has dunning level {} but in context we have {}. Using candidate's dunning level",
						new Object[] { candidate, candidate.getC_DunningLevel(), contextDunningLevel });
			}

			doc.setC_DunningLevel_ID(candidate.getC_DunningLevel_ID());
		}

		// Use DunningDate from context (if available), else candidate's dunning date shall be used
		final Date contextDunningDate = context.getDunningDate();
		if (contextDunningDate != null)
		{
			doc.setDunningDate(TimeUtil.asTimestamp(contextDunningDate));
		}
		else
		{
			doc.setDunningDate(candidate.getDunningDate());
		}

		doc.setC_BPartner_ID(candidate.getC_BPartner_ID());
		doc.setC_BPartner_Location_ID(candidate.getC_BPartner_Location_ID());
		doc.setC_Dunning_Contact_ID(candidate.getC_Dunning_Contact_ID());
		doc.setIsActive(true);
		doc.setProcessed(false);

		Services.get(IDocumentLocationBL.class).setBPartnerAddress(new DunningDocDocumentLocationAdapter(doc));

		return doc;
	}

	protected I_C_DunningDoc_Line createDunningDocLine(final I_C_Dunning_Candidate candidate)
	{
		final IDunningDAO dunningDAO = Services.get(IDunningDAO.class);

		final BigDecimal amt = getAmount(candidate);
		final int salesRepId = getSalesRep_ID(candidate);

		if (dunningDoc.getC_DunningDoc_ID() <= 0)
		{
			dunningDAO.save(dunningDoc);
		}

		final I_C_DunningDoc_Line line = dunningDAO.newInstance(getDunningContext(), I_C_DunningDoc_Line.class);
		line.setAD_Org_ID(candidate.getAD_Org_ID());
		line.setC_DunningDoc(dunningDoc);
		line.setC_DunningLevel_ID(candidate.getC_DunningLevel_ID());
		line.setC_BPartner_ID(candidate.getC_BPartner_ID());
		line.setC_Dunning_Contact_ID(candidate.getC_Dunning_Contact_ID());
		line.setAmt(amt);
		line.setC_Currency_ID(candidate.getC_Currency_ID());
		line.setSalesRep_ID(salesRepId);
		line.setIsActive(true);
		line.setProcessed(false);

		return line;
	}

	protected I_C_DunningDoc_Line_Source createDunningDocLineSource(final I_C_Dunning_Candidate candidate)
	{
		final IDunningDAO dunningDAO = Services.get(IDunningDAO.class);

		if (dunningDocLine.getC_DunningDoc_Line_ID() <= 0)
		{
			dunningDAO.save(dunningDocLine);
		}

		final I_C_DunningDoc_Line_Source source = dunningDAO.newInstance(getDunningContext(), I_C_DunningDoc_Line_Source.class);
		source.setAD_Org_ID(candidate.getAD_Org_ID());
		source.setC_Dunning_Candidate(candidate);
		source.setC_DunningDoc_Line(dunningDocLine);
		source.setProcessed(false);
		source.setIsActive(true);
		
		source.setIsWriteOff(candidate.isWriteOff());
		source.setIsWriteOffApplied(false);
		
		dunningDAO.save(source);

		dunningDocLineSources.add(source);

		return source;
	}

	protected void aggregateDunningDocLine(I_C_DunningDoc_Line dunningDocLine, I_C_Dunning_Candidate candidate)
	{
		final IDunningUtil util = Services.get(IDunningUtil.class);

		final BigDecimal candidateAmtSrc = getAmount(candidate);
		final BigDecimal candidateAmt = util.currencyConvert(
				getDunningContext().getCtx(),
				candidateAmtSrc,
				candidate.getC_Currency_ID(), // curFrom_ID
				dunningDocLine.getC_Currency_ID(), // curTo_ID
				candidate.getDunningDate(), // conversionDate
				util.getDefaultCurrencyConvertionTypeId(), // conversion type
				dunningDocLine.getAD_Client_ID(), dunningDocLine.getAD_Org_ID()); // adClientId, adOrgId

		final BigDecimal amtOld = dunningDocLine.getAmt();
		final BigDecimal amtNew = amtOld.add(candidateAmt);
		dunningDocLine.setAmt(amtNew);
	}

	protected BigDecimal getAmount(final I_C_Dunning_Candidate candidate)
	{
		final BigDecimal amt = candidate.getDunningInterestAmt().add(candidate.getFeeAmt());
		return amt;
	}

	protected int getSalesRep_ID(final I_C_Dunning_Candidate candidate)
	{
		// TODO: at this moment we consider that the SalesRep_ID is the user which is executing this process
		// for future: we shall use de.metas.workflow.model.I_C_Doc_Responsible

		final Properties ctx = getDunningContext().getCtx();
		return Env.getAD_User_ID(ctx);
	}

	protected void completeDunningDoc()
	{
		if (dunningDoc == null)
		{
			return;
		}

		completeDunningDocLine();

		final IDunningDAO dunningDAO = Services.get(IDunningDAO.class);
		dunningDAO.save(dunningDoc);

		// If ProcessDunningDoc option is set in context, we need to automatically process the dunningDoc too
		if (getDunningContext().isProperty(CONTEXT_ProcessDunningDoc, DEFAULT_ProcessDunningDoc))
		{
			Services.get(IDunningBL.class).processDunningDoc(getDunningContext(), dunningDoc);
		}

		dunningDoc = null;
		dunningDocLine = null;
	}

	protected void completeDunningDocLine()
	{
		for (I_C_DunningDoc_Line_Source source : dunningDocLineSources)
		{
			completeDunningDocLineSource(source);
		}
		dunningDocLineSources.clear();

		if (dunningDocLine == null)
		{
			return;
		}

		final IDunningDAO dunningDAO = Services.get(IDunningDAO.class);
		dunningDAO.save(dunningDocLine);

		dunningDocLine = null;
	}

	protected void completeDunningDocLineSource(final I_C_DunningDoc_Line_Source source)
	{
		final I_C_Dunning_Candidate candidate = source.getC_Dunning_Candidate();
		candidate.setProcessed(true);
		InterfaceWrapperHelper.save(candidate);
	}

	@Override
	public void finish()
	{
		completeDunningDoc();
	}

	@Override
	public void addAggregator(IDunningAggregator aggregator)
	{
		dunningAggregators.addAggregator(aggregator);
	}

}
