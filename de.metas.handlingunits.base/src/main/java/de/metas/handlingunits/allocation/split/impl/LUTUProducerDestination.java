package de.metas.handlingunits.allocation.split.impl;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.adempiere.uom.api.Quantity;
import org.adempiere.util.Check;
import org.adempiere.util.Services;
import org.compiere.model.I_C_UOM;
import org.compiere.model.I_M_Product;

import de.metas.handlingunits.IHUCapacityBL;
import de.metas.handlingunits.IHUCapacityDefinition;
import de.metas.handlingunits.IHUTransaction;
import de.metas.handlingunits.IHUTransactionBL;
import de.metas.handlingunits.IHandlingUnitsBL;
import de.metas.handlingunits.allocation.IAllocationRequest;
import de.metas.handlingunits.allocation.IAllocationResult;
import de.metas.handlingunits.allocation.ILUTUProducerAllocationDestination;
import de.metas.handlingunits.allocation.impl.AbstractProducerDestination;
import de.metas.handlingunits.allocation.impl.AllocationUtils;
import de.metas.handlingunits.allocation.impl.IMutableAllocationResult;
import de.metas.handlingunits.allocation.split.IHUSplitDefinition;
import de.metas.handlingunits.document.IHUAllocations;
import de.metas.handlingunits.exceptions.HUException;
import de.metas.handlingunits.model.I_M_HU;
import de.metas.handlingunits.model.I_M_HU_Item;
import de.metas.handlingunits.model.I_M_HU_LUTU_Configuration;
import de.metas.handlingunits.model.I_M_HU_PI;
import de.metas.handlingunits.model.I_M_HU_PI_Item;
import de.metas.handlingunits.util.HUByIdComparator;

public class LUTUProducerDestination extends AbstractProducerDestination implements ILUTUProducerAllocationDestination
{
	//
	// Services
	private final transient IHUTransactionBL huTransactionBL = Services.get(IHUTransactionBL.class);
	private final transient IHandlingUnitsBL handlingUnitsBL = Services.get(IHandlingUnitsBL.class);
	private final transient IHUCapacityBL huCapacityBL = Services.get(IHUCapacityBL.class);

	/**
	 * Load Unit PI
	 */
	private I_M_HU_PI luPI;

	/**
	 * Load Unit PI Item (where we will link produced TUs)
	 */
	private I_M_HU_PI_Item luItemPI;

	/**
	 * Current Transport Units (TUs) producer
	 */
	private final Map<Integer, TUProducerDestination> luId2tuProducer = new HashMap<>();

	/**
	 * Current Transport Units (TUs) producer for remaining qty (which could not loaded to some LUs).
	 */
	private TUProducerDestination tuProducerForRemaining = null;
	private Set<I_M_HU> tuProducerForRemaining_alreadyCreatedTUs = null;

	/**
	 * Max handling units to produce.
	 *
	 * Default: infinite (i.e. don't enforce it)
	 */
	private int maxLUs = Integer.MAX_VALUE;

	/**
	 * Transport Unit (TU) PI
	 */
	private I_M_HU_PI tuPI;

	/**
	 * TU Capacities by M_Product_ID
	 */
	private final Map<Integer, IHUCapacityDefinition> productId2tuCapacity = new HashMap<>();

	/**
	 * How many TUs to put into one LU
	 *
	 * Default: take the default from LU configuration
	 */
	private int maxTUsPerLU = -1;

	/** How may TUs were maximum created for an LU */
	private int maxTUsPerLU_ActuallyCreated = 0;

	/**
	 * Shall we create TUs which are not linked to any LU for remaining Qty?
	 */
	private boolean createTUsForRemainingQty = false;

	/**
	 * How many TUs to create for remaining Qty (i.e. after all LUs were created)?
	 */
	private int maxTUsForRemainingQty = Integer.MAX_VALUE;
	/** How may TUs for remaining Qty were maximum created */
	private int maxTUsForRemainingQty_ActuallyCreated = 0;

	/**
	 * How many LUs were actually created
	 */
	private int _createdLUsCount = 0;
	/**
	 * How many TUs were actually created for remaining Qty
	 */
	private int _createdTUsForRemaingQtyCount = 0;
	/**
	 * Config: existing HUs to be considered when we create the actual LUs/TUs.
	 */
	private IHUAllocations existingHUs;

	public LUTUProducerDestination()
	{
		super();
	}

