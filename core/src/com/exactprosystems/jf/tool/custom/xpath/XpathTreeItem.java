package com.exactprosystems.jf.tool.custom.xpath;

import com.exactprosystems.jf.tool.CssVariables;
import com.exactprosystems.jf.tool.dictionary.dialog.DialogWizardController;
import com.exactprosystems.jf.tool.dictionary.dialog.ElementWizardBean;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import org.w3c.dom.Node;

import java.util.*;

public class XpathTreeItem extends XpathItem
{
	public enum TreeItemState
	{
		UPDATE(CssVariables.Icons.REFRESH, Color.web("#2687fb")),
		ADD(CssVariables.Icons.ADD_16_ICON, Color.web("#2687fb")),
		MARK(CssVariables.Icons.MARK_ICON, Color.web("#2a9635")),
		QUESTION(CssVariables.Icons.QUESTION_ICON, Color.web("#f3c738"));

		private String iconPath;
		private Color color;

		TreeItemState(String iconPath, Color color)
		{
			this.iconPath = iconPath;
			this.color = color;
		}

		public String getIconPath()
		{
			return iconPath;
		}

		public Color color()
		{
			return color;
		}
	}

	private boolean markIsVisible = true;
	private Set<BeanWithMark> set = new HashSet<>();
	private TreeItemState currentState;

	public XpathTreeItem(HBox box, Node node)
	{
		super(box, node);
	}

	public void changeState()
	{
		if (currentState == null)
		{
			this.currentState = TreeItemState.ADD;
			this.addRelation(null, TreeItemState.ADD);
			this.set.stream().map(BeanWithMark::getBean).filter(Objects::nonNull).forEach(b -> b.setStyleClass(null));
		}
		else if (currentState == TreeItemState.ADD || currentState == TreeItemState.UPDATE)
		{
			this.currentState = null;
			this.set.stream().map(BeanWithMark::getBean).filter(Objects::nonNull).forEach(b -> b.setStyleClass(null));
			this.set.clear();
		}
		else
		{
			this.currentState = TreeItemState.UPDATE;
			this.set.stream().map(BeanWithMark::getBean).filter(Objects::nonNull).forEach(b -> b.setStyleClass(CssVariables.COLOR_ADD));
			this.set.forEach(b -> b.setState(this.currentState));
		}
	}

	public void addRelation(ElementWizardBean bean, TreeItemState state)
	{
		if (bean != null)
		{
			bean.setStyleClass(DialogWizardController.styleByState(state));
		}
		this.set.add(new BeanWithMark(bean, state));
		this.currentState = state;
	}

	public void clearRelation(ElementWizardBean bean)
	{
		this.set.clear();
		this.currentState = null;
	}

	public boolean contains(ElementWizardBean bean)
	{
		return this.set.stream().map(BeanWithMark::getBean).anyMatch(bean::equals);
	}

	public List<BeanWithMark> getList()
	{
		return new ArrayList<>(set);
	}

	public TreeItemState getState()
	{
		return currentState;
	}

	public boolean isMarkVisible()
	{
		return markIsVisible;
	}

	public void setMarkIsVisible(boolean markIsVisible)
	{
		this.markIsVisible = markIsVisible;
	}

	public static class BeanWithMark
	{
		private ElementWizardBean bean;
		private TreeItemState state;

		public BeanWithMark(ElementWizardBean bean, TreeItemState state)
		{
			this.bean = bean;
			this.state = state;
		}

		public ElementWizardBean getBean()
		{
			return bean;
		}

		public void setBean(ElementWizardBean bean)
		{
			this.bean = bean;
		}

		public TreeItemState getState()
		{
			return state;
		}

		public void setState(TreeItemState state)
		{
			this.state = state;
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			BeanWithMark that = (BeanWithMark) o;

			return bean != null ? bean.equals(that.bean) : that.bean == null;
		}

		@Override
		public int hashCode()
		{
			return bean != null ? bean.hashCode() : 0;
		}
	}
}
