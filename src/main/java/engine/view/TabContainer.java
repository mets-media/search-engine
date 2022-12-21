package engine.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;

import java.util.HashMap;
import java.util.Map;

@Tag(Tag.DIV)
public class TabContainer extends Component implements HasComponents {
    private Tabs tabList;
    private Map<String, Tab> tabNameMap;
    private Map<Tab, VerticalLayout> tabLayoutMap;
    private VerticalLayout currentLayout;

    public TabContainer() {
        tabList = new Tabs();
        tabLayoutMap = new HashMap<>();
        tabNameMap = new HashMap<>();
        tabList.addSelectedChangeListener(event -> {
            setSelected(event.getSelectedTab());
        });
        add(tabList);
        currentLayout = new VerticalLayout();
    }

    public void setSelected(Tab tab) {
        currentLayout.setVisible(false);
        currentLayout = tabLayoutMap.get(tab);
        currentLayout.setVisible(true);
        tabList.setSelectedTab(tab);
    }

    public void setSelected(String tabName) {
        setSelected(tabNameMap.get(tabName));
    }

    public void addTabLayout(String tabName, VerticalLayout vl) {
        Tab tab = new Tab(tabName);
        tabLayoutMap.put(tab, vl);
        tabList.add(tab);
        tabNameMap.put(tabName, tab);
        currentLayout.setVisible(false);
        add(vl);
        currentLayout = vl;
        currentLayout.setVisible(true);
    }

}