	public LUTUProducerDestination(final IHUSplitDefinition splitDefinition)
	{
		super();

		Check.assumeNotNull(splitDefinition, "splitDefinition not null");

		luItemPI = splitDefinition.getLuPIItem();
		luPI = splitDefinition.getLuPI();

		tuPI = splitDefinition.getTuPI();
		final I_M_Product cuProduct = splitDefinition.getCuProduct();
		final I_C_UOM cuUOM = splitDefinition.getCuUOM();
		final BigDecimal qtyCUPerTU = splitDefinition.getCuPerTU();
		addTUCapacity(cuProduct, qtyCUPerTU, cuUOM);

		final int maxLUs = splitDefinition.getMaxLUToAllocate().intValueExact();
		setMaxLUs(maxLUs);

		final int qtyTUPerLU = splitDefinition.getTuPerLU().intValueExact();
		setMaxTUsPerLU(qtyTUPerLU);
	}

	/**
	 *
	 * @return true if we shall create TU handling units for remaining qty
	 * @see #loadRemaining(IAllocationRequest)
	 */
	@Override
	public boolean isCreateTUsForRemainingQty()
	{
		return createTUsForRemainingQty;
	}

	/**
	 *
	 * @param createTUsForRemainingQty true if we shall create TU handling units for remaining qty
	 * @see #loadRemaining(IAllocationRequest)
	 */
	@Override
	public void setCreateTUsForRemainingQty(final boolean createTUsForRemainingQty)
	{
		assertConfigurable();
		this.createTUsForRemainingQty = createTUsForRemainingQty;
	}

	/**
	 *
	 * @return How many TUs to create for remaining Qty (i.e. after all LUs were created)
	 */
	@Override
	public int getMaxTUsForRemainingQty()
	{
		return maxTUsForRemainingQty;
	}

	@Override
	public void setMaxTUsForRemainingQty(final int maxTUsForRemainingQty)
	{
		assertConfigurable();
		Check.assume(maxTUsForRemainingQty > 0, "maxTUsForRemainingQty > 0 but it was {}", maxTUsForRemainingQty);
		this.maxTUsForRemainingQty = maxTUsForRemainingQty;
	}

	@Override
	public void setMaxTUsForRemainingQtyInfinite()
	{
		setMaxTUsForRemainingQty(Integer.MAX_VALUE);
	}

	@Override
	public int getMaxTUsForRemainingQty_ActuallyCreated()
	{
		return maxTUsForRemainingQty_ActuallyCreated;
	}

	@Override
	public boolean isMaxTUsForRemainingQtyInfinite()
	{
		final int maxTUsForRemainingQty = getMaxTUsForRemainingQty();
		return maxTUsForRemainingQty >= Integer.MAX_VALUE;
	}

	@Override
	public int getMaxTUsPerLU()
	{
		return maxTUsPerLU;
	}

	@Override
	public final int getMaxTUsPerLU_Effective()
	{
		//
		// Check if it was directly configured
		final int maxTUsPerLU = getMaxTUsPerLU();
		if (maxTUsPerLU > 0)
		{
			return maxTUsPerLU;
		}

		//
		// Check if we have it in LU PI Item
		final I_M_HU_PI_Item luPIItem = getLUItemPI();
		if (luPIItem != null)
		{
			final int luPiItemQty = luPIItem.getQty().intValueExact();

			// NOTE: we also accept ZERO because ZERO is also a valid number => i.e. no TUs
			if (luPiItemQty >= 0)
			{
				return luPiItemQty;
			}
		}

		//
		// Fallback: Max TU/LU was not configured => throw exception
		throw new HUException("Max TU/LU was not configured for " + this);
	}

	@Override
	public final int getMaxTUsPerLU_ActuallyCreated()
	{
		return maxTUsPerLU_ActuallyCreated;
	}

	@Override
	public void setMaxTUsPerLU(final int maxTUsPerLU)
	{
		assertConfigurable();
		Check.assume(maxTUsPerLU > 0, "maxTUsPerLU > 0 but it was {}", maxTUsPerLU);
		this.maxTUsPerLU = maxTUsPerLU;
	}

	@Override
	protected final I_M_HU_PI getM_HU_PI()
	{
		return getLUPI();
	}

	@Override
	public int getMaxLUs()
	{
		return maxLUs;
	}

	@Override
	public void setMaxLUs(final int maxLUs)
	{
		assertConfigurable();
		Check.assume(maxLUs >= 0, "maxLUs >= 0 but it was {}", maxLUs);
		this.maxLUs = maxLUs;
	}

