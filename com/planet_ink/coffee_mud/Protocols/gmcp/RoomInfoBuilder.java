package com.planet_ink.coffee_mud.Protocols.gmcp;

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

	public String build() {
		addBasicInfo();
		addExits();
		addCoordinates();
		return "room.info " + json.toString();
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

	private String getDomainType() {
		if ((room.domainType() & Room.INDOORS) == 0)
			return Room.DOMAIN_OUTDOOR_DESCS[room.domainType()];
		else
			return Room.DOMAIN_INDOORS_DESCS[CMath.unsetb(room.domainType(), Room.INDOORS)];
	}

	private void addExits() {
		JSONObject exits = new JSONObject();
		for (int d = 0; d < Directions.NUM_DIRECTIONS(); d++) {
			final Room R2 = room.getRoomInDir(d);
			if ((R2 != null) && (room.getExitInDir(d) != null)) {
				final String room2ID = CMLib.map().getExtendedRoomID(R2);
				if (room2ID.length() > 0) {
					exits.put(CMLib.directions().getDirectionChar(d), CMath.abs(room2ID.hashCode()));
				}
			}
		}
		json.put("exits", exits);
	}

	private void addCoordinates() {
		JSONObject coord = new MiniJSON.JSONObject();
		coord.put("id", 0);
		coord.put("x", -1);
		coord.put("y", -1);
		coord.put("cont", 0);
		json.put("coord", coord);
	}
}
