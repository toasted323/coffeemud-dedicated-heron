package com.planet_ink.coffee_mud.Protocols.gmcp;

import com.planet_ink.coffee_mud.Common.interfaces.Climate;
import com.planet_ink.coffee_mud.Exits.interfaces.Exit;
import com.planet_ink.coffee_mud.Locales.interfaces.Room;
import com.planet_ink.coffee_mud.MOBS.interfaces.MOB;
import com.planet_ink.coffee_mud.core.CMLib;
import com.planet_ink.coffee_mud.core.CMath;
import com.planet_ink.coffee_mud.core.Directions;
import com.planet_ink.coffee_mud.core.MiniJSON;
import com.planet_ink.coffee_mud.core.MiniJSON.JSONObject;

public class RoomInfoBuilder {
	private final Room room;
	private final MOB mob;
	private final JSONObject json;

	public RoomInfoBuilder(Room room, MOB mob) {
		this.room = room;
		this.mob = mob;
		this.json = new JSONObject();
	}

	private void addBasicInfo() {
		final String roomID = CMLib.map().getExtendedRoomID(room);
		final String domType = getDomainType();

		json.put("num", CMath.abs(roomID.hashCode()));
		json.put("id", roomID);
		json.put("name", MiniJSON.toJSONString(room.displayText(mob)));
		json.put("zone", MiniJSON.toJSONString(room.getArea().name()));
		json.put("desc", MiniJSON.toJSONString(room.description(mob)));
		json.put("terrain", domType.toLowerCase());
		json.put("details", "");
	}

	public String build() {
		if (mob == null || room == null || mob.isMonster() || !CMLib.flags().canSee(mob)) {
			return "room.info {}";
		}

		addBasicInfo();
		addExits();
		addCoordinates();
		return "room.info " + json.toString();
	}

	private String getDomainType() {
		if ((room.domainType() & Room.INDOORS) == 0)
			return Room.DOMAIN_OUTDOOR_DESCS[room.domainType()];
		else
			return Room.DOMAIN_INDOORS_DESCS[CMath.unsetb(room.domainType(), Room.INDOORS)];
	}

	private void addExits() {
		JSONObject exits = new JSONObject();
		if (room.getArea().getClimateObj().weatherType(room) == Climate.WEATHER_FOG) {
			json.put("exits", exits);
			return;
		}

		for (int d : Directions.DISPLAY_CODES()) {
			Exit exit = room.getExitInDir(d);
			Room room2 = room.getRoomInDir(d);
			String viewableText = (exit != null) ? exit.viewableText(mob, room2).toString() : "";

			if (viewableText.length() > 0 || (room2 != null && mob.isAttributeSet(MOB.Attrib.SYSOPMSGS))) {
				JSONObject exitInfo = new JSONObject();
				exitInfo.put("direction", CMLib.directions().getDirectionName(d, CMLib.flags().getDirType(room)));
				exitInfo.put("roomId", room2 != null ? CMath.abs(CMLib.map().getExtendedRoomID(room2).hashCode()) : null);
				exitInfo.put("description", viewableText);
				exitInfo.put("visited", mob.playerStats() != null && room2 != null && mob.playerStats().hasVisited(room2));
				exits.put(CMLib.directions().getDirectionChar(d), exitInfo);
			}
		}
		json.put("exits", exits);
	}

	private void addCoordinates() {
		JSONObject coord = new MiniJSON.JSONObject();
		if (room.getArea().getClimateObj().weatherType(room) == Climate.WEATHER_FOG) {
			json.put("coord", coord);
			return;
		}

		coord.put("id", 0);
		coord.put("x", -1);
		coord.put("y", -1);
		coord.put("cont", 0);
		json.put("coord", coord);
	}
}
