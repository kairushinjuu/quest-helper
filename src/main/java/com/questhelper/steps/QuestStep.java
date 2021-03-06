/*
 * Copyright (c) 2019, Trevor <https://github.com/Trevor159>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.questhelper.steps;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Module;
import com.questhelper.QuestVarbits;
import com.questhelper.steps.conditional.ConditionForStep;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.api.VarClientInt;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import com.questhelper.questhelpers.QuestHelper;
import static com.questhelper.QuestHelperOverlay.TITLED_CONTENT_COLOR;
import com.questhelper.QuestHelperPlugin;
import com.questhelper.steps.choice.DialogChoiceStep;
import com.questhelper.steps.choice.DialogChoiceSteps;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public abstract class QuestStep implements Module
{
	@Inject
	protected Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	SpriteManager spriteManager;

	@Setter
	@Getter
	protected String text;

	/* Locking applies to ConditionalSteps. Intended to be used as a method of forcing a step to run if it's been locked */
	@Getter
	private boolean locked;

	@Getter
	@Setter
	private boolean isLockable;

	@Getter
	private boolean unlockable = true;

	@Getter
	@Setter
	private ConditionForStep lockingCondition;

	private int currentCutsceneStatus = 0;
	protected boolean inCutscene;

	protected int iconItemID = -1;
	protected BufferedImage itemIcon;

	@Getter
	protected final QuestHelper questHelper;

	@Getter
	protected DialogChoiceSteps choices = new DialogChoiceSteps();

	@Getter
	private final ArrayList<QuestStep> substeps = new ArrayList<>();

	@Getter
	@Setter
	private boolean showInSidebar = true;

	public QuestStep(QuestHelper questHelper, String text)
	{
		this.text = text;
		this.questHelper = questHelper;
	}

	@Override
	public void configure(Binder binder)
	{
	}

	public void startUp()
	{
	}

	public void shutDown()
	{
	}

	public void addSubSteps(QuestStep... substep) {
		this.substeps.addAll(Arrays.asList(substep));
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int newCutsceneStatus = client.getVarbitValue(QuestVarbits.CUTSCENE.getId());
		if (currentCutsceneStatus == 0 && newCutsceneStatus == 1) {
			enteredCutscene();
		} else if (currentCutsceneStatus == 1 && newCutsceneStatus == 0){
			leftCutscene();
		}
		currentCutsceneStatus = newCutsceneStatus;
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if(event.getGroupId() == 219)
		{
			clientThread.invokeLater(this::highlightChoice);
		}
	}

	public void enteredCutscene()
	{
		inCutscene = true;
	}

	public void leftCutscene()
	{
		inCutscene = false;
	}

	public void highlightChoice() {
		choices.checkChoices(client);
	}

	public void addDialogStep(String choice)
	{
		choices.addChoice(new DialogChoiceStep(choice));
	}

	public void addDialogSteps(String... newChoices)
	{
		for (String choice : newChoices)
		{
			choices.addChoice(new DialogChoiceStep(choice));
		}
	}

	public void makeOverlayHint(PanelComponent panelComponent, QuestHelperPlugin plugin)
	{
		panelComponent.getChildren().add(LineComponent.builder()
			.left(questHelper.getQuest().getName())
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left(text)
			.leftColor(TITLED_CONTENT_COLOR)
			.build());
	}

	public void addIcon(int iconItemID)
	{
		this.iconItemID = iconItemID;
	}

	public void makeWorldOverlayHint(Graphics2D graphics, QuestHelperPlugin plugin)
	{
	}

	public void makeWidgetOverlayHint(Graphics2D graphics, QuestHelperPlugin plugin){
	}

	public void setLockedManually(boolean isLocked)
	{
		locked = isLocked;
	}

	public boolean isLocked()
	{
		boolean autoLocked = lockingCondition != null && lockingCondition.checkCondition(client);
		unlockable = !autoLocked;
		if (autoLocked)
		{
			locked = true;
		}
		return locked;
	}

	public QuestStep getActiveStep() {
		return this;
	}

	public BufferedImage getQuestImage()
	{
		return spriteManager.getSprite(SpriteID.TAB_QUESTS, 0);
	}
}