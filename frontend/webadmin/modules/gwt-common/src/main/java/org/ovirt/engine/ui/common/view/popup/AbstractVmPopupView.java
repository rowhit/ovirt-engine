package org.ovirt.engine.ui.common.view.popup;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ovirt.engine.ui.common.widget.AbstractUiCommandButton;
import org.ovirt.engine.ui.common.widget.HasUiCommandClickHandlers;
import org.ovirt.engine.ui.common.widget.HasValidation;
import org.ovirt.engine.ui.common.widget.LeftAlignedUiCommandButton;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTab;
import org.ovirt.engine.ui.common.widget.dialog.tab.DialogTabPanel;
import org.ovirt.engine.ui.common.widget.popup.AbstractVmBasedPopupPresenterWidget;
import org.ovirt.engine.ui.common.widget.uicommon.popup.AbstractVmPopupWidget;
import org.ovirt.engine.ui.uicommonweb.models.TabName;
import org.ovirt.engine.ui.uicommonweb.models.vms.UnitVmModel;
import org.ovirt.engine.ui.uicommonweb.models.vms.VmBasedWidgetSwitchModeCommand;

import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;

public abstract class AbstractVmPopupView extends AbstractModelBoundWidgetPopupView<UnitVmModel> implements
    AbstractVmBasedPopupPresenterWidget.ViewDef {

    @Inject
    public AbstractVmPopupView(EventBus eventBus, AbstractVmPopupWidget popupWidget) {
        this(eventBus, popupWidget, "760px", "580px"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public AbstractVmPopupView(EventBus eventBus, AbstractVmPopupWidget popupWidget, String width, String height) {
        super(eventBus, popupWidget, width, height);
    }

    @Override
    public void switchAttachToInstanceType(boolean isAttached) {
        if (getContentWidget() instanceof AbstractVmPopupWidget) {
            ((AbstractVmPopupWidget) getContentWidget()).switchAttachToInstanceType(isAttached);
        }
    }

    @Override
    public void switchMode(boolean isAdvanced) {
        if (getContentWidget() instanceof AbstractVmPopupWidget) {
            ((AbstractVmPopupWidget) getContentWidget()).switchMode(isAdvanced);
        }
    }

    @Override
    public void initToCreateInstanceMode() {
        if (getContentWidget() instanceof AbstractVmPopupWidget) {
            ((AbstractVmPopupWidget) getContentWidget()).initCreateInstanceMode();
        }
    }

    @Override
    public void setSpiceProxyOverrideExplanation(String explanation) {
        if (getContentWidget() instanceof AbstractVmPopupWidget) {
            ((AbstractVmPopupWidget) getContentWidget()).setSpiceProxyOverrideExplanation(explanation);
        }

    }

    @Override
    protected AbstractUiCommandButton createCommandButton(String label, String uniqueId) {
        if (VmBasedWidgetSwitchModeCommand.NAME.equals(uniqueId)) {
            return new LeftAlignedUiCommandButton(label);
        }

        return super.createCommandButton(label, uniqueId);
    }

    @Override
    public List<HasValidation> getInvalidWidgets() {
        if (getContentWidget() instanceof AbstractVmPopupWidget) {
            return ((AbstractVmPopupWidget) getContentWidget()).getInvalidWidgets();
        }

        return Collections.EMPTY_LIST;
    }

    @Override
    public DialogTabPanel getTabPanel() {
        return ((AbstractVmPopupWidget) getContentWidget()).getTabPanel();
    }

    @Override
    public Map<TabName, DialogTab> getTabNameMapping() {
        return ((AbstractVmPopupWidget) getContentWidget()).getTabNameMapping();
    }

    @Override
    public HasUiCommandClickHandlers getNumaSupportButton() {
        return ((AbstractVmPopupWidget) getContentWidget()).getNumaSupportButton();
    }
}
