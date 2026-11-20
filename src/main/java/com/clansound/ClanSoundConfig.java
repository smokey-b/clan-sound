package com.clansound;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("clansound")
public interface ClanSoundConfig extends Config
{
	@Range(max = 100)
	@ConfigItem(
		keyName = "volume",
		name = "Volume",
		description = "Volume of the sound, from 0 (muted) to 100 (full)"
	)
	default int volume()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "ignoreOwnMessages",
		name = "Ignore my own messages",
		description = "Do not play the sound for messages you send yourself"
	)
	default boolean ignoreOwnMessages()
	{
		return true;
	}

	@Range(max = 60)
	@ConfigItem(
		keyName = "cooldownSeconds",
		name = "Cooldown (seconds)",
		description = "Minimum seconds between sounds, so they do not overlap. 0 means no cooldown"
	)
	default int cooldownSeconds()
	{
		return 0;
	}
}
