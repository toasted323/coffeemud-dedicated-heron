package com.planet_ink.coffee_mud.MOBS;

import java.util.*;
import com.planet_ink.coffee_mud.utils.*;
import com.planet_ink.coffee_mud.interfaces.*;
import com.planet_ink.coffee_mud.common.*;
public class HillGiant extends StdMOB
{
	public String ID(){return "HillGiant";}
	public HillGiant()
	{
		super();
		Username="a Hill Giant";
		setDescription("A tall humanoid standing about 16 feet tall and very smelly.");
		setDisplayText("A Hill Giant glares at you.");
		setAlignment(0);
		setMoney(0);
		baseEnvStats.setWeight(3500 + Dice.roll(1, 1000, 0));


		baseCharStats().setStat(CharStats.INTELLIGENCE,6 + Dice.roll(1, 2, 0));
		baseCharStats().setStat(CharStats.STRENGTH,20);
		baseCharStats().setStat(CharStats.DEXTERITY,13);
		baseCharStats().setMyRace(CMClass.getRace("Giant"));
		baseCharStats().getMyRace().startRacing(this,false);

		baseEnvStats().setDamage(19);
		baseEnvStats().setSpeed(1.0);
		baseEnvStats().setAbility(0);
		baseEnvStats().setLevel(12);
		baseEnvStats().setArmor(0);

		baseState.setHitPoints(Dice.roll(baseEnvStats().level(),20,baseEnvStats().level()));

		addBehavior(CMClass.getBehavior("Aggressive"));

		recoverMaxState();
		resetToMaxState();
		recoverEnvStats();
		recoverCharStats();
	}
	public Environmental newInstance()
	{
		return new HillGiant();
	}
}
