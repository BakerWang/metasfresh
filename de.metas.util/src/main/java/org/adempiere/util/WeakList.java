package org.adempiere.util;

/*
 * #%L
 * de.metas.util
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


import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.adempiere.util.collections.IteratorUtils;

/**
 * List that can contain weak elements
 *
 * @author Teo Sarca
 */
public class WeakList<T> extends AbstractList<T>
{
	/**
	 * Flag used for debugging this internal state of this weak list.
	 *
	 * When enabled, a string representation of weak value is stored. That representation is accessible even after the value was garbaged.
	 */
	private static final boolean DEBUG = false;

	private final ReferenceQueue<T> queue = new ReferenceQueue<T>();
	private final List<ListEntry> list = new ArrayList<ListEntry>();
	private final ReentrantLock lock = new ReentrantLock();
	private boolean weakDefault = true;

	public WeakList()
	{
		super();
	}

	public WeakList(final boolean weakDefault)
	{
		this();
		setWeakDefault(weakDefault);
	}

	public boolean isWeakDefault()
	{
		return weakDefault;
	}

	public void setWeakDefault(final boolean weakDefault)
	{
		this.weakDefault = weakDefault;
	}

	public boolean add(final T o, final boolean weak)
	{
		expungeStaleEntries();
		return list.add(new ListEntry(o, weak));
	}

	@Override
	public boolean add(final T o)
	{
		lock.lock();
		try
		{
			boolean weak = weakDefault;
			if (!weak && o instanceof IWeak)
			{
				weak = true;
			}
			return add(o, weak);
		}
		finally
		{
			lock.unlock();
		}
	}

	@Override
	public T get(final int i)
	{
		expungeStaleEntries();
		final ListEntry e = list.get(i);
		return e.get();
	}

	@Override
	public int size()
	{
		expungeStaleEntries();
		return list.size();
	}

	@Override
	public final boolean remove(final Object o)
	{
		// NOTE: we need to fully override the original implementation because
		// that one is using the iterator to search and remove the object,
		// which in our case does not work because our iterator() method is returning an immutable iterator over the hard list.

		lock.lock();
		try
		{
			for (final Iterator<ListEntry> it = list.iterator(); it.hasNext();)
			{
				final ListEntry e = it.next();

				// Check if the entry is still available
				// NOTE: we also use this opportunity to discard expired references
				if (!e.isAvailable())
				{
					it.remove();
					continue;
				}

				final T value = e.get();

				// Case: we are asked to remove a null value
				// => check if current entry value is null
				if (o == null)
				{
					if (value == null)
					{
						it.remove();
						return true;
					}
				}
				// Case: we are asked to remove a not null value
				// => use equals() to check if current entry value is matching
				else if (o.equals(value))
				{
					it.remove();
					return true;
				}
			}
		}
		finally
		{
			lock.unlock();
		}

		return false;
	}

	@Override
	public T remove(final int index)
	{

		lock.lock();
		try
		{
			expungeStaleEntriesAssumeSync();
			final ListEntry e = list.remove(index);
			return e == null ? null : e.get();
		}
		finally
		{
			lock.unlock();
		}
	}

	public List<T> hardList()
	{
		// We implement this similarly to expunge, because the old approach called expunge 3 times.
		// We initialize result with the maximum possible size to prevent reallocations.
		lock.lock();
		try
		{
			final List<T> result = new ArrayList<T>(list.size());

			expungeStaleEntriesAssumeSync();

			for (final ListEntry l : list)
			{
				if (!l.isAvailable())
				{
					continue;
				}
				result.add(l.get());
			}

			return result;
		}
		finally
		{
			lock.unlock();
		}
	}

	@Override
	public Iterator<T> iterator()
	{
		final Iterator<T> it = hardList().iterator();

		// NOTE: iterator is decoupled from this list, so to not confuse developer it's better to make it readonly. Then it will be obvious for developer that he cannot remove from this iterator.
		return IteratorUtils.unmodifiableIterator(it);
	}

	/**
	 * Expunge stale entries from the list.
	 */

	private void expungeStaleEntries()
	{
		lock.lock();
		try
		{
			expungeStaleEntriesAssumeSync();
		}
		finally
		{
			lock.unlock();
		}
	}

	/**
	 * Expunge stale entries. This method should only be called from inside a lock.
	 *
	 */
	@SuppressWarnings("unchecked")
	private final void expungeStaleEntriesAssumeSync()
	{
		ListWeakReference r;
		while ((r = (ListWeakReference)queue.poll()) != null)
		{
			final ListEntry le = r.getListEntry();
			final int i = list.indexOf(le);
			if (i != -1)
			{
				list.remove(i);
			}
		}
	}

	private class ListWeakReference extends WeakReference<T>
	{
		private final ListEntry parent;

		public ListWeakReference(final T value, final ListEntry parent)
		{
			super(value, queue);
			this.parent = parent;
		}

		public ListEntry getListEntry()
		{
			return this.parent;
		}

	}

	private class ListEntry
	{
		final T value;
		final ListWeakReference weakValue;
		final boolean isWeak;

		/**
		 * String representation of "value" (only if isWeak).
		 *
		 * NOTE: this variable is filled only if {@link WeakList#DEBUG} flag is set.
		 */
		final String valueStr;

		public ListEntry(final T value, final boolean isWeak)
		{
			if (isWeak)
			{
				this.value = null;
				this.weakValue = new ListWeakReference(value, this);
				this.isWeak = true;
				this.valueStr = DEBUG ? String.valueOf(value) : null;
			}
			else
			{
				this.value = value;
				this.weakValue = null;
				this.isWeak = false;
				this.valueStr = null;
			}
		}

		private boolean isAvailable()
		{
			if (isWeak)
			{
				return weakValue.get() != null;
			}
			else
			{
				return true;
			}
		}

		public T get()
		{
			if (isWeak)
			{
				return weakValue.get();
			}
			else
			{
				return value;
			}
		}

		/**
		 * Always compare by identity.
		 * 
		 * @return true if it's the same instance.
		 */
		@Override
		public boolean equals(final Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			return false;
		}

		@Override
		public int hashCode()
		{
			// NOTE: we implemented this method only to get rid of "implemented equals() but not hashCode() warning"
			return super.hashCode();
		}

		@Override
		public String toString()
		{
			if (!isAvailable())
			{
				return DEBUG ? "<GARBAGED: " + valueStr + ">" : "<GARBAGED>";
			}
			else
			{
				return String.valueOf(this.get());
			}
		}
	}

	/**
	 * Gets internal lock used by this weak list.
	 *
	 * NOTE: use it only if you know what are you doing.
	 *
	 * @return internal lock used by this weak list.
	 */
	public final ReentrantLock getReentrantLock()
	{
		return lock;
	}
}
