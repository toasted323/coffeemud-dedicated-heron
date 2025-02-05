package com.planet_ink.coffee_mud.Commands;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.WebMacros.BankAccountInfo;
import com.planet_ink.coffee_mud.WebMacros.BankAccountInfo.BankAccountStuff;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.MoneyLibrary.DebtItem;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Commands.*;
import com.planet_ink.coffee_mud.Commands.Inventory.InventoryList;

import java.util.*;

/*
	Written by Robert Little - The Looking Glass
   Copyright 2010-2024 Bo Zimmerman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

	   http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
public class Wealth extends Inventory
{
	public Wealth()
	{
	}

	private final String[]	access	= I(new String[] { "WEALTH" });

	@Override
	public String[] getAccessWords()
	{
		return access;
	}

	private final static Class<?>[][] internalParameters=new Class<?>[][]{{MOB.class}};

	@Override
	public StringBuilder getInventory(final MOB seer, final MOB mob, final String mask, final boolean longInv)
	{
		final StringBuilder msg=new StringBuilder("");
		final InventoryList list = fetchInventory(seer,mob);
		if(list.moneyItems.size()==0)
			msg.append(L("\n\r^HMoney:^N None!\n\r"));
		else
			msg.append(getShowableMoney(list));
		for(final Enumeration<String> j=CMLib.city().bankChains(null);j.hasMoreElements();)
		{
			final String bankChain=j.nextElement();
			final Banker bankerM=CMLib.city().getBank(bankChain,bankChain);
			if(bankerM instanceof MOB)
			{
				if(!bankerM.isSold(ShopKeeper.DEAL_CLANBANKER))
				{
					final double balance=bankerM.getBalance(mob.Name()); // this works for clans because name==clan name
					if(balance > 0.0)
					{
						msg.append(L("\n\r^HMoney at bank chain '@x1': ^N\n\r@x2",
								bankChain,
								CMLib.beanCounter().nameCurrencyLong((MOB)bankerM, balance)));
					}
				}
			}
		}
		return msg;
	}

	@Override
	public boolean execute(final MOB mob, final List<String> commands, final int metaFlags)
		throws java.io.IOException
	{
		final StringBuilder msg=getInventory(mob,mob,CMParms.combine(commands,1),false);
		if(msg.length()==0)
			mob.tell(L("You have no money on you."));
		else
		if(!mob.isMonster())
			mob.session().getOutputFormatter().wraplessPrintln(msg.toString());
		return false;
	}

	@Override
	public Object executeInternal(final MOB mob, final int metaFlags, final Object... args) throws java.io.IOException
	{
		if(!super.checkArguments(internalParameters, args))
			return "";
		final MOB M=(MOB)args[0];
		return getInventory(M,mob,null,false).toString();
	}

	public int ticksToExecute()
	{
		return 0;
	}

	@Override
	public boolean canBeOrdered()
	{
		return true;
	}
}
