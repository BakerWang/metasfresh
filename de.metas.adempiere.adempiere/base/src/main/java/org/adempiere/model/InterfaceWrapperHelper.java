package org.adempiere.model;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.adempiere.ad.model.util.IModelCopyHelper;
import org.adempiere.ad.model.util.ModelCopyHelper;
import org.adempiere.ad.persistence.IModelClassInfo;
import org.adempiere.ad.persistence.IModelInternalAccessor;
import org.adempiere.ad.persistence.ModelClassIntrospector;
import org.adempiere.ad.persistence.ModelDynAttributeAccessor;
import org.adempiere.ad.table.api.IADTableDAO;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.ad.trx.api.ITrxManager;
import org.adempiere.ad.trx.api.OnTrxMissingPolicy;
import org.adempiere.ad.wrapper.POJOLookupMap;
import org.adempiere.ad.wrapper.POJOWrapper;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.adempiere.util.lang.ITableRecordReference;
import org.compiere.Adempiere;
import org.compiere.model.GridField;
import org.compiere.model.GridTab;
import org.compiere.model.PO;
import org.compiere.model.POInfo;
import org.compiere.util.Env;
import org.compiere.util.Evaluatee;
import org.slf4j.Logger;

import com.google.common.base.Optional;

import de.metas.i18n.IModelTranslationMap;
import de.metas.i18n.impl.NullModelTranslationMap;
import de.metas.logging.LogManager;

public class InterfaceWrapperHelper
{
	private static final transient Logger logger = LogManager.getLogger(InterfaceWrapperHelper.class);

	public static final String COLUMNNAME_SUFFIX_Override = "_Override";

	private static final POJOLookupMap getInMemoryDatabaseForModel(final Class<?> modelClass)
	{
		return POJOLookupMap.getInMemoryDatabaseForModel(modelClass);
	}

	private static final POJOLookupMap getInMemoryDatabaseForTableName(final String tableName)
	{
		return POJOLookupMap.getInMemoryDatabaseForTableName(tableName);
	}

	private static final boolean isInMemoryDatabaseOnly()
	{
		return org.compiere.Adempiere.isUnitTestMode();
	}

	/**
	 * Creates a new instance of given object using same context and trxName as <code>contextProvider</code>.
	 * <p>
	 * <b>IMPORTANT:</b> if the given contextProvider has the trxName <code>null</code>, then the new instance's trxName will be {@link ITrx#TRXNAME_ThreadInherited}!
	 * <p>
	 * The method invokes {@link #newInstance(Class, Object, boolean)} with <code>useCLientOrgFromProvider = true</code>.
	 * <p>
	 *
	 * @param cl
	 * @param contextProvider any object that carries a context (e.g. a PO, a wrapped PO, GridTab, a wrapped GridTab etc)
	 * @return new instance
	 */
	public static <T> T newInstance(final Class<T> cl, final Object contextProvider)
	{
		return newInstance(cl, contextProvider, true); // useCLientOrgFromProvider = true
	}

	/**
	 * Creates a new instance of given object using same context and trxName as <code>contextProvider</code>
	 *
	 * @param cl
	 * @param contextProvider any object that carries a context (e.g. a PO, a wrapped PO, GridTab, a wrapped GridTab etc)
	 * @param useCLientOrgFromProvider if {@code true}, then the context used to create the new instance will have the {@code contextProvider}'s {@code AD_Client_ID} and {@code AD_Org_ID} as
	 *            {@code #AD_Client_ID} resp. {@code #clone().AD_Org_ID}.
	 * @return new instance
	 */
	public static <T> T newInstance(final Class<T> cl, final Object contextProvider, final boolean useCLientOrgFromProvider)
	{
		Check.assumeNotNull(contextProvider, "contextProvider not null");
		final Properties ctx = getCtx(contextProvider, useCLientOrgFromProvider);

		//
		// Get transaction name from contextProvider.
		// If contextProvider's transaction name is NULL and we have a thread inherited transaction, then let's use that one
		final ITrxManager trxManager = getTrxManager();
		String trxName = getTrxName(contextProvider);
		if (trxManager.isNull(trxName))
		{
			final ITrx trxThreadInherited = trxManager.get(ITrx.TRXNAME_ThreadInherited, OnTrxMissingPolicy.ReturnTrxNone);
			if (trxThreadInherited != null)
			{
				trxName = ITrx.TRXNAME_ThreadInherited;
			}
		}

		return create(ctx, cl, trxName);
	}

	/**
	 * Convenient method to create a new instance of given class, using current context and current transaction.
	 * 
	 * @param cl
	 */
	public static <T> T newInstance(final Class<T> cl)
	{
		final Properties ctx = Env.getCtx();
		final String trxName = ITrx.TRXNAME_ThreadInherited;
		return create(ctx, cl, trxName);
	}

	public static <T> T create(final Object model, final Class<T> cl)
	{
		final boolean useOldValues = false;
		return create(model, cl, useOldValues);
	}

