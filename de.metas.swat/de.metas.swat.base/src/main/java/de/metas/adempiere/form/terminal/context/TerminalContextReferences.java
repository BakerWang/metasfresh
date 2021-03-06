package de.metas.adempiere.form.terminal.context;

/*
 * #%L
 * de.metas.swat.base
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
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import de.metas.logging.LogManager;

import org.adempiere.util.WeakList;
import org.adempiere.util.beans.WeakPropertyChangeSupport;
import de.metas.adempiere.form.terminal.IDisposable;

/* package */final class TerminalContextReferences implements ITerminalContextReferences
{
	private static final transient Logger logger = LogManager.getLogger(TerminalContextReferences.class);

	private List<WeakPropertyChangeSupport> propertyChangeSupports = null;
	private final WeakList<IDisposable> _disposableComponents = new WeakList<>(true); // weakDefault=true

	@Override
	public void dispose()
	{
		disposeComponents();

		disposePropertyChangeSupports();
	}

	@Override
	public WeakPropertyChangeSupport createPropertyChangeSupport(final Object sourceBean)
	{
		final boolean weakDefault = false;
		return createPropertyChangeSupport(sourceBean, weakDefault);
	}

	@Override
	public WeakPropertyChangeSupport createPropertyChangeSupport(final Object sourceBean, final boolean weakDefault)
	{
		if (propertyChangeSupports == null)
		{
			propertyChangeSupports = new WeakList<>(true); // weakDefault = true
		}

		final WeakPropertyChangeSupport pcs = new WeakPropertyChangeSupport(sourceBean, weakDefault);
		propertyChangeSupports.add(pcs);

		return pcs;
	}

	@Override
	public final void addToDisposableComponents(final IDisposable comp)
	{
		if (comp == null)
		{
			return;
		}

		_disposableComponents.add(comp);
	}

	/**
	 * Dispose all created components
	 */
	private final void disposeComponents()
	{
		// NOTE: we are disposing the components in the reverse order of their creation,
		// just because we want to give them the opportunity to dispose nicely
		final List<IDisposable> disposables = new ArrayList<>(_disposableComponents);
		Collections.reverse(disposables);
		
		int countDisposed = 0;
		for (final IDisposable disposable : disposables)
		{
			// guard: skip null dispoables
			if(disposable == null)
			{
				continue;
			}
			
			try
			{
				disposable.dispose();
				countDisposed++;
			}
			catch (Exception e)
			{
				logger.warn("Error while disposing component " + disposable + ". Ignored.", e);
			}
		}

		logger.info("Disposed {} components", countDisposed);
	}

	/**
	 * Clear all {@link WeakPropertyChangeSupport}s.
	 */
	private void disposePropertyChangeSupports()
	{
		if (this.propertyChangeSupports == null)
		{
			// already cleared
			return;
		}

		final int countAll = propertyChangeSupports.size();
		for (final WeakPropertyChangeSupport pcs : propertyChangeSupports)
		{
			pcs.clear();
		}
		this.propertyChangeSupports.clear();
		this.propertyChangeSupports = null;

		logger.info("Cleared {} property change supports", countAll);
	}

}