	@Override
	public void setMaxLUsInfinite()
	{
		assertConfigurable();
		setMaxLUs(Integer.MAX_VALUE);
	}

	@Override
	public boolean isMaxLUsInfinite()
	{
		return maxLUs >= Integer.MAX_VALUE;
	}

	@Override
	public boolean isAllowCreateNewHU()
	{
		final int countLUsCreated = getCreatedHUsCount();
		return countLUsCreated < maxLUs;
	}

	/**
	 * Gets/Creates the TU Producer for given LU.
	 *
	 * @param luHU
	 * @return TU producer; never return null
	 */
	private final TUProducerDestination getCreateTUProducerDestination(final I_M_HU luHU)
	{
		//
		// Get existing TU Producer
		final int luId = luHU.getM_HU_ID();
		TUProducerDestination tuProducer = luId2tuProducer.get(luId);
		if (tuProducer != null)
		{
			return tuProducer;
		}

		//
		// Create a new TU Producer
		final I_M_HU_PI_Item luItemPI = getLUItemPI();
		Check.assumeNotNull(luItemPI, "Member luItemPI not null");

		final I_M_HU_Item luItem = handlingUnitsDAO.retrieveItem(luHU, luItemPI);
		Check.assumeNotNull(luItem, "luItem not null");

		tuProducer = createTUProducerDestination(getMaxTUsPerLU_Effective());
		tuProducer.setParentItem(luItem);

		luId2tuProducer.put(luId, tuProducer);

		return tuProducer;
	}

	/**
	 * Intended use is for unit testing only.
	 *
	 * @param toProducerDestination
	 */
	/* package */void setTUProducer(final I_M_HU luHU, final TUProducerDestination tuProducerDestination)
	{
		final int luId = luHU.getM_HU_ID();
		luId2tuProducer.put(luId, tuProducerDestination);
	}

	@Override
	public void setLUPI(final I_M_HU_PI luPI)
	{
		assertConfigurable();
		this.luPI = luPI;
	}

	@Override
	public I_M_HU_PI getLUPI()
	{
		return luPI;
	}

	/**
	 * Sets LU PI Item on which the TU will be included
	 *
	 * @param luItemPI
	 */
	@Override
	public void setLUItemPI(final I_M_HU_PI_Item luItemPI)
	{
		assertConfigurable();
		this.luItemPI = luItemPI;
	}

	@Override
	public I_M_HU_PI_Item getLUItemPI()
	{
		return luItemPI;
	}

	@Override
	public boolean isNoLU()
	{
		return luItemPI == null;
	}

	@Override
	public void setNoLU()
	{
		setLUItemPI(null);
		setLUPI(null);
		setMaxLUs(0);
	}

	@Override
	public void setTUPI(final I_M_HU_PI tuPI)
	{
		assertConfigurable();
		this.tuPI = tuPI;
	}

	@Override
	public I_M_HU_PI getTUPI()
	{
		return tuPI;
	}

	@Override
	public void addTUCapacity(final IHUCapacityDefinition tuCapacity)
	{
		Check.assumeNotNull(tuCapacity, "tuCapacity not null");

		final int productId = tuCapacity.getM_Product().getM_Product_ID();
		productId2tuCapacity.put(productId, tuCapacity);
	}

	@Override
	public void addTUCapacity(final I_M_Product cuProduct, final BigDecimal qtyCUPerTU, final I_C_UOM cuUOM)
	{
		final IHUCapacityDefinition tuCapacity = createCapacity(cuProduct, qtyCUPerTU, cuUOM);
		addTUCapacity(tuCapacity);
	}

	@Override
	public IHUCapacityDefinition getTUCapacity()
	{
		if (productId2tuCapacity.isEmpty())
		{
			throw new HUException("No TU capacities were defined");
		}
		else if (productId2tuCapacity.size() == 1)
		{
			return productId2tuCapacity.values().iterator().next();
		}
		else
		{
			throw new HUException("More than one TU capacity defined: " + productId2tuCapacity);
		}
	}

	@Override
	public IHUCapacityDefinition getTUCapacity(final I_M_Product cuProduct)
	{
		Check.assumeNotNull(cuProduct, "cuProduct not null");
		final int cuProductId = cuProduct.getM_Product_ID();
		final IHUCapacityDefinition tuCapacity = productId2tuCapacity.get(cuProductId);
		return tuCapacity;
	}

