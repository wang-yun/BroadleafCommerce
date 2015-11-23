/*
 * #%L
 * BroadleafCommerce Open Admin Platform
 * %%
 * Copyright (C) 2009 - 2013 Broadleaf Commerce
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.broadleafcommerce.openadmin.server.dao.provider.metadata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.common.presentation.AdminGroupPresentation;
import org.broadleafcommerce.common.presentation.AdminPresentationClass;
import org.broadleafcommerce.common.presentation.AdminTabPresentation;
import org.broadleafcommerce.common.presentation.override.AdminGroupPresentationOverride;
import org.broadleafcommerce.common.presentation.override.AdminTabPresentationOverride;
import org.broadleafcommerce.common.presentation.override.PropertyType;
import org.broadleafcommerce.common.util.BLCAnnotationUtils;
import org.broadleafcommerce.openadmin.dto.GroupMetadata;
import org.broadleafcommerce.openadmin.dto.TabMetadata;
import org.broadleafcommerce.openadmin.dto.override.FieldMetadataOverride;
import org.broadleafcommerce.openadmin.dto.override.GroupMetadataOverride;
import org.broadleafcommerce.openadmin.dto.override.MetadataOverride;
import org.broadleafcommerce.openadmin.server.dao.provider.metadata.request.AddMetadataRequest;
import org.broadleafcommerce.openadmin.server.dao.provider.metadata.request.OverrideViaAnnotationRequest;
import org.broadleafcommerce.openadmin.server.dao.provider.metadata.request.OverrideViaXmlRequest;
import org.broadleafcommerce.openadmin.server.service.type.MetadataProviderResponse;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Chris Kittrell
 */
@Component("blBasicEntityMetadataProvider")
@Scope("prototype")
public class BasicEntityMetadataProvider extends EntityMetadataProviderAdapter {

    private static final Log LOG = LogFactory.getLog(BasicEntityMetadataProvider.class);

    @Override
    public MetadataProviderResponse addTabAndGroupMetadata(AddMetadataRequest addMetadataRequest, Map<String, TabMetadata> metadata) {
        AdminPresentationClass annot = (AdminPresentationClass) BLCAnnotationUtils.getAnnotationFromEntityOrInterface(AdminPresentationClass.class, addMetadataRequest.getTargetClass());

        if (annot == null) {
            return MetadataProviderResponse.NOT_HANDLED;
        }

        for (AdminTabPresentation tabPresentation : annot.tabs()) {
            metadata.put(tabPresentation.name(), buildTabMetadata(tabPresentation, addMetadataRequest.getTargetClass(), metadata));
        }

        return MetadataProviderResponse.HANDLED;
    }

    @Override
    public MetadataProviderResponse overrideMetadataViaAnnotation(OverrideViaAnnotationRequest overrideViaAnnotationRequest, Map<String, TabMetadata> metadata) {
        AdminPresentationClass annot = (AdminPresentationClass) BLCAnnotationUtils.getAnnotationFromEntityOrInterface(AdminPresentationClass.class, overrideViaAnnotationRequest.getRequestedEntity());

        if (annot == null) {
            return MetadataProviderResponse.NOT_HANDLED;
        }

        for (AdminTabPresentationOverride tabOverride : annot.tabOverrides()) {
            TabMetadata tab = getTabFromMetadata(tabOverride.tabName(), metadata);
            if (tab != null) {
                applyTabMetadataOverrideViaAnnotation(tab, tabOverride);
            }
        }
        for (AdminGroupPresentationOverride groupOverride : annot.groupOverrides()) {
            GroupMetadata group = getGroupFromMetadata(groupOverride.groupName(), metadata);
            if (group != null) {
                applyGroupMetadataOverrideViaAnnotation(group, groupOverride);
            }
        }

        return MetadataProviderResponse.HANDLED;
    }

