package com.clansound;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.audio.AudioPlayer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.ImageUtil;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Clan Sound",
	description = "Plays a custom sound when a clan chat message arrives",
	tags = {"clan", "sound", "audio", "notification"},
	internalName = "clan-sound"
)
public class ClanSoundPlugin extends Plugin
{
	private static final String SOUND_FILE_NAME = "custom.wav";

	@Inject
	private ClanSoundConfig config;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private Client client;

	@Inject
	private AudioPlayer audioPlayer;

	@Inject
	private ScheduledExecutorService executor;

	private ClanSoundPanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp()
	{
		panel = new ClanSoundPanel(this::chooseSound, this::testSound);

		navButton = NavigationButton.builder()
			.tooltip("Clan Sound")
				.icon(ImageUtil.loadImageResource(ClanSoundPlugin.class, "/sidebar_icon.png"))
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);

		executor.execute(this::refreshStatus);
	}

	@Override
	protected void shutDown()
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		panel = null;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if (type != ChatMessageType.CLAN_CHAT
				&& type != ChatMessageType.CLAN_GUEST_CHAT)
		{
			return;
		}

		if (config.ignoreOwnMessages() && isOwnMessage(event))
		{
			return;
		}

		// play() reads from disk, so keep it off the client thread
		executor.execute(this::play);
	}

	private boolean isOwnMessage(ChatMessage event)
	{
		Player local = client.getLocalPlayer();
		if (local == null || local.getName() == null || event.getName() == null)
		{
			return false;
		}

		return Text.sanitize(event.getName())
				.equalsIgnoreCase(Text.sanitize(local.getName()));
	}

	private void chooseSound()
	{
		List<Filepath> chosen = new Filepath.Chooser()
			.setDialogTitle("Choose a WAV sound file")
			.setAcceptsFiles()
			.addExtensionFilter("WAV audio", "wav")
			.showDialog(panel);

		if (chosen == null || chosen.isEmpty())
		{
			return;
		}

		Filepath source = chosen.get(0);
		executor.execute(() -> copySound(source));
	}

	private void testSound()
	{
		executor.execute(() ->
		{
			if (!play())
			{
				showError("No sound is loaded, or it could not be played. "
					+ "Choose a standard WAV file and try again.");
			}
		});
	}

	private void copySound(Filepath source)
	{
		try
		{
			Filepath dir = getPluginDirectory();
			dir.createDirectories();
			Filepath target = dir.joinSegment(SOUND_FILE_NAME);

			try (InputStream in = source.openInputStream();
				OutputStream out = target.openOutputStream())
			{
				in.transferTo(out);
			}
		}
		catch (IOException e)
		{
			log.warn("Could not save the sound file", e);
			showError("That file could not be saved.");
			return;
		}

		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.setHasSound(true);
			}
		});

		if (!play())
		{
			showError("The file was saved but could not be played. "
				+ "Try a standard WAV file.");
		}
	}

	private void refreshStatus()
	{
		boolean hasSound;
		try
		{
			hasSound = getPluginDirectory().joinSegment(SOUND_FILE_NAME).isFile();
		}
		catch (IOException e)
		{
			hasSound = false;
		}

		boolean found = hasSound;
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.setHasSound(found);
			}
		});
	}

	private boolean play()
	{
		try
		{
			Filepath file = getPluginDirectory().joinSegment(SOUND_FILE_NAME);
			if (!file.isFile())
			{
				return false;
			}

			float volume = config.volume() / 100f;
			if (volume <= 0f)
			{
				return true;
			}

			float gain = (float) (20.0 * Math.log10(volume));
			audioPlayer.play(file, gain);
			return true;
		}
		catch (Exception e)
		{
			log.debug("Could not play the sound", e);
			return false;
		}
	}

	private void showError(String message)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (panel != null)
			{
				panel.showError(message);
			}
		});
	}

	private BufferedImage makeIcon()
	{
		BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setColor(Color.ORANGE);
		g.fillOval(2, 2, 12, 12);
		g.dispose();
		return img;
	}

	@Provides
	ClanSoundConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(ClanSoundConfig.class);
	}
}
