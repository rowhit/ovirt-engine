package org.ovirt.engine.ui.common.widget.tooltip;

import org.gwtbootstrap3.client.ui.Tooltip;

import com.google.gwt.user.client.ui.Widget;

/**
 * Wrapper around Bootstrap Tooltip that sets up oVirt-specific config values.
 * Use as a drop-in for b:Tooltip.
 */
public class WidgetTooltip extends Tooltip {

    public WidgetTooltip(Widget w) {
        super(w);
        init();
    }

    public WidgetTooltip() {
        super();
        init();
    }

    private void init() {
        this.setIsAnimated(TooltipConfig.IS_ANIMATED);
        this.setPlacement(TooltipConfig.PLACEMENT);
        this.setIsHtml(TooltipConfig.IS_HTML);
        this.setTrigger(TooltipConfig.TRIGGER);
        this.setShowDelayMs(TooltipConfig.SHOW_DELAY_MS);
        this.setHideDelayMs(TooltipConfig.HIDE_DELAY_MS);
        this.setContainer(TooltipConfig.CONTAINER);
    }

}