	private final IHUCapacityDefinition createCapacity(final I_M_Product cuProduct,
			final BigDecimal qtyCUPerTU,
			final I_C_UOM cuUOM)
	{
		Check.assumeNotNull(cuProduct, "cuProduct not null");
		Check.assumeNotNull(qtyCUPerTU, "qtyCUPerTU not null");
		Check.assume(qtyCUPerTU.signum() > 0, "qtyCUPerTU > 0, but it was {}", qtyCUPerTU);
		Check.assumeNotNull(cuUOM, "cuUOM not null");
		return huCapacityBL.createCapacity(qtyCUPerTU, cuProduct, cuUOM, false); // allowNegativeCapacity=false;
	}

	@Override
	protected IAllocationResult loadHU(final I_M_HU luHU, final IAllocationRequest request)
	{
		final TUProducerDestination tuProducer = getCreateTUProducerDestination(luHU);

		final IMutableAllocationResult result = AllocationUtils.createMutableAllocationResult(request);

		// 06647: create and add a HU-trx just so that HULoader.load0() will later on transfer the source's attributes also to the LU and not just to the TUs
		final I_M_HU_PI_Item luItemPI = getLUItemPI();
		final IHUTransaction luTrx = huTransactionBL.createLUTransactionForAttributeTransfer(luHU, luItemPI, request);
		result.addTransaction(luTrx);

		//
		// now to the actually job of loading the Qtys to the TUs
		final IAllocationResult tuProducerResult = tuProducer.load(request);
		AllocationUtils.mergeAllocationResult(result, tuProducerResult);

		//
		// Update Max TUs/LU that were actually created
		final int createdTUsCount = tuProducer.getCreatedHUsCount();
		if (createdTUsCount >= 0 && createdTUsCount > maxTUsPerLU_ActuallyCreated)
		{
			maxTUsPerLU_ActuallyCreated = createdTUsCount;
		}

		//
		// Return the result
		return result;
	}

	@Override
	protected final IAllocationResult loadRemaining(final IAllocationRequest request)
	{
		//
		// If we are not asked to create more TUs for remaining Qty then do nothing
		if (!isCreateTUsForRemainingQty())
		{
			return AllocationUtils.createMutableAllocationResult(request);
		}

		//
		// Create & setup TU Producer to create as many TUs as needed
		final TUProducerDestination tuProducerForRemainings = getCreateTUProducerDestinationForRemaining();

		//
		// There is no parent for remaining TUs
		tuProducerForRemainings.setParentItem(null);

		//
		// Create TUs for remaining Qty
		final IAllocationResult finalResult = tuProducerForRemainings.load(request);

		//
		// Add produced TU for remaining quantity, right in our main result
		final List<I_M_HU> createdTUs = tuProducerForRemainings.getCreatedHUs();
		addToCreatedHUs(createdTUs);

		//
		// Update Max TUs/LU that were actually created
		final int createdTUsCount = createdTUs.size();
		if (createdTUsCount >= 0 && createdTUsCount > maxTUsForRemainingQty_ActuallyCreated)
		{
			maxTUsForRemainingQty_ActuallyCreated = createdTUsCount;
		}

		//
		// Return the result
		return finalResult;
	}

	@Override
	protected void loadStarting(final IAllocationRequest request)
	{
		loadExistingHUIfAny();
	}

	private final void loadExistingHUIfAny()
	{
		//
		// Get current assignments to this receipt schedule line
		final IHUAllocations huAllocations = existingHUs;
		if (huAllocations == null)
		{
			// no existing HUs were configured => do nothing
			return;
		}

		//
		// Get target LU/TU Configuration which will be used for generating the new LUs and TUs
		final I_M_HU_LUTU_Configuration lutuConfiguration = getM_HU_LUTU_Configuration();
		int lutuConfigurationId = lutuConfiguration != null ? lutuConfiguration.getM_HU_LUTU_Configuration_ID() : -1;
		if (lutuConfigurationId <= 0)
		{
			lutuConfigurationId = -1;
		}

		//
		// Iterate all assigned HUs and add them as "created HUs" to "destination", if their LU/TU configuration matches.
		// If configuration does not match, destroy them because for sure we won't use them again.
		final List<I_M_HU> husAssigned = huAllocations.getAssignedHUs();
		for (final I_M_HU hu : husAssigned)
		{
			//
			// Skip those HUs which were destroyed in meantime.
			// It's tempting to also remove their assignments but think to the case when
			// the assignment was left there intentionatelly, for audit/documentation purposes.
			if (handlingUnitsBL.isDestroyed(hu))
			{
				continue;
			}

			// Check if current HU has the same configuration and if yes, add it to "created HUs"
			final int huConfigurationId = hu.getM_HU_LUTU_Configuration_ID();
			if (lutuConfigurationId > 0 && lutuConfigurationId == huConfigurationId)
			{
				addCreatedLUOrTU(hu);
				continue;
			}

			//
			// Else, HU does not have the same configuration so we can safely destroy it
			huAllocations.destroyAssignedHU(hu);
		}

		//
		// Reset existing HUs to load, to make sure we are not loading them again
		existingHUs = null;
	}