    @Override
    public MetadataProviderResponse overrideMetadataViaXml(OverrideViaXmlRequest overrideViaXmlRequest, Map<String, TabMetadata> metadata) {
        Map<String, MetadataOverride> overrides = getTargetedOverride(overrideViaXmlRequest.getDynamicEntityDao(), overrideViaXmlRequest.getRequestedConfigKey(), overrideViaXmlRequest.getRequestedCeilingEntity());
        if (overrides != null) {
            for (String overrideKey : overrides.keySet()) {
                MetadataOverride overrideMetadata = overrides.get(overrideKey);
                if (overrideMetadata instanceof GroupMetadataOverride) {
                    GroupMetadataOverride groupOverrideMetadata = (GroupMetadataOverride) overrideMetadata;
                    applyGroupMetadataOverrideViaXml(overrideKey, groupOverrideMetadata, metadata);
                } else if (!(overrideMetadata instanceof FieldMetadataOverride)) {
                    // Strictly applies to a Tab
                    applyTabMetadataOverrideViaXml(overrideKey, overrideMetadata, metadata);
                }
            }
        }
        return MetadataProviderResponse.HANDLED;
    }

    private void applyTabMetadataOverrideViaAnnotation(TabMetadata tab, AdminTabPresentationOverride tabOverride) {
        String stringValue = tabOverride.value();
        if (tabOverride.property().equals(PropertyType.AdminTabPresentation.NAME)) {
            tab.setTabName(stringValue);
        } else if (tabOverride.property().equals(PropertyType.AdminTabPresentation.ORDER)) {
            tab.setTabOrder(Integer.valueOf(stringValue));
        }
    }

    private void applyGroupMetadataOverrideViaAnnotation(GroupMetadata group, AdminGroupPresentationOverride groupOverride) {
        String stringValue = groupOverride.value();
        if (groupOverride.property().equals(PropertyType.AdminGroupPresentation.NAME)) {
            group.setGroupName(stringValue);
        } else if (groupOverride.property().equals(PropertyType.AdminGroupPresentation.ORDER)) {
            group.setGroupOrder(Integer.valueOf(stringValue));
        } else if (groupOverride.property().equals(PropertyType.AdminGroupPresentation.COLUMN)) {
            group.setColumn(Integer.valueOf(stringValue));
        } else if (groupOverride.property().equals(PropertyType.AdminGroupPresentation.UNTITLED)) {
            group.setUntitled(Boolean.valueOf(stringValue));
        } else if (groupOverride.property().equals(PropertyType.AdminGroupPresentation.COLLAPSED)) {
            group.setCollapsed(Boolean.valueOf(stringValue));
        } else if (groupOverride.property().equals(PropertyType.AdminGroupPresentation.TOOLTIP)) {
            group.setTooltip(stringValue);
        }
    }

    private void applyGroupMetadataOverrideViaXml(String overrideKey, GroupMetadataOverride override, Map<String, TabMetadata> metadata) {
        String[] keySplit = overrideKey.split("-@-");
        String tabName = keySplit[0];
        String groupName = keySplit[1];

        GroupMetadata group = getGroupFromMetadata(groupName, metadata);
        if (group != null) {
            if (override.getName() != null) {
                group.setGroupName(override.getName());
            }
            if (override.getOrder() != null) {
                group.setGroupOrder(override.getOrder());
            }
            if (override.getUntitled() != null) {
                group.setUntitled(override.getUntitled());
            }
            if (override.getColumn() != null) {
                group.setColumn(override.getColumn());
            }
            if (override.getCollapsed() != null) {
                group.setCollapsed(override.getCollapsed());
            }
            if (override.getTooltip() != null) {
                group.setTooltip(override.getTooltip());
            }
        } else {
            buildGroupFromMetadataOverride(groupName, tabName, override, metadata);
        }
    }

