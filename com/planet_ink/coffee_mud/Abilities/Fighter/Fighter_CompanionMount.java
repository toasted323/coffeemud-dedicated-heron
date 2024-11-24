package com.planet_ink.coffee_mud.Abilities.Fighter;

import com.planet_ink.coffee_mud.Abilities.interfaces.Ability;
import com.planet_ink.coffee_mud.Common.interfaces.PhyStats;
import com.planet_ink.coffee_mud.Common.interfaces.TimeClock;
import com.planet_ink.coffee_mud.Locales.interfaces.Room;
import com.planet_ink.coffee_mud.MOBS.interfaces.MOB;
import com.planet_ink.coffee_mud.Races.interfaces.Race;
import com.planet_ink.coffee_mud.core.CMClass;
import com.planet_ink.coffee_mud.core.CMLib;
import com.planet_ink.coffee_mud.core.CMProps;
import com.planet_ink.coffee_mud.core.Log;
import com.planet_ink.coffee_mud.core.collections.PairList;
import com.planet_ink.coffee_mud.core.interfaces.Rideable;
import com.planet_ink.coffee_mud.core.interfaces.Tickable;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

/*
   Copyright 2024 github.com/toasted323

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

   CHANGES:
   2024-11 toasted323: TODO summarize before merge
*/
public class Fighter_CompanionMount extends FighterSkill
{
	@Override
	public String ID()
	{
		return "Fighter_CompanionMount";
	}

	private final static String localizedName = CMLib.lang().L("Companion Mount");

	@Override
	public String name()
	{
		return localizedName;
	}

	@Override
	protected int canAffectCode()
	{
		return CAN_MOBS;
	}

	@Override
	public String displayText()
	{
		if((affected instanceof MOB)
				&& (mountMob != null)
				&& (mountTicks < Integer.MAX_VALUE/2))
		{
			final MOB mob = (MOB) affected;
			final MOB mn = mountMob;
			if((mn != null) && (mob != null))
			{
				final TimeClock C = CMLib.time().homeClock(mob);
				if(C != null)
				{
					return L("(Building loyalty with @x1, @x2 remain)",mn.name(),
							C.deriveEllapsedTimeString(mountTicks*CMProps.getTickMillis()));
				}
			}
		}
		return "";
	}

	@Override
	public int abstractQuality()
	{
		return Ability.QUALITY_INDIFFERENT;
	}

	@Override
	public int classificationCode()
	{
		return Ability.ACODE_SKILL|Ability.DOMAIN_ANIMALAFFINITY;
	}

	@Override
	public boolean isAutoInvoked()
	{
		return true;
	}

	@Override
	public boolean canBeUninvoked()
	{
		return false;
	}

	protected volatile int mountTicks = Integer.MAX_VALUE;
	protected volatile MOB mountMob = null;

