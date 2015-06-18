/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.vaadin.jmxconfiggenerator.ui.mbeans;

import com.vaadin.data.Item;
import com.vaadin.server.FontAwesome;
import org.opennms.features.vaadin.jmxconfiggenerator.data.MetaMBeanItem;
import org.opennms.features.vaadin.jmxconfiggenerator.data.StringRenderer;
import org.opennms.features.vaadin.jmxconfiggenerator.ui.IconProvider;
import org.opennms.xmlns.xsd.config.jmx_datacollection.CompAttrib;
import org.opennms.xmlns.xsd.config.jmx_datacollection.Mbean;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Markus von Rüden
 */
class MBeansItemStrategyHandler {

	private final Map<Class<?>, ItemStrategy> propertyStrategy = new HashMap<Class<?>, ItemStrategy>();
	private final Map<Class<?>, StringRenderer<?>> extractors = new HashMap<Class<?>, StringRenderer<?>>();

	public MBeansItemStrategyHandler() {
		propertyStrategy.put(Map.Entry.class, new EntryItemStrategy());
		propertyStrategy.put(Mbean.class, new MBeanItemStrategy());
		propertyStrategy.put(String.class, new StringItemStrategy());
		propertyStrategy.put(CompAttrib.class, new CompAttribItemStrategy());

		//add extractors, is needed for comparsion (so tree is sorted alphabetically)
		extractors.put(String.class, new StringRenderer<String>() {
			@Override
			public String render(String input) {
				return input;
			}
		});
		extractors.put(Mbean.class, new StringRenderer<Mbean>() {
			@Override
			public String render(Mbean input) {
				return MBeansHelper.getLeafLabel(input);
			}
		});
		extractors.put(Entry.class, new StringRenderer<Entry>() {
			@Override
			public String render(Entry entry) {
				return (String) entry.getValue();
			}
		});
	}

	protected ItemStrategy getStrategy(Class<?> clazz) {
		return MBeansHelper.getValueForClass(propertyStrategy, clazz);
	}

	protected StringRenderer getStringRenderer(Class<?> clazz) {
		return MBeansHelper.getValueForClass(extractors, clazz);
	}

	protected void setItemProperties(Item item, Object itemId) {
		if (itemId == null || item == null) return;
		final ItemStrategy strategy = getStrategy(itemId.getClass());
		strategy.setItemProperties(item, itemId);
		strategy.updateIcon(item);
	}

	protected static interface ItemStrategy {

		void setItemProperties(Item item, Object itemId);

		void handleSelectDeselect(Item item, Object itemId, boolean select);

		void updateIcon(Item item);

		void updateModel(Item item, Object itemId);
	}

	private static class StringItemStrategy implements ItemStrategy {

		@Override
		public void setItemProperties(Item item, Object itemId) {
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.ICON).setValue(IconProvider.DUMMY);
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.CAPTION).setValue(itemId);
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.TOOLTIP).setValue(itemId);
		}

		@Override
		public void handleSelectDeselect(Item item, Object itemId, boolean selected) {
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.SELECTED).setValue(selected);
		}

		@Override
		public void updateIcon(Item item) {
			MBeansItemStrategyHandler.updateIcon(item, (Boolean) item.getItemProperty(MBeansTree.MetaMBeansTreeItem.SELECTED).getValue());
		}

		@Override
		public void updateModel(Item item, Object itemId) {
			; //read only
		}
	}

	private static class EntryItemStrategy implements ItemStrategy {

		@Override
		public void setItemProperties(Item item, Object itemId) {
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.ICON).setValue(IconProvider.DUMMY);
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.CAPTION).setValue(((Map.Entry) itemId).getValue());
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.TOOLTIP).setValue(((Map.Entry) itemId).getValue());
		}

		@Override
		public void handleSelectDeselect(Item item, Object itemId, boolean selected) {
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.SELECTED).setValue(selected);
		}

		@Override
		public void updateIcon(Item item) {
			MBeansItemStrategyHandler.updateIcon(item, (Boolean) item.getItemProperty(MBeansTree.MetaMBeansTreeItem.SELECTED).getValue());
		}

		@Override
		public void updateModel(Item item, Object itemId) {
			; //read only
		}
	}

	private static class MBeanItemStrategy implements ItemStrategy {

		@Override
		public void setItemProperties(Item item, Object itemId) {
			if (!(itemId instanceof Mbean)) return;
			Mbean bean = (Mbean) itemId;
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.ICON).setValue(FontAwesome.SITEMAP); //IconProvider.getIcon(IconProvider.MBEANS_ICON));
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.TOOLTIP).setValue(bean.getObjectname());
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.CAPTION).setValue(MBeansHelper.getLeafLabel(bean));
		}

		@Override
		public void handleSelectDeselect(Item item, Object itemId, boolean selected) {
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.SELECTED).setValue(selected);
		}

		@Override
		public void updateIcon(Item item) {
			MBeansItemStrategyHandler.updateIcon(item, (Boolean) item.getItemProperty(MBeansTree.MetaMBeansTreeItem.SELECTED).getValue());
		}

		// TODO MVR wann wird das hier eigentlich aufgerufen?
		@Override
		public void updateModel(Item item, Object itemId) {
			if (itemId == null || !(itemId instanceof Mbean)) return;
			Mbean bean = (Mbean)itemId;
			bean.setName((String)item.getItemProperty(MetaMBeanItem.NAME).getValue());
		}
	}

	private static class CompAttribItemStrategy implements ItemStrategy  {

		@Override
		public void setItemProperties(Item item, Object itemId) {
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.ICON).setValue(IconProvider.DUMMY);
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.CAPTION).setValue(((CompAttrib) itemId).getName());
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.TOOLTIP).setValue(((CompAttrib) itemId).getName());
		}

		@Override
		public void handleSelectDeselect(Item item, Object itemId, boolean selected) {
			item.getItemProperty(MBeansTree.MetaMBeansTreeItem.SELECTED).setValue(selected);
		}

		@Override
		public void updateIcon(Item item) {
			MBeansItemStrategyHandler.updateIcon(item, (Boolean) item.getItemProperty(MBeansTree.MetaMBeansTreeItem.SELECTED).getValue());
		}

		@Override
		public void updateModel(Item item, Object itemId) {

		}
	}

	private static void updateIcon(Item item, boolean selected) {
		item.getItemProperty(MBeansTree.MetaMBeansTreeItem.ICON).setValue(IconProvider.getIconForSelection(selected));
	}
}
