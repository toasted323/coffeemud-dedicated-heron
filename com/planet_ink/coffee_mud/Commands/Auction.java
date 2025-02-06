package com.planet_ink.coffee_mud.Commands;
import com.planet_ink.coffee_mud.core.interfaces.*;
import com.planet_ink.coffee_mud.core.interfaces.ShopKeeper.ViewType;
import com.planet_ink.coffee_mud.core.*;
import com.planet_ink.coffee_mud.core.collections.*;
import com.planet_ink.coffee_mud.Abilities.interfaces.*;
import com.planet_ink.coffee_mud.Areas.interfaces.*;
import com.planet_ink.coffee_mud.Behaviors.interfaces.*;
import com.planet_ink.coffee_mud.CharClasses.interfaces.*;
import com.planet_ink.coffee_mud.Commands.interfaces.*;
import com.planet_ink.coffee_mud.Common.interfaces.*;
import com.planet_ink.coffee_mud.Exits.interfaces.*;
import com.planet_ink.coffee_mud.Items.interfaces.*;
import com.planet_ink.coffee_mud.Libraries.interfaces.*;
import com.planet_ink.coffee_mud.Locales.interfaces.*;
import com.planet_ink.coffee_mud.MOBS.interfaces.*;
import com.planet_ink.coffee_mud.Races.interfaces.*;

import java.util.*;

