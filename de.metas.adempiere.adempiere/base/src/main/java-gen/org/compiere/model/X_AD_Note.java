/******************************************************************************
 * Product: Adempiere ERP & CRM Smart Business Solution                       *
 * Copyright (C) 1999-2007 ComPiere, Inc. All Rights Reserved.                *
 * This program is free software, you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY, without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program, if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 * For the text or an alternative of this public license, you may reach us    *
 * ComPiere, Inc., 2620 Augustine Dr. #245, Santa Clara, CA 95054, USA        *
 * or via info@compiere.org or http://www.compiere.org/license.html           *
 *****************************************************************************/
/** Generated Model - DO NOT CHANGE */
package org.compiere.model;

import java.sql.ResultSet;
import java.util.Properties;

/** Generated Model for AD_Note
 *  @author Adempiere (generated) 
 */
@SuppressWarnings("javadoc")
public class X_AD_Note extends org.compiere.model.PO implements I_AD_Note, org.compiere.model.I_Persistent 
{

	/**
	 *
	 */
	private static final long serialVersionUID = -2117935516L;

    /** Standard Constructor */
    public X_AD_Note (Properties ctx, int AD_Note_ID, String trxName)
    {
      super (ctx, AD_Note_ID, trxName);
      /** if (AD_Note_ID == 0)
        {
			setAD_Message_ID (0);
			setAD_Note_ID (0);
        } */
    }

    /** Load Constructor */
    public X_AD_Note (Properties ctx, ResultSet rs, String trxName)
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
    public String toString()
    {
      StringBuilder sb = new StringBuilder ("X_AD_Note[")
        .append(get_ID()).append("]");
      return sb.toString();
    }

