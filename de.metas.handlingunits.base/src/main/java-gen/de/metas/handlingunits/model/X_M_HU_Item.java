/** Generated Model - DO NOT CHANGE */
package de.metas.handlingunits.model;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Properties;
import org.compiere.util.Env;

/** Generated Model for M_HU_Item
 *  @author Adempiere (generated) 
 */
@SuppressWarnings("javadoc")
public class X_M_HU_Item extends org.compiere.model.PO implements I_M_HU_Item, org.compiere.model.I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = 1637152521L;

    /** Standard Constructor */
    public X_M_HU_Item (Properties ctx, int M_HU_Item_ID, String trxName)
    {
      super (ctx, M_HU_Item_ID, trxName);
      /** if (M_HU_Item_ID == 0)
        {
			setM_HU_ID (0);
			setM_HU_Item_ID (0);
			setM_HU_PI_Item_ID (0);
        } */
    }

    /** Load Constructor */
    public X_M_HU_Item (Properties ctx, ResultSet rs, String trxName)
    {
      super (ctx, rs, trxName);
    }


    /** Load Meta Data */
    @Override
    protected org.compiere.model.POInfo initPO (Properties ctx)
    {
      org.compiere.model.POInfo poi = org.compiere.model.POInfo.getPOInfo (ctx, Table_Name, get_TrxName());
      return poi;
    }

	@Override
	public de.metas.handlingunits.model.I_M_HU getM_HU() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_M_HU_ID, de.metas.handlingunits.model.I_M_HU.class);
	}

	@Override
	public void setM_HU(de.metas.handlingunits.model.I_M_HU M_HU)
	{
		set_ValueFromPO(COLUMNNAME_M_HU_ID, de.metas.handlingunits.model.I_M_HU.class, M_HU);
	}

	/** Set Handling Units.
		@param M_HU_ID Handling Units	  */
	@Override
	public void setM_HU_ID (int M_HU_ID)
	{
		if (M_HU_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_HU_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_HU_ID, Integer.valueOf(M_HU_ID));
	}

	/** Get Handling Units.
		@return Handling Units	  */
	@Override
	public int getM_HU_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_HU_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Handling Units Item.
		@param M_HU_Item_ID Handling Units Item	  */
	@Override
	public void setM_HU_Item_ID (int M_HU_Item_ID)
	{
		if (M_HU_Item_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_HU_Item_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_HU_Item_ID, Integer.valueOf(M_HU_Item_ID));
	}

	/** Get Handling Units Item.
		@return Handling Units Item	  */
	@Override
	public int getM_HU_Item_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_HU_Item_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Override
	public de.metas.handlingunits.model.I_M_HU_PI_Item getM_HU_PI_Item() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_M_HU_PI_Item_ID, de.metas.handlingunits.model.I_M_HU_PI_Item.class);
	}

	@Override
	public void setM_HU_PI_Item(de.metas.handlingunits.model.I_M_HU_PI_Item M_HU_PI_Item)
	{
		set_ValueFromPO(COLUMNNAME_M_HU_PI_Item_ID, de.metas.handlingunits.model.I_M_HU_PI_Item.class, M_HU_PI_Item);
	}

	/** Set Packvorschrift Position.
		@param M_HU_PI_Item_ID Packvorschrift Position	  */
	@Override
	public void setM_HU_PI_Item_ID (int M_HU_PI_Item_ID)
	{
		if (M_HU_PI_Item_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_M_HU_PI_Item_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_M_HU_PI_Item_ID, Integer.valueOf(M_HU_PI_Item_ID));
	}

	/** Get Packvorschrift Position.
		@return Packvorschrift Position	  */
	@Override
	public int getM_HU_PI_Item_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_HU_PI_Item_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Menge.
		@param Qty 
		Menge
	  */
	@Override
	public void setQty (java.math.BigDecimal Qty)
	{
		set_Value (COLUMNNAME_Qty, Qty);
	}

	/** Get Menge.
		@return Menge
	  */
	@Override
	public java.math.BigDecimal getQty () 
	{
		BigDecimal bd = (BigDecimal)get_Value(COLUMNNAME_Qty);
		if (bd == null)
			 return Env.ZERO;
		return bd;
	}
}