	@Override
	protected final void loadFinished(final IMutableAllocationResult result)
	{
		// Check if we enqueued TUs but the producer for remaining TUs was not called (because it was no need)
		// In this case let's add those enqueued TUs in our main result right now
		//
		// UPDATE NOTE: we are not adding it here because it means it was not necessary
		//
		// if (tuProducerForRemaining_alreadyCreatedTUs != null && !tuProducerForRemaining_alreadyCreatedTUs.isEmpty())
		// {
		// addToCreatedHUs(tuProducerForRemaining_alreadyCreatedTUs);
		// }
		// tuProducerForRemaining_alreadyCreatedTUs = null;
	}

	private TUProducerDestination getCreateTUProducerDestinationForRemaining()
	{
		// Check if the TU Producer for remaining Qty was already created
		if (tuProducerForRemaining != null)
		{
			return tuProducerForRemaining;
		}

		//
		// Create a new TU Producer for remaining instance
		final int maxTUsForRemainingQty = getMaxTUsForRemainingQty();
		tuProducerForRemaining = createTUProducerDestination(maxTUsForRemainingQty);
		tuProducerForRemaining.setM_HU_LUTU_Configuration(getM_HU_LUTU_Configuration());

		//
		// Add enqueued already created TUs to our TU producer
		// NOTE: clear enqueued list after
		tuProducerForRemaining.addToCreatedHUsIfAllowCreateNewHU(tuProducerForRemaining_alreadyCreatedTUs);
		tuProducerForRemaining_alreadyCreatedTUs = null;

		//
		// From this point on this producer is not configurable anymore
		setNotConfigurable();

		//
		// Return the created producer
		return tuProducerForRemaining;
	}

	private TUProducerDestination createTUProducerDestination(final int maxTUs)
	{
		final I_M_HU_PI tuPI = getTUPI();
		final TUProducerDestination producer = new TUProducerDestination(tuPI);
		producer.addCapacityConstraints(productId2tuCapacity.values());

		//
		// Set maximum TUs to produce as Qty TU/LU
		producer.setMaxTUs(maxTUs);

		// 06902: Make sure we inherit the status and locator.
		producer.setHUStatus(getHUStatus());
		producer.setM_Locator(getM_Locator());
		producer.setC_BPartner(getC_BPartner());
		producer.setC_BPartner_Location_ID(getC_BPartner_Location_ID());

		return producer;
	}

	/**
	 * @return <code>null</code>.
	 */
	@Override
	protected final I_M_HU_Item getParent_HU_Item()
	{
		return null;
	}

	@Override
	public String toString()
	{
		return "LUTUProducerDestination ["
				// LU
				+ "\n luPI=" + luPI
				+ "\n , luItemPI=" + luItemPI
				+ "\n , maxLUs=" + maxLUs
				+ "\n , maxTUsPerLU=" + maxTUsPerLU
				// TU/CU
				+ "\n , tuProducers=" + luId2tuProducer.values()
				+ "\n , tuProducerForRemaining=" + tuProducerForRemaining
				+ "\n , tuPI=" + tuPI
				+ "\n , TU capacities=" + productId2tuCapacity.values()
				// TUs (for remaining)
				+ "\n , createTUsForRemainingQty=" + createTUsForRemainingQty
				+ "\n , maxTUsForRemainingQty=" + maxTUsForRemainingQty
				+ "\n ]";
	}

	@Override
	public final void addCreatedLUOrTU(final I_M_HU hu)
	{
		// NOTE: we cannot enforce to be configurable because if
		// assertConfigurable();

		Check.assumeNotNull(hu, "hu not null");

		//
		// LU: add it directly to already created LUs (i.e. HU)
		if (handlingUnitsBL.isLoadingUnit(hu))
		{
			addToCreatedHUsIfAllowCreateNewHU(hu);
		}
		//
		// TU or Virtual: add it to the list of already created TUs for remaining Qty
		else
		{
			if (tuProducerForRemaining_alreadyCreatedTUs == null)
			{
				tuProducerForRemaining_alreadyCreatedTUs = new TreeSet<I_M_HU>(HUByIdComparator.instance);
			}
			tuProducerForRemaining_alreadyCreatedTUs.add(hu);
		}
	}

