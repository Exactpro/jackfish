package com.exactprosystems.jf.tool.custom;

import com.exactprosystems.jf.tool.Common;
import com.exactprosystems.jf.tool.CssVariables;
import com.exactprosystems.jf.tool.custom.layout.CustomRectangle;
import com.exactprosystems.jf.tool.custom.layout.LayoutExpressionBuilderController;
import com.exactprosystems.jf.tool.custom.scale.IScaleListener;
import com.exactprosystems.jf.tool.custom.scale.ScalePane;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class ImageViewWithScale implements IScaleListener
{
	private final BorderPane mainPane;
	private final ScrollPane scrollPane;
	private final AnchorPane anchorPane;
	private final ScalePane scalePane;

	private final HBox hBox;
	private final ToggleButton btnInspect;
	private final Label lblInspect;

	private final Group group;

	private ImageView imageView;
	private CustomRectangle rectangle;
	private boolean needInspect = false;
	private CustomRectangle inspectRectangle;

	private Node waitingNode;
	private double scale = 1.0;

	private Dimension initial;
	private BufferedImage image;

	private Map<Rectangle, Set<Rectangle>> rectanglesMap = new HashMap<>();

	private Consumer<Rectangle> clickConsumer;

	public ImageViewWithScale()
	{
		this.mainPane = new BorderPane();
		this.scrollPane = new ScrollPane();
		this.anchorPane = new AnchorPane();
		this.scalePane = new ScalePane();
		this.hBox = new HBox();
		this.group = new Group();
		this.btnInspect = new ToggleButton();
		this.lblInspect = new Label();

		this.scrollPane.setFitToHeight(true);
		this.scrollPane.setFitToWidth(true);

		this.mainPane.setCenter(this.scrollPane);

		this.scrollPane.setContent(this.anchorPane);
		BorderPane.setAlignment(this.anchorPane, Pos.CENTER);

		this.anchorPane.getChildren().add(this.group);
		AnchorPane.setTopAnchor(this.group, 0.0);
		AnchorPane.setLeftAnchor(this.group, 0.0);
		AnchorPane.setRightAnchor(this.group, 0.0);
		AnchorPane.setBottomAnchor(this.group, 0.0);

		this.mainPane.setTop(this.hBox);
		this.hBox.setAlignment(Pos.CENTER_LEFT);

		this.btnInspect.setOpacity(0.5);
		this.hBox.getChildren().addAll(this.scalePane
				, Common.createSpacer(Common.SpacerEnum.HorizontalMid)
				, this.btnInspect
				, Common.createSpacer(Common.SpacerEnum.HorizontalMid)
				, this.lblInspect
		);

		addWaitingPane();

		this.inspectRectangle = new CustomRectangle();
		this.inspectRectangle.setWidthLine(LayoutExpressionBuilderController.BORDER_WIDTH);
		this.inspectRectangle.addStyleClass(CssVariables.XPATH_INSPECT_RECTNAGLE);
		this.inspectRectangle.setVisible(false);

		this.scalePane.setListener(this);

		Common.customizeLabeled(this.btnInspect, CssVariables.TRANSPARENT_BACKGROUND, CssVariables.Icons.INSPECT_ICON);

		listeners();
	}

	//region public methods
	public BorderPane getContent()
	{
		return this.mainPane;
	}

	public void displayImage(BufferedImage image)
	{
		Platform.runLater(() -> {
			this.image = image;
			this.hBox.getChildren().forEach(node ->  node.setDisable(false));
			this.anchorPane.getChildren().remove(this.waitingNode);

			this.initial = new Dimension(image.getWidth(), image.getHeight());
			this.scrollPane.setMaxHeight(Region.USE_COMPUTED_SIZE);
			this.scrollPane.setMaxWidth(Region.USE_COMPUTED_SIZE);
			Common.tryCatch(() -> createCanvas(image), "Error on create canvas");
		});
	}

	public BufferedImage getImage()
	{
		return this.image;
	}

	public void replaceWaitingPane(Node node)
	{
		AnchorPane.setTopAnchor(node, 50.0);
		AnchorPane.setLeftAnchor(node, 50.0);

		this.anchorPane.getChildren().remove(this.waitingNode);
		this.waitingNode = node;
		this.anchorPane.getChildren().add(this.waitingNode);
	}

	public void setListRectangles(Map<Rectangle, Set<Rectangle>> rectanglesMap)
	{
		this.rectanglesMap = rectanglesMap;
	}

	public void setClickConsumer(Consumer<Rectangle> consumer)
	{
		this.clickConsumer = consumer;
	}

	public void displayRectangle(Rectangle rectangle)
	{
		if (rectangle == null || isRectEmpty(rectangle))
		{
			hideRectangle();
		}
		else if (this.rectangle != null)
		{
			this.rectangle.updateRectangle(rectangle, this.scale);
			this.rectangle.setVisible(true);
		}
	}
	//endregion

	@Override
	public void changeScale(double scale)
	{
		this.scale = scale;
		this.imageView.setFitHeight(this.scale * this.initial.height);
		this.imageView.setFitWidth(this.scale * this.initial.width);
		hideRectangle();
		hideInspectRectangle();
	}

	//region private methods
	private void addWaitingPane()
	{
		this.waitingNode = new BorderPane();
		((BorderPane)this.waitingNode).setCenter(new ProgressIndicator(ProgressIndicator.INDETERMINATE_PROGRESS));
		((BorderPane)this.waitingNode).setBottom(new Text("Waiting for image..."));

		AnchorPane.setLeftAnchor(this.waitingNode, 50.0);
		AnchorPane.setTopAnchor(this.waitingNode, 50.0);

		this.anchorPane.getChildren().add(this.waitingNode);
		this.hBox.getChildren().forEach(node ->  node.setDisable(true));
	}

	private void createCanvas(BufferedImage bufferedImage) throws IOException
	{
		this.imageView = new ImageView();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ImageIO.write(bufferedImage, "jpg", outputStream);
		javafx.scene.image.Image image = new javafx.scene.image.Image(new ByteArrayInputStream(outputStream.toByteArray()));
		this.imageView.setImage(image);
		this.group.getChildren().add(imageView);
		this.imageView.setPreserveRatio(true);
		this.rectangle = new CustomRectangle();
		this.rectangle.addStyleClass(CssVariables.XPATH_RECTANGLE);
		this.rectangle.setGroup(this.group);
		this.rectangle.setWidthLine(LayoutExpressionBuilderController.BORDER_WIDTH);
		this.rectangle.setVisible(false);
		this.inspectRectangle.setGroup(this.group);
	}

	private void hideRectangle()
	{
		if (this.rectangle != null)
		{
			this.rectangle.setVisible(false);
		}
	}

	private void hideInspectRectangle()
	{
		this.inspectRectangle.setVisible(false);
	}

	private void listeners()
	{
		this.group.setOnMouseMoved(event -> {
			this.printMouseCoords(event);
			if (needInspect)
			{
				moveOnImage(event.getX(), event.getY());
			}
		});

		this.group.setOnMouseClicked(event -> {
			if (needInspect)
			{
				clickOnImage(event.getX(), event.getY());
			}
		});

		this.btnInspect.selectedProperty().addListener((observable, oldValue, newValue) -> {
			this.btnInspect.setOpacity(newValue ? 1.0 : 0.5);
			this.needInspect = newValue;
			if (!newValue)
			{
				hideInspectRectangle();
			}
		});
	}

	private void clickOnImage(double x, double y)
	{
		Rectangle rectangle = findRectangle(x, y);
		if (rectangle != null)
		{
			Optional.ofNullable(this.clickConsumer).ifPresent(c -> c.accept(rectangle));
		}
		this.btnInspect.setSelected(false);
	}

	private void moveOnImage(double x, double y)
	{
		Rectangle rectangle = findRectangle(x, y);
		if (rectangle != null)
		{
			displayInspectRectangle(rectangle);
		}
	}

	private void displayInspectRectangle(Rectangle rectangle)
	{
		this.inspectRectangle.updateRectangle(rectangle, this.scale);
		this.inspectRectangle.setVisible(true);
	}

	private Rectangle findRectangle(double x, double y)
	{
		int intX = (int) (x / this.scale);
		int intY = (int) (y / this.scale);

		int sizeX = this.initial.getSize().width / 16;
		int sizeY = this.initial.getSize().height / 16;
		Rectangle key = new Rectangle((intX / sizeX) * sizeX, (intY / sizeY) * sizeY, sizeX, sizeY);
		Set<Rectangle> set = this.rectanglesMap.get(key);
		if (set == null)
		{
			return null;
		}
		Point mousePoint = new Point(intX, intY);

		Optional<Rectangle> inspected = set.stream()
				.filter(item -> item.contains(mousePoint))
				.sorted(Comparator.comparingDouble(ImageViewWithScale::square))
				.findFirst();

		return inspected.isPresent() ? inspected.get() : null;
	}

	private static double square(Rectangle rec)
	{
		if (rec == null)
		{
			return 0.0;
		}
		return rec.width * rec.height;
	}

	private void printMouseCoords(MouseEvent event)
	{
		Point point = getMouseCoords(event);
		this.lblInspect.setText("X=" + point.x + " Y=" + point.y);
	}

	private Point getMouseCoords(MouseEvent event)
	{
		int x, y;
		Rectangle rect = this.rectangle.getRectangle();
		if(this.rectangle.isVisible())
		{
			Point mouseRelativeCoords = getMouseRelativeCoords(event.getX(), event.getY(), rect);
			x = mouseRelativeCoords.x;
			y = mouseRelativeCoords.y;
		}
		else
		{
			x = (int) event.getX();
			y = (int) event.getY();
		}
		return new Point(x,y);
	}

	private Point getMouseRelativeCoords(double absoluteX, double absoluteY, Rectangle rect)
	{
		int x = (int)absoluteX - rect.x;
		int y = (int)absoluteY - rect.y;
		return new Point(x, y);
	}

	private boolean isRectEmpty(Rectangle rect)
	{
		return rect.height <= 0 && rect.width <= 0;
	}
	//endregion

}
