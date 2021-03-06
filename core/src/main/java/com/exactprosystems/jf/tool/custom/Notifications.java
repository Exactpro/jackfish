/*******************************************************************************
 * Copyright 2009-2018 Exactpro (Exactpro Systems Limited)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.exactprosystems.jf.tool.custom;

import com.exactprosystems.jf.functions.Notifier;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.CssVariables;

import com.exactprosystems.jf.tool.helpers.DialogsHelper;
import javafx.animation.*;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Notifications
{
	private static final Pos POSITION = Pos.BOTTOM_RIGHT;

	private Duration hideAfterDuration = Duration.seconds(5);
	private Notifier state;
	private String title;
	private String msg;

	private EventHandler<ActionEvent> onAction;

	private Notifications()
	{
	}

	public static Notifications create()
	{
		return new Notifications();
	}

	public Notifications msg(String msg)
	{
		this.msg = msg;
		return this;
	}

	public Notifications title(String title)
	{
		this.title = title;
		return this;
	}

	public Notifications hideAfter(Duration duration)
	{
		this.hideAfterDuration = duration;
		return this;
	}

	public Notifications onAction(EventHandler<ActionEvent> onAction)
	{
		this.onAction = onAction;
		return this;
	}

	public Notifications state(Notifier state)
	{
		this.state = state;
		return this;
	}

	public void show()
	{
		if (Common.node == null || Common.node.getScene() == null)
		{
			System.err.println(this.msg);
			return;
		}
		
		NotificationPopupHandler.getInstance().show(Common.node.getScene().getWindow(), this);
	}

	private static final class NotificationPopupHandler
	{
		private static final Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

		private double startX = 0;
		private double startY = 0;

		private static NotificationPopupHandler INSTANCE;

		static synchronized NotificationPopupHandler getInstance()
		{
			if (INSTANCE == null)
			{
				INSTANCE = new NotificationPopupHandler();
			}

			return INSTANCE;
		}

		private final Map<Pos, List<Popup>> popupsMap = new HashMap<>();
		private static final double padding = 10;

		private ParallelTransition parallelTransition = new ParallelTransition(new FillTransition());

		private boolean isShowing = false;

		private void show(Window owner, final Notifications notification)
		{
			Rectangle currentBounds = DialogsHelper.getCurrentScreenBounds();
			double screenWidth = currentBounds.getWidth() + currentBounds.getX();
			double screenHeight = currentBounds.getHeight() + currentBounds.getY();
			final Popup popup = new Popup();
			popup.setAutoFix(false);

			final NotificationBar notificationBar = new NotificationBar(notification.msg, notification.title, notification.state)
			{
				@Override
				public boolean isShowing()
				{
					return isShowing;
				}

				@Override
				protected double computeMinWidth(double height)
				{
					return 400;
				}

				@Override
				public void hide()
				{
					isShowing = false;
					createHideTimeline(popup, this, Duration.ZERO).play();
				}

				@Override
				public double getContainerHeight()
				{
					return startY + screenHeight;
				}

				@Override
				public void relocateInParent(double x, double y)
				{
					// this allows for us to slide the notification upwards
					popup.setAnchorY(y - padding);
				}
			};

			notificationBar.setOnMouseClicked(e -> {
				if (notification.onAction != null)
				{
					ActionEvent actionEvent = new ActionEvent(notificationBar, notificationBar);
					notification.onAction.handle(actionEvent);

					// animate out the popup
					createHideTimeline(popup, notificationBar, Duration.ZERO).play();
				}
			});

			popup.getContent().add(notificationBar);
			popup.show(owner, 0, 0);

			// determine location for the popup
			final double barWidth = notificationBar.getWidth();
			final double barHeight = notificationBar.getHeight();
			double anchorX = startX + screenWidth - barWidth - padding;
			double anchorY = startY + screenHeight - barHeight - padding;

			popup.setAnchorX(anchorX);
			popup.setAnchorY(anchorY);

			isShowing = true;
			notificationBar.doShow();
			addPopupToMap(popup);
			Timeline timeline = createHideTimeline(popup, notificationBar, notification.hideAfterDuration);
			notificationBar.setTimeline(timeline);
			timeline.play();
		}

		private void hide(Popup popup, Pos p)
		{
			popup.hide();
			removePopupFromMap(p, popup);
		}

		private Timeline createHideTimeline(final Popup popup, NotificationBar bar, Duration startDelay)
		{
			KeyValue fadeOutBegin = new KeyValue(bar.opacityProperty(), 1.0);
			KeyValue fadeOutEnd = new KeyValue(bar.opacityProperty(), 0.0);

			KeyFrame kfBegin = new KeyFrame(Duration.ZERO, fadeOutBegin);
			KeyFrame kfEnd = new KeyFrame(Duration.millis(500), fadeOutEnd);

			Timeline timeline = new Timeline(kfBegin, kfEnd);
			timeline.setDelay(startDelay);
			timeline.setOnFinished(e -> hide(popup, POSITION));

			return timeline;
		}

		private void addPopupToMap(Popup popup)
		{
			List<Popup> popups;
			if (!popupsMap.containsKey(POSITION))
			{
				popups = new LinkedList<>();
				popupsMap.put(POSITION, popups);
			}
			else
			{
				popups = popupsMap.get(POSITION);
			}

			doAnimation(popup);

			// add the popup to the list so it is kept in memory and can be
			// accessed later on
			popups.add(popup);
		}

		private void removePopupFromMap(Pos p, Popup popup)
		{
			if (popupsMap.containsKey(p))
			{
				List<Popup> popups = popupsMap.get(p);
				popups.remove(popup);
			}
		}

		private void doAnimation(Popup changedPopup)
		{
			List<Popup> popups = popupsMap.get(POSITION);
			if (popups == null)
			{
				return;
			}

			parallelTransition.stop();
			parallelTransition.getChildren().clear();

			// animate all other popups in the list upwards so that the new one
			// is in the 'new' area.
			// firstly, we need to determine the target positions for all popups
			double sum = 0;
			double targetAnchors[] = new double[popups.size()];
			for (int i = popups.size() - 1; i >= 0; i--)
			{
				Popup _popup = popups.get(i);

				final double popupHeight = _popup.getContent().get(0).getBoundsInParent().getHeight();

				if (i == popups.size() - 1)
				{
					sum = changedPopup.getAnchorY() - popupHeight - padding;
				}
				else
				{
					sum -= popupHeight + padding;
				}

				targetAnchors[i] = sum;
			}

			// then we set up animations for each popup to animate towards the
			// target
			for (int i = popups.size() - 1; i >= 0; i--)
			{
				final Popup _popup = popups.get(i);
				final double anchorYTarget = targetAnchors[i];
				if (anchorYTarget < 0)
				{
					_popup.hide();
				}
				final double oldAnchorY = _popup.getAnchorY();
				final double distance = anchorYTarget - oldAnchorY;

				Transition t = new Transition()
				{
					{
						setCycleDuration(Duration.millis(350));
					}

					@Override
					protected void interpolate(double frac)
					{
						double newAnchorY = oldAnchorY + distance * frac;
						_popup.setAnchorY(newAnchorY);
					}
				};
				t.setCycleCount(1);
				parallelTransition.getChildren().add(t);
			}
			parallelTransition.play();
		}
	}

	private abstract static class NotificationBar extends Region
	{
		private static final double MIN_HEIGHT = 40;
		private Timeline closeTimeLine;

		Label label;
		Label title;
		Button closeBtn;
		Button copyBtn;
		CheckBox pinBtn;

		private GridPane pane;

		private DoubleProperty transition = new SimpleDoubleProperty()
		{
			@Override
			protected void invalidated()
			{
				requestContainerLayout();
			}
		};


		public void requestContainerLayout()
		{
			layoutChildren();
		}

		public abstract void hide();

		public abstract boolean isShowing();

		public abstract double getContainerHeight();

		public abstract void relocateInParent(double x, double y);

		public NotificationBar(String msg, String ttl, Notifier state)
		{
			getStyleClass().add(CssVariables.NOTIFICATION_BAR);

			setVisible(isShowing());

			this.pane = new GridPane();
			String stateClass = state.name().toLowerCase();
			this.pane.getStyleClass().addAll(CssVariables.NOTIFICATION_PANE, stateClass);
			this.pane.setAlignment(Pos.BASELINE_LEFT);
			getChildren().setAll(this.pane);
			if (!ttl.isEmpty())
			{
				this.title = new Label();
				this.title.getStyleClass().add(CssVariables.NOTIFICATION_TITLE);
				this.title.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
				GridPane.setHgrow(title, Priority.ALWAYS);

				this.title.setText(ttl);
				this.title.opacityProperty().bind(transition);
			}

			// initialise label area
			this.label = new Label();
			this.label.setWrapText(true);
			this.label.setText(msg);
			this.label.getStyleClass().addAll(CssVariables.NOTIFICATION_MSG, stateClass);
			int length = msg.split(System.lineSeparator()).length + 2;
			length = Math.min(length, 10);
            this.label.setPrefSize(400, length * 20);
			this.label.setMinSize(400, length * 20);
			this.label.setMaxSize(400, Screen.getPrimary().getVisualBounds().getHeight() / 4);
			this.label.setWrapText(true);
			GridPane.setVgrow(this.label, Priority.ALWAYS);
			GridPane.setHgrow(this.label, Priority.ALWAYS);
			GridPane.setColumnSpan(this.label, 4);
			this.label.opacityProperty().bind(transition);

			// initialise close button area
			this.closeBtn = new Button();
			this.closeBtn.setOnAction(arg0 -> hide());
			this.closeBtn.getStyleClass().setAll(CssVariables.NOTIFICATION_CLOSE_BUTTON);
			StackPane graphic = new StackPane();
			graphic.getStyleClass().setAll("graphic");
			this.closeBtn.setGraphic(graphic);
			this.closeBtn.setMinSize(16, 16);
			this.closeBtn.setPrefSize(16, 16);
			this.closeBtn.setFocusTraversable(false);
			this.closeBtn.opacityProperty().bind(this.transition);

			this.copyBtn = new Button("C");
			this.copyBtn.setOnAction(e -> Common.copyText(this.label.getText()));
			this.copyBtn.getStyleClass().setAll(CssVariables.NOTIFICATION_COPY_BUTTON);
			this.copyBtn.setMinSize(16, 16);
			this.copyBtn.setPrefSize(16, 16);
			this.copyBtn.setMaxSize(16, 16);
			this.copyBtn.setFocusTraversable(false);
			this.copyBtn.opacityProperty().bind(this.transition);

			this.pinBtn = new CheckBox();
			this.pinBtn.getStyleClass().clear();
			this.pinBtn.setGraphic(new ImageView(new Image(CssVariables.Icons.NOTIFICATION_PIN_FALSE)));
			this.pinBtn.getStyleClass().addAll(CssVariables.TRANSPARENT_BACKGROUND);
			this.pinBtn.selectedProperty().addListener((observable, oldValue, newValue) -> {
				this.pinBtn.setGraphic(new ImageView(new Image(newValue ? CssVariables.Icons.NOTIFICATION_PIN_TRUE : CssVariables.Icons.NOTIFICATION_PIN_FALSE)));
				if (newValue)
				{
					this.closeTimeLine.pause();
				}
				else
				{
					this.closeTimeLine.playFrom(Duration.ZERO);
				}
			});
			this.pinBtn.setMinSize(16, 16);
			this.pinBtn.setPrefSize(16, 16);

			GridPane.setMargin(this.closeBtn, new Insets(0, 0, 0, 8));
			GridPane.setValignment(this.closeBtn, VPos.TOP);
			updatePane();
		}

		public void setTimeline(Timeline timeline)
		{
			this.closeTimeLine = timeline;
		}

		void updatePane()
		{
			this.pane.getChildren().clear();
			this.pane.add(this.title, 0, 0);
			this.pane.add(this.label, 0, 1, 3, 1);
			this.pane.add(this.copyBtn, 1,0);
			this.pane.add(this.pinBtn, 2, 0);
			this.pane.add(this.closeBtn, 3, 0);
		}

		@Override
		protected void layoutChildren()
		{
			final double w = getWidth();
			final double notificationBarHeight = prefHeight(w);
			pane.resize(w, notificationBarHeight);
			relocateInParent(0, getContainerHeight() - notificationBarHeight);
		}

		@Override
		protected double computeMinHeight(double width)
		{
			return Math.max(super.computePrefHeight(width), MIN_HEIGHT);
		}

		@Override
		protected double computePrefHeight(double width)
		{
			return Math.max(pane.prefHeight(width), minHeight(width)) * transition.get();
		}

		public void doShow()
		{
			transitionStartValue = 0;
			doAnimationTransition();
		}

		public void doHide()
		{
			transitionStartValue = 1;
			doAnimationTransition();
		}


		// --- animation timeline code
		private static final Duration TRANSITION_DURATION = new Duration(350.0);
		private Timeline timeline;
		private double transitionStartValue;

		private void doAnimationTransition()
		{
			Duration duration;

			if (timeline != null && (timeline.getStatus() != Animation.Status.STOPPED))
			{
				duration = timeline.getCurrentTime();

				// fix for #70 - the notification pane freezes up as it has zero
				// duration to expand / contract
				duration = duration == Duration.ZERO ? TRANSITION_DURATION : duration;
				transitionStartValue = transition.get();
				// --- end of fix

				timeline.stop();
			}
			else
			{
				duration = TRANSITION_DURATION;
			}

			timeline = new Timeline();
			timeline.setCycleCount(1);

			KeyFrame startKeyFrame;
			KeyFrame endKeyFrame;

			if (isShowing())
			{
				startKeyFrame = new KeyFrame(Duration.ZERO, event -> {
					// start expand
					setCache(true);
					setVisible(true);

				}, new KeyValue(transition, transitionStartValue));

				endKeyFrame = new KeyFrame(duration, event -> pane.setCache(false), new KeyValue(transition, 1, Interpolator.EASE_OUT));
			}
			else
			{
				startKeyFrame = new KeyFrame(Duration.ZERO, event -> pane.setCache(true), new KeyValue(this.opacityProperty(), 1.0));

				endKeyFrame = new KeyFrame(duration, event -> {
					setCache(false);
					setVisible(false);
				}, new KeyValue(transition, 0.0, Interpolator.EASE_IN));
			}

			timeline.getKeyFrames().setAll(startKeyFrame, endKeyFrame);
			timeline.play();
		}
	}

}