	/**
	 * Wraps given model to given model class.
	 *
	 * @param model
	 * @param cl model class
	 * @param useOldValues
	 *            <ul>
	 *            <li>true if old values shall be used
	 *            <li>false if model's old values flag shall BE PRESERVED. i.e. if it was "true" we shall use old values, if it was "false" we shall NOT use old values.
	 *            </ul>
	 * @return model wrapped or <code>null</code> if model was <code>null</code>
	 *
	 * @deprecated Because this method is tricky and we consider to make it private, please use:
	 *             <ul>
	 *             <li> {@link #create(Object, Class)}
	 *             <li>or {@link #createOld(Object, Class)}
	 *             </ul>
	 */
	@Deprecated
	public static <T> T create(final Object model, final Class<T> cl, final boolean useOldValues)
	{
		if (model == null)
		{
			return null;
		}
		else if (cl.isInstance(model) && !useOldValues)
		{
			@SuppressWarnings("unchecked")
			final T modelCasted = (T)model;
			return modelCasted;
		}
		else if (POWrapper.isHandled(model))
		{
			if (useOldValues)
			{
				return POWrapper.create(model, cl, true);
			}
			else
			{
				// preserve "old values" flag
				return POWrapper.create(model, cl);
			}
		}
		else if (GridTabWrapper.isHandled(model))
		{
			if (useOldValues)
			{
				return GridTabWrapper.create(model, cl, true);
			}
			else
			{
				// preserve "old values" flag
				return GridTabWrapper.create(model, cl);
			}
		}
		else if (POJOWrapper.isHandled(model))
		{
			if (useOldValues)
			{
				return POJOWrapper.create(model, cl, true);
			}
			else
			{
				// preserve "old values" flag
				return POJOWrapper.create(model, cl);
			}
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model
					+ "\n Class: " + (model == null ? null : model.getClass())
					+ "\n useOldValues: " + useOldValues);
		}
	}

	/**
	 * Wrap given <code>model</code> and use old values for all model getters.
	 *
	 * @param model
	 * @param cl
	 * @return
	 */
	public static <T> T createOld(final Object model, final Class<T> cl)
	{
		final boolean useOldValues = true;
		return create(model, cl, useOldValues);
	}

	public static <T> T create(final Properties ctx, final Class<T> cl, final String trxName)
	{
		if (getInMemoryDatabaseForModel(cl) != null)
		{
			return POJOWrapper.create(ctx, cl, trxName);
		}
		final T bean = POWrapper.create(ctx, cl, trxName);
		return bean;
	}

	/**
	 * Loads the record with the given <code>id</code>.
	 * <p>
	 * Note: if you want to load a record from <code>(AD_Table_ID, Reference_ID)</code>,<br>
	 * then it's probably better to use e.g. {@link org.adempiere.util.lang.impl.TableRecordReference#TableRecordReference(int, int)}.
	 *
	 * @param ctx
	 * @param id
	 * @param cl
	 * @param trxName
	 * @return
	 */
	public static <T> T create(final Properties ctx, final int id, final Class<T> cl, final String trxName)
	{
		if (getInMemoryDatabaseForModel(cl) != null)
		{
			return POJOWrapper.create(ctx, id, cl, trxName);
		}
		final T bean = POWrapper.create(ctx, id, cl, trxName);
		return bean;
	}

	/**
	 * Loads the record with the given <code>id</code>. Similar to {@link #create(Properties, String, int, Class, String)}, but explicitly specifies the table name.<br>
	 * This is useful in case the table name can't be deduced from the given <code>cl</code>.
	 * <p>
	 * Note: if you want to load a record from <code>(AD_Table_ID, Reference_ID)</code>,<br>
	 * then it's probably better to use {@link org.adempiere.util.lang.impl.TableRecordReference#TableRecordReference(int, int)}.
	 *
	 * @param ctx
	 * @param tableName
	 * @param id
	 * @param cl
	 * @param trxName
	 * @return
	 */
	public static <T> T create(final Properties ctx, final String tableName, final int id, final Class<T> cl, final String trxName)
	{
		if (getInMemoryDatabaseForTableName(tableName) != null)
		{
			return POJOWrapper.create(ctx, tableName, id, cl, trxName);
		}
		final T bean = POWrapper.create(ctx, tableName, id, cl, trxName);
		return bean;
	}

	/**
	 * Converts given list to target type.
	 *
	 * @param list list to be converted
	 * @param clazz target model class
	 * @return converted list to given model
	 */
	public static <T, S> List<T> createList(final List<S> list, final Class<T> clazz)
	{
		if (list == null)
		{
			return null;
		}

		final List<T> result = new ArrayList<T>(list.size());
		for (final S model : list)
		{
			final T modelConv = create(model, clazz);
			result.add(modelConv);
		}

		return result;
	}

	public static void refresh(final Object model)
	{
		final boolean discardChanges = false;
		refresh(model, discardChanges);
	}

	/**
	 * Refresh all models that were given using {@link #refresh(Object)}.
	 *
	 * NOTE: developers are encouraged to use this method because here we would be able to do more optimizations.
	 *
	 * @param models
	 */
	public static <T> void refreshAll(final Iterable<T> models)
	{
		if (models == null)
		{
			return;
		}

		for (final Object model : models)
		{
			InterfaceWrapperHelper.refresh(model);
		}
	}

	/**
	 * Like {@link #refreshAll(Iterable)}, but uses {@link #refresh(Object, String)} instead.
	 *
	 * @param models
	 * @param trxName
	 */
	public static <T> void refreshAll(final Iterable<T> models, final String trxName)
	{
		if (models == null)
		{
			return;
		}

		for (final Object model : models)
		{
			InterfaceWrapperHelper.refresh(model, trxName);
		}
	}

	/**
	 * Mark the model as staled. It means that it needs to be reloaded first in case some values need to be retrieved.
	 *
	 * NOTE: this method is currently refreshing the model right away, because we did not implement it.
	 *
	 * @param model
	 */
	public static void markStaled(final Object model)
	{
		// TODO: we need to implement it
		refresh(model);
	}

	/**
	 * Reload given model from database.
	 *
	 * @param model
	 * @param discardChanges hint for actual handler to tell that if there are any unsaved changes, it's ok to discard them.
	 */
	public static void refresh(final Object model, final boolean discardChanges)
	{
		if (GridTabWrapper.isHandled(model))
		{
			GridTabWrapper.refresh(model);
		}
		else if (POWrapper.isHandled(model))
		{
			POWrapper.refresh(model);
		}
		else if (POJOWrapper.isHandled(model))
		{
			POJOWrapper.refresh(model, discardChanges, POJOWrapper.getTrxName(model));
		}
		else
		{
			throw new AdempiereException("Not supported model " + model + " (class:" + model.getClass() + ")");
		}
	}

	/**
	 * Refreshes the given model, and if the model is handled by {@link POWrapper} or {@link POJOWrapper}, then uses the given <code>trxName</code>.
	 *
	 * @param model
	 * @param trxName
	 */
	public static void refresh(final Object model, final String trxName)
	{
		if (GridTabWrapper.isHandled(model))
		{
			GridTabWrapper.refresh(model);
		}
		else if (POWrapper.isHandled(model))
		{
			POWrapper.refresh(model, trxName);
		}
		else if (POJOWrapper.isHandled(model))
		{
			final boolean discardChanges = false;
			POJOWrapper.refresh(model, discardChanges, trxName);
		}
		else
		{
			throw new AdempiereException("Unsupported model " + model + " (class:" + model.getClass() + ")");
		}
	}

	public static void setTrxName(final Object model, final String trxName)
	{
		final boolean ignoreIfNotHandled = false;
		setTrxName(model, trxName, ignoreIfNotHandled);
	}

	/**
	 *
	 * @param model
	 * @param trxName
	 * @param ignoreIfNotHandled <code>true</code> and the given model can not be handled (no PO, GridTab etc), then don't throw an exception,
	 *
	 * @throws AdempiereException if the given model is neither handled by {@link POWrapper} nor by {@link POJOWrapper} and ignoreIfNotHandled is <code>false</code>.
	 */
	public static void setTrxName(final Object model, final String trxName, final boolean ignoreIfNotHandled)
	{
		if (POWrapper.isHandled(model))
		{
			POWrapper.setTrxName(model, trxName);
		}
		else if (POJOWrapper.isHandled(model))
		{
			POJOWrapper.setTrxName(model, trxName);
		}
		else if (!ignoreIfNotHandled)
		{
			throw new AdempiereException("Not supported model " + model + " (class:" + model.getClass() + ")");
		}
	}

	private static ITrxManager getTrxManager()
	{
		// TODO: consider using as a class field because this method is/will accessed many times
		return Services.get(ITrxManager.class);
	}

	/**
	 * Set current thread inerited transaction name to given model.
	 *
	 * @param model
	 */
	public static void setThreadInheritedTrxName(final Object model)
	{
		final ITrxManager trxManager = getTrxManager();
		String trxName = trxManager.getThreadInheritedTrxName();
		if (trxName == null)
		{
			trxName = ITrx.TRXNAME_None;
		}
		setTrxName(model, trxName);
	}

	/**
	 * Sets trxName to {@link ITrx#TRXNAME_ThreadInherited}.
	 *
	 * @param model
	 */
	public static void setThreadInheritedTrxNameMarker(final Object model)
	{
		InterfaceWrapperHelper.setTrxName(model, ITrx.TRXNAME_ThreadInherited);
	}

	/**
	 * Set current thread inerited transaction name to given models.
	 *
	 * @param models
	 */
	public static void setThreadInheritedTrxName(final Collection<?> models)
	{
		if (models == null || models.isEmpty())
		{
			return;
		}

		final ITrxManager trxManager = getTrxManager();
		String trxName = trxManager.getThreadInheritedTrxName();
		if (trxName == null)
		{
			trxName = ITrx.TRXNAME_None;
		}

		for (final Object model : models)
		{
			setTrxName(model, trxName);
		}
	}

	public static void save(final Object model)
	{
		final Object modelToSave;
		if (model instanceof IModelWrapper)
		{
			modelToSave = ((IModelWrapper)model).getModel();
		}
		else
		{
			modelToSave = model;
		}

		if (modelToSave == null)
		{
			final AdempiereException ex = new AdempiereException("Saving null model ignored. Possible development issue. Ignored.");
			logger.warn(ex.getLocalizedMessage(), ex);
		}
		else if (GridTabWrapper.isHandled(modelToSave))
		{
			GridTabWrapper.save(modelToSave);
		}
		else if (POWrapper.isHandled(modelToSave))
		{
			POWrapper.save(modelToSave);
		}
		else if (POJOWrapper.isHandled(modelToSave))
		{
			POJOWrapper.save(modelToSave);
		}
		else
		{
			final AdempiereException ex = new AdempiereException("PO not handled: " + modelToSave + "(class=" + modelToSave.getClass() + "). Ignored.");
			logger.error(ex.getLocalizedMessage(), ex);
		}
	}

	public static void save(final Object model, final String trxName)
	{
		if (model == null)
		{
			final AdempiereException ex = new AdempiereException("Saving null model ignored. Possible development issue. Ignored.");
			logger.warn(ex.getLocalizedMessage(), ex);
		}

		final String trxNameOld = getTrxName(model);
		setTrxName(model, trxName);
		try
		{
			save(model);
		}
		finally
		{
			// Restore the trxName
			setTrxName(model, trxNameOld);
		}
	}

	/**
	 * Get context from model.
	 *
	 * @param model
	 * @return
	 */
	public static Properties getCtx(final Object model)
	{
		return getCtx(model, false);
	}

	/**
	 * Get context from model and setting in context AD_Client_ID and AD_Org_ID according to the model if useClientOrgFromModel is true
	 *
	 * @param model
	 * @param useClientOrgFromModel
	 * @return
	 */
	public static Properties getCtx(final Object model, final boolean useClientOrgFromModel)
	{
		if (model == null)
		{
			return Env.getCtx();
		}
		else if (model instanceof IContextAware)
		{
			return ((IContextAware)model).getCtx();
		}
		else if (model instanceof Properties)
		{
			return (Properties)model; // this *is* already the ctx
		}
		else if (GridTabWrapper.isHandled(model))
		{
			return Env.getCtx();
		}
		else if (POWrapper.isHandled(model))
		{
			return POWrapper.getCtx(model, useClientOrgFromModel);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.getCtx(model, useClientOrgFromModel);
		}
		else
		{
			final AdempiereException ex = new AdempiereException("Cannot get context from object: " + model + ". Returning global context.");
			logger.warn(ex.getLocalizedMessage(), ex);
			return Env.getCtx();
		}
	}

	public static String getTrxName(final Object model)
	{
		final boolean ignoreIfNotHandled = false;
		return getTrxName(model, ignoreIfNotHandled);
	}

	/**
	 *
	 * @param model
	 * @param ignoreIfNotHandled if <code>true</code> and the given model can not be handeled (no PO, GridTab etc), then just return {@link ITrx#TRXNAME_None} without loggin a warning.
	 *
	 * @return
	 */
	public static String getTrxName(final Object model, final boolean ignoreIfNotHandled)
	{
		if (model == null)
		{
			return ITrx.TRXNAME_None;
		}
		else if (model instanceof IContextAware)
		{
			return ((IContextAware)model).getTrxName();
		}
		else if (model instanceof Properties)
		{
			return ITrx.TRXNAME_None;
		}
		else if (GridTabWrapper.isHandled(model))
		{
			return ITrx.TRXNAME_None;
		}
		else if (POWrapper.isHandled(model))
		{
			return POWrapper.getTrxName(model);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.getTrxName(model);
		}
		else if (!ignoreIfNotHandled)
		{
			final AdempiereException ex = new AdempiereException("Cannot get trxName from object: " + model + ". Returning null.");
			logger.warn(ex.getLocalizedMessage(), ex);
		}
		return ITrx.TRXNAME_None;
	}

	public static IContextAware getContextAware(final Object model)
	{
		if (model instanceof IContextAware)
		{
			return (IContextAware)model;
		}
		else
		{
			return new ModelContextAware(model);
		}
	}

	public static void delete(final Object model)
	{
		Check.assume(model != null, "model is null");

		if (POWrapper.isHandled(model))
		{
			POWrapper.delete(model);
		}
		else if (POJOWrapper.isHandled(model))
		{
			POJOWrapper.delete(model);
		}
		else
		{
			throw new IllegalStateException("delete is not supported for " + model);
		}
	}

	/**
	 *
	 * @param model
	 * @return underlying {@link PO} or null
	 */
	public static <T extends PO> T getPO(final Object model)
	{
		if (Adempiere.isUnitTestMode())
		{
			throw new UnsupportedOperationException("Getting PO from '" + model + "' is not supported in JUnit testing mode");
		}
		return POWrapper.getPO(model);
	}

	public static int getId(final Object model)
	{
		if (model == null)
		{
			return -1;
		}
		else if (POWrapper.isHandled(model))
		{
			final PO po = POWrapper.getPO(model, false);
			if (po == null)
			{
				return -1;
			}

			final String[] keyColumns = po.get_KeyColumns();
			if (keyColumns == null || keyColumns.length != 1)
			{
				return -1;
			}

			return po.get_ID();
		}
		else if (GridTabWrapper.isHandled(model))
		{
			return GridTabWrapper.getId(model);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.getWrapper(model).getId();
		}
		else if (model instanceof ITableRecordReference)
		{
			return ((ITableRecordReference)model).getRecord_ID();
		}
		else
		{
			throw new AdempiereException("Not supported model " + model + " (class:" + model.getClass() + ")");
		}
	}

	/**
	 * Introducing this exception to be thrown instead of ADempiereException. Reason: It's a pain if you have a breakpoint on "AdempiereException" and the debugger stops every 2 seconds because
	 * InterfaceWrapperHelper throws it.
	 */
	/* package */static class MissingTableNameException extends AdempiereException
	{
		private static final long serialVersionUID = 6469196469943285793L;

		private MissingTableNameException(final Class<?> clazz)
		{
			super("@NotFound@ @TableName@ (class=" + clazz + ")");
		}
	}

	private static final IModelClassInfo getModelClassInfoOrNull(final Class<?> clazz)
	{
		return ModelClassIntrospector
				.getInstance()
				.getModelClassInfo(clazz);
	}

	/**
	 * Checks static variable "Table_Name" of given interface and returns it's content.
	 *
	 * @param clazz
	 * @return tableName associated with given interface
	 * @throws AdempiereException if "Table_Name" static variable is not defined or is not accessible
	 */
	public static String getTableName(final Class<?> clazz) throws AdempiereException
	{
		final String tableName = getTableNameOrNull(clazz);
		if (tableName == null)
		{
			throw new MissingTableNameException(clazz);
		}
		return tableName;
	}

	/**
	 * Checks static variable "Table_Name" of given interface and returns it's content.
	 *
	 * @param clazz
	 * @return tableName associated with given interface or null if interface has no Table_Name
	 */
	public static String getTableNameOrNull(final Class<?> clazz)
	{
		final IModelClassInfo modelClassInfo = getModelClassInfoOrNull(clazz);
		if (modelClassInfo == null)
		{
			return null;
		}
		return modelClassInfo.getTableName();
	}

	public static final boolean isModelInterface(final Class<?> modelClass)
	{
		final IModelClassInfo modelClassInfo = getModelClassInfoOrNull(modelClass);
		if (modelClassInfo == null)
		{
			return false;
		}
		if (modelClassInfo.getTableName() == null)
		{
			return false;
		}

		return true;
	}

	public static int getTableId(final Class<?> clazz)
	{
		final String tableName = getTableName(clazz);
		return Services.get(IADTableDAO.class).retrieveTableId(tableName);
	}

	/**
	 * @param clazz
	 * @return AD_Table_ID or <code>-1</code>.
	 */
	public static final int getTableIdOrNull(final Class<?> clazz)
	{
		final String tableName = getTableNameOrNull(clazz);
		if (tableName == null)
		{
			return -1;
		}
		return Services.get(IADTableDAO.class).retrieveTableId(tableName);

	}

	public static final String getKeyColumnName(final Class<?> clazz)
	{
		final String tableName = getTableName(clazz);
		return getKeyColumnName(tableName);
	}

	public static final String getKeyColumnName(final String tableName)
	{
		// NOTE: we assume the key column name is <TableName>_ID
		final String keyColumnName = tableName + "_ID"; // TODO: hardcoded
		return keyColumnName;
	}

	/**
	 * Get Table_ID of wrapped model. If model is null, an exception will be thrown
	 *
	 * @param model
	 * @return Table_ID
	 * @throws AdempiereException if model is null or model is not supported
	 */
	public static int getModelTableId(final Object model)
	{
		final String modelTableName = getModelTableName(model);
		return Services.get(IADTableDAO.class).retrieveTableId(modelTableName);
	}

	/**
	 * Get TableName of wrapped model. If model is null or is not supported, an exception will be thrown.
	 *
	 * @param model
	 * @return table name
	 * @throws AdempiereException if model is null or model is not supported
	 */
	public static String getModelTableName(final Object model)
	{
		if (model == null)
		{
			throw new AdempiereException("Cannot get TableName for a null model. Possible development issue.");
		}

		final String modelTableName = getModelTableNameOrNull(model);
		if (modelTableName == null)
		{
			throw new AdempiereException("Cannot get TableName from model: " + model);
		}

		return modelTableName;
	}

	/**
	 * Get TableName of wrapped model.
	 *
	 * This method returns null when:
	 * <ul>
	 * <li>model is null
	 * <li>model is not supported
	 * </ul>
	 *
	 * @param model persistent object, wrapped model, {@link ITableRecordReference}
	 * @return table name or null
	 */
	public static String getModelTableNameOrNull(final Object model)
	{
		if (model == null)
		{
			return null;
		}
		else if (GridTabWrapper.isHandled(model))
		{
			return GridTabWrapper.getGridTab(model).getTableName();
		}
		else if (POWrapper.isHandled(model))
		{
			return POWrapper.getPO(model).get_TableName();
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.getWrapper(model).getTableName();
		}
		else if (model instanceof ITableRecordReference)
		{
			return ((ITableRecordReference)model).getTableName();
		}
		else
		{
			return null;
		}
	}

	public static void copyValues(final Object from, final Object to)
	{
		// 03710 we must honor IsCalculated unless we definitely know what we do. Otherwise all kind of problems will occur.
		final boolean honorIsCalculated = true;

		copyValues(from, to, honorIsCalculated);
	}

	public static void copyValues(final Object from, final Object to, final boolean honorIsCalculated)
	{
		if (POWrapper.isHandled(to))
		{
			final PO fromPO = getPO(from);
			final PO toPO = getPO(to);
			PO.copyValues(fromPO, toPO, honorIsCalculated);
		}
		else if (POJOWrapper.isHandled(from) && POJOWrapper.isHandled(to))
		{
			// NOTE: commented out because some tests are failing because of this
			// Check.assume(!honorIsCalculated, "honorIsCalculated=true not supported by {}", POJOWrapper.class);

			final POJOWrapper fromWrapper = POJOWrapper.getWrapper(from);
			final POJOWrapper toWrapper = POJOWrapper.getWrapper(to);
			toWrapper.copyFrom(fromWrapper);
		}
		else
		{
			throw new AdempiereException("Unsupported models: from=" + from + ", to=" + to);
		}
	}

	/**
	 * Checks if given columnName's value is <code>null</code>
	 *
	 * @param model
	 * @param columnName
	 * @return <code>true</code> if columnName's value is <code>null</code>
	 */
	public static boolean isNull(final Object model, final String columnName)
	{
		if (model == null)
		{
			return true;
		}
		else if (GridTabWrapper.isHandled(model))
		{
			return GridTabWrapper.isNull(model, columnName);
		}
		else if (POWrapper.isHandled(model))
		{
			return POWrapper.isNull(model, columnName);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.isNull(model, columnName);
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	/**
	 * Checks if given columnName's value is <code>null</code> or (only in case it is a string!) empty.
	 *
	 * @param model
	 * @param columnName
	 * @return <code>true</code> if columnName's value is <code>null</code> or an empty string.
	 */
	public static boolean isNullOrEmpty(final Object model, final String columnName)
	{
		if (isNull(model, columnName))
		{
			return true;
		}

		final Object value = getValue(model, columnName).orNull();
		if (value instanceof String)
		{
			return Check.isEmpty((String)value);
		}

		return false;
	}

	public static boolean hasModelColumnName(final Object model, final String columnName)
	{
		Check.assumeNotNull(model, "model is not null");
		Check.assumeNotNull(columnName, "columnName is not null");

		if (GridTabWrapper.isHandled(model))
		{
			return GridTabWrapper.hasColumnName(model, columnName);
		}
		else if (POWrapper.isHandled(model))
		{
			return POWrapper.hasModelColumnName(model, columnName);
		}
		else if (POJOWrapper.isHandled(model))
		{
			final POJOWrapper wrapper = POJOWrapper.getWrapper(model);
			return wrapper.hasColumnName(columnName);
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	public static boolean hasColumnName(final Class<?> modelClass, final String columnName)
	{
		Check.assumeNotNull(modelClass, "modelClass is not null");
		Check.assumeNotNull(columnName, "columnName is not null");

		if (isInMemoryDatabaseOnly())
		{
			return POJOWrapper.hasColumnName(modelClass, columnName);
		}
		else
		{
			return POWrapper.hasColumnName(modelClass, columnName);
		}

	}

	public static <T> T getValueByColumnId(final Object model, final int adColumnId)
	{
		Check.assumeNotNull(model, "model is not null");
		Check.assume(adColumnId > 0, "adColumnId > 0");

		if (GridTabWrapper.isHandled(model))
		{
			final GridTab gridTab = GridTabWrapper.getGridTab(model);
			for (final GridField field : gridTab.getFields())
			{
				if (field.getAD_Column_ID() == adColumnId)
				{
					@SuppressWarnings("unchecked")
					final T value = (T)field.getValue();
					return value;
				}
			}

			throw new AdempiereException("No field with AD_Column_ID=" + adColumnId + " found in " + gridTab + " for " + model);
		}
		else if (POWrapper.isHandled(model))
		{
			final PO po = POWrapper.getPO(model, false);
			@SuppressWarnings("unchecked")
			final T value = (T)po.get_ValueOfColumn(adColumnId);
			return value;
		}
		// else if (POJOWrapper.isHandled(model))
		// {
		// final POJOWrapper wrapper = POJOWrapper.getWrapper(model);
		// // MColumn.getColumnName(ctx, AD_Column_ID)
		// }
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	public static <T> T getValueOrNull(final Object model, final String columnName)
	{
		final boolean throwExIfColumnNotFound = false;
		final boolean useOverrideColumnIfAvailable = false;
		final T value = getValue(model, columnName, throwExIfColumnNotFound, useOverrideColumnIfAvailable);
		return value;
	}

	public static <T> Optional<T> getValue(final Object model, final String columnName)
	{
		final boolean throwExIfColumnNotFound = true;
		final boolean useOverrideColumnIfAvailable = false;
		final T value = getValue(model, columnName, throwExIfColumnNotFound, useOverrideColumnIfAvailable);
		return Optional.fromNullable(value);
	}

	/**
	 * Gets [columnName]_Override if the override column is available and not null, else column name value is returned.
	 *
	 * @param model
	 * @param columnName
	 * @return value of [columnName]_Override or [columnName]; <b>might return null</b>, so don't blindly use as int.
	 * @throws AdempiereException if neither the "normal" value nor the override value is available.
	 *
	 */
	public static <T> T getValueOverrideOrValue(final Object model, final String columnName)
	{
		final boolean throwExIfColumnNotFound = true;
		final boolean useOverrideColumnIfAvailable = true;
		final T value = getValue(model, columnName, throwExIfColumnNotFound, useOverrideColumnIfAvailable);
		return value;
	}

	private static final <T> T getValue(final Object model,
			final String columnName,
			final boolean throwExIfColumnNotFound,
			final boolean useOverrideColumnIfAvailable)
	{
		Check.assumeNotNull(model, "model is not null");
		Check.assumeNotNull(columnName, "columnName is not null");

		if (POWrapper.isHandled(model))
		{
			if (useOverrideColumnIfAvailable)
			{
				final IModelInternalAccessor modelAccessor = POWrapper.getModelInternalAccessor(model);
				final T value = getValueOverrideOrNull(modelAccessor, columnName);
				if (value != null)
				{
					return value;
				}
			}
			//
			final PO po = POWrapper.getPO(model, false);
			final int idxColumnName = po.get_ColumnIndex(columnName);
			if (idxColumnName < 0)
			{
				if (throwExIfColumnNotFound)
				{
					throw new AdempiereException("No columnName " + columnName + " found for " + model);
				}
				else
				{
					return null;
				}
			}
			@SuppressWarnings("unchecked")
			final T value = (T)po.get_Value(idxColumnName);
			return value;
		}
		else if (GridTabWrapper.isHandled(model))
		{
			final GridTab gridTab = GridTabWrapper.getGridTab(model);
			if (useOverrideColumnIfAvailable)
			{
				final IModelInternalAccessor modelAccessor = GridTabWrapper.getModelInternalAccessor(model);
				final T value = getValueOverrideOrNull(modelAccessor, columnName);
				if (value != null)
				{
					return value;
				}
			}
			//
			final GridField gridField = gridTab.getField(columnName);
			if (gridField == null)
			{
				if (throwExIfColumnNotFound)
				{
					throw new AdempiereException("No field with ColumnName=" + columnName + " found in " + gridTab + " for " + model);
				}
				else
				{
					return null;
				}
			}

			@SuppressWarnings("unchecked")
			final T value = (T)gridField.getValue();
			return value;
		}
		else if (POJOWrapper.isHandled(model))
		{
			final POJOWrapper wrapper = POJOWrapper.getWrapper(model);
			if (useOverrideColumnIfAvailable)
			{
				final IModelInternalAccessor modelAccessor = wrapper.getModelInternalAccessor();
				final T value = getValueOverrideOrNull(modelAccessor, columnName);
				if (value != null)
				{
					return value;
				}
			}
			//
			if (!wrapper.hasColumnName(columnName))
			{
				if (throwExIfColumnNotFound)
				{
					throw new AdempiereException("No columnName " + columnName + " found for " + model);
				}
				else
				{
					return null;
				}
			}
			@SuppressWarnings("unchecked")
			final T value = (T)wrapper.getValuesMap().get(columnName);
			return value;
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	/**
	 * Gets columnName's override value or null
	 *
	 * @param modelAccessor
	 * @param columnName
	 * @return
	 */
	private static final <T> T getValueOverrideOrNull(final IModelInternalAccessor modelAccessor, final String columnName)
	{
		//
		// Try ColumnName_Override
		// e.g. for "C_Tax_ID", the C_Tax_ID_Override" will be checked
		{
			final String overrideColumnName = columnName + COLUMNNAME_SUFFIX_Override;
			if (modelAccessor.hasColumnName(overrideColumnName))
			{
				@SuppressWarnings("unchecked")
				final T value = (T)modelAccessor.getValue(overrideColumnName, Object.class);
				if (value != null)
				{
					return value;
				}
			}
		}

		//
		// Try ColumnName_Override_ID
		// e.g. for "C_Tax_ID", the C_Tax_Override_ID" will be checked
		if (columnName.endsWith("_ID"))
		{
			final String overrideColumnName = columnName.substring(0, columnName.length() - 3) + COLUMNNAME_SUFFIX_Override + "_ID";
			if (modelAccessor.hasColumnName(overrideColumnName))
			{
				@SuppressWarnings("unchecked")
				final T value = (T)modelAccessor.getValue(overrideColumnName, Object.class);
				if (value != null)
				{
					return value;
				}
			}
		}

		// No override values found => return null
		return null;
	}

	public static <ModelType> ModelType getModelValue(final Object model, final String columnName, final Class<ModelType> columnModelType)
	{
		if (POWrapper.isHandled(model))
		{
			return POWrapper.getModelValue(model, columnName, columnModelType);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.getWrapper(model).getModelValue(columnName, columnModelType);
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	/**
	 * Set value for given <code>columnName</code>.
	 *
	 * If column was not found in <code>model</code> a warning will be logged but no exception will be thrown
	 *
	 * @param model
	 * @param columnName
	 * @param value
	 * @return true if value was set
	 */
	public static boolean setValue(final Object model, final String columnName, final Object value)
	{
		final boolean throwExIfColumnNotFound = false;
		return setValue(model, columnName, value, throwExIfColumnNotFound);
	}

	/**
	 * Set values for given <code>Map<columnName, value></code>.
	 *
	 * If a column was not found in <code>model</code>, an exception will be thrown.
	 *
	 * @param model
	 * @param values
	 * @return true if all values were set
	 */
	public static boolean setValues(final Object model, final Map<String, Object> values)
	{
		//
		// Assume all values will be set
		boolean isAllValuesSet = true;

		//
		// Do NOT allow any value to be set if the column was not found
		// These cases must be covered when using this method
		final boolean throwExIfColumnNotFound = true;
		for (final Entry<String, Object> valueEntry : values.entrySet())
		{
			//
			// If a value could not be set, modifier will be changed to false
			isAllValuesSet = isAllValuesSet && setValue(model, valueEntry.getKey(), valueEntry.getValue(), throwExIfColumnNotFound);
		}
		return isAllValuesSet;
	}

	private static boolean setValue(final Object model, final String columnName, final Object value, final boolean throwExIfColumnNotFound)
	{
		Check.assumeNotNull(model, "model is not null");
		Check.assumeNotNull(columnName, "columnName is not null");

		if (GridTabWrapper.isHandled(model))
		{
			final GridTab gridTab = GridTabWrapper.getGridTab(model);
			final GridField gridField = gridTab.getField(columnName);
			if (gridField == null)
			{
				final AdempiereException ex = new AdempiereException("No field with ColumnName=" + columnName + " found in " + gridTab + " for " + model);
				if (throwExIfColumnNotFound)
				{
					throw ex;
				}
				else
				{
					logger.warn(ex.getLocalizedMessage(), ex);
					return false;
				}
			}

			gridTab.setValue(gridField, value);
		}
		else if (POWrapper.isHandled(model))
		{
			final PO po = POWrapper.getPO(model, false);
			final int idx = po.get_ColumnIndex(columnName);
			if (idx < 0)
			{
				final AdempiereException ex = new AdempiereException("No columnName " + columnName + " found for " + model);
				if (throwExIfColumnNotFound)
				{
					throw ex;
				}
				else
				{
					logger.warn(ex.getLocalizedMessage(), ex);
					return false;
				}
			}

			po.set_ValueOfColumn(columnName, value);
		}
		else if (POJOWrapper.isHandled(model))
		{
			final POJOWrapper wrapper = POJOWrapper.getWrapper(model);
			if (!wrapper.hasColumnName(columnName))
			{
				final AdempiereException ex = new AdempiereException("No columnName " + columnName + " found for " + model);
				if (throwExIfColumnNotFound)
				{
					throw ex;
				}
				else
				{
					logger.warn(ex.getLocalizedMessage(), ex);
					return false;
				}
			}

			wrapper.setValue(columnName, value);
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}

		return true;
	}

	/**
	 * Explicitly mark a column that was changed.
	 *
	 * It is helpful to do this when:
	 * <ul>
	 * <li>you set a value for a column but the new value can be the same as the old value
	 * <li>and you really really what to trigger the database UPDATE or you really really want to trigger the model validators
	 * </ul>
	 *
	 * NOTE:
	 * <ul>
	 * <li>if you are marking the column as changed but you are not explicitly set a value (i.e. a new value), this command will have no effect
	 * <li>this command has effect only for {@link POWrapper}ed objects
	 * </ul>
	 *
	 * @param model
	 * @param columnName column name to be marked as changed
	 */
	public static void markColumnChanged(final Object model, final String columnName)
	{
		if (POWrapper.isHandled(model))
		{
			final PO po = POWrapper.getPO(model, false);
			po.markColumnChanged(columnName);
		}

		// Other wrappers are not supporting this feature
	}

	/**
	 * <b>IMPORTANT:</b> Please consider using {@link org.adempiere.ad.persistence.ModelDynAttributeAccessor} instead if this method. It's typesafe.
	 *
	 * @param model
	 * @param attributeName
	 * @param value
	 * @return old value or null
	 */
	public static Object setDynAttribute(final Object model, final String attributeName, final Object value)
	{
		Check.assumeNotNull(model, "model not null");

		if (POWrapper.isHandled(model))
		{
			return POWrapper.setDynAttribute(model, attributeName, value);
		}
		else if (GridTabWrapper.isHandled(model))
		{
			return GridTabWrapper.getWrapper(model).setDynAttribute(attributeName, value);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.setDynAttribute(model, attributeName, value);
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	/**
	 * <b>IMPORTANT:</b> Please consider using {@link org.adempiere.ad.persistence.ModelDynAttributeAccessor} instead if this method. It's typesafe.
	 *
	 * @param model
	 * @param attributeName
	 * @return
	 */
	public static <T> T getDynAttribute(final Object model, final String attributeName)
	{
		if (POWrapper.isHandled(model))
		{
			final T value = POWrapper.getDynAttribute(model, attributeName);
			return value;
		}
		else if (GridTabWrapper.isHandled(model))
		{
			final T value = GridTabWrapper.getWrapper(model).getDynAttribute(attributeName);
			return value;
		}
		else if (POJOWrapper.isHandled(model))
		{
			final T value = POJOWrapper.getDynAttribute(model, attributeName);
			return value;
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	/**
	 * Check if given <code>model</code> can be casted to <code>interfaceClass</code>. NOTE: by casted we mean using create(...) methods.
	 *
	 * @param model
	 * @param interfaceClass
	 * @return true if we can cast the model to given interface.
	 */
	public static boolean isInstanceOf(final Object model, final Class<?> interfaceClass)
	{
		if (model == null)
		{
			return false;
		}

		final String interfaceTableName = getTableNameOrNull(interfaceClass);
		if (interfaceTableName == null)
		{
			return interfaceClass.isAssignableFrom(model.getClass());
		}

		final String modelTableName = getModelTableName(model);

		return interfaceTableName.equals(modelTableName);
	}

	/**
	 * Boolean dynamic attribute. If set underlying record cannot be saved or deleted.
	 */
	private static final String DYNATTR_SaveDeleteDisabled = "SaveDeleteDisabled";

	/**
	 *
	 * @param model
	 * @return true if save/delete was not disabled on purpose for given model
	 * @see #DYNATTR_SaveDeleteDisabled
	 */
	public static boolean isSaveDeleteDisabled(final Object model)
	{
		if (model == null)
		{
			return false;
		}
		final Object saveDeleteDisabled = getDynAttribute(model, InterfaceWrapperHelper.DYNATTR_SaveDeleteDisabled);
		if (saveDeleteDisabled == null)
		{
			return false;
		}
		if (!(saveDeleteDisabled instanceof Boolean))
		{
			return false;
		}

		final boolean saveDeleteDisabledBoolean = (Boolean)saveDeleteDisabled;
		return saveDeleteDisabledBoolean;
	}

	/**
	 * Sets the dynanamic attribute {@link #DYNATTR_SaveDeleteDisabled} to the given <code>disabled</code> value if <code>true</code> or resets if if <code>false</code>. If set to <code>true</code>,
	 * both {@link PO} and {@link POJOLookupMap} will throw an {@link AdempiereException} on save/delete invocations for the given <code>model</code>.
	 *
	 * @param model
	 * @param disabled
	 * @see #isSaveDeleteDisabled(Object)
	 */
	public static void setSaveDeleteDisabled(final Object model, final boolean disabled)
	{
		if (disabled)
		{
			setDynAttribute(model, DYNATTR_SaveDeleteDisabled, true);
		}
		else
		{
			setDynAttribute(model, DYNATTR_SaveDeleteDisabled, null);
		}
	}

	public static <T> T translate(final T model, final Class<T> cl)
	{
		final String adLanguage = null; // autodetect from context
		return translate(model, cl, adLanguage);
	}

	public static <T> T translate(final T model, final Class<T> cl, final String adLanguage)
	{
		if (POWrapper.isHandled(model))
		{
			return POWrapper.translate(model, cl, adLanguage);
		}
		else if (POJOWrapper.isHandled(model))
		{
			// nothing to translate in testing
			return POJOWrapper.create(model, cl);
		}
		else
		{
			throw new AdempiereException("Model translation is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}
	
	public static final IModelTranslationMap getModelTranslationMap(final Object model)
	{
		Check.assumeNotNull(model, "model not null");
		if (POWrapper.isHandled(model))
		{
			return POWrapper.getModelTranslationMap(model);
		}
		else
		{
			return NullModelTranslationMap.instance;
		}
	}
	
	/**
	 * @param model
	 * @return true if model is a new record (not yet saved in database)
	 */
	public static boolean isNew(final Object model)
	{
		Check.assumeNotNull(model, "model not null");
		if (GridTabWrapper.isHandled(model))
		{
			return GridTabWrapper.isNew(model);
		}
		else if (POWrapper.isHandled(model))
		{
			return POWrapper.isNew(model);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.isNew(model);
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	/**
	 *
	 * @return true if this object was just created (saved or not). Compared to {@link #isNew(Object)} this method will return <code>true</code> even if the model was already saved.
	 */
	public static boolean isJustCreated(final Object model)
	{
		if (POWrapper.isHandled(model))
		{
			return POWrapper.getPO(model, false).is_JustCreated();
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.getWrapper(model).isJustCreated();
		}
		else
		{
			throw new AdempiereException("is_JustCreated() is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	/**
	 *
	 * @return true if this model is created, updated or deleted by a manual user action (from UI window)
	 */
	public static boolean isUIAction(final Object model)
	{
		if (GridTabWrapper.isHandled(model))
		{
			return true;
		}
		else if (POWrapper.isHandled(model))
		{
			return POWrapper.isUIAction(model);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return false;
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	public static boolean recordChanged(final Object model, final String columnName)
	{
		Check.assumeNotNull(model, "model not null");

		if (POWrapper.isHandled(model))
		{
			return POWrapper.isRecordChanged(model);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.isRecordChanged(model);
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	public static boolean isValueChanged(final Object model, final String columnName)
	{
		Check.assumeNotNull(model, "model not null");

		if (POWrapper.isHandled(model))
		{
			return POWrapper.isValueChanged(model, columnName);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.isValueChanged(model, columnName);
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	/**
	 * @param model
	 * @param columnNames
	 * @return true if any of given column names where changed
	 */
	public static boolean isValueChanged(final Object model, final Set<String> columnNames)
	{
		Check.assumeNotNull(model, "model not null");

		if (POWrapper.isHandled(model))
		{
			return POWrapper.isValueChanged(model, columnNames);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.isValueChanged(model, columnNames);
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	public static boolean hasChanges(final Object model)
	{
		Check.assumeNotNull(model, "model not null");

		if (POWrapper.isHandled(model))
		{
			return POWrapper.hasChanges(model);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.getWrapper(model).hasChanges();
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	/**
	 *
	 * @param model
	 * @return how many times given model was loaded/reloaded
	 */
	public static int getLoadCount(final Object model)
	{
		Check.assumeNotNull(model, "model not null");

		if (POWrapper.isHandled(model))
		{
			return POWrapper.getPO(model).getLoadCount();
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.getWrapper(model).getLoadCount();
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	public static Evaluatee getEvaluatee(final Object model)
	{
		if (model == null)
		{
			return null;
		}
		else if (model instanceof Evaluatee)
		{
			final Evaluatee evaluatee = (Evaluatee)model;
			return evaluatee;
		}
		else if (POWrapper.isHandled(model))
		{
			return POWrapper.getPO(model);
		}
		else if (GridTabWrapper.isHandled(model))
		{
			return GridTabWrapper.getGridTab(model);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.getWrapper(model).asEvaluatee();
		}
		else
		{
			throw new AdempiereException("Evaluatee not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	/**
	 * @param modelClass
	 * @return immutable list of column names of modelClass's table
	 */
	public static Set<String> getModelColumnNames(final Class<?> modelClass)
	{
		if (Adempiere.isUnitTestMode())
		{
			return ModelClassIntrospector.getInstance()
					.getModelClassInfo(modelClass)
					.getDefinedColumnNames();
		}

		final String tableName = InterfaceWrapperHelper.getTableName(modelClass);
		final POInfo poInfo = POInfo.getPOInfo(tableName);
		Check.assumeNotNull(poInfo, "poInfo not null for {}", tableName); // shall not happen

		return poInfo.getColumnNames();
	}

	public static IModelInternalAccessor getModelInternalAccessor(final Object model)
	{
		Check.assumeNotNull(model, "model not null");
		if (POWrapper.isHandled(model))
		{
			final IModelInternalAccessor modelInternalAccessor = POWrapper.getModelInternalAccessor(model);
			Check.assumeNotNull(modelInternalAccessor, "modelInternalAccessor not null");
			return modelInternalAccessor;
		}
		else if (POJOWrapper.isHandled(model))
		{
			final IModelInternalAccessor modelInternalAccessor = POJOWrapper.getModelInternalAccessor(model);
			Check.assumeNotNull(modelInternalAccessor, "modelInternalAccessor not null");
			return modelInternalAccessor;
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model + " (class:" + model.getClass() + ")");
		}
	}

	public static IModelCopyHelper copy()
	{
		return new ModelCopyHelper();
	}

	public static boolean isOldValues(final Object model)
	{
		Check.assumeNotNull(model, "model not null");
		if (POWrapper.isHandled(model))
		{
			return POWrapper.isOldValues(model);
		}
		else if (GridTabWrapper.isHandled(model))
		{
			return GridTabWrapper.isOldValues(model);
		}
		else if (POJOWrapper.isHandled(model))
		{
			return POJOWrapper.isOldValues(model);
		}
		else
		{
			throw new AdempiereException("Model wrapping is not supported for " + model
					+ "\n Class: " + (model == null ? null : model.getClass()));
		}
	}

	/**
	 * If the given <code>model</code> is not null and has all the columns which are defined inside the given <code>clazz</code>'s {@link IModelClassInfo},<br>
	 * then return an instance using {@link #create(Object, Class)}.<br>
	 * Otherwise, return <code>null</code> .
	 *
	 * @param model
	 * @param clazz
	 * @return
	 */
	public static <T> T asColumnReferenceAwareOrNull(final Object model,
			final Class<T> clazz)
	{
		if (model == null)
		{
			return null;
		}
		if (clazz.isAssignableFrom(model.getClass()))
		{
			return clazz.cast(model);
		}

		final IModelClassInfo clazzInfo = ModelClassIntrospector
				.getInstance()
				.getModelClassInfo(clazz);
		for (final String columnName : clazzInfo.getDefinedColumnNames())
		{
			if (!hasModelColumnName(model, columnName))
			{
				// not all columns of clazz are also in model => we can't do it.
				return null;
			}
		}

		return InterfaceWrapperHelper.create(model, clazz);
	}
	
	/**
	 * Disables the read only (i.e. not updateable) columns enforcement.
	 * So basically, after you are calling this method you will be able to change the values for any not updateable column.
	 * 
	 * WARNING: please make sure you know what are you doing before calling this method. If you are not sure, please don't use it.
	 * 
	 * @param model
	 */
	public static final void disableReadOnlyColumnCheck(final Object model)
	{
		Check.assumeNotNull(model, "model not null");
		ATTR_ReadOnlyColumnCheckDisabled.setValue(model, Boolean.TRUE);
	}
	
	public static final ModelDynAttributeAccessor<Object, Boolean> ATTR_ReadOnlyColumnCheckDisabled = new ModelDynAttributeAccessor<>(InterfaceWrapperHelper.class.getName(), "ReadOnlyColumnCheckDisabled", Boolean.class);
	
}