	@Override
	public org.compiere.model.I_AD_Message getAD_Message() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_AD_Message_ID, org.compiere.model.I_AD_Message.class);
	}

	@Override
	public void setAD_Message(org.compiere.model.I_AD_Message AD_Message)
	{
		set_ValueFromPO(COLUMNNAME_AD_Message_ID, org.compiere.model.I_AD_Message.class, AD_Message);
	}

	/** Set Meldung.
		@param AD_Message_ID 
		System Message
	  */
	@Override
	public void setAD_Message_ID (int AD_Message_ID)
	{
		if (AD_Message_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Message_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Message_ID, Integer.valueOf(AD_Message_ID));
	}

	/** Get Meldung.
		@return System Message
	  */
	@Override
	public int getAD_Message_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Message_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Statusmeldung.
		@param AD_Note_ID 
		System Notice
	  */
	@Override
	public void setAD_Note_ID (int AD_Note_ID)
	{
		if (AD_Note_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Note_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Note_ID, Integer.valueOf(AD_Note_ID));
	}

	/** Get Statusmeldung.
		@return System Notice
	  */
	@Override
	public int getAD_Note_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Note_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Override
	public org.compiere.model.I_AD_Table getAD_Table() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_AD_Table_ID, org.compiere.model.I_AD_Table.class);
	}

	@Override
	public void setAD_Table(org.compiere.model.I_AD_Table AD_Table)
	{
		set_ValueFromPO(COLUMNNAME_AD_Table_ID, org.compiere.model.I_AD_Table.class, AD_Table);
	}

	/** Set DB-Tabelle.
		@param AD_Table_ID 
		Database Table information
	  */
	@Override
	public void setAD_Table_ID (int AD_Table_ID)
	{
		if (AD_Table_ID < 1) 
			set_ValueNoCheck (COLUMNNAME_AD_Table_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_AD_Table_ID, Integer.valueOf(AD_Table_ID));
	}

	/** Get DB-Tabelle.
		@return Database Table information
	  */
	@Override
	public int getAD_Table_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_Table_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Override
	public org.compiere.model.I_AD_User getAD_User() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_AD_User_ID, org.compiere.model.I_AD_User.class);
	}

	@Override
	public void setAD_User(org.compiere.model.I_AD_User AD_User)
	{
		set_ValueFromPO(COLUMNNAME_AD_User_ID, org.compiere.model.I_AD_User.class, AD_User);
	}

	/** Set Ansprechpartner.
		@param AD_User_ID 
		User within the system - Internal or Business Partner Contact
	  */
	@Override
	public void setAD_User_ID (int AD_User_ID)
	{
		if (AD_User_ID < 1) 
			set_Value (COLUMNNAME_AD_User_ID, null);
		else 
			set_Value (COLUMNNAME_AD_User_ID, Integer.valueOf(AD_User_ID));
	}

	/** Get Ansprechpartner.
		@return User within the system - Internal or Business Partner Contact
	  */
	@Override
	public int getAD_User_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_User_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Override
	public org.compiere.model.I_AD_WF_Activity getAD_WF_Activity() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_AD_WF_Activity_ID, org.compiere.model.I_AD_WF_Activity.class);
	}

	@Override
	public void setAD_WF_Activity(org.compiere.model.I_AD_WF_Activity AD_WF_Activity)
	{
		set_ValueFromPO(COLUMNNAME_AD_WF_Activity_ID, org.compiere.model.I_AD_WF_Activity.class, AD_WF_Activity);
	}

	/** Set Workflow-Aktivität.
		@param AD_WF_Activity_ID 
		Workflow Activity
	  */
	@Override
	public void setAD_WF_Activity_ID (int AD_WF_Activity_ID)
	{
		if (AD_WF_Activity_ID < 1) 
			set_Value (COLUMNNAME_AD_WF_Activity_ID, null);
		else 
			set_Value (COLUMNNAME_AD_WF_Activity_ID, Integer.valueOf(AD_WF_Activity_ID));
	}

	/** Get Workflow-Aktivität.
		@return Workflow Activity
	  */
	@Override
	public int getAD_WF_Activity_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_AD_WF_Activity_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Beschreibung.
		@param Description Beschreibung	  */
	@Override
	public void setDescription (java.lang.String Description)
	{
		set_Value (COLUMNNAME_Description, Description);
	}

	/** Get Beschreibung.
		@return Beschreibung	  */
	@Override
	public java.lang.String getDescription () 
	{
		return (java.lang.String)get_Value(COLUMNNAME_Description);
	}

	@Override
	public org.compiere.model.I_M_Product getM_Product() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_M_Product_ID, org.compiere.model.I_M_Product.class);
	}

	@Override
	public void setM_Product(org.compiere.model.I_M_Product M_Product)
	{
		set_ValueFromPO(COLUMNNAME_M_Product_ID, org.compiere.model.I_M_Product.class, M_Product);
	}

	/** Set Produkt.
		@param M_Product_ID 
		Produkt, Leistung, Artikel
	  */
	@Override
	public void setM_Product_ID (int M_Product_ID)
	{
		if (M_Product_ID < 1) 
			set_Value (COLUMNNAME_M_Product_ID, null);
		else 
			set_Value (COLUMNNAME_M_Product_ID, Integer.valueOf(M_Product_ID));
	}

	/** Get Produkt.
		@return Produkt, Leistung, Artikel
	  */
	@Override
	public int getM_Product_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Product_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Override
	public org.compiere.model.I_M_Warehouse getM_Warehouse() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_M_Warehouse_ID, org.compiere.model.I_M_Warehouse.class);
	}

	@Override
	public void setM_Warehouse(org.compiere.model.I_M_Warehouse M_Warehouse)
	{
		set_ValueFromPO(COLUMNNAME_M_Warehouse_ID, org.compiere.model.I_M_Warehouse.class, M_Warehouse);
	}

	/** Set Lager.
		@param M_Warehouse_ID 
		Lager oder Ort für Dienstleistung
	  */
	@Override
	public void setM_Warehouse_ID (int M_Warehouse_ID)
	{
		if (M_Warehouse_ID < 1) 
			set_Value (COLUMNNAME_M_Warehouse_ID, null);
		else 
			set_Value (COLUMNNAME_M_Warehouse_ID, Integer.valueOf(M_Warehouse_ID));
	}

	/** Get Lager.
		@return Lager oder Ort für Dienstleistung
	  */
	@Override
	public int getM_Warehouse_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_M_Warehouse_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Override
	public org.compiere.model.I_S_Resource getPP_Plant() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_PP_Plant_ID, org.compiere.model.I_S_Resource.class);
	}

	@Override
	public void setPP_Plant(org.compiere.model.I_S_Resource PP_Plant)
	{
		set_ValueFromPO(COLUMNNAME_PP_Plant_ID, org.compiere.model.I_S_Resource.class, PP_Plant);
	}

	/** Set Produktionsstätte.
		@param PP_Plant_ID Produktionsstätte	  */
	@Override
	public void setPP_Plant_ID (int PP_Plant_ID)
	{
		if (PP_Plant_ID < 1) 
			set_Value (COLUMNNAME_PP_Plant_ID, null);
		else 
			set_Value (COLUMNNAME_PP_Plant_ID, Integer.valueOf(PP_Plant_ID));
	}

	/** Get Produktionsstätte.
		@return Produktionsstätte	  */
	@Override
	public int getPP_Plant_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Plant_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	@Override
	public org.eevolution.model.I_PP_Product_Planning getPP_Product_Planning() throws RuntimeException
	{
		return get_ValueAsPO(COLUMNNAME_PP_Product_Planning_ID, org.eevolution.model.I_PP_Product_Planning.class);
	}

	@Override
	public void setPP_Product_Planning(org.eevolution.model.I_PP_Product_Planning PP_Product_Planning)
	{
		set_ValueFromPO(COLUMNNAME_PP_Product_Planning_ID, org.eevolution.model.I_PP_Product_Planning.class, PP_Product_Planning);
	}

	/** Set Product Planning.
		@param PP_Product_Planning_ID Product Planning	  */
	@Override
	public void setPP_Product_Planning_ID (int PP_Product_Planning_ID)
	{
		if (PP_Product_Planning_ID < 1) 
			set_Value (COLUMNNAME_PP_Product_Planning_ID, null);
		else 
			set_Value (COLUMNNAME_PP_Product_Planning_ID, Integer.valueOf(PP_Product_Planning_ID));
	}

	/** Get Product Planning.
		@return Product Planning	  */
	@Override
	public int getPP_Product_Planning_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_PP_Product_Planning_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Verarbeitet.
		@param Processed 
		Checkbox sagt aus, ob der Beleg verarbeitet wurde. 
	  */
	@Override
	public void setProcessed (boolean Processed)
	{
		set_Value (COLUMNNAME_Processed, Boolean.valueOf(Processed));
	}

	/** Get Verarbeitet.
		@return Checkbox sagt aus, ob der Beleg verarbeitet wurde. 
	  */
	@Override
	public boolean isProcessed () 
	{
		Object oo = get_Value(COLUMNNAME_Processed);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Verarbeiten.
		@param Processing Verarbeiten	  */
	@Override
	public void setProcessing (boolean Processing)
	{
		set_Value (COLUMNNAME_Processing, Boolean.valueOf(Processing));
	}

	/** Get Verarbeiten.
		@return Verarbeiten	  */
	@Override
	public boolean isProcessing () 
	{
		Object oo = get_Value(COLUMNNAME_Processing);
		if (oo != null) 
		{
			 if (oo instanceof Boolean) 
				 return ((Boolean)oo).booleanValue(); 
			return "Y".equals(oo);
		}
		return false;
	}

	/** Set Datensatz-ID.
		@param Record_ID 
		Direct internal record ID
	  */
	@Override
	public void setRecord_ID (int Record_ID)
	{
		if (Record_ID < 0) 
			set_ValueNoCheck (COLUMNNAME_Record_ID, null);
		else 
			set_ValueNoCheck (COLUMNNAME_Record_ID, Integer.valueOf(Record_ID));
	}

	/** Get Datensatz-ID.
		@return Direct internal record ID
	  */
	@Override
	public int getRecord_ID () 
	{
		Integer ii = (Integer)get_Value(COLUMNNAME_Record_ID);
		if (ii == null)
			 return 0;
		return ii.intValue();
	}

	/** Set Referenz.
		@param Reference 
		Reference for this record
	  */
	@Override
	public void setReference (java.lang.String Reference)
	{
		set_Value (COLUMNNAME_Reference, Reference);
	}

	/** Get Referenz.
		@return Reference for this record
	  */
	@Override
	public java.lang.String getReference () 
	{
		return (java.lang.String)get_Value(COLUMNNAME_Reference);
	}

	/** Set Mitteilung.
		@param TextMsg 
		Text Message
	  */
	@Override
	public void setTextMsg (java.lang.String TextMsg)
	{
		set_Value (COLUMNNAME_TextMsg, TextMsg);
	}

	/** Get Mitteilung.
		@return Text Message
	  */
	@Override
	public java.lang.String getTextMsg () 
	{
		return (java.lang.String)get_Value(COLUMNNAME_TextMsg);
	}
}