    private void buildGroupFromMetadataOverride(String groupName, String tabName, GroupMetadataOverride override, Map<String, TabMetadata> metadata) {
        TabMetadata constructedTab = getTabFromMetadata(tabName, metadata);
        groupName = override.getName() == null || override.getName().isEmpty() ? groupName : override.getName();

        GroupMetadata group = new GroupMetadata();
        group.setGroupName(groupName);
        group.setGroupOrder(override.getOrder());
        group.setColumn(override.getColumn());
        group.setUntitled(override.getUntitled());
        group.setTooltip(override.getTooltip());
        group.setCollapsed(override.getCollapsed());

        constructedTab.getGroupMetadata().put(groupName, group);
    }

    private void applyTabMetadataOverrideViaXml(String tabName, MetadataOverride override, Map<String, TabMetadata> metadata) {
        TabMetadata tab = getTabFromMetadata(tabName, metadata);
        if (tab != null) {
            if (override.getName() != null) {
                tab.setTabName(override.getName());
            }
            if (override.getOrder() != null) {
                tab.setTabOrder(override.getOrder());
            }
        } else {
            buildTabFromMetadataOverride(tabName, override, metadata);
        }
    }

    private void buildTabFromMetadataOverride(String tabName, MetadataOverride override, Map<String, TabMetadata> metadata) {
        tabName = override.getName() == null || override.getName().isEmpty() ? tabName : override.getName();

        TabMetadata tab = new TabMetadata();
        tab.setTabName(tabName);
        tab.setTabOrder(override.getOrder());
        tab.setGroupMetadata(new HashMap<String, GroupMetadata>());
        metadata.put(tabName, tab);
    }

    protected TabMetadata buildTabMetadata(AdminTabPresentation tabPresentation, Class<?> owningClass, Map<String, TabMetadata> metadata) {
        TabMetadata constructedTab = getTabFromMetadata(tabPresentation.name(), metadata);

        TabMetadata tab;
        Map<String, GroupMetadata> groupMetadataMap;
        if (constructedTab != null) {
            tab = constructedTab;
            groupMetadataMap = tab.getGroupMetadata();
        } else {
            tab = new TabMetadata();
            groupMetadataMap = new HashMap<>();
            tab.setTabName(tabPresentation.name());
            tab.setTabOrder(tabPresentation.order());
            tab.setOwningClass(owningClass.getCanonicalName());
        }

        for (AdminGroupPresentation groupPresentation : tabPresentation.groups()) {
            if (getGroupFromMetadata(groupPresentation.name(), metadata) == null) {
                groupMetadataMap.put(groupPresentation.name(), buildGroupMetadata(groupPresentation, owningClass, metadata));
            }
        }
        tab.setGroupMetadata(groupMetadataMap);

        return tab;
    }

    protected GroupMetadata buildGroupMetadata(AdminGroupPresentation groupPresentation, Class<?> owningClass, Map<String, TabMetadata> metadata) {
        GroupMetadata group = new GroupMetadata();

        group.setOwningClass(owningClass.getCanonicalName());
        group.setGroupName(groupPresentation.name());
        group.setGroupOrder(groupPresentation.order());
        group.setColumn(groupPresentation.column());
        group.setUntitled(groupPresentation.untitled());
        group.setTooltip(groupPresentation.tooltip());
        group.setCollapsed(groupPresentation.collapsed());

        return group;
    }

    private TabMetadata getTabFromMetadata(String tabName, Map<String, TabMetadata> metadata) {
        return metadata.get(tabName);
    }

    private GroupMetadata getGroupFromMetadata(String groupName, Map<String, TabMetadata> metadata) {
        for (TabMetadata tabMetadata : metadata.values()) {
            Map<String, GroupMetadata> groupMetadata = tabMetadata.getGroupMetadata();
            if (groupMetadata != null && groupMetadata.get(groupName) != null) {
                return groupMetadata.get(groupName);
            }
        }
        return null;
    }

    @Override
    public int getOrder() {
        return FieldMetadataProvider.BASIC;
    }
}