	@Override
	protected void afterHUAddedToCreatedList(final I_M_HU hu)
	{
		if (handlingUnitsBL.isLoadingUnit(hu))
		{
			_createdLUsCount++;
		}
		else
		{
			_createdTUsForRemaingQtyCount++;
		}
	}

	@Override
	protected void afterHURemovedFromCreatedList(final I_M_HU hu)
	{
		if (handlingUnitsBL.isLoadingUnit(hu))
		{
			_createdLUsCount--;
			Check.assume(_createdLUsCount >= 0, "createdLUsCount >= 0"); // shall not happen. development error
		}
		else
		{
			_createdTUsForRemaingQtyCount--;
			Check.assume(_createdTUsForRemaingQtyCount >= 0, "createdTUsForRemaingQtyCount >= 0"); // shall not happen. development error
		}
	}

	@Override
	public int getCreatedLUsCount()
	{
		return _createdLUsCount;
	}

	@Override
	public int getCreatedTUsForRemainingQtyCount()
	{
		return _createdTUsForRemaingQtyCount;
	}

	@Override
	public Quantity calculateTotalQtyCU()
	{
		final IHUCapacityDefinition tuCapacity = getTUCapacity();
		return calculateTotalQtyCU(tuCapacity);
	}

	@Override
	public Quantity calculateTotalQtyCU(final I_M_Product cuProduct)
	{
		final IHUCapacityDefinition tuCapacity = getTUCapacity(cuProduct);
		Check.assumeNotNull(tuCapacity, "tuCapacity defined for {}", cuProduct);

		return calculateTotalQtyCU(tuCapacity);
	}

	private final Quantity calculateTotalQtyCU(final IHUCapacityDefinition tuCapacity)
	{
		Check.assumeNotNull(tuCapacity, "tuCapacity not null");
		final BigDecimal qtyCUsPerTU = tuCapacity.getCapacity();
		final I_C_UOM cuUOM = tuCapacity.getC_UOM();

		// If Qty CU/TU is infinite, we cannot calculate
		if (tuCapacity.isInfiniteCapacity())
		{
			return Quantity.infinite(cuUOM);
		}
		Check.assume(qtyCUsPerTU != null && qtyCUsPerTU.signum() > 0, "Valid QtyCUsPerTU: {}", qtyCUsPerTU);

		//
		// Case: No LU
		if (isNoLU())
		{
			// No LU and don't create TUs for remaining => QtyCU total is ZERO
			if (!isCreateTUsForRemainingQty())
			{
				return Quantity.zero(cuUOM);
			}

			// Infinite TUs => QtyCU total is infinite
			if (isMaxTUsForRemainingQtyInfinite())
			{
				return Quantity.infinite(cuUOM);
			}

			final int qtyTUs = getMaxTUsForRemainingQty();
			if (qtyTUs <= 0)
			{
				return Quantity.zero(cuUOM);
			}
			final BigDecimal qtyCUsTotal = qtyCUsPerTU.multiply(BigDecimal.valueOf(qtyTUs));
			return new Quantity(qtyCUsTotal, cuUOM);
		}
		//
		// Case: we have LU
		else
		{
			// Infinite LUs => QtyCU total is infinite
			if (isMaxLUsInfinite())
			{
				return Quantity.infinite(cuUOM);
			}

			final BigDecimal qtyLUs = BigDecimal.valueOf(getMaxLUs());
			Check.assume(qtyLUs.signum() >= 0, "Valid QtyLUs: {}", qtyLUs);

			final BigDecimal qtyTUsPerLU = BigDecimal.valueOf(getMaxTUsPerLU_Effective());
			Check.assume(qtyTUsPerLU.signum() >= 0, "Valid QtyTUsPerLU: {}", qtyTUsPerLU);

			final BigDecimal qtyCUsTotal = qtyCUsPerTU.multiply(qtyTUsPerLU).multiply(qtyLUs);
			return new Quantity(qtyCUsTotal, cuUOM);
		}
	}

	@Override
	public void setExistingHUs(final IHUAllocations existingHUs)
	{
		assertConfigurable();
		this.existingHUs = existingHUs;
	}
}