	@Override
	public boolean tick(final Tickable ticking,final int tickID)
	{
		if(!super.tick(ticking,tickID)) return false;
		if(affected instanceof MOB)
		{
			final MOB mob = (MOB) affected;
			Log.debugOut("Fighter_CompanionMount",mob.Name()+" tick started.");
			mob.tell(L("Debug: CompanionMount tick started."));

			final Rideable riding = mob.riding();
			if(riding instanceof MOB)
			{
				MOB target = mountMob;
				if(riding == mountMob)
				{
					Log.debugOut("Fighter_CompanionMount",mob.Name()+" is riding "+mountMob.Name()+". Ticks remaining: "+mountTicks);
					mob.tell(L("Debug: You are riding @x1. Ticks remaining: @x2",mountMob.Name(),""+mountTicks));

					if((mountTicks%5 == 0) && (CMLib.dice().rollPercentage() < 10))
					{
						super.helpProficiency(mob,0);
						Log.debugOut("Fighter_CompanionMount",mob.Name()+" gained proficiency.");
						mob.tell(L("Debug: You gained proficiency in CompanionMount."));
					}

					if(!proficiencyCheck(mob,0,false))
					{
						Log.debugOut("Fighter_CompanionMount",mob.Name()+" failed proficiency check.");
						mob.tell(L("Debug: You failed the proficiency check for CompanionMount."));
						return false;
					}
					mountTicks--;
					if(mountTicks <= 0)
					{
						Log.debugOut("Fighter_CompanionMount",mob.Name()+" mount ticks reached 0. Attempting to make loyal companion.");
						mob.tell(L("Debug: Mount ticks reached 0. Attempting to make loyal companion."));

						if(target.fetchEffect("Loyalty") == null)
						{
							if(mob.location() != null)
							{
								int numLoyal = 0;
								for(int f = 0; f < mob.numFollowers(); f++)
								{
									final MOB M = mob.fetchFollower(f);
									if((M != mob)
											&& (CMLib.flags().isAnimalIntelligence(M))
											&& (M.fetchEffect("Loyalty") != null))
										numLoyal++;
								}
								if(numLoyal > this.getXLEVELLevel(mob))
								{
									mob.tell(L("You lack the expertise to gain another companion."));
									mountTicks = Integer.MAX_VALUE/2;
									return true;
								}
								final List<Ability> affects = new LinkedList<Ability>();
								for(final Enumeration<Ability> a = target.personalEffects(); a.hasMoreElements(); )
								{
									final Ability A = a.nextElement();
									affects.add(A);
									target.delEffect(A);
								}
								final MOB targetCopy = (MOB) target.copyOf();
								for(final Ability A : affects)
									target.addEffect(A);
								for(final Enumeration<Ability> a = target.personalEffects(); a.hasMoreElements(); )
								{
									final Ability A = a.nextElement();
									if((A != null)
											&& ((A.flags()&Ability.FLAG_CHARMING) != 0)
											&& (A.canBeUninvoked()))
									{
										affects.remove(A);
										// in case there is wandering off...
										final Room oldR = target.location();
										oldR.delInhabitant(target);
										target.setLocation(null);
										A.unInvoke();
										oldR.addInhabitant(target);
										target.setLocation(oldR);
										mob.makePeace(true);
										target.makePeace(true);
										if((target.amFollowing() != mob)
												&& (!target.amDead())
												&& (!target.amDestroyed()))
											target.setFollowing(mob);
									}
								}
								try
								{
									for(Ability A : affects)
									{
										A = (Ability) A.copyOf();
										targetCopy.addEffect(A);
									}
								} catch(final Throwable t)
								{
								}

								if(target.amDestroyed() || target.amDead())
								{
									target = targetCopy;
									target.basePhyStats().setRejuv(PhyStats.NO_REJUV);
									target.phyStats().setRejuv(PhyStats.NO_REJUV);
									target.text();
									target.bringToLife(mob.location(),false);
									Log.debugOut("Fighter_CompanionMount",mob.Name()+"'s mount "+target.Name()+" was resurrected.");
									mob.tell(L("Debug: Your mount @x1 has been brought back to life.",target.Name()));
								} else if(target.location() != mob.location())
								{
									mob.location().bringMobHere(target,true);
									targetCopy.destroy();
									Log.debugOut("Fighter_CompanionMount",mob.Name()+"'s mount "+target.Name()+" was relocated to the player's location.");
									mob.tell(L("Debug: Your mount @x1 has been brought to your location.",target.Name()));
								} else
									targetCopy.destroy();
								mob.makePeace(true);
								target.makePeace(true);
								if((target.basePhyStats().rejuv() > 0) && (target.basePhyStats().rejuv() != PhyStats.NO_REJUV) && (target.getStartRoom() != null))
								{
									final MOB oldTarget = target;
									mob.setRiding(null);
									target = (MOB) target.copyOf();
									target.basePhyStats().setRejuv(PhyStats.NO_REJUV);
									target.phyStats().setRejuv(PhyStats.NO_REJUV);
									target.text();
									oldTarget.killMeDead(false);
									target.bringToLife(mob.location(),false);
									mob.setRiding((Rideable) target);

									Log.debugOut("Fighter_CompanionMount",mob.Name()+"'s mount "+target.Name()+" was rejuvenated.");
									mob.tell(L("Debug: Your mount @x1 has been rejuvenated with no rejuvenation timer.",target.Name()));
								}
								if(target.amFollowing() != mob)
									target.setFollowing(mob);
								Ability A = target.fetchEffect("Loyalty");
								if(A == null)
								{
									A = CMClass.getAbility("Loyalty");
									A.setMiscText("NAME="+mob.Name());
									A.setSavable(true);
									target.addNonUninvokableEffect(A);
									mob.tell(mob,target,null,L("<T-NAME> is now a loyal companion to you."));
									mob.location().recoverRoomStats();

									Log.sysOut("Fighter_CompanionMount",mob.Name()+"'s mount "+target.Name()+" has become loyal.");
								}
							}
							Log.debugOut("Fighter_CompanionMount",mob.Name()+" successfully made "+target.Name()+" a loyal companion.");
							mob.tell(L("Debug: You successfully made @x1 a loyal companion.",target.Name()));
						} else
						{
							mountTicks = Integer.MAX_VALUE;
							Log.debugOut("Fighter_CompanionMount",mob.Name()+"'s mount "+target.Name()+" is already loyal.");
							mob.tell(L("Debug: Your mount @x1 is already loyal.",target.Name()));
						}
					}
				} else
				{
					mountMob = (MOB) riding;
					Log.debugOut("Fighter_CompanionMount",mob.Name()+" is riding a new mount: "+mountMob.Name());
					mob.tell(L("Debug: You are riding a new mount: @x1",mountMob.Name()));

					if((mountMob.fetchEffect("Loyalty") == null)
							&& (mountMob.isMonster())
							&& (CMLib.flags().isAnimalIntelligence(mountMob)))
					{
						target = mountMob;
						final PairList<String, Race> choices = CMLib.utensils().getFavoredMounts(mob);
						if((choices.containsSecond(target.baseCharStats().getMyRace()))
								|| (choices.containsFirst(target.baseCharStats().getMyRace().racialCategory())))
						{
							final TimeClock C = CMLib.time().homeClock(mob);
							final long ticksPerHour = CMProps.getTicksPerMudHour();
							final int hoursPerDay = C.getHoursInDay();
							final int daysPerMonth = C.getDaysInMonth();

							Log.debugOut("Fighter_CompanionMount","Time calculations: "+ticksPerHour+" ticks per hour, "
									+hoursPerDay+" hours per day, "+daysPerMonth+" days per month.");
							mob.tell(L("Debug: Time calculations: @x1 ticks/hour, @x2 hours/day, @x3 days/month",
									""+ticksPerHour,""+hoursPerDay,""+daysPerMonth));

							mountTicks = (int) (daysPerMonth*hoursPerDay*ticksPerHour);
							mountTicks -= (int) (super.getXTIMELevel(mob)*ticksPerHour);
							if(mountTicks < 10) mountTicks = 10;

							Log.debugOut("Fighter_CompanionMount",mob.Name()+"'s mount ticks set to "+mountTicks);
							mob.tell(L("Debug: Mount ticks set to @x1",""+mountTicks));
						} else
							mountTicks = Integer.MAX_VALUE;
					} else
						mountTicks = Integer.MAX_VALUE;

					Log.debugOut("Fighter_CompanionMount",mob.Name()+" set new mount ticks to "+mountTicks);
					mob.tell(L("Debug: New mount ticks set to @x1",""+mountTicks));
				}
			} else
			{
				Log.debugOut("Fighter_CompanionMount",mob.Name()+" is not riding anything.");
				mob.tell(L("Debug: You are not riding anything."));
			}
		}
		return true;
	}
}
