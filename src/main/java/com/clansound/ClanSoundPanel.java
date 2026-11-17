package com.clansound;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.PluginPanel;

public class ClanSoundPanel extends PluginPanel
{
	private final JLabel status = new JLabel();

	public ClanSoundPanel(Runnable onChoose, Runnable onTest)
	{
		super(false);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(10, 10, 10, 10));

		JPanel content = new JPanel(new GridLayout(0, 1, 0, 8));

		JButton choose = new JButton("Choose sound file (.wav)");
		choose.addActionListener(e -> onChoose.run());

		JButton test = new JButton("Test sound");
		test.addActionListener(e -> onTest.run());

		content.add(choose);
		content.add(test);
		content.add(status);
		add(content, BorderLayout.NORTH);

		setHasSound(false);
	}

	public void setHasSound(boolean hasSound)
	{
		status.setText(hasSound ? "Sound loaded" : "No sound chosen yet");
	}

	public void showError(String message)
	{
		JOptionPane.showMessageDialog(this, message);
	}
}
