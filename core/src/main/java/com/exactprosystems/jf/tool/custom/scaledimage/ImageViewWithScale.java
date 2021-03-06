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

package com.exactprosystems.jf.tool.custom.scaledimage;

import com.exactprosystems.jf.api.common.i18n.R;
import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.CssVariables;
import com.exactprosystems.jf.tool.custom.controls.rect.DecoratedRectangle;
import com.exactprosystems.jf.tool.wizard.related.MarkerStyle;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class ImageViewWithScale extends BorderPane
{
	private static final double WIDHT_COORDS = 120;

	//region fields
	private final ScrollPane scrollPane;
	private final AnchorPane anchorPane;
	private final ScalePane  scalePane;

	private final HBox         hBox;
	private final ToggleButton btnInspect;
	private final Label        lblInspect;
	private final Label        lblColor;
	private final CheckBox     cbIds;

	private final javafx.scene.shape.Rectangle rectangleColor;
	private final Group                        group;
	private final CheckBox                     cbCustomGrid;
	private final CustomGrid                   customGrid;
	private       ImageView                    imageView;

	private Node          waitingNode;
	private Dimension     initial;
	private BufferedImage image;
	private Map<Rectangle, Set<Rectangle>> searchingMap = new HashMap<>();
	private Consumer<Rectangle> onRectangleClick;

	//endregion

	public ImageViewWithScale()
	{
		super();
		this.scrollPane = new ScrollPane();
		this.anchorPane = new AnchorPane();
		this.scalePane = new ScalePane();
		this.hBox = new HBox();
		this.group = new Group();
		this.btnInspect = new ToggleButton();
		this.lblInspect = new Label("X = 0 Y = 0");
		this.lblInspect.setPrefWidth(WIDHT_COORDS);
		this.lblInspect.setMaxWidth(WIDHT_COORDS);
		this.lblInspect.setMinWidth(WIDHT_COORDS);

		this.lblColor = new Label(Color.BLACK.toString());
		this.rectangleColor = new javafx.scene.shape.Rectangle();
		this.rectangleColor.setWidth(12.0);
		this.rectangleColor.setHeight(12.0);
		this.cbIds = new CheckBox("Id's");

		this.cbCustomGrid = new CheckBox("Grid");
		this.cbCustomGrid.setDisable(true);

		this.customGrid = new CustomGrid();
		this.customGrid.setGroup(this.group);

		Label lblColorName = new Label(R.IMAGE_VIEW_PIXEL_COLOR.get());

		this.scrollPane.setFitToHeight(true);
		this.scrollPane.setFitToWidth(true);

		this.setCenter(this.scrollPane);

		this.scrollPane.setContent(this.anchorPane);
		BorderPane.setAlignment(this.anchorPane, Pos.CENTER);

		this.anchorPane.getChildren().add(this.group);
		AnchorPane.setTopAnchor(this.group, 0.0);
		AnchorPane.setLeftAnchor(this.group, 0.0);
		AnchorPane.setRightAnchor(this.group, 0.0);
		AnchorPane.setBottomAnchor(this.group, 0.0);

		this.setTop(this.hBox);
		this.hBox.setAlignment(Pos.CENTER_LEFT);

		this.btnInspect.setId(CssVariables.BUTTON_INSPECT);
		this.btnInspect.getStyleClass().addAll(CssVariables.TRANSPARENT_BACKGROUND, CssVariables.TOGGLE_BUTTON_WITHOUT_BORDER);

		this.hBox.getChildren().addAll(this.scalePane
				, Common.createSpacer(Common.SpacerEnum.HorizontalMid)
				, this.btnInspect
				, Common.createSpacer(Common.SpacerEnum.HorizontalMid)
				, this.cbIds
				, Common.createSpacer(Common.SpacerEnum.HorizontalMid)
				, this.lblInspect
				, Common.createSpacer(Common.SpacerEnum.HorizontalMid)
				, lblColorName
				, this.rectangleColor
				, this.lblColor
		);

		addWaitingPane();
		this.scalePane.setOnScaleChanged(s -> {
			this.group.setScaleX(s);
			this.group.setScaleY(s);

			Common.runLater(() -> {
				this.group.setTranslateX(0);
				this.group.setTranslateY(0);
				this.group.setTranslateX(this.group.getBoundsInParent().getMinX() * -1);
				this.group.setTranslateY(this.group.getBoundsInParent().getMinY() * -1);

				this.anchorPane.setPrefSize(this.initial.width * s, this.initial.height * s);
				this.anchorPane.setMinSize(this.initial.width * s, this.initial.height * s);
				this.anchorPane.setMaxSize(this.initial.width * s, this.initial.height * s);
			});
			hideAllRectangles(null);
		});
		listeners();
	}

	//region public methods
	public void showRectangle(java.awt.Rectangle rectangle, MarkerStyle style, String text, boolean selected)
	{
		DecoratedRectangle newRectangle = new DecoratedRectangle(rectangle, style, text);
		newRectangle.setTextVisible(this.cbIds.isSelected());
		newRectangle.setOpacity(selected ? 1.0 : 0.5);
		this.group.getChildren().add(newRectangle);
	}

	public void hideRectangle(java.awt.Rectangle rectangle, MarkerStyle style)
	{
		this.group.getChildren().removeIf(d -> (d instanceof DecoratedRectangle) && (((DecoratedRectangle) d).matches(rectangle, style)));
	}

	public void hideRectangle(java.awt.Rectangle rectangle, Color color)
	{
		this.group.getChildren().removeIf(d -> (d instanceof DecoratedRectangle) && (((DecoratedRectangle) d).matches(rectangle, color)));
	}

	public void visibleRectangle(MarkerStyle style, boolean visible)
	{
		if (style.color() != null)
		{
			this.group.getChildren().stream().filter(d -> (d instanceof DecoratedRectangle) && (((DecoratedRectangle) d).getMarkerStyle() == style)).forEach(d -> d.setVisible(visible));
		}
	}

	public void hideAllRectangles(MarkerStyle style)
	{
		if (style == null)
		{
			this.group.getChildren().removeIf(d -> (d instanceof DecoratedRectangle));
		}
		else
		{
			this.group.getChildren().removeIf(d -> (d instanceof DecoratedRectangle) && (((DecoratedRectangle) d).getMarkerStyle() == style));
		}
	}

	public void setTextVisible(boolean value)
	{
		this.group.getChildren().filtered(d -> d instanceof DecoratedRectangle).forEach(d -> ((DecoratedRectangle) d).setTextVisible(value));
	}

	public void setListForSearch(List<Rectangle> list)
	{
		if (this.image != null)
		{
			int width = image.getWidth();
			int height = image.getHeight();
			buildMap(width, height, new Dimension(width / 16, height / 16), list);
		}
	}

	public void displayImage(BufferedImage image)
	{
		this.image = image;
		Common.runLater(() -> {
			this.hBox.getChildren().forEach(node -> node.setDisable(false));
			this.anchorPane.getChildren().remove(this.waitingNode);

			this.initial = new Dimension(image.getWidth(), image.getHeight());
			this.scrollPane.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
			this.scrollPane.setMaxSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
			this.cbCustomGrid.setDisable(false);
			this.customGrid.setSize(this.image.getWidth(), this.image.getHeight());
			createCanvas(image);
		});
	}

	public void setOnRectangleClick(Consumer<Rectangle> onClick)
	{
		this.onRectangleClick = onClick;
	}

	public void addShowGrid()
	{
		this.hBox.getChildren().addAll(Common.createSpacer(Common.SpacerEnum.HorizontalMid), this.cbCustomGrid);
	}
	//endregion

	//region private methods
	private void addWaitingPane()
	{
		this.waitingNode = new BorderPane();
		((BorderPane) this.waitingNode).setCenter(new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS));
		((BorderPane) this.waitingNode).setBottom(new Text(R.IMAGE_VIEW_WAITING.get()));

		AnchorPane.setLeftAnchor(this.waitingNode, 50.0);
		AnchorPane.setTopAnchor(this.waitingNode, 50.0);

		this.anchorPane.getChildren().add(this.waitingNode);
		this.hBox.getChildren().forEach(node -> node.setDisable(true));
	}

	private void createCanvas(BufferedImage bufferedImage)
	{
		this.imageView = new ImageView();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try
		{
			ImageIO.write(bufferedImage, "jpg", outputStream);
		}
		catch (IOException ignored)
		{
			;//never raise
		}
		javafx.scene.image.Image image = new javafx.scene.image.Image(new ByteArrayInputStream(outputStream.toByteArray()));
		this.imageView.setImage(image);
		this.group.getChildren().add(imageView);
		this.imageView.setPreserveRatio(true);
	}

	public void removeCurrentImage()
	{
		if (this.group.getChildren().remove(this.imageView))
		{
			addWaitingPane();
		}

	}

	private void listeners()
	{
		this.cbCustomGrid.selectedProperty().addListener((observable, oldValue, newValue) -> {
			Runnable show = newValue ? this.customGrid::show : this.customGrid::hide;
			show.run();
		});
		this.cbIds.selectedProperty().addListener((observable, oldValue, newValue) -> setTextVisible(newValue));

		this.group.setOnMouseMoved(event -> {
			Point point = getMouseCoords(event);
			printMouseCoords(point);
			printPixelColor(point);

			if (this.btnInspect.selectedProperty().get())
			{
				double x = event.getX();
				double y = event.getY();
				Rectangle rectangle = findRectangle(x, y);
				if (rectangle != null)
				{
					hideAllRectangles(MarkerStyle.INSPECT);
					showRectangle(rectangle, MarkerStyle.INSPECT, null, true);
				}
			}
		});

		this.group.setOnMouseClicked(event -> {
			if (this.btnInspect.selectedProperty().get())
			{
				double x = event.getX();
				double y = event.getY();
				Rectangle rectangle = findRectangle(x, y);
				if (rectangle != null && this.onRectangleClick != null)
				{
					this.onRectangleClick.accept(rectangle);
				}
				this.btnInspect.setSelected(false);
			}
		});

		this.btnInspect.selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue)
			{
				hideAllRectangles(MarkerStyle.INSPECT);
			}
		});
	}

	private Rectangle findRectangle(double x, double y)
	{
		if (this.initial == null)
		{
			return null;
		}

		int intX = (int) x;
		int intY = (int) y;

		int sizeX = this.initial.getSize().width / 16;
		int sizeY = this.initial.getSize().height / 16;
		Rectangle key = new Rectangle((intX / sizeX) * sizeX, (intY / sizeY) * sizeY, sizeX, sizeY);
		Set<Rectangle> set = this.searchingMap.get(key);
		if (set == null)
		{
			return null;
		}
		Point mousePoint = new Point(intX, intY);

		Optional<Rectangle> inspected = set.stream()
				.filter(item -> item.contains(mousePoint))
				.sorted(Comparator.comparingDouble(rec -> rec.width * rec.height))
				.findFirst();

		return inspected.orElse(null);
	}

	private javafx.scene.paint.Color getPixelColor(Point point)
	{
		if (this.image == null)
		{
			return Color.TRANSPARENT;
		}
		try
		{
			int color = this.image.getRGB(point.x, point.y);
			int red = (color & 0x00ff0000) >> 16;
			int green = (color & 0x0000ff00) >> 8;
			int blue = color & 0x000000ff;
			return Color.rgb(red, green, blue);
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			return Color.TRANSPARENT;
		}

	}

	private void printMouseCoords(Point point)
	{
		this.lblInspect.setText("X=" + point.x + " Y=" + point.y);
	}

	private void printPixelColor(Point point)
	{
		Color pixelColor = getPixelColor(point);
		this.lblColor.setText(pixelColor.toString());
		this.rectangleColor.setFill(pixelColor);
	}

	private Point getMouseCoords(MouseEvent event)
	{
		int x = (int) event.getX();
		int y = (int) event.getY();
		return new Point(x, y);
	}

	private void buildMap(int width, int height, Dimension cellSize, List<Rectangle> list)
	{
		this.searchingMap = new HashMap<>();

		int x = 0;
		while (x < width)
		{
			int y = 0;
			while (y < height)
			{
				Rectangle key = new Rectangle(new Point(x, y), cellSize);
				Set<Rectangle> set = new HashSet<>();
				list.stream().filter(r -> r.intersects(key)).forEach(set::add);
				if (!set.isEmpty())
				{
					this.searchingMap.put(key, set);
				}

				y += cellSize.height;
			}
			x += cellSize.width;
		}
	}

	//endregion
}