/*
   Copyright 2004-2024 Bo Zimmerman

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
public class Auction extends Channel implements Tickable
{
	public Auction()
	{
	}

	private final String[]	access	= I(new String[] { "AUCTION" });

	@Override
	public String[] getAccessWords()
	{
		return access;
	}

	@Override
	public String name()
	{
		return "Auction";
	}

	protected final static String MESSAGE_NOAUCTION()
	{
		return CMLib.lang().fullSessionTranslation("There is not currently a live auction.  Use AUCTION UP syntax to add one, or visit an auctioneer for a long auction.");
	}

	public String liveAuctionStatus()
	{
		if(getLiveData().getAuctionedItem()!=null)
		{
			String bidWords=CMLib.beanCounter().nameCurrencyShort(getLiveData().getCurrency(),getLiveData().getBid());
			if(bidWords.length()==0)
				bidWords="0";
			return "Up for live auction: "+getLiveData().getAuctionedItem().name()+".  The current bid is "+bidWords+".";
		}
		return "";
	}

	protected AuctionData   liveAuctionData=null;

	protected AuctionData getLiveData()
	{
		if(liveAuctionData == null)
		{
			liveAuctionData = (AuctionData)CMClass.getCommon("DefaultAuction");
		}
		return liveAuctionData;
	}

	protected static final int	STATE_START		= 0;
	protected static final int	STATE_RUNOUT	= 1;
	protected static final int	STATE_ONCE		= 2;
	protected static final int	STATE_TWICE		= 3;
	protected static final int	STATE_THREE		= 4;
	protected static final int	STATE_CLOSED	= 5;

	private static final Set<ViewType> viewFlags = new XHashSet<ViewType>(new ViewType[] {ViewType.BASIC});

	@Override
	public int getTickStatus()
	{
		return Tickable.STATUS_NOT;
	}

	public void setLiveAuctionState(final int code)
	{
		getLiveData().setAuctionState(code);
		getLiveData().setAuctionTickDown(15000/CMProps.getTickMillis());
	}

	@Override
	public boolean tick(final Tickable ticking, final int tickID)
	{
		if(tickID==Tickable.TICKID_LIVEAUCTION)
		{
			final AuctionData AD = getLiveData();
			AD.setAuctionTickDown(AD.getAuctionTickDown()-1);
			if(AD.getAuctionTickDown()<=0)
			{
				if((AD.getAuctionState()==STATE_START)
				&&((System.currentTimeMillis()-AD.getStartTime())<(5*15000)))
				{
					final MOB auctioneerM=AD.getAuctioningMob();
					final MOB winnerM=AD.getHighBidderMob();
					if(((System.currentTimeMillis()-AD.getStartTime())>(3*15000))
					&&((winnerM==null)||(winnerM==auctioneerM)))
						setLiveAuctionState(STATE_RUNOUT);
					else
						setLiveAuctionState(STATE_START);
					return true;
				}
				setLiveAuctionState(AD.getAuctionState()+1);
				final List<String> auctionCmdV=new ArrayList<String>();
				auctionCmdV.add("AUCTION");
				auctionCmdV.add("CHANNEL");
				switch(AD.getAuctionState())
				{
				case STATE_RUNOUT:
					auctionCmdV.add(L("The live auction for ^[@x1^] is almost done. The current bid is @x2.",
							AD.getAuctionedItem().name(),CMLib.beanCounter().nameCurrencyShort(AD.getCurrency(),AD.getBid())));
					break;
				case STATE_ONCE:
					auctionCmdV.add(L("@x1 for ^[@x2^] going ONCE!",CMLib.beanCounter().nameCurrencyShort(AD.getCurrency(),AD.getBid()),AD.getAuctionedItem().name()));
					break;
				case STATE_TWICE:
					auctionCmdV.add(L("@x1 for ^[@x2^] going TWICE!",CMLib.beanCounter().nameCurrencyShort(AD.getCurrency(),AD.getBid()),AD.getAuctionedItem().name()));
					break;
				case STATE_THREE:
					auctionCmdV.add(L("@x1 going for ^[@x2^]! Last chance!",AD.getAuctionedItem().name(),CMLib.beanCounter().nameCurrencyShort(AD.getCurrency(),AD.getBid())));
					break;
				case STATE_CLOSED:
					{
						final MOB auctioneerM=AD.getAuctioningMob();
						final MOB winnerM=AD.getHighBidderMob();
						if((winnerM!=null)&&(winnerM!=AD.getAuctioningMob()))
						{
							auctionCmdV.add(L("^[@x1^] SOLD to @x2 for @x3.",AD.getAuctionedItem().name(),winnerM.name(),
									CMLib.beanCounter().nameCurrencyShort(AD.getCurrency(),AD.getBid())));
							auctioneerM.doCommand(auctionCmdV,MUDCmdProcessor.METAFLAG_FORCED);
							if(AD.getAuctionedItem() != null)
							{
								AD.getAuctionedItem().unWear();
								final AuctionPolicy aRates=(AuctionPolicy)CMClass.getCommon("DefaultAuctionPolicy");
								winnerM.location().moveItemTo(AD.getAuctionedItem(),ItemPossessor.Expire.Player_Drop);
								final double houseCut=Math.floor(AD.getBid()*aRates.liveFinalCutPct());
								final double finalAmount=AD.getBid()-houseCut;
								CMLib.coffeeShops().returnMoney(winnerM,AD.getCurrency(),AD.getHighBid()-AD.getBid());
								CMLib.coffeeShops().returnMoney(auctioneerM,AD.getCurrency(),finalAmount);
								auctioneerM.tell(L("@x1 has been transferred to you as payment from @x2, after the house took a cut of @x3.  The goods have also been transferred in exchange.",
										CMLib.beanCounter().nameCurrencyShort(AD.getCurrency(),finalAmount),
										winnerM.name(auctioneerM),
										CMLib.beanCounter().nameCurrencyShort(AD.getCurrency(),houseCut)));
								CMLib.commands().postStand(winnerM, true, true);
								final boolean addRobberyProtection = !AD.getAuctionedItem().phyStats().isAmbiance(PhyStats.Ambiance.SUPPRESS_ROBBERY);
								if(addRobberyProtection)
								{
									AD.getAuctionedItem().basePhyStats().addAmbiance(PhyStats.Ambiance.SUPPRESS_ROBBERY.code());
									AD.getAuctionedItem().recoverPhyStats();
								}
								try
								{
									if(CMLib.commands().postGet(winnerM,null,AD.getAuctionedItem(),false)
									||(winnerM.isMine(AD.getAuctionedItem())))
									{
										winnerM.tell(L("@x1 has been transferred to @x2.  You should have received the auctioned goods.  This auction is complete.",
												CMLib.beanCounter().nameCurrencyShort(AD.getCurrency(),AD.getBid()),
												auctioneerM.name(winnerM)));
										if(AD.getAuctionedItem() instanceof LandTitle)
										{
											final CMMsg msg=CMClass.getMsg(auctioneerM,winnerM,AD.getAuctionedItem(),CMMsg.MASK_ALWAYS|CMMsg.TYP_GIVE,null);
											AD.getAuctionedItem().executeMsg(winnerM,msg);
										}
									}
									else
									{
										auctioneerM.moveItemTo(AD.getAuctionedItem());
										auctioneerM.tell(L("Your transaction could not be completed because @x1 was unable to collect the item.  "
												+ "Please contact @x2 about receipt of ^[@x3^] for @x4.",
												winnerM.name(auctioneerM),
												winnerM.name(auctioneerM),
												AD.getAuctionedItem().name(winnerM),
												CMLib.beanCounter().nameCurrencyShort(AD.getCurrency(),AD.getBid())));
										winnerM.tell(L("Your transaction could not be completed because you were unable to collect the item.  "
												+ "Please contact @x1 about receipt of ^[@x2^] for @x3.",
												auctioneerM.name(winnerM),
												AD.getAuctionedItem().name(winnerM),
												CMLib.beanCounter().nameCurrencyShort(AD.getCurrency(),AD.getBid())));
									}
								}
								finally
								{
									if(addRobberyProtection)
									{
										AD.getAuctionedItem().basePhyStats().delAmbiance(PhyStats.Ambiance.SUPPRESS_ROBBERY.code());
										AD.getAuctionedItem().recoverPhyStats();
									}
								}
							}
						}
						else
						{
							if(!auctioneerM.isMine(AD.getAuctionedItem()))
								auctioneerM.moveItemTo(AD.getAuctionedItem());
							auctionCmdV.clear();
							auctionCmdV.add("AUCTION");
							auctionCmdV.add("CHANNEL");
							auctionCmdV.add(L("The auction for ^[@x1^] has ended without a winner.",AD.getAuctionedItem().name()));
							auctioneerM.doCommand(auctionCmdV,MUDCmdProcessor.METAFLAG_FORCED);
						}
						AD.setAuctioningMob(null);
						AD.setAuctioningMobName(null);
						AD.setAuctionedItem(null);
						AD.setHighBidderMob(null);
						AD.setHighBidderMobName(null);
						AD.setHighBid(0.0);
						AD.setBid(0.0);
						AD.setAuctionState(0);
						CMLib.threads().deleteTick(this,Tickable.TICKID_LIVEAUCTION);
					}
					return false;
				}
				final MOB auctioneerM=AD.getAuctioningMob();
				auctioneerM.doCommand(auctionCmdV,MUDCmdProcessor.METAFLAG_FORCED);
			}
		}
		return true;
	}

	public boolean doLiveAuction(final MOB mob, final List<String> commands, final Environmental target)
	{
		final List<String> V=new ArrayList<String>();
		V.add("AUCTION");
		V.add("CHANNEL");
		if(target!=null)
		{
			if(!(target instanceof Item))
				return false;
			getLiveData().setAuctioningMob(mob);
			getLiveData().setAuctioningMobName(null);
			getLiveData().setAuctionedItem((Item)target);
			final String sb=CMParms.combine(commands,0);
			getLiveData().setCurrency(CMLib.english().parseNumPossibleGoldCurrency(mob,sb));
			if((getLiveData().getCurrency().length()==0)&&(CMath.isInteger(sb)))
				getLiveData().setCurrency(CMLib.beanCounter().getCurrency(mob));
			final double denomination=CMLib.english().parseNumPossibleGoldDenomination(null,getLiveData().getCurrency(),sb);
			final long num=CMLib.english().parseNumPossibleGold(null,sb);
			getLiveData().setBid(CMath.mul(denomination,num));
			getLiveData().setHighBid(getLiveData().getBid()-1);
			getLiveData().setHighBidderMob(null);
			getLiveData().setHighBidderMobName(null);
			getLiveData().setStartTime(System.currentTimeMillis());
			setLiveAuctionState(STATE_START);
			CMLib.threads().startTickDown(this,Tickable.TICKID_LIVEAUCTION,1);
			final String bidWords=CMLib.beanCounter().nameCurrencyShort(getLiveData().getCurrency(),getLiveData().getBid());
			if(target instanceof Item)
				mob.delItem((Item)target);
			V.add(L("New live auction: ^[@x1^].  The opening bid is @x2.",getLiveData().getAuctionedItem().name(),bidWords));
			if(getLiveData().getAuctioningMob()!=null)
				getLiveData().getAuctioningMob().doCommand(V,MUDCmdProcessor.METAFLAG_FORCED);
		}
		else
		{
			if(getLiveData().getAuctionState()>0)
				setLiveAuctionState(STATE_RUNOUT);
			String sb="";
			if(commands!=null)
				sb=CMParms.combine(commands,0);
			final MOB M=getLiveData().getHighBidderMob();
			final Triad<String,Double,Long> bidObjs=CMLib.english().parseMoneyStringSDL(mob,sb,getLiveData().getCurrency());
			if(bidObjs != null)
			{
				final String currency=bidObjs.first;
				final double amt=CMath.mul(bidObjs.second.doubleValue(),bidObjs.third.doubleValue());
				final String[] resp=CMLib.coffeeShops().bid(mob,amt,currency,getLiveData(),getLiveData().getAuctionedItem(),V);
				if(resp!=null)
				{
					if(resp[0]!=null)
						mob.tell(resp[0]);
					if((resp[1]!=null)&&(M!=null))
						M.tell(resp[1]);
				}
				if((V.size()>2)
				&&(getLiveData().getAuctioningMob()!=null))
					getLiveData().getAuctioningMob().doCommand(V,MUDCmdProcessor.METAFLAG_FORCED);
			}
		}
		return true;
	}

	public void auctionNotify(final MOB M, final String resp, final String regardingItem) throws java.io.IOException
	{
		if(CMLib.flags().isInTheGame(M,true))
			M.tell(resp);
		else
		if(M.playerStats()!=null)
		{
			CMLib.smtp().emailIfPossible(CMProps.getVar(CMProps.Str.SMTPSERVERNAME),
										"auction@"+CMProps.getVar(CMProps.Str.MUDDOMAIN).toLowerCase(),
										"noreply@"+CMProps.getVar(CMProps.Str.MUDDOMAIN).toLowerCase(),
										M.playerStats().getEmail(),
										L("Auction Update for item: @x1",regardingItem),
										resp);
		}
	}

	protected Set<ViewType> viewFlags()
	{
		return viewFlags;
	}

	@Override
	public boolean execute(final MOB mob, final List<String> commands, final int metaFlags)
		throws java.io.IOException
	{
		//mob.tell(L("Auctions are currently closed for maintenance.  When it re-opens, this command will continue to remain available for live auctions, and new auctioneer mobs will be placed in the major cities for doing multi-day auctions, so keep your eyes open for that coming soon!"));
		//if((mob!=null)||(commands!=null)) return false;
		final PlayerStats pstats=mob.playerStats();
		if(pstats==null)
			return false;
		final int channelInt=CMLib.channels().getChannelIndex("AUCTION");
		final int channelNum=CMLib.channels().getChannelCodeNumber("AUCTION");

		if(CMath.isSet(pstats.getChannelMask(),channelInt))
		{
			pstats.setChannelMask(pstats.getChannelMask()&(pstats.getChannelMask()-channelNum));
			mob.tell(L("The AUCTION channel has been turned on.  Use `NOAUCTION` to turn it off again."));
		}

		String cmd=null;
		commands.remove(0);
		if(commands.size()<1)
			cmd="";
		else
			cmd=commands.get(0).toUpperCase();

		if(cmd.equals("LIST"))
		{
			commands.remove(0);
			final StringBuffer buf=new StringBuffer("");
			if(getLiveData().getAuctionedItem()!=null)
			{
				buf.append(L("\n\r^HCurrent *live* auction: ^N\n\r"));
				buf.append(liveAuctionStatus()+"\n\r");
			}
			else
				buf.append(MESSAGE_NOAUCTION());
			mob.tell(buf.toString());
			return true;
		}
		else
		if(cmd.equals("UP"))
		{
			commands.remove(0);
			if(getLiveData().getAuctionedItem()!=null)
			{
				mob.tell(L("A live auction is already underway.  Do AUCTION LIST to see it."));
				return false;
			}
			final List<String> V=new ArrayList<String>();
			if((commands.size()>=2)
			&&((CMLib.english().parseNumPossibleGold(mob,commands.get(commands.size()-1))>0)||(commands.get(commands.size()-1).equals("0"))))
			{
				V.add(commands.get(commands.size()-1));
				commands.remove(commands.size()-1);
			}
			else
				V.add("0");

			final String s=CMParms.combine(commands,0);
			final Environmental E=mob.findItem(null,s);
			if((E==null)||(E instanceof MOB))
			{
				mob.tell(L("@x1 is not an item you can auction.",s));
				return false;
			}
			if((E instanceof Container)&&(((Container)E).hasContent()))
			{
				mob.tell(L("^[@x1^] will have to be emptied first.",E.name()));
				return false;
			}
			if(!(((Item)E).amWearingAt(Wearable.IN_INVENTORY)))
			{
				mob.tell(L("^[@x1^] will have to be removed first.",E.name()));
				return false;
			}
			final AuctionPolicy aRates=(AuctionPolicy)CMClass.getCommon("DefaultAuctionPolicy");
			final double deposit=aRates.liveListingPrice();
			final String depositAmt=CMLib.beanCounter().nameCurrencyLong(mob, deposit);

			if(deposit>0.0)
			{
				if((mob.isMonster())
				||(!mob.session().getSyncModalDialogManager().confirm(L("Auctioning ^[@x1^] will cost a listing fee of @x2, proceed (Y/n)?",E.name(),depositAmt),"Y")))
					return false;
			}
			else
			if((mob.isMonster())
			||(!mob.session().getSyncModalDialogManager().confirm(L("Auction ^[@x1^] live, with a starting bid of @x2 (Y/n)?",E.name(),(V.get(0))),"Y")))
				return false;
			if(CMLib.beanCounter().getTotalAbsoluteValue(mob,CMLib.beanCounter().getCurrency(mob))<deposit)
			{
				mob.tell(L("You don't have enough @x1 to cover the listing fee!",CMLib.beanCounter().getDenominationName(CMLib.beanCounter().getCurrency(mob))));
				return false;
			}
			CMLib.beanCounter().subtractMoney(mob, CMLib.beanCounter().getCurrency(mob), deposit);
			doLiveAuction(mob,V,E);
			if(getLiveData().getAuctionedItem()!=null)
			{
				getLiveData().setAuctioningMob(mob);
				getLiveData().setAuctioningMobName(null);
			}
			return true;
		}
		else
		if(cmd.equals("BID"))
		{
			commands.remove(0);
			if(getLiveData().getAuctionedItem()==null)
			{
				mob.tell(MESSAGE_NOAUCTION());
				return false;
			}
			if(commands.size()<1)
			{
				mob.tell(L("Bid how much?"));
				return false;
			}
			final String amount=CMParms.combine(commands,0);
			if(amount.trim().equals("0")||(amount.startsWith("0 ")))
			{
				mob.tell(L("Bid how much?"));
				return false;
			}
			doLiveAuction(mob,new XVector<String>(amount),null);
			return true;
		}
		else
		if(cmd.equals("CLOSE"))
		{
			commands.remove(0);
			if((getLiveData().getAuctionedItem()==null)
			||(getLiveData().getAuctioningMob()==null)) // must be non-null below
			{
				mob.tell(MESSAGE_NOAUCTION());
				return false;
			}
			if((getLiveData().getAuctionedItem()==null)
			||(getLiveData().getAuctioningMob()!=mob))
			{
				mob.tell(L("You are not currently running a live auction."));
				return false;
			}
			final List<String> V=new ArrayList<String>();
			V.add("AUCTION");
			V.add(L("The auction has been closed."));
			CMLib.threads().deleteTick(this,Tickable.TICKID_LIVEAUCTION);
			getLiveData().getAuctioningMob().moveItemTo(getLiveData().getAuctionedItem());
			if((getLiveData().getHighBid()>0.0)&&(getLiveData().getHighBidderMob()!=null))
				CMLib.coffeeShops().returnMoney(getLiveData().getHighBidderMob(),getLiveData().getCurrency(),getLiveData().getHighBid());
			getLiveData().setAuctioningMob(null);
			getLiveData().setAuctioningMobName(null);
			getLiveData().setAuctionedItem(null);
			super.execute(mob,V,metaFlags);
			return true;
		}
		else
		if(cmd.equals("INFO"))
		{
			commands.remove(0);
			if((getLiveData().getAuctionedItem()==null)||(getLiveData().getAuctioningMob()==null))
			{
				mob.tell(MESSAGE_NOAUCTION());
				return false;
			}
			Environmental E=null;
			E=getLiveData().getAuctionedItem();
			mob.tell(L("Item: ^[@x1^]",E.name()));
			CMLib.commands().handleBeingLookedAt(CMClass.getMsg(mob,CMMsg.MASK_ALWAYS|CMMsg.MSG_EXAMINE,null));
			mob.tell(CMLib.coffeeShops().getViewDescription(mob,E, new XHashSet<ViewType>(ViewType.BASIC)));
			return true;
		}
		else
		if(cmd.equals("CHANNEL"))
		{
			commands.remove(0);
			if(commands.size()==0)
			{
				mob.tell(L("Channel what?"));
				return false;
			}
			if(getLiveData().getAuctionedItem()==null)
			{
				mob.tell(L("Channeling is only allowed during live auctions."));
				return false;
			}
			commands.add(0,"AUCTION");
			super.execute(mob,commands,metaFlags);
			return true;
		}
		commands.add(0,"AUCTION");
		super.execute(mob,commands,metaFlags);
		return false;
	}

	@Override
	public boolean canBeOrdered()
	{
		return false;
	}


	@Override
	public boolean securityCheck(final MOB mob)
	{
		return !CMSecurity.isDisabled(CMSecurity.DisFlag.CHANNELAUCTION);
	}
}
