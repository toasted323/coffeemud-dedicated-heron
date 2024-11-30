package com.planet_ink.coffee_mud.Abilities.Properties;

public abstract class BaseLoggableProperty extends Property {
	protected PropertyLogger logger;

	public BaseLoggableProperty() {
		logger = new PropertyLogger(ID());
	